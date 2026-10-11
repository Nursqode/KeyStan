[English](README.md) · **Русский**

# KeyGo — ваш защищённый цифровой сейф

[![GitHub release](https://img.shields.io/github/release/OffRange/KeyGo?include_prereleases=&sort=semver)](https://github.com/OffRange/KeyGo/releases/)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)

KeyGo — это безопасный менеджер паролей для Android с открытым исходным кодом, который позволяет
хранить пароли, passkeys и данные банковских карт в зашифрованном виде полностью на вашем локальном
устройстве.

![KeyGo Banner](docs/assets/feature-graphic.png)

### В основе — современный и интуитивный дизайн

<div align="center">
    <img src="docs/assets/phone/01_hero.png" width="32%" alt="Vault List Screenshot"/>
    <img src="docs/assets/phone/02_password_health.png" width="32%" alt="Password Health Screenshot"/>
    <img src="docs/assets/phone/03_2fa.png" width="32%" alt="Two-Factor Authenticator Screenshot"/>
</div>

## Возможности

* **100% бесплатно и с открытым исходным кодом:** все функции доступны бесплатно.
* **Хранение только локально:** ваши данные никогда не покидают устройство.
* **Комплексное управление:** хранение паролей, passkeys и банковских карт.
* **Поддержка MFA:** встроенный аутентификатор для TOTP-токенов.
* **Продвинутая безопасность:** шифрование каждой записи по отраслевому стандарту и проверка
  надёжности паролей.
* **Бесшовная интеграция:** системный сервис автозаполнения для всех ваших учётных данных.

## Загрузка

Последнюю версию KeyGo можно скачать напрямую из Play Store или с GitHub:

|     Платформа     |                                                                                                          Статус                                                                                                           |
|:-----------------:|:-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------:|
| Google Play Store | [<img src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" alt="Download from Google Play" height="80">](https://play.google.com/store/apps/details?id=de.davis.passwordmanager) |
|      GitHub       |                                    [![GitHub tag](https://img.shields.io/github/release/OffRange/KeyGo?include_prereleases=&sort=semver)](https://github.com/OffRange/KeyGo/releases/)                                    |

## KeyStan Desktop (Windows)

KeyStan работает и на ПК. [`desktop/`](desktop/) — это приложение на Compose Multiplatform, которое
сохраняет внешний вид Material 3 с телефона и — что важнее — его **формат файлов**: бэкап
(резервная копия), экспортированный на ПК, восстанавливается на телефоне, а бэкап, экспортированный
на телефоне, открывается на ПК. Никакого этапа конвертации, никакого сервера, никакой учётной
записи — ничего не покидает машину.

* **Настоящий локальный сейф** на десктопе, запечатанный мастер-паролем (Argon2id → AES-256-GCM-SIV).
* **Сейфы, логины и карты** с теми же полями, что и на телефоне: имена пользователей, пароли, сайты,
  TOTP-секреты, теги, заметки, закрепление и те же значки сейфов.
* **Живые одноразовые коды** (RFC 6238; SHA-1/256/512, с учётом digits и period из URI `otpauth://`).
* **Генератор паролей** с оценкой энтропии.
* **Импорт и экспорт**: восстановить всё из бэкапа или влить бэкап в выбранный сейф без дублирования
  записей.
* **Поиск** по названиям, именам пользователей, заметкам, тегам и сайтам.
* **Интерфейс на английском и русском**, по умолчанию язык системы.

Соберите упакованное приложение (портативная папка с `KeyStan.exe`, ~160 МБ, JVM на целевой машине
не требуется):

```bat
cd desktop
..\gradlew.bat -p . :app:createDistributable
```

Результат — `desktop\app\build\compose\binaries\main\app\KeyStan\`. Что оно делает, как хранит сейф,
как проверяется совместимость с приложением для Android и чего оно намеренно пока не умеет — всё это
в [desktop/README.md](desktop/README.md).

## Соберите сами

### Требования

Вам потребуется установить **Rust**. Rust используется для криптографических функций и других
замечательных возможностей — например, создания бэкапов.

Если вы используете Linux или macOS, просто выполните в терминале:

```bash
curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh
```

*(Для Windows скачайте установщик с [официального сайта Rust](https://rustup.rs/).)*

### Соберите APK

Установив Rust, вы готовы собрать приложение — для этого выполните следующую команду в корне проекта:

```bash
./gradlew assembleFdroidRelease
```

Собранный APK появится в `app/build/outputs/apk/`.

### Установите на устройство

1. Подключите телефон к компьютеру кабелем USB.
2. [Включите отладку по USB на устройстве](https://developer.android.com/studio/debug/dev-options#Enable-debugging).
3. Установите APK через ADB, выполнив:

```bash
adb install <path-to-apk-file>
```

## Лицензия

Лицензировано под **GNU General Public License v3.0**. Это означает, что KeyGo — свободное
программное обеспечение: вы можете распространять и/или изменять его на условиях GPLv3. Полные
подробности — в файле [LICENSE](https://www.google.com/search?q=LICENSE).
