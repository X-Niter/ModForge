# ModForge IntelliJ Plugin Production Deployment Checklist

This checklist helps ensure that the ModForge IntelliJ Plugin is properly validated and ready for production deployment. Follow these steps to verify that all critical components are functioning correctly before distributing the plugin to users.

## System Requirements Verification

- [ ] Java JDK 21.0.6 or newer is installed and set as the project SDK
- [ ] IntelliJ IDEA 2025.1 (Build #IC-251.23774.435) or compatible version is being used
- [ ] Gradle version 8.6 or newer is being used
- [ ] All required dependencies are resolved without conflicts

## Build Verification

- [ ] Project compiles successfully without errors
- [ ] All unit tests pass successfully
- [ ] The plugin builds successfully with `./gradlew buildPlugin`
- [ ] The plugin validation task completes successfully with `./gradlew validatePluginForProduction`
- [ ] Plugin ZIP artifact is created in the `build/distributions` directory

## Feature Validation

- [ ] ModForge tool window appears correctly in the IDE
- [ ] GitHub authentication works correctly with tokens 
- [ ] Compatibility validation runs at startup and displays appropriate warnings if needed
- [ ] Project creation templates generate valid Minecraft mod projects
- [ ] AI code generation feature works with proper API key configuration
- [ ] Continuous development features operate without errors
- [ ] GitHub workflow generation produces valid workflow files

## Performance and Reliability

- [ ] Plugin startup time is reasonable (under 5 seconds)
- [ ] Background operations use virtual threads properly
- [ ] Network operations include proper retry mechanisms
- [ ] Circuit breakers prevent cascading failures on API errors
- [ ] Memory usage is stable during extended usage sessions
- [ ] No UI freezes occur during network operations

## Security

- [ ] All API keys and sensitive information are stored securely
- [ ] No credentials are stored in plaintext
- [ ] Network connections use HTTPS
- [ ] OAuth token handling follows security best practices
- [ ] No sensitive information is logged or exposed in error messages

## Documentation and Metadata

- [ ] Plugin metadata in `plugin.xml` is up-to-date and accurate
- [ ] Version number follows semantic versioning guidelines (MAJOR.MINOR.PATCH)
- [ ] README contains up-to-date installation and usage instructions
- [ ] Change notes in `patchPluginXml` accurately reflect the latest changes
- [ ] Support contact information is current and valid

## Distribution Preparation

- [ ] License information is accurate and included in the distribution
- [ ] Third-party licenses are acknowledged appropriately
- [ ] Plugin icon is provided in appropriate resolutions
- [ ] Screenshots for marketplace listing are prepared and up-to-date
- [ ] Plugin description for marketplace is prepared and compelling
- [ ] Final plugin ZIP file has been inspected to ensure only required files are included

## Post-Deployment Verification

- [ ] Plugin installs successfully from ZIP file in a clean IntelliJ IDEA instance
- [ ] Plugin appears correctly in the installed plugins list
- [ ] No errors appear in the IDE log after installation
- [ ] All features function correctly in the installed version
- [ ] Plugin can be uninstalled cleanly without errors or debris

---

## Troubleshooting Common Issues

### Plugin Fails to Load

- Check IDE compatibility range in plugin.xml
- Verify all dependencies are properly declared
- Check IDE logs for detailed error messages
- Ensure Java 21 compatibility is maintained

### GitHub Integration Issues

- Verify token validity and permissions
- Check network connectivity to GitHub
- Examine logs for specific API error responses
- Verify circuit breaker status for operations

### Memory and Performance Issues

- Check for resource leaks in long-running services
- Verify thread management and proper executor shutdown
- Examine heap usage patterns during extended operation
- Ensure virtual threads are being used appropriately

---

Use this checklist before each production release to ensure a high-quality, reliable plugin for users.