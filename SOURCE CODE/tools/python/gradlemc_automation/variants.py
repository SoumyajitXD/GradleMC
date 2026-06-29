from __future__ import annotations

import json
import os
import re
import shutil
import subprocess
import sys
from collections import Counter
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Iterable

ROOT = Path(__file__).resolve().parents[3]
DEFAULT_MATRIX = ROOT / "config" / "gradlemc-variants.json"
GENERATED_DIR = ROOT / "build" / "generated" / "gradlemc"
REPORT_DIR = ROOT / "build" / "reports" / "gradlemc"

ALLOWED_ROOT_FIELDS = {
    "$schema",
    "productName",
    "modId",
    "modVersion",
    "artifactPattern",
    "defaultOutputPath",
    "variants",
}
ALLOWED_VARIANT_FIELDS = {
    "id",
    "minecraftVersion",
    "loader",
    "javaVersion",
    "status",
    "enabled",
    "publish",
    "buildable",
    "runClientTask",
    "runServerTask",
    "artifactName",
    "outputPath",
    "loaderVersion",
    "mappingsVersion",
    "gradlePluginKind",
    "notes",
    "reasonDisabled",
    "supportGate",
}
ALLOWED_LOADERS = {"forge", "neoforge", "fabric"}
ALLOWED_STATUSES = {"supported", "planned", "experimental", "unsupported", "needs-verification", "deprecated"}
ALLOWED_PLUGIN_KINDS = {"forgegradle", "fabric-loom", "fabric-loom-remap", "moddevgradle", "neogradle", "unknown"}
SUPPORT_GATE_KEYS = {
    "adapter",
    "buildConfig",
    "clientLaunch",
    "serverLaunch",
    "commandSmokeTest",
    "guiSmokeTest",
    "dedicatedServerSideSafe",
    "reportsPathVerified",
    "ciBuild",
    "docsHonest",
}

ID_PATTERN = re.compile(r"^(forge|fabric|neoforge)-([0-9]+(?:\.[0-9]+){1,2})$")
ARTIFACT_PATTERN = re.compile(r"^gradlemc-[A-Za-z0-9_.+-]+-(forge|neoforge|fabric)-[^\\/]+\.jar$")


class MatrixError(Exception):
    pass


@dataclass(frozen=True)
class CommandResult:
    ok: bool
    name: str
    detail: str


def repo_path(path: str | Path) -> Path:
    value = Path(path)
    return value if value.is_absolute() else ROOT / value


def load_matrix(path: str | Path = DEFAULT_MATRIX) -> dict[str, Any]:
    matrix_path = repo_path(path)
    try:
        return json.loads(matrix_path.read_text(encoding="utf-8"))
    except FileNotFoundError as exc:
        raise MatrixError(f"{matrix_path}: file not found") from exc
    except json.JSONDecodeError as exc:
        raise MatrixError(f"{matrix_path}: invalid JSON at line {exc.lineno}, column {exc.colno}: {exc.msg}") from exc


def variants(matrix: dict[str, Any]) -> list[dict[str, Any]]:
    values = matrix.get("variants")
    if not isinstance(values, list):
        return []
    return [value for value in values if isinstance(value, dict)]


def version_tuple(version: str) -> tuple[int, ...]:
    return tuple(int(part) for part in version.split("."))


def is_26_or_newer(version: str) -> bool:
    parts = version_tuple(version)
    return bool(parts and parts[0] >= 26)


def selected_variants(
    matrix: dict[str, Any],
    include_planned: bool = False,
    include_experimental: bool = False,
    only_variant: str | None = None,
) -> list[dict[str, Any]]:
    selected: list[dict[str, Any]] = []
    for variant in variants(matrix):
        if variant.get("enabled"):
            selected.append(variant)
        elif include_planned and variant.get("status") == "planned":
            selected.append(variant)
        elif include_experimental and variant.get("status") == "experimental":
            selected.append(variant)
    if only_variant:
        selected = [variant for variant in selected if variant.get("id") == only_variant]
    return selected


