@echo off
:: Java 21 Detection Tool for ModForge IntelliJ Plugin
:: This script will find all Java 21 installations on your system

setlocal EnableDelayedExpansion

echo ===== Java 21 Detection Tool for ModForge =====
echo.
echo This tool will scan your system for all Java 21 installations
echo to help identify which one to use for building the plugin.
echo.

:: Current Java in path
echo Checking current Java version:
for /f "tokens=* USEBACKQ" %%v in ('java -version 2^>^&1') do (
    echo %%v | findstr /i "version" > nul
    if not errorlevel 1 (
        echo %%v
        echo %%v | findstr /i "21" > nul
        if not errorlevel 1 (
            echo DEFAULT JAVA: ^ Java 21 is already your default Java!
            for /f "tokens=*" %%j in ('where java') do (
                echo Path: %%~dpj..
            )
        )
    )
)

echo.
echo ===== SCANNING FOR ALL JAVA 21 INSTALLATIONS =====
echo.

:: Make a comprehensive list of all places to search
echo Scanning IntelliJ IDEA locations...
echo.

set FOUND=0

:: Check IntelliJ IDEA JDKs directory
if exist "%USERPROFILE%\.jdks" (
    echo Scanning IntelliJ IDEA JDKs folder...
    for /f "tokens=*" %%i in ('dir /b "%USERPROFILE%\.jdks"') do (
        if exist "%USERPROFILE%\.jdks\%%i\bin\java.exe" (
            for /f "tokens=*" %%v in ('"%USERPROFILE%\.jdks\%%i\bin\java.exe" -version 2^>^&1') do (
                echo %%v | findstr /i "version" > nul
                if not errorlevel 1 (
                    echo %%v | findstr /i "21" > nul
                    if not errorlevel 1 (
                        echo [FOUND] Java 21 in JDKs folder: %USERPROFILE%\.jdks\%%i
                        set /a FOUND+=1
                    )
                )
            )
        )
    )
)

:: Check JetBrains Toolbox installations
if exist "%LOCALAPPDATA%\JetBrains\Toolbox" (
    echo Scanning JetBrains Toolbox locations...
    for /f "tokens=*" %%d in ('dir /b /s /a:d "%LOCALAPPDATA%\JetBrains\Toolbox\apps\*jbr*"') do (
        if exist "%%d\bin\java.exe" (
            for /f "tokens=*" %%v in ('%%d\bin\java.exe -version 2^>^&1') do (
                echo %%v | findstr /i "version" > nul
                if not errorlevel 1 (
                    echo %%v | findstr /i "21" > nul
                    if not errorlevel 1 (
                        echo [FOUND] Java 21 in Toolbox: %%d
                        set /a FOUND+=1
                    )
                )
            )
        )
    )
)

:: Check standard IntelliJ installations
if exist "%LOCALAPPDATA%\JetBrains" (
    echo Scanning IntelliJ IDEA installations...
    for /f "tokens=*" %%d in ('dir /b "%LOCALAPPDATA%\JetBrains"') do (
        if exist "%LOCALAPPDATA%\JetBrains\%%d\jbr\bin\java.exe" (
            for /f "tokens=*" %%v in ('"%LOCALAPPDATA%\JetBrains\%%d\jbr\bin\java.exe" -version 2^>^&1') do (
                echo %%v | findstr /i "version" > nul
                if not errorlevel 1 (
                    echo %%v | findstr /i "21" > nul
                    if not errorlevel 1 (
                        echo [FOUND] Java 21 in IntelliJ installation: %LOCALAPPDATA%\JetBrains\%%d\jbr
                        set /a FOUND+=1
                    )
                )
            )
        )
    )
)

:: Check Program Files locations
echo.
echo Scanning standard installation locations...
echo.

set "STD_PATTERNS=Eclipse Adoptium\jdk-21* Java\jdk-21* BellSoft\LibericaJDK-21* Amazon Corretto\jdk21*"

for %%d in ("%ProgramFiles%" "%ProgramFiles(x86)%" "C:\") do (
    for %%p in (%STD_PATTERNS%) do (
        if exist "%%~d\%%p\bin\java.exe" (
            for /f "tokens=*" %%j in ('dir /b /a:d "%%~d\%%p"') do (
                echo [FOUND] Java 21 at: %%~d\%%p
                set /a FOUND+=1
            )
        )
    )
)

echo.
echo ===== SCAN COMPLETE =====
echo.

if !FOUND! EQU 0 (
    echo No Java 21 installations were found on this system.
    echo.
    echo Please install Java 21 from one of these sources:
    echo 1. IntelliJ IDEA: File ^> Project Structure ^> Project ^> SDK ^> Add SDK ^> Download JDK
    echo 2. Eclipse Adoptium: https://adoptium.net/
    echo 3. Oracle: https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html
) else (
    echo Found !FOUND! Java 21 installation^(s^) on your system.
    echo.
    echo To use one of these Java installations for building:
    echo 1. Run the fix-build.bat script which will auto-detect Java 21
    echo 2. Or manually edit gradle.properties and add this line with the correct path:
    echo    org.gradle.java.home=PATH_TO_JAVA_21
)
echo.
echo Press any key to exit...
pause > nul