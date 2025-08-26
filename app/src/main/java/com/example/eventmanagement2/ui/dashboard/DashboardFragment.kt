package com.example.eventmanagement2.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.eventmanagement2.R
import com.example.eventmanagement2.data.model.Event
import com.example.eventmanagement2.databinding.FragmentDashboardBinding
import com.example.eventmanagement2.ui.events.EventFilterType
import com.example.eventmanagement2.ui.events.viewmodel.DashboardViewModel
import com.example.eventmanagement2.ui.events.viewmodel.EventListState
import com.example.eventmanagement2.util.ChartUtils
import com.example.eventmanagement2.util.showSnackbar
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import java.util.*

@AndroidEntryPoint
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by viewModels()

    private var allEvents: List<Event> = emptyList()
    private var selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1
    private val monthNames = arrayOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

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
        setupMonthFilterChips()
        observeViewModel()
        viewModel.refreshAll()
    }

    private fun setupViews() {
        binding.apply {
            btnAddEvent.setOnClickListener {
                val action = DashboardFragmentDirections.actionDashboardFragmentToAddOrEditEventFragment(null)
                findNavController().navigate(action)
            }

            totalEvent.setOnClickListener { navigateToEventList(EventFilterType.ALL) }
            upcomingEvent.setOnClickListener { navigateToEventList(EventFilterType.UPCOMING) }
            pastEvent.setOnClickListener { navigateToEventList(EventFilterType.PAST) }

            btnMore.setOnClickListener { view ->
                val popupMenu = PopupMenu(requireContext(), view)
                popupMenu.menuInflater.inflate(R.menu.menu_dashboard, popupMenu.menu)
                popupMenu.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
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
        // Total events
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.allEventsState.collect { state ->
                if (state is EventListState.Success) {
                    binding.totalEventsText.text = state.events.size.toString()
                }
            }
        }

        // Upcoming
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.upcomingEventsState.collect { state ->
                if (state is EventListState.Success) {
                    binding.upcomingEventsText.text = state.events.size.toString()
                }
            }
        }

        // Past
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.pastEventsState.collect { state ->
                if (state is EventListState.Success) {
                    binding.pastEventsText.text = state.events.size.toString()
                }
            }
        }

        // Chart
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.chartEventsState.collectLatest { state ->
                when (state) {
                    is EventListState.Loading -> {
                        binding.progressBar.isVisible = true
                        binding.monthlyEventsChart.visibility = View.GONE
                    }
                    is EventListState.Success -> {
                        binding.progressBar.isVisible = false
                        allEvents = state.events
                        updateChart()
                    }
                    is EventListState.Error -> {
                        binding.progressBar.isVisible = false
                        showSnackbar(state.message)
                    }
                }
            }
        }
    }

    private fun navigateToEventList(filterType: EventFilterType) {
        val action = when (filterType) {
            EventFilterType.ALL -> DashboardFragmentDirections.actionDashboardFragmentToEventListFragment(EventFilterType.ALL.name)
            EventFilterType.UPCOMING -> DashboardFragmentDirections.actionDashboardFragmentToEventListFragment(EventFilterType.UPCOMING.name)
            EventFilterType.PAST -> DashboardFragmentDirections.actionDashboardFragmentToEventListFragment(EventFilterType.PAST.name)
        }
        findNavController().navigate(action)
    }

    private fun setupMonthFilterChips() {
        binding.categoryFilterChipGroup.removeAllViews()
        
        // Get current month and year
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH) + 1 // Adding 1 since Calendar.MONTH is 0-based
        
        // Set the selected month to current month by default
        selectedMonth = currentMonth
        
        // Add chips for each month
        monthNames.forEachIndexed { index, monthName ->
            val chip = Chip(requireContext()).apply {
                id = index + 1 // Use month number as ID
                text = "${monthName.take(3)} $currentYear"
                isCheckable = true
                isChecked = (index + 1) == selectedMonth
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelLarge)
                setChipBackgroundColorResource(if (isChecked) R.color.colorSecondary else R.color.colorBackground)
                setTextColor(requireActivity().getColor(if (isChecked) R.color.white else R.color.colorSecondary))
                // Add padding to chips for better touch target
                setPadding(
                    resources.getDimensionPixelSize(R.dimen.chip_horizontal_padding),
                    resources.getDimensionPixelSize(R.dimen.chip_vertical_padding),
                    resources.getDimensionPixelSize(R.dimen.chip_horizontal_padding),
                    resources.getDimensionPixelSize(R.dimen.chip_vertical_padding)
                )
            }

            chip.setOnClickListener {
                selectedMonth = index + 1
                updateChart()
                
                // Update chip styles
                binding.categoryFilterChipGroup.children.forEach { child ->
                    if (child is Chip) {
                        val isSelected = child.id == chip.id
                        child.isChecked = isSelected
                        child.setChipBackgroundColorResource(if (isSelected) R.color.colorSecondary else R.color.colorBackground)
                        child.setTextColor(requireActivity().getColor(if (isSelected) R.color.white else R.color.colorSecondary))
                    }
                }
                
                // Smooth scroll to the selected chip with padding
                binding.chipScrollView.post {
                    val scrollX = chip.left - (binding.chipScrollView.width - chip.width) / 2
                    binding.chipScrollView.smoothScrollTo(
                        maxOf(0, scrollX),
                        0
                    )
                }
            }
            
            binding.categoryFilterChipGroup.addView(chip)
        }
        
        // Auto-scroll to current month after all chips are added and measured
        binding.categoryFilterChipGroup.post {
            val currentMonthChip = binding.categoryFilterChipGroup.findViewById<Chip>(currentMonth)
            currentMonthChip?.let { chip ->
                val scrollView = binding.chipScrollView
                val scrollTo = (chip.left + chip.width / 2) - (scrollView.width / 2)
                scrollView.smoothScrollTo(scrollTo, 0)
            }
        }
    }

    private fun updateChart() {
        if (allEvents.isEmpty()) {
            binding.monthlyEventsChart.visibility = View.GONE
            binding.noDataText.visibility = View.VISIBLE
            return
        }

        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)

        // Filter events for the selected month and year
        val filteredEvents = allEvents.filter { event ->
            try {
                val eventDate = event.date
                calendar.time = eventDate
                val eventMonth = calendar.get(Calendar.MONTH) + 1 // +1 because Calendar.MONTH is 0-based
                val eventYear = calendar.get(Calendar.YEAR)
                
                eventMonth == selectedMonth && eventYear == currentYear
            } catch (e: Exception) {
                false
            }
        }

        if (filteredEvents.isEmpty()) {
            binding.monthlyEventsChart.visibility = View.GONE
            binding.noDataText.visibility = View.VISIBLE
            binding.noDataText.text = getString(R.string.no_events_this_month)
        } else {
            binding.monthlyEventsChart.visibility = View.VISIBLE
            binding.noDataText.visibility = View.GONE
            
            // Update the chart with the filtered events
            ChartUtils.setupMonthlyEventsChart(
                binding.monthlyEventsChart,
                filteredEvents,
                selectedMonth,
                currentYear
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}