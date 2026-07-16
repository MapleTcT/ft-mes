#!/usr/bin/env python3
"""Regression tests for GreenDill module i18n JavaScript generation."""

from __future__ import annotations

import importlib.util
import sys
import tempfile
import unittest
from pathlib import Path


SCRIPT_PATH = Path(__file__).with_name("generate-module-i18n-js.py")
SPEC = importlib.util.spec_from_file_location("module_i18n_generator", SCRIPT_PATH)
if SPEC is None or SPEC.loader is None:
    raise RuntimeError(f"cannot load {SCRIPT_PATH}")
GENERATOR = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = GENERATOR
SPEC.loader.exec_module(GENERATOR)


class ModuleI18nGeneratorTest(unittest.TestCase):
    def test_parses_java_escapes_comments_and_continuations(self):
        resources = GENERATOR.parse_properties(
            "# generated\n"
            "PATROL.inputStandard.InputStandard.code=录入标准编码\n"
            "escaped\\:key=值\\:一\\!\n"
            "continued=第一段\\\n"
            "  第二段\n"
            "unicode=\\u5de1\\u68c0\n"
        )

        self.assertEqual("录入标准编码", resources["PATROL.inputStandard.InputStandard.code"])
        self.assertEqual("值:一!", resources["escaped:key"])
        self.assertEqual("第一段第二段", resources["continued"])
        self.assertEqual("巡检", resources["unicode"])

    def test_renders_safe_javascript_assignments(self):
        script = GENERATOR.render_javascript(
            {"PATROL.label": "巡检\"标准", "line": "a\nb"},
            "PATROL_zh_CN.properties",
            "PATROL",
        )

        self.assertIn("window.InternationalResource = window.InternationalResource || {};", script)
        self.assertIn('["PATROL.label"] = "巡检\\\"标准";', script)
        self.assertIn('["line"] = "a\\nb";', script)
        self.assertIn("getLanguageObjData", script)
        self.assertIn("getTextWithModuleFallback", script)
        self.assertIn("__adpModuleI18nObserver_PATROL", script)

    def test_primary_bundle_overrides_compatibility_fallback(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            fallback = root / "platform.properties"
            primary = root / "PATROL.properties"
            fallback.write_text("shared=platform\nplatform.only=确认\n", encoding="utf-8")
            primary.write_text("shared=module\nPATROL.only=巡检\n", encoding="utf-8")

            resources, source_name = GENERATOR.merge_properties(primary, [fallback])

        self.assertEqual("module", resources["shared"])
        self.assertEqual("确认", resources["platform.only"])
        self.assertEqual("platform.properties + PATROL.properties", source_name)


if __name__ == "__main__":
    unittest.main()
