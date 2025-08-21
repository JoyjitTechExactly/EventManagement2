package com.example.eventmanagement2.ui.dashboard.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.eventmanagement2.R
import com.example.eventmanagement2.data.model.Event
import java.text.SimpleDateFormat
import java.util.*

class EventAdapter(
    private val onEventClick: (Event) -> Unit
) : ListAdapter<Event, EventAdapter.EventViewHolder>(EventDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_event, parent, false)
        return EventViewHolder(view, onEventClick)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = getItem(position)
        holder.bind(event)
    }

    class EventViewHolder(
        itemView: View,
        private val onEventClick: (Event) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val eventTitle: TextView = itemView.findViewById(R.id.textTitle)
        private val eventDate: TextView = itemView.findViewById(R.id.textDateTime)
        private val eventLocation: TextView = itemView.findViewById(R.id.textLocation)
        private val eventDescription: TextView = itemView.findViewById(R.id.textDescription)
        
        private val dateFormat = SimpleDateFormat(
            itemView.context.getString(R.string.date_time_format_full), 
            Locale.getDefault()
        )
        
        fun bind(event: Event) {
            eventTitle.text = event.title
            eventDate.text = dateFormat.format(event.date)
            eventLocation.text = event.location
            eventDescription.text = event.description
            
            itemView.setOnClickListener { onEventClick(event) }
        }
    }
}

class EventDiffCallback : DiffUtil.ItemCallback<Event>() {
    override fun areItemsTheSame(oldItem: Event, newItem: Event): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Event, newItem: Event): Boolean {
        return oldItem == newItem
    }
}
