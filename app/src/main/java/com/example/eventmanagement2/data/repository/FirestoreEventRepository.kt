package com.example.eventmanagement2.data.repository

import android.content.Context
import com.example.eventmanagement2.R
import com.example.eventmanagement2.data.model.Event
import com.example.eventmanagement2.data.model.Event.Companion.toFirestore
import com.example.eventmanagement2.util.Result
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

private const val CACHE_DURATION_MS = 5 * 60 * 1000L // 5 minutes cache

@Singleton
class FirestoreEventRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val context: Context,
    private val auth: FirebaseAuth
) : EventRepository {

    private val eventsCollection =
        firestore.collection(context.getString(R.string.db_collection_events))

    private var cachedEvents: List<Event> = emptyList()
    private var lastFetchTime: Long = 0

    private val currentUserId: String
        get() = auth.currentUser?.uid ?: throw IllegalStateException("User not logged in")

    override fun getEvents(forceRefresh: Boolean): Flow<List<Event>> = callbackFlow {
        val currentTime = System.currentTimeMillis()
        val currentUserId = auth.currentUser?.uid ?: ""

        // Return cached events if we have them and don't need to force refresh
        if (!forceRefresh && cachedEvents.isNotEmpty() &&
            (currentTime - lastFetchTime < CACHE_DURATION_MS)
        ) {
            trySend(cachedEvents.filter { it.userId == currentUserId })
        }

        val subscription = eventsCollection
            .whereEqualTo(context.getString(R.string.db_field_user_id), currentUserId)
            .orderBy(context.getString(R.string.db_field_date), Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Error getting events")
                    close(error)
                    return@addSnapshotListener
                }

                val events = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        Event.fromFirestore(doc.id, doc.data ?: emptyMap(), context)
                    } catch (e: Exception) {
                        Timber.e(e, "Error parsing event document: ${doc.id}")
                        null
                    }
                } ?: emptyList()

                // Update cache
                cachedEvents = events
                lastFetchTime = currentTime

                trySend(events)
            }

        awaitClose { subscription.remove() }
    }

    override fun getUpcomingEvents(forceRefresh: Boolean): Flow<List<Event>> = callbackFlow {
        val currentTime = System.currentTimeMillis()
        val currentUserId = auth.currentUser?.uid ?: ""

        // If we have cached events and don't need to force refresh, filter them
        if (!forceRefresh && cachedEvents.isNotEmpty() &&
            (currentTime - lastFetchTime < CACHE_DURATION_MS)
        ) {
            trySend(cachedEvents.filter { it.isUpcoming() && it.userId == currentUserId })
        }

        val subscription = eventsCollection
            .whereEqualTo(context.getString(R.string.db_field_user_id), currentUserId)
            .whereGreaterThanOrEqualTo(context.getString(R.string.db_field_date), Timestamp.now())
            .orderBy(context.getString(R.string.db_field_date), Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Error getting upcoming events")
                    close(error)
                    return@addSnapshotListener
                }

                val events = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        Event.fromFirestore(doc.id, doc.data ?: emptyMap(), context)
                    } catch (e: Exception) {
                        Timber.e(e, "Error parsing upcoming event document: ${doc.id}")
                        null
                    }
                } ?: emptyList()

                trySend(events)
            }

        awaitClose { subscription.remove() }
    }

    override fun getPastEvents(forceRefresh: Boolean): Flow<List<Event>> = callbackFlow {
        val currentTime = System.currentTimeMillis()
        val currentUserId = auth.currentUser?.uid ?: ""

        // If we have cached events and don't need to force refresh, filter them
        if (!forceRefresh && cachedEvents.isNotEmpty() &&
            (currentTime - lastFetchTime < CACHE_DURATION_MS)
        ) {
            trySend(cachedEvents.filter { it.isPast() && it.userId == currentUserId })
        }

        val subscription = eventsCollection
            .whereEqualTo(context.getString(R.string.db_field_user_id), currentUserId)
            .whereLessThan(context.getString(R.string.db_field_date), Timestamp.now())
            .orderBy(context.getString(R.string.db_field_date), Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Error getting past events")
                    close(error)
                    return@addSnapshotListener
                }

                val events = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        Event.fromFirestore(doc.id, doc.data ?: emptyMap(), context)
                    } catch (e: Exception) {
                        Timber.e(e, "Error parsing past event document: ${doc.id}")
                        null
                    }
                } ?: emptyList()

                trySend(events)
            }

        awaitClose { subscription.remove() }
    }

    override fun getEventsByUser(userId: String): Flow<List<Event>> = callbackFlow {
        val subscription = eventsCollection
            .whereEqualTo(context.getString(R.string.db_field_created_by), userId)
            .orderBy(context.getString(R.string.db_field_date), Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Error getting events by user: $userId")
                    close(error)
                    return@addSnapshotListener
                }

                val events = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        Event.fromFirestore(doc.id, doc.data ?: emptyMap(), context)
                    } catch (e: Exception) {
                        Timber.e(e, "Error parsing event document: ${doc.id}")
                        null
                    }
                } ?: emptyList()

                trySend(events)
            }

        awaitClose { subscription.remove() }
    }

    override suspend fun getEventById(eventId: String): Event? {
        return try {
            val document = eventsCollection.document(eventId).get().await()
            if (document.exists()) {
                Event.fromFirestore(document.id, document.data ?: emptyMap(), context)
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting event by ID: $eventId")
            throw e
        }
    }

    override suspend fun createEvent(event: Event): Result<Unit> {
        return try {
            // Generate a new ID if one isn't provided
            val eventId = if (event.id.isBlank()) {
                eventsCollection.document().id
            } else {
                event.id
            }
            
            // Create the event with all required fields
            val newEvent = event.copy(
                id = eventId,
                createdBy = currentUserId,
                userId = currentUserId,
                createdAt = Date(),
                updatedAt = Date()
            )
            
            // Convert to Firestore map
            val eventMap = newEvent.toFirestore(context)
            
            // Create the document with the specified ID
            eventsCollection.document(eventId).set(eventMap).await()
            
            // Invalidate cache
            cachedEvents = emptyList()
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error creating event: ${e.message}")
            Result.Error(e)
        }
    }

    override suspend fun updateEvent(event: Event): Result<Unit> {
        return try {
            require(event.id.isNotBlank()) { "Cannot update event without ID" }

            // Create a mutable map of updates
            val updates = event.toFirestore(context).toMutableMap().apply {
                // Don't update createdBy and createdAt fields
                remove(context.getString(R.string.db_field_created_by))
                remove(context.getString(R.string.db_field_created_at))
            }
            
            // First update the document with all fields except updatedAt
            eventsCollection.document(event.id).update(updates).await()
            
            // Then update only the updatedAt field with server timestamp
            eventsCollection.document(event.id)
                .update(context.getString(R.string.db_field_updated_at), FieldValue.serverTimestamp())
                .await()

            // Invalidate cache
            cachedEvents = emptyList()
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error updating event ${event.id}: ${e.message}")
            Result.Error(e)
        }
    }

    override suspend fun deleteEvent(eventId: String): Result<Unit> {
        return try {
            require(eventId.isNotBlank()) { "Cannot delete event with blank ID" }

            eventsCollection.document(eventId).delete().await()
            // Invalidate cache
            cachedEvents = emptyList()
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error deleting event: $eventId: ${e.message}")
            Result.Error(e)
        }
    }
}
