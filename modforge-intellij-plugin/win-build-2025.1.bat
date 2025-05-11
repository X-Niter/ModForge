@echo off
:: ModForge IntelliJ Plugin Build Script for IntelliJ IDEA 2025.1.1.1
:: This script automates the build process specifically for 2025.1.1.1

echo ===== ModForge IntelliJ Plugin Builder for 2025.1.1.1 =====
echo.
echo Target: IntelliJ IDEA 2025.1.1.1 (Build: 251.25410.129)
echo.

:: First, check if Java is available
where java >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Java not found. Please install JDK 17 or higher.
    exit /b 1
)

:: Check Java version
for /f "tokens=* USEBACKQ" %%a in (`java -version 2^>^&1 ^| findstr /i "version"`) do (
    echo Java detected: %%a
)

:: Ensure we have Java 21
call scripts\setup-java21-windows.bat
if %ERRORLEVEL% NEQ 0 (
    echo Java 21 setup failed. Cannot continue.
    exit /b 1
)

:: Backup original build.gradle
echo Creating temporary build.gradle for compatibility...
copy build.gradle build.gradle.bak >nul

:: Now let's create a temporary build file that uses either local or repo version
set /p "buildType=Build with [1] repository version or [2] local IntelliJ installation? (1/2): "

if "%buildType%"=="2" (
    echo.
    echo Selected: Build using local IntelliJ IDEA 2025.1.1.1 installation
    echo.
    
    set /p "ideaPath=Enter the path to IntelliJ IDEA 2025.1.1.1 installation (e.g., C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2025.1.1.1): "
    
    if not exist "%ideaPath%" (
        echo ERROR: The specified path does not exist.
        goto cleanup
    )
    
    :: Create temp build file with local path
    powershell -Command "(Get-Content build.gradle) -replace 'version = ''2023\.3\.6''', '// version = ''2023.3.6'' // Commented out to use localPath instead' -replace '// localPath = .*', 'localPath = ''%ideaPath:\=\\%'' // Using local IntelliJ'" > build.gradle.temp
    
) else (
    echo.
    echo Selected: Build using JetBrains repository version
    echo.
    
    :: Use repo version - no changes needed
    copy build.gradle build.gradle.temp >nul
)

:: Apply temporary build file
copy build.gradle.temp build.gradle >nul

echo.
echo Building plugin for IntelliJ IDEA 2025.1.1.1...
echo.

:: Run the Gradle build
call gradlew.bat clean buildPlugin --info

set BUILD_SUCCESS=%ERRORLEVEL%

:cleanup
:: Restore original build.gradle
echo.
echo Restoring original build.gradle...
copy build.gradle.bak build.gradle >nul
del build.gradle.bak >nul
del build.gradle.temp >nul

echo.
if %BUILD_SUCCESS% EQU 0 (
    if exist "build\distributions\modforge-intellij-plugin-2.1.0.zip" (
        echo ----------------------------------------
        echo BUILD SUCCESSFUL!
        echo ----------------------------------------
        echo Plugin is available at: build\distributions\modforge-intellij-plugin-2.1.0.zip
        echo.
        echo Installation Instructions:
        echo 1. Open IntelliJ IDEA 2025.1.1.1
        echo 2. Go to Settings → Plugins → ⚙ → Install Plugin from Disk...
        echo 3. Select the generated ZIP file
        echo 4. Restart IntelliJ IDEA when prompted
    ) else (
        echo ----------------------------------------
        echo BUILD FAILED!
        echo ----------------------------------------
        echo Build process completed but plugin ZIP was not found.
    )
) else (
    echo ----------------------------------------
    echo BUILD FAILED!
    echo ----------------------------------------
    echo Please check the error messages above.
    echo.
    echo Troubleshooting Tips:
    echo 1. Make sure you have the correct IntelliJ IDEA path
    echo 2. Check that your Java version is compatible (JDK 21)
    echo 3. Try running with --stacktrace flag: gradlew buildPlugin --stacktrace
    echo 4. Check internet connectivity for downloading dependencies
)

echo.
echo ===== Build process completed =====

exit /b %BUILD_SUCCESS%
