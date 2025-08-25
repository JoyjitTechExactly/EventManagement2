package com.example.eventmanagement2.ui.events

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import kotlinx.coroutines.flow.drop
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class AddOrEditEventFragment : Fragment() {

    private var _binding: FragmentAddEditEventBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddEditEventViewModel by viewModels()
    private val args: AddOrEditEventFragmentArgs by navArgs()

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    private var selectedDateTime = Calendar.getInstance().apply {
        // Set initial time to next hour
        add(Calendar.HOUR_OF_DAY, 1)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    private var currentEvent: Event? = null
    private val isEditMode get() = !args.eventId.isNullOrEmpty()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
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

        if (isEditMode) {
            binding.progressBar.isVisible = true
            binding.buttonSave.isEnabled = false
            viewModel.loadEvent(args.eventId!!)
        } else {
            updateUIForAddMode()
        }
    }

    private fun setupToolbar() {
        binding.toolbar.title = if (isEditMode) {
            getString(R.string.edit_event)
        } else {
            getString(R.string.add_event)
        }

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupClickListeners() {
        binding.inputLayoutDateTime.setEndIconOnClickListener { showDateTimePicker() }
        binding.editTextDateTime.setOnClickListener { showDateTimePicker() }
        binding.buttonSave.setOnClickListener { saveEvent() }
    }

    private fun setupObservers() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            if (event != null) {
                currentEvent = event
                populateForm(event) // this also enables save button
            } else if (isEditMode) {
                showSnackbar(getString(R.string.error_loading_event), Snackbar.LENGTH_LONG)
                disableForm()
            }
        }

        // Only toggle save button on loading state (don’t validate here)
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.isLoading.collect { isLoading ->
                binding.progressBar.isVisible = isLoading
                binding.buttonSave.isEnabled = !isLoading
            }
        }

        // Handle save result
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.saveResult.drop(1).collect { result ->
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
                    is Result.Error -> handleError(result.message)
                }
            }
        }
    }

    private fun showDateTimePicker() {
        val now = Calendar.getInstance()
        
        // Set minimum date to today
        val minDate = now.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        // Initialize with current date if not set
        if (binding.editTextDateTime.text.isNullOrEmpty()) {
            selectedDateTime.timeInMillis = now.timeInMillis
        }
        
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
        datePicker.datePicker.minDate = minDate
        datePicker.setTitle("Select Date")
        datePicker.show()
    }
    
    private fun isToday(calendar: Calendar): Boolean {
        val today = Calendar.getInstance()
        return calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
               calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
    }

    private fun showTimePicker() {
        val now = Calendar.getInstance()
        val isToday = isToday(selectedDateTime)
        
        // Set initial time values
        val initialHour = if (isToday) {
            // If today, default to current hour + 1, or next hour if minutes > 45
            val currentHour = now.get(Calendar.HOUR_OF_DAY)
            if (now.get(Calendar.MINUTE) > 45) currentHour + 2 else currentHour + 1
        } else {
            // For future dates, default to 10:00 AM
            10
        }
        
        val timePicker = TimePickerDialog(
            requireContext(),
            { _, hour, minute ->
                val selectedTime = Calendar.getInstance().apply {
                    set(Calendar.YEAR, selectedDateTime.get(Calendar.YEAR))
                    set(Calendar.MONTH, selectedDateTime.get(Calendar.MONTH))
                    set(Calendar.DAY_OF_MONTH, selectedDateTime.get(Calendar.DAY_OF_MONTH))
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                
                // If selected time is in the past, show error and don't update
                if (isToday && selectedTime.before(now)) {
                    showSnackbar("Please select a future time")
                    showTimePicker() // Show time picker again
                    return@TimePickerDialog
                }
                
                selectedDateTime.timeInMillis = selectedTime.timeInMillis
                updateDateTimeField()
            },
            initialHour,
            0, // minutes
            android.text.format.DateFormat.is24HourFormat(requireContext())
        )
        
        timePicker.setTitle("Select Time for ${dateFormat.format(selectedDateTime.time)}")
        timePicker.show()
    }

    private fun updateDateTimeField() {
        val dateTimeStr = "${dateFormat.format(selectedDateTime.time)} at ${timeFormat.format(selectedDateTime.time)}"
        binding.editTextDateTime.setText(dateTimeStr)
    }

    private fun validateForm(silent: Boolean = false): Boolean {
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
        } else if (title.length < 3) {
            binding.inputLayoutTitle.error = getString(R.string.error_title_too_short)
            isValid = false
        }

        // Validate description
        if (description.isEmpty()) {
            binding.inputLayoutDescription.error = getString(R.string.error_field_required)
            isValid = false
        } else if (description.length < 10) {
            binding.inputLayoutDescription.error = getString(R.string.error_description_too_short)
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
        } else {
            val now = Calendar.getInstance()
            // Add 1 minute buffer to account for the time it takes to fill the form
            now.add(Calendar.MINUTE, 1)
            
            if (selectedDateTime.before(now)) {
                binding.inputLayoutDateTime.error = getString(R.string.error_date_in_past)
                // Auto-correct to the nearest future time
                selectedDateTime.timeInMillis = now.timeInMillis
                updateDateTimeField()
                isValid = false
            }
        }
        
        // If not silent mode and form is invalid, show error message
        if (!silent && !isValid) {
            showSnackbar("Please fix the errors in the form")
        }
        
        return isValid
    }

    private fun saveEvent() {
        if (!validateForm()) return
        val title = binding.editTextTitle.text.toString().trim()
        val description = binding.editTextDescription.text.toString().trim()
        val location = binding.editTextLocation.text.toString().trim()

        val event = if (isEditMode && currentEvent != null) {
            currentEvent!!.copy(
                title = title,
                description = description,
                location = location,
                date = selectedDateTime.time,
                updatedAt = Date()
            )
        } else {
            Event(
                id = "",
                title = title,
                description = description,
                location = location,
                date = selectedDateTime.time,
                createdAt = Date(),
                updatedAt = Date(),
                createdBy = "",
                userId = ""
            )
        }

        binding.buttonSave.isEnabled = false
        binding.progressBar.isVisible = true
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
            // Initial date/time is already set in the property initialization
            updateDateTimeField()
        }
    }

    private fun disableForm() {
        binding.apply {
            editTextTitle.isEnabled = false
            editTextDescription.isEnabled = false
            editTextLocation.isEnabled = false
            inputLayoutDateTime.isEnabled = false
            buttonSave.isEnabled = false
            progressBar.isVisible = false
        }
    }

    private fun showSuccessMessage() {
        val message = if (isEditMode) {
            getString(R.string.event_updated_successfully)
        } else {
            getString(R.string.event_created_successfully)
        }
        showSnackbar(message, Snackbar.LENGTH_SHORT)
    }

    private fun handleError(message: String?) {
        binding.buttonSave.isEnabled = true
        binding.progressBar.isVisible = false
        val errorMessage = message ?: getString(
            if (isEditMode) R.string.error_updating_event else R.string.error_creating_event
        )
        showSnackbar(errorMessage, Snackbar.LENGTH_LONG)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}