package com.example.droidploy.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp

@Composable
fun CustomText(
    text: String,
    weight: FontWeight = FontWeight.Bold,
    size: Int = 24
){
    Text(text, style = TextStyle(
        fontWeight = weight,
        fontSize = size.sp
    ))
}
