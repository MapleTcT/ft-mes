-- QCS 6.1.3.5 custom initialization SQL only ships vendor-specific seed data.
-- The Linux/PostgreSQL smoke environment needs the TableType dictionary table
-- for QCS list filters such as QCS_TABLE_TYPES.CODE = 'manu'.

DO $$
BEGIN
  IF to_regclass('public.qcs_table_types') IS NULL THEN
    CREATE TABLE public.qcs_table_types (LIKE public.qcs_busi_types INCLUDING DEFAULTS);
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint
    WHERE conrelid = 'public.qcs_table_types'::regclass
      AND contype = 'p'
  ) THEN
    ALTER TABLE public.qcs_table_types ADD CONSTRAINT qcs_table_types_pkey PRIMARY KEY (id);
  END IF;
END $$;

ALTER TABLE public.qcs_table_types
  ADD COLUMN IF NOT EXISTS insp_code_rule text,
  ADD COLUMN IF NOT EXISTS report_code_rule text,
  ADD COLUMN IF NOT EXISTS un_qlf_code_rule text,
  ADD COLUMN IF NOT EXISTS release_code_rule text;

CREATE INDEX IF NOT EXISTS idx_qcs_table_types_code ON public.qcs_table_types (code);
CREATE INDEX IF NOT EXISTS idx_qcs_table_types_cid ON public.qcs_table_types (cid);
CREATE INDEX IF NOT EXISTS idx_qcs_table_types_valid ON public.qcs_table_types (valid);

