@echo off
setlocal enabledelayedexpansion

REM ===================================
REM ModForge Resolution Error Scanner
REM ===================================
REM
REM This script attempts to find potential "Cannot resolve" errors by scanning
REM for references to packages and classes that have been relocated or removed
REM in IntelliJ IDEA 2025.1.1.1.

REM Default values
set "OUTPUT_FILE=resolution-errors.md"
set "SOURCE_DIR=src\main\java"
set "TEMP_MATCHES=%TEMP%\resolution_matches.txt"
set "TEMP_FILES=%TEMP%\resolution_files.txt"
set "TEMP_IMPORTS=%TEMP%\imports.txt"
set "TEMP_PATTERN_FILE=%TEMP%\relocation_patterns.txt"

echo ==== Scanning for Potential Resolution Errors ====

REM Check if the source directory exists
if not exist "%SOURCE_DIR%" (
    echo Source directory does not exist: %SOURCE_DIR%
    exit /b 1
)

REM Create list of packages and classes that have been relocated or removed
> "%TEMP_PATTERN_FILE%" (
    REM Relocated packages
    echo com.intellij.util.ui.update
    echo com.intellij.openapi.fileChooser.impl
    echo com.intellij.openapi.ui.popup.impl
    echo com.intellij.openapi.components.impl
    echo com.intellij.ui.components.panels
    echo com.intellij.ui.treeStructure
    echo com.intellij.openapi.actionSystem.ex
    echo com.intellij.openapi.util.io.FileUtil
    echo com.intellij.openapi.wm.impl
    echo com.intellij.openapi.wm.ex
    echo com.intellij.psi.impl.source
    echo com.intellij.openapi.vcs.ex
    echo com.intellij.ide.actions.OpenProjectFileChooserDescriptor
    echo com.intellij.openapi.editor.ex
    echo com.intellij.openapi.editor.impl
    echo com.intellij.ui.EditorNotificationPanel
    echo com.intellij.openapi.compiler.CompilerPaths
    echo com.intellij.openapi.fileEditor.impl
    echo com.intellij.openapi.options.ex
    echo com.intellij.openapi.project.ProjectUtil
    echo com.intellij.ide.projectView.impl
    echo com.intellij.ide
    echo com.intellij.execution.impl
    echo com.intellij.openapi.progress.impl
    echo com.intellij.codeInsight.daemon.impl
    echo com.intellij.codeInsight.hint
    echo com.intellij.xml.util
    echo org.jdom
    
    REM Renamed or removed classes
    echo ProjectManagerEx
    echo ContentManagerEx
    echo ToolWindowManagerEx
    echo FileEditorManagerEx
    echo NotificationGroup
    echo JBPopupFactoryImpl
    echo IntentionActionDelegate
    echo GotoActionBase
    echo RefreshAction
    echo ProjectFileIndex
    echo PluginId
    echo ExtensionsArea
    echo JavaPsiFacade
    echo ClassUtil
    echo VirtualFileManager
    echo PsiUtils
    echo PsiUtil
    echo PsiTreeUtil
    echo FileChooserDescriptor
    echo LightVirtualFile
    echo CoreLocalVirtualFile
    echo JavaDirectoryService
    echo PsiElementFactory
    echo XmlElementFactory
    echo DomElementXmlAttributeDescriptor
    echo IconManager
    echo AllIcons
    echo XPathSupport
    echo VirtualFileFilter
    echo AbstractTreeStructure
    echo VirtualFileVisitor
    echo GutterIconProvider
    echo LineMarkerInfo
    echo LineMarkerProvider
    echo DocumentListener
    echo EditorActionHandler
    echo AbstractProjectViewPane
    echo IdeaPluginDescriptorImpl
    echo IdeaPluginDescriptor
    echo LanguageExtension
)

REM Create output file with header
> "%OUTPUT_FILE%" (
    echo # Potential Resolution Error Report
    echo.
    echo This report identifies potential "Cannot resolve" errors for IntelliJ IDEA 2025.1.1.1.
    echo Generated on %DATE% %TIME%.
    echo.
    echo ## Overview
    echo.
    echo This scan identifies references to packages and classes that have been:
    echo.
    echo 1. Relocated to a different package
    echo 2. Renamed or significantly changed
    echo 3. Removed entirely in IntelliJ IDEA 2025.1.1.1
    echo.
    echo These references are likely to cause "Cannot resolve symbol" errors during compilation.
    echo.
)

