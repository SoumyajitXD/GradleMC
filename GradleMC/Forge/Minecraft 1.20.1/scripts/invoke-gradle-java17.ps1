[CmdletBinding()]
param([Parameter(ValueFromRemainingArguments = $true)][string[]]$GradleArguments)

$roots = @()
if ($env:GRADLEMC_JAVA17_HOME) { $roots += $env:GRADLEMC_JAVA17_HOME }
$roots += @('C:\Program Files\Eclipse Adoptium', 'C:\Program Files\Java', 'C:\Program Files\Microsoft', 'C:\Program Files\Amazon Corretto', 'C:\Program Files\Zulu', 'C:\Program Files\BellSoft', "$env:LOCALAPPDATA\Programs\Eclipse Adoptium", "$env:LOCALAPPDATA\Programs\Java")
$candidates = foreach ($root in $roots | Where-Object { $_ -and (Test-Path -LiteralPath $_) }) {
    if (Test-Path -LiteralPath (Join-Path $root 'bin\java.exe')) { Get-Item -LiteralPath $root } else { Get-ChildItem -LiteralPath $root -Directory -ErrorAction SilentlyContinue }
}
$jdk = $candidates | Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName 'bin\java.exe'); Test-Path -LiteralPath (Join-Path $_.FullName 'bin\javac.exe') } | ForEach-Object { $version = & (Join-Path $_.FullName 'bin\java.exe') -version 2>&1 | Select-Object -First 1; $compiler = & (Join-Path $_.FullName 'bin\javac.exe') -version 2>&1 | Select-Object -First 1; if ("$version $compiler" -match '17\.') { $_.FullName } } | Select-Object -First 1
if (-not $jdk) { Write-Error 'No Java 17 JDK (java.exe and javac.exe) was found. Set GRADLEMC_JAVA17_HOME to a valid JDK 17 home.'; exit 1 }
$env:JAVA_HOME = $jdk
$env:Path = "$jdk\bin;" + (($env:Path -split ';' | Where-Object { $_ -and $_ -notmatch '(?i)\\Java\\|\\jdk-|\\jre-' }) -join ';')
& (Join-Path $PSScriptRoot '..\gradlew.bat') @GradleArguments
exit $LASTEXITCODE
