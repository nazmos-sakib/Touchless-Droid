package com.example.touchlessdroid.domain.usecase

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.SocketException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.Inet4Address
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference

class RobotServer(val context: Context, val port: Int) {

    private var serverSocket: ServerSocket? = null
    private val executor = Executors.newSingleThreadExecutor()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()
    @Volatile
    private var running = false

    private val status = AtomicReference("EMPTY")

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.GERMANY)


    /**
     * Starts the TCP server.
     *
     * @param ip IP address to bind to (e.g. "0.0.0.0" or "192.168.1.100")
     * @param port TCP port
     */
    fun startServer() {
        if (running) return

        running = true
        val ip = getWifiIpAddress(this.context)
        if (ip == null) {
            running = false
            return
        }

        Log.i("Server", "Binding to: $ip:$port")

        executor.execute {
            try {
                serverSocket = ServerSocket().apply {
                    reuseAddress = true
                    bind(InetSocketAddress(ip, port))
                }
                _isRunning.value = true
                while (running) {
                    try {
                        val client = serverSocket?.accept() ?: break
                        client.use {
                            val response =
                                "${status.get()} ${timeFormat.format(Date())}"

                            BufferedWriter(OutputStreamWriter(it.getOutputStream())).use { writer ->
                                writer.write(response)
                                writer.newLine()
                                writer.flush()
                            }
                        }
                    } catch (_: SocketException) {
                        // Happens when stopServer() closes the socket.
                        if (!running) break
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                running = false
                _isRunning.value = false
                try {
                    serverSocket?.close()
                } catch (_: Exception) {
                }
                serverSocket = null
            }
        }
    }

    /**
     * Stops the server.
     */
    fun stopServer() {
        running = false
        _isRunning.value = false
        try {
            serverSocket?.close()
        } catch (_: Exception) {
        }
    }

    /**
     * Can safely be called from the UI thread.
     */
    fun setStatus(newStatus: String) {
        status.set(newStatus)
    }

    /**
     * Optional getter.
     */
    fun getStatus(): String = status.get()

    @SuppressLint("DefaultLocale")
    private fun getWifiIpAddress(context: Context): String? {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        val ip = wifiManager.connectionInfo.ipAddress
        if (ip == 0) return null

        return String.format(
            "%d.%d.%d.%d",
            ip and 0xff,
            ip shr 8 and 0xff,
            ip shr 16 and 0xff,
            ip shr 24 and 0xff
        )
    }
    private fun getWifiIpAddressV2(context: Context): String? {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val wifiNetwork = connectivityManager.allNetworks.firstOrNull { network ->
            connectivityManager
                .getNetworkCapabilities(network)
                ?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        } ?: return null

        val linkProperties =
            connectivityManager.getLinkProperties(wifiNetwork) ?: return null

        return linkProperties.linkAddresses
            .map { it.address }
            .firstOrNull { address ->
                address is Inet4Address && !address.isLoopbackAddress
            }
            ?.hostAddress
    }
}