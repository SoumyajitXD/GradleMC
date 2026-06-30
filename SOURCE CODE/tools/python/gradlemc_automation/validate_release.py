from __future__ import annotations

import argparse
import re
import sys
import zipfile
from pathlib import Path

from .variants import DEFAULT_MATRIX, ROOT, load_matrix, validate_matrix, variants

OLD_PROJECT_LABEL = "GradleMC Forge 1.20.1"
OLD_PROJECT_PATH = re.compile(
    r"[A-Za-z]:[\\/]+Users[\\/]+[^\\/]+[\\/]+Downloads[\\/]+MODs[\\/]+GradleMC Forge 1\.20\.1"
)
TEXT_SUFFIXES = {
    ".gradle",
    ".groovy",
    ".java",
    ".json",
    ".md",
    ".properties",
    ".ps1",
    ".py",
    ".sh",
    ".toml",
    ".txt",
    ".yml",
    ".yaml",
}
SKIP_PARTS = {".git", ".gradle", "build", "__pycache__", "run", "run-data"}
ALLOWED_OLD_LABEL_REFERENCES = {
    Path("AGENTS.md"),
    Path("tools/python/gradlemc_automation/check_claims.py"),
    Path("tools/python/gradlemc_automation/validate_release.py"),
}
ALLOWED_UPPERCASE_COMMAND_REFERENCES = {
    Path("build.gradle"),
    Path("tools/python/gradlemc_automation/check_command_casing.py"),
    Path("tools/python/gradlemc_automation/validate_release.py"),
    Path("tools/python/tests/test_automation.py"),
}
EXPECTED_RELEASE_ARTIFACT = "gradlemc-1.0.1-forge-1.20.1.jar"


def repo_relative(path: Path) -> str:
    return path.relative_to(ROOT).as_posix()


def read_properties(path: Path) -> dict[str, str]:
    values: dict[str, str] = {}
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        values[key.strip()] = value.strip()
    return values


def iter_text_files() -> list[Path]:
    files: list[Path] = []
    for path in ROOT.rglob("*"):
        if not path.is_file():
            continue
        if any(part in SKIP_PARTS for part in path.relative_to(ROOT).parts):
            continue
        if path.suffix.lower() in TEXT_SUFFIXES or path.name in {"gradlew", "gradlew.bat"}:
            files.append(path)
    return files


def line_number(text: str, offset: int) -> int:
    return text.count("\n", 0, offset) + 1


def check_required_files() -> list[str]:
    errors: list[str] = []
    required = [
        "build.gradle",
        "settings.gradle",
        "gradle.properties",
        "gradlew.bat",
        "gradle/wrapper/gradle-wrapper.properties",
        "config/gradlemc-variants.json",
        "src/main/java/com/soumyajit/gradlemc/GradleMC.java",
        "src/main/resources/META-INF/mods.toml",
        "src/main/resources/pack.mcmeta",
        "src/main/resources/GradleMC_logo.png",
        "src/main/resources/assets/gradlemc/lang/en_us.json",
    ]
    for relative in required:
        if not (ROOT / relative).exists():
            errors.append(f"missing required file: {relative}")
    return errors


def check_stale_paths() -> list[str]:
    errors: list[str] = []
    for path in iter_text_files():
        relative = path.relative_to(ROOT)
        text = path.read_text(encoding="utf-8", errors="replace")
        match = OLD_PROJECT_PATH.search(text)
        if match:
            errors.append(f"{repo_relative(path)}:{line_number(text, match.start())}: stale absolute old project path")
        index = text.find(OLD_PROJECT_LABEL)
        if index >= 0 and relative not in ALLOWED_OLD_LABEL_REFERENCES:
            errors.append(f"{repo_relative(path)}:{line_number(text, index)}: stale old project label")
    return errors


def check_command_casing() -> list[str]:
    errors: list[str] = []
    uppercase_command = re.compile(r"(?<![A-Za-z0-9_./-])/GradleMC(?:\s|$|[<`.,)])")
    uppercase_literal = re.compile(r'Commands\.literal\("GradleMC"\)')
    for path in iter_text_files():
        relative = path.relative_to(ROOT)
        text = path.read_text(encoding="utf-8", errors="replace")
        for pattern, label in (
            (uppercase_command, "uppercase /GradleMC command reference"),
            (uppercase_literal, "uppercase Brigadier command literal"),
        ):
            for match in pattern.finditer(text):
                if relative in ALLOWED_UPPERCASE_COMMAND_REFERENCES:
                    continue
                errors.append(f"{repo_relative(path)}:{line_number(text, match.start())}: {label}")
    return errors


