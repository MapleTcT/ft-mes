#!/usr/bin/env python3
"""Regression checks for the PostgreSQL low-code model table runtime patch."""

from __future__ import annotations

import importlib.util
import io
import sys
import unittest
import zipfile
from pathlib import Path


SCRIPT_DIR = Path(__file__).resolve().parent
ROOT = SCRIPT_DIR.parents[2]
PATCHER_PATH = SCRIPT_DIR / "patch-configuration-entity-model-runtime.py"
MODEL_SYNC_PATH = (
    ROOT
    / "backend/modules/com/supcon/supfusion/configuration/configuration-services-service/1.0.0-SNAPSHOT"
    / "com/supcon/supfusion/configuration/services/utils/ModelSyncDBUtils.java"
)
POSTGRES_SUPPORT_PATH = MODEL_SYNC_PATH.with_name("PostgresModelSyncSupport.java")
MODEL_SERVICE_PATH = MODEL_SYNC_PATH.parents[1] / "service/impl/ModelServiceImpl.java"

SPEC = importlib.util.spec_from_file_location("patch_configuration_entity_model_runtime", PATCHER_PATH)
if SPEC is None or SPEC.loader is None:
    raise RuntimeError(f"cannot load {PATCHER_PATH}")
PATCHER = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = PATCHER
SPEC.loader.exec_module(PATCHER)


class ConfigurationPostgresModelSyncTest(unittest.TestCase):
    def test_postgres_branch_is_patched_into_runtime(self) -> None:
        model_sync = MODEL_SYNC_PATH.read_text(encoding="utf-8")
        support = POSTGRES_SUPPORT_PATH.read_text(encoding="utf-8")
        service = MODEL_SERVICE_PATH.read_text(encoding="utf-8")

        self.assertIn('dbName.startsWith("postgres")', model_sync)
        self.assertIn("PostgresModelSyncSupport.sync", model_sync)
        self.assertIn("PostgresModelSyncSupport.createMneCodeTable", model_sync)
        self.assertIn('Pattern.compile("[A-Za-z][A-Za-z0-9_]{0,62}")', support)
        self.assertIn("CREATE TABLE IF NOT EXISTS public.", support)
        self.assertIn("information_schema.tables", support)
        self.assertIn("information_schema.columns", support)
        self.assertIn("PostgreSQL model table already exists", support)
        self.assertNotIn("DROP TABLE", support.upper())
        self.assertIn("PostgreSQL physical model table synchronization failed", service)

        service_targets = PATCHER.PATCH_TARGETS[PATCHER.SERVICE_JAR]
        self.assertIn(
            "com/supcon/supfusion/configuration/services/utils/PostgresModelSyncSupport.class",
            service_targets,
        )
        self.assertIn(
            "com/supcon/supfusion/configuration/services/utils/ModelSyncDBUtils.class",
            service_targets,
        )

    def test_runtime_patcher_replaces_both_sync_classes(self) -> None:
        target_classes = PATCHER.PATCH_TARGETS[PATCHER.SERVICE_JAR]
        original = io.BytesIO()
        with zipfile.ZipFile(original, "w") as archive:
            for class_name in target_classes:
                archive.writestr(class_name, b"old")

        replacements = {class_name: f"new:{class_name}".encode() for class_name in target_classes}
        patched = PATCHER.patch_inner_jar(original.getvalue(), replacements, target_classes)
        with zipfile.ZipFile(io.BytesIO(patched), "r") as archive:
            for class_name in target_classes:
                self.assertEqual(replacements[class_name], archive.read(class_name))

    def test_runtime_patcher_fails_closed_when_compiled_class_is_missing(self) -> None:
        target_classes = PATCHER.PATCH_TARGETS[PATCHER.SERVICE_JAR]
        empty_jar = io.BytesIO()
        with zipfile.ZipFile(empty_jar, "w") as archive:
            archive.writestr("placeholder", b"old")

        with self.assertRaisesRegex(SystemExit, "missing compiled patch classes"):
            PATCHER.patch_inner_jar(empty_jar.getvalue(), {}, target_classes)


if __name__ == "__main__":
    unittest.main()
