@echo off
setlocal enabledelayedexpansion

echo ===== ModForge IntelliJ Plugin Builder (Local IDE Only) =====
echo.

echo This script will build the plugin using your local IntelliJ IDEA installation.
echo IMPORTANT: This method bypasses version checking in repositories.
echo.

echo Checking Java installation...
java -version > nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo Error: Java not found. Please install JDK 17 or higher.
    exit /b 1
)

echo.
echo Please enter the path to your IntelliJ IDEA installation:
echo Example: C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2025.1.1.1
echo.
set /p INTELLIJ_PATH="Path: "

if not exist "!INTELLIJ_PATH!" (
    echo Error: The specified path does not exist.
    exit /b 1
)

echo.
echo Creating temporary build.gradle for local build...
set TEMP_GRADLE=build.gradle.temp

type build.gradle > %TEMP_GRADLE%

echo Modifying build configuration to use local IntelliJ...
powershell -Command "(Get-Content %TEMP_GRADLE%) -replace 'version = .+', '// version = \"2023.3.6\"; // Commented out to use localPath instead' | Set-Content %TEMP_GRADLE%"
powershell -Command "(Get-Content %TEMP_GRADLE%) -replace '\/\/ localPath = .+', 'localPath = \"!INTELLIJ_PATH!\"; // Using local IntelliJ' | Set-Content %TEMP_GRADLE%"

echo Backing up original build.gradle...
copy build.gradle build.gradle.bak

echo Applying temporary build file...
copy %TEMP_GRADLE% build.gradle

echo.
echo ========================================
echo Building plugin using local IntelliJ at:
echo !INTELLIJ_PATH!
echo ========================================
echo.

call gradlew clean buildPlugin

echo.
echo Restoring original build.gradle...
copy build.gradle.bak build.gradle
del build.gradle.bak
del %TEMP_GRADLE%

echo.
if exist "build\distributions\modforge-intellij-plugin-2.1.0.zip" (
    echo ----------------------------------------
    echo BUILD SUCCESSFUL!
    echo ----------------------------------------
    echo Plugin is available at: build\distributions\modforge-intellij-plugin-2.1.0.zip
    echo.
    echo Installation Instructions:
    echo 1. Open IntelliJ IDEA
    echo 2. Go to Settings → Plugins → ⚙ → Install Plugin from Disk...
    echo 3. Select the generated ZIP file
    echo 4. Restart IntelliJ IDEA when prompted
) else (
    echo ----------------------------------------
    echo BUILD FAILED!
    echo ----------------------------------------
    echo Please check the error messages above.
)

echo.
echo ===== Build process completed =====
endlocal
