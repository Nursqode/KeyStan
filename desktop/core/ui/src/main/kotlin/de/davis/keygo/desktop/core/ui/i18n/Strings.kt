/*
 * KeyStan Desktop - a Windows companion for the KeyGo Android password manager.
 * Copyright (C) 2026 Nursqode
 *
 * Part of KeyStan (https://github.com/Nursqode/KeyStan), a fork of KeyGo
 * (https://github.com/OffRange/KeyGo). Licensed under the GNU General Public License v3.0 or
 * later, like the project it belongs to; see LICENSE.
 */

package de.davis.keygo.desktop.core.ui.i18n

import de.davis.keygo.desktop.core.vault.data.AppLanguage
import de.davis.keygo.desktop.core.vault.domain.VaultError

/**
 * Every piece of text the window shows.
 *
 * An interface rather than a resource file, so that a language cannot silently fall behind: a new
 * string is a compile error in every language until it is translated. The translations live in this
 * module because they are presentation, and the model below them - [VaultError], for one - stays
 * free of prose.
 */
interface Strings {

    // region window and status

    val busyWorking: String
    val dismiss: String

    // endregion

    // region sidebar

    val tagline: String
    val vaultsSection: String
    val newVault: String
    val generator: String
    val importBackup: String
    val exportBackup: String
    val settings: String
    val lock: String
    val vaultActions: String
    val rename: String
    val delete: String
    fun vaultCounts(logins: Int, cards: Int): String

    // endregion

    // region lock screen

    val createYourVault: String
    val unlockTitle: String
    val createVaultExplanation: String
    fun unlockExplanation(fileName: String): String
    val masterPassphrase: String
    val repeatPassphrase: String
    val passphrasesDoNotMatch: String
    fun atLeastCharacters(min: Int): String
    val createVault: String
    val unlock: String
    val openAnotherVaultFile: String
    val openVaultFileDialog: String

    // endregion

    // region vault screen

    val vaultFallback: String
    fun itemCount(count: Int): String
    val login: String
    val card: String
    val searchThisVault: String
    val nothingHereYet: String
    val noMatches: String
    val nothingHereHint: String
    fun noMatchesHint(query: String): String
    val untitled: String
    val pin: String
    val unpin: String
    val noCode: String
    fun changesIn(seconds: Int): String

    /** The bare countdown shown beside a code in a list row, where there is no room for a sentence. */
    fun secondsShort(seconds: Int): String
    val nothingSelected: String
    val pickAnItem: String
    val notes: String
    val edit: String
    val move: String
    fun moveTo(vault: String): String
    val showSecrets: String
    val hideSecrets: String
    val username: String
    val password: String
    val oneTimeCode: String
    val website: String
    val cardholder: String
    val number: String
    val cardNumber: String
    val expires: String
    val cvv: String
    fun copyField(label: String): String
    fun copied(label: String): String

    // endregion

    // region dialogs

    val renameVault: String
    val deleteItemTitle: String
    val deleteItemText: String
    fun deleteVaultTitle(name: String): String
    val deleteVaultText: String
    val deleteVault: String
    val cancel: String
    val save: String
    val name: String
    val icon: String
    val changePassphraseTitle: String
    val changePassphraseText: String
    val newPassphrase: String
    val change: String
    val newItem: String
    val editItem: String
    val titleField: String
    val websitesLabel: String
    val totpSecretLabel: String
    val month: String
    val year: String
    val tagsLabel: String
    val pinned: String
    val generatePassword: String

    // endregion

    // region generator

    val passwordGenerator: String
    val selectCharacterSet: String
    val copyPassword: String
    val generateAnother: String
    fun entropyBits(bits: Int, strength: String): String
    val strengthWeak: String
    val strengthFair: String
    val strengthStrong: String
    val strengthExcellent: String
    fun lengthLabel(length: Int): String
    val lowercaseLetters: String
    val uppercaseLetters: String
    val digits: String
    val symbols: String
    val avoidAmbiguous: String
    val avoidProblematic: String
    val resetStrongDefault: String
    val passwordCopied: String
    val generatorFootnote: String

