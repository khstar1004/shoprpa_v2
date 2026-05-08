@echo off
chcp 65001 >nul

REM Save script directory path IMMEDIATELY
set "SCRIPT_DIR=%~dp0"
if "%SCRIPT_DIR:~-1%"=="\" set "SCRIPT_DIR=%SCRIPT_DIR:~0,-1%"

setlocal enabledelayedexpansion

REM ============================================
REM 1. Argument Parsing
REM ============================================

:parse_args
if "%~1"=="" goto end_parse_args
if /i "%~1"=="--python-exe" (
    set PYTHON_EXE=%~2
    shift
    shift
    goto parse_args
)
if /i "%~1"=="-p" (
    set PYTHON_EXE=%~2
    shift
    shift
    goto parse_args
)
if /i "%~1"=="--sevenz-exe" (
    set SEVENZ_EXE=%~2
    shift
    shift
    goto parse_args
)
if /i "%~1"=="-s" (
    set SEVENZ_EXE=%~2
    shift
    shift
    goto parse_args
)
if /i "%~1"=="--skip-engine" (
    set SKIP_ENGINE=1
    shift
    goto parse_args
)
if /i "%~1"=="--skip-frontend" (
    set SKIP_FRONTEND=1
    shift
    goto parse_args
)
if /i "%~1"=="--help" goto show_help
if /i "%~1"=="-h" goto show_help
echo Unknown parameter: %~1
goto show_help

:show_help
echo.
echo Usage: build.bat [options]
echo.
echo Options:
echo   --python-exe, -p ^<path^>   Specify Python executable path
echo   --sevenz-exe, -s ^<path^>   Specify 7-Zip executable path
echo   --skip-engine              Skip engine (Python) build
echo   --skip-frontend            Skip frontend build
echo   --help, -h                 Display this help message
echo.
echo Examples:
echo   build.bat --python-exe "C:\Python313\python.exe"
echo   build.bat -p "C:\Python313\python.exe" -s "C:\7-Zip\7z.exe"
echo   build.bat --skip-frontend
echo   build.bat --skip-engine
echo.
exit /b 1

:end_parse_args

REM ============================================
REM 2. Configuration
REM ============================================

if "%PYTHON_EXE%"=="" set PYTHON_EXE=C:\Program Files\Python313\python.exe
if "%SEVENZ_EXE%"=="" set SEVENZ_EXE=C:\Program Files\7-Zip\7z.exe
set ENGINE_DIR=engine
set BUILD_DIR=build
set PYTHON_CORE_DIR=%BUILD_DIR%\python_core
set DIST_DIR=%BUILD_DIR%\dist
set ARCHIVE_DIST_DIR=resources
set PORTABLE_DIST_DIR=frontend\packages\electron-app\dist\win-portable
set PORTABLE_RESOURCES_DIR=%PORTABLE_DIST_DIR%\resources

REM Auto-detect common local tool locations before failing the build.
if not exist "%PYTHON_EXE%" if exist "%LOCALAPPDATA%\Programs\Python\Python313\python.exe" set "PYTHON_EXE=%LOCALAPPDATA%\Programs\Python\Python313\python.exe"
if not exist "%PYTHON_EXE%" if exist "%ProgramFiles%\Python313\python.exe" set "PYTHON_EXE=%ProgramFiles%\Python313\python.exe"
if not exist "%PYTHON_EXE%" if exist "%ProgramFiles(x86)%\Python313\python.exe" set "PYTHON_EXE=%ProgramFiles(x86)%\Python313\python.exe"
if not exist "%SEVENZ_EXE%" if exist "%SCRIPT_DIR%\resources\7zr.exe" set "SEVENZ_EXE=%SCRIPT_DIR%\resources\7zr.exe"
if not exist "%SEVENZ_EXE%" if exist "%ProgramFiles%\7-Zip\7z.exe" set "SEVENZ_EXE=%ProgramFiles%\7-Zip\7z.exe"

set "POWERSHELL_EXE=%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe"
set "UV_EXE=uv"
set "UV_ARGS="
set "UV_INDEX_ARGS="
if "%UV_LOCK_TIMEOUT%"=="" set "UV_LOCK_TIMEOUT=300"
if not "%SHOPRPA_PIP_INDEX_URL%"=="" set "UV_INDEX_ARGS=--index-url %SHOPRPA_PIP_INDEX_URL%"

REM ============================================
REM 3. Environment Check
REM ============================================

REM Check if both engine and frontend are skipped
if "%SKIP_ENGINE%"=="1" if "%SKIP_FRONTEND%"=="1" (
    echo Error: Cannot skip both engine and frontend builds
    exit /b 1
)

