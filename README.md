# android-ndk-aide

Stripped Android NDK toolchain for **on-device** native compilation.  
Used by [Dex2c -+-](https://github.com/Matrix1999) to compile DEX→C++ generated sources directly on the Android device — no PC, no server, no internet after install.

## What this is

This is the **AIDE NDK Builder** source — a minimal Android NDK toolchain that runs natively on ARM64 Android devices (not on a desktop PC). Unlike the standard Android NDK which only runs on Linux/macOS/Windows, this toolchain runs on the device itself, enabling fully offline on-device C/C++ compilation.

## How it works

```
User APK (DEX bytecode)
        ↓  [Dex2c transpiler — on device]
    C++ source files
        ↓  [android-ndk-aide / ndk-build — on device]
    Native .so library
        ↓  [APK repacker + signer]
    Protected APK (bytecode stripped, native code only)
```

The NDK binary (`ndk-build`) is compiled for ARM64 Android — it runs on the phone, not a PC.

## One-time setup (inside the Dex2c app)

1. Open **Dex2c -+-** → tap **Install Compiler**
2. The app downloads this NDK package (~50 MB, one-time)  
3. Extracts to private app storage (`filesDir`) — no root needed  
4. After that: **fully offline forever** — no internet needed to protect APKs

## Contents

```
app/
  src/main/
    java/com/a4455jkjh/ndk/
      InstallNdk.java     — download + extract NDK tarball to filesDir
      NdkBuild.java       — run ndk-build via ProcessBuilder
      MainActivity.java   — simple launcher UI
      Q.java              — path helpers
    jni/
      link.c              — JNI bridge (symlink/hardlink for tar extraction)
      Android.mk
      Application.mk      — APP_STL=c++_static, APP_CPPFLAGS=-std=c++17
    jniLibs/
      arm64-v8a/liblink.so
      armeabi-v7a/liblink.so
      x86/liblink.so
      x86_64/liblink.so
```

## Building

Standard Android Gradle build:

```bash
./gradlew assembleDebug
```

Requires Android SDK + NDK. The `liblink.so` JNI library (for symlink/hardlink support during tar extraction) is pre-built for all 4 ABIs.

## License

Original NDK Builder by `a4455jkjh`. Modifications by `@aantik_mods`.  
Shared for educational purposes. AIDE is a product of Bline Systems.
