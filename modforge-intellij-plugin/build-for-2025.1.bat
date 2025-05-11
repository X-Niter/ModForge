@echo off
echo ===== ModForge IntelliJ Plugin Builder for 2025.1 =====
echo.

echo Checking Java installation...
java -version > nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo Java not found. Please install JDK 17 or higher.
    exit /b 1
)

echo Checking for local IntelliJ installation...
set INTELLIJ_PATH=C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2025.1
if not exist "%INTELLIJ_PATH%" (
    echo IntelliJ IDEA 2025.1 not found at default location.
    set /p INTELLIJ_PATH="Please enter the path to IntelliJ IDEA 2025.1 installation: "
    if not exist "!INTELLIJ_PATH!" (
        echo IntelliJ IDEA 2025.1 not found. Will build using repository version.
        set USE_LOCAL=false
    ) else (
        set USE_LOCAL=true
    )
) else (
    set USE_LOCAL=true
)

echo Creating temporary build.gradle for 2025.1 compatibility...
set TEMP_GRADLE=build.gradle.temp

type build.gradle > %TEMP_GRADLE%

if "%USE_LOCAL%"=="true" (
    echo Using local IntelliJ at: %INTELLIJ_PATH%
    powershell -Command "(Get-Content %TEMP_GRADLE%) -replace '\/\/ localPath = .+', 'localPath = \""%INTELLIJ_PATH%\""; // Using local IntelliJ' -replace 'version = .+', '\/\/ version = \"2023.3.6\"; // Commented out to use localPath instead' | Set-Content %TEMP_GRADLE%"
) else (
    echo Using IntelliJ from repository.
)

echo Backing up original build.gradle...
copy build.gradle build.gradle.bak

echo Applying temporary build file...
copy %TEMP_GRADLE% build.gradle

echo Building plugin...
call gradlew clean buildPlugin --stacktrace

echo Restoring original build.gradle...
copy build.gradle.bak build.gradle
del build.gradle.bak
del %TEMP_GRADLE%

echo.
if exist "build\distributions\modforge-intellij-plugin-2.1.0.zip" (
    echo BUILD SUCCESSFUL!
    echo Plugin is available at: build\distributions\modforge-intellij-plugin-2.1.0.zip
) else (
    echo BUILD FAILED!
    echo Please check the error messages above.
)

echo.
echo ===== Build process completed =====
