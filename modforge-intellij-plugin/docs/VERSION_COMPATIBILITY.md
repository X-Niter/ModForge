# ModForge IntelliJ Plugin: Version Compatibility Matrix

This document provides detailed compatibility information for different versions of the ModForge IntelliJ Plugin with various versions of IntelliJ IDEA and Java Development Kit (JDK).

## Compatibility Matrix

| ModForge Plugin | IntelliJ IDEA      | Java JDK        | Minecraft Dev Plugin | Notes                           |
|-----------------|--------------------|-----------------|-----------------------|---------------------------------|
| 2.1.0           | 2025.1 (251.23774+)| JDK 21.0.6+     | 2025.1-1.8.4+        | Current release, recommended    |
| 2.0.x           | 2024.3 (243.xxxx+) | JDK 21          | 2024.3-1.8.x+        | Legacy - not actively maintained|
| 1.9.x           | 2024.2             | JDK 17+         | 2024.2-1.7.x+        | Legacy - not supported          |
| 1.8.x           | 2024.1             | JDK 17+         | 2024.1-1.7.x+        | Legacy - not supported          |
| 1.7.x           | 2023.3             | JDK 17          | 2023.3-1.6.x+        | Legacy - not supported          |

## IntelliJ IDEA 2025.1 Compatibility

ModForge Plugin version 2.1.0 is fully compatible with IntelliJ IDEA 2025.1 (Build #IC-251.23774.435) and has been specifically optimized for this version. Key features:

- Full support for Java 21 features including virtual threads
- Optimized for IntelliJ IDEA 2025.1 APIs
- Enhanced thread management and performance
- Compatible with the latest Minecraft Development plugin

## Java Version Requirements

- **JDK 21.0.6 or later**: Required for ModForge Plugin version 2.1.0
  - Support for virtual threads (used extensively by the plugin)
  - Enhanced HTTP client APIs
  - Improved performance and stability

If you attempt to run the plugin with an older Java version, you will see warnings or errors in the IDE log, and some features may not function correctly.

## Minecraft Development Plugin Compatibility

The ModForge IntelliJ Plugin depends on the Minecraft Development plugin. The table below shows compatible versions:

| ModForge Plugin | Minecraft Development Plugin Versions |
|-----------------|--------------------------------------|
| 2.1.0           | 2025.1-1.8.4 or later                |
| 2.0.x           | 2024.3-1.8.0 through 2025.1-1.8.3    |

We recommend always using the latest version of the Minecraft Development plugin that matches your IntelliJ IDEA version.

## Upgrading from Previous Versions

When upgrading from previous versions of the ModForge Plugin, consider the following:

### Upgrading from 2.0.x to 2.1.0

- **Prerequisites**: Upgrade to IntelliJ IDEA 2025.1 and JDK 21.0.6+ first
- **Settings Migration**: Settings will be automatically migrated
- **Project Compatibility**: Existing projects should continue to work without modifications

### Upgrading from 1.x to 2.1.0

- **Major Changes**: This is a significant upgrade with many internal changes
- **Settings Migration**: Some settings may need to be reconfigured
- **Project Compatibility**: Projects should be reviewed for compatibility

## Downgrading

If you need to downgrade to a previous version of the plugin:

1. Uninstall the current version: **File → Settings → Plugins**, find ModForge AI, click the gear icon, and select "Uninstall"
2. Download the appropriate version for your IntelliJ IDEA and Java environment
3. Install the downloaded version: **File → Settings → Plugins → ⚙️ → Install Plugin from Disk...**
4. Restart IntelliJ IDEA

## Troubleshooting Version Issues

If you encounter compatibility issues:

1. Verify that your IntelliJ IDEA version matches the requirements for your plugin version
2. Ensure your Java JDK version meets the minimum requirements
3. Check that you have the correct version of the Minecraft Development plugin
4. Look for version-related errors in the IntelliJ IDEA log (**Help → Show Log in Explorer/Finder**)

---

Last updated: May 10, 2025