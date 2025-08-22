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
                findNavController().navigate(R.id.action_dashboardFragment_to_addEditEventFragment)
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

                totalEvent.setOnClickListener {
                    navigateToEventList(EventFilterType.ALL)
                }

                upcomingEvent.setOnClickListener {
                    navigateToEventList(EventFilterType.UPCOMING)
                }

                pastEvent.setOnClickListener {
                    navigateToEventList(EventFilterType.PAST)
                }
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
        // Observe all events state
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.allEventsState.collect { state ->
                when (state) {
                    is EventListState.Loading -> {
                        if (!binding.swipeRefreshLayout.isRefreshing) {
                            binding.progressBar.isVisible = true
                        }
                    }
                    is EventListState.Success -> {
                        binding.progressBar.isVisible = false
                        binding.swipeRefreshLayout.isRefreshing = false
                        binding.totalEventsText.text = state.events.size.toString()
                    }
                    is EventListState.Error -> {
                        binding.progressBar.isVisible = false
                        binding.swipeRefreshLayout.isRefreshing = false
                        showErrorSnackbar(state.message)
                    }
                }
            }
        }

        // Observe upcoming events state
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.upcomingEventsState.collect { state ->
                if (state is EventListState.Success) {
                    binding.upcomingEventsText.text = state.events.size.toString()
                }
            }
        }

        // Observe past events state
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.pastEventsState.collect { state ->
                if (state is EventListState.Success) {
                    binding.pastEventsText.text = state.events.size.toString()
                }
            }
        }
    }
    
    private fun navigateToEventList(type: EventFilterType) {
        // Navigate to EventListTabFragment and set the initial tab based on the type
        val action = DashboardFragmentDirections.actionDashboardFragmentToEventListFragment(type.toString())
        findNavController().navigate(action)
        
        // The actual tab switching will be handled by the ViewPager in EventListTabFragment
        // based on the initial tab position set in the adapter
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
