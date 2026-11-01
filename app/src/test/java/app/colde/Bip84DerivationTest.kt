package app.colde

import org.bitcoinj.base.ScriptType
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.wallet.DeterministicSeed
import org.bitcoinj.wallet.KeyChainGroupStructure
import org.bitcoinj.wallet.Wallet
import org.junit.Assert.assertEquals
import org.junit.Test

// Vetores oficiais do BIP84: seed "abandon ... about" deve derivar os endereços
// documentados na spec. Se a derivação regredir para o layout legado do bitcoinj
// (m/1'), este teste quebra — é o que garante a portabilidade das 12 palavras.
class Bip84DerivationTest {

    private val params = MainNetParams.get()

    private fun walletFrom(mnemonic: String, passphrase: String = ""): Wallet {
        val seed = DeterministicSeed.ofMnemonic(mnemonic.split(" "), passphrase)
        return Wallet.fromSeed(params, seed, ScriptType.P2WPKH, KeyChainGroupStructure.BIP43)
    }

    @Test
    fun `abandon seed derives the official BIP84 first receive address`() {
        val wallet = walletFrom(
            "abandon abandon abandon abandon abandon abandon " +
            "abandon abandon abandon abandon abandon about"
        )
        assertEquals(
            "bc1qcr8te4kr609gcawutmrza0j4xv80jy8z306fyu",
            wallet.currentReceiveAddress().toString()
        )
    }

    @Test
    fun `passphrase changes the derived address`() {
        val noPass = walletFrom(
            "abandon abandon abandon abandon abandon abandon " +
            "abandon abandon abandon abandon abandon about"
        )
        val withPass = walletFrom(
            "abandon abandon abandon abandon abandon abandon " +
            "abandon abandon abandon abandon abandon about",
            "minha-frase-secreta"
        )
        assertEquals("bc1qcr8te4kr609gcawutmrza0j4xv80jy8z306fyu", noPass.currentReceiveAddress().toString())
        org.junit.Assert.assertNotEquals(
            noPass.currentReceiveAddress().toString(),
            withPass.currentReceiveAddress().toString()
        )
    }
}
