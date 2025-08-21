package com.example.eventmanagement2.ui.auth

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.eventmanagement2.R
import com.example.eventmanagement2.data.model.AuthState
import com.example.eventmanagement2.databinding.FragmentSignupBinding
import com.example.eventmanagement2.util.hideKeyboard
import com.example.eventmanagement2.util.showSnackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SignUpFragment : Fragment() {

    private var _binding: FragmentSignupBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        observeAuthState()
    }

    private fun setupViews() {
        binding.apply {
            buttonSignup.setOnClickListener { attemptSignUp() }
            
            editTextConfirmPassword.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    attemptSignUp()
                    return@setOnEditorActionListener true
                }
                false
            }

            // Clear errors when typing
            val textWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    clearErrors()
                }
                override fun afterTextChanged(s: Editable?) {}
            }
            
            editTextName.addTextChangedListener(textWatcher)
            editTextEmail.addTextChangedListener(textWatcher)
            editTextPassword.addTextChangedListener(textWatcher)
            editTextConfirmPassword.addTextChangedListener(textWatcher)
            
            // Set up back button
            toolbar.setNavigationOnClickListener {
                findNavController().navigateUp()
            }
        }
    }

    private fun observeAuthState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe loading state
                viewModel.isLoading.collect { isLoading ->
                    binding.progressBar.isVisible = isLoading
                    binding.buttonSignup.isEnabled = !isLoading
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe authentication state
                viewModel.authState.collect { state ->
                    when (state) {
                        is AuthState.Authenticated -> navigateToMain()
                        is AuthState.Error -> showError(state.message)
                        else -> { /* Handle other states if needed */ }
                    }
                }
            }
        }
    }

    private fun attemptSignUp() {
        val name = binding.editTextName.text.toString().trim()
        val email = binding.editTextEmail.text.toString().trim()
        val password = binding.editTextPassword.text.toString().trim()
        val confirmPassword = binding.editTextConfirmPassword.text.toString().trim()

        if (validateInputs(name, email, password, confirmPassword)) {
            binding.root.hideKeyboard()
            viewModel.signUp(name, email, password, confirmPassword)
        }
    }

    private fun validateInputs(
        name: String,
        email: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        var isValid = true

        if (name.isBlank()) {
            binding.inputLayoutName.error = getString(R.string.error_name_required)
            binding.editTextName.requestFocus()
            isValid = false
        }

        if (email.isBlank()) {
            binding.inputLayoutEmail.error = getString(R.string.error_email_required)
            if (isValid) binding.editTextEmail.requestFocus()
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.inputLayoutEmail.error = getString(R.string.error_invalid_email)
            if (isValid) binding.editTextEmail.requestFocus()
            isValid = false
        }

        if (password.isBlank()) {
            binding.inputLayoutPassword.error = getString(R.string.error_password_required)
            if (isValid) binding.editTextPassword.requestFocus()
            isValid = false
        } else if (password.length < 6) {
            binding.inputLayoutPassword.error = getString(R.string.error_password_too_short)
            if (isValid) binding.editTextPassword.requestFocus()
            isValid = false
        }

        if (confirmPassword.isBlank()) {
            binding.inputLayoutConfirmPassword.error = getString(R.string.error_confirm_password_required)
            if (isValid) binding.editTextConfirmPassword.requestFocus()
            isValid = false
        } else if (password != confirmPassword) {
            binding.inputLayoutConfirmPassword.error = getString(R.string.error_passwords_do_not_match)
            if (isValid) binding.editTextConfirmPassword.requestFocus()
            isValid = false
        }

        return isValid
    }

    private fun showError(message: String) {
        binding.root.showSnackbar(message)
    }

    private fun clearErrors() {
        binding.inputLayoutName.error = null
        binding.inputLayoutEmail.error = null
        binding.inputLayoutPassword.error = null
        binding.inputLayoutConfirmPassword.error = null
    }

    private fun navigateToMain() {
        // Navigate to main screen and clear back stack
        findNavController().navigate(R.id.action_signUpFragment_to_dashboardFragment) {
            popUpTo(R.id.nav_graph) { inclusive = true }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
