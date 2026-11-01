package app.colde.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

val LocalAppTheme = compositionLocalOf { AppTheme.OCEANO }
val LocalCustomColors = compositionLocalOf { CustomThemeColors() }

enum class AppTheme(
    val label: String,
    private val primarySeed: Int,
    private val secondarySeed: Int,
    private val tertiarySeed: Int
) {
    OCEANO("Oceano", 0xFF0061A4.toInt(), 0xFF4F6178.toInt(), 0xFF1E6A87.toInt()),
    ESMERALDA("Esmeralda", 0xFF006B4F.toInt(), 0xFF4C6358.toInt(), 0xFF3F6374.toInt()),
    AMETISTA("Ametista", 0xFF6750A4.toInt(), 0xFF625B71.toInt(), 0xFF7D5260.toInt()),
    AMBAR("Âmbar", 0xFF7F5800.toInt(), 0xFF6E5D40.toInt(), 0xFF566632.toInt()),
    POR_DO_SOL("Pôr do Sol", 0xFF9A4525.toInt(), 0xFF775654.toInt(), 0xFF705D00.toInt()),
    AURORA("Aurora", 0xFF00696B.toInt(), 0xFF7A5AAA.toInt(), 0xFFA04073.toInt()),
    CUSTOM("Personalizado", 0xFF546E7A.toInt(), 0xFF78909C.toInt(), 0xFF8D6E63.toInt());

    val previewColors = listOf(Color(primarySeed), Color(secondarySeed), Color(tertiarySeed))

    fun lightScheme(): ColorScheme = lightColorScheme(
        primary = Color(tone(primarySeed, 40)), onPrimary = Color(tone(primarySeed, 100)),
        primaryContainer = Color(tone(primarySeed, 90)), onPrimaryContainer = Color(tone(primarySeed, 10)),
        secondary = Color(tone(secondarySeed, 40)), onSecondary = Color(tone(secondarySeed, 100)),
        secondaryContainer = Color(tone(secondarySeed, 90)), onSecondaryContainer = Color(tone(secondarySeed, 10)),
        tertiary = Color(tone(tertiarySeed, 40)), onTertiary = Color(tone(tertiarySeed, 100)),
        tertiaryContainer = Color(tone(tertiarySeed, 90)), onTertiaryContainer = Color(tone(tertiarySeed, 10)),
        background = Color(neutral(98)), onBackground = Color(neutral(10)),
        surface = Color(neutral(98)), onSurface = Color(neutral(10)),
        surfaceVariant = Color(neutralVariant(90)), onSurfaceVariant = Color(neutralVariant(30)),
        outline = Color(neutralVariant(50)), outlineVariant = Color(neutralVariant(80)),
        surfaceContainerLowest = Color(neutral(100)),
        surfaceContainerLow = Color(neutral(96)),
        surfaceContainer = Color(neutral(94)),
        surfaceContainerHigh = Color(neutral(92)),
        surfaceContainerHighest = Color(neutral(90))
    )

    fun darkScheme(): ColorScheme = darkColorScheme(
        primary = Color(tone(primarySeed, 80)), onPrimary = Color(tone(primarySeed, 20)),
        primaryContainer = Color(tone(primarySeed, 30)), onPrimaryContainer = Color(tone(primarySeed, 90)),
        secondary = Color(tone(secondarySeed, 80)), onSecondary = Color(tone(secondarySeed, 20)),
        secondaryContainer = Color(tone(secondarySeed, 30)), onSecondaryContainer = Color(tone(secondarySeed, 90)),
        tertiary = Color(tone(tertiarySeed, 80)), onTertiary = Color(tone(tertiarySeed, 20)),
        tertiaryContainer = Color(tone(tertiarySeed, 30)), onTertiaryContainer = Color(tone(tertiarySeed, 90)),
        background = Color(neutral(6)), onBackground = Color(neutral(90)),
        surface = Color(neutral(6)), onSurface = Color(neutral(90)),
        surfaceVariant = Color(neutralVariant(30)), onSurfaceVariant = Color(neutralVariant(80)),
        outline = Color(neutralVariant(60)), outlineVariant = Color(neutralVariant(30)),
        surfaceContainerLowest = Color(neutral(4)),
        surfaceContainerLow = Color(neutral(10)),
        surfaceContainer = Color(neutral(12)),
        surfaceContainerHigh = Color(neutral(17)),
        surfaceContainerHighest = Color(neutral(22))
    )

    private fun tone(seed: Int, t: Int): Int = hslShift(seed, null, t)

    // Neutros = cinza no tom t com a seed misturada por blend direto.
    // Blend garante tinta visível — HSL com sat baixa colapsa pra ~branco
    // em luminosidade alta, que foi o bug do "fundo que não muda".
    private fun neutral(t: Int): Int = blendArgb(grayTone(t), primarySeed, if (t < 50) 0.20f else 0.14f)
    private fun neutralVariant(t: Int): Int = blendArgb(grayTone(t), primarySeed, if (t < 50) 0.28f else 0.22f)

    fun surfaceColorInt(dark: Boolean): Int = if (dark) neutral(6) else neutral(98)

    companion object {
        fun fromKey(key: String?): AppTheme = entries.find { it.name == key } ?: OCEANO
    }
}

