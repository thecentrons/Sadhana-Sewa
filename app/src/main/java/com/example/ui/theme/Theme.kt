package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = SacredSaffron,
    onPrimary = Color.White,
    primaryContainer = SoftSaffronContainer,
    onPrimaryContainer = OnSaffronContainer,
    secondary = TulasiTeal,
    onSecondary = Color.White,
    secondaryContainer = SoftTulasiContainer,
    onSecondaryContainer = OnTulasiContainer,
    tertiary = TempleGold,
    onTertiary = Color.White,
    tertiaryContainer = SoftGoldContainer,
    onTertiaryContainer = OnGoldContainer,
    background = ParchmentCream,
    onBackground = SandalwoodInk,
    surface = IvorySurface,
    onSurface = SandalwoodInk,
    surfaceVariant = SandstoneSurface,
    onSurfaceVariant = UmberMuted,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = IvorySurface,
    surfaceContainer = SandstoneSurface,
    surfaceContainerHigh = Color(0xFFEDE1CF),
    outline = SandstoneBorder,
    outlineVariant = Color(0xFFEDE2D2),
    error = DevotionalError,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkSaffronPrimary,
    onPrimary = DarkOnSaffron,
    primaryContainer = DarkSaffronContainer,
    onPrimaryContainer = SoftSaffronContainer,
    secondary = DarkTulasiSecondary,
    onSecondary = DarkOnTulasi,
    secondaryContainer = DarkTulasiContainer,
    onSecondaryContainer = SoftTulasiContainer,
    tertiary = DarkGoldTertiary,
    onTertiary = DarkOnGold,
    tertiaryContainer = DarkGoldContainer,
    onTertiaryContainer = SoftGoldContainer,
    background = DarkBackground,
    onBackground = DarkInk,
    surface = DarkSurface,
    onSurface = DarkInk,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkMutedInk,
    surfaceContainerLow = DarkSurface,
    surfaceContainer = DarkSurfaceVariant,
    surfaceContainerHigh = Color(0xFF3C2D22),
    outline = DarkOutline,
    outlineVariant = Color(0xFF3E3026),
    error = Color(0xFFFF8982),
    onError = Color(0xFF5A0B0B)
)

val SadhanaShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun SadhanaSevaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = SadhanaShapes,
        content = content
    )
}
