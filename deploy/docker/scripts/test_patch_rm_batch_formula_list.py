from __future__ import annotations

import importlib.util
import unittest
from pathlib import Path


SCRIPT_PATH = Path(__file__).with_name("patch-rm-batch-formula-list.py")
SPEC = importlib.util.spec_from_file_location("patch_rm_batch_formula_list", SCRIPT_PATH)
assert SPEC is not None and SPEC.loader is not None
MODULE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MODULE)


class DownloadFilenameCompatTest(unittest.TestCase):
    def test_injects_before_body_and_is_idempotent(self) -> None:
        original = "<html><body><main>RM</main></body></html>"

        patched, changed = MODULE.inject_download_filename_compat(original)
        second, second_changed = MODULE.inject_download_filename_compat(patched)

        self.assertTrue(changed)
        self.assertFalse(second_changed)
        self.assertEqual(second, patched)
        self.assertIn(MODULE.DOWNLOAD_FILENAME_COMPAT_MARKER, patched)
        self.assertIn('this.download = "RM_batchFormulaList.xlsx"', patched)
        self.assertLess(patched.index(MODULE.DOWNLOAD_FILENAME_COMPAT_MARKER), patched.index("</body>"))


class BrowserAuthCompatTest(unittest.TestCase):
    def test_injects_browser_ticket_for_xhr_and_fetch_once(self) -> None:
        original = "<html><body><main>RM</main></body></html>"

        patched, changed = MODULE.inject_auth_compat(original)
        second, second_changed = MODULE.inject_auth_compat(patched)

        self.assertTrue(changed)
        self.assertFalse(second_changed)
        self.assertEqual(second, patched)
        self.assertIn(MODULE.AUTH_COMPAT_MARKER, patched)
        self.assertIn('"Authorization", "Bearer " + token', patched)
        self.assertIn("xhrPrototype.__adpRmAuthPatched", patched)
        self.assertIn("authenticatedFetch.__adpRmAuthPatched", patched)
        self.assertLess(patched.index(MODULE.AUTH_COMPAT_MARKER), patched.index("</body>"))


if __name__ == "__main__":
    unittest.main()
