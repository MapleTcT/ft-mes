-- Restore the PostgreSQL runtime layout JSON for the LIMS quality standard reference dialog.
-- Without runtime_extra_view.view_json, /baseService/view/layoutJson returns a NullPointerException
-- and the quality standard reference modal renders as an empty shell.

DO $$
DECLARE
    runtime_extra_view_json_is_oid boolean;
    runtime_extra_view_payload text := $json$
{
  "pageType": "LIST",
  "title": "质量标准参照",
  "url": "/msService/LIMSBasic/qualityStd/stdVersion/qualityStdVerRef",
  "isMain": false,
  "hasAttachment": false,
  "onlyForQuery": false,
  "components": [
    {
      "type": "layout",
      "layoutmethod": "column",
      "components": [
        {
          "type": "layoutSearchWidget",
          "code": "query",
          "layoutName": "layoutSearchWidget",
          "layoutmethod": "container",
          "fix_h": 120,
          "fastProperty": [],
          "advProperty": []
        },
        {
          "type": "layoutDatagrid",
          "layoutmethod": "container",
          "ratio_h": 100,
          "DataGridCode": "LIMSBasic_1.0.0_qualityStd_qualityStdVerRef",
          "modelCode": "LIMSBasic_1.0.0_qualityStd_StdVersion",
          "hasFastQuery": true,
          "mainDisplayName": "name",
          "idPrefix": "compat_LIMSBasic_1.0.0_qualityStd_qualityStdVerRef",
          "listPT": false,
          "buttons": [],
          "fields": [
            {
              "key": "stdId.name",
              "namekey": "质量标准",
              "showType": "TEXTFIELD",
              "showFormat": "TEXT",
              "width": 220,
              "isHidden": false,
              "columnType": "TEXT"
            },
            {
              "key": "stdId.standard",
              "namekey": "标准内容",
              "showType": "TEXTFIELD",
              "showFormat": "TEXT",
              "width": 220,
              "isHidden": false,
              "columnType": "TEXT"
            },
            {
              "key": "name",
              "namekey": "版本名称",
              "showType": "TEXTFIELD",
              "showFormat": "TEXT",
              "width": 220,
              "isHidden": false,
              "columnType": "TEXT"
            },
            {
              "key": "busiVersion",
              "namekey": "业务版本",
              "showType": "TEXTFIELD",
              "showFormat": "TEXT",
              "width": 120,
              "isHidden": false,
              "columnType": "TEXT"
            },
            {
              "key": "startDate",
              "namekey": "生效日期",
              "showType": "DATE",
              "showFormat": "DATE",
              "width": 160,
              "isHidden": false,
              "columnType": "DATE"
            },
            {
              "key": "memoField",
              "namekey": "备注",
              "showType": "TEXTFIELD",
              "showFormat": "TEXT",
              "width": 180,
              "isHidden": false,
              "columnType": "TEXT"
            }
          ],
          "downloadXls": "/msService/LIMSBasic/qualityStd/stdVersion/downloadXls",
          "importMainXls": "/msService/LIMSBasic/qualityStd/stdVersion/importMainXls"
        }
      ]
    }
  ],
  "isFileView": true,
  "moveFlag": false
}
$json$;
BEGIN
    SELECT udt_name = 'oid' INTO runtime_extra_view_json_is_oid
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'runtime_extra_view'
      AND column_name = 'view_json';

    IF COALESCE(runtime_extra_view_json_is_oid, false) THEN
        INSERT INTO public.runtime_extra_view (code, ec_env, version, view_code, view_json, proj_flag)
        VALUES (
            'LIMSBasic_1.0.0_qualityStd_qualityStdVerRef',
            'product',
            0,
            'LIMSBasic_1.0.0_qualityStd_qualityStdVerRef',
            lo_from_bytea(0, convert_to(runtime_extra_view_payload, 'UTF8')),
            false
        )
        ON CONFLICT (code) DO UPDATE SET
            ec_env = COALESCE(EXCLUDED.ec_env, public.runtime_extra_view.ec_env),
            version = GREATEST(COALESCE(public.runtime_extra_view.version, 0), EXCLUDED.version),
            view_code = EXCLUDED.view_code,
            view_json = EXCLUDED.view_json,
            proj_flag = COALESCE(public.runtime_extra_view.proj_flag, EXCLUDED.proj_flag);
    ELSE
        INSERT INTO public.runtime_extra_view (code, ec_env, version, view_code, view_json, proj_flag)
        VALUES (
            'LIMSBasic_1.0.0_qualityStd_qualityStdVerRef',
            'product',
            0,
            'LIMSBasic_1.0.0_qualityStd_qualityStdVerRef',
            runtime_extra_view_payload,
            false
        )
        ON CONFLICT (code) DO UPDATE SET
            ec_env = COALESCE(EXCLUDED.ec_env, public.runtime_extra_view.ec_env),
            version = GREATEST(COALESCE(public.runtime_extra_view.version, 0), EXCLUDED.version),
            view_code = EXCLUDED.view_code,
            view_json = EXCLUDED.view_json,
            proj_flag = COALESCE(public.runtime_extra_view.proj_flag, EXCLUDED.proj_flag);
    END IF;
END $$;
