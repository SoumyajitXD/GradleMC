from __future__ import annotations

import copy
import io
import sys
import unittest
from contextlib import redirect_stdout
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(ROOT / "tools" / "python"))

from gradlemc_automation import variants  # noqa: E402
from gradlemc_automation.check_command_casing import PATTERNS  # noqa: E402
from gradlemc_automation.check_claims import check_false_support_claims  # noqa: E402
from gradlemc_automation.validate_release import check_metadata, check_required_files  # noqa: E402


class AutomationTests(unittest.TestCase):
    def setUp(self) -> None:
        self.matrix = variants.load_matrix()

    def assert_has_error(self, matrix: dict, expected: str) -> None:
        errors = variants.validate_matrix(matrix)
        self.assertTrue(any(expected in error for error in errors), f"missing {expected!r} in {errors}")

    def test_manifest_parses_and_validates(self) -> None:
        self.assertEqual([], variants.validate_matrix(self.matrix))

    def test_duplicate_id_detection(self) -> None:
        matrix = copy.deepcopy(self.matrix)
        matrix["variants"].append(copy.deepcopy(matrix["variants"][0]))
        self.assert_has_error(matrix, "duplicate variant id")

    def test_artifact_naming_detection(self) -> None:
        matrix = copy.deepcopy(self.matrix)
        matrix["variants"][0]["artifactName"] = "gradlemc-latest.jar"
        self.assert_has_error(matrix, "artifactName must match")

    def test_github_matrix_defaults_to_enabled_only(self) -> None:
        payload = variants.github_matrix(self.matrix)
        ids = [entry["variant"] for entry in payload["include"]]
        self.assertEqual(["forge-1.20.1"], ids)

    def test_docs_table_contains_supported_variant(self) -> None:
        self.assertIn("`forge-1.20.1`", variants.variant_table(self.matrix))

    def test_command_casing_pattern_catches_uppercase_command(self) -> None:
        self.assertTrue(any(pattern.search("Run /GradleMC status") for pattern, _ in PATTERNS))

    def test_false_claim_checker_accepts_current_docs(self) -> None:
        self.assertEqual([], check_false_support_claims(self.matrix))

    def test_print_summary_is_readable(self) -> None:
        output = io.StringIO()
        with redirect_stdout(output):
            variants.print_summary(self.matrix)
        self.assertIn("GradleMC variant matrix", output.getvalue())

    def test_release_metadata_matches_current_supported_artifact(self) -> None:
        errors, artifact_name = check_metadata()
        self.assertEqual([], errors)
        self.assertEqual("gradlemc-1.0.1-forge-1.20.1.jar", artifact_name)

    def test_required_release_files_exist(self) -> None:
        self.assertEqual([], check_required_files())


if __name__ == "__main__":
    unittest.main()
