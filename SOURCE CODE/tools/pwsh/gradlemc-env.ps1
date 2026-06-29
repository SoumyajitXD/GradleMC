$ErrorActionPreference = "Stop"

function Get-GradleMCJavaMajor {
    param([string] $JavaExe)
    if (-not $JavaExe -or -not (Test-Path $JavaExe)) {
        return $null
    }
    $output = & $JavaExe -version 2>&1
    $text = ($output -join " ")
    if ($text -match 'version "1\.(\d+)') {
        return [int] $Matches[1]
    }
    if ($text -match 'version "(\d+)') {
        return [int] $Matches[1]
    }
    return $null
}

function Get-GradleMCJavaCandidates {
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
        foreach ($root in @("$env:ProgramFiles\Java", "$env:ProgramFiles\Eclipse Adoptium", "$env:ProgramFiles\Microsoft")) {
            if (Test-Path $root) {
                Get-ChildItem -Path $root -Recurse -Filter java.exe -ErrorAction SilentlyContinue |
                    Where-Object { $_.FullName -match '\\bin\\java\.exe$' } |
                    ForEach-Object { $candidates.Add($_.FullName) }
            }
        }
    }
    $candidates | Where-Object { $_ } | Select-Object -Unique
}

function Use-GradleMCJava17 {
    $currentJava = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME "bin/java.exe" } else { $null }
    if ((Get-GradleMCJavaMajor $currentJava) -eq 17) {
        return
    }

    foreach ($candidate in Get-GradleMCJavaCandidates) {
        if ((Get-GradleMCJavaMajor $candidate) -eq 17) {
            $javaHome = Split-Path (Split-Path $candidate -Parent) -Parent
            $env:JAVA_HOME = $javaHome
            $env:Path = "$(Join-Path $javaHome 'bin');$env:Path"
            Write-Host "Using Java 17 for GradleMC: $javaHome"
            return
        }
    }

    throw "GradleMC's Forge 1.20.1 build requires Java 17 to run Gradle safely. Install JDK 17 or set JAVA_HOME to a JDK 17 path."
}
