package com.autoclicker.app.util

import android.net.ConnectivityManager
import org.junit.Assert.assertEquals
import org.junit.Test

class NetworkUtilsTest {

    @Test
    fun `mapLegacyNetworkType returns NONE when no active network`() {
        val type = NetworkUtils.mapLegacyNetworkType(null)

        assertEquals(NetworkUtils.NetworkType.NONE, type)
    }

    @Test
    fun `mapLegacyNetworkType returns WIFI for wifi network`() {
        val type = NetworkUtils.mapLegacyNetworkType(ConnectivityManager.TYPE_WIFI)

        assertEquals(NetworkUtils.NetworkType.WIFI, type)
    }

    @Test
    fun `mapLegacyNetworkType returns MOBILE for mobile network`() {
        val type = NetworkUtils.mapLegacyNetworkType(ConnectivityManager.TYPE_MOBILE)

        assertEquals(NetworkUtils.NetworkType.MOBILE, type)
    }
}
