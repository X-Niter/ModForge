# IntelliJ IDEA 2025.1.1.1 Compatibility Checklist

Use this checklist to ensure your code is compatible with IntelliJ IDEA 2025.1.1.1 (Build #IC-251.25410.129).

## API Usage

- [ ] Replaced deprecated `Project.getBaseDir()` with `CompatibilityUtil.getProjectBaseDir(project)`
- [ ] Replaced `CacheUpdater` and `CacheUpdaterFacade` references with `CompatibilityUtil.refreshAll(project)`
- [ ] Replaced direct `ApplicationManager` access with `CompatibilityUtil` methods
- [ ] Replaced direct `PsiManager` file access with `CompatibilityUtil.findPsiFile()`
- [ ] Replaced direct `FileEditorManager` access with `CompatibilityUtil` methods
- [ ] Updated editor manipulation to use `CompatibilityUtil` methods
- [ ] Fixed any XML manipulation to use `CompatibilityUtil.createXmlFile()`
- [ ] Updated VirtualFile handling to use `VirtualFileUtil` methods

## Service Registration

- [ ] Updated service registration to use the `@Service` annotation
- [ ] Registered services in plugin.xml with appropriate level (project/application)
- [ ] Using constructor-based dependency injection
- [ ] Services properly dispose resources in `dispose()` method if needed

## Thread Management

- [ ] Using `ThreadUtils.createVirtualThreadExecutor()` for network operations
- [ ] All long-running operations are run in background threads
- [ ] UI updates are executed on the UI thread via `ApplicationManager.getApplication().invokeLater()`
- [ ] Read/write actions are properly wrapped

## Plugin Configuration

- [ ] Fixed plugin.xml `<id>` tags to use full element syntax (not shorthand)
- [ ] Specified appropriate `sinceBuild` and `untilBuild` version constraints
- [ ] Verified plugin works with both IntelliJ IDEA 2023.3.6 and 2025.1.1.1
- [ ] Validated plugin structure with `validatePluginForProduction` task

## Java 21 Compatibility

- [ ] Configured Java 21 toolchain in Gradle
- [ ] Ensured compatibility with Java 17 (for build)
- [ ] Added required JVM arguments for module access
- [ ] Using Java 21 features where appropriate (virtual threads, records, etc.)

## Testing

- [ ] Tested with IntelliJ IDEA 2023.3.6 (build compatibility)
- [ ] Tested with IntelliJ IDEA 2025.1.1.1 (runtime target)
- [ ] Verified all features work across both versions
- [ ] Validated with multiple operating systems (if possible)
- [ ] Automated tests pass on CI

## Documentation

- [ ] Updated documentation to reflect API changes
- [ ] Added compatibility notes to README
- [ ] Updated changelog with compatibility improvements
- [ ] Added TODO comments for future improvements

## Performance

- [ ] Measured and optimized startup time
- [ ] Used virtual threads for I/O operations
- [ ] Minimized UI freezes
- [ ] Optimized memory usage
- [ ] Implemented proper background task handling

## Security Considerations

- [ ] Properly handle sensitive data (API keys, tokens)
- [ ] Used secure communication for external services
- [ ] Validated user input

## Final Verification

- [ ] Ran plugin in IntelliJ IDEA 2025.1.1.1
- [ ] Verified all major features work correctly
- [ ] Checked for exceptions in the IDE log
- [ ] Ensured good user experience with proper error handling