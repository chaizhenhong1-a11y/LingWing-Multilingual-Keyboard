param([string]$Sdk = '', [string]$BuildTemp = $env:TEMP)
$ErrorActionPreference = 'Stop'
$project = Split-Path $PSScriptRoot -Parent
if (!$Sdk) {
    $localProperties = Join-Path $project 'local.properties'
    $sdkLine = if (Test-Path -LiteralPath $localProperties) { Get-Content -LiteralPath $localProperties | Where-Object { $_ -match '^sdk.dir=' } | Select-Object -First 1 }
    $Sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } elseif ($sdkLine) { $sdkLine.Substring(8).Replace('\\', '\').Replace('\:', ':') } else { "$env:LOCALAPPDATA\Android\Sdk" }
}
$toolDirectory = Join-Path $project '.sandbox-tools'
New-Item -ItemType Directory -Force $toolDirectory | Out-Null
$compiler = Join-Path $env:WINDIR 'Microsoft.NET\Framework64\v4.0.30319\csc.exe'
$adapter = Join-Path $toolDirectory 'aapt2.exe'
& $compiler /nologo /r:System.IO.Compression.dll /r:System.IO.Compression.FileSystem.dll "/out:$adapter" (Join-Path $PSScriptRoot 'AaptDirectoryAdapter.cs')
if ($LASTEXITCODE -ne 0) { throw 'Resource adapter compilation failed.' }
$env:LAZYTALK_SDK_AAPT2 = Join-Path $Sdk 'build-tools\36.0.0\aapt2.exe'
$env:LAZYTALK_BUILD_TEMP = $BuildTemp
New-Item -ItemType Directory -Force $env:LAZYTALK_BUILD_TEMP | Out-Null
if (!(Test-Path -LiteralPath $env:LAZYTALK_SDK_AAPT2)) { throw 'Install Android SDK Build Tools 36.0.0 or set LAZYTALK_SDK_AAPT2 appropriately.' }
& (Join-Path $project 'gradlew.bat') -p $project :app:compileDebugKotlin :app:testDebugUnitTest :app:lintDebug :app:assembleDebug "-Pandroid.aapt2FromMavenOverride=$adapter" --no-daemon --max-workers=1 --console=plain
exit $LASTEXITCODE
