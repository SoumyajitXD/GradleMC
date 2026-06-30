from __future__ import annotations

import argparse
import re
from pathlib import Path

from .variants import DEFAULT_MATRIX, ROOT, load_matrix, read_text, validate_matrix, variants

DOC_PATHS = ["README.md", "AGENTS.md", "docs", "curseforge-description", "curseforge-description.html"]
FALSE_SUPPORT_PATTERNS = [
    (re.compile(r"\bsupports\s+fabric\b", re.IGNORECASE), "Fabric support claim"),
    (re.compile(r"\bfabric\s+is\s+supported\b", re.IGNORECASE), "Fabric support claim"),
    (re.compile(r"\bsupports\s+neoforge\b", re.IGNORECASE), "NeoForge support claim"),
    (re.compile(r"\bneoforge\s+is\s+supported\b", re.IGNORECASE), "NeoForge support claim"),
    (re.compile(r"\bsupports\s+all\s+(minecraft\s+)?versions\b", re.IGNORECASE), "all versions support claim"),
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


def supported_loaders(matrix: dict) -> set[str]:
    return {variant["loader"] for variant in variants(matrix) if variant.get("status") == "supported" and variant.get("enabled") and variant.get("buildable")}


def check_identity(matrix: dict) -> list[str]:
    errors: list[str] = []
    if matrix.get("productName") != "GradleMC":
        errors.append("Manifest productName must be GradleMC")
    settings = read_text(ROOT / "settings.gradle")
    if "rootProject.name = 'GradleMC'" not in settings and 'rootProject.name = "GradleMC"' not in settings:
        errors.append("settings.gradle rootProject.name must be GradleMC")
    props = read_text(ROOT / "gradle.properties")
    if "product_name=GradleMC" not in props:
        errors.append("gradle.properties product_name must be GradleMC")
    if re.search(r"rootProject\.name\s*=\s*['\"]GradleMC Forge 1\.20\.1['\"]", settings):
        errors.append("GradleMC Forge 1.20.1 must not be used as the project name")
    return errors


def check_false_support_claims(matrix: dict) -> list[str]:
    loaders = supported_loaders(matrix)
    errors: list[str] = []
    for path in iter_files(DOC_PATHS):
        text = read_text(path)
        for pattern, description in FALSE_SUPPORT_PATTERNS:
            if pattern.search(text):
                if "Fabric" in description and "fabric" in loaders:
                    continue
                if "NeoForge" in description and "neoforge" in loaders:
                    continue
                line = text[: pattern.search(text).start()].count("\n") + 1
                errors.append(f"{path.relative_to(ROOT)}:{line}: {description}")
        if "config/gradlemc/reports" in text:
            line = text.index("config/gradlemc/reports")
            errors.append(f"{path.relative_to(ROOT)}:{text[:line].count(chr(10)) + 1}: generated outputs should use <gameDir>/gradlemc, not config/gradlemc")
    return errors


def main() -> int:
    parser = argparse.ArgumentParser(description="Check GradleMC identity and support claims.")
    parser.add_argument("--matrix", type=Path, default=DEFAULT_MATRIX)
    parser.add_argument("--identity", action="store_true")
    parser.add_argument("--false-support", action="store_true")
    args = parser.parse_args()

    matrix = load_matrix(args.matrix)
    errors = validate_matrix(matrix)
    if args.identity or (not args.identity and not args.false_support):
        errors.extend(check_identity(matrix))
    if args.false_support or (not args.identity and not args.false_support):
        errors.extend(check_false_support_claims(matrix))

    if errors:
        print("GradleMC claim check failed:")
        for error in errors:
            print(f"- {error}")
        return 1
    print("GradleMC claim check passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
