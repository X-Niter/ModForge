#!/bin/bash

# Script to build the plugin with the fixed plugin.xml file

# Fix the plugin.xml file first
./fix-plugin-xml.sh

# Run Gradle build
./gradlew clean buildPlugin

echo "Build completed with fixed plugin.xml"