@echo off
:: Compatibility-fixed build script for ModForge IntelliJ Plugin targeting 2025.1.1.1
:: This script resolves configuration cache issues and Java 21 compatibility problems

echo ===== ModForge Compatible Build for IntelliJ IDEA 2025.1.1.1 =====
echo.
echo This script will build the plugin with all compatibility fixes applied
echo.

setlocal EnableDelayedExpansion

:: First run the detect-java script to find Java 21
echo Detecting Java 21 installations...
call detect-java.bat

:: Ask if user wants to specify Java 21 path
echo.
set /p SPECIFY_JAVA=Do you want to specify a custom Java 21 path? (y/n):

if /i "%SPECIFY_JAVA%"=="y" (
    echo.
    set /p JAVA21_PATH=Enter the path to your Java 21 installation:
    
    if not exist "!JAVA21_PATH!\bin\java.exe" (
        echo ERROR: Invalid Java path. Java executable not found at !JAVA21_PATH!\bin\java.exe
        echo Continuing without custom Java path.
    ) else (
        echo Using custom Java 21 at: !JAVA21_PATH!
        
        :: Create temporary gradle.properties with Java 21 path
        copy gradle.properties gradle.properties.bak > nul
        powershell -Command "(Get-Content gradle.properties) -replace '#\s*org\.gradle\.java\.home.*', 'org.gradle.java.home=!JAVA21_PATH:\=\\!' | Set-Content gradle.properties.new"
        
        :: Check if replacement was successful
        findstr /c:"org.gradle.java.home=" gradle.properties.new > nul
        if errorlevel 1 (
            echo # Java 21 path for building >> gradle.properties.new
            echo org.gradle.java.home=!JAVA21_PATH:\=\\! >> gradle.properties.new
        )
        
        move /y gradle.properties.new gradle.properties > nul
    )
)

:: Temporarily disable configuration cache to avoid issues
echo.
echo Temporarily disabling Gradle configuration cache...
copy gradle.properties gradle.properties.bak2 > nul
echo. >> gradle.properties
echo # Temporarily disable configuration cache to avoid issues >> gradle.properties
echo org.gradle.configuration-cache=false >> gradle.properties

:: Run the build
echo.
echo Running build with compatibility fixes...
echo.
call gradlew clean build --info

:: Restore original gradle.properties
echo.
echo Restoring original gradle.properties...
if exist gradle.properties.bak (
    move /y gradle.properties.bak gradle.properties > nul
) else if exist gradle.properties.bak2 (
    move /y gradle.properties.bak2 gradle.properties > nul
)
if exist gradle.properties.bak2 del gradle.properties.bak2 > nul

:: Check if build was successful
if exist "build\distributions\modforge-intellij-plugin-2.1.0.zip" (
    echo.
    echo ===== BUILD SUCCESSFUL =====
    echo.
    echo Plugin is available at: build\distributions\modforge-intellij-plugin-2.1.0.zip
    echo.
    echo Installation instructions:
    echo 1. Open IntelliJ IDEA 2025.1.1.1
    echo 2. Go to Settings → Plugins → ⚙ → Install Plugin from Disk...
    echo 3. Select the plugin ZIP file
    echo 4. Restart IntelliJ IDEA when prompted
) else (
    echo.
    echo ===== BUILD FAILED =====
    echo.
    echo If you're still having issues:
    echo 1. Try the simple-build.bat script which skips validation
    echo 2. Check your Java 21 installation with detect-java.bat
    echo 3. Manually edit gradle.properties with your Java 21 path
)

echo.