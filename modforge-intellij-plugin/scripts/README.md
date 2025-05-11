# ModForge IntelliJ Plugin Build Scripts

This directory contains utility scripts to help build and configure the ModForge IntelliJ plugin, especially for compatibility with IntelliJ IDEA 2025.1.1.1.

## Available Scripts

### Windows Scripts

- **setup-java21-windows.bat**: Automatically detects and configures Java 21 for building
- **../easy-build-2025.1.1.1.bat**: All-in-one build script that handles Java 21 setup and builds the plugin for 2025.1.1.1
- **../win-build-2025.1.bat**: Interactive build script with support for both local and repository builds

### Java 21 Compatibility

IntelliJ IDEA 2025.1.1.1 requires Java 21 compatibility. The build scripts in this directory help ensure:

1. Your Gradle build is using Java 21
2. The plugin is compiled with Java 21 compatibility
3. Proper JVM args are set to handle module access requirements

## Common Issues & Solutions

### "Java 21 required" Error

If you're still seeing "Java 21 required" errors despite setting your project structure to use Java 21:

1. Run **easy-build-2025.1.1.1.bat** which automatically configures everything
2. Ensure your JAVA_HOME environment variable points to a valid Java 21 installation
3. Check if the JDK used by Gradle (defined in gradle.properties) is Java 21

### Build Errors with GitHub Plugin

The plugin is now configured to make GitHub integration optional:

1. The GitHub plugin dependency is marked as optional in plugin.xml
2. Optional integration files are set up for graceful degradation
3. No build errors should occur if the GitHub plugin is missing

### Plugin Version Compatibility

The plugin is configured to support:
- Minimum: IntelliJ 2023.3 (Build 233)
- Maximum: All IntelliJ 2025.1.x versions (Build 251.*)

This ensures compatibility while still working with the latest 2025.1.1.1 release.

## Running the Scripts

Always run these scripts from the root project directory (the folder containing build.gradle).

Example:
```
cd modforge-intellij-plugin
easy-build-2025.1.1.1.bat
```

The scripts will create temporary configuration files during the build process and restore your original configuration files when done.