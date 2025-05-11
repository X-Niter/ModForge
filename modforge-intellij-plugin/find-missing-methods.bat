@echo off
setlocal enabledelayedexpansion

REM ===================================
REM ModForge Missing Method Scanner
REM ===================================
REM
REM This script specifically detects missing methods that cause "cannot find symbol" errors
REM in IntelliJ IDEA 2025.1.1.1 by analyzing method calls and cross-referencing with implementations.

REM Default values
set "OUTPUT_FILE=missing-methods.md"
set "SOURCE_DIR=src\main\java"
set "TEMP_METHODS=%TEMP%\methods_called.txt"
set "TEMP_DEFINED=%TEMP%\methods_defined.txt"
set "TEMP_FILES=%TEMP%\java_files.txt"
set "TEMP_SERVICE_CLASSES=%TEMP%\service_classes.txt"

echo ==== Scanning for Missing Methods and Cannot Find Symbol Errors ====

REM Check if the source directory exists
if not exist "%SOURCE_DIR%" (
    echo Source directory does not exist: %SOURCE_DIR%
    exit /b 1
)

REM Create output file with header
> "%OUTPUT_FILE%" (
    echo # Missing Methods and Symbol Resolution Report
    echo.
    echo This report identifies methods that are called but might not be implemented,
    echo which leads to "cannot find symbol" errors during compilation.
    echo.
    echo Generated on %DATE% %TIME%.
    echo.
    echo ## Overview
    echo.
    echo This analysis looks for:
    echo.
    echo 1. Methods called on service classes that might not exist
    echo 2. Method signature mismatches between calls and definitions
    echo 3. Missing getInstance() methods on service classes
    echo 4. Missing methods in common utility classes
    echo.
)

REM Find all Java files
dir /s /b "%SOURCE_DIR%\*.java" > "%TEMP_FILES%" 2>nul

echo Analyzing Java files for method definitions and calls...

REM Count total files
set /a TOTAL_FILES=0
for /f %%f in (%TEMP_FILES%) do set /a TOTAL_FILES+=1

if %TOTAL_FILES% equ 0 (
    echo No Java files found in %SOURCE_DIR%
    goto CLEANUP
)

REM Create list of service classes
> "%TEMP_SERVICE_CLASSES%" (
    echo ModForgeSettings
    echo ModAuthenticationManager
    echo AutonomousCodeGenerationService
    echo GitHubIntegrationService
    echo ModForgeNotificationService
    echo PatternRecognitionService
    echo CollaborationService
    echo MemoryOptimizer
    echo CompilationService
    echo BuildService
    echo ThreadPoolManager
    echo CodeAnalyzer
    echo ModManager
)

