-- Keep the normalized button tables aligned with the restored embedded view
-- actions.  The permission_code is the parent-qualified RBAC operation while
-- button_operation_code remains the original packaged child action code.
WITH action_buttons(code, operation_code, permission_code) AS (
    VALUES
        ('LIMSSample_5.0.0.0_sample_sampleRegisterPart_BUTTON_register', 'sampleRegisterPart_register_add_LIMSSample_5.0.0.0_sample_sampleRegisterPart', 'LIMSSample_5.0.0.0_sample_sampleRegisterLayout_sampleRegisterPart_register_add_LIMSSample_5.0.0.0_sample_sampleRegisterPart'),
        ('LIMSSample_5.0.0.0_sample_sampleRegisterPart_BUTTON_setTestItem', 'sampleRegisterPart_setTestItem_modify_LIMSSample_5.0.0.0_sample_sampleRegisterPart', 'LIMSSample_5.0.0.0_sample_sampleRegisterLayout_sampleRegisterPart_setTestItem_modify_LIMSSample_5.0.0.0_sample_sampleRegisterPart'),
        ('LIMSSample_5.0.0.0_sample_sampleRegisterPart_BUTTON_cancel', 'sampleRegisterPart_cancel_del_LIMSSample_5.0.0.0_sample_sampleRegisterPart', 'LIMSSample_5.0.0.0_sample_sampleRegisterLayout_sampleRegisterPart_cancel_del_LIMSSample_5.0.0.0_sample_sampleRegisterPart'),
        ('LIMSBasic_1.0.0_testPlan_planSetSampleList_BUTTON_createSample', 'planSetSampleList_createSample_add_LIMSBasic_1.0.0_testPlan_planSetSampleList', 'planSetSampleList_createSample_add_LIMSBasic_1.0.0_testPlan_planSetSampleList'),
        ('LIMSSample_5.0.0.0_sample_sampTaskAlcatPart_BUTTON_taskAlcat', 'sampTaskAlcatPart_taskAlcat_add_LIMSSample_5.0.0.0_sample_sampTaskAlcatPart', 'LIMSSample_5.0.0.0_sample_sampTaskAlcatLayOut_sampTaskAlcatPart_taskAlcat_add_LIMSSample_5.0.0.0_sample_sampTaskAlcatPart'),
        ('LIMSSample_5.0.0.0_sample_collectListPart_BUTTON_collect', 'collectListPart_collect_add_LIMSSample_5.0.0.0_sample_collectListPart', 'LIMSSample_5.0.0.0_sample_collectListLayout_collectListPart_collect_add_LIMSSample_5.0.0.0_sample_collectListPart'),
        ('LIMSSample_5.0.0.0_sample_collectListPart_BUTTON_collectInfoEdit', 'collectListPart_collectInfoEdit_add_LIMSSample_5.0.0.0_sample_collectListPart', 'LIMSSample_5.0.0.0_sample_collectListLayout_collectListPart_collectInfoEdit_add_LIMSSample_5.0.0.0_sample_collectListPart'),
        ('LIMSSample_5.0.0.0_sample_receiveListPart_BUTTON_receive', 'receiveListPart_receive_add_LIMSSample_5.0.0.0_sample_receiveListPart', 'LIMSSample_5.0.0.0_sample_receiveListLayout_receiveListPart_receive_add_LIMSSample_5.0.0.0_sample_receiveListPart'),
        ('LIMSSample_5.0.0.0_sample_receiveListPart_BUTTON_setTestItem', 'receiveListPart_setTestItem_add_LIMSSample_5.0.0.0_sample_receiveListPart', 'LIMSSample_5.0.0.0_sample_receiveListLayout_receiveListPart_setTestItem_add_LIMSSample_5.0.0.0_sample_receiveListPart'),
        ('LIMSSample_5.0.0.0_sample_sampleSweepReceivedg1588230538491_BUTTON_receiveSubmit', 'sampleSweepReceive_receiveSubmit_add_LIMSSample_5.0.0.0_sample_sampleSweepReceive', 'sampleSweepReceive_receiveSubmit_add_LIMSSample_5.0.0.0_sample_sampleSweepReceive'),
        ('LIMSSample_5.0.0.0_sample_sampleSweepReceivedg1588230538491_BUTTON_del', 'sampleSweepReceive_del_del_LIMSSample_5.0.0.0_sample_sampleSweepReceive', 'sampleSweepReceive_del_del_LIMSSample_5.0.0.0_sample_sampleSweepReceive'),
        ('LIMSSample_5.0.0.0_sample_makeListPart_BUTTON_make', 'makeListPart_make_add_LIMSSample_5.0.0.0_sample_makeListPart', 'LIMSSample_5.0.0.0_sample_makeListLayout_makeListPart_make_add_LIMSSample_5.0.0.0_sample_makeListPart'),
        ('LIMSSample_5.0.0.0_sample_handoverListPart_BUTTON_handover', 'handoverListPart_handover_add_LIMSSample_5.0.0.0_sample_handoverListPart', 'LIMSSample_5.0.0.0_sample_handoverListLayout_handoverListPart_handover_add_LIMSSample_5.0.0.0_sample_handoverListPart'),
        ('LIMSSample_5.0.0.0_sample_recordBySingleSampledg1592183350560_BUTTON_devRecord', 'recordBySingleSample_devRecord_add_LIMSSample_5.0.0.0_sample_recordBySingleSample', 'recordBySingleSample_devRecord_add_LIMSSample_5.0.0.0_sample_recordBySingleSample'),
        ('LIMSSample_5.0.0.0_sample_recordBySampledg1592378259080_BUTTON_devRecord', 'recordBySample_devRecord_modify_LIMSSample_5.0.0.0_sample_recordBySample', 'recordBySample_devRecord_modify_LIMSSample_5.0.0.0_sample_recordBySample'),
        ('LIMSSample_5.0.0.0_sample_recordCheckByTestdg1598406003698_BUTTON_refresh', 'recordCheckByTest_refresh_add_LIMSSample_5.0.0.0_sample_recordCheckByTest', 'recordCheckByTest_refresh_add_LIMSSample_5.0.0.0_sample_recordCheckByTest'),
        ('LIMSSample_5.0.0.0_sample_sampleCheckdg1592385064205_BUTTON_viewSample', 'sampleCheck_viewSample_modify_LIMSSample_5.0.0.0_sample_sampleCheck', 'sampleCheck_viewSample_modify_LIMSSample_5.0.0.0_sample_sampleCheck'),
        ('LIMSSample_5.0.0.0_sample_sampleDealListPart_BUTTON_sampleInfoModify', 'sampleDealListPart_sampleInfoModify_modify_LIMSSample_5.0.0.0_sample_sampleDealListPart', 'LIMSSample_5.0.0.0_sample_sampleDealListLayout_sampleDealListPart_sampleInfoModify_modify_LIMSSample_5.0.0.0_sample_sampleDealListPart'),
        ('LIMSSample_5.0.0.0_sample_sampleDealListPart_BUTTON_setTestItem', 'sampleDealListPart_setTestItem_modify_LIMSSample_5.0.0.0_sample_sampleDealListPart', 'LIMSSample_5.0.0.0_sample_sampleDealListLayout_sampleDealListPart_setTestItem_modify_LIMSSample_5.0.0.0_sample_sampleDealListPart'),
        ('LIMSSample_5.0.0.0_sample_sampleDealListPart_BUTTON_cancel', 'sampleDealListPart_cancel_del_LIMSSample_5.0.0.0_sample_sampleDealListPart', 'LIMSSample_5.0.0.0_sample_sampleDealListLayout_sampleDealListPart_cancel_del_LIMSSample_5.0.0.0_sample_sampleDealListPart'),
        ('LIMSSample_5.0.0.0_sample_sampleDealListPart_BUTTON_recover', 'sampleDealListPart_recover_recover_LIMSSample_5.0.0.0_sample_sampleDealListPart', 'LIMSSample_5.0.0.0_sample_sampleDealListLayout_sampleDealListPart_recover_recover_LIMSSample_5.0.0.0_sample_sampleDealListPart'),
        ('LIMSSample_5.0.0.0_sample_sampleDealListPart_BUTTON_collectTestAgain', 'sampleDealListPart_collectTestAgain_add_LIMSSample_5.0.0.0_sample_sampleDealListPart', 'LIMSSample_5.0.0.0_sample_sampleDealListLayout_sampleDealListPart_collectTestAgain_add_LIMSSample_5.0.0.0_sample_sampleDealListPart'),
        ('LIMSSample_5.0.0.0_sample_sampleDealListPart_BUTTON_active', 'sampleDealListPart_active_add_LIMSSample_5.0.0.0_sample_sampleDealListPart', 'LIMSSample_5.0.0.0_sample_sampleDealListLayout_sampleDealListPart_active_add_LIMSSample_5.0.0.0_sample_sampleDealListPart'),
        ('LIMSSample_5.0.0.0_sample_sampleDealListPart_BUTTON_delete', 'sampleDealListPart_delete_del_LIMSSample_5.0.0.0_sample_sampleDealListPart', 'LIMSSample_5.0.0.0_sample_sampleDealListLayout_sampleDealListPart_delete_del_LIMSSample_5.0.0.0_sample_sampleDealListPart'),
        ('LIMSSample_5.0.0.0_sample_retainListPart_BUTTON_retain', 'retainListPart_retain_add_LIMSSample_5.0.0.0_sample_retainListPart', 'LIMSSample_5.0.0.0_sample_retainListLayout_retainListPart_retain_add_LIMSSample_5.0.0.0_sample_retainListPart'),
        ('LIMSSample_5.0.0.0_sample_sampleInfoListPart_BUTTON_sampleInfoCheck', 'sampleInfoListPart_sampleInfoCheck_add_LIMSSample_5.0.0.0_sample_sampleInfoListPart', 'LIMSSample_5.0.0.0_sample_sampleInfoLayout_sampleInfoListPart_sampleInfoCheck_add_LIMSSample_5.0.0.0_sample_sampleInfoListPart'),
        ('LIMSSample_5.0.0.0_sample_sampleInfoListPart_BUTTON_dealInfoCheck', 'sampleInfoListPart_dealInfoCheck_add_LIMSSample_5.0.0.0_sample_sampleInfoListPart', 'LIMSSample_5.0.0.0_sample_sampleInfoLayout_sampleInfoListPart_dealInfoCheck_add_LIMSSample_5.0.0.0_sample_sampleInfoListPart'),
        ('LIMSSample_5.0.0.0_sample_remainSamplePart_BUTTON_back', 'remainSamplePart_recover_recover_LIMSSample_5.0.0.0_sample_remainSamplePart', 'LIMSSample_5.0.0.0_sample_remainSampleLayout_remainSamplePart_recover_recover_LIMSSample_5.0.0.0_sample_remainSamplePart'),
        ('LIMSSample_5.0.0.0_sample_remainSamplePart_BUTTON_destroy', 'remainSamplePart_delete_del_LIMSSample_5.0.0.0_sample_remainSamplePart', 'LIMSSample_5.0.0.0_sample_remainSampleLayout_remainSamplePart_delete_del_LIMSSample_5.0.0.0_sample_remainSamplePart')
)
UPDATE public.runtime_button AS button
SET is_published = true,
    is_permission = true,
    is_hide = false,
    button_operation_code = action_buttons.operation_code,
    permission_code = action_buttons.permission_code
