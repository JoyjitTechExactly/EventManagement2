package com.example.eventmanagement2.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkUtils @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    val isConnected: Boolean
        get() = checkNetworkConnection()
    
    private fun checkNetworkConnection(): Boolean {
        val connectivityManager = context.getSystemService(
            Context.CONNECTIVITY_SERVICE
        ) as ConnectivityManager
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo != null && networkInfo.isConnected
        }
    }
    
    /**
     * Executes the given action if there is an active network connection.
     * @param action The action to execute if connected
     * @param onDisconnected Optional action to execute if not connected
     * @return true if connected, false otherwise
     */
    inline fun ifConnected(
        action: () -> Unit,
        onDisconnected: () -> Unit
    ): Boolean {
        return if (isConnected) {
            action()
            true
        } else {
            onDisconnected.invoke()
            false
        }
    }
}
