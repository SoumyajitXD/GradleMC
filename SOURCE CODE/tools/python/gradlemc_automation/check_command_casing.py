from __future__ import annotations

import argparse
import re
from pathlib import Path

from .variants import ROOT, read_text

DEFAULT_PATHS = ["README.md", "AGENTS.md", "docs", "src/main", "src/main/resources", "curseforge-description"]
PATTERNS = [
    (re.compile(r"(?<![A-Za-z0-9_./-])/GradleMC(?:\s|$|[<`.,)])"), "uppercase /GradleMC command example"),
    (re.compile(r"Commands\.literal\(\"GradleMC\"\)"), "uppercase Brigadier command literal"),
]


def iter_files(paths: list[str]) -> list[Path]:
    files: list[Path] = []
    for raw in paths:
        path = ROOT / raw
        if path.is_dir():
            files.extend(file for file in path.rglob("*") if file.is_file())
        elif path.is_file():
            files.append(path)
    return files


def main() -> int:
    parser = argparse.ArgumentParser(description="Check GradleMC command casing.")
    parser.add_argument("paths", nargs="*", default=DEFAULT_PATHS)
    args = parser.parse_args()

    errors: list[str] = []
    for path in iter_files(args.paths):
        if any(part in {"build", ".gradle", "__pycache__"} for part in path.parts):
            continue
        text = read_text(path)
        for pattern, description in PATTERNS:
            for match in pattern.finditer(text):
                line = text.count("\n", 0, match.start()) + 1
                errors.append(f"{path.relative_to(ROOT)}:{line}: {description}")

    if errors:
        print("Command casing check failed:")
        for error in errors:
            print(f"- {error}")
        return 1
    print("Command casing check passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