    // endregion

    // region settings

    val appearance: String
    val followSystem: String
    val light: String
    val dark: String
    val security: String
    fun lockAfterMinutes(minutes: Int): String
    val autoLockOff: String
    val autoLockHint: String
    val concealSecrets: String
    val vaultFile: String
    val openAnotherFile: String
    val switchingFilesHint: String
    val about: String
    fun aboutVersion(version: String): String
    val aboutAuthor: String
    val aboutCredits: String
    val aboutFormat: String
    val language: String
    val languageSystem: String
    val languageEnglish: String
    val languageRussian: String

    // endregion

    // region import and export

    val importTitle: String
    val exportTitle: String
    val importExplanation: String
    val exportExplanation: String
    val backupFile: String
    val choose: String
    val chooseBackupDialog: String
    val saveBackupDialog: String
    val importPassphraseLabel: String
    val exportPassphraseLabel: String
    val howApplied: String
    val mergeOption: String
    val replaceOption: String
    fun intoVault(name: String): String
    val restoreWarning: String
    val whatContains: String
    val everyVault: String
    val onlySelectedVault: String
    val importAction: String
    val exportAction: String
    val transferFootnote: String

    // endregion

    // region progress, status and errors

    val busyCreating: String
    val busyUnlocking: String
    val busyExporting: String
    val busyImporting: String
    val busyResealing: String
    val statusVaultLocked: String
    val statusItemDeleted: String
    val statusItemMoved: String
    val statusPassphraseChanged: String
    val statusVaultMustRemain: String
    fun statusExported(fileName: String): String
    fun statusImported(logins: Int, cards: Int): String
    val errorNoVaultFile: String
    val errorWrongPassphrase: String
    fun errorWeakPassphrase(min: Int): String
    val errorNeedsAccountKey: String
    val errorPassphraseMismatch: String
    fun errorUnreadable(reason: String): String
    fun errorIo(reason: String): String

    // endregion
}

/**
 * The text for a vault failure.
 *
 * A failed AEAD tag is the only failure that means "wrong passphrase"; the rest describe the file,
 * and saying otherwise would send the user hunting for a password that was never going to work.
 */
fun Strings.textFor(error: VaultError): String = when (error) {
    VaultError.NoVaultFile -> errorNoVaultFile
    VaultError.WrongPassphrase -> errorWrongPassphrase
    VaultError.WeakPassphrase -> errorWeakPassphrase(PassphraseMinimum)
    VaultError.NeedsAccountKey -> errorNeedsAccountKey
    VaultError.PassphraseMismatch -> errorPassphraseMismatch
    is VaultError.Unreadable -> errorUnreadable(error.reason)
    is VaultError.Io -> errorIo(error.reason)
}

/** Kept here so a caller does not have to know where the policy lives to write a message about it. */
private val PassphraseMinimum = de.davis.keygo.desktop.core.vault.domain.PassphrasePolicy.MIN_LENGTH

/** The catalog for a language choice. */
fun stringsFor(language: AppLanguage): Strings = when (language) {
    AppLanguage.ENGLISH -> EnglishStrings
    AppLanguage.RUSSIAN -> RussianStrings
    // Following the system means following the JVM's locale, which on Windows is the user's
    // display language - the closest thing to "what the rest of the desktop speaks".
    AppLanguage.SYSTEM -> if (java.util.Locale.getDefault().language == "ru") RussianStrings else EnglishStrings
}

/** English, and the fallback for any language without a catalog. */
object EnglishStrings : Strings {
    override val busyWorking = "Working"
    override val dismiss = "Dismiss"

    override val tagline = "Password manager · by Nursqode"
    override val vaultsSection = "VAULTS"
    override val newVault = "New vault"
    override val generator = "Generator"
    override val importBackup = "Import a backup"
    override val exportBackup = "Export a backup"
    override val settings = "Settings"
    override val lock = "Lock"
    override val vaultActions = "Vault actions"
    override val rename = "Rename"
    override val delete = "Delete"
    override fun vaultCounts(logins: Int, cards: Int) =
        "$logins logins · $cards cards"

