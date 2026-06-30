$ErrorActionPreference = "Stop"

$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "../..")
Set-Location $RepoRoot
. (Join-Path $PSScriptRoot "gradlemc-env.ps1")
Use-GradleMCJava17

if (Test-Path "./package.json") {
    if (-not (Test-Path "./package-lock.json")) {
        throw "package.json exists but package-lock.json is missing; refusing non-reproducible Node tooling install."
    }

    $npm = if ($IsWindows -or $env:OS -eq "Windows_NT") { "npm.cmd" } else { "npm" }
    $npmCommand = Get-Command $npm -ErrorAction SilentlyContinue
    if (-not $npmCommand) {
        throw "Node tooling is present, but npm was not found on PATH. Install Node.js or use the CI setup-node step."
    }

    Write-Host "Installing GradleMC Node tooling dependencies with npm ci..."
    & $npm ci
    if ($LASTEXITCODE -ne 0) {
        throw "GradleMC Node tooling install failed with exit code $LASTEXITCODE."
    }
}

$gradlew = if (Test-Path "./gradlew.bat") { "./gradlew.bat" } else { "./gradlew" }
& $gradlew checkAutomationTools validateVariantMatrix checkProjectIdentity checkCommandCasing checkFalseSupportClaims checkReleaseMetadata
if ($LASTEXITCODE -ne 0) {
    throw "GradleMC validation failed with exit code $LASTEXITCODE."
}