WITH table_types(id, name, code, insp_prefix, report_prefix, unqlf_prefix, release_prefix) AS (
  VALUES
    (1::bigint, '产品检验', 'manu', 'manuInspect', 'manuReport', 'manuUnQlfDeal', 'manuRelease'),
    (2::bigint, '来料检验', 'purch', 'purchInspect', 'purchReport', 'purchUnQlfDeal', 'purchRelease'),
    (3::bigint, '其他检验', 'other', 'otherInspect', 'otherReport', 'otherUnQlfDeal', NULL),
    (4::bigint, '质量巡检', 'quality', 'qualityInspect', 'qualityReport', NULL, NULL)
),
rules AS (
  SELECT
    id,
    name,
    code,
    CASE WHEN insp_prefix IS NULL THEN NULL ELSE jsonb_build_object(
      'rollbackable', false,
      'pattern', '{1}_{2}{3}{4}_{0,number,000}',
      'config', jsonb_build_array(
        jsonb_build_object('type', 'custom', 'thecase', 'original', 'value', insp_prefix, 'separator', '_'),
        jsonb_build_object('type', 'date', 'thecase', 'original', 'value', '_systemdate', 'dateType', 'YearA', 'separator', ''),
        jsonb_build_object('type', 'date', 'thecase', 'original', 'value', '_systemdate', 'dateType', 'Month', 'separator', ''),
        jsonb_build_object('type', 'date', 'thecase', 'original', 'value', '_systemdate', 'dateType', 'Date', 'separator', '_'),
        jsonb_build_object('type', 'auto', 'thecase', 'original', 'value', '', 'digit', '3', 'autoType', 'Code', 'countType', 'none', 'separator', '_')
      )
    )::text END AS insp_code_rule,
    CASE WHEN report_prefix IS NULL THEN NULL ELSE jsonb_build_object(
      'rollbackable', false,
      'pattern', '{1}_{2}{3}{4}_{0,number,000}',
      'config', jsonb_build_array(
        jsonb_build_object('type', 'custom', 'thecase', 'original', 'value', report_prefix, 'separator', '_'),
        jsonb_build_object('type', 'date', 'thecase', 'original', 'value', '_systemdate', 'dateType', 'YearA', 'separator', ''),
        jsonb_build_object('type', 'date', 'thecase', 'original', 'value', '_systemdate', 'dateType', 'Month', 'separator', ''),
        jsonb_build_object('type', 'date', 'thecase', 'original', 'value', '_systemdate', 'dateType', 'Date', 'separator', '_'),
        jsonb_build_object('type', 'auto', 'thecase', 'original', 'value', '', 'digit', '3', 'autoType', 'Code', 'countType', 'none', 'separator', '_')
      )
    )::text END AS report_code_rule,
    CASE WHEN unqlf_prefix IS NULL THEN NULL ELSE jsonb_build_object(
      'rollbackable', false,
      'pattern', '{1}_{2}{3}{4}_{0,number,000}',
      'config', jsonb_build_array(
        jsonb_build_object('type', 'custom', 'thecase', 'original', 'value', unqlf_prefix, 'separator', '_'),
        jsonb_build_object('type', 'date', 'thecase', 'original', 'value', '_systemdate', 'dateType', 'YearA', 'separator', ''),
        jsonb_build_object('type', 'date', 'thecase', 'original', 'value', '_systemdate', 'dateType', 'Month', 'separator', ''),
        jsonb_build_object('type', 'date', 'thecase', 'original', 'value', '_systemdate', 'dateType', 'Date', 'separator', '_'),
        jsonb_build_object('type', 'auto', 'thecase', 'original', 'value', '', 'digit', '3', 'autoType', 'Code', 'countType', 'none', 'separator', '_')
      )
    )::text END AS un_qlf_code_rule,
    CASE WHEN release_prefix IS NULL THEN NULL ELSE jsonb_build_object(
      'rollbackable', false,
      'pattern', '{1}_{2}{3}{4}_{0,number,000}',
      'config', jsonb_build_array(
        jsonb_build_object('type', 'custom', 'thecase', 'original', 'value', release_prefix, 'separator', '_'),
        jsonb_build_object('type', 'date', 'thecase', 'original', 'value', '_systemdate', 'dateType', 'YearA', 'separator', ''),
        jsonb_build_object('type', 'date', 'thecase', 'original', 'value', '_systemdate', 'dateType', 'Month', 'separator', ''),
        jsonb_build_object('type', 'date', 'thecase', 'original', 'value', '_systemdate', 'dateType', 'Date', 'separator', '_'),
        jsonb_build_object('type', 'auto', 'thecase', 'original', 'value', '', 'digit', '3', 'autoType', 'Code', 'countType', 'none', 'separator', '_')
      )
    )::text END AS release_code_rule
  FROM table_types
)
INSERT INTO public.qcs_table_types (
  id, version, create_time, create_staff_id, valid, cid,
  effective_state, owner_staff_id, name, code, create_department_id,
  create_position_id, insp_code_rule, report_code_rule, un_qlf_code_rule,
  release_code_rule
)
SELECT
  id, 1, CURRENT_TIMESTAMP, 1, true, 1000,
  0, 1, name, code, 1,
  1, insp_code_rule, report_code_rule, un_qlf_code_rule,
  release_code_rule
FROM rules
WHERE NOT EXISTS (
  SELECT 1 FROM public.qcs_table_types existing WHERE existing.id = rules.id
);

INSERT INTO public.qcs_busi_types (
  id, version, create_time, create_staff_id, valid, cid,
  table_type_id, name, code, is_init
)
VALUES
  (1, 1, CURRENT_TIMESTAMP, 1, true, 1000, 1, '成品检验', 'product', true),
  (2, 1, CURRENT_TIMESTAMP, 1, true, 1000, 1, '产品复验', 'productRetest', true),
  (3, 1, CURRENT_TIMESTAMP, 1, true, 1000, 1, '增项检验', 'addItem', true),
  (4, 1, CURRENT_TIMESTAMP, 1, true, 1000, 2, '来料检验', 'material', true),
  (5, 1, CURRENT_TIMESTAMP, 1, true, 1000, 2, '来料复验', 'materialRetest', true),
  (6, 1, CURRENT_TIMESTAMP, 1, true, 1000, 3, '其他检验', 'other', true),
  (7, 1, CURRENT_TIMESTAMP, 1, true, 1000, 3, '其他复验', 'otherRetest', true),
  (8, 1, CURRENT_TIMESTAMP, 1, true, 1000, 4, '质量巡检', 'quality', true)
ON CONFLICT (id) DO NOTHING;
