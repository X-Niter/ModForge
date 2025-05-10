# IntelliJ IDEA 2025.1 Compatibility

This document outlines the changes made to ensure compatibility with IntelliJ IDEA 2025.1 (Build #IC-251.23774.435).

## System Requirements

- IntelliJ IDEA 2025.1 (Build #IC-251.23774.435) or compatible version
- Java Development Kit (JDK) 21.0.6 or later (compatible with JBR runtime 21.0.6+9-b895.109)
- Minecraft Development plugin (v2025.1-1.8.4)

## Key Compatibility Changes

### Java 21 Support

- Updated Java language level to 21 (from 17)
- Utilized Virtual Threads (Java 21 feature) for improved performance
- Updated build.gradle to specify Java 21 compatibility
- Added compatibility validation to check for Java 21 runtime

### IntelliJ API Updates

- Replaced deprecated `AppExecutorUtil` calls with Java 21 thread factories
- Updated notification handling for IntelliJ 2025.1 compatibility
- Fixed Maven repositories and dependency versions for 2025.1
- Added startup validation to check IDE version compatibility
- Updated plugin.xml to support 2025.1 API changes

### Minecraft Support

- Updated GitHub Actions workflow generation to use JDK 21
- Updated build scripts for compatibility with latest Minecraft versions
- Added support for Minecraft Development plugin v2025.1-1.8.4

### Reliability Improvements

- Added retry logic with exponential backoff for network operations
- Implemented circuit breaker pattern to prevent cascading failures
- Enhanced error handling with detailed error messages
- Added validation to ensure safe operation in unsupported environments

## Testing and Validation

For plugin validation, the following tests should be performed:

1. **Installation Test**: Verify that the plugin installs without errors in IntelliJ IDEA 2025.1
2. **Startup Test**: Confirm the plugin loads correctly without exceptions on IDE startup
3. **Compatibility Validation**: Check that the plugin's compatibility validator correctly identifies the IDE version
4. **Thread Usage**: Verify that virtual threads are being used correctly for background operations
5. **API Integration**: Test that all IntelliJ API integrations function as expected
6. **Error Handling**: Validate that network failures and API errors are handled gracefully

## Known Limitations

- The plugin must be installed in IntelliJ IDEA 2025.1 or newer; it will not work in older versions
- Java 21 features are required; the plugin will not function correctly with older JDK versions
- If running in an incompatible environment, the plugin will display a warning notification upon startup

## Required Dependencies

In addition to the core IntelliJ platform, the plugin requires:

- Java module
- Git4Idea plugin
- GitHub plugin
- Minecraft Development plugin

These dependencies are specified in both the plugin.xml and build.gradle files.