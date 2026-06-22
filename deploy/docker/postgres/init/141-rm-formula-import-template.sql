-- Recovered RM batch formula import-template metadata.
--
-- The recovered PostgreSQL runtime has RM formula property metadata, but it does
-- not include a runtime_import_template/ec_import_template row for the formula
-- entity. The legacy downloadXls endpoint then attempts to synthesize a template
-- from all formula child metadata and emits an empty 200 response after looking
-- up missing associated child properties. Keep the template intentionally scoped
-- to main formula fields so template download/import can use the stable master
-- table contract first.

DO $$
DECLARE
    template_code text := 'RM_1.0.0_formula_Formula';
    template_xml text := '<?xml version="1.0" encoding="UTF-8"?>'
        || '<list>'
        || '<list-item><name><![CDATA[formualCode]]></name><dispalyName><![CDATA[RM.formula.Formula.formualCode]]></dispalyName><propertyCode><![CDATA[RM_1.0.0_formula_Formula_formualCode]]></propertyCode><namekey><![CDATA[RM.formula.Formula.formualCode]]></namekey><multable><![CDATA[false]]></multable><seniorsystemcode><![CDATA[false]]></seniorsystemcode><propshowformat><![CDATA[TEXT]]></propshowformat><containLower><![CDATA[false]]></containLower><columnName><![CDATA[formualCode]]></columnName><layRec><![CDATA[formualCode]]></layRec><key><![CDATA[formualCode]]></key><showType><![CDATA[TEXTFIELD]]></showType><nullable><![CDATA[false]]></nullable><columntype><![CDATA[BAPCODE]]></columntype><isCustom><![CDATA[false]]></isCustom></list-item>'
        || '<list-item><name><![CDATA[formulaName]]></name><dispalyName><![CDATA[RM.formula.Formula.formulaName]]></dispalyName><propertyCode><![CDATA[RM_1.0.0_formula_Formula_formulaName]]></propertyCode><namekey><![CDATA[RM.formula.Formula.formulaName]]></namekey><multable><![CDATA[false]]></multable><seniorsystemcode><![CDATA[false]]></seniorsystemcode><propshowformat><![CDATA[TEXT]]></propshowformat><containLower><![CDATA[false]]></containLower><columnName><![CDATA[formulaName]]></columnName><layRec><![CDATA[formulaName]]></layRec><key><![CDATA[formulaName]]></key><showType><![CDATA[TEXTFIELD]]></showType><nullable><![CDATA[false]]></nullable><columntype><![CDATA[TEXT]]></columntype><isCustom><![CDATA[false]]></isCustom></list-item>'
        || '<list-item><name><![CDATA[formulaEdtion]]></name><dispalyName><![CDATA[RM.formula.Formula.formulaEdtion]]></dispalyName><propertyCode><![CDATA[RM_1.0.0_formula_Formula_formulaEdtion]]></propertyCode><namekey><![CDATA[RM.formula.Formula.formulaEdtion]]></namekey><multable><![CDATA[false]]></multable><seniorsystemcode><![CDATA[false]]></seniorsystemcode><propshowformat><![CDATA[TEXT]]></propshowformat><containLower><![CDATA[false]]></containLower><columnName><![CDATA[formulaEdtion]]></columnName><layRec><![CDATA[formulaEdtion]]></layRec><key><![CDATA[formulaEdtion]]></key><showType><![CDATA[TEXTFIELD]]></showType><nullable><![CDATA[true]]></nullable><columntype><![CDATA[TEXT]]></columntype><isCustom><![CDATA[false]]></isCustom></list-item>'
        || '<list-item><name><![CDATA[productId.code]]></name><dispalyName><![CDATA[RM.formula.Formula.productId,BaseSet.material.Material.code]]></dispalyName><propertyCode><![CDATA[RM_1.0.0_formula_Formula_productId||BaseSet_1.0.0_material_Material_code]]></propertyCode><namekey><![CDATA[RM.formula.Formula.productId,BaseSet.material.Material.code]]></namekey><multable><![CDATA[false]]></multable><seniorsystemcode><![CDATA[false]]></seniorsystemcode><propshowformat><![CDATA[TEXT]]></propshowformat><containLower><![CDATA[false]]></containLower><assPropertyName><![CDATA[code]]></assPropertyName><columnName><![CDATA[productId.code]]></columnName><layRec><![CDATA[BASESET_MATERIALS,ID,RM_FORMULAS,PRODUCT_ID-code]]></layRec><key><![CDATA[productId.code]]></key><nullable><![CDATA[false]]></nullable><columntype><![CDATA[TEXT]]></columntype><isCustom><![CDATA[false]]></isCustom></list-item>'
        || '<list-item><name><![CDATA[batchFormulaID]]></name><dispalyName><![CDATA[RM.formula.Formula.batchFormulaID]]></dispalyName><propertyCode><![CDATA[RM_1.0.0_formula_Formula_batchFormulaID]]></propertyCode><namekey><![CDATA[RM.formula.Formula.batchFormulaID]]></namekey><multable><![CDATA[false]]></multable><seniorsystemcode><![CDATA[false]]></seniorsystemcode><propshowformat><![CDATA[TEXT]]></propshowformat><containLower><![CDATA[false]]></containLower><columnName><![CDATA[batchFormulaID]]></columnName><layRec><![CDATA[batchFormulaID]]></layRec><key><![CDATA[batchFormulaID]]></key><showType><![CDATA[TEXTFIELD]]></showType><nullable><![CDATA[true]]></nullable><columntype><![CDATA[TEXT]]></columntype><isCustom><![CDATA[false]]></isCustom></list-item>'
        || '<list-item><name><![CDATA[batchFormulaCode]]></name><dispalyName><![CDATA[RM.formula.Formula.batchFormulaCode]]></dispalyName><propertyCode><![CDATA[RM_1.0.0_formula_Formula_batchFormulaCode]]></propertyCode><namekey><![CDATA[RM.formula.Formula.batchFormulaCode]]></namekey><multable><![CDATA[false]]></multable><seniorsystemcode><![CDATA[false]]></seniorsystemcode><propshowformat><![CDATA[TEXT]]></propshowformat><containLower><![CDATA[false]]></containLower><columnName><![CDATA[batchFormulaCode]]></columnName><layRec><![CDATA[batchFormulaCode]]></layRec><key><![CDATA[batchFormulaCode]]></key><showType><![CDATA[TEXTFIELD]]></showType><nullable><![CDATA[true]]></nullable><columntype><![CDATA[TEXT]]></columntype><isCustom><![CDATA[false]]></isCustom></list-item>'
        || '<list-item><name><![CDATA[batchFormulaEdition]]></name><dispalyName><![CDATA[RM.formula.Formula.batchFormulaEdition]]></dispalyName><propertyCode><![CDATA[RM_1.0.0_formula_Formula_batchFormulaEdition]]></propertyCode><namekey><![CDATA[RM.formula.Formula.batchFormulaEdition]]></namekey><multable><![CDATA[false]]></multable><seniorsystemcode><![CDATA[false]]></seniorsystemcode><propshowformat><![CDATA[TEXT]]></propshowformat><containLower><![CDATA[false]]></containLower><columnName><![CDATA[batchFormulaEdition]]></columnName><layRec><![CDATA[batchFormulaEdition]]></layRec><key><![CDATA[batchFormulaEdition]]></key><showType><![CDATA[TEXTFIELD]]></showType><nullable><![CDATA[true]]></nullable><columntype><![CDATA[TEXT]]></columntype><isCustom><![CDATA[false]]></isCustom></list-item>'
        || '<list-item><name><![CDATA[norSize]]></name><dispalyName><![CDATA[RM.formula.Formula.norSize]]></dispalyName><propertyCode><![CDATA[RM_1.0.0_formula_Formula_norSize]]></propertyCode><namekey><![CDATA[RM.formula.Formula.norSize]]></namekey><multable><![CDATA[false]]></multable><seniorsystemcode><![CDATA[false]]></seniorsystemcode><propshowformat><![CDATA[TEXT]]></propshowformat><containLower><![CDATA[false]]></containLower><columnName><![CDATA[norSize]]></columnName><layRec><![CDATA[norSize]]></layRec><key><![CDATA[norSize]]></key><showType><![CDATA[TEXTFIELD]]></showType><nullable><![CDATA[true]]></nullable><columntype><![CDATA[TEXT]]></columntype><isCustom><![CDATA[false]]></isCustom></list-item>'
        || '<list-item><name><![CDATA[description]]></name><dispalyName><![CDATA[RM.formula.Formula.description]]></dispalyName><propertyCode><![CDATA[RM_1.0.0_formula_Formula_description]]></propertyCode><namekey><![CDATA[RM.formula.Formula.description]]></namekey><multable><![CDATA[false]]></multable><seniorsystemcode><![CDATA[false]]></seniorsystemcode><propshowformat><![CDATA[TEXT]]></propshowformat><containLower><![CDATA[false]]></containLower><columnName><![CDATA[description]]></columnName><layRec><![CDATA[description]]></layRec><key><![CDATA[description]]></key><showType><![CDATA[TEXTFIELD]]></showType><nullable><![CDATA[true]]></nullable><columntype><![CDATA[LONGTEXT]]></columntype><isCustom><![CDATA[false]]></isCustom></list-item>'
        || '</list>';
    actual_xml_oid oid;
