#!/bin/bash

# Production build script for ModForge IntelliJ Plugin
# This script builds a production-ready plugin ZIP file for installation in IntelliJ IDEA 2025.1

set -e

# Display banner
echo "====================================================="
echo "      ModForge IntelliJ Plugin Production Build      "
echo "====================================================="
echo "Building plugin for IntelliJ IDEA 2025.1 with Java 21"
echo "====================================================="

# Check Java version
java_version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
echo "Using Java version: $java_version"

if [[ "$java_version" != 21* ]] && [[ "$java_version" != 21.* ]]; then
    echo "ERROR: Java 21 is required for building the plugin."
    echo "Current Java version: $java_version"
    echo "Please set JAVA_HOME to point to a JDK 21 installation."
    exit 1
fi

echo "✓ Java version check passed"

# Check Gradle
if ! command -v ./gradlew &> /dev/null; then
    echo "ERROR: Gradle wrapper not found."
    echo "Please run this script from the root of the ModForge IntelliJ plugin directory."
    exit 1
fi

echo "✓ Gradle wrapper check passed"

# Clean previous build artifacts
echo "Cleaning previous build artifacts..."
./gradlew clean
echo "✓ Clean completed"

# Run tests
echo "Running tests..."
./gradlew test
if [ $? -ne 0 ]; then
    echo "⚠️ Tests failed. Do you want to continue anyway? (y/n)"
    read -r continue_build
    if [[ ! "$continue_build" =~ ^[Yy]$ ]]; then
        echo "Build aborted."
        exit 1
    fi
    echo "Continuing build despite test failures."
else
    echo "✓ Tests passed"
fi

# Run validation
echo "Validating plugin..."
./gradlew validatePluginForProduction
echo "✓ Validation passed"

# Build the plugin
echo "Building plugin..."
./gradlew buildPlugin
echo "✓ Build completed"

# Check for the plugin zip
plugin_zip=$(find build/distributions -name "*.zip" | head -n 1)
if [ -z "$plugin_zip" ]; then
    echo "ERROR: Plugin ZIP file not found after build."
    exit 1
fi

# Create a distribution directory
mkdir -p dist
cp "$plugin_zip" dist/
latest_zip=$(basename "$plugin_zip")

echo "====================================================="
echo "✅ Build successful!"
echo "Plugin ZIP file created: $(pwd)/dist/$latest_zip"
echo ""
echo "Installation Instructions:"
echo "1. Open IntelliJ IDEA 2025.1"
echo "2. Go to File > Settings > Plugins"
echo "3. Click the gear icon and select 'Install Plugin from Disk...'"
echo "4. Select the plugin ZIP file from dist/$latest_zip"
echo "5. Restart IntelliJ IDEA when prompted"
echo "====================================================="