package com.example.droidploy.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.materialIcon
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.droidploy.ui.components.CustomButton
import com.example.droidploy.ui.components.CustomField
import com.example.droidploy.ui.components.CustomText

@Composable
fun DashboardScreen(paddingValues: PaddingValues, onSettingsClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.SpaceBetween
//        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column {

            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp).fillMaxWidth().wrapContentHeight(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Deploy Server", style = TextStyle(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp
                    )
                )

                IconButton(
                    onClick = onSettingsClick,
                ) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = "")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

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
                        text = " IMPORT PROJECT (ZIP/FOLDER) ",
                        modifier = Modifier
                            .wrapContentHeight()
                            .fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.DarkGray,
                            contentColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

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

                    var commandText by remember { mutableStateOf("") }

                    CustomText("Runtime Config")
                    Spacer(modifier = Modifier.height(8.dp))
                    CustomField(
                        label = "Custom Command",
                        value = commandText,
                        onValueChange = { commandText = it },
                        placeholder = "node index.js",

                        )
                }
            }
        }

        CustomButton(text = "DEPLOY SERVER", modifier = Modifier.fillMaxWidth().height(35.dp))

    }

}