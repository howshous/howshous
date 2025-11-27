$javaHome = "C:\Program Files\Android\Android Studio\jbr"
$projectRoot = "D:\Android_projects\howshous-1"

$env:JAVA_HOME = $javaHome

if ($env:Path -notlike "*$javaHome\bin*") {
    $env:Path = "$javaHome\bin;$env:Path"
}

Set-Location $projectRoot

firebase emulators:start

