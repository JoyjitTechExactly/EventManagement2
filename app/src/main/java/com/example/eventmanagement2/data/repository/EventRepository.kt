package com.example.eventmanagement2.data.repository

import com.example.eventmanagement2.data.model.Event
import kotlinx.coroutines.flow.Flow
import java.util.Date

interface EventRepository {
    /**
     * Get all events from the data source
     * @param forceRefresh If true, forces a refresh from the network
     */
    fun getEvents(forceRefresh: Boolean = false): Flow<List<Event>>
    
    /**
     * Get upcoming events (events with date in the future)
     */
    fun getUpcomingEvents(forceRefresh: Boolean = false): Flow<List<Event>>
    
    /**
     * Get past events (events with date in the past)
     */
    fun getPastEvents(forceRefresh: Boolean = false): Flow<List<Event>>
    
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
