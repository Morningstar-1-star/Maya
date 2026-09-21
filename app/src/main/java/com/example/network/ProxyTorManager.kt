package com.example.network

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI

enum class ProxyMode(val displayName: String, val description: String) {
    DIRECT("Direct (Clearnet)", "Standard direct connection with no proxy routing"),
    TOR_ORBOT("Tor Network (Orbot)", "Routes traffic through local Orbot SOCKS5 proxy (127.0.0.1:9050)"),
    TOR_LOCAL("Tor Standalone (9150)", "Routes traffic through standard Tor SOCKS5 daemon (127.0.0.1:9150)"),
    CUSTOM_SOCKS5("Custom SOCKS5 Proxy", "Custom SOCKS5 proxy server address and port"),
    CUSTOM_HTTP("Custom HTTP/HTTPS Proxy", "Custom HTTP/HTTPS proxy server address and port")
}

object ProxyTorManager {
    private const val TAG = "ProxyTorManager"
    private const val PREFS_NAME = "browser_proxy_prefs"
    private const val KEY_PROXY_MODE = "key_proxy_mode"
    private const val KEY_CUSTOM_HOST = "key_custom_host"
    private const val KEY_CUSTOM_PORT = "key_custom_port"
    private const val KEY_AUTO_ROUTE_ONION = "key_auto_route_onion"
    private const val KEY_TOR2WEB_GATEWAY = "key_tor2web_gateway"

    private lateinit var prefs: SharedPreferences

    private val _currentMode = MutableStateFlow(ProxyMode.DIRECT)
    val currentMode: StateFlow<ProxyMode> = _currentMode.asStateFlow()

    private val _customHost = MutableStateFlow("127.0.0.1")
    val customHost: StateFlow<String> = _customHost.asStateFlow()

    private val _customPort = MutableStateFlow(9050)
    val customPort: StateFlow<Int> = _customPort.asStateFlow()

    private val _autoRouteOnion = MutableStateFlow(true)
    val autoRouteOnion: StateFlow<Boolean> = _autoRouteOnion.asStateFlow()

    private val _selectedGateway = MutableStateFlow("onion.pet")
    val selectedGateway: StateFlow<String> = _selectedGateway.asStateFlow()

    private val _isProxyApplied = MutableStateFlow(false)
    val isProxyApplied: StateFlow<Boolean> = _isProxyApplied.asStateFlow()

    private val _statusMessage = MutableStateFlow("Direct connection")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _isTorDaemonRunning = MutableStateFlow<Boolean?>(null)
    val isTorDaemonRunning: StateFlow<Boolean?> = _isTorDaemonRunning.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedModeStr = prefs.getString(KEY_PROXY_MODE, ProxyMode.DIRECT.name) ?: ProxyMode.DIRECT.name
        _currentMode.value = try {
            ProxyMode.valueOf(savedModeStr)
        } catch (e: Exception) {
            ProxyMode.DIRECT
        }
        _customHost.value = prefs.getString(KEY_CUSTOM_HOST, "127.0.0.1") ?: "127.0.0.1"
        _customPort.value = prefs.getInt(KEY_CUSTOM_PORT, 9050)
        _autoRouteOnion.value = prefs.getBoolean(KEY_AUTO_ROUTE_ONION, true)
        _selectedGateway.value = prefs.getString(KEY_TOR2WEB_GATEWAY, "onion.pet") ?: "onion.pet"

