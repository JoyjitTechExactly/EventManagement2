package com.example.eventmanagement2.data.model

import com.example.eventmanagement2.R
import java.util.*

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Event(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val date: Date = Date(),
    val location: String = "",
    val imageUrl: String = "",
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    val createdBy: String = ""
) : Parcelable {
    /**
     * Returns true if the event is upcoming (in the future)
     */
    fun isUpcoming(): Boolean = date.after(Date())
    
    /**
     * Returns true if the event is in the past
     */
    fun isPast(): Boolean = !isUpcoming()
    
    /**
     * Returns true if the event is happening today
     */
    fun isToday(): Boolean {
        val today = Calendar.getInstance()
        val eventDate = Calendar.getInstance().apply { time = date }
        
        return today.get(Calendar.YEAR) == eventDate.get(Calendar.YEAR) &&
               today.get(Calendar.DAY_OF_YEAR) == eventDate.get(Calendar.DAY_OF_YEAR)
    }
    
    /**
     * Returns true if the event is happening tomorrow
     */
    fun isTomorrow(): Boolean {
        val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        val eventDate = Calendar.getInstance().apply { time = date }
        
        return tomorrow.get(Calendar.YEAR) == eventDate.get(Calendar.YEAR) &&
               tomorrow.get(Calendar.DAY_OF_YEAR) == eventDate.get(Calendar.DAY_OF_YEAR)
    }
    
    /**
     * Returns true if the event is happening this week
     */
    fun isThisWeek(): Boolean {
        val today = Calendar.getInstance()
        val eventDate = Calendar.getInstance().apply { time = date }
        
        // Check if same week of the year and same year
        return today.get(Calendar.WEEK_OF_YEAR) == eventDate.get(Calendar.WEEK_OF_YEAR) &&
               today.get(Calendar.YEAR) == eventDate.get(Calendar.YEAR) &&
               !isToday() && !isTomorrow()
    }
    
    companion object {
        // Helper function to create an Event from a Firestore document
        fun fromFirestore(
            id: String,
            data: Map<String, Any>,
            context: android.content.Context
        ): Event {
            return try {
                Event(
                    id = id,
                    title = data[context.getString(R.string.db_field_title)] as? String ?: "",
                    description = data[context.getString(R.string.db_field_description)] as? String ?: "",
                    date = (data[context.getString(R.string.db_field_date)] as? com.google.firebase.Timestamp)?.toDate() ?: Date(),
                    location = data[context.getString(R.string.location)] as? String ?: "",
                    imageUrl = data[context.getString(R.string.db_field_image_url)] as? String ?: "",
                    createdAt = (data[context.getString(R.string.db_field_created_at)] as? com.google.firebase.Timestamp)?.toDate() ?: Date(),
                    updatedAt = (data[context.getString(R.string.db_field_updated_at)] as? com.google.firebase.Timestamp)?.toDate() ?: Date(),
                    createdBy = data[context.getString(R.string.db_field_created_by)] as? String ?: ""
                )
            } catch (e: Exception) {
                // Log the error and return a default event
                android.util.Log.e("Event", "Error parsing event: ${e.message}")
                Event(id = id)
            }
        }
        
        // Helper function to convert Event to a Map for Firestore
        fun Event.toFirestore(context: android.content.Context): Map<String, Any> = mapOf(
            context.getString(R.string.db_field_title) to title,
            context.getString(R.string.db_field_description) to description,
            context.getString(R.string.db_field_date) to com.google.firebase.Timestamp(date),
            context.getString(R.string.location) to location,
            context.getString(R.string.db_field_image_url) to imageUrl,
            context.getString(R.string.db_field_created_at) to com.google.firebase.Timestamp(createdAt),
            context.getString(R.string.db_field_updated_at) to com.google.firebase.Timestamp(updatedAt),
            context.getString(R.string.db_field_created_by) to createdBy
        )
    }
}
