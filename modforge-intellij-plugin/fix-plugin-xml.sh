#!/bin/bash

# Script to fix the plugin.xml file
# This fixes the issue where <name> appears as <n> in the plugin.xml file

# Backup the original file
cp src/main/resources/META-INF/plugin.xml src/main/resources/META-INF/plugin.xml.bak

# Copy the fixed file
cp src/main/resources/META-INF/fixed-plugin.xml src/main/resources/META-INF/plugin.xml

echo "plugin.xml file has been fixed."