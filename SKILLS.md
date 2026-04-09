# Local Build Notes

## Android Compile

This workspace runs in a Linux shell on Windows, and `JAVA_HOME` is not always set in the shell.
The reliable way to compile is to use the Windows Gradle wrapper through `cmd.exe` and point it at
Android Studio's bundled JBR.

Use this from the repo root:

```bat
"C:\Windows\System32\cmd.exe" /c "set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr&& set PATH=%JAVA_HOME%\bin;%PATH%&& gradlew.bat compileDebugKotlin"
```

## Why

- plain `./gradlew ...` may fail because `JAVA_HOME` is missing
- plain `gradlew.bat ...` may also fail for the same reason
- Android Studio already ships a working JBR at:
  - `C:\Program Files\Android\Android Studio\jbr`

## Useful Variants

Compile Kotlin only:

```bat
"C:\Windows\System32\cmd.exe" /c "set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr&& set PATH=%JAVA_HOME%\bin;%PATH%&& gradlew.bat compileDebugKotlin"
```

Assemble debug app:

```bat
"C:\Windows\System32\cmd.exe" /c "set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr&& set PATH=%JAVA_HOME%\bin;%PATH%&& gradlew.bat assembleDebug"
```

Run unit tests:

```bat
"C:\Windows\System32\cmd.exe" /c "set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr&& set PATH=%JAVA_HOME%\bin;%PATH%&& gradlew.bat testDebugUnitTest"
```
