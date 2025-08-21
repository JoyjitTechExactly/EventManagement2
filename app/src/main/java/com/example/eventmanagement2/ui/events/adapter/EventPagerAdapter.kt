package com.example.eventmanagement2.ui.events.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.eventmanagement2.data.model.Event
import com.example.eventmanagement2.ui.events.EventListTabFragment

class EventPagerAdapter(fragmentActivity: FragmentActivity) : 
    FragmentStateAdapter(fragmentActivity) {

    private var events: List<Event> = emptyList()

    fun submitList(events: List<Event>) {
        this.events = events
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = 2 // Upcoming and Past tabs

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> EventListTabFragment.newInstance(true) // Upcoming events
            else -> EventListTabFragment.newInstance(false) // Past events
        }
    }

    fun getEventsForTab(position: Int): List<Event> {
        val now = System.currentTimeMillis()
        return events.filter { event ->
            val isUpcoming = event.date.time > now
            if (position == 0) isUpcoming else !isUpcoming
        }
    }
}
