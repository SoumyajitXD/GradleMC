param(
    [Parameter(Mandatory = $true, Position = 0)]
    [ValidateSet("check-env", "validate", "build-variant", "generate-matrix", "export-release")]
    [string] $Command,

    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]] $RemainingArgs
)

$ErrorActionPreference = "Stop"

switch ($Command) {
    "check-env" {
        & (Join-Path $PSScriptRoot "check-env.ps1") @RemainingArgs
    }
    "validate" {
        & (Join-Path $PSScriptRoot "validate.ps1") @RemainingArgs
    }
    "build-variant" {
        & (Join-Path $PSScriptRoot "build-variant.ps1") @RemainingArgs
    }
    "generate-matrix" {
        & (Join-Path $PSScriptRoot "generate-matrix.ps1") @RemainingArgs
    }
    "export-release" {
        & (Join-Path $PSScriptRoot "export-release.ps1") @RemainingArgs
    }
}
