package app.colde

import app.colde.ui.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AppThemeTest {

    @Test
    fun `every theme produces a visibly different background`() {
        val lightBgs = AppTheme.entries.map { it.surfaceColorInt(dark = false) }.toSet()
        val darkBgs = AppTheme.entries.map { it.surfaceColorInt(dark = true) }.toSet()

        assertEquals(AppTheme.entries.size, lightBgs.size)
        assertEquals(AppTheme.entries.size, darkBgs.size)
    }

    @Test
    fun `light and dark backgrounds differ within the same theme`() {
        AppTheme.entries.forEach { theme ->
            assertNotEquals(theme.surfaceColorInt(true), theme.surfaceColorInt(false))
        }
    }

    @Test
    fun `fromKey resolves valid names and falls back to OCEANO`() {
        assertEquals(AppTheme.AURORA, AppTheme.fromKey("AURORA"))
        assertEquals(AppTheme.OCEANO, AppTheme.fromKey(null))
        assertEquals(AppTheme.OCEANO, AppTheme.fromKey("nao-existe"))
    }
}
