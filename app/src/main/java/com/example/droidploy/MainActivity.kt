package com.example.droidploy

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.droidploy.service.ServerService
import com.example.droidploy.utils.FileManager
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var projectPath by remember { mutableStateOf<String?>(null) }
    var tunnelToken by remember { mutableStateOf("") }
    var logs by remember { mutableStateOf("Ready to deploy.\n") }
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                logs += "Importing project...\n"
                val path = FileManager.copyProjectFromUri(context, it, "my_server")
                if (path != null) {
                    projectPath = path
                    logs += "Project imported to: $path\n"
                } else {
                    logs += "Failed to import project.\n"
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Droidploy", style = MaterialTheme.typography.headlineMedium)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(onClick = { launcher.launch(arrayOf("application/zip")) }) {
            Text("Select Project (Zip)")
        }
        
        if (projectPath != null) {
            Text("Project loaded", color = MaterialTheme.colorScheme.primary)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = tunnelToken,
            onValueChange = { tunnelToken = it },
            label = { Text("Cloudflare Tunnel Token") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = {
                if (projectPath != null && tunnelToken.isNotEmpty()) {
                    val intent = Intent(context, ServerService::class.java).apply {
                        putExtra("PROJECT_PATH", projectPath)
                        putExtra("TUNNEL_TOKEN", tunnelToken)
                    }
                    context.startForegroundService(intent)
                    logs += "Starting server...\n"
                } else {
                    logs += "Please select a project and enter a token.\n"
                }
            },
            enabled = projectPath != null && tunnelToken.isNotEmpty()
        ) {
            Text("Deploy Server")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text("Logs:", style = MaterialTheme.typography.titleMedium)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Text(logs)
        }
    }
}

@Composable
fun DroidployTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}