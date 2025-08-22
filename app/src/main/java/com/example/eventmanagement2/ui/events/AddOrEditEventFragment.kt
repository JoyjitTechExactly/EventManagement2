package com.example.eventmanagement2.ui.events

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
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
class AddOrEditEventFragment : Fragment() {
    private var _binding: FragmentAddEditEventBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddEditEventViewModel by viewModels()
    private val args: AddOrEditEventFragmentArgs by navArgs()

    private val dateTimeFormat = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
    private var selectedDateTime = Calendar.getInstance()
    private var currentEvent: Event? = null
    private val isEditMode get() = args.eventId?.isNotEmpty()

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
        setupObservers()
        
        if (isEditMode == true) {
            args.eventId?.let { viewModel.loadEvent(it) }
        } else {
            updateUIForAddMode()
        }
    }

    private fun setupToolbar() {
        binding.toolbar.title = if (isEditMode == true) {
            getString(R.string.edit_event)
        } else {
            getString(R.string.add_event)
        }
        
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
        }

        // Save Button
        binding.buttonSave.setOnClickListener {
            if (validateForm()) {
                saveEvent()
            }
        }
    }

    private fun setupObservers() {
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
                        showSuccessMessage()
                        findNavController().navigateUp()
                    }
                    is Result.Error -> {
                        handleError(result.message)
                    }
                }
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
        datePicker.datePicker.minDate = System.currentTimeMillis() - 1000
        datePicker.show()
    }

    private fun showTimePicker() {
        val timePicker = TimePickerDialog(
            requireContext(),
            { _, hour, minute ->
                selectedDateTime.set(Calendar.HOUR_OF_DAY, hour)
                selectedDateTime.set(Calendar.MINUTE, minute)

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

        val title = binding.editTextTitle.text.toString().trim()
        val description = binding.editTextDescription.text.toString().trim()
        val location = binding.editTextLocation.text.toString().trim()
        val dateTime = binding.editTextDateTime.text.toString().trim()

        // Reset errors
        binding.inputLayoutTitle.error = null
        binding.inputLayoutDescription.error = null
        binding.inputLayoutLocation.error = null
        binding.inputLayoutDateTime.error = null

        // Validate title
        if (title.isEmpty()) {
            binding.inputLayoutTitle.error = getString(R.string.error_field_required)
            isValid = false
        }

        // Validate description
        if (description.isEmpty()) {
            binding.inputLayoutDescription.error = getString(R.string.error_field_required)
            isValid = false
        }

        // Validate location
        if (location.isEmpty()) {
            binding.inputLayoutLocation.error = getString(R.string.error_field_required)
            isValid = false
        }

        // Validate date/time
        if (dateTime.isEmpty()) {
            binding.inputLayoutDateTime.error = getString(R.string.error_field_required)
            isValid = false
        } else if (selectedDateTime.time.before(Calendar.getInstance().time)) {
            binding.inputLayoutDateTime.error = getString(R.string.error_date_in_past)
            isValid = false
        }

        return isValid
    }

    private fun saveEvent() {
        val title = binding.editTextTitle.text.toString().trim()
        val description = binding.editTextDescription.text.toString().trim()
        val location = binding.editTextLocation.text.toString().trim()

        val event = if (isEditMode == true && currentEvent != null) {
            currentEvent!!.copy(
                title = title,
                description = description,
                location = location,
                date = selectedDateTime.time,
                updatedAt = Date()
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

    private fun populateForm(event: Event) {
        binding.apply {
            editTextTitle.setText(event.title)
            editTextDescription.setText(event.description)
            editTextLocation.setText(event.location)
            selectedDateTime.time = event.date
            updateDateTimeField()
            buttonSave.isEnabled = true
        }
    }

    private fun updateUIForAddMode() {
        binding.apply {
            buttonSave.isEnabled = true
            // Set default date to next hour
            selectedDateTime = Calendar.getInstance().apply {
                add(Calendar.HOUR_OF_DAY, 1)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            updateDateTimeField()
        }
    }

    private fun showSuccessMessage() {
        val message = if (isEditMode == true) {
            getString(R.string.event_updated_successfully)
        } else {
            getString(R.string.event_created_successfully)
        }
        showSnackbar(message)
    }

    private fun handleError(message: String?) {
        binding.apply {
            buttonSave.isEnabled = true
            progressBar.isVisible = false
        }
        val errorMessage = message ?: getString(
            if (isEditMode == true) R.string.error_updating_event
            else R.string.error_creating_event
        )
        showSnackbar(errorMessage, Snackbar.LENGTH_LONG)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
