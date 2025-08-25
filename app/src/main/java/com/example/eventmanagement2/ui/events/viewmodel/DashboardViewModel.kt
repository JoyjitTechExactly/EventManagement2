package com.example.eventmanagement2.ui.events.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eventmanagement2.data.model.Event
import com.example.eventmanagement2.data.repository.EventRepository
import com.example.eventmanagement2.data.repository.FirestoreAuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val authRepository: FirestoreAuthRepository
) : ViewModel() {

    private val _allEventsState = MutableStateFlow<EventListState>(EventListState.Loading)
    val allEventsState: StateFlow<EventListState> = _allEventsState

    private val _upcomingEventsState = MutableStateFlow<EventListState>(EventListState.Loading)
    val upcomingEventsState: StateFlow<EventListState> = _upcomingEventsState

    private val _pastEventsState = MutableStateFlow<EventListState>(EventListState.Loading)
    val pastEventsState: StateFlow<EventListState> = _pastEventsState

    // New state just for chart usage
    private val _chartEventsState = MutableStateFlow<EventListState>(EventListState.Loading)
    val chartEventsState: StateFlow<EventListState> = _chartEventsState

    init {
        loadAllEvents()
        loadUpcomingEvents()
        loadPastEvents()
    }

    fun refreshAll() {
        loadAllEvents(true)
        loadUpcomingEvents(true)
        loadPastEvents(true)
    }

    private fun loadAllEvents(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _allEventsState.value = EventListState.Loading
            try {
                eventRepository.getEvents(forceRefresh).collectLatest { events ->
                    _allEventsState.value = EventListState.Success(events)
                    _chartEventsState.value = EventListState.Success(events) // also update chart
                }
            } catch (e: Exception) {
                _allEventsState.value = EventListState.Error(e.message ?: "An error occurred")
                _chartEventsState.value = EventListState.Error(e.message ?: "An error occurred")
            }
        }
    }

    private fun loadUpcomingEvents(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _upcomingEventsState.value = EventListState.Loading
            try {
                eventRepository.getUpcomingEvents(forceRefresh).collectLatest { events ->
                    _upcomingEventsState.value = EventListState.Success(events)
                }
            } catch (e: Exception) {
                _upcomingEventsState.value = EventListState.Error(e.message ?: "An error occurred")
            }
        }
    }

    private fun loadPastEvents(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _pastEventsState.value = EventListState.Loading
            try {
                eventRepository.getPastEvents(forceRefresh).collectLatest { events ->
                    _pastEventsState.value = EventListState.Success(events)
                }
            } catch (e: Exception) {
                _pastEventsState.value = EventListState.Error(e.message ?: "An error occurred")
            }
        }
    }

    fun getEventCount(state: EventListState): Int {
        return when (state) {
            is EventListState.Success -> state.events.size
            else -> 0
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }
}