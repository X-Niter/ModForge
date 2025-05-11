@echo off
setlocal enabledelayedexpansion

REM ===================================
REM ModForge Builder
REM Advanced build and analysis script
REM ===================================

set "LOG_DIR=logs"
set "BUILD_LOG=%LOG_DIR%\build.log"
set "ERROR_LOG=%LOG_DIR%\error_analysis.log"
set "COMPATIBILITY_REPORT=%LOG_DIR%\compatibility_issues.md"
set "RESOLUTION_ERRORS_REPORT=%LOG_DIR%\resolution_errors.md"
set "TEMP_DIR=%TEMP%\modforge-build"

echo ===== ModForge Builder =====
echo.
echo Starting build process on %DATE% %TIME%
echo.

REM Create logs directory and temp directory
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"
if not exist "%TEMP_DIR%" mkdir "%TEMP_DIR%"

REM Clear previous logs
if exist "%BUILD_LOG%" del "%BUILD_LOG%"
if exist "%ERROR_LOG%" del "%ERROR_LOG%"
if exist "%COMPATIBILITY_REPORT%" del "%COMPATIBILITY_REPORT%"
if exist "%RESOLUTION_ERRORS_REPORT%" del "%RESOLUTION_ERRORS_REPORT%"

REM -----------------------------------------
REM First attempt - Standard build
REM -----------------------------------------
echo Building plugin with Gradle...
call gradlew.bat clean buildPlugin --stacktrace > "%BUILD_LOG%" 2>&1

REM Check build result
findstr /i /c:"BUILD SUCCESSFUL" "%BUILD_LOG%" > nul
if %ERRORLEVEL% equ 0 (
    echo Build successful!
    goto :build_success
) else (
    echo First build attempt failed. Retrying with additional memory...
    
    REM Try with more memory
    call gradlew.bat clean buildPlugin --stacktrace -Dorg.gradle.jvmargs=-Xmx2g >> "%BUILD_LOG%" 2>&1
    
    findstr /i /c:"BUILD SUCCESSFUL" "%BUILD_LOG%" > nul
    if %ERRORLEVEL% equ 0 (
        echo Build successful with additional memory!
        goto :build_success
    ) else (
        echo Second build attempt failed. Trying with full error reporting...
        
        REM Try with full error reporting
        call gradlew.bat clean buildPlugin --stacktrace -Dcompiler.args="-Xmaxerrs 0 -Xmaxwarns 0" >> "%BUILD_LOG%" 2>&1
        
        findstr /i /c:"BUILD SUCCESSFUL" "%BUILD_LOG%" > nul
        if %ERRORLEVEL% equ 0 (
            echo Build successful with full error reporting!
            goto :build_success
        ) else (
            echo All build attempts failed. Generating error analysis...
            goto :analyze_errors
        )
    )
)

:analyze_errors
echo.
echo Analyzing build errors...

REM Extract different types of errors
findstr /i /c:"error: cannot find symbol" "%BUILD_LOG%" > "%TEMP_DIR%\symbol_errors.txt"
findstr /i /c:"error: incompatible types" "%BUILD_LOG%" > "%TEMP_DIR%\type_errors.txt"
findstr /i /c:"error: method does not override" "%BUILD_LOG%" > "%TEMP_DIR%\override_errors.txt"
findstr /i /c:"error:" /c:"warning:" "%BUILD_LOG%" | findstr /v /i /c:"cannot find symbol" /c:"incompatible types" /c:"method does not override" > "%TEMP_DIR%\other_errors.txt"

REM Count errors
for /f %%a in ('type "%TEMP_DIR%\symbol_errors.txt" ^| find /c /v ""') do set SYMBOL_ERRORS=%%a
for /f %%a in ('type "%TEMP_DIR%\type_errors.txt" ^| find /c /v ""') do set TYPE_ERRORS=%%a
for /f %%a in ('type "%TEMP_DIR%\override_errors.txt" ^| find /c /v ""') do set OVERRIDE_ERRORS=%%a
for /f %%a in ('type "%TEMP_DIR%\other_errors.txt" ^| find /c /v ""') do set OTHER_ERRORS=%%a

