-- Generated from latest foundation module entities on 2026-06-12.
-- Scope: ChartReportMap_1.0.0, DataSet_1.0.0, DocManage_1.0.0 business tables only.
-- Foreign-key constraints are intentionally omitted for idempotent Linux/PostgreSQL smoke deployment.

create table if not exists crm_chart_customs (
       id int8 not null,
        version int4,
        create_staff_id int8,
        create_time timestamp,
        modify_staff_id int8,
        modify_time timestamp,
        valid boolean,
        cid int8 not null,
        sort int4,
        create_department_id int8,
        create_position_id int8,
        deployment_id int8,
        effect_staff_id int8,
        effect_time timestamp,
        effective_state int4,
        group_id int8,
        owner_department_id int8,
        owner_position_id int8,
        owner_staff_id int8,
        position_lay_rec varchar(255),
        process_key varchar(255),
        process_version int4,
        status int4,
        table_no varchar(255),
        interval_time int4,
        other_attrs varchar(500),
        user_code varchar(100),
        primary key (id)
    );
create table if not exists crm_chart_customs_di (
       id int8 not null,
        version int4 not null,
        activity_name varchar(255),
        assign_staff varchar(4000),
        assign_staff_id varchar(255),
        cid int8,
        comments varchar(4000),
        create_time timestamp,
        dealinfo_type varchar(255),
        entity_code varchar(255),
        instance_id varchar(255),
        outcome varchar(255),
        outcome_des varchar(255),
        outcome_des_zh_cn varchar(255),
        pending_create_time timestamp,
        process_key varchar(255),
        process_version int4,
        proxy_staff varchar(255),
        proxy_staff_ids varchar(255),
        signature varchar(400),
        task_description varchar(255),
        task_description_zh_cn varchar(255),
        user_id int8,
        recalled_flag boolean,
        sort int4,
        table_info_id int8,
        user_agent varchar(255),
        main_obj int8,
        staff int8,
        primary key (id)
    );
create table if not exists crm_chart_reports (
       id int8 not null,
        version int4,
        create_staff_id int8,
        create_time timestamp,
        modify_staff_id int8,
        modify_time timestamp,
        valid boolean,
        cid int8 not null,
        sort int4,
        create_department_id int8,
        create_position_id int8,
        deployment_id int8,
        effect_staff_id int8,
        effect_time timestamp,
        effective_state int4,
        group_id int8,
        owner_department_id int8,
        owner_position_id int8,
        owner_staff_id int8,
        position_lay_rec varchar(255),
        process_key varchar(255),
        process_version int4,
        status int4,
        table_no varchar(255),
        chart_name varchar(200),
        primary key (id)
    );
create table if not exists crm_chart_reports_di (
       id int8 not null,
        version int4 not null,
        activity_name varchar(255),
        assign_staff varchar(4000),
        assign_staff_id varchar(255),
        cid int8,
        comments varchar(4000),
        create_time timestamp,
        dealinfo_type varchar(255),
        entity_code varchar(255),
        instance_id varchar(255),
        outcome varchar(255),
        outcome_des varchar(255),
        outcome_des_zh_cn varchar(255),
        pending_create_time timestamp,
        process_key varchar(255),
        process_version int4,
        proxy_staff varchar(255),
        proxy_staff_ids varchar(255),
        signature varchar(400),
        task_description varchar(255),
        task_description_zh_cn varchar(255),
        user_id int8,
        recalled_flag boolean,
        sort int4,
        table_info_id int8,
        user_agent varchar(255),
        main_obj int8,
        staff int8,
        primary key (id)
    );
create table if not exists crm_my_chart_elements (
       id int8 not null,
        version int4,
        create_staff_id int8,
        create_time timestamp,
        modify_staff_id int8,
        modify_time timestamp,
        valid INTEGER not null,
        cid int8 not null,
        sort int4,
        decimal_digit int4,
        down_alarm varchar(20),
        down_limit varchar(20),
        element_name varchar(200),
        is_show_line boolean,
        is_show_point boolean,
        other_attrs varchar(2000),
        other_attrs_extra varchar(2000),
        up_alarm varchar(20),
        up_limit varchar(20),
        chart_id int8,
        primary key (id)
    );
create table if not exists crm_my_charts (
       id int8 not null,
        version int4,
        create_staff_id int8,
        create_time timestamp,
        modify_staff_id int8,
        modify_time timestamp,
        valid boolean,
        cid int8 not null,
        sort int4,
        create_department_id int8,
        create_position_id int8,
        deployment_id int8,
        effect_staff_id int8,
        effect_time timestamp,
        effective_state int4,
        group_id int8,
        owner_department_id int8,
        owner_position_id int8,
        owner_staff_id int8,
        position_lay_rec varchar(255),
        process_key varchar(255),
        process_version int4,
        status int4,
        table_no varchar(255),
        axisxheight int4,
        axisywidth int4,
        chart_desc varchar(500),
        chart_name varchar(200),
        chart_type varchar(50),
        column_count int4,
        his_offset_time int4,
        other_attrs varchar(2000),
        point_number int4,
        real_offset_time int4,
        real_refresh_interval int4,
        row_count int4,
        table_height int4,
        user_code varchar(100),
        primary key (id)
    );
create table if not exists crm_my_charts_di (
       id int8 not null,
        version int4 not null,
        activity_name varchar(255),
        assign_staff varchar(4000),
        assign_staff_id varchar(255),
        cid int8,
        comments varchar(4000),
        create_time timestamp,
        dealinfo_type varchar(255),
        entity_code varchar(255),
        instance_id varchar(255),
        outcome varchar(255),
        outcome_des varchar(255),
        outcome_des_zh_cn varchar(255),
        pending_create_time timestamp,
        process_key varchar(255),
        process_version int4,
        proxy_staff varchar(255),
        proxy_staff_ids varchar(255),
        signature varchar(400),
        task_description varchar(255),
        task_description_zh_cn varchar(255),
        user_id int8,
        recalled_flag boolean,
        sort int4,
        table_info_id int8,
        user_agent varchar(255),
        main_obj int8,
        staff int8,
        primary key (id)
    );
create table if not exists crm_scatte_chart_sets (
       id int8 not null,
        version int4,
        create_staff_id int8,
        create_time timestamp,
        modify_staff_id int8,
        modify_time timestamp,
        valid boolean,
        cid int8 not null,
        sort int4,
        create_department_id int8,
        create_position_id int8,
        deployment_id int8,
        effect_staff_id int8,
        effect_time timestamp,
        effective_state int4,
        group_id int8,
        owner_department_id int8,
        owner_position_id int8,
        owner_staff_id int8,
        position_lay_rec varchar(255),
        process_key varchar(255),
        process_version int4,
        status int4,
        table_no varchar(255),
        axisxbg_color varchar(255),
        axisxcolor varchar(255),
        axisxfont_color varchar(255),
        axisxfont_size int4,
        axisxheight int4,
        axisyfontsize int4,
        axisywidth int4,
        background_color varchar(255),
        border_color varchar(255),
        chart_height int4,
        chart_status boolean,
        chart_theme varchar(200),
        chart_width int4,
        columns_count int4,
        cp_default_colors varchar(200),
        decimal_digits int4,
        grid_bg_color varchar(255),
        grid_line_color varchar(255),
        max_element_group_num int4,
        max_point_num int4,
        other_attrs varchar(2000),
        rows_count varchar(256),
        system_theme boolean,
        data_interface varchar(255),
        primary key (id)
    );
create table if not exists crm_scatte_chart_sets_di (
       id int8 not null,
        version int4 not null,
        activity_name varchar(255),
        assign_staff varchar(4000),
        assign_staff_id varchar(255),
        cid int8,
        comments varchar(4000),
        create_time timestamp,
        dealinfo_type varchar(255),
        entity_code varchar(255),
        instance_id varchar(255),
        outcome varchar(255),
        outcome_des varchar(255),
        outcome_des_zh_cn varchar(255),
        pending_create_time timestamp,
        process_key varchar(255),
        process_version int4,
        proxy_staff varchar(255),
        proxy_staff_ids varchar(255),
        signature varchar(400),
        task_description varchar(255),
        task_description_zh_cn varchar(255),
        user_id int8,
        recalled_flag boolean,
        sort int4,
        table_info_id int8,
        user_agent varchar(255),
        main_obj int8,
        staff int8,
        primary key (id)
    );
create table if not exists crm_trend_chart_sets (
       id int8 not null,
        version int4,
        create_staff_id int8,
        create_time timestamp,
        modify_staff_id int8,
        modify_time timestamp,
        valid boolean,
        cid int8 not null,
        sort int4,
        create_department_id int8,
        create_position_id int8,
        deployment_id int8,
        effect_staff_id int8,
        effect_time timestamp,
        effective_state int4,
        group_id int8,
        owner_department_id int8,
        owner_position_id int8,
        owner_staff_id int8,
        position_lay_rec varchar(255),
        process_key varchar(255),
        process_version int4,
        status int4,
        table_no varchar(255),
        axisxbg_color varchar(255),
        axisxcolor varchar(255),
        axisxfont_color varchar(255),
        axisxfont_size int4,
        axisxheight int4,
        axisyfontsize int4,
        axisywidth int4,
        background_color varchar(255),
        border_color varchar(255),
        chart_height int4,
        chart_status boolean,
        chart_theme varchar(200),
        chart_width int4,
        columns_count int4,
        cp_default_colors varchar(500),
        decimal_digits int4,
        grid_bg_color varchar(255),
        grid_line_color varchar(255),
        his_interval_time int4,
        is_convert_bool_to_true boolean,
        is_quality_alarm boolean,
        is_show_max_min boolean,
        is_user_defined_bound boolean,
        max_element_num int4,
        max_point_num int4,
        maxi_mum_tip boolean,
        other_attrs varchar(500),
        quality_alarm_range varchar(500),
        real_interval_time int4,
        real_refresh_time int4,
        rows_count int4,
        show_max_min_interval int4,
        system_theme boolean,
        table_columns_width int4,
        table_fontsize int4,
        axis_mode varchar(255),
        data_interface varchar(255),
        primary key (id)
    );
create table if not exists crm_trend_chart_sets_di (
       id int8 not null,
        version int4 not null,
        activity_name varchar(255),
        assign_staff varchar(4000),
        assign_staff_id varchar(255),
        cid int8,
        comments varchar(4000),
        create_time timestamp,
        dealinfo_type varchar(255),
        entity_code varchar(255),
        instance_id varchar(255),
        outcome varchar(255),
        outcome_des varchar(255),
        outcome_des_zh_cn varchar(255),
        pending_create_time timestamp,
        process_key varchar(255),
        process_version int4,
        proxy_staff varchar(255),
        proxy_staff_ids varchar(255),
        signature varchar(400),
        task_description varchar(255),
        task_description_zh_cn varchar(255),
        user_id int8,
        recalled_flag boolean,
        sort int4,
        table_info_id int8,
        user_agent varchar(255),
        main_obj int8,
        staff int8,
        primary key (id)
    );
create table if not exists doc_class_securities (
       id int8 not null,
        version int4,
        create_staff_id int8,
        create_time timestamp,
        modify_staff_id int8,
        modify_time timestamp,
        valid INTEGER not null,
        cid int8 not null,
        sort int4,
        bigintparama int4,
        bigintparamb int4,
        charparama varchar(255),
        charparamb varchar(255),
        code varchar(255),
        contain_sub boolean,
        dateparama timestamp,
        dateparamb timestamp,
        extra_col text,
        numberparama numeric(19, 2),
        numberparamb numeric(19, 2),
        objparama int8,
        objparamb int8,
        other_range int8,
        scparama varchar(255),
        scparamb varchar(255),
        staff_range varchar(256),
        class_id int8,
        doc_class_dept int8,
        doc_class_position int8,
        doc_class_role int8,
        operate_type varchar(255),
        power_rule varchar(255),
        primary key (id)
    );
create table if not exists doc_classifications (
       id int8 not null,
        version int4,
        create_staff_id int8,
        create_time timestamp,
        modify_staff_id int8,
        modify_time timestamp,
        valid INTEGER not null,
        cid int8 not null,
        full_path_name varchar(255),
        lay_no int4,
        lay_rec varchar(255),
        leaf INTEGER,
        parent_id int8,
        sort int8,
        code varchar(200),
        memo_field varchar(256),
        name varchar(256),
        primary key (id)
    );
create table if not exists doc_doc_borrows (
       id int8 not null,
        version int4,
        create_staff_id int8,
        create_time timestamp,
        modify_staff_id int8,
        modify_time timestamp,
        valid INTEGER not null,
        cid int8 not null,
        sort int4,
        create_department_id int8,
        create_position_id int8,
        deployment_id int8,
        effect_staff_id int8,
        effect_time timestamp,
        effective_state int4,
        group_id int8,
        owner_department_id int8,
        owner_position_id int8,
        owner_staff_id int8,
        position_lay_rec varchar(255),
        process_key varchar(255),
        process_version int4,
        status int4,
        table_no varchar(255),
        bigintparama int4,
        bigintparamb int4,
        bigintparamc int4,
        bigintparamd int4,
        bigintparame int4,
        bigintparamf int4,
        bigintparamg int4,
        bigintparamh int4,
        bigintparami int4,
        bigintparamj int4,
        bor_type varchar(255),
        charparama varchar(255),
        charparamb varchar(255),
        charparamc varchar(255),
        charparamd varchar(255),
        charparame varchar(255),
        charparamf varchar(255),
        charparamg varchar(255),
        charparamh varchar(255),
        charparami varchar(255),
        charparamj varchar(255),
        dateparama timestamp,
        dateparamb timestamp,
        dateparamc timestamp,
        dateparamd timestamp,
        dateparame timestamp,
        dateparamf timestamp,
        dateparamg timestamp,
        dateparamh timestamp,
        day_length int8,
        is_forever boolean,
        numberparama numeric(19, 2),
        numberparamb numeric(19, 2),
        numberparamc numeric(19, 2),
        numberparamd numeric(19, 2),
        numberparame numeric(19, 2),
        numberparamf numeric(19, 2),
        objparama int8,
        objparamb int8,
        objparamc int8,
        objparamd int8,
        reason varchar(2000),
        scparama varchar(255),
        scparamb varchar(255),
        scparamc varchar(255),
        scparamd varchar(255),
        start_date date,
        table_info_id int8,
        document_id int8,
        staff_id int8,
        primary key (id)
    );
create table if not exists doc_doc_borrows_di (
       id int8 not null,
        version int4 not null,
        activity_name varchar(255),
        assign_staff varchar(4000),
        assign_staff_id varchar(255),
        cid int8,
        comments varchar(4000),
        create_time timestamp,
        dealinfo_type varchar(255),
        entity_code varchar(255),
        instance_id varchar(255),
        outcome varchar(255),
        outcome_des varchar(255),
        outcome_des_zh_cn varchar(255),
        pending_create_time timestamp,
        process_key varchar(255),
        process_version int4,
        proxy_staff varchar(255),
        proxy_staff_ids varchar(255),
        signature varchar(400),
        task_description varchar(255),
        task_description_zh_cn varchar(255),
        user_id int8,
        recalled_flag boolean,
        sort int4,
        table_info_id int8,
        user_agent varchar(255),
        main_obj int8,
        staff int8,
        primary key (id)
    );
create table if not exists doc_doc_borrows_pa (
       id int8 not null,
        version int4 not null,
        create_staff_id int8,
        create_time timestamp,
        delete_staff_id int8,
        delete_time timestamp,
        modify_staff_id int8,
        modify_time timestamp,
        table_info_id int8,
        valid INTEGER not null,
        main_obj int8,
        staff int8,
        primary key (id)
    );
create table if not exists doc_doc_borrows_sv (
       id int8 not null,
        version int4 not null,
        create_staff_id int8,
        create_time timestamp,
        delete_staff_id int8,
        delete_time timestamp,
        modify_staff_id int8,
        modify_time timestamp,
        table_info_id int8,
        valid INTEGER not null,
        main_obj int8,
        staff int8 not null,
        primary key (id)
    );
create table if not exists doc_doc_classes (
       id int8 not null,
        version int4,
        create_staff_id int8,
        create_time timestamp,
        modify_staff_id int8,
        modify_time timestamp,
        valid boolean,
        cid int8 not null,
        full_path_name varchar(255),
        lay_no int4,
        lay_rec varchar(255),
        leaf INTEGER,
        parent_id int8,
        sort int8,
        create_department_id int8,
        create_position_id int8,
        deployment_id int8,
        effect_staff_id int8,
        effect_time timestamp,
        effective_state int4,
        oa text,
        owner_department_id int8,
        owner_position_id int8,
        owner_staff_id int8,
        position_lay_rec varchar(255),
        process_key varchar(255),
        process_version int4,
        status int4,
        table_no varchar(255),
        bigintparama int4,
        bigintparamb int4,
        charparama varchar(255),
        charparamb varchar(255),
        code varchar(255),
        dateparama timestamp,
        dateparamb timestamp,
        extra_col text,
        memo_field text,
        name varchar(256),
        numberparama numeric(19, 2),
        numberparamb numeric(19, 2),
        objparama int8,
        objparamb int8,
        scparama varchar(255),
        scparamb varchar(255),
        primary key (id)
    );
create table if not exists doc_doc_classes_di (
       id int8 not null,
        version int4 not null,
        activity_name varchar(255),
        assign_staff varchar(4000),
        assign_staff_id varchar(255),
        cid int8,
        comments varchar(4000),
        create_time timestamp,
        dealinfo_type varchar(255),
        entity_code varchar(255),
        instance_id varchar(255),
        outcome varchar(255),
        outcome_des varchar(255),
        outcome_des_zh_cn varchar(255),
        pending_create_time timestamp,
        process_key varchar(255),
        process_version int4,
        proxy_staff varchar(255),
        proxy_staff_ids varchar(255),
        signature varchar(400),
        task_description varchar(255),
        task_description_zh_cn varchar(255),
        user_id int8,
        recalled_flag boolean,
        sort int4,
        table_info_id int8,
        user_agent varchar(255),
        main_obj int8,
        staff int8,
        primary key (id)
    );
