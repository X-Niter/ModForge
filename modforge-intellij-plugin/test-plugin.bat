@echo off
REM ModForge IntelliJ Plugin Installation/Testing Script
REM For IntelliJ IDEA 2025.1 compatibility
REM Version 2.1.0

echo ===================================================
echo ModForge IntelliJ Plugin Installation/Testing Tool
echo ===================================================
echo.

REM Set variables for important paths
set PLUGIN_JAR=dist\ModForge-2.1.0.zip
set LOG_FILE=plugin-test.log

echo Checking environment...
echo Timestamp: %date% %time% > %LOG_FILE%
echo System: %OS% >> %LOG_FILE%
echo Java Version: >> %LOG_FILE%
java -version 2>> %LOG_FILE%

REM Check if plugin file exists
if not exist "%PLUGIN_JAR%" (
    echo ERROR: Plugin file not found at "%PLUGIN_JAR%"
    echo Please build the plugin first or check the path
    echo Plugin file not found at "%CD%\%PLUGIN_JAR%" >> %LOG_FILE%
    goto :ERROR
)

echo Plugin file found: %PLUGIN_JAR%
echo Plugin file found: %CD%\%PLUGIN_JAR% >> %LOG_FILE%

REM Find IntelliJ IDEA installation
echo Searching for IntelliJ IDEA 2025.1 installation...
set IDEA_FOUND=0

REM Try common installation paths for IntelliJ IDEA 2025.1
set IDEA_PATHS=^
"C:\Program Files\JetBrains\IntelliJ IDEA 2025.1\bin\idea64.exe" ^
"C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2025.1\bin\idea64.exe" ^
"C:\Program Files (x86)\JetBrains\IntelliJ IDEA 2025.1\bin\idea.exe" ^
"C:\Program Files (x86)\JetBrains\IntelliJ IDEA Community Edition 2025.1\bin\idea.exe" ^
"%USERPROFILE%\AppData\Local\JetBrains\Toolbox\apps\IDEA-U\ch-0\251.*\bin\idea64.exe" ^
"%USERPROFILE%\AppData\Local\JetBrains\Toolbox\apps\IDEA-C\ch-0\251.*\bin\idea64.exe"

for %%I in (%IDEA_PATHS%) do (
    echo Checking: %%I
    if exist %%I (
        set IDEA_PATH=%%I
        set IDEA_FOUND=1
        echo Found IntelliJ IDEA: %%I
        echo Found IntelliJ IDEA: %%I >> %LOG_FILE%
        goto :FOUND_IDEA
    )
)

:FOUND_IDEA
if %IDEA_FOUND% EQU 0 (
    echo IntelliJ IDEA 2025.1 not found in common locations.
    echo.
    echo Please enter the full path to IntelliJ IDEA executable:
    echo Example: C:\Program Files\JetBrains\IntelliJ IDEA 2025.1\bin\idea64.exe
    echo.
    set /p IDEA_PATH="Path: "
    
    if not exist "!IDEA_PATH!" (
        echo ERROR: The specified file does not exist.
        echo Custom path not found: !IDEA_PATH! >> %LOG_FILE%
        goto :ERROR
    )
    
    echo Using custom IntelliJ IDEA path: !IDEA_PATH!
    echo Using custom IntelliJ IDEA path: !IDEA_PATH! >> %LOG_FILE%
)

echo.
echo ===================================================
echo Installation options:
echo ===================================================
echo 1. Install plugin from disk
echo 2. Run IntelliJ IDEA with plugin (for testing)
echo 3. Exit
echo.
choice /C 123 /M "Select an option:"

if errorlevel 3 goto :EOF
if errorlevel 2 goto :RUN_TEST
if errorlevel 1 goto :INSTALL

:INSTALL
echo.
echo Installing plugin...
echo Executing: "%IDEA_PATH%" installPluginFromDisk "%CD%\%PLUGIN_JAR%" >> %LOG_FILE%

"%IDEA_PATH%" installPluginFromDisk "%CD%\%PLUGIN_JAR%"
if %ERRORLEVEL% NEQ 0 (
    echo Installation command failed with error code: %ERRORLEVEL% >> %LOG_FILE%
    echo WARNING: There might have been an issue with installation.
    echo Please check if IntelliJ IDEA opened a dialog to complete installation.
) else (
    echo Plugin installation command executed successfully.
    echo Installation command succeeded >> %LOG_FILE%
)

echo.
echo Plugin installation initiated. If IntelliJ IDEA opened, please follow the on-screen instructions.
echo After installation, restart IntelliJ IDEA to activate the plugin.
goto :SUCCESS

:RUN_TEST
echo.
echo Running IntelliJ IDEA with plugin for testing...
echo Creating temporary plugin directory... >> %LOG_FILE%

set TEMP_PLUGINS_DIR=%TEMP%\ModForgePluginTest
if exist "%TEMP_PLUGINS_DIR%" rd /s /q "%TEMP_PLUGINS_DIR%"
mkdir "%TEMP_PLUGINS_DIR%"

echo Copying plugin to: %TEMP_PLUGINS_DIR% >> %LOG_FILE%
copy "%PLUGIN_JAR%" "%TEMP_PLUGINS_DIR%\" >> %LOG_FILE%

echo Executing: "%IDEA_PATH%" -Didea.plugins.path="%TEMP_PLUGINS_DIR%" >> %LOG_FILE%
start "" "%IDEA_PATH%" -Didea.plugins.path="%TEMP_PLUGINS_DIR%"

echo.
echo IntelliJ IDEA started with custom plugin directory.
echo Plugin should be loaded from: %TEMP_PLUGINS_DIR%
echo Note: This is a temporary installation for testing only.
goto :SUCCESS

:SUCCESS
echo.
echo ===================================================
echo Process completed successfully!
echo See %LOG_FILE% for details.
echo ===================================================
goto :EOF

:ERROR
echo.
echo ===================================================
echo ERROR: Process failed. See %LOG_FILE% for details.
echo ===================================================
pause
exit /b 1