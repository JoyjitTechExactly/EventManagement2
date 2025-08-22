package com.example.eventmanagement2.ui.events

import com.example.eventmanagement2.data.model.Event

sealed class EventListState {
    object Loading : EventListState()
    data class Success(val events: List<Event>) : EventListState()
    data class Error(val message: String) : EventListState()
}