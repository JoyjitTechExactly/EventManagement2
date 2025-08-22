package com.example.eventmanagement2.data.repository

import android.content.Context
import com.example.eventmanagement2.R
import com.example.eventmanagement2.data.model.Event
import com.example.eventmanagement2.data.model.Event.Companion.toFirestore
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreEventRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val context: Context
) : EventRepository {

    private val eventsCollection = firestore.collection(context.getString(R.string.db_collection_events))

    private var cachedEvents: List<Event> = emptyList()
    private var lastFetchTime: Long = 0

    override fun getEvents(forceRefresh: Boolean): Flow<List<Event>> = callbackFlow {
        val currentTime = System.currentTimeMillis()
        
        // Return cached events if we have them and don't need to force refresh
        if (!forceRefresh && cachedEvents.isNotEmpty() && 
            (currentTime - lastFetchTime < CACHE_DURATION_MS)) {
            trySend(cachedEvents)
        }
        
        val subscription = eventsCollection
            .orderBy(context.getString(R.string.db_field_date), com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val events = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        Event.fromFirestore(doc.id, doc.data ?: emptyMap(), context)
                    } catch (e: Exception) {
                        e.printStackTrace()
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
        
        // If we have cached events and don't need to force refresh, filter them
        if (!forceRefresh && cachedEvents.isNotEmpty() && 
            (currentTime - lastFetchTime < CACHE_DURATION_MS)) {
            trySend(cachedEvents.filter { it.isUpcoming() })
        }
        
        val subscription = eventsCollection
            .whereGreaterThanOrEqualTo(context.getString(R.string.db_field_date), Date())
            .orderBy(context.getString(R.string.db_field_date), com.google.firebase.firestore.Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val events = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        Event.fromFirestore(doc.id, doc.data ?: emptyMap(), context)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                } ?: emptyList()
                
                trySend(events)
            }

        awaitClose { subscription.remove() }
    }
    
    override fun getPastEvents(forceRefresh: Boolean): Flow<List<Event>> = callbackFlow {
        val currentTime = System.currentTimeMillis()
        
        // If we have cached events and don't need to force refresh, filter them
        if (!forceRefresh && cachedEvents.isNotEmpty() && 
            (currentTime - lastFetchTime < CACHE_DURATION_MS)) {
            trySend(cachedEvents.filter { it.isPast() })
        }
        
        val subscription = eventsCollection
            .whereLessThan(context.getString(R.string.db_field_date), Date())
            .orderBy(context.getString(R.string.db_field_date), com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val events = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        Event.fromFirestore(doc.id, doc.data ?: emptyMap(), context)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                } ?: emptyList()
                
                trySend(events)
            }

        awaitClose { subscription.remove() }
    }
    
    companion object {
        private const val CACHE_DURATION_MS = 5 * 60 * 1000 // 5 minutes cache duration
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
            null
        }
    }

    override suspend fun createEvent(event: Event): Result<Unit> {
        return try {
            val eventWithId = if (event.id.isEmpty()) {
                event.copy(id = eventsCollection.document().id)
            } else {
                event
            }
            eventsCollection.document(eventWithId.id).set(eventWithId.toFirestore(context)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateEvent(event: Event): Result<Unit> {
        return try {
            eventsCollection.document(event.id).set(event.toFirestore(context)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteEvent(eventId: String): Result<Unit> {
        return try {
            eventsCollection.document(eventId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getEventsByUser(userId: String): Flow<List<Event>> = callbackFlow {
        val subscription = eventsCollection
            .whereEqualTo(context.getString(R.string.db_field_created_by), userId)
            .orderBy(context.getString(R.string.db_field_date), com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val events = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        Event.fromFirestore(doc.id, doc.data ?: emptyMap(), context)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                } ?: emptyList()
                trySend(events)
            }

        awaitClose { subscription.remove() }
    }
}