    override val createYourVault = "Create your vault"
    override val unlockTitle = "Unlock KeyStan"
    override val createVaultExplanation =
        "Your vault is sealed with this passphrase. It is never stored, and there is no " +
            "recovery - write it down."
    override fun unlockExplanation(fileName: String) = "Enter the master passphrase for $fileName."
    override val masterPassphrase = "Master passphrase"
    override val repeatPassphrase = "Repeat passphrase"
    override val passphrasesDoNotMatch = "The two passphrases do not match"
    override fun atLeastCharacters(min: Int) = "Use at least $min characters"
    override val createVault = "Create vault"
    override val unlock = "Unlock"
    override val openAnotherVaultFile = "Open a different vault file"
    override val openVaultFileDialog = "Open a vault file"

    override val vaultFallback = "Vault"
    override fun itemCount(count: Int) = "$count items"
    override val login = "Login"
    override val card = "Card"
    override val searchThisVault = "Search this vault"
    override val nothingHereYet = "Nothing here yet"
    override val noMatches = "No matches"
    override val nothingHereHint = "Add a login or a card, or import a backup from your phone."
    override fun noMatchesHint(query: String) = "Nothing in this vault matches \"$query\"."
    override val untitled = "Untitled"
    override val pin = "Pin"
    override val unpin = "Unpin"
    override val noCode = "no code"
    override fun changesIn(seconds: Int) = "Changes in ${seconds}s"
    override fun secondsShort(seconds: Int) = "${seconds}s"
    override val nothingSelected = "Nothing selected"
    override val pickAnItem = "Pick an item on the left, or add a new one."
    override val notes = "Notes"
    override val edit = "Edit"
    override val move = "Move"
    override fun moveTo(vault: String) = "Move to $vault"
    override val showSecrets = "Show secrets"
    override val hideSecrets = "Hide secrets"
    override val username = "Username"
    override val password = "Password"
    override val oneTimeCode = "One-time code"
    override val website = "Website"
    override val cardholder = "Cardholder"
    override val number = "Number"
    override val cardNumber = "Card number"
    override val expires = "Expires"
    override val cvv = "CVV"
    override fun copyField(label: String) = "Copy $label"
    override fun copied(label: String) = "$label copied"

    override val renameVault = "Rename vault"
    override val deleteItemTitle = "Delete this item?"
    override val deleteItemText =
        "The item is removed from the vault and the file is rewritten. This cannot be undone."
    override fun deleteVaultTitle(name: String) = "Delete \"$name\"?"
    override val deleteVaultText = "Every login and card in this vault is deleted with it."
    override val deleteVault = "Delete vault"
    override val cancel = "Cancel"
    override val save = "Save"
    override val name = "Name"
    override val icon = "Icon"
    override val changePassphraseTitle = "Change the master passphrase"
    override val changePassphraseText =
        "The vault file is re-sealed with the new passphrase. There is no recovery if it is lost."
    override val newPassphrase = "New passphrase"
    override val change = "Change"
    override val newItem = "New item"
    override val editItem = "Edit item"
    override val titleField = "Title"
    override val websitesLabel = "Websites (one per line)"
    override val totpSecretLabel = "TOTP secret or otpauth:// URI"
    override val month = "Month"
    override val year = "Year"
    override val tagsLabel = "Tags (comma separated)"
    override val pinned = "Pinned"
    override val generatePassword = "Generate a password"

