# Colde — Offline Bitcoin Cold Wallet

An offline Android app for generating Bitcoin cold wallets. No internet permission,
no accounts, no tracking — everything happens on-device, mathematically.

[English](#english) | [Português](#português)

---

## English

### What it does

- **BIP39 mnemonic** — generates a 12-word seed phrase with an optional passphrase
  (the "25th word")
- **BIP84 Native SegWit** — derives `bc1...` addresses on `m/84'/0'/0'`, the same
  path used by BlueWallet, Sparrow and other standard wallets. Your 12 words are
  fully portable: import them anywhere and get the same addresses
- **Watch-only xpub** — export the extended public key to track balance and
  receive addresses in a watch-only wallet (Sparrow/BlueWallet) without exposing keys
- **Encrypted `.colde` backups** — export the wallet as a password-protected file:
  Argon2id (t=3, 64 MiB, p=1) key derivation + AES-256-GCM. Password minimum 12
  characters with confirmation and show/hide toggle. The app can also
  **import** `.colde` files to restore a wallet
- **Screenshot protection** — `FLAG_SECURE` blocks screenshots and recent-task
  thumbnails
- **Auto-wipe on background** — the screen is cleared whenever the app goes to
  the background (`ON_STOP`)
- **Themes** — 6 generated palettes plus a fully custom theme editor (pick
  primary/secondary/accent/background/text colors with live preview)
- **Zero permissions** — the manifest requests no permissions at all, not even
  INTERNET. The app physically cannot reach the network
- **`allowBackup="false"`** — Android's adb backup can't exfiltrate app data

### Security model

| Layer | Mechanism |
|-------|-----------|
| Key generation | `SecureRandom` + bitcoinj BIP39 (128-bit entropy) |
| Backup encryption | AES-256-GCM with random 16-byte salt and 12-byte nonce |
| Brute-force resistance | Argon2id memory-hard KDF (64 MiB, 3 iterations) |
| File format | Self-describing JSON (`colde-backup` v1) — open and inspectable |

Wrong passwords and tampered ciphertexts fail safely — nothing is restored.

> **Note:** the `.colde` file protects the backup at rest. The seed words **are**
> the wallet — anyone who sees them owns the funds. Write them on paper.

### Install

Download the signed APK from [Releases](../../releases) and sideload it
(allow "install unknown apps" when prompted). All releases are signed with the
same key, so updates install in place without losing data.

### Build

Requires JDK 21 and the Android SDK.

```bash
./gradlew installDebug        # debug build on a connected device/emulator
./gradlew assembleRelease     # signed release (uses keystore.properties or env vars)
./gradlew testDebugUnitTest   # unit tests
./gradlew connectedDebugAndroidTest  # UI tests on device/emulator
```

Release signing reads `keystore.properties` locally or `RELEASE_STORE_*`
environment variables in CI. See the workflow in `.github/workflows/`.

---

## Português

### O que o app faz

- **Mnemonic BIP39** — gera uma seed de 12 palavras com passphrase opcional
  (a "25ª palavra")
- **SegWit nativo BIP84** — deriva endereços `bc1...` em `m/84'/0'/0'`, o mesmo
  path da BlueWallet, Sparrow e outras carteiras padrão. Suas 12 palavras são
  totalmente portáteis: importe em qualquer lugar e obtenha os mesmos endereços
- **xpub watch-only** — exporte a chave pública estendida para acompanhar saldo
  e endereços numa carteira watch-only (Sparrow/BlueWallet) sem expor as chaves
- **Backups `.colde` criptografados** — exporte a carteira num arquivo protegido
  por senha: derivação Argon2id (t=3, 64 MiB, p=1) + AES-256-GCM. Senha mínima de
  12 caracteres com confirmação e opção de mostrar/ocultar. O app também
  **importa** arquivos `.colde` para restaurar a carteira
- **Proteção contra print** — `FLAG_SECURE` bloqueia capturas de tela e
  miniaturas na lista de apps recentes
- **Limpeza automática** — a tela é apagada sempre que o app vai para segundo
  plano (`ON_STOP`)
- **Temas** — 6 paletas geradas mais um editor de tema personalizado (escolha
  cores de primária/secundária/destaque/fundo/texto com preview ao vivo)
- **Zero permissões** — o manifest não pede nenhuma permissão, nem mesmo
  INTERNET. O app fisicamente não consegue acessar a rede
- **`allowBackup="false"`** — o backup do Android via adb não vaza os dados

### Modelo de segurança

| Camada | Mecanismo |
|--------|-----------|
| Geração de chaves | `SecureRandom` + bitcoinj BIP39 (entropia de 128 bits) |
| Criptografia do backup | AES-256-GCM com salt aleatório de 16 bytes e nonce de 12 bytes |
| Resistência a brute force | KDF memory-hard Argon2id (64 MiB, 3 iterações) |
| Formato do arquivo | JSON autodescritivo (`colde-backup` v1) — aberto e inspecionável |

Senha errada e ciphertext adulterado falham com segurança — nada é restaurado.

> **Atenção:** a senha do `.colde` protege apenas o arquivo. As 12 palavras
> **são** a carteira — quem as vir, fica com os fundos. Anote em papel.

### Instalação

Baixe o APK assinado em [Releases](../../releases) e instale via sideload
(permita "instalar apps desconhecidos" quando solicitado). Todas as releases são
assinadas com a mesma chave, então updates instalam por cima sem perder dados.

### Build

Requer JDK 21 e o Android SDK.

```bash
./gradlew installDebug        # build debug num device/emulador conectado
./gradlew assembleRelease     # release assinada (usa keystore.properties ou env vars)
./gradlew testDebugUnitTest   # testes unitários
./gradlew connectedDebugAndroidTest  # testes de UI no device/emulador
```

A assinatura de release lê `keystore.properties` localmente ou as variáveis
`RELEASE_STORE_*` no CI. Veja o workflow em `.github/workflows/`.