create table if not exists doc_doc_documents (
       id int8 not null,
        version int4,
        create_staff_id int8,
        create_time timestamp,
        modify_staff_id int8,
        modify_time timestamp,
        valid INTEGER not null,
        cid int8 not null,
        sort int4,
        create_department_id int8,
        create_position_id int8,
        deployment_id int8,
        effect_staff_id int8,
        effect_time timestamp,
        effective_state int4,
        group_id int8,
        owner_department_id int8,
        owner_position_id int8,
        owner_staff_id int8,
        position_lay_rec varchar(255),
        process_key varchar(255),
        process_version int4,
        status int4,
        table_no varchar(255),
        bigintparama int4,
        bigintparamb int4,
        bigintparamc int4,
        bigintparamd int4,
        bigintparame int4,
        bigintparamf int4,
        bigintparamg int4,
        bigintparamh int4,
        bigintparami int4,
        bigintparamj int4,
        charparama varchar(255),
        charparamb varchar(255),
        charparamc varchar(255),
        charparamd varchar(255),
        charparame varchar(255),
        charparamf varchar(255),
        charparamg varchar(255),
        charparamh varchar(255),
        charparami varchar(255),
        charparamj varchar(255),
        code varchar(256),
        dateparama timestamp,
        dateparamb timestamp,
        dateparamc timestamp,
        dateparamd timestamp,
        dateparame timestamp,
        dateparamf timestamp,
        dateparamg timestamp,
        dateparamh timestamp,
        doc_summary varchar(256),
        doc_version varchar(256),
        expire_date date,
        extra_col text,
        is_current boolean,
        is_effective boolean,
        keyword varchar(256),
        main_author varchar(256),
        memo_field text,
        numberparama numeric(19, 2),
        numberparamb numeric(19, 2),
        numberparamc numeric(19, 2),
        numberparamd numeric(19, 2),
        numberparame numeric(19, 2),
        numberparamf numeric(19, 2),
        objparama int8,
        objparamb int8,
        objparamc int8,
        objparamd int8,
        remind_date date,
        scparama varchar(255),
        scparamb varchar(255),
        scparamc varchar(255),
        scparamd varchar(255),
        sec_author varchar(256),
        table_info_id int8,
        take_effect boolean,
        title varchar(256),
        doc_class_id int8,
        doc_type varchar(255),
        document_state varchar(255),
        language_type varchar(255),
        secret_class varchar(255),
        primary key (id)
    );
create table if not exists doc_doc_documents_di (
       id int8 not null,
        version int4 not null,
        activity_name varchar(255),
        assign_staff varchar(4000),
        assign_staff_id varchar(255),
        cid int8,
        comments varchar(4000),
        create_time timestamp,
        dealinfo_type varchar(255),
        entity_code varchar(255),
        instance_id varchar(255),
        outcome varchar(255),
        outcome_des varchar(255),
        outcome_des_zh_cn varchar(255),
        pending_create_time timestamp,
        process_key varchar(255),
        process_version int4,
        proxy_staff varchar(255),
        proxy_staff_ids varchar(255),
        signature varchar(400),
        task_description varchar(255),
        task_description_zh_cn varchar(255),
        user_id int8,
        recalled_flag boolean,
        sort int4,
        table_info_id int8,
        user_agent varchar(255),
        main_obj int8,
        staff int8,
        primary key (id)
    );
create table if not exists doc_doc_documents_pa (
       id int8 not null,
        version int4 not null,
        create_staff_id int8,
        create_time timestamp,
        delete_staff_id int8,
        delete_time timestamp,
        modify_staff_id int8,
        modify_time timestamp,
        table_info_id int8,
        valid INTEGER not null,
        main_obj int8,
        staff int8,
        primary key (id)
    );
create table if not exists doc_doc_documents_sv (
       id int8 not null,
        version int4 not null,
        create_staff_id int8,
        create_time timestamp,
        delete_staff_id int8,
        delete_time timestamp,
        modify_staff_id int8,
        modify_time timestamp,
        table_info_id int8,
        valid INTEGER not null,
        main_obj int8,
        staff int8 not null,
        primary key (id)
    );
create table if not exists doc_doc_histories (
       id int8 not null,
        version int4,
        create_staff_id int8,
        create_time timestamp,
        modify_staff_id int8,
        modify_time timestamp,
        valid INTEGER not null,
        cid int8 not null,
        sort int4,
        modify_content varchar(256),
        modify_reason varchar(256),
        record_time timestamp,
        table_info_id int8,
        upper_doc_id varchar(256),
        document_id int8,
        modify_type varchar(255),
        staff_id int8,
        primary key (id)
    );
create table if not exists doc_doc_objects (
       id int8 not null,
        version int4,
        create_staff_id int8,
        create_time timestamp,
        modify_staff_id int8,
        modify_time timestamp,
        valid INTEGER not null,
        cid int8 not null,
        sort int4,
        model_code varchar(256),
        object_id int8,
        table_info_id int8,
        document_id int8,
        primary key (id)
    );
create table if not exists doc_doc_reminders (
       id int8 not null,
        version int4,
        create_staff_id int8,
        create_time timestamp,
        modify_staff_id int8,
        modify_time timestamp,
        valid INTEGER not null,
        cid int8 not null,
        sort int4,
        table_info_id int8,
        document_id int8,
        reminder int8,
        primary key (id)
    );
create table if not exists doc_docu_depts (
       id int8 not null,
        version int4,
        create_staff_id int8,
        create_time timestamp,
        modify_staff_id int8,
        modify_time timestamp,
        valid INTEGER not null,
        cid int8 not null,
        sort int4,
        table_info_id int8,
        doc_dept_id int8,
        document_id int8,
        primary key (id)
    );
create table if not exists doc_enclosure_heads (
       id int8 not null,
        version int4,
        create_staff_id int8,
        create_time timestamp,
        modify_staff_id int8,
        modify_time timestamp,
        valid boolean,
        cid int8 not null,
        sort int4,
        create_department_id int8,
        create_position_id int8,
        deployment_id int8,
        effect_staff_id int8,
        effect_time timestamp,
        effective_state int4,
        group_id int8,
        owner_department_id int8,
        owner_position_id int8,
        owner_staff_id int8,
        position_lay_rec varchar(255),
        process_key varchar(255),
        process_version int4,
        status int4,
        table_no varchar(255),
        change int4,
        code varchar(256),
        doc_table_info_id int8,
        doc_table_no varchar(256),
        from_id int8,
        model_code varchar(256),
        property_code varchar(256),
        doc_id int8,
        primary key (id)
    );
create table if not exists doc_enclosure_heads_di (
       id int8 not null,
        version int4 not null,
        activity_name varchar(255),
        assign_staff varchar(4000),
        assign_staff_id varchar(255),
        cid int8,
        comments varchar(4000),
        create_time timestamp,
        dealinfo_type varchar(255),
        entity_code varchar(255),
        instance_id varchar(255),
        outcome varchar(255),
        outcome_des varchar(255),
        outcome_des_zh_cn varchar(255),
        pending_create_time timestamp,
        process_key varchar(255),
        process_version int4,
        proxy_staff varchar(255),
        proxy_staff_ids varchar(255),
        signature varchar(400),
        task_description varchar(255),
        task_description_zh_cn varchar(255),
        user_id int8,
        recalled_flag boolean,
        sort int4,
        table_info_id int8,
        user_agent varchar(255),
        main_obj int8,
        staff int8,
        primary key (id)
    );
create table if not exists doc_enclosure_mngs (
       id int8 not null,
        version int4,
        create_staff_id int8,
        create_time timestamp,
        modify_staff_id int8,
        modify_time timestamp,
        valid INTEGER not null,
        cid int8 not null,
        sort int4,
        change int4,
        code varchar(256),
        data_id int8,
        doc_table_info_id int8,
        doc_table_no varchar(256),
        model_code varchar(256),
        property_code varchar(256),
        doc_id int8,
        head_id int8,
        primary key (id)
    );
create table if not exists doc_laws (
       id int8 not null,
        version int4,
        create_staff_id int8,
        create_time timestamp,
        modify_staff_id int8,
        modify_time timestamp,
        valid boolean,
        cid int8 not null,
        sort int4,
        create_department_id int8,
        create_position_id int8,
        deployment_id int8,
        effect_staff_id int8,
        effect_time timestamp,
        effective_state int4,
        group_id int8,
        owner_department_id int8,
        owner_position_id int8,
        owner_staff_id int8,
        position_lay_rec varchar(255),
        process_key varchar(255),
        process_version int4,
        status int4,
        table_no varchar(255),
        apply_dept varchar(256),
        bigintparama int4,
        bigintparamb int4,
        bigintparamc int4,
        bigintparamd int4,
        bigintparame int4,
        bigintparamf int4,
        bigintparamg int4,
        bigintparamh int4,
        bigintparami int4,
        bigintparamj int4,
        charparama varchar(255),
        charparamb varchar(255),
        charparamc varchar(255),
        charparamd varchar(255),
        charparame varchar(255),
        charparamf varchar(255),
        charparamg varchar(255),
        charparamh varchar(255),
        charparami varchar(255),
        charparamj varchar(255),
        code varchar(255),
        dateparama timestamp,
        dateparamb timestamp,
        dateparamc timestamp,
        dateparamd timestamp,
        dateparame timestamp,
        dateparamf timestamp,
        dateparamg timestamp,
        dateparamh timestamp,
        effective_date date,
        memo_field text,
        name varchar(256),
        numberparama numeric(19, 2),
        numberparamb numeric(19, 2),
        numberparamc numeric(19, 2),
        numberparamd numeric(19, 2),
        numberparame numeric(19, 2),
        numberparamf numeric(19, 2),
        objparama int8,
        objparamb int8,
        objparamc int8,
        objparamd int8,
        release_date date,
        release_dept varchar(256),
        release_num varchar(256),
        scparama varchar(255),
        scparamb varchar(255),
        scparamc varchar(255),
        scparamd varchar(255),
        classification int8,
        primary key (id)
    );
create table if not exists doc_laws_di (
       id int8 not null,
        version int4 not null,
        activity_name varchar(255),
        assign_staff varchar(4000),
        assign_staff_id varchar(255),
        cid int8,
        comments varchar(4000),
        create_time timestamp,
        dealinfo_type varchar(255),
        entity_code varchar(255),
        instance_id varchar(255),
        outcome varchar(255),
        outcome_des varchar(255),
        outcome_des_zh_cn varchar(255),
        pending_create_time timestamp,
        process_key varchar(255),
        process_version int4,
        proxy_staff varchar(255),
        proxy_staff_ids varchar(255),
        signature varchar(400),
        task_description varchar(255),
        task_description_zh_cn varchar(255),
        user_id int8,
        recalled_flag boolean,
        sort int4,
        table_info_id int8,
        user_agent varchar(255),
        main_obj int8,
        staff int8,
        primary key (id)
    );
create table if not exists doc_legal_provisions (
       id int8 not null,
        version int4,
        create_staff_id int8,
        create_time timestamp,
        modify_staff_id int8,
        modify_time timestamp,
        valid INTEGER not null,
        cid int8 not null,
        sort int4,
        bigintparama int4,
        bigintparamb int4,
        bigintparamc int4,
        bigintparamd int4,
        bigintparame int4,
        bigintparamf int4,
        bigintparamg int4,
        bigintparamh int4,
        bigintparami int4,
        bigintparamj int4,
        charparama varchar(255),
        charparamb varchar(255),
        charparamc varchar(255),
        charparamd varchar(255),
        charparame varchar(255),
        charparamf varchar(255),
        charparamg varchar(255),
        charparamh varchar(255),
        charparami varchar(255),
        charparamj varchar(255),
        clause varchar(256),
        code varchar(256),
        content varchar(2000),
        dateparama timestamp,
        dateparamb timestamp,
        dateparamc timestamp,
        dateparamd timestamp,
        dateparame timestamp,
        dateparamf timestamp,
        dateparamg timestamp,
        dateparamh timestamp,
        memo_field varchar(256),
        numberparama numeric(19, 2),
        numberparamb numeric(19, 2),
        numberparamc numeric(19, 2),
        numberparamd numeric(19, 2),
        numberparame numeric(19, 2),
        numberparamf numeric(19, 2),
        objparama int8,
        objparamb int8,
        objparamc int8,
        objparamd int8,
        scparama varchar(255),
        scparamb varchar(255),
        scparamc varchar(255),
        scparamd varchar(255),
        the_content text,
        law int8,
        primary key (id)
    );
create table if not exists doc_provision_detais (
       id int8 not null,
        version int4,
        create_staff_id int8,
        create_time timestamp,
        modify_staff_id int8,
        modify_time timestamp,
        valid INTEGER not null,
        cid int8 not null,
        sort int4,
        bigintparama int4,
        bigintparamb int4,
        bigintparamc int4,
        bigintparamd int4,
        bigintparame int4,
        bigintparamf int4,
        bigintparamg int4,
        bigintparamh int4,
        bigintparami int4,
        bigintparamj int4,
        charparama varchar(255),
        charparamb varchar(255),
        charparamc varchar(255),
        charparamd varchar(255),
        charparame varchar(255),
        charparamf varchar(255),
        charparamg varchar(255),
        charparamh varchar(255),
        charparami varchar(255),
        charparamj varchar(255),
        code varchar(255),
        conclusion varchar(256),
        dateparama timestamp,
        dateparamb timestamp,
        dateparamc timestamp,
        dateparamd timestamp,
        dateparame timestamp,
        dateparamf timestamp,
        dateparamg timestamp,
        dateparamh timestamp,
        description_info varchar(256),
        measure varchar(256),
        memo_field text,
        numberparama numeric(19, 2),
        numberparamb numeric(19, 2),
        numberparamc numeric(19, 2),
        numberparamd numeric(19, 2),
        numberparame numeric(19, 2),
        numberparamf numeric(19, 2),
        objparama int8,
        objparamb int8,
        objparamc int8,
        objparamd int8,
        scparama varchar(255),
        scparamb varchar(255),
        scparamc varchar(255),
        scparamd varchar(255),
        table_info_id int8,
        the_content text,
        legal_provision int8,
        provision int8,
        primary key (id)
    );
create table if not exists doc_provisions (
       id int8 not null,
        version int4,
        create_staff_id int8,
        create_time timestamp,
        modify_staff_id int8,
        modify_time timestamp,
        valid INTEGER not null,
        cid int8 not null,
        sort int4,
        create_department_id int8,
        create_position_id int8,
        deployment_id int8,
        effect_staff_id int8,
        effect_time timestamp,
        effective_state int4,
        group_id int8,
        owner_department_id int8,
        owner_position_id int8,
        owner_staff_id int8,
        position_lay_rec varchar(255),
        process_key varchar(255),
        process_version int4,
        status int4,
        table_no varchar(255),
        bigintparama int4,
        bigintparamb int4,
        bigintparamc int4,
        bigintparamd int4,
        bigintparame int4,
        bigintparamf int4,
        bigintparamg int4,
        bigintparamh int4,
        bigintparami int4,
        bigintparamj int4,
        charparama varchar(255),
        charparamb varchar(255),
        charparamc varchar(255),
        charparamd varchar(255),
        charparame varchar(255),
        charparamf varchar(255),
        charparamg varchar(255),
        charparamh varchar(255),
        charparami varchar(255),
        charparamj varchar(255),
        code varchar(255),
        dateparama timestamp,
        dateparamb timestamp,
        dateparamc timestamp,
        dateparamd timestamp,
        dateparame timestamp,
        dateparamf timestamp,
        dateparamg timestamp,
        dateparamh timestamp,
        memo_field varchar(256),
        numberparama numeric(19, 2),
        numberparamb numeric(19, 2),
        numberparamc numeric(19, 2),
        numberparamd numeric(19, 2),
        numberparame numeric(19, 2),
        numberparamf numeric(19, 2),
        objparama int8,
        objparamb int8,
        objparamc int8,
        objparamd int8,
        scparama varchar(255),
        scparamb varchar(255),
        scparamc varchar(255),
        scparamd varchar(255),
        table_info_id int8,
        law int8,
        primary key (id)
    );
create table if not exists doc_provisions_di (
       id int8 not null,
        version int4 not null,
        activity_name varchar(255),
        assign_staff varchar(4000),
        assign_staff_id varchar(255),
        cid int8,
        comments varchar(4000),
        create_time timestamp,
        dealinfo_type varchar(255),
        entity_code varchar(255),
        instance_id varchar(255),
        outcome varchar(255),
        outcome_des varchar(255),
        outcome_des_zh_cn varchar(255),
        pending_create_time timestamp,
        process_key varchar(255),
        process_version int4,
        proxy_staff varchar(255),
        proxy_staff_ids varchar(255),
        signature varchar(400),
        task_description varchar(255),
        task_description_zh_cn varchar(255),
        user_id int8,
        recalled_flag boolean,
        sort int4,
        table_info_id int8,
        user_agent varchar(255),
        main_obj int8,
        staff int8,
        primary key (id)
    );
create table if not exists doc_provisions_sv (
       id int8 not null,
        version int4 not null,
        create_staff_id int8,
        create_time timestamp,
        delete_staff_id int8,
        delete_time timestamp,
        modify_staff_id int8,
        modify_time timestamp,
        table_info_id int8,
        valid INTEGER not null,
        main_obj int8,
        staff int8 not null,
        primary key (id)
    );
create table if not exists doc_staff_ranges (
       id int8 not null,
        version int4,
        create_staff_id int8,
        create_time timestamp,
        modify_staff_id int8,
        modify_time timestamp,
        valid INTEGER not null,
        cid int8 not null,
        sort int4,
        bigintparama int4,
        bigintparamb int4,
        charparama varchar(255),
        charparamb varchar(255),
        dateparama timestamp,
        dateparamb timestamp,
        numberparama numeric(19, 2),
        numberparamb numeric(19, 2),
        objparama int8,
        objparamb int8,
        scparama varchar(255),
        scparamb varchar(255),
        class_security_id int8,
        staff_id int8,
        primary key (id)
    );
create table if not exists doc_user_classes (
       id int8 not null,
        version int4,
        create_staff_id int8,
        create_time timestamp,
        modify_staff_id int8,
        modify_time timestamp,
        valid INTEGER not null,
        cid int8 not null,
        sort int4,
        bigintparama int4,
        bigintparamb int4,
        charparama varchar(255),
        charparamb varchar(255),
        code varchar(255),
        contain_sub boolean,
        dateparama timestamp,
        dateparamb timestamp,
        memo_field text,
        numberparama numeric(19, 2),
        numberparamb numeric(19, 2),
        objparama int8,
        objparamb int8,
        scparama varchar(255),
        scparamb varchar(255),
        doc_class int8,
        user_power_id int8,
        primary key (id)
    );
create table if not exists doc_user_powers (
       id int8 not null,
        version int4,
        create_staff_id int8,
        create_time timestamp,
        modify_staff_id int8,
        modify_time timestamp,
        valid boolean,
        cid int8 not null,
        sort int4,
        create_department_id int8,
        create_position_id int8,
        deployment_id int8,
        effect_staff_id int8,
        effect_time timestamp,
        effective_state int4,
        group_id int8,
        owner_department_id int8,
        owner_position_id int8,
        owner_staff_id int8,
        position_lay_rec varchar(255),
        process_key varchar(255),
        process_version int4,
        status int4,
        table_no varchar(255),
        bigintparama int4,
        bigintparamb int4,
        charparama varchar(255),
        charparamb varchar(255),
        code varchar(255),
        dateparama timestamp,
        dateparamb timestamp,
        is_all_docs boolean,
        memo_field text,
        numberparama numeric(19, 2),
        numberparamb numeric(19, 2),
        objparama int8,
        objparamb int8,
        scparama varchar(255),
        scparamb varchar(255),
        staff_id int8,
        primary key (id)
    );
create table if not exists doc_user_powers_di (
       id int8 not null,
        version int4 not null,
        activity_name varchar(255),
        assign_staff varchar(4000),
        assign_staff_id varchar(255),
        cid int8,
        comments varchar(4000),
        create_time timestamp,
        dealinfo_type varchar(255),
        entity_code varchar(255),
        instance_id varchar(255),
        outcome varchar(255),
        outcome_des varchar(255),
        outcome_des_zh_cn varchar(255),
        pending_create_time timestamp,
        process_key varchar(255),
        process_version int4,
        proxy_staff varchar(255),
        proxy_staff_ids varchar(255),
        signature varchar(400),
        task_description varchar(255),
        task_description_zh_cn varchar(255),
        user_id int8,
        recalled_flag boolean,
        sort int4,
        table_info_id int8,
        user_agent varchar(255),
        main_obj int8,
        staff int8,
        primary key (id)
    );
