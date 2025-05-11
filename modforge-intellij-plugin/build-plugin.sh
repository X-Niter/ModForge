#!/bin/bash

# ===================================
# ModForge IntelliJ Plugin Builder
# ===================================
#
# This script automates the build process for the ModForge IntelliJ plugin,
# handling compatibility issues with IntelliJ IDEA 2025.1.1.1.
#
# Usage: ./build-plugin.sh [clean|test|build|all]
#   - clean: Clean build artifacts
#   - test: Run tests only
#   - build: Build the plugin
#   - all: Clean, test, and build (default)

set -e  # Exit on any error

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

WORKSPACE_DIR="$PWD"
PLUGIN_XML_SRC="src/main/resources/META-INF/fixed-plugin.xml"
PLUGIN_XML_DST="src/main/resources/META-INF/plugin.xml"

# Function to print formatted messages
print_heading() {
    echo -e "${BLUE}==== $1 ====${NC}"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

# Fix the plugin.xml file
fix_plugin_xml() {
    print_heading "Fixing plugin.xml"
    
    if [ -f "$PLUGIN_XML_SRC" ]; then
        cp "$PLUGIN_XML_SRC" "$PLUGIN_XML_DST"
        print_success "Successfully copied fixed plugin.xml"
    else
        print_error "Fixed plugin.xml source file not found at $PLUGIN_XML_SRC"
        exit 1
    fi
}

# Clean build artifacts
clean_build() {
    print_heading "Cleaning build artifacts"
    ./gradlew clean
    print_success "Cleaned build artifacts"
}

# Run tests
run_tests() {
    print_heading "Running tests"
    ./gradlew test
    print_success "Tests completed"
}

# Build the plugin
build_plugin() {
    print_heading "Building plugin"
    
    # First fix the plugin.xml file
    fix_plugin_xml
    
    # Create necessary directories if they don't exist
    mkdir -p "src/main/java/com/modforge/intellij/plugin/services"
    mkdir -p "src/main/java/com/modforge/intellij/plugin/settings"
    mkdir -p "src/main/java/com/modforge/intellij/plugin/utils"
    
    # Validate the plugin before building
    print_heading "Validating plugin"
    ./gradlew validatePluginForProduction
    
    # Build the plugin
    print_heading "Compiling and packaging plugin"
    ./gradlew buildPlugin
    
    if [ $? -eq 0 ]; then
        print_success "Plugin built successfully"
        
        # Find the built plugin
        PLUGIN_PATH=$(find "build/distributions" -name "*.zip" | head -n 1)
        
        if [ -n "$PLUGIN_PATH" ]; then
            print_success "Plugin archive created: $PLUGIN_PATH"
            echo ""
            echo "To install the plugin:"
            echo "  1. Open IntelliJ IDEA"
            echo "  2. Go to Settings -> Plugins"
            echo "  3. Click the gear icon and select 'Install Plugin from Disk...'"
            echo "  4. Select the file: $WORKSPACE_DIR/$PLUGIN_PATH"
        else
            print_warning "Plugin was built but the archive file was not found in the expected location."
        fi
    else
        print_error "Plugin build failed"
        exit 1
    fi
}

# Build everything
build_all() {
    clean_build
    run_tests
    build_plugin
}

# Parse command line arguments
if [ $# -eq 0 ]; then
    # Default action if no arguments provided
    build_all
else
    case "$1" in
        clean)
            clean_build
            ;;
        test)
            run_tests
            ;;
        build)
            build_plugin
            ;;
        all)
            build_all
            ;;
        *)
            echo "Unknown command: $1"
            echo "Usage: $0 [clean|test|build|all]"
            exit 1
            ;;
    esac
fi

print_heading "Build process completed"