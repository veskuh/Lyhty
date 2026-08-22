package net.veskuh.lyhty.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface NetworkMonitor {
    val isOnline: StateFlow<Boolean>
}

@Singleton
class NetworkMonitorImpl @Inject constructor(
    @ApplicationContext context: Context
) : NetworkMonitor {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isOnline = MutableStateFlow(checkConnectivity())
    override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    init {
        try {
            connectivityManager.registerDefaultNetworkCallback(
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        val connected = checkConnectivity()
                        LyhtyLogger.info("NetworkMonitor", "Default network AVAILABLE (connected=$connected)")
                        _isOnline.value = connected
                    }

                    override fun onCapabilitiesChanged(
                        network: Network,
                        networkCapabilities: NetworkCapabilities
                    ) {
                        val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                                (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) ||
                                 networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED))
                        LyhtyLogger.debug("NetworkMonitor", "Default network capabilities changed: hasInternet=$hasInternet")
                        _isOnline.value = hasInternet
                    }

                    override fun onLost(network: Network) {
                        val connected = checkConnectivity()
                        LyhtyLogger.warn("NetworkMonitor", "Default network LOST (activeConnectivity=$connected)")
                        _isOnline.value = connected
                    }
                }
            )
        } catch (e: Exception) {
            LyhtyLogger.warn("NetworkMonitor", "Could not register default network callback", e)
        }
    }

    private fun checkConnectivity(): Boolean {
        return try {
            val activeNetwork = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) ||
                     capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED))
        } catch (_: Exception) {
            true
        }
    }
}
