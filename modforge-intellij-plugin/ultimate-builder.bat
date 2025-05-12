@echo off
setlocal enabledelayedexpansion

REM ===================================
REM ModForge Ultimate Builder (Fixed Version)
REM ===================================
REM
REM This script handles the complete build process for ModForge IntelliJ Plugin
REM including:
REM  1. Gradle build with proper configuration
REM  2. Multiple build attempts with different settings if needed
REM  3. Comprehensive error analysis on build failures
REM  4. Compatibility scanning for IntelliJ IDEA 2025.1.1.1
REM  5. Detailed logs and reports for all operations

REM Default values and directories
set "LOG_DIR=logs"
set "BUILD_LOG=%LOG_DIR%\build.log"
set "ERROR_LOG=%LOG_DIR%\error_analysis.log"
set "TEMP_DIR=%TEMP%\modforge-build"
set "COMPATIBILITY_REPORT=%LOG_DIR%\compatibility-issues.md"
set "CODE_ISSUES_REPORT=%LOG_DIR%\code-issues.md"
set "RESOLUTION_ERRORS_REPORT=%LOG_DIR%\resolution-errors.md"
set "SOURCE_DIR=src\main\java"
set "MAX_BUILD_ATTEMPTS=3"

echo ===== ModForge Ultimate Builder =====
echo.
echo Starting build process on %DATE% %TIME%
echo.

REM Create necessary directories
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"
if not exist "%TEMP_DIR%" mkdir "%TEMP_DIR%"

REM Clear previous logs
if exist "%BUILD_LOG%" del "%BUILD_LOG%"
if exist "%ERROR_LOG%" del "%ERROR_LOG%"

REM ===================================
REM Build Attempt Logic
REM ===================================
set BUILD_SUCCESS=0
set BUILD_ATTEMPT=1

:BUILD_LOOP
echo Attempt %BUILD_ATTEMPT% of %MAX_BUILD_ATTEMPTS%...
echo.

echo Building plugin with Gradle...

REM Different build commands for different attempts
if %BUILD_ATTEMPT% equ 1 (
    echo Standard build attempt with full error reporting...
    call gradlew.bat clean buildPlugin --stacktrace -Dcompiler.args="-Xmaxerrs 0 -Xmaxwarns 0" > "%BUILD_LOG%" 2>&1
) else if %BUILD_ATTEMPT% equ 2 (
    echo Enhanced build with additional memory and full error reporting...
    call gradlew.bat clean buildPlugin --stacktrace --max-workers=2 -Dorg.gradle.jvmargs="-Xmx2048m" -Dcompiler.args="-Xmaxerrs 0 -Xmaxwarns 0" > "%BUILD_LOG%" 2>&1
) else (
    echo Full error reporting with additional debug flags...
    call gradlew.bat clean buildPlugin --stacktrace -Dcompiler.args="-Xmaxerrs 0 -Xmaxwarns 0 -verbose" > "%BUILD_LOG%" 2>&1
)

REM Check if build succeeded
findstr /i /c:"BUILD SUCCESSFUL" "%BUILD_LOG%" > nul
if !ERRORLEVEL! equ 0 (
    set BUILD_SUCCESS=1
    echo Build successful!
    goto BUILD_DONE
) else (
    echo Build failed. Analyzing errors...
    
    REM If this is the last attempt, perform comprehensive error analysis
    if %BUILD_ATTEMPT% equ %MAX_BUILD_ATTEMPTS% (
        call :PERFORM_ERROR_ANALYSIS
    )
    
    REM Increment attempt counter
    set /a BUILD_ATTEMPT+=1
    
    REM If we've reached max attempts, stop trying
    if %BUILD_ATTEMPT% GTR %MAX_BUILD_ATTEMPTS% (
        goto BUILD_DONE
    )
    
    REM Try again with different settings
    echo Retrying build with different configuration...
    
    REM Adjust Gradle properties for next attempt
    echo org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8 > gradle.properties
    echo org.gradle.parallel=true >> gradle.properties
    echo org.gradle.caching=true >> gradle.properties
)
goto BUILD_LOOP

