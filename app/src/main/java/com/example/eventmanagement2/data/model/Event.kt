package com.example.eventmanagement2.data.model

import com.example.eventmanagement2.R
import java.util.*

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import timber.log.Timber

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
    val createdBy: String = "",
    val userId: String = ""
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
        @Throws(IllegalArgumentException::class)
        fun fromFirestore(
            id: String,
            data: Map<String, Any>,
            context: android.content.Context
        ): Event {
            return try {
                require(id.isNotBlank()) { "Event ID cannot be blank" }
                
                val title = data[context.getString(R.string.db_field_title)] as? String
                require(!title.isNullOrBlank()) { "Event title is required" }
                
                val date = (data[context.getString(R.string.db_field_date)] as? com.google.firebase.Timestamp)?.toDate()
                    ?: throw IllegalArgumentException("Event date is required")
                
                // Ensure the date is not in the past (except for existing events)
                if (date.before(Date()) && id.isBlank()) {
                    throw IllegalArgumentException("Event date cannot be in the past")
                }
                
                Event(
                    id = id,
                    title = title,
                    description = data[context.getString(R.string.db_field_description)] as? String ?: "",
                    date = date,
                    location = data[context.getString(R.string.location)] as? String ?: "",
                    imageUrl = data[context.getString(R.string.db_field_image_url)] as? String ?: "",
                    createdAt = (data[context.getString(R.string.db_field_created_at)] as? com.google.firebase.Timestamp)?.toDate() ?: Date(),
                    updatedAt = (data[context.getString(R.string.db_field_updated_at)] as? com.google.firebase.Timestamp)?.toDate() ?: Date(),
                    createdBy = data[context.getString(R.string.db_field_created_by)] as? String ?: "",
                    userId = data[context.getString(R.string.db_field_user_id)] as? String ?: ""
                )
            } catch (e: Exception) {
                Timber.e(e, "Error parsing event with ID: $id")
                throw e
            }
        }
        
        // Helper function to convert Event to a Map for Firestore
        fun Event.toFirestore(context: android.content.Context): Map<String, Any> {
            require(title.isNotBlank()) { "Event title is required" }
            require(date.after(Date()) || id.isNotBlank()) { "Event date cannot be in the past" }
            
            return mapOf(
                context.getString(R.string.db_field_title) to title,
                context.getString(R.string.db_field_description) to description,
                context.getString(R.string.db_field_date) to com.google.firebase.Timestamp(date),
                context.getString(R.string.location) to location,
                context.getString(R.string.db_field_image_url) to imageUrl,
                context.getString(R.string.db_field_created_at) to com.google.firebase.Timestamp(createdAt),
                context.getString(R.string.db_field_updated_at) to com.google.firebase.Timestamp(updatedAt),
                context.getString(R.string.db_field_created_by) to createdBy,
                context.getString(R.string.db_field_user_id) to userId.ifEmpty { createdBy }
            )
        }
    }
}
