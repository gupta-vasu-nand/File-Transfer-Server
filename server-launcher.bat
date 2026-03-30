@echo off
title File Transfer Server
color 0A

:: ============================================
:: Server Launcher - Windows Compatible
:: ============================================

:: Get the directory where this batch file is located
set PROJECT_DIR=%~dp0
cd /d "%PROJECT_DIR%"

:: Configuration
set STORAGE_PATH=C:\Users\Vasu\Downloads\Telegram Desktop
set SERVER_PORT=9090
set LOG_DIR=logs

:: Create timestamp for log file
for /f "tokens=1-5 delims=/: " %%a in ("%date% %time%") do (
    set YEAR=%%a
    set MONTH=%%b
    set DAY=%%c
    set HOUR=%%d
    set MINUTE=%%e
)
set LOG_FILE=server_%YEAR%%MONTH%%DAY%_%HOUR%%MINUTE%.log
set LOG_FILE=%LOG_FILE: =0%
set LOG_FILE=%LOG_FILE::=%

:: Create logs directory if it doesn't exist
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"

:: Set environment variables
set FILE_STORAGE_PATH=%STORAGE_PATH%
set SERVER_PORT=%SERVER_PORT%
set FILE_MAX_SIZE=1073741824
set ALLOWED_EXTENSIONS=*
set JAVA_OPTS=-Xmx512m -Xms256m

:: Display header
echo.
echo ============================================
echo     File Transfer Server
echo ============================================
echo.
echo Project: %PROJECT_DIR%
echo Storage: %STORAGE_PATH%
echo Port: %SERVER_PORT%
echo Log: %LOG_DIR%\%LOG_FILE%
echo.
echo Access URLs:
echo   File Browser: http://localhost:%SERVER_PORT%/
echo   Media Player: http://localhost:%SERVER_PORT%/player.html
echo   API: http://localhost:%SERVER_PORT%/api/files
echo.
echo Logs are being written to: %LOG_DIR%\%LOG_FILE%
echo.
echo Press Ctrl+C to stop the server
echo ============================================
echo.

:: Start the server with logging (Windows compatible)
echo [%date% %time%] Server starting... >> "%LOG_DIR%\%LOG_FILE%"
echo [%date% %time%] Storage path: %STORAGE_PATH% >> "%LOG_DIR%\%LOG_FILE%"
echo [%date% %time%] Server port: %SERVER_PORT% >> "%LOG_DIR%\%LOG_FILE%"
echo. >> "%LOG_DIR%\%LOG_FILE%"

:: Run the server and capture output to both console and file
:: Using findstr to handle both stdout and stderr
gradlew.bat bootRun 2>&1 | findstr /R ".*" >> "%LOG_DIR%\%LOG_FILE%"

:: Alternative method if the above doesn't work well:
:: gradlew.bat bootRun > "%LOG_DIR%\%LOG_FILE%" 2>&1
:: type "%LOG_DIR%\%LOG_FILE%"

:: If we get here, server stopped
echo.
echo [%date% %time%] Server stopped >> "%LOG_DIR%\%LOG_FILE%"
echo.
echo Server stopped at %date% %time%
echo Log saved to: %LOG_DIR%\%LOG_FILE%
pause