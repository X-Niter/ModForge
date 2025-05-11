@echo off
:: Setup Java 21 for ModForge IntelliJ Plugin
:: This batch file helps configure the correct Java 21 path

echo ===== ModForge Java 21 Environment Setup =====
echo.

:: Check current Java version
echo Current Java version:
java -version 2>&1
echo.

:: Try to detect Java 21 location
set JAVA21_FOUND=0
set JAVA21_PATH=

:: Check JAVA_HOME first
if defined JAVA_HOME (
    for /f "tokens=*" %%i in ('"%JAVA_HOME%\bin\java.exe" -version 2^>^&1') do (
        echo %%i | findstr /C:"21" > nul
        if not errorlevel 1 (
            echo Found Java 21 at JAVA_HOME: %JAVA_HOME%
            set JAVA21_FOUND=1
            set JAVA21_PATH=%JAVA_HOME%
            goto :found
        )
    )
)

:: Try common directories
set POSSIBLE_PATHS=^
    "%ProgramFiles%\Eclipse Adoptium\jdk-21"^
    "%ProgramFiles%\Java\jdk-21"^
    "%ProgramFiles%\BellSoft\LibericaJDK-21"^
    "%ProgramFiles%\Amazon Corretto\jdk21"^
    "%ProgramFiles(x86)%\Eclipse Adoptium\jdk-21"^
    "%ProgramFiles(x86)%\Java\jdk-21"^
    "%ProgramFiles(x86)%\BellSoft\LibericaJDK-21"^
    "%ProgramFiles(x86)%\Amazon Corretto\jdk21"

for %%p in (%POSSIBLE_PATHS%) do (
    if exist %%p (
        echo Found Java 21 at: %%p
        set JAVA21_FOUND=1
        set JAVA21_PATH=%%p
        goto :found
    )
)

:found
if %JAVA21_FOUND%==0 (
    echo ERROR: Could not find Java 21 installation.
    echo Please install Java 21 from one of these sources:
    echo - Eclipse Adoptium: https://adoptium.net/
    echo - Oracle: https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html
    echo - Amazon Corretto: https://aws.amazon.com/corretto/
    echo - BellSoft Liberica: https://bell-sw.com/pages/downloads/
    exit /b 1
)

:: Update gradle.properties
echo Updating gradle.properties with Java 21 path: %JAVA21_PATH%
echo.

:: Check if gradle.properties exists
if not exist gradle.properties (
    echo Creating new gradle.properties file
    (
        echo # Gradle properties for ModForge IntelliJ Plugin
        echo # Created by setup-java21-windows.bat
        echo.
        echo # Java 21 path for building
        echo org.gradle.java.home=%JAVA21_PATH:\=\\%
        echo.
        echo # Performance settings
        echo org.gradle.parallel=true
        echo org.gradle.caching=true
        echo org.gradle.daemon=true
        echo org.gradle.jvmargs=-Xmx2g --add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8
    ) > gradle.properties
) else (
    :: Create a temporary file
    if exist gradle.properties.new del gradle.properties.new
    
    :: Check if org.gradle.java.home already exists
    findstr /C:"org.gradle.java.home" gradle.properties > nul
    if not errorlevel 1 (
        :: Replace existing org.gradle.java.home line
        for /f "tokens=*" %%a in (gradle.properties) do (
            echo %%a | findstr /C:"org.gradle.java.home" > nul
            if not errorlevel 1 (
                echo org.gradle.java.home=%JAVA21_PATH:\=\\%>> gradle.properties.new
            ) else (
                echo %%a>> gradle.properties.new
            )
        )
    ) else (
        :: Add org.gradle.java.home line
        copy gradle.properties gradle.properties.new > nul
        echo.>> gradle.properties.new
        echo # Java 21 path for building>> gradle.properties.new
        echo org.gradle.java.home=%JAVA21_PATH:\=\\%>> gradle.properties.new
    )
    
    :: Replace original file with the new one
    del gradle.properties
    rename gradle.properties.new gradle.properties
)

echo Java 21 setup completed successfully!
echo You can now build the plugin with: gradlew buildPlugin
echo.