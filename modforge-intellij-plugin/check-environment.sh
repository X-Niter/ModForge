#!/bin/bash

# Environment checking script for ModForge IntelliJ Plugin
# This script verifies that your system meets the requirements for building the plugin

set -e

echo "====================================================="
echo "      ModForge IntelliJ Plugin Environment Check     "
echo "====================================================="
echo ""

# Check Java version
echo "Checking Java version..."
if ! command -v java &>/dev/null; then
    echo "❌ Java not found. Please install JDK 21.0.6 or newer."
    exit 1
fi

java_version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
echo "  Found Java version: $java_version"

if [[ "$java_version" != 21* ]] && [[ "$java_version" != 21.* ]]; then
    echo "❌ Java 21 is required. Please install JDK 21.0.6 or newer."
    exit 1
else
    echo "✅ Java version is compatible"
fi

# Check Gradle
echo ""
echo "Checking Gradle availability..."
if ! command -v ./gradlew &>/dev/null; then
    echo "❌ Gradle wrapper not found. Are you in the correct directory?"
    exit 1
else
    echo "✅ Gradle wrapper found"
fi

# Check for IntelliJ Plugin Development setup
echo ""
echo "Checking IntelliJ plugin development setup..."
if [ -f "build.gradle" ]; then
    if grep -q "org.jetbrains.intellij" "build.gradle"; then
        echo "✅ IntelliJ plugin Gradle configuration found"
    else
        echo "❌ IntelliJ plugin Gradle configuration not found in build.gradle"
        exit 1
    fi
else
    echo "❌ build.gradle not found. Are you in the correct directory?"
    exit 1
fi

# Check development environment
echo ""
echo "Checking development environment..."

# Check for required tools
commands=("git" "javac")
for cmd in "${commands[@]}"; do
    if ! command -v "$cmd" &>/dev/null; then
        echo "❌ Required tool not found: $cmd"
        missing_tools=true
    fi
done

if [ -n "$missing_tools" ]; then
    echo "Please install the missing tools and try again."
    exit 1
else
    echo "✅ All required development tools are available"
fi

# Summarize system readiness
echo ""
echo "====================================================="
echo "✅ System ready for ModForge IntelliJ Plugin development"
echo ""
echo "You can now build the plugin with:"
echo "  ./build-production-plugin.sh"
echo "====================================================="