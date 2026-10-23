@echo off
rem Run this AFTER Maven "package" has created target\game-backlog-1.0.jar
if not exist target\game-backlog-1.0.jar ( echo Run Maven "package" first. & pause & exit /b 1 )
if exist build rmdir /s /q build
if exist dist rmdir /s /q dist
mkdir build
copy target\game-backlog-1.0.jar build\ >nul
jpackage --type app-image --name Playora --input build --main-jar game-backlog-1.0.jar --main-class gamebacklog.Launcher --dest dist --add-modules java.base,java.sql,java.net.http,java.desktop,java.logging,java.xml,java.naming,jdk.unsupported
echo.
echo Done. Your app is in dist\Playora  (run dist\Playora\Playora.exe)
pause
