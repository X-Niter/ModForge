@echo off
:: Fix JDK compatibility issues for ModForge IntelliJ Plugin
:: This simple script finds Java 21 and directly modifies the build files

echo ===== ModForge Build Fixer for IntelliJ IDEA 2025.1.1.1 =====
echo.
echo This script will update your gradle.properties for building with Java 21
echo.

:: Enable delayed expansion for variables that change in loops
setlocal EnableDelayedExpansion

:: Find Java 21 from all possible locations
echo Finding Java 21 on your system...
echo.

set JAVA21_FOUND=false
set JAVA21_PATH=

:: ----- CHECK INTELLIJ DIRECTORIES FIRST -----
echo Checking IntelliJ IDEA bundled JDKs...
set "INTELLIJ_PATHS=^
%USERPROFILE%\.jdks\jbr-21*^
%USERPROFILE%\.jdks\corretto-21*^
%USERPROFILE%\.jdks\temurin-21*^
%USERPROFILE%\.jdks\jbr-*^
%LOCALAPPDATA%\JetBrains\Toolbox\apps\IDEA-C\ch-0\*\jbr^
%LOCALAPPDATA%\JetBrains\IntelliJIdea*\jbr^
%APPDATA%\JetBrains\IntelliJIdea*\jbr"

for %%p in (%INTELLIJ_PATHS%) do (
    if exist "%%p\bin\java.exe" (
        for /f "tokens=*" %%v in ('"%%p\bin\java.exe" -version 2^>^&1') do (
            echo %%v | findstr /i "version" > nul
            if not errorlevel 1 (
                echo %%v | findstr /i "21" > nul
                if not errorlevel 1 (
                    echo FOUND: Java 21 in IntelliJ at: %%p
                    set JAVA21_PATH=%%p
                    set JAVA21_FOUND=true
                    goto :search_done
                )
            )
        )
    )
)

:: ----- SEARCH OTHER LOCATIONS -----
echo IntelliJ JDK not found. Checking standard locations...

set "COMMON_PATHS=^
%ProgramFiles%\Eclipse Adoptium\jdk-21*^
%ProgramFiles%\Java\jdk-21*^
%ProgramFiles%\BellSoft\LibericaJDK-21*^
%ProgramFiles%\Amazon Corretto\jdk21*^
%ProgramFiles(x86)%\Eclipse Adoptium\jdk-21*^
%ProgramFiles(x86)%\Java\jdk-21*^
%ProgramFiles(x86)%\BellSoft\LibericaJDK-21*^
%ProgramFiles(x86)%\Amazon Corretto\jdk21*"

for %%p in (%COMMON_PATHS%) do (
    if exist "%%p\bin\java.exe" (
        echo FOUND: Java 21 at standard location: %%p
        set JAVA21_PATH=%%p
        set JAVA21_FOUND=true
        goto :search_done
    )
)

:: Check default Java if nothing else found
for /f "tokens=*" %%v in ('java -version 2^>^&1') do (
    echo %%v | findstr /i "version" > nul
    if not errorlevel 1 (
        echo %%v | findstr /i "21" > nul
        if not errorlevel 1 (
            for /f "tokens=*" %%j in ('where java') do (
                set JAVA_PATH=%%j
                set JAVA21_PATH=%%~dpj..
                echo FOUND: Default Java is version 21 at: !JAVA21_PATH!
                set JAVA21_FOUND=true
                goto :search_done
            )
        )
    )
)

:search_done

if "%JAVA21_FOUND%"=="false" (
    echo.
    echo ERROR: Could not find Java 21 on your system.
    echo Please install Java 21 or provide the path manually.
    echo.
    set /p JAVA21_PATH=Enter the path to your Java 21 installation (or press Enter to cancel):
    
    if "!JAVA21_PATH!"=="" (
        echo Cancelled. No changes made.
        exit /b 1
    )
    
    if not exist "!JAVA21_PATH!\bin\java.exe" (
        echo ERROR: Invalid Java path. Java executable not found at !JAVA21_PATH!\bin\java.exe
        echo No changes made.
        exit /b 1
    )
)

echo.
echo ===== UPDATING BUILD FILES =====
echo.

:: Fix the gradle.properties file
echo Making a backup of gradle.properties...
copy gradle.properties gradle.properties.bak > nul

:: Read the file to detect if org.gradle.java.home is already set
set FOUND_PROPERTY=0
for /f "tokens=1* delims==" %%a in (gradle.properties) do (
    if "%%a"=="org.gradle.java.home" set FOUND_PROPERTY=1
)

if %FOUND_PROPERTY%==1 (
    echo Updating existing Java home in gradle.properties...
    powershell -Command "(Get-Content gradle.properties) -replace '^org\.gradle\.java\.home.*', 'org.gradle.java.home=%JAVA21_PATH:\=\\%' | Set-Content gradle.properties.new"
    move /y gradle.properties.new gradle.properties > nul
) else (
    echo Adding Java home to gradle.properties...
    echo. >> gradle.properties
    echo # Java 21 path for building (auto-detected) >> gradle.properties
    echo org.gradle.java.home=%JAVA21_PATH:\=\\% >> gradle.properties
    echo. >> gradle.properties
)

:: Update JVM arguments
findstr /c:"--add-opens" gradle.properties > nul
if errorlevel 1 (
    echo Adding Java 21 module access flags to JVM arguments...
    powershell -Command "(Get-Content gradle.properties) -replace 'org\.gradle\.jvmargs\s*=\s*-Xmx\d+m', 'org.gradle.jvmargs = -Xmx2048m --add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED' | Set-Content gradle.properties.new"
    move /y gradle.properties.new gradle.properties > nul
)

:: Update build.gradle Java compatibility if needed
echo Checking Java language levels in build.gradle...
findstr /c:"sourceCompatibility = JavaVersion.VERSION_21" build.gradle > nul
if errorlevel 1 (
    echo Updating Java compatibility in build.gradle...
    copy build.gradle build.gradle.bak > nul
    powershell -Command "(Get-Content build.gradle) -replace 'sourceCompatibility = JavaVersion.VERSION_\d+', 'sourceCompatibility = JavaVersion.VERSION_21' -replace 'targetCompatibility = JavaVersion.VERSION_\d+', 'targetCompatibility = JavaVersion.VERSION_21' | Set-Content build.gradle.new"
    move /y build.gradle.new build.gradle > nul
)

:: Check untilBuild in plugin.xml
echo Checking plugin.xml compatibility...
findstr /c:"until-build=\"251\.\*\"" src\main\resources\META-INF\plugin.xml > nul
if errorlevel 1 (
    echo Updating plugin.xml compatibility range...
    copy src\main\resources\META-INF\plugin.xml src\main\resources\META-INF\plugin.xml.bak > nul
    powershell -Command "(Get-Content 'src\main\resources\META-INF\plugin.xml') -replace 'until-build=\"[^\"]*\"', 'until-build=\"251.*\"' | Set-Content 'src\main\resources\META-INF\plugin.xml.new'"
    move /y src\main\resources\META-INF\plugin.xml.new src\main\resources\META-INF\plugin.xml > nul
)

echo.
echo ===== BUILD ENVIRONMENT CONFIGURED SUCCESSFULLY =====
echo.
echo Java 21 path set to: %JAVA21_PATH%
echo Java module access flags added to JVM arguments
echo.
echo You can now build the plugin using: gradlew buildPlugin
echo.
echo Note: If the build still fails, you may need to also run:
echo       gradlew cleanBuildCache before building
echo.