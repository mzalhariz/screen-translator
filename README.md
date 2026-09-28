# Screen Translator (Android)

Translate the text of **any app** on your phone into your chosen language, on-device, with an
overlay that keeps up as you **scroll**. Built with an `AccessibilityService` + Google **ML Kit**
translation — no server, no API key, works offline after a one-time model download.

> Status: working MVP. See [Limitations](#limitations) before expecting miracles.

---

## What it does

- Reads the visible text of whatever app is in the foreground via the accessibility tree.
- Detects the source language (ML Kit Language ID) and translates it (ML Kit Translation).
- Paints the translations in a floating overlay positioned over the original text.
- **Lazy translate-on-scroll:** only visible nodes are translated; scrolling translates newly
  visible text, and already-seen text is served instantly from an LRU cache.
- A draggable **floating bubble** toggles the overlay; a **settings screen** picks the target
  language and downloads models.

## How it works

```
Foreground app
   │  AccessibilityEvents (window changed / content changed / scrolled)
   ▼
TranslateAccessibilityService ── EventDebouncer (~300ms) ── refresh()
   ▼
NodeCollector: walk rootInActiveWindow → visible text nodes + getBoundsInScreen()
   ├─ TranslationCache (LRU)      hits → rendered immediately
   ▼  misses
TranslationEngine: LanguageIdentification → ML Kit Translator (suspend/await)
   ▼
OverlayController → OverlayView (WindowManager, TYPE_ACCESSIBILITY_OVERLAY)
   draws a translated chip at each node's screen bounds
```

- The overlay uses `TYPE_ACCESSIBILITY_OVERLAY`, which an accessibility service may add **without**
  the `SYSTEM_ALERT_WINDOW` permission.
- Event bursts from scrolling are debounced so we refresh once the screen settles.
- Each refresh cancels the previous in-flight translation pass, so we only translate what's on screen.

## Project structure

```
app/src/main/java/com/example/screentranslator/
  ScreenTranslatorApp.kt            Application (global context)
  MainActivity.kt                   Compose settings host
  overlay/
    TranslateAccessibilityService.kt  service lifecycle + translate-on-scroll loop
    OverlayController.kt              WindowManager overlay window
    OverlayView.kt                    custom View that draws translated chips
    FloatingBubble.kt                 draggable toggle button
  screen/
    NodeCollector.kt                  harvest visible text nodes + bounds
    EventDebouncer.kt                 coalesce accessibility events
  translate/
    TranslationEngine.kt              ML Kit language-id + translate (suspend)
    ModelManager.kt                   model download / listing
    TranslationCache.kt               LRU caches
  ui/settings/
    SettingsScreen.kt / SettingsViewModel.kt
  data/SettingsRepository.kt          DataStore preferences
app/src/main/res/xml/accessibility_service_config.xml
```

## Build

### Option A — GitHub Actions (no local Android SDK needed)
This repo ships `.github/workflows/android.yml`. On every push it:
1. sets up JDK 17 + the Android SDK + Gradle,
2. runs `./gradlew :app:assembleDebug`,
3. uploads **`app-debug.apk`** as a workflow artifact.

Open your repo → **Actions** → latest run → **Artifacts** → download the APK → sideload it.

### Option B — Android Studio
Open the project folder in Android Studio (Giraffe+), let it sync, then **Run**. Studio provides
the SDK and Gradle automatically.

### Option C — command line (with an Android SDK installed)
```bash
./gradlew :app:assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

## Install & enable

1. Install the APK (enable "Install unknown apps" for your browser/file manager, or `adb install`).
2. Open **Screen Translator** → **Open Accessibility settings**.
3. Find **Screen Translator** under *Installed services* (or *Downloaded services*) and turn it on.
   Android will show a permission warning — this is normal for accessibility services.
4. Pick your **Translate into** language and tap **Download language model** (needs internet once).
5. Open any text app and tap the **floating bubble** to toggle the overlay. Scroll to see it keep up.

Or enable via adb:
```bash
adb shell settings put secure enabled_accessibility_services \
  com.example.screentranslator/.overlay.TranslateAccessibilityService
adb shell settings put secure accessibility_enabled 1
```

## Tech / versions

| Piece | Value |
|---|---|
| Language | Kotlin 2.0.21 |
| UI | Jetpack Compose (Material 3), BOM 2024.12.01 |
| Translation | `com.google.mlkit:translate:17.0.3` |
| Language ID | `com.google.mlkit:language-id:17.0.6` |
| Build | AGP 8.7.3, Gradle 8.9, JDK 17 |
| SDK | minSdk 24, compileSdk/targetSdk 35 |

## Limitations

- **Images / canvas text can't be translated.** Games, maps, photos, video subtitles, and some
  web `<canvas>` content expose no accessible text nodes. Translating those needs OCR + a camera/
  screenshot pipeline — a different architecture.
- **Overlay placement is approximate.** Chips are drawn at each node's bounds; on dense or
  custom-drawn layouts they can overlap or not perfectly cover the original.
- **FLAG_SECURE apps** (many banking/DRM apps) may block overlays.
- **Accessibility permission is sensitive.** Google Play restricts accessibility API use and
  requires a privacy policy + a clear justification; expect stricter review if you publish.
- First translation of a language downloads a model (tens of MB) and needs internet.

## Roadmap ideas

- Per-node "cover" mode that masks the original text background more cleanly.
- Merge fragmented nodes (a sentence split across many `TextView`s) before translating.
- Per-app allowlist / auto-enable.
- Optional cloud or LLM backend for higher-quality, context-aware whole-page translation.
- An emulator-based UI smoke test in CI.

## License

MIT — do whatever you like. (Add a `LICENSE` file if you publish.)