        // Apply proxy to WebView if not DIRECT
        if (_currentMode.value != ProxyMode.DIRECT) {
            applyProxyConfiguration()
        }
        checkTorDaemonStatus()
    }

    fun setProxyMode(mode: ProxyMode) {
        _currentMode.value = mode
        if (::prefs.isInitialized) {
            prefs.edit().putString(KEY_PROXY_MODE, mode.name).apply()
        }
        applyProxyConfiguration()
        if (mode == ProxyMode.TOR_ORBOT || mode == ProxyMode.TOR_LOCAL) {
            checkTorDaemonStatus()
        }
    }

    fun setCustomProxy(host: String, port: Int) {
        _customHost.value = host.trim()
        _customPort.value = port
        if (::prefs.isInitialized) {
            prefs.edit()
                .putString(KEY_CUSTOM_HOST, host.trim())
                .putInt(KEY_CUSTOM_PORT, port)
                .apply()
        }
        if (_currentMode.value == ProxyMode.CUSTOM_SOCKS5 || _currentMode.value == ProxyMode.CUSTOM_HTTP) {
            applyProxyConfiguration()
        }
    }

    fun setAutoRouteOnion(enabled: Boolean) {
        _autoRouteOnion.value = enabled
        if (::prefs.isInitialized) {
            prefs.edit().putBoolean(KEY_AUTO_ROUTE_ONION, enabled).apply()
        }
    }

    fun setSelectedGateway(gateway: String) {
        _selectedGateway.value = gateway
        if (::prefs.isInitialized) {
            prefs.edit().putString(KEY_TOR2WEB_GATEWAY, gateway).apply()
        }
    }

    fun applyProxyConfiguration() {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            _statusMessage.value = "Proxy Override not supported on this Android WebView version"
            _isProxyApplied.value = false
            return
        }

        val mode = _currentMode.value
        val controller = ProxyController.getInstance()

        try {
            when (mode) {
                ProxyMode.DIRECT -> {
                    controller.clearProxyOverride(
                        { command -> command.run() },
                        {
                            _isProxyApplied.value = false
                            _statusMessage.value = "Direct Clearnet connection active"
                            Log.d(TAG, "Proxy cleared: Direct Clearnet")
                        }
                    )
                }
                ProxyMode.TOR_ORBOT -> {
                    val config = ProxyConfig.Builder()
                        .addProxyRule("socks5://127.0.0.1:9050")
                        .build()
                    controller.setProxyOverride(
                        config,
                        { command -> command.run() },
                        {
                            _isProxyApplied.value = true
                            _statusMessage.value = "Tor SOCKS5 active via Orbot (127.0.0.1:9050)"
                            Log.d(TAG, "Proxy applied: Tor Orbot (127.0.0.1:9050)")
                        }
                    )
                }
                ProxyMode.TOR_LOCAL -> {
                    val config = ProxyConfig.Builder()
                        .addProxyRule("socks5://127.0.0.1:9150")
                        .build()
                    controller.setProxyOverride(
                        config,
                        { command -> command.run() },
                        {
                            _isProxyApplied.value = true
                            _statusMessage.value = "Tor SOCKS5 active via local daemon (127.0.0.1:9150)"
                            Log.d(TAG, "Proxy applied: Tor Local (127.0.0.1:9150)")
                        }
                    )
                }
                ProxyMode.CUSTOM_SOCKS5 -> {
                    val host = _customHost.value
                    val port = _customPort.value
                    val config = ProxyConfig.Builder()
                        .addProxyRule("socks5://$host:$port")
                        .build()
                    controller.setProxyOverride(
                        config,
                        { command -> command.run() },
                        {
                            _isProxyApplied.value = true
                            _statusMessage.value = "SOCKS5 Proxy active: $host:$port"
                            Log.d(TAG, "Proxy applied: SOCKS5 $host:$port")
                        }
                    )
                }
                ProxyMode.CUSTOM_HTTP -> {
                    val host = _customHost.value
                    val port = _customPort.value
                    val config = ProxyConfig.Builder()
                        .addProxyRule("http://$host:$port")
                        .build()
                    controller.setProxyOverride(
                        config,
                        { command -> command.run() },
                        {
                            _isProxyApplied.value = true
                            _statusMessage.value = "HTTP Proxy active: $host:$port"
                            Log.d(TAG, "Proxy applied: HTTP $host:$port")
                        }
                    )
                }
            }
        } catch (e: Exception) {
            _statusMessage.value = "Error applying proxy: ${e.localizedMessage}"
            Log.e(TAG, "Failed to apply proxy", e)
        }
    }

    fun checkTorDaemonStatus() {
        CoroutineScope(Dispatchers.IO).launch {
            val port = if (_currentMode.value == ProxyMode.TOR_LOCAL) 9150 else 9050
            val isReachable = try {
                val socket = Socket()
                socket.connect(InetSocketAddress("127.0.0.1", port), 800)
                socket.close()
                true
            } catch (e: Exception) {
                false
            }
            withContext(Dispatchers.Main) {
                _isTorDaemonRunning.value = isReachable
            }
        }
    }

    /**
     * Checks if a URL contains a .onion domain
     */
    fun isOnionUrl(url: String): Boolean {
        return try {
            val clean = url.trim().lowercase()
            val uri = URI(clean)
            val host = uri.host ?: ""
            host.endsWith(".onion") || clean.contains(".onion/") || clean.contains(".onion:")
        } catch (e: Exception) {
            url.trim().lowercase().contains(".onion")
        }
    }

    /**
     * Converts a .onion URL into a Tor2Web gateway URL (e.g. .onion.pet, .onion.ws, .onion.ly)
     */
    fun convertToTor2WebGateway(url: String, gateway: String = _selectedGateway.value): String {
        var cleanUrl = url.trim()
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            cleanUrl = "http://$cleanUrl"
        }
        return try {
            val uri = URI(cleanUrl)
            val host = uri.host ?: return cleanUrl
            if (host.endsWith(".onion")) {
                val newHost = "$host.$gateway"
                // Standard gateways use HTTPS
                val pathAndQuery = buildString {
                    append(uri.rawPath ?: "")
                    if (!uri.rawQuery.isNullOrEmpty()) {
                        append("?").append(uri.rawQuery)
                    }
                    if (!uri.rawFragment.isNullOrEmpty()) {
                        append("#").append(uri.rawFragment)
                    }
                }
                "https://$newHost$pathAndQuery"
            } else {
                cleanUrl
            }
        } catch (e: Exception) {
            cleanUrl.replace(".onion", ".onion.$gateway")
        }
    }
}
