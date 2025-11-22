package com.example.droidploy.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                statusMessage = "Importing project..."
                val path = FileManager.copyProjectFromUri(context, it, "my_server")
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
            .padding(horizontal = 12.dp),
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
                    "Deploy Server", style = TextStyle(
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
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(8.dp)
                ) {
                    CustomText("Project Source")
                    Spacer(modifier = Modifier.height(8.dp))
                    CustomButton(
                        text = "IMPORT PROJECT (ZIP/FOLDER)",
                        modifier = Modifier
                            .wrapContentHeight()
                            .fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.DarkGray,
                            contentColor = Color.White
                        ),
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
                        Spacer(modifier = Modifier.height(8.dp))
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

            // Status Card
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
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Check notifications for status",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
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