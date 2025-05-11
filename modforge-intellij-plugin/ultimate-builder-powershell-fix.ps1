# Fixes the PowerShell command in ultimate-builder.bat
# This script will search for the broken PowerShell command and fix it

$builderBatPath = "ultimate-builder.bat"

if (-not (Test-Path $builderBatPath)) {
    Write-Host "Error: Could not find $builderBatPath in the current directory."
    exit 1
}

# Get the content of the file
$content = Get-Content $builderBatPath -Raw

# Define the correct PowerShell commands for code fixes
$correctPowerShellCodeFixes = @'
    :: Fix common import issues
    powershell -Command "& { $ErrorActionPreference = 'SilentlyContinue'; $allFiles = Get-ChildItem -Path 'src\main\java' -Filter '*.java' -Recurse; foreach ($file in $allFiles) { $content = Get-Content $file.FullName -Raw; if ($content -match 'Arrays\.' -and -not ($content -match 'import java\.util\.Arrays')) { $content = $content -replace 'package com\.modforge', 'package com.modforge\r\n\r\nimport java.util.Arrays;'; $content | Set-Content $file.FullName -Encoding UTF8 }; if ($content -match 'CompletableFuture' -and -not ($content -match 'import java\.util\.concurrent\.CompletableFuture')) { $content = $content -replace 'package com\.modforge', 'package com.modforge\r\n\r\nimport java.util.concurrent.CompletableFuture;'; $content | Set-Content $file.FullName -Encoding UTF8 }; if ($content -match 'problem\.getDescription\(\)') { $content = $content -replace 'problem\.getDescription\(\)', 'problem.toString()'; $content | Set-Content $file.FullName -Encoding UTF8 }; if ($content -match 'WebSocketClient' -and -not ($content -match 'import org\.java_websocket\.client\.WebSocketClient')) { $content = $content -replace 'package com\.modforge', 'package com.modforge\r\n\r\nimport org.java_websocket.client.WebSocketClient;\r\nimport org.java_websocket.handshake.ServerHandshake;'; $content | Set-Content $file.FullName -Encoding UTF8 }; if ($content -match 'NotificationType' -and -not ($content -match 'import com\.intellij\.notification\.NotificationType')) { $content = $content -replace 'package com\.modforge', 'package com.modforge\r\n\r\nimport com.intellij.notification.NotificationType;'; $content | Set-Content $file.FullName -Encoding UTF8 }; if ($content -match 'Messages\.showInfoDialog\(') { $content = $content -replace 'Messages\.showInfoDialog\((.*?),(.*?),(.*?)\)', 'Messages.showMessageDialog($1,$2,$3, Messages.getInformationIcon())'; $content | Set-Content $file.FullName -Encoding UTF8 }; } }"
    
    :: Fix PushToGitHubDialog issues
    powershell -Command "& { $dialogFiles = Get-ChildItem -Path 'src\main\java' -Filter 'PushToGitHubDialog.java' -Recurse; foreach ($file in $dialogFiles) { $content = Get-Content $file.FullName -Raw; if ($content -match 'public String getOwner\(\)') { $content = $content -replace 'public String getOwner\(\)', 'public String getRepositoryOwner()'; $content = $content -replace 'getOwner\(\)', 'getRepositoryOwner()'; $content | Set-Content $file.FullName -Encoding UTF8 } } }"
    
    :: Update plugin.xml compatibility
    if exist "src\main\resources\META-INF\plugin.xml" (
        powershell -Command "& { $content = Get-Content 'src\main\resources\META-INF\plugin.xml' -Raw; if ($content -match 'idea-version since-build') { $content = $content -replace '(since-build=.+? until-build=.)([^\"]+)(.)', '$1251.*$3'; $content | Set-Content 'src\main\resources\META-INF\plugin.xml' -Encoding UTF8; } }"
        CALL :LOG "Updated plugin.xml compatibility version"
    )
'@

# Define a pattern to find the build process section
$buildProcessSectionPattern = ":: ===================================\r?\n:: BUILD PROCESS\r?\n:: ===================================\r?\n"

# Find all matches of the pattern
if ($content -match $buildProcessSectionPattern) {
    # Find the section after the build process header
    $sections = $content -split $buildProcessSectionPattern
    if ($sections.Count -ge 2) {
        # Replace the old PowerShell commands with the corrected ones
        $newContent = $sections[0] + $buildProcessSectionPattern + 
                     "CALL :LOG `"Checking for and fixing common code issues...`"\r\n\r\n" +
                     ":: Apply fixes for common issues\r\n" +
                     "if `"%HAS_POWERSHELL%`" == `"1`" (\r\n" +
                     "    CALL :LOG `"Applying automated fixes using PowerShell...`"\r\n" +
                     "    \r\n" +
                     $correctPowerShellCodeFixes +
                     "\r\n) else (\r\n" +
                     "    CALL :LOG `"PowerShell not available for automated fixes. Proceeding without code fixes.`"\r\n" +
                     ")\r\n\r\n" +
                     ":: ===================================\r\n" +
                     ":: BUILD THE PLUGIN\r\n" +
                     ":: ===================================\r\n"
        
        # Find the rest of the content
        $restOfContent = $sections[1]
        $buildPluginSectionIndex = $restOfContent.IndexOf("CALL :LOG `"Building ModForge IntelliJ plugin...`"")
        
        if ($buildPluginSectionIndex -ge 0) {
            $newContent += $restOfContent.Substring($buildPluginSectionIndex)
        } else {
            # Couldn't find the build plugin section, just append everything
            $newContent += $restOfContent
        }
        
        # Save the updated content
        $newContent | Set-Content $builderBatPath -Encoding UTF8
        Write-Host "Fixed PowerShell command in $builderBatPath"
    } else {
        Write-Host "Error: Could not parse file structure correctly."
        exit 1
    }
} else {
    Write-Host "Error: Could not find BUILD PROCESS section in the file."
    exit 1
}