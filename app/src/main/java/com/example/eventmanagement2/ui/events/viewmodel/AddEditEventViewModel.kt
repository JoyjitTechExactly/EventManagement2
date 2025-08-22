package com.example.eventmanagement2.ui.events.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eventmanagement2.data.model.Event
import com.example.eventmanagement2.data.repository.EventRepository
import com.example.eventmanagement2.util.Result
import com.example.eventmanagement2.util.Result.Error
import com.example.eventmanagement2.util.Result.Loading
import com.example.eventmanagement2.util.Result.Success
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class AddEditEventViewModel @Inject constructor(
    private val eventRepository: EventRepository
) : ViewModel() {

    private val _event = MutableLiveData<Event?>()
    val event: LiveData<Event?> = _event

    private val _saveResult = MutableStateFlow<Result<Unit>>(Success(Unit))
    val saveResult: StateFlow<Result<Unit>> = _saveResult

    private val _deleteResult = MutableStateFlow<Result<Unit>>(Success(Unit))
    val deleteResult: StateFlow<Result<Unit>> = _deleteResult

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private fun handleError(message: String? = null, default: String): String {
        return message ?: default
    }

    fun loadEvent(eventId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val event = eventRepository.getEventById(eventId)
                _event.postValue(event)
            } catch (e: Exception) {
                _event.postValue(null)
            } finally {
                _isLoading.value = false
            }
        }
    }


    fun saveEvent(event: Event) {
        viewModelScope.launch {
            _isLoading.value = true
            _saveResult.value = Loading
            try {
                if (event.id.isNotEmpty()) {
                    eventRepository.updateEvent(event)
                } else {
                    eventRepository.createEvent(event)
                }
                _saveResult.value = Success(Unit)
            } catch (e: Exception) {
                _saveResult.value = Error(handleError(e.message, "Failed to save event"))
            } finally {
                _isLoading.value = false
            }
        }
    }
}
