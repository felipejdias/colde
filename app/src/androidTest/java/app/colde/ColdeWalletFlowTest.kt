package app.colde

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ColdeWalletFlowTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun generateWallet_showsSeedAddressAndXpub() {
        composeTestRule.onNodeWithTag("GenerateWalletButton").performClick()

        composeTestRule.onNodeWithTag("SeedCard").assertIsDisplayed()
        (1..12).forEach { i ->
            composeTestRule.onNodeWithTag("SeedWord_$i").assertExists()
        }
        composeTestRule.onNodeWithTag("BitcoinAddress").assertTextContains("bc1", substring = true)
        composeTestRule.onNodeWithTag("ExtendedPubKey").assertIsDisplayed()
        composeTestRule.onNodeWithTag("GenerateWalletButton").assertDoesNotExist()
    }

    @Test
    fun themePicker_changesAndPersistsTheme() {
        composeTestRule.onNodeWithTag("ThemePickerButton").performClick()
        composeTestRule.onNodeWithText("Tema do aplicativo").assertIsDisplayed()

        composeTestRule.onNodeWithTag("ThemeItem_AURORA").performClick()
        composeTestRule.onNodeWithText("Tema do aplicativo").assertDoesNotExist()

        val prefs = ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("colde_prefs", Context.MODE_PRIVATE)
        assertEquals("AURORA", prefs.getString("theme", null))
    }

    @Test
    fun clearScreen_hidesWalletData() {
        composeTestRule.onNodeWithTag("GenerateWalletButton").performClick()
        composeTestRule.onNodeWithTag("SeedCard").assertIsDisplayed()

        composeTestRule.onNodeWithTag("ClearScreenButton").performScrollTo().performClick()

        composeTestRule.onNodeWithTag("SeedCard").assertDoesNotExist()
        composeTestRule.onNodeWithTag("GenerateWalletButton").assertIsDisplayed()
    }

    @Test
    fun exportDialog_rejectsShortPassword() {
        composeTestRule.onNodeWithTag("GenerateWalletButton").performClick()
        composeTestRule.onNodeWithTag("ExportBackupButton").performScrollTo().performClick()

        composeTestRule.onNodeWithTag("ExportPasswordField").performTextInput("curta123")
        composeTestRule.onNodeWithTag("ExportConfirmField").performTextInput("curta123")
        composeTestRule.onNodeWithText("Exportar").performClick()

        composeTestRule.onNodeWithTag("ExportError")
            .assertTextContains("mínimo 12 caracteres", substring = true)
    }

    @Test
    fun exportDialog_rejectsMismatchedPasswords() {
        composeTestRule.onNodeWithTag("GenerateWalletButton").performClick()
        composeTestRule.onNodeWithTag("ExportBackupButton").performScrollTo().performClick()

        composeTestRule.onNodeWithTag("ExportPasswordField").performTextInput("senha-comprida-123")
        composeTestRule.onNodeWithTag("ExportConfirmField").performTextInput("senha-diferente-456")
        composeTestRule.onNodeWithText("Exportar").performClick()

        composeTestRule.onNodeWithTag("ExportError")
            .assertTextContains("não coincidem", substring = true)
    }

    @Test
    fun exportDialog_passwordVisibilityToggle() {
        composeTestRule.onNodeWithTag("GenerateWalletButton").performClick()
        composeTestRule.onNodeWithTag("ExportBackupButton").performScrollTo().performClick()

        composeTestRule.onNodeWithText("Mostrar").performClick()
        composeTestRule.onNodeWithText("Ocultar").assertIsDisplayed()
    }
}
