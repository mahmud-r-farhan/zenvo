# Zenvo: Focus Timer Widget

**Zenvo** is a completely customizable, minimal, and battery-friendly Focus Timer Widget for Android. It lives right on your home screen, so you can stay focused, run Pomodoro sessions, or time-block your day without ever opening an app.

<p align="center">
  <img alt="Platform" src="https://img.shields.io/badge/platform-Android-3DDC84?logo=android&logoColor=white">
  <img alt="Language" src="https://img.shields.io/badge/language-Kotlin-7F52FF?logo=kotlin&logoColor=white">
  <img alt="Build" src="https://img.shields.io/badge/build-Gradle%20Kotlin%20DSL-02303A?logo=gradle&logoColor=white">
</p>

---

## ✨ Why Zenvo?

- **Always Visible, Always Focused** — Your timer lives directly on the home screen, so a quick glance tells you exactly how much focus time is left.
- **Zero Distractions** — No complicated menus, no social feeds. Just a clean, minimal countdown.
- **Battery Friendly & Reliable** — Built to work around Doze mode limitations so the timer keeps running accurately in the background without draining your battery.

## 🚀 Key Features

- **Minimalist Widget Designs** — Modern, home-screen widgets that blend with any wallpaper or theme (Material You support).
- **One-Tap Controls** — Start, pause, or reset a focus session directly from the widget.
- **Customizable Time Blocks** — Use a 25-minute Pomodoro sprint, a 60-minute deep work block, or any duration you like.
- **Lightweight & Fast** — Optimized to be gentle on performance and battery.

## 🎯 Perfect For

- Studying for exams
- Remote work and home-office task management
- ADHD management and overcoming procrastination
- Reading, meditation, and habit building
- Writing and coding in flow states

## 🛠️ Tech Stack

- **Language:** Kotlin
- **Build System:** Gradle (Kotlin DSL — `build.gradle.kts`, `settings.gradle.kts`)
- **Platform:** Android (App Widget)

## 📂 Project Structure

```
zenvo/
├── app/                     # Main Android application module
├── gradle/                  # Gradle wrapper files
├── build.gradle.kts         # Top-level build configuration
├── settings.gradle.kts      # Gradle project settings
├── gradle.properties        # Gradle configuration properties
├── gradlew / gradlew.bat    # Gradle wrapper scripts
└── zenvo_aso_materials.md   # App Store Optimization content/notes
```

## 🧰 Getting Started

### Prerequisites

- [Android Studio](https://developer.android.com/studio) (latest stable release recommended)
- JDK 17+
- An Android device or emulator running a recent Android version

### Clone the repository

```bash
git clone https://github.com/mahmud-r-farhan/zenvo.git
cd zenvo
```

### Build the project

Using the included Gradle wrapper:

```bash
# macOS / Linux
./gradlew build

# Windows
gradlew.bat build
```

### Run the app

- Open the project in Android Studio and click **Run**, or
- Install a debug build via the wrapper:

```bash
./gradlew installDebug
```

Then add the **Zenvo** widget to your home screen: long-press on your home screen → **Widgets** → **Zenvo**.

## 🤝 Contributing

Contributions are welcome!

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/your-feature`)
3. Commit your changes (`git commit -m "Add your feature"`)
4. Push to the branch (`git push origin feature/your-feature`)
5. Open a Pull Request


## 👤 Author

**Mahmud Rahman** ([@mahmud-r-farhan](https://github.com/mahmud-r-farhan))

---

*Stop losing time to endless scrolling. Reclaim your attention, master your schedule, and get more done with Zenvo.*
