#
# PowerShell script for building ModForge IntelliJ plugin for 2025.1.1.1
# 
# This script provides advanced troubleshooting and dependency resolution
# to build the plugin for IntelliJ IDEA 2025.1.1.1.
#

Write-Host "===== ModForge IntelliJ Plugin Builder for 2025.1.1.1 =====" -ForegroundColor Cyan
Write-Host ""
Write-Host "Target: IntelliJ IDEA 2025.1.1.1 (Build: 251.25410.129)" -ForegroundColor Cyan
Write-Host ""

# Check Java installation
try {
    $javaVersion = (java -version 2>&1)
    Write-Host "Java detected: " -NoNewline
    Write-Host $javaVersion[0] -ForegroundColor Green
}
catch {
    Write-Host "Error: Java not found. Please install JDK 17 or higher." -ForegroundColor Red
    exit 1
}

# Prompt for build type
Write-Host ""
Write-Host "Build Options:" -ForegroundColor Yellow
Write-Host "1. Build using local IntelliJ IDEA 2025.1.1.1 installation (recommended)" -ForegroundColor Yellow
Write-Host "2. Build using JetBrains repository version (2023.3.6)" -ForegroundColor Yellow
Write-Host ""

$buildOption = Read-Host "Select build option (1 or 2)"

$useLocal = $false
$intellijPath = ""

if ($buildOption -eq "1") {
    Write-Host ""
    Write-Host "Selected: Build using local IntelliJ IDEA 2025.1.1.1 installation" -ForegroundColor Cyan
    Write-Host ""

    $defaultPath = "C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2025.1.1.1"
    if (Test-Path $defaultPath) {
        $intellijPath = $defaultPath
        Write-Host "Found IntelliJ IDEA at default location: $intellijPath" -ForegroundColor Green
    }
    else {
        Write-Host "IntelliJ IDEA 2025.1.1.1 not found at default location." -ForegroundColor Yellow
        $intellijPath = Read-Host "Please enter the path to IntelliJ IDEA 2025.1.1.1 installation"
        
        if (!(Test-Path $intellijPath)) {
            Write-Host "ERROR: The specified path does not exist." -ForegroundColor Red
            Write-Host "Falling back to repository version." -ForegroundColor Yellow
            $useLocal = $false
        }
        else {
            $useLocal = $true
        }
    }
}
else {
    Write-Host ""
    Write-Host "Selected: Build using JetBrains repository version (2023.3.6)" -ForegroundColor Cyan
    Write-Host ""
    $useLocal = $false
}

Write-Host "Creating temporary build.gradle for compatibility..." -ForegroundColor Cyan
$tempGradle = "build.gradle.temp"

# Read the original build.gradle
$buildGradleContent = Get-Content -Path "build.gradle" -Raw

if ($useLocal) {
    Write-Host "Using local IntelliJ at: $intellijPath" -ForegroundColor Green
    
    # Replace version with commented version
    $buildGradleContent = $buildGradleContent -replace "version = '2023\.3\.6'", "// version = '2023.3.6' // Commented out to use localPath instead"
    
    # Replace localPath comment with actual localPath
    $escapedPath = $intellijPath -replace '\\', '\\'
    $buildGradleContent = $buildGradleContent -replace "// localPath = .*", "localPath = '$escapedPath' // Using local IntelliJ"
}
else {
    Write-Host "Using IntelliJ from repository." -ForegroundColor Green
}

# Backup original build.gradle
Copy-Item -Path "build.gradle" -Destination "build.gradle.bak"

# Write the modified content to temp file
$buildGradleContent | Set-Content -Path $tempGradle

# Apply temporary build file
Copy-Item -Path $tempGradle -Destination "build.gradle"

Write-Host ""
Write-Host "Building plugin for IntelliJ IDEA 2025.1.1.1..." -ForegroundColor Cyan
Write-Host ""

# Build the plugin
try {
    if ($useLocal) {
        .\gradlew.bat clean buildPlugin --info
    }
    else {
        .\gradlew.bat clean buildPlugin --info
    }
    $buildSuccessful = $?
}
catch {
    $buildSuccessful = $false
    Write-Host "Error during build: $_" -ForegroundColor Red
}

# Restore original build.gradle
Write-Host ""
Write-Host "Restoring original build.gradle..." -ForegroundColor Cyan
Copy-Item -Path "build.gradle.bak" -Destination "build.gradle"
Remove-Item -Path "build.gradle.bak"
Remove-Item -Path $tempGradle

Write-Host ""
if ($buildSuccessful -and (Test-Path "build\distributions\modforge-intellij-plugin-2.1.0.zip")) {
    Write-Host "----------------------------------------" -ForegroundColor Green
    Write-Host "BUILD SUCCESSFUL!" -ForegroundColor Green
    Write-Host "----------------------------------------" -ForegroundColor Green
    Write-Host "Plugin is available at: build\distributions\modforge-intellij-plugin-2.1.0.zip" -ForegroundColor Green
    Write-Host ""
    Write-Host "Installation Instructions:" -ForegroundColor Cyan
    Write-Host "1. Open IntelliJ IDEA 2025.1.1.1" -ForegroundColor Cyan
    Write-Host "2. Go to Settings → Plugins → ⚙ → Install Plugin from Disk..." -ForegroundColor Cyan 
    Write-Host "3. Select the generated ZIP file" -ForegroundColor Cyan
    Write-Host "4. Restart IntelliJ IDEA when prompted" -ForegroundColor Cyan
}
else {
    Write-Host "----------------------------------------" -ForegroundColor Red
    Write-Host "BUILD FAILED!" -ForegroundColor Red
    Write-Host "----------------------------------------" -ForegroundColor Red
    Write-Host "Please check the error messages above." -ForegroundColor Red
    
    Write-Host ""
    Write-Host "Troubleshooting Tips:" -ForegroundColor Yellow
    Write-Host "1. Make sure you have the correct IntelliJ IDEA path" -ForegroundColor Yellow
    Write-Host "2. Check that your Java version is compatible (JDK 17+)" -ForegroundColor Yellow
    Write-Host "3. Try running with --stacktrace flag: .\gradlew buildPlugin --stacktrace" -ForegroundColor Yellow
    Write-Host "4. Check internet connectivity for downloading dependencies" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "===== Build process completed =====" -ForegroundColor Cyan
