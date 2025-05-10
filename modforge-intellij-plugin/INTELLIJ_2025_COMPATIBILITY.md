# ModForge IntelliJ Plugin - 2025.1 Compatibility Guide

This document details the compatibility of the ModForge plugin with IntelliJ IDEA 2025.1, including implementation details, requirements, and troubleshooting steps.

## Compatibility Information

The ModForge plugin has been specifically optimized and tested for IntelliJ IDEA 2025.1 with the following configurations:

| Component | Requirement |
|-----------|-------------|
| IntelliJ IDEA | 2025.1 (Build #IC-251.23774.435 or later) |
| Java Runtime | JDK 21.0.6+9-b895.109 or newer |
| Operating System | Windows 10/11, macOS 12+, Linux (Ubuntu 22.04+) |
| Memory | 2GB minimum (4GB recommended) |
| Plugin API | 2025.1 compatible |

## Implementation Details

The plugin has been updated to use the latest IntelliJ APIs and patterns, including:

- Enhanced Environment Validation for robust version detection
- Support for Virtual Threads (Java 21 feature)
- New Project Model API compatibility
- Improved GitHub integration with smart retry logic
- Enhanced error recovery and circuit breaker pattern
- Updated UI components for seamless integration with 2025.1 themes

## Installation Instructions

### Direct Installation
1. Download the plugin package (`ModForge-2.1.0.zip`) from the plugin repository
2. In IntelliJ IDEA, go to **File → Settings → Plugins → ⚙️ → Install Plugin from Disk**
3. Select the downloaded zip file and click **OK**
4. Restart IntelliJ IDEA when prompted

### From Plugin Marketplace
1. In IntelliJ IDEA, go to **File → Settings → Plugins**
2. Click on **Marketplace** and search for "ModForge"
3. Click **Install** next to the ModForge plugin
4. Restart IntelliJ IDEA when prompted

## Verification

After installation, the plugin will automatically validate your environment. If any compatibility issues are detected, you will receive a notification with detailed information.

To manually verify compatibility:
1. Go to **Help → About**
2. Confirm your IntelliJ IDEA version is 2025.1 or newer
3. Go to **File → Project Structure → SDKs**
4. Verify that a JDK 21 is configured and set as the project SDK

## Troubleshooting

### Java Version Issues
- **Symptom**: Warning about incompatible Java version
- **Solution**: 
  1. Download JDK 21.0.6 or newer from [Oracle](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html) or [Eclipse Adoptium](https://adoptium.net/temurin/releases/?version=21)
  2. Install the JDK and configure it in IntelliJ IDEA
  3. Restart IntelliJ IDEA and the plugin

### IntelliJ Version Issues
- **Symptom**: Warning about incompatible IntelliJ version
- **Solution**: Update to IntelliJ IDEA 2025.1 or newer

### GitHub Integration Issues
- **Symptom**: GitHub operations fail or time out
- **Solution**:
  1. Check your network connection
  2. Verify your GitHub token has the necessary permissions
  3. Try the operation again (the plugin includes automatic retry with exponential backoff)
  4. If problems persist, try revoking and generating a new GitHub token

### Plugin Not Loading
- **Symptom**: Plugin doesn't appear in the Tools menu
- **Solution**:
  1. Go to **File → Settings → Plugins**
  2. Check if ModForge is listed and enabled
  3. If not found, try reinstalling
  4. Check the IDE log for any plugin loading errors (**Help → Show Log in Explorer/Finder**)

## Advanced Configuration

The plugin supports several advanced configuration options:

### Custom GitHub API Endpoint
For users with GitHub Enterprise or self-hosted instances, you can configure a custom API endpoint:
1. Go to **File → Settings → Tools → ModForge Settings**
2. Enter your custom GitHub API URL
3. Click **Apply**

### Multiple GitHub Accounts
The plugin supports authentication with multiple GitHub accounts:
1. Go to **File → Settings → Tools → ModForge Settings → GitHub Accounts**
2. Click the **+** button to add a new account
3. Configure the account name and token
4. Click **Apply**

### Performance Tuning
For large projects or slower machines:
1. Go to **File → Settings → Tools → ModForge Settings → Performance**
2. Adjust the concurrent operations limit and timeout values
3. Click **Apply**

## Support and Feedback

If you encounter any issues or have suggestions for improving the plugin's compatibility with IntelliJ IDEA 2025.1, please:

1. Check our [GitHub repository](https://github.com/modforge/intellij-plugin/issues) for known issues
2. Submit a detailed bug report if your issue isn't already reported
3. Join our [Discord community](https://discord.gg/modforge) for direct support

## Release Notes

### Version 2.1.0
- Added full compatibility with IntelliJ IDEA 2025.1
- Enhanced JDK 21 detection and validation
- Improved GitHub integration with intelligent retry mechanism
- Fixed disk space monitoring on all operating systems
- Added comprehensive error handling and recovery
- Enhanced logging for better troubleshooting
- Optimized performance for large Minecraft mod projects
- Added support for Java 21 virtual threads
- Updated UI components for compatibility with 2025.1 themes

---

Last updated: May 10, 2025