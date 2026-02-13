package com.example.sariaudit.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = DeepOrange,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    secondary = TealGreen,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    background = OffWhite,
    surface = androidx.compose.ui.graphics.Color.White,
    error = ErrorRed
)

@Composable
fun SariAuditTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}