# ModForge IntelliJ IDEA Plugin

IntelliJ IDEA plugin for ModForge, an AI-powered development platform for Minecraft mods.

## Features

- Connect to the ModForge server for AI-assisted mod development
- Automatically fix compilation errors using AI
- Generate code, documentation, and explanations
- Continuous development mode that automatically improves your mod
- GitHub integration for seamless version control

## Authentication System

The plugin uses a robust token-based authentication system, which provides:

- Secure and persistent connections to the ModForge server
- Token-based authentication for API requests
- Automatic authentication verification
- Session persistence between IDE restarts

### Authentication Flow

1. User logs in with username and password
2. Server generates a JWT token with a 30-day expiry
3. Token is stored securely in the plugin settings
4. All API requests use the token for authentication
5. Token is automatically verified on IDE startup

## Installation

1. Install the plugin from the JetBrains Marketplace
2. Open IntelliJ IDEA settings and navigate to **Tools > ModForge**
3. Enter your ModForge server URL and credentials
4. Click **Test Connection** to verify the connection

## Usage

After authenticating, you can access ModForge features from:

1. **Tools > ModForge** menu
2. The ModForge toolbar
3. The ModForge tool window
4. Right-click in the editor for context-specific options

## Development and Testing

The plugin includes several tools for testing authentication:

- **Verify Authentication**: Simple check that authentication is working
- **Test Token Authentication**: Comprehensive testing of token-based requests
- **Test Authentication Endpoints**: Tests all authentication endpoints
- **Test Complete Auth Flow**: Tests the entire authentication flow from login to using the token

## Requirements

- IntelliJ IDEA 2021.3 or higher
- Java 11 or higher
- Internet connection to the ModForge server

## License

This plugin is licensed under the [MIT License](LICENSE).