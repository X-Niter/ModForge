#!/bin/bash

# Build script for ModForge IntelliJ Plugin
# This script builds the plugin and outputs the location of the generated plugin file

echo "Building ModForge IntelliJ Plugin..."
echo "======================================"

# Navigate to the plugin directory
cd "$(dirname "$0")"

# Ensure gradlew is executable
chmod +x ./gradlew

# Clean and build the plugin
./gradlew clean build

# Check if build was successful
if [ $? -eq 0 ]; then
    echo "======================================"
    echo "Build successful!"
    
    # Find the generated plugin file
    PLUGIN_FILE=$(find ./build/distributions -name "*.zip" | head -n 1)
    
    if [ -n "$PLUGIN_FILE" ]; then
        echo "Plugin file created at: $PLUGIN_FILE"
        echo ""
        echo "To install in IntelliJ IDEA:"
        echo "1. Open IntelliJ IDEA"
        echo "2. Go to Settings/Preferences → Plugins"
        echo "3. Click the gear icon → Install Plugin from Disk..."
        echo "4. Select the generated plugin file"
        echo "5. Restart IntelliJ IDEA when prompted"
    else
        echo "Could not find generated plugin file. Check build output for errors."
    fi
else
    echo "======================================"
    echo "Build failed! Check the output above for errors."
fi