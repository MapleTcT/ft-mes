#!/usr/bin/env python3
"""Regression tests for recovered-module access SQL generation."""

from __future__ import annotations

import importlib.util
import sys
import unittest
from pathlib import Path


SCRIPT_PATH = Path(__file__).with_name("generate-module-access-workflow-sql.py")
SPEC = importlib.util.spec_from_file_location("module_access_sql_generator", SCRIPT_PATH)
if SPEC is None or SPEC.loader is None:
    raise RuntimeError(f"cannot load {SCRIPT_PATH}")
GENERATOR = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = GENERATOR
SPEC.loader.exec_module(GENERATOR)


def sample_module(acronym: str = "MP"):
    menu = GENERATOR.Menu(
        code="PATROL_1.0.0_sample",
        name="Sample",
        valid=True,
        memo="",
        target="",
        url="/sample",
        namespace="",
        action="",
        sort=1.0,
        css_class="",
        system_default=True,
        module_code="PATROL_1.0.0",
        entity_code="",
        leaf=True,
        lay_no=1,
        is_hide=False,
        absolute_hidden=False,
        parent_code="",
    )
    return GENERATOR.ModuleAccess(
        module_code="PATROL_1.0.0",
        acronym=acronym,
        menus=(menu,),
        operations=(),
        workflows=(),
        app=None,
    )


class GenerateAccessSqlTest(unittest.TestCase):
    def test_localizes_menu_names_without_changing_codes(self):
        localized = GENERATOR.localize_module(sample_module(), {"Sample": "Patrol setup"})

        self.assertEqual("Patrol setup", localized.menus[0].name)
        self.assertEqual("PATROL_1.0.0_sample", localized.menus[0].code)

    def test_explicit_host_app_code_overrides_module_acronym(self):
        output = GENERATOR.generate_access_sql(
            sample_module(), 7_181_000_000_000_000, 1, "test", "EAM"
        )

        self.assertIn("-- menu_app_code: EAM", output)
        self.assertIn("-- bootstrap_admin_user_id: 1", output)
        self.assertIn("CREATE TEMP TABLE adp_module_user_permission_seed", output)
        self.assertIn("INSERT INTO public.rbac_userpermission", output)
        self.assertIn("CREATE TEMP TABLE adp_module_company_menu_seed", output)
        self.assertIn("INSERT INTO public.rbac_menuinfo_company_ref", output)
        self.assertIn("\nBEGIN;\n\nCREATE TEMP TABLE", output)
        self.assertTrue(output.rstrip().endswith("COMMIT;"))
        self.assertIn("seed.name, seed.code, 'EAM', true", output)
        self.assertIn("app = 'EAM'", output)
        self.assertIn("module_code = 'PATROL_1.0.0'", output)
        self.assertIn("'EAM', false, false", output)
        self.assertNotIn("seed.name, seed.code, 'MP', true", output)

    def test_module_acronym_remains_the_default(self):
        output = GENERATOR.generate_access_sql(
            sample_module(), 7_181_000_000_000_000, 1, "test"
        )

        self.assertIn("-- menu_app_code: MP", output)
        self.assertIn("seed.name, seed.code, 'MP', true", output)

    def test_empty_host_app_code_is_rejected(self):
        with self.assertRaisesRegex(ValueError, "cannot be empty"):
            GENERATOR.generate_access_sql(
                sample_module(acronym=""), 7_181_000_000_000_000, 1, "test", ""
            )


if __name__ == "__main__":
    unittest.main()
