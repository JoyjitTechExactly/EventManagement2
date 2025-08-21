package com.example.eventmanagement2.ui.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eventmanagement2.R
import com.example.eventmanagement2.data.model.Event
import com.example.eventmanagement2.data.repository.EventRepository
import com.example.eventmanagement2.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val authRepository: AuthRepository,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _currentUser = MutableStateFlow<String?>(null)
    val currentUser = _currentUser.asStateFlow()

    init {
        loadCurrentUser()
        observeAuthState()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            _currentUser.value = authRepository.getCurrentUser()?.email
        }
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            authRepository.authState.collect { authState ->
                when (authState) {
                    is com.example.eventmanagement2.data.model.AuthState.Authenticated -> {
                        _currentUser.value = authState.user.email
                        loadEvents()
                    }
                    is com.example.eventmanagement2.data.model.AuthState.Unauthenticated -> {
                        _currentUser.value = null
                        _uiState.value = DashboardUiState.Error(context.getString(R.string.error_sign_in_required))
                    }
                    else -> {
                        // Handle other states if needed
                    }
                }
            }
        }
    }

    fun refresh() {
        loadEvents()
    }

    private fun loadEvents() {
        viewModelScope.launch {
            _uiState.value = DashboardUiState.Loading
            try {
                eventRepository.getEvents().collectLatest { events ->
                    _uiState.value = DashboardUiState.Success(events)
                }
            } catch (e: Exception) {
                _uiState.value = DashboardUiState.Error(
                    e.message ?: context.getString(R.string.error_loading_events)
                )
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }
}

sealed class DashboardUiState {
    object Loading : DashboardUiState()
    data class Success(val events: List<Event>) : DashboardUiState()
    data class Error(val message: String) : DashboardUiState()
}
