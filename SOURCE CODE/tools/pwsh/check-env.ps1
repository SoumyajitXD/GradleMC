$ErrorActionPreference = "Stop"

$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "../..")
Set-Location $RepoRoot

function Write-Status {
    param([string] $Name, [bool] $Ok, [string] $Detail)
    $prefix = if ($Ok) { "OK" } else { "ERROR" }
    Write-Host "${prefix}: ${Name}: ${Detail}"
    if (-not $Ok) {
        $script:HadError = $true
    }
}

function Get-JavaMajor {
    param([string] $JavaExe)
    if (-not (Test-Path $JavaExe)) {
        return $null
    }
    $output = & $JavaExe -version 2>&1
    $text = ($output -join " ")
    if ($text -match 'version "(\d+)') {
        return [int] $Matches[1]
    }
    return $null
}

function Get-JavaCandidates {
    $candidates = New-Object System.Collections.Generic.List[string]
    if ($env:JAVA_HOME) {
        $candidates.Add((Join-Path $env:JAVA_HOME "bin/java.exe"))
        $candidates.Add((Join-Path $env:JAVA_HOME "bin/java"))
    }
    $javaCommand = Get-Command java -ErrorAction SilentlyContinue
    if ($javaCommand) {
        $candidates.Add($javaCommand.Source)
    }
    if ($IsWindows -or $env:OS -eq "Windows_NT") {
        $programFilesX86 = [Environment]::GetEnvironmentVariable("ProgramFiles(x86)")
        $roots = @(
            "$env:ProgramFiles\Java",
            "$env:ProgramFiles\Eclipse Adoptium",
            "$env:ProgramFiles\Microsoft",
            "$(if ($programFilesX86) { $programFilesX86 } else { '' })\Java"
        ) | Where-Object { $_ -and (Test-Path $_) }
        foreach ($root in $roots) {
            Get-ChildItem -Path $root -Recurse -Filter java.exe -ErrorAction SilentlyContinue |
                Where-Object { $_.FullName -match '\\bin\\java\.exe$' } |
                ForEach-Object { $candidates.Add($_.FullName) }
        }
    }
    $candidates | Select-Object -Unique
}

function Get-PythonForMinor {
    param([string] $Minor)
    $py = Get-Command py -ErrorAction SilentlyContinue
    if ($py) {
        $probe = & py "-$Minor" -c "import sys; print(sys.executable)" 2>$null
        if ($LASTEXITCODE -eq 0 -and $probe) {
            return ($probe | Select-Object -First 1)
        }
    }
    $named = Get-Command "python$Minor" -ErrorAction SilentlyContinue
    if ($named) {
        return $named.Source
    }
    return $null
}

$script:HadError = $false

Write-Host "GradleMC environment check"
Write-Host "Repository: $RepoRoot"
Write-Host "JAVA_HOME: $(if ($env:JAVA_HOME) { $env:JAVA_HOME } else { '<not set>' })"

$hasGradleWrapper = (Test-Path "./gradlew.bat") -or (Test-Path "./gradlew")
Write-Status "Gradle wrapper" $hasGradleWrapper "gradlew/gradlew.bat"

$python = Get-Command python -ErrorAction SilentlyContinue
if ($python) {
    $pythonVersion = & python -c "import sys; print('.'.join(map(str, sys.version_info[:3])))"
    $pythonOk = [version] $pythonVersion -ge [version] "3.12"
    Write-Status "Python" $pythonOk "$pythonVersion at $($python.Source)"
} else {
    Write-Status "Python" $false "not found"
}

foreach ($minor in @("3.12", "3.13", "3.14")) {
    $pythonForMinor = Get-PythonForMinor $minor
    $isRequired = $minor -eq "3.12"
    if ($pythonForMinor) {
        Write-Status "Python $minor" $true $pythonForMinor
    } else {
        Write-Status "Python $minor" (-not $isRequired) "$(if ($isRequired) { 'not detected; Python 3.12 is the baseline' } else { 'not detected; optional local check' })"
    }
}

$pwshVersion = $PSVersionTable.PSVersion.ToString()
Write-Status "PowerShell" ($PSVersionTable.PSVersion.Major -ge 7) "$pwshVersion"

$node = Get-Command node -ErrorAction SilentlyContinue
if ($node) {
    Write-Status "Node.js" $true "$(& node --version) at $($node.Source)"
} else {
    Write-Status "Node.js" $true "not found; not required unless package.json exists"
}

$javaByMajor = @{}
foreach ($candidate in Get-JavaCandidates) {
    $major = Get-JavaMajor $candidate
    if ($major -and -not $javaByMajor.ContainsKey($major)) {
        $javaByMajor[$major] = $candidate
    }
}

foreach ($required in @(17, 21, 25)) {
    if ($javaByMajor.ContainsKey($required)) {
        Write-Status "Java $required" $true $javaByMajor[$required]
    } else {
        Write-Status "Java $required" $false "not detected locally"
    }
}

python -m gradlemc_automation.toolchain_report --check-tools
if ($LASTEXITCODE -ne 0) {
    $script:HadError = $true
}

if ($script:HadError) {
    throw "GradleMC environment check failed."
}
