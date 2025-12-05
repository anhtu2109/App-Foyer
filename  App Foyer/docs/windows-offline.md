# Windows 10 Offline Packaging Guide

The target PC has no Java runtime and no Internet access, so the app must be copied together with its own JRE and JavaFX bits. Because JavaFX natives are platform-specific, you **must** prepare the bundle from a Windows 10 x64 machine (physical, VM, or a friend’s laptop) at least once. Cross-compiling from macOS is not supported by `jlink/jpackage`.

## 1. Collect everything while you still have Internet

Do this on any machine that *does* have Internet access.

1. Build the application JAR so all dependencies land in `target/` and in your local Maven cache:
   ```bash
   mvn -Djavafx.platform=win clean package
   ```
2. Download the following Windows binaries (zip archives) and keep them next to the project:
   - [Temurin JDK 17 (Windows x64, `.zip`)](https://github.com/adoptium/temurin17-binaries/releases) — contains `jlink`/`jpackage`.
   - [JavaFX SDK 21.0.3 for Windows x64 (`javafx-sdk-21.0.3-windows-x64.zip`)](https://gluonhq.com/products/javafx/).
   - (Optional) [WiX Toolset 3.14 offline installer](https://wixtoolset.org/) if you want an `.msi` instead of a plain `.exe`.
3. Copy the whole project folder, `restaurant.db`, the downloaded ZIPs, and your local Maven repo segment `~/.m2/repository/org/openjfx` onto a USB drive.

## 2. Prepare the offline Windows packaging machine

On the Windows 10 box (still offline):

1. Copy the USB contents to, e.g., `C:\FoyerApp`.
2. Extract the JDK ZIP to `C:\FoyerApp\jdk-17`.
3. Extract the JavaFX ZIP to `C:\FoyerApp\javafx-sdk-21.0.3`.
4. (Optional) Install WiX if you plan to emit an MSI.

## 3. Build a self-contained runtime image

Open **Developer Command Prompt** and run:

```bat
cd C:\FoyerApp
set JDK_HOME=C:\FoyerApp\jdk-17
set JAVAFX_HOME=C:\FoyerApp\javafx-sdk-21.0.3

rem Create a runtime with only the modules we need
"%JDK_HOME%\bin\jlink.exe" ^
  --module-path "%JDK_HOME%\jmods;%JAVAFX_HOME%\lib" ^
  --add-modules javafx.controls,javafx.fxml,java.sql ^
  --output runtime-win ^
  --strip-debug --compress=2 --no-header-files --no-man-pages
```

This produces `runtime-win\` with a private JRE that already includes the Windows JavaFX natives.

## 4. Stage the application

1. Create `dist\app` and copy:
   - `target/restaurant-app-1.0.0-SNAPSHOT.jar`
   - Each dependency from `target/lib/*.jar` (use `mvn dependency:copy-dependencies` if `target/lib` is missing).
   - `restaurant.db`
2. Add a launcher script `dist\RunRestaurantApp.bat`:

```bat
@echo off
set DIR=%~dp0
set APP=%DIR%app
set RUNTIME=%DIR%..\runtime-win

"%RUNTIME%\bin\java.exe" ^
  --module-path "%APP%";"%DIR%..\javafx-sdk-21.0.3\lib" ^
  --add-modules javafx.controls,javafx.fxml ^
  -cp "%APP%\*;%APP%\lib\*" ^
  App
```

Double-clicking this batch file launches the UI using the embedded runtime—no system-wide Java needed.

## 5. (Optional) Create a real `.exe` with `jpackage`

Still on the offline Windows box:

```bat
"%JDK_HOME%\bin\jpackage.exe" ^
  --type exe ^
  --name FoyerVietnam ^
  --input dist\app ^
  --main-jar restaurant-app-1.0.0-SNAPSHOT.jar ^
  --main-class App ^
  --runtime-image runtime-win ^
  --icon path\to\icon.ico ^
  --win-dir-chooser ^
  --win-shortcut
```

If WiX is installed you can add `--type msi` to produce an installer; otherwise `.exe` creates a simple app image folder plus launcher.

## 6. Move to the final offline PC

Copy either:

- The whole `dist\` folder + `runtime-win\` + `javafx-sdk-21.0.3\` + `restaurant.db` and run `RunRestaurantApp.bat`, or
- The generated `.exe`/`.msi` installer produced by `jpackage`.

Because we bundled the runtime, the Windows 10 machine never needs to install Java or connect to the Internet.

## Notes

- Anytime you change code, rerun steps 1, 3, and 4 to refresh the bundle.
- For future-proofing, consider adding the Maven Wrapper so `mvnw.cmd` can run without a system-wide Maven installation once Java is available.
