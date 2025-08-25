package com.example.eventmanagement2.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.eventmanagement2.R
import com.example.eventmanagement2.databinding.FragmentDashboardBinding
import com.example.eventmanagement2.ui.events.EventFilterType
import com.example.eventmanagement2.ui.events.list.EventListFragmentDirections
import com.example.eventmanagement2.ui.events.viewmodel.DashboardViewModel
import com.example.eventmanagement2.ui.events.viewmodel.EventListState
import com.example.eventmanagement2.util.showSnackbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by viewModels()

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
        setupViews()
        observeViewModel()
        viewModel.refreshAll()
    }

    private fun setupViews() {
        binding.apply {
            // Set up refresh listener
            swipeRefreshLayout.setOnRefreshListener {
                viewModel.refreshAll()
            }

            // Set up FAB click listener
            btnAddEvent.setOnClickListener {
                val action = DashboardFragmentDirections.actionDashboardFragmentToAddOrEditEventFragment(null)
                findNavController().navigate(action)
            }

            // Set up click listeners for event cards
            totalEvent.setOnClickListener {
                navigateToEventList(EventFilterType.ALL)
            }

            upcomingEvent.setOnClickListener {
                navigateToEventList(EventFilterType.UPCOMING)
            }

            pastEvent.setOnClickListener {
                navigateToEventList(EventFilterType.PAST)
            }

            // Handle More button with PopupMenu
            btnMore.setOnClickListener { view ->
                val popupMenu = PopupMenu(requireContext(), view)
                popupMenu.menuInflater.inflate(R.menu.menu_dashboard, popupMenu.menu)
                popupMenu.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.action_refresh -> {
                            viewModel.refreshAll()
                            true
                        }

                        R.id.action_logout -> {
                            showLogoutConfirmation()
                            true
                        }

                        else -> false
                    }
                }
                popupMenu.show()
            }
        }
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

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.allEventsState.collect { state ->
                when (state) {
                    is EventListState.Success -> {
                        binding.totalEventsText.text = state.events.size.toString()
                    }

                    is EventListState.Error -> {
                        showSnackbar("Error loading events: ${state.message}")
                    }

                    else -> { /* Loading state handled by swipe refresh */
                    }
                }
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.upcomingEventsState.collect { state ->
                when (state) {
                    is EventListState.Success -> {
                        binding.upcomingEventsText.text = state.events.size.toString()
                    }

                    is EventListState.Error -> {
                        showSnackbar("Error loading upcoming events: ${state.message}")
                    }

                    else -> { /* Loading state handled by swipe refresh */
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.pastEventsState.collect { state ->
                when (state) {
                    is EventListState.Success -> {
                        binding.pastEventsText.text = state.events.size.toString()
                    }

                    is EventListState.Error -> {
                        showSnackbar("Error loading past events: ${state.message}")
                    }

                    else -> { /* Loading state handled by swipe refresh */
                    }
                }
            }
        }
    }

    private fun navigateToEventList(filterType: EventFilterType) {
        val action = when (filterType) {
            EventFilterType.ALL -> DashboardFragmentDirections.actionDashboardFragmentToEventListFragment(
                EventFilterType.ALL.name
            )

            EventFilterType.UPCOMING -> DashboardFragmentDirections.actionDashboardFragmentToEventListFragment(
                EventFilterType.UPCOMING.name
            )

            EventFilterType.PAST -> DashboardFragmentDirections.actionDashboardFragmentToEventListFragment(
                EventFilterType.PAST.name
            )
        }
        findNavController().navigate(action)
    }

    private fun showErrorSnackbar(message: String) {
        view?.let { view ->
            showSnackbar(message)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
