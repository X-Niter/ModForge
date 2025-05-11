#!/bin/bash
#
# Shell script for building ModForge IntelliJ plugin for 2025.1.1.1
# 
# This script provides advanced troubleshooting and dependency resolution
# to build the plugin for IntelliJ IDEA 2025.1.1.1 on Linux/Mac.
#

echo -e "\033[1;36m===== ModForge IntelliJ Plugin Builder for 2025.1.1.1 =====\033[0m"
echo ""
echo -e "\033[1;36mTarget: IntelliJ IDEA 2025.1.1.1 (Build: 251.25410.129)\033[0m"
echo ""

# Check Java installation
if ! java -version > /dev/null 2>&1; then
    echo -e "\033[1;31mError: Java not found. Please install JDK 17 or higher.\033[0m"
    exit 1
else
    echo -e "\033[1;32mJava detected:\033[0m $(java -version 2>&1 | head -n 1)"
fi

# Prompt for build type
echo ""
echo -e "\033[1;33mBuild Options:\033[0m"
echo -e "\033[1;33m1. Build using local IntelliJ IDEA 2025.1.1.1 installation (recommended)\033[0m"
echo -e "\033[1;33m2. Build using JetBrains repository version (2023.3.6)\033[0m"
echo ""

read -p "Select build option (1 or 2): " buildOption

useLocal=false
intellijPath=""

if [ "$buildOption" = "1" ]; then
    echo ""
    echo -e "\033[1;36mSelected: Build using local IntelliJ IDEA 2025.1.1.1 installation\033[0m"
    echo ""
    
    # Default paths to try
    defaultPaths=(
        "/Applications/IntelliJ IDEA.app/Contents"
        "/Applications/IntelliJ IDEA CE.app/Contents"
        "/opt/idea/idea-IC-251.25410.129"
        "/usr/local/idea/idea-IC-251.25410.129"
        "$HOME/idea/idea-IC-251.25410.129"
    )
    
    for path in "${defaultPaths[@]}"; do
        if [ -d "$path" ]; then
            intellijPath="$path"
            echo -e "\033[1;32mFound IntelliJ IDEA at: $intellijPath\033[0m"
            break
        fi
    done
    
    if [ -z "$intellijPath" ]; then
        echo -e "\033[1;33mIntelliJ IDEA 2025.1.1.1 not found at default locations.\033[0m"
        read -p "Please enter the path to IntelliJ IDEA 2025.1.1.1 installation: " intellijPath
        
        if [ ! -d "$intellijPath" ]; then
            echo -e "\033[1;31mERROR: The specified path does not exist.\033[0m"
            echo -e "\033[1;33mFalling back to repository version.\033[0m"
            useLocal=false
        else
            useLocal=true
        fi
    else
        useLocal=true
    fi
else
    echo ""
    echo -e "\033[1;36mSelected: Build using JetBrains repository version (2023.3.6)\033[0m"
    echo ""
    useLocal=false
fi

echo -e "\033[1;36mCreating temporary build.gradle for compatibility...\033[0m"
tempGradle="build.gradle.temp"

# Read the original build.gradle
buildGradleContent=$(cat build.gradle)

if [ "$useLocal" = true ]; then
    echo -e "\033[1;32mUsing local IntelliJ at: $intellijPath\033[0m"
    
    # Replace version with commented version
    buildGradleContent=$(echo "$buildGradleContent" | sed "s/version = '2023\.3\.6'/\/\/ version = '2023.3.6' \/\/ Commented out to use localPath instead/")
    
    # Replace localPath comment with actual localPath
    escapedPath=$(echo "$intellijPath" | sed 's/\//\\\//g')
    buildGradleContent=$(echo "$buildGradleContent" | sed "s/\/\/ localPath = .*/localPath = '$escapedPath' \/\/ Using local IntelliJ/")
else
    echo -e "\033[1;32mUsing IntelliJ from repository.\033[0m"
fi

# Backup original build.gradle
cp build.gradle build.gradle.bak

# Write the modified content to temp file
echo "$buildGradleContent" > $tempGradle

# Apply temporary build file
cp $tempGradle build.gradle

echo ""
echo -e "\033[1;36mBuilding plugin for IntelliJ IDEA 2025.1.1.1...\033[0m"
echo ""

# Build the plugin
if [ "$useLocal" = true ]; then
    ./gradlew clean buildPlugin --info
else
    ./gradlew clean buildPlugin --info
fi

buildSuccessful=$?

# Restore original build.gradle
echo ""
echo -e "\033[1;36mRestoring original build.gradle...\033[0m"
cp build.gradle.bak build.gradle
rm build.gradle.bak
rm $tempGradle

echo ""
if [ $buildSuccessful -eq 0 ] && [ -f "build/distributions/modforge-intellij-plugin-2.1.0.zip" ]; then
    echo -e "\033[1;32m----------------------------------------\033[0m"
    echo -e "\033[1;32mBUILD SUCCESSFUL!\033[0m"
    echo -e "\033[1;32m----------------------------------------\033[0m"
    echo -e "\033[1;32mPlugin is available at: build/distributions/modforge-intellij-plugin-2.1.0.zip\033[0m"
    echo ""
    echo -e "\033[1;36mInstallation Instructions:\033[0m"
    echo -e "\033[1;36m1. Open IntelliJ IDEA 2025.1.1.1\033[0m"
    echo -e "\033[1;36m2. Go to Settings → Plugins → ⚙ → Install Plugin from Disk...\033[0m"
    echo -e "\033[1;36m3. Select the generated ZIP file\033[0m"
    echo -e "\033[1;36m4. Restart IntelliJ IDEA when prompted\033[0m"
else
    echo -e "\033[1;31m----------------------------------------\033[0m"
    echo -e "\033[1;31mBUILD FAILED!\033[0m"
    echo -e "\033[1;31m----------------------------------------\033[0m"
    echo -e "\033[1;31mPlease check the error messages above.\033[0m"
    
    echo ""
    echo -e "\033[1;33mTroubleshooting Tips:\033[0m"
    echo -e "\033[1;33m1. Make sure you have the correct IntelliJ IDEA path\033[0m"
    echo -e "\033[1;33m2. Check that your Java version is compatible (JDK 17+)\033[0m"
    echo -e "\033[1;33m3. Try running with --stacktrace flag: ./gradlew buildPlugin --stacktrace\033[0m"
    echo -e "\033[1;33m4. Check internet connectivity for downloading dependencies\033[0m"
fi

echo ""
echo -e "\033[1;36m===== Build process completed =====\033[0m"