REM Skip engine environment checks if engine build is disabled
if "%SKIP_ENGINE%"=="1" goto skip_engine_checks

if not exist "%PYTHON_EXE%" (
    echo Local Python environment not found: %PYTHON_EXE%
    echo Please set PYTHON_EXE by one of the following methods:
    echo   1. Use parameter: build.bat --python-exe "path"
    echo   2. Set environment variable: $env:PYTHON_EXE = "C:\Program Files\Python313\python.exe"
    echo   3. Modify default value in build.bat
    exit /b 1
)

if not exist "%SEVENZ_EXE%" (
    echo 7z.exe not found: %SEVENZ_EXE%
    echo Please set SEVENZ_EXE by one of the following methods:
    echo   1. Use parameter: build.bat --sevenz-exe "path"
    echo   2. Set environment variable: $env:SEVENZ_EXE = "C:\Program Files\7-Zip\7z.exe"
    echo   3. Modify default value in build.bat
    exit /b 1
)

"%UV_EXE%" %UV_ARGS% --version >nul 2>&1
if errorlevel 1 (
    set "UV_EXE=%PYTHON_EXE%"
    set "UV_ARGS=-m uv"
    "!UV_EXE!" !UV_ARGS! --version >nul 2>&1
    if errorlevel 1 (
        echo uv not found. Install uv for Python 3.13 or make uv available in PATH.
        echo Tried:
        echo   uv
        echo   "%PYTHON_EXE%" -m uv
        exit /b 1
    )
)

:skip_engine_checks

REM ============================================
REM 4. Browser Bridge Inject Sync
REM ============================================

echo.
echo ============================================
echo Preparing Browser Bridge Inject Assets
echo ============================================

echo Checking pnpm installation through Corepack...
call corepack pnpm --version >nul 2>&1
if !errorlevel! neq 0 (
    echo.
    echo ERROR: pnpm not available through Corepack. Please install Node.js 22+ and run: corepack enable
    exit /b 1
)
echo pnpm check passed

echo Navigating to frontend directory: %SCRIPT_DIR%\frontend
cd /d "%SCRIPT_DIR%\frontend"
if errorlevel 1 (
    echo Frontend directory not found: %SCRIPT_DIR%\frontend
    exit /b 1
)

if not exist "package.json" (
    echo ERROR: package.json not found in frontend directory
    cd /d "%SCRIPT_DIR%"
    exit /b 1
)

if exist "node_modules\.bin\vite.cmd" if exist "node_modules\.bin\tsdown.cmd" (
    echo Dependencies already installed, skipping pnpm install
) else (
    echo Installing dependencies...
    call corepack pnpm install --prefer-offline
    if !errorlevel! neq 0 (
        echo pnpm install failed
        cd /d "%SCRIPT_DIR%"
        exit /b 1
    )
)

echo Building and syncing browser bridge inject assets...
call corepack pnpm --filter @rpa/extension run build:bridge-inject
if !errorlevel! neq 0 (
    echo Browser bridge inject build failed
    cd /d "%SCRIPT_DIR%"
    exit /b 1
)
cd /d "%SCRIPT_DIR%"

REM ============================================
REM 5. Engine Build
REM ============================================

if "%SKIP_ENGINE%"=="1" (
    echo.
    echo ============================================
    echo Engine Build Skipped
    echo ============================================
    goto skip_engine_build
)

echo.
echo ============================================
echo Starting Engine Build
echo ============================================

REM ============================================
REM 5.1. Environment Setup
REM ============================================

echo Cleaning dist directory...
if exist "%DIST_DIR%" (
    rmdir /s /q "%DIST_DIR%"
    if errorlevel 1 (
        echo Failed to clean dist directory
        exit /b 1
    )
)

echo Existing requirements.txt will be overwritten only after a successful wheel build.

if exist %ENGINE_DIR%\pyproject.toml.backup (
    move /y %ENGINE_DIR%\pyproject.toml.backup %ENGINE_DIR%\pyproject.toml >nul
)

