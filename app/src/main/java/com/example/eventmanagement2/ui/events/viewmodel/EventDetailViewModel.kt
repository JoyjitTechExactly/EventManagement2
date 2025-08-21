package com.example.eventmanagement2.ui.events.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eventmanagement2.data.model.Event
import com.example.eventmanagement2.data.repository.EventRepository
import com.example.eventmanagement2.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EventDetailViewModel @Inject constructor(
    private val eventRepository: EventRepository
) : ViewModel() {

    private val _event = MutableLiveData<Result<Event>>()
    val event: LiveData<Result<Event>> = _event

    private val _deleteResult = MutableStateFlow<Result<Unit>>(Result.Success(Unit))
    val deleteResult: StateFlow<Result<Unit>> = _deleteResult

    fun loadEvent(eventId: String) {
        viewModelScope.launch {
            _event.value = Result.Loading
            try {
                val event = eventRepository.getEventById(eventId)
                if (event != null) {
                    _event.value = Result.Success(event)
                } else {
                    _event.value = Result.Error("Event not found")
                }
            } catch (e: Exception) {
                _event.value = Result.Error(e.message ?: "Error loading event")
            }
        }
    }

    fun deleteEvent() {
        val currentEvent = (_event.value as? Result.Success)?.data ?: run {
            _deleteResult.value = Result.Error("No event to delete")
            return
        }

        viewModelScope.launch {
            _deleteResult.value = Result.Loading
            try {
                eventRepository.deleteEvent(currentEvent.id)
                _deleteResult.value = Result.Success(Unit)
            } catch (e: Exception) {
                _deleteResult.value = Result.Error(e.message ?: "Error deleting event")
            }
        }
    }
}
