package com.example.eventmanagement2.ui.events

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.eventmanagement2.R
import com.example.eventmanagement2.data.model.Event
import com.example.eventmanagement2.databinding.ItemEventBinding
import java.text.SimpleDateFormat
import java.util.*

class EventsAdapter(
    private val onEventClick: (Event) -> Unit
) : ListAdapter<Event, EventsAdapter.EventViewHolder>(EventDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemEventBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return EventViewHolder(binding, onEventClick)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class EventViewHolder(
        private val binding: ItemEventBinding,
        private val onEventClick: (Event) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat(
            itemView.context.getString(R.string.date_format_month_day_year),
            Locale.getDefault()
        )
        private val timeFormat = SimpleDateFormat(
            itemView.context.getString(R.string.time_format_12h),
            Locale.getDefault()
        )

        @SuppressLint("StringFormatInvalid")
        fun bind(event: Event) {
            binding.apply {
                textTitle.text = event.title
                textDescription.text = event.description
                textDateTime.text = itemView.context.getString(
                    R.string.date_time_format_full,
                    dateFormat.format(event.date),
                    timeFormat.format(event.date)
                )
                textLocation.text = event.location

                // Set click listener
                root.setOnClickListener { onEventClick(event) }

                // Set options button click listener
                buttonOptions.setOnClickListener {
                    // Show options menu
                    showEventOptionsMenu(it, event)
                }
            }
        }

        private fun showEventOptionsMenu(view: View, event: Event) {
            // Implement options menu for the event
            // For example: edit, delete, share, etc.
        }
    }
}

private class EventDiffCallback : DiffUtil.ItemCallback<Event>() {
    override fun areItemsTheSame(oldItem: Event, newItem: Event): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Event, newItem: Event): Boolean {
        return oldItem == newItem
    }
}
