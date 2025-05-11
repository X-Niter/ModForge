@echo off
setlocal enabledelayedexpansion

REM ===================================
REM Basic ModForge Builder
REM ===================================
REM Super simplified version with no advanced syntax

set "LOG_DIR=logs"
set "BUILD_LOG=%LOG_DIR%\build.log"
set "ERROR_LOG=%LOG_DIR%\error_analysis.log"

echo ===== Basic ModForge Builder =====
echo.
echo Starting build process on %DATE% %TIME%
echo.

REM Create logs directory
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"

REM Clear previous logs
if exist "%BUILD_LOG%" del "%BUILD_LOG%"
if exist "%ERROR_LOG%" del "%ERROR_LOG%"

REM -----------------------------------------
REM Basic build with all errors
REM -----------------------------------------
echo Building plugin with full error reporting...
call gradlew.bat clean buildPlugin --stacktrace -Dcompiler.args="-Xmaxerrs 0 -Xmaxwarns 0" > "%BUILD_LOG%" 2>&1

REM Check build result
findstr /i /c:"BUILD SUCCESSFUL" "%BUILD_LOG%" > nul
if %ERRORLEVEL% equ 0 (
    echo Build successful!
    echo.
    echo Plugin has been built successfully.
    echo Plugin file location: build\libs\modforge-intellij-plugin.jar
) else (
    echo Build failed! Creating error report...
    
    REM Simple error analysis
    echo MODFORGE BUILD ERROR ANALYSIS > "%ERROR_LOG%"
    echo ---------------------------------- >> "%ERROR_LOG%"
    echo Generated on %DATE% %TIME% >> "%ERROR_LOG%"
    echo. >> "%ERROR_LOG%"
    
    REM Extract and count errors
    findstr /i /c:"error:" "%BUILD_LOG%" > "%TEMP%\all_errors.txt"
    for /f %%a in ('type "%TEMP%\all_errors.txt" ^| find /c /v ""') do set TOTAL_ERRORS=%%a
    
    REM Symbol errors
    findstr /i /c:"cannot find symbol" "%BUILD_LOG%" > "%TEMP%\symbol_errors.txt"
    for /f %%a in ('type "%TEMP%\symbol_errors.txt" ^| find /c /v ""') do set SYMBOL_ERRORS=%%a
    
    REM Method errors
    findstr /i /c:"method" /c:"missing" "%BUILD_LOG%" > "%TEMP%\method_errors.txt"
    for /f %%a in ('type "%TEMP%\method_errors.txt" ^| find /c /v ""') do set METHOD_ERRORS=%%a
    
    echo ERROR COUNTS: >> "%ERROR_LOG%"
    echo Total errors: %TOTAL_ERRORS% >> "%ERROR_LOG%"
    echo Symbol errors: %SYMBOL_ERRORS% >> "%ERROR_LOG%"
    echo Method errors: %METHOD_ERRORS% >> "%ERROR_LOG%"
    echo. >> "%ERROR_LOG%"
    
    echo MISSING METHODS: >> "%ERROR_LOG%"
    echo ---------------------------------- >> "%ERROR_LOG%"
    findstr /i /c:"method" "%BUILD_LOG%" >> "%ERROR_LOG%"
    echo. >> "%ERROR_LOG%"
    echo. >> "%ERROR_LOG%"
    
    echo ALL ERRORS: >> "%ERROR_LOG%"
    echo ---------------------------------- >> "%ERROR_LOG%"
    type "%TEMP%\all_errors.txt" >> "%ERROR_LOG%"
    
    echo.
    echo Build failed with %TOTAL_ERRORS% errors
    echo See error report for details: %ERROR_LOG%
)

echo.
echo Build process completed.

REM Clean up temp files
if exist "%TEMP%\all_errors.txt" del "%TEMP%\all_errors.txt"
if exist "%TEMP%\symbol_errors.txt" del "%TEMP%\symbol_errors.txt"
if exist "%TEMP%\method_errors.txt" del "%TEMP%\method_errors.txt"

endlocal