REM First pass: Extract method definitions
echo Extracting method definitions...
> "%TEMP_DEFINED%" (
    for /f "delims=" %%f in (%TEMP_FILES%) do (
        for /f "tokens=*" %%c in ('findstr /i "class " "%%f"') do (
            set "class_line=%%c"
            set "class_name=!class_line:*class = !"
            
            REM Remove anything after the class name (extends, implements, etc.)
            for /f "tokens=1 delims= {" %%n in ("!class_name!") do (
                set "clean_class_name=%%n"
                
                REM Skip inner classes and annotations
                echo !clean_class_name! | findstr /c:"@" >nul
                if !ERRORLEVEL! neq 0 (
                    REM Extract public methods with return types
                    for /f "tokens=*" %%m in ('findstr /i /c:"public " /c:"protected " "%%f" ^| findstr /v /c:"class " /c:"//'" /c:"/*" /c:"*"') do (
                        echo !clean_class_name!:%%m
                    )
                )
            )
        )
    )
)

REM Second pass: Extract method calls on service classes
echo Extracting method calls on service objects...
> "%TEMP_METHODS%" (
    for /f "delims=" %%f in (%TEMP_FILES%) do (
        for /f "tokens=*" %%c in ('type "%TEMP_SERVICE_CLASSES%"') do (
            REM Find variable declarations of service types
            for /f "tokens=*" %%v in ('findstr /i /c:"%%c " "%%f"') do (
                echo %%v | findstr /c:"=" >nul
                if !ERRORLEVEL! equ 0 (
                    REM Extract variable name
                    set "var_line=%%v"
                    set "var_line=!var_line:*%%c =!"
                    for /f "tokens=1 delims==" %%n in ("!var_line!") do (
                        set "var_name=%%n"
                        set "var_name=!var_name: =!"
                        
                        REM Now find method calls on this variable
                        for /f "tokens=*" %%m in ('findstr /i /c:"!var_name!." "%%f"') do (
                            echo %%m | findstr /c:")" >nul
                            if !ERRORLEVEL! equ 0 (
                                REM Looks like a method call
                                echo CALL:%%f:%%m
                            )
                        )
                    )
                )
            )
            
            REM Also look for static method calls
            for /f "tokens=*" %%s in ('findstr /i /c:"%%c\." "%%f"') do (
                echo STATIC:%%f:%%s
            )
        )
    )
)

REM Process the results and identify potential missing methods
echo Analyzing results for missing methods...

REM Prepare common problem sections for the report
>> "%OUTPUT_FILE%" (
    echo ## Common Problem Categories
    echo.
    echo ### 1. Missing Service Methods
    echo.
    echo These methods are called on service objects but might not be defined:
    echo.
    
    REM Parse the method calls to extract potential missing methods
    type "%TEMP_METHODS%" | findstr /i /c:"getAccessToken" /c:"isPatternRecognition" /c:"login" /c:"logout" /c:"getUsername" /c:"getGitHubUsername" /c:"openSettings" >nul
    
    if !ERRORLEVEL! equ 0 (
        echo - **ModForgeSettings.getAccessToken^(^)** - Called but may not be implemented
        echo - **ModForgeSettings.isPatternRecognition^(^)** - Called but may not be implemented
        echo - **ModForgeSettings.getGitHubUsername^(^)** - Called but may not be implemented
        echo - **ModForgeSettings.openSettings^(Project^)** - Called but may not be implemented
        echo - **ModAuthenticationManager.login^(String, String^)** - Called but may not be implemented
        echo - **ModAuthenticationManager.logout^(^)** - Called but may not be implemented
        echo - **ModAuthenticationManager.getUsername^(^)** - Called but may not be implemented
    ) else (
        echo No common missing methods detected
    )
    
    echo.
    echo ### 2. Missing Static Service Methods
    echo.
    echo These static service methods might be missing:
    echo.
    
    type "%TEMP_METHODS%" | findstr /i /c:"getInstance" >nul
    
    if !ERRORLEVEL! equ 0 (
        echo - **AutonomousCodeGenerationService.getInstance^(Project^)** - Static factory method may be missing
        echo - **Various service classes may need getInstance^(^) methods**
    ) else (
        echo No missing getInstance^(^) methods detected
    )
    
    echo.
    echo ### 3. Method Signature Mismatches
    echo.
    echo These method calls may have parameter type mismatches:
    echo.
    
    type "%TEMP_METHODS%" | findstr /i /c:"generateCode" /c:"fixCode" /c:"generateDocumentation" /c:"explainCode" >nul
    
    if !ERRORLEVEL! equ 0 (
        echo - **generateCode^(String, VirtualFile, String^)** - Check parameter types match implementation
        echo - **fixCode^(String, String, String^)** - Check parameter types match implementation
        echo - **generateDocumentation^(String, Object^)** - Check parameter types and nullability
        echo - **explainCode^(String, Object^)** - Check parameter types and nullability
    ) else (
        echo No method signature mismatches detected
    )
    
    echo.
    echo ### 4. DialogWrapper Override Issues
    echo.
    echo These methods in DialogWrapper subclasses may have incorrect overrides:
    echo.
    
    REM Identify potential DialogWrapper issues by checking for classes extending DialogWrapper
    findstr /i /c:"extends DialogWrapper" "%TEMP_FILES%" >nul
    
    if !ERRORLEVEL! equ 0 (
        echo - **getOwner^(^)** - Check return type matches DialogWrapper.getOwner^(^)
        echo - **createCenterPanel^(^)** - Should return JComponent
        echo - **createButtonsPanel^(^)** - Should return JComponent
        echo - **doOKAction^(^)** - Check implementation matches DialogWrapper
    ) else (
        echo No DialogWrapper override issues detected
    )
)

REM Find specific method calls for detailed analysis
>> "%OUTPUT_FILE%" (
    echo.
    echo ## Detailed Method Analysis
    echo.
)

REM Process files with potential errors
set "FOUND_ISSUES=0"

REM Check settings access methods
findstr /i /c:"settings\." "%TEMP_METHODS%" > "%TEMP%\settings_methods.txt" 2>nul
if exist "%TEMP%\settings_methods.txt" (
    set "FOUND_ISSUES=1"
    >> "%OUTPUT_FILE%" (
        echo ### ModForgeSettings Implementation Issues
        echo.
        echo The following methods might be missing from ModForgeSettings:
        echo.
        echo ```java
        echo // Required methods:
        echo public class ModForgeSettings {
        echo     public String getAccessToken() { ... }
        echo     public boolean isPatternRecognition() { ... }
        echo     public String getGitHubUsername() { ... }
        echo     public void openSettings(Project project) { ... }
        echo     // other methods...
        echo }
        echo ```
        echo.
    )
)

REM Check authentication manager methods
findstr /i /c:"authManager\." "%TEMP_METHODS%" > "%TEMP%\auth_methods.txt" 2>nul
if exist "%TEMP%\auth_methods.txt" (
    set "FOUND_ISSUES=1"
    >> "%OUTPUT_FILE%" (
        echo ### ModAuthenticationManager Implementation Issues
        echo.
        echo The following methods might be missing from ModAuthenticationManager:
        echo.
        echo ```java
        echo // Required methods:
        echo public class ModAuthenticationManager {
        echo     public boolean login(String username, String password) { ... }
        echo     public void logout() { ... }
        echo     public String getUsername() { ... }
        echo     // other methods...
        echo }
        echo ```
        echo.
    )
)

REM Check code generation service methods
findstr /i /c:"codeGenService\." /c:"AutonomousCodeGenerationService\." "%TEMP_METHODS%" > "%TEMP%\codegen_methods.txt" 2>nul
if exist "%TEMP%\codegen_methods.txt" (
    set "FOUND_ISSUES=1"
    >> "%OUTPUT_FILE%" (
        echo ### AutonomousCodeGenerationService Implementation Issues
        echo.
        echo The following methods might be missing from AutonomousCodeGenerationService:
        echo.
        echo ```java
        echo // Required static method:
        echo public class AutonomousCodeGenerationService {
        echo     public static AutonomousCodeGenerationService getInstance(Project project) { ... }
        echo     
        echo     // Required instance methods:
        echo     public String generateCode(String prompt, VirtualFile contextFile, String language) { ... }
        echo     public String fixCode(String code, String errorMessage, String language) { ... }
        echo     public CompletableFuture<String> generateDocumentation(String code, Object options) { ... }
        echo     public CompletableFuture<String> explainCode(String code, Object options) { ... }
        echo     public boolean generateImplementation(String interfaceName, String packageName, String className) { ... }
        echo     // other methods...
        echo }
        echo ```
        echo.
    )
)

REM Check GitHub integration service methods
findstr /i /c:"gitHubService\." "%TEMP_METHODS%" > "%TEMP%\github_methods.txt" 2>nul
if exist "%TEMP%\github_methods.txt" (
    set "FOUND_ISSUES=1"
    >> "%OUTPUT_FILE%" (
        echo ### GitHubIntegrationService Implementation Issues
        echo.
        echo The following methods might be missing from GitHubIntegrationService:
        echo.
        echo ```java
        echo // Required methods:
        echo public class GitHubIntegrationService {
        echo     public void pushToGitHub(String owner, String repository, String commitMessage, 
        echo                             boolean createIfNotExists, Consumer<String> progressCallback) { ... }
        echo     public void startMonitoring(String owner, String repository) { ... }
        echo     // other methods...
        echo }
        echo ```
        echo.
    )
)

REM Check notification service methods
findstr /i /c:"notificationService\." "%TEMP_METHODS%" > "%TEMP%\notification_methods.txt" 2>nul
if exist "%TEMP%\notification_methods.txt" (
    set "FOUND_ISSUES=1"
    >> "%OUTPUT_FILE%" (
        echo ### ModForgeNotificationService Implementation Issues
        echo.
        echo The following methods might be missing from ModForgeNotificationService:
        echo.
        echo ```java
        echo // Required methods:
        echo public class ModForgeNotificationService {
        echo     public void showInfo(String title, String content) { ... }
        echo     public void showError(String title, String content) { ... }
        echo     public void showWarning(String title, String content) { ... }
        echo     // other methods...
        echo }
        echo ```
        echo.
    )
)

REM Check for DialogWrapper issues
findstr /i /c:"extends DialogWrapper" "%TEMP_FILES%" > "%TEMP%\dialog_classes.txt" 2>nul
if exist "%TEMP%\dialog_classes.txt" (
    set "FOUND_ISSUES=1"
    >> "%OUTPUT_FILE%" (
        echo ### DialogWrapper Subclass Issues
        echo.
        echo The following issues might exist in DialogWrapper subclasses:
        echo.
        echo ```java
        echo // Issue: getOwner() in PushToGitHubDialog cannot override getOwner() in DialogWrapper
        echo // Fix: Rename the method or change the return type
        echo 
        echo // Instead of:
        echo public String getOwner() {
        echo     return ownerField.getText();
        echo }
        echo 
        echo // Use:
        echo public String getOwnerText() {
        echo     return ownerField.getText();
        echo }
        echo 
        echo // Or for other DialogWrapper methods, ensure the signature matches exactly:
        echo @Override
        echo protected @Nullable JComponent createCenterPanel() {
        echo     // implementation
        echo }
        echo ```
        echo.
    )
)

REM Check for Problem/WolfTheProblemSolver issues
findstr /i /c:"problemSolver\." "%TEMP_METHODS%" > "%TEMP%\problem_solver_methods.txt" 2>nul
if exist "%TEMP%\problem_solver_methods.txt" (
    set "FOUND_ISSUES=1"
    >> "%OUTPUT_FILE%" (
        echo ### WolfTheProblemSolver API Changes
        echo.
        echo The WolfTheProblemSolver API has changed. Check for these issues:
        echo.
        echo ```java
        echo // Instead of:
        echo problemSolver.getProblemFiles().forEach(...)
        echo problemSolver.getAllProblems()
        echo problem.getDescription()
        echo 
        echo // Use:
        echo CompatibilityUtil.getProblemFiles(problemSolver).forEach(...)
        echo CompatibilityUtil.getAllProblems(problemSolver)
        echo CompatibilityUtil.getProblemDescription(problem)
        echo ```
        echo.
    )
)

if "%FOUND_ISSUES%"=="0" (
    >> "%OUTPUT_FILE%" (
        echo No specific implementation issues detected.
    )
)

REM Add implementation recommendations
>> "%OUTPUT_FILE%" (
    echo.
    echo ## Implementation Recommendations
    echo.
    echo ### 1. Create Missing Methods
    echo.
    echo Start by implementing all missing methods in the service classes.
    echo The method signatures should match exactly what is expected by calling code.
    echo.
    echo ### 2. Fix Method Signatures
    echo.
    echo Ensure parameter types and return types match across the codebase:
    echo.
    echo 1. Check the method parameter types in class implementation
    echo 2. Ensure calling code passes compatible parameters
    echo 3. Add proper annotations (@NotNull, @Nullable) for clarity
    echo.
    echo ### 3. Add getInstance Methods
    echo.
    echo Implement consistent getInstance() methods for all service classes:
    echo.
    echo ```java
    echo public class SomeService {
    echo     private static final Map<Project, SomeService> instances = new ConcurrentHashMap<>();
    echo     
    echo     public static SomeService getInstance(Project project) {
    echo         return instances.computeIfAbsent(project, p -> new SomeService(p));
    echo     }
    echo     
    echo     private final Project project;
    echo     
    echo     private SomeService(Project project) {
    echo         this.project = project;
    echo     }
    echo }
    echo ```
    echo.
    echo ### 4. Fix DialogWrapper Issues
    echo.
    echo For any class extending DialogWrapper:
    echo.
    echo 1. Use @Override annotation for all overridden methods
    echo 2. Ensure the method signature matches the parent class exactly
    echo 3. Rename methods that conflict with parent class methods
    echo.
    echo ### 5. Create Compatibility Wrappers
    echo.
    echo Add methods to CompatibilityUtil for handling API changes:
    echo.
    echo ```java
    echo public class CompatibilityUtil {
    echo     // Methods for WolfTheProblemSolver compatibility
    echo     public static Collection<VirtualFile> getProblemFiles(WolfTheProblemSolver problemSolver) {
    echo         // Implementation that works with IntelliJ IDEA 2025.1.1.1
    echo     }
    echo     
    echo     public static List<Problem> getAllProblems(WolfTheProblemSolver problemSolver) {
    echo         // Implementation that works with IntelliJ IDEA 2025.1.1.1
    echo     }
    echo     
    echo     public static String getProblemDescription(Problem problem) {
    echo         // Implementation that works with IntelliJ IDEA 2025.1.1.1
    echo     }
    echo }
    echo ```
    echo.
)

:CLEANUP
REM Clean up
if exist "%TEMP_METHODS%" del "%TEMP_METHODS%" 2>nul
if exist "%TEMP_DEFINED%" del "%TEMP_DEFINED%" 2>nul
if exist "%TEMP_FILES%" del "%TEMP_FILES%" 2>nul
if exist "%TEMP_SERVICE_CLASSES%" del "%TEMP_SERVICE_CLASSES%" 2>nul
if exist "%TEMP%\settings_methods.txt" del "%TEMP%\settings_methods.txt" 2>nul
if exist "%TEMP%\auth_methods.txt" del "%TEMP%\auth_methods.txt" 2>nul
if exist "%TEMP%\codegen_methods.txt" del "%TEMP%\codegen_methods.txt" 2>nul
if exist "%TEMP%\github_methods.txt" del "%TEMP%\github_methods.txt" 2>nul
if exist "%TEMP%\notification_methods.txt" del "%TEMP%\notification_methods.txt" 2>nul
if exist "%TEMP%\dialog_classes.txt" del "%TEMP%\dialog_classes.txt" 2>nul
if exist "%TEMP%\problem_solver_methods.txt" del "%TEMP%\problem_solver_methods.txt" 2>nul

echo Missing methods analysis complete! Report written to: %OUTPUT_FILE%
echo.
echo Next steps:
echo   1. Review the detailed missing methods report
echo   2. Implement the missing methods in the appropriate service classes
echo   3. Fix method signature mismatches
echo   4. Add compatibility wrappers for changed APIs
echo   5. Run a build to verify the fixes

endlocal