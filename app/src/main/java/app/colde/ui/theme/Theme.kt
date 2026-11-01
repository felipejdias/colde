package app.colde.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember

@Composable
fun ColdeTheme(
    appTheme: AppTheme = AppTheme.OCEANO,
    customColors: CustomThemeColors = CustomThemeColors(),
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = remember(appTheme, customColors, darkTheme) {
        when {
            // CUSTOM ignora claro/escuro do SO — as cores escolhidas valem sempre.
            appTheme == AppTheme.CUSTOM -> customScheme(customColors)
            darkTheme -> appTheme.darkScheme()
            else -> appTheme.lightScheme()
        }
    }

    CompositionLocalProvider(
        LocalAppTheme provides appTheme,
        LocalCustomColors provides customColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