// ----- Matemática de cor compartilhada (JVM puro, testável em unit test) -----

internal fun grayTone(t: Int): Int {
    val v = (t.coerceIn(0, 100) * 2.55f).roundToInt().coerceIn(0, 255)
    return -0x1000000 or (v shl 16) or (v shl 8) or v
}

internal fun blendArgb(base: Int, tint: Int, amount: Float): Int {
    val a = amount.coerceIn(0f, 1f)
    val r = ((base shr 16 and 0xFF) * (1f - a) + (tint shr 16 and 0xFF) * a).roundToInt()
    val g = ((base shr 8 and 0xFF) * (1f - a) + (tint shr 8 and 0xFF) * a).roundToInt()
    val b = ((base and 0xFF) * (1f - a) + (tint and 0xFF) * a).roundToInt()
    return -0x1000000 or (r shl 16) or (g shl 8) or b
}

// Luminância relativa W3C — decide texto claro ou escuro sobre uma cor.
internal fun relativeLuminance(c: Int): Float {
    fun f(v: Int): Float {
        val s = v / 255f
        return if (s <= 0.03928f) s / 12.92f else ((s + 0.055f) / 1.055f).pow(2.4f)
    }
    return 0.2126f * f(c shr 16 and 0xFF) + 0.7152f * f(c shr 8 and 0xFF) + 0.0722f * f(c and 0xFF)
}

internal fun bestOnColor(bg: Int): Int =
    if (relativeLuminance(bg) > 0.30f) 0xFF14181A.toInt() else 0xFFF7F7F7.toInt()

// HSL puro (sem android.graphics) para rodar em testes JVM locais.
internal fun hslShift(seed: Int, sat: Float?, t: Int): Int {
    val r = (seed shr 16 and 0xFF) / 255f
    val g = (seed shr 8 and 0xFF) / 255f
    val b = (seed and 0xFF) / 255f

    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val d = max - min
    var h = 0f
    var s = 0f
    if (d != 0f) {
        val l = (max + min) / 2f
        s = if (l > 0.5f) d / (2f - max - min) else d / (max + min)
        h = when (max) {
            r -> (g - b) / d + (if (g < b) 6f else 0f)
            g -> (b - r) / d + 2f
            else -> (r - g) / d + 4f
        } * 60f
    }
    s = sat ?: s
    val l = t.coerceIn(0, 100) / 100f

    val c = (1f - abs(2f * l - 1f)) * s
    val hp = h / 60f
    val x = c * (1f - abs(hp % 2f - 1f))
    val (r1, g1, b1) = when {
        hp < 1f -> Triple(c, x, 0f)
        hp < 2f -> Triple(x, c, 0f)
        hp < 3f -> Triple(0f, c, x)
        hp < 4f -> Triple(0f, x, c)
        hp < 5f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    val m = l - c / 2f
    val ri = ((r1 + m) * 255f).roundToInt().coerceIn(0, 255)
    val gi = ((g1 + m) * 255f).roundToInt().coerceIn(0, 255)
    val bi = ((b1 + m) * 255f).roundToInt().coerceIn(0, 255)
    return -0x1000000 or (ri shl 16) or (gi shl 8) or bi
}
