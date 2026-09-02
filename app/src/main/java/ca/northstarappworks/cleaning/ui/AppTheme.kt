package ca.northstarappworks.cleaning.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Forest = Color(0xFF245C50)
val ForestDeep = Color(0xFF173F38)
val Mint = Color(0xFFDCEDE6)
val Peach = Color(0xFFF4B184)
val Cream = Color(0xFFF8F6F0)
val Ink = Color(0xFF20312D)
val MutedInk = Color(0xFF687873)

private val HomeColours = lightColorScheme(
    primary = Forest,
    onPrimary = Color.White,
    primaryContainer = Mint,
    onPrimaryContainer = ForestDeep,
    secondary = Peach,
    onSecondary = Ink,
    background = Cream,
    surface = Color.White,
    onSurface = Ink,
    onSurfaceVariant = MutedInk,
    error = Color(0xFFB3261E)
)

@Composable
fun OurHomeTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = HomeColours, content = content)
}
