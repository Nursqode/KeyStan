**English** · [Русский](README.ru.md)

# KeyStan

[![Release](https://img.shields.io/github/v/release/Nursqode/KeyStan?include_prereleases&sort=semver)](https://github.com/Nursqode/KeyStan/releases)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)
[![Platforms](https://img.shields.io/badge/platforms-Android%20%C2%B7%20Windows-informational)](#keystan-for-windows)

A private password manager: passwords, passkeys and cards, encrypted on your own device. No account,
no server, no telemetry — and a Windows companion that shares the same vault format, so a backup
written on one side opens on the other.

**KeyStan is a fork of [KeyGo](https://github.com/OffRange/KeyGo)** by Davis Alessandru, under its own
name and its own application id, with a few interface changes of its own. What was modified is recorded
in [NOTICE](NOTICE); the licence stays GPLv3, like the original.

## Screenshots

<div align="center">
    <img src="docs/assets/android/01_vaults.png" width="30%" alt="Item list"/>
    <img src="docs/assets/android/02_item.png" width="30%" alt="Item details"/>
    <img src="docs/assets/android/03_generator.png" width="30%" alt="Password generator"/>
</div>

## Features

* **Everything local.** Vaults are encrypted on the device with a key derived from your master
  password (Argon2id), and every item is sealed individually. There is no cloud to trust.
* **Passwords, passkeys and cards** in one place, with tags, notes, websites and pinning.
* **Two-factor codes** built in: RFC 6238 TOTP, right next to the login it belongs to.
* **Password health**, including a check against known breaches — the only feature that ever talks to
  the network, and only while you leave it on (it sends five characters of a hash, never a password).
* **Autofill** system-wide, so the vault is there where the passwords are needed.
* **Import and export** of backups as JSON or CSV.

## Download

| Platform | Where |
|---|---|
| Android | [Releases](https://github.com/Nursqode/KeyStan/releases) — the APK is attached to every release |
| Windows | [Releases](https://github.com/Nursqode/KeyStan/releases?q=desktop) — portable build and MSI installer |

Not on Google Play: the fork has its own application id (`de.nursqode.keystan`), and publishing there
is a separate step.

> **Upgrading from KeyGo?** On Android, KeyStan is a different application — it installs beside KeyGo
> rather than over it, and does not read KeyGo's data. Export a backup in KeyGo and import it in
> KeyStan; the format is the same.

## KeyStan for Windows

The `desktop/` directory holds a Compose Multiplatform application: the same Material 3 look, the same
vault, and the same sealed JSON backup format. A file exported on the PC restores on the phone, and a
phone backup opens on the PC.

* master password and a single vault file, rewritten atomically on every edit;
* vaults, logins and cards — usernames, passwords, websites, TOTP secrets, tags, notes, pinning;
* live one-time codes, a password generator with an entropy readout, search across everything;
* import and export, either replacing everything or merging into a chosen vault without duplicates;
* interface in English and Russian, following the system language by default.

How the compatibility is verified — against the Rust core's frozen vectors, re-sealing a real Android
backup into a byte-identical ciphertext — is described in [desktop/README.md](desktop/README.md).

## Build it yourself

Android (needs Rust, the Android SDK and the NDK; the Gradle build drives the Rust half itself):

```bash
./gradlew assemblePlayStoreDebug
```

Windows:

```bat
cd desktop
..\gradlew.bat -p . :app:createDistributable
```

## Licence

GNU General Public License v3.0 or later — see [LICENSE](LICENSE). Copyright in the original work
remains with the KeyGo authors; this fork adds its own changes under the same licence, and
[NOTICE](NOTICE) lists them.
