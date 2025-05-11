#!/bin/bash

echo "=== Building ModForge IntelliJ Plugin ==="
echo "Java version:"
java -version

# Verify JAVA_HOME is set to Java 21
if [[ -n "$JAVA_HOME" ]]; then
    echo "JAVA_HOME is set to: $JAVA_HOME"
else
    echo "WARNING: JAVA_HOME is not set. This may cause build issues."
fi

# Clean the build directory first
echo "Cleaning previous builds..."
./gradlew clean

# Build with debug information
echo "Building plugin with debug information..."
./gradlew buildPlugin --info

# Check if build was successful
if [ $? -eq 0 ]; then
    echo "=== Build completed successfully ==="
    echo "Plugin ZIP file location:"
    find ./build/distributions -name "*.zip" -type f
else
    echo "=== Build failed ==="
    echo "Check the error messages above for details."
    
    # Provide troubleshooting guidance
    echo ""
    echo "=== Troubleshooting Guide ==="
    echo "1. Check if you have IntelliJ IDEA 2025.1 installed locally."
    echo "2. Make sure the localPath in build.gradle points to your IntelliJ installation:"
    echo "   - Windows: C:/Program Files/JetBrains/IntelliJ IDEA Community Edition 2025.1"
    echo "   - macOS: /Applications/IntelliJ IDEA.app/Contents"
    echo "   - Linux: /opt/intellij-idea-community"
    echo "3. Make sure Java 21 is being used (java -version)"
    echo "4. If you get 'Both intellij.localPath and intellij.version are specified' error:"
    echo "   - Edit build.gradle and comment out either localPath OR version (not both)"
    echo "5. If you get 'git4idea' plugin errors:"
    echo "   a. Open build.gradle and ensure correct plugin IDs are used"
    echo "   b. Install IntelliJ IDEA 2025.1 locally and point to it using localPath"
    echo ""
    echo "For more help, see: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html"
    
    exit 1
fi