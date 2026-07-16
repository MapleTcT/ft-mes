import importlib.util
from pathlib import Path
import sys
import tempfile
import unittest


SCRIPT = Path(__file__).with_name("generate-module-system-code-sql.py")
SPEC = importlib.util.spec_from_file_location("generate_module_system_code_sql", SCRIPT)
GENERATOR = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
sys.modules[SPEC.name] = GENERATOR
SPEC.loader.exec_module(GENERATOR)


SAMPLE_XML = """<?xml version="1.0" encoding="UTF-8"?>
<bapSystemCode>
  <systementity code="PATROL_valueType" moduleCode="PATROL" name="PATROL.valueType"
      sysDefault="false" listType="list" multiFlag="false" memo="" valid="true">
    <systemcode id="PATROL_valueType/char" code="char" value="PATROL.value.char"
        sort="1" valid="true" leaf="true" defaultFlag="false" layNo="1"
        layRec="1439496803712944" fullPathName="字符" memo="O'Reilly" />
  </systementity>
</bapSystemCode>
"""


class GenerateModuleSystemCodeSqlTest(unittest.TestCase):
    def test_parses_entity_and_value(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "systemcode.xml"
            path.write_text(SAMPLE_XML, encoding="utf-8")
            entities = GENERATOR.parse_system_codes(path)

        self.assertEqual(1, len(entities))
        self.assertEqual("PATROL_valueType", entities[0].code)
        self.assertEqual(1439496803712944, entities[0].codes[0].id)
        self.assertEqual("字符", entities[0].codes[0].display_name)

    def test_renders_idempotent_logical_upserts(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "systemcode.xml"
            path.write_text(SAMPLE_XML, encoding="utf-8")
            sql = GENERATOR.render_sql(GENERATOR.parse_system_codes(path), path)

        self.assertIn("target.code = source.code", sql)
        self.assertIn("target.entity_code = source.entity_code AND target.code = source.code", sql)
        self.assertIn("'PATROL_valueType'", sql)
        self.assertIn("'字符'", sql)
        self.assertIn("'O''Reilly'", sql)
        self.assertNotIn("DELETE FROM public.sys_", sql)


if __name__ == "__main__":
    unittest.main()
