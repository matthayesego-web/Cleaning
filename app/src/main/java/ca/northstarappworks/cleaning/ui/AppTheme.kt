package ca.northstarappworks.cleaning.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val HomeColours = lightColorScheme(
    primary = Color(0xFF356859),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD5E8DF),
    secondary = Color(0xFFE49B65),
    background = Color(0xFFF7F7F2),
    surface = Color.White,
    error = Color(0xFFB3261E)
)

@Composable
fun OurHomeTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = HomeColours, content = content)
}
