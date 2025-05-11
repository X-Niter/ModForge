@echo off
setlocal enabledelayedexpansion

REM ===================================
REM ModForge Simple Builder v1.0
REM ===================================
REM
REM This script handles the complete build process for ModForge IntelliJ Plugin
REM using a simplified approach to avoid syntax errors.

REM Default values and directories
set "LOG_DIR=logs"
set "BUILD_LOG=%LOG_DIR%\build.log"
set "ERROR_LOG=%LOG_DIR%\error_analysis.log"
set "COMPATIBILITY_REPORT=%LOG_DIR%\compatibility-issues.md"

echo ===== ModForge Simple Builder =====
echo.
echo Starting build process on %DATE% %TIME%
echo.

REM Create necessary directories
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"

REM Clear previous logs
if exist "%BUILD_LOG%" del "%BUILD_LOG%"
if exist "%ERROR_LOG%" del "%ERROR_LOG%"

REM ===================================
REM Build Logic
REM ===================================
set BUILD_SUCCESS=0

echo Building plugin with Gradle...
call gradlew.bat clean buildPlugin --stacktrace > "%BUILD_LOG%" 2>&1

REM Check if build succeeded
findstr /i /c:"BUILD SUCCESSFUL" "%BUILD_LOG%" > nul
if !ERRORLEVEL! equ 0 (
    set BUILD_SUCCESS=1
    echo Build successful!
) else (
    echo Build failed. Analyzing errors...
    
    REM Create error report
    echo # ModForge Build Error Analysis > "%ERROR_LOG%"
    echo. >> "%ERROR_LOG%"
    echo ## Build Errors >> "%ERROR_LOG%"
    echo. >> "%ERROR_LOG%"
    
    REM Count and extract different error types
    findstr /i /c:"cannot find symbol" "%BUILD_LOG%" > "%TEMP%\symbol_errors.txt"
    for /f %%a in ('type "%TEMP%\symbol_errors.txt" ^| find /c /v ""') do set SYMBOL_ERRORS=%%a
    
    findstr /i /c:"incompatible types" "%BUILD_LOG%" > "%TEMP%\type_errors.txt"
    for /f %%a in ('type "%TEMP%\type_errors.txt" ^| find /c /v ""') do set TYPE_ERRORS=%%a
    
    echo ### Symbol Errors: %SYMBOL_ERRORS% >> "%ERROR_LOG%"
    echo. >> "%ERROR_LOG%"
    echo ```java >> "%ERROR_LOG%"
    type "%TEMP%\symbol_errors.txt" >> "%ERROR_LOG%"
    echo ``` >> "%ERROR_LOG%"
    echo. >> "%ERROR_LOG%"
    
    echo ### Type Errors: %TYPE_ERRORS% >> "%ERROR_LOG%"
    echo. >> "%ERROR_LOG%"
    echo ```java >> "%ERROR_LOG%"
    type "%TEMP%\type_errors.txt" >> "%ERROR_LOG%"
    echo ``` >> "%ERROR_LOG%"
    echo. >> "%ERROR_LOG%"
    
    REM Look for common missing methods
    echo ## Common Missing Methods >> "%ERROR_LOG%"
    echo. >> "%ERROR_LOG%"
    echo These are common methods that might be missing in your service classes: >> "%ERROR_LOG%"
    echo. >> "%ERROR_LOG%"
    
    REM Check ModForgeSettings
    findstr /i /c:"settings" /c:"isPatternRecognition" /c:"getAccessToken" "%BUILD_LOG%" > "%TEMP%\settings_errors.txt"
    if exist "%TEMP%\settings_errors.txt" (
        echo ### ModForgeSettings Methods >> "%ERROR_LOG%"
        echo. >> "%ERROR_LOG%"
        echo - getAccessToken() >> "%ERROR_LOG%"
        echo - isPatternRecognition() >> "%ERROR_LOG%"
        echo - getGitHubUsername() >> "%ERROR_LOG%"
        echo - openSettings(Project) >> "%ERROR_LOG%"
        echo. >> "%ERROR_LOG%"
    )
    
    REM Check ModAuthenticationManager
    findstr /i /c:"authManager" /c:"login" /c:"logout" /c:"getUsername" "%BUILD_LOG%" > "%TEMP%\auth_errors.txt"
    if exist "%TEMP%\auth_errors.txt" (
        echo ### ModAuthenticationManager Methods >> "%ERROR_LOG%"
        echo. >> "%ERROR_LOG%"
        echo - login(String, String) >> "%ERROR_LOG%"
        echo - logout() >> "%ERROR_LOG%"
        echo - getUsername() >> "%ERROR_LOG%"
        echo. >> "%ERROR_LOG%"
    )
    
    REM Check AutonomousCodeGenerationService
    findstr /i /c:"codeGenService" /c:"getInstance" /c:"generateCode" /c:"fixCode" "%BUILD_LOG%" > "%TEMP%\codegen_errors.txt"
    if exist "%TEMP%\codegen_errors.txt" (
        echo ### AutonomousCodeGenerationService Methods >> "%ERROR_LOG%"
        echo. >> "%ERROR_LOG%"
        echo - static getInstance(Project) >> "%ERROR_LOG%"
        echo - generateCode(String, VirtualFile, String) >> "%ERROR_LOG%"
        echo - fixCode(String, String, String) >> "%ERROR_LOG%"
        echo - generateDocumentation(String, Object) >> "%ERROR_LOG%"
        echo - explainCode(String, Object) >> "%ERROR_LOG%"
        echo. >> "%ERROR_LOG%"
    )
    
    REM Add compatibility note
    echo ## IntelliJ Compatibility Note >> "%ERROR_LOG%"
    echo. >> "%ERROR_LOG%"
    echo Remember to use CompatibilityUtil methods for better IntelliJ API compatibility: >> "%ERROR_LOG%"
    echo. >> "%ERROR_LOG%"
    echo 1. Use CompatibilityUtil.getProjectBaseDir(project) instead of project.getBaseDir() >> "%ERROR_LOG%"
    echo 2. Use CompatibilityUtil.runReadAction() instead of ApplicationManager.getApplication().runReadAction() >> "%ERROR_LOG%"
    echo 3. Use CompatibilityUtil.runWriteAction() instead of ApplicationManager.getApplication().runWriteAction() >> "%ERROR_LOG%"
    echo 4. Replace Components with Services using @Service annotations >> "%ERROR_LOG%"
    
    REM Print error summary
    echo Build failed with the following error counts:
    echo - Symbol errors (cannot find symbol): %SYMBOL_ERRORS%
    echo - Type errors (incompatible types): %TYPE_ERRORS%
    echo - Total errors: See build log for details
)

echo.
if %BUILD_SUCCESS% equ 1 (
    echo ===== Build completed successfully! =====
    echo.
    echo Plugin has been built successfully.
    echo Plugin file location: build\libs\modforge-intellij-plugin.jar
) else (
    echo ===== Build failed =====
    echo.
    echo Please check the error analysis report:
    echo - Build log: %BUILD_LOG%
    echo - Error analysis: %ERROR_LOG%
)

echo.
echo Build process completed in %TIME%.
echo.
echo Thank you for using ModForge Simple Builder!
echo.

endlocal