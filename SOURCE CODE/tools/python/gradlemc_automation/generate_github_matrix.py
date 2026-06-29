from __future__ import annotations

import argparse
import json
from pathlib import Path

from .variants import DEFAULT_MATRIX, GENERATED_DIR, github_matrix, load_matrix, validate_matrix


def main() -> int:
    parser = argparse.ArgumentParser(description="Generate the GradleMC GitHub Actions matrix.")
    parser.add_argument("--matrix", type=Path, default=DEFAULT_MATRIX)
    parser.add_argument("--include-planned", action="store_true")
    parser.add_argument("--include-experimental", action="store_true")
    parser.add_argument("--only-variant")
    parser.add_argument("--output", type=Path, default=GENERATED_DIR / "github-matrix.json")
    args = parser.parse_args()

    matrix = load_matrix(args.matrix)
    errors = validate_matrix(matrix)
    if errors:
        for error in errors:
            print(f"ERROR: {error}")
        return 1
    payload = github_matrix(matrix, args.include_planned, args.include_experimental, args.only_variant)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(payload, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

