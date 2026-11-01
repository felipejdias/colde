package app.colde

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.util.Base64
import javax.crypto.AEADBadTagException

class BackupCryptoTest {

    private val password = "senha-super-secreta-123".toCharArray()
    private val mnemonic = listOf(
        "abandon", "ability", "able", "about", "above", "absent",
        "absorb", "abstract", "absurd", "abuse", "access", "accident"
    )

    private fun payload() = BackupCrypto.buildPayload(mnemonic, "pass", "bc1qtest", "zpub6test")

    @Test
    fun `encrypt then decrypt round-trips the payload`() {
        val envelope = BackupCrypto.encryptBackup(payload(), password)
        assertEquals(payload(), BackupCrypto.decryptBackup(envelope, password))
    }

    @Test
    fun `envelope carries kdf and cipher metadata`() {
        val root = JSONObject(BackupCrypto.encryptBackup(payload(), password))
        assertEquals("colde-backup", root.getString("format"))
        val kdf = root.getJSONObject("kdf")
        assertEquals("argon2id", kdf.getString("algo"))
        assertEquals(65536, kdf.getInt("m"))
        val cipher = root.getJSONObject("cipher")
        assertEquals("AES-256-GCM", cipher.getString("algo"))
    }

    @Test
    fun `same payload encrypts to different envelopes`() {
        // Salt + nonce aleatórios: dois exports nunca geram o mesmo arquivo.
        val a = BackupCrypto.encryptBackup(payload(), password)
        val b = BackupCrypto.encryptBackup(payload(), password)
        assertNotEquals(a, b)
    }

    @Test
    fun `wrong password fails to decrypt`() {
        val envelope = BackupCrypto.encryptBackup(payload(), password)
        assertThrows(AEADBadTagException::class.java) {
            BackupCrypto.decryptBackup(envelope, "senha-errada-000".toCharArray())
        }
    }

    @Test
    fun `tampered ciphertext fails to decrypt`() {
        val root = JSONObject(BackupCrypto.encryptBackup(payload(), password))
        val cipherObj = root.getJSONObject("cipher")
        val data = Base64.getDecoder().decode(cipherObj.getString("data"))
        data[0] = (data[0] + 1).toByte()
        cipherObj.put("data", Base64.getEncoder().encodeToString(data))
        assertThrows(AEADBadTagException::class.java) {
            BackupCrypto.decryptBackup(root.toString(), password)
        }
    }

    @Test
    fun `non-colde file is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            BackupCrypto.decryptBackup("{\"foo\":1}", password)
        }
    }

    @Test
    fun `parsePayload restores wallet fields`() {
        val parsed = BackupCrypto.parsePayload(payload())
        assertEquals(mnemonic, parsed.mnemonic)
        assertEquals("pass", parsed.passphrase)
        assertEquals("bc1qtest", parsed.address)
        assertEquals("zpub6test", parsed.xpub)
    }

    @Test
    fun `parsePayload rejects mnemonic without 12 words`() {
        val bad = JSONObject()
            .put("type", "bip39-segwit-wallet")
            .put("mnemonic", "abandon abandon")
            .put("address", "a")
            .put("xpub", "x")
            .toString()
        assertThrows(IllegalArgumentException::class.java) {
            BackupCrypto.parsePayload(bad)
        }
    }
}
