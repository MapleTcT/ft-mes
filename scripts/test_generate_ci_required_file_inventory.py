from __future__ import annotations

import importlib.util
import unittest
from pathlib import Path


SCRIPT_PATH = Path(__file__).with_name("generate-ci-required-file-inventory.py")
SPEC = importlib.util.spec_from_file_location("generate_ci_required_file_inventory", SCRIPT_PATH)
assert SPEC is not None and SPEC.loader is not None
MODULE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MODULE)


class GeneratedReferenceTest(unittest.TestCase):
    def test_maven_target_artifact_is_generated(self) -> None:
        self.assertTrue(
            MODULE.is_generated_reference(
                "backend/source-modules/material-wms/target/material-wms-0.1.0-SNAPSHOT.jar"
            )
        )

    def test_source_and_runtime_artifacts_are_not_generated_references(self) -> None:
        self.assertFalse(MODULE.is_generated_reference("backend/source-modules/material-wms/pom.xml"))
        self.assertFalse(MODULE.is_generated_reference("runtime/bap-server/material-wms.jar"))

    def test_qualify_patch_jar_is_excluded_as_generated_output(self) -> None:
        self.assertTrue(
            MODULE.should_exclude_candidate(
                MODULE.ROOT
                / "deploy/docker/patches/qualify-config-defaults/qualify-config-defaults.jar"
            )
        )


if __name__ == "__main__":
    unittest.main()
