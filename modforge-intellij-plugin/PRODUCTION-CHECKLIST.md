# ModForge IntelliJ Plugin - Production Checklist

Use this checklist to ensure your ModForge IntelliJ Plugin is properly configured for production use.

## 🔍 Pre-Production Checklist

### Configuration
- [ ] Server URL is properly configured in settings
- [ ] OpenAI API key is set in ModForge settings
- [ ] GitHub token is set (if using GitHub integration)
- [ ] All connection settings have been tested
- [ ] Proper error logging is enabled

### Build Configuration
- [ ] Plugin version is set correctly in `build.gradle`
- [ ] Plugin compatibility range is set (currently 2023.2 - 2024.1)
- [ ] All dependencies are up-to-date
- [ ] Plugin description and change notes are accurate

### Features
- [ ] All mod loaders are properly detected (Forge, Fabric, Quilt, Architectury)
- [ ] GitHub integration works properly
- [ ] Code generation and error fixing works
- [ ] Continuous development features work
- [ ] Pattern recognition for API cost optimization works

## 📋 Production Deployment Steps

1. **Build the Plugin**
   - Run `./build-plugin.sh` from the plugin directory
   - Verify the plugin zip file was created successfully

2. **Install the Plugin in IntelliJ IDEA**
   - Open IntelliJ IDEA
   - Go to Settings/Preferences → Plugins
   - Click the gear icon → Install Plugin from Disk...
   - Select the zip file from the `build/distributions` directory
   - Restart IntelliJ IDEA when prompted

3. **Configure the Plugin**
   - Open Settings/Preferences → Tools → ModForge AI Settings
   - Enter your ModForge server URL
   - Enter your OpenAI API key
   - Enter your GitHub token (if using GitHub integration)
   - Click "Test Connection" to verify everything works

4. **Verify Features**
   - Create a new mod project to verify templates work
   - Test code generation with a simple prompt
   - Verify GitHub integration by pushing to a test repository
   - Try continuous development on a test project

## 🔄 Update Process

When updating the plugin, follow these steps:

1. **Update Dependencies**
   - Check for new versions of dependencies in `build.gradle`
   - Update to the latest compatible versions

2. **Update IntelliJ Compatibility**
   - Check the latest IntelliJ version supported
   - Update the compatibility range in `patchPluginXml` if needed

3. **Update ModForge Server Connection**
   - Ensure the plugin works with the latest ModForge server API

4. **Update Version Number**
   - Increment the version number in `build.gradle`
   - Update the change notes with new features and fixes

5. **Test Thoroughly**
   - Test all features with all supported mod loaders
   - Verify GitHub integration works
   - Check error handling and recovery

6. **Rebuild and Deploy**
   - Run `./build-plugin.sh` to create the updated plugin
   - Install in IntelliJ IDEA for final testing
   - Distribute to users

## 🚨 Troubleshooting

If you encounter issues:

1. **Check Logs**
   - Look in the IntelliJ IDEA log (Help → Show Log)
   - Check for errors related to ModForge

2. **Verify Settings**
   - Make sure all settings are correctly configured
   - Test the connection to the ModForge server

3. **Clear Caches**
   - Try File → Invalidate Caches / Restart...
   - Restart IntelliJ IDEA

4. **Reinstall Plugin**
   - Uninstall and reinstall the plugin
   - Verify you have the latest version