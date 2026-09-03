package ca.northstarappworks.cleaning.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Forest = Color(0xFF1F5B4D)
val ForestDeep = Color(0xFF123C33)
val ForestSoft = Color(0xFF347364)
val Mint = Color(0xFFE1EEE8)
val MintSoft = Color(0xFFF0F6F3)
val Peach = Color(0xFFEFAE82)
val Champagne = Color(0xFFF1D7A7)
val Lilac = Color(0xFFEAE2F2)
val Cream = Color(0xFFF8F7F3)
val Paper = Color(0xFFFFFEFC)
val Ink = Color(0xFF1D2B27)
val MutedInk = Color(0xFF6D7D77)
val Hairline = Color(0xFFE7EAE7)

private val HomeColours = lightColorScheme(
    primary = Forest,
    onPrimary = Color.White,
    primaryContainer = Mint,
    onPrimaryContainer = ForestDeep,
    secondary = Peach,
    onSecondary = Ink,
    secondaryContainer = Color(0xFFFFE8D9),
    onSecondaryContainer = Color(0xFF5F3924),
    tertiary = Color(0xFF80679A),
    tertiaryContainer = Lilac,
    background = Cream,
    surface = Paper,
    surfaceVariant = MintSoft,
    onSurface = Ink,
    onSurfaceVariant = MutedInk,
    outline = Hairline,
    error = Color(0xFFB84A44)
)

private val HomeTypography = Typography().run {
    copy(
        headlineLarge = headlineLarge.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.35).sp),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.15).sp),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.Bold),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.15.sp),
        bodyLarge = bodyLarge.copy(lineHeight = 23.sp),
        bodyMedium = bodyMedium.copy(lineHeight = 20.sp)
    )
}

@Composable
fun OurHomeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = HomeColours,
        typography = HomeTypography,
        content = content
    )
}
