package app.colde.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Cores escolhidas manualmente pelo usuário. Serializa como hex CSV
 * pra caber numa única chave de SharedPreferences ("custom_theme").
 */
data class CustomThemeColors(
    val primary: Int = 0xFF0061A4.toInt(),
    val secondary: Int = 0xFF4F6178.toInt(),
    val tertiary: Int = 0xFF1E6A87.toInt(),
    val background: Int = 0xFFF6FAFE.toInt(),
    val text: Int = 0xFF181C20.toInt()
) {
    fun serialize(): String =
        listOf(primary, secondary, tertiary, background, text)
            .joinToString(",") { Integer.toUnsignedString(it, 16) }

    companion object {
        fun deserialize(s: String?): CustomThemeColors? {
            val parts = s?.split(",")?.mapNotNull { it.toLongOrNull(16)?.toInt() } ?: return null
            return if (parts.size == 5)
                CustomThemeColors(parts[0], parts[1], parts[2], parts[3], parts[4])
            else null
        }
    }
}

/**
 * Esquema único a partir das cores escolhidas — ignora claro/escuro do SO:
 * o usuário escolheu as cores, elas valem sempre. Contraste de "on*" é
 * derivado por luminância; containers são blends entre seed e fundo.
 */
fun customScheme(c: CustomThemeColors): ColorScheme {
    val bg = c.background
    val text = c.text
    fun cont(seed: Int, amount: Float) = blendArgb(seed, bg, amount)
    return lightColorScheme(
        primary = Color(c.primary),
        onPrimary = Color(bestOnColor(c.primary)),
        primaryContainer = Color(cont(c.primary, 0.45f)),
        onPrimaryContainer = Color(bestOnColor(cont(c.primary, 0.45f))),
        secondary = Color(c.secondary),
        onSecondary = Color(bestOnColor(c.secondary)),
        secondaryContainer = Color(cont(c.secondary, 0.55f)),
        onSecondaryContainer = Color(bestOnColor(cont(c.secondary, 0.55f))),
        tertiary = Color(c.tertiary),
        onTertiary = Color(bestOnColor(c.tertiary)),
        tertiaryContainer = Color(cont(c.tertiary, 0.55f)),
        onTertiaryContainer = Color(bestOnColor(cont(c.tertiary, 0.55f))),
        background = Color(bg),
        onBackground = Color(text),
        surface = Color(bg),
        onSurface = Color(text),
        surfaceVariant = Color(blendArgb(bg, c.primary, 0.18f)),
        onSurfaceVariant = Color(text),
        surfaceTint = Color(c.primary),
        inverseSurface = Color(text),
        inverseOnSurface = Color(bg),
        inversePrimary = Color(blendArgb(c.primary, 0xFFFFFFFF.toInt(), 0.25f)),
        outline = Color(blendArgb(text, bg, 0.55f)),
        outlineVariant = Color(blendArgb(text, bg, 0.25f)),
        error = Color(0xFFB3261E),
        onError = Color(0xFFFFFFFF.toInt()),
        errorContainer = Color(0xFFF9DEDC.toInt()),
        onErrorContainer = Color(0xFF410E0B.toInt()),
        scrim = Color(0xFF000000.toInt()),
        surfaceContainerLowest = Color(blendArgb(bg, text, 0.03f)),
        surfaceContainerLow = Color(blendArgb(bg, text, 0.05f)),
        surfaceContainer = Color(blendArgb(bg, text, 0.08f)),
        surfaceContainerHigh = Color(blendArgb(bg, text, 0.12f)),
        surfaceContainerHighest = Color(blendArgb(bg, text, 0.16f))
    )
}
