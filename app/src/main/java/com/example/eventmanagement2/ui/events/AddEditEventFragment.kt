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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.eventmanagement2.R
import com.example.eventmanagement2.data.model.Event
import com.example.eventmanagement2.databinding.FragmentAddEditEventBinding
import com.example.eventmanagement2.ui.events.viewmodel.AddEditEventViewModel
import com.example.eventmanagement2.util.Result
import com.example.eventmanagement2.util.showSnackbar
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
    private var currentEvent: Event? = null

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
        
        android.util.Log.d("AddEditEventFragment", "onViewCreated called")
        android.util.Log.d("AddEditEventFragment", "Received eventId: ${args.eventId}")

        setupToolbar()
        initClickListeners()
        observeViewModel()

        // Set title based on mode
        binding.toolbar.title = if (args.eventId.isNullOrEmpty()) {
            android.util.Log.d("AddEditEventFragment", "Setting up in ADD mode")
            getString(R.string.add_event)
        } else {
            android.util.Log.d("AddEditEventFragment", "Setting up in EDIT mode for eventId: ${args.eventId}")
            isEditMode = true
            viewModel.loadEvent(args.eventId!!)
            getString(R.string.edit_event)
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun initClickListeners() {
        // Date/Time Picker
        binding.inputLayoutDateTime.setEndIconOnClickListener {
            showDateTimePicker()
        }

        // Location Picker
        binding.inputLayoutLocation.setEndIconOnClickListener {

        }

        // Save Button
        binding.buttonSave.setOnClickListener {
            if (validateForm()) {
                saveEvent()
            }
        }

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
            false
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
            val title = binding.editTextTitle.text.toString().trim()
            val description = binding.editTextDescription.text.toString().trim()
            val location = binding.editTextLocation.text.toString().trim()

            if (title.isEmpty()) {
                binding.inputLayoutTitle.error = getString(R.string.error_field_required)
                return false
            }
            if (description.isEmpty()) {
                binding.inputLayoutDescription.error = getString(R.string.error_field_required)
                return false
            }
            if (location.isEmpty()) {
                binding.inputLayoutLocation.error = getString(R.string.error_field_required)
                return false
            }
        }
        return true
    }

    private fun saveEvent() {
        val title = binding.editTextTitle.text.toString().trim()
        val description = binding.editTextDescription.text.toString().trim()
        val location = binding.editTextLocation.text.toString().trim()

        val event = if (isEditMode && currentEvent != null) {
            currentEvent!!.copy(
                title = title,
                description = description,
                location = location,
                date = selectedDateTime.time
            )
        } else {
            Event(
                id = "", // Will be generated by the repository
                title = title,
                description = description,
                location = location,
                date = selectedDateTime.time,
                createdAt = Date(),
                updatedAt = Date()
            )
        }

        viewModel.saveEvent(event)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            // Observe event data for editing
            viewModel.event.observe(viewLifecycleOwner) { event ->
                event?.let { 
                    currentEvent = it
                    populateForm(it)
                }
            }

            // Observe save result
            viewModel.saveResult.collect { result ->
                when (result) {
                    is Result.Loading -> {
                        binding.buttonSave.isEnabled = false
                        binding.progressBar.isVisible = true
                    }
                    is Result.Success -> {
                        binding.progressBar.isVisible = false
                        showSnackbar("Event ${if (isEditMode) "updated" else "saved"} successfully")
                        findNavController().navigateUp()
                    }
                    is Result.Error -> {
                        binding.buttonSave.isEnabled = true
                        binding.progressBar.isVisible = false
                        showSnackbar(
                            result.message ?: if (isEditMode) "Error updating event"
                            else "Error creating event"
                        )
                    }
                }
            }
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
            updateDateTimeField()

            // Enable save button
            buttonSave.isEnabled = true

            // Update UI for edit mode
            toolbar.title = getString(R.string.edit_event)
        }
    }
    
    private fun showMessage(message: String) {
        showSnackbar(message)
    }
    
    private fun showError(message: String) {
        showSnackbar( message, Snackbar.LENGTH_SHORT)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
