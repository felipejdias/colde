package app.colde

import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import org.json.JSONObject
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object BackupCrypto {
    private const val FORMAT = "colde-backup"
    private const val VERSION = 1
    private const val SALT_BYTES = 16
    private const val NONCE_BYTES = 12
    private const val KEY_BYTES = 32

    // Argon2id — memory-hard de propósito: cada tentativa de brute force
    // custa ~64 MiB de RAM + ~1s de CPU, inviabilizando GPU/ASIC em escala.
    private const val ARGON2_T = 3
    private const val ARGON2_M_KIB = 65536
    private const val ARGON2_P = 1

    fun encryptBackup(payload: String, password: CharArray): String {
        val rng = SecureRandom()
        val salt = ByteArray(SALT_BYTES).also(rng::nextBytes)
        val nonce = ByteArray(NONCE_BYTES).also(rng::nextBytes)

        val key = deriveKey(password, salt, ARGON2_T, ARGON2_M_KIB, ARGON2_P)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
        val ciphertext = cipher.doFinal(payload.toByteArray(Charsets.UTF_8))

        return JSONObject()
            .put("format", FORMAT)
            .put("version", VERSION)
            .put("kdf", JSONObject()
                .put("algo", "argon2id")
                .put("t", ARGON2_T)
                .put("m", ARGON2_M_KIB)
                .put("p", ARGON2_P)
                .put("salt", b64(salt)))
            .put("cipher", JSONObject()
                .put("algo", "AES-256-GCM")
                .put("nonce", b64(nonce))
                .put("data", b64(ciphertext)))
            .toString(2)
    }

    fun decryptBackup(envelope: String, password: CharArray): String {
        val root = JSONObject(envelope)
        require(root.optString("format") == FORMAT) { "Arquivo não é um backup Colde" }
        val kdf = root.getJSONObject("kdf")
        require(kdf.getString("algo") == "argon2id") { "KDF não suportado" }
        val c = root.getJSONObject("cipher")
        require(c.getString("algo") == "AES-256-GCM") { "Cifra não suportada" }

        val key = deriveKey(
            password,
            unb64(kdf.getString("salt")),
            kdf.getInt("t"), kdf.getInt("m"), kdf.getInt("p")
        )
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(key, "AES"),
            GCMParameterSpec(128, unb64(c.getString("nonce")))
        )
        return String(cipher.doFinal(unb64(c.getString("data"))), Charsets.UTF_8)
    }

    private fun deriveKey(password: CharArray, salt: ByteArray, t: Int, m: Int, p: Int): ByteArray {
        val passwordBytes = String(password).toByteArray(Charsets.UTF_8)
        return try {
            val params = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                .withIterations(t)
                .withMemoryAsKB(m)
                .withParallelism(p)
                .withSalt(salt)
                .build()
            val generator = Argon2BytesGenerator()
            generator.init(params)
            ByteArray(KEY_BYTES).also { generator.generateBytes(passwordBytes, it, 0, it.size) }
        } finally {
            passwordBytes.fill(0)
        }
    }

    fun buildPayload(mnemonic: List<String>, passphrase: String, address: String, xpub: String): String =
        JSONObject()
            .put("type", "bip39-segwit-wallet")
            .put("mnemonic", mnemonic.joinToString(" "))
            .put("passphrase", passphrase)
            .put("address", address)
            .put("xpub", xpub)
            .toString()

    data class WalletPayload(
        val mnemonic: List<String>,
        val passphrase: String,
        val address: String,
        val xpub: String
    )

    fun parsePayload(payload: String): WalletPayload {
        val json = JSONObject(payload)
        require(json.optString("type") == "bip39-segwit-wallet") { "Payload não é uma carteira Colde" }
        val mnemonic = json.getString("mnemonic").trim().split(Regex("\\s+"))
        require(mnemonic.size == 12) { "Mnemonic deve ter 12 palavras" }
        return WalletPayload(
            mnemonic = mnemonic,
            passphrase = json.optString("passphrase"),
            address = json.getString("address"),
            xpub = json.getString("xpub")
        )
    }

    private fun b64(b: ByteArray): String = Base64.getEncoder().encodeToString(b)
    private fun unb64(s: String): ByteArray = Base64.getDecoder().decode(s)
}
