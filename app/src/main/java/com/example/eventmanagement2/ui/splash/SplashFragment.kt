package com.example.eventmanagement2.ui.splash

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.eventmanagement2.R
import com.example.eventmanagement2.data.model.AuthState
import com.example.eventmanagement2.databinding.FragmentSplashBinding
import com.example.eventmanagement2.ui.auth.AuthViewModel
import com.example.eventmanagement2.util.showError
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SplashFragment : Fragment() {

    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSplashBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Delay for the splash screen
        Handler(Looper.getMainLooper()).postDelayed({
            checkAuthState()
        }, 1500) // 1.5 seconds delay
    }

    private fun checkAuthState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe authentication state
                viewModel.authState.collect { state ->
                    when (state) {
                        is AuthState.Authenticated -> {
                            // Navigate to dashboard on successful authentication
                            val navOptions = NavOptions.Builder()
                                .setPopUpTo(R.id.splashFragment, true)
                                .build()

                            findNavController().navigate(
                                R.id.action_splashFragment_to_dashboardFragment,
                                null,
                                navOptions
                            )
                        }
                        is AuthState.Error -> {
                            // Show error message if any
                            if (state.message != null) {
                                showError(state.message)
                            }
                        }
                        AuthState.Loading -> {
                            // Handle loading state if needed
                        }
                        AuthState.Unauthenticated -> {
                            // Navigate to login if not authenticated
                            findNavController().navigate(R.id.action_splashFragment_to_loginFragment)
                        }
                        is AuthState.PasswordResetSent -> {
                            // Handle password reset sent state if needed
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
