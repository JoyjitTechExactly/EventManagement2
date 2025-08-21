package com.example.eventmanagement2.data.repository

import com.example.eventmanagement2.data.model.Event
import kotlinx.coroutines.flow.Flow

interface EventRepository {
    /**
     * Get all events from the data source
     */
    fun getEvents(): Flow<List<Event>>
    
    /**
     * Get a single event by its ID
     */
    suspend fun getEventById(eventId: String): Event?
    
    /**
     * Create a new event
     */
    suspend fun createEvent(event: Event): Result<Unit>
    
    /**
     * Update an existing event
     */
    suspend fun updateEvent(event: Event): Result<Unit>
    
    /**
     * Delete an event by its ID
     */
    suspend fun deleteEvent(eventId: String): Result<Unit>
    
    /**
     * Get events created by a specific user
     */
    fun getEventsByUser(userId: String): Flow<List<Event>>
}
