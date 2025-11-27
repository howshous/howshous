# Script to launch emulator and run the Android app
Write-Host "=== Starting Firebase Emulators, Android Emulator and App ===" -ForegroundColor Green

# Set JAVA_HOME
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
if (-not (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
    Write-Host "ERROR: Java not found at $env:JAVA_HOME" -ForegroundColor Red
    exit 1
}

# Add Java to PATH if not already there
if ($env:Path -notlike "*$env:JAVA_HOME\bin*") {
    $env:Path = "$env:JAVA_HOME\bin;$env:Path"
}

# Kill any existing Firebase emulator processes
Write-Host "`n[1/6] Checking for existing Firebase emulator processes..." -ForegroundColor Yellow
Get-Process | Where-Object {$_.ProcessName -eq "node" -or $_.ProcessName -eq "java"} | Where-Object {
    try {
        $_.CommandLine -like "*firebase*emulator*" -or $_.Path -like "*firebase*"
    } catch {
        $false
    }
} | Stop-Process -Force -ErrorAction SilentlyContinue
Start-Sleep -Seconds 1

# Start Firebase emulators in a new window
Write-Host "[2/6] Starting Firebase emulators..." -ForegroundColor Yellow
$firebaseProcess = Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$PWD'; firebase emulators:start" -WindowStyle Normal -PassThru
Write-Host "Firebase emulators starting in new window (PID: $($firebaseProcess.Id))..." -ForegroundColor Cyan
Write-Host "Waiting for Firebase emulators to initialize..." -ForegroundColor Cyan
Start-Sleep -Seconds 8

# Kill any existing Android emulator processes
Write-Host "[3/6] Checking for existing Android emulator processes..." -ForegroundColor Yellow
Get-Process | Where-Object {$_.ProcessName -like "*emulator*" -or $_.ProcessName -like "*qemu*"} | Stop-Process -Force -ErrorAction SilentlyContinue
Start-Sleep -Seconds 2

# Kill ADB server and restart
Write-Host "[4/6] Restarting ADB server..." -ForegroundColor Yellow
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" kill-server 2>$null
Start-Sleep -Seconds 1

# Start Android emulator
Write-Host "[5/6] Starting Android emulator (this may take 30-60 seconds)..." -ForegroundColor Yellow
$emulatorPath = "$env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe"
if (-not (Test-Path $emulatorPath)) {
    Write-Host "ERROR: Emulator not found at $emulatorPath" -ForegroundColor Red
    exit 1
}

# Launch emulator in new window
Start-Process -FilePath $emulatorPath -ArgumentList "-avd", "Medium_Phone_API_36.1" -WindowStyle Normal

# Wait for emulator to boot
Write-Host "Waiting for emulator to boot..." -ForegroundColor Cyan
$timeout = 90
$elapsed = 0
$booted = $false

while ($elapsed -lt $timeout) {
    Start-Sleep -Seconds 5
    $elapsed += 5
    $devices = & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" devices 2>&1
    if ($devices -match "emulator.*device") {
        Write-Host "Emulator is ready! ($elapsed seconds)" -ForegroundColor Green
        $booted = $true
        break
    }
    Write-Host "  Still booting... ($elapsed seconds)" -ForegroundColor Gray
}

if (-not $booted) {
    Write-Host "ERROR: Emulator failed to boot within $timeout seconds" -ForegroundColor Red
    exit 1
}

# Bring emulator window to foreground
Write-Host "[6/6] Bringing emulator window to foreground..." -ForegroundColor Yellow
Add-Type -TypeDefinition @"
using System;
using System.Runtime.InteropServices;
public class Win32 {
    [DllImport("user32.dll")]
    public static extern bool ShowWindow(IntPtr hWnd, int nCmdShow);
    [DllImport("user32.dll")]
    public static extern bool SetForegroundWindow(IntPtr hWnd);
    public const int SW_RESTORE = 9;
    public const int SW_SHOWMAXIMIZED = 3;
}
"@

$proc = Get-Process | Where-Object {$_.MainWindowTitle -like "*Android Emulator*"}
if ($proc -and $proc.MainWindowHandle -ne [IntPtr]::Zero) {
    [Win32]::ShowWindow($proc.MainWindowHandle, [Win32]::SW_RESTORE)
    Start-Sleep -Milliseconds 300
    [Win32]::ShowWindow($proc.MainWindowHandle, [Win32]::SW_SHOWMAXIMIZED)
    Start-Sleep -Milliseconds 300
    [Win32]::SetForegroundWindow($proc.MainWindowHandle)
    Write-Host "Emulator window is now visible" -ForegroundColor Green
} else {
    Write-Host "Warning: Could not find emulator window handle" -ForegroundColor Yellow
}

# Build and install app
Write-Host "Building and installing app..." -ForegroundColor Yellow
& .\gradlew.bat installDebug
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Build failed" -ForegroundColor Red
    exit 1
}

# Launch app
Write-Host "Launching app..." -ForegroundColor Yellow
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" shell monkey -p io.github.howshous -c android.intent.category.LAUNCHER 1
if ($LASTEXITCODE -eq 0) {
    Write-Host "`nApp launched successfully!" -ForegroundColor Green
    Write-Host "The emulator window should be visible with your app running." -ForegroundColor Green
} else {
    Write-Host "Warning: App launch command returned error code $LASTEXITCODE" -ForegroundColor Yellow
}

Write-Host "`n=== Done ===" -ForegroundColor Green
Write-Host "Firebase emulators are running in a separate window (PID: $($firebaseProcess.Id))." -ForegroundColor Cyan
Write-Host "Firebase UI: http://localhost:4001" -ForegroundColor Cyan
Write-Host "Auth: http://localhost:9100" -ForegroundColor Cyan
Write-Host "Firestore: http://localhost:8085" -ForegroundColor Cyan
Write-Host "Storage: http://localhost:9190" -ForegroundColor Cyan
Write-Host "`nTo stop Firebase emulators, close the Firebase emulator window or press Ctrl+C in that window." -ForegroundColor Gray

