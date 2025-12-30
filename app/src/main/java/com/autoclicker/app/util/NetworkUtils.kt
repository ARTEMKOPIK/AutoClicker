package com.autoclicker.app.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build

/**
 * Утилита для работы с сетью
 * Проверка доступности интернета и мониторинг соединения
 */
object NetworkUtils {
    
    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private val listeners = mutableListOf<NetworkStateListener>()
    
    fun init(context: Context) {
        connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }
    
    /**
     * Проверить доступность интернета
     */
    fun isNetworkAvailable(): Boolean {
        val cm = connectivityManager ?: return false
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(network) ?: return false
            
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = cm.activeNetworkInfo
            networkInfo?.isConnected == true
        }
    }
    
    /**
     * Получить тип сети (WiFi, Mobile, etc.)
     */
    fun getNetworkType(): NetworkType {
        val cm = connectivityManager ?: return NetworkType.NONE
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = cm.activeNetwork ?: return NetworkType.NONE
            val capabilities = cm.getNetworkCapabilities(network) ?: return NetworkType.NONE
            
            when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.MOBILE
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
                else -> NetworkType.OTHER
            }
        } else {
            @Suppress("DEPRECATION")
            when (cm.activeNetworkInfo?.type) {
                ConnectivityManager.TYPE_WIFI -> NetworkType.WIFI
                ConnectivityManager.TYPE_MOBILE -> NetworkType.MOBILE
                ConnectivityManager.TYPE_ETHERNET -> NetworkType.ETHERNET
                else -> NetworkType.OTHER
            }
        }
    }
    
    /**
     * Проверить, подключен ли WiFi
     */
    fun isWifiConnected(): Boolean {
        return getNetworkType() == NetworkType.WIFI
    }
    
    /**
     * Проверить, подключен ли мобильный интернет
     */
    fun isMobileConnected(): Boolean {
        return getNetworkType() == NetworkType.MOBILE
    }
    
    /**
     * Начать мониторинг состояния сети
     */
    fun startMonitoring(context: Context) {
        if (networkCallback != null) return
        
        val cm = connectivityManager ?: run {
            init(context)
            connectivityManager
        } ?: return
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    notifyListeners(true)
                }
                
                override fun onLost(network: Network) {
                    notifyListeners(false)
                }
                
                override fun onCapabilitiesChanged(
                    network: Network,
                    capabilities: NetworkCapabilities
                ) {
                    val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                                     capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    notifyListeners(hasInternet)
                }
            }
            
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            
            cm.registerNetworkCallback(request, networkCallback!!)
        }
    }
    
    /**
     * Остановить мониторинг состояния сети
     */
    fun stopMonitoring() {
        networkCallback?.let {
            connectivityManager?.unregisterNetworkCallback(it)
            networkCallback = null
        }
    }
    
    /**
     * Добавить слушателя изменений сети
     */
    fun addListener(listener: NetworkStateListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }
    
    /**
     * Удалить слушателя изменений сети
     */
    fun removeListener(listener: NetworkStateListener) {
        listeners.remove(listener)
    }
    
    private fun notifyListeners(isConnected: Boolean) {
        listeners.forEach { it.onNetworkStateChanged(isConnected) }
    }
    
    /**
     * Получить информацию о сети для отладки
     */
    fun getNetworkInfo(): String {
        val available = isNetworkAvailable()
        val type = getNetworkType()
        return "Сеть: ${if (available) "доступна" else "недоступна"}, Тип: $type"
    }
    
    enum class NetworkType {
        NONE,
        WIFI,
        MOBILE,
        ETHERNET,
        OTHER
    }
    
    interface NetworkStateListener {
        fun onNetworkStateChanged(isConnected: Boolean)
    }
}

