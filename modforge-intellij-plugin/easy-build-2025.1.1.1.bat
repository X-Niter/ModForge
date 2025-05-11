@echo off
:: Easy build script for ModForge IntelliJ Plugin targeting 2025.1.1.1
:: This is a simplified version that handles all the Java 21 setup automatically

echo ===== ModForge IntelliJ Plugin Easy Builder for 2025.1.1.1 =====
echo.
echo This script will build the plugin specifically for IntelliJ IDEA 2025.1.1.1
echo.

:: Check Java version
echo Checking Java version...
for /f "tokens=* USEBACKQ" %%a in (`java -version 2^>^&1 ^| findstr /i "version"`) do (
    set JAVA_VERSION=%%a
    echo %%a
)

:: Display Java compiler version for more detail
javac -version

:: Create temporary build files
echo.
echo Preparing build environment...

:: Save original files
copy build.gradle build.gradle.original >nul
copy gradle.properties gradle.properties.original >nul

echo.
echo ===== STEP 1: Setting up Java 21 compatibility =====

:: Update gradle.properties to work with Java 21
echo # ModForge IntelliJ Plugin Gradle Properties > gradle.properties.new
echo. >> gradle.properties.new
echo # Plugin Information >> gradle.properties.new
echo pluginGroup = com.modforge.intellij.plugin >> gradle.properties.new
echo pluginName = modforge-intellij-plugin >> gradle.properties.new
echo pluginVersion = 2.1.0 >> gradle.properties.new
echo. >> gradle.properties.new
echo # IntelliJ Platform Settings >> gradle.properties.new
echo platformType = IC >> gradle.properties.new
echo platformVersion = 2023.3.6 >> gradle.properties.new
echo platformPlugins = java >> gradle.properties.new
echo. >> gradle.properties.new
echo # Build Number Range - Support from 2023.3 through all 2025.1.x versions >> gradle.properties.new
echo pluginSinceBuild = 233 >> gradle.properties.new
echo pluginUntilBuild = 251.* >> gradle.properties.new
echo. >> gradle.properties.new
echo # Gradle Settings >> gradle.properties.new
echo gradleVersion = 8.5 >> gradle.properties.new
echo kotlin.stdlib.default.dependency = false >> gradle.properties.new
echo. >> gradle.properties.new
echo # JVM Arguments for Java 21 Compatibility >> gradle.properties.new
echo org.gradle.jvmargs = -Xmx2048m --add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8 >> gradle.properties.new
echo. >> gradle.properties.new
echo # Performance Settings >> gradle.properties.new
echo org.gradle.parallel = true >> gradle.properties.new
echo org.gradle.caching = true >> gradle.properties.new
echo org.gradle.configuration-cache = true >> gradle.properties.new
echo. >> gradle.properties.new
echo # Build Compatibility >> gradle.properties.new
echo disableDynamicLoadWarning = true >> gradle.properties.new

:: Apply the new properties
copy gradle.properties.new gradle.properties >nul
del gradle.properties.new >nul

echo.
echo ===== STEP 2: Updating build.gradle for 2025.1 compatibility =====

:: Update build.gradle to set the Java language level to 21
powershell -Command "(Get-Content build.gradle) -replace 'sourceCompatibility = JavaVersion.VERSION_\d+', 'sourceCompatibility = JavaVersion.VERSION_21' -replace 'targetCompatibility = JavaVersion.VERSION_\d+', 'targetCompatibility = JavaVersion.VERSION_21'" > build.gradle.new
copy build.gradle.new build.gradle >nul
del build.gradle.new >nul

:: Ensure patchPluginXml uses correct values
powershell -Command "(Get-Content build.gradle) -replace 'untilBuild.set\(\"[^\"]*\"\)', 'untilBuild.set(\"251.*\")'" > build.gradle.new
copy build.gradle.new build.gradle >nul
del build.gradle.new >nul

echo.
echo ===== STEP 3: Building the plugin =====
echo.
echo This may take a few minutes...
echo.

:: Build the plugin
call gradlew.bat clean buildPlugin --info

set BUILD_RESULT=%ERRORLEVEL%

:: Restore original files
echo.
echo Restoring original configuration files...
copy build.gradle.original build.gradle >nul
copy gradle.properties.original gradle.properties >nul

del build.gradle.original >nul
del gradle.properties.original >nul

echo.
if %BUILD_RESULT% EQU 0 (
    if exist "build\distributions\modforge-intellij-plugin-2.1.0.zip" (
        echo ----------------------------------------
        echo BUILD SUCCESSFUL!
        echo ----------------------------------------
        echo Plugin is available at: 
        echo build\distributions\modforge-intellij-plugin-2.1.0.zip
        echo.
        echo Installation Instructions:
        echo 1. Open IntelliJ IDEA 2025.1.1.1
        echo 2. Go to Settings → Plugins → ⚙ → Install Plugin from Disk...
        echo 3. Select the generated ZIP file
        echo 4. Restart IntelliJ IDEA when prompted
    ) else (
        echo ----------------------------------------
        echo BUILD PROCESS COMPLETED BUT PLUGIN ZIP NOT FOUND
        echo ----------------------------------------
        echo Check the build directory manually.
    )
) else (
    echo ----------------------------------------
    echo BUILD FAILED!
    echo ----------------------------------------
    echo.
    echo TROUBLESHOOTING TIPS:
    echo 1. Make sure Java 21 is installed and set as JAVA_HOME
    echo 2. Try running in administrator mode
    echo 3. Check internet connectivity
    echo.
    echo For detailed errors, run: gradlew.bat clean buildPlugin --debug
)

echo.
echo Build process completed!
echo.