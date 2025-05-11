#!/bin/bash

# ===================================
# ModForge Error Extractor
# ===================================
#
# This script extracts compilation errors from build logs,
# organizes them by source file, and generates a summary report.
#
# Usage: ./extract-errors.sh [build_log_file] [output_file]
#   - If no build_log_file is provided, it will attempt to build the plugin and capture the output
#   - If no output_file is provided, it will use "compilation-errors.md"

set -e  # Exit on any error

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values
OUTPUT_FILE="compilation-errors.md"
TEMP_LOG_FILE="temp-build-log.txt"
SHOULD_BUILD=true

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

# Check if input file is provided
if [ $# -ge 1 ]; then
    if [ -f "$1" ]; then
        BUILD_LOG_FILE="$1"
        SHOULD_BUILD=false
        print_success "Using existing build log file: $BUILD_LOG_FILE"
    else
        print_error "Input file does not exist: $1"
        exit 1
    fi
fi

# Check if output file is provided
if [ $# -ge 2 ]; then
    OUTPUT_FILE="$2"
    print_success "Will write output to: $OUTPUT_FILE"
fi

# Run build if required
if [ "$SHOULD_BUILD" = true ]; then
    print_heading "Building plugin to capture errors"
    BUILD_LOG_FILE="$TEMP_LOG_FILE"
    
    # Fix plugin.xml first
    if [ -f "src/main/resources/META-INF/fixed-plugin.xml" ]; then
        cp "src/main/resources/META-INF/fixed-plugin.xml" "src/main/resources/META-INF/plugin.xml"
        print_success "Copied fixed plugin.xml"
    fi
    
    # Run build and capture output
    ./gradlew clean build > "$BUILD_LOG_FILE" 2>&1 || true
    
    print_success "Build completed (with errors) and log saved to $BUILD_LOG_FILE"
fi

# Define error patterns to search for
ERROR_PATTERNS=(
    "error: cannot find symbol"
    "error: method .* in .* cannot be applied to given types"
    "error: incompatible types"
    "error: .* is deprecated"
    "error: .* is not public in .*. Cannot be accessed from outside package"
    "error: cannot access"
    "error: package .* does not exist"
    "error: no suitable method found for"
    "cannot resolve symbol"
    "Cannot resolve method"
    "\[ERROR\].*"
)

print_heading "Extracting errors from build log"

# Create output file with header
cat << EOF > "$OUTPUT_FILE"
# ModForge Compilation Errors Report

This report contains all compilation errors extracted from the build log.
Generated on $(date).

## Summary of Errors

EOF

# Extract and organize errors
ERROR_COUNT=0
FILE_COUNT=0
UNIQUE_FILES=()
declare -A ERROR_TYPES
declare -A FILE_ERRORS

# First pass: Collect all error lines and categorize them
print_heading "Analyzing error patterns"
while IFS= read -r line; do
    for pattern in "${ERROR_PATTERNS[@]}"; do
        if echo "$line" | grep -E "$pattern" > /dev/null; then
            # Extract file name if possible
            FILE_NAME=$(echo "$line" | grep -oE '([A-Za-z0-9_/]+\.java)' | head -1)
            
            if [ -n "$FILE_NAME" ]; then
                # Add to unique files if not seen before
                if [[ ! " ${UNIQUE_FILES[@]} " =~ " ${FILE_NAME} " ]]; then
                    UNIQUE_FILES+=("$FILE_NAME")
                fi
                
                # Store error by file
                FILE_ERRORS["$FILE_NAME"]+="$line"$'\n'
            fi
            
            # Categorize error type
            ERROR_TYPE=$(echo "$line" | grep -oE "(cannot find symbol|incompatible types|is deprecated|cannot access|package .* does not exist|no suitable method found|cannot resolve)" | head -1)
            if [ -n "$ERROR_TYPE" ]; then
                if [ -n "${ERROR_TYPES[$ERROR_TYPE]}" ]; then
                    ERROR_TYPES["$ERROR_TYPE"]=$((ERROR_TYPES["$ERROR_TYPE"] + 1))
                else
                    ERROR_TYPES["$ERROR_TYPE"]=1
                fi
            fi
            
            ERROR_COUNT=$((ERROR_COUNT + 1))
            break
        fi
    done
done < "$BUILD_LOG_FILE"

# Write error summary to output file
echo "Found $ERROR_COUNT errors across ${#UNIQUE_FILES[@]} files." >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"
echo "### Error Types" >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"

for type in "${!ERROR_TYPES[@]}"; do
    echo "- **$type**: ${ERROR_TYPES[$type]} occurrences" >> "$OUTPUT_FILE"
done

echo "" >> "$OUTPUT_FILE"
echo "### Affected Files" >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"

for file in "${UNIQUE_FILES[@]}"; do
    echo "- $file" >> "$OUTPUT_FILE"
done

echo "" >> "$OUTPUT_FILE"
echo "## Detailed Errors by File" >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"

# Write detailed errors by file
for file in "${UNIQUE_FILES[@]}"; do
    echo "### $file" >> "$OUTPUT_FILE"
    echo "" >> "$OUTPUT_FILE"
    echo '```' >> "$OUTPUT_FILE"
    echo "${FILE_ERRORS[$file]}" >> "$OUTPUT_FILE"
    echo '```' >> "$OUTPUT_FILE"
    echo "" >> "$OUTPUT_FILE"
done

# Add common fixes section
cat << EOF >> "$OUTPUT_FILE"
## Common Fixes for IntelliJ IDEA 2025.1.1.1 Compatibility

### Symbol Resolution Issues
- Replace \`Project.getBaseDir()\` with \`CompatibilityUtil.getProjectBaseDir(project)\`
- Replace \`PsiManager.getInstance(project).findFile()\` with \`CompatibilityUtil.findPsiFile(project, file)\`
- Replace \`FileEditorManager.getSelectedTextEditor()\` with \`CompatibilityUtil.getSelectedTextEditor(project)\`

### Deprecated API Solutions
- Replace \`CacheUpdater\` and \`CacheUpdaterFacade\` with \`CompatibilityUtil.refreshAll(project)\`
- Replace direct \`ApplicationManager\` access with \`CompatibilityUtil\` methods
- Update thread management code to use \`ThreadUtils.createVirtualThreadExecutor()\`

### Missing Package Issues
- Ensure all necessary dependencies are declared in \`build.gradle\`
- Check for package relocations in IntelliJ Platform API

### Method Signature Changes
- Update method parameters to match new API signatures
- Use \`@NotNull\` and \`@Nullable\` annotations consistently
EOF

print_success "Error extraction complete! See $OUTPUT_FILE for results."
echo ""
echo "Next steps:"
echo "  1. Review the error report"
echo "  2. Fix the most common error patterns first"
echo "  3. Rerun the build to check progress"
echo ""
echo "You can use this file to systematically fix all compilation errors."