set /a TOTAL_ERRORS=SYMBOL_ERRORS+TYPE_ERRORS+OVERRIDE_ERRORS+OTHER_ERRORS

echo.
echo Error Analysis Complete
echo -----------------------
echo Cannot find symbol errors: %SYMBOL_ERRORS%
echo Incompatible types errors: %TYPE_ERRORS% 
echo Override errors: %OVERRIDE_ERRORS%
echo Other errors: %OTHER_ERRORS%
echo Total errors: %TOTAL_ERRORS%
echo.

REM Create error analysis log
echo # ModForge Error Analysis > "%ERROR_LOG%"
echo Generated on %DATE% %TIME% >> "%ERROR_LOG%"
echo. >> "%ERROR_LOG%"

echo ## Error Summary >> "%ERROR_LOG%"
echo. >> "%ERROR_LOG%"
echo - Cannot find symbol errors: %SYMBOL_ERRORS% >> "%ERROR_LOG%"
echo - Incompatible types errors: %TYPE_ERRORS% >> "%ERROR_LOG%"
echo - Override errors: %OVERRIDE_ERRORS% >> "%ERROR_LOG%"
echo - Other errors: %OTHER_ERRORS% >> "%ERROR_LOG%"
echo - Total errors: %TOTAL_ERRORS% >> "%ERROR_LOG%"
echo. >> "%ERROR_LOG%"

REM Analyze resolution errors from the build log (missing classes or methods)
findstr /i /c:"cannot find symbol" /c:"cannot resolve symbol" /c:"cannot access" /c:"cannot be applied" "%BUILD_LOG%" > "%TEMP_DIR%\build_resolution_errors.txt"

REM Create resolution errors report
echo # ModForge Resolution Error Analysis > "%RESOLUTION_ERRORS_REPORT%"
echo Generated on %DATE% %TIME% >> "%RESOLUTION_ERRORS_REPORT%"
echo. >> "%RESOLUTION_ERRORS_REPORT%"

echo ## Resolution Errors from Build Log >> "%RESOLUTION_ERRORS_REPORT%"
echo. >> "%RESOLUTION_ERRORS_REPORT%"
echo The following resolution errors were found during compilation: >> "%RESOLUTION_ERRORS_REPORT%"
echo. >> "%RESOLUTION_ERRORS_REPORT%"
echo --- Java Code --- >> "%RESOLUTION_ERRORS_REPORT%"
type "%TEMP_DIR%\build_resolution_errors.txt" >> "%RESOLUTION_ERRORS_REPORT%"
echo --- End Code --- >> "%RESOLUTION_ERRORS_REPORT%"
echo. >> "%RESOLUTION_ERRORS_REPORT%"

echo ## Fix Recommendations >> "%RESOLUTION_ERRORS_REPORT%"
echo. >> "%RESOLUTION_ERRORS_REPORT%"
echo 1. Update imports to use the latest package paths >> "%RESOLUTION_ERRORS_REPORT%"
echo 2. Use CompatibilityUtil for deprecated method calls >> "%RESOLUTION_ERRORS_REPORT%"
echo 3. Fix method signatures to match exactly what callers expect >> "%RESOLUTION_ERRORS_REPORT%"
echo 4. Add missing methods to service classes >> "%RESOLUTION_ERRORS_REPORT%"
echo 5. Implement proper getInstance() methods for service classes >> "%RESOLUTION_ERRORS_REPORT%"
echo. >> "%RESOLUTION_ERRORS_REPORT%"

echo Build analysis complete.
echo See logs for details:
echo - Build log: %BUILD_LOG%
echo - Error analysis: %ERROR_LOG%
echo - Resolution errors: %RESOLUTION_ERRORS_REPORT%
goto :end

:build_success
echo.
echo Build completed successfully.
echo Plugin file location: build\libs\modforge-intellij-plugin.jar
echo.

:end
REM Clean up temp files
if exist "%TEMP_DIR%" rmdir /s /q "%TEMP_DIR%"

echo.
echo ModForge build process completed on %DATE% %TIME%
endlocal