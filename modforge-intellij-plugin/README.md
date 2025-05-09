# ModForge IntelliJ Plugin

The ModForge IntelliJ Plugin provides an integrated development environment for Minecraft mod development with AI assistance, helping you build mods more efficiently.

## Features

- **AI-assisted Code Generation**: Generate code with natural language prompts
- **Automatic Error Correction**: Fix compilation errors automatically
- **Documentation Generation**: Generate high-quality JavaDoc documentation
- **Continuous Development Mode**: Automatically improve your code over time
- **Pattern Recognition**: Reduce API costs by using cached responses for similar requests
- **Cross-Loader Support**: Develop for Forge, Fabric, and Quilt with the same codebase
- **GitHub Integration**: Sync with GitHub repositories seamlessly

## Building the Plugin

### Prerequisites
- JDK 17 or higher
- IntelliJ IDEA (Community or Ultimate)

### Quick Build

Use the included build script which automatically finds a compatible JDK:

```bash
cd modforge-intellij-plugin
./build-plugin.sh
```

### Manual Build

If you prefer to build manually:

```bash
cd modforge-intellij-plugin
export JAVA_HOME=/path/to/jdk-17
./gradlew clean buildPlugin
```

The plugin ZIP file will be generated in `build/distributions/`.

## Installation

### From Build

1. Open IntelliJ IDEA
2. Go to **File → Settings → Plugins**
3. Click the gear icon and select **Install Plugin from Disk...**
4. Navigate to `build/distributions/` and select the generated ZIP file
5. Restart IntelliJ IDEA when prompted

### First-time Configuration

1. Open IntelliJ IDEA after installing the plugin
2. Go to **Settings → Tools → ModForge**
3. Enter your API keys:
   - OpenAI API Key for AI code generation
   - GitHub token for repository integration
4. Configure project-specific settings if needed

## Usage Guide

### AI Code Generation

1. Right-click in an editor and select **ModForge → Generate Code**
2. Enter a description of the code you want to generate
3. The generated code will be inserted at the cursor position

### Error Fixing

1. When a compilation error occurs, right-click on the error and select **ModForge → Fix Error**
2. The plugin will analyze the error and suggest fixes

### Documentation Generation

1. Right-click on a class or method that needs documentation
2. Select **ModForge → Generate Documentation**
3. The plugin will generate appropriate JavaDoc comments

### Continuous Development

1. Enable continuous development mode via **Tools → ModForge → Toggle Continuous Development**
2. The plugin will monitor your code for errors and fix them automatically

## Troubleshooting

### Plugin Doesn't Build

- Ensure JDK 17 or higher is installed and JAVA_HOME is set correctly
- Try using the included `build-plugin.sh` script which attempts to locate JDK 17

### Plugin Doesn't Load

- Check IntelliJ IDEA logs at **Help → Show Log in Explorer/Finder**
- Verify the plugin is compatible with your version of IntelliJ IDEA
- Try invalidating caches via **File → Invalidate Caches / Restart**

### API Connectivity Issues

- Check your API keys in the settings
- Verify your network connection and proxy settings
- Check firewall rules that might block API requests

## License

This project is licensed under the MIT License - see the LICENSE file for details.