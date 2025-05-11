#!/bin/bash

echo "===== ModForge IntelliJ Plugin Builder for 2025.1 ====="
echo

echo "Checking Java installation..."
if ! command -v java &> /dev/null; then
    echo "Java not found. Please install JDK 17 or higher."
    exit 1
fi

echo "Checking for local IntelliJ installation..."
# Check common installation paths
INTELLIJ_PATHS=(
    "/Applications/IntelliJ IDEA CE.app/Contents"
    "/opt/intellij-idea-community"
    "/usr/local/intellij-idea-community"
    "$HOME/Applications/IntelliJ IDEA CE.app/Contents"
    "$HOME/.local/share/JetBrains/IntelliJIdea2025.1"
)

USE_LOCAL=false
for path in "${INTELLIJ_PATHS[@]}"; do
    if [ -d "$path" ]; then
        INTELLIJ_PATH="$path"
        USE_LOCAL=true
        break
    fi
done

if [ "$USE_LOCAL" = false ]; then
    echo "IntelliJ IDEA 2025.1 not found at default locations."
    read -p "Please enter the path to IntelliJ IDEA 2025.1 installation (or leave empty to use repository version): " CUSTOM_PATH
    if [ -n "$CUSTOM_PATH" ] && [ -d "$CUSTOM_PATH" ]; then
        INTELLIJ_PATH="$CUSTOM_PATH"
        USE_LOCAL=true
    else
        echo "Will build using repository version."
    fi
fi

echo "Creating temporary build.gradle for 2025.1 compatibility..."
TEMP_GRADLE="build.gradle.temp"

cp build.gradle "$TEMP_GRADLE"

if [ "$USE_LOCAL" = true ]; then
    echo "Using local IntelliJ at: $INTELLIJ_PATH"
    # Modify build.gradle to use local IntelliJ
    if [[ "$OSTYPE" == "darwin"* ]]; then
        sed -i '' "s|// localPath = .*|localPath = \"$INTELLIJ_PATH\"; // Using local IntelliJ|" "$TEMP_GRADLE"
        sed -i '' "s|version = .*|// version = \"2023.3.6\"; // Commented out to use localPath instead|" "$TEMP_GRADLE"
    else
        sed -i "s|// localPath = .*|localPath = \"$INTELLIJ_PATH\"; // Using local IntelliJ|" "$TEMP_GRADLE"
        sed -i "s|version = .*|// version = \"2023.3.6\"; // Commented out to use localPath instead|" "$TEMP_GRADLE"
    fi
else
    echo "Using IntelliJ from repository."
fi

echo "Backing up original build.gradle..."
cp build.gradle build.gradle.bak

echo "Applying temporary build file..."
cp "$TEMP_GRADLE" build.gradle

echo "Building plugin..."
./gradlew clean buildPlugin --stacktrace

echo "Restoring original build.gradle..."
cp build.gradle.bak build.gradle
rm build.gradle.bak
rm "$TEMP_GRADLE"

echo
if [ -f "build/distributions/modforge-intellij-plugin-2.1.0.zip" ]; then
    echo "BUILD SUCCESSFUL!"
    echo "Plugin is available at: build/distributions/modforge-intellij-plugin-2.1.0.zip"
else
    echo "BUILD FAILED!"
    echo "Please check the error messages above."
fi

echo
echo "===== Build process completed ====="
