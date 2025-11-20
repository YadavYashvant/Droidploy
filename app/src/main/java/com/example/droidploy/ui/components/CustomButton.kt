package com.example.droidploy.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Composable
fun CustomButton(
    modifier: Modifier = Modifier,
    colors: ButtonColors = ButtonDefaults.buttonColors(
        containerColor = Color.Yellow,
        contentColor = Color.Black,
        disabledContentColor = Color.DarkGray,
        disabledContainerColor = Color.LightGray
    ),
    shape: Shape = RoundedCornerShape(20.dp),
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit = {}
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = colors,
        shape = shape,
        enabled = enabled
    ) {
        Text(text)
    }
}