echo Creating build directory structure...
if not exist %BUILD_DIR% mkdir %BUILD_DIR%
if not exist %DIST_DIR% mkdir %DIST_DIR%
if not exist %PYTHON_CORE_DIR% mkdir %PYTHON_CORE_DIR%
if not exist %ARCHIVE_DIST_DIR% mkdir %ARCHIVE_DIST_DIR%
for /f "delims=" %%F in ("%PYTHON_EXE%") do set "PYTHON_SOURCE_DIR=%%~dpF"
if not exist "%PYTHON_CORE_DIR%\python.exe" (
    echo Copying Python environment...
    if exist "%PYTHON_SOURCE_DIR%" (
        xcopy /E /I /Y "%PYTHON_SOURCE_DIR%*" "%PYTHON_CORE_DIR%\"
        if errorlevel 1 (
            echo Python directory copy failed
            exit /b 1
        )
        echo Python environment copied successfully
    ) else (
        echo %PYTHON_SOURCE_DIR% directory not found
        exit /b 1
    )
) else (
    echo Python environment already exists, skipping copy...
)

REM ============================================
REM 5.2. Build Packages
REM ============================================

echo Backing up original and adding workspace
copy %ENGINE_DIR%\pyproject.toml %ENGINE_DIR%\pyproject.toml.backup >nul

REM Dynamically build workspace members list
echo Building workspace members list...
set "WORKSPACE_MEMBERS="

