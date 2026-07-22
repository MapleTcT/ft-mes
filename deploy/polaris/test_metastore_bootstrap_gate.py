from __future__ import annotations

import os
import subprocess
import tempfile
import textwrap
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
CHECK_SCRIPT = ROOT / "deploy/polaris/check_metastore_bootstrap.sh"
BOOTSTRAP_SCRIPT = ROOT / "deploy/polaris/bootstrap_metastore_if_required.sh"


class MetastoreBootstrapGateTest(unittest.TestCase):
    def setUp(self) -> None:
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.bin = self.root / "bin"
        self.state = self.root / "state"
        self.bin.mkdir()
        fake_psql = self.bin / "psql"
        fake_psql.write_text(
            textwrap.dedent(
                """\
                #!/bin/sh
                case "$*" in
                    *to_regclass*) printf '%s\\n' "${FAKE_POLARIS_AUTH_TABLE:-}" ;;
                    *)
                        query=$(cat)
                        case "$query" in
                            *principal_client_id*) printf '%s\\n' "${FAKE_POLARIS_MATCHING_ROWS:-0}" ;;
                            *) printf '%s\\n' "${FAKE_POLARIS_REALM_ROWS:-0}" ;;
                        esac
                        ;;
                esac
                """
            ),
            encoding="utf-8",
        )
        fake_psql.chmod(0o755)
        fake_java = self.bin / "java"
        fake_java.write_text(
            "#!/bin/sh\nprintf '%s\\n' \"$*\" >\"$FAKE_JAVA_ARGS_FILE\"\n",
            encoding="utf-8",
        )
        fake_java.chmod(0o755)

    def environment(self, **overrides: str) -> dict[str, str]:
        environment = os.environ.copy()
        environment.update(
            {
                "PATH": f"{self.bin}:{environment['PATH']}",
                "PGHOST": "postgres",
                "PGPORT": "5432",
                "PGDATABASE": "polaris",
                "PGUSER": "polaris",
                "PGPASSWORD": "test-only-password",
                "BPI_POLARIS_REALM": "POLARIS",
                "BPI_POLARIS_BOOTSTRAP_CLIENT_ID": "bpi-root",
                "BPI_POLARIS_BOOTSTRAP_CLIENT_SECRET": "test-only-client-secret",
                "BPI_POLARIS_BOOTSTRAP_STATE_DIR": str(self.state),
            }
        )
        environment.update(overrides)
        return environment

    def run_check(self, **overrides: str) -> subprocess.CompletedProcess[str]:
        return subprocess.run(
            ["sh", str(CHECK_SCRIPT)],
            env=self.environment(**overrides),
            text=True,
            capture_output=True,
            check=False,
        )

    def test_fresh_metastore_requires_bootstrap(self) -> None:
        result = self.run_check()

        self.assertEqual(0, result.returncode, result.stderr)
        self.assertEqual("BOOTSTRAP_REQUIRED\n", (self.state / "metastore-state").read_text())

    def test_matching_realm_and_client_skip_admin_bootstrap(self) -> None:
        result = self.run_check(
            FAKE_POLARIS_AUTH_TABLE="polaris_schema.principal_authentication_data",
            FAKE_POLARIS_REALM_ROWS="1",
            FAKE_POLARIS_MATCHING_ROWS="1",
        )

        self.assertEqual(0, result.returncode, result.stderr)
        state_file = self.state / "metastore-state"
        self.assertEqual("BOOTSTRAP_COMPLETE\n", state_file.read_text())
        wrapper = subprocess.run(
            ["sh", str(BOOTSTRAP_SCRIPT), "bootstrap", "--realm=POLARIS"],
            env=self.environment(BPI_POLARIS_BOOTSTRAP_STATE_FILE=str(state_file)),
            text=True,
            capture_output=True,
            check=False,
        )
        self.assertEqual(0, wrapper.returncode, wrapper.stderr)
        self.assertIn("admin bootstrap skipped", wrapper.stdout)

    def test_existing_realm_with_different_client_fails_closed(self) -> None:
        result = self.run_check(
            FAKE_POLARIS_AUTH_TABLE="polaris_schema.principal_authentication_data",
            FAKE_POLARIS_REALM_ROWS="1",
            FAKE_POLARIS_MATCHING_ROWS="0",
        )

        self.assertEqual(1, result.returncode)
        self.assertIn("different bootstrap client ID", result.stderr)
        self.assertFalse((self.state / "metastore-state").exists())

    def test_required_bootstrap_reads_secret_inside_wrapper_without_printing_it(self) -> None:
        self.state.mkdir()
        state_file = self.state / "metastore-state"
        state_file.write_text("BOOTSTRAP_REQUIRED\n", encoding="utf-8")
        args_file = self.root / "java-args"
        wrapper = subprocess.run(
            ["sh", str(BOOTSTRAP_SCRIPT), "bootstrap"],
            env=self.environment(
                BPI_POLARIS_BOOTSTRAP_STATE_FILE=str(state_file),
                FAKE_JAVA_ARGS_FILE=str(args_file),
            ),
            text=True,
            capture_output=True,
            check=False,
        )

        self.assertEqual(0, wrapper.returncode, wrapper.stderr)
        self.assertNotIn("test-only-client-secret", wrapper.stdout + wrapper.stderr)
        self.assertIn("--realm=POLARIS", args_file.read_text(encoding="utf-8"))
        self.assertIn(
            "--credential=POLARIS,bpi-root,test-only-client-secret",
            args_file.read_text(encoding="utf-8"),
        )


if __name__ == "__main__":
    unittest.main()
