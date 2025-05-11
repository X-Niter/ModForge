# ModForge IntelliJ IDEA 2025.1.1.1 Compatibility Guide

## Overview

This guide provides comprehensive instructions for ensuring compatibility with IntelliJ IDEA 2025.1.1.1 (Build #IC-251.25410.129).

## Key Compatibility Issues

### Missing Method Errors

Many "cannot find symbol" errors occur because methods are missing from service classes or have changed signatures:

```java
// Error: cannot find symbol - method isPatternRecognition()
boolean usePatterns = settings.isPatternRecognition();

// Error: cannot find symbol - method getAccessToken()
String token = settings.getAccessToken();
```

**Solution:** Ensure all service classes have the required methods with correct signatures.

### Service Implementation Issues

Service classes are missing proper getInstance() methods or other key methods:

```java
// Error: cannot find symbol - method getInstance(Project)
AutonomousCodeGenerationService codeGenService = AutonomousCodeGenerationService.getInstance(project);

// Error: cannot find symbol - method generateDocumentation(String,<null>)
String documentedCode = codeGenService.generateDocumentation(code, null).get();
```

**Solution:** 
1. Implement the missing methods
2. Ensure getInstance() methods are properly defined
3. Check method signatures across all service classes

### API Changes

APIs like Messages and notifications have changed:

```java
// Error: cannot find symbol - method showInfoDialog(Project,String,String)
Messages.showInfoDialog(project, message, title);

// Error: cannot find symbol - method showInfo(String,String)
notificationService.showInfo("Title", "Message");
```

**Solution:** Use `CompatibilityUtil` to provide wrapper methods for these APIs.

### Parameter Type Mismatches

Parameter types in method calls don't match expected types:

```java
// Error: incompatible types: VirtualFile cannot be converted to String
String code = codeGenService.generateCode(prompt, file, language);
```

**Solution:** Update method signatures or use proper type conversion.

### DialogWrapper Issues

DialogWrapper override conflicts:

```java
// Error: getOwner() in PushToGitHubDialog cannot override getOwner() in DialogWrapper
// return type String is not compatible with Window
public String getOwner() {
    return ownerField.getText();
}
```

**Solution:** Rename the method or change the return type to match the parent class.

## Implementation Strategy

1. **Create Service Interfaces**:
   - Define clear interfaces for all services with required methods
   - Ensure implementation classes adhere to interfaces

2. **Update Compatibility Utilities**:
   - Add wrappers for all problematic API calls
   - Create adapter methods for different IntelliJ versions

3. **Fix Method Signatures**:
   - Ensure all parameters match expected types
   - Add proper annotations (@NotNull, @Nullable)
   - Use consistent return types

4. **Standardize Service Access**:
   - Use consistent getInstance() patterns
   - Add proper project context to service methods

## Testing Approach

1. **Incremental Testing**:
   - Fix one category of issues at a time
   - Compile after each set of changes

2. **Verify with Latest IDE**:
   - Test with IntelliJ IDEA 2025.1.1.1 specifically
   - Check for runtime exceptions even after compilation succeeds

3. **Validate Plugin**:
   - Use `verifyPlugin` Gradle task
   - Check for any warnings in verification output

## Recommended Utility Classes

1. **CompatibilityUtil.java**:
   - Project operations (getProjectBaseDir, etc.)
   - Read/write actions
   - Dialog wrappers

2. **ServiceManager.java**:
   - Centralized service access
   - Standard getInstance() implementations
   - Service registration

3. **NotificationWrapper.java**:
   - Cross-version notification methods
   - Standard info/error/warning methods
   - Proper notification group handling

## Common Error Patterns to Watch For

1. **Cannot find symbol** - Usually means a method is missing or signature has changed
2. **Incompatible types** - Method parameter or return type mismatch
3. **Cannot override** - Method signature in child class doesn't match parent
4. **Method not found** - API method has been removed or relocated

## IntelliJ IDEA 2025.1.1.1 API Changes

Major API changes in this version include:

1. Notification system overhaul
2. Dialog system refactoring
3. Project structure API changes
4. File system access modifications
5. Service model enhancements

Always check the official IntelliJ Platform SDK documentation for the most recent guidance.