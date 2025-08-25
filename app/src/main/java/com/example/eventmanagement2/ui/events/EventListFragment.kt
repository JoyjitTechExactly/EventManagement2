package com.example.eventmanagement2.ui.events

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.eventmanagement2.R
import com.example.eventmanagement2.databinding.FragmentEventListBinding
import com.example.eventmanagement2.ui.events.list.EventListFragmentDirections
import com.example.eventmanagement2.ui.events.list.adapter.EventAdapter
import com.example.eventmanagement2.ui.events.viewmodel.EventListState
import com.example.eventmanagement2.ui.events.viewmodel.EventListViewModel
import com.example.eventmanagement2.util.Result
import com.example.eventmanagement2.util.showSnackbar
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EventListFragment : Fragment() {
    private var _binding: FragmentEventListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EventListViewModel by viewModels()
    private lateinit var eventAdapter: EventAdapter

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

        setupToolbar()
        setupRecyclerView()
        setupClickListeners()
        observeEvents()

        // Always load events when view is created
        loadEvents(forceRefresh = true)
    }

    private fun setupToolbar() {
        binding.toolbar.title = getString(R.string.events)
    }

    private fun setupRecyclerView() {
        eventAdapter = EventAdapter(
            onEventClick = { event ->
                val action = EventListFragmentDirections
                    .actionEventListFragmentToEventDetailFragment(event.id)
                findNavController().navigate(action)
            },
            onDeleteClick = { event ->
                viewModel.deleteEvent(event.id)
            },
            onEditClick = { event ->
                val action = EventListFragmentDirections
                    .actionEventListFragmentToAddOrEditEventFragment(event.id)
                findNavController().navigate(action)
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = eventAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupClickListeners() {
        binding.fabAddEvent.setOnClickListener {
            val action = EventListFragmentDirections
                .actionEventListFragmentToAddOrEditEventFragment(null)
            findNavController().navigate(action)
        }
    }

    private fun loadEvents(forceRefresh: Boolean = false) {
        viewModel.loadAllEvents(forceRefresh)
    }

    private fun observeEvents() {
        // Observe events state
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.eventsState.collect { state ->
                when (state) {
                    is EventListState.Loading -> {
                        showLoading(true)
                        binding.layoutEmptyState.root.visibility = View.GONE

                    }

                    is EventListState.Success -> {
                        showLoading(false)

                        if (state.events.isEmpty()) {
                            showEmptyView(true)
                        } else {
                            showEmptyView(false)
                            val sortedEvents = state.events.sortedByDescending { it.date }
                            eventAdapter.submitList(sortedEvents)
                            binding.recyclerView.scrollToPosition(0)
                        }
                    }

                    is EventListState.Error -> {
                        showLoading(false)
                        showError(state.message ?: getString(R.string.error_loading_events))
                        if (eventAdapter.itemCount == 0) {
                            showEmptyView(true)
                        }
                    }
                }
            }
        }

        //Observe delete events
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.eventDeleted.collect { result ->
                when (result) {
                    is Result.Loading -> showSnackbar("Deleting...")
                    is Result.Success -> {
                        showSnackbar("Event deleted successfully")
                        loadEvents(forceRefresh = true) // reload list
                    }

                    is Result.Error -> {
                        showError(result.message ?: "Error deleting event")
                    }
                }
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.isVisible = isLoading
        binding.recyclerView.isVisible = !isLoading
    }

    private fun showEmptyView(show: Boolean) {
        binding.apply {
            fabAddEvent.isVisible = true // keep FAB visible
            recyclerView.isVisible = !show
            layoutEmptyState.root.isVisible = show

            if (show) {
                layoutEmptyState.apply {
                    btnAddEvent.visibility = View.VISIBLE
                    btnAddEvent.text = getString(R.string.add_event)
                    btnAddEvent.setOnClickListener {
                        val action = EventListFragmentDirections
                            .actionEventListFragmentToAddOrEditEventFragment(null)
                        findNavController().navigate(action)
                    }
                }
            }
        }
    }

    private fun showError(message: String) {
        showSnackbar(message = message, duration = Snackbar.LENGTH_LONG)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}