@echo off
title File Transfer Server
color 0A

echo ========================================
echo     File Transfer Server Launcher
echo ========================================
echo.

:: Set your specific paths
set PROJECT_PATH=C:\Users\Vasu\IntelliJ IDEA\File-Transfer-Server
set STORAGE_PATH=C:\Users\Vasu\Downloads\Telegram Desktop
set SERVER_PORT=9090

:: Navigate to project directory
echo [INFO] Navigating to project directory...
cd /d "%PROJECT_PATH%"

if errorlevel 1 (
    echo [ERROR] Cannot find project at: %PROJECT_PATH%
    echo Please check if the path is correct.
    pause
    exit /b 1
)

echo [INFO] Project found at: %PROJECT_PATH%
echo.

:: Check if Java is installed
java -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java is not installed or not in PATH!
    echo Please install Java 17 or higher.
    pause
    exit /b 1
)

:: Display Java version
echo [INFO] Java version:
java -version 2>&1 | findstr /i "version"
echo.

:: Check storage directory
if not exist "%STORAGE_PATH%" (
    echo [WARNING] Storage directory not found: %STORAGE_PATH%
    echo Creating directory...
    mkdir "%STORAGE_PATH%" 2>nul
    if errorlevel 1 (
        echo [ERROR] Cannot create storage directory!
        pause
        exit /b 1
    )
) else (
    echo [INFO] Storage directory found: %STORAGE_PATH%
)

:: Display configuration
echo.
echo ========================================
echo     Server Configuration
echo ========================================
echo   Project Path: %PROJECT_PATH%
echo   Storage Path: %STORAGE_PATH%
echo   Server Port:  %SERVER_PORT%
echo   Max File Size: 1GB
echo   Allowed Extensions: All files (*)
echo ========================================
echo.

:: Set environment variables for the server
set FILE_STORAGE_PATH=%STORAGE_PATH%
set SERVER_PORT=%SERVER_PORT%
set FILE_MAX_SIZE=1073741824
set ALLOWED_EXTENSIONS=*

:: Check if gradlew.bat exists
if not exist "gradlew.bat" (
    echo [ERROR] gradlew.bat not found in project directory!
    echo Please make sure you're in the correct project folder.
    pause
    exit /b 1
)

echo [INFO] Starting File Transfer Server...
echo.
echo   Access the web interface at:
echo   http://localhost:%SERVER_PORT%
echo   http://127.0.0.1:%SERVER_PORT%
echo.
echo   To find your computer's IP for other devices:
echo   Run 'ipconfig' in another command prompt
echo   Look for "IPv4 Address" under your Wi-Fi adapter
echo.
echo   Other devices can access at:
echo   http://[YOUR-IP]:%SERVER_PORT%
echo.
echo   Press Ctrl+C to stop the server
echo.
echo ========================================
echo.

:: Run the Spring Boot application
gradlew bootRun

:: If gradlew fails, try using gradle directly
if errorlevel 1 (
    echo.
    echo [ERROR] Failed to start with gradlew.bat
    echo Trying with gradle command...
    gradle bootRun
)

pause