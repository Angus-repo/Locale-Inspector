# 語系檢查 / Locale Inspector 1.0.0

這是獨立的 Android 診斷工具，顯示 AiDEX CN 1.15.1 `LanguageUtil.isOsChinese()` 所需的系統 Locale 欄位。

## 下載

- [下載 Locale Inspector 1.0.0 APK](releases/Locale-Inspector-1.0.0.apk?raw=true)
- [SHA-256 校驗碼](releases/SHA256SUMS.txt)

App 名稱為「語系檢查」，支援 Android 7.0 以上。

## 使用方式

1. 在 Android 7.0 或以上的手機安裝 `Locale-Inspector-1.0.0.apk`。
2. 開啟「語系檢查」，查看 `language`、`script`、`country` 與完整 language tag。
3. 按「執行 isOsChinese()」，畫面會顯示實際回傳的 `true` 或 `false`。
4. 按「複製結果」可取得上述欄位、判斷結果與判斷分支的文字報告。
5. 可透過「開啟系統語言設定」調整手機語言；回到 App 後會重新讀取，請再按檢查按鈕。

空字串會顯示為 `""（空字串）`。程式不會依國碼自行推算 Script。
介面使用繁體中文；讀取的是系統 Locale，而非介面文字的語言。
本工具不要求網路、藍牙、定位或儲存空間權限。

## 讀取與判斷

`src/tw/angus/localeinspector/LanguageUtil.java` 移植自已還原的 AiDEX CN 1.15.1 方法。
保留原本的系統 Locale 來源、備援、提前回傳順序及例外時回傳 false 的行為。
省略原 App 的日誌與不相關偏好設定；本 APK 不依賴或呼叫已安裝的 AiDEX App。

```java
Configuration configuration = Resources.getSystem().getConfiguration();
Locale locale = !configuration.getLocales().isEmpty()
    ? configuration.getLocales().get(0) : null;
if (locale == null) locale = Locale.getDefault();
```

- `language` 必須是 `zh`。
- `script` 有值時，只有 `Hans` 通過，且直接回傳，不再檢查 `country`。
- `script` 為空時，`country` 為 TW、HK、MO 會失敗；其他值或空字串通過。
- 原函式的 `"CN".equals(upperCase);` 未使用比較結果，因此不是只能接受 CN。
- 檢查按鈕實際呼叫 `LanguageUtil.isOsChinese()`。`LocaleRule` 僅用於產生欄位對應的分支說明。

檢查通過僅代表此項語言條件通過，不表示網路、裝置或其他配對檢查也會通過。

## 建置

- Java 17 執行環境；可使用 JDK 的 javac，或 Eclipse ECJ 3.33.0。
- Android SDK Platform 35。
- Android SDK Build Tools 35.0.0。
- 最低 Android API 24，target API 34。
- 僅使用 Android 平台元件，沒有第三方 App 執行依賴或原生函式庫。

有完整 JDK 與 Android SDK 時：

```bash
python3 build.py \
  --build-tools /absolute/path/android-sdk/build-tools/35.0.0 \
  --android-jar /absolute/path/android-sdk/platforms/android-35/android.jar
```

若在 Linux x86_64 環境只有 Java 執行環境，可用 `tools/bootstrap.py` 下載官方 SDK 檔案及 Maven Central 的 ECJ，再執行：

```bash
python3 tools/bootstrap.py
python3 build.py \
  --build-tools tools/toolchain/build-tools/android-15 \
  --android-jar tools/toolchain/platform/android-35/android.jar \
  --ecj tools/toolchain/ecj.jar
```

`build.py` 會編譯、執行測試、產生 DEX、封裝、對齊並簽署 APK。
首次建置時會在本機產生 `signing/locale-inspector-debug.keystore`，alias 為 `locale-inspector`、開發用密碼為 `android`。
儲存庫不包含任何簽章私鑰，`.gitignore` 會排除簽章與暫存檔。
自行建置的簽章與提供下載的 APK 不同，不能直接覆蓋安裝；請保留自己的簽章供後續更新。
此工具使用開發簽章，請勿將該簽章用於正式產品。

## 驗證範圍

- 21 個 JVM 測試案例通過：含 Hans/Hant 優先於國碼、國碼黑名單、空值、首筆系統 Locale、預設 Locale 備援及例外。
- 測試實際執行 APK 使用的 `LanguageUtil` 類別；Android 資源介面以測試替身提供資料。
- `tests/android` 的測試替身不會封裝進 APK。
- APK 簽章與 ZIP 對齊驗證通過。
- 尚未在 Android 手機或模擬器上執行，不能將 JVM 測試視為實機驗證。

驗證紀錄位於 `verification/`。

## 參考

- Android Resources.getSystem(): https://developer.android.com/reference/android/content/res/Resources#getSystem()
- Android Locale: https://developer.android.com/reference/java/util/Locale
- Android APK 簽署與驗證: https://developer.android.com/tools/apksigner
