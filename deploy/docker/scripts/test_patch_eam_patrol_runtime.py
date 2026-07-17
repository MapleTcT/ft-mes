#!/usr/bin/env python3
"""Regression tests for guarded PATROL runtime merges."""

from __future__ import annotations

import importlib.util
import sys
import tempfile
import unittest
import zipfile
from pathlib import Path


SCRIPT_PATH = Path(__file__).with_name("patch-eam-patrol-runtime.py")
SPEC = importlib.util.spec_from_file_location("patch_eam_patrol_runtime", SCRIPT_PATH)
if SPEC is None or SPEC.loader is None:
    raise RuntimeError(f"cannot load {SCRIPT_PATH}")
PATCHER = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = PATCHER
SPEC.loader.exec_module(PATCHER)


class PatrolRuntimePatchTest(unittest.TestCase):
    def make_artifacts(self, root: Path) -> dict[str, Path]:
        artifacts = {}
        for prefix in PATCHER.REQUIRED_ARTIFACT_PREFIXES:
            path = root / f"{prefix}6.0.4.0.jar"
            with zipfile.ZipFile(path, "w") as archive:
                archive.writestr("marker.txt", f"new:{prefix}")
            artifacts[prefix] = path
        return artifacts

    def make_runtime(self, path: Path, artifacts: dict[str, Path]) -> None:
        bootstrap = "\n".join(
            (
                "supfusion.cloud.registry.instanceAliases[0]=PATROL",
                "service.moduleCodes[0]=PATROL_1.0.0",
                "supfusion.cloud.i18n.locale-modules[0]=PATROL",
                "supfusion.cloud.i18n.module-versions[0]=PATROLold",
                "supfusion.cloud.i18n.module-versions[0]=PATROLstale",
                "supfusion.cloud.i18n.modules[0]=PATROL",
                "",
            )
        )
        with zipfile.ZipFile(path, "w") as archive:
            archive.writestr(PATCHER.BOOTSTRAP_PATH, bootstrap)
            archive.writestr("BOOT-INF/classes/application.properties", "server.port=0\n")
            for artifact in artifacts.values():
                info = zipfile.ZipInfo(f"BOOT-INF/lib/{artifact.name}")
                info.compress_type = zipfile.ZIP_STORED
                archive.writestr(info, b"old-artifact")

    def test_replace_existing_preserves_entries_and_updates_artifacts(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            artifacts = self.make_artifacts(root)
            input_path = root / "input.jar"
            output_path = root / "output.jar"
            self.make_runtime(input_path, artifacts)

            nested, input_count, replaced = PATCHER.build_output(
                input_path,
                output_path,
                artifacts,
                "202607170700",
                replace_existing=True,
            )

            self.assertEqual(set(nested), set(replaced))
            self.assertEqual(5, input_count)
            with zipfile.ZipFile(input_path) as original, zipfile.ZipFile(output_path) as output:
                self.assertEqual(
                    {entry.filename for entry in original.infolist()},
                    {entry.filename for entry in output.infolist()},
                )
                for artifact in artifacts.values():
                    nested_name = f"BOOT-INF/lib/{artifact.name}"
                    self.assertEqual(artifact.read_bytes(), output.read(nested_name))
                bootstrap = output.read(PATCHER.BOOTSTRAP_PATH).decode("utf-8")
                self.assertEqual(
                    1,
                    bootstrap.count(
                        "supfusion.cloud.i18n.module-versions[0]=PATROL202607170700"
                    ),
                )
                self.assertNotIn("PATROLstale", bootstrap)

    def test_existing_artifacts_require_explicit_replace_flag(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            artifacts = self.make_artifacts(root)
            input_path = root / "input.jar"
            self.make_runtime(input_path, artifacts)

            with self.assertRaisesRegex(ValueError, "already exist"):
                PATCHER.build_output(
                    input_path,
                    root / "output.jar",
                    artifacts,
                    "202607170700",
                )


if __name__ == "__main__":
    unittest.main()
