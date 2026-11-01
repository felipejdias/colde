package app.colde

import android.content.Context
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
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.colde.ui.components.CustomThemeDialog
import app.colde.ui.components.ThemePickerDialog
import app.colde.ui.theme.AppTheme
import app.colde.ui.theme.ColdeTheme
import app.colde.ui.theme.CustomThemeColors
import java.time.LocalDate
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.base.ScriptType
import org.bitcoinj.wallet.DeterministicSeed
import org.bitcoinj.wallet.KeyChainGroupStructure
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
            val prefs = remember { getSharedPreferences("colde_prefs", Context.MODE_PRIVATE) }
            var appTheme by remember { mutableStateOf(AppTheme.fromKey(prefs.getString("theme", null))) }
            var customColors by remember {
                mutableStateOf(CustomThemeColors.deserialize(prefs.getString("custom_theme", null)) ?: CustomThemeColors())
            }

            ColdeTheme(appTheme = appTheme, customColors = customColors) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ColdWalletScreen(
                        appTheme = appTheme,
                        customColors = customColors,
                        onThemeChange = { theme ->
                            appTheme = theme
                            prefs.edit().putString("theme", theme.name).apply()
                        },
                        onCustomColorsChange = { colors ->
                            customColors = colors
                            appTheme = AppTheme.CUSTOM
                            prefs.edit()
                                .putString("custom_theme", colors.serialize())
                                .putString("theme", AppTheme.CUSTOM.name)
                                .apply()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ColdWalletScreen(
    appTheme: AppTheme,
    customColors: CustomThemeColors,
    onThemeChange: (AppTheme) -> Unit,
    onCustomColorsChange: (CustomThemeColors) -> Unit
) {
    var seedWords by remember { mutableStateOf<List<String>?>(null) }
    var bitcoinAddress by remember { mutableStateOf("") }
    var extendedPubKey by remember { mutableStateOf("") }
    var passphrase by remember { mutableStateOf("") }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var exportPassword by remember { mutableStateOf("") }
    var exportPasswordConfirm by remember { mutableStateOf("") }
    var exportError by remember { mutableStateOf<String?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importPassword by remember { mutableStateOf("") }
    var importError by remember { mutableStateOf<String?>(null) }
    var pendingEnvelope by remember { mutableStateOf<String?>(null) }
    var pendingExportBytes by remember { mutableStateOf<ByteArray?>(null) }
    var showExportPassword by remember { mutableStateOf(false) }
    var showImportPassword by remember { mutableStateOf(false) }
    var showCustomThemeDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val params: NetworkParameters = MainNetParams.get()

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        // O picker do SAF joga o app pra background e o ON_STOP limpa a seed —
        // por isso os bytes criptografados são gerados ANTES de abrir o picker
        // e lidos de pendingExportBytes aqui (estado que a limpeza não toca).
        val bytes = pendingExportBytes
        if (uri != null && bytes != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(bytes)
                } ?: error("Não foi possível abrir o arquivo")
            }.onSuccess {
                Toast.makeText(context, "Backup criptografado salvo", Toast.LENGTH_LONG).show()
            }.onFailure {
                Toast.makeText(context, "Falha ao salvar: ${it.message}", Toast.LENGTH_LONG).show()
            }
        }
        pendingExportBytes = null
        exportPassword = ""
        exportPasswordConfirm = ""
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use {
                    String(it.readBytes(), Charsets.UTF_8)
                } ?: error("Não foi possível abrir o arquivo")
            }.onSuccess {
                pendingEnvelope = it
                importError = null
                showImportDialog = true
            }.onFailure {
                Toast.makeText(context, "Falha ao ler: ${it.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun generateNewWallet() {
        val secureRandom = SecureRandom()
        // Cria uma seed determinística com 128 bits de entropia (12 palavras) + passphrase opcional
        val seed = DeterministicSeed(secureRandom, 128, passphrase.trim())
        // Cria a carteira usando BIP84 (m/84'/0'/0') — o mesmo path da BlueWallet/Sparrow,
        // então as 12 palavras restauram a mesma carteira em qualquer wallet padrão.
        val wallet = Wallet.fromSeed(params, seed, ScriptType.P2WPKH, KeyChainGroupStructure.BIP43)

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

    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 64.dp, start = 24.dp, end = 24.dp, bottom = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(
                onClick = { showThemeDialog = true },
                modifier = Modifier.testTag("ThemePickerButton")
            ) {
                Icon(
                    painterResource(R.drawable.ic_palette),
                    contentDescription = "Escolher tema",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Text("Gerador de Cold Wallet", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Offline & Matemático", color = MaterialTheme.colorScheme.onSurfaceVariant)

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
                modifier = Modifier.fillMaxWidth().height(56.dp).testTag("GenerateWalletButton")
            ) {
                Text("GERAR NOVA CARTEIRA")
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("*/*")) },
                modifier = Modifier.fillMaxWidth().height(56.dp).testTag("ImportBackupButton")
            ) {
                Text("IMPORTAR BACKUP .COLDE")
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("SeedCard"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "SUA SEED (12 PALAVRAS):",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        "Anote no papel em ordem. É a única forma de recuperar seu dinheiro.",
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
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
                                    modifier = Modifier.weight(1f).testTag("SeedWord_$index"),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
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
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            modifier = Modifier.testTag("BitcoinAddress")
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
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.testTag("ExtendedPubKey")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(
                onClick = { showExportDialog = true },
                modifier = Modifier.fillMaxWidth().height(56.dp).testTag("ExportBackupButton")
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
            modifier = Modifier.fillMaxWidth().testTag("ClearScreenButton")
        ) {
            Text("APAGAR TELA (SEGURANÇA)")
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Certifique-se de estar offline ao usar este app.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

        Text(
            text = "v${BuildConfig.VERSION_NAME} · ${BuildConfig.BUILD_TIMESTAMP}",
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
        )
    }

    if (showThemeDialog) {
        ThemePickerDialog(
            currentTheme = appTheme,
            customColors = customColors,
            onThemeSelected = { theme ->
                if (theme == AppTheme.CUSTOM) {
                    showThemeDialog = false
                    showCustomThemeDialog = true
                } else {
                    onThemeChange(theme)
                    showThemeDialog = false
                }
            },
            onDismiss = { showThemeDialog = false }
        )
    }

    if (showCustomThemeDialog) {
        CustomThemeDialog(
            initial = customColors,
            onSave = { colors ->
                onCustomColorsChange(colors)
                showCustomThemeDialog = false
            },
            onDismiss = { showCustomThemeDialog = false }
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
                        modifier = Modifier.fillMaxWidth().testTag("ExportPasswordField"),
                        visualTransformation = if (showExportPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            TextButton(onClick = { showExportPassword = !showExportPassword }) {
                                Text(if (showExportPassword) "Ocultar" else "Mostrar", fontSize = 11.sp)
                            }
                        },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = exportPasswordConfirm,
                        onValueChange = { exportPasswordConfirm = it; exportError = null },
                        label = { Text("Confirmar senha") },
                        visualTransformation = if (showExportPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("ExportConfirmField")
                    )
                    exportError?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.testTag("ExportError"))
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
                            // Criptografa ANTES de abrir o picker: o ON_STOP dispara
                            // clearScreen() e zera a seed enquanto o SAF está aberto.
                            runCatching {
                                pendingExportBytes = BackupCrypto.encryptBackup(
                                    BackupCrypto.buildPayload(
                                        mnemonic = seedWords!!,
                                        passphrase = passphrase.trim(),
                                        address = bitcoinAddress,
                                        xpub = extendedPubKey
                                    ),
                                    exportPassword.toCharArray()
                                ).toByteArray(Charsets.UTF_8)
                            }.onSuccess {
                                showExportDialog = false
                                exportError = null
                                exportLauncher.launch("colde_backup_${LocalDate.now()}.colde")
                            }.onFailure {
                                exportError = "Falha ao criptografar o backup."
                            }
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

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = {
                showImportDialog = false
                importPassword = ""
                importError = null
                pendingEnvelope = null
            },
            title = { Text("Importar Backup") },
            text = {
                Column {
                    Text(
                        "Digite a senha do arquivo .colde para restaurar a carteira.",
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = importPassword,
                        onValueChange = { importPassword = it; importError = null },
                        label = { Text("Senha do backup") },
                        modifier = Modifier.fillMaxWidth().testTag("ImportPasswordField"),
                        visualTransformation = if (showImportPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            TextButton(onClick = { showImportPassword = !showImportPassword }) {
                                Text(if (showImportPassword) "Ocultar" else "Mostrar", fontSize = 11.sp)
                            }
                        },
                        singleLine = true
                    )
                    importError?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val envelope = pendingEnvelope
                    if (envelope == null) {
                        importError = "Nenhum arquivo selecionado."
                    } else {
                        runCatching {
                            BackupCrypto.parsePayload(
                                BackupCrypto.decryptBackup(envelope, importPassword.toCharArray())
                            )
                        }.onSuccess { wallet ->
                            seedWords = wallet.mnemonic
                            passphrase = wallet.passphrase
                            bitcoinAddress = wallet.address
                            extendedPubKey = wallet.xpub
                            showImportDialog = false
                            importPassword = ""
                            pendingEnvelope = null
                            Toast.makeText(context, "Carteira restaurada do backup", Toast.LENGTH_LONG).show()
                        }.onFailure {
                            importError = "Senha incorreta ou arquivo inválido."
                        }
                    }
                }) { Text("Importar") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showImportDialog = false
                    importPassword = ""
                    importError = null
                    pendingEnvelope = null
                }) { Text("Cancelar") }
            }
        )
    }
}