REM Check shared/* directories
for /d %%d in ("%ENGINE_DIR%\shared\*") do (
    if exist "%%d\pyproject.toml" (
        set "MEMBER_PATH=%%~nxd"
        set "WORKSPACE_MEMBERS=!WORKSPACE_MEMBERS! "shared/!MEMBER_PATH!","
    )
)

REM Check servers/* directories
for /d %%d in ("%ENGINE_DIR%\servers\*") do (
    if exist "%%d\pyproject.toml" (
        set "MEMBER_PATH=%%~nxd"
        set "WORKSPACE_MEMBERS=!WORKSPACE_MEMBERS! "servers/!MEMBER_PATH!","
    )
)

REM Check components/* directories
for /d %%d in ("%ENGINE_DIR%\components\*") do (
    if exist "%%d\pyproject.toml" (
        set "MEMBER_PATH=%%~nxd"
        if /i not "!MEMBER_PATH!"=="astronverse-database" (
            set "WORKSPACE_MEMBERS=!WORKSPACE_MEMBERS! "components/!MEMBER_PATH!","
        )
    )
)

REM Remove trailing comma and build final members list
if defined WORKSPACE_MEMBERS (
    set "WORKSPACE_MEMBERS=!WORKSPACE_MEMBERS:~0,-1!"
) else (
    echo Warning: No valid workspace members found
    set "WORKSPACE_MEMBERS=\"\""
)

echo. >> %ENGINE_DIR%\pyproject.toml
echo [tool.uv.workspace] >> %ENGINE_DIR%\pyproject.toml
echo members = [!WORKSPACE_MEMBERS!] >> %ENGINE_DIR%\pyproject.toml

if not exist "%SCRIPT_DIR%\.run-logs" mkdir "%SCRIPT_DIR%\.run-logs"
set "ENGINE_BUILD_LOG=%SCRIPT_DIR%\.run-logs\engine-build.log"
echo Starting batch build of all packages...
echo Engine build log: %ENGINE_BUILD_LOG%
if "%SHOPRPA_OFFLINE_ENGINE_OVERLAY%"=="1" (
    echo SHOPRPA_OFFLINE_ENGINE_OVERLAY=1, overlaying engine sources into existing python_core.
    if not exist "%PYTHON_CORE_DIR%\python.exe" (
        echo Existing python_core is required for offline engine overlay.
        exit /b 1
    )
    move %ENGINE_DIR%\pyproject.toml.backup %ENGINE_DIR%\pyproject.toml >nul
    "%POWERSHELL_EXE%" -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT_DIR%\scripts\offline-engine-overlay.ps1" -EngineDir "%SCRIPT_DIR%\%ENGINE_DIR%" -PythonCoreDir "%SCRIPT_DIR%\%PYTHON_CORE_DIR%"
    if errorlevel 1 exit /b 1
    goto compress_python_core
)
"%UV_EXE%" %UV_ARGS% build --project %ENGINE_DIR% --all-packages --wheel --out-dir "%DIST_DIR%" > "%ENGINE_BUILD_LOG%" 2>&1
if errorlevel 1 (
    move %ENGINE_DIR%\pyproject.toml.backup %ENGINE_DIR%\pyproject.toml >nul
    if exist "%PYTHON_CORE_DIR%\python.exe" (
        echo.
        echo WARNING: Engine wheel build failed. Trying offline source overlay into existing python_core.
        "%POWERSHELL_EXE%" -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT_DIR%\scripts\offline-engine-overlay.ps1" -EngineDir "%SCRIPT_DIR%\%ENGINE_DIR%" -PythonCoreDir "%SCRIPT_DIR%\%PYTHON_CORE_DIR%"
        if not errorlevel 1 (
            echo Offline engine source overlay succeeded. Rebuilding python_core archive.
            goto compress_python_core
        )
        echo Offline engine source overlay failed.
    )
    if exist "%SCRIPT_DIR%\%ARCHIVE_DIST_DIR%\python_core.7z" if exist "%SCRIPT_DIR%\%ARCHIVE_DIST_DIR%\python_core.7z.sha256.txt" (
        if "%SHOPRPA_STRICT_ENGINE_BUILD%"=="1" (
            if exist "%ENGINE_BUILD_LOG%" type "%ENGINE_BUILD_LOG%"
            exit /b 1
        )
        echo.
        echo WARNING: Engine source build failed. Reusing existing resources\python_core.7z bundle.
        echo Engine build log: %ENGINE_BUILD_LOG%
        echo Set SHOPRPA_STRICT_ENGINE_BUILD=1 to fail instead of using the existing engine bundle.
        goto skip_engine_build
    )
    if exist "%ENGINE_BUILD_LOG%" type "%ENGINE_BUILD_LOG%"
    exit /b 1
)
move %ENGINE_DIR%\pyproject.toml.backup %ENGINE_DIR%\pyproject.toml >nul
echo All packages built successfully

REM ============================================
REM 5.3. Install Packages
REM ============================================

echo Upgrading pip...
%PYTHON_CORE_DIR%\python.exe -m pip install --upgrade pip 2>nul

echo Generating requirements.txt from built packages...

REM Generate requirements.txt from wheel files using PowerShell
"%POWERSHELL_EXE%" -Command "$files = Get-ChildItem '%DIST_DIR%\*.whl' | ForEach-Object { $name = $_.BaseName -replace '_','-'; $name -replace '-\d+\.\d+\.\d+-py3-none-any$','' }; Set-Content -Path '%ENGINE_DIR%\requirements.txt' -Value '# Generated requirements from built packages'; Add-Content -Path '%ENGINE_DIR%\requirements.txt' -Value $files"

echo Installing packages from requirements.txt...
"%UV_EXE%" %UV_ARGS% pip install --link-mode=copy --python "%PYTHON_CORE_DIR%\python.exe" --find-links="%DIST_DIR%" -r "%ENGINE_DIR%\requirements.txt" --upgrade --force-reinstall %UV_INDEX_ARGS%
if errorlevel 1 (
    echo Package installation failed
    if exist "%SCRIPT_DIR%\%ARCHIVE_DIST_DIR%\python_core.7z" if exist "%SCRIPT_DIR%\%ARCHIVE_DIST_DIR%\python_core.7z.sha256.txt" (
        if "%SHOPRPA_STRICT_ENGINE_BUILD%"=="1" exit /b 1
        echo.
        echo WARNING: Engine package installation failed. Reusing existing resources\python_core.7z bundle.
        echo Set SHOPRPA_STRICT_ENGINE_BUILD=1 to fail instead of using the existing engine bundle.
        goto skip_engine_build
    )
    exit /b 1
)
echo Batch installation successful

@REM ===========================================
@REM Run meta_json.py to generate temp.json
@REM ===========================================
@REM echo Running meta_json.py to generate temp.json...
@REM %PYTHON_CORE_DIR%\python.exe %ENGINE_DIR%\meta_json.py
@REM if errorlevel 1 (
@REM     echo meta_json.py execution failed
@REM     exit /b 1
@REM )
@REM echo meta_json.py executed successfully

REM ============================================
REM 5.4. Package and Release
REM ============================================

:compress_python_core
echo Compressing python_core directory...
set "ENGINE_ARCHIVE=%SCRIPT_DIR%\%ARCHIVE_DIST_DIR%\python_core.7z"
set "ENGINE_ARCHIVE_TMP=%SCRIPT_DIR%\%ARCHIVE_DIST_DIR%\python_core.7z.tmp"
if exist "%ENGINE_ARCHIVE_TMP%" del /f /q "%ENGINE_ARCHIVE_TMP%"
cd /d "%PYTHON_CORE_DIR%"
"%SEVENZ_EXE%" a -t7z "%ENGINE_ARCHIVE_TMP%" "*" >nul
cd /d "%SCRIPT_DIR%"
if errorlevel 1 (
    echo python_core directory compression failed
    if exist "%ENGINE_ARCHIVE_TMP%" del /f /q "%ENGINE_ARCHIVE_TMP%"
    exit /b 1
)
move /y "%ENGINE_ARCHIVE_TMP%" "%ENGINE_ARCHIVE%" >nul
if errorlevel 1 (
    echo python_core archive replacement failed
    if exist "%ENGINE_ARCHIVE_TMP%" del /f /q "%ENGINE_ARCHIVE_TMP%"
    exit /b 1
)
echo Python_core directory compressed successfully, file saved to: %ENGINE_ARCHIVE%

REM Generate SHA-256 checksum for the engine archive.
set "HASH_FILE=%SCRIPT_DIR%\%ARCHIVE_DIST_DIR%\python_core.7z.sha256.txt"

"%POWERSHELL_EXE%" -NoProfile -Command "(Get-FileHash -Algorithm SHA256 -LiteralPath '%ENGINE_ARCHIVE%').Hash | Set-Content -NoNewline -Encoding ASCII -LiteralPath '%HASH_FILE%'"
if errorlevel 1 (
    echo SHA-256 generation failed
    exit /b 1
)
echo Hash file generated: %HASH_FILE%

if "%SKIP_FRONTEND%"=="1" if exist "%SCRIPT_DIR%\%PORTABLE_RESOURCES_DIR%" (
    echo Updating existing portable Python runtime archive...
    copy /y "%ENGINE_ARCHIVE%" "%SCRIPT_DIR%\%PORTABLE_RESOURCES_DIR%\python_core.7z" >nul
    if errorlevel 1 (
        echo Failed to update portable python_core.7z
        exit /b 1
    )
    copy /y "%HASH_FILE%" "%SCRIPT_DIR%\%PORTABLE_RESOURCES_DIR%\python_core.7z.sha256.txt" >nul
    if errorlevel 1 (
        echo Failed to update portable python_core.7z.sha256.txt
        exit /b 1
    )
    echo Existing portable Python runtime archive updated.
)

echo.
echo ============================================
echo Engine Build Complete!
echo ============================================

:skip_engine_build

REM ============================================
REM 6. Frontend Build
REM ============================================

if "%SKIP_FRONTEND%"=="1" (
    echo.
    echo ============================================
    echo Frontend Build Skipped
    echo ============================================
    if exist "%SCRIPT_DIR%\%PORTABLE_DIST_DIR%\ShopRPA.cmd" (
        echo Running portable host verification against the existing portable package...
        "%POWERSHELL_EXE%" -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT_DIR%\scripts\verify-portable-host.ps1"
        if !errorlevel! neq 0 (
            echo Portable host verification failed
            exit /b 1
        )
        echo Portable host verification completed successfully
    )
    goto skip_frontend_build
)

echo.
echo ============================================
echo Starting Frontend Build
echo ============================================

echo Checking pnpm installation through Corepack...
call corepack pnpm --version >nul 2>&1
if !errorlevel! neq 0 (
    echo.
    echo ERROR: pnpm not available through Corepack. Please install Node.js 22+ and run: corepack enable
    exit /b 1
)
echo pnpm check passed

echo Navigating to frontend directory: %SCRIPT_DIR%\frontend
cd /d "%SCRIPT_DIR%\frontend"
if errorlevel 1 (
    echo Frontend directory not found: %SCRIPT_DIR%\frontend
    exit /b 1
)
echo Successfully entered frontend directory
echo Current directory: %CD%

echo Checking for package.json...
if not exist "package.json" (
    echo ERROR: package.json not found in frontend directory
    cd /d "%SCRIPT_DIR%"
    exit /b 1
)

if exist "node_modules\.bin\vite.cmd" if exist "node_modules\.bin\tsdown.cmd" (
    echo Dependencies already installed, skipping pnpm install
) else (
    echo Installing dependencies...
    call corepack pnpm install --prefer-offline
    if !errorlevel! neq 0 (
        echo pnpm install failed
        cd /d "%SCRIPT_DIR%"
        exit /b 1
    )
)

echo Building desktop application...
call corepack pnpm build:desktop
if !errorlevel! neq 0 (
    echo Desktop application build failed
    cd /d "%SCRIPT_DIR%"
    exit /b 1
)

echo Frontend build completed successfully

REM Return to script directory
cd /d "%SCRIPT_DIR%"

echo Running portable host verification...
"%POWERSHELL_EXE%" -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT_DIR%\scripts\verify-portable-host.ps1"
if !errorlevel! neq 0 (
    echo Portable host verification failed
    exit /b 1
)
echo Portable host verification completed successfully

:skip_frontend_build

echo.
echo ============================================
echo Full Build Complete!
echo ============================================
echo.
echo Desktop application location:
echo   Portable app: frontend\packages\electron-app\dist\win-portable\ShopRPA.cmd
echo.
echo Installer build command:
echo   cd frontend ^&^& corepack pnpm build:installer
