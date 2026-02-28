package com.fjdias.cold_wallet_offline

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fjdias.cold_wallet_offline.ui.theme.ColdwalletofflineTheme
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.script.Script
import org.bitcoinj.wallet.DeterministicSeed
import org.bitcoinj.wallet.Wallet
import java.security.SecureRandom

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ColdwalletofflineTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ColdWalletScreen()
                }
            }
        }
    }
}

@Composable
fun ColdWalletScreen() {
    var seedWords by remember { mutableStateOf<List<String>?>(null) }
    var bitcoinAddress by remember { mutableStateOf("") }
    val params: NetworkParameters = MainNetParams.get()

    fun generateNewWallet() {
        val secureRandom = SecureRandom()
        // Cria uma seed determinística com 128 bits de entropia (12 palavras)
        val seed = DeterministicSeed(secureRandom, 128, "")
        // Cria a carteira usando o padrão SegWit Nativo (Bech32 - começa com bc1)
        val wallet = Wallet.fromSeed(params, seed, Script.ScriptType.P2WPKH)

        seedWords = seed.mnemonicCode
        bitcoinAddress = wallet.currentReceiveAddress().toString()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 64.dp, start = 24.dp, end = 24.dp, bottom = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Gerador de Cold Wallet", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Offline & Matemático", color = Color.Gray)

        Spacer(modifier = Modifier.height(32.dp))

        if (seedWords == null) {
            Button(
                onClick = { generateNewWallet() },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("GERAR NOVA CARTEIRA")
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9C4))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "SUA SEED (12 PALAVRAS):",
                        fontWeight = FontWeight.Bold,
                        color = Color.Red
                    )
                    Text(
                        "Anote no papel em ordem. É a única forma de recuperar seu dinheiro.",
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Exibe as palavras em grid
                    seedWords?.chunked(2)?.forEachIndexed { rowIndex, row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            row.forEachIndexed { colIndex, word ->
                                val index = (rowIndex * 2) + colIndex + 1
                                Text(
                                    "$index. $word",
                                    modifier = Modifier.weight(1f),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("ENDEREÇO DE RECEBIMENTO (BTC):", fontWeight = FontWeight.Bold)
                    Text("Você pode compartilhar este endereço para receber BTC.", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    SelectionContainer {
                        Text(
                            bitcoinAddress,
                            color = Color(0xFF004D40),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    seedWords = null
                    bitcoinAddress = ""
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("APAGAR TELA (SEGURANÇA)")
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Certifique-se de estar offline ao usar este app.",
            fontSize = 11.sp,
            color = Color.Gray
        )
    }
}