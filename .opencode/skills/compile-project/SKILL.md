---
name: compile-project
description: Use the Windows Android toolchain from WSL or Linux-based OpenCode sessions by invoking gradlew.bat through cmd.exe with Android Studio's bundled JBR
---

# Android Windows Toolchain

Use this skill when working on an Android project from OpenCode running in Linux or WSL while the Android development environment is installed on Windows.

## When to use

Use this skill when:
- OpenCode is running in Linux, WSL, or a Linux shell on Windows
- the project is an Android or Gradle-based Android project
- Android Studio is installed on Windows
- Java or Gradle resolution in Linux is unreliable or inconvenient
- you need to run Android Gradle tasks such as compile, assemble, test, or lint

## Core rule

For Android tasks in this environment, prefer the Windows toolchain instead of the Linux one.

Do not rely on `./gradlew` or Linux-side Java by default.

From the repository root, invoke `gradlew.bat` through `cmd.exe` and set `JAVA_HOME` to Android Studio's bundled JBR for that command.

## Windows toolchain paths

Android Studio JBR:

```bat
C:\Program Files\Android\Android Studio\jbr
````

Command shell:

```bat
C:\Windows\System32\cmd.exe
```

## Command pattern

Use this pattern for Android Gradle tasks:

```bat
"C:\Windows\System32\cmd.exe" /c "set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr&& set PATH=%JAVA_HOME%\bin;%PATH%&& gradlew.bat <TASK>"
```

Replace `<TASK>` with the needed Gradle task.

## Common examples

Compile Kotlin:

```bat
"C:\Windows\System32\cmd.exe" /c "set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr&& set PATH=%JAVA_HOME%\bin;%PATH%&& gradlew.bat compileDebugKotlin"
```

Assemble debug:

```bat
"C:\Windows\System32\cmd.exe" /c "set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr&& set PATH=%JAVA_HOME%\bin;%PATH%&& gradlew.bat assembleDebug"
```

Run unit tests:

```bat
"C:\Windows\System32\cmd.exe" /c "set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr&& set PATH=%JAVA_HOME%\bin;%PATH%&& gradlew.bat testDebugUnitTest"
```

Run lint:

```bat
"C:\Windows\System32\cmd.exe" /c "set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr&& set PATH=%JAVA_HOME%\bin;%PATH%&& gradlew.bat lintDebug"
```

## Why

In this setup:

* OpenCode runs in Linux or WSL
* Android development is installed on Windows
* Linux-side Android setup is often incomplete, brittle, or inconvenient
* `JAVA_HOME` may be missing in the Linux shell
* Android Studio already ships a working JBR on Windows

Using the Windows wrapper and Windows JBR is the most reliable default.

## Guidance

When asked to run an Android Gradle task:

* run it from the repository root
* use `cmd.exe` plus `gradlew.bat`
* set `JAVA_HOME` inline for the command
* prepend `%JAVA_HOME%\bin` to `PATH` inline for the command
* keep the same command structure and only swap the Gradle task name
* prefer this workflow unless the project explicitly requires a Linux-native Android setup

## Avoid

Avoid:

* defaulting to `./gradlew`
* assuming Linux-side Java, Android SDK, or Gradle are configured correctly
* changing global Java settings unless explicitly requested

## Output style

Provide exact copy-pasteable commands.
For Android tasks, preserve the Windows `cmd.exe` + `gradlew.bat` + Android Studio JBR pattern and only change the Gradle task as needed.

````