def list_unknown_fields(matrix: dict[str, Any]) -> list[str]:
    unknown: list[str] = []
    unknown.extend(f"root.{field}" for field in sorted(set(matrix) - ALLOWED_ROOT_FIELDS))
    for index, raw_variant in enumerate(matrix.get("variants", [])):
        if not isinstance(raw_variant, dict):
            continue
        label = raw_variant.get("id", f"#{index}")
        unknown.extend(f"variants[{label}].{field}" for field in sorted(set(raw_variant) - ALLOWED_VARIANT_FIELDS))
    return unknown


def validate_matrix(matrix: dict[str, Any]) -> list[str]:
    errors: list[str] = []
    raw_variants = matrix.get("variants")
    if not isinstance(raw_variants, list) or not raw_variants:
        return ["root.variants must be a non-empty list"]

    if matrix.get("productName") != "GradleMC":
        errors.append("root.productName must be GradleMC")
    if matrix.get("modId") != "gradlemc":
        errors.append("root.modId must be gradlemc")

    errors.extend(f"unknown field: {field}" for field in list_unknown_fields(matrix))
    matrix_variants = variants(matrix)
    ids = [variant.get("id") for variant in matrix_variants]
    for variant_id, count in Counter(ids).items():
        if variant_id and count > 1:
            errors.append(f"duplicate variant id: {variant_id}")

    artifact_names = [variant.get("artifactName") for variant in matrix_variants if variant.get("buildable") or variant.get("publish")]
    for artifact, count in Counter(artifact_names).items():
        if artifact and count > 1:
            errors.append(f"duplicate buildable/publishable artifactName: {artifact}")

    mod_version = matrix.get("modVersion")
    artifact_pattern = matrix.get("artifactPattern", "gradlemc-{modVersion}-{loader}-{minecraftVersion}.jar")

    for index, raw_variant in enumerate(raw_variants):
        label = raw_variant.get("id", f"#{index}") if isinstance(raw_variant, dict) else f"#{index}"
        if not isinstance(raw_variant, dict):
            errors.append(f"variant {label} must be an object")
            continue

        required = [
            "id",
            "minecraftVersion",
            "loader",
            "javaVersion",
            "status",
            "enabled",
            "publish",
            "buildable",
            "artifactName",
            "outputPath",
            "gradlePluginKind",
            "notes",
            "reasonDisabled",
            "supportGate",
        ]
        for field in required:
            if field not in raw_variant or (raw_variant[field] in (None, "") and field != "reasonDisabled"):
                errors.append(f"{label}: {field} is required")

        variant_id = raw_variant.get("id")
        loader = raw_variant.get("loader")
        status = raw_variant.get("status")
        enabled = raw_variant.get("enabled")
        publish = raw_variant.get("publish")
        buildable = raw_variant.get("buildable")
        java_version = raw_variant.get("javaVersion")
        artifact = raw_variant.get("artifactName", "")
        minecraft_version = raw_variant.get("minecraftVersion")
        plugin_kind = raw_variant.get("gradlePluginKind")
        support_gate = raw_variant.get("supportGate")

        if loader not in ALLOWED_LOADERS:
            errors.append(f"{label}: loader must be one of {', '.join(sorted(ALLOWED_LOADERS))}")
        if status not in ALLOWED_STATUSES:
            errors.append(f"{label}: status must be one of {', '.join(sorted(ALLOWED_STATUSES))}")
        if plugin_kind not in ALLOWED_PLUGIN_KINDS:
            errors.append(f"{label}: gradlePluginKind must be one of {', '.join(sorted(ALLOWED_PLUGIN_KINDS))}")
        if not isinstance(java_version, int):
            errors.append(f"{label}: javaVersion must be an integer")
        if not isinstance(enabled, bool):
            errors.append(f"{label}: enabled must be true or false")
        if not isinstance(publish, bool):
            errors.append(f"{label}: publish must be true or false")
        if not isinstance(buildable, bool):
            errors.append(f"{label}: buildable must be true or false")
        if not isinstance(support_gate, dict):
            errors.append(f"{label}: supportGate must be an object")
        elif status == "supported":
            unknown_gate_keys = sorted(set(support_gate) - SUPPORT_GATE_KEYS)
            if unknown_gate_keys:
                errors.append(f"{label}: supportGate has unknown keys: {', '.join(unknown_gate_keys)}")

        if variant_id and not ID_PATTERN.match(str(variant_id)):
            errors.append(f"{label}: id must match <loader>-<minecraftVersion>")
        if variant_id and loader and minecraft_version and variant_id != f"{loader}-{minecraft_version}":
            errors.append(f"{label}: id must equal {loader}-{minecraft_version}")
        if publish and not enabled:
            errors.append(f"{label}: publish cannot be true when enabled is false")
        if publish and not buildable:
            errors.append(f"{label}: publish cannot be true when buildable is false")
        if status == "unsupported" and enabled:
            errors.append(f"{label}: unsupported variants cannot be enabled")
        if status in {"experimental", "needs-verification"} and publish:
            errors.append(f"{label}: {status} variants cannot publish")
        if enabled and plugin_kind == "unknown":
            errors.append(f"{label}: enabled variants must have a known gradlePluginKind")
        if not enabled and not raw_variant.get("reasonDisabled"):
            errors.append(f"{label}: disabled variants must explain reasonDisabled")
        if status == "supported" and not enabled:
            errors.append(f"{label}: supported variants must be enabled")
        if enabled and not buildable:
            errors.append(f"{label}: enabled variants must be buildable")

        if artifact and not ARTIFACT_PATTERN.match(artifact):
            errors.append(f"{label}: artifactName must match gradlemc-<modVersion>-<loader>-<minecraftVersion>.jar")
        if loader and artifact and f"-{loader}-" not in artifact:
            errors.append(f"{label}: artifactName does not include loader {loader}")
        if minecraft_version and artifact and not artifact.endswith(f"-{minecraft_version}.jar"):
            errors.append(f"{label}: artifactName does not end with minecraftVersion {minecraft_version}.jar")
        if mod_version and loader and minecraft_version and artifact:
            expected = artifact_pattern.format(
                modVersion=mod_version,
                loader=loader,
                minecraftVersion=minecraft_version,
                id=variant_id or "",
            )
            if artifact != expected:
                errors.append(f"{label}: artifactName should be {expected}")

        if loader == "fabric" and isinstance(minecraft_version, str):
            if is_26_or_newer(minecraft_version) and plugin_kind != "fabric-loom":
                errors.append(f"{label}: 26.1+ Fabric variants should use fabric-loom unless re-verified")
            if not is_26_or_newer(minecraft_version) and plugin_kind != "fabric-loom-remap":
                errors.append(f"{label}: 1.21.11-or-older Fabric variants should use fabric-loom-remap unless re-verified")
        if loader == "neoforge" and isinstance(minecraft_version, str):
            if version_tuple(minecraft_version) < (1, 20, 2) and status not in {"unsupported", "needs-verification"}:
                errors.append(f"{label}: NeoForge variants below verified support must stay unsupported or needs-verification")
    return errors


