from __future__ import annotations

import argparse
from pathlib import Path

from .variants import DEFAULT_MATRIX, GENERATED_DIR, ROOT, load_matrix, validate_matrix, variant_table


def replace_marked_section(text: str, start: str, end: str, replacement: str) -> str:
    if start not in text or end not in text:
        raise ValueError(f"missing markers {start!r} and {end!r}")
    before, rest = text.split(start, 1)
    _, after = rest.split(end, 1)
    return f"{before}{start}\n{replacement.rstrip()}\n{end}{after}"


def main() -> int:
    parser = argparse.ArgumentParser(description="Generate GradleMC docs tables from the variant manifest.")
    parser.add_argument("--matrix", type=Path, default=DEFAULT_MATRIX)
    parser.add_argument("--output", type=Path, default=GENERATED_DIR / "variant-table.md")
    parser.add_argument("--update-docs", action="store_true")
    args = parser.parse_args()

    matrix = load_matrix(args.matrix)
    errors = validate_matrix(matrix)
    if errors:
        for error in errors:
            print(f"ERROR: {error}")
        return 1
    table = variant_table(matrix)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(table, encoding="utf-8")
    print(f"Wrote {args.output}")

    if args.update_docs:
        docs_path = ROOT / "docs" / "PORTING_MATRIX.md"
        text = docs_path.read_text(encoding="utf-8")
        updated = replace_marked_section(text, "<!-- gradlemc:variant-table:start -->", "<!-- gradlemc:variant-table:end -->", table)
        docs_path.write_text(updated, encoding="utf-8")
        print(f"Updated {docs_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

