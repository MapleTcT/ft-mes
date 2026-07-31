#!/usr/bin/env python3

import importlib.util
import io
import tempfile
import unittest
import zipfile
from pathlib import Path


SCRIPT_PATH = Path(__file__).with_name("patch-wts-runtime-compat.py")
SPEC = importlib.util.spec_from_file_location("patch_wts_runtime_compat", SCRIPT_PATH)
MODULE = importlib.util.module_from_spec(SPEC)
assert SPEC and SPEC.loader
SPEC.loader.exec_module(MODULE)


class WtsStatisticsExportPatchTest(unittest.TestCase):
    def build_jar(self) -> bytes:
        output = io.BytesIO()
        html = b'<html><body><div id="root"></div><script src="umi.js"></script></body></html>'
        with zipfile.ZipFile(output, "w") as archive:
            for path in MODULE.STAT_EXPORT_HTML:
                archive.writestr(path, html)
            archive.writestr("custom/WTS/unrelated.html", html)
        return output.getvalue()

    def test_adds_script_to_both_statistics_entries(self):
        script = b"window.__wtsStatisticsExport = true;"
        patched, stats = MODULE.patch_inner_wts_jar(
            self.build_jar(),
            statistics_export_script=script,
        )

        self.assertCountEqual(stats["patched_statistics_html"], MODULE.STAT_EXPORT_HTML)
        self.assertTrue(stats["patched_statistics_script"])
        with zipfile.ZipFile(io.BytesIO(patched), "r") as archive:
            self.assertEqual(archive.read(MODULE.STAT_EXPORT_SCRIPT), script)
            for path in MODULE.STAT_EXPORT_HTML:
                html = archive.read(path).decode("utf-8")
                self.assertEqual(html.count(MODULE.STAT_EXPORT_MARKER), 1)
            unrelated = archive.read("custom/WTS/unrelated.html").decode("utf-8")
            self.assertNotIn(MODULE.STAT_EXPORT_MARKER, unrelated)

    def test_patches_the_html_served_by_the_public_statistics_route(self):
        script = b"window.__wtsStatisticsExport = true;"
        patched, _ = MODULE.patch_inner_wts_jar(
            self.build_jar(),
            statistics_export_script=script,
        )

        route_html = "custom/WTS/workTicket/assWorkTickets/workTicket/index.html"
        with zipfile.ZipFile(io.BytesIO(patched), "r") as archive:
            html = archive.read(route_html).decode("utf-8")
            self.assertEqual(html.count(MODULE.STAT_EXPORT_MARKER), 1)

    def test_patch_is_idempotent(self):
        script = b"window.__wtsStatisticsExport = true;"
        patched, _ = MODULE.patch_inner_wts_jar(
            self.build_jar(),
            statistics_export_script=script,
        )
        repatched, stats = MODULE.patch_inner_wts_jar(
            patched,
            statistics_export_script=script,
        )

        self.assertEqual(stats["patched_statistics_html"], [])
        self.assertFalse(stats["patched_statistics_script"])
        with zipfile.ZipFile(io.BytesIO(repatched), "r") as archive:
            self.assertEqual(archive.namelist().count(MODULE.STAT_EXPORT_SCRIPT), 1)
            for path in MODULE.STAT_EXPORT_HTML:
                html = archive.read(path).decode("utf-8")
                self.assertEqual(html.count(MODULE.STAT_EXPORT_MARKER), 1)

    def test_patches_extracted_statistics_workspace(self):
        script = b"window.__wtsStatisticsExport = true;"
        html = '<html><body><div id="root"></div>' + MODULE.STAT_UMI_SCRIPT_TAG + "</body></html>"
        with tempfile.TemporaryDirectory() as temp_name:
            static_root = Path(temp_name)
            for relative_path in MODULE.STAT_RUNTIME_HTML:
                html_path = static_root / relative_path
                html_path.parent.mkdir(parents=True, exist_ok=True)
                html_path.write_text(html, encoding="utf-8")

            result = MODULE.patch_statistics_static_root(static_root, script, ".backup")

            self.assertTrue(result["changed"])
            self.assertTrue(result["script_patched"])
            self.assertCountEqual(result["html_patched"], [str(path) for path in MODULE.STAT_RUNTIME_HTML])
            self.assertEqual((static_root / "job-statistics-export-compat.js").read_bytes(), script)
            for relative_path in MODULE.STAT_RUNTIME_HTML:
                patched = (static_root / relative_path).read_text(encoding="utf-8")
                self.assertEqual(patched.count(MODULE.STAT_EXPORT_MARKER), 1)
                self.assertTrue((static_root / relative_path).with_name(relative_path.name + ".backup").exists())

            second = MODULE.patch_statistics_static_root(static_root, script, ".backup")
            self.assertFalse(second["changed"])
            self.assertFalse(second["script_patched"])
            self.assertEqual(second["html_patched"], [])

    def test_inner_jar_adds_missing_runtime_class(self):
        class_entry = "com/supcon/orchid/WTS/entities/Recovered.class"
        patched, stats = MODULE.patch_inner_wts_jar(
            self.build_jar(),
            extra_classes={class_entry: b"recovered-class"},
        )

        self.assertIn(class_entry, stats["patched_extra_classes"])
        with zipfile.ZipFile(io.BytesIO(patched), "r") as archive:
            self.assertEqual(archive.read(class_entry), b"recovered-class")


if __name__ == "__main__":
    unittest.main()