    override val passwordGenerator = "Password generator"
    override val selectCharacterSet = "Select at least one character set"
    override val copyPassword = "Copy the password"
    override val generateAnother = "Generate another"
    override fun entropyBits(bits: Int, strength: String) = "$bits bits of entropy · $strength"
    override val strengthWeak = "weak"
    override val strengthFair = "fair"
    override val strengthStrong = "strong"
    override val strengthExcellent = "excellent"
    override fun lengthLabel(length: Int) = "Length: $length"
    override val lowercaseLetters = "Lowercase letters"
    override val uppercaseLetters = "Uppercase letters"
    override val digits = "Digits"
    override val symbols = "Symbols"
    override val avoidAmbiguous = "Avoid look-alike characters (0, O, 1, l, I)"
    override val avoidProblematic = "Avoid characters some sites reject"
    override val resetStrongDefault = "Reset to a strong default"
    override val passwordCopied = "Password copied"
    override val generatorFootnote =
        "Passwords are generated here with the JDK's secure random source. Nothing about them is " +
            "stored unless you save one into an item."

    override val appearance = "Appearance"
    override val followSystem = "Follow the system"
    override val light = "Light"
    override val dark = "Dark"
    override val security = "Security"
    override fun lockAfterMinutes(minutes: Int) = "Lock after $minutes minutes without input"
    override val autoLockOff = "Automatic locking is off"
    override val autoLockHint = "Idle time before the vault is sealed again"
    override val concealSecrets = "Hide passwords and card numbers until asked"
    override val vaultFile = "Vault file"
    override val openAnotherFile = "Open another file"
    override val switchingFilesHint =
        "Switching files locks the current vault first; the file on disk is left exactly as it was."
    override val about = "About"
    override fun aboutVersion(version: String) = "KeyStan Desktop $version · vault format v1"
    override val aboutAuthor = "Author: Nursqode"
    override val aboutCredits =
        "KeyStan is a fork of KeyGo by Davis Alessandru (OffRange), and the two share their vault " +
            "format. Licensed under the GNU GPL v3.0 or later."
    override val aboutFormat =
        "The vault file is the same sealed JSON backup the KeyGo Android app writes: Argon2id " +
            "(64 MiB, 3 passes, 4 lanes) derives a key that AES-256-GCM-SIV uses to seal the " +
            "payload. A file exported here restores on the phone, and a backup exported there " +
            "imports here."
    override val language = "Language"
    override val languageSystem = "Follow the system"
    override val languageEnglish = "English"
    override val languageRussian = "Русский"

    override val importTitle = "Import a backup"
    override val exportTitle = "Export a backup"
    override val importExplanation =
        "A backup file from KeyGo on Android - or from another KeyStan install - opens here with " +
            "the passphrase it was sealed with."
    override val exportExplanation =
        "The file written here is the same sealed JSON backup the Android app restores from. Give " +
            "it its own passphrase rather than the master one if it is going to travel."
    override val backupFile = "Backup file"
    override val choose = "Choose…"
    override val chooseBackupDialog = "Choose a backup"
    override val saveBackupDialog = "Save the backup"
    override val importPassphraseLabel = "Passphrase of that file"
    override val exportPassphraseLabel = "Passphrase for the export"
    override val howApplied = "How should it be applied?"
    override val mergeOption = "Merge into a vault in this app"
    override val replaceOption = "Replace everything in this app"
    override fun intoVault(name: String) = "Into \"$name\""
    override val restoreWarning =
        "Restoring replaces the local vault with the file's contents. Anything saved here since " +
            "that file was written is gone."
    override val whatContains = "What should it contain?"
    override val everyVault = "Every vault"
    override val onlySelectedVault = "Only the selected vault"
    override val importAction = "Import"
    override val exportAction = "Export"
    override val transferFootnote =
        "The file is sealed with Argon2id and AES-256-GCM-SIV, the format KeyGo v1 uses. Nothing " +
            "leaves this machine."