def github_matrix(
    matrix: dict[str, Any],
    include_planned: bool = False,
    include_experimental: bool = False,
    only_variant: str | None = None,
) -> dict[str, list[dict[str, Any]]]:
    return {
        "include": [
            {
                "variant": variant["id"],
                "minecraft": variant["minecraftVersion"],
                "loader": variant["loader"],
                "java": variant["javaVersion"],
                "buildable": variant["buildable"],
                "status": variant["status"],
                "artifactName": variant["artifactName"],
            }
            for variant in selected_variants(matrix, include_planned, include_experimental, only_variant)
        ]
    }


def variant_table(matrix: dict[str, Any]) -> str:
    lines = [
        "| Variant | Minecraft | Loader | Java | Status | Enabled | Buildable |",
        "| --- | --- | --- | --- | --- | --- | --- |",
    ]
    for variant in variants(matrix):
        lines.append(
            f"| `{variant['id']}` | `{variant['minecraftVersion']}` | {variant['loader']} | "
            f"{variant['javaVersion']} | {variant['status']} | {str(variant['enabled']).lower()} | "
            f"{str(variant['buildable']).lower()} |"
        )
    return "\n".join(lines) + "\n"


def gap_list(variant: dict[str, Any]) -> list[str]:
    support_gate = variant.get("supportGate") if isinstance(variant.get("supportGate"), dict) else {}
    gaps: list[str] = []
    if not support_gate.get("adapter"):
        gaps.append(f"missing {variant['loader']} adapter/build config")
    if not support_gate.get("buildConfig"):
        gaps.append("missing build config")
    if not support_gate.get("clientLaunch"):
        gaps.append("missing client launch verification")
    if not support_gate.get("serverLaunch"):
        gaps.append("missing server launch verification")
    if not support_gate.get("docsHonest"):
        gaps.append("missing docs honesty gate")
    if not support_gate.get("ciBuild"):
        gaps.append("missing CI build gate")
    return gaps