def check_metadata() -> tuple[list[str], str]:
    errors: list[str] = []
    props = read_properties(ROOT / "gradle.properties")
    matrix = load_matrix(DEFAULT_MATRIX)
    errors.extend(validate_matrix(matrix))
    supported = [variant for variant in variants(matrix) if variant.get("enabled") and variant.get("buildable") and variant.get("status") == "supported"]
    if len(supported) != 1:
        errors.append(f"expected exactly one supported buildable variant, found {len(supported)}")
        return errors, props.get("artifact_name", "")

    variant = supported[0]
    expected_pairs = {
        "product_name": matrix.get("productName"),
        "mod_id": matrix.get("modId"),
        "mod_version": matrix.get("modVersion"),
        "minecraft_version": variant.get("minecraftVersion"),
        "mapping_version": variant.get("mappingsVersion"),
        "forge_version": variant.get("loaderVersion"),
        "loader_name": variant.get("loader"),
        "variant_name": variant.get("id"),
        "artifact_name": variant.get("artifactName"),
    }
    for key, expected in expected_pairs.items():
        if props.get(key) != expected:
            errors.append(f"gradle.properties {key} must be {expected!r}, found {props.get(key)!r}")

    if props.get("artifact_name") != EXPECTED_RELEASE_ARTIFACT:
        errors.append(f"release artifact must remain {EXPECTED_RELEASE_ARTIFACT}")

    gradlemc_java = (ROOT / "src/main/java/com/soumyajit/gradlemc/GradleMC.java").read_text(encoding="utf-8")
    required_java = {
        'public static final String MOD_ID = "gradlemc";',
        'public static final String CURRENT_MINECRAFT_VERSION = "1.20.1";',
        'public static final String CURRENT_VARIANT_ID = "forge-1.20.1";',
    }
    for snippet in required_java:
        if snippet not in gradlemc_java:
            errors.append(f"GradleMC.java missing metadata constant: {snippet}")

    mods_toml = (ROOT / "src/main/resources/META-INF/mods.toml").read_text(encoding="utf-8")
    for snippet in ('modId="${mod_id}"', 'version="${mod_version}"', 'displayName="${mod_name}"', 'logoFile="GradleMC_logo.png"'):
        if snippet not in mods_toml:
            errors.append(f"mods.toml missing expected metadata template: {snippet}")

    pack_mcmeta = (ROOT / "src/main/resources/pack.mcmeta").read_text(encoding="utf-8")
    if '"pack_format": 15' not in pack_mcmeta:
        errors.append("pack.mcmeta must keep pack_format 15 for Minecraft 1.20.1 resources")
    return errors, props.get("artifact_name", "")


def check_artifact(path: Path, expected_name: str) -> list[str]:
    errors: list[str] = []
    artifact = path if path.is_absolute() else ROOT / path
    if not artifact.exists():
        return [f"artifact not found: {artifact}"]
    if artifact.name != expected_name:
        errors.append(f"artifact name must be {expected_name}, found {artifact.name}")
    try:
        with zipfile.ZipFile(artifact) as jar:
            names = set(jar.namelist())
    except zipfile.BadZipFile:
        return errors + [f"artifact is not a valid jar/zip file: {artifact}"]

    required_entries = [
        "META-INF/mods.toml",
        "pack.mcmeta",
        "GradleMC_logo.png",
        "assets/gradlemc/lang/en_us.json",
        "com/soumyajit/gradlemc/GradleMC.class",
    ]
    for entry in required_entries:
        if entry not in names:
            errors.append(f"artifact missing required entry: {entry}")
    return errors


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Validate GradleMC release metadata, paths, casing, and optional jar contents.")
    parser.add_argument("--artifact", type=Path, help="Built or exported jar to inspect.")
    args = parser.parse_args(argv)

    if not (ROOT / "settings.gradle").exists() or not (ROOT / "gradle.properties").exists():
        print(f"ERROR: expected GradleMC repository root at {ROOT}", file=sys.stderr)
        return 1

    errors: list[str] = []
    errors.extend(check_required_files())
    errors.extend(check_stale_paths())
    errors.extend(check_command_casing())
    metadata_errors, expected_artifact = check_metadata()
    errors.extend(metadata_errors)
    if args.artifact:
        errors.extend(check_artifact(args.artifact, expected_artifact))

    if errors:
        print("GradleMC release validation failed:", file=sys.stderr)
        for error in errors:
            print(f"- {error}", file=sys.stderr)
        return 1

    if args.artifact:
        print(f"GradleMC release validation passed: {(args.artifact if args.artifact.is_absolute() else ROOT / args.artifact)}")
    else:
        print("GradleMC release validation passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
