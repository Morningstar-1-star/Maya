package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object PrivacyShieldManager {
    private const val PREFS_NAME = "browser_privacy_shield_prefs"
    private const val KEY_TRACKER_STRIPPER_ENABLED = "key_tracker_stripper_enabled"
    private const val KEY_CANVAS_PROTECTION_ENABLED = "key_canvas_protection_enabled"
    private const val KEY_AUDIO_PROTECTION_ENABLED = "key_audio_protection_enabled"
    private const val KEY_HARDWARE_SPOOF_ENABLED = "key_hardware_spoof_enabled"
    private const val KEY_TOTAL_STRIPPED_COUNT = "key_total_stripped_count"

    private lateinit var prefs: SharedPreferences

    private val _trackerStripperEnabled = MutableStateFlow(true)
    val trackerStripperEnabled: StateFlow<Boolean> = _trackerStripperEnabled.asStateFlow()

    private val _canvasProtectionEnabled = MutableStateFlow(true)
    val canvasProtectionEnabled: StateFlow<Boolean> = _canvasProtectionEnabled.asStateFlow()

    private val _audioProtectionEnabled = MutableStateFlow(true)
    val audioProtectionEnabled: StateFlow<Boolean> = _audioProtectionEnabled.asStateFlow()

    private val _hardwareSpoofEnabled = MutableStateFlow(true)
    val hardwareSpoofEnabled: StateFlow<Boolean> = _hardwareSpoofEnabled.asStateFlow()

    private val _totalTrackersStripped = MutableStateFlow(0)
    val totalTrackersStripped: StateFlow<Int> = _totalTrackersStripped.asStateFlow()

    // Tracking parameter blacklist
    private val TRACKING_PARAMS = setOf(
        "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content",
        "utm_id", "utm_source_platform", "utm_creative_format", "utm_marketing_tactic",
        "fbclid", "gclid", "gclsrc", "dclid", "msclkid", "yclid",
        "mc_eid", "_hsenc", "_hsmi", "igshid", "si", "ref", "ref_src",
        "ref_url", "wickedid", "mkt_tok", "action_object_map", "action_type_map",
        "action_ref_map", "_openstat", "zanpid", "sc_customer"
    )

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _trackerStripperEnabled.value = prefs.getBoolean(KEY_TRACKER_STRIPPER_ENABLED, true)
        _canvasProtectionEnabled.value = prefs.getBoolean(KEY_CANVAS_PROTECTION_ENABLED, true)
        _audioProtectionEnabled.value = prefs.getBoolean(KEY_AUDIO_PROTECTION_ENABLED, true)
        _hardwareSpoofEnabled.value = prefs.getBoolean(KEY_HARDWARE_SPOOF_ENABLED, true)
        _totalTrackersStripped.value = prefs.getInt(KEY_TOTAL_STRIPPED_COUNT, 0)
    }

    fun setTrackerStripperEnabled(enabled: Boolean) {
        _trackerStripperEnabled.value = enabled
        if (::prefs.isInitialized) {
            prefs.edit().putBoolean(KEY_TRACKER_STRIPPER_ENABLED, enabled).apply()
        }
    }

    fun setCanvasProtectionEnabled(enabled: Boolean) {
        _canvasProtectionEnabled.value = enabled
        if (::prefs.isInitialized) {
            prefs.edit().putBoolean(KEY_CANVAS_PROTECTION_ENABLED, enabled).apply()
        }
    }

    fun setAudioProtectionEnabled(enabled: Boolean) {
        _audioProtectionEnabled.value = enabled
        if (::prefs.isInitialized) {
            prefs.edit().putBoolean(KEY_AUDIO_PROTECTION_ENABLED, enabled).apply()
        }
    }

    fun setHardwareSpoofEnabled(enabled: Boolean) {
        _hardwareSpoofEnabled.value = enabled
        if (::prefs.isInitialized) {
            prefs.edit().putBoolean(KEY_HARDWARE_SPOOF_ENABLED, enabled).apply()
        }
    }

    /**
     * Cleans tracking parameters from a URL.
     * Returns a Pair of: (Cleaned URL, Count of stripped parameters)
     */
    fun cleanTrackingParameters(url: String): Pair<String, Int> {
        if (!_trackerStripperEnabled.value) return Pair(url, 0)
        if (!url.startsWith("http://") && !url.startsWith("https://")) return Pair(url, 0)

        return try {
            val uri = Uri.parse(url)
            if (!uri.isHierarchical || uri.queryParameterNames.isEmpty()) {
                return Pair(url, 0)
            }

            val paramNames = uri.queryParameterNames
            var strippedCount = 0
            val preservedParams = mutableListOf<Pair<String, String>>()

            for (param in paramNames) {
                if (TRACKING_PARAMS.contains(param.lowercase())) {
                    strippedCount++
                } else {
                    val value = uri.getQueryParameter(param) ?: ""
                    preservedParams.add(Pair(param, value))
                }
            }

            if (strippedCount == 0) {
                return Pair(url, 0)
            }

            val builder = uri.buildUpon().clearQuery()
            for ((key, value) in preservedParams) {
                builder.appendQueryParameter(key, value)
            }

            val cleanedUrl = builder.build().toString()

            // Update stats
            val newTotal = _totalTrackersStripped.value + strippedCount
            _totalTrackersStripped.value = newTotal
            if (::prefs.isInitialized) {
                prefs.edit().putInt(KEY_TOTAL_STRIPPED_COUNT, newTotal).apply()
            }

            Pair(cleanedUrl, strippedCount)
        } catch (e: Exception) {
            Pair(url, 0)
        }
    }

    /**
     * Injects anti-fingerprinting protection script into the DOM.
     * Spoofs Canvas, Audio, WebGL, and Device Hardware fingerprinting surfaces with micro-noise.
     */
    fun getAntiFingerprintingScript(): String {
        val enableCanvas = _canvasProtectionEnabled.value
        val enableAudio = _audioProtectionEnabled.value
        val enableHardware = _hardwareSpoofEnabled.value

        return """
            (function() {
                if (window.__antifingerprint_injected) return;
                window.__antifingerprint_injected = true;

                // 1. Canvas Anti-Fingerprinting
                ${if (enableCanvas) """
                try {
                    const originalToDataURL = HTMLCanvasElement.prototype.toDataURL;
                    HTMLCanvasElement.prototype.toDataURL = function(type) {
                        try {
                            const ctx = this.getContext('2d');
                            if (ctx && this.width > 0 && this.height > 0) {
                                const w = Math.min(this.width, 16);
                                const h = Math.min(this.height, 16);
                                const imgData = ctx.getImageData(0, 0, w, h);
                                for (let i = 0; i < imgData.data.length; i += 4) {
                                    // Micro noise: +/- 1 on least significant bits
                                    imgData.data[i] = (imgData.data[i] ^ 1);
                                }
                                ctx.putImageData(imgData, 0, 0);
                            }
                        } catch(e) {}
                        return originalToDataURL.apply(this, arguments);
                    };

                    const originalGetImageData = CanvasRenderingContext2D.prototype.getImageData;
                    CanvasRenderingContext2D.prototype.getImageData = function() {
                        const res = originalGetImageData.apply(this, arguments);
                        try {
                            if (res && res.data && res.data.length > 0) {
                                res.data[0] = (res.data[0] ^ 1);
                            }
                        } catch(e) {}
                        return res;
                    };
                } catch(e) {}
                """ else ""}

                // 2. AudioContext Anti-Fingerprinting
                ${if (enableAudio) """
                try {
                    const originalGetChannelData = AudioBuffer.prototype.getChannelData;
                    AudioBuffer.prototype.getChannelData = function(channel) {
                        const data = originalGetChannelData.apply(this, arguments);
                        try {
                            for (let i = 0; i < Math.min(data.length, 32); i += 4) {
                                data[i] += 0.0000001 * (Math.random() - 0.5);
                            }
                        } catch(e) {}
                        return data;
                    };
                } catch(e) {}
                """ else ""}

                // 3. Hardware & Battery Spoofing
                ${if (enableHardware) """
                try {
                    Object.defineProperty(navigator, 'hardwareConcurrency', { get: () => 8 });
                    Object.defineProperty(navigator, 'deviceMemory', { get: () => 8 });
                    if (navigator.getBattery) {
                        navigator.getBattery = function() {
                            return Promise.resolve({
                                charging: true,
                                chargingTime: 0,
                                dischargingTime: Infinity,
                                level: 1.0,
                                addEventListener: function() {},
                                removeEventListener: function() {}
                            });
                        };
                    }
                } catch(e) {}
                """ else ""}
            })();
        """.trimIndent()
    }
}
