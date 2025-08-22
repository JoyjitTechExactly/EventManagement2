package com.example.eventmanagement2.ui.events.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.eventmanagement2.R
import com.example.eventmanagement2.data.model.Event
import com.example.eventmanagement2.databinding.FragmentEventListBinding
import com.example.eventmanagement2.ui.events.EventFilterType
import com.example.eventmanagement2.ui.events.list.adapter.EventAdapter
import com.example.eventmanagement2.ui.events.viewmodel.EventListState
import com.example.eventmanagement2.ui.events.viewmodel.EventListViewModel
import com.example.eventmanagement2.util.showSnackbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EventListFragment : Fragment() {

    private var _binding: FragmentEventListBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: EventListViewModel by viewModels()
    
    private lateinit var adapter: EventAdapter

    private var filterType: EventFilterType = EventFilterType.ALL

    private val args : EventListFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEventListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Get filter type from arguments
        val filterTypeName = args.filterType
        filterType = EventFilterType.valueOf(filterTypeName)
        
        // Set filter type in ViewModel
        viewModel.setFilterType(filterType)
        
        setupToolbar()
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
        
        // Load events based on filter
        loadEvents()
    }
    
    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        
        binding.toolbar.title = when (filterType) {
            EventFilterType.UPCOMING -> getString(R.string.upcoming_events)
            EventFilterType.PAST -> getString(R.string.past_events)
            else -> getString(R.string.all_events)
        }
    }
    
    private fun setupRecyclerView() {
        adapter = EventAdapter(
            onEventClick = { event ->
                // Navigate to event details
                findNavController().navigate(
                    EventListFragmentDirections.actionEventListFragmentToEventDetailFragment(event.id)
                )
            },
            onDeleteClick = { event ->
                showDeleteConfirmation(event)
            }
        )
        
        binding.recyclerView.apply {
            this.adapter = this@EventListFragment.adapter
            setHasFixedSize(true)
        }
        
        // Set up swipe to refresh
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadEvents(forceRefresh = true)
        }
    }
    
    private fun setupClickListeners() {
        binding.apply {
            fabAddEvent.setOnClickListener {
                val action = EventListFragmentDirections.actionEventListFragmentToAddEditEventFragment(eventId = "")
                findNavController().navigate(action)
            }
            layoutEmptyState.btnAddEvent.setOnClickListener {
                val action = EventListFragmentDirections.actionEventListFragmentToAddEditEventFragment(eventId = "")
                findNavController().navigate(action)
            }
        }
    }
    
    private fun loadEvents(forceRefresh: Boolean = false) {
        when (filterType) {
            EventFilterType.UPCOMING -> viewModel.loadUpcomingEvents(forceRefresh)
            EventFilterType.PAST -> viewModel.loadPastEvents(forceRefresh)
            EventFilterType.ALL -> viewModel.loadAllEvents(forceRefresh)
        }
    }
    
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.eventsState.collect { state ->
                    when (state) {
                        is EventListState.Loading -> {
                            if (!binding.swipeRefreshLayout.isRefreshing) {
                                showLoading(true)
                            }
                        }
                        is EventListState.Success -> {
                            showLoading(false)
                            binding.swipeRefreshLayout.isRefreshing = false
                            
                            if (state.events.isEmpty()) {
                                showEmptyState()
                            } else {
                                showEvents(state.events)
                            }
                        }
                        is EventListState.Error -> {
                            showLoading(false)
                            binding.swipeRefreshLayout.isRefreshing = false
                            showSnackbar(state.message, Snackbar.LENGTH_SHORT)
                        }
                    }
                }
            }
        }
    }
    
    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            binding.progressBar.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
            binding.layoutEmptyState.root.visibility = View.GONE
        } else {
            binding.progressBar.visibility = View.GONE
        }
    }
    
    private fun showEvents(events: List<Event>) {
        val isEmpty = events.isEmpty()
        
        if (isEmpty) {
            binding.recyclerView.visibility = View.GONE
            binding.layoutEmptyState.root.visibility = View.VISIBLE
        } else {
            binding.recyclerView.visibility = View.VISIBLE
            binding.layoutEmptyState.root.visibility = View.GONE
            adapter.submitList(events) {
                // Smooth scroll to top when data is updated
                binding.recyclerView.scrollToPosition(0)
            }
        }
    }
    
    private fun showEmptyState() {
        binding.recyclerView.isVisible = false
        binding.fabAddEvent.isVisible = false
        binding.layoutEmptyState.root.isVisible = true
        binding.layoutEmptyState.btnAddEvent.setOnClickListener{
            // Navigate to AddEditEventFragment with empty eventId for creating a new event
            val action = EventListFragmentDirections.actionEventListFragmentToAddEditEventFragment("")
            findNavController().navigate(action)
        }
    }
    
    private fun showDeleteConfirmation(event: Event) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_event)
            .setMessage(R.string.delete_event_confirmation)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteEvent(eventId = event.id)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
