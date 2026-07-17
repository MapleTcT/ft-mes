#!/usr/bin/env python3
"""Regression tests for the PATROL PostgreSQL source patch."""

from __future__ import annotations

import importlib.util
import sys
import unittest
from pathlib import Path


SCRIPT_PATH = Path(__file__).with_name("patch-patrol-postgres-source.py")
SPEC = importlib.util.spec_from_file_location("patch_patrol_postgres_source", SCRIPT_PATH)
if SPEC is None or SPEC.loader is None:
    raise RuntimeError(f"cannot load {SCRIPT_PATH}")
PATCHER = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = PATCHER
SPEC.loader.exec_module(PATCHER)


class PatrolPostgresSourcePatchTest(unittest.TestCase):
    def test_generated_sql_conversions_use_dialect_helper(self):
        source = (
            b"  \tbuffer.toString().replace('\"', '`');\r\n"
            b"realSql.toString().replace('\"', '`');\r\n"
            b"treesql.replace('\"', '`');\r\n"
        )

        patched, count, indentation_count = PATCHER.patch_service(source)

        self.assertEqual(3, count)
        self.assertEqual(1, indentation_count)
        self.assertNotIn(b".replace('\"', '`')", patched)
        self.assertNotIn(b" \t", patched.splitlines()[0])
        self.assertEqual(3, patched.count(PATCHER.QUALIFIED_HELPER))
        self.assertIn(b"\r\n", patched)

    def test_postgres_result_aliases_remain_uppercase(self):
        source = (
            b'String selectSql = "select " + origId + " AS OID";\n'
            b'selectSql += "," + id1 + " AS ID1";\n'
            b'selectSql += "," + id2 + " AS ID2";\n'
            b'selectSql += "," + value + " AS VAL";\n'
            b'selectSql += ", BASE_SYSTEMCODE.VALUE AS REALVAL";\n'
            b'orderSql += "2".equals(level) ? "ID1 ASC" : "OID ASC";\n'
            b'orderSql += "2".equals(level) ? "ID2 ASC" : "ID1 ASC";\n'
        )

        patched, count, _ = PATCHER.patch_service(source)

        self.assertEqual(7, count)
        for alias in (b"OID", b"ID1", b"ID2", b"VAL", b"REALVAL"):
            self.assertIn(b'AS \\"' + alias + b'\\"', patched)
        self.assertIn(b'? "\\"ID1\\" ASC" : "\\"OID\\" ASC";', patched)
        self.assertIn(b'? "\\"ID2\\" ASC" : "\\"ID1\\" ASC";', patched)

    def test_task_generation_patch_is_complete_and_idempotent(self):
        source = (
            b"public void create() {\r\n"
            b"    potrolTask.setPatrolPlanId(potrolPlan.getId());//patrol plan\r\n"
            b'    String sql = "EAM_ID,TASK_AREA_ID,SORT) "\r\n'
            b'            + " values(?,?,?,?,?)";\r\n'
            b"                    ps.addBatch();\r\n"
            b"}\r\n"
        )

        patched, count = PATCHER.patch_task_generation(source)
        patched_twice, count_twice = PATCHER.patch_task_generation(patched)

        self.assertEqual(3, count)
        self.assertEqual(0, count_twice)
        self.assertEqual(patched, patched_twice)
        self.assertIn(PATCHER.PLAN_ASSOCIATION_ASSIGNMENT, patched)
        self.assertIn(PATCHER.PATCHED_TASK_DETAIL_COLUMNS, patched)
        self.assertIn(b"values(?,?,?,?,?,?,?,?)", patched)
        self.assertIn(b"ps.setBoolean(28, true)", patched)
        self.assertIn(b"ps.setInt(29, 0)", patched)
        self.assertIn(PATCHER.TASK_DETAIL_DEFAULT_MARKER, patched)

    def test_hidden_danger_patch_adds_degraded_persistence_and_is_idempotent(self):
        source = (
            b"    @Override\r\n"
            b"    public Map<String, Object> createHiddenDanger(String ids, Map<String, String> headerMap, Boolean isCheckFault) {\r\n"
            b"        return new HashMap<>();\r\n"
            b"    }\r\n\r\n"
            b"    private Map<String, Object> generateDetailMap(PATROLTaskDetail patrolTaskDetail) {\r\n"
            + PATCHER.source_fragment(PATCHER.HIDDEN_DANGER_OLD_CONTEXT, b"\r\n")
            + b"        if (StringUtil.isNotBlank(patrolTaskDetail.getWorkItemId().getNormalRange())) {\r\n"
            b"        }\r\n"
            b"        return new HashMap<>();\r\n"
            b"    }\r\n"
            b"    public void checkFaultExist(List<PATROLTaskDetail> taskDetails) {\r\n"
            b"        List<Long> workItemIds = new ArrayList<>();\r\n"
            + PATCHER.HIDDEN_DANGER_WORK_ITEM_ANCHOR
            + b"\r\n"
            b"        String sql = \""
            + PATCHER.HIDDEN_DANGER_ALIAS_OLD
            + b"\";\r\n"
            b"    }\r\n"
        )

        patched, count = PATCHER.patch_hidden_danger(source)
        patched_twice, count_twice = PATCHER.patch_hidden_danger(patched)

        self.assertEqual(5, count)
        self.assertEqual(0, count_twice)
        self.assertEqual(patched, patched_twice)
        self.assertIn(PATCHER.HIDDEN_DANGER_COMPATIBILITY_MARKER, patched)
        self.assertIn(b"saveRiskHandleCompatibility", patched)
        self.assertIn(b"getFaultId() != null", patched)
        self.assertIn(b"createdCount", patched)
        self.assertIn(b"resolveHiddenDangerFinder", patched)
        self.assertIn(b"org.hibernate.type.LongType.INSTANCE", patched)
        self.assertIn(b"org.hibernate.type.StringType.INSTANCE", patched)
        self.assertIn(PATCHER.HIDDEN_DANGER_NORMAL_RANGE_NEW, patched)
        self.assertIn(PATCHER.HIDDEN_DANGER_WORK_ITEM_GUARD, patched)
        self.assertIn(PATCHER.HIDDEN_DANGER_ALIAS_NEW, patched)
        self.assertNotIn(PATCHER.HIDDEN_DANGER_ALIAS_OLD, patched)

    def test_utility_patch_is_idempotent_and_preserves_postgres_quotes(self):
        source = (
            b"package test;\n"
            + PATCHER.UTILITY_IMPORT_ANCHOR
            + b"\n"
            + PATCHER.UTILITY_CLASS_ANCHOR
            + b"\n}\n"
        )

        patched, changed = PATCHER.patch_utility(source)
        patched_twice, changed_twice = PATCHER.patch_utility(patched)

        self.assertTrue(changed)
        self.assertFalse(changed_twice)
        self.assertEqual(patched, patched_twice)
        self.assertIn(b"return mysql ? sql.replace", patched)
        self.assertIn(b": sql;", patched)

    def test_report_patch_counts_compatibility_handoffs_as_pending(self):
        source = b"\n".join(
            (
                PATCHER.REPORT_PENDING_REPLACEMENTS[0][0],
                PATCHER.REPORT_PENDING_REPLACEMENTS[1][0],
                PATCHER.REPORT_PENDING_REPLACEMENTS[2][0],
                PATCHER.REPORT_PENDING_REPLACEMENTS[3][0],
            )
        )

        patched, count = PATCHER.patch_report_pending_handoff(source)
        patched_twice, count_twice = PATCHER.patch_report_pending_handoff(patched)

        self.assertEqual(4, count)
        self.assertEqual(0, count_twice)
        self.assertEqual(patched, patched_twice)
        self.assertIn(b"hr.risk_mode riskMode", patched)
        self.assertIn(b"group by hr.status, hr.risk_mode", patched)
        self.assertIn(b'.addScalar("riskMode", HibernateType.STRING)', patched)
        self.assertIn(PATCHER.REPORT_PENDING_MARKER, patched)
        self.assertIn(b"pending = pending.add(new BigDecimal(total))", patched)

    def test_gather_data_patch_is_safe_precise_and_idempotent(self):
        source = b"\n".join(original for original, _ in PATCHER.GATHER_DATA_REPLACEMENTS)

        patched, count = PATCHER.patch_gather_data_consumer(source)
        patched_twice, count_twice = PATCHER.patch_gather_data_consumer(patched)

        self.assertEqual(len(PATCHER.GATHER_DATA_REPLACEMENTS), count)
        self.assertEqual(0, count_twice)
        self.assertEqual(patched, patched_twice)
        self.assertIn(b"ObjectUtils.isEmpty(workItemTaskIds)", patched)
        self.assertIn(b"Message<?> value", patched)
        self.assertIn(b"StandardCharsets.UTF_8", patched)
        self.assertIn(b"payload instanceof byte[]", patched)
        self.assertIn(b"messageData == null", patched)
        self.assertIn(b'new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")', patched)
        self.assertIn(b"ObjectUtils.isEmpty(workItemIds)", patched)
        self.assertIn(b"invalid task id", patched)
        self.assertIn(b"invalid work-item list", patched)
        self.assertIn(b"invalid work-item id", patched)
        self.assertIn(b"taskDetail.getCompleteDate() == null", patched)
        self.assertIn(b"malformed TagManagement history response", patched)
        self.assertIn(b"response == null ? null", patched)
        self.assertIn(b"BigDecimal.valueOf(median)", patched)
        self.assertIn(b"return data.get(size / 2);", patched)
        self.assertNotIn(b"Math.ceil", patched)
        self.assertNotIn(b"new BigDecimal(median)", patched)


if __name__ == "__main__":
    unittest.main()
