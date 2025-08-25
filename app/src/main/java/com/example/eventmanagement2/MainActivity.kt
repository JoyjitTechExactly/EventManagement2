package com.example.eventmanagement2

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.navigateUp
import com.example.eventmanagement2.databinding.ActivityMainBinding
import com.example.eventmanagement2.ui.auth.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        // Set the app theme before calling super.onCreate()
        setTheme(R.style.Theme_EventManagement)
        super.onCreate(savedInstanceState)

        // Set status bar color and light icons
        window.statusBarColor = getColor(R.color.colorPrimary)
        window.decorView.systemUiVisibility = window.decorView.systemUiVisibility and 
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        enableEdgeToEdge()
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        
        // Add padding for status bar and navigation bar
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            // Check if current destination is sign in/up screens
            val isAuthScreen = when (navController.currentDestination?.id) {
                R.id.loginFragment, R.id.signUpFragment -> true
                else -> false
            }
            
            if (!isAuthScreen) {
                // Apply padding for non-auth screens
                view.setPadding(
                    view.paddingLeft,
                    systemBars.top,
                    view.paddingRight,
                    systemBars.bottom
                )
            } else {
                // Reset padding for auth screens
                view.setPadding(0, 0, 0, 0)
            }
            
            insets
        }
        
        // Listen for navigation changes to update padding
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val isAuthScreen = when (destination.id) {
                R.id.loginFragment, R.id.signUpFragment -> true
                else -> false
            }
            
            if (isAuthScreen) {
                binding.root.setPadding(0, 0, 0, 0)
            } else {
                val insets = ViewCompat.getRootWindowInsets(binding.root)
                val systemBars = insets?.getInsets(WindowInsetsCompat.Type.systemBars())
                binding.root.setPadding(
                    binding.root.paddingLeft,
                    systemBars?.top ?: 0,
                    binding.root.paddingRight,
                    systemBars?.bottom ?: 0
                )
            }
        }
    }
}
