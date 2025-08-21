package com.example.eventmanagement2.ui.events

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.eventmanagement2.R
import com.example.eventmanagement2.data.model.Event
import com.example.eventmanagement2.databinding.FragmentAddEditEventBinding
import com.example.eventmanagement2.ui.events.viewmodel.AddEditEventViewModel
import com.example.eventmanagement2.util.Result
import com.example.eventmanagement2.util.UiUtils
import com.example.eventmanagement2.util.UiUtils.showSnackbar
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class AddEditEventFragment : Fragment() {

    private var _binding: FragmentAddEditEventBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: AddEditEventViewModel by viewModels()
    private val args: AddEditEventFragmentArgs by navArgs()
    
    private val dateTimeFormat = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
    private var selectedDateTime = Calendar.getInstance()
    private var isEditMode = false
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEditEventBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupToolbar()
        setupClickListeners()
        observeViewModel()
        
        // Load event if in edit mode
        args.eventId?.let { eventId ->
            isEditMode = true
            binding.toolbar.title = getString(R.string.edit_event)
            binding.buttonDelete.visibility = View.VISIBLE
            viewModel.loadEvent(eventId)
        } ?: run {
            binding.toolbar.title = getString(R.string.add_event)
            binding.buttonDelete.visibility = View.GONE
        }
    }
    
    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }
    
    private fun setupClickListeners() {
        // Date/Time Picker
        binding.inputLayoutDateTime.setEndIconOnClickListener {
            showDateTimePicker()
        }
        
        // Location Picker
        binding.inputLayoutLocation.setEndIconOnClickListener {
            // TODO: Implement location picker
            showSnackbar("Location picker will be implemented here")
        }
        
        // Save Button
        binding.buttonSave.setOnClickListener {
            saveEvent()
        }
        
        // Delete Button
        binding.buttonDelete.setOnClickListener {
            deleteEvent()
        }
    }
        setupCategorySpinner()
        setupClickListeners()
        observeViewModel()
        
        // If we're editing an existing event, load its data
        args.eventId?.let { eventId ->
            viewModel.loadEvent(eventId)
            binding.toolbar.title = getString(R.string.edit_event)
        } ?: run {
            binding.toolbar.title = getString(R.string.add_event)
            binding.buttonDelete.isVisible = false
        }
    }

    
    private fun setupCategorySpinner() {
        val categories = resources.getStringArray(R.array.event_categories).toList()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.inputLayoutCategory.setAdapter(adapter)
    }
    
    private fun setupClickListeners() {
        binding.buttonSave.setOnClickListener {
            if (validateForm()) {
                saveEvent()
            }
        }
        
        binding.buttonDelete.setOnClickListener {
            args.eventId?.let { eventId ->
                viewModel.deleteEvent(eventId)
            }
    private fun showDateTimePicker() {
        val datePicker = DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                selectedDateTime.set(year, month, day)
                showTimePicker()
            },
            selectedDateTime.get(Calendar.YEAR),
            selectedDateTime.get(Calendar.MONTH),
            selectedDateTime.get(Calendar.DAY_OF_MONTH)
        )
        // Prevent selecting past dates
        datePicker.datePicker.minDate = System.currentTimeMillis() - 1000
        datePicker.show()
    }

    private fun showTimePicker() {
        val timePicker = TimePickerDialog(
            requireContext(),
            { _, hour, minute ->
                // Update the time
                selectedDateTime.set(Calendar.HOUR_OF_DAY, hour)
                selectedDateTime.set(Calendar.MINUTE, minute)
                
                // If the selected time is in the past, set it to the next day
                if (selectedDateTime.timeInMillis < System.currentTimeMillis()) {
                    selectedDateTime.add(Calendar.DAY_OF_MONTH, 1)
                    showSnackbar("Selected time is in the past, moved to tomorrow")
                }
                
                updateDateTimeField()
            },
            selectedDateTime.get(Calendar.HOUR_OF_DAY),
            selectedDateTime.get(Calendar.MINUTE),
            false // 24-hour format
        )
        timePicker.show()
    }

    private fun updateDateTimeField() {
        binding.editTextDateTime.setText(dateTimeFormat.format(selectedDateTime.time))
    }
    
    private fun validateForm(): Boolean {
        var isValid = true
        
        if (binding.editTextTitle.text.isNullOrBlank()) {
            binding.inputLayoutTitle.error = getString(R.string.error_field_required)
            isValid = false
        } else {
            binding.inputLayoutTitle.error = null
        }
        
        if (binding.editTextDateTime.text.isNullOrBlank()) {
            binding.inputLayoutDateTime.error = getString(R.string.error_field_required)
            isValid = false
        } else {
            binding.inputLayoutDateTime.error = null
        }
        
        // Validate date is not in the past
        if (isValid && selectedDateTime.time.before(Calendar.getInstance().time)) {
            binding.inputLayoutDateTime.error = getString(R.string.error_date_in_past)
            isValid = false
        }
        
        return isValid
    }
    
    private fun saveEvent() {
        // Get field values
        val title = binding.editTextTitle.text.toString().trim()
        val description = binding.editTextDescription.text.toString().trim()
        val location = binding.editTextLocation.text.toString().trim()
        
        // Reset errors
        binding.inputLayoutTitle.error = null
        
        // Validate fields
        var hasError = false
        
        if (title.isEmpty()) {
            binding.inputLayoutTitle.error = getString(R.string.error_field_required)
            hasError = true
        }
        
        if (selectedDateTime.timeInMillis < System.currentTimeMillis()) {
            showSnackbar("Please select a future date and time")
            hasError = true
        }
        
        if (hasError) return
        
        // Create and save event
        val event = Event(
            id = args.eventId ?: "",
            title = title,
            description = description,
            date = selectedDateTime.time,
            location = location,
            category = "" // Empty since we removed categories
        )
        
        viewModel.saveEvent(event)
    }
    
    private fun observeViewModel() {
        // Observe save/update result
        viewModel.saveResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Loading -> showLoading(true)
                is Result.Success -> {
                    showLoading(false)
                    showSnackbar(
                        if (isEditMode) "Event updated successfully" 
                        else "Event created successfully"
                    )
                    findNavController().navigateUp()
                }
                is Result.Error -> {
                    showLoading(false)
                    showSnackbar(
                        result.message ?: if (isEditMode) "Error updating event" 
                        else "Error creating event"
                    )
                }
            }
        }
        
        // Observe delete result
        viewModel.deleteResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Loading -> showLoading(true)
                is Result.Success -> {
                    showLoading(false)
                    showSnackbar("Event deleted successfully")
                    findNavController().navigateUp()
                }
                is Result.Error -> {
                    showLoading(false)
                    showSnackbar(result.message ?: "Error deleting event")
                }
            }
        }
        
        // Observe event data for editing
        viewModel.event.observe(viewLifecycleOwner) { event ->
            event?.let { populateForm(it) }
        }
    }
    
    private fun populateForm(event: Event) {
        binding.apply {
            // Set basic fields
            editTextTitle.setText(event.title)
            editTextDescription.setText(event.description)
            editTextLocation.setText(event.location)
            
            // Set date and time
            selectedDateTime.time = event.date
            editTextDateTime.setText(dateTimeFormat.format(event.date))
            
            // Update UI for edit mode
            buttonDelete.visibility = View.VISIBLE
            toolbar.title = getString(R.string.edit_event)
        }
    }
    
    private fun showMessage(message: String) {
        UiUtils.showSnackbar(requireView(), message)
    }
    
    private fun showError(message: String) {
        UiUtils.showSnackbar(requireView(), message, Snackbar.LENGTH_LONG)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