FROM action_buttons
WHERE button.code = action_buttons.code;

UPDATE public.ec_button AS product_button
SET is_published = runtime_button.is_published,
    is_permission = runtime_button.is_permission,
    is_hide = runtime_button.is_hide,
    button_operation_code = runtime_button.button_operation_code,
    permission_code = runtime_button.permission_code
FROM public.runtime_button AS runtime_button
WHERE product_button.code = runtime_button.code
  AND runtime_button.code IN (
      'LIMSSample_5.0.0.0_sample_sampleRegisterPart_BUTTON_register',
      'LIMSSample_5.0.0.0_sample_sampleRegisterPart_BUTTON_setTestItem',
      'LIMSSample_5.0.0.0_sample_sampleRegisterPart_BUTTON_cancel',
      'LIMSBasic_1.0.0_testPlan_planSetSampleList_BUTTON_createSample',
      'LIMSSample_5.0.0.0_sample_sampTaskAlcatPart_BUTTON_taskAlcat',
      'LIMSSample_5.0.0.0_sample_collectListPart_BUTTON_collect',
      'LIMSSample_5.0.0.0_sample_collectListPart_BUTTON_collectInfoEdit',
      'LIMSSample_5.0.0.0_sample_receiveListPart_BUTTON_receive',
      'LIMSSample_5.0.0.0_sample_receiveListPart_BUTTON_setTestItem',
      'LIMSSample_5.0.0.0_sample_sampleSweepReceivedg1588230538491_BUTTON_receiveSubmit',
      'LIMSSample_5.0.0.0_sample_sampleSweepReceivedg1588230538491_BUTTON_del',
      'LIMSSample_5.0.0.0_sample_makeListPart_BUTTON_make',
      'LIMSSample_5.0.0.0_sample_handoverListPart_BUTTON_handover',
      'LIMSSample_5.0.0.0_sample_recordBySingleSampledg1592183350560_BUTTON_devRecord',
      'LIMSSample_5.0.0.0_sample_recordBySampledg1592378259080_BUTTON_devRecord',
      'LIMSSample_5.0.0.0_sample_recordCheckByTestdg1598406003698_BUTTON_refresh',
      'LIMSSample_5.0.0.0_sample_sampleCheckdg1592385064205_BUTTON_viewSample',
      'LIMSSample_5.0.0.0_sample_sampleDealListPart_BUTTON_sampleInfoModify',
      'LIMSSample_5.0.0.0_sample_sampleDealListPart_BUTTON_setTestItem',
      'LIMSSample_5.0.0.0_sample_sampleDealListPart_BUTTON_cancel',
      'LIMSSample_5.0.0.0_sample_sampleDealListPart_BUTTON_recover',
      'LIMSSample_5.0.0.0_sample_sampleDealListPart_BUTTON_collectTestAgain',
      'LIMSSample_5.0.0.0_sample_sampleDealListPart_BUTTON_active',
      'LIMSSample_5.0.0.0_sample_sampleDealListPart_BUTTON_delete',
      'LIMSSample_5.0.0.0_sample_retainListPart_BUTTON_retain',
      'LIMSSample_5.0.0.0_sample_sampleInfoListPart_BUTTON_sampleInfoCheck',
      'LIMSSample_5.0.0.0_sample_sampleInfoListPart_BUTTON_dealInfoCheck',
      'LIMSSample_5.0.0.0_sample_remainSamplePart_BUTTON_back',
      'LIMSSample_5.0.0.0_sample_remainSamplePart_BUTTON_destroy'
  );

