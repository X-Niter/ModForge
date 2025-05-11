# ModForge IntelliJ Plugin

## Overview
This IntelliJ IDEA plugin provides AI-powered assistance for Minecraft mod development with multi-mod loader support (Forge, Fabric, Quilt, Architectury).

## Compatibility

### IntelliJ IDEA 2025.1.1.1 Compatibility Guide
This plugin is specially optimized for IntelliJ IDEA 2025.1.1.1 (Build: 251.25410.129) and designed to utilize Java 21 features.

### Built for Production
The plugin has undergone extensive testing and optimization to ensure compatibility, stability, and optimal performance, especially for the target IntelliJ IDEA 2025.1.1.1 version.

## Building the Plugin

Due to the complexity of building for the newest IntelliJ versions, we've provided specialized build scripts:

### For Windows:
```
.\build-plugin-2025.1.1.1.ps1
```

### For Linux/Mac:
```
./build-for-2025.1.sh
```

These scripts provide an interactive process that will:
1. Detect your environment
2. Ask whether to build using repository version or local IntelliJ installation 
3. Create temporary configuration for the build
4. Handle dependency resolution automatically
5. Provide detailed error messages if issues occur

### Alternative Manual Build

If you prefer to build manually, you can:

1. **Option A: Build using local IntelliJ installation**
   - Open `build.gradle`
   - Comment out the `version = '2023.3.6'` line
   - Uncomment and modify the `localPath = '...'` line to point to your IntelliJ installation
   - Run `./gradlew buildPlugin`

2. **Option B: Build using repository version (easier)**
   - Run `./gradlew buildPlugin` directly
   - This uses IntelliJ 2023.3.6 from the repository but sets compatibility metadata for 2025.1.1.1

## Installation

1. Build the plugin using one of the methods above
2. Open IntelliJ IDEA 2025.1.1.1
3. Go to Settings → Plugins → ⚙ → Install Plugin from Disk...
4. Select the generated ZIP file from `build/distributions/`
5. Restart IntelliJ IDEA when prompted

## Feature Highlights

- AI-driven code generation
- Automatic error detection and fixing
- Multi-mod loader support (Forge, Fabric, Quilt, Architectury)
- Pattern recognition to reduce API costs
- Continuous development for 24/7 improvement
- GitHub integration for version control
- Enhanced memory management
- Memory visualization tools
- Java 21 virtual thread optimization

## Plugin Structure

The plugin architecture follows a modular design with several key components:

- `com.modforge.intellij.plugin` - Core plugin classes
- `com.modforge.intellij.plugin.ai` - AI code generation and pattern learning
- `com.modforge.intellij.plugin.memory` - Memory management system
- `com.modforge.intellij.plugin.loaders` - Mod loader specific implementations
- `com.modforge.intellij.plugin.designers` - Visual designers for game elements
- `com.modforge.intellij.plugin.github` - GitHub integration

## Optional Dependencies

The plugin has been designed to work even when certain IntelliJ plugins are not available:

- **GitHub Plugin**: Enhances GitHub integration but not required
- **Git4Idea Plugin**: Provides additional Git features but falls back gracefully
- **Minecraft Development Plugin**: Adds specialized Minecraft support but has fallbacks

Each optional dependency is defined in its own configuration file in `META-INF/`:
- `github-integration.xml`
- `git-integration.xml`
- `minecraft-dev-integration.xml`

## Troubleshooting Build Issues

If you encounter build issues:

1. **GitHub Plugin Dependency Issues**:
   - This is now handled through optional dependency configuration
   - The GitHub integration functionality will be disabled if the plugin is missing
   - No build errors will occur due to missing GitHub plugin

2. **Java Version Errors**:
   - Ensure you're using Java 17 or higher to build
   - JDK 21 is recommended for development but not strictly required for building

3. **Build Script Errors**:
   - Make sure scripts have execute permissions on Linux/Mac
   - On Windows, you may need to run PowerShell with administrator privileges

4. **Missing Dependencies**:
   - The build scripts attempt to detect and resolve dependency issues
   - Add `--debug` or `--stacktrace` flags to gradle commands for more detailed error information
