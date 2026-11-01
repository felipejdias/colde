# Colde — Cold Wallet Offline

App Android (Jetpack Compose, Material 3) que gera carteiras Bitcoin frias offline:
seed BIP39 de 12 palavras + passphrase opcional (25ª palavra), endereço SegWit
nativo (bc1...), xpub para watch-only, export/import de backup criptografado `.colde`,
seletor de tema (paletas geradas por seed + tema Personalizado editável).

## Estado atual (pendências para continuar)

- **Derivação BIP84 aplicada**: `generateNewWallet()` usa
  `KeyChainGroupStructure.BIP43` → `m/84'/0'/0'` (BlueWallet/Sparrow compatível).
  Antes usava `BIP32` → `m/1'` (legado bitcoinj, incompatível com wallets padrão).
  `Bip84DerivationTest` trava o vetor oficial (`abandon...about` → `bc1qcr8te...`).
- **Pendente**: usuário vai validar no emulador se o endereço gerado pelo Colde bate
  com o primeiro endereço que a BlueWallet deriva das mesmas 12 palavras
  (importou e viu carteira diferente + msg de "conversão" — investigar tipo de
  wallet criada / seed antiga reimportada / Electrum-seed detection).
- **Carteiras antigas (formato m/1')**: recuperáveis via backup `.colde` ou seed em
  wallet com derivação customizada (Sparrow/Electrum) path `m/1'/0/0`.
- **Push pendente**: commit local existe; só dar push após OK do usuário no
  emulador. Push dispara release automática (vX.Y.Z + APK assinado).
- **Secrets do GitHub Actions** (repo `cold-wallet-offline`, Settings > Secrets):
  precisa dos 4: `KEYSTORE_BASE64` (conteúdo de `keystore-base64.txt`),
  `KEYSTORE_PASSWORD` e `KEY_PASSWORD` (valor em `keystore.properties`),
  `KEY_ALIAS` = `colde`. Sem eles o workflow falha no check — adicionar e re-rodar.

## Comandos

- Testes unitários: `./gradlew testDebugUnitTest`
- Testes de UI (emulador): `./gradlew connectedDebugAndroidTest` — rodar UM app por
  vez no emulador (duas suítes em paralelo quebram por disputa de foco)
- Instalar no emulador: `./gradlew installDebug`
- adb: `C:\Users\jacon\AppData\Local\Android\Sdk\platform-tools\adb.exe -s emulator-5554`

## Notas de arquitetura

- `applicationId`/`namespace`: `app.colde` (pacote antigo `com.fjdias.cold_wallet_offline` removido)
- Backup `.colde`: JSON `colde-backup` v1 — Argon2id (t=3, m=65536 KiB, p=1) +
  AES-256-GCM. `BackupCrypto` usa BouncyCastle puro-Java (testável em JVM) e
  `java.util.Base64` — não usar `android.util.Base64`/argon2kt JNI nessa classe.
- Export criptografa ANTES de abrir o SAF picker (`pendingExportBytes`): o picker
  joga o app pra background → `ON_STOP` → `clearScreen()` zera a seed.
- Temas: `AppTheme` enum + `CustomThemeColors` (prefs `colde_prefs`, chaves
  `theme`/`custom_theme`). Cores geradas por blend puro em Kotlin — sem
  `androidx.core.graphics.ColorUtils`/`android.graphics.Color` (quebram em teste JVM).
- Keystore release local: `release.keystore` + `keystore.properties` +
  `keystore-base64.txt` (todos gitignored). Alias `colde`.
