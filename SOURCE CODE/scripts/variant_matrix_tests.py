#!/usr/bin/env python3
from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools" / "python"))

if __name__ == "__main__":
    raise SystemExit(unittest.main(module="tests.test_automation", argv=[sys.argv[0]]))