REM Find all Java files
dir /s /b "%SOURCE_DIR%\*.java" > "%TEMP_FILES%" 2>nul

echo Searching for potential resolution errors...

REM Count total files
set /a TOTAL_FILES=0
for /f %%f in (%TEMP_FILES%) do set /a TOTAL_FILES+=1

REM Initialize counters
set /a TOTAL_ISSUES=0
set /a AFFECTED_FILES=0

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
    set /a MOD=!PROCESSED! %% 20
    if !MOD! equ 0 (
        echo Processed !PROCESSED! of %TOTAL_FILES% files...
    )
    
    REM Create a file flag to track if this file has issues
    set "HAS_ISSUE=0"
    
    REM First, extract imports to a temporary file
    findstr /n /c:"import " "%%f" > "%TEMP_IMPORTS%" 2>nul
    
    REM Check imports against relocated packages
    if exist "%TEMP_IMPORTS%" (
        for /f "tokens=1,* delims=:" %%a in (%TEMP_IMPORTS%) do (
            for /f "tokens=*" %%p in (%TEMP_PATTERN_FILE%) do (
                echo %%b | findstr /c:"%%p" >nul 2>&1
                if !ERRORLEVEL! equ 0 (
                    echo %%f:%%a:IMPORT:%%b >> "%TEMP_MATCHES%"
                    set "HAS_ISSUE=1"
                )
            )
        )
    )
    
    REM Check file contents for class references
    for /f "tokens=*" %%p in (%TEMP_PATTERN_FILE%) do (
        findstr /n /c:"%%p" "%%f" > "%TEMP%\class_refs.txt" 2>nul
        
        if exist "%TEMP%\class_refs.txt" (
            for /f "tokens=1,* delims=:" %%a in (%TEMP%\class_refs.txt) do (
                REM Skip lines that are in imports or comments
                echo %%b | findstr /c:"import " /c:"* " /c:"//" >nul 2>&1
                if !ERRORLEVEL! neq 0 (
                    echo %%f:%%a:CLASS:%%b >> "%TEMP_MATCHES%"
                    set "HAS_ISSUE=1"
                )
            )
        )
    )
    
    REM Also check for specific method calls that may cause resolution errors
    for %%m in (getBaseDir findFileByPath getInstanceEx getFileSystem resolveFile) do (
        findstr /n /c:".%%m(" "%%f" > "%TEMP%\method_calls.txt" 2>nul
        
        if exist "%TEMP%\method_calls.txt" (
            for /f "tokens=1,* delims=:" %%a in (%TEMP%\method_calls.txt) do (
                echo %%b | findstr /c:"import " /c:"* " /c:"//" >nul 2>&1
                if !ERRORLEVEL! neq 0 (
                    echo %%f:%%a:METHOD:%%b >> "%TEMP_MATCHES%"
                    set "HAS_ISSUE=1"
                )
            )
        )
    )
    
    if "!HAS_ISSUE!"=="1" (
        set /a AFFECTED_FILES+=1
    )
    
    if exist "%TEMP_IMPORTS%" del "%TEMP_IMPORTS%" 2>nul
    if exist "%TEMP%\class_refs.txt" del "%TEMP%\class_refs.txt" 2>nul
    if exist "%TEMP%\method_calls.txt" del "%TEMP%\method_calls.txt" 2>nul
)

REM Count total issues
if exist "%TEMP_MATCHES%" (
    for /f %%a in ('type "%TEMP_MATCHES%" ^| find /c /v ""') do set TOTAL_ISSUES=%%a
)

echo Resolution error scan complete. Found %TOTAL_ISSUES% potential issues in %AFFECTED_FILES% files.

REM Add summary to output file
>> "%OUTPUT_FILE%" (
    echo ## Summary
    echo.
    echo * Total Java files scanned: %TOTAL_FILES%
    echo * Files with potential resolution issues: %AFFECTED_FILES%
    echo * Total potential resolution errors found: %TOTAL_ISSUES%
    echo.
)

REM Process and add issues to output file
echo Generating detailed report...

