# ModForge IntelliJ Plugin

> **IMPORTANT UPDATE**: This plugin has been completely updated for compatibility with IntelliJ IDEA 2025.1 (Build #IC-251.23774.435) and Java 21. If you're using an older version of IntelliJ IDEA, please use an older version of this plugin. See [INTELLIJ_2025_COMPATIBILITY.md](INTELLIJ_2025_COMPATIBILITY.md) for details.

## Overview

ModForge IntelliJ Plugin is an AI-powered assistant for Minecraft mod development. The plugin integrates with the IntelliJ IDEA development environment to provide intelligent code generation, error detection and fixing, and continuous development capabilities.

## Features

- **Multi-mod loader support** - Works with Forge, Fabric, Quilt, and Architectury
- **AI-driven code generation** - Generate code by describing what you want
- **Automatic error detection and fixing** - Fix compilation errors with AI
- **Pattern recognition** - Reduces API costs by learning from patterns
- **Continuous development** - Keep working on your mods even when you're not
- **GitHub integration** - Automatic workflows for continuous improvement
- **Full IntelliJ IDEA 2025.1 compatibility** - Optimized for the latest IDE version
- **Java 21 optimization** - Uses virtual threads for improved performance

## System Requirements

- IntelliJ IDEA 2025.1 (Build #IC-251.23774.435) or compatible version (Community or Ultimate edition)
- Java Development Kit (JDK) 21.0.6 or later (compatible with JBR runtime 21.0.6+9-b895.109)
- Internet connection for API access
- Compatible with Minecraft Development plugin (v2025.1-1.8.4)

## Installation

### Installation from Disk

1. Build the plugin or download the latest release
2. Open IntelliJ IDEA
3. Go to Settings/Preferences → Plugins
4. Click the gear icon → Install Plugin from Disk...
5. Select the .zip file from the `build/distributions` directory
6. Restart IntelliJ IDEA when prompted

### Building from Source

1. Clone this repository
2. Navigate to the plugin directory
3. Run the production build script:
   ```
   ./build-production-plugin.sh
   ```
4. The script will output the location of the generated plugin file
5. Follow the installation steps above to install the plugin

## Configuration

1. After installation, open the ModForge settings in IntelliJ IDEA:
   - Settings/Preferences → Tools → ModForge AI Settings
2. Configure the following settings:
   - Server URL: The URL of your ModForge server
   - API Key: Your OpenAI API key
   - GitHub Token: Your GitHub personal access token (if using GitHub integration)
3. Click "Test Connection" to verify your settings
4. Click "Apply" to save your settings

## Usage

### Generate Code with AI

1. Right-click in the editor
2. Select "Generate Code with ModForge AI"
3. Describe what you want to generate
4. Review and accept the generated code

### Fix Errors with AI

1. If there are compilation errors, right-click on the error
2. Select "Fix Errors with ModForge AI"
3. The plugin will analyze and fix the errors

### Create a New Mod

1. Go to File → New → ModForge → Minecraft Mod
2. Select the mod loader (Forge, Fabric, Quilt, or Architectury)
3. Enter the mod details
4. Click "Create" to generate the mod template

### Push to GitHub

1. From the Tools menu, select ModForge AI → Push to GitHub
2. Enter the repository details
3. Click "Push" to create or update the GitHub repository

### Toggle Continuous Development

1. From the Tools menu, select ModForge AI → Toggle Continuous Development
2. The plugin will start monitoring and improving your code automatically

## Troubleshooting

- If the plugin is not working, check the following:
  - Make sure you have a valid API key
  - Check your network connection
  - Verify the server URL is correct
  - Check the IntelliJ IDEA log for errors (Help → Show Log)

## Support

For support, please contact:
- Email: support@modforge.dev
- Website: https://www.modforge.dev

## License

This plugin is distributed under the terms of the Apache License 2.0.