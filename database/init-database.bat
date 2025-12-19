@echo off
setlocal EnableDelayedExpansion

echo ====================================
echo Database Initialization Script
echo ====================================
echo.

REM Get script directory
set SCRIPT_DIR=%~dp0
set PROJECT_ROOT=%SCRIPT_DIR%..

REM Check if schema.sql exists
if not exist "%SCRIPT_DIR%schema.sql" (
    echo Error: schema.sql not found in %SCRIPT_DIR%
    echo Please make sure the script is in the database folder.
    pause
    exit /b 1
)

set /p DB_USER="Enter MySQL username (default: root): "
if "%DB_USER%"=="" set DB_USER=root

set /p DB_PASSWORD="Enter MySQL password: "
if "%DB_PASSWORD%"=="" (
    echo Error: Password cannot be empty
    pause
    exit /b 1
)

set /p DB_NAME="Enter database name (default: beverage_platform): "
if "%DB_NAME%"=="" set DB_NAME=beverage_platform

echo.
echo Initializing database...
echo.

REM Create database
echo [1/3] Creating database %DB_NAME%...
mysql -u%DB_USER% -p%DB_PASSWORD% -e "CREATE DATABASE IF NOT EXISTS %DB_NAME% CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>nul
if errorlevel 1 (
    echo Error: Failed to create database. Please check your MySQL credentials.
    pause
    exit /b 1
)
echo Database created successfully.

REM Execute schema script
echo [2/3] Executing schema script...
mysql -u%DB_USER% -p%DB_PASSWORD% %DB_NAME% < "%SCRIPT_DIR%schema.sql" 2>nul
if errorlevel 1 (
    echo Error: Failed to execute schema script.
    echo Please check the SQL file for syntax errors.
    pause
    exit /b 1
)
echo Schema created successfully.

REM Ask about test data
set /p IMPORT_TEST_DATA="Import test data? (y/n, default: n): "
if /i "%IMPORT_TEST_DATA%"=="y" (
    echo [3/3] Importing test data...
    mysql -u%DB_USER% -p%DB_PASSWORD% %DB_NAME% < "%SCRIPT_DIR%test-data.sql" 2>nul
    if errorlevel 1 (
        echo Warning: Failed to import test data, but database structure was created successfully.
    ) else (
        echo Test data imported successfully!
    )
) else (
    echo [3/3] Skipping test data import.
)

echo.
echo ====================================
echo Database Initialization Complete!
echo ====================================
echo.
echo Database name: %DB_NAME%
echo Username: %DB_USER%
echo.
echo Please configure database connection in:
echo backend/src/main/resources/application.properties
echo.
echo spring.datasource.url=jdbc:mysql://localhost:3306/%DB_NAME%
echo spring.datasource.username=%DB_USER%
echo spring.datasource.password=your_password
echo.
pause

