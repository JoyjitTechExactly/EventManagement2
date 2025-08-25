package com.example.eventmanagement2.ui.events.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eventmanagement2.R
import com.example.eventmanagement2.data.model.Event
import com.example.eventmanagement2.data.repository.EventRepository
import com.example.eventmanagement2.util.Result
import com.example.eventmanagement2.util.Result.Error
import com.example.eventmanagement2.util.Result.Loading
import com.example.eventmanagement2.util.Result.Success
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*
import javax.inject.Inject

@HiltViewModel
class AddEditEventViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _event = MutableLiveData<Event?>()
    val event: LiveData<Event?> = _event

    private val _saveResult = MutableStateFlow<Result<Unit>>(Success(Unit))
    val saveResult: StateFlow<Result<Unit>> = _saveResult

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val auth: FirebaseAuth = Firebase.auth

    private fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: throw IllegalStateException("User not logged in")
    }

    fun loadEvent(eventId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val event = eventRepository.getEventById(eventId)
                _event.value = event
            } catch (e: Exception) {
                Timber.e(e, "Error loading event $eventId")
                _event.value = null
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
                val result = if (event.id.isNotEmpty()) {
                    // Update existing event
                    eventRepository.updateEvent(event.copy(updatedAt = Date()))
                } else {
                    // Create new event
                    eventRepository.createEvent(
                        event.copy(
                            createdBy = getCurrentUserId(),
                            userId = getCurrentUserId(),
                            createdAt = Date(),
                            updatedAt = Date()
                        )
                    )
                }

                when (result) {
                    is Result.Success -> _saveResult.value = Success(Unit)
                    is Result.Error -> _saveResult.value = Error(
                        result.message ?: context.getString(
                            if (event.id.isNotEmpty()) R.string.error_updating_event
                            else R.string.error_creating_event
                        )
                    )
                    else -> _saveResult.value = Error(context.getString(R.string.unknown_error))
                }
            } catch (e: Exception) {
                _saveResult.value = Error(e.message ?: context.getString(R.string.unknown_error))
            } finally {
                _isLoading.value = false
            }
        }
    }
}