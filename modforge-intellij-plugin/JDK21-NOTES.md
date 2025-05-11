# Java 21 Configuration for IntelliJ IDEA 2025.1.1.1

This document provides important information about Java 21 compatibility for the ModForge IntelliJ plugin when targeting IntelliJ IDEA 2025.1.1.1.

## Critical Requirements

IntelliJ IDEA 2025.1.1.1 (Build: 251.25410.129) runs on Java 21. For proper plugin compatibility:

1. **Your plugin must be compiled with Java 21 compatibility**
2. **Gradle must use Java 21 for the build process**
3. **Special JVM arguments are required for module access**

## Quick Solution

The fastest way to resolve Java 21 compatibility issues is to use our included build script:

```
easy-build-2025.1.1.1.bat
```

This script:
- Automatically configures all necessary Java 21 settings
- Temporarily modifies build files for compatibility
- Restores original configuration after building
- Works even if you don't have Java 21 as your default JDK

## Manual Configuration

If you prefer to configure Java 21 manually:

### 1. Project Structure

- Open IntelliJ IDEA
- Go to File → Project Structure
- Set Project SDK to Java 21
- Set Project language level to "21 - Records, patterns, sealed types, var, switches"
- Make sure Module SDK is also set to Java 21

### 2. Gradle Configuration

Add these lines to your `gradle.properties`:

```
# Java 21 path (update the path to match your Java 21 installation)
org.gradle.java.home=C:\\Program Files\\Eclipse Adoptium\\jdk-21.0.1.12-hotspot

# JVM arguments for Java 21 modules
org.gradle.jvmargs=-Xmx2048m --add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8
```

### 3. Build Configuration

In `build.gradle`, ensure:

```groovy
java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}
```

## Common Java 21 Installations

Here are common paths where Java 21 might be installed:

### Windows
- `C:\Program Files\Eclipse Adoptium\jdk-21`
- `C:\Program Files\Java\jdk-21`
- `C:\Program Files\BellSoft\LibericaJDK-21`
- `C:\Program Files\Amazon Corretto\jdk21`

### macOS
- `/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home`
- `/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home`
- `/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home`

### Linux
- `/usr/lib/jvm/java-21-openjdk`
- `/usr/lib/jvm/java-21-openjdk-amd64`
- `/usr/lib/jvm/bellsoft-java21`
- `/usr/lib/jvm/temurin-21`

## Downloading Java 21

If you don't have Java 21 installed, you can download it from:

- **Eclipse Adoptium (recommended)**: https://adoptium.net/
- **Oracle**: https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html
- **Amazon Corretto**: https://aws.amazon.com/corretto/
- **BellSoft Liberica**: https://bell-sw.com/pages/downloads/

## Verifying Java 21

To check if Java 21 is properly configured:

```bash
java -version
```

You should see output containing "version 21" or "21.0.x".