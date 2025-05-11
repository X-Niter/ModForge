@echo off
setlocal enabledelayedexpansion

echo ===== ModForge IntelliJ Plugin Builder for 2025.1.1.1 =====
echo.

echo Target: IntelliJ IDEA 2025.1.1.1 (Build: 251.25410.129)
echo.

echo Checking Java installation...
java -version > nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo Java not found. Please install JDK 17 or higher.
    exit /b 1
)

echo.
echo Building options:
echo 1. Use locally installed IntelliJ IDEA 2025.1.1.1 (recommended)
echo 2. Use JetBrains repository version (2023.3.6)
echo.

set /p BUILD_OPTION="Select build option (1 or 2): "

if "%BUILD_OPTION%"=="1" (
    echo.
    echo Selected: Build using local IntelliJ IDEA 2025.1.1.1 installation
    echo.

    set USE_LOCAL=true
    set INTELLIJ_PATH=C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2025.1.1.1
    
    if not exist "!INTELLIJ_PATH!" (
        echo IntelliJ IDEA 2025.1.1.1 not found at default location.
        set /p INTELLIJ_PATH="Please enter the path to IntelliJ IDEA 2025.1.1.1 installation: "
        
        if not exist "!INTELLIJ_PATH!" (
            echo ERROR: The specified path does not exist.
            echo Falling back to repository version.
            set USE_LOCAL=false
        )
    )
) else (
    echo.
    echo Selected: Build using JetBrains repository version (2023.3.6)
    echo.
    set USE_LOCAL=false
)

echo Creating temporary build.gradle for compatibility...
set TEMP_GRADLE=build.gradle.temp

type build.gradle > %TEMP_GRADLE%

if "%USE_LOCAL%"=="true" (
    echo Using local IntelliJ at: %INTELLIJ_PATH%
    powershell -Command "(Get-Content %TEMP_GRADLE%) -replace 'version = .+', '// version = \"2023.3.6\"; // Commented out to use localPath instead' | Set-Content %TEMP_GRADLE%"
    powershell -Command "(Get-Content %TEMP_GRADLE%) -replace '\/\/ localPath = .+', 'localPath = \""%INTELLIJ_PATH%\""; // Using local IntelliJ' | Set-Content %TEMP_GRADLE%"
) else (
    echo Using IntelliJ from repository.
)

echo Backing up original build.gradle...
copy build.gradle build.gradle.bak

echo Applying temporary build file...
copy %TEMP_GRADLE% build.gradle

echo.
echo Building plugin for IntelliJ IDEA 2025.1...
echo.

call gradlew clean buildPlugin --info

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
    echo 1. Open IntelliJ IDEA 2025.1
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
