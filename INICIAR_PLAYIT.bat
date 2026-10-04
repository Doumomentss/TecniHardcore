@echo off
title Playit.gg - TecniHardcore
color 0B
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0playit\start-playit.ps1"
if errorlevel 1 pause
