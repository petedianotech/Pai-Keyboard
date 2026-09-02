# Pai Keyboard ⌨️

**A fast, beautiful, power-efficient Android custom keyboard** built with modern Kotlin.

> Designed to feel smoother and more professional than stock keyboards while staying lightweight and private.

![Android](https://img.shields.io/badge/Android-24%2B-green)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-purple)
![License](https://img.shields.io/badge/License-MIT-blue)

## ✨ Features

- **Professional modern design** – clean rounded keys, proper spacing, Material-inspired look
- **Always-on number row** – type numbers without switching layers
- **Lightning-fast response** – direct `commitText`, minimal allocations, view reuse
- **Power efficient** – no background services, no network, hardware accelerated, light haptics only
- **Stable** – null-safe InputConnection handling, no crashes on edge cases
- **Offline suggestions** – tiny built-in word list (zero internet, zero privacy risk)
- **Symbols keyboard** – full punctuation and symbols layer
- **Smart Enter key** – shows Go / Search / Send / Done / Next based on the text field
- **Haptic + optional sound feedback**
- **100% offline** – no tracking, no analytics, no internet permission

## 🚀 How to use

1. Install the APK or build from source
2. Open **Pai Keyboard** app
3. Tap **Enable Pai Keyboard** → turn it on in system settings
4. Tap **Select Pai Keyboard** (or long-press space / use the keyboard switcher in any text field)
5. Start typing!

## 🛠 Build from source

```bash
git clone https://github.com/petedianotech/Pai-Keyboard.git
cd Pai-Keyboard
./gradlew assembleDebug
```

The APK will be at:  
`app/build/outputs/apk/debug/app-debug.apk`

### Requirements
- Android Studio Ladybug or newer (or command-line Gradle 8.9+)
- JDK 17
- minSdk 24 (Android 7.0)

## 📁 Project structure

```
app/src/main/java/com/paikeyboard/ime/
├── PaiKeyboardService.kt   # Core IME (InputMethodService)
├── MainActivity.kt         # Enable & select UI
└── SettingsActivity.kt     # Preferences

res/
├── layout/
│   ├── keyboard_view.xml   # Main QWERTY + number row
│   └── keyboard_symbols.xml
├── xml/method.xml          # IME declaration
└── ...
```

## 🔋 Why it’s power-friendly

- Views are inflated once and reused
- No continuous animation loops
- Extremely light haptic (12 ms)
- No ML models, no cloud predictions
- `onEvaluateFullscreenMode()` forced to false
- Proper cleanup in `onDestroy()`

## 🎨 Design choices

- Soft rounded keys (10 dp radius)
- Subtle borders + elevation for depth
- Indigo accent (`#6366F1`) for Enter key
- Clean light theme (dark theme support can be added easily)
- Consistent 3 dp row gaps and good touch targets

## 🗺 Roadmap (ideas)

- [ ] Full dark theme
- [ ] Custom themes
- [ ] Clipboard history
- [ ] One-handed mode
- [ ] More languages / layouts
- [ ] Emoji keyboard panel
- [ ] Swipe typing (optional, offline)

## License

MIT License – feel free to use, modify and ship.

---

Made with ❤️ for people who want a clean, fast, private keyboard.
