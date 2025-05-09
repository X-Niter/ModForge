#!/bin/bash

# This script builds the ModForge IntelliJ plugin and prepares it for installation

# Determine if we have Java 17 available
JAVA_CMD=""

# Check if a JDK 17 is available using alternatives
if command -v update-alternatives &> /dev/null; then
    JDK_PATH=$(update-alternatives --list java 2>/dev/null | grep -i "java-17" | head -n 1)
    if [ ! -z "$JDK_PATH" ]; then
        JAVA_CMD="$JDK_PATH"
        echo "Found JDK 17 via alternatives: $JAVA_CMD"
    fi
fi

# Check for JAVA_HOME
if [ -z "$JAVA_CMD" ] && [ ! -z "$JAVA_HOME" ]; then
    if [ -f "$JAVA_HOME/bin/java" ]; then
        JAVA_CMD="$JAVA_HOME/bin/java"
        echo "Using Java from JAVA_HOME: $JAVA_CMD"
    fi
fi

# Check PATH for java
if [ -z "$JAVA_CMD" ] && command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -ge 17 ]; then
        JAVA_CMD="java"
        echo "Using Java from PATH: $(which java), version: $JAVA_VERSION"
    fi
fi

# If we still don't have Java, try to find it
if [ -z "$JAVA_CMD" ]; then
    echo "Java 17 not found in standard locations. Searching..."
    # Try to find Java in common locations
    POSSIBLE_JAVA_HOMES=(
        "/usr/lib/jvm/java-17-openjdk"
        "/usr/lib/jvm/java-17-openjdk-amd64"
        "/usr/lib/jvm/java-17-oracle"
        "/usr/lib/jvm/jdk-17"
        "/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home"
        "/opt/jdk-17"
        "$HOME/.sdkman/candidates/java/17.0.7-tem"
        "/nix/store/*/jdk-17"
    )

    for JH in "${POSSIBLE_JAVA_HOMES[@]}"; do
        if [ -d "$JH" ] && [ -f "$JH/bin/java" ]; then
            JAVA_HOME="$JH"
            JAVA_CMD="$JH/bin/java"
            echo "Found Java 17 at: $JAVA_HOME"
            break
        fi
    done
fi

# If we still don't have Java, error out
if [ -z "$JAVA_CMD" ]; then
    echo "Error: Could not find Java 17. Please install Java 17 or set JAVA_HOME."
    exit 1
fi

# Export JAVA_HOME for Gradle to use
if [ -z "$JAVA_HOME" ] && [ ! -z "$JAVA_CMD" ]; then
    if [[ "$JAVA_CMD" == */bin/java ]]; then
        JAVA_HOME="${JAVA_CMD%/bin/java}"
    fi
fi

# Export JAVA_HOME for Gradle
export JAVA_HOME

echo "Using JAVA_HOME: $JAVA_HOME"
echo "Building plugin..."

# Add execute permissions to gradlew
chmod +x gradlew

# Build plugin
./gradlew clean buildPlugin

# Check if build was successful
if [ $? -eq 0 ]; then
    PLUGIN_FILE=$(find build/distributions -name "*.zip" | head -n 1)
    if [ ! -z "$PLUGIN_FILE" ]; then
        echo ""
        echo "Build successful! Plugin file created at: $PLUGIN_FILE"
        echo ""
        echo "Installation instructions:"
        echo "1. Open IntelliJ IDEA"
        echo "2. Go to File → Settings → Plugins"
        echo "3. Click the gear icon and select 'Install Plugin from Disk...'"
        echo "4. Navigate to and select the file: $PLUGIN_FILE"
        echo "5. Restart IntelliJ IDEA when prompted"
        echo ""
    else
        echo "Build completed, but could not find plugin ZIP file in build/distributions"
    fi
else
    echo "Build failed."
fi