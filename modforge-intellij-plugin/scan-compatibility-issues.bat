@echo off
setlocal enabledelayedexpansion

REM ===================================
REM ModForge Compatibility Scanner
REM ===================================
REM
REM This script proactively scans the entire codebase for IntelliJ API compatibility issues
REM with IntelliJ IDEA 2025.1.1.1 (Build #IC-251.25410.129).

REM Default values
set "OUTPUT_FILE=compatibility-issues.md"
set "SOURCE_DIR=src\main\java"
set "TEMP_MATCHES=%TEMP%\compatibility_matches.txt"
set "TEMP_FILES=%TEMP%\compatibility_files.txt"

echo ==== Scanning codebase for compatibility issues ====

REM Check if the source directory exists
if not exist "%SOURCE_DIR%" (
    echo Source directory does not exist: %SOURCE_DIR%
    exit /b 1
)

REM Create output file with header
echo # ModForge IntelliJ IDEA 2025.1.1.1 Compatibility Issues > "%OUTPUT_FILE%"
echo. >> "%OUTPUT_FILE%"
echo This report contains potential compatibility issues found in the codebase. >> "%OUTPUT_FILE%"
echo Generated on %DATE% %TIME%. >> "%OUTPUT_FILE%"
echo. >> "%OUTPUT_FILE%"
echo ## Overview >> "%OUTPUT_FILE%"
echo. >> "%OUTPUT_FILE%"
echo This scan looks for deprecated APIs, problematic patterns, and compatibility issues >> "%OUTPUT_FILE%"
echo that might affect the plugin's functionality with IntelliJ IDEA 2025.1.1.1. >> "%OUTPUT_FILE%"
echo. >> "%OUTPUT_FILE%"

REM Add known deprecations as a list instead of a table
echo ## Known Deprecated APIs and Replacements >> "%OUTPUT_FILE%"
echo. >> "%OUTPUT_FILE%"
echo * **Project.getBaseDir()** - Replace with CompatibilityUtil.getProjectBaseDir(project) - Removed in 2020.3+ >> "%OUTPUT_FILE%"
echo * **CacheUpdater** - Replace with CompatibilityUtil.refreshAll(project) - Removed in 2020.1+ >> "%OUTPUT_FILE%"
echo * **CacheUpdaterFacade** - Replace with CompatibilityUtil.refreshAll(project) - Removed in 2020.1+ >> "%OUTPUT_FILE%"
echo * **ApplicationManager.getApplication().runReadAction()** - Replace with CompatibilityUtil.runReadAction() >> "%OUTPUT_FILE%"
echo * **ApplicationManager.getApplication().runWriteAction()** - Replace with CompatibilityUtil.runWriteAction() >> "%OUTPUT_FILE%"
echo. >> "%OUTPUT_FILE%"

REM Find all Java files
dir /s /b "%SOURCE_DIR%\*.java" > "%TEMP_FILES%" 2>nul

echo Searching for potentially problematic API usage...

REM Count total files
set /a TOTAL_FILES=0
for /f %%f in (%TEMP_FILES%) do set /a TOTAL_FILES+=1

REM Initialize counters
set /a TOTAL_ISSUES=0
set /a AFFECTED_FILES=0

if exist found_file.tmp del found_file.tmp 2>nul
if exist "%TEMP_MATCHES%" del "%TEMP_MATCHES%" 2>nul

REM Process each file
set PROCESSED=0

echo Scanning %TOTAL_FILES% Java files...

if %TOTAL_FILES% equ 0 (
    echo No Java files found in %SOURCE_DIR%
    goto CLEANUP
)

for /f "delims=" %%f in (%TEMP_FILES%) do (
    set /a PROCESSED+=1
    
    REM Progress indicator
    set /a MOD=!PROCESSED! %% 10
    if !MOD! equ 0 (
        echo Processed !PROCESSED! of %TOTAL_FILES% files...
    )
    
    REM Check for key patterns
    for %%p in (getBaseDir CacheUpdater CacheUpdaterFacade runReadAction runWriteAction getSelectedTextEditor ApplicationComponent ProjectComponent ModuleComponent createXmlTag) do (
        findstr /c:"%%p" "%%f" >nul 2>&1
        if !ERRORLEVEL! equ 0 (
            echo %%f:%%p >> "%TEMP_MATCHES%"
            echo 1 > found_file.tmp
        )
    )
    
    if exist found_file.tmp (
        set /a AFFECTED_FILES+=1
        del found_file.tmp 2>nul
    )
)

REM Count total issues
if exist "%TEMP_MATCHES%" (
    for /f %%a in ('type "%TEMP_MATCHES%" ^| find /c /v ""') do set TOTAL_ISSUES=%%a
)

echo Scan complete. Found %TOTAL_ISSUES% potential issues in %AFFECTED_FILES% files.

REM Add summary to output file
echo ## Summary >> "%OUTPUT_FILE%"
echo. >> "%OUTPUT_FILE%"
echo * Total Java files scanned: %TOTAL_FILES% >> "%OUTPUT_FILE%"
echo * Files with potential compatibility issues: %AFFECTED_FILES% >> "%OUTPUT_FILE%"
echo * Total potential issues found: %TOTAL_ISSUES% >> "%OUTPUT_FILE%"
echo. >> "%OUTPUT_FILE%"
echo ## Detailed Issue List >> "%OUTPUT_FILE%"
echo. >> "%OUTPUT_FILE%"

REM Process and add issues to output file
echo Processing issues and generating report...

if exist "%TEMP_MATCHES%" (
    for /f "tokens=1,2 delims=:" %%a in (%TEMP_MATCHES%) do (
        echo ### Issue in %%a >> "%OUTPUT_FILE%"
        echo. >> "%OUTPUT_FILE%"
        echo * Potential problem: `%%b` >> "%OUTPUT_FILE%"
        
        REM Add suggested fix based on pattern
        if "%%b"=="getBaseDir" (
            echo * **Suggested fix:** Replace `Project.getBaseDir()` with `CompatibilityUtil.getProjectBaseDir(project)` >> "%OUTPUT_FILE%"
        ) else if "%%b"=="CacheUpdater" (
            echo * **Suggested fix:** Replace with `CompatibilityUtil.refreshAll(project)` >> "%OUTPUT_FILE%"
        ) else if "%%b"=="runReadAction" (
            echo * **Suggested fix:** Replace with `CompatibilityUtil.runReadAction(() -> { ... })` >> "%OUTPUT_FILE%"
        ) else if "%%b"=="runWriteAction" (
            echo * **Suggested fix:** Replace with `CompatibilityUtil.runWriteAction(() -> { ... })` >> "%OUTPUT_FILE%"
        ) else if "%%b"=="getSelectedTextEditor" (
            echo * **Suggested fix:** Replace with `CompatibilityUtil.getSelectedTextEditor(project)` >> "%OUTPUT_FILE%"
        ) else if "%%b"=="ProjectComponent" (
            echo * **Suggested fix:** Replace with `@Service(Service.Level.PROJECT)` annotation >> "%OUTPUT_FILE%"
        ) else if "%%b"=="ApplicationComponent" (
            echo * **Suggested fix:** Replace with `@Service(Service.Level.APPLICATION)` annotation >> "%OUTPUT_FILE%"
        ) else (
            echo * **Suggested fix:** Check compatibility with IntelliJ IDEA 2025.1.1.1 and use `CompatibilityUtil` methods where appropriate >> "%OUTPUT_FILE%"
        )
        
        echo. >> "%OUTPUT_FILE%"
    )
) else (
    echo No compatibility issues found! >> "%OUTPUT_FILE%"
)

REM Add compatibility guide
echo ## Compatibility Guide >> "%OUTPUT_FILE%"
echo. >> "%OUTPUT_FILE%"
echo ### Key IntelliJ IDEA 2025.1.1.1 Compatibility Requirements: >> "%OUTPUT_FILE%"
echo. >> "%OUTPUT_FILE%"
echo 1. **Replace deprecated Project methods** >> "%OUTPUT_FILE%"
echo    - Use `CompatibilityUtil.getProjectBaseDir(project)` instead of `project.getBaseDir()` >> "%OUTPUT_FILE%"
echo    - Use `ProjectRootManager.getInstance(project).getContentRoots()` for content roots >> "%OUTPUT_FILE%"
echo. >> "%OUTPUT_FILE%"
echo 2. **Update Threading** >> "%OUTPUT_FILE%"
echo    - Use `CompatibilityUtil.runReadAction()` instead of direct ApplicationManager calls >> "%OUTPUT_FILE%"
echo    - Use `CompatibilityUtil.runWriteAction()` for write operations >> "%OUTPUT_FILE%"
echo    - Use virtual threads with `ThreadUtils.createVirtualThreadExecutor()` for better performance >> "%OUTPUT_FILE%"
echo. >> "%OUTPUT_FILE%"
echo 3. **Update Service Management** >> "%OUTPUT_FILE%"
echo    - Use `@Service` annotation instead of component registration >> "%OUTPUT_FILE%"
echo    - Register services in plugin.xml with appropriate level (application/project) >> "%OUTPUT_FILE%"
echo    - Remove usage of ProjectComponent, ApplicationComponent, etc. >> "%OUTPUT_FILE%"
echo. >> "%OUTPUT_FILE%"
echo 4. **Plugin Configuration** >> "%OUTPUT_FILE%"
echo    - Use full element tags in plugin.xml (no shorthand) >> "%OUTPUT_FILE%"
echo    - Specify proper since/until build numbers >> "%OUTPUT_FILE%"
echo    - Use only necessary plugin dependencies >> "%OUTPUT_FILE%"

:CLEANUP
REM Clean up
if exist "%TEMP_MATCHES%" del "%TEMP_MATCHES%" 2>nul
if exist "%TEMP_FILES%" del "%TEMP_FILES%" 2>nul

echo Compatibility analysis complete! Report written to: %OUTPUT_FILE%
echo.
echo Next steps:
echo   1. Review the detailed compatibility report
echo   2. Prioritize fixing the most common issues first
echo   3. Use CompatibilityUtil for consistent API access across IntelliJ versions
echo   4. Run this scan again after making changes to verify improvements

endlocal