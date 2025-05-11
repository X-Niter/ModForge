# Building ModForge IntelliJ Plugin for 2025.1.1.1

This guide provides detailed instructions for building the ModForge IntelliJ plugin specifically for IntelliJ IDEA 2025.1.1.1.

## Prerequisites

- JDK 17 or later (JDK 21 recommended)
- Git
- IntelliJ IDEA 2025.1.1.1 (for local build path option)

## Build Options

### Option 1: Use the Automated Build Scripts (Recommended)

#### For Windows:
```powershell
.\build-plugin-2025.1.1.1.ps1
```

#### For Linux/Mac:
```bash
./build-for-2025.1.sh
```

These scripts will:
1. Detect your environment
2. Allow you to choose between building with a local IntelliJ installation or using the repository version
3. Automatically adjust the build configuration
4. Build the plugin
5. Provide detailed error information if needed

### Option 2: Manual Build

#### Step 1: Choose Your Build Strategy

You have two main strategies:

1. **Build with repository version (easier):**
   - Uses IntelliJ IDEA 2023.3.6 from the JetBrains repository
   - Sets compatibility metadata for 2025.1.1.1
   - Just run `./gradlew buildPlugin`

2. **Build with local IntelliJ installation (more precise):**
   - Edit `build.gradle` to use your local IntelliJ IDEA 2025.1.1.1 installation
   - Comment out the `version = '2023.3.6'` line
   - Uncomment and modify the `localPath` line to point to your installation
   - Run `./gradlew buildPlugin`

#### Step 2: Execute the Build

```bash
./gradlew clean buildPlugin
```

Add `--info` or `--debug` for more detailed output if you encounter issues.

## Troubleshooting

### Common Issues

1. **GitHub plugin dependency errors:**
   - We've now made GitHub integration optional
   - The plugin will build successfully even without GitHub plugin
   - Functionality gracefully degrades when the plugin is missing

2. **Build errors with "Class not found":**
   - Try clearing Gradle caches: `./gradlew cleanBuildCache`
   - Make sure you're using JDK 17 or higher

3. **"Cannot determine version" errors:**
   - This usually means the build script can't find the local IntelliJ installation
   - Double-check your path when using the local installation option

4. **IDE compatibility warnings:**
   - These are expected when building with 2023.3.6 for 2025.1.1.1
   - The plugin.xml modifications ensure it works with 2025.1.1.1

### Testing Your Build

After building, the plugin will be available at `build/distributions/modforge-intellij-plugin-2.1.0.zip`.

To test:
1. Open IntelliJ IDEA 2025.1.1.1
2. Go to Settings → Plugins → ⚙ → Install Plugin from Disk...
3. Select the ZIP file
4. Restart IntelliJ when prompted

## Expert Mode: Advanced Building

For developers who need to customize the build process:

1. Examine `build.gradle` to understand the build configuration
2. Edit the IntelliJ IDEA version in `gradle.properties` if needed
3. Modify plugin dependencies in both `build.gradle` and `plugin.xml`
4. Run with additional flags:
   ```
   ./gradlew buildPlugin --stacktrace --debug
   ```

## Continuous Integration

The plugin is designed to be built in CI environments with:
```
./gradlew buildPlugin -PbuildNumber=$CI_BUILD_NUMBER
```
