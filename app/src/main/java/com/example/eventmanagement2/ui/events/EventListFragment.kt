package com.example.eventmanagement2.ui.events

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.eventmanagement2.R
import com.example.eventmanagement2.data.model.Event
import com.example.eventmanagement2.databinding.FragmentEventListBinding
import com.example.eventmanagement2.ui.events.adapter.EventAdapter
import com.example.eventmanagement2.ui.events.viewmodel.EventListViewModel
import com.example.eventmanagement2.util.Result
import com.example.eventmanagement2.util.UiUtils
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

    private var isFirstLoad = true
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupToolbar()
        setupRecyclerView()
        setupClickListeners()
        setupSwipeRefresh()
        observeEvents()
        
        // Initial load
        if (isFirstLoad) {
            loadEvents()
            isFirstLoad = false
        }
    }
    
    private fun setupToolbar() {
        binding.toolbar.title = getString(R.string.app_name)
        binding.toolbar.inflateMenu(R.menu.menu_events)
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_refresh -> {
                    loadEvents(forceRefresh = true)
                    true
                }
                R.id.action_settings -> {
                    // Navigate to settings
                    true
                }
                else -> false
            }
        }
    }
    
    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setColorSchemeResources(
            R.color.purple_500,
            R.color.teal_200,
            R.color.purple_700
        )
        
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadEvents(forceRefresh = true)
        }
    }

    private fun setupRecyclerView() {
        eventAdapter = EventAdapter { event ->
            // Navigate to edit event
            val action = EventListFragmentDirections.actionEventListToAddEditEvent(event.id)
            findNavController().navigate(action)
        }
        
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = eventAdapter
            setHasFixedSize(true)
            // Add item decoration if needed
            // addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
    }

    private fun setupClickListeners() {
        binding.fabAddEvent.setOnClickListener {
            val action = EventListFragmentDirections.actionEventListToAddEditEvent()
            findNavController().navigate(action)
        }
    }

    private fun loadEvents(forceRefresh: Boolean = false) {
        viewModel.loadEvents(forceRefresh)
    }

    private fun observeEvents() {
        viewModel.events.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Loading -> {
                    if (!binding.swipeRefreshLayout.isRefreshing) {
                        showLoading(true)
                    }
                }
                is Result.Success -> {
                    showLoading(false)
                    binding.swipeRefreshLayout.isRefreshing = false
                    
                    result.data?.let { events ->
                        if (events.isEmpty()) {
                            showEmptyView(true)
                        } else {
                            showEmptyView(false)
                            eventAdapter.submitList(events.sortedByDescending { it.date })
                        }
                    } ?: showEmptyView(true)
                }
                is Result.Error -> {
                    showLoading(false)
                    binding.swipeRefreshLayout.isRefreshing = false
                    showError(result.message ?: getString(R.string.error_loading_events))
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
            textEmpty.isVisible = show
            imageEmpty.isVisible = show
            recyclerView.isVisible = !show
            
            if (show) {
                textEmpty.text = getString(R.string.no_events_found)
                textEmptyDescription.text = getString(R.string.no_events_description)
                buttonCreateEvent.visibility = View.VISIBLE
            } else {
                buttonCreateEvent.visibility = View.GONE
            }
        }
    }
    
    private fun showError(message: String) {
        if (view != null) {
            UiUtils.showSnackbar(
                view = requireView(),
                message = message,
                duration = Snackbar.LENGTH_LONG,
                actionText = getString(R.string.retry),
                action = { loadEvents(forceRefresh = true) }
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
