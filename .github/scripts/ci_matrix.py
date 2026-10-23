#!/usr/bin/env python3
"""Fail-closed affected-target selection for GradleMC CI."""
import json
import os
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


def load_projects():
    projects = json.loads((ROOT / ".github/ci/targets.json").read_text(encoding="utf-8"))["projects"]
    if len(projects) != 8 or len({p["project_dir"] for p in projects}) != 8:
        raise ValueError("Expected eight unique maintained Gradle projects")
    for p in projects:
        if not p["project_dir"].startswith("GradleMC/") or p["kind"] not in ("forge", "fabric", "neoforge") or p["java"] not in ("17", "21", "25") or not p["command"]:
            raise ValueError("Invalid target: " + repr(p))
    return projects


def changed_in_pr(event):
    """Return None to force full build when a trustworthy PR diff is unavailable."""
    try:
        base = event["pull_request"]["base"]["sha"]
        head = event["pull_request"]["head"]["sha"]
        common = subprocess.run(["git", "merge-base", base, head], check=True, capture_output=True, text=True).stdout.strip()
        if not common:
            return None
        result = subprocess.run(
            ["git", "diff", "--name-only", "--diff-filter=ACDMRT", common, head],
            check=True, capture_output=True, text=True
        )
        return result.stdout.splitlines()
    except (KeyError, TypeError, ValueError, OSError, subprocess.CalledProcessError):
        return None


def documentation_only(path):
    if path == "AGENTS.md":
        return False
    if path.startswith(("docs/", "Screenshots/")):
        return True
    return "/" not in path and (path.endswith((".md", ".png", ".jpg", ".jpeg", ".webp")) or path == "curseforge-description.html")


def choose(projects, event_name, changed):
    if event_name != "pull_request":
        return projects, "full matrix: " + event_name
    if not changed:
        return projects, "missing PR diff: full matrix"
    if any(path.startswith(".github/") for path in changed):
        return projects, "CI changed: full matrix"
    selected = [p for p in projects if any(f.startswith(p["project_dir"] + "/") for f in changed)]
    unknown = [f for f in changed if not documentation_only(f) and not any(
        f.startswith(p["project_dir"] + "/") for p in projects)]
    if unknown:
        return projects, "unclassified change: full matrix"
    if selected:
        return selected, "changed Gradle targets"
    return [], "documentation-only PR"


def main():
    projects = load_projects()
    event = json.loads(Path(os.environ["GITHUB_EVENT_PATH"]).read_text(encoding="utf-8"))
    name = os.environ["GITHUB_EVENT_NAME"]
    selected, reason = choose(projects, name, changed_in_pr(event) if name == "pull_request" else None)
    outputs = {
        "matrix": json.dumps({"include": selected}, separators=(",", ":")),
        "has-projects": str(bool(selected)).lower(),
        "selected-count": str(len(selected)),
        "expected-count": str(len(projects))
    }
    with Path(os.environ["GITHUB_OUTPUT"]).open("a", encoding="utf-8") as f:
        for k, v in outputs.items():
            f.write(k + "=" + v + "\n")
    summary = ["### GradleMC target selection", "", "- Event: " + name,
               "- Reason: " + reason, "- Selected: " + str(len(selected)) + "/8",
               "", "| Target | Java | OS |", "| --- | --- | --- |"]
    summary.extend("| " + p["name"] + " | " + p["java"] + " | " + p["runner"] + " |" for p in selected)
    with Path(os.environ["GITHUB_STEP_SUMMARY"]).open("a", encoding="utf-8") as f:
        f.write("\n".join(summary) + "\n")
    print(reason)
    for p in selected:
        print(p["name"], p["command"])


if __name__ == "__main__":
    sys.exit(main())
