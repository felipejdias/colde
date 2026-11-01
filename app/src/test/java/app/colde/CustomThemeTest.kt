package app.colde

import androidx.compose.ui.graphics.Color
import app.colde.ui.theme.AppTheme
import app.colde.ui.theme.CustomThemeColors
import app.colde.ui.theme.bestOnColor
import app.colde.ui.theme.customScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CustomThemeTest {

    @Test
    fun `custom scheme applies the chosen background and text`() {
        val colors = CustomThemeColors(
            background = 0xFF102030.toInt(),
            text = 0xFFEEFFEE.toInt()
        )
        val scheme = customScheme(colors)
        assertEquals(Color(0xFF102030), scheme.background)
        assertEquals(Color(0xFF102030), scheme.surface)
        assertEquals(Color(0xFFEEFFEE), scheme.onBackground)
        assertEquals(Color(0xFFEEFFEE), scheme.onSurface)
    }

    @Test
    fun `custom scheme applies the chosen primary`() {
        val colors = CustomThemeColors(primary = 0xFFAA00CC.toInt())
        val scheme = customScheme(colors)
        assertEquals(Color(0xFFAA00CC), scheme.primary)
        assertEquals(Color(0xFFAA00CC), scheme.surfaceTint)
    }

    @Test
    fun `bestOnColor picks readable contrast`() {
        assertEquals(0xFF14181A.toInt(), bestOnColor(0xFFFFFFFF.toInt()))
        assertEquals(0xFFF7F7F7.toInt(), bestOnColor(0xFF101418.toInt()))
    }

    @Test
    fun `custom colors serialize and deserialize round-trip`() {
        val colors = CustomThemeColors(
            primary = 0xFF112233.toInt(),
            secondary = 0xFF445566.toInt(),
            tertiary = 0xFF778899.toInt(),
            background = 0xFFAABBCC.toInt(),
            text = 0xFFDDEEFF.toInt()
        )
        assertEquals(colors, CustomThemeColors.deserialize(colors.serialize()))
    }

    @Test
    fun `deserialize rejects invalid or partial data`() {
        assertNull(CustomThemeColors.deserialize(null))
        assertNull(CustomThemeColors.deserialize("lixo"))
        assertNull(CustomThemeColors.deserialize("ff112233,ff445566"))
    }

    @Test
    fun `fromKey resolves CUSTOM`() {
        assertEquals(AppTheme.CUSTOM, AppTheme.fromKey("CUSTOM"))
    }
}
