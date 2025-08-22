package com.example.eventmanagement2.util

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.example.eventmanagement2.R
import com.example.eventmanagement2.data.model.Event
import com.google.firebase.Timestamp
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreUtils @Inject constructor(
    private val context: Context
) {
    
    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance().apply {
            // Enable offline persistence
            firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
        }
    }
    
    fun getEventsCollection() = firestore.collection(
        context.getString(R.string.db_collection_events)
    )
    
    fun getEventsCollectionForUser(userId: String) = 
        firestore.collection(context.getString(R.string.db_collection_users))
            .document(userId)
            .collection(context.getString(R.string.db_collection_events))
    
    companion object {
        // Convert Firestore Timestamp to Date
        fun timestampToDate(timestamp: com.google.firebase.Timestamp?): Date {
            return timestamp?.toDate() ?: Date()
        }
        
        // Convert Date to Firestore Timestamp
        fun dateToTimestamp(date: Date): com.google.firebase.Timestamp {
            return com.google.firebase.Timestamp(date)
        }
        
        // Convert Event to Map for Firestore
        fun eventToMap(event: Event, context: Context): Map<String, Any> {
            return mapOf(
                context.getString(R.string.db_field_title) to event.title,
                context.getString(R.string.db_field_description) to event.description,
                context.getString(R.string.db_field_date) to dateToTimestamp(event.date),
                context.getString(R.string.location) to event.location,
                context.getString(R.string.db_field_created_at) to dateToTimestamp(event.createdAt),
                context.getString(R.string.db_field_updated_at) to dateToTimestamp(event.updatedAt),
                context.getString(R.string.db_field_created_by) to event.createdBy
            )
        }
        
        // Convert Map to Event from Firestore
        fun mapToEvent(id: String, map: Map<String, Any>, context: Context): Event {
            return Event(
                id = id,
                title = map[context.getString(R.string.db_field_title)] as? String ?: "",
                description = map[context.getString(R.string.db_field_description)] as? String ?: "",
                date = timestampToDate(map[context.getString(R.string.db_field_date)] as? Timestamp),
                location = map[context.getString(R.string.location)] as? String ?: "",
                createdAt = timestampToDate(map[context.getString(R.string.db_field_created_at)] as? Timestamp),
                updatedAt = timestampToDate(map[context.getString(R.string.db_field_updated_at)] as? Timestamp),
                createdBy = map[context.getString(R.string.db_field_created_by)] as? String ?: ""
            )
        }
    }
}
