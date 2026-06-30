package com.example.data

import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import org.json.JSONObject

object DnsManager {
    private const val TAG = "DnsManager"
    
    // Cache: Hostname -> Blocked? (True means it resolved to a blocked IP like 0.0.0.0 or 127.0.0.1)
    private val blockCache = ConcurrentHashMap<String, Boolean>()
    
    // Preset DoH and UDP configurations
    val presets = listOf(
        DnsPreset("adguard", "AdGuard Default (Adblock & Security)", "https://dns.adguard-dns.com/resolve", "94.140.14.14"),
        DnsPreset("mullvad", "Mullvad Adblock & Privacy", "https://dns.mullvad.net/dns-query", "194.242.2.2"),
        DnsPreset("cloudflare", "Cloudflare Standard (Fast)", "https://cloudflare-dns.com/dns-query", "1.1.1.1"),
        DnsPreset("google", "Google Public DNS", "https://dns.google/resolve", "8.8.8.8"),
        DnsPreset("adguard_family", "AdGuard Family (Safe Search)", "https://dns-family.adguard-dns.com/dns-query", "94.140.14.15")
    )

    data class DnsPreset(
        val id: String,
        val name: String,
        val dohUrl: String,
        val fallbackIp: String
    )

    fun clearCache() {
        blockCache.clear()
    }

