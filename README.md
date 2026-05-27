<div align="center">
  <img src="banner.png" width="100%" alt="BlockerSpam Banner">
  <h1>🛡️ BlockerSpam</h1>
  <p><strong>Advanced Call Interceptor & Spam Blocker for Android</strong></p>

  <p>
    <img src="https://img.shields.io/badge/Kotlin-1.9.0-blue.svg?style=for-the-badge&logo=kotlin" alt="Kotlin" />
    <img src="https://img.shields.io/badge/Android_SDK-34-4CAF50.svg?style=for-the-badge&logo=android" alt="Android SDK" />
    <img src="https://img.shields.io/badge/Architecture-MVVM-FF9800.svg?style=for-the-badge" alt="MVVM" />
    <img src="https://img.shields.io/badge/Database-Room-00BCD4.svg?style=for-the-badge&logo=sqlite" alt="Room DB" />
  </p>
</div>

## 📖 Overview

**BlockerSpam** is a robust, privacy-first Android application designed to automatically intercept and reject calls from unsaved or suspicious numbers. Built with a sleek, premium dark-mode UI (steampunk/high-tech aesthetic), it runs silently in the background using the modern Android `CallScreeningService` API, ensuring that spam calls never ring or disrupt your focus.

## ✨ Key Features

- **🛡️ Silent Interception**: Utilizes `CallScreeningService` to instantly reject unknown numbers without the phone ever ringing.
- **📇 Intelligent Whitelisting**: Only allows calls from your saved contacts, automatically blocking everything else.
- **📊 Comprehensive Call Log**: Maintains a detailed history of all blocked calls, including timestamp, region, carrier, and type, stored locally via Room Database.
- **🎨 Premium UI/UX**: Full-screen edge-to-edge support with a custom steampunk-themed interface and fluid animations.
- **🔄 In-App Auto-Updates**: Seamlessly checks for new releases on GitHub and prompts for an update directly inside the app.
- **🔒 Privacy First**: All data is stored locally on the device. No external tracking or data collection.

## 🛠️ Tech Stack

- **Language:** [Kotlin](https://kotlinlang.org/)
- **UI Toolkit:** Android Views (XML) & Edge-to-Edge display
- **Architecture:** MVVM (Model-View-ViewModel)
- **Database:** [Room Persistence Library](https://developer.android.com/training/data-storage/room)
- **Concurrency:** Kotlin Coroutines
- **Telecom:** `CallScreeningService` for seamless call interception

## 🚀 Getting Started

### 📥 Download the App

You can download the compiled APK directly from the repository and install it on your device:

<a href="https://github.com/Mr4bb1t/BlockerSpam/raw/main/BlockerSpam1.1.apk">
  <img src="https://img.shields.io/badge/Download-BlockerSpam1.1.apk-brightgreen?style=for-the-badge&logo=android" alt="Download APK" />
</a>

> **Note**: You might need to allow "Install from Unknown Sources" in your Android settings to install the APK.

---

### 💻 Build from Source

#### Prerequisites
- Android Studio Iguana / Jellyfish or newer
- Android SDK 34
- A physical Android device (Call blocking features are difficult to test on an emulator)

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/Mr4bb1t/BlockerSpam.git
   ```
2. Open the project in **Android Studio**.
3. Sync Gradle files.
4. Build and run the app on your physical device.

> **Note**: Upon first launch, the app will request to be set as the Default Caller ID & Spam App. This is strictly required by Android to enable the `CallScreeningService`.

## 🤝 Contributing

Contributions, issues, and feature requests are welcome! 
Feel free to check [issues page](https://github.com/Mr4bb1t/BlockerSpam/issues).

## 📄 License

This project is licensed under the MIT License.

<div align="center">
  <p>Built with ❤️ by <a href="https://github.com/Mr4bb1t">Emerson (Mr4bb1t)</a></p>
</div>
