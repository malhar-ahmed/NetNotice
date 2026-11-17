@echo off
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0Launch-NetNotice.ps1" -Mode client
if errorlevel 1 pause
