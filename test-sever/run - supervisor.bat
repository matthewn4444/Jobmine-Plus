@echo off
supervisor --extensions "js|html" app.js
if errorlevel == 1 (
    pause
)