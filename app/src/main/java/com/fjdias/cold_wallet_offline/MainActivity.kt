package com.fjdias.cold_wallet_offline

import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fjdias.cold_wallet_offline.ui.theme.ColdwalletofflineTheme
import java.time.LocalDate
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.base.ScriptType
import org.bitcoinj.wallet.DeterministicSeed
import org.bitcoinj.wallet.Wallet
import java.security.SecureRandom

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Impede screenshots e thumbnails na lista de apps recentes (a seed não pode vazar)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
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
    var extendedPubKey by remember { mutableStateOf("") }
    var passphrase by remember { mutableStateOf("") }
    var showExportDialog by remember { mutableStateOf(false) }
    var exportPassword by remember { mutableStateOf("") }
    var exportPasswordConfirm by remember { mutableStateOf("") }
    var exportError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val params: NetworkParameters = MainNetParams.get()

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null && seedWords != null) {
            runCatching {
                val payload = BackupCrypto.buildPayload(
                    mnemonic = seedWords!!,
                    passphrase = passphrase.trim(),
                    address = bitcoinAddress,
                    xpub = extendedPubKey
                )
                val encrypted = BackupCrypto.encryptBackup(payload, exportPassword.toCharArray())
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(encrypted.toByteArray(Charsets.UTF_8))
                } ?: error("Não foi possível abrir o arquivo")
            }.onSuccess {
                Toast.makeText(context, "Backup criptografado salvo", Toast.LENGTH_LONG).show()
            }.onFailure {
                Toast.makeText(context, "Falha ao salvar: ${it.message}", Toast.LENGTH_LONG).show()
            }
        }
        exportPassword = ""
        exportPasswordConfirm = ""
    }

    fun generateNewWallet() {
        val secureRandom = SecureRandom()
        // Cria uma seed determinística com 128 bits de entropia (12 palavras) + passphrase opcional
        val seed = DeterministicSeed(secureRandom, 128, passphrase.trim())
        // Cria a carteira usando o padrão SegWit Nativo (Bech32 - começa com bc1)
        val wallet = Wallet.fromSeed(params, seed, ScriptType.P2WPKH)

        seedWords = seed.mnemonicCode
        bitcoinAddress = wallet.currentReceiveAddress().toString()
        extendedPubKey = wallet.watchingKey.serializePubB58(params)
    }

    fun clearScreen() {
        seedWords = null
        bitcoinAddress = ""
        extendedPubKey = ""
        passphrase = ""
    }

    // Limpa a tela automaticamente quando o app vai para segundo plano
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) clearScreen()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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
            OutlinedTextField(
                value = passphrase,
                onValueChange = { passphrase = it },
                label = { Text("Passphrase (opcional — a 25ª palavra)") },
                supportingText = {
                    Text(
                        "Deixa as 12 palavras inúteis sozinhas: quem achar o papel restaura uma carteira vazia. " +
                        "ATENÇÃO: muda a carteira inteira — sem ela você NÃO recupera os fundos. " +
                        "Memorize ou guarde em local separado do papel. Não é salva pelo app."
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
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

            Spacer(modifier = Modifier.height(24.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("XPUB (MONITORAMENTO):", fontWeight = FontWeight.Bold)
                    Text(
                        "Chave pública estendida — só leitura. Importe numa watch-only wallet (Sparrow/BlueWallet) para ver saldo de TODOS os endereços. Não gasta fundos, mas revela seu histórico — compartilhe com cuidado.",
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SelectionContainer {
                        Text(
                            extendedPubKey,
                            color = Color(0xFF004D40),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(
                onClick = { showExportDialog = true },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("EXPORTAR BACKUP CRIPTOGRAFADO")
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        Spacer(modifier = Modifier.weight(1f, fill = false))
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { clearScreen() },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("APAGAR TELA (SEGURANÇA)")
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Certifique-se de estar offline ao usar este app.",
            fontSize = 11.sp,
            color = Color.Gray
        )
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = {
                showExportDialog = false
                exportError = null
            },
            title = { Text("Backup Criptografado") },
            text = {
                Column {
                    Text(
                        "O arquivo .colde sai com AES-256-GCM e chave derivada por Argon2id " +
                        "(KDF memory-hard — resistente a brute force por GPU/ASIC). " +
                        "Use uma senha longa: sem ela, nem o melhor KDF salva. " +
                        "NÃO é recuperável — esqueceu, perdeu o backup.",
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = exportPassword,
                        onValueChange = { exportPassword = it; exportError = null },
                        label = { Text("Senha do backup (mín. 12 caracteres)") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = exportPasswordConfirm,
                        onValueChange = { exportPasswordConfirm = it; exportError = null },
                        label = { Text("Confirmar senha") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    exportError?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    when {
                        exportPassword.length < 12 ->
                            exportError = "Senha curta demais — mínimo 12 caracteres."
                        exportPassword != exportPasswordConfirm ->
                            exportError = "As senhas não coincidem."
                        else -> {
                            showExportDialog = false
                            exportError = null
                            exportLauncher.launch("colde_backup_${LocalDate.now()}.colde")
                        }
                    }
                }) { Text("Exportar") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showExportDialog = false
                    exportError = null
                }) { Text("Cancelar") }
            }
        )
    }
}