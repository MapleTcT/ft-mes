#!/usr/bin/env python3
"""Regression checks for PostgreSQL workflow XML large-object compatibility."""

from __future__ import annotations

import importlib.util
import sys
import unittest
from pathlib import Path


SCRIPT_DIR = Path(__file__).resolve().parent
ROOT = SCRIPT_DIR.parents[2]
PATCHER_PATH = SCRIPT_DIR / "patch-configuration-entity-model-runtime.py"
PROCESS_SERVICE_PATH = (
    ROOT
    / "backend/modules/com/supcon/supfusion/configuration/configuration-workflow/1.0.0-SNAPSHOT"
    / "com/supcon/supfusion/configuration/workflow/service/impl/ProcessServiceFlowImpl.java"
)

SPEC = importlib.util.spec_from_file_location(
    "patch_configuration_entity_model_runtime_workflow_test", PATCHER_PATH
)
if SPEC is None or SPEC.loader is None:
    raise RuntimeError(f"cannot load {PATCHER_PATH}")
PATCHER = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = PATCHER
SPEC.loader.exec_module(PATCHER)


class ConfigurationWorkflowPostgresCompatTest(unittest.TestCase):
    def test_workflow_xml_oid_is_decoded_before_dom_parsing(self) -> None:
        source = PROCESS_SERVICE_PATH.read_text(encoding="utf-8")
        handle_start = source.index("public String handleFlowXml(String flowXml)")
        parse_start = source.index("DocumentHelper.parseText(flowXml)", handle_start)
        resolver_call = source.index("flowXml = resolvePostgresLargeObject(flowXml)", handle_start)

        self.assertLess(resolver_call, parse_start)
        self.assertIn('if (!oid.matches("\\\\d+"))', source)
        self.assertIn(
            '"SELECT convert_from(lo_get(CAST(? AS oid)), \'UTF8\')"',
            source,
        )
        self.assertIn("new Object[]{Long.valueOf(oid)}", source)

    def test_runtime_patcher_replaces_workflow_service_class(self) -> None:
        self.assertIn(PATCHER.WORKFLOW_JAR, PATCHER.PATCH_TARGETS)
        self.assertEqual(
            [
                "com/supcon/supfusion/configuration/workflow/service/impl/"
                "ProcessServiceFlowImpl.class"
            ],
            PATCHER.PATCH_TARGETS[PATCHER.WORKFLOW_JAR],
        )


if __name__ == "__main__":
    unittest.main()