def print_variant_gaps(matrix: dict[str, Any]) -> None:
    for variant in variants(matrix):
        print(f"{variant['id']}:")
        gaps = gap_list(variant)
        if gaps:
            for gap in gaps:
                print(f"  - {gap}")
        else:
            print("  - no scaffold gaps recorded")


def print_porting_plan() -> None:
    print("GradleMC porting plan")
    print("1. Keep forge-1.20.1 as the only supported/buildable target until runtime smoke tests are refreshed.")
    print("2. Extract pure Java scoring, profiler math, report models, and serialization into common-core.")
    print("3. Add a minecraft-common bridge for gameDir, loaded mods, tick samples, FPS samples, command feedback, and report paths.")
    print("4. Create one loader adapter at a time; do not duplicate the full Forge project per target.")
    print("5. Promote candidates only after build, client launch, server launch, command, GUI, report path, docs, and CI gates pass.")
    print("")
    print("Recommended target queue:")
    print("- fabric-1.20.1")
    print("- fabric-1.21.1")
    print("- neoforge-1.21.1")
    print("- 26.x only after official loader/tooling data is refreshed")


def suggest_next_port() -> None:
    print("Suggested next port: fabric-1.20.1")
    print("Reasons:")
    print("- It stays on Java 17, matching the current Forge 1.20.1 code.")
    print("- It tests loader-adapter boundaries without taking the Java 21 API jump at the same time.")
    print("- It is disabled and not buildable today, so it requires common-core and bridge work before promotion.")


def print_summary(matrix: dict[str, Any]) -> None:
    all_variants = variants(matrix)
    enabled = [variant for variant in all_variants if variant.get("enabled")]
    supported = [variant for variant in enabled if variant.get("status") == "supported"]
    print("GradleMC variant matrix")
    print(f"Product: {matrix.get('productName', 'GradleMC')}")
    print(f"Matrix: {DEFAULT_MATRIX.relative_to(ROOT)}")
    print(f"Total variants: {len(all_variants)}")
    print(f"Enabled variants: {len(enabled)}")
    print(f"Supported variants: {len(supported)}")
    print("")
    for variant in all_variants:
        marker = "ENABLED" if variant.get("enabled") else "disabled"
        build = "buildable" if variant.get("buildable") else "not-buildable"
        publish = "publish" if variant.get("publish") else "no-publish"
        print(
            f"- {variant['id']}: mc {variant['minecraftVersion']} / {variant['loader']} / "
            f"Java {variant['javaVersion']} / {variant['status']} / {marker} / {build} / {publish}"
        )
        if variant.get("reasonDisabled"):
            print(f"  disabled: {variant['reasonDisabled']}")


def list_by_status(matrix: dict[str, Any], statuses: Iterable[str]) -> None:
    wanted = set(statuses)
    for variant in variants(matrix):
        if variant.get("status") in wanted:
            print(
                f"{variant['id']} mc={variant['minecraftVersion']} loader={variant['loader']} "
                f"java={variant['javaVersion']} enabled={variant['enabled']} buildable={variant['buildable']}"
            )


