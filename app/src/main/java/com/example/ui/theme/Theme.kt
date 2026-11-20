package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.example.model.ThemePreference

private val LightColorScheme = lightColorScheme(
    primary = ElectricViolet,
    onPrimary = PureWhite,
    primaryContainer = LavenderGlow,
    onPrimaryContainer = MidnightNavy,
    secondary = SubtleCyan,
    onSecondary = PureWhite,
    secondaryContainer = CyanContainerLight,
    onSecondaryContainer = MidnightNavy,
    tertiary = SoftViolet,
    onTertiary = PureWhite,
    background = SoftLilac,
    onBackground = TextPrimaryLight,
    surface = PureWhite,
    onSurface = TextPrimaryLight,
    surfaceVariant = ColorTokens.LightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = LilacBorder,
    outlineVariant = ColorTokens.LightOutlineVariant,
    error = RiskCriticalColor,
    onError = PureWhite,
    errorContainer = RiskCriticalContainerLight,
    onErrorContainer = RiskCriticalContainerDark
)

private val DarkColorScheme = darkColorScheme(
    primary = SoftViolet,
    onPrimary = MidnightNavy,
    primaryContainer = DeepIndigoElevated,
    onPrimaryContainer = LavenderGlow,
    secondary = BrightCyan,
    onSecondary = MidnightNavy,
    secondaryContainer = CyanContainerDark,
    onSecondaryContainer = CyanContainerLight,
    tertiary = LavenderGlow,
    onTertiary = MidnightNavy,
    background = MidnightNavy,
    onBackground = TextPrimaryDark,
    surface = DeepIndigoSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = DeepIndigoCard,
    onSurfaceVariant = TextSecondaryDark,
    outline = DeepIndigoElevated,
    outlineVariant = ColorTokens.DarkOutlineVariant,
    error = RiskCriticalColor,
    onError = PureWhite,
    errorContainer = RiskCriticalContainerDark,
    onErrorContainer = RiskCriticalContainerLight
)

private object ColorTokens {
    val LightSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFECE6FF)
    val LightOutlineVariant = androidx.compose.ui.graphics.Color(0xFFD9CEFF)
    val DarkOutlineVariant = androidx.compose.ui.graphics.Color(0xFF363073)
}

val ScamShieldShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun ScamShieldTheme(
    themePreference: ThemePreference = ThemePreference.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themePreference) {
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ScamShieldTypography,
        shapes = ScamShieldShapes,
        content = content
    )
}
