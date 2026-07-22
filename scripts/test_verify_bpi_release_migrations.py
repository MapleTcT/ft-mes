from __future__ import annotations

import subprocess
import sys
import tempfile
import unittest
import zipfile
from pathlib import Path


SCRIPT = Path(__file__).with_name("verify-bpi-release-migrations.py")


class VerifyBpiReleaseMigrationsTest(unittest.TestCase):
    def setUp(self) -> None:
        self.temporary = tempfile.TemporaryDirectory()
        self.root = Path(self.temporary.name)
        self.migrations = self.root / "migration"
        self.migrations.mkdir()
        (self.migrations / "V1__baseline.sql").write_text(
            "select 1;\n", encoding="utf-8"
        )
        (self.migrations / "V2__feature.sql").write_text(
            "select 2;\n", encoding="utf-8"
        )

    def tearDown(self) -> None:
        self.temporary.cleanup()

    def package(self, entries: dict[str, str]) -> Path:
        jar = self.root / "service.jar"
        with zipfile.ZipFile(jar, "w") as archive:
            for name, content in entries.items():
                archive.writestr(
                    f"BOOT-INF/classes/db/migration/{name}", content
                )
        return jar

    def run_check(self, jar: Path, expected: int = 2) -> subprocess.CompletedProcess[str]:
        return subprocess.run(
            [
                sys.executable,
                str(SCRIPT),
                "--jar",
                str(jar),
                "--migrations-dir",
                str(self.migrations),
                "--expected-version",
                str(expected),
            ],
            check=False,
            capture_output=True,
            text=True,
        )

    def test_accepts_exact_packaged_migration_set(self) -> None:
        result = self.run_check(
            self.package(
                {
                    "V1__baseline.sql": "select 1;\n",
                    "V2__feature.sql": "select 2;\n",
                }
            )
        )

        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("PASS (V2", result.stdout)

    def test_rejects_stale_jar(self) -> None:
        result = self.run_check(
            self.package({"V1__baseline.sql": "select 1;\n"})
        )

        self.assertNotEqual(0, result.returncode)
        self.assertIn("packaged migration head is V1", result.stderr)

    def test_rejects_checksum_drift(self) -> None:
        result = self.run_check(
            self.package(
                {
                    "V1__baseline.sql": "select 1;\n",
                    "V2__feature.sql": "select 999;\n",
                }
            )
        )

        self.assertNotEqual(0, result.returncode)
        self.assertIn("checksum differs", result.stderr)

    def test_rejects_expected_version_behind_source(self) -> None:
        result = self.run_check(
            self.package(
                {
                    "V1__baseline.sql": "select 1;\n",
                    "V2__feature.sql": "select 2;\n",
                }
            ),
            expected=1,
        )

        self.assertNotEqual(0, result.returncode)
        self.assertIn("source migration head is V2", result.stderr)


if __name__ == "__main__":
    unittest.main()
