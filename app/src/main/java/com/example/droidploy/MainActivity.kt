package com.example.droidploy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.droidploy.ui.NavigationGraph
import com.example.droidploy.ui.theme.DroidployTheme
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Copy demo server from assets on first launch
        copyDemoServerIfNeeded()

        setContent {
            DroidployTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }

    private fun copyDemoServerIfNeeded() {
        val serverDir = File(filesDir, "my_server")
        val indexFile = File(serverDir, "index.js")

        // Always log the attempt
        android.util.Log.d("MainActivity", "Checking demo server at: ${serverDir.absolutePath}")
        android.util.Log.d("MainActivity", "Index.js exists: ${indexFile.exists()}")

        // Only copy if the demo server doesn't exist yet
        if (!indexFile.exists()) {
            try {
                serverDir.mkdirs()
                android.util.Log.d("MainActivity", "Created server directory: ${serverDir.absolutePath}")

                // Copy index.js from assets
                assets.open("demo_server/index.js").use { input ->
                    FileOutputStream(indexFile).use { output ->
                        input.copyTo(output)
                    }
                }

                // Copy README.md from assets
                val readmeFile = File(serverDir, "README.md")
                assets.open("demo_server/README.md").use { input ->
                    FileOutputStream(readmeFile).use { output ->
                        input.copyTo(output)
                    }
                }

                android.util.Log.d("MainActivity", "✓ Demo server copied successfully")
                android.util.Log.d("MainActivity", "Index.js size: ${indexFile.length()} bytes")
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Failed to copy demo server", e)
                e.printStackTrace()
            }
        } else {
            android.util.Log.d("MainActivity", "Demo server already exists, skipping copy")
        }
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()

    Scaffold { paddingValues ->
        NavigationGraph(
            navController = navController,
            paddingValues = paddingValues
        )
    }
}
