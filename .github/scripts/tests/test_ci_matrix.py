"""Regression tests for the GitHub Actions build selector."""
import sys
import unittest
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from ci_matrix import load_projects, choose, documentation_only

class TestSelection(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.projects = load_projects()

    def test_supported_count(self):
        self.assertEqual(len(self.projects), 8)

    def test_all_on_main_schedule_dispatch_merge_queue(self):
        for event in ("push", "schedule", "workflow_dispatch", "merge_group"):
            with self.subTest(event=event):
                self.assertEqual(len(choose(self.projects, event, ["README.md"])[0]), 8)

    def test_pr_changes_select_single_target(self):
        selected, _ = choose(self.projects, "pull_request", [
            "GradleMC/Fabric/Minecraft 1.20.1/src/main/kotlin/Example.kt", "docs/CI.md"])
        self.assertEqual([p["name"] for p in selected], ["Fabric 1.20.1"])

    def test_pr_docs_only_skip(self):
        selected, reason = choose(self.projects, "pull_request", [
            "README.md", "docs/CI.md", "Screenshots/1.png"])
        self.assertEqual(selected, [])
        self.assertIn("documentation-only", reason)

    def test_pr_ci_change_forces_full(self):
        self.assertEqual(len(choose(self.projects, "pull_request", [".github/workflows/ci.yml"])[0]), 8)

    def test_unknown_or_unreliable_diff_forces_full(self):
        for paths in (None, [], ["some/new/build.gradle"], ["AGENTS.md"]):
            with self.subTest(paths=paths):
                self.assertEqual(len(choose(self.projects, "pull_request", paths)[0]), 8)

    def test_deleted_target_path_selects_target(self):
        selected, _ = choose(self.projects, "pull_request", [
            "GradleMC/NeoForge/Minecraft 26.1.2/src/main/java/Deleted.java"])
        self.assertEqual([p["name"] for p in selected], ["NeoForge 26.1.2"])

    def test_agency_file_is_not_documentation_only(self):
        self.assertFalse(documentation_only("AGENTS.md"))

if __name__ == "__main__":
    unittest.main()
