@echo off
:: Java 21 Detection Script for ModForge IntelliJ Plugin
:: This script finds all Java 21 installations on the system

echo ===== Java 21 Detection Tool =====
echo.
echo Searching for Java 21 installations...
echo.

setlocal EnableDelayedExpansion

:: Create results file
echo -- Java 21 Installations Found -- > java21_locations.txt

:: Set flag to track if any Java 21 was found
set FOUND_JAVA21=0

:: Check common installation paths
set PATHS_TO_CHECK=^
C:\Program Files\Java^
C:\Program Files (x86)\Java^
C:\Java^
%LOCALAPPDATA%\Programs\Eclipse Adoptium^
%USERPROFILE%\.jdks

:: Check JetBrains bundled JDKs
set JB_PATHS_TO_CHECK=^
%USERPROFILE%\.jdks^
%LOCALAPPDATA%\JetBrains\Toolbox\apps\IDEA-C^
%LOCALAPPDATA%\JetBrains\Toolbox\apps\IDEA-U^
%LOCALAPPDATA%\JetBrains\Toolbox\apps\AndroidStudio^
%LOCALAPPDATA%\JetBrains\IntelliJIdea*^
%PROGRAMFILES%\JetBrains\IntelliJ*

echo Checking standard Java installation paths...

:: Loop through each path
for %%p in (%PATHS_TO_CHECK%) do (
    if exist "%%p" (
        :: Find all Java installations in this path
        for /d %%j in ("%%p\*") do (
            set "JAVA_PATH=%%j"
            
            :: Check if java.exe exists
            if exist "!JAVA_PATH!\bin\java.exe" (
                :: Get Java version
                "!JAVA_PATH!\bin\java.exe" -version 2>temp_version.txt
                findstr /c:"version" /c:"openjdk version" /c:"java version" temp_version.txt > nul
                if not errorlevel 1 (
                    findstr /c:"21" /c:"21." temp_version.txt > nul
                    if not errorlevel 1 (
                        echo FOUND Java 21: !JAVA_PATH!
                        echo !JAVA_PATH! >> java21_locations.txt
                        set FOUND_JAVA21=1
                    )
                )
                del temp_version.txt > nul 2>&1
            )
        )
    )
)

echo.
echo Checking JetBrains bundled Java installations...

:: Loop through JetBrains paths
for %%p in (%JB_PATHS_TO_CHECK%) do (
    if exist "%%p" (
        :: Find all potential JBR installations recursively
        for /f "tokens=*" %%j in ('dir /b /s "%%p\jbr" 2^>nul') do (
            set "JAVA_PATH=%%j"
            
            :: Check if java.exe exists
            if exist "!JAVA_PATH!\bin\java.exe" (
                :: Get Java version
                "!JAVA_PATH!\bin\java.exe" -version 2>temp_version.txt
                findstr /c:"version" /c:"openjdk version" /c:"java version" temp_version.txt > nul
                if not errorlevel 1 (
                    findstr /c:"21" /c:"21." temp_version.txt > nul
                    if not errorlevel 1 (
                        echo FOUND JetBrains Bundled Java 21: !JAVA_PATH!
                        echo !JAVA_PATH! [JetBrains Bundled] >> java21_locations.txt
                        set FOUND_JAVA21=1
                    )
                )
                del temp_version.txt > nul 2>&1
            )
        )
        
        :: Check direct .jdks entries which might be symlinks
        if "%%p"=="%USERPROFILE%\.jdks" (
            for /d %%j in ("%USERPROFILE%\.jdks\*") do (
                set "JAVA_PATH=%%j"
                
                :: Check if this might be a Java 21 installation
                echo "!JAVA_PATH!" | findstr /c:"21" /c:"jbr-21" /c:"jbr-17" > nul
                if not errorlevel 1 (
                    if exist "!JAVA_PATH!\bin\java.exe" (
                        :: Get Java version
                        "!JAVA_PATH!\bin\java.exe" -version 2>temp_version.txt
                        findstr /c:"version" /c:"openjdk version" /c:"java version" temp_version.txt > nul
                        if not errorlevel 1 (
                            findstr /c:"21" /c:"21." temp_version.txt > nul
                            if not errorlevel 1 (
                                echo FOUND JetBrains .jdks Java 21: !JAVA_PATH!
                                echo !JAVA_PATH! [JetBrains .jdks] >> java21_locations.txt
                                set FOUND_JAVA21=1
                            )
                        )
                        del temp_version.txt > nul 2>&1
                    )
                )
            )
        )
    )
)

echo.
echo Checking JAVA_HOME environment variable...

:: Check JAVA_HOME
if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\java.exe" (
        "%JAVA_HOME%\bin\java.exe" -version 2>temp_version.txt
        findstr /c:"version" /c:"openjdk version" /c:"java version" temp_version.txt > nul
        if not errorlevel 1 (
            findstr /c:"21" /c:"21." temp_version.txt > nul
            if not errorlevel 1 (
                echo FOUND Java 21 in JAVA_HOME: %JAVA_HOME%
                echo %JAVA_HOME% [JAVA_HOME] >> java21_locations.txt
                set FOUND_JAVA21=1
            ) else (
                echo JAVA_HOME points to a non-Java-21 installation: %JAVA_HOME%
            )
        )
        del temp_version.txt > nul 2>&1
    ) else (
        echo WARNING: JAVA_HOME is set but doesn't contain bin\java.exe: %JAVA_HOME%
    )
) else (
    echo JAVA_HOME is not set
)

echo.
echo Checking system PATH for Java 21...

:: Check for Java in PATH
where java 2>nul >nul
if not errorlevel 1 (
    for /f "tokens=*" %%j in ('where java') do (
        set "JAVA_EXE=%%j"
        set "JAVA_PATH=!JAVA_EXE:~0,-9!"
        
        "!JAVA_EXE!" -version 2>temp_version.txt
        findstr /c:"version" /c:"openjdk version" /c:"java version" temp_version.txt > nul
        if not errorlevel 1 (
            findstr /c:"21" /c:"21." temp_version.txt > nul
            if not errorlevel 1 (
                echo FOUND Java 21 in PATH: !JAVA_PATH!
                echo !JAVA_PATH! [PATH] >> java21_locations.txt
                set FOUND_JAVA21=1
            ) else (
                echo Current PATH Java is not version 21
            )
        )
        del temp_version.txt > nul 2>&1
    )
) else (
    echo Java is not in PATH
)

echo.
if %FOUND_JAVA21%==1 (
    echo ===== Java 21 installations found =====
    echo The following Java 21 installations were found:
    echo.
    type java21_locations.txt
    echo.
    echo These locations have been saved to java21_locations.txt
    echo To use one of these installations, either:
    echo 1. Set org.gradle.java.home in gradle.properties to the path
    echo 2. Set JAVA_HOME environment variable before building
    echo 3. Use the compatible-build.bat script which will prompt you for the path
) else (
    echo ===== No Java 21 installations found =====
    echo.
    echo Please install Java 21 and try again.
    echo Recommended downloads:
    echo - Eclipse Temurin: https://adoptium.net/
    echo - Oracle JDK: https://www.oracle.com/java/technologies/downloads/
    echo.
    echo Alternatively, download Java 21 through IntelliJ IDEA:
    echo 1. Open IntelliJ IDEA
    echo 2. Go to File → Project Structure → Platform Settings → SDKs
    echo 3. Click + and select "Download JDK..."
    echo 4. Select version 21 and a vendor (e.g., Eclipse Temurin)
    echo 5. Click Download
)

echo.
endlocal