BEGIN
    actual_xml_oid := lo_from_bytea(0, convert_to(template_xml, 'UTF8'));

    INSERT INTO public.ec_import_template (code, ec_env, version, proj_flag, value)
    VALUES (template_code, 'product', 0, 0, template_xml)
    ON CONFLICT (code) DO UPDATE
    SET ec_env = EXCLUDED.ec_env,
        version = EXCLUDED.version,
        proj_flag = EXCLUDED.proj_flag,
        value = EXCLUDED.value;

    INSERT INTO public.runtime_import_template (code, ec_env, version, proj_flag, value)
    VALUES (template_code, 'product', 0, false, actual_xml_oid)
    ON CONFLICT (code) DO UPDATE
    SET ec_env = EXCLUDED.ec_env,
        version = EXCLUDED.version,
        proj_flag = EXCLUDED.proj_flag,
        value = EXCLUDED.value;
END $$;

-- Recover object-property association metadata for the formula model. The
-- original module.xml carries these codes, but the restored PostgreSQL runtime
-- had them empty, which makes ExportServiceImpl dereference a null associated
-- property while building the generated Excel query config.
DO $$
DECLARE
    item record;
BEGIN
    FOR item IN
        SELECT *
        FROM (
            VALUES
                ('RM_1.0.0_formula_Formula_batchServerId', 'BaseSet_1.0.0_otherSystem_OtherSystem_id'),
                ('RM_1.0.0_formula_Formula_createDepartment', 'base_department_id'),
                ('RM_1.0.0_formula_Formula_createPosition', 'base_position_id'),
                ('RM_1.0.0_formula_Formula_createStaff', 'base_staff_id'),
                ('RM_1.0.0_formula_Formula_deleteStaff', 'base_staff_id'),
                ('RM_1.0.0_formula_Formula_effectStaff', 'base_staff_id'),
                ('RM_1.0.0_formula_Formula_formulaBom', 'RM_1.0.0_formulaBOM_FormulaBomMain_id'),
                ('RM_1.0.0_formula_Formula_formulaTypeId', 'RM_1.0.0_formulaType_FormulaType_id'),
                ('RM_1.0.0_formula_Formula_inspectSystem', 'BaseSet_1.0.0_otherSystem_OtherSystem_id'),
                ('RM_1.0.0_formula_Formula_modifyStaff', 'base_staff_id'),
                ('RM_1.0.0_formula_Formula_ownerDepartment', 'base_department_id'),
                ('RM_1.0.0_formula_Formula_ownerPosition', 'base_position_id'),
                ('RM_1.0.0_formula_Formula_ownerStaff', 'base_staff_id'),
                ('RM_1.0.0_formula_Formula_productId', 'BaseSet_1.0.0_material_Material_id'),
                ('RM_1.0.0_formula_Formula_qualityStdId', 'RM_1.0.0_rmQualityStd_RmQualityStd_id'),
                ('RM_1.0.0_formula_Formula_rejectSystem', 'BaseSet_1.0.0_otherSystem_OtherSystem_id')
        ) AS associations(property_code, associated_property_code)
    LOOP
        IF NOT EXISTS (
            SELECT 1
            FROM public.runtime_property
            WHERE code = item.associated_property_code
        ) THEN
            RAISE EXCEPTION 'Missing associated runtime property % for %',
                item.associated_property_code,
                item.property_code;
        END IF;

        UPDATE public.runtime_property
        SET associated_property_code = item.associated_property_code
        WHERE code = item.property_code
          AND (associated_property_code IS NULL OR associated_property_code = '');

        UPDATE public.ec_property
        SET associated_property_code = item.associated_property_code
        WHERE code = item.property_code
          AND (associated_property_code IS NULL OR associated_property_code = '');
    END LOOP;
