#!/usr/bin/env python3
"""Regression tests for PostgreSQL mapping dialect detection."""

from __future__ import annotations

import importlib.util
import sys
import unittest
from pathlib import Path


SCRIPT_PATH = Path(__file__).with_name("audit-postgres-mappings.py")
SPEC = importlib.util.spec_from_file_location("audit_postgres_mappings", SCRIPT_PATH)
if SPEC is None or SPEC.loader is None:
    raise RuntimeError(f"cannot load {SCRIPT_PATH}")
AUDITOR = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = AUDITOR
SPEC.loader.exec_module(AUDITOR)


class PostgresMappingAuditTest(unittest.TestCase):
    def patterns(self, text: str) -> set[str]:
        return {item["pattern"] for item in AUDITOR.scan_text("sample.sql", text, 10)}

    def test_oracle_rownum_and_number_types_remain_blocking(self):
        findings = self.patterns(
            "select * from sample where rownum <= 1;\n"
            "create table sample_upper (amount NUMBER(12, 2));\n"
            "select * from sample_upper where ROWNUM <= 1;\n"
            "create table sample_lower (amount number(12, 2));\n"
        )

        self.assertIn("oracle-rownum", findings)
        self.assertIn("oracle-number", findings)

    def test_javascript_row_num_and_number_constructor_are_not_sql(self):
        findings = self.patterns(
            "function valueOnchange(value, rowNum) {\n"
            "  value = Number(value).toFixed(2);\n"
            "  table.setValueByKey(rowNum, 'defaultVal', value);\n"
            "}\n"
        )

        self.assertNotIn("oracle-rownum", findings)
        self.assertNotIn("oracle-number", findings)


if __name__ == "__main__":
    unittest.main()
