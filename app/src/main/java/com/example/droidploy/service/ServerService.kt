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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.io.BufferedReader
import java.io.InputStreamReader

class ServerService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var nodeProcess: Process? = null
    private var tunnelProcess: Process? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundService()
        
        val projectPath = intent?.getStringExtra("PROJECT_PATH")
        val tunnelToken = intent?.getStringExtra("TUNNEL_TOKEN")

        if (projectPath != null) {
            startNodeServer(projectPath)
        }
        
        if (tunnelToken != null) {
            startCloudflareTunnel(tunnelToken)
        }

        return START_STICKY
    }

    private fun startForegroundService() {
        val channelId = "server_service_channel"
        val channelName = "Server Service"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Droidploy Server")
            .setContentText("Server is running...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, notification)
        }
    }

    private fun startNodeServer(projectPath: String) {
        serviceScope.launch {
            try {
                val nativeDir = applicationInfo.nativeLibraryDir
                val nodeExecutable = File(nativeDir, "libnode.so").absolutePath
                
                // Ensure executable permission (just in case, though system handles this)
                // File(nodeExecutable).setExecutable(true) 

                val processBuilder = ProcessBuilder(nodeExecutable, "index.js")
                processBuilder.directory(File(projectPath))
                processBuilder.redirectErrorStream(true)
                
                val process = processBuilder.start()
                nodeProcess = process
                
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    // Log or broadcast output
                    println("NODE: $line")
                }
                
                process.waitFor()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun startCloudflareTunnel(token: String) {
        serviceScope.launch {
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
                
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    println("CLOUDFLARED: $line")
                }
                
                process.waitFor()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        nodeProcess?.destroy()
        tunnelProcess?.destroy()
    }
}
