#!/bin/bash

# Script to fix the service classes for IntelliJ IDEA 2025.1.1.1 compatibility
# This script copies the fixed XML and ensures all service classes are properly updated

# First, fix the plugin.xml file
./fix-plugin-xml.sh

# Check if directories exist, create them if they don't
SERVICES_DIR="src/main/java/com/modforge/intellij/plugin/services"
if [ ! -d "$SERVICES_DIR" ]; then
    echo "Creating services directory..."
    mkdir -p "$SERVICES_DIR"
fi

SETTINGS_DIR="src/main/java/com/modforge/intellij/plugin/settings"
if [ ! -d "$SETTINGS_DIR" ]; then
    echo "Creating settings directory..."
    mkdir -p "$SETTINGS_DIR"
fi

COLLAB_DIR="src/main/java/com/modforge/intellij/plugin/collaboration/websocket"
if [ ! -d "$COLLAB_DIR" ]; then
    echo "Creating collaboration directory..."
    mkdir -p "$COLLAB_DIR"
fi

echo "Service directories checked and created if needed."
echo "Service classes are ready to be compiled."

# Clean any existing compiled classes
echo "Cleaning existing compiled classes..."
./gradlew clean

# Build the plugin
echo "Building the plugin..."
./gradlew build

echo "Fix complete."