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
import com.example.eventmanagement2.databinding.FragmentLoginBinding
import com.example.eventmanagement2.util.hideKeyboard
import com.example.eventmanagement2.util.showSnackbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        observeAuthState()
    }

    private fun setupViews() {
        binding.apply {
            buttonLogin.setOnClickListener { attemptLogin() }
            buttonForgotPassword.setOnClickListener { showForgotPasswordDialog() }
            buttonSignup.setOnClickListener { navigateToSignUp() }
            
            editTextPassword.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    attemptLogin()
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
            editTextEmail.addTextChangedListener(textWatcher)
            editTextPassword.addTextChangedListener(textWatcher)
        }
    }

    private fun observeAuthState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe loading state
                viewModel.isLoading.collect { isLoading ->
                    binding.progressBar.isVisible = isLoading
                    binding.buttonLogin.isEnabled = !isLoading
                    binding.buttonSignup.isEnabled = !isLoading
                    binding.buttonForgotPassword.isEnabled = !isLoading
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

    private fun attemptLogin() {
        val email = binding.editTextEmail.text.toString().trim()
        val password = binding.editTextPassword.text.toString().trim()

        if (validateInputs(email, password)) {
            binding.root.hideKeyboard()
            viewModel.signIn(email, password)
        }
    }

    private fun validateInputs(email: String, password: String): Boolean {
        var isValid = true

        if (email.isBlank()) {
            binding.inputLayoutEmail.error = getString(R.string.error_email_required)
            binding.editTextEmail.requestFocus()
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.inputLayoutEmail.error = getString(R.string.error_invalid_email)
            binding.editTextEmail.requestFocus()
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

        return isValid
    }

    private fun showForgotPasswordDialog() {
        val input = TextInputEditText(requireContext())
        input.hint = getString(R.string.hint_email)
        input.setSingleLine()
        input.inputType = EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.title_forgot_password)
            .setMessage(R.string.message_forgot_password)
            .setView(input)
            .setPositiveButton(R.string.send) { _, _ ->
                val email = input.text?.toString()?.trim()
                if (!email.isNullOrEmpty()) {
                    viewModel.sendPasswordResetEmail(email)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showError(message: String) {
        binding.root.showSnackbar(message)
    }

    private fun clearErrors() {
        binding.inputLayoutEmail.error = null
        binding.inputLayoutPassword.error = null
    }

    private fun navigateToMain() {
        findNavController().navigate(R.id.action_loginFragment_to_dashboardFragment)
    }

    private fun navigateToSignUp() {
        findNavController().navigate(R.id.action_loginFragment_to_signUpFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
