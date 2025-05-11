#!/bin/bash

# ===================================
# ModForge Compatibility Scanner
# ===================================
#
# This script proactively scans the entire codebase for IntelliJ API compatibility issues
# with IntelliJ IDEA 2025.1.1.1 (Build #IC-251.25410.129).
#
# Usage: ./scan-compatibility-issues.sh [output_file]
#   - If no output_file is provided, it will use "compatibility-issues.md"

set -e  # Exit on any error

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values
OUTPUT_FILE="compatibility-issues.md"
SOURCE_DIR="src/main/java"
MAX_THREADS=4

# Function to print formatted messages
print_heading() {
    echo -e "${BLUE}==== $1 ====${NC}"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

# Check if output file is provided
if [ $# -ge 1 ]; then
    OUTPUT_FILE="$1"
    print_success "Will write output to: $OUTPUT_FILE"
fi

# Check if the source directory exists
if [ ! -d "$SOURCE_DIR" ]; then
    print_error "Source directory does not exist: $SOURCE_DIR"
    exit 1
fi

print_heading "Scanning codebase for compatibility issues"

# Create output file with header
cat << EOF > "$OUTPUT_FILE"
# ModForge IntelliJ IDEA 2025.1.1.1 Compatibility Issues

This report contains potential compatibility issues found in the codebase.
Generated on $(date).

## Overview

This scan looks for deprecated APIs, problematic patterns, and compatibility issues
that might affect the plugin's functionality with IntelliJ IDEA 2025.1.1.1.

EOF

# Known deprecated APIs and their replacements
cat << EOF >> "$OUTPUT_FILE"
## Known Deprecated APIs and Replacements

| Deprecated API | Replacement | Compatibility Note |
|----------------|-------------|-------------------|
| \`Project.getBaseDir()\` | \`CompatibilityUtil.getProjectBaseDir(project)\` | Removed in 2020.3+ |
| \`CacheUpdater\` | \`CompatibilityUtil.refreshAll(project)\` | Removed in 2020.1+ |
| \`CacheUpdaterFacade\` | \`CompatibilityUtil.refreshAll(project)\` | Removed in 2020.1+ |
| \`ApplicationManager.getApplication().runReadAction()\` | \`CompatibilityUtil.runReadAction()\` | Better API in 2020.3+ |
| \`ApplicationManager.getApplication().runWriteAction()\` | \`CompatibilityUtil.runWriteAction()\` | Better API in 2020.3+ |
| \`PsiElementFactory.getInstance(project).createXmlTag()\` | \`XmlElementFactory.getInstance(project).createTagFromText()\` | Changed in 2020.2+ |
| \`FileEditorManager.getSelectedTextEditor()\` | \`CompatibilityUtil.getSelectedTextEditor(project)\` | Better null handling |
| \`VirtualFileManager.getInstance().getFileSystem()\` | \`VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL)\` | Changed in 2020.1+ |
| \`ActionPlaces.*\` constants | \`com.intellij.openapi.actionSystem.ActionPlaces\` | Relocated in 2020.3+ |

EOF

# Function to find all Java files
find_java_files() {
    find "$SOURCE_DIR" -name "*.java" -type f
}

# List of patterns to search for
declare -a API_PATTERNS=(
    "getBaseDir"
    "CacheUpdater"
    "CacheUpdaterFacade"
    "ApplicationManager.getApplication().runReadAction"
    "ApplicationManager.getApplication().runWriteAction"
    "createXmlTag"
    "getSelectedTextEditor"
    "VirtualFileManager.getInstance().getFileSystem"
    "ActionPlaces"
    "PlatformDataKeys"
    "DataConstants"
    "DataKey.create"
    "TransactionGuard"
    "ProjectLevelVcsManager"
    "VcsException"
    "VcsRoot"
    "ShowSettingsUtil"
    "CreateElementActionBase"
    "RefactoringActionHandler"
    "BasicEditorPopupActionGroup"
    "TreeUtil"
    "IdeEventQueue"
    "FileTypeManagerEx"
    "Messages.showMessageDialog"
    "Messages.showErrorDialog"
    "Messages.showInfoMessage"
    "FileChooserFactory"
    "ProjectView.getInstance"
    "StandardDartboardPlacement"
    "createNotification"
    "NotificationGroup"
    ".addAction(new Action"
    "executeAndGetResult"
    "executeOnPooledThread"
    "JBPopupFactory"
    "JBList"
    "JBTable"
    "ModalityState"
    "ProgressIndicator"
    "ProgressManager"
    "ReadonlyStatusHandler"
    "ShowFilePathAction"
    "StatusBar"
    "ToolWindowAnchor"
    "ToolWindowManager"
    "ProjectManager.getInstance().getOpenProjects"
    "import com.intellij.util.ui.UIUtil"
    "import com.intellij.openapi.ui.DialogWrapper"
    "extends AnAction"
    "extends LightweightAction"
    "createFromText"
    "resolveScope"
    "PsiManager.getInstance"
    "@Override public void update"
    "public void update(AnAction"
    "@Override public void actionPerformed"
    "com.intellij.ide.actions.OpenFileAction"
    "com.intellij.openapi.editor.ex"
    "com.intellij.openapi.vfs.LocalFileSystem"
    "com.intellij.openapi.vcs"
    "com.intellij.openapi.roots.ProjectRootManager"
    "getInstanceMethod"
    "getContentRoots"
    "ProjectSettingsService"
    "getProjectFilePath"
    "getBasePath"
    "isReadAccessAllowed"
    "isDispatchThread"
    "getComponents("
    "getComponent("
    "registerComponent"
    "ProjectComponent"
    "ApplicationComponent"
    "ModuleComponent"
    "BaseComponent"
    "getProjectDir"
    "registerProjectComponent"
    "registerApplicationComponent"
    "getProjectIdeDependencies"
    "ShowUsagesAction"
    "VcsConfiguration"
    "com.intellij.codeInsight.generation"
    "com.intellij.codeInsight.intention"
    "com.intellij.codeInsight.completion"
    "public void registerExternalResourceProvider"
    "SchemaProvider"
    "extends RenamePsiElementProcessor"
    "extends RenamePsiElementProcessorBase"
    "ProjectTaskManager"
    "EditorColors"
)

# Temporary files
MATCHES_FILE=$(mktemp)
SUMMARY_FILE=$(mktemp)

# Track counts
TOTAL_FILES=0
TOTAL_ISSUES=0
POTENTIALLY_AFFECTED_FILES=0

# Initialize summary
echo "0" > "$SUMMARY_FILE"

# Find all matches in parallel
print_heading "Searching for potentially problematic API usage"

find_java_files | while read -r file; do
    TOTAL_FILES=$((TOTAL_FILES + 1))
    
    # For each pattern, check if the file contains it
    for pattern in "${API_PATTERNS[@]}"; do
        if grep -q "$pattern" "$file"; then
            # File contains at least one pattern
            echo "$file:$pattern" >> "$MATCHES_FILE"
            echo "$TOTAL_ISSUES + 1" > "$SUMMARY_FILE"
            
            if ! grep -q "$file" "$OUTPUT_FILE"; then
                # First issue for this file
                echo "$POTENTIALLY_AFFECTED_FILES + 1" > "$SUMMARY_FILE"
            fi
            
            TOTAL_ISSUES=$((TOTAL_ISSUES + 1))
            break
        fi
    done
    
    # Show progress every 50 files
    if [ $((TOTAL_FILES % 50)) -eq 0 ]; then
        echo -ne "Processed $TOTAL_FILES files...\r"
    fi
done

# Update counts with values from temp files
TOTAL_ISSUES=$(cat "$SUMMARY_FILE")
POTENTIALLY_AFFECTED_FILES=$(cat "$SUMMARY_FILE" | head -1)

echo -ne "Processed $TOTAL_FILES files. Found $TOTAL_ISSUES potential issues in $POTENTIALLY_AFFECTED_FILES files.\r"
echo ""

print_heading "Analyzing issues and generating report"

# Add summary to output file
cat << EOF >> "$OUTPUT_FILE"
## Summary

* Total Java files scanned: $TOTAL_FILES
* Files with potential compatibility issues: $POTENTIALLY_AFFECTED_FILES
* Total potential issues found: $TOTAL_ISSUES

## Detailed Analysis by Category

EOF

# Process and categorize the issues
declare -A CATEGORY_COUNTS
declare -A CATEGORY_FILES
declare -A FILE_ISSUES

# Categories
CATEGORIES=(
    "Project Structure API"
    "UI and Components API"
    "Threading and Progress API"
    "File System API"
    "PSI and Language API"
    "Actions and AnAction API"
    "VCS API"
    "Other API"
)

# Initialize categories
for category in "${CATEGORIES[@]}"; do
    CATEGORY_COUNTS["$category"]=0
    CATEGORY_FILES["$category"]=""
done

# Group patterns by category
categorize_pattern() {
    local pattern="$1"
    
    case "$pattern" in
        *"getBaseDir"*|*"getContentRoots"*|*"ProjectRootManager"*|*"getProjectDir"*|*"getBasePath"*)
            echo "Project Structure API"
            ;;
        *"ApplicationManager"*|*"runReadAction"*|*"runWriteAction"*|*"isReadAccessAllowed"*|*"isDispatchThread"*|*"TransactionGuard"*|*"ProgressIndicator"*|*"ProgressManager"*)
            echo "Threading and Progress API"
            ;;
        *"VirtualFile"*|*"VirtualFileManager"*|*"LocalFileSystem"*|*"FileSystem"*)
            echo "File System API"
            ;;
        *"PsiManager"*|*"createFromText"*|*"resolveScope"*|*"SchemaProvider"*)
            echo "PSI and Language API"
            ;;
        *"AnAction"*|*"actionPerformed"*|*"update("*|*"OpenFileAction"*|*"ShowUsagesAction"*)
            echo "Actions and AnAction API"
            ;;
        *"Messages"*|*"DialogWrapper"*|*"JBPopup"*|*"UIUtil"*|*"JBList"*|*"JBTable"*|*"StatusBar"*|*"ToolWindow"*)
            echo "UI and Components API"
            ;;
        *"Vcs"*|*"ProjectLevelVcsManager"*)
            echo "VCS API"
            ;;
        *)
            echo "Other API"
            ;;
    esac
}

# Process each match and categorize it
while IFS=: read -r file pattern; do
    category=$(categorize_pattern "$pattern")
    
    # Increment category count
    CATEGORY_COUNTS["$category"]=$((CATEGORY_COUNTS["$category"] + 1))
    
    # Add file to category if not already present
    if [[ ! "${CATEGORY_FILES[$category]}" =~ "$file" ]]; then
        CATEGORY_FILES["$category"]="${CATEGORY_FILES[$category]} $file"
    fi
    
    # Store issue for the file
    if [[ -z "${FILE_ISSUES[$file]}" ]]; then
        FILE_ISSUES["$file"]="$pattern"
    else
        FILE_ISSUES["$file"]="${FILE_ISSUES[$file]}, $pattern"
    fi
done < "$MATCHES_FILE"

# Write category statistics
cat << EOF >> "$OUTPUT_FILE"
### Issues by Category

| Category | Issue Count |
|----------|-------------|
EOF

for category in "${CATEGORIES[@]}"; do
    count="${CATEGORY_COUNTS[$category]}"
    if [ "$count" -gt 0 ]; then
        echo "| $category | $count |" >> "$OUTPUT_FILE"
    fi
done

# Write detailed analysis for each category
for category in "${CATEGORIES[@]}"; do
    count="${CATEGORY_COUNTS[$category]}"
    if [ "$count" -gt 0 ]; then
        echo "" >> "$OUTPUT_FILE"
        echo "### $category Issues" >> "$OUTPUT_FILE"
        echo "" >> "$OUTPUT_FILE"
        
        for file in ${CATEGORY_FILES[$category]}; do
            echo "#### $file" >> "$OUTPUT_FILE"
            echo "" >> "$OUTPUT_FILE"
            echo "Potential issues: ${FILE_ISSUES[$file]}" >> "$OUTPUT_FILE"
            echo "" >> "$OUTPUT_FILE"
            
            # Suggest fixes based on patterns
            if [[ "${FILE_ISSUES[$file]}" =~ "getBaseDir" ]]; then
                echo "**Suggested fix:** Replace \`Project.getBaseDir()\` with \`CompatibilityUtil.getProjectBaseDir(project)\`" >> "$OUTPUT_FILE"
            fi
            
            if [[ "${FILE_ISSUES[$file]}" =~ "ApplicationManager.getApplication().runReadAction" ]]; then
                echo "**Suggested fix:** Replace with \`CompatibilityUtil.runReadAction(() -> { ... })\`" >> "$OUTPUT_FILE"
            fi
            
            if [[ "${FILE_ISSUES[$file]}" =~ "ApplicationManager.getApplication().runWriteAction" ]]; then
                echo "**Suggested fix:** Replace with \`CompatibilityUtil.runWriteAction(() -> { ... })\`" >> "$OUTPUT_FILE"
            fi
            
            if [[ "${FILE_ISSUES[$file]}" =~ "CacheUpdater" ]] || [[ "${FILE_ISSUES[$file]}" =~ "CacheUpdaterFacade" ]]; then
                echo "**Suggested fix:** Replace with \`CompatibilityUtil.refreshAll(project)\`" >> "$OUTPUT_FILE"
            fi
            
            if [[ "${FILE_ISSUES[$file]}" =~ "getSelectedTextEditor" ]]; then
                echo "**Suggested fix:** Replace with \`CompatibilityUtil.getSelectedTextEditor(project)\`" >> "$OUTPUT_FILE"
            fi
            
            if [[ "${FILE_ISSUES[$file]}" =~ "createXmlTag" ]]; then
                echo "**Suggested fix:** Replace with \`XmlElementFactory.getInstance(project).createTagFromText('<tag></tag>')\`" >> "$OUTPUT_FILE"
            fi
            
            if [[ "${FILE_ISSUES[$file]}" =~ "update(" ]]; then
                echo "**Suggested fix:** Replace \`update(AnActionEvent)\` with \`@Override public void update(@NotNull AnActionEvent e) {\` or better yet, use \`getActionUpdateThread\` and \`updateButton\` methods" >> "$OUTPUT_FILE"
            fi
            
            echo "" >> "$OUTPUT_FILE"
        done
    fi
done

# Add compatibility guide
cat << EOF >> "$OUTPUT_FILE"
## Compatibility Guide

### Key IntelliJ IDEA 2025.1.1.1 Compatibility Requirements:

1. **Replace deprecated Project methods**
   - Use \`CompatibilityUtil.getProjectBaseDir(project)\` instead of \`project.getBaseDir()\`
   - Use \`ProjectRootManager.getInstance(project).getContentRoots()\` for content roots

2. **Update Actions**
   - Override \`getActionUpdateThread()\` in AnAction implementations
   - Make sure action updates happen on correct threads

3. **Update Threading**
   - Use \`CompatibilityUtil.runReadAction()\` instead of direct ApplicationManager calls
   - Use \`CompatibilityUtil.runWriteAction()\` for write operations
   - Use virtual threads with \`ThreadUtils.createVirtualThreadExecutor()\` for better performance

4. **Update Service Management**
   - Use \`@Service\` annotation instead of component registration
   - Register services in plugin.xml with appropriate level (application/project)
   - Remove usage of ProjectComponent, ApplicationComponent, etc.

5. **Plugin Configuration**
   - Use full element tags in plugin.xml (no shorthand)
   - Specify proper since/until build numbers
   - Use only necessary plugin dependencies

6. **UI Updates**
   - Update to newer notification API
   - Use new dialog API methods

7. **File System Access**
   - Use VfsUtil methods consistently
   - Handle invalid files properly

8. **PSI Operations**
   - Always run in read actions
   - Handle null results properly
EOF

# Clean up
rm "$MATCHES_FILE" "$SUMMARY_FILE"

print_success "Compatibility analysis complete! Report written to: $OUTPUT_FILE"
echo ""
echo "Next steps:"
echo "  1. Review the detailed compatibility report"
echo "  2. Prioritize fixing 'Project Structure API' and 'Threading and Progress API' issues first"
echo "  3. Use CompatibilityUtil for consistent API access across IntelliJ versions"
echo "  4. Run this scan again after making changes to verify improvements"