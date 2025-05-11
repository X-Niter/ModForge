# ModForge IntelliJ IDEA Plugin

IntelliJ IDEA plugin for the ModForge autonomous Minecraft mod development platform.

## Requirements

- **IntelliJ IDEA 2025.1.1.1** (Build #IC-251.25410.129) 
- **Java 21** or higher

## Installation

### Automated Installation

1. Download the plugin ZIP file
2. Run the `modforge-builder.bat` script (Windows)
3. Follow the on-screen prompts

The script will automatically:
- Find or download Java 21
- Fix common build issues
- Build the plugin
- Install it to your IntelliJ IDEA

### Manual Installation

If you prefer to install manually:

1. Build the plugin using: `gradlew clean build`
2. Locate the plugin ZIP file in: `build\distributions\modforge-intellij-plugin-2.1.0.zip`
3. In IntelliJ IDEA, go to Settings → Plugins → ⚙ → Install Plugin from Disk...
4. Select the plugin ZIP file
5. Restart IntelliJ IDEA when prompted

## Build Troubleshooting

If you're having issues building the plugin:

1. Ensure you're using Java 21 - check with `java -version`
2. Make sure your `JAVA_HOME` is set correctly
3. Run `modforge-builder.bat` which handles most common build issues automatically
4. Check the logs in `modforge-builder.log` for detailed error information

### Java 21 Configuration

The builder script can detect, download, and configure Java 21 automatically. If you need to manually configure:

1. Edit `gradle.properties`
2. Set `org.gradle.java.home=C:/path/to/your/jdk-21`
3. Run `gradlew clean build`

## Features

- Seamless IntelliJ IDEA integration for ModForge
- Access to AI-powered mod generation
- GitHub integration for version control
- Code generation and optimization tools
- Cross-loader mod development support

## Plugin Requirements

The plugin is designed to work with IntelliJ IDEA 2025.1.1.1 (Build #IC-251.25410.129) and requires Java 21.

## Need Help?

If you encounter any issues with the plugin:

1. Check the detailed log file in `modforge-builder.log`
2. Run the builder script with more verbose output using: `modforge-builder.bat --debug`
3. Contact support or file an issue in the GitHub repository