    override val busyCreating = "Creating your vault"
    override val busyUnlocking = "Unlocking"
    override val busyExporting = "Exporting"
    override val busyImporting = "Importing"
    override val busyResealing = "Re-sealing the vault"
    override val statusVaultLocked = "Vault locked"
    override val statusItemDeleted = "Item deleted"
    override val statusItemMoved = "Item moved"
    override val statusPassphraseChanged = "Master passphrase changed"
    override val statusVaultMustRemain = "A vault must remain"
    override fun statusExported(fileName: String) = "Exported to $fileName"
    override fun statusImported(logins: Int, cards: Int) = "Imported $logins logins and $cards cards"
    override val errorNoVaultFile = "No vault file at that path"
    override val errorWrongPassphrase = "That passphrase does not open this vault"
    override fun errorWeakPassphrase(min: Int) = "Use at least $min characters"
    override val errorNeedsAccountKey =
        "This file is sealed with the Android account key; export it from the phone with a " +
            "passphrase instead"
    override val errorPassphraseMismatch = "The two passphrases do not match"
    override fun errorUnreadable(reason: String) = "Cannot read this file: $reason"
    override fun errorIo(reason: String) = "File error: $reason"
}

/**
 * Russian.
 *
 * Counted nouns go through [plural], because "1 логин", "2 логина" and "5 логинов" are one form in
 * English and three in Russian; a bare "$n logins" would read as broken Russian at every count that
 * matters.
 */
object RussianStrings : Strings {
    override val busyWorking = "Выполняется"
    override val dismiss = "Скрыть"

    override val tagline = "Менеджер паролей · Nursqode"
    override val vaultsSection = "СЕЙФЫ"
    override val newVault = "Новый сейф"
    override val generator = "Генератор"
    override val importBackup = "Импорт бэкапа"
    override val exportBackup = "Экспорт бэкапа"
    override val settings = "Настройки"
    override val lock = "Заблокировать"
    override val vaultActions = "Действия с сейфом"
    override val rename = "Переименовать"
    override val delete = "Удалить"
    override fun vaultCounts(logins: Int, cards: Int) =
        "${plural(logins, "логин", "логина", "логинов")} · ${plural(cards, "карта", "карты", "карт")}"

    override val createYourVault = "Создайте свой сейф"
    override val unlockTitle = "Разблокировать KeyStan"
    override val createVaultExplanation =
        "Сейф шифруется этим паролем. Он нигде не хранится, и восстановить его невозможно - " +
            "запишите пароль."
    override fun unlockExplanation(fileName: String) = "Введите мастер-пароль для файла $fileName."
    override val masterPassphrase = "Мастер-пароль"
    override val repeatPassphrase = "Повторите пароль"
    override val passphrasesDoNotMatch = "Пароли не совпадают"
    override fun atLeastCharacters(min: Int) = "Используйте не менее $min символов"
    override val createVault = "Создать сейф"
    override val unlock = "Разблокировать"
    override val openAnotherVaultFile = "Открыть другой файл сейфа"
    override val openVaultFileDialog = "Открыть файл сейфа"

    override val vaultFallback = "Сейф"
    override fun itemCount(count: Int) = plural(count, "запись", "записи", "записей")
    override val login = "Логин"
    override val card = "Карта"
    override val searchThisVault = "Поиск в этом сейфе"
    override val nothingHereYet = "Здесь пока пусто"
    override val noMatches = "Ничего не найдено"
    override val nothingHereHint =
        "Добавьте логин или карту либо импортируйте бэкап с телефона."
    override fun noMatchesHint(query: String) = "В этом сейфе нет ничего по запросу «$query»."
    override val untitled = "Без названия"
    override val pin = "Закрепить"
    override val unpin = "Открепить"
    override val noCode = "нет кода"
    override fun changesIn(seconds: Int) = "Сменится через $seconds с"
    override fun secondsShort(seconds: Int) = "$seconds с"
    override val nothingSelected = "Ничего не выбрано"
    override val pickAnItem = "Выберите запись слева или добавьте новую."
    override val notes = "Заметки"
    override val edit = "Изменить"
    override val move = "Переместить"
    override fun moveTo(vault: String) = "Переместить в «$vault»"
    override val showSecrets = "Показать секреты"
    override val hideSecrets = "Скрыть секреты"
    override val username = "Имя пользователя"
    override val password = "Пароль"
    override val oneTimeCode = "Одноразовый код"
    override val website = "Сайт"
    override val cardholder = "Держатель карты"
    override val number = "Номер"
    override val cardNumber = "Номер карты"
    override val expires = "Срок действия"
    override val cvv = "CVV"
    override fun copyField(label: String) = "Скопировать: $label"
    override fun copied(label: String) = "$label скопировано"

