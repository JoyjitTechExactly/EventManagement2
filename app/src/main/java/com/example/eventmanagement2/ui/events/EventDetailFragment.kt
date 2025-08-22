package com.example.eventmanagement2.ui.events

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.eventmanagement2.R
import com.example.eventmanagement2.data.model.Event
import com.example.eventmanagement2.databinding.FragmentEventDetailsBinding
import com.example.eventmanagement2.ui.events.viewmodel.EventDetailViewModel
import com.example.eventmanagement2.util.Result
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class EventDetailFragment : Fragment() {

    private var _binding: FragmentEventDetailsBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: EventDetailViewModel by viewModels()
    private val args: EventDetailFragmentArgs by navArgs()
    
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEventDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMenu()
        setupClickListeners()
        observeViewModel()
        
        // Load the event data
        viewModel.loadEvent(args.eventId)
    }

    
    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_event_detail, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_edit -> {
                        navigateToEdit()
                        true
                    }
                    R.id.action_delete -> {
                        showDeleteConfirmation()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }
    
    private fun setupClickListeners() {
        binding.btnEdit.setOnClickListener {
            navigateToEdit()
        }
    }
    
    private fun observeViewModel() {
        viewModel.event.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.content.visibility = View.GONE
                }
                is Result.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.content.visibility = View.VISIBLE
                    result.data?.let { event ->
                        populateEventDetails(event)
                    } ?: run {
                        showError("Event not found")
                        findNavController().navigateUp()
                    }
                }
                is Result.Error -> {
                    binding.progressBar.visibility = View.GONE
                    showError(result.message ?: "Error loading event")
                    findNavController().navigateUp()
                }
            }
        }

    }
    
    private fun populateEventDetails(event: Event) {
        binding.toolbar.title = event.title
        
        binding.apply {
            textTitle.text = event.title
            textDescription.text = event.description.ifEmpty { "No description" }
            textLocation.text = event.location.ifEmpty { "Location not specified" }
            textDateTime.text = dateFormat.format(event.date)
            
            // Show/hide map button based on location availability
            buttonOpenMap.visibility = if (event.location.isNotBlank()) View.VISIBLE else View.GONE
        }
    }
    
    private fun navigateToEdit() {
        args.eventId.let { eventId ->
            val action = EventDetailFragmentDirections.actionEventDetailToAddOrEditEvent(eventId)
            findNavController().navigate(action)
        }
    }
    
    private fun showDeleteConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Event")
            .setMessage("Are you sure you want to delete this event?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteEvent()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showMessage(message: String) {
        view?.let {
            Snackbar.make(it, message, Snackbar.LENGTH_SHORT).show()
        }
    }
    
    private fun showError(message: String) {
        view?.let {
            Snackbar.make(it, message, Snackbar.LENGTH_LONG).show()
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
