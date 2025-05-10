@echo off
echo ===================================================
echo ModForge IntelliJ Plugin - Installation Verification
echo ===================================================
echo.
echo This script will help verify if your environment is ready
echo for the ModForge plugin and provide installation guidance.
echo.

REM Check Java version
echo Checking Java version...
java -version 2>&1 | findstr "21" >nul
if %errorlevel% neq 0 (
    echo [WARNING] Java 21 not detected. ModForge requires JDK 21.0.6 or newer.
    echo Please download and install Java 21 from: https://adoptium.net/
    echo.
) else (
    echo [OK] Java 21 detected.
    echo.
)

REM Check if IntelliJ is installed
echo Checking IntelliJ IDEA installation...
if exist "%APPDATA%\JetBrains\IntelliJIdea*" (
    echo [OK] IntelliJ IDEA installation found.
    echo.
) else (
    echo [WARNING] IntelliJ IDEA installation not found in standard location.
    echo Please ensure you have IntelliJ IDEA 2025.1 installed.
    echo.
)

REM Provide installation instructions
echo ===================================================
echo Installation Instructions:
echo ===================================================
echo.
echo 1. Launch IntelliJ IDEA 2025.1
echo 2. Go to File ^> Settings ^> Plugins
echo 3. Click the gear icon and select "Install Plugin from Disk..."
echo 4. Navigate to the "dist" folder and select "ModForge-2.1.0.zip"
echo 5. Click "Install" and restart IntelliJ IDEA when prompted
echo.

echo For detailed instructions, see the file:
echo dist\INSTALL-MODFORGE.md
echo.

echo ===================================================
echo Press any key to exit...
pause >nul