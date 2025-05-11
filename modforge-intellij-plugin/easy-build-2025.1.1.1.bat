@echo off
:: Easy build script for ModForge IntelliJ Plugin targeting 2025.1.1.1
:: This is a simplified version that handles all the Java 21 setup automatically
:: and intelligently finds Java 21 from all possible locations

echo ===== ModForge IntelliJ Plugin Easy Builder for 2025.1.1.1 =====
echo.
echo This script will build the plugin specifically for IntelliJ IDEA 2025.1.1.1
echo.

:: Check current Java version
echo Checking current Java version...
for /f "tokens=* USEBACKQ" %%a in (`java -version 2^>^&1 ^| findstr /i "version"`) do (
    set JAVA_VERSION=%%a
    echo %%a
)

:: Find Java 21 from all possible locations
echo Finding Java 21 on your system...
echo.

set JAVA21_FOUND=false
set JAVA21_PATH=

:: PART 1: First check if we're already using Java 21
for /f "tokens=* USEBACKQ" %%a in (`java -version 2^>^&1 ^| findstr /i "version"`) do (
    echo %%a | findstr /i "21" > nul
    if not errorlevel 1 (
        echo FOUND: System default Java is already Java 21
        for /f "tokens=* USEBACKQ" %%j in (`where java`) do (
            set JAVA_BIN=%%j
            set JAVA21_PATH=%%~dpj..
            set JAVA21_FOUND=true
            echo Using Java 21 at: !JAVA21_PATH!
            goto :found_java21
        )
    )
)

:: PART 2: Check IntelliJ bundled JDKs - where IDEs often store downloaded JDKs
echo Checking IntelliJ-bundled JDKs...
set "INTELLIJ_JDK_PATHS=^
%LOCALAPPDATA%\JetBrains\Toolbox\apps\IDEA-C\ch-0\*\jbr^
%LOCALAPPDATA%\JetBrains\IntelliJIdea*\jbr^
%LOCALAPPDATA%\Programs\IntelliJ IDEA*\jbr^
%APPDATA%\JetBrains\IntelliJIdea*\jbr^
%USERPROFILE%\.jdks\*"

for %%p in (%INTELLIJ_JDK_PATHS%) do (
    if exist "%%p" (
        if exist "%%p\bin\java.exe" (
            for /f "tokens=* USEBACKQ" %%a in (`"%%p\bin\java.exe" -version 2^>^&1 ^| findstr /i "version"`) do (
                echo %%a | findstr /i "21" > nul
                if not errorlevel 1 (
                    echo FOUND: IntelliJ bundled Java 21 at: %%p
                    set JAVA21_PATH=%%p
                    set JAVA21_FOUND=true
                    goto :found_java21
                )
            )
        )
    )
)

:: PART 3: Check JAVA_HOME
echo Checking JAVA_HOME environment variable...
if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\java.exe" (
        for /f "tokens=* USEBACKQ" %%a in (`"%JAVA_HOME%\bin\java.exe" -version 2^>^&1 ^| findstr /i "version"`) do (
            echo %%a | findstr /i "21" > nul
            if not errorlevel 1 (
                echo FOUND: Java 21 in JAVA_HOME at: %JAVA_HOME%
                set JAVA21_PATH=%JAVA_HOME%
                set JAVA21_FOUND=true
                goto :found_java21
            )
        )
    )
)

:: PART 4: Check standard installation locations
echo Checking standard Java installation directories...
set "STD_PATHS=^
%ProgramFiles%\Eclipse Adoptium\jdk-21*^
%ProgramFiles%\Java\jdk-21*^
%ProgramFiles%\BellSoft\LibericaJDK-21*^
%ProgramFiles%\Amazon Corretto\jdk21*^
%ProgramFiles(x86)%\Eclipse Adoptium\jdk-21*^
%ProgramFiles(x86)%\Java\jdk-21*^
%ProgramFiles(x86)%\BellSoft\LibericaJDK-21*^
%ProgramFiles(x86)%\Amazon Corretto\jdk21*^
C:\jdk-21*^
%USERPROFILE%\scoop\apps\openjdk\21*"

for %%p in (%STD_PATHS%) do (
    if exist "%%p\bin\java.exe" (
        echo FOUND: Java 21 at standard location: %%p
        set JAVA21_PATH=%%p
        set JAVA21_FOUND=true
        goto :found_java21
    )
)

:: PART 5: Scan all JetBrains folders for hidden JDKs (recursive)
echo Scanning for hidden JDKs in JetBrains directories...
if exist "%LOCALAPPDATA%\JetBrains" (
    for /f "tokens=*" %%d in ('dir /b /s /a:d "%LOCALAPPDATA%\JetBrains\*jbr*"') do (
        if exist "%%d\bin\java.exe" (
            for /f "tokens=* USEBACKQ" %%a in (`"%%d\bin\java.exe" -version 2^>^&1 ^| findstr /i "version"`) do (
                echo %%a | findstr /i "21" > nul
                if not errorlevel 1 (
                    echo FOUND: JetBrains bundled Java 21 at: %%d
                    set JAVA21_PATH=%%d
                    set JAVA21_FOUND=true
                    goto :found_java21
                )
            )
        )
    )
)

:: PART 6: Look in IntelliJ's project JDK configuration - more precise project settings
echo Checking IntelliJ IDEA project JDK settings...
if exist ".idea\misc.xml" (
    type ".idea\misc.xml" | findstr /i "jdk-21" > nul
    if not errorlevel 1 (
        for /f "tokens=*" %%j in ('type ".idea\misc.xml" ^| findstr /i "jdk-21" ^| findstr /i "jdk.home"') do (
            for /f "tokens=2 delims=><" %%p in ("%%j") do (
                if exist "%%p\bin\java.exe" (
                    echo FOUND: Project configured Java 21 at: %%p
                    set JAVA21_PATH=%%p
                    set JAVA21_FOUND=true
                    goto :found_java21
                )
            )
        )
    )
)

echo.
echo WARNING: Could not find Java 21 on your system.
set /p CONTINUE=Do you want to continue the build anyway? (y/n):

if /i not "%CONTINUE%"=="y" (
    echo Build canceled. Please install Java 21 and try again.
    exit /b 1
)

:found_java21
echo.

:: Enabling delayed expansion for variables that change in loops
setlocal EnableDelayedExpansion

:: Create temporary build files
echo.
echo Preparing build environment...

:: Save original files
copy build.gradle build.gradle.original >nul
copy gradle.properties gradle.properties.original >nul

:: If we have Java 21, configure it in gradle.properties
if "%JAVA21_FOUND%"=="true" (
    echo Found Java 21! Configuring build to use: %JAVA21_PATH%
    echo.
)

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

:: Add Java 21 path if we found it
if "%JAVA21_FOUND%"=="true" (
    echo # Using Java 21 detected by the script >> gradle.properties.new
    echo org.gradle.java.home = %JAVA21_PATH:\=\\% >> gradle.properties.new
    echo. >> gradle.properties.new
)

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