END $$;

-- Recover the query metadata used by the legacy list/export pipeline for the
-- batch formula view. Without these rows the restored page renders as a shell
-- and the downloadXls endpoint reaches ExportServiceImpl with a null query
-- config.
DO $$
DECLARE
    view_code text := 'RM_1.0.0_formula_batchFormulaList';
    model_code text := 'RM_1.0.0_formula_Formula';
    list_sql text := 'SELECT "formula".ID AS "id","formula".VERSION AS "version","formula".CID AS "cid","formula".STATUS AS "status","formula".TABLE_INFO_ID AS "tableInfoId","formula".TABLE_NO AS "tableNo","formula".FORMUAL_CODE AS "formualCode","formula".FORMULA_NAME AS "formulaName","formula".FORMULA_EDTION AS "formulaEdtion","productId".CODE AS "productId.code","productId".NAME AS "productId.name","formula".BATCH_FORMULAID AS "batchFormulaID","formula".BATCH_FORMULA_CODE AS "batchFormulaCode","formula".BATCH_FORMULA_EDITION AS "batchFormulaEdition","formula".BATCH_SERVER_ID AS "batchServerId","formula".BATCH_STATUS AS "batchStatus","formula".NOR_SIZE AS "norSize","formula".STATE AS "state","formula".DESCRIPTION AS "description","productId".ID AS "productId.id","formula".VALID AS "valid" FROM RM_FORMULAS "formula" LEFT OUTER JOIN BASESET_MATERIALS "productId" ON "productId".ID = "formula".PRODUCT_ID';
    fast_query_xml text := '<queryConfig><config><fastQueryJson><list>'
        || '<list-item><name><![CDATA[tableNo]]></name><columnType><![CDATA[TEXT]]></columnType><exp><![CDATA[like]]></exp><propertyCode><![CDATA[RM_1.0.0_formula_Formula_tableNo]]></propertyCode><selfType><![CDATA[TEXT]]></selfType><partDepend><![CDATA[common]]></partDepend><multable><![CDATA[false]]></multable><containLower><![CDATA[false]]></containLower><caseSensitive><![CDATA[false]]></caseSensitive><modelCode><![CDATA[RM_1.0.0_formula_Formula]]></modelCode><layRec><![CDATA[tableNo]]></layRec><key><![CDATA[tableNo]]></key><showType><![CDATA[TEXTFIELD]]></showType><showFormat><![CDATA[TEXT]]></showFormat><entityCode><![CDATA[RM_1.0.0_formula]]></entityCode><moduleCode><![CDATA[RM_1.0.0]]></moduleCode><columnName><![CDATA[TABLE_NO]]></columnName></list-item>'
        || '<list-item><name><![CDATA[formualCode]]></name><columnType><![CDATA[BAPCODE]]></columnType><exp><![CDATA[like]]></exp><propertyCode><![CDATA[RM_1.0.0_formula_Formula_formualCode]]></propertyCode><selfType><![CDATA[BAPCODE]]></selfType><partDepend><![CDATA[common]]></partDepend><multable><![CDATA[false]]></multable><containLower><![CDATA[false]]></containLower><caseSensitive><![CDATA[false]]></caseSensitive><modelCode><![CDATA[RM_1.0.0_formula_Formula]]></modelCode><layRec><![CDATA[formualCode]]></layRec><key><![CDATA[formualCode]]></key><showType><![CDATA[TEXTFIELD]]></showType><showFormat><![CDATA[TEXT]]></showFormat><entityCode><![CDATA[RM_1.0.0_formula]]></entityCode><moduleCode><![CDATA[RM_1.0.0]]></moduleCode><columnName><![CDATA[FORMUAL_CODE]]></columnName></list-item>'
        || '<list-item><name><![CDATA[formulaName]]></name><columnType><![CDATA[TEXT]]></columnType><exp><![CDATA[like]]></exp><propertyCode><![CDATA[RM_1.0.0_formula_Formula_formulaName]]></propertyCode><selfType><![CDATA[TEXT]]></selfType><partDepend><![CDATA[common]]></partDepend><multable><![CDATA[false]]></multable><containLower><![CDATA[false]]></containLower><caseSensitive><![CDATA[false]]></caseSensitive><modelCode><![CDATA[RM_1.0.0_formula_Formula]]></modelCode><layRec><![CDATA[formulaName]]></layRec><key><![CDATA[formulaName]]></key><showType><![CDATA[TEXTFIELD]]></showType><showFormat><![CDATA[TEXT]]></showFormat><entityCode><![CDATA[RM_1.0.0_formula]]></entityCode><moduleCode><![CDATA[RM_1.0.0]]></moduleCode><columnName><![CDATA[FORMULA_NAME]]></columnName></list-item>'
        || '</list></fastQueryJson></config></queryConfig>';
    adv_query_xml text := '<queryConfig><config><advQueryJson><list>'
        || '<list-item><name><![CDATA[tableNo]]></name><columnType><![CDATA[TEXT]]></columnType><exp><![CDATA[like]]></exp><propertyCode><![CDATA[RM_1.0.0_formula_Formula_tableNo]]></propertyCode><selfType><![CDATA[TEXT]]></selfType><partDepend><![CDATA[common]]></partDepend><multable><![CDATA[false]]></multable><containLower><![CDATA[false]]></containLower><caseSensitive><![CDATA[false]]></caseSensitive><modelCode><![CDATA[RM_1.0.0_formula_Formula]]></modelCode><layRec><![CDATA[tableNo]]></layRec><key><![CDATA[tableNo]]></key><showType><![CDATA[TEXTFIELD]]></showType><showFormat><![CDATA[TEXT]]></showFormat><entityCode><![CDATA[RM_1.0.0_formula]]></entityCode><moduleCode><![CDATA[RM_1.0.0]]></moduleCode><columnName><![CDATA[TABLE_NO]]></columnName></list-item>'
        || '<list-item><name><![CDATA[formualCode]]></name><columnType><![CDATA[BAPCODE]]></columnType><exp><![CDATA[like]]></exp><propertyCode><![CDATA[RM_1.0.0_formula_Formula_formualCode]]></propertyCode><selfType><![CDATA[BAPCODE]]></selfType><partDepend><![CDATA[common]]></partDepend><multable><![CDATA[false]]></multable><containLower><![CDATA[false]]></containLower><caseSensitive><![CDATA[false]]></caseSensitive><modelCode><![CDATA[RM_1.0.0_formula_Formula]]></modelCode><layRec><![CDATA[formualCode]]></layRec><key><![CDATA[formualCode]]></key><showType><![CDATA[TEXTFIELD]]></showType><showFormat><![CDATA[TEXT]]></showFormat><entityCode><![CDATA[RM_1.0.0_formula]]></entityCode><moduleCode><![CDATA[RM_1.0.0]]></moduleCode><columnName><![CDATA[FORMUAL_CODE]]></columnName></list-item>'
        || '<list-item><name><![CDATA[formulaName]]></name><columnType><![CDATA[TEXT]]></columnType><exp><![CDATA[like]]></exp><propertyCode><![CDATA[RM_1.0.0_formula_Formula_formulaName]]></propertyCode><selfType><![CDATA[TEXT]]></selfType><partDepend><![CDATA[common]]></partDepend><multable><![CDATA[false]]></multable><containLower><![CDATA[false]]></containLower><caseSensitive><![CDATA[false]]></caseSensitive><modelCode><![CDATA[RM_1.0.0_formula_Formula]]></modelCode><layRec><![CDATA[formulaName]]></layRec><key><![CDATA[formulaName]]></key><showType><![CDATA[TEXTFIELD]]></showType><showFormat><![CDATA[TEXT]]></showFormat><entityCode><![CDATA[RM_1.0.0_formula]]></entityCode><moduleCode><![CDATA[RM_1.0.0]]></moduleCode><columnName><![CDATA[FORMULA_NAME]]></columnName></list-item>'
        || '</list></advQueryJson></config></queryConfig>';
    runtime_extra_view_payload text := '{"pageType":"LIST","title":"RM.viewtitle.randon1583373640631","url":"/msService/RM/formula/formula/batchFormulaList","isMain":true,"hasAttachment":false,"onlyForQuery":false,"components":[{"type":"layout","layoutmethod":"column","components":[{"type":"layoutSearchWidget","code":"query","layoutName":"layoutSearchWidget","layoutmethod":"container","fix_h":150,"fastProperty":[],"advProperty":[]},{"type":"layoutDatagrid","layoutmethod":"container","ratio_h":100,"DataGridCode":"RM_1.0.0_formula_batchFormulaList","modelCode":"RM_1.0.0_formula_Formula","hasFastQuery":true,"mainDisplayName":"formulaName","idPrefix":"compat_RM_1.0.0_formula_batchFormulaList","listPT":false,"buttons":[{"id":"rmBatchFormulaDownloadTemplate","showname":"下载模板","namekey":"下载模板","buttonstyle":"eighteen-dt-op-download-import-template","operatetype":"CUSTOM","operateType":"CUSTOM","isHide":false,"ispermission":false,"isPublished":false,"buttonoperationcode":"RM_1.0.0_formula_batchFormulaList_downloadXls","funcname":"onclick=''rmBatchFormulaDownloadTemplate(event)''","funcbody":"function rmBatchFormulaDownloadTemplate(event) { window.open(''/msService/RM/formula/formula/downloadXls'', ''_blank''); }","funcbody_es5":"function rmBatchFormulaDownloadTemplate(event) { window.open(''/msService/RM/formula/formula/downloadXls'', ''_blank''); }","iscallback":"false","iscustomfunc":"false","useInMore":"false","isconfirm":false,"cellCode":"cell_rm_batch_formula_download","regionType":"BUTTON","i18nKey":"下载模板","name":"下载模板","onclick":"rmBatchFormulaDownloadTemplate(event)","ONCLICK":"rmBatchFormulaDownloadTemplate(event)","CODE":"RM_1.0.0_formula_batchFormulaList_downloadXls","NAME":"下载模板","ICONCLS":"cui-btn-eighteen-dt-op-download-import-template","USEINMORE":"false","SEPARATENUM":"0"},{"id":"rmBatchFormulaImportMainXls","showname":"导入Excel","namekey":"导入Excel","buttonstyle":"eighteen-dt-op-upload-sourcecode","operatetype":"CUSTOM","operateType":"CUSTOM","isHide":false,"ispermission":false,"isPublished":false,"buttonoperationcode":"RM_1.0.0_formula_batchFormulaList_importMainXls","funcname":"onclick=''rmBatchFormulaImportMainXls(event)''","funcbody":"function rmBatchFormulaImportMainXls(event) { var input = document.createElement(''input''); input.type = ''file''; input.accept = ''.xls,.xlsx''; input.style.display = ''none''; input.onchange = function() { var file = input.files && input.files[0]; if (input.parentNode) { input.parentNode.removeChild(input); } if (!file) { return; } var formData = new FormData(); formData.append(''file'', file); var notice = function(type, msg) { if (window.ReactAPI && ReactAPI.showMessage) { ReactAPI.showMessage(type, msg); } else { alert(msg); } }; $.ajax({ url: ''/inter-api/file-server/v1/file/upload/file'', type: ''post'', data: formData, processData: false, contentType: false, success: function(uploadRes) { var data = uploadRes && uploadRes.data ? uploadRes.data : {}; var path = data.path || data.filePath; if (!path) { notice(''f'', ''文件上传失败''); return; } $.ajax({ url: ''/msService/RM/formula/formula/importMainXls'', type: ''get'', data: { filePath: path, viewCode: ''RM_1.0.0_formula_batchFormulaList'', isReplace: false, isIgnore: false }, success: function(res) { if (res && res.success === false) { notice(''f'', res.msg || ''导入失败''); return; } notice(''s'', ''导入完成''); if (window.ReactAPI && ReactAPI.getComponentAPI) { try { ReactAPI.getComponentAPI(''ListView'').SearchList.submitEditDialogCallback(); } catch (e) {} } }, error: function(xhr) { notice(''f'', ''导入接口失败:'' + xhr.status); } }); }, error: function(xhr) { notice(''f'', ''文件上传失败:'' + xhr.status); } }); }; document.body.appendChild(input); input.click(); }","funcbody_es5":"function rmBatchFormulaImportMainXls(event) { var input = document.createElement(''input''); input.type = ''file''; input.accept = ''.xls,.xlsx''; input.style.display = ''none''; input.onchange = function() { var file = input.files && input.files[0]; if (input.parentNode) { input.parentNode.removeChild(input); } if (!file) { return; } var formData = new FormData(); formData.append(''file'', file); var notice = function(type, msg) { if (window.ReactAPI && ReactAPI.showMessage) { ReactAPI.showMessage(type, msg); } else { alert(msg); } }; $.ajax({ url: ''/inter-api/file-server/v1/file/upload/file'', type: ''post'', data: formData, processData: false, contentType: false, success: function(uploadRes) { var data = uploadRes && uploadRes.data ? uploadRes.data : {}; var path = data.path || data.filePath; if (!path) { notice(''f'', ''文件上传失败''); return; } $.ajax({ url: ''/msService/RM/formula/formula/importMainXls'', type: ''get'', data: { filePath: path, viewCode: ''RM_1.0.0_formula_batchFormulaList'', isReplace: false, isIgnore: false }, success: function(res) { if (res && res.success === false) { notice(''f'', res.msg || ''导入失败''); return; } notice(''s'', ''导入完成''); if (window.ReactAPI && ReactAPI.getComponentAPI) { try { ReactAPI.getComponentAPI(''ListView'').SearchList.submitEditDialogCallback(); } catch (e) {} } }, error: function(xhr) { notice(''f'', ''导入接口失败:'' + xhr.status); } }); }, error: function(xhr) { notice(''f'', ''文件上传失败:'' + xhr.status); } }); }; document.body.appendChild(input); input.click(); }","iscallback":"false","iscustomfunc":"false","useInMore":"false","isconfirm":false,"cellCode":"cell_rm_batch_formula_import","regionType":"BUTTON","i18nKey":"导入Excel","name":"导入Excel","onclick":"rmBatchFormulaImportMainXls(event)","ONCLICK":"rmBatchFormulaImportMainXls(event)","CODE":"RM_1.0.0_formula_batchFormulaList_importMainXls","NAME":"导入Excel","ICONCLS":"cui-btn-eighteen-dt-op-upload-sourcecode","USEINMORE":"false","SEPARATENUM":"0"}],"fields":[{"key":"tableNo","namekey":"ec.common.tableNo","showType":"TEXTFIELD","showFormat":"TEXT","width":150,"isHidden":false,"columnType":"TEXT"},{"key":"formualCode","namekey":"RM.formula.Formula.formualCode","showType":"TEXTFIELD","showFormat":"TEXT","width":140,"isHidden":false,"columnType":"BAPCODE"},{"key":"formulaName","namekey":"RM.formula.Formula.formulaName","showType":"TEXTFIELD","showFormat":"TEXT","width":160,"isHidden":false,"columnType":"TEXT"},{"key":"formulaEdtion","namekey":"RM.formula.Formula.formulaEdtion","showType":"TEXTFIELD","showFormat":"TEXT","width":110,"isHidden":false,"columnType":"TEXT"},{"key":"productId.code","namekey":"RM.formula.Formula.productId,BaseSet.material.Material.code","showType":"TEXTFIELD","showFormat":"TEXT","width":120,"isHidden":false,"columnType":"TEXT"},{"key":"batchFormulaID","namekey":"RM.formula.Formula.batchFormulaID","showType":"TEXTFIELD","showFormat":"TEXT","width":140,"isHidden":false,"columnType":"TEXT"},{"key":"batchFormulaCode","namekey":"RM.formula.Formula.batchFormulaCode","showType":"TEXTFIELD","showFormat":"TEXT","width":140,"isHidden":false,"columnType":"TEXT"},{"key":"batchFormulaEdition","namekey":"RM.formula.Formula.batchFormulaEdition","showType":"TEXTFIELD","showFormat":"TEXT","width":120,"isHidden":false,"columnType":"TEXT"},{"key":"norSize","namekey":"RM.formula.Formula.norSize","showType":"TEXTFIELD","showFormat":"TEXT","width":100,"isHidden":false,"columnType":"TEXT"},{"key":"description","namekey":"RM.formula.Formula.description","showType":"TEXTFIELD","showFormat":"TEXT","width":180,"isHidden":false,"columnType":"LONGTEXT"}],"downloadXls":"/msService/RM/formula/formula/downloadXls","importMainXls":"/msService/RM/formula/formula/importMainXls"}]}],"isFileView":false,"moveFlag":false}';
    runtime_extra_view_json_is_oid boolean;
