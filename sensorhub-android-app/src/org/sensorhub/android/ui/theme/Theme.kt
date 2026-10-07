package org.sensorhub.android.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val OSHColorScheme = darkColorScheme(
private val OSHDarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    tertiary = Tertiary,
    onTertiary = OnTertiary,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = OnTertiaryContainer,
    error = Error,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    outline = Outline
)

private val OSHLightColorScheme = lightColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimary,
    primaryContainer = Color(0xFFFFDBCF),
    onPrimaryContainer = Color(0xFF381000),
    secondary = Color(0xFF5F5E5E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE5E2E1),
    onSecondaryContainer = Color(0xFF1C1B1B),
    tertiary = Color(0xFF9C4325),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDBCF),
    onTertiaryContainer = Color(0xFF351000),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFFFBFF),
    onBackground = Color(0xFF201A18),
    surface = Color(0xFFFFFBFF),
    onSurface = Color(0xFF201A18),
    surfaceVariant = Color(0xFFF3DED7),
    onSurfaceVariant = Color(0xFF51433F),
    outline = Color(0xFF83736E),
)

@Immutable
data class OshStatusColors(
    val success: Color,
    val onSuccess: Color,
    val warning: Color,
    val onWarning: Color,
)

private val DefaultStatusColors = OshStatusColors(
    success = SuccessContainer,
    onSuccess = OnSuccessContainer,
    warning = WarningContainer,
    onWarning = OnWarningContainer,
)

val LocalOshStatusColors = staticCompositionLocalOf { DefaultStatusColors }

@Composable
fun OSHTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = AccentOrange.toArgb()
            window.navigationBarColor = SurfaceLow.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    CompositionLocalProvider(LocalOshStatusColors provides DefaultStatusColors) {
        MaterialTheme(
            colorScheme = OSHColorScheme,
            typography = OSHTypography,
            shapes = OSHShapes,
            content = content,
        )
    }
}
