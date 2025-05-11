@echo off
:: ModForge IntelliJ Plugin Builder Launcher
:: This script automatically detects whether to use the batch or PowerShell version

echo ===== ModForge IntelliJ Plugin Builder Launcher =====
echo.

:: Check if running in PowerShell
set IS_POWERSHELL=0
powershell -Command "exit" 2>nul
if %errorlevel% equ 0 (
    set IS_POWERSHELL=1
)

:: Check if running in Windows Terminal
set IS_TERMINAL=0
if defined WT_SESSION (
    set IS_TERMINAL=1
)

if %IS_POWERSHELL% equ 1 (
    echo Detected PowerShell environment.
    echo Launching PowerShell builder script...
    echo.
    powershell -ExecutionPolicy Bypass -File "%~dp0modforge-builder.ps1"
) else (
    echo Launching batch builder script...
    echo.
    call "%~dp0modforge-builder.bat"
)

echo.
echo ===== Builder completed =====
pause