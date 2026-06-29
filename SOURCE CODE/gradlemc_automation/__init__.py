"""Bootstrap package for running GradleMC automation from the repo root.

The real package lives under tools/python so the tooling stays grouped, but
`python -m gradlemc_automation.<module>` also works from the repository root.
"""
from __future__ import annotations

from pathlib import Path

_TOOLS_PACKAGE = Path(__file__).resolve().parents[1] / "tools" / "python" / "gradlemc_automation"
if _TOOLS_PACKAGE.exists():
    __path__.append(str(_TOOLS_PACKAGE))

