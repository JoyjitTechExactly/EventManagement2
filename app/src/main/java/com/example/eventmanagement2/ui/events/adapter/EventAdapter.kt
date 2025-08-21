package com.example.eventmanagement2.ui.events.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.eventmanagement2.R
import com.example.eventmanagement2.data.model.Event
import com.example.eventmanagement2.databinding.ItemEventBinding
import com.example.eventmanagement2.util.UiUtils
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class EventAdapter(
    private val onEventClick: (Event) -> Unit
) : ListAdapter<Event, EventAdapter.EventViewHolder>(EventDiffCallback()) {

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

        private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        private val dateTimeFormat = SimpleDateFormat("EEEE, MMM d, yyyy 'at' h:mm a", Locale.getDefault())

        fun bind(event: Event) {
            with(binding) {
                // Set event title
                textTitle.text = event.title
                
                // Set location with icon
                textLocation.text = event.location.ifEmpty { 
                    root.context.getString(R.string.no_location_selected) 
                }
                
                // Format date and time
                val now = Date()
                val daysUntil = TimeUnit.DAYS.convert(
                    event.date.time - now.time,
                    TimeUnit.MILLISECONDS
                )
                
                // Set date text with relative time
                textDate.text = when {
                    daysUntil < 0 -> root.context.getString(R.string.past_event)
                    daysUntil == 0L -> root.context.getString(R.string.today)
                    daysUntil == 1L -> root.context.getString(R.string.tomorrow)
                    daysUntil < 7 -> root.context.getString(R.string.this_week)
                    else -> dateFormat.format(event.date)
                }
                
                // Set time
                textTime.text = timeFormat.format(event.date)
                
                // Set click listener
                root.setOnClickListener { onEventClick(event) }
                
                // Set background color based on date
                val backgroundColor = when {
                    daysUntil < 0 -> R.color.past_event_background
                    daysUntil == 0L -> R.color.today_event_background
                    daysUntil <= 7 -> R.color.upcoming_event_background
                    else -> R.color.default_event_background
                }
                
                root.setCardBackgroundColor(
                    ContextCompat.getColor(root.context, backgroundColor)
                )
                
                // Set up click listener for the menu button if needed
                // buttonMenu.setOnClickListener { showPopupMenu(it, event) }
            }
        }
        
        // Add this if you want to show a popup menu for each event
        /*
        private fun showPopupMenu(view: View, event: Event) {
            val popup = PopupMenu(view.context, view)
            popup.menuInflater.inflate(R.menu.menu_event_item, popup.menu)
            
            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_edit -> {
                        onEventClick(event)
                        true
                    }
                    R.id.action_delete -> {
                        // Handle delete
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
        */
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
