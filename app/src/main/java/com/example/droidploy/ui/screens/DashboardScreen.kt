package com.example.droidploy.ui.screens

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.droidploy.data.PreferencesManager
import com.example.droidploy.service.ServerService
import com.example.droidploy.ui.components.CustomButton
import com.example.droidploy.ui.components.CustomField
import com.example.droidploy.ui.components.CustomText
import com.example.droidploy.utils.FileManager
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(paddingValues: PaddingValues, onSettingsClick: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefsManager = remember { PreferencesManager(context) }

    var projectPath by remember { mutableStateOf<String?>(null) }
    var commandText by remember { mutableStateOf(prefsManager.getLastCommand()) }
    var statusMessage by remember { mutableStateOf("") }
    var isDeploying by remember { mutableStateOf(false) }
    var serverUrl by remember { mutableStateOf<String?>(null) }

    // Set up broadcast receiver for server status updates
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val message = intent?.getStringExtra(ServerService.EXTRA_STATUS_MESSAGE)
                val url = intent?.getStringExtra(ServerService.EXTRA_SERVER_URL)
                if (message != null) {
                    statusMessage = message
                }
                if (url != null) {
                    serverUrl = url
                }
            }
        }

        val filter = IntentFilter(ServerService.ACTION_SERVER_STATUS)
        LocalBroadcastManager.getInstance(context).registerReceiver(receiver, filter)

        onDispose {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver)
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                statusMessage = "Importing project..."
                val path = FileManager.copyProjectFromUri(context, it)
                if (path != null) {
                    projectPath = path
                    statusMessage = "✓ Project imported successfully"
                } else {
                    statusMessage = "✗ Failed to import project"
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .background(Color.Black)
            .padding(horizontal = 12.dp)
            .padding(top = 16.dp)
        ,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Row(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 8.dp)
                    .fillMaxWidth()
                    .wrapContentHeight(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Droidploy \uD83D\uDDA5\uFE0F", style = TextStyle(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp
                    )
                )

                IconButton(onClick = onSettingsClick) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Project Source Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(12.dp)
                ) {
                    CustomText("Project Source")
                    Spacer(modifier = Modifier.height(12.dp))
                    CustomButton(
                        text = "IMPORT PROJECT ZIP",
                        modifier = Modifier
                            .wrapContentHeight()
                            .fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.DarkGray,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        onClick = {
                            launcher.launch(arrayOf("application/zip", "application/octet-stream"))
                        }
                    )

                    if (projectPath != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "✓ Project loaded",
                            color = Color.Green,
                            fontSize = 12.sp
                        )
                    }

                    if (statusMessage.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            statusMessage,
                            fontSize = 12.sp,
                            color = if (statusMessage.contains("✓")) Color.Green else Color.Red
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Runtime Config Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(8.dp)
                ) {
                    CustomText("Runtime Config")
                    Spacer(modifier = Modifier.height(8.dp))
                    CustomField(
                        label = "Custom Command",
                        value = commandText,
                        onValueChange = { commandText = it },
                        placeholder = "node index.js"
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Status Card - Show server URL when available
            if (isDeploying) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Text(
                            "🚀 Server is running",
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.Green
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        if (serverUrl != null) {
                            // Clickable URL Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = serverUrl!!,
                                        style = TextStyle(
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            textDecoration = TextDecoration.Underline
                                        ),
                                        modifier = Modifier.clickable {
                                            // Open URL in browser
                                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(serverUrl))
                                            context.startActivity(browserIntent)
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Tap URL to open in browser",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }

                                // Copy Button
                                Button(
                                    onClick = {
                                        // Copy URL to clipboard
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Server URL", serverUrl)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "✓ URL copied!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ),
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    Text("📋 Copy", fontSize = 12.sp)
                                }
                            }
                        } else {
                            Text(
                                "Check notifications for status",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }

        // Deploy Button
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            CustomButton(
                text = if (isDeploying) "STOP SERVER" else "DEPLOY SERVER",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(45.dp),
                enabled = if (isDeploying) true else (projectPath != null && prefsManager.isConfigured()),
                colors = if (isDeploying) {
                    ButtonDefaults.buttonColors(
                        containerColor = Color.Red,
                        contentColor = Color.White
                    )
                } else {
                    ButtonDefaults.buttonColors(
                        containerColor = Color.Yellow,
                        contentColor = Color.Black,
                        disabledContainerColor = Color.LightGray,
                        disabledContentColor = Color.DarkGray
                    )
                },
                onClick = {
                    if (isDeploying) {
                        val intent = Intent(context, ServerService::class.java)
                        context.stopService(intent)
                        isDeploying = false
                        serverUrl = null
                        statusMessage = "Server stopped"
                    } else {
                        if (projectPath != null && prefsManager.isConfigured()) {
                            prefsManager.saveLastCommand(commandText)

                            val intent = Intent(context, ServerService::class.java).apply {
                                putExtra("PROJECT_PATH", projectPath)
                                putExtra("COMMAND", commandText)
                                putExtra("API_TOKEN", prefsManager.getApiToken())
                                putExtra("ACCOUNT_ID", prefsManager.getAccountId())
                                putExtra("ZONE_ID", prefsManager.getZoneId())
                                putExtra("DOMAIN", prefsManager.getDomain())
                            }

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                context.startForegroundService(intent)
                            } else {
                                context.startService(intent)
                            }
                            isDeploying = true
                            statusMessage = "Deploying server..."
                        }
                    }
                }
            )

            if (!prefsManager.isConfigured() && projectPath != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "⚠ Please configure Cloudflare settings first",
                    fontSize = 12.sp,
                    color = Color(0xFFFFAA00),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}