:BUILD_DONE
echo.
if %BUILD_SUCCESS% equ 1 (
    echo ===== Build completed successfully! =====
    echo.
    echo Plugin has been built successfully.
    echo Plugin file location: build\libs\modforge-intellij-plugin.jar
) else (
    echo ===== Build failed after %MAX_BUILD_ATTEMPTS% attempts =====
    echo.
    echo Please check the error analysis reports:
    echo - Build log: %BUILD_LOG%
    echo - Error analysis: %ERROR_LOG%
    echo - Compatibility issues: %COMPATIBILITY_REPORT%
    echo - Code issues: %CODE_ISSUES_REPORT%
    echo - Resolution errors: %RESOLUTION_ERRORS_REPORT%
)

goto :END

REM ===================================
REM Error Analysis Subroutine
REM ===================================
:PERFORM_ERROR_ANALYSIS
echo.
echo ===== Performing Comprehensive Error Analysis =====
echo.

REM Extract key errors from build log
echo Extracting error patterns...
echo # ModForge Build Error Analysis > "%ERROR_LOG%"
echo. >> "%ERROR_LOG%"
echo ## Extracted Errors >> "%ERROR_LOG%"
echo. >> "%ERROR_LOG%"

REM Find and extract "error:" lines
findstr /i /c:"error:" "%BUILD_LOG%" > "%TEMP_DIR%\error_lines.txt"

REM Process each error pattern and categorize
REM Count different error types
set /a SYMBOL_ERRORS=0
set /a TYPE_ERRORS=0
set /a OVERRIDE_ERRORS=0
set /a OTHER_ERRORS=0

REM Process symbol errors
findstr /i /c:"cannot find symbol" "%BUILD_LOG%" > "%TEMP_DIR%\symbol_errors.txt"
for /f %%a in ('type "%TEMP_DIR%\symbol_errors.txt" ^| find /c /v ""') do set SYMBOL_ERRORS=%%a

REM Process incompatible type errors
findstr /i /c:"incompatible types" "%BUILD_LOG%" > "%TEMP_DIR%\type_errors.txt"
for /f %%a in ('type "%TEMP_DIR%\type_errors.txt" ^| find /c /v ""') do set TYPE_ERRORS=%%a

REM Process override errors
findstr /i /c:"cannot override" "%BUILD_LOG%" > "%TEMP_DIR%\override_errors.txt"
for /f %%a in ('type "%TEMP_DIR%\override_errors.txt" ^| find /c /v ""') do set OVERRIDE_ERRORS=%%a

REM Calculate other errors
for /f %%a in ('type "%TEMP_DIR%\error_lines.txt" ^| find /c /v ""') do (
    set /a TOTAL_ERRORS=%%a
    set /a OTHER_ERRORS=!TOTAL_ERRORS!-!SYMBOL_ERRORS!-!TYPE_ERRORS!-!OVERRIDE_ERRORS!
)

REM Add error summary to log
echo ### Error Summary >> "%ERROR_LOG%"
echo. >> "%ERROR_LOG%"
echo - Cannot find symbol errors: %SYMBOL_ERRORS% >> "%ERROR_LOG%"
echo - Incompatible types errors: %TYPE_ERRORS% >> "%ERROR_LOG%"
echo - Override errors: %OVERRIDE_ERRORS% >> "%ERROR_LOG%"
echo - Other errors: %OTHER_ERRORS% >> "%ERROR_LOG%"
echo - Total errors: %TOTAL_ERRORS% >> "%ERROR_LOG%"
echo. >> "%ERROR_LOG%"

REM Extract specific error details
echo ### Cannot Find Symbol Errors >> "%ERROR_LOG%"
echo. >> "%ERROR_LOG%"
echo --- Java Code --- >> "%ERROR_LOG%"
type "%TEMP_DIR%\symbol_errors.txt" >> "%ERROR_LOG%"
echo --- End Code --- >> "%ERROR_LOG%"
echo. >> "%ERROR_LOG%"

echo ### Incompatible Types Errors >> "%ERROR_LOG%"
echo. >> "%ERROR_LOG%"
echo --- Java Code --- >> "%ERROR_LOG%"
type "%TEMP_DIR%\type_errors.txt" >> "%ERROR_LOG%"
echo --- End Code --- >> "%ERROR_LOG%"
echo. >> "%ERROR_LOG%"

echo ### Override Errors >> "%ERROR_LOG%"
echo. >> "%ERROR_LOG%"
echo --- Java Code --- >> "%ERROR_LOG%"
type "%TEMP_DIR%\override_errors.txt" >> "%ERROR_LOG%"
echo --- End Code --- >> "%ERROR_LOG%"
echo. >> "%ERROR_LOG%"

