param(
    [string] $OutputDir = ""
)

$ErrorActionPreference = "Stop"

$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "../..")
Set-Location $RepoRoot
. (Join-Path $PSScriptRoot "gradlemc-env.ps1")
Use-GradleMCJava17

if (-not (Test-Path "./settings.gradle") -or -not (Test-Path "./gradle.properties") -or -not (Test-Path "./src/main/resources/META-INF/mods.toml")) {
    throw "This script must resolve to the GradleMC project root. Missing settings.gradle, gradle.properties, or mods.toml."
}

$gradlew = if (Test-Path "./gradlew.bat") { "./gradlew.bat" } else { "./gradlew" }
$args = @("exportReleaseJar")
if ($OutputDir) {
    $resolvedOutput = $ExecutionContext.SessionState.Path.GetUnresolvedProviderPathFromPSPath($OutputDir)
    $args += "-PgradlemcExportDir=$resolvedOutput"
}

& $gradlew @args
if ($LASTEXITCODE -ne 0) {
    throw "Gradle release export failed with exit code $LASTEXITCODE."
}

$artifactName = (Select-String -Path "./gradle.properties" -Pattern "^artifact_name=(.+)$").Matches.Groups[1].Value
$finalDir = if ($OutputDir) { $resolvedOutput } else { Join-Path $RepoRoot "build/exports" }
$finalJar = Join-Path $finalDir $artifactName

if (-not (Test-Path $finalJar)) {
    throw "Release export reported success but the expected artifact is missing: $finalJar"
}

python -m gradlemc_automation.validate_release --artifact $finalJar
if ($LASTEXITCODE -ne 0) {
    throw "Exported artifact validation failed with exit code $LASTEXITCODE."
}

Write-Host "GradleMC release artifact: $finalJar"
