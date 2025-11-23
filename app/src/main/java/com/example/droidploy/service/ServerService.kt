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
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.localbroadcastmanager.content.LocalBroadcastManager.*
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
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.UUID

class ServerService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var nodeProcess: Process? = null
    private var tunnelProcess: Process? = null
    private var notificationManager: NotificationManager? = null
    private val CHANNEL_ID = "server_service_channel"
    private val NOTIFICATION_ID = 1

    companion object {
        const val ACTION_SERVER_STATUS = "com.example.droidploy.SERVER_STATUS"
        const val EXTRA_STATUS_MESSAGE = "status_message"
        const val EXTRA_SERVER_URL = "server_url"
    }

    private var currentServerUrl: String? = null

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

        // Only create demo server if NO project path is provided (first launch scenario)
        if (projectPath == null) {
            ensureDemoServerExists()
        }

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

                    val serverUrl = "https://$hostname"
                    currentServerUrl = serverUrl
                    updateNotification("✓ Server live at $serverUrl")

                    // Send broadcast with server URL
                    sendServerStatusBroadcast("✓ Server live at $serverUrl", serverUrl)

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

            // Use hardcoded Cloudflare edge IP addresses to bypass DNS
            val processBuilder = ProcessBuilder(
                cloudflaredExecutable,
                "tunnel",
                "--no-autoupdate",
                "--protocol", "quic",
                "--edge-ip-version", "4",
                "--edge", "198.41.192.167:7844",  // Hardcoded Cloudflare edge IP
                "run",
                "--token",
                token
            )
            processBuilder.redirectErrorStream(true)

            // Set working directory
            processBuilder.directory(cacheDir)

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

    private fun ensureDemoServerExists() {
        // Only create demo server if no user files exist
        try {
            val projectDir = File(filesDir, "my_server")

            // Check if directory exists and has an index.js file
            val indexJsFile = File(projectDir, "index.js")

            if (indexJsFile.exists()) {
                // Check if it's the demo server by looking for a specific marker
                val content = indexJsFile.readText()
                if (!content.contains("Droidploy Demo Server")) {
                    // It's user's custom file, don't overwrite
                    android.util.Log.d("ServerService", "User's custom index.js exists, skipping demo server creation")
                    return
                }
            }

            // Create demo server only if no custom index.js exists
            if (!projectDir.exists()) {
                projectDir.mkdirs()
            }

            if (!indexJsFile.exists()) {
                val indexJsContent = """
                    const http = require('http');
                    const port = process.env.PORT || 8080;

                    const requestHandler = (request, response) => {
                        response.writeHead(200, { 'Content-Type': 'text/html' });
                        response.end(`
                            <!DOCTYPE html>
                            <html>
                            <head>
                                <title>Droidploy Demo</title>
                                <style>
                                    body { font-family: system-ui; max-width: 800px; margin: 50px auto; padding: 20px;
                                           background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; }
                                    .container { background: rgba(255,255,255,0.1); padding: 40px; border-radius: 20px; }
                                    h1 { font-size: 2.5em; margin: 0; }
                                    .status { background: #10b981; padding: 10px 20px; border-radius: 25px;
                                             display: inline-block; margin: 20px 0; font-weight: bold; }
                                </style>
                            </head>
                            <body>
                                <div class="container">
                                    <h1>🚀 Droidploy Demo Server</h1>
                                    <div class="status">✓ Server Running</div>
                                    <p><strong>Congratulations!</strong> Your Node.js backend is running on Android!</p>
                                    <p>To deploy your own server:</p>
                                    <ul>
                                        <li>1. Create a zip file with your Node.js project</li>
                                        <li>2. Click "Select Project" in the app</li>
                                        <li>3. Choose your zip file</li>
                                        <li>4. Click "Deploy Server"</li>
                                    </ul>
                                    <p><strong>Server Info:</strong></p>
                                    <ul>
                                        <li>Node.js version: ${'$'}{process.version}</li>
                                        <li>Platform: ${'$'}{process.platform} (${'$'}{process.arch})</li>
                                        <li>Port: ${'$'}{port}</li>
                                    </ul>
                                </div>
                            </body>
                            </html>
                        `);
                    }

                    const server = http.createServer(requestHandler);

                    server.listen(port, (err) => {
                        if (err) {
                            return console.log('Something bad happened', err);
                        }
                        console.log(`Server is listening on port ${'$'}{port}`);
                    });
                """.trimIndent()

                indexJsFile.writeText(indexJsContent)
                android.util.Log.d("ServerService", "Demo server created at ${indexJsFile.absolutePath}")
            } else {
                android.util.Log.d("ServerService", "Demo server already exists, skipping creation")
            }

        } catch (e: Exception) {
            android.util.Log.e("ServerService", "Failed to create demo server", e)
            e.printStackTrace()
        }
    }

    private fun sendServerStatusBroadcast(message: String, serverUrl: String) {
        val intent = Intent(ACTION_SERVER_STATUS)
        intent.putExtra(EXTRA_STATUS_MESSAGE, message)
        intent.putExtra(EXTRA_SERVER_URL, serverUrl)
        sendBroadcast(intent)
        getInstance(this).sendBroadcast(intent)
        android.util.Log.d("ServerService", "Broadcast sent: $message, URL: $serverUrl")
    }

    override fun onDestroy() {
        super.onDestroy()
        nodeProcess?.destroy()
        tunnelProcess?.destroy()
        updateNotification("Server stopped")
    }
}
