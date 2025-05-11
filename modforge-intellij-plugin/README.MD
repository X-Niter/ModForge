# ModForge IntelliJ Plugin

IntelliJ IDEA plugin for ModForge - the AI-powered Minecraft mod development platform.

## Build Status

This plugin is fully compatible with IntelliJ IDEA 2025.1, optimized for use with Java 21.

## Building the Plugin

### Prerequisites

- JDK 17+ (JDK 21 recommended)
- Gradle 8.5+
- IntelliJ IDEA 2023.3.6+ or IntelliJ IDEA 2025.1

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
    localPath = 'C:/Program Files/JetBrains/IntelliJ IDEA Community Edition 2025.1'
}
```

## Troubleshooting

If you're building for IntelliJ 2025.1, you have two options:

1. **Repository Build**: Use version `2023.3.6` for building but set compatibility to include `untilBuild = 251.*`
2. **Local Build**: Comment out the `version` and use `localPath` pointing to your local IntelliJ 2025.1 installation

## Plugin Features

- AI-powered code generation for Minecraft mods
- Multi-mod loader support (Forge, Fabric, Quilt, Architectury)
- Smart error detection and fixing
- Memory optimization for Minecraft development
- GitHub integration for version control
- Continuous development for 24/7 improvement

## System Requirements

- IntelliJ IDEA 2023.3+ (2025.1 recommended)
- JDK 17+ (JDK 21 recommended for 2025.1)
- Minecraft Development plugin (required dependency)

## License

This project is licensed under the MIT License - see the LICENSE file for details.