DO $$
DECLARE restored_runtime_buttons integer;
DECLARE restored_product_buttons integer;
DECLARE restored_views integer;
BEGIN
    SELECT count(*) INTO restored_runtime_buttons
    FROM public.runtime_button
    WHERE is_published = true
      AND is_permission = true
      AND permission_code IS NOT NULL
      AND code IN (
          'LIMSSample_5.0.0.0_sample_sampleRegisterPart_BUTTON_register',
          'LIMSSample_5.0.0.0_sample_sampleRegisterPart_BUTTON_setTestItem',
          'LIMSSample_5.0.0.0_sample_sampleRegisterPart_BUTTON_cancel',
          'LIMSBasic_1.0.0_testPlan_planSetSampleList_BUTTON_createSample',
          'LIMSSample_5.0.0.0_sample_sampTaskAlcatPart_BUTTON_taskAlcat',
          'LIMSSample_5.0.0.0_sample_collectListPart_BUTTON_collect',
          'LIMSSample_5.0.0.0_sample_collectListPart_BUTTON_collectInfoEdit',
          'LIMSSample_5.0.0.0_sample_receiveListPart_BUTTON_receive',
          'LIMSSample_5.0.0.0_sample_receiveListPart_BUTTON_setTestItem',
          'LIMSSample_5.0.0.0_sample_sampleSweepReceivedg1588230538491_BUTTON_receiveSubmit',
          'LIMSSample_5.0.0.0_sample_sampleSweepReceivedg1588230538491_BUTTON_del',
          'LIMSSample_5.0.0.0_sample_makeListPart_BUTTON_make',
          'LIMSSample_5.0.0.0_sample_handoverListPart_BUTTON_handover',
          'LIMSSample_5.0.0.0_sample_recordBySingleSampledg1592183350560_BUTTON_devRecord',
          'LIMSSample_5.0.0.0_sample_recordBySampledg1592378259080_BUTTON_devRecord',
          'LIMSSample_5.0.0.0_sample_recordCheckByTestdg1598406003698_BUTTON_refresh',
          'LIMSSample_5.0.0.0_sample_sampleCheckdg1592385064205_BUTTON_viewSample',
          'LIMSSample_5.0.0.0_sample_sampleDealListPart_BUTTON_sampleInfoModify',
          'LIMSSample_5.0.0.0_sample_sampleDealListPart_BUTTON_setTestItem',
          'LIMSSample_5.0.0.0_sample_sampleDealListPart_BUTTON_cancel',
          'LIMSSample_5.0.0.0_sample_sampleDealListPart_BUTTON_recover',
          'LIMSSample_5.0.0.0_sample_sampleDealListPart_BUTTON_collectTestAgain',
          'LIMSSample_5.0.0.0_sample_sampleDealListPart_BUTTON_active',
          'LIMSSample_5.0.0.0_sample_sampleDealListPart_BUTTON_delete',
          'LIMSSample_5.0.0.0_sample_retainListPart_BUTTON_retain',
          'LIMSSample_5.0.0.0_sample_sampleInfoListPart_BUTTON_sampleInfoCheck',
          'LIMSSample_5.0.0.0_sample_sampleInfoListPart_BUTTON_dealInfoCheck',
          'LIMSSample_5.0.0.0_sample_remainSamplePart_BUTTON_back',
          'LIMSSample_5.0.0.0_sample_remainSamplePart_BUTTON_destroy'
      );

    SELECT count(*) INTO restored_product_buttons
    FROM public.ec_button
    WHERE is_published = true
      AND is_permission = true
      AND permission_code IS NOT NULL
      AND code IN (
          SELECT code
          FROM public.runtime_button
          WHERE is_published = true
            AND is_permission = true
            AND permission_code IS NOT NULL
            AND module_code IN ('LIMSSample_5.0.0.0', 'LIMSBasic_1.0.0')
      );

    SELECT count(*) INTO restored_views
    FROM public.runtime_extra_view
    WHERE code IN (
        'LIMSSample_5.0.0.0_sample_sampleRegisterLayout',
        'LIMSBasic_1.0.0_testPlan_planSetSampleList',
        'LIMSSample_5.0.0.0_sample_sampTaskAlcatLayOut',
        'LIMSSample_5.0.0.0_sample_collectListLayout',
        'LIMSSample_5.0.0.0_sample_receiveListLayout',
        'LIMSSample_5.0.0.0_sample_sampleSweepReceive',
        'LIMSSample_5.0.0.0_sample_makeListLayout',
        'LIMSSample_5.0.0.0_sample_handoverListLayout',
        'LIMSSample_5.0.0.0_sample_recordBySingleSample',
        'LIMSSample_5.0.0.0_sample_recordBySample',
        'LIMSSample_5.0.0.0_sample_recordCheckByTest',
        'LIMSSample_5.0.0.0_sample_sampleCheck',
        'LIMSSample_5.0.0.0_sample_sampleDealListLayout',
        'LIMSSample_5.0.0.0_sample_retainListLayout',
        'LIMSSample_5.0.0.0_sample_sampleInfoLayout',
        'LIMSSample_5.0.0.0_sample_remainSampleLayout'
    );

    IF restored_runtime_buttons <> 29 THEN
        RAISE EXCEPTION 'Expected 29 restored runtime buttons, found %', restored_runtime_buttons;
    END IF;
    IF restored_product_buttons < 29 THEN
        RAISE EXCEPTION 'Expected at least 29 restored product buttons, found %', restored_product_buttons;
    END IF;
    IF restored_views <> 16 THEN
        RAISE EXCEPTION 'Expected 16 restored Sample Management views, found %', restored_views;
    END IF;
END $$;
