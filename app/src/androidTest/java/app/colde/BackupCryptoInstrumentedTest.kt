package app.colde

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupCryptoInstrumentedTest {

    @Test
    fun exportImportRoundTripOnDevice() {
        val password = "senha-super-secreta-123".toCharArray()
        val payload = BackupCrypto.buildPayload(
            List(12) { "abandon" }, "", "bc1qtest", "zpub6test"
        )
        val envelope = BackupCrypto.encryptBackup(payload, password)
        assertEquals(payload, BackupCrypto.decryptBackup(envelope, password))
    }
}