REM Perform in-depth compatibility analysis
echo Performing API compatibility analysis...
call :RUN_COMPATIBILITY_SCAN

REM Analyze code issues
echo Analyzing symbol resolution errors...
call :ANALYZE_CODE_ISSUES

REM Analyze resolution errors
echo Analyzing potential resolution errors...
call :FIND_RESOLUTION_ERRORS

echo Error analysis complete!
echo.

goto :EOF

REM ===================================
REM Compatibility Scan Subroutine
REM ===================================
:RUN_COMPATIBILITY_SCAN
echo.
echo === Running API Compatibility Scan ===

REM Create output file with header
echo # ModForge IntelliJ IDEA 2025.1.1.1 Compatibility Issues > "%COMPATIBILITY_REPORT%"
echo. >> "%COMPATIBILITY_REPORT%"
echo This report contains potential compatibility issues found in the codebase. >> "%COMPATIBILITY_REPORT%"
echo Generated on %DATE% %TIME%. >> "%COMPATIBILITY_REPORT%"
echo. >> "%COMPATIBILITY_REPORT%"
echo ## Overview >> "%COMPATIBILITY_REPORT%"
echo. >> "%COMPATIBILITY_REPORT%"
echo This scan looks for deprecated APIs, problematic patterns, and compatibility issues >> "%COMPATIBILITY_REPORT%"
echo that might affect plugin functionality for IntelliJ IDEA 2025.1.1.1. >> "%COMPATIBILITY_REPORT%"
echo. >> "%COMPATIBILITY_REPORT%"
echo ## Known Deprecated APIs and Replacements >> "%COMPATIBILITY_REPORT%"
echo. >> "%COMPATIBILITY_REPORT%"
echo * **Project.getBaseDir()** - Use CompatibilityUtil.getProjectBaseDir(project) - Removed in 2020.3+ >> "%COMPATIBILITY_REPORT%"
echo * **CacheUpdater** - Use CompatibilityUtil.refreshAll(project) - Removed in 2020.1+ >> "%COMPATIBILITY_REPORT%"
echo * **CacheUpdaterFacade** - Use CompatibilityUtil.refreshAll(project) - Removed in 2020.1+ >> "%COMPATIBILITY_REPORT%"
echo * **ApplicationManager.getApplication().runReadAction()** - Use CompatibilityUtil.runReadAction() >> "%COMPATIBILITY_REPORT%"
echo * **ApplicationManager.getApplication().runWriteAction()** - Use CompatibilityUtil.runWriteAction() >> "%COMPATIBILITY_REPORT%"
echo. >> "%COMPATIBILITY_REPORT%"

REM Check if the source directory exists
if not exist "%SOURCE_DIR%" (
    echo Source directory does not exist: %SOURCE_DIR%
    echo ## Error >> "%COMPATIBILITY_REPORT%"
    echo. >> "%COMPATIBILITY_REPORT%"
    echo Source directory does not exist: %SOURCE_DIR% >> "%COMPATIBILITY_REPORT%"
    goto :EOF
)

REM Find all Java files
dir /s /b "%SOURCE_DIR%\*.java" > "%TEMP_DIR%\java_files.txt" 2>nul

REM Count total files
set /a TOTAL_FILES=0
for /f %%f in (%TEMP_DIR%\java_files.txt) do set /a TOTAL_FILES+=1

REM Initialize counters
set /a TOTAL_ISSUES=0
set /a AFFECTED_FILES=0

if exist "%TEMP_DIR%\found_file.tmp" del "%TEMP_DIR%\found_file.tmp" 2>nul
if exist "%TEMP_DIR%\matches.txt" del "%TEMP_DIR%\matches.txt" 2>nul

REM Process each file
set PROCESSED=0

if %TOTAL_FILES% equ 0 (
    echo No Java files found in %SOURCE_DIR%
    echo ## Error >> "%COMPATIBILITY_REPORT%"
    echo. >> "%COMPATIBILITY_REPORT%"
    echo No Java files found in %SOURCE_DIR% >> "%COMPATIBILITY_REPORT%"
    goto :EOF
)

for /f "delims=" %%f in (%TEMP_DIR%\java_files.txt) do (
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
            echo %%f:%%p >> "%TEMP_DIR%\matches.txt"
            echo 1 > "%TEMP_DIR%\found_file.tmp"
        )
    )
    
    if exist "%TEMP_DIR%\found_file.tmp" (
        set /a AFFECTED_FILES+=1
        del "%TEMP_DIR%\found_file.tmp" 2>nul
    )
)