if exist "%TEMP_MATCHES%" (
    >> "%OUTPUT_FILE%" (
        echo ## Detailed Resolution Issues
        echo.
    )
    
    set "CURRENT_FILE="
    
    REM Sort the matches by filename to group them
    sort "%TEMP_MATCHES%" /o "%TEMP_MATCHES%"
    
    for /f "tokens=1-4 delims=:" %%a in (%TEMP_MATCHES%) do (
        if not "!CURRENT_FILE!"=="%%a" (
            set "CURRENT_FILE=%%a"
            echo. >> "%OUTPUT_FILE%"
            echo ### %%a >> "%OUTPUT_FILE%"
            echo. >> "%OUTPUT_FILE%"
        )
        
        echo * Line %%b - %%c reference: `%%d` >> "%OUTPUT_FILE%"
        
        REM Add specific suggestions based on the reference type
        if "%%c"=="IMPORT" (
            set "IMPORT_PATH=%%d"
            set "IMPORT_PATH=!IMPORT_PATH:import =!"
            set "IMPORT_PATH=!IMPORT_PATH:;=!"
            
            echo   * **Potential resolution error:** Import path may have changed in IntelliJ IDEA 2025.1.1.1 >> "%OUTPUT_FILE%"
            
            REM Suggest alternative imports based on patterns
            if "!IMPORT_PATH!" == "com.intellij.openapi.fileChooser.impl" (
                echo   * **Try:** `import com.intellij.openapi.fileChooser.*` and use updated classes >> "%OUTPUT_FILE%"
            ) else if "!IMPORT_PATH!" == "com.intellij.codeInsight.daemon.impl" (
                echo   * **Try:** `import com.intellij.codeInsight.daemon` and check for relocated classes >> "%OUTPUT_FILE%"
            ) else if "!IMPORT_PATH!" == "com.intellij.openapi.ui.popup.impl" (
                echo   * **Try:** `import com.intellij.openapi.ui.popup.*` and use base interfaces >> "%OUTPUT_FILE%"
            ) else if "!IMPORT_PATH!" == "com.intellij.openapi.components.impl" (
                echo   * **Try:** `import com.intellij.openapi.components.*` and use service interfaces >> "%OUTPUT_FILE%"
            ) else (
                echo   * **Consider:** Check for relocated packages or use compatibility utilities >> "%OUTPUT_FILE%"
            )
            
        ) else if "%%c"=="CLASS" (
            REM Trim the line to extract just the class reference
            set "CLASS_REF=%%d"
            set "CLASS_REF=!CLASS_REF: =!"
            
            echo   * **Potential resolution error:** Class reference may no longer exist or has been relocated >> "%OUTPUT_FILE%"
            
            REM Suggest alternatives based on the class
            if "!CLASS_REF!" == "NotificationGroup" (
                echo   * **Try:** Use `NotificationGroupManager.getInstance().getNotificationGroup()` instead >> "%OUTPUT_FILE%"
            ) else if "!CLASS_REF!" == "ProjectManagerEx" (
                echo   * **Try:** Use `ProjectManager.getInstance()` instead >> "%OUTPUT_FILE%"
            ) else if "!CLASS_REF!" == "PluginId" (
                echo   * **Try:** Use `PluginId.findId()` or update import path >> "%OUTPUT_FILE%"
            ) else if "!CLASS_REF!" == "FileEditorManagerEx" (
                echo   * **Try:** Use `FileEditorManager.getInstance(project)` instead >> "%OUTPUT_FILE%"
            ) else if "!CLASS_REF!" == "ContentManagerEx" (
                echo   * **Try:** Use `ContentManager` interface methods instead >> "%OUTPUT_FILE%"
            ) else (
                echo   * **Consider:** Check API documentation for renamed or relocated class >> "%OUTPUT_FILE%"
            )
            
        ) else if "%%c"=="METHOD" (
            REM Extract just the method call
            set "METHOD_CALL=%%d"
            set "METHOD_CALL=!METHOD_CALL: =!"
            
            echo   * **Potential resolution error:** Method call may no longer exist or signature changed >> "%OUTPUT_FILE%"
            
            REM Suggest alternatives based on the method
            if "!METHOD_CALL!" == "getBaseDir" (
                echo   * **Try:** Replace with `CompatibilityUtil.getProjectBaseDir(project)` >> "%OUTPUT_FILE%"
            ) else if "!METHOD_CALL!" == "findFileByPath" (
                echo   * **Try:** Use `VirtualFileUtil.findFileByPath(path)` >> "%OUTPUT_FILE%"
            ) else if "!METHOD_CALL!" == "getInstanceEx" (
                echo   * **Try:** Use the standard `.getInstance()` method instead >> "%OUTPUT_FILE%"
            ) else if "!METHOD_CALL!" == "getFileSystem" (
                echo   * **Try:** Use `VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL)` >> "%OUTPUT_FILE%"
            ) else if "!METHOD_CALL!" == "resolveFile" (
                echo   * **Try:** Use `CompatibilityUtil.findPsiFile(project, file)` >> "%OUTPUT_FILE%"
            ) else (
                echo   * **Consider:** Check method signature for annotation or parameter changes >> "%OUTPUT_FILE%"
            )
        )
    )
    
    >> "%OUTPUT_FILE%" (
        echo.
        echo ## Resolution Error Categories
        echo.
        echo ### Package Relocations
        echo.
        echo Many packages have been restructured in newer IntelliJ IDEA versions:
        echo.
        echo * `com.intellij.openapi.fileChooser.impl` → Various locations
        echo * `com.intellij.openapi.ui.popup.impl` → Various locations
        echo * `com.intellij.openapi.components.impl` → Various locations
        echo * `com.intellij.ui.components.panels` → Various locations
        echo.
        echo ### Class Renames
        echo.
        echo * `NotificationGroup` → Use `NotificationGroupManager` instead
        echo * `ProjectManagerEx` → Use `ProjectManager` with proper methods
        echo * `FileEditorManagerEx` → Use `FileEditorManager` with proper methods
        echo * Most classes with `Ex` suffix have been integrated into main classes
        echo.
        echo ### API Method Changes
        echo.
        echo * `Project.getBaseDir()` → Replaced with content roots API
        echo * `findFileByPath()` → Various replacements depending on context
        echo * Many methods now require proper annotations (@NotNull, etc.)
        echo.
    )
) else (
    echo No resolution issues found! >> "%OUTPUT_FILE%"
)

