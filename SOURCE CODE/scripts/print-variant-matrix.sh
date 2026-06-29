#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 -m gradlemc_automation.validate_variants --print "$@"
