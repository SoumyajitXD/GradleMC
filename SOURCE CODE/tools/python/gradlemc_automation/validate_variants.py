from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

from .variants import (
    DEFAULT_MATRIX,
    github_matrix,
    list_by_status,
    load_matrix,
    print_porting_plan,
    print_summary,
    print_variant_gaps,
    suggest_next_port,
    validate_matrix,
    variant_table,
    write_generated_outputs,
)


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Validate and print the GradleMC variant matrix.")
    parser.add_argument("--matrix", type=Path, default=DEFAULT_MATRIX)
    parser.add_argument("--print", action="store_true")
    parser.add_argument("--list-supported", action="store_true")
    parser.add_argument("--list-planned", action="store_true")
    parser.add_argument("--list-experimental", action="store_true")
    parser.add_argument("--github-matrix", action="store_true")
    parser.add_argument("--include-planned", action="store_true")
    parser.add_argument("--include-experimental", action="store_true")
    parser.add_argument("--only-variant")
    parser.add_argument("--readme-table", action="store_true")
    parser.add_argument("--write-generated", action="store_true")
    parser.add_argument("--porting-plan", action="store_true")
    parser.add_argument("--variant-gaps", action="store_true")
    parser.add_argument("--suggest-next-port", action="store_true")
    parser.add_argument("--check-docs", action="store_true", help="compatibility option; use check_claims for docs checks")
    args = parser.parse_args(argv)

    try:
        matrix = load_matrix(args.matrix)
    except Exception as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 1

    errors = validate_matrix(matrix)
    if errors:
        print("Variant matrix validation failed:", file=sys.stderr)
        for error in errors:
            print(f"- {error}", file=sys.stderr)
        return 1

    if args.write_generated:
        write_generated_outputs(matrix, args.include_planned, args.include_experimental)

    emitted = False
    if args.print:
        print_summary(matrix)
        emitted = True
    if args.list_supported:
        list_by_status(matrix, {"supported"})
        emitted = True
    if args.list_planned:
        list_by_status(matrix, {"planned"})
        emitted = True
    if args.list_experimental:
        list_by_status(matrix, {"experimental"})
        emitted = True
    if args.github_matrix:
        print(json.dumps(github_matrix(matrix, args.include_planned, args.include_experimental, args.only_variant), indent=2))
        emitted = True
    if args.readme_table:
        print(variant_table(matrix), end="")
        emitted = True
    if args.porting_plan:
        print_porting_plan()
        emitted = True
    if args.variant_gaps:
        print_variant_gaps(matrix)
        emitted = True
    if args.suggest_next_port:
        suggest_next_port()
        emitted = True
    if not emitted:
        print("Variant matrix validation passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
