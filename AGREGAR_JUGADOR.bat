@echo off
setlocal
py -3 "%~dp0tools\whitelist29.py" preguntar
if errorlevel 1 pause
echo El servidor 2.9 recargara la lista automaticamente en unos segundos.
pause
