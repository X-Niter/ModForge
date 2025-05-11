#!/bin/bash

# Script to fix the plugin.xml file for IntelliJ IDEA 2025.1.1.1 compatibility
# This script replaces the <n> tag with <name> tag and adds services

SOURCE_FILE="src/main/resources/META-INF/fixed-plugin.xml"
TARGET_FILE="src/main/resources/META-INF/plugin.xml"

if [ ! -f "$SOURCE_FILE" ]; then
    echo "Error: Source file $SOURCE_FILE does not exist."
    exit 1
fi

echo "Copying fixed plugin.xml..."
cp "$SOURCE_FILE" "$TARGET_FILE"

if [ $? -eq 0 ]; then
    echo "Successfully replaced plugin.xml with fixed version."
else
    echo "Error: Failed to replace plugin.xml."
    exit 1
fi

echo "Fix complete."