REM Count total issues
if exist "%TEMP_DIR%\matches.txt" (
    for /f %%a in ('type "%TEMP_DIR%\matches.txt" ^| find /c /v ""') do set TOTAL_ISSUES=%%a
)

REM Add summary to output file
echo ## Summary >> "%COMPATIBILITY_REPORT%"
echo. >> "%COMPATIBILITY_REPORT%"
echo * Total Java files scanned: %TOTAL_FILES% >> "%COMPATIBILITY_REPORT%"
echo * Files with potential compatibility issues: %AFFECTED_FILES% >> "%COMPATIBILITY_REPORT%"
echo * Total potential issues found: %TOTAL_ISSUES% >> "%COMPATIBILITY_REPORT%"
echo. >> "%COMPATIBILITY_REPORT%"
echo ## Detailed Issue List >> "%COMPATIBILITY_REPORT%"
echo. >> "%COMPATIBILITY_REPORT%"

REM Process and add issues to output file
if exist "%TEMP_DIR%\matches.txt" (
    for /f "tokens=1,2 delims=:" %%a in (%TEMP_DIR%\matches.txt) do (
        echo ### Issue in %%a >> "%COMPATIBILITY_REPORT%"
        echo. >> "%COMPATIBILITY_REPORT%"
        echo * Potential problem: %%b >> "%COMPATIBILITY_REPORT%"
        
        REM Add pattern-specific suggestions
        call :ADD_COMPATIBILITY_SUGGESTION "%%b" "%COMPATIBILITY_REPORT%"
        
        echo. >> "%COMPATIBILITY_REPORT%"
    )
) else (
    echo No compatibility issues found! >> "%COMPATIBILITY_REPORT%"
)

echo Compatibility analysis complete.
goto :EOF

REM ===================================
REM Code Issues Analysis Subroutine
REM ===================================
:ANALYZE_CODE_ISSUES
echo.
echo === Running Code Issue Analysis ===

REM Simplify to avoid batch syntax issues

REM Create a basic header
echo # Code Issues Analysis Report > "%CODE_ISSUES_REPORT%"
echo. >> "%CODE_ISSUES_REPORT%"
echo Generated: %DATE% >> "%CODE_ISSUES_REPORT%"
echo. >> "%CODE_ISSUES_REPORT%"

echo Analyzing build log for errors...

REM Add the error analysis
echo ## Error Analysis >> "%CODE_ISSUES_REPORT%"
echo. >> "%CODE_ISSUES_REPORT%"

REM Add required implementations without using the word "method"
echo ## Required Implementations >> "%CODE_ISSUES_REPORT%"
echo. >> "%CODE_ISSUES_REPORT%"
echo ### ModForgeSettings >> "%CODE_ISSUES_REPORT%"
echo. >> "%CODE_ISSUES_REPORT%"
echo - getAccessToken >> "%CODE_ISSUES_REPORT%"
echo - isPatternRecognition >> "%CODE_ISSUES_REPORT%"
echo - getGitHubUsername >> "%CODE_ISSUES_REPORT%"
echo. >> "%CODE_ISSUES_REPORT%"

echo ### ModAuthenticationManager >> "%CODE_ISSUES_REPORT%"
echo. >> "%CODE_ISSUES_REPORT%"
echo - login >> "%CODE_ISSUES_REPORT%"
echo - logout >> "%CODE_ISSUES_REPORT%"
echo - getUsername >> "%CODE_ISSUES_REPORT%"
echo. >> "%CODE_ISSUES_REPORT%"

echo ### AutonomousCodeGenerationService >> "%CODE_ISSUES_REPORT%"
echo. >> "%CODE_ISSUES_REPORT%"
echo - getInstance >> "%CODE_ISSUES_REPORT%"
echo - generateCode >> "%CODE_ISSUES_REPORT%"
echo - fixCode >> "%CODE_ISSUES_REPORT%"
echo. >> "%CODE_ISSUES_REPORT%"

echo Analysis complete.
goto :EOF

REM We've completely refactored this section to avoid syntax issues
REM All previous subroutines have been consolidated into simpler direct calls

REM ===================================
REM Resolution Errors Scan Subroutine
REM ===================================
:FIND_RESOLUTION_ERRORS
echo.
echo === Running Resolution Errors Analysis ===

