# ModForge IntelliJ Plugin: Installation Verification

Follow these steps to verify that your ModForge IntelliJ Plugin is correctly installed and configured.

## Step 1: Verify Plugin Installation

1. Open IntelliJ IDEA
2. Go to **File → Settings → Plugins**
3. Check the "Installed" tab
4. Confirm that "ModForge AI" is listed and enabled
5. The plugin version should be "2.1.0" or higher

## Step 2: Verify Java Version

1. Go to **File → Project Structure → Project**
2. Confirm that "Project SDK" is set to Java 21 or higher
3. Confirm that "Language level" is at least "21 - Virtual threads..."

## Step 3: Check IDE Version

1. Go to **Help → About**
2. Verify that you are using IntelliJ IDEA 2025.1 (Build #IC-251.23774.435) or newer

## Step 4: Verify Plugin Functionality

### Test the ModForge Tool Window

1. Look for the ModForge icon in the right toolbar (vertical tab)
2. Click it to open the ModForge tool window
3. The window should open without errors

### Test API Configuration

1. Go to **File → Settings → Tools → ModForge AI Settings**
2. Enter your OpenAI API key
3. Click "Test Connection"
4. You should see a success message

### Test Code Generation

1. Create or open a Java file in a Minecraft mod project
2. Right-click in the editor and select "Generate Code with ModForge AI"
3. Enter a simple prompt (e.g., "Create a basic item class")
4. Click "Generate"
5. The code generation dialog should appear with generated code

## Step 5: Collect Diagnostic Information (If Needed)

If you encounter any issues during verification, collect the following diagnostic information:

### System Information

1. Go to **Help → About**
2. Click "Copy" to copy system information to clipboard
3. Paste this information in your issue report

### Plugin Logs

1. Go to **Help → Show Log in Explorer/Finder**
2. Look for entries related to "ModForge" in the log file
3. Attach relevant log sections to your issue report

### IDE Diagnostic Info

1. Go to **Help → Diagnostic Tools → Debug Log Settings**
2. Add `#com.modforge.intellij.plugin` to the end of the log configuration
3. Click "OK"
4. Restart IntelliJ IDEA
5. Reproduce the issue
6. Go back to the log file, which will now contain detailed plugin logs

## Troubleshooting Common Issues

### Plugin Not Appearing

- Verify that you've restarted IntelliJ IDEA after installation
- Check if the plugin is compatible with your IntelliJ IDEA version
- Try reinstalling the plugin

### API Connection Issues

- Verify your internet connection
- Check that your API key is correct
- Ensure you don't have a firewall or proxy blocking connections

### UI Not Rendering Correctly

- Try invalidating caches (**File → Invalidate Caches**)
- Restart IntelliJ IDEA
- Try updating IntelliJ IDEA to the latest version

### Java Compatibility Issues

- Ensure you are using JDK 21.0.6 or later
- Set both your Project SDK and Module SDK to use Java 21

---

If you continue to experience issues after following these steps, please contact support@modforge.dev with your diagnostic information.