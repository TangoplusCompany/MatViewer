# Android

# MatViewer Android Application

A modern Android application designed for pressure mat data visualization, real-time device connection, location tracking, and cloud data synchronization.

---

## 🛠 Tech Stack & Key Features

- **Language & Build System:** Kotlin (2.0+) with Gradle Kotlin DSL (`.kts`) and Version Catalogs (`libs.versions.toml`).
- **Hardware Interface:** Real-time USB Serial communication via `usb-serial-for-android`.
- **Local Database:** Reactive, typesafe local storage using **Jetpack Room** fully powered by **KSP (Kotlin Symbol Processing)**.
- **Cloud Storage:** Direct cloud synchronization using the **Supabase Storage KTX** client.
- **Networking:** Modern asynchronous HTTP requests using **Ktor Client** with JSON content negotiation and built-in logging.
- **Location Services:** Highly accurate geolocation logging via **Google Play Services Location**.
- **Utilities:** Embedded **ZXING** library for QR code decoding and processing.

---

## 📋 Prerequisites & Requirements

- **Minimum SDK:** API 31 (Android 12)
- **Target SDK:** API 36 (Android 14+ / Next-Gen compatibility)
- **Java Compatibility:** Java 11 (`JavaVersion.VERSION_11`)
- **Jetpack Libraries:** Full ViewBinding architecture enabled for safe UI updates.

---

## 📦 Architecture & Key Dependencies

### Core Architecture & UI

- `androidx.appcompat`, `material`, `constraintlayout`: Modern Material Design UI components.
- `fragment-ktx`, `activity-ktx`: Elegant Kotlin extensions for simplified ViewModel and lifecycle management.
- `recyclerview`, `cardview`: Optimized structured listing structures for record logs.

### Hardware & Peripherals

- `com.github.mik3y:usb-serial-for-android:3.9.0`: Manages physical USB connections to pressure sensor mats.
- `com.google.zxing:core:3.5.4`: QR code matrix scanner integration.

### Database (Room with KSP)

````kotlin
val roomVersion = "2.7.1"
implementation("androidx.room:room-runtime:$roomVersion")
implementation("androidx.room:room-ktx:$roomVersion")
ksp("androidx.room:room-compiler:$roomVersion") // Blazing-fast KSP compilation


---
---
---

# Front-end
# React + TypeScript + Vite

This template provides a minimal setup to get React working in Vite with HMR and some ESLint rules.

Currently, two official plugins are available:

- [@vitejs/plugin-react](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react) uses [Oxc](https://oxc.rs)
- [@vitejs/plugin-react-swc](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react-swc) uses [SWC](https://swc.rs/)

## React Compiler

The React Compiler is not enabled on this template because of its impact on dev & build performances. To add it, see [this documentation](https://react.dev/learn/react-compiler/installation).

## Expanding the ESLint configuration

If you are developing a production application, we recommend updating the configuration to enable type-aware lint rules:

```js
export default defineConfig([
  globalIgnores(['dist']),
  {
    files: ['**/*.{ts,tsx}'],
    extends: [
      // Other configs...

      // Remove tseslint.configs.recommended and replace with this
      tseslint.configs.recommendedTypeChecked,
      // Alternatively, use this for stricter rules
      tseslint.configs.strictTypeChecked,
      // Optionally, add this for stylistic rules
      tseslint.configs.stylisticTypeChecked,

      // Other configs...
    ],
    languageOptions: {
      parserOptions: {
        project: ['./tsconfig.node.json', './tsconfig.app.json'],
        tsconfigRootDir: import.meta.dirname,
      },
      // other options...
    },
  },
])

````

You can also install [eslint-plugin-react-x](https://github.com/Rel1cx/eslint-react/tree/main/packages/plugins/eslint-plugin-react-x) and [eslint-plugin-react-dom](https://github.com/Rel1cx/eslint-react/tree/main/packages/plugins/eslint-plugin-react-dom) for React-specific lint rules:

```js
// eslint.config.js
import reactX from "eslint-plugin-react-x";
import reactDom from "eslint-plugin-react-dom";

export default defineConfig([
  globalIgnores(["dist"]),
  {
    files: ["**/*.{ts,tsx}"],
    extends: [
      // Other configs...
      // Enable lint rules for React
      reactX.configs["recommended-typescript"],
      // Enable lint rules for React DOM
      reactDom.configs.recommended,
    ],
    languageOptions: {
      parserOptions: {
        project: ["./tsconfig.node.json", "./tsconfig.app.json"],
        tsconfigRootDir: import.meta.dirname,
      },
      // other options...
    },
  },
]);
```
