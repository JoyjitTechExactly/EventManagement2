package com.example.eventmanagement2.ui.events.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eventmanagement2.R
import com.example.eventmanagement2.data.model.Event
import com.example.eventmanagement2.data.repository.EventRepository
import com.example.eventmanagement2.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

private const val CACHE_DURATION_MS = 5 * 60 * 1000 // 5 minutes

@HiltViewModel
class EventListViewModel @Inject constructor(
    private val eventRepository: EventRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<Result<List<Event>>>(Result.Loading())
    val uiState: StateFlow<Result<List<Event>>> = _uiState.asStateFlow()

    // Expose LiveData for compatibility with older code
    private val _events = MutableLiveData<Result<List<Event>>>()
    val events: LiveData<Result<List<Event>>> = _events

    private var lastRefreshTime: Long = 0
    private var isRefreshing = false

    init {
        loadEvents()
    }

    /**
     * Load events from the repository
     * @param forceRefresh If true, ignores cache and forces a refresh from the network
     */
    fun loadEvents(forceRefresh: Boolean = false) {
        val currentTime = System.currentTimeMillis()
        
        // Don't refresh if we're already refreshing
        if (isRefreshing && !forceRefresh) return
        
        // Don't refresh if cache is still valid and we're not forcing a refresh
        if (!forceRefresh && currentTime - lastRefreshTime < CACHE_DURATION_MS) {
            return
        }
        
        isRefreshing = true
        _uiState.value = Result.Loading()
        _events.postValue(Result.Loading())
        
        viewModelScope.launch {
            try {
                eventRepository.getEvents(forceRefresh).collect { result ->
                    when (result) {
                        is Result.Success -> {
                            val sortedEvents = result.data.sortedByDescending { it.date }
                            _uiState.value = Result.Success(sortedEvents)
                            _events.postValue(Result.Success(sortedEvents))
                            lastRefreshTime = currentTime
                        }
                        is Result.Error -> {
                            val errorMessage = result.message ?: "Error loading events"
                            _uiState.value = Result.Error(errorMessage)
                            _events.postValue(Result.Error(errorMessage))
                        }
                        is Result.Loading -> {
                            _uiState.value = Result.Loading()
                            _events.postValue(Result.Loading())
                        }
                    }
                }
            } catch (e: Exception) {
                val errorMessage = e.message ?: "An unexpected error occurred"
                _uiState.value = Result.Error(errorMessage)
                _events.postValue(Result.Error(errorMessage))
            } finally {
                isRefreshing = false
            }
        }
    }
    
    /**
     * Delete an event by ID
     */
    fun deleteEvent(eventId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                eventRepository.deleteEvent(eventId)
                // Refresh the list after successful deletion
                loadEvents(forceRefresh = true)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to delete event")
            }
        }
    }
    
    /**
     * Get events filtered by a specific date
     */
    fun getEventsForDate(date: Date): List<Event> {
        return (_uiState.value as? Result.Success)?.data?.filter { event ->
            val eventDate = Calendar.getInstance().apply { time = event.date }
            val targetDate = Calendar.getInstance().apply { time = date }
            
            eventDate.get(Calendar.YEAR) == targetDate.get(Calendar.YEAR) &&
            eventDate.get(Calendar.MONTH) == targetDate.get(Calendar.MONTH) &&
            eventDate.get(Calendar.DAY_OF_MONTH) == targetDate.get(Calendar.DAY_OF_MONTH)
        } ?: emptyList()
    }
}
