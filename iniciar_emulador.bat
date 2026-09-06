@echo off
title ValleGO - Emulador Android
echo =====================================================
echo       Iniciando Emulador de ValleGO en tu pantalla
echo =====================================================
echo.

REM 1. Limpieza preventiva de procesos y bloqueos antiguos
taskkill /F /IM qemu-system-x86_64.exe >nul 2>&1
taskkill /F /IM emulator.exe >nul 2>&1
timeout /t 1 >nul

if exist "%USERPROFILE%\.android\avd\medium_phone.avd\hardware-qemu.ini.lock" (
    rmdir /s /q "%USERPROFILE%\.android\avd\medium_phone.avd\hardware-qemu.ini.lock" >nul 2>&1
)
if exist "%USERPROFILE%\.android\avd\medium_phone.avd\multiinstance.lock" (
    del /f /q "%USERPROFILE%\.android\avd\medium_phone.avd\multiinstance.lock" >nul 2>&1
)

echo Iniciando el emulador Android...
echo La ventana del telefono aparecera en pantalla en breves segundos.
echo No cierres esta consola hasta que termines de usar el emulador.
echo.

"%LOCALAPPDATA%\Android\Sdk\emulator\emulator.exe" -avd medium_phone -gpu auto
if errorlevel 1 (
    echo.
    echo Reintentando con acelerador de compatibilidad grafica...
    "%LOCALAPPDATA%\Android\Sdk\emulator\emulator.exe" -avd medium_phone -gpu swiftshader_indirect
)

echo.
echo El emulador ha finalizado.
pause
