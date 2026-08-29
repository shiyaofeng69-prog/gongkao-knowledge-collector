$ErrorActionPreference = "Stop"

$toolchainRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..\toolchains")).Path
$jdkRoot = (Resolve-Path -LiteralPath (Join-Path $toolchainRoot "jdk\jdk-17.0.20.1+1")).Path
$androidSdkRoot = (Resolve-Path -LiteralPath (Join-Path $toolchainRoot "android-sdk")).Path
$androidUserRoot = (Resolve-Path -LiteralPath (Join-Path $toolchainRoot "android-user-home")).Path
$gradleUserRoot = (Resolve-Path -LiteralPath (Join-Path $toolchainRoot "gradle-user-home")).Path
$gradleExecutable = (Resolve-Path -LiteralPath (Join-Path $toolchainRoot "gradle\gradle-9.4.1\bin\gradle.bat")).Path

$env:JAVA_HOME = $jdkRoot
$env:ANDROID_HOME = $androidSdkRoot
$env:ANDROID_SDK_ROOT = $androidSdkRoot
$env:ANDROID_USER_HOME = $androidUserRoot
$env:GRADLE_USER_HOME = $gradleUserRoot
Remove-Item Env:ANDROID_PREFS_ROOT -ErrorAction SilentlyContinue
Remove-Item Env:GRADLE_OPTS -ErrorAction SilentlyContinue

& $gradleExecutable @args
exit $LASTEXITCODE
