# ModForge IntelliJ Plugin

IntelliJ IDEA plugin for ModForge - the AI-powered Minecraft mod development platform.

## Build Status

This plugin is fully compatible with IntelliJ IDEA 2025.1.1.1 (Build: 251.25410.129), optimized for use with Java 21.

![Compatible with IntelliJ IDEA 2025.1.1.1](https://img.shields.io/badge/IntelliJ_IDEA-2025.1.1.1-blue)
![Java](https://img.shields.io/badge/Java-21-orange)

## Building the Plugin

### Prerequisites

- JDK 17+ (JDK 21 recommended)
- Gradle 8.5+
- IntelliJ IDEA 2023.3.6+ or IntelliJ IDEA 2025.1.1.1

### Build Commands

```bash
# Using Gradle wrapper (recommended)
./gradlew buildPlugin

# The plugin ZIP will be generated in:
# build/distributions/modforge-intellij-plugin-2.1.0.zip
```

### Development Setup

For optimal development experience:

1. Clone the repository
2. Open in IntelliJ IDEA
3. Use the Gradle plugin to import the build file
4. Run the `runIde` task to test in a development instance

## Configuration Options

### Use a specific IntelliJ version from repositories

```groovy
intellij {
    version = '2023.3.6'
    type = 'IC'
}
```

### Use your locally installed IntelliJ

```groovy
intellij {
    localPath = 'C:/Program Files/JetBrains/IntelliJ IDEA Community Edition 2025.1.1.1'
}
```

## Build Options for 2025.1.1.1

### Option 1: Use helper scripts (Recommended)

We've provided helper scripts for building with IntelliJ 2025.1.1.1:

- **Windows**: Run `build-for-2025.1.1.1.bat` or `build-plugin-local.bat`
- **Mac/Linux**: Run `./build-for-2025.1.1.1.sh`

### Option 2: Manual configuration

You have two approaches for building the plugin:

1. **Repository Build**: Use version `2023.3.6` for building but set compatibility to include `untilBuild = 251.25410.129`
2. **Local Build**: Comment out the `version` and use `localPath` pointing to your local IntelliJ 2025.1.1.1 installation

## Plugin Features

- AI-powered code generation for Minecraft mods
- Multi-mod loader support (Forge, Fabric, Quilt, Architectury)
- Smart error detection and fixing
- Memory optimization for Minecraft development
- GitHub integration for version control
- Continuous development for 24/7 improvement

## System Requirements

- IntelliJ IDEA 2023.3+ (2025.1.1.1 recommended)
- JDK 17+ (JDK 21 required for 2025.1.1.1)
- Minecraft Development plugin (optional, with fallback mechanism)

## License

This project is licensed under the MIT License - see the LICENSE file for details.