    override val renameVault = "Переименовать сейф"
    override val deleteItemTitle = "Удалить эту запись?"
    override val deleteItemText =
        "Запись удаляется из сейфа, и файл перезаписывается. Отменить это нельзя."
    override fun deleteVaultTitle(name: String) = "Удалить «$name»?"
    override val deleteVaultText = "Все логины и карты этого сейфа будут удалены вместе с ним."
    override val deleteVault = "Удалить сейф"
    override val cancel = "Отмена"
    override val save = "Сохранить"
    override val name = "Название"
    override val icon = "Значок"
    override val changePassphraseTitle = "Смена мастер-пароля"
    override val changePassphraseText =
        "Файл сейфа будет заново зашифрован новым паролем. Восстановить его невозможно."
    override val newPassphrase = "Новый пароль"
    override val change = "Сменить"
    override val newItem = "Новая запись"
    override val editItem = "Изменение записи"
    override val titleField = "Название"
    override val websitesLabel = "Сайты (по одному в строке)"
    override val totpSecretLabel = "Секрет TOTP или ссылка otpauth://"
    override val month = "Месяц"
    override val year = "Год"
    override val tagsLabel = "Метки (через запятую)"
    override val pinned = "Закреплено"
    override val generatePassword = "Сгенерировать пароль"

    override val passwordGenerator = "Генератор паролей"
    override val selectCharacterSet = "Выберите хотя бы один набор символов"
    override val copyPassword = "Скопировать пароль"
    override val generateAnother = "Сгенерировать ещё"
    override fun entropyBits(bits: Int, strength: String) = "$bits бит энтропии · $strength"
    override val strengthWeak = "слабый"
    override val strengthFair = "средний"
    override val strengthStrong = "надёжный"
    override val strengthExcellent = "отличный"
    override fun lengthLabel(length: Int) = "Длина: $length"
    override val lowercaseLetters = "Строчные буквы"
    override val uppercaseLetters = "Прописные буквы"
    override val digits = "Цифры"
    override val symbols = "Символы"
    override val avoidAmbiguous = "Без похожих символов (0, O, 1, l, I)"
    override val avoidProblematic = "Без символов, которые не принимают некоторые сайты"
    override val resetStrongDefault = "Вернуть надёжные значения"
    override val passwordCopied = "Пароль скопирован"
    override val generatorFootnote =
        "Пароли генерируются здесь защищённым генератором JDK. Ничего из них не сохраняется, " +
            "пока вы не сохраните пароль в записи."

    override val appearance = "Оформление"
    override val followSystem = "Как в системе"
    override val light = "Светлое"
    override val dark = "Тёмное"
    override val security = "Безопасность"
    override fun lockAfterMinutes(minutes: Int) =
        "Блокировать после $minutes мин без действий"
    override val autoLockOff = "Автоблокировка отключена"
    override val autoLockHint = "Время простоя до повторного шифрования сейфа"
    override val concealSecrets = "Скрывать пароли и номера карт до запроса"
    override val vaultFile = "Файл сейфа"
    override val openAnotherFile = "Открыть другой файл"
    override val switchingFilesHint =
        "Перед сменой файла текущий сейф блокируется; файл на диске остаётся без изменений."
    override val about = "О приложении"
    override fun aboutVersion(version: String) = "KeyStan Desktop $version · формат сейфа v1"
    override val aboutAuthor = "Автор: Nursqode"
    override val aboutCredits =
        "KeyStan - форк KeyGo (автор Davis Alessandru, OffRange); у них общий формат сейфа. " +
            "Лицензия - GNU GPL v3.0 или новее."
    override val aboutFormat =
        "Файл сейфа - тот же зашифрованный JSON-бэкап, который создаёт Android-приложение KeyGo: " +
            "Argon2id (64 МиБ, 3 прохода, 4 потока) выводит ключ, которым AES-256-GCM-SIV " +
            "шифрует данные. Файл, экспортированный здесь, восстанавливается на телефоне, а " +
            "бэкап с телефона открывается здесь."
    override val language = "Язык"
    override val languageSystem = "Как в системе"
    override val languageEnglish = "English"
    override val languageRussian = "Русский"

