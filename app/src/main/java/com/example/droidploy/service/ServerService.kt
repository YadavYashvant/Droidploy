package com.example.droidploy.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.droidploy.R
import com.example.droidploy.network.CloudflareManager
import com.example.droidploy.network.ConfigureTunnelRequest
import com.example.droidploy.network.CreateDnsRecordRequest
import com.example.droidploy.network.CreateTunnelRequest
import com.example.droidploy.network.IngressRule
import com.example.droidploy.network.TunnelConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.UUID

class ServerService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var nodeProcess: Process? = null
    private var tunnelProcess: Process? = null
    private var notificationManager: NotificationManager? = null
    private val CHANNEL_ID = "server_service_channel"
    private val NOTIFICATION_ID = 1

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        notificationManager = getSystemService(NotificationManager::class.java)
        startForegroundService()
        
        val projectPath = intent?.getStringExtra("PROJECT_PATH")
        val command = intent?.getStringExtra("COMMAND") ?: "node index.js"
        val apiToken = intent?.getStringExtra("API_TOKEN")
        val accountId = intent?.getStringExtra("ACCOUNT_ID")
        val zoneId = intent?.getStringExtra("ZONE_ID")
        val domain = intent?.getStringExtra("DOMAIN")

        if (projectPath != null && apiToken != null && accountId != null &&
            zoneId != null && domain != null) {

            serviceScope.launch {
                try {
                    updateNotification("Setting up Cloudflare Tunnel...")

                    // Step 1: Create tunnel via API
                    val tunnelName = "droidploy-${UUID.randomUUID().toString().take(8)}"
                    val createResponse = CloudflareManager.api.createTunnel(
                        token = "Bearer $apiToken",
                        accountId = accountId,
                        body = CreateTunnelRequest(tunnelName)
                    )

                    val tunnelId = createResponse.result.id
                    val tunnelToken = createResponse.result.token

                    updateNotification("Configuring tunnel routes...")

                    // Step 2: Configure tunnel ingress
                    val subdomain = "app-${UUID.randomUUID().toString().take(6)}"
                    val hostname = "$subdomain.$domain"

                    CloudflareManager.api.configureTunnel(
                        token = "Bearer $apiToken",
                        accountId = accountId,
                        tunnelId = tunnelId,
                        body = ConfigureTunnelRequest(
                            config = TunnelConfig(
                                ingress = listOf(
                                    IngressRule(hostname = hostname, service = "http://localhost:8080"),
                                    IngressRule(service = "http_status:404")
                                )
                            )
                        )
                    )

                    updateNotification("Creating DNS record...")

                    // Step 3: Create DNS record
                    CloudflareManager.api.createDnsRecord(
                        token = "Bearer $apiToken",
                        zoneId = zoneId,
                        body = CreateDnsRecordRequest(
                            name = subdomain,
                            content = "$tunnelId.cfargotunnel.com"
                        )
                    )

                    updateNotification("Starting Node.js server...")

                    // Step 4: Start Node.js server
                    startNodeServer(projectPath, command)

                    delay(2000) // Wait for Node to start

                    updateNotification("Starting Cloudflare tunnel...")

                    // Step 5: Start Cloudflare tunnel
                    startCloudflareTunnel(tunnelToken)

                    updateNotification("✓ Server live at https://$hostname")

                } catch (e: Exception) {
                    e.printStackTrace()
                    updateNotification("✗ Deployment failed: ${e.message}")
                }
            }
        } else if (projectPath != null) {
            // Fallback: just start Node without tunnel
            serviceScope.launch {
                startNodeServer(projectPath, command)
                updateNotification("Server running locally (no tunnel)")
            }
        }

        return START_STICKY
    }

    private fun startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Server Service",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager?.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Droidploy Server")
            .setContentText("Initializing...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(message: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Droidploy Server")
            .setContentText(message)
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()

        notificationManager?.notify(NOTIFICATION_ID, notification)
    }

    private fun startNodeServer(projectPath: String, command: String) {
        try {
            val nativeDir = applicationInfo.nativeLibraryDir
            val nodeExecutable = File(nativeDir, "libnode.so").absolutePath

            // Parse command to extract arguments
            val parts = command.trim().split("\\s+".toRegex())
            val args = if (parts[0].equals("node", ignoreCase = true)) {
                // Replace "node" with actual path
                listOf(nodeExecutable) + parts.drop(1)
            } else {
                // Direct execution
                listOf(nodeExecutable) + parts
            }

            val processBuilder = ProcessBuilder(args)
            processBuilder.directory(File(projectPath))
            processBuilder.redirectErrorStream(true)

            // Set environment variables
            val env = processBuilder.environment()
            env["HOME"] = filesDir.absolutePath
            env["TMPDIR"] = cacheDir.absolutePath
            env["PORT"] = "8080"

            val process = processBuilder.start()
            nodeProcess = process

            // Log output
            serviceScope.launch {
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    println("NODE: $line")
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
            updateNotification("✗ Node.js failed: ${e.message}")
        }
    }

    private fun startCloudflareTunnel(token: String) {
        try {
            val nativeDir = applicationInfo.nativeLibraryDir
            val cloudflaredExecutable = File(nativeDir, "libcloudflared.so").absolutePath

            val processBuilder = ProcessBuilder(
                cloudflaredExecutable,
                "tunnel",
                "run",
                "--token",
                token
            )
            processBuilder.redirectErrorStream(true)

            val process = processBuilder.start()
            tunnelProcess = process

            // Log output
            serviceScope.launch {
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    println("CLOUDFLARED: $line")
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
            updateNotification("✗ Tunnel failed: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        nodeProcess?.destroy()
        tunnelProcess?.destroy()
        updateNotification("Server stopped")
    }
}
