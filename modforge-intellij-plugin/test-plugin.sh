#!/bin/bash
# ModForge IntelliJ Plugin Installation/Testing Script
# For IntelliJ IDEA 2025.1 compatibility
# Version 2.1.0

echo "==================================================="
echo "ModForge IntelliJ Plugin Installation/Testing Tool"
echo "==================================================="
echo ""

# Set variables for important paths
PLUGIN_JAR="dist/ModForge-2.1.0.zip"
LOG_FILE="plugin-test.log"

# Start logging
echo "Timestamp: $(date)" > "$LOG_FILE"
echo "System: $(uname -a)" >> "$LOG_FILE"
echo "Java Version:" >> "$LOG_FILE"
java -version 2>> "$LOG_FILE"

# Check if plugin file exists
if [ ! -f "$PLUGIN_JAR" ]; then
    echo "ERROR: Plugin file not found at \"$PLUGIN_JAR\""
    echo "Please build the plugin first or check the path"
    echo "Plugin file not found at \"$(pwd)/$PLUGIN_JAR\"" >> "$LOG_FILE"
    exit 1
fi

echo "Plugin file found: $PLUGIN_JAR"
echo "Plugin file found: $(pwd)/$PLUGIN_JAR" >> "$LOG_FILE"

# Find IntelliJ IDEA installation
echo "Searching for IntelliJ IDEA 2025.1 installation..."
IDEA_FOUND=0
IDEA_PATH=""

# Try common installation paths for IntelliJ IDEA 2025.1
IDEA_PATHS=(
    "/Applications/IntelliJ IDEA.app/Contents/MacOS/idea"
    "/Applications/IntelliJ IDEA CE.app/Contents/MacOS/idea"
    "/usr/local/bin/idea"
    "/usr/bin/idea"
    "/opt/idea/bin/idea.sh"
    "/opt/intellij-idea/bin/idea.sh"
    "/snap/intellij-idea-community/current/bin/idea.sh"
    "/snap/intellij-idea-ultimate/current/bin/idea.sh"
    "$HOME/.local/share/JetBrains/Toolbox/apps/IDEA-U/ch-0/*/bin/idea.sh"
    "$HOME/.local/share/JetBrains/Toolbox/apps/IDEA-C/ch-0/*/bin/idea.sh"
    "$HOME/idea/bin/idea.sh"
)

for idea_exec in "${IDEA_PATHS[@]}"; do
    # Handle paths with wildcards
    if [[ $idea_exec == *"*"* ]]; then
        # Use eval to expand the wildcard
        eval "matching_paths=($idea_exec)"
        for match in "${matching_paths[@]}"; do
            if [ -f "$match" ]; then
                IDEA_PATH="$match"
                IDEA_FOUND=1
                echo "Found IntelliJ IDEA: $IDEA_PATH"
                echo "Found IntelliJ IDEA: $IDEA_PATH" >> "$LOG_FILE"
                break 2
            fi
        done
    else
        # Regular path, no wildcards
        if [ -f "$idea_exec" ]; then
            IDEA_PATH="$idea_exec"
            IDEA_FOUND=1
            echo "Found IntelliJ IDEA: $IDEA_PATH"
            echo "Found IntelliJ IDEA: $IDEA_PATH" >> "$LOG_FILE"
            break
        fi
    fi
done

if [ $IDEA_FOUND -eq 0 ]; then
    echo "IntelliJ IDEA 2025.1 not found in common locations."
    echo ""
    echo "Please enter the full path to IntelliJ IDEA executable:"
    echo "Example: /Applications/IntelliJ IDEA.app/Contents/MacOS/idea"
    echo ""
    read -p "Path: " IDEA_PATH
    
    if [ ! -f "$IDEA_PATH" ]; then
        echo "ERROR: The specified file does not exist."
        echo "Custom path not found: $IDEA_PATH" >> "$LOG_FILE"
        exit 1
    fi
    
    echo "Using custom IntelliJ IDEA path: $IDEA_PATH"
    echo "Using custom IntelliJ IDEA path: $IDEA_PATH" >> "$LOG_FILE"
fi

# Make sure the idea script is executable
chmod +x "$IDEA_PATH"

echo ""
echo "==================================================="
echo "Installation options:"
echo "==================================================="
echo "1. Install plugin from disk"
echo "2. Run IntelliJ IDEA with plugin (for testing)"
echo "3. Exit"
echo ""
read -p "Select an option (1-3): " OPTION

case $OPTION in
    1)
        echo ""
        echo "Installing plugin..."
        echo "Executing: \"$IDEA_PATH\" installPluginFromDisk \"$(pwd)/$PLUGIN_JAR\"" >> "$LOG_FILE"
        
        "$IDEA_PATH" installPluginFromDisk "$(pwd)/$PLUGIN_JAR"
        if [ $? -ne 0 ]; then
            echo "Installation command failed with error code: $?" >> "$LOG_FILE"
            echo "WARNING: There might have been an issue with installation."
            echo "Please check if IntelliJ IDEA opened a dialog to complete installation."
        else
            echo "Plugin installation command executed successfully."
            echo "Installation command succeeded" >> "$LOG_FILE"
        fi
        
        echo ""
        echo "Plugin installation initiated. If IntelliJ IDEA opened, please follow the on-screen instructions."
        echo "After installation, restart IntelliJ IDEA to activate the plugin."
        ;;
        
    2)
        echo ""
        echo "Running IntelliJ IDEA with plugin for testing..."
        echo "Creating temporary plugin directory..." >> "$LOG_FILE"
        
        TEMP_PLUGINS_DIR="/tmp/ModForgePluginTest"
        rm -rf "$TEMP_PLUGINS_DIR"
        mkdir -p "$TEMP_PLUGINS_DIR"
        
        echo "Copying plugin to: $TEMP_PLUGINS_DIR" >> "$LOG_FILE"
        cp "$(pwd)/$PLUGIN_JAR" "$TEMP_PLUGINS_DIR/"
        
        echo "Executing: \"$IDEA_PATH\" -Didea.plugins.path=\"$TEMP_PLUGINS_DIR\"" >> "$LOG_FILE"
        "$IDEA_PATH" -Didea.plugins.path="$TEMP_PLUGINS_DIR" &
        
        echo ""
        echo "IntelliJ IDEA started with custom plugin directory."
        echo "Plugin should be loaded from: $TEMP_PLUGINS_DIR"
        echo "Note: This is a temporary installation for testing only."
        ;;
        
    3)
        echo "Exiting..."
        exit 0
        ;;
        
    *)
        echo "Invalid option. Exiting."
        exit 1
        ;;
esac

echo ""
echo "==================================================="
echo "Process completed successfully!"
echo "See $LOG_FILE for details."
echo "==================================================="