package com.example.eventmanagement2.ui.dashboard

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
import com.example.eventmanagement2.data.model.Event
import com.example.eventmanagement2.databinding.FragmentDashboardBinding
import com.example.eventmanagement2.ui.auth.AuthViewModel
import com.example.eventmanagement2.ui.dashboard.adapter.EventAdapter
import com.example.eventmanagement2.ui.events.EventsAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Locale

@AndroidEntryPoint
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    
    private val authViewModel: AuthViewModel by viewModels()
    private val viewModel: DashboardViewModel by viewModels()
    private lateinit var eventsAdapter: EventsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupViews()
        observeViewModel()
        viewModel.refresh()
    }
    
    private fun setupRecyclerView() {
        eventsAdapter = EventsAdapter { event ->
            // Handle event item click
            // Navigate to event details
            // findNavController().navigate(DashboardFragmentDirections.actionDashboardToEventDetails(event.id))
        }
        
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = eventsAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupViews() {
        binding.apply {
            // Set up toolbar
            toolbar.inflateMenu(R.menu.menu_dashboard)
            toolbar.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_refresh -> {
                        viewModel.refresh()
                        true
                    }
                    R.id.action_logout -> {
                        showLogoutConfirmation()
                        true
                    }
                    else -> false
                }
            }
            
            // Set up refresh FAB
            fabRefresh.setOnClickListener {
                viewModel.refresh()
            }
        }
    }
    
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.uiState.collect { state ->
                when (state) {
                    is DashboardUiState.Loading -> {
                        binding.swipeRefreshLayout.isRefreshing = true
                    }
                    is DashboardUiState.Success -> {
                        binding.swipeRefreshLayout.isRefreshing = false
                        updateEventStats(state.events)
                        updateEventList(state.events)
                    }
                    is DashboardUiState.Error -> {
                        binding.swipeRefreshLayout.isRefreshing = false
                        showError(state.message)
                    }
                }
            }
        }
    }
    
    private fun updateEventStats(events: List<Event>) {
        binding.apply {
            totalEventsText.text = events.size.toString()
            upcomingEventsText.text = events.count { it.isUpcoming() }.toString()
            pastEventsText.text = events.count { it.isPast() }.toString()
            
            // Update charts if needed
            updateCharts(events)
        }
    }
    
    private fun updateEventList(events: List<Event>) {
        // Sort events by date (newest first)
        val sortedEvents = events.sortedByDescending { it.date }
        
        // Set up RecyclerView with adapter
        val adapter = EventAdapter() { event ->
            // Handle event item click
            // navigateToEventDetails(event.id)
        }
        
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = adapter
            setHasFixedSize(true)
        }
    }
    
    private fun updateCharts(events: List<Event>) {
        // Update monthly events chart
        val monthlyEvents = events.groupBy { 
            SimpleDateFormat(
                requireContext().getString(R.string.date_format_month_year), 
                Locale.getDefault()
            ).format(it.date)
        }.mapValues { it.value.size }
        
        // Update category distribution chart
        val categoryDistribution = events.groupBy { it.category }.mapValues { it.value.size }
    }
    
    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun showLogoutConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.logout_confirmation_title)
            .setMessage(R.string.logout_confirmation_message)
            .setPositiveButton(R.string.logout) { _, _ ->
                viewModel.signOut()
                findNavController().navigateUp()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