BEGIN
    INSERT INTO public.runtime_sql (code, ec_env, version, proj_flag, data_grid_code, view_code, type, query_sql)
    VALUES
        (view_code || '_3', 'product', 0, false, NULL, view_code, 3, 'SELECT COUNT(*) count FROM'),
        (view_code || '_6', 'product', 0, false, NULL, view_code, 6, list_sql)
    ON CONFLICT (code) DO UPDATE
    SET ec_env = EXCLUDED.ec_env,
        version = EXCLUDED.version,
        proj_flag = EXCLUDED.proj_flag,
        data_grid_code = EXCLUDED.data_grid_code,
        view_code = EXCLUDED.view_code,
        type = EXCLUDED.type,
        query_sql = EXCLUDED.query_sql;

    INSERT INTO public.ec_sql (code, ec_env, version, proj_flag, data_grid_code, view_code, type, query_sql)
    VALUES
        (view_code || '_3', 'product', 0, 0, NULL, view_code, 3, 'SELECT COUNT(*) count FROM'),
        (view_code || '_6', 'product', 0, 0, NULL, view_code, 6, list_sql)
    ON CONFLICT (code) DO UPDATE
    SET ec_env = EXCLUDED.ec_env,
        version = EXCLUDED.version,
        proj_flag = EXCLUDED.proj_flag,
        data_grid_code = EXCLUDED.data_grid_code,
        view_code = EXCLUDED.view_code,
        type = EXCLUDED.type,
        query_sql = EXCLUDED.query_sql;

    INSERT INTO public.ec_fast_query_json (code, ec_env, version, targetmodel_code, proj_flag, layout_name, query_config, view_code)
    VALUES (view_code, 'product', 0, model_code, 0, NULL, fast_query_xml, view_code)
    ON CONFLICT (code) DO UPDATE
    SET ec_env = EXCLUDED.ec_env,
        version = EXCLUDED.version,
        targetmodel_code = EXCLUDED.targetmodel_code,
        proj_flag = EXCLUDED.proj_flag,
        layout_name = EXCLUDED.layout_name,
        query_config = EXCLUDED.query_config,
        view_code = EXCLUDED.view_code;

    INSERT INTO public.ec_adv_query_json (code, ec_env, version, targetmodel_code, proj_flag, name, layout_name, query_config, view_code)
    VALUES (view_code, 'product', 0, model_code, 0, NULL, NULL, adv_query_xml, view_code)
    ON CONFLICT (code) DO UPDATE
    SET ec_env = EXCLUDED.ec_env,
        version = EXCLUDED.version,
        targetmodel_code = EXCLUDED.targetmodel_code,
        proj_flag = EXCLUDED.proj_flag,
        name = EXCLUDED.name,
        layout_name = EXCLUDED.layout_name,
        query_config = EXCLUDED.query_config,
        view_code = EXCLUDED.view_code;

    INSERT INTO public.runtime_fast_query_json (code, ec_env, version, targetmodel_code, proj_flag, layout_name, query_config, view_code)
    VALUES (view_code, 'product', 0, model_code, false, NULL, lo_from_bytea(0, convert_to(fast_query_xml, 'UTF8')), view_code)
    ON CONFLICT (code) DO UPDATE
    SET ec_env = EXCLUDED.ec_env,
        version = EXCLUDED.version,
        targetmodel_code = EXCLUDED.targetmodel_code,
        proj_flag = EXCLUDED.proj_flag,
        layout_name = EXCLUDED.layout_name,
        query_config = EXCLUDED.query_config,
        view_code = EXCLUDED.view_code;

    INSERT INTO public.runtime_adv_query_json (code, ec_env, version, targetmodel_code, proj_flag, name, layout_name, query_config, view_code)
    VALUES (view_code, 'product', 0, model_code, false, NULL, NULL, lo_from_bytea(0, convert_to(adv_query_xml, 'UTF8')), view_code)
    ON CONFLICT (code) DO UPDATE
    SET ec_env = EXCLUDED.ec_env,
        version = EXCLUDED.version,
        targetmodel_code = EXCLUDED.targetmodel_code,
        proj_flag = EXCLUDED.proj_flag,
        name = EXCLUDED.name,
        layout_name = EXCLUDED.layout_name,
        query_config = EXCLUDED.query_config,
        view_code = EXCLUDED.view_code;

    SELECT udt_name = 'oid' INTO runtime_extra_view_json_is_oid
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'runtime_extra_view'
      AND column_name = 'view_json';

    IF COALESCE(runtime_extra_view_json_is_oid, false) THEN
        INSERT INTO public.runtime_extra_view (code, ec_env, version, view_code, view_json, proj_flag)
        VALUES (view_code, 'product', 0, view_code, lo_from_bytea(0, convert_to(runtime_extra_view_payload, 'UTF8')), false)
        ON CONFLICT (code) DO UPDATE
        SET ec_env = EXCLUDED.ec_env,
            version = EXCLUDED.version,
            view_code = EXCLUDED.view_code,
            view_json = EXCLUDED.view_json,
            proj_flag = EXCLUDED.proj_flag;
    ELSE
        INSERT INTO public.runtime_extra_view (code, ec_env, version, view_code, view_json, proj_flag)
        VALUES (view_code, 'product', 0, view_code, runtime_extra_view_payload, false)
        ON CONFLICT (code) DO UPDATE
        SET ec_env = EXCLUDED.ec_env,
            version = EXCLUDED.version,
            view_code = EXCLUDED.view_code,
            view_json = EXCLUDED.view_json,
            proj_flag = EXCLUDED.proj_flag;
    END IF;
END $$;
