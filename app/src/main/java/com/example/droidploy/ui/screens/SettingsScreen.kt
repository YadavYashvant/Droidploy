package com.example.droidploy.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.droidploy.ui.components.CustomButton
import com.example.droidploy.ui.components.CustomField
import com.example.droidploy.ui.components.CustomText

@Composable
fun SettingsScreen(
    paddingValues: PaddingValues,
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefsManager = remember { PreferencesManager(context) }

    var apiToken by remember { mutableStateOf(prefsManager.getApiToken()) }
    var accountId by remember { mutableStateOf(prefsManager.getAccountId()) }
    var zoneId by remember { mutableStateOf(prefsManager.getZoneId()) }
    var domain by remember { mutableStateOf(prefsManager.getDomain()) }
    var showSaved by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            IconButton(onClick = onBackClick) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Settings",
                style = TextStyle(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Cloudflare Configuration Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                CustomText("Cloudflare Configuration", size = 18)
                Spacer(modifier = Modifier.height(12.dp))

                CustomField(
                    label = "API Token",
                    value = apiToken,
                    onValueChange = { apiToken = it },
                    placeholder = "Enter Cloudflare API Token"
                )

                Spacer(modifier = Modifier.height(12.dp))

                CustomField(
                    label = "Account ID",
                    value = accountId,
                    onValueChange = { accountId = it },
                    placeholder = "Enter Account ID"
                )

                Spacer(modifier = Modifier.height(12.dp))

                CustomField(
                    label = "Zone ID",
                    value = zoneId,
                    onValueChange = { zoneId = it },
                    placeholder = "Enter Zone ID"
                )

                Spacer(modifier = Modifier.height(12.dp))

                CustomField(
                    label = "Domain",
                    value = domain,
                    onValueChange = { domain = it },
                    placeholder = "example.com"
                )

                Spacer(modifier = Modifier.height(16.dp))

                CustomButton(
                    text = "SAVE CONFIGURATION",
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    onClick = {
                        prefsManager.saveCloudflareCredentials(apiToken, accountId, zoneId)
                        prefsManager.saveDomain(domain)
                        showSaved = true
                    }
                )

                if (showSaved) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "✓ Configuration saved successfully",
                        color = Color.Green,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Help Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                CustomText("How to get credentials", size = 16)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "1. Go to Cloudflare Dashboard\n" +
                    "2. Get API Token from My Profile > API Tokens\n" +
                    "3. Get Account ID from any domain overview\n" +
                    "4. Get Zone ID from domain overview page",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}