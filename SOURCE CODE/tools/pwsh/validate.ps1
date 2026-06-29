$ErrorActionPreference = "Stop"

$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "../..")
Set-Location $RepoRoot
. (Join-Path $PSScriptRoot "gradlemc-env.ps1")
Use-GradleMCJava17

$gradlew = if (Test-Path "./gradlew.bat") { "./gradlew.bat" } else { "./gradlew" }
& $gradlew checkAutomationTools validateVariantMatrix checkProjectIdentity checkCommandCasing checkFalseSupportClaims checkReleaseMetadata
if ($LASTEXITCODE -ne 0) {
    throw "GradleMC validation failed with exit code $LASTEXITCODE."
}