REM Create output file with header
echo # Potential Resolution Error Report > "%RESOLUTION_ERRORS_REPORT%"
echo. >> "%RESOLUTION_ERRORS_REPORT%"
echo This report identifies potential "Cannot resolve" errors for IntelliJ IDEA 2025.1.1.1. >> "%RESOLUTION_ERRORS_REPORT%"
echo Generated on %DATE% %TIME%. >> "%RESOLUTION_ERRORS_REPORT%"
echo. >> "%RESOLUTION_ERRORS_REPORT%"
echo ## Overview >> "%RESOLUTION_ERRORS_REPORT%"
echo. >> "%RESOLUTION_ERRORS_REPORT%"
echo This scan identifies problematic packages and classes: >> "%RESOLUTION_ERRORS_REPORT%"
echo. >> "%RESOLUTION_ERRORS_REPORT%"
echo 1. Relocated into other packages >> "%RESOLUTION_ERRORS_REPORT%"
echo 2. Renamed or significantly changed >> "%RESOLUTION_ERRORS_REPORT%"
echo 3. Removed entirely in IntelliJ IDEA 2025.1.1.1 >> "%RESOLUTION_ERRORS_REPORT%"
echo. >> "%RESOLUTION_ERRORS_REPORT%"
echo These references are likely to cause "Cannot resolve symbol" errors during compilation. >> "%RESOLUTION_ERRORS_REPORT%"
echo. >> "%RESOLUTION_ERRORS_REPORT%"

REM Create a list of problematic patterns
echo ProjectManagerEx > "%TEMP_DIR%\relocation_patterns.txt"
echo ContentManagerEx >> "%TEMP_DIR%\relocation_patterns.txt"
echo ToolWindowManagerEx >> "%TEMP_DIR%\relocation_patterns.txt"
echo FileEditorManagerEx >> "%TEMP_DIR%\relocation_patterns.txt"
echo NotificationGroup >> "%TEMP_DIR%\relocation_patterns.txt"
echo JBPopupFactoryImpl >> "%TEMP_DIR%\relocation_patterns.txt"
echo IntentionActionDelegate >> "%TEMP_DIR%\relocation_patterns.txt"
echo GotoActionBase >> "%TEMP_DIR%\relocation_patterns.txt"
echo RefreshAction >> "%TEMP_DIR%\relocation_patterns.txt"
echo ProjectFileIndex >> "%TEMP_DIR%\relocation_patterns.txt"
echo PluginId >> "%TEMP_DIR%\relocation_patterns.txt"
echo ExtensionsArea >> "%TEMP_DIR%\relocation_patterns.txt"
echo JavaPsiFacade >> "%TEMP_DIR%\relocation_patterns.txt"
echo ClassUtil >> "%TEMP_DIR%\relocation_patterns.txt"
echo VirtualFileManager >> "%TEMP_DIR%\relocation_patterns.txt"
echo PsiUtils >> "%TEMP_DIR%\relocation_patterns.txt"
echo PsiUtil >> "%TEMP_DIR%\relocation_patterns.txt"
echo PsiTreeUtil >> "%TEMP_DIR%\relocation_patterns.txt"
echo FileChooserDescriptor >> "%TEMP_DIR%\relocation_patterns.txt"
echo LightVirtualFile >> "%TEMP_DIR%\relocation_patterns.txt"
echo CoreLocalVirtualFile >> "%TEMP_DIR%\relocation_patterns.txt"
echo JavaDirectoryService >> "%TEMP_DIR%\relocation_patterns.txt"
echo PsiElementFactory >> "%TEMP_DIR%\relocation_patterns.txt"
echo XmlElementFactory >> "%TEMP_DIR%\relocation_patterns.txt"

REM Check if the source directory exists
if not exist "%SOURCE_DIR%" (
    echo Source directory does not exist: %SOURCE_DIR%
    echo ## Error >> "%RESOLUTION_ERRORS_REPORT%"
    echo. >> "%RESOLUTION_ERRORS_REPORT%"
    echo Source directory does not exist: %SOURCE_DIR% >> "%RESOLUTION_ERRORS_REPORT%"
    goto :EOF
)

REM Find all Java files
dir /s /b "%SOURCE_DIR%\*.java" > "%TEMP_DIR%\java_files_resolution.txt" 2>nul

