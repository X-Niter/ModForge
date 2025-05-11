@echo off
setlocal enabledelayedexpansion

REM ===================================
REM ModForge Advanced Compatibility Scanner
REM ===================================
REM
REM This script performs deep analysis of code for IntelliJ API compatibility issues
REM with IntelliJ IDEA 2025.1.1.1 (Build #IC-251.25410.129).

REM Default values
set "OUTPUT_FILE=advanced-compatibility-issues.md"
set "SOURCE_DIR=src\main\java"
set "TEMP_MATCHES=%TEMP%\advanced_compatibility_matches.txt"
set "TEMP_FILES=%TEMP%\advanced_compatibility_files.txt"
set "TEMP_PATTERN_FILE=%TEMP%\pattern_file.txt"

echo ==== Running Advanced Compatibility Analysis ====

REM Check if the source directory exists
if not exist "%SOURCE_DIR%" (
    echo Source directory does not exist: %SOURCE_DIR%
    exit /b 1
)

REM Create massive pattern list file for more thorough detection
> "%TEMP_PATTERN_FILE%" (
    echo Project.getBaseDir
    echo com.intellij.openapi.project.Project.getBaseDir
    echo project.getBaseDir
    echo getBaseDir(
    echo .getBaseDir(
    echo CacheUpdater
    echo CacheUpdaterFacade
    echo com.intellij.openapi.vcs.changes.committed.CacheUpdaterFacade
    echo CacheUpdaterReader
    echo runReadAction
    echo getApplication().runReadAction
    echo ApplicationManager.getApplication().runReadAction
    echo runWriteAction
    echo getApplication().runWriteAction
    echo ApplicationManager.getApplication().runWriteAction
    echo getSelectedTextEditor
    echo FileEditorManager.getSelectedTextEditor
    echo createXmlTag
    echo PsiElementFactory.getInstance
    echo createFileFromText
    echo VirtualFileSystem.getProtocol
    echo VirtualFileManager.getInstance().getFileSystem
    echo FileTypeManager.getInstance
    echo FileTypeManagerEx.getInstanceEx
    echo ApplicationComponent
    echo ProjectComponent
    echo ModuleComponent
    echo ComponentManager
    echo registerComponent
    echo getComponent(
    echo getPsiManager
    echo LanguageLevel
    echo getProjectDir
    echo getModuleDir
    echo ProjectRootsUtil
    echo ProjectRootManager.getInstance
    echo ASTFactory
    echo PsiBuilderFactory
    echo PsiBuilder
    echo GenericAttributeValue
    echo GenericDomValue
    echo HtmlUtil
    echo JavaPsiFacade
    echo ProgressIndicator
    echo ProgressManager
    echo getContent(
    echo update(AnActionEvent
    echo extends AnAction
    echo AnAction.update(
    echo actionPerformed(
    echo DataContext
    echo DataProvider
    echo DataSink
    echo DataKey
    echo ListPopup
    echo ListPopupStep
    echo DialogWrapper
    echo Messages.showMessageDialog
    echo Messages.showErrorDialog
    echo Messages.showInfoMessage
    echo Messages.showYesNoDialog
    echo Messages.showOkCancelDialog
    echo ShowSettings
    echo ShowSettingsUtil
    echo AsyncResult
    echo PsiReference.resolve(
    echo OnlineDocumentationProvider
    echo DocumentationProvider
    echo NotificationGroup
    echo ActionPlaces.
    echo FileSystem.findFileByPath
    echo showDialog(
    echo TreeUtil
    echo NotificationGroupManager
    echo Notification(
    echo new JBPopup
    echo JBPopupFactory
    echo VirtualFile.getUrl
    echo JavaModule
    echo JavaModuleSystem
    echo PlatformModuleSystem
    echo VcsConfiguration
    echo VcsRoot
    echo ProjectLevelVcsManager
    echo VcsException
    echo ShowFilePathAction
    echo ShowFolderPathAction
    echo LocalFileSystem.getInstance
    echo findClass(
    echo JavaPsiFacade.findClass
    echo getInstanceMethod
    echo PsiJavaFile
    echo AbstractProjectViewPane
    echo getBasePath
    echo resolveFile
    echo PsiUtil
    echo PsiMethodCallExpression
    echo PsiMethodReferenceExpression
    echo PsiReferenceExpression
    echo RefactoringActionHandler
    echo RefactoringSupportProvider
    echo RefactoringActionHandlerFactory
    echo NameSuggestionProvider
    echo VariableInplaceRenamer
    echo InplaceRefactoring
    echo LookupManager
    echo FileContentUtil
    echo XmlFile
    echo XmlTag
    echo DomManager
    echo DomElement
    echo DomFileElement
    echo GutterIconRenderer
    echo SimpleTextAttributes
    echo PluginId.getId(
    echo PluginManager.getPlugin(
    echo LocalInspectionTool
    echo GlobalInspectionTool
    echo InspectionProfileEntry
    echo InspectionManager
    echo InspectionToolProvider
    echo PsiClass
    echo PsiElement
    echo PsiManager.getInstance
    echo PsiDirectory
    echo LocalInspectionEP
    echo GlobalInspectionEP
    echo FormattingModelBuilder
    echo FileTypeFactory
    echo FileTypeBean
    echo LanguageFileType
    echo FileType
    echo StandardFileSystems
    echo VirtualFileSystem
    echo VirtualFileListener
    echo UIUtil
    echo JBColor
    echo JBUI
    echo UIManager
    echo ToolWindow
    echo ToolWindowFactory
    echo ToolWindowManager
    echo ContentManagerEvent
    echo ToolWindowManagerListener
    echo ProjectManagerListener
    echo ProjectManager
    echo EditorColorsManager
    echo EditorColorsScheme
    echo ExternalAnnotator
    echo GotoClassContributor
    echo GotoFileContributor
    echo GotoSymbolContributor
    echo ChooseByNameContributor
    echo ChooseByNameRegistry
    echo VcsRootChecker
    echo VcsRootDetector
    echo VcsIgnoreChecker
    echo ExternalSystemManager
    echo BuildManager
    echo CompilerManager
    echo ServerConnectionManager
    echo TaskManager
    echo CacheManager
    echo DumbService
    echo DumbModeTask
    echo DumbAware
    echo StartupActivity
    echo StartupManager
    echo PostStartupActivity
    echo PreloadingActivity
    echo BackgroundableProcessIndicator
    echo ProgressWindow
    echo CoreProgressManager
    echo HighlightingPass
    echo HighlightingPassFactory
    echo HighlightingManager
    echo HighlightInfo
    echo HighlightSeverity
    echo HighlightDisplayLevel
    echo StdFileTypes
    echo CompletionContributor
    echo CompletionProvider
    echo CompletionType
    echo CompletionParameters
    echo LookupElement
    echo DocumentationManager
    echo DocumentationProvider
    echo QuickDocProvider
    echo CodeStyleManager
    echo PsiDocumentManager
    echo FileDocumentManager
    echo IdeDocumentHistory
    echo PerformInBackgroundOption
    echo ReadonlyStatusHandler
    echo TaskRepository
    echo TaskManager
    echo TemplateManager
    echo TemplateImpl
    echo FoldingModelEx
    echo FoldRegion
    echo ReadAction
    echo WriteAction
    echo ProcessHandler
    echo BaseProcessHandler
    echo ConsoleView
    echo ColoredProcessHandler
    echo CodeInsightBundle
    echo IntelliJLaf
    echo DarculaLaf
    echo CancelablePromise
    echo getStateStore
    echo Project.isDisposed
    echo Project.isOpen
    echo JDOMUtil
    echo JDOMExternalizerUtil
    echo Component.getService
    echo EntityHelper
)

REM Create output file with header
> "%OUTPUT_FILE%" (
    echo # Advanced ModForge Compatibility Analysis
    echo.
    echo This report contains detailed compatibility analysis for IntelliJ IDEA 2025.1.1.1.
    echo Generated on %DATE% %TIME%.
    echo.
    echo ## Overview
    echo.
    echo This scan performs deep analysis of API usage patterns that may cause compatibility 
    echo issues with IntelliJ IDEA 2025.1.1.1. It looks for:
    echo.
    echo 1. Deprecated APIs and classes
    echo 2. Changed method signatures
    echo 3. Relocated packages
    echo 4. Removed interfaces and classes
    echo 5. Platform API changes requiring wrapper methods
    echo.
)

REM Find all Java files
dir /s /b "%SOURCE_DIR%\*.java" > "%TEMP_FILES%" 2>nul

echo Searching for potentially problematic API usage...

REM Count total files
set /a TOTAL_FILES=0
for /f %%f in (%TEMP_FILES%) do set /a TOTAL_FILES+=1

REM Initialize counters
set /a TOTAL_ISSUES=0
set /a AFFECTED_FILES=0
set /a PATTERN_MATCHES=0

if exist found_file.tmp del found_file.tmp 2>nul
if exist "%TEMP_MATCHES%" del "%TEMP_MATCHES%" 2>nul

REM Process each file
set PROCESSED=0

echo Scanning %TOTAL_FILES% Java files...

if %TOTAL_FILES% equ 0 (
    echo No Java files found in %SOURCE_DIR%
    goto CLEANUP
)

echo This may take a few minutes for large codebases...

for /f "delims=" %%f in (%TEMP_FILES%) do (
    set /a PROCESSED+=1
    
    REM Progress indicator
    set /a MOD=!PROCESSED! %% 20
    if !MOD! equ 0 (
        echo Processed !PROCESSED! of %TOTAL_FILES% files...
    )
    
    REM Create a file flag to track if this file has issues
    set "HAS_ISSUE=0"
    
    REM Use findstr with the pattern file for much more thorough detection
    findstr /f:"%TEMP_PATTERN_FILE%" /i /n /c: "%%f" > "%TEMP%\matches.txt" 2>nul
    
    if exist "%TEMP%\matches.txt" (
        for /f "tokens=1,2 delims=:" %%a in (%TEMP%\matches.txt) do (
            set /a PATTERN_MATCHES+=1
            set "PATTERN_TEXT="
            
            REM Extract actual pattern text from the line for better context
            for /f "usebackq delims=" %%x in (`findstr /n "^" "%%f" ^| findstr /b "%%a:"`) do (
                set "PATTERN_TEXT=%%x"
            )
            
            REM Strip line number from the beginning
            set "PATTERN_TEXT=!PATTERN_TEXT:*:=!"
            
            REM Trim the pattern text
            set "PATTERN_TEXT=!PATTERN_TEXT: =!"
            if not "!PATTERN_TEXT!"=="" (
                echo %%f:%%a:!PATTERN_TEXT! >> "%TEMP_MATCHES%"
                set "HAS_ISSUE=1"
            )
        )
    )
    
    if "!HAS_ISSUE!"=="1" (
        set /a AFFECTED_FILES+=1
    )
    
    if exist "%TEMP%\matches.txt" del "%TEMP%\matches.txt" 2>nul
)

REM Count total issues
if exist "%TEMP_MATCHES%" (
    for /f %%a in ('type "%TEMP_MATCHES%" ^| find /c /v ""') do set TOTAL_ISSUES=%%a
)

echo Analysis complete. Found %TOTAL_ISSUES% potential issues across %AFFECTED_FILES% files.

REM Add summary to output file
>> "%OUTPUT_FILE%" (
    echo ## Summary
    echo.
    echo * Total Java files scanned: %TOTAL_FILES%
    echo * Files with potential compatibility issues: %AFFECTED_FILES%
    echo * Total potential issues found: %TOTAL_ISSUES%
    echo * Detailed pattern matches: %PATTERN_MATCHES%
    echo.
    echo ## Compatibility Categories
    echo.
    echo This analysis identifies several categories of compatibility issues:
    echo.
    echo ### 1. Deprecated or Removed APIs
    echo.
    echo * `Project.getBaseDir()` - Replace with `CompatibilityUtil.getProjectBaseDir(project)`
    echo * `CacheUpdater` interfaces - Replace with `CompatibilityUtil.refreshAll(project)`
    echo * `ApplicationManager.getApplication().runReadAction()` - Use `CompatibilityUtil.runReadAction()`
    echo * Component APIs (ProjectComponent, etc.) - Replace with `@Service` annotation
    echo.
    echo ### 2. Method Signature Changes
    echo.
    echo * `AnAction.update(AnActionEvent)` - Ensure proper override with `@NotNull` annotations
    echo * `actionPerformed(AnActionEvent)` - Check parameter types and annotations
    echo * `PsiReference.resolve()` - Handle nullable return values
    echo.
    echo ### 3. Changed Package Locations
    echo.
    echo * UI utility classes - Check imports for relocated packages
    echo * VCS integration classes - Check for package restructuring
    echo * Dialog and notification APIs - Use newer notification system
    echo.
)

REM Process and add issues to output file
echo Generating detailed report...

REM Create file-based sections
if exist "%TEMP_MATCHES%" (
    >> "%OUTPUT_FILE%" (
        echo ## Detailed Issues By File
        echo.
        echo The following files contain potential compatibility issues:
        echo.
    )
    
    set "CURRENT_FILE="
    
    REM Sort the matches by filename to group them
    sort "%TEMP_MATCHES%" /o "%TEMP_MATCHES%"
    
    for /f "tokens=1-3 delims=:" %%a in (%TEMP_MATCHES%) do (
        if not "!CURRENT_FILE!"=="%%a" (
            set "CURRENT_FILE=%%a"
            echo. >> "%OUTPUT_FILE%"
            echo ### %%a >> "%OUTPUT_FILE%"
            echo. >> "%OUTPUT_FILE%"
        )
        
        echo * Line %%b: `%%c` >> "%OUTPUT_FILE%"
        
        REM Add fix suggestions based on pattern detection
        if "%%c" == "" (
            REM Skip empty lines
        ) else if "%%c" == "getBaseDir" (
            echo   * **Fix:** Replace with `CompatibilityUtil.getProjectBaseDir(project)` >> "%OUTPUT_FILE%"
        ) else if "%%c" == "CacheUpdater" (
            echo   * **Fix:** Replace with `CompatibilityUtil.refreshAll(project)` >> "%OUTPUT_FILE%"
        ) else if "%%c" == "runReadAction" (
            echo   * **Fix:** Replace with `CompatibilityUtil.runReadAction(() -> { ... })` >> "%OUTPUT_FILE%"
        ) else if "%%c" == "runWriteAction" (
            echo   * **Fix:** Replace with `CompatibilityUtil.runWriteAction(() -> { ... })` >> "%OUTPUT_FILE%"
        ) else if "%%c" == "getSelectedTextEditor" (
            echo   * **Fix:** Replace with `CompatibilityUtil.getSelectedTextEditor(project)` >> "%OUTPUT_FILE%"
        ) else if "%%c" == "ProjectComponent" (
            echo   * **Fix:** Replace with `@Service(Service.Level.PROJECT)` annotation >> "%OUTPUT_FILE%"
        ) else if "%%c" == "ApplicationComponent" (
            echo   * **Fix:** Replace with `@Service(Service.Level.APPLICATION)` annotation >> "%OUTPUT_FILE%"
        ) else if "%%c" == "ModuleComponent" (
            echo   * **Fix:** Replace with `@Service(Service.Level.MODULE)` annotation >> "%OUTPUT_FILE%"
        ) else if "%%c" == "update(AnActionEvent" (
            echo   * **Fix:** Ensure correct signature with `@Override public void update(@NotNull AnActionEvent e)` >> "%OUTPUT_FILE%"
        ) else if "%%c" == "actionPerformed(" (
            echo   * **Fix:** Ensure correct signature with `@Override public void actionPerformed(@NotNull AnActionEvent e)` >> "%OUTPUT_FILE%"
        ) else if "%%c" == "LocalFileSystem.getInstance" (
            echo   * **Fix:** Handle properly with null checks >> "%OUTPUT_FILE%"
        ) else if "%%c" == "ProjectManager.getInstance" (
            echo   * **Fix:** Handle projects array safely with null checks >> "%OUTPUT_FILE%"
        ) else if "%%c" == "PsiManager.getInstance" (
            echo   * **Fix:** Use in read action with `CompatibilityUtil.runReadAction()` >> "%OUTPUT_FILE%"
        ) else if "%%c" == "NotificationGroup" (
            echo   * **Fix:** Use `NotificationGroupManager.getInstance().getNotificationGroup()` >> "%OUTPUT_FILE%"
        ) else if "%%c" == "createFileFromText" (
            echo   * **Fix:** Use in write action with `CompatibilityUtil.runWriteAction()` >> "%OUTPUT_FILE%"
        ) else if "%%c" == "Messages.show" (
            echo   * **Fix:** Consider using `ModForgeNotificationService` instead >> "%OUTPUT_FILE%"
        ) else (
            echo   * Consider using a compatibility utility method >> "%OUTPUT_FILE%"
        )
    )
) else (
    echo No compatibility issues found! >> "%OUTPUT_FILE%"
)

REM Add detailed compatibility guide
>> "%OUTPUT_FILE%" (
    echo.
    echo ## Comprehensive Compatibility Guide
    echo.
    echo ### General Guidelines
    echo.
    echo 1. **Always use CompatibilityUtil for core platform APIs**
    echo    - File operations through VirtualFileUtil
    echo    - Project operations through CompatibilityUtil
    echo    - Threading operations using ThreadUtils
    echo.
    echo 2. **Prefer service model over component model**
    echo    - Replace components with services using @Service annotation
    echo    - Register services in plugin.xml with proper level
    echo    - Use DI through constructors instead of getComponent calls
    echo.
    echo 3. **Handle threading properly**
    echo    - UI updates must be on EDT via invokeLater
    echo    - PSI operations must be in read actions
    echo    - VFS modifications must be in write actions
    echo    - Long operations should use virtual threads
    echo.
    echo 4. **Use proper annotations**
    echo    - @NotNull on parameters that must not be null
    echo    - @Nullable on return values that might be null
    echo    - @Override on overridden methods
    echo.
    echo 5. **Plugin.xml best practices**
    echo    - Use full element tags (no shorthand)
    echo    - Specify proper sinceBuild/untilBuild
    echo    - Minimize plugin dependencies
    echo.
    echo ### Implementation Notes
    echo.
    echo The utility classes created for compatibility:
    echo.
    echo 1. **CompatibilityUtil.java**: Core compatibility methods
    echo 2. **VirtualFileUtil.java**: VFS operations
    echo 3. **ThreadUtils.java**: Thread management with Java 21 virtual threads
    echo.
)

:CLEANUP
REM Clean up
if exist "%TEMP_PATTERN_FILE%" del "%TEMP_PATTERN_FILE%" 2>nul
if exist "%TEMP_MATCHES%" del "%TEMP_MATCHES%" 2>nul
if exist "%TEMP_FILES%" del "%TEMP_FILES%" 2>nul
if exist "%TEMP%\matches.txt" del "%TEMP%\matches.txt" 2>nul

echo Advanced compatibility analysis complete! Report written to: %OUTPUT_FILE%
echo.
echo Next steps:
echo   1. Review the detailed compatibility report
echo   2. Use the fix suggestions to systematically address each issue
echo   3. Implement the utility classes in your codebase
echo   4. Run this analysis again after making changes

endlocal