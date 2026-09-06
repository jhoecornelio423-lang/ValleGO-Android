@echo off
title ValleGO - Instalar en Emulador
echo =====================================================
echo    Instalando ultima version de ValleGO en Emulador
echo =====================================================
echo.
echo Conectando con el emulador...
"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" -e wait-for-device
echo Instalando APK...
"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" -e install -r -d "%~dp0ValleGO-latest.apk"
echo Abriendo aplicacion en el emulador...
"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" -e shell am start -n com.example.vallego/.MainActivity
echo.
echo Instalacion completada con exito.
timeout /t 5