def write_generated_outputs(matrix: dict[str, Any], include_planned: bool = False, include_experimental: bool = False) -> None:
    GENERATED_DIR.mkdir(parents=True, exist_ok=True)
    REPORT_DIR.mkdir(parents=True, exist_ok=True)
    (GENERATED_DIR / "variant-matrix.json").write_text(json.dumps(matrix, indent=2) + "\n", encoding="utf-8")
    (GENERATED_DIR / "github-matrix.json").write_text(
        json.dumps(github_matrix(matrix, include_planned, include_experimental), indent=2) + "\n",
        encoding="utf-8",
    )
    (GENERATED_DIR / "variant-table.md").write_text(variant_table(matrix), encoding="utf-8")
    (REPORT_DIR / "automation-report.txt").write_text(automation_report(matrix), encoding="utf-8")


def automation_report(matrix: dict[str, Any]) -> str:
    by_java: dict[int, list[str]] = {}
    for variant in variants(matrix):
        by_java.setdefault(int(variant["javaVersion"]), []).append(variant["id"])
    lines = [
        "GradleMC automation report",
        "==========================",
        "",
        f"Product: {matrix.get('productName')}",
        f"Supported variants: {', '.join(v['id'] for v in variants(matrix) if v.get('status') == 'supported')}",
        f"Buildable variants: {', '.join(v['id'] for v in variants(matrix) if v.get('buildable'))}",
        "",
        "Java requirements:",
    ]
    for java_version in sorted(by_java):
        lines.append(f"- Java {java_version}: {', '.join(by_java[java_version])}")
    lines.extend(
        [
            "",
            "Language roles:",
            "- Java: Minecraft mod and common-core logic.",
            "- Gradle: primary build orchestrator and variant task entrypoint.",
            "- Python 3.12+: manifest validation, matrix generation, docs tables, and checks.",
            "- PowerShell 7: Windows-first wrapper commands.",
            "- Node/TypeScript: docs and optional web-facing asset validation when package.json exists.",
            "- Kotlin: Gradle build-logic helpers only; not mod runtime code.",
            "",
        ]
    )
    return "\n".join(lines)


def run_tool(command: list[str]) -> CommandResult:
    try:
        completed = subprocess.run(command, cwd=ROOT, text=True, capture_output=True, timeout=15, check=False)
    except FileNotFoundError:
        return CommandResult(False, command[0], "not found on PATH")
    except subprocess.TimeoutExpired:
        return CommandResult(False, command[0], "timed out")
    output = (completed.stdout or completed.stderr).strip().splitlines()
    detail = output[0] if output else f"exit code {completed.returncode}"
    return CommandResult(completed.returncode == 0, command[0], detail)


def check_automation_tools(require_node: bool = False) -> list[str]:
    errors: list[str] = []
    if sys.version_info < (3, 12):
        errors.append(f"Python 3.12+ is required; running {sys.version.split()[0]}")

    checks = [
        CommandResult(True, "python", f"{sys.version.split()[0]} ({sys.executable})"),
        run_tool(["java", "-version"]),
        run_tool(["pwsh", "-NoLogo", "-NoProfile", "-Command", "$PSVersionTable.PSVersion.ToString()"]),
    ]
    if (ROOT / "gradlew.bat").exists() or (ROOT / "gradlew").exists():
        checks.append(CommandResult(True, "gradle-wrapper", "found"))
    else:
        checks.append(CommandResult(False, "gradle-wrapper", "gradlew/gradlew.bat not found"))

    node_path = shutil.which("node")
    if node_path:
        checks.append(run_tool(["node", "--version"]))
    elif require_node:
        checks.append(CommandResult(False, "node", "package.json exists but Node.js was not found"))
    else:
        checks.append(CommandResult(True, "node", "not required; package.json is absent"))

    npm_command = "npm.cmd" if os.name == "nt" else "npm"
    npm_path = shutil.which(npm_command) or shutil.which("npm")
    if require_node and npm_path:
        checks.append(run_tool([npm_command, "--version"]))
    elif require_node:
        checks.append(CommandResult(False, "npm", "package.json exists but npm was not found"))

    for check in checks:
        status = "OK" if check.ok else "ERROR"
        print(f"{status}: {check.name}: {check.detail}")
        if not check.ok:
            errors.append(f"{check.name}: {check.detail}")
    print(f"JAVA_HOME: {os.environ.get('JAVA_HOME', '<not set>')}")
    return errors


def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8", errors="replace")
