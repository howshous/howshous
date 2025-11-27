$javaHome = "C:\Program Files\Android\Android Studio\jbr"
$projectRoot = "D:\Android_projects\howshous"

$env:JAVA_HOME = $javaHome

if ($env:Path -notlike "*$javaHome\bin*") {
    $env:Path = "$javaHome\bin;$env:Path"
}

Set-Location $projectRoot

Write-Host "Starting Firebase Emulators..." -ForegroundColor Green
Write-Host "  Auth: port 9100" -ForegroundColor Cyan
Write-Host "  Firestore: port 8085" -ForegroundColor Cyan
Write-Host "  Storage: port 9190" -ForegroundColor Cyan
Write-Host "  UI: port 4001" -ForegroundColor Cyan
Write-Host ""

firebase emulators:start

