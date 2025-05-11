@echo off
echo === Building ModForge IntelliJ Plugin ===
echo Java version:
java -version

REM Verify JAVA_HOME is set to Java 21
if defined JAVA_HOME (
    echo JAVA_HOME is set to: %JAVA_HOME%
) else (
    echo WARNING: JAVA_HOME is not set. This may cause build issues.
)

REM Clean the build directory first
echo Cleaning previous builds...
call gradlew clean

REM Build with debug information
echo Building plugin with debug information...
call gradlew buildPlugin --info

REM Check if build was successful
if %ERRORLEVEL% EQU 0 (
    echo === Build completed successfully ===
    echo Plugin ZIP file location:
    for /r "build\distributions" %%f in (*.zip) do echo %%f
) else (
    echo === Build failed ===
    echo Check the error messages above for details.
    
    REM Provide troubleshooting guidance
    echo.
    echo === Troubleshooting Guide ===
    echo 1. Check if you have IntelliJ IDEA 2025.1 installed locally.
    echo 2. Update the localPath in build.gradle to point to your IntelliJ installation:
    echo    - Windows: C:/Program Files/JetBrains/IntelliJ IDEA Community Edition 2025.1
    echo    - macOS: /Applications/IntelliJ IDEA.app/Contents
    echo    - Linux: /opt/intellij-idea-community
    echo 3. Make sure Java 21 is being used (java -version^)
    echo 4. If you get 'git4idea' plugin errors:
    echo    a. Open build.gradle and ensure correct plugin IDs are used
    echo    b. Install IntelliJ IDEA 2025.1 locally and point to it using localPath
    echo.
    echo For more help, see: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
    
    exit /b 1
)