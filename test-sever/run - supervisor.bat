@echo off
supervisor app.js
if errorlevel == 1 (
    pause
)