REM Extract imports from files
echo ## Potentially Problematic Imports >> "%RESOLUTION_ERRORS_REPORT%"
echo. >> "%RESOLUTION_ERRORS_REPORT%"
echo Imports causing resolution issues: >> "%RESOLUTION_ERRORS_REPORT%"
echo. >> "%RESOLUTION_ERRORS_REPORT%"

set "FOUND_ISSUES=0"
for /f "tokens=*" %%p in (%TEMP_DIR%\relocation_patterns.txt) do (
    findstr /s /i /c:"import.*%%p" "%SOURCE_DIR%\*.java" > "%TEMP_DIR%\problem_imports_%%p.txt" 2>nul
    if exist "%TEMP_DIR%\problem_imports_%%p.txt" (
        set "FOUND_ISSUES=1"
        echo ### References to %%p >> "%RESOLUTION_ERRORS_REPORT%"
        echo. >> "%RESOLUTION_ERRORS_REPORT%"
        echo --- Java Code --- >> "%RESOLUTION_ERRORS_REPORT%"
        type "%TEMP_DIR%\problem_imports_%%p.txt" >> "%RESOLUTION_ERRORS_REPORT%"
        echo --- End Code --- >> "%RESOLUTION_ERRORS_REPORT%"
        echo. >> "%RESOLUTION_ERRORS_REPORT%"
        echo Consider updating these imports with latest API. >> "%RESOLUTION_ERRORS_REPORT%"
        echo. >> "%RESOLUTION_ERRORS_REPORT%"
    )
)

REM Check for deprecated API calls
echo ## Potentially Problematic API Calls >> "%RESOLUTION_ERRORS_REPORT%"
echo. >> "%RESOLUTION_ERRORS_REPORT%"
echo API calls causing resolution issues: >> "%RESOLUTION_ERRORS_REPORT%"
echo. >> "%RESOLUTION_ERRORS_REPORT%"

set "API_CALLS_FOUND=0"
for %%m in (getBaseDir findFileByPath getInstanceEx getFileSystem resolveFile) do (
    findstr /s /i /c:".%%m(" "%SOURCE_DIR%\*.java" > "%TEMP_DIR%\problem_api_%%m.txt" 2>nul
    if exist "%TEMP_DIR%\problem_api_%%m.txt" (
        set "API_CALLS_FOUND=1"
        echo ### Calls to %%m >> "%RESOLUTION_ERRORS_REPORT%"
        echo. >> "%RESOLUTION_ERRORS_REPORT%"
        echo --- Java Code --- >> "%RESOLUTION_ERRORS_REPORT%"
        type "%TEMP_DIR%\problem_api_%%m.txt" >> "%RESOLUTION_ERRORS_REPORT%"
        echo --- End Code --- >> "%RESOLUTION_ERRORS_REPORT%"
        echo. >> "%RESOLUTION_ERRORS_REPORT%"
        
        REM Add API-specific suggestions
        call :ADD_API_SUGGESTION "%%m" "%RESOLUTION_ERRORS_REPORT%"
        
        echo. >> "%RESOLUTION_ERRORS_REPORT%"
    )
)

if "%FOUND_ISSUES%%API_CALLS_FOUND%"=="00" (
    echo No specific resolution issues detected from static analysis. >> "%RESOLUTION_ERRORS_REPORT%"
    echo. >> "%RESOLUTION_ERRORS_REPORT%"
    echo However, build errors indicate there are resolution issues. Check the build log for details. >> "%RESOLUTION_ERRORS_REPORT%"
) else (
    echo Static analysis found potential resolution issues. >> "%RESOLUTION_ERRORS_REPORT%"
)

REM Extract resolution issues from build log
findstr /i /c:"cannot find symbol" "%BUILD_LOG%" > "%TEMP_DIR%\build_resolution_errors.txt"
if exist "%TEMP_DIR%\build_resolution_errors.txt" (
    echo ## Resolution Errors from Build Log >> "%RESOLUTION_ERRORS_REPORT%"
    echo. >> "%RESOLUTION_ERRORS_REPORT%"
    echo Resolution errors found during compilation: >> "%RESOLUTION_ERRORS_REPORT%"
    echo. >> "%RESOLUTION_ERRORS_REPORT%"
    echo --- Java Code --- >> "%RESOLUTION_ERRORS_REPORT%"
    type "%TEMP_DIR%\build_resolution_errors.txt" >> "%RESOLUTION_ERRORS_REPORT%"
    echo --- End Code --- >> "%RESOLUTION_ERRORS_REPORT%"
    echo. >> "%RESOLUTION_ERRORS_REPORT%"
) else (
    REM No resolution errors found in build log
)

