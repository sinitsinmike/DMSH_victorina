# Android-оболочка «ДМШ Викторина»

Нативное приложение с Android WebView. При запуске открывает ровно сайт:

https://sinitsinmike.github.io/DMSH_victorina/

Внутри APK нет отдельной копии сайта: все изменения страниц появляются без обновления приложения. Для загрузки нужен интернет; приложение не обещает офлайн-доступ.

## Возможности

- Открытие сайта без адресной строки и регистрации.
- Звук по нажатию, остановка звука при сворачивании приложения.
- Навигация «Назад» внутри сайта; внешние HTTPS-ссылки открываются в браузере.
- Выбор MP3 и JSON системным диалогом Android без разрешения на всё хранилище.
- Экспорт JSON через системное окно сохранения (включая Blob-ссылки сайта).
- Черновики конструктора в хранилище WebView; они отделены от черновиков браузера.
- Повторная загрузка при ошибке сети; адаптация под системные панели и клавиатуру.

Android 8.0 (API 26) и новее, с актуальным Android System WebView/Chrome. При очистке данных или удалении приложения локальные черновики удаляются.

## Установка

[Скачать подписанный APK 1.0](https://github.com/sinitsinmike/DMSH_victorina/raw/refs/heads/main/downloads/dmsh-victorina-1.0.apk)

Скопируйте `dmsh-victorina-1.0.apk` на телефон и откройте его. Android может предложить разрешить установку для того приложения, из которого открыт APK. В Google Play приложение не опубликовано.

APK подписан отдельным ключом проекта. Ключ и настройки подписи хранятся только локально в `android/.signing/` и исключены из Git. **Сохраните эту папку в надёжной резервной копии:** без исходного ключа нельзя выпустить обновление поверх установленного приложения.

## Сборка

Требуются JDK 17–24 и Android SDK Platform 35.

```sh
cd android
./gradlew assembleDebug
./gradlew assembleRelease lintRelease
```

Задайте `ANDROID_HOME` или `sdk.dir` в игнорируемом `local.properties`. Если `.signing/signing.properties` отсутствует, release APK будет **неподписанным**; debug APK подписывается стандартным ключом разработчика. Для обновления распространяемой сборки используйте сохранённый release-ключ, увеличивайте versionCode и versionName в `app/build.gradle`.

Готовая подписанная сборка: `app/build/outputs/apk/release/app-release.apk`.

Проверка ограничения URL без эмулятора:

```sh
mkdir -p /tmp/dmsh-android-tests
javac -d /tmp/dmsh-android-tests app/src/main/java/ru/dmsh/victorina/SitePolicy.java tests/ru/dmsh/victorina/SitePolicyTest.java
java -cp /tmp/dmsh-android-tests ru.dmsh.victorina.SitePolicyTest
```