REM Add detailed compatibility guide
>> "%OUTPUT_FILE%" (
    echo.
    echo ## Fixing Resolution Errors
    echo.
    echo ### General Approach
    echo.
    echo 1. **Use compatibility utilities**:
    echo    - `CompatibilityUtil` for project and PSI operations
    echo    - `VirtualFileUtil` for file operations
    echo    - `ThreadUtils` for threading operations
    echo.
    echo 2. **Update imports**:
    echo    - Check IntelliJ Platform API documentation for relocations
    echo    - Try removing ".impl" from import paths
    echo    - Use base interfaces instead of implementation classes
    echo.
    echo 3. **Replace deprecated method calls**:
    echo    - Use the methods from compatibility utilities
    echo    - Check for method signature changes (parameters, annotations)
    echo    - Use newer API replacements from IntelliJ documentation
    echo.
    echo 4. **Class replacements**:
    echo    - Replace `Ex` classes with standard classes
    echo    - Use service model instead of component model
    echo    - Use interface types instead of implementation types
    echo.
)

:CLEANUP
REM Clean up
if exist "%TEMP_PATTERN_FILE%" del "%TEMP_PATTERN_FILE%" 2>nul
if exist "%TEMP_MATCHES%" del "%TEMP_MATCHES%" 2>nul
if exist "%TEMP_FILES%" del "%TEMP_FILES%" 2>nul
if exist "%TEMP_IMPORTS%" del "%TEMP_IMPORTS%" 2>nul
if exist "%TEMP%\class_refs.txt" del "%TEMP%\class_refs.txt" 2>nul
if exist "%TEMP%\method_calls.txt" del "%TEMP%\method_calls.txt" 2>nul

echo Resolution error analysis complete! Report written to: %OUTPUT_FILE%
echo.
echo Next steps:
echo   1. Review the detailed resolution errors report
echo   2. Focus on the imports and class references identified
echo   3. Use the compatibility utilities to fix resolution issues
echo   4. Run this analysis again after making changes

endlocal