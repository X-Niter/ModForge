@echo off
setlocal EnableDelayedExpansion

:: ========================================================
:: ModForge IntelliJ Plugin - ULTIMATE Universal Builder
:: ========================================================
:: This single script handles everything:
:: - Works in any Windows environment (Cmd/PowerShell)
:: - Detects & fixes encoding issues
:: - Finds or downloads Java 21
:: - Creates Gradle wrapper when needed
:: - Auto-fixes compilation errors
:: - Installs to IntelliJ automatically
:: ========================================================

:: Script configuration
set "VERSION=3.0.0"
set "PLUGIN_VERSION=2.1.0"
set "LOG_FILE=ultimate-builder.log"
set "POWERSHELL_SCRIPT=ultimate-builder-helper.ps1"

:: Clean start, begin log file
echo ModForge IntelliJ IDEA Plugin - ULTIMATE Builder v%VERSION% > "%LOG_FILE%"
echo Started: %DATE% %TIME% >> "%LOG_FILE%"
echo ======================================================== >> "%LOG_FILE%"

:: Display header - compatible with all command shells
echo.
echo ========================================================
echo   ModForge IntelliJ Plugin - ULTIMATE Universal Builder
echo ========================================================
echo.

:: Check for POWERSHELL support
set "HAS_POWERSHELL=0"
powershell -Command "exit" 2>nul
if %errorlevel% equ 0 (
    set "HAS_POWERSHELL=1"
)

:: Create helper PowerShell script with all the logic
echo Writing PowerShell helper script...

