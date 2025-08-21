package com.example.eventmanagement2.ui.events

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.eventmanagement2.data.model.Event
import com.example.eventmanagement2.databinding.FragmentEventListTabBinding
import com.example.eventmanagement2.ui.events.adapter.EventAdapter
import com.example.eventmanagement2.ui.events.adapter.EventPagerAdapter
import com.example.eventmanagement2.ui.events.viewmodel.EventListViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EventListTabFragment : Fragment() {

    private var _binding: FragmentEventListTabBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: EventListViewModel by activityViewModels()
    private lateinit var adapter: EventAdapter
    private var isUpcomingTab: Boolean = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEventListTabBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, arguments)
        isUpcomingTab = arguments?.getBoolean(ARG_IS_UPCOMING, true) ?: true
        setupRecyclerView()
        observeEvents()
    }

    private fun setupRecyclerView() {
        adapter = EventAdapter { event ->
            // Handle event click
            // TODO: Navigate to event details
        }
        binding.recyclerViewEvents.layoutManager = LinearLayoutManager(context)
        binding.recyclerViewEvents.adapter = adapter
    }

    private fun observeEvents() {
        viewModel.events.observe(viewLifecycleOwner) { result ->
            when (result) {
                is com.example.eventmanagement2.util.Result.Success -> {
                    val events = result.data ?: emptyList()
                    val filteredEvents = events.filter { event ->
                        val isEventUpcoming = event.date.time > System.currentTimeMillis()
                        if (isUpcomingTab) isEventUpcoming else !isEventUpcoming
                    }
                    adapter.submitList(filteredEvents)
                    
                    if (filteredEvents.isEmpty()) {
                        binding.textEmpty.visibility = View.VISIBLE
                        binding.recyclerViewEvents.visibility = View.GONE
                    } else {
                        binding.textEmpty.visibility = View.GONE
                        binding.recyclerViewEvents.visibility = View.VISIBLE
                    }
                }
                else -> {
                    // Handle other states if needed
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_IS_UPCOMING = "is_upcoming"

        fun newInstance(isUpcoming: Boolean): EventListTabFragment {
            return EventListTabFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_IS_UPCOMING, isUpcoming)
                }
            }
        }
    }
}
