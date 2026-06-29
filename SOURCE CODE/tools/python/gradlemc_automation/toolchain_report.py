from __future__ import annotations

import argparse
import os
import platform
from pathlib import Path

from .variants import DEFAULT_MATRIX, check_automation_tools, load_matrix, variants


def main() -> int:
    parser = argparse.ArgumentParser(description="Report GradleMC local automation tool availability.")
    parser.add_argument("--matrix", type=Path, default=DEFAULT_MATRIX)
    parser.add_argument("--check-tools", action="store_true")
    args = parser.parse_args()

    matrix = load_matrix(args.matrix)
    print("GradleMC automation tools")
    print(f"Platform: {platform.platform()}")
    print(f"JAVA_HOME: {os.environ.get('JAVA_HOME', '<not set>')}")
    print("")
    print("Variant Java requirements:")
    for variant in variants(matrix):
        print(f"- {variant['id']}: Java {variant['javaVersion']} ({variant['status']})")
    print("")
    errors = check_automation_tools(require_node=(Path("package.json").exists()))
    return 1 if errors and args.check_tools else 0


if __name__ == "__main__":
    raise SystemExit(main())