create table if not exists ds_category_mgts (
       id int8 not null,
        version int4,
        create_staff_id int8,
        create_time timestamp,
        modify_staff_id int8,
        modify_time timestamp,
        valid boolean,
        cid int8 not null,
        full_path_name varchar(255),
        lay_no int4,
        lay_rec varchar(255),
        leaf INTEGER,
        parent_id int8,
        sort int8,
        create_department_id int8,
        create_position_id int8,
        deployment_id int8,
        effect_staff_id int8,
        effect_time timestamp,
        effective_state int4,
        oa text,
        owner_department_id int8,
        owner_position_id int8,
        owner_staff_id int8,
        position_lay_rec varchar(255),
        process_key varchar(255),
        process_version int4,
        status int4,
        table_no varchar(255),
        category_code varchar(30),
        category_name varchar(30),
        category_remark varchar(2000),
        primary key (id)
    );
create table if not exists ds_category_mgts_di (
       id int8 not null,
        version int4 not null,
        activity_name varchar(255),
        assign_staff varchar(4000),
        assign_staff_id varchar(255),
        cid int8,
        comments varchar(4000),
        create_time timestamp,
        dealinfo_type varchar(255),
        entity_code varchar(255),
        instance_id varchar(255),
        outcome varchar(255),
        outcome_des varchar(255),
        outcome_des_zh_cn varchar(255),
        pending_create_time timestamp,
        process_key varchar(255),
        process_version int4,
        proxy_staff varchar(255),
        proxy_staff_ids varchar(255),
        signature varchar(400),
        task_description varchar(255),
        task_description_zh_cn varchar(255),
        user_id int8,
        recalled_flag boolean,
        sort int4,
        table_info_id int8,
        user_agent varchar(255),
        main_obj int8,
        staff int8,
        primary key (id)
    );
create table if not exists ds_connection_mgts (
       id int8 not null,
        version int4,
        create_staff_id int8,
        create_time timestamp,
        modify_staff_id int8,
        modify_time timestamp,
        valid boolean,
        cid int8 not null,
        sort int4,
        create_department_id int8,
        create_position_id int8,
        deployment_id int8,
        effect_staff_id int8,
        effect_time timestamp,
        effective_state int4,
        group_id int8,
        owner_department_id int8,
        owner_position_id int8,
        owner_staff_id int8,
        position_lay_rec varchar(255),
        process_key varchar(255),
        process_version int4,
        status int4,
        table_no varchar(255),
        conn_name varchar(30),
        conn_remark varchar(2000),
        connection_str text,
        database_account varchar(20),
        database_name varchar(30),
        database_password varchar(20),
        server_addr varchar(30),
        server_port int4,
        data_source_type varchar(255),
        database_type varchar(255),
        primary key (id)
    );
create table if not exists ds_connection_mgts_di (
       id int8 not null,
        version int4 not null,
        activity_name varchar(255),
        assign_staff varchar(4000),
        assign_staff_id varchar(255),
        cid int8,
        comments varchar(4000),
        create_time timestamp,
        dealinfo_type varchar(255),
        entity_code varchar(255),
        instance_id varchar(255),
        outcome varchar(255),
        outcome_des varchar(255),
        outcome_des_zh_cn varchar(255),
        pending_create_time timestamp,
        process_key varchar(255),
        process_version int4,
        proxy_staff varchar(255),
        proxy_staff_ids varchar(255),
        signature varchar(400),
        task_description varchar(255),
        task_description_zh_cn varchar(255),
        user_id int8,
        recalled_flag boolean,
        sort int4,
        table_info_id int8,
        user_agent varchar(255),
        main_obj int8,
        staff int8,
        primary key (id)
    );
create table if not exists ds_definition_mgts (
       id int8 not null,
        version int4,
        create_staff_id int8,
        create_time timestamp,
        modify_staff_id int8,
        modify_time timestamp,
        valid boolean,
        cid int8 not null,
        sort int4,
        create_department_id int8,
        create_position_id int8,
        deployment_id int8,
        effect_staff_id int8,
        effect_time timestamp,
        effective_state int4,
        group_id int8,
        owner_department_id int8,
        owner_position_id int8,
        owner_staff_id int8,
        position_lay_rec varchar(255),
        process_key varchar(255),
        process_version int4,
        status int4,
        table_no varchar(255),
        data_set_alias varchar(30),
        data_set_name varchar(30),
        data_set_remark varchar(2000),
        sql_statement text,
        category_id int8,
        conn_id int8,
        sql_type varchar(255),
        primary key (id)
    );
create table if not exists ds_definition_mgts_di (
       id int8 not null,
        version int4 not null,
        activity_name varchar(255),
        assign_staff varchar(4000),
        assign_staff_id varchar(255),
        cid int8,
        comments varchar(4000),
        create_time timestamp,
        dealinfo_type varchar(255),
        entity_code varchar(255),
        instance_id varchar(255),
        outcome varchar(255),
        outcome_des varchar(255),
        outcome_des_zh_cn varchar(255),
        pending_create_time timestamp,
        process_key varchar(255),
        process_version int4,
        proxy_staff varchar(255),
        proxy_staff_ids varchar(255),
        signature varchar(400),
        task_description varchar(255),
        task_description_zh_cn varchar(255),
        user_id int8,
        recalled_flag boolean,
        sort int4,
        table_info_id int8,
        user_agent varchar(255),
        main_obj int8,
        staff int8,
        primary key (id)
    );
create table if not exists ds_field_mgts (
       id int8 not null,
        version int4,
        create_staff_id int8,
        create_time timestamp,
        modify_staff_id int8,
        modify_time timestamp,
        valid INTEGER not null,
        cid int8 not null,
        sort int4,
        analysis boolean,
        field_alias varchar(100),
        field_name varchar(30),
        field_type varchar(30),
        data_set_alias int8,
        primary key (id)
    );
create table if not exists ds_param_mgts (
       id int8 not null,
        version int4,
        create_staff_id int8,
        create_time timestamp,
        modify_staff_id int8,
        modify_time timestamp,
        valid INTEGER not null,
        cid int8 not null,
        sort int4,
        param_name varchar(30),
        param_remark varchar(30),
        param_value varchar(500),
        data_set_alias int8,
        param_type varchar(255),
        primary key (id)
    );
create table if not exists ds_preview_mgts (
       id int8 not null,
        version int4,
        create_staff_id int8,
        create_time timestamp,
        modify_staff_id int8,
        modify_time timestamp,
        valid INTEGER not null,
        cid int8 not null,
        sort int4,
        data_set_alias int8,
        primary key (id)
    );