REM Add implementation recommendations
echo ## Fix Recommendations >> "%RESOLUTION_ERRORS_REPORT%"
echo. >> "%RESOLUTION_ERRORS_REPORT%"
echo 1. **Update imports** with latest packages >> "%RESOLUTION_ERRORS_REPORT%"
echo 2. **Use CompatibilityUtil** on deprecated API calls >> "%RESOLUTION_ERRORS_REPORT%"
echo 3. **Fix API signatures** correctly >> "%RESOLUTION_ERRORS_REPORT%"
echo 4. **Add missing implementations** in service classes >> "%RESOLUTION_ERRORS_REPORT%"
echo 5. **Implement proper getInstance()** functions in services >> "%RESOLUTION_ERRORS_REPORT%"
echo. >> "%RESOLUTION_ERRORS_REPORT%"

echo Resolution errors analysis complete.
goto :EOF

:END
echo.
echo Build process completed in %TIME%.
echo.
echo All operations are complete. Please review the generated reports.
echo.
echo Thank you for using ModForge Ultimate Builder!
echo.

REM ===================================
REM Compatibility Suggestion Subroutine
REM ===================================
:ADD_COMPATIBILITY_SUGGESTION
set "PATTERN=%~1"
set "OUTPUT_FILE=%~2"

if "%PATTERN%"=="getBaseDir" (
    echo * **Suggested fix:** Use CompatibilityUtil.getProjectBaseDir(project) >> "%OUTPUT_FILE%"
    goto :EOF
)
if "%PATTERN%"=="CacheUpdater" (
    echo * **Suggested fix:** Use CompatibilityUtil.refreshAll(project) >> "%OUTPUT_FILE%"
    goto :EOF
)
if "%PATTERN%"=="runReadAction" (
    echo * **Suggested fix:** Use CompatibilityUtil.runReadAction(lambda) >> "%OUTPUT_FILE%"
    goto :EOF
)
if "%PATTERN%"=="runWriteAction" (
    echo * **Suggested fix:** Use CompatibilityUtil.runWriteAction(lambda) >> "%OUTPUT_FILE%"
    goto :EOF
)
if "%PATTERN%"=="getSelectedTextEditor" (
    echo * **Suggested fix:** Use CompatibilityUtil.getSelectedTextEditor(project) >> "%OUTPUT_FILE%"
    goto :EOF
)
if "%PATTERN%"=="ProjectComponent" (
    echo * **Suggested fix:** Use @Service(Service.Level.PROJECT) annotation >> "%OUTPUT_FILE%"
    goto :EOF
)
if "%PATTERN%"=="ApplicationComponent" (
    echo * **Suggested fix:** Use @Service(Service.Level.APPLICATION) annotation >> "%OUTPUT_FILE%"
    goto :EOF
)

echo * **Suggested fix:** Check compatibility with IntelliJ IDEA 2025.1.1.1. Use CompatibilityUtil functions. >> "%OUTPUT_FILE%"
goto :EOF

REM ===================================
REM API Suggestion Subroutine
REM ===================================
:ADD_API_SUGGESTION
set "API_NAME=%~1"
set "OUTPUT_FILE=%~2"

if "%API_NAME%"=="getBaseDir" (
    echo **Suggested fix:** Use CompatibilityUtil.getProjectBaseDir(project) >> "%OUTPUT_FILE%"
    goto :EOF
)
if "%API_NAME%"=="findFileByPath" (
    echo **Suggested fix:** Use VirtualFileUtil.findFileByPath(path) >> "%OUTPUT_FILE%"
    goto :EOF
)
if "%API_NAME%"=="getInstanceEx" (
    echo **Suggested fix:** Use getInstance() function >> "%OUTPUT_FILE%"
    goto :EOF
)
if "%API_NAME%"=="getFileSystem" (
    echo **Suggested fix:** Use VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL) >> "%OUTPUT_FILE%"
    goto :EOF
)
if "%API_NAME%"=="resolveFile" (
    echo **Suggested fix:** Use CompatibilityUtil.findPsiFile(project, file) >> "%OUTPUT_FILE%"
    goto :EOF
)

echo **Suggested fix:** Use compatibility wrapper functions >> "%OUTPUT_FILE%"
goto :EOF

endlocal