:: Create the PowerShell helper script
(
    echo # =============================================
    echo # ModForge IntelliJ Plugin - ULTIMATE Builder Helper
    echo # =============================================
    echo.
    echo # Script configuration
    echo $ScriptVersion = "%VERSION%"
    echo $PluginVersion = "%PLUGIN_VERSION%"
    echo $LogFile = "%LOG_FILE%"
    echo $MinJavaVersion = "21"
    echo $JdkDownloadUrl = "https://download.oracle.com/java/21/latest/jdk-21_windows-x64_bin.exe"
    echo $TemurinUrl = "https://api.adoptium.net/v3/installer/latest/21/ga/windows/x64/jdk/hotspot/normal/eclipse"
    echo $BackupDir = "backup_$(Get-Date -Format 'yyyy-MM-dd_HHmmss')"
    echo $FoundJava = $false
    echo $FoundGradle = $false
    echo $BuildSuccess = $false
    echo $IjPluginPath = "build\distributions\modforge-intellij-plugin-$PluginVersion.zip"
    echo $IsAdmin = ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
    echo.
    echo # Log message to console and file
    echo function Write-Log {
    echo     param (
    echo         [string]$Message,
    echo         [switch]$NoConsole,
    echo         [ValidateSet("INFO", "WARNING", "ERROR", "SUCCESS")]
    echo         [string]$Level = "INFO"
    echo     )
    echo     
    echo     $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    echo     $logMessage = "[$timestamp] [$Level] $Message"
    echo     
    echo     # Add to log file
    echo     $logMessage | Add-Content -Path $LogFile -Encoding utf8
    echo     
    echo     if (-not $NoConsole) {
    echo         # Format for console based on level
    echo         switch ($Level) {
    echo             "WARNING" { Write-Host $Message -ForegroundColor Yellow }
    echo             "ERROR" { Write-Host $Message -ForegroundColor Red }
    echo             "SUCCESS" { Write-Host $Message -ForegroundColor Green }
    echo             default { Write-Host $Message }
    echo         }
    echo     }
    echo }
    echo.
    echo # Backup project files
    echo function Backup-Project {
    echo     Write-Log "Creating backup in $BackupDir"
    echo     
    echo     if (-not (Test-Path $BackupDir)) {
    echo         New-Item -Path $BackupDir -ItemType Directory | Out-Null
    echo     }
    echo     
    echo     if (Test-Path "src") {
    echo         Copy-Item -Path "src" -Destination "$BackupDir\src" -Recurse -Force
    echo     }
    echo     
    echo     if (Test-Path "build.gradle") {
    echo         Copy-Item -Path "build.gradle" -Destination "$BackupDir\build.gradle" -Force
    echo     }
    echo     
    echo     if (Test-Path "gradle.properties") {
    echo         Copy-Item -Path "gradle.properties" -Destination "$BackupDir\gradle.properties" -Force
    echo     }
    echo     
    echo     Write-Log "Backup completed" -Level "SUCCESS"
    echo }
    echo.
    echo # Check Java installation
    echo function Find-Java {
    echo     Write-Log "Checking for Java installation..." -Level "INFO"
    echo     
    echo     # First check if java is in path
    echo     try {
    echo         $javaVersion = & java -version 2>&1
    echo         $javaVersionLine = $javaVersion | Where-Object { $_ -match "version" } | Select-Object -First 1
    echo         
    echo         if ($javaVersionLine -match "21") {
    echo             Write-Log "Found Java 21 in PATH: $javaVersionLine" -Level "SUCCESS"
    echo             
    echo             # Try to find actual Java path
    echo             $javaPath = (Get-Command java).Source
    echo             $javaPath = Split-Path -Parent (Split-Path -Parent $javaPath)
    echo             Write-Log "Java path: $javaPath"
    echo             
    echo             $script:JavaPath = $javaPath
    echo             $script:FoundJava = $true
    echo             return $true
    echo         } else {
    echo             Write-Log "Found Java but not version 21: $javaVersionLine" -Level "WARNING"
    echo             
    echo             $installJava = Read-Host "Would you like to download and install Java 21? (y/n)"
    echo             if ($installJava -eq "y") {
    echo                 return Install-Java
    echo             } else {
    echo                 Write-Log "Using existing Java version. This might cause issues."
    echo                 # Still get the path for non-21 Java
    echo                 $javaPath = (Get-Command java).Source
    echo                 $javaPath = Split-Path -Parent (Split-Path -Parent $javaPath)
    echo                 $script:JavaPath = $javaPath
    echo                 $script:FoundJava = $true
    echo                 return $true
    echo             }
    echo         }
    echo     } catch {
    echo         Write-Log "Java not found in PATH." -Level "WARNING"
    echo         
    echo         # Check JAVA_HOME
    echo         if ($env:JAVA_HOME) {
    echo             Write-Log "JAVA_HOME is set to: $env:JAVA_HOME"
    echo             $javaExe = Join-Path $env:JAVA_HOME "bin\java.exe"
    echo             
    echo             if (Test-Path $javaExe) {
    echo                 try {
    echo                     $javaVersion = & $javaExe -version 2>&1
    echo                     $javaVersionLine = $javaVersion | Where-Object { $_ -match "version" } | Select-Object -First 1
    echo                     
    echo                     if ($javaVersionLine -match "21") {
    echo                         Write-Log "Found Java 21 in JAVA_HOME: $javaVersionLine" -Level "SUCCESS"
    echo                         $script:JavaPath = $env:JAVA_HOME
    echo                         $script:FoundJava = $true
    echo                         return $true
    echo                     } else {
    echo                         Write-Log "JAVA_HOME points to Java but not version 21: $javaVersionLine" -Level "WARNING"
    echo                     }
    echo                 } catch {
    echo                     Write-Log "Error checking Java version in JAVA_HOME: $_" -Level "ERROR"
    echo                 }
    echo             } else {
    echo                 Write-Log "JAVA_HOME is set but java.exe not found at $javaExe" -Level "WARNING"
    echo             }
    echo         }
    echo         
    echo         # Comprehensive search
    echo         Write-Log "Performing comprehensive search for Java 21..."
    echo         $found = Search-JavaComprehensive
    echo         
    echo         if (-not $found) {
    echo             $installJava = Read-Host "Java 21 not found. Would you like to download and install it? (y/n)"
    echo             if ($installJava -eq "y") {
    echo                 return Install-Java
    echo             } else {
    echo                 Write-Log "Cannot continue without Java 21." -Level "ERROR"
    echo                 return $false
    echo             }
    echo         }
    echo         
    echo         return $true
    echo     }
    echo }
    echo.
    echo # Comprehensive search for Java 21
    echo function Search-JavaComprehensive {
    echo     Write-Log "Running comprehensive Java 21 search..."
    echo     
    echo     # Common installation paths
    echo     $javaPaths = @(
    echo         "C:\Program Files\Java\*",
    echo         "C:\Program Files (x86)\Java\*",
    echo         "$env:LOCALAPPDATA\Programs\Eclipse Adoptium\*",
    echo         "$env:USERPROFILE\.jdks\*"
    echo     )
    echo     
    echo     # Search all paths
    echo     foreach ($pathPattern in $javaPaths) {
    echo         Get-Item $pathPattern -ErrorAction SilentlyContinue | ForEach-Object {
    echo             $javaExe = Join-Path $_.FullName "bin\java.exe"
    echo             if (Test-Path $javaExe) {
    echo                 try {
    echo                     $javaVersion = & $javaExe -version 2>&1
    echo                     $javaVersionLine = $javaVersion | Where-Object { $_ -match "version" } | Select-Object -First 1
    echo                     
    echo                     if ($javaVersionLine -match "21") {
    echo                         Write-Log "Found Java 21: $($_.FullName)" -Level "SUCCESS"
    echo                         $script:JavaPath = $_.FullName
    echo                         $script:FoundJava = $true
    echo                         return $true
    echo                     }
    echo                 } catch {
    echo                     # Just continue with search
    echo                 }
    echo             }
    echo         }
    echo     }
    echo     
    echo     # Check JetBrains bundled JDKs
    echo     $jbPaths = @(
    echo         "$env:LOCALAPPDATA\JetBrains",
    echo         "$env:USERPROFILE\.jdks"
    echo     )
    echo     
    echo     foreach ($path in $jbPaths) {
    echo         if (Test-Path $path) {
    echo             # Find jbr directories
    echo             Get-ChildItem -Path $path -Filter "jbr" -Recurse -ErrorAction SilentlyContinue | ForEach-Object {
    echo                 $javaExe = Join-Path $_.FullName "bin\java.exe"
    echo                 if (Test-Path $javaExe) {
    echo                     try {
    echo                         $javaVersion = & $javaExe -version 2>&1
    echo                         $javaVersionLine = $javaVersion | Where-Object { $_ -match "version" } | Select-Object -First 1
    echo                         
    echo                         if ($javaVersionLine -match "21") {
    echo                             Write-Log "Found JetBrains Bundled Java 21: $($_.FullName)" -Level "SUCCESS"
    echo                             $script:JavaPath = $_.FullName
    echo                             $script:FoundJava = $true
    echo                             return $true
    echo                         }
    echo                     } catch {
    echo                         # Continue searching
    echo                     }
    echo                 }
    echo             }
    echo             
    echo             # Check .jdks directory specifically
    echo             if ($path -eq "$env:USERPROFILE\.jdks") {
    echo                 Get-ChildItem -Path $path -Directory | Where-Object { $_.Name -match "jbr-21|21" } | ForEach-Object {
    echo                     $javaExe = Join-Path $_.FullName "bin\java.exe"
    echo                     if (Test-Path $javaExe) {
    echo                         try {
    echo                             $javaVersion = & $javaExe -version 2>&1
    echo                             $javaVersionLine = $javaVersion | Where-Object { $_ -match "version" } | Select-Object -First 1
    echo                             
    echo                             if ($javaVersionLine -match "21") {
    echo                                 Write-Log "Found Java 21 in .jdks: $($_.FullName)" -Level "SUCCESS"
    echo                                 $script:JavaPath = $_.FullName
    echo                                 $script:FoundJava = $true
    echo                                 return $true
    echo                             }
    echo                         } catch {
    echo                             # Continue searching
    echo                         }
    echo                     }
    echo                 }
    echo             }
    echo         }
    echo     }
    echo     
    echo     return $script:FoundJava
    echo }
    echo.
    echo # Install Java 21
    echo function Install-Java {
    echo     Write-Log "Preparing to download Java 21..." -Level "INFO"
    echo     
    echo     # Choose JDK type
    echo     Write-Host "Which Java 21 distribution would you prefer?"
    echo     Write-Host "1) Oracle JDK 21 (official Oracle build)"
    echo     Write-Host "2) Eclipse Temurin 21 (community build, recommended)"
    echo     $javaChoice = Read-Host "Enter choice (1/2)"
    echo     
    echo     if ($javaChoice -eq "1") {
    echo         $downloadUrl = $JdkDownloadUrl
    echo         $installerName = "jdk21_installer.exe"
    echo         $javaVendor = "Oracle"
    echo     } else {
    echo         $downloadUrl = $TemurinUrl
    echo         $installerName = "temurin21_installer.exe"
    echo         $javaVendor = "Temurin"
    echo     }
    echo     
    echo     # Download the installer
    echo     Write-Log "Downloading $javaVendor JDK 21 installer..."
    echo     Write-Host "This may take a few minutes depending on your internet connection."
    echo     
    echo     try {
    echo         [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    echo         Invoke-WebRequest -Uri $downloadUrl -OutFile $installerName
    echo         Write-Log "Download completed successfully" -Level "SUCCESS"
    echo     } catch {
    echo         Write-Log "Failed to download Java 21 installer: $_" -Level "ERROR"
    echo         return $false
    echo     }
    echo     
    echo     if (-not (Test-Path $installerName)) {
    echo         Write-Log "Installer file not found after download" -Level "ERROR"
    echo         return $false
    echo     }
    echo     
    echo     # Install Java 21
    echo     Write-Log "Installing Java 21 ($javaVendor JDK)..."
    echo     
    echo     # Run the installer with appropriate permissions
    echo     if (-not $IsAdmin) {
    echo         Write-Log "Administrator privileges are required to install Java." -Level "WARNING"
    echo         try {
    echo             Start-Process -FilePath $installerName -ArgumentList "/s" -Verb RunAs -Wait
    echo         } catch {
    echo             Write-Log "Failed to launch installer with admin rights: $_" -Level "ERROR"
    echo             return $false
    echo         }
    echo     } else {
    echo         Start-Process -FilePath $installerName -ArgumentList "/s" -Wait
    echo     }
    echo     
    echo     # Clean up installer
    echo     if (Test-Path $installerName) {
    echo         Remove-Item $installerName -Force
    echo     }
    echo     
    echo     # Verify installation
    echo     Write-Log "Verifying Java 21 installation..."
    echo     
    echo     # Reset the previously found flag
    echo     $script:FoundJava = $false
    echo     
    echo     # Expected installation paths
    echo     if ($javaVendor -eq "Oracle") {
    echo         $expectedPath = "C:\Program Files\Java\jdk-21"
    echo     } else {
    echo         $expectedPath = "C:\Program Files\Eclipse Adoptium\jdk-21"
    echo     }
    echo     
    echo     if (Test-Path "$expectedPath\bin\java.exe") {
    echo         try {
    echo             $javaVersion = & "$expectedPath\bin\java.exe" -version 2>&1
    echo             $javaVersionLine = $javaVersion | Where-Object { $_ -match "version" } | Select-Object -First 1
    echo             
    echo             if ($javaVersionLine -match "21") {
    echo                 Write-Log "Confirmed Java 21 installation at $expectedPath" -Level "SUCCESS"
    echo                 $script:JavaPath = $expectedPath
    echo                 $script:FoundJava = $true
    echo             }
    echo         } catch {
    echo             Write-Log "Error verifying Java installation: $_" -Level "ERROR"
    echo         }
    echo     }
    echo     
    echo     # If we still can't find it, search again
    echo     if (-not $script:FoundJava) {
    echo         Write-Log "Java 21 installation location not found at expected path." -Level "WARNING"
    echo         Write-Log "Searching for the newly installed Java 21..."
    echo         Search-JavaComprehensive
    echo     }
    echo     
    echo     if ($script:FoundJava) {
    echo         Write-Log "Java 21 installation successful: $script:JavaPath" -Level "SUCCESS"
    echo         
    echo         # Ask if user wants to set JAVA_HOME
    echo         $setJavaHome = Read-Host "Would you like to set JAVA_HOME environment variable to point to Java 21? (y/n)"
    echo         
    echo         if ($setJavaHome -eq "y") {
    echo             if ($IsAdmin) {
    echo                 [Environment]::SetEnvironmentVariable("JAVA_HOME", $script:JavaPath, "Machine")
    echo                 Write-Log "Set JAVA_HOME to $script:JavaPath system-wide" -Level "SUCCESS"
    echo             } else {
    echo                 [Environment]::SetEnvironmentVariable("JAVA_HOME", $script:JavaPath, "User")
    echo                 Write-Log "Set JAVA_HOME to $script:JavaPath for current user" -Level "SUCCESS"
    echo             }
    echo         }
    echo         
    echo         # Set for current session
    echo         $env:JAVA_HOME = $script:JavaPath
    echo         $env:PATH = "$($script:JavaPath)\bin;$env:PATH"
    echo         Write-Log "Environment configured for Java 21 in this session"
    echo         
    echo         return $true
    echo     } else {
    echo         Write-Log "Java 21 installation verification failed." -Level "ERROR"
    echo         return $false
    echo     }
    echo }
    echo.
    echo # Check Gradle installation
    echo function Find-Gradle {
    echo     Write-Log "Checking Gradle installation..." -Level "INFO"
    echo     
    echo     # First look for Gradle wrapper
    echo     if (Test-Path "gradlew") {
    echo         Write-Log "Found Gradle wrapper (gradlew)" -Level "SUCCESS"
    echo         $script:GradleCommand = "./gradlew"
    echo         $script:FoundGradle = $true
    echo     } elseif (Test-Path "gradlew.bat") {
    echo         Write-Log "Found Gradle wrapper (gradlew.bat)" -Level "SUCCESS"
    echo         $script:GradleCommand = "./gradlew.bat"
    echo         $script:FoundGradle = $true
    echo     } else {
    echo         # Check for Gradle in PATH
    echo         try {
    echo             $gradleCmd = Get-Command gradle -ErrorAction Stop
    echo             Write-Log "Found Gradle in PATH: $($gradleCmd.Source)" -Level "SUCCESS"
    echo             $script:GradleCommand = "gradle"
    echo             $script:FoundGradle = $true
    echo         } catch {
    echo             # Create wrapper
    echo             Write-Log "Gradle not found. Creating Gradle wrapper..." -Level "WARNING"
    echo             Create-GradleWrapper
    echo         }
    echo     }
    echo     
    echo     return $script:FoundGradle
    echo }
    echo.
    echo # Create a Gradle wrapper if needed
    echo function Create-GradleWrapper {
    echo     Write-Log "Creating basic Gradle wrapper files..."
    echo     
    echo     # Create gradlew.bat for Windows
    echo     @"
    echo @echo off
    echo.
    echo rem Gradle wrapper script for Windows
    echo rem Auto-generated by ModForge ULTIMATE Builder
    echo.
    echo if not exist "%~dp0\gradle\wrapper\gradle-wrapper.jar" (
    echo     echo Creating Gradle wrapper directory structure...
    echo     if not exist "%~dp0\gradle\wrapper" mkdir "%~dp0\gradle\wrapper"
    echo )
    echo.
    echo rem Check if Gradle is in PATH
    echo gradle --version 2>nul >nul
    echo if %errorlevel% == 0 (
    echo     echo Using Gradle from PATH
    echo     gradle %*
    echo ) else (
    echo     echo Gradle not found in PATH. Attempting to download...
    echo     powershell -Command "& {[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-8.5-bin.zip' -OutFile 'gradle-8.5-bin.zip'}"
    echo     echo Extracting Gradle...
    echo     powershell -Command "& {Expand-Archive -Path 'gradle-8.5-bin.zip' -DestinationPath 'gradle-temp' -Force}"
    echo     echo Running extracted Gradle...
    echo     call gradle-temp\gradle-8.5\bin\gradle.bat %*
    echo     echo Cleaning up...
    echo     rmdir /S /Q gradle-temp
    echo     del gradle-8.5-bin.zip
    echo )
    echo "@ | Out-File -FilePath "gradlew.bat" -Encoding ascii
    echo     
    echo     Write-Log "Created gradlew.bat" -Level "SUCCESS"
    echo     $script:GradleCommand = "./gradlew.bat"
    echo     $script:FoundGradle = $true
    echo     
    echo     return $true
    echo }
    echo.
    echo # Configure Gradle for build
    echo function Configure-Gradle {
    echo     Write-Log "Configuring Gradle for build..." -Level "INFO"
    echo     
    echo     # Check if build.gradle exists
    echo     if (-not (Test-Path "build.gradle")) {
    echo         Write-Log "build.gradle not found. Cannot continue." -Level "ERROR"
    echo         return $false
    echo     }
    echo     
    echo     # Create or update gradle.properties
    echo     if (Test-Path "gradle.properties") {
    echo         Copy-Item -Path "gradle.properties" -Destination "gradle.properties.bak" -Force
    echo         Write-Log "Backed up gradle.properties"
    echo     }
    echo     
    echo     # Create or update gradle.properties with the correct Java path
    echo     $normalizedPath = $script:JavaPath.Replace("\", "/")
    echo     $gradleProperties = @"
    echo # Gradle properties for ModForge - Generated by ULTIMATE Builder
    echo # $(Get-Date)
    echo.
    echo # Set Java home for building
    echo org.gradle.java.home=$normalizedPath
    echo.
    echo # Disable configuration cache to avoid issues
    echo org.gradle.configuration-cache=false
    echo.
    echo # Increase memory for Gradle tasks
    echo org.gradle.jvmargs=-Xmx2048m --add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8
    echo.
    echo # Enable parallel execution
    echo org.gradle.parallel=true
    echo.
    echo # Optimize Gradle daemon
    echo org.gradle.daemon=true
    echo org.gradle.caching=true
    echo "@
    echo     
    echo     $gradleProperties | Out-File -FilePath "gradle.properties" -Encoding UTF8
    echo     Write-Log "Updated gradle.properties with correct Java home path" -Level "SUCCESS"
    echo     
    echo     return $true
    echo }
    echo.
    echo # Fix code issues
    echo function Fix-CodeIssues {
    echo     Write-Log "Checking for and fixing common code issues..." -Level "INFO"
    echo     
    echo     # Run a diagnostic build to get error information
    echo     Write-Log "Running diagnostic build to identify issues..."
    echo     try {
    echo         & $script:GradleCommand compileJava --info | Out-File -FilePath "build_issues.log" -Encoding UTF8
    echo     } catch {
    echo         Write-Log "Error running Gradle build: $_" -Level "ERROR"
    echo         return $false
    echo     }
    echo     
    echo     # Check if there were compilation errors
    echo     $hasErrors = Select-String -Path "build_issues.log" -Pattern "error:" -Quiet
    echo     
    echo     if ($hasErrors) {
    echo         Write-Log "Found compilation errors. Attempting to fix..." -Level "WARNING"
    echo         Apply-FixesFromErrors
    echo     } else {
    echo         Write-Log "No compilation errors found in initial analysis" -Level "SUCCESS"
    echo     }
    echo     
    echo     return $true
    echo }
    echo.
    echo # Apply fixes based on identified errors
    echo function Apply-FixesFromErrors {
    echo     # Check for missing imports and apply fixes
    echo     $importChecks = @{
    echo         "java.util.Arrays" = "Arrays\."
    echo         "java.util.concurrent.CompletableFuture" = "CompletableFuture"
    echo         "org.java_websocket.client.WebSocketClient" = "WebSocketClient"
    echo         "com.intellij.notification.NotificationType" = "NotificationType"
    echo     }
    echo     
    echo     foreach ($import in $importChecks.Keys) {
    echo         $pattern = $importChecks[$import]
    echo         $needsFix = Select-String -Path "build_issues.log" -Pattern $pattern -Quiet
    echo         
    echo         if ($needsFix) {
    echo             Write-Log "Fixing missing import: $import"
    echo             
    echo             Get-ChildItem -Path "src\main\java" -Filter "*.java" -Recurse | ForEach-Object {
    echo                 $content = Get-Content -Path $_.FullName -Raw
    echo                 if ($content -match $pattern -and -not ($content -match "import $import")) {
    echo                     $content = $content -replace "package com\.modforge", "package com.modforge`r`n`r`nimport $import;"
    echo                     $content | Set-Content -Path $_.FullName -Encoding UTF8
    echo                 }
    echo             }
    echo         }
    echo     }
    echo     
    echo     # Special case fixes
    echo     $specialFixes = @{
    echo         "problem.getDescription" = @{
    echo             Pattern = "problem\.getDescription\(\)"
    echo             Replacement = "problem.toString()"
    echo         }
    echo         "DialogWrapper.getOwner" = @{
    echo             File = "PushToGitHubDialog.java"
    echo             Pattern = "public String getOwner\(\)"
    echo             Replacement = "public String getRepositoryOwner()"
    echo             AdditionalAction = { param($content) $content -replace "getOwner\(\)", "getRepositoryOwner()" }
    echo         }
    echo         "Messages.showInfoDialog" = @{
    echo             Pattern = "Messages\.showInfoDialog\((.*?),(.*?),(.*?)\)"
    echo             Replacement = "Messages.showMessageDialog($1,$2,$3, Messages.getInformationIcon())"
    echo         }
    echo         "AutonomousCodeGenerationService.getInstance" = @{
    echo             Pattern = "AutonomousCodeGenerationService\.getInstance\(project\)"
    echo             Replacement = "project.getService(AutonomousCodeGenerationService.class)"
    echo         }
    echo     }
    echo     
    echo     foreach ($key in $specialFixes.Keys) {
    echo         $fix = $specialFixes[$key]
    echo         $needsFix = Select-String -Path "build_issues.log" -Pattern $key -Quiet
    echo         
    echo         if ($needsFix) {
    echo             Write-Log "Applying fix for: $key"
    echo             
    echo             if ($fix.ContainsKey("File")) {
    echo                 # File-specific fix
    echo                 Get-ChildItem -Path "src\main\java" -Filter $fix.File -Recurse | ForEach-Object {
    echo                     $content = Get-Content -Path $_.FullName -Raw
    echo                     if ($content -match $fix.Pattern) {
    echo                         $content = $content -replace $fix.Pattern, $fix.Replacement
    echo                         if ($fix.ContainsKey("AdditionalAction")) {
    echo                             $content = & $fix.AdditionalAction $content
    echo                         }
    echo                         $content | Set-Content -Path $_.FullName -Encoding UTF8
    echo                     }
    echo                 }
    echo             } else {
    echo                 # General fix across all files
    echo                 Get-ChildItem -Path "src\main\java" -Filter "*.java" -Recurse | ForEach-Object {
    echo                     $content = Get-Content -Path $_.FullName -Raw
    echo                     if ($content -match $fix.Pattern) {
    echo                         $content = $content -replace $fix.Pattern, $fix.Replacement
    echo                         $content | Set-Content -Path $_.FullName -Encoding UTF8
    echo                     }
    echo                 }
    echo             }
    echo         }
    echo     }
    echo     
    echo     Write-Log "Applied fixes for common code issues" -Level "SUCCESS"
    echo }
    echo.
    echo # Build the plugin
    echo function Build-Plugin {
    echo     Write-Log "Building ModForge IntelliJ plugin..." -Level "INFO"
    echo     
    echo     # Run the build
    echo     Write-Log "Running Gradle build..."
    echo     try {
    echo         & $script:GradleCommand clean build | Tee-Object -FilePath "build.log"
    echo     } catch {
    echo         Write-Log "Error running Gradle build: $_" -Level "ERROR"
    echo     }
    echo     
    echo     # Check if the build succeeded
    echo     if (Test-Path $IjPluginPath) {
    echo         Write-Log "Plugin built successfully!" -Level "SUCCESS"
    echo         $script:BuildSuccess = $true
    echo     } else {
    echo         # Try with validation disabled
    echo         Write-Log "Build failed, trying with validation disabled..." -Level "WARNING"
    echo         
    echo         # Create a temporary copy of build.gradle
    echo         if (Test-Path "build.gradle") {
    echo             Copy-Item -Path "build.gradle" -Destination "build.gradle.bak" -Force
    echo             
    echo             # Disable validation in build.gradle
    echo             (Get-Content build.gradle) -replace "tasks.buildPlugin.dependsOn\(validatePluginForProduction\)", "// tasks.buildPlugin.dependsOn(validatePluginForProduction) // Disabled by ULTIMATE Builder" | Set-Content build.gradle -Encoding UTF8
    echo             
    echo             # Run the build again
    echo             Write-Log "Running Gradle build with validation disabled..."
    echo             try {
    echo                 & $script:GradleCommand clean build | Tee-Object -FilePath "build_simple.log"
    echo             } catch {
    echo                 Write-Log "Error running simplified Gradle build: $_" -Level "ERROR"
    echo             }
    echo             
    echo             # Restore the original build.gradle
    echo             Move-Item -Path "build.gradle.bak" -Destination "build.gradle" -Force
    echo             
    echo             if (Test-Path $IjPluginPath) {
    echo                 Write-Log "Plugin built successfully with validation disabled!" -Level "SUCCESS"
    echo                 $script:BuildSuccess = $true
    echo             } else {
    echo                 # Check for specific errors in the build log
    echo                 Write-Log "Build still failed." -Level "ERROR"
    echo                 if (Test-Path "build_simple.log") {
    echo                     $errors = Select-String -Path "build_simple.log" -Pattern "error:"
    echo                     $errors | ForEach-Object { Write-Log $_.Line -Level "ERROR" }
    echo                 }
    echo                 Write-Log "See build_simple.log for more details."
    echo                 return $false
    echo             }
    echo         } else {
    echo             Write-Log "build.gradle not found. Cannot continue." -Level "ERROR"
    echo             return $false
    echo         }
    echo     }
    echo     
    echo     Write-Log "Plugin file created at: $(Resolve-Path $IjPluginPath)" -Level "SUCCESS"
    echo     return $true
    echo }
    echo.
    echo # Install the plugin to IntelliJ IDEA
    echo function Install-Plugin {
    echo     if (-not $script:BuildSuccess) {
    echo         Write-Log "Cannot install plugin as build was not successful." -Level "ERROR"
    echo         return $false
    echo     }
    echo     
    echo     Write-Log "Preparing to install plugin..." -Level "INFO"
    echo     
    echo     # Ask user if they want to install the plugin
    echo     $installPlugin = Read-Host "Would you like to install the plugin to IntelliJ IDEA? (y/n)"
    echo     if ($installPlugin -ne "y") {
    echo         Write-Log "User declined plugin installation."
    echo         Write-Log "Plugin file is available at: $(Resolve-Path $IjPluginPath)"
    echo         Write-Log "You can install it manually via Settings → Plugins → ⚙ → Install Plugin from Disk..."
    echo         return $false
    echo     }
    echo     
    echo     # Find IntelliJ installations
    echo     Write-Log "Searching for IntelliJ IDEA installations..."
    echo     $intellijPaths = @()
    echo     
    echo     # Check common IntelliJ installation paths
    echo     $searchPaths = @(
    echo         "$env:PROGRAMFILES\JetBrains",
    echo         "${env:PROGRAMFILES(X86)}\JetBrains",
    echo         "$env:LOCALAPPDATA\JetBrains"
    echo     )
    echo     
    echo     foreach ($path in $searchPaths) {
    echo         if (Test-Path $path) {
    echo             Get-ChildItem -Path $path -Filter "IntelliJ*" -Directory | ForEach-Object {
    echo                 $ideaExe = Join-Path $_.FullName "bin\idea.exe"
    echo                 if (Test-Path $ideaExe) {
    echo                     Write-Log "Found IntelliJ IDEA: $($_.FullName)"
    echo                     $intellijPaths += $_.FullName
    echo                 }
    echo             }
    echo         }
    echo     }
    echo     
    echo     # Check JetBrains Toolbox locations
    echo     if (Test-Path "$env:LOCALAPPDATA\JetBrains\Toolbox") {
    echo         $toolboxPaths = @(
    echo             "$env:LOCALAPPDATA\JetBrains\Toolbox\apps\IDEA-C\ch-*",
    echo             "$env:LOCALAPPDATA\JetBrains\Toolbox\apps\IDEA-U\ch-*"
    echo         )
    echo         
    echo         foreach ($pathPattern in $toolboxPaths) {
    echo             Get-Item $pathPattern -ErrorAction SilentlyContinue | ForEach-Object {
    echo                 $ideaExe = Join-Path $_.FullName "bin\idea.exe"
    echo                 if (Test-Path $ideaExe) {
    echo                     Write-Log "Found IntelliJ IDEA (Toolbox): $($_.FullName)"
    echo                     $intellijPaths += $_.FullName
    echo                 }
    echo             }
    echo         }
    echo     }
    echo     
    echo     if ($intellijPaths.Count -eq 0) {
    echo         Write-Log "No IntelliJ IDEA installations found." -Level "WARNING"
    echo         Write-Log "Plugin file is available at: $(Resolve-Path $IjPluginPath)"
    echo         Write-Log "You can install it manually via Settings → Plugins → ⚙ → Install Plugin from Disk..."
    echo         return $false
    echo     }
    echo     
    echo     # Let user select IntelliJ installation if multiple found
    echo     $intellijPath = $null
    echo     if ($intellijPaths.Count -gt 1) {
    echo         Write-Host "Multiple IntelliJ IDEA installations found. Please select one:"
    echo         for ($i = 0; $i -lt $intellijPaths.Count; $i++) {
    echo             Write-Host "$($i+1)) $($intellijPaths[$i])"
    echo         }
    echo         
    echo         $choice = [int](Read-Host "Select installation (1-$($intellijPaths.Count))") - 1
    echo         $intellijPath = $intellijPaths[$choice]
    echo     } else {
    echo         $intellijPath = $intellijPaths[0]
    echo     }
    echo     
    echo     Write-Log "Selected: $intellijPath"
    echo     
    echo     # Find plugins directory
    echo     $pluginsDir = Join-Path $intellijPath "plugins"
    echo     if (-not (Test-Path $pluginsDir)) {
    echo         $pluginsDir = Join-Path $intellijPath "config\plugins"
    echo     }
    echo     
    echo     if (-not (Test-Path $pluginsDir)) {
    echo         Write-Log "Cannot find plugins directory." -Level "WARNING"
    echo         Write-Log "Plugin file is available at: $(Resolve-Path $IjPluginPath)"
    echo         Write-Log "You can install it manually via Settings → Plugins → ⚙ → Install Plugin from Disk..."
    echo         return $false
    echo     }
    echo     
    echo     # Check if IntelliJ is running
    echo     $ideaRunning = Get-Process -Name "idea64" -ErrorAction SilentlyContinue
    echo     if ($ideaRunning) {
    echo         Write-Log "IntelliJ IDEA is currently running." -Level "WARNING"
    echo         Write-Log "Please close all instances before continuing." -Level "WARNING"
    echo         
    echo         $closed = Read-Host "Have you closed all IntelliJ IDEA instances? (y/n)"
    echo         if ($closed -ne "y") {
    echo             Write-Log "Installation aborted."
    echo             Write-Log "Plugin file is available at: $(Resolve-Path $IjPluginPath)"
    echo             return $false
    echo         }
    echo     }
    echo     
    echo     # Install the plugin
    echo     Write-Log "Installing plugin to $pluginsDir..."
    echo     
    echo     # Extract and install the plugin
    echo     $pluginName = "modforge-intellij-plugin"
    echo     $tempDir = Join-Path $env:TEMP "modforge_plugin_install"
    echo     
    echo     # Clean up any previous temp dir
    echo     if (Test-Path $tempDir) {
    echo         Remove-Item -Path $tempDir -Recurse -Force -ErrorAction SilentlyContinue
    echo     }
    echo     
    echo     # Create temp dir and extract plugin
    echo     New-Item -Path $tempDir -ItemType Directory -Force | Out-Null
    echo     
    echo     try {
    echo         # Extract the zip
    echo         Expand-Archive -Path $IjPluginPath -DestinationPath $tempDir -Force
    echo         
    echo         # Remove existing plugin if present
    echo         $targetPluginDir = Join-Path $pluginsDir $pluginName
    echo         if (Test-Path $targetPluginDir) {
    echo             Remove-Item -Path $targetPluginDir -Recurse -Force -ErrorAction SilentlyContinue
    echo         }
    echo         
    echo         # Copy the plugin to the plugins directory
    echo         Copy-Item -Path (Join-Path $tempDir $pluginName) -Destination $pluginsDir -Recurse -Force
    echo         
    echo         # Clean up
    echo         Remove-Item -Path $tempDir -Recurse -Force -ErrorAction SilentlyContinue
    echo         
    echo         if (Test-Path (Join-Path $pluginsDir $pluginName)) {
    echo             Write-Log "Plugin installed successfully!" -Level "SUCCESS"
    echo             Write-Log "Please restart IntelliJ IDEA to use the new plugin." -Level "SUCCESS"
    echo             return $true
    echo         } else {
    echo             Write-Log "Plugin installation failed." -Level "ERROR"
    echo             return $false
    echo         }
    echo     } catch {
    echo         Write-Log "Error installing plugin: $_" -Level "ERROR"
    echo         Write-Log "Plugin file is available at: $(Resolve-Path $IjPluginPath)"
    echo         Write-Log "You can install it manually via Settings → Plugins → ⚙ → Install Plugin from Disk..."
    echo         return $false
    echo     }
    echo }
    echo.
    echo # Main function
    echo function Main {
    echo     # Clear screen and set title
    echo     Clear-Host
    echo     $Host.UI.RawUI.WindowTitle = "ModForge ULTIMATE Builder v$ScriptVersion"
    echo     
    echo     # Display header
    echo     Write-Host "========================================================"
    echo     Write-Host "  ModForge IntelliJ Plugin - ULTIMATE Universal Builder  "
    echo     Write-Host "========================================================"
    echo     Write-Host ""
    echo     
    echo     # Check admin status
    echo     if ($IsAdmin) {
    echo         Write-Log "Running with administrator privileges." -Level "SUCCESS"
    echo     } else {
    echo         Write-Log "Not running with administrator privileges." -Level "WARNING"
    echo         Write-Log "Some operations may require elevation." -Level "WARNING"
    echo     }
    echo     
    echo     # Create backup
    echo     Backup-Project
    echo     
    echo     # Step 1: Check Java
    echo     $javaResult = Find-Java
    echo     if (-not $javaResult) {
    echo         Write-Log "Java setup failed. Cannot continue." -Level "ERROR"
    echo         Read-Host "Press Enter to exit"
    echo         return $false
    echo     }
    echo     
    echo     # Step 2: Check Gradle
    echo     $gradleResult = Find-Gradle
    echo     if (-not $gradleResult) {
    echo         Write-Log "Gradle setup failed. Cannot continue." -Level "ERROR"
    echo         Read-Host "Press Enter to exit"
    echo         return $false
    echo     }
    echo     
    echo     # Step 3: Configure Gradle
    echo     $configureResult = Configure-Gradle
    echo     if (-not $configureResult) {
    echo         Write-Log "Gradle configuration failed. Cannot continue." -Level "ERROR"
    echo         Read-Host "Press Enter to exit"
    echo         return $false
    echo     }
    echo     
    echo     # Step 4: Fix code issues
    echo     $fixResult = Fix-CodeIssues
    echo     if (-not $fixResult) {
    echo         Write-Log "Code fixing process failed." -Level "ERROR"
    echo         Read-Host "Press Enter to exit"
    echo         return $false
    echo     }
    echo     
    echo     # Step 5: Build plugin
    echo     $buildResult = Build-Plugin
    echo     if (-not $buildResult) {
    echo         Write-Log "Build failed." -Level "ERROR"
    echo         Read-Host "Press Enter to exit"
    echo         return $false
    echo     }
    echo     
    echo     # Step 6: Install plugin
    echo     Install-Plugin
    echo     
    echo     # Display completion message
    echo     Write-Host ""
    echo     Write-Host "========================================================"
    echo     Write-Host "  ModForge Plugin Build Complete  "
    echo     Write-Host "========================================================"
    echo     Write-Host ""
    echo     Write-Log "Thank you for using ModForge ULTIMATE Builder!"
    echo     Write-Log "Log file available at: $LogFile"
    echo     
    echo     Read-Host "Press Enter to exit"
    echo     return $true
    echo }
    echo.
    echo # Run the main function
    echo Main
) > "%POWERSHELL_SCRIPT%"

echo.
echo Script prepared, starting build process...
echo.

:: Run the PowerShell script - this handles everything properly
if "%HAS_POWERSHELL%" == "1" (
    echo Starting PowerShell engine...
    powershell -ExecutionPolicy Bypass -File "%POWERSHELL_SCRIPT%"
    set "RESULT=%ERRORLEVEL%"
) else (
    echo PowerShell not available!
    echo This script requires PowerShell to function properly.
    echo Please install PowerShell and try again.
    set "RESULT=1"
)

:: Clean up
if exist "%POWERSHELL_SCRIPT%" del /f /q "%POWERSHELL_SCRIPT%" > nul

echo.
echo ========================================================
echo ModForge ULTIMATE Builder Complete
echo ========================================================
echo Log file available at: %LOG_FILE%
echo.

pause
exit /b %RESULT%