    fun shouldBlockDomain(
        host: String,
        enabled: Boolean,
        mode: String,
        presetId: String,
        customValue: String
    ): Boolean {
        if (!enabled || host.isBlank()) return false
        
        val cleanHost = host.trim().lowercase()
        
        // Skip common essential system/navigation/localhost domains to prevent breaking pages
        if (cleanHost == "localhost" || 
            cleanHost == "127.0.0.1" || 
            cleanHost == "dineinstyle.com" || 
            cleanHost == "google.com" || 
            cleanHost.endsWith(".google.com") ||
            cleanHost == "google.co.in" ||
            cleanHost.endsWith(".google.co.in") ||
            cleanHost.contains("google-analytics") // but block analytics if necessary, usually standard trackers
        ) {
            return false
        }

        // Return cached answer immediately
        blockCache[cleanHost]?.let { return it }

        // Resolve synchronously (since shouldInterceptRequest runs on a background thread)
        var isBlocked = false
        try {
            val resolvedIps = if (mode == "custom" && customValue.isNotBlank()) {
                val value = customValue.trim()
                if (value.startsWith("http://") || value.startsWith("https://")) {
                    queryDoh(value, cleanHost)
                } else if (value.matches(Regex("""^[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}$"""))) {
                    queryUdp(value, cleanHost)
                } else {
                    // Try DoH if looks like a domain, otherwise default to UDP
                    queryDoh("https://$value/dns-query", cleanHost)
                }
            } else {
                val preset = presets.find { it.id == presetId } ?: presets.first()
                // Try DoH first, fallback to UDP IP
                val ips = queryDoh(preset.dohUrl, cleanHost)
                if (ips.isEmpty()) {
                    queryUdp(preset.fallbackIp, cleanHost)
                } else {
                    ips
                }
            }

            // If the DNS resolves to standard block IPs, flag it
            if (resolvedIps.isNotEmpty()) {
                isBlocked = resolvedIps.any { ip ->
                    ip == "0.0.0.0" || ip == "127.0.0.1" || ip == "::" || ip == "::1"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "DNS resolution failed for $cleanHost: ${e.message}")
        }

        blockCache[cleanHost] = isBlocked
        return isBlocked
    }

    private fun queryDoh(dohUrl: String, host: String): List<String> {
        val ips = mutableListOf<String>()
        try {
            // Normalize DoH url to Google/Cloudflare/AdGuard resolve protocol compatibility
            val separator = if (dohUrl.contains("?")) "&" else "?"
            val finalUrl = if (dohUrl.contains("name=")) {
                dohUrl
            } else {
                "$dohUrl${separator}name=$host&type=A"
            }
            
            val url = URL(finalUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 1200
            conn.readTimeout = 1200
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("accept", "application/dns-json")
            
            if (conn.responseCode == 200) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                if (json.has("Answer")) {
                    val answerArray = json.getJSONArray("Answer")
                    for (i in 0 until answerArray.length()) {
                        val obj = answerArray.getJSONObject(i)
                        // A records have type 1
                        val type = if (obj.has("type")) obj.getInt("type") else 1
                        if (type == 1 && obj.has("data")) {
                            ips.add(obj.getString("data").trim())
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "DoH error for $host over $dohUrl: ${e.message}")
        }
        return ips
    }

    private fun queryUdp(dnsServerIp: String, host: String): List<String> {
        val ips = mutableListOf<String>()
        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket()
            socket.soTimeout = 1200
            
            val baos = ByteArrayOutputStream()
            val dos = DataOutputStream(baos)
            
            // Transaction ID
            dos.writeShort(0x8899)
            // Flags: Standard query, recursion desired (0x0100)
            dos.writeShort(0x0100)
            // Questions count: 1
            dos.writeShort(1)
            // Answer RRs count: 0
            dos.writeShort(0)
            // Authority RRs count: 0
            dos.writeShort(0)
            // Additional RRs count: 0
            dos.writeShort(0)
            
            // Query Section: Name
            val parts = host.split(".")
            for (part in parts) {
                val bytes = part.toByteArray(Charsets.US_ASCII)
                dos.writeByte(bytes.size)
                dos.write(bytes)
            }
            dos.writeByte(0) // Null byte terminates label list
            
            // Type: A (1)
            dos.writeShort(1)
            // Class: IN (1)
            dos.writeShort(1)
            
            val requestBytes = baos.toByteArray()
            val address = InetAddress.getByName(dnsServerIp)
            val requestPacket = DatagramPacket(requestBytes, requestBytes.size, address, 53)
            socket.send(requestPacket)
            
            val responseBuffer = ByteArray(512)
            val responsePacket = DatagramPacket(responseBuffer, responseBuffer.size)
            socket.receive(responsePacket)
            
            val responseBytes = responsePacket.data
            // Answers section parsing
            // Skip header (12 bytes)
            var idx = 12
            // Skip original query name labels
            while (responseBytes[idx] != 0.toByte()) {
                idx += (responseBytes[idx].toInt() and 0xFF) + 1
            }
            idx += 5 // Terminating 0 byte + Type (2 bytes) + Class (2 bytes)
            
            val answersCount = ((responseBytes[6].toInt() and 0xFF) shl 8) or (responseBytes[7].toInt() and 0xFF)
            for (i in 0 until answersCount) {
                if (idx + 12 > responseBytes.size) break
                // If label is pointer (starts with 0xC0)
                if ((responseBytes[idx].toInt() and 0xC0) == 0xC0) {
                    idx += 2
                } else {
                    while (responseBytes[idx] != 0.toByte()) {
                        idx += (responseBytes[idx].toInt() and 0xFF) + 1
                    }
                    idx += 1
                }
                if (idx + 10 > responseBytes.size) break
                val type = ((responseBytes[idx].toInt() and 0xFF) shl 8) or (responseBytes[idx+1].toInt() and 0xFF)
                val dataLen = ((responseBytes[idx+8].toInt() and 0xFF) shl 8) or (responseBytes[idx+9].toInt() and 0xFF)
                idx += 10
                if (idx + dataLen > responseBytes.size) break
                if (type == 1 && dataLen == 4) { // A record
                    val ip = "${responseBytes[idx].toInt() and 0xFF}.${responseBytes[idx+1].toInt() and 0xFF}.${responseBytes[idx+2].toInt() and 0xFF}.${responseBytes[idx+3].toInt() and 0xFF}"
                    ips.add(ip)
                }
                idx += dataLen
            }
        } catch (e: Exception) {
            Log.d(TAG, "UDP query error for $host over $dnsServerIp: ${e.message}")
        } finally {
            socket?.close()
        }
        return ips
    }
}