    override val importTitle = "Импорт бэкапа"
    override val exportTitle = "Экспорт бэкапа"
    override val importExplanation =
        "Файл бэкапа из KeyGo на Android - или из другой установки KeyStan - открывается здесь " +
            "тем паролем, которым был зашифрован."
    override val exportExplanation =
        "Записанный здесь файл - тот же зашифрованный JSON-бэкап, из которого восстанавливается " +
            "Android-приложение. Если файл будет куда-то передаваться, задайте ему отдельный " +
            "пароль, а не мастер-пароль."
    override val backupFile = "Файл бэкапа"
    override val choose = "Выбрать…"
    override val chooseBackupDialog = "Выберите бэкап"
    override val saveBackupDialog = "Сохранить бэкап"
    override val importPassphraseLabel = "Пароль этого файла"
    override val exportPassphraseLabel = "Пароль для экспорта"
    override val howApplied = "Как применить?"
    override val mergeOption = "Добавить в сейф в этом приложении"
    override val replaceOption = "Заменить всё в этом приложении"
    override fun intoVault(name: String) = "В «$name»"
    override val restoreWarning =
        "Восстановление заменяет локальный сейф содержимым файла. Всё, что сохранили здесь " +
            "после создания этого файла, будет потеряно."
    override val whatContains = "Что должно попасть в файл?"
    override val everyVault = "Все сейфы"
    override val onlySelectedVault = "Только выбранный сейф"
    override val importAction = "Импортировать"
    override val exportAction = "Экспортировать"
    override val transferFootnote =
        "Файл шифруется Argon2id и AES-256-GCM-SIV - форматом KeyGo v1. Данные не покидают " +
            "этот компьютер."

    override val busyCreating = "Создаём сейф"
    override val busyUnlocking = "Разблокировка"
    override val busyExporting = "Экспорт"
    override val busyImporting = "Импорт"
    override val busyResealing = "Перешифрование сейфа"
    override val statusVaultLocked = "Сейф заблокирован"
    override val statusItemDeleted = "Запись удалена"
    override val statusItemMoved = "Запись перемещена"
    override val statusPassphraseChanged = "Мастер-пароль изменён"
    override val statusVaultMustRemain = "Должен остаться хотя бы один сейф"
    override fun statusExported(fileName: String) = "Экспортировано в $fileName"
    override fun statusImported(logins: Int, cards: Int) =
        "Импортировано: ${plural(logins, "логин", "логина", "логинов")}, " +
            plural(cards, "карта", "карты", "карт")
    override val errorNoVaultFile = "По этому пути нет файла сейфа"
    override val errorWrongPassphrase = "Этот пароль не подходит к сейфу"
    override fun errorWeakPassphrase(min: Int) = "Используйте не менее $min символов"
    override val errorNeedsAccountKey =
        "Файл зашифрован ключом аккаунта Android; экспортируйте его с телефона с паролем"
    override val errorPassphraseMismatch = "Пароли не совпадают"
    override fun errorUnreadable(reason: String) = "Не удалось прочитать файл: $reason"
    override fun errorIo(reason: String) = "Ошибка файла: $reason"

    /**
     * Russian plural form for [count].
     *
     * The one/few/many split is why counts cannot be interpolated into a single string: 21 логин,
     * 22 логина, 25 логинов - but also 11 логинов, which is why the teens are checked first.
     */
    private fun plural(count: Int, one: String, few: String, many: String): String {
        val absolute = kotlin.math.abs(count)
        val form = when {
            absolute % 100 in 11..14 -> many
            absolute % 10 == 1 -> one
            absolute % 10 in 2..4 -> few
            else -> many
        }
        return "$count $form"
    }
}
