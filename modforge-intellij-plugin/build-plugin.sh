#!/bin/bash

echo "=== Building ModForge IntelliJ Plugin ==="
echo "Java version:"
java -version

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
    exit 1
fi