package com.example.eventmanagement2.ui.events.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eventmanagement2.data.model.Event
import com.example.eventmanagement2.data.repository.EventRepository
import com.example.eventmanagement2.ui.events.EventFilterType
import com.example.eventmanagement2.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

private const val CACHE_DURATION_MS = 5 * 60 * 1000 // 5 minutes

@HiltViewModel
class EventListViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _allEvents = MutableStateFlow<List<Event>>(emptyList())
    private val _filterType = MutableStateFlow(EventFilterType.ALL)

    private val _eventsState = MutableStateFlow<EventListState>(EventListState.Loading)
    val eventsState: StateFlow<EventListState> = _eventsState

    // Shared flow for one-time events like delete success
    private val _eventDeleted = MutableSharedFlow<Result<String>>(replay = 1)
    val eventDeleted: SharedFlow<Result<String>> = _eventDeleted.asSharedFlow()

    private var lastRefreshTime: Long = 0
    private var isRefreshing = false

    init {
        // Combine all events with filter type to get filtered events
        viewModelScope.launch {
            combine(_allEvents, _filterType) { events, filterType ->
                filterEvents(events, filterType)
            }.collect { filteredEvents ->
                _eventsState.value = EventListState.Success(filteredEvents)
            }
        }
    }

    /**
     * Set the current filter type
     */
    fun setFilterType(filterType: EventFilterType) {
        _filterType.value = filterType
    }

    /**
     * Load all events
     * @param forceRefresh If true, forces a refresh from the network
     */
    fun loadAllEvents(forceRefresh: Boolean = false) {
        if (!shouldLoad(forceRefresh)) return

        _eventsState.value = EventListState.Loading
        isRefreshing = true

        viewModelScope.launch {
            eventRepository.getEvents(forceRefresh)
                .catch { e ->
                    _eventsState.value = EventListState.Error(e.message ?: "An error occurred")
                    isRefreshing = false
                }
                .collect { events ->
                    _allEvents.value = events
                    lastRefreshTime = System.currentTimeMillis()
                    isRefreshing = false
                }
        }
    }

    /**
     * Load upcoming events
     */
    fun loadUpcomingEvents(forceRefresh: Boolean = false) {
        if (!shouldLoad(forceRefresh)) return

        _eventsState.value = EventListState.Loading
        isRefreshing = true

        viewModelScope.launch {
            try {
                eventRepository.getUpcomingEvents(forceRefresh).collect { events ->
                    _allEvents.value = events
                    _filterType.value = EventFilterType.UPCOMING
                    lastRefreshTime = System.currentTimeMillis()
                    isRefreshing = false
                }
            } catch (e: Exception) {
                _eventsState.value = EventListState.Error(e.message ?: "Failed to load upcoming events")
                isRefreshing = false
            }
        }
    }

    /**
     * Load past events
     */
    fun loadPastEvents(forceRefresh: Boolean = false) {
        if (!shouldLoad(forceRefresh)) return

        _eventsState.value = EventListState.Loading
        isRefreshing = true

        viewModelScope.launch {
            try {
                eventRepository.getPastEvents(forceRefresh).collect { events ->
                    _allEvents.value = events
                    _filterType.value = EventFilterType.PAST
                    lastRefreshTime = System.currentTimeMillis()
                    isRefreshing = false
                }
            } catch (e: Exception) {
                _eventsState.value = EventListState.Error(e.message ?: "Failed to load past events")
                isRefreshing = false
            }
        }
    }

    /**
     * Load today's events
     */
    fun loadTodaysEvents(forceRefresh: Boolean = false) {
        if (!shouldLoad(forceRefresh)) return

        _eventsState.value = EventListState.Loading
        isRefreshing = true

        viewModelScope.launch {
            try {
                eventRepository.getEvents(forceRefresh).collect { events ->
                    _allEvents.value = events
                    _filterType.value = EventFilterType.ALL
                    lastRefreshTime = System.currentTimeMillis()
                    isRefreshing = false
                }
            } catch (e: Exception) {
                _eventsState.value = EventListState.Error(e.message ?: "Failed to load today's events")
                isRefreshing = false
            }
        }
    }

    private fun shouldLoad(forceRefresh: Boolean): Boolean {
        return !isRefreshing && (forceRefresh || System.currentTimeMillis() - lastRefreshTime > CACHE_DURATION_MS)
    }

    /**
     * Delete an event by ID
     */
    fun deleteEvent(eventId: String) {
        viewModelScope.launch {
            _eventDeleted.emit(Result.Loading)
            try {
                eventRepository.deleteEvent(eventId)
                _eventDeleted.emit(Result.Success("Event deleted successfully"))
                // Refresh the events list
                loadAllEvents(forceRefresh = true)
            } catch (e: Exception) {
                _eventDeleted.emit(Result.error("Failed to delete Event."))
            }
        }
    }

    /**
     * Get events filtered by a specific date
     */
    fun getEventsForDate(date: Date): List<Event> {
        return when (val state = _eventsState.value) {
            is EventListState.Success -> {
                state.events.filter { event ->
                    val eventDate = Calendar.getInstance().apply { time = event.date }
                    val targetDate = Calendar.getInstance().apply { time = date }

                    eventDate.get(Calendar.YEAR) == targetDate.get(Calendar.YEAR) &&
                            eventDate.get(Calendar.MONTH) == targetDate.get(Calendar.MONTH) &&
                            eventDate.get(Calendar.DAY_OF_MONTH) == targetDate.get(Calendar.DAY_OF_MONTH)
                }
            }

            else -> emptyList()
        }
    }

    /**
     * Filter events based on the given filter type
     */
    private fun filterEvents(events: List<Event>, filterType: EventFilterType): List<Event> {
        val currentTime = System.currentTimeMillis()

        return when (filterType) {
            EventFilterType.ALL -> events

            EventFilterType.UPCOMING -> events.filter { event ->
                event.date.time >= currentTime
            }

            EventFilterType.PAST -> events.filter { event ->
                event.date.time < currentTime
            }
        }.sortedBy { it.date } // optional: always sort by date
    }

}
