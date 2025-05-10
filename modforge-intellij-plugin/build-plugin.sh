#!/bin/bash

# ModForge IntelliJ Plugin Build Script
# This script automates the building of the ModForge IntelliJ plugin

echo "=== Building ModForge IntelliJ Plugin ==="
echo "Target compatibility: IntelliJ IDEA 2025.1 (Build #IC-251.23774.435)"
echo

# Check for JDK 21
if type -p java >/dev/null; then
    _java=java
elif [[ -n "$JAVA_HOME" ]] && [[ -x "$JAVA_HOME/bin/java" ]]; then
    _java="$JAVA_HOME/bin/java"
else
    echo "ERROR: Java is not installed or JAVA_HOME is not set properly."
    echo "Please install JDK 21 or later."
    exit 1
fi

JAVA_VERSION=$("$_java" -version 2>&1 | awk -F '"' '/version/ {print $2}')
echo "Using Java version: $JAVA_VERSION"

# Check if Java version is at least 21
if [[ "$JAVA_VERSION" < "21" ]]; then
    echo "ERROR: Java 21 or higher is required to build the plugin."
    echo "Current version: $JAVA_VERSION"
    echo "Please install JDK 21 or later."
    exit 1
fi

# Clean any previous build
echo "Cleaning previous builds..."
./gradlew clean

# Build the plugin
echo "Building plugin..."
./gradlew buildPlugin

# Check if build was successful
if [ $? -eq 0 ]; then
    echo
    echo "Build successful!"
    
    # Find the built plugin file
    PLUGIN_ZIP=$(find build/distributions -name "*.zip" | head -n 1)
    
    if [ -n "$PLUGIN_ZIP" ]; then
        echo "Plugin file: $PLUGIN_ZIP"
        echo
        echo "To install the plugin:"
        echo "1. Open IntelliJ IDEA 2025.1"
        echo "2. Go to Settings/Preferences → Plugins"
        echo "3. Click the gear icon → Install Plugin from Disk..."
        echo "4. Select the zip file above"
        echo "5. Restart IntelliJ IDEA when prompted"
    else
        echo "Warning: Plugin file not found. Check the build logs for errors."
    fi
else
    echo
    echo "Build failed. Check the logs above for errors."
    exit 1
fi

echo
echo "=== Build Process Complete ==="