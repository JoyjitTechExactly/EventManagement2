package com.example.eventmanagement2.ui.events.list.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.eventmanagement2.R
import com.example.eventmanagement2.data.model.Event
import com.example.eventmanagement2.databinding.ItemEventBinding
import com.example.eventmanagement2.util.gone
import com.example.eventmanagement2.util.visible
import java.text.SimpleDateFormat
import java.util.*

class EventAdapter(
    private val onEventClick: (Event) -> Unit,
    private val onDeleteClick: ((Event) -> Unit)? = null,
    private val onEditClick: ((Event) -> Unit)? = null,
) : ListAdapter<Event, EventAdapter.EventViewHolder>(EventDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemEventBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return EventViewHolder(binding, onEventClick, onDeleteClick, onEditClick)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = getItem(position)
        holder.bind(event)
    }

    class EventViewHolder(
        private val binding: ItemEventBinding,
        private val onEventClick: (Event) -> Unit,
        private val onDeleteClick: ((Event) -> Unit)?,
        private val onEditClick: ((Event) -> Unit)?,
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

        fun bind(event: Event) {
            binding.apply {
                // Set event data to views
                eventTitle.text = event.title
                eventLocation.text = event.location
                eventDate.text = dateFormat.format(event.date)
                eventTime.text = timeFormat.format(event.date)

                // Set click listeners
                root.setOnClickListener { onEventClick(event) }

                // Set up delete button
                val isUpcoming = event.isUpcoming()
                btnDelete.visibility = if (isUpcoming) View.VISIBLE else View.GONE
                
                if (isUpcoming) {
                    btnDelete.apply{
                        visible()
                        setOnClickListener {
                            onDeleteClick?.invoke(event)
                        }
                    }
                    btnEdit.apply{
                        visible()
                        setOnClickListener {
                            onEditClick?.invoke(event)
                        }
                    }
                } else {
                    btnDelete.gone()
                    btnEdit.gone()
                }
            }
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