alter table crm_chart_customs add column if not exists id int8;
alter table crm_chart_customs add column if not exists version int4;
alter table crm_chart_customs add column if not exists create_staff_id int8;
alter table crm_chart_customs add column if not exists create_time timestamp;
alter table crm_chart_customs add column if not exists modify_staff_id int8;
alter table crm_chart_customs add column if not exists modify_time timestamp;
alter table crm_chart_customs add column if not exists valid boolean;
alter table crm_chart_customs add column if not exists cid int8;
alter table crm_chart_customs add column if not exists sort int4;
alter table crm_chart_customs add column if not exists create_department_id int8;
alter table crm_chart_customs add column if not exists create_position_id int8;
alter table crm_chart_customs add column if not exists deployment_id int8;
alter table crm_chart_customs add column if not exists effect_staff_id int8;
alter table crm_chart_customs add column if not exists effect_time timestamp;
alter table crm_chart_customs add column if not exists effective_state int4;
alter table crm_chart_customs add column if not exists group_id int8;
alter table crm_chart_customs add column if not exists owner_department_id int8;
alter table crm_chart_customs add column if not exists owner_position_id int8;
alter table crm_chart_customs add column if not exists owner_staff_id int8;
alter table crm_chart_customs add column if not exists position_lay_rec varchar(255);
alter table crm_chart_customs add column if not exists process_key varchar(255);
alter table crm_chart_customs add column if not exists process_version int4;
alter table crm_chart_customs add column if not exists status int4;
alter table crm_chart_customs add column if not exists table_no varchar(255);
alter table crm_chart_customs add column if not exists interval_time int4;
alter table crm_chart_customs add column if not exists other_attrs varchar(500);
alter table crm_chart_customs add column if not exists user_code varchar(100);
alter table crm_chart_customs_di add column if not exists id int8;
alter table crm_chart_customs_di add column if not exists version int4;
alter table crm_chart_customs_di add column if not exists activity_name varchar(255);
alter table crm_chart_customs_di add column if not exists assign_staff varchar(4000);
alter table crm_chart_customs_di add column if not exists assign_staff_id varchar(255);
alter table crm_chart_customs_di add column if not exists cid int8;
alter table crm_chart_customs_di add column if not exists comments varchar(4000);
alter table crm_chart_customs_di add column if not exists create_time timestamp;
alter table crm_chart_customs_di add column if not exists dealinfo_type varchar(255);
alter table crm_chart_customs_di add column if not exists entity_code varchar(255);
alter table crm_chart_customs_di add column if not exists instance_id varchar(255);
alter table crm_chart_customs_di add column if not exists outcome varchar(255);
alter table crm_chart_customs_di add column if not exists outcome_des varchar(255);
alter table crm_chart_customs_di add column if not exists outcome_des_zh_cn varchar(255);
alter table crm_chart_customs_di add column if not exists pending_create_time timestamp;
alter table crm_chart_customs_di add column if not exists process_key varchar(255);
alter table crm_chart_customs_di add column if not exists process_version int4;
alter table crm_chart_customs_di add column if not exists proxy_staff varchar(255);
alter table crm_chart_customs_di add column if not exists proxy_staff_ids varchar(255);
alter table crm_chart_customs_di add column if not exists signature varchar(400);
alter table crm_chart_customs_di add column if not exists task_description varchar(255);
alter table crm_chart_customs_di add column if not exists task_description_zh_cn varchar(255);
alter table crm_chart_customs_di add column if not exists user_id int8;
alter table crm_chart_customs_di add column if not exists recalled_flag boolean;
alter table crm_chart_customs_di add column if not exists sort int4;
alter table crm_chart_customs_di add column if not exists table_info_id int8;
alter table crm_chart_customs_di add column if not exists user_agent varchar(255);
alter table crm_chart_customs_di add column if not exists main_obj int8;
alter table crm_chart_customs_di add column if not exists staff int8;
alter table crm_chart_reports add column if not exists id int8;
alter table crm_chart_reports add column if not exists version int4;
alter table crm_chart_reports add column if not exists create_staff_id int8;
alter table crm_chart_reports add column if not exists create_time timestamp;
alter table crm_chart_reports add column if not exists modify_staff_id int8;
alter table crm_chart_reports add column if not exists modify_time timestamp;
alter table crm_chart_reports add column if not exists valid boolean;
alter table crm_chart_reports add column if not exists cid int8;
alter table crm_chart_reports add column if not exists sort int4;
alter table crm_chart_reports add column if not exists create_department_id int8;
alter table crm_chart_reports add column if not exists create_position_id int8;
alter table crm_chart_reports add column if not exists deployment_id int8;
alter table crm_chart_reports add column if not exists effect_staff_id int8;
alter table crm_chart_reports add column if not exists effect_time timestamp;
alter table crm_chart_reports add column if not exists effective_state int4;
alter table crm_chart_reports add column if not exists group_id int8;
alter table crm_chart_reports add column if not exists owner_department_id int8;
alter table crm_chart_reports add column if not exists owner_position_id int8;
alter table crm_chart_reports add column if not exists owner_staff_id int8;
alter table crm_chart_reports add column if not exists position_lay_rec varchar(255);
alter table crm_chart_reports add column if not exists process_key varchar(255);
alter table crm_chart_reports add column if not exists process_version int4;
alter table crm_chart_reports add column if not exists status int4;
alter table crm_chart_reports add column if not exists table_no varchar(255);
alter table crm_chart_reports add column if not exists chart_name varchar(200);
alter table crm_chart_reports_di add column if not exists id int8;
alter table crm_chart_reports_di add column if not exists version int4;
alter table crm_chart_reports_di add column if not exists activity_name varchar(255);
alter table crm_chart_reports_di add column if not exists assign_staff varchar(4000);
alter table crm_chart_reports_di add column if not exists assign_staff_id varchar(255);
alter table crm_chart_reports_di add column if not exists cid int8;
alter table crm_chart_reports_di add column if not exists comments varchar(4000);
alter table crm_chart_reports_di add column if not exists create_time timestamp;
alter table crm_chart_reports_di add column if not exists dealinfo_type varchar(255);
alter table crm_chart_reports_di add column if not exists entity_code varchar(255);
alter table crm_chart_reports_di add column if not exists instance_id varchar(255);
alter table crm_chart_reports_di add column if not exists outcome varchar(255);
alter table crm_chart_reports_di add column if not exists outcome_des varchar(255);
alter table crm_chart_reports_di add column if not exists outcome_des_zh_cn varchar(255);
alter table crm_chart_reports_di add column if not exists pending_create_time timestamp;
alter table crm_chart_reports_di add column if not exists process_key varchar(255);
alter table crm_chart_reports_di add column if not exists process_version int4;
alter table crm_chart_reports_di add column if not exists proxy_staff varchar(255);
alter table crm_chart_reports_di add column if not exists proxy_staff_ids varchar(255);
alter table crm_chart_reports_di add column if not exists signature varchar(400);
alter table crm_chart_reports_di add column if not exists task_description varchar(255);
alter table crm_chart_reports_di add column if not exists task_description_zh_cn varchar(255);
alter table crm_chart_reports_di add column if not exists user_id int8;
alter table crm_chart_reports_di add column if not exists recalled_flag boolean;
alter table crm_chart_reports_di add column if not exists sort int4;
alter table crm_chart_reports_di add column if not exists table_info_id int8;
alter table crm_chart_reports_di add column if not exists user_agent varchar(255);
alter table crm_chart_reports_di add column if not exists main_obj int8;
alter table crm_chart_reports_di add column if not exists staff int8;
alter table crm_my_chart_elements add column if not exists id int8;
alter table crm_my_chart_elements add column if not exists version int4;
alter table crm_my_chart_elements add column if not exists create_staff_id int8;
alter table crm_my_chart_elements add column if not exists create_time timestamp;
alter table crm_my_chart_elements add column if not exists modify_staff_id int8;
alter table crm_my_chart_elements add column if not exists modify_time timestamp;
alter table crm_my_chart_elements add column if not exists valid INTEGER;
alter table crm_my_chart_elements add column if not exists cid int8;
alter table crm_my_chart_elements add column if not exists sort int4;
alter table crm_my_chart_elements add column if not exists decimal_digit int4;
alter table crm_my_chart_elements add column if not exists down_alarm varchar(20);
alter table crm_my_chart_elements add column if not exists down_limit varchar(20);
alter table crm_my_chart_elements add column if not exists element_name varchar(200);
alter table crm_my_chart_elements add column if not exists is_show_line boolean;
alter table crm_my_chart_elements add column if not exists is_show_point boolean;
alter table crm_my_chart_elements add column if not exists other_attrs varchar(2000);
alter table crm_my_chart_elements add column if not exists other_attrs_extra varchar(2000);
alter table crm_my_chart_elements add column if not exists up_alarm varchar(20);
alter table crm_my_chart_elements add column if not exists up_limit varchar(20);
alter table crm_my_chart_elements add column if not exists chart_id int8;
alter table crm_my_charts add column if not exists id int8;
alter table crm_my_charts add column if not exists version int4;
alter table crm_my_charts add column if not exists create_staff_id int8;
alter table crm_my_charts add column if not exists create_time timestamp;
alter table crm_my_charts add column if not exists modify_staff_id int8;
alter table crm_my_charts add column if not exists modify_time timestamp;
alter table crm_my_charts add column if not exists valid boolean;
alter table crm_my_charts add column if not exists cid int8;
alter table crm_my_charts add column if not exists sort int4;
alter table crm_my_charts add column if not exists create_department_id int8;
alter table crm_my_charts add column if not exists create_position_id int8;
alter table crm_my_charts add column if not exists deployment_id int8;
alter table crm_my_charts add column if not exists effect_staff_id int8;
alter table crm_my_charts add column if not exists effect_time timestamp;
alter table crm_my_charts add column if not exists effective_state int4;
alter table crm_my_charts add column if not exists group_id int8;
alter table crm_my_charts add column if not exists owner_department_id int8;
alter table crm_my_charts add column if not exists owner_position_id int8;
alter table crm_my_charts add column if not exists owner_staff_id int8;
alter table crm_my_charts add column if not exists position_lay_rec varchar(255);
alter table crm_my_charts add column if not exists process_key varchar(255);
alter table crm_my_charts add column if not exists process_version int4;
alter table crm_my_charts add column if not exists status int4;
alter table crm_my_charts add column if not exists table_no varchar(255);
alter table crm_my_charts add column if not exists axisxheight int4;
alter table crm_my_charts add column if not exists axisywidth int4;
alter table crm_my_charts add column if not exists chart_desc varchar(500);
alter table crm_my_charts add column if not exists chart_name varchar(200);
alter table crm_my_charts add column if not exists chart_type varchar(50);
alter table crm_my_charts add column if not exists column_count int4;
alter table crm_my_charts add column if not exists his_offset_time int4;
alter table crm_my_charts add column if not exists other_attrs varchar(2000);
alter table crm_my_charts add column if not exists point_number int4;
alter table crm_my_charts add column if not exists real_offset_time int4;
alter table crm_my_charts add column if not exists real_refresh_interval int4;
alter table crm_my_charts add column if not exists row_count int4;
alter table crm_my_charts add column if not exists table_height int4;
alter table crm_my_charts add column if not exists user_code varchar(100);
alter table crm_my_charts_di add column if not exists id int8;
alter table crm_my_charts_di add column if not exists version int4;
alter table crm_my_charts_di add column if not exists activity_name varchar(255);
alter table crm_my_charts_di add column if not exists assign_staff varchar(4000);
alter table crm_my_charts_di add column if not exists assign_staff_id varchar(255);
alter table crm_my_charts_di add column if not exists cid int8;
alter table crm_my_charts_di add column if not exists comments varchar(4000);
alter table crm_my_charts_di add column if not exists create_time timestamp;
alter table crm_my_charts_di add column if not exists dealinfo_type varchar(255);
alter table crm_my_charts_di add column if not exists entity_code varchar(255);
alter table crm_my_charts_di add column if not exists instance_id varchar(255);
alter table crm_my_charts_di add column if not exists outcome varchar(255);
alter table crm_my_charts_di add column if not exists outcome_des varchar(255);
alter table crm_my_charts_di add column if not exists outcome_des_zh_cn varchar(255);
alter table crm_my_charts_di add column if not exists pending_create_time timestamp;
alter table crm_my_charts_di add column if not exists process_key varchar(255);
alter table crm_my_charts_di add column if not exists process_version int4;
alter table crm_my_charts_di add column if not exists proxy_staff varchar(255);
alter table crm_my_charts_di add column if not exists proxy_staff_ids varchar(255);
alter table crm_my_charts_di add column if not exists signature varchar(400);
alter table crm_my_charts_di add column if not exists task_description varchar(255);
alter table crm_my_charts_di add column if not exists task_description_zh_cn varchar(255);
alter table crm_my_charts_di add column if not exists user_id int8;
alter table crm_my_charts_di add column if not exists recalled_flag boolean;
alter table crm_my_charts_di add column if not exists sort int4;
alter table crm_my_charts_di add column if not exists table_info_id int8;
alter table crm_my_charts_di add column if not exists user_agent varchar(255);
alter table crm_my_charts_di add column if not exists main_obj int8;
alter table crm_my_charts_di add column if not exists staff int8;
alter table crm_scatte_chart_sets add column if not exists id int8;
alter table crm_scatte_chart_sets add column if not exists version int4;
alter table crm_scatte_chart_sets add column if not exists create_staff_id int8;
alter table crm_scatte_chart_sets add column if not exists create_time timestamp;
alter table crm_scatte_chart_sets add column if not exists modify_staff_id int8;
alter table crm_scatte_chart_sets add column if not exists modify_time timestamp;
alter table crm_scatte_chart_sets add column if not exists valid boolean;
alter table crm_scatte_chart_sets add column if not exists cid int8;
alter table crm_scatte_chart_sets add column if not exists sort int4;
alter table crm_scatte_chart_sets add column if not exists create_department_id int8;
alter table crm_scatte_chart_sets add column if not exists create_position_id int8;
alter table crm_scatte_chart_sets add column if not exists deployment_id int8;
alter table crm_scatte_chart_sets add column if not exists effect_staff_id int8;
alter table crm_scatte_chart_sets add column if not exists effect_time timestamp;
alter table crm_scatte_chart_sets add column if not exists effective_state int4;
alter table crm_scatte_chart_sets add column if not exists group_id int8;
alter table crm_scatte_chart_sets add column if not exists owner_department_id int8;
alter table crm_scatte_chart_sets add column if not exists owner_position_id int8;
alter table crm_scatte_chart_sets add column if not exists owner_staff_id int8;
alter table crm_scatte_chart_sets add column if not exists position_lay_rec varchar(255);
alter table crm_scatte_chart_sets add column if not exists process_key varchar(255);
alter table crm_scatte_chart_sets add column if not exists process_version int4;
alter table crm_scatte_chart_sets add column if not exists status int4;
alter table crm_scatte_chart_sets add column if not exists table_no varchar(255);
alter table crm_scatte_chart_sets add column if not exists axisxbg_color varchar(255);
alter table crm_scatte_chart_sets add column if not exists axisxcolor varchar(255);
alter table crm_scatte_chart_sets add column if not exists axisxfont_color varchar(255);
alter table crm_scatte_chart_sets add column if not exists axisxfont_size int4;
alter table crm_scatte_chart_sets add column if not exists axisxheight int4;
alter table crm_scatte_chart_sets add column if not exists axisyfontsize int4;
alter table crm_scatte_chart_sets add column if not exists axisywidth int4;
alter table crm_scatte_chart_sets add column if not exists background_color varchar(255);
alter table crm_scatte_chart_sets add column if not exists border_color varchar(255);
alter table crm_scatte_chart_sets add column if not exists chart_height int4;
alter table crm_scatte_chart_sets add column if not exists chart_status boolean;
alter table crm_scatte_chart_sets add column if not exists chart_theme varchar(200);
alter table crm_scatte_chart_sets add column if not exists chart_width int4;
alter table crm_scatte_chart_sets add column if not exists columns_count int4;
alter table crm_scatte_chart_sets add column if not exists cp_default_colors varchar(200);
alter table crm_scatte_chart_sets add column if not exists decimal_digits int4;
alter table crm_scatte_chart_sets add column if not exists grid_bg_color varchar(255);
alter table crm_scatte_chart_sets add column if not exists grid_line_color varchar(255);
alter table crm_scatte_chart_sets add column if not exists max_element_group_num int4;
alter table crm_scatte_chart_sets add column if not exists max_point_num int4;
alter table crm_scatte_chart_sets add column if not exists other_attrs varchar(2000);
alter table crm_scatte_chart_sets add column if not exists rows_count varchar(256);
alter table crm_scatte_chart_sets add column if not exists system_theme boolean;
alter table crm_scatte_chart_sets add column if not exists data_interface varchar(255);
alter table crm_scatte_chart_sets_di add column if not exists id int8;
alter table crm_scatte_chart_sets_di add column if not exists version int4;
alter table crm_scatte_chart_sets_di add column if not exists activity_name varchar(255);
alter table crm_scatte_chart_sets_di add column if not exists assign_staff varchar(4000);
alter table crm_scatte_chart_sets_di add column if not exists assign_staff_id varchar(255);
alter table crm_scatte_chart_sets_di add column if not exists cid int8;
alter table crm_scatte_chart_sets_di add column if not exists comments varchar(4000);
alter table crm_scatte_chart_sets_di add column if not exists create_time timestamp;
alter table crm_scatte_chart_sets_di add column if not exists dealinfo_type varchar(255);
alter table crm_scatte_chart_sets_di add column if not exists entity_code varchar(255);
alter table crm_scatte_chart_sets_di add column if not exists instance_id varchar(255);
alter table crm_scatte_chart_sets_di add column if not exists outcome varchar(255);
alter table crm_scatte_chart_sets_di add column if not exists outcome_des varchar(255);
alter table crm_scatte_chart_sets_di add column if not exists outcome_des_zh_cn varchar(255);
alter table crm_scatte_chart_sets_di add column if not exists pending_create_time timestamp;
alter table crm_scatte_chart_sets_di add column if not exists process_key varchar(255);
alter table crm_scatte_chart_sets_di add column if not exists process_version int4;
alter table crm_scatte_chart_sets_di add column if not exists proxy_staff varchar(255);
alter table crm_scatte_chart_sets_di add column if not exists proxy_staff_ids varchar(255);
alter table crm_scatte_chart_sets_di add column if not exists signature varchar(400);
alter table crm_scatte_chart_sets_di add column if not exists task_description varchar(255);
alter table crm_scatte_chart_sets_di add column if not exists task_description_zh_cn varchar(255);
alter table crm_scatte_chart_sets_di add column if not exists user_id int8;
alter table crm_scatte_chart_sets_di add column if not exists recalled_flag boolean;
alter table crm_scatte_chart_sets_di add column if not exists sort int4;
alter table crm_scatte_chart_sets_di add column if not exists table_info_id int8;
alter table crm_scatte_chart_sets_di add column if not exists user_agent varchar(255);
alter table crm_scatte_chart_sets_di add column if not exists main_obj int8;
alter table crm_scatte_chart_sets_di add column if not exists staff int8;
alter table crm_trend_chart_sets add column if not exists id int8;
alter table crm_trend_chart_sets add column if not exists version int4;
alter table crm_trend_chart_sets add column if not exists create_staff_id int8;
alter table crm_trend_chart_sets add column if not exists create_time timestamp;
alter table crm_trend_chart_sets add column if not exists modify_staff_id int8;
alter table crm_trend_chart_sets add column if not exists modify_time timestamp;
alter table crm_trend_chart_sets add column if not exists valid boolean;
alter table crm_trend_chart_sets add column if not exists cid int8;
alter table crm_trend_chart_sets add column if not exists sort int4;
alter table crm_trend_chart_sets add column if not exists create_department_id int8;
alter table crm_trend_chart_sets add column if not exists create_position_id int8;
alter table crm_trend_chart_sets add column if not exists deployment_id int8;
alter table crm_trend_chart_sets add column if not exists effect_staff_id int8;
alter table crm_trend_chart_sets add column if not exists effect_time timestamp;
alter table crm_trend_chart_sets add column if not exists effective_state int4;
alter table crm_trend_chart_sets add column if not exists group_id int8;
alter table crm_trend_chart_sets add column if not exists owner_department_id int8;
alter table crm_trend_chart_sets add column if not exists owner_position_id int8;
alter table crm_trend_chart_sets add column if not exists owner_staff_id int8;
alter table crm_trend_chart_sets add column if not exists position_lay_rec varchar(255);
alter table crm_trend_chart_sets add column if not exists process_key varchar(255);
alter table crm_trend_chart_sets add column if not exists process_version int4;
alter table crm_trend_chart_sets add column if not exists status int4;
alter table crm_trend_chart_sets add column if not exists table_no varchar(255);
alter table crm_trend_chart_sets add column if not exists axisxbg_color varchar(255);
alter table crm_trend_chart_sets add column if not exists axisxcolor varchar(255);
alter table crm_trend_chart_sets add column if not exists axisxfont_color varchar(255);
alter table crm_trend_chart_sets add column if not exists axisxfont_size int4;
alter table crm_trend_chart_sets add column if not exists axisxheight int4;
alter table crm_trend_chart_sets add column if not exists axisyfontsize int4;
alter table crm_trend_chart_sets add column if not exists axisywidth int4;
alter table crm_trend_chart_sets add column if not exists background_color varchar(255);
alter table crm_trend_chart_sets add column if not exists border_color varchar(255);
alter table crm_trend_chart_sets add column if not exists chart_height int4;
alter table crm_trend_chart_sets add column if not exists chart_status boolean;
alter table crm_trend_chart_sets add column if not exists chart_theme varchar(200);
alter table crm_trend_chart_sets add column if not exists chart_width int4;
alter table crm_trend_chart_sets add column if not exists columns_count int4;
alter table crm_trend_chart_sets add column if not exists cp_default_colors varchar(500);
alter table crm_trend_chart_sets add column if not exists decimal_digits int4;
alter table crm_trend_chart_sets add column if not exists grid_bg_color varchar(255);
alter table crm_trend_chart_sets add column if not exists grid_line_color varchar(255);
alter table crm_trend_chart_sets add column if not exists his_interval_time int4;
alter table crm_trend_chart_sets add column if not exists is_convert_bool_to_true boolean;
alter table crm_trend_chart_sets add column if not exists is_quality_alarm boolean;
alter table crm_trend_chart_sets add column if not exists is_show_max_min boolean;
alter table crm_trend_chart_sets add column if not exists is_user_defined_bound boolean;
alter table crm_trend_chart_sets add column if not exists max_element_num int4;
alter table crm_trend_chart_sets add column if not exists max_point_num int4;
alter table crm_trend_chart_sets add column if not exists maxi_mum_tip boolean;
alter table crm_trend_chart_sets add column if not exists other_attrs varchar(500);
alter table crm_trend_chart_sets add column if not exists quality_alarm_range varchar(500);
alter table crm_trend_chart_sets add column if not exists real_interval_time int4;
alter table crm_trend_chart_sets add column if not exists real_refresh_time int4;
alter table crm_trend_chart_sets add column if not exists rows_count int4;
alter table crm_trend_chart_sets add column if not exists show_max_min_interval int4;
alter table crm_trend_chart_sets add column if not exists system_theme boolean;
alter table crm_trend_chart_sets add column if not exists table_columns_width int4;
alter table crm_trend_chart_sets add column if not exists table_fontsize int4;
alter table crm_trend_chart_sets add column if not exists axis_mode varchar(255);
alter table crm_trend_chart_sets add column if not exists data_interface varchar(255);
alter table crm_trend_chart_sets_di add column if not exists id int8;
alter table crm_trend_chart_sets_di add column if not exists version int4;
alter table crm_trend_chart_sets_di add column if not exists activity_name varchar(255);
alter table crm_trend_chart_sets_di add column if not exists assign_staff varchar(4000);
alter table crm_trend_chart_sets_di add column if not exists assign_staff_id varchar(255);
alter table crm_trend_chart_sets_di add column if not exists cid int8;
alter table crm_trend_chart_sets_di add column if not exists comments varchar(4000);
alter table crm_trend_chart_sets_di add column if not exists create_time timestamp;
alter table crm_trend_chart_sets_di add column if not exists dealinfo_type varchar(255);
alter table crm_trend_chart_sets_di add column if not exists entity_code varchar(255);
alter table crm_trend_chart_sets_di add column if not exists instance_id varchar(255);
alter table crm_trend_chart_sets_di add column if not exists outcome varchar(255);
alter table crm_trend_chart_sets_di add column if not exists outcome_des varchar(255);
alter table crm_trend_chart_sets_di add column if not exists outcome_des_zh_cn varchar(255);
alter table crm_trend_chart_sets_di add column if not exists pending_create_time timestamp;
alter table crm_trend_chart_sets_di add column if not exists process_key varchar(255);
alter table crm_trend_chart_sets_di add column if not exists process_version int4;
alter table crm_trend_chart_sets_di add column if not exists proxy_staff varchar(255);
alter table crm_trend_chart_sets_di add column if not exists proxy_staff_ids varchar(255);
alter table crm_trend_chart_sets_di add column if not exists signature varchar(400);
alter table crm_trend_chart_sets_di add column if not exists task_description varchar(255);
alter table crm_trend_chart_sets_di add column if not exists task_description_zh_cn varchar(255);
alter table crm_trend_chart_sets_di add column if not exists user_id int8;
alter table crm_trend_chart_sets_di add column if not exists recalled_flag boolean;
alter table crm_trend_chart_sets_di add column if not exists sort int4;
alter table crm_trend_chart_sets_di add column if not exists table_info_id int8;
alter table crm_trend_chart_sets_di add column if not exists user_agent varchar(255);
alter table crm_trend_chart_sets_di add column if not exists main_obj int8;
alter table crm_trend_chart_sets_di add column if not exists staff int8;
alter table doc_class_securities add column if not exists id int8;
alter table doc_class_securities add column if not exists version int4;
alter table doc_class_securities add column if not exists create_staff_id int8;
alter table doc_class_securities add column if not exists create_time timestamp;
alter table doc_class_securities add column if not exists modify_staff_id int8;
alter table doc_class_securities add column if not exists modify_time timestamp;
alter table doc_class_securities add column if not exists valid INTEGER;
alter table doc_class_securities add column if not exists cid int8;
alter table doc_class_securities add column if not exists sort int4;
alter table doc_class_securities add column if not exists bigintparama int4;
alter table doc_class_securities add column if not exists bigintparamb int4;
alter table doc_class_securities add column if not exists charparama varchar(255);
alter table doc_class_securities add column if not exists charparamb varchar(255);
alter table doc_class_securities add column if not exists code varchar(255);
alter table doc_class_securities add column if not exists contain_sub boolean;
alter table doc_class_securities add column if not exists dateparama timestamp;
alter table doc_class_securities add column if not exists dateparamb timestamp;
alter table doc_class_securities add column if not exists extra_col text;
alter table doc_class_securities add column if not exists numberparama numeric(19, 2);
alter table doc_class_securities add column if not exists numberparamb numeric(19, 2);
alter table doc_class_securities add column if not exists objparama int8;
alter table doc_class_securities add column if not exists objparamb int8;
alter table doc_class_securities add column if not exists other_range int8;
alter table doc_class_securities add column if not exists scparama varchar(255);
alter table doc_class_securities add column if not exists scparamb varchar(255);
alter table doc_class_securities add column if not exists staff_range varchar(256);
alter table doc_class_securities add column if not exists class_id int8;
alter table doc_class_securities add column if not exists doc_class_dept int8;
alter table doc_class_securities add column if not exists doc_class_position int8;
alter table doc_class_securities add column if not exists doc_class_role int8;
alter table doc_class_securities add column if not exists operate_type varchar(255);
alter table doc_class_securities add column if not exists power_rule varchar(255);
alter table doc_classifications add column if not exists id int8;
alter table doc_classifications add column if not exists version int4;
alter table doc_classifications add column if not exists create_staff_id int8;
alter table doc_classifications add column if not exists create_time timestamp;
alter table doc_classifications add column if not exists modify_staff_id int8;
alter table doc_classifications add column if not exists modify_time timestamp;
alter table doc_classifications add column if not exists valid INTEGER;
alter table doc_classifications add column if not exists cid int8;
alter table doc_classifications add column if not exists full_path_name varchar(255);
alter table doc_classifications add column if not exists lay_no int4;
alter table doc_classifications add column if not exists lay_rec varchar(255);
alter table doc_classifications add column if not exists leaf INTEGER;
alter table doc_classifications add column if not exists parent_id int8;
alter table doc_classifications add column if not exists sort int8;
alter table doc_classifications add column if not exists code varchar(200);
alter table doc_classifications add column if not exists memo_field varchar(256);
alter table doc_classifications add column if not exists name varchar(256);
alter table doc_doc_borrows add column if not exists id int8;
alter table doc_doc_borrows add column if not exists version int4;
alter table doc_doc_borrows add column if not exists create_staff_id int8;
alter table doc_doc_borrows add column if not exists create_time timestamp;
alter table doc_doc_borrows add column if not exists modify_staff_id int8;
alter table doc_doc_borrows add column if not exists modify_time timestamp;
alter table doc_doc_borrows add column if not exists valid INTEGER;
alter table doc_doc_borrows add column if not exists cid int8;
alter table doc_doc_borrows add column if not exists sort int4;
alter table doc_doc_borrows add column if not exists create_department_id int8;
alter table doc_doc_borrows add column if not exists create_position_id int8;
alter table doc_doc_borrows add column if not exists deployment_id int8;
alter table doc_doc_borrows add column if not exists effect_staff_id int8;
alter table doc_doc_borrows add column if not exists effect_time timestamp;
alter table doc_doc_borrows add column if not exists effective_state int4;
alter table doc_doc_borrows add column if not exists group_id int8;
alter table doc_doc_borrows add column if not exists owner_department_id int8;
alter table doc_doc_borrows add column if not exists owner_position_id int8;
alter table doc_doc_borrows add column if not exists owner_staff_id int8;
alter table doc_doc_borrows add column if not exists position_lay_rec varchar(255);
alter table doc_doc_borrows add column if not exists process_key varchar(255);
alter table doc_doc_borrows add column if not exists process_version int4;
alter table doc_doc_borrows add column if not exists status int4;
alter table doc_doc_borrows add column if not exists table_no varchar(255);
alter table doc_doc_borrows add column if not exists bigintparama int4;
alter table doc_doc_borrows add column if not exists bigintparamb int4;
alter table doc_doc_borrows add column if not exists bigintparamc int4;
alter table doc_doc_borrows add column if not exists bigintparamd int4;
alter table doc_doc_borrows add column if not exists bigintparame int4;
alter table doc_doc_borrows add column if not exists bigintparamf int4;
alter table doc_doc_borrows add column if not exists bigintparamg int4;
alter table doc_doc_borrows add column if not exists bigintparamh int4;
alter table doc_doc_borrows add column if not exists bigintparami int4;
alter table doc_doc_borrows add column if not exists bigintparamj int4;
alter table doc_doc_borrows add column if not exists bor_type varchar(255);
alter table doc_doc_borrows add column if not exists charparama varchar(255);
alter table doc_doc_borrows add column if not exists charparamb varchar(255);
alter table doc_doc_borrows add column if not exists charparamc varchar(255);
alter table doc_doc_borrows add column if not exists charparamd varchar(255);
alter table doc_doc_borrows add column if not exists charparame varchar(255);
alter table doc_doc_borrows add column if not exists charparamf varchar(255);
alter table doc_doc_borrows add column if not exists charparamg varchar(255);
alter table doc_doc_borrows add column if not exists charparamh varchar(255);
alter table doc_doc_borrows add column if not exists charparami varchar(255);
alter table doc_doc_borrows add column if not exists charparamj varchar(255);
alter table doc_doc_borrows add column if not exists dateparama timestamp;
alter table doc_doc_borrows add column if not exists dateparamb timestamp;
alter table doc_doc_borrows add column if not exists dateparamc timestamp;
alter table doc_doc_borrows add column if not exists dateparamd timestamp;
alter table doc_doc_borrows add column if not exists dateparame timestamp;
alter table doc_doc_borrows add column if not exists dateparamf timestamp;
alter table doc_doc_borrows add column if not exists dateparamg timestamp;
alter table doc_doc_borrows add column if not exists dateparamh timestamp;
alter table doc_doc_borrows add column if not exists day_length int8;
alter table doc_doc_borrows add column if not exists is_forever boolean;
alter table doc_doc_borrows add column if not exists numberparama numeric(19, 2);
alter table doc_doc_borrows add column if not exists numberparamb numeric(19, 2);
alter table doc_doc_borrows add column if not exists numberparamc numeric(19, 2);
alter table doc_doc_borrows add column if not exists numberparamd numeric(19, 2);
alter table doc_doc_borrows add column if not exists numberparame numeric(19, 2);
alter table doc_doc_borrows add column if not exists numberparamf numeric(19, 2);
alter table doc_doc_borrows add column if not exists objparama int8;
alter table doc_doc_borrows add column if not exists objparamb int8;
alter table doc_doc_borrows add column if not exists objparamc int8;
alter table doc_doc_borrows add column if not exists objparamd int8;
alter table doc_doc_borrows add column if not exists reason varchar(2000);
alter table doc_doc_borrows add column if not exists scparama varchar(255);
alter table doc_doc_borrows add column if not exists scparamb varchar(255);
alter table doc_doc_borrows add column if not exists scparamc varchar(255);
alter table doc_doc_borrows add column if not exists scparamd varchar(255);
alter table doc_doc_borrows add column if not exists start_date date;
alter table doc_doc_borrows add column if not exists table_info_id int8;
alter table doc_doc_borrows add column if not exists document_id int8;
alter table doc_doc_borrows add column if not exists staff_id int8;
alter table doc_doc_borrows_di add column if not exists id int8;
alter table doc_doc_borrows_di add column if not exists version int4;
alter table doc_doc_borrows_di add column if not exists activity_name varchar(255);
alter table doc_doc_borrows_di add column if not exists assign_staff varchar(4000);
alter table doc_doc_borrows_di add column if not exists assign_staff_id varchar(255);
alter table doc_doc_borrows_di add column if not exists cid int8;
alter table doc_doc_borrows_di add column if not exists comments varchar(4000);
alter table doc_doc_borrows_di add column if not exists create_time timestamp;
alter table doc_doc_borrows_di add column if not exists dealinfo_type varchar(255);
alter table doc_doc_borrows_di add column if not exists entity_code varchar(255);
alter table doc_doc_borrows_di add column if not exists instance_id varchar(255);
alter table doc_doc_borrows_di add column if not exists outcome varchar(255);
alter table doc_doc_borrows_di add column if not exists outcome_des varchar(255);
alter table doc_doc_borrows_di add column if not exists outcome_des_zh_cn varchar(255);
alter table doc_doc_borrows_di add column if not exists pending_create_time timestamp;
alter table doc_doc_borrows_di add column if not exists process_key varchar(255);
alter table doc_doc_borrows_di add column if not exists process_version int4;
alter table doc_doc_borrows_di add column if not exists proxy_staff varchar(255);
alter table doc_doc_borrows_di add column if not exists proxy_staff_ids varchar(255);
alter table doc_doc_borrows_di add column if not exists signature varchar(400);
alter table doc_doc_borrows_di add column if not exists task_description varchar(255);
alter table doc_doc_borrows_di add column if not exists task_description_zh_cn varchar(255);
alter table doc_doc_borrows_di add column if not exists user_id int8;
alter table doc_doc_borrows_di add column if not exists recalled_flag boolean;
alter table doc_doc_borrows_di add column if not exists sort int4;
alter table doc_doc_borrows_di add column if not exists table_info_id int8;
alter table doc_doc_borrows_di add column if not exists user_agent varchar(255);
alter table doc_doc_borrows_di add column if not exists main_obj int8;
alter table doc_doc_borrows_di add column if not exists staff int8;
alter table doc_doc_borrows_pa add column if not exists id int8;
alter table doc_doc_borrows_pa add column if not exists version int4;
alter table doc_doc_borrows_pa add column if not exists create_staff_id int8;
alter table doc_doc_borrows_pa add column if not exists create_time timestamp;
alter table doc_doc_borrows_pa add column if not exists delete_staff_id int8;
alter table doc_doc_borrows_pa add column if not exists delete_time timestamp;
alter table doc_doc_borrows_pa add column if not exists modify_staff_id int8;
alter table doc_doc_borrows_pa add column if not exists modify_time timestamp;
alter table doc_doc_borrows_pa add column if not exists table_info_id int8;
alter table doc_doc_borrows_pa add column if not exists valid INTEGER;
alter table doc_doc_borrows_pa add column if not exists main_obj int8;
alter table doc_doc_borrows_pa add column if not exists staff int8;
alter table doc_doc_borrows_sv add column if not exists id int8;
alter table doc_doc_borrows_sv add column if not exists version int4;
alter table doc_doc_borrows_sv add column if not exists create_staff_id int8;
alter table doc_doc_borrows_sv add column if not exists create_time timestamp;
alter table doc_doc_borrows_sv add column if not exists delete_staff_id int8;
alter table doc_doc_borrows_sv add column if not exists delete_time timestamp;
alter table doc_doc_borrows_sv add column if not exists modify_staff_id int8;
alter table doc_doc_borrows_sv add column if not exists modify_time timestamp;
alter table doc_doc_borrows_sv add column if not exists table_info_id int8;
alter table doc_doc_borrows_sv add column if not exists valid INTEGER;
alter table doc_doc_borrows_sv add column if not exists main_obj int8;
alter table doc_doc_borrows_sv add column if not exists staff int8;
alter table doc_doc_classes add column if not exists id int8;
alter table doc_doc_classes add column if not exists version int4;
alter table doc_doc_classes add column if not exists create_staff_id int8;
alter table doc_doc_classes add column if not exists create_time timestamp;
alter table doc_doc_classes add column if not exists modify_staff_id int8;
alter table doc_doc_classes add column if not exists modify_time timestamp;
alter table doc_doc_classes add column if not exists valid boolean;
alter table doc_doc_classes add column if not exists cid int8;
alter table doc_doc_classes add column if not exists full_path_name varchar(255);
alter table doc_doc_classes add column if not exists lay_no int4;
alter table doc_doc_classes add column if not exists lay_rec varchar(255);
alter table doc_doc_classes add column if not exists leaf INTEGER;
alter table doc_doc_classes add column if not exists parent_id int8;
alter table doc_doc_classes add column if not exists sort int8;
alter table doc_doc_classes add column if not exists create_department_id int8;
alter table doc_doc_classes add column if not exists create_position_id int8;
alter table doc_doc_classes add column if not exists deployment_id int8;
alter table doc_doc_classes add column if not exists effect_staff_id int8;
alter table doc_doc_classes add column if not exists effect_time timestamp;
alter table doc_doc_classes add column if not exists effective_state int4;
alter table doc_doc_classes add column if not exists oa text;
alter table doc_doc_classes add column if not exists owner_department_id int8;
alter table doc_doc_classes add column if not exists owner_position_id int8;
alter table doc_doc_classes add column if not exists owner_staff_id int8;
alter table doc_doc_classes add column if not exists position_lay_rec varchar(255);
alter table doc_doc_classes add column if not exists process_key varchar(255);
alter table doc_doc_classes add column if not exists process_version int4;
alter table doc_doc_classes add column if not exists status int4;
alter table doc_doc_classes add column if not exists table_no varchar(255);
alter table doc_doc_classes add column if not exists bigintparama int4;
alter table doc_doc_classes add column if not exists bigintparamb int4;
alter table doc_doc_classes add column if not exists charparama varchar(255);
alter table doc_doc_classes add column if not exists charparamb varchar(255);
alter table doc_doc_classes add column if not exists code varchar(255);
alter table doc_doc_classes add column if not exists dateparama timestamp;
alter table doc_doc_classes add column if not exists dateparamb timestamp;
alter table doc_doc_classes add column if not exists extra_col text;
alter table doc_doc_classes add column if not exists memo_field text;
alter table doc_doc_classes add column if not exists name varchar(256);
alter table doc_doc_classes add column if not exists numberparama numeric(19, 2);
alter table doc_doc_classes add column if not exists numberparamb numeric(19, 2);
alter table doc_doc_classes add column if not exists objparama int8;
alter table doc_doc_classes add column if not exists objparamb int8;
alter table doc_doc_classes add column if not exists scparama varchar(255);
alter table doc_doc_classes add column if not exists scparamb varchar(255);
alter table doc_doc_classes_di add column if not exists id int8;
alter table doc_doc_classes_di add column if not exists version int4;
alter table doc_doc_classes_di add column if not exists activity_name varchar(255);
alter table doc_doc_classes_di add column if not exists assign_staff varchar(4000);
alter table doc_doc_classes_di add column if not exists assign_staff_id varchar(255);
alter table doc_doc_classes_di add column if not exists cid int8;
alter table doc_doc_classes_di add column if not exists comments varchar(4000);
alter table doc_doc_classes_di add column if not exists create_time timestamp;
alter table doc_doc_classes_di add column if not exists dealinfo_type varchar(255);
alter table doc_doc_classes_di add column if not exists entity_code varchar(255);
alter table doc_doc_classes_di add column if not exists instance_id varchar(255);
alter table doc_doc_classes_di add column if not exists outcome varchar(255);
alter table doc_doc_classes_di add column if not exists outcome_des varchar(255);
alter table doc_doc_classes_di add column if not exists outcome_des_zh_cn varchar(255);
alter table doc_doc_classes_di add column if not exists pending_create_time timestamp;
alter table doc_doc_classes_di add column if not exists process_key varchar(255);
alter table doc_doc_classes_di add column if not exists process_version int4;
alter table doc_doc_classes_di add column if not exists proxy_staff varchar(255);
alter table doc_doc_classes_di add column if not exists proxy_staff_ids varchar(255);
alter table doc_doc_classes_di add column if not exists signature varchar(400);
alter table doc_doc_classes_di add column if not exists task_description varchar(255);
alter table doc_doc_classes_di add column if not exists task_description_zh_cn varchar(255);
alter table doc_doc_classes_di add column if not exists user_id int8;
alter table doc_doc_classes_di add column if not exists recalled_flag boolean;
alter table doc_doc_classes_di add column if not exists sort int4;
alter table doc_doc_classes_di add column if not exists table_info_id int8;
alter table doc_doc_classes_di add column if not exists user_agent varchar(255);
alter table doc_doc_classes_di add column if not exists main_obj int8;
alter table doc_doc_classes_di add column if not exists staff int8;
alter table doc_doc_documents add column if not exists id int8;
alter table doc_doc_documents add column if not exists version int4;
alter table doc_doc_documents add column if not exists create_staff_id int8;
alter table doc_doc_documents add column if not exists create_time timestamp;
alter table doc_doc_documents add column if not exists modify_staff_id int8;
alter table doc_doc_documents add column if not exists modify_time timestamp;
alter table doc_doc_documents add column if not exists valid INTEGER;
alter table doc_doc_documents add column if not exists cid int8;
alter table doc_doc_documents add column if not exists sort int4;
alter table doc_doc_documents add column if not exists create_department_id int8;
alter table doc_doc_documents add column if not exists create_position_id int8;
alter table doc_doc_documents add column if not exists deployment_id int8;
alter table doc_doc_documents add column if not exists effect_staff_id int8;
alter table doc_doc_documents add column if not exists effect_time timestamp;
alter table doc_doc_documents add column if not exists effective_state int4;
alter table doc_doc_documents add column if not exists group_id int8;
alter table doc_doc_documents add column if not exists owner_department_id int8;
alter table doc_doc_documents add column if not exists owner_position_id int8;
alter table doc_doc_documents add column if not exists owner_staff_id int8;
alter table doc_doc_documents add column if not exists position_lay_rec varchar(255);
alter table doc_doc_documents add column if not exists process_key varchar(255);
alter table doc_doc_documents add column if not exists process_version int4;
alter table doc_doc_documents add column if not exists status int4;
alter table doc_doc_documents add column if not exists table_no varchar(255);
alter table doc_doc_documents add column if not exists bigintparama int4;
alter table doc_doc_documents add column if not exists bigintparamb int4;
alter table doc_doc_documents add column if not exists bigintparamc int4;
alter table doc_doc_documents add column if not exists bigintparamd int4;
alter table doc_doc_documents add column if not exists bigintparame int4;
alter table doc_doc_documents add column if not exists bigintparamf int4;
alter table doc_doc_documents add column if not exists bigintparamg int4;
alter table doc_doc_documents add column if not exists bigintparamh int4;
alter table doc_doc_documents add column if not exists bigintparami int4;
alter table doc_doc_documents add column if not exists bigintparamj int4;
alter table doc_doc_documents add column if not exists charparama varchar(255);
alter table doc_doc_documents add column if not exists charparamb varchar(255);
alter table doc_doc_documents add column if not exists charparamc varchar(255);
alter table doc_doc_documents add column if not exists charparamd varchar(255);
alter table doc_doc_documents add column if not exists charparame varchar(255);
alter table doc_doc_documents add column if not exists charparamf varchar(255);
alter table doc_doc_documents add column if not exists charparamg varchar(255);
alter table doc_doc_documents add column if not exists charparamh varchar(255);
alter table doc_doc_documents add column if not exists charparami varchar(255);
alter table doc_doc_documents add column if not exists charparamj varchar(255);
alter table doc_doc_documents add column if not exists code varchar(256);
alter table doc_doc_documents add column if not exists dateparama timestamp;
alter table doc_doc_documents add column if not exists dateparamb timestamp;
alter table doc_doc_documents add column if not exists dateparamc timestamp;
alter table doc_doc_documents add column if not exists dateparamd timestamp;
alter table doc_doc_documents add column if not exists dateparame timestamp;
alter table doc_doc_documents add column if not exists dateparamf timestamp;
alter table doc_doc_documents add column if not exists dateparamg timestamp;
alter table doc_doc_documents add column if not exists dateparamh timestamp;
alter table doc_doc_documents add column if not exists doc_summary varchar(256);
alter table doc_doc_documents add column if not exists doc_version varchar(256);
alter table doc_doc_documents add column if not exists expire_date date;
alter table doc_doc_documents add column if not exists extra_col text;
alter table doc_doc_documents add column if not exists is_current boolean;
alter table doc_doc_documents add column if not exists is_effective boolean;
alter table doc_doc_documents add column if not exists keyword varchar(256);
alter table doc_doc_documents add column if not exists main_author varchar(256);
alter table doc_doc_documents add column if not exists memo_field text;
alter table doc_doc_documents add column if not exists numberparama numeric(19, 2);
alter table doc_doc_documents add column if not exists numberparamb numeric(19, 2);
alter table doc_doc_documents add column if not exists numberparamc numeric(19, 2);
alter table doc_doc_documents add column if not exists numberparamd numeric(19, 2);
alter table doc_doc_documents add column if not exists numberparame numeric(19, 2);
alter table doc_doc_documents add column if not exists numberparamf numeric(19, 2);
alter table doc_doc_documents add column if not exists objparama int8;
alter table doc_doc_documents add column if not exists objparamb int8;
alter table doc_doc_documents add column if not exists objparamc int8;
alter table doc_doc_documents add column if not exists objparamd int8;
alter table doc_doc_documents add column if not exists remind_date date;
alter table doc_doc_documents add column if not exists scparama varchar(255);
alter table doc_doc_documents add column if not exists scparamb varchar(255);
alter table doc_doc_documents add column if not exists scparamc varchar(255);
alter table doc_doc_documents add column if not exists scparamd varchar(255);
alter table doc_doc_documents add column if not exists sec_author varchar(256);
alter table doc_doc_documents add column if not exists table_info_id int8;
alter table doc_doc_documents add column if not exists take_effect boolean;
alter table doc_doc_documents add column if not exists title varchar(256);
alter table doc_doc_documents add column if not exists doc_class_id int8;
alter table doc_doc_documents add column if not exists doc_type varchar(255);
alter table doc_doc_documents add column if not exists document_state varchar(255);
alter table doc_doc_documents add column if not exists language_type varchar(255);
alter table doc_doc_documents add column if not exists secret_class varchar(255);
alter table doc_doc_documents_di add column if not exists id int8;
alter table doc_doc_documents_di add column if not exists version int4;
alter table doc_doc_documents_di add column if not exists activity_name varchar(255);
alter table doc_doc_documents_di add column if not exists assign_staff varchar(4000);
alter table doc_doc_documents_di add column if not exists assign_staff_id varchar(255);
alter table doc_doc_documents_di add column if not exists cid int8;
alter table doc_doc_documents_di add column if not exists comments varchar(4000);
alter table doc_doc_documents_di add column if not exists create_time timestamp;
alter table doc_doc_documents_di add column if not exists dealinfo_type varchar(255);
alter table doc_doc_documents_di add column if not exists entity_code varchar(255);
alter table doc_doc_documents_di add column if not exists instance_id varchar(255);
alter table doc_doc_documents_di add column if not exists outcome varchar(255);
alter table doc_doc_documents_di add column if not exists outcome_des varchar(255);
alter table doc_doc_documents_di add column if not exists outcome_des_zh_cn varchar(255);
alter table doc_doc_documents_di add column if not exists pending_create_time timestamp;
alter table doc_doc_documents_di add column if not exists process_key varchar(255);
alter table doc_doc_documents_di add column if not exists process_version int4;
alter table doc_doc_documents_di add column if not exists proxy_staff varchar(255);
alter table doc_doc_documents_di add column if not exists proxy_staff_ids varchar(255);
alter table doc_doc_documents_di add column if not exists signature varchar(400);
alter table doc_doc_documents_di add column if not exists task_description varchar(255);
alter table doc_doc_documents_di add column if not exists task_description_zh_cn varchar(255);
alter table doc_doc_documents_di add column if not exists user_id int8;
alter table doc_doc_documents_di add column if not exists recalled_flag boolean;
alter table doc_doc_documents_di add column if not exists sort int4;
alter table doc_doc_documents_di add column if not exists table_info_id int8;
alter table doc_doc_documents_di add column if not exists user_agent varchar(255);
alter table doc_doc_documents_di add column if not exists main_obj int8;
alter table doc_doc_documents_di add column if not exists staff int8;
alter table doc_doc_documents_pa add column if not exists id int8;
alter table doc_doc_documents_pa add column if not exists version int4;
alter table doc_doc_documents_pa add column if not exists create_staff_id int8;
alter table doc_doc_documents_pa add column if not exists create_time timestamp;
alter table doc_doc_documents_pa add column if not exists delete_staff_id int8;
alter table doc_doc_documents_pa add column if not exists delete_time timestamp;
alter table doc_doc_documents_pa add column if not exists modify_staff_id int8;
alter table doc_doc_documents_pa add column if not exists modify_time timestamp;
alter table doc_doc_documents_pa add column if not exists table_info_id int8;
alter table doc_doc_documents_pa add column if not exists valid INTEGER;
alter table doc_doc_documents_pa add column if not exists main_obj int8;
alter table doc_doc_documents_pa add column if not exists staff int8;
alter table doc_doc_documents_sv add column if not exists id int8;
alter table doc_doc_documents_sv add column if not exists version int4;
alter table doc_doc_documents_sv add column if not exists create_staff_id int8;
alter table doc_doc_documents_sv add column if not exists create_time timestamp;
alter table doc_doc_documents_sv add column if not exists delete_staff_id int8;
alter table doc_doc_documents_sv add column if not exists delete_time timestamp;
alter table doc_doc_documents_sv add column if not exists modify_staff_id int8;
alter table doc_doc_documents_sv add column if not exists modify_time timestamp;
alter table doc_doc_documents_sv add column if not exists table_info_id int8;
alter table doc_doc_documents_sv add column if not exists valid INTEGER;
alter table doc_doc_documents_sv add column if not exists main_obj int8;
alter table doc_doc_documents_sv add column if not exists staff int8;
alter table doc_doc_histories add column if not exists id int8;
alter table doc_doc_histories add column if not exists version int4;
alter table doc_doc_histories add column if not exists create_staff_id int8;
alter table doc_doc_histories add column if not exists create_time timestamp;
alter table doc_doc_histories add column if not exists modify_staff_id int8;
alter table doc_doc_histories add column if not exists modify_time timestamp;
alter table doc_doc_histories add column if not exists valid INTEGER;
alter table doc_doc_histories add column if not exists cid int8;
alter table doc_doc_histories add column if not exists sort int4;
alter table doc_doc_histories add column if not exists modify_content varchar(256);
alter table doc_doc_histories add column if not exists modify_reason varchar(256);
alter table doc_doc_histories add column if not exists record_time timestamp;
alter table doc_doc_histories add column if not exists table_info_id int8;
alter table doc_doc_histories add column if not exists upper_doc_id varchar(256);
alter table doc_doc_histories add column if not exists document_id int8;
alter table doc_doc_histories add column if not exists modify_type varchar(255);
alter table doc_doc_histories add column if not exists staff_id int8;
alter table doc_doc_objects add column if not exists id int8;
alter table doc_doc_objects add column if not exists version int4;
alter table doc_doc_objects add column if not exists create_staff_id int8;
alter table doc_doc_objects add column if not exists create_time timestamp;
alter table doc_doc_objects add column if not exists modify_staff_id int8;
alter table doc_doc_objects add column if not exists modify_time timestamp;
alter table doc_doc_objects add column if not exists valid INTEGER;
alter table doc_doc_objects add column if not exists cid int8;
alter table doc_doc_objects add column if not exists sort int4;
alter table doc_doc_objects add column if not exists model_code varchar(256);
alter table doc_doc_objects add column if not exists object_id int8;
alter table doc_doc_objects add column if not exists table_info_id int8;
alter table doc_doc_objects add column if not exists document_id int8;
alter table doc_doc_reminders add column if not exists id int8;
alter table doc_doc_reminders add column if not exists version int4;
alter table doc_doc_reminders add column if not exists create_staff_id int8;
alter table doc_doc_reminders add column if not exists create_time timestamp;
alter table doc_doc_reminders add column if not exists modify_staff_id int8;
alter table doc_doc_reminders add column if not exists modify_time timestamp;
alter table doc_doc_reminders add column if not exists valid INTEGER;
alter table doc_doc_reminders add column if not exists cid int8;
alter table doc_doc_reminders add column if not exists sort int4;
alter table doc_doc_reminders add column if not exists table_info_id int8;
alter table doc_doc_reminders add column if not exists document_id int8;
alter table doc_doc_reminders add column if not exists reminder int8;
alter table doc_docu_depts add column if not exists id int8;
alter table doc_docu_depts add column if not exists version int4;
alter table doc_docu_depts add column if not exists create_staff_id int8;
alter table doc_docu_depts add column if not exists create_time timestamp;
alter table doc_docu_depts add column if not exists modify_staff_id int8;
alter table doc_docu_depts add column if not exists modify_time timestamp;
alter table doc_docu_depts add column if not exists valid INTEGER;
alter table doc_docu_depts add column if not exists cid int8;
alter table doc_docu_depts add column if not exists sort int4;
alter table doc_docu_depts add column if not exists table_info_id int8;
alter table doc_docu_depts add column if not exists doc_dept_id int8;
alter table doc_docu_depts add column if not exists document_id int8;
alter table doc_enclosure_heads add column if not exists id int8;
alter table doc_enclosure_heads add column if not exists version int4;
alter table doc_enclosure_heads add column if not exists create_staff_id int8;
alter table doc_enclosure_heads add column if not exists create_time timestamp;
alter table doc_enclosure_heads add column if not exists modify_staff_id int8;
alter table doc_enclosure_heads add column if not exists modify_time timestamp;
alter table doc_enclosure_heads add column if not exists valid boolean;
alter table doc_enclosure_heads add column if not exists cid int8;
alter table doc_enclosure_heads add column if not exists sort int4;
alter table doc_enclosure_heads add column if not exists create_department_id int8;
alter table doc_enclosure_heads add column if not exists create_position_id int8;
alter table doc_enclosure_heads add column if not exists deployment_id int8;
alter table doc_enclosure_heads add column if not exists effect_staff_id int8;
alter table doc_enclosure_heads add column if not exists effect_time timestamp;
alter table doc_enclosure_heads add column if not exists effective_state int4;
alter table doc_enclosure_heads add column if not exists group_id int8;
alter table doc_enclosure_heads add column if not exists owner_department_id int8;
alter table doc_enclosure_heads add column if not exists owner_position_id int8;
alter table doc_enclosure_heads add column if not exists owner_staff_id int8;
alter table doc_enclosure_heads add column if not exists position_lay_rec varchar(255);
alter table doc_enclosure_heads add column if not exists process_key varchar(255);
alter table doc_enclosure_heads add column if not exists process_version int4;
alter table doc_enclosure_heads add column if not exists status int4;
alter table doc_enclosure_heads add column if not exists table_no varchar(255);
alter table doc_enclosure_heads add column if not exists change int4;
alter table doc_enclosure_heads add column if not exists code varchar(256);
alter table doc_enclosure_heads add column if not exists doc_table_info_id int8;
alter table doc_enclosure_heads add column if not exists doc_table_no varchar(256);
alter table doc_enclosure_heads add column if not exists from_id int8;
alter table doc_enclosure_heads add column if not exists model_code varchar(256);
alter table doc_enclosure_heads add column if not exists property_code varchar(256);
alter table doc_enclosure_heads add column if not exists doc_id int8;
alter table doc_enclosure_heads_di add column if not exists id int8;
alter table doc_enclosure_heads_di add column if not exists version int4;
alter table doc_enclosure_heads_di add column if not exists activity_name varchar(255);
alter table doc_enclosure_heads_di add column if not exists assign_staff varchar(4000);
alter table doc_enclosure_heads_di add column if not exists assign_staff_id varchar(255);
alter table doc_enclosure_heads_di add column if not exists cid int8;
alter table doc_enclosure_heads_di add column if not exists comments varchar(4000);
alter table doc_enclosure_heads_di add column if not exists create_time timestamp;
alter table doc_enclosure_heads_di add column if not exists dealinfo_type varchar(255);
alter table doc_enclosure_heads_di add column if not exists entity_code varchar(255);
alter table doc_enclosure_heads_di add column if not exists instance_id varchar(255);
alter table doc_enclosure_heads_di add column if not exists outcome varchar(255);
alter table doc_enclosure_heads_di add column if not exists outcome_des varchar(255);
alter table doc_enclosure_heads_di add column if not exists outcome_des_zh_cn varchar(255);
alter table doc_enclosure_heads_di add column if not exists pending_create_time timestamp;
alter table doc_enclosure_heads_di add column if not exists process_key varchar(255);
alter table doc_enclosure_heads_di add column if not exists process_version int4;
alter table doc_enclosure_heads_di add column if not exists proxy_staff varchar(255);
alter table doc_enclosure_heads_di add column if not exists proxy_staff_ids varchar(255);
alter table doc_enclosure_heads_di add column if not exists signature varchar(400);
alter table doc_enclosure_heads_di add column if not exists task_description varchar(255);
alter table doc_enclosure_heads_di add column if not exists task_description_zh_cn varchar(255);
alter table doc_enclosure_heads_di add column if not exists user_id int8;
alter table doc_enclosure_heads_di add column if not exists recalled_flag boolean;
alter table doc_enclosure_heads_di add column if not exists sort int4;
alter table doc_enclosure_heads_di add column if not exists table_info_id int8;
alter table doc_enclosure_heads_di add column if not exists user_agent varchar(255);
alter table doc_enclosure_heads_di add column if not exists main_obj int8;
alter table doc_enclosure_heads_di add column if not exists staff int8;
alter table doc_enclosure_mngs add column if not exists id int8;
alter table doc_enclosure_mngs add column if not exists version int4;
alter table doc_enclosure_mngs add column if not exists create_staff_id int8;
alter table doc_enclosure_mngs add column if not exists create_time timestamp;
alter table doc_enclosure_mngs add column if not exists modify_staff_id int8;
alter table doc_enclosure_mngs add column if not exists modify_time timestamp;
alter table doc_enclosure_mngs add column if not exists valid INTEGER;
alter table doc_enclosure_mngs add column if not exists cid int8;
alter table doc_enclosure_mngs add column if not exists sort int4;
alter table doc_enclosure_mngs add column if not exists change int4;
alter table doc_enclosure_mngs add column if not exists code varchar(256);
alter table doc_enclosure_mngs add column if not exists data_id int8;
alter table doc_enclosure_mngs add column if not exists doc_table_info_id int8;
alter table doc_enclosure_mngs add column if not exists doc_table_no varchar(256);
alter table doc_enclosure_mngs add column if not exists model_code varchar(256);
alter table doc_enclosure_mngs add column if not exists property_code varchar(256);
alter table doc_enclosure_mngs add column if not exists doc_id int8;
alter table doc_enclosure_mngs add column if not exists head_id int8;
alter table doc_laws add column if not exists id int8;
alter table doc_laws add column if not exists version int4;
alter table doc_laws add column if not exists create_staff_id int8;
alter table doc_laws add column if not exists create_time timestamp;
alter table doc_laws add column if not exists modify_staff_id int8;
alter table doc_laws add column if not exists modify_time timestamp;
alter table doc_laws add column if not exists valid boolean;
alter table doc_laws add column if not exists cid int8;
alter table doc_laws add column if not exists sort int4;
alter table doc_laws add column if not exists create_department_id int8;
alter table doc_laws add column if not exists create_position_id int8;
alter table doc_laws add column if not exists deployment_id int8;
alter table doc_laws add column if not exists effect_staff_id int8;
alter table doc_laws add column if not exists effect_time timestamp;
alter table doc_laws add column if not exists effective_state int4;
alter table doc_laws add column if not exists group_id int8;
alter table doc_laws add column if not exists owner_department_id int8;
alter table doc_laws add column if not exists owner_position_id int8;
alter table doc_laws add column if not exists owner_staff_id int8;
alter table doc_laws add column if not exists position_lay_rec varchar(255);
alter table doc_laws add column if not exists process_key varchar(255);
alter table doc_laws add column if not exists process_version int4;
alter table doc_laws add column if not exists status int4;
alter table doc_laws add column if not exists table_no varchar(255);
alter table doc_laws add column if not exists apply_dept varchar(256);
alter table doc_laws add column if not exists bigintparama int4;
alter table doc_laws add column if not exists bigintparamb int4;
alter table doc_laws add column if not exists bigintparamc int4;
alter table doc_laws add column if not exists bigintparamd int4;
alter table doc_laws add column if not exists bigintparame int4;
alter table doc_laws add column if not exists bigintparamf int4;
alter table doc_laws add column if not exists bigintparamg int4;
alter table doc_laws add column if not exists bigintparamh int4;
alter table doc_laws add column if not exists bigintparami int4;
alter table doc_laws add column if not exists bigintparamj int4;
alter table doc_laws add column if not exists charparama varchar(255);
alter table doc_laws add column if not exists charparamb varchar(255);
alter table doc_laws add column if not exists charparamc varchar(255);
alter table doc_laws add column if not exists charparamd varchar(255);
alter table doc_laws add column if not exists charparame varchar(255);
alter table doc_laws add column if not exists charparamf varchar(255);
alter table doc_laws add column if not exists charparamg varchar(255);
alter table doc_laws add column if not exists charparamh varchar(255);
alter table doc_laws add column if not exists charparami varchar(255);
alter table doc_laws add column if not exists charparamj varchar(255);
alter table doc_laws add column if not exists code varchar(255);
alter table doc_laws add column if not exists dateparama timestamp;
alter table doc_laws add column if not exists dateparamb timestamp;
alter table doc_laws add column if not exists dateparamc timestamp;
alter table doc_laws add column if not exists dateparamd timestamp;
alter table doc_laws add column if not exists dateparame timestamp;
alter table doc_laws add column if not exists dateparamf timestamp;
alter table doc_laws add column if not exists dateparamg timestamp;
alter table doc_laws add column if not exists dateparamh timestamp;
alter table doc_laws add column if not exists effective_date date;
alter table doc_laws add column if not exists memo_field text;
alter table doc_laws add column if not exists name varchar(256);
alter table doc_laws add column if not exists numberparama numeric(19, 2);
alter table doc_laws add column if not exists numberparamb numeric(19, 2);
alter table doc_laws add column if not exists numberparamc numeric(19, 2);
alter table doc_laws add column if not exists numberparamd numeric(19, 2);
alter table doc_laws add column if not exists numberparame numeric(19, 2);
alter table doc_laws add column if not exists numberparamf numeric(19, 2);
alter table doc_laws add column if not exists objparama int8;
alter table doc_laws add column if not exists objparamb int8;
alter table doc_laws add column if not exists objparamc int8;
alter table doc_laws add column if not exists objparamd int8;
alter table doc_laws add column if not exists release_date date;
alter table doc_laws add column if not exists release_dept varchar(256);
alter table doc_laws add column if not exists release_num varchar(256);
alter table doc_laws add column if not exists scparama varchar(255);
alter table doc_laws add column if not exists scparamb varchar(255);
alter table doc_laws add column if not exists scparamc varchar(255);
alter table doc_laws add column if not exists scparamd varchar(255);
alter table doc_laws add column if not exists classification int8;
alter table doc_laws_di add column if not exists id int8;
alter table doc_laws_di add column if not exists version int4;
alter table doc_laws_di add column if not exists activity_name varchar(255);
alter table doc_laws_di add column if not exists assign_staff varchar(4000);
alter table doc_laws_di add column if not exists assign_staff_id varchar(255);
alter table doc_laws_di add column if not exists cid int8;
alter table doc_laws_di add column if not exists comments varchar(4000);
alter table doc_laws_di add column if not exists create_time timestamp;
alter table doc_laws_di add column if not exists dealinfo_type varchar(255);
alter table doc_laws_di add column if not exists entity_code varchar(255);
alter table doc_laws_di add column if not exists instance_id varchar(255);
alter table doc_laws_di add column if not exists outcome varchar(255);
alter table doc_laws_di add column if not exists outcome_des varchar(255);
alter table doc_laws_di add column if not exists outcome_des_zh_cn varchar(255);
alter table doc_laws_di add column if not exists pending_create_time timestamp;
alter table doc_laws_di add column if not exists process_key varchar(255);
alter table doc_laws_di add column if not exists process_version int4;
alter table doc_laws_di add column if not exists proxy_staff varchar(255);
alter table doc_laws_di add column if not exists proxy_staff_ids varchar(255);
alter table doc_laws_di add column if not exists signature varchar(400);
alter table doc_laws_di add column if not exists task_description varchar(255);
alter table doc_laws_di add column if not exists task_description_zh_cn varchar(255);
alter table doc_laws_di add column if not exists user_id int8;
alter table doc_laws_di add column if not exists recalled_flag boolean;
alter table doc_laws_di add column if not exists sort int4;
alter table doc_laws_di add column if not exists table_info_id int8;
alter table doc_laws_di add column if not exists user_agent varchar(255);
alter table doc_laws_di add column if not exists main_obj int8;
alter table doc_laws_di add column if not exists staff int8;
alter table doc_legal_provisions add column if not exists id int8;
alter table doc_legal_provisions add column if not exists version int4;
alter table doc_legal_provisions add column if not exists create_staff_id int8;
alter table doc_legal_provisions add column if not exists create_time timestamp;
alter table doc_legal_provisions add column if not exists modify_staff_id int8;
alter table doc_legal_provisions add column if not exists modify_time timestamp;
alter table doc_legal_provisions add column if not exists valid INTEGER;
alter table doc_legal_provisions add column if not exists cid int8;
alter table doc_legal_provisions add column if not exists sort int4;
alter table doc_legal_provisions add column if not exists bigintparama int4;
alter table doc_legal_provisions add column if not exists bigintparamb int4;
alter table doc_legal_provisions add column if not exists bigintparamc int4;
alter table doc_legal_provisions add column if not exists bigintparamd int4;
alter table doc_legal_provisions add column if not exists bigintparame int4;
alter table doc_legal_provisions add column if not exists bigintparamf int4;
alter table doc_legal_provisions add column if not exists bigintparamg int4;
alter table doc_legal_provisions add column if not exists bigintparamh int4;
alter table doc_legal_provisions add column if not exists bigintparami int4;
alter table doc_legal_provisions add column if not exists bigintparamj int4;
alter table doc_legal_provisions add column if not exists charparama varchar(255);
alter table doc_legal_provisions add column if not exists charparamb varchar(255);
alter table doc_legal_provisions add column if not exists charparamc varchar(255);
alter table doc_legal_provisions add column if not exists charparamd varchar(255);
alter table doc_legal_provisions add column if not exists charparame varchar(255);
alter table doc_legal_provisions add column if not exists charparamf varchar(255);
alter table doc_legal_provisions add column if not exists charparamg varchar(255);
alter table doc_legal_provisions add column if not exists charparamh varchar(255);
alter table doc_legal_provisions add column if not exists charparami varchar(255);
alter table doc_legal_provisions add column if not exists charparamj varchar(255);
alter table doc_legal_provisions add column if not exists clause varchar(256);
alter table doc_legal_provisions add column if not exists code varchar(256);
alter table doc_legal_provisions add column if not exists content varchar(2000);
alter table doc_legal_provisions add column if not exists dateparama timestamp;
alter table doc_legal_provisions add column if not exists dateparamb timestamp;
alter table doc_legal_provisions add column if not exists dateparamc timestamp;
alter table doc_legal_provisions add column if not exists dateparamd timestamp;
alter table doc_legal_provisions add column if not exists dateparame timestamp;
alter table doc_legal_provisions add column if not exists dateparamf timestamp;
alter table doc_legal_provisions add column if not exists dateparamg timestamp;
alter table doc_legal_provisions add column if not exists dateparamh timestamp;
alter table doc_legal_provisions add column if not exists memo_field varchar(256);
alter table doc_legal_provisions add column if not exists numberparama numeric(19, 2);
alter table doc_legal_provisions add column if not exists numberparamb numeric(19, 2);
alter table doc_legal_provisions add column if not exists numberparamc numeric(19, 2);
alter table doc_legal_provisions add column if not exists numberparamd numeric(19, 2);
alter table doc_legal_provisions add column if not exists numberparame numeric(19, 2);
alter table doc_legal_provisions add column if not exists numberparamf numeric(19, 2);
alter table doc_legal_provisions add column if not exists objparama int8;
alter table doc_legal_provisions add column if not exists objparamb int8;
alter table doc_legal_provisions add column if not exists objparamc int8;
alter table doc_legal_provisions add column if not exists objparamd int8;
alter table doc_legal_provisions add column if not exists scparama varchar(255);
alter table doc_legal_provisions add column if not exists scparamb varchar(255);
alter table doc_legal_provisions add column if not exists scparamc varchar(255);
alter table doc_legal_provisions add column if not exists scparamd varchar(255);
alter table doc_legal_provisions add column if not exists the_content text;
alter table doc_legal_provisions add column if not exists law int8;
alter table doc_provision_detais add column if not exists id int8;
alter table doc_provision_detais add column if not exists version int4;
alter table doc_provision_detais add column if not exists create_staff_id int8;
alter table doc_provision_detais add column if not exists create_time timestamp;
alter table doc_provision_detais add column if not exists modify_staff_id int8;
alter table doc_provision_detais add column if not exists modify_time timestamp;
alter table doc_provision_detais add column if not exists valid INTEGER;
alter table doc_provision_detais add column if not exists cid int8;
alter table doc_provision_detais add column if not exists sort int4;
alter table doc_provision_detais add column if not exists bigintparama int4;
alter table doc_provision_detais add column if not exists bigintparamb int4;
alter table doc_provision_detais add column if not exists bigintparamc int4;
alter table doc_provision_detais add column if not exists bigintparamd int4;
alter table doc_provision_detais add column if not exists bigintparame int4;
alter table doc_provision_detais add column if not exists bigintparamf int4;
alter table doc_provision_detais add column if not exists bigintparamg int4;
alter table doc_provision_detais add column if not exists bigintparamh int4;
alter table doc_provision_detais add column if not exists bigintparami int4;
alter table doc_provision_detais add column if not exists bigintparamj int4;
alter table doc_provision_detais add column if not exists charparama varchar(255);
alter table doc_provision_detais add column if not exists charparamb varchar(255);
alter table doc_provision_detais add column if not exists charparamc varchar(255);
alter table doc_provision_detais add column if not exists charparamd varchar(255);
alter table doc_provision_detais add column if not exists charparame varchar(255);
alter table doc_provision_detais add column if not exists charparamf varchar(255);
alter table doc_provision_detais add column if not exists charparamg varchar(255);
alter table doc_provision_detais add column if not exists charparamh varchar(255);
alter table doc_provision_detais add column if not exists charparami varchar(255);
alter table doc_provision_detais add column if not exists charparamj varchar(255);
alter table doc_provision_detais add column if not exists code varchar(255);
alter table doc_provision_detais add column if not exists conclusion varchar(256);
alter table doc_provision_detais add column if not exists dateparama timestamp;
alter table doc_provision_detais add column if not exists dateparamb timestamp;
alter table doc_provision_detais add column if not exists dateparamc timestamp;
alter table doc_provision_detais add column if not exists dateparamd timestamp;
alter table doc_provision_detais add column if not exists dateparame timestamp;
alter table doc_provision_detais add column if not exists dateparamf timestamp;
alter table doc_provision_detais add column if not exists dateparamg timestamp;
alter table doc_provision_detais add column if not exists dateparamh timestamp;
alter table doc_provision_detais add column if not exists description_info varchar(256);
alter table doc_provision_detais add column if not exists measure varchar(256);
alter table doc_provision_detais add column if not exists memo_field text;
alter table doc_provision_detais add column if not exists numberparama numeric(19, 2);
alter table doc_provision_detais add column if not exists numberparamb numeric(19, 2);
alter table doc_provision_detais add column if not exists numberparamc numeric(19, 2);
alter table doc_provision_detais add column if not exists numberparamd numeric(19, 2);
alter table doc_provision_detais add column if not exists numberparame numeric(19, 2);
alter table doc_provision_detais add column if not exists numberparamf numeric(19, 2);
alter table doc_provision_detais add column if not exists objparama int8;
alter table doc_provision_detais add column if not exists objparamb int8;
alter table doc_provision_detais add column if not exists objparamc int8;
alter table doc_provision_detais add column if not exists objparamd int8;
alter table doc_provision_detais add column if not exists scparama varchar(255);
alter table doc_provision_detais add column if not exists scparamb varchar(255);
alter table doc_provision_detais add column if not exists scparamc varchar(255);
alter table doc_provision_detais add column if not exists scparamd varchar(255);
alter table doc_provision_detais add column if not exists table_info_id int8;
alter table doc_provision_detais add column if not exists the_content text;
alter table doc_provision_detais add column if not exists legal_provision int8;
alter table doc_provision_detais add column if not exists provision int8;
alter table doc_provisions add column if not exists id int8;
alter table doc_provisions add column if not exists version int4;
alter table doc_provisions add column if not exists create_staff_id int8;
alter table doc_provisions add column if not exists create_time timestamp;
alter table doc_provisions add column if not exists modify_staff_id int8;
alter table doc_provisions add column if not exists modify_time timestamp;
alter table doc_provisions add column if not exists valid INTEGER;
alter table doc_provisions add column if not exists cid int8;
alter table doc_provisions add column if not exists sort int4;
alter table doc_provisions add column if not exists create_department_id int8;
alter table doc_provisions add column if not exists create_position_id int8;
alter table doc_provisions add column if not exists deployment_id int8;
alter table doc_provisions add column if not exists effect_staff_id int8;
alter table doc_provisions add column if not exists effect_time timestamp;
alter table doc_provisions add column if not exists effective_state int4;
alter table doc_provisions add column if not exists group_id int8;
alter table doc_provisions add column if not exists owner_department_id int8;
alter table doc_provisions add column if not exists owner_position_id int8;
alter table doc_provisions add column if not exists owner_staff_id int8;
alter table doc_provisions add column if not exists position_lay_rec varchar(255);
alter table doc_provisions add column if not exists process_key varchar(255);
alter table doc_provisions add column if not exists process_version int4;
alter table doc_provisions add column if not exists status int4;
alter table doc_provisions add column if not exists table_no varchar(255);
alter table doc_provisions add column if not exists bigintparama int4;
alter table doc_provisions add column if not exists bigintparamb int4;
alter table doc_provisions add column if not exists bigintparamc int4;
alter table doc_provisions add column if not exists bigintparamd int4;
alter table doc_provisions add column if not exists bigintparame int4;
alter table doc_provisions add column if not exists bigintparamf int4;
alter table doc_provisions add column if not exists bigintparamg int4;
alter table doc_provisions add column if not exists bigintparamh int4;
alter table doc_provisions add column if not exists bigintparami int4;
alter table doc_provisions add column if not exists bigintparamj int4;
alter table doc_provisions add column if not exists charparama varchar(255);
alter table doc_provisions add column if not exists charparamb varchar(255);
alter table doc_provisions add column if not exists charparamc varchar(255);
alter table doc_provisions add column if not exists charparamd varchar(255);
alter table doc_provisions add column if not exists charparame varchar(255);
alter table doc_provisions add column if not exists charparamf varchar(255);
alter table doc_provisions add column if not exists charparamg varchar(255);
alter table doc_provisions add column if not exists charparamh varchar(255);
alter table doc_provisions add column if not exists charparami varchar(255);
alter table doc_provisions add column if not exists charparamj varchar(255);
alter table doc_provisions add column if not exists code varchar(255);
alter table doc_provisions add column if not exists dateparama timestamp;
alter table doc_provisions add column if not exists dateparamb timestamp;
alter table doc_provisions add column if not exists dateparamc timestamp;
alter table doc_provisions add column if not exists dateparamd timestamp;
alter table doc_provisions add column if not exists dateparame timestamp;
alter table doc_provisions add column if not exists dateparamf timestamp;
alter table doc_provisions add column if not exists dateparamg timestamp;
alter table doc_provisions add column if not exists dateparamh timestamp;
alter table doc_provisions add column if not exists memo_field varchar(256);
alter table doc_provisions add column if not exists numberparama numeric(19, 2);
alter table doc_provisions add column if not exists numberparamb numeric(19, 2);
alter table doc_provisions add column if not exists numberparamc numeric(19, 2);
alter table doc_provisions add column if not exists numberparamd numeric(19, 2);
alter table doc_provisions add column if not exists numberparame numeric(19, 2);
alter table doc_provisions add column if not exists numberparamf numeric(19, 2);
alter table doc_provisions add column if not exists objparama int8;
alter table doc_provisions add column if not exists objparamb int8;
alter table doc_provisions add column if not exists objparamc int8;
alter table doc_provisions add column if not exists objparamd int8;
alter table doc_provisions add column if not exists scparama varchar(255);
alter table doc_provisions add column if not exists scparamb varchar(255);
alter table doc_provisions add column if not exists scparamc varchar(255);
alter table doc_provisions add column if not exists scparamd varchar(255);
alter table doc_provisions add column if not exists table_info_id int8;
alter table doc_provisions add column if not exists law int8;
alter table doc_provisions_di add column if not exists id int8;
alter table doc_provisions_di add column if not exists version int4;
alter table doc_provisions_di add column if not exists activity_name varchar(255);
alter table doc_provisions_di add column if not exists assign_staff varchar(4000);
alter table doc_provisions_di add column if not exists assign_staff_id varchar(255);
alter table doc_provisions_di add column if not exists cid int8;
alter table doc_provisions_di add column if not exists comments varchar(4000);
alter table doc_provisions_di add column if not exists create_time timestamp;
alter table doc_provisions_di add column if not exists dealinfo_type varchar(255);
alter table doc_provisions_di add column if not exists entity_code varchar(255);
alter table doc_provisions_di add column if not exists instance_id varchar(255);
alter table doc_provisions_di add column if not exists outcome varchar(255);
alter table doc_provisions_di add column if not exists outcome_des varchar(255);
alter table doc_provisions_di add column if not exists outcome_des_zh_cn varchar(255);
alter table doc_provisions_di add column if not exists pending_create_time timestamp;
alter table doc_provisions_di add column if not exists process_key varchar(255);
alter table doc_provisions_di add column if not exists process_version int4;
alter table doc_provisions_di add column if not exists proxy_staff varchar(255);
alter table doc_provisions_di add column if not exists proxy_staff_ids varchar(255);
alter table doc_provisions_di add column if not exists signature varchar(400);
alter table doc_provisions_di add column if not exists task_description varchar(255);
alter table doc_provisions_di add column if not exists task_description_zh_cn varchar(255);
alter table doc_provisions_di add column if not exists user_id int8;
alter table doc_provisions_di add column if not exists recalled_flag boolean;
alter table doc_provisions_di add column if not exists sort int4;
alter table doc_provisions_di add column if not exists table_info_id int8;
alter table doc_provisions_di add column if not exists user_agent varchar(255);
alter table doc_provisions_di add column if not exists main_obj int8;
alter table doc_provisions_di add column if not exists staff int8;
alter table doc_provisions_sv add column if not exists id int8;
alter table doc_provisions_sv add column if not exists version int4;
alter table doc_provisions_sv add column if not exists create_staff_id int8;
alter table doc_provisions_sv add column if not exists create_time timestamp;
alter table doc_provisions_sv add column if not exists delete_staff_id int8;
alter table doc_provisions_sv add column if not exists delete_time timestamp;
alter table doc_provisions_sv add column if not exists modify_staff_id int8;
alter table doc_provisions_sv add column if not exists modify_time timestamp;
alter table doc_provisions_sv add column if not exists table_info_id int8;
alter table doc_provisions_sv add column if not exists valid INTEGER;
alter table doc_provisions_sv add column if not exists main_obj int8;
alter table doc_provisions_sv add column if not exists staff int8;
alter table doc_staff_ranges add column if not exists id int8;
alter table doc_staff_ranges add column if not exists version int4;
alter table doc_staff_ranges add column if not exists create_staff_id int8;
alter table doc_staff_ranges add column if not exists create_time timestamp;
alter table doc_staff_ranges add column if not exists modify_staff_id int8;
alter table doc_staff_ranges add column if not exists modify_time timestamp;
alter table doc_staff_ranges add column if not exists valid INTEGER;
alter table doc_staff_ranges add column if not exists cid int8;
alter table doc_staff_ranges add column if not exists sort int4;
alter table doc_staff_ranges add column if not exists bigintparama int4;
alter table doc_staff_ranges add column if not exists bigintparamb int4;
alter table doc_staff_ranges add column if not exists charparama varchar(255);
alter table doc_staff_ranges add column if not exists charparamb varchar(255);
alter table doc_staff_ranges add column if not exists dateparama timestamp;
alter table doc_staff_ranges add column if not exists dateparamb timestamp;
alter table doc_staff_ranges add column if not exists numberparama numeric(19, 2);
alter table doc_staff_ranges add column if not exists numberparamb numeric(19, 2);
alter table doc_staff_ranges add column if not exists objparama int8;
alter table doc_staff_ranges add column if not exists objparamb int8;
alter table doc_staff_ranges add column if not exists scparama varchar(255);
alter table doc_staff_ranges add column if not exists scparamb varchar(255);
alter table doc_staff_ranges add column if not exists class_security_id int8;
alter table doc_staff_ranges add column if not exists staff_id int8;
alter table doc_user_classes add column if not exists id int8;
alter table doc_user_classes add column if not exists version int4;
alter table doc_user_classes add column if not exists create_staff_id int8;
alter table doc_user_classes add column if not exists create_time timestamp;
alter table doc_user_classes add column if not exists modify_staff_id int8;
alter table doc_user_classes add column if not exists modify_time timestamp;
alter table doc_user_classes add column if not exists valid INTEGER;
alter table doc_user_classes add column if not exists cid int8;
alter table doc_user_classes add column if not exists sort int4;
alter table doc_user_classes add column if not exists bigintparama int4;
alter table doc_user_classes add column if not exists bigintparamb int4;
alter table doc_user_classes add column if not exists charparama varchar(255);
alter table doc_user_classes add column if not exists charparamb varchar(255);
alter table doc_user_classes add column if not exists code varchar(255);
alter table doc_user_classes add column if not exists contain_sub boolean;
alter table doc_user_classes add column if not exists dateparama timestamp;
alter table doc_user_classes add column if not exists dateparamb timestamp;
alter table doc_user_classes add column if not exists memo_field text;
alter table doc_user_classes add column if not exists numberparama numeric(19, 2);
alter table doc_user_classes add column if not exists numberparamb numeric(19, 2);
alter table doc_user_classes add column if not exists objparama int8;
alter table doc_user_classes add column if not exists objparamb int8;
alter table doc_user_classes add column if not exists scparama varchar(255);
alter table doc_user_classes add column if not exists scparamb varchar(255);
alter table doc_user_classes add column if not exists doc_class int8;
alter table doc_user_classes add column if not exists user_power_id int8;
alter table doc_user_powers add column if not exists id int8;
alter table doc_user_powers add column if not exists version int4;
alter table doc_user_powers add column if not exists create_staff_id int8;
alter table doc_user_powers add column if not exists create_time timestamp;
alter table doc_user_powers add column if not exists modify_staff_id int8;
alter table doc_user_powers add column if not exists modify_time timestamp;
alter table doc_user_powers add column if not exists valid boolean;
alter table doc_user_powers add column if not exists cid int8;
alter table doc_user_powers add column if not exists sort int4;
alter table doc_user_powers add column if not exists create_department_id int8;
alter table doc_user_powers add column if not exists create_position_id int8;
alter table doc_user_powers add column if not exists deployment_id int8;
alter table doc_user_powers add column if not exists effect_staff_id int8;
alter table doc_user_powers add column if not exists effect_time timestamp;
alter table doc_user_powers add column if not exists effective_state int4;
alter table doc_user_powers add column if not exists group_id int8;
alter table doc_user_powers add column if not exists owner_department_id int8;
alter table doc_user_powers add column if not exists owner_position_id int8;
alter table doc_user_powers add column if not exists owner_staff_id int8;
alter table doc_user_powers add column if not exists position_lay_rec varchar(255);
alter table doc_user_powers add column if not exists process_key varchar(255);
alter table doc_user_powers add column if not exists process_version int4;
alter table doc_user_powers add column if not exists status int4;
alter table doc_user_powers add column if not exists table_no varchar(255);
alter table doc_user_powers add column if not exists bigintparama int4;
alter table doc_user_powers add column if not exists bigintparamb int4;
alter table doc_user_powers add column if not exists charparama varchar(255);
alter table doc_user_powers add column if not exists charparamb varchar(255);
alter table doc_user_powers add column if not exists code varchar(255);
alter table doc_user_powers add column if not exists dateparama timestamp;
alter table doc_user_powers add column if not exists dateparamb timestamp;
alter table doc_user_powers add column if not exists is_all_docs boolean;
alter table doc_user_powers add column if not exists memo_field text;
alter table doc_user_powers add column if not exists numberparama numeric(19, 2);
alter table doc_user_powers add column if not exists numberparamb numeric(19, 2);
alter table doc_user_powers add column if not exists objparama int8;
alter table doc_user_powers add column if not exists objparamb int8;
alter table doc_user_powers add column if not exists scparama varchar(255);
alter table doc_user_powers add column if not exists scparamb varchar(255);
alter table doc_user_powers add column if not exists staff_id int8;
alter table doc_user_powers_di add column if not exists id int8;
alter table doc_user_powers_di add column if not exists version int4;
alter table doc_user_powers_di add column if not exists activity_name varchar(255);
alter table doc_user_powers_di add column if not exists assign_staff varchar(4000);
alter table doc_user_powers_di add column if not exists assign_staff_id varchar(255);
alter table doc_user_powers_di add column if not exists cid int8;
alter table doc_user_powers_di add column if not exists comments varchar(4000);
alter table doc_user_powers_di add column if not exists create_time timestamp;
alter table doc_user_powers_di add column if not exists dealinfo_type varchar(255);
alter table doc_user_powers_di add column if not exists entity_code varchar(255);
alter table doc_user_powers_di add column if not exists instance_id varchar(255);
alter table doc_user_powers_di add column if not exists outcome varchar(255);
alter table doc_user_powers_di add column if not exists outcome_des varchar(255);
alter table doc_user_powers_di add column if not exists outcome_des_zh_cn varchar(255);
alter table doc_user_powers_di add column if not exists pending_create_time timestamp;
alter table doc_user_powers_di add column if not exists process_key varchar(255);
alter table doc_user_powers_di add column if not exists process_version int4;
alter table doc_user_powers_di add column if not exists proxy_staff varchar(255);
alter table doc_user_powers_di add column if not exists proxy_staff_ids varchar(255);
alter table doc_user_powers_di add column if not exists signature varchar(400);
alter table doc_user_powers_di add column if not exists task_description varchar(255);
alter table doc_user_powers_di add column if not exists task_description_zh_cn varchar(255);
alter table doc_user_powers_di add column if not exists user_id int8;
alter table doc_user_powers_di add column if not exists recalled_flag boolean;
alter table doc_user_powers_di add column if not exists sort int4;
alter table doc_user_powers_di add column if not exists table_info_id int8;
alter table doc_user_powers_di add column if not exists user_agent varchar(255);
alter table doc_user_powers_di add column if not exists main_obj int8;
alter table doc_user_powers_di add column if not exists staff int8;
alter table ds_category_mgts add column if not exists id int8;
alter table ds_category_mgts add column if not exists version int4;
alter table ds_category_mgts add column if not exists create_staff_id int8;
alter table ds_category_mgts add column if not exists create_time timestamp;
alter table ds_category_mgts add column if not exists modify_staff_id int8;
alter table ds_category_mgts add column if not exists modify_time timestamp;
alter table ds_category_mgts add column if not exists valid boolean;
alter table ds_category_mgts add column if not exists cid int8;
alter table ds_category_mgts add column if not exists full_path_name varchar(255);
alter table ds_category_mgts add column if not exists lay_no int4;
alter table ds_category_mgts add column if not exists lay_rec varchar(255);
alter table ds_category_mgts add column if not exists leaf INTEGER;
alter table ds_category_mgts add column if not exists parent_id int8;
alter table ds_category_mgts add column if not exists sort int8;
alter table ds_category_mgts add column if not exists create_department_id int8;
alter table ds_category_mgts add column if not exists create_position_id int8;
alter table ds_category_mgts add column if not exists deployment_id int8;
alter table ds_category_mgts add column if not exists effect_staff_id int8;
alter table ds_category_mgts add column if not exists effect_time timestamp;
alter table ds_category_mgts add column if not exists effective_state int4;
alter table ds_category_mgts add column if not exists oa text;
alter table ds_category_mgts add column if not exists owner_department_id int8;
alter table ds_category_mgts add column if not exists owner_position_id int8;
alter table ds_category_mgts add column if not exists owner_staff_id int8;
alter table ds_category_mgts add column if not exists position_lay_rec varchar(255);
alter table ds_category_mgts add column if not exists process_key varchar(255);
alter table ds_category_mgts add column if not exists process_version int4;
alter table ds_category_mgts add column if not exists status int4;
alter table ds_category_mgts add column if not exists table_no varchar(255);
alter table ds_category_mgts add column if not exists category_code varchar(30);
alter table ds_category_mgts add column if not exists category_name varchar(30);
alter table ds_category_mgts add column if not exists category_remark varchar(2000);
alter table ds_category_mgts_di add column if not exists id int8;
alter table ds_category_mgts_di add column if not exists version int4;
alter table ds_category_mgts_di add column if not exists activity_name varchar(255);
alter table ds_category_mgts_di add column if not exists assign_staff varchar(4000);
alter table ds_category_mgts_di add column if not exists assign_staff_id varchar(255);
alter table ds_category_mgts_di add column if not exists cid int8;
alter table ds_category_mgts_di add column if not exists comments varchar(4000);
alter table ds_category_mgts_di add column if not exists create_time timestamp;
alter table ds_category_mgts_di add column if not exists dealinfo_type varchar(255);
alter table ds_category_mgts_di add column if not exists entity_code varchar(255);
alter table ds_category_mgts_di add column if not exists instance_id varchar(255);
alter table ds_category_mgts_di add column if not exists outcome varchar(255);
alter table ds_category_mgts_di add column if not exists outcome_des varchar(255);
alter table ds_category_mgts_di add column if not exists outcome_des_zh_cn varchar(255);
alter table ds_category_mgts_di add column if not exists pending_create_time timestamp;
alter table ds_category_mgts_di add column if not exists process_key varchar(255);
alter table ds_category_mgts_di add column if not exists process_version int4;
alter table ds_category_mgts_di add column if not exists proxy_staff varchar(255);
alter table ds_category_mgts_di add column if not exists proxy_staff_ids varchar(255);
alter table ds_category_mgts_di add column if not exists signature varchar(400);
alter table ds_category_mgts_di add column if not exists task_description varchar(255);
alter table ds_category_mgts_di add column if not exists task_description_zh_cn varchar(255);
alter table ds_category_mgts_di add column if not exists user_id int8;
alter table ds_category_mgts_di add column if not exists recalled_flag boolean;
alter table ds_category_mgts_di add column if not exists sort int4;
alter table ds_category_mgts_di add column if not exists table_info_id int8;
alter table ds_category_mgts_di add column if not exists user_agent varchar(255);
alter table ds_category_mgts_di add column if not exists main_obj int8;
alter table ds_category_mgts_di add column if not exists staff int8;
alter table ds_connection_mgts add column if not exists id int8;
alter table ds_connection_mgts add column if not exists version int4;
alter table ds_connection_mgts add column if not exists create_staff_id int8;
alter table ds_connection_mgts add column if not exists create_time timestamp;
alter table ds_connection_mgts add column if not exists modify_staff_id int8;
alter table ds_connection_mgts add column if not exists modify_time timestamp;
alter table ds_connection_mgts add column if not exists valid boolean;
alter table ds_connection_mgts add column if not exists cid int8;
alter table ds_connection_mgts add column if not exists sort int4;
alter table ds_connection_mgts add column if not exists create_department_id int8;
alter table ds_connection_mgts add column if not exists create_position_id int8;
alter table ds_connection_mgts add column if not exists deployment_id int8;
alter table ds_connection_mgts add column if not exists effect_staff_id int8;
alter table ds_connection_mgts add column if not exists effect_time timestamp;
alter table ds_connection_mgts add column if not exists effective_state int4;
alter table ds_connection_mgts add column if not exists group_id int8;
alter table ds_connection_mgts add column if not exists owner_department_id int8;
alter table ds_connection_mgts add column if not exists owner_position_id int8;
alter table ds_connection_mgts add column if not exists owner_staff_id int8;
alter table ds_connection_mgts add column if not exists position_lay_rec varchar(255);
alter table ds_connection_mgts add column if not exists process_key varchar(255);
alter table ds_connection_mgts add column if not exists process_version int4;
alter table ds_connection_mgts add column if not exists status int4;
alter table ds_connection_mgts add column if not exists table_no varchar(255);
alter table ds_connection_mgts add column if not exists conn_name varchar(30);
alter table ds_connection_mgts add column if not exists conn_remark varchar(2000);
alter table ds_connection_mgts add column if not exists connection_str text;
alter table ds_connection_mgts add column if not exists database_account varchar(20);
alter table ds_connection_mgts add column if not exists database_name varchar(30);
alter table ds_connection_mgts add column if not exists database_password varchar(20);
alter table ds_connection_mgts add column if not exists server_addr varchar(30);
alter table ds_connection_mgts add column if not exists server_port int4;
alter table ds_connection_mgts add column if not exists data_source_type varchar(255);
alter table ds_connection_mgts add column if not exists database_type varchar(255);
alter table ds_connection_mgts_di add column if not exists id int8;
alter table ds_connection_mgts_di add column if not exists version int4;
alter table ds_connection_mgts_di add column if not exists activity_name varchar(255);
alter table ds_connection_mgts_di add column if not exists assign_staff varchar(4000);
alter table ds_connection_mgts_di add column if not exists assign_staff_id varchar(255);
alter table ds_connection_mgts_di add column if not exists cid int8;
alter table ds_connection_mgts_di add column if not exists comments varchar(4000);
alter table ds_connection_mgts_di add column if not exists create_time timestamp;
alter table ds_connection_mgts_di add column if not exists dealinfo_type varchar(255);
alter table ds_connection_mgts_di add column if not exists entity_code varchar(255);
alter table ds_connection_mgts_di add column if not exists instance_id varchar(255);
alter table ds_connection_mgts_di add column if not exists outcome varchar(255);
alter table ds_connection_mgts_di add column if not exists outcome_des varchar(255);
alter table ds_connection_mgts_di add column if not exists outcome_des_zh_cn varchar(255);
alter table ds_connection_mgts_di add column if not exists pending_create_time timestamp;
alter table ds_connection_mgts_di add column if not exists process_key varchar(255);
alter table ds_connection_mgts_di add column if not exists process_version int4;
alter table ds_connection_mgts_di add column if not exists proxy_staff varchar(255);
alter table ds_connection_mgts_di add column if not exists proxy_staff_ids varchar(255);
alter table ds_connection_mgts_di add column if not exists signature varchar(400);
alter table ds_connection_mgts_di add column if not exists task_description varchar(255);
alter table ds_connection_mgts_di add column if not exists task_description_zh_cn varchar(255);
alter table ds_connection_mgts_di add column if not exists user_id int8;
alter table ds_connection_mgts_di add column if not exists recalled_flag boolean;
alter table ds_connection_mgts_di add column if not exists sort int4;
alter table ds_connection_mgts_di add column if not exists table_info_id int8;
alter table ds_connection_mgts_di add column if not exists user_agent varchar(255);
alter table ds_connection_mgts_di add column if not exists main_obj int8;
alter table ds_connection_mgts_di add column if not exists staff int8;
alter table ds_definition_mgts add column if not exists id int8;
alter table ds_definition_mgts add column if not exists version int4;
alter table ds_definition_mgts add column if not exists create_staff_id int8;
alter table ds_definition_mgts add column if not exists create_time timestamp;
alter table ds_definition_mgts add column if not exists modify_staff_id int8;
alter table ds_definition_mgts add column if not exists modify_time timestamp;
alter table ds_definition_mgts add column if not exists valid boolean;
alter table ds_definition_mgts add column if not exists cid int8;
alter table ds_definition_mgts add column if not exists sort int4;
alter table ds_definition_mgts add column if not exists create_department_id int8;
alter table ds_definition_mgts add column if not exists create_position_id int8;
alter table ds_definition_mgts add column if not exists deployment_id int8;
alter table ds_definition_mgts add column if not exists effect_staff_id int8;
alter table ds_definition_mgts add column if not exists effect_time timestamp;
alter table ds_definition_mgts add column if not exists effective_state int4;
alter table ds_definition_mgts add column if not exists group_id int8;
alter table ds_definition_mgts add column if not exists owner_department_id int8;
alter table ds_definition_mgts add column if not exists owner_position_id int8;
alter table ds_definition_mgts add column if not exists owner_staff_id int8;
alter table ds_definition_mgts add column if not exists position_lay_rec varchar(255);
alter table ds_definition_mgts add column if not exists process_key varchar(255);
alter table ds_definition_mgts add column if not exists process_version int4;
alter table ds_definition_mgts add column if not exists status int4;
alter table ds_definition_mgts add column if not exists table_no varchar(255);
alter table ds_definition_mgts add column if not exists data_set_alias varchar(30);
alter table ds_definition_mgts add column if not exists data_set_name varchar(30);
alter table ds_definition_mgts add column if not exists data_set_remark varchar(2000);
alter table ds_definition_mgts add column if not exists sql_statement text;
alter table ds_definition_mgts add column if not exists category_id int8;
alter table ds_definition_mgts add column if not exists conn_id int8;
alter table ds_definition_mgts add column if not exists sql_type varchar(255);
alter table ds_definition_mgts_di add column if not exists id int8;
alter table ds_definition_mgts_di add column if not exists version int4;
alter table ds_definition_mgts_di add column if not exists activity_name varchar(255);
alter table ds_definition_mgts_di add column if not exists assign_staff varchar(4000);
alter table ds_definition_mgts_di add column if not exists assign_staff_id varchar(255);
alter table ds_definition_mgts_di add column if not exists cid int8;
alter table ds_definition_mgts_di add column if not exists comments varchar(4000);
alter table ds_definition_mgts_di add column if not exists create_time timestamp;
alter table ds_definition_mgts_di add column if not exists dealinfo_type varchar(255);
alter table ds_definition_mgts_di add column if not exists entity_code varchar(255);
alter table ds_definition_mgts_di add column if not exists instance_id varchar(255);
alter table ds_definition_mgts_di add column if not exists outcome varchar(255);
alter table ds_definition_mgts_di add column if not exists outcome_des varchar(255);
alter table ds_definition_mgts_di add column if not exists outcome_des_zh_cn varchar(255);
alter table ds_definition_mgts_di add column if not exists pending_create_time timestamp;
alter table ds_definition_mgts_di add column if not exists process_key varchar(255);
alter table ds_definition_mgts_di add column if not exists process_version int4;
alter table ds_definition_mgts_di add column if not exists proxy_staff varchar(255);
alter table ds_definition_mgts_di add column if not exists proxy_staff_ids varchar(255);
alter table ds_definition_mgts_di add column if not exists signature varchar(400);
alter table ds_definition_mgts_di add column if not exists task_description varchar(255);
alter table ds_definition_mgts_di add column if not exists task_description_zh_cn varchar(255);
alter table ds_definition_mgts_di add column if not exists user_id int8;
alter table ds_definition_mgts_di add column if not exists recalled_flag boolean;
alter table ds_definition_mgts_di add column if not exists sort int4;
alter table ds_definition_mgts_di add column if not exists table_info_id int8;
alter table ds_definition_mgts_di add column if not exists user_agent varchar(255);
alter table ds_definition_mgts_di add column if not exists main_obj int8;
alter table ds_definition_mgts_di add column if not exists staff int8;
alter table ds_field_mgts add column if not exists id int8;
alter table ds_field_mgts add column if not exists version int4;
alter table ds_field_mgts add column if not exists create_staff_id int8;
alter table ds_field_mgts add column if not exists create_time timestamp;
alter table ds_field_mgts add column if not exists modify_staff_id int8;
alter table ds_field_mgts add column if not exists modify_time timestamp;
alter table ds_field_mgts add column if not exists valid INTEGER;
alter table ds_field_mgts add column if not exists cid int8;
alter table ds_field_mgts add column if not exists sort int4;
alter table ds_field_mgts add column if not exists analysis boolean;
alter table ds_field_mgts add column if not exists field_alias varchar(100);
alter table ds_field_mgts add column if not exists field_name varchar(30);
alter table ds_field_mgts add column if not exists field_type varchar(30);
alter table ds_field_mgts add column if not exists data_set_alias int8;
alter table ds_param_mgts add column if not exists id int8;
alter table ds_param_mgts add column if not exists version int4;
alter table ds_param_mgts add column if not exists create_staff_id int8;
alter table ds_param_mgts add column if not exists create_time timestamp;
alter table ds_param_mgts add column if not exists modify_staff_id int8;
alter table ds_param_mgts add column if not exists modify_time timestamp;
alter table ds_param_mgts add column if not exists valid INTEGER;
alter table ds_param_mgts add column if not exists cid int8;
alter table ds_param_mgts add column if not exists sort int4;
alter table ds_param_mgts add column if not exists param_name varchar(30);
alter table ds_param_mgts add column if not exists param_remark varchar(30);
alter table ds_param_mgts add column if not exists param_value varchar(500);
alter table ds_param_mgts add column if not exists data_set_alias int8;
alter table ds_param_mgts add column if not exists param_type varchar(255);
alter table ds_preview_mgts add column if not exists id int8;
alter table ds_preview_mgts add column if not exists version int4;
alter table ds_preview_mgts add column if not exists create_staff_id int8;
alter table ds_preview_mgts add column if not exists create_time timestamp;
alter table ds_preview_mgts add column if not exists modify_staff_id int8;
alter table ds_preview_mgts add column if not exists modify_time timestamp;
alter table ds_preview_mgts add column if not exists valid INTEGER;
alter table ds_preview_mgts add column if not exists cid int8;
alter table ds_preview_mgts add column if not exists sort int4;
alter table ds_preview_mgts add column if not exists data_set_alias int8;

