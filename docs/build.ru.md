# Сборка MLib

[Документация](README.ru.md) · [English](build.md) · **Русский**

MLib — отдельное приложение. Оно устанавливается рядом со сторонним Spotygram и не обновляет его, не использует его каталог, сессию Telegram или ключи подписи.

## Каталог состояния сборки

Все результаты сборки, исходники TDLib/OpenSSL, кэши, учётные данные и закрытые ключи хранятся вне репозитория в `MLIB_STATE`.

По умолчанию: `~/.local/state/mlib`. Для Android-скрипта также учитывается `XDG_STATE_HOME`. Переменная `SPOTYGRAM_STATE` не используется и не должна указывать на каталог Spotygram.

```sh
export ANDROID_HOME=/path/to/android-sdk
export MLIB_STATE="$HOME/.local/state/mlib"
```

Создайте `$MLIB_STATE/telegram.env` с API credentials приложения из [my.telegram.org](https://my.telegram.org):

```dotenv
TELEGRAM_API_ID=YOUR_NUMERIC_API_ID
TELEGRAM_API_HASH=YOUR_API_HASH
```

Это ключи приложения Telegram, а не учётные данные пользователя и не токен бота. Они встраиваются в APK. Не помещайте сюда сессии или токены.

## Android debug APK

Требуются Linux, JDK 21, Android SDK platform 37, build-tools 36.0.0, NDK `28.2.13676358`, CMake, Ninja, gperf, Perl, C++ compiler, Git и Make.

```sh
bash tools/build.sh debug
```

Первая сборка скачивает закреплённые версии TDLib и OpenSSL и компилирует JNI-библиотеки для ARM64 и x86-64. APK находятся в:

```text
$MLIB_STATE/build/app/outputs/apk/debug/
```

Идентификатор установленного debug-приложения — `pt.ginjeira.mlib.dev`; release — `pt.ginjeira.mlib`. Оба отличаются от Spotygram (`app.spotygram`).

## Подпись Android release

Для MLib нужен отдельный ключ. Не используйте ключ Spotygram.

Создайте ключ один раз и сохраните резервную копию в безопасном месте:

```sh
keytool -genkeypair -keystore "$MLIB_STATE/mlib-release.jks" \\
  -alias mlib -keyalg RSA -keysize 3072 -validity 10000
```

В приватном файле `$MLIB_STATE/signing.properties`:

```properties
storePassword=YOUR_KEYSTORE_PASSWORD
keyAlias=mlib
keyPassword=YOUR_KEY_PASSWORD
```

Ограничьте права доступа к обоим файлам. Каждый будущий APK MLib должен быть подписан тем же ключом. Сборка:

```sh
bash tools/build.sh release
```

Проверяйте APK командой Android SDK `apksigner verify --verbose`. Android `versionCode` вычисляется из MAJOR.MINOR.PATCH и должен возрастать при каждом выпуске.

Workflow `.github/workflows/android-release.yml` подготовлен для ручного выпуска по существующему тегу `vMAJOR.MINOR.PATCH`. Он требует секреты GitHub Actions: `TELEGRAM_API_ID`, `TELEGRAM_API_HASH`, `MLIB_ANDROID_KEYSTORE_BASE64`, `MLIB_ANDROID_KEYSTORE_PASSWORD`, `MLIB_ANDROID_KEY_ALIAS` и `MLIB_ANDROID_KEY_PASSWORD`. Для значения keystore используйте base64, например `base64 -w0 mlib-release.jks`. Workflow проверяет package ID, версию, ABI и подпись, затем загружает artifact; опция `publish_draft=true` создаёт черновик GitHub Release. Запустить workflow вручную можно только после появления его файла в default branch репозитория.

## Desktop image для разработки

Нужны JDK 21, Python 3.12+ и OpenSSL 3. Сборку выполняйте на целевой ОС:

```sh
python3 tools/build-desktop.py --image-only
```

В Windows используйте `python`. Образ приложения находится под:

```text
$MLIB_STATE/build/desktop/distributions/main/app/
```

Он использует отдельные каталоги данных MLib и идентификатор MLib в системном хранилище ключей.

## Desktop release

**Создание установщиков и публикация desktop release отключены.** Унаследованные сценарии упаковки, манифест подписи и updater ещё содержат предположения Spotygram. До публикации необходимо согласованно перенести идентификаторы установщика и обновления, создать новую пару ключей Ed25519 MLib и встроить соответствующий публичный ключ. Закрытый ключ Spotygram недоступен и использоваться не должен.

Не запускайте старый workflow Spotygram и не публикуйте его artifacts от имени MLib. Desktop image-only предназначен только для разработки и проверки.
