# KeyStan Desktop

A Windows desktop companion for the KeyGo Android password manager: the same Material 3 look, the
same vault contents, and - the part that actually matters - **the same backup file**. A file
exported here restores on the phone, and a backup exported there opens here.

Built by [Nursqode](https://github.com/Nursqode) as part of
[KeyStan](https://github.com/Nursqode/KeyStan), a fork of
[KeyGo](https://github.com/OffRange/KeyGo) by Davis Alessandru. Licensed under the GNU GPL v3.0 or
later, like the project it belongs to.

```
desktop/
├── core/backup   the KeyGo v1 backup format: envelope, KDFs, AEAD, payload model
├── core/vault    the local vault: session, atomic file storage, settings, TOTP, generator
├── core/ui       the app's theme and shared composables
└── app           the window, its state, and the screens
```

## Running it

From this directory (`desktop/`):

```bat
..\gradlew.bat -p . :app:run
```

or `run.cmd`. The first run downloads the Kotlin and Compose Multiplatform toolchain.

**Packaging** (a self-contained folder with a `KeyStan.exe`, no installer and no WiX needed):

```bat
..\gradlew.bat -p . :app:createDistributable
```

The result is `app\build\compose\binaries\main\app\KeyStan\KeyStan.exe` - about 160 MB, because it
carries its own Java runtime and the Skia renderer, and it runs on a machine with no JVM installed.
Copy the whole `KeyStan` folder anywhere, including a USB stick; it is portable, and `KEYSTAN_DATA_DIR`
decides where it keeps the vault.

`packageMsi` and `packageExe` produce installers instead, but jpackage needs **WiX Toolset 3.x** on
the `PATH`; its own bundled copy is not accepted, and the build fails with
`Detected [light.exe] version ... but version 3.0 is required`.

Note that `gradlew clean` deletes the packaged folder along with the rest of `build/`; re-run
`createDistributable` (or `build-app.cmd`) to get it back.

**Tests:**

```bat
..\gradlew.bat -p . test
```

### Where the vault is kept

`%APPDATA%\KeyStan\vault.ksb` (and `settings.json` beside it). `KEYSTAN_DATA_DIR` or the
`keystan.dataDir` system property moves it, which is what makes a portable install on a USB stick
possible:

```bat
set KEYSTAN_DATA_DIR=E:\KeyStan
KeyStan.exe
```

### If the home directory is read-only

Skiko, Compose's renderer, unpacks `skiko-windows-x64.dll` and `icudtl.dat` into `~/.skiko` on
first run. Where that is not possible - a locked-down machine, or a sandbox that denies writes
outside the project - point it at a writable directory that already holds both files, taken from
`skiko-awt-runtime-windows-x64-<version>.jar`:

```bat
..\gradlew.bat -p . :app:run -Pskiko.library.path=C:\some\writable\dir
```

A packaged build does not need this: the Compose plugin passes `-Dskiko.library.path=$APPDIR` and
ships the two files beside the executable.

## The window

Three panes, which is what a desktop is for - the phone's back-and-forth between list and detail
disappears, and nothing about what an item *is* changes:

- **Left rail**: the vaults with their icons and item counts, then Generator, Import, Export and
  Settings, the name of the vault file, and Lock.
- **Middle**: the selected vault's items, searched and sorted with pinned entries first. Each row
  shows what identifies the entry - a username, a website, the last four digits of a card - and a
  login's one-time code with the seconds left before it changes.
- **Right**: the selected item in full, with per-field copy buttons, one switch that reveals every
  secret on the screen, and Edit / Move / Delete.

The item editor is a dialog over that layout, and a new entry can switch between login and card
before it is saved. Dialogs, not screens, for anything that can be dismissed.

## What it does

- Master passphrase, one local vault file, everything sealed at rest.
- Vaults with the Android app's own icon names, so a vault made here looks right on the phone.
- Logins (username, password, websites, TOTP secret, tags, notes, pinning) and cards
  (cardholder, number, expiry, CVV).
- Live one-time codes (RFC 6238, SHA-1/256/512, honouring an `otpauth://` URI's digits and period).
- Password generator with an entropy readout.
- Search across titles, usernames, notes, tags and websites.
- Import and export as a backup file, either replacing everything (Android's restore semantics) or
  merging into a chosen vault without duplicating entries.

## The compatibility contract

Both apps read and write the same envelope, and the desktop implementation was built against the
Rust core's own frozen test vectors rather than against a written spec:

```json
{"version":1,
 "encryption":{"source":"passphrase",
               "kdf":{"type":"argon2id","salt":"…","mem_kib":65536,"iters":3,"lanes":4},
               "nonce":"…"},
 "payload":"<base64 of ciphertext || 16-byte tag>"}
```

- **Key**: Argon2id (v1.3) over `passphrase` with the salt `salt || "v1:backup/passphrase"`
  (`"v1:backup/ark"` and HKDF-SHA256 for the account-key path), 64 MiB, 3 passes, 4 lanes, 32-byte
  output.
- **AEAD**: AES-256-GCM-SIV (RFC 8452), 12-byte nonce, 16-byte tag appended.
- **Additional data**: `BackupAad { version: u32, source: KeySource }` under BCS. BCS mixes
  encodings - the `u32` is four little-endian bytes while the enum's variant index is ULEB128 - and
  the result authenticates the file's version and key source.
- **Payload**: compact JSON in the current model (`vaults[].logins[]` / `cards[]`, snake_case,
  explicit nulls). Older writers' single `website`/`passkey` fields are promoted into the current
  lists on read, so an old backup does not lose a site.

### How that is verified

`core/backup/src/test/.../JsonBackupFormatTest.kt` contains the two vectors the Rust core keeps
frozen. The passphrase one is the strong test: the desktop decrypts the file Android wrote and then
**re-seals the same plaintext with the same salt and nonce, and the ciphertext comes out byte for
byte identical**. That can only happen if the KDF, the BCS-authenticated AAD and the AEAD all agree
with the Rust implementation, which is exactly what "the phone will accept this file" means.

The AEAD is additionally checked against RFC 8452's AES-256 vectors and the HKDF against RFC 5869's,
so a mistake in either would be caught even if the vectors were ever regenerated.

## Security notes

- The master passphrase is held as a `CharArray`, converted to UTF-8 bytes only for a derivation,
  and wiped when it is no longer needed; derived keys are wiped after use. The app never writes a
  passphrase anywhere.
- The vault file is written through a flushed temporary file and an atomic rename, so an interrupted
  save leaves the previous file intact rather than a truncated one. This matters more than usual
  because the whole file is rewritten on every edit.
- Nothing in the app talks to the network, and nothing is logged.
- What a JVM app cannot do: the decrypted payload exists as `String`s while the vault is unlocked,
  and those cannot be reliably zeroed. Locking drops the reference, which is the best a managed
  runtime offers.

## Deliberate limitations

- **Account-key (ARK) sealed files** can be recognised but not opened: the ARK belongs to the
  phone's account. Export from Android with a passphrase instead.
- **Passkeys** are carried through untouched - they survive an import and an export - but there is
  no passkey UI yet.
- **CSV** import/export (the Android app's other format) is not implemented.
- No autofill, no browser integration, no tray icon, no scheduled backups yet.

## Author and licence

The desktop version is by **Nursqode** (<https://github.com/Nursqode>), built on
[KeyGo](https://github.com/OffRange/KeyGo) by Davis Alessandru (OffRange).

Both are licensed under the **GNU General Public License v3.0 or later** - see [../LICENSE](../LICENSE).
Every desktop source file carries that notice in its header. The vault format is shared with KeyGo on
purpose, so the two applications are interoperable rather than merely similar.

## Notes for the Android side

Three things found while matching the format, worth a look in `rust/rust-code`:

1. **`bcs` encodes `u32` as four little-endian bytes**, not ULEB128 (`serialize_u32` uses
   `to_le_bytes`, while an enum's variant index goes through `output_variant_index`). The AAD for a
   version-1 passphrase backup is therefore `01 00 00 00 00`. Anyone reimplementing the format from
   the BCS overview alone will get this wrong; it is now pinned by a test here.
2. **The frozen ARK vector does not open.** `golden_v1_ark_still_decrypts` cannot pass with the
   derivation `key.rs` describes, for the ARK the test hardcodes (`[9u8; 32]`): the passphrase vector
   in the same file verifies the envelope, the KDF inputs and the AEAD, so the mismatch is specific
   to that file or to how it was generated. The corresponding desktop test is `@Ignore`d with that
   explanation rather than deleted.
3. **Old single-site backups lose data on import.** The frozen v1 payload carries
   `"website": "https://mail.example"` (a string) and `"passkey": null`, while the current model has
   `websites`/`passkeys` lists that serde fills from `#[serde(default)]`. serde ignores the unknown
   singular keys, so restoring such a file on Android drops the site. The desktop promotes them.