create index if not exists idx_chartcustom_ditableid on crm_chart_customs_di (table_info_id);
create index if not exists idx_chartreport_ditableid on crm_chart_reports_di (table_info_id);
create index if not exists idx_mychart_ditableid on crm_my_charts_di (table_info_id);
create index if not exists idx_scattechartset_ditableid on crm_scatte_chart_sets_di (table_info_id);
create index if not exists idx_trendchartset_ditableid on crm_trend_chart_sets_di (table_info_id);
create index if not exists index_classsecurity_code on doc_class_securities (code);
create index if not exists index_classification_code on doc_classifications (code);
create index if not exists idx_docborrow_table_id on doc_doc_borrows (table_info_id);
create index if not exists idx_docborrow_ditableid on doc_doc_borrows_di (table_info_id);
create index if not exists idx_docborrow_sutableid on doc_doc_borrows_pa (table_info_id);
create index if not exists idx_docborrow_sutableid on doc_doc_borrows_sv (table_info_id);
create index if not exists index_docclass_code on doc_doc_classes (code);
create index if not exists idx_docclass_ditableid on doc_doc_classes_di (table_info_id);
create index if not exists idx_docdocument_table_id on doc_doc_documents (table_info_id);
create index if not exists idx_docdocument_ditableid on doc_doc_documents_di (table_info_id);
create index if not exists idx_docdocument_sutableid on doc_doc_documents_pa (table_info_id);
create index if not exists idx_docdocument_sutableid on doc_doc_documents_sv (table_info_id);
create index if not exists idx_dochistory_table_id on doc_doc_histories (table_info_id);
create index if not exists idx_docobject_table_id on doc_doc_objects (table_info_id);
create index if not exists idx_docreminder_table_id on doc_doc_reminders (table_info_id);
create index if not exists idx_docudept_table_id on doc_docu_depts (table_info_id);
create index if not exists idx_enclosurehead_ditableid on doc_enclosure_heads_di (table_info_id);
create index if not exists index_law_code on doc_laws (code);
create index if not exists idx_law_ditableid on doc_laws_di (table_info_id);
create index if not exists index_provisiondetai_code on doc_provision_detais (code);
create index if not exists idx_provisiondetai_table_id on doc_provision_detais (table_info_id);
create index if not exists index_provision_code on doc_provisions (code);
create index if not exists idx_provision_table_id on doc_provisions (table_info_id);
create index if not exists idx_provision_ditableid on doc_provisions_di (table_info_id);
create index if not exists idx_provision_sutableid on doc_provisions_sv (table_info_id);
create index if not exists index_userclass_code on doc_user_classes (code);
create index if not exists index_userpower_code on doc_user_powers (code);
create index if not exists idx_userpower_ditableid on doc_user_powers_di (table_info_id);
create index if not exists index_categorymgt_categorycode on ds_category_mgts (category_code);
create index if not exists idx_categorymgt_ditableid on ds_category_mgts_di (table_info_id);
create index if not exists index_connectionmgt_connname on ds_connection_mgts (conn_name);
create index if not exists idx_connectionmgt_ditableid on ds_connection_mgts_di (table_info_id);
create index if not exists index_definitionmgt_datasetalias on ds_definition_mgts (data_set_alias);
create index if not exists idx_definitionmgt_ditableid on ds_definition_mgts_di (table_info_id);
