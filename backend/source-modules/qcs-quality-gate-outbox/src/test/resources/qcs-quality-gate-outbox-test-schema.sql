CREATE TABLE public.baseset_materials (
    id BIGINT PRIMARY KEY,
    code VARCHAR(200)
);

CREATE TABLE public.rm_formulas (
    id BIGINT PRIMARY KEY,
    formual_code VARCHAR(255),
    formula_edtion VARCHAR(255)
);

CREATE TABLE public.wom_produce_tasks (
    id BIGINT PRIMARY KEY,
    valid BOOLEAN,
    cid BIGINT,
    line_id BIGINT,
    table_no VARCHAR(255),
    produce_batch_num VARCHAR(255),
    product_id BIGINT,
    formula_id BIGINT,
    task_run_state VARCHAR(255),
    act_start_time TIMESTAMP,
    act_end_time TIMESTAMP,
    modify_time TIMESTAMP
);

CREATE TABLE public.qcs_inspects (
    id BIGINT PRIMARY KEY,
    valid BOOLEAN,
    source_id BIGINT,
    source_type VARCHAR(255),
    batch_code VARCHAR(255)
);

CREATE TABLE public.qcs_inspect_reports (
    id BIGINT PRIMARY KEY,
    version INTEGER,
    create_time TIMESTAMP,
    modify_time TIMESTAMP,
    effect_time TIMESTAMP,
    valid BOOLEAN,
    status INTEGER,
    inspect_id BIGINT,
    check_result VARCHAR(255)
);

CREATE TABLE public.qcs_report_coms (
    id BIGINT PRIMARY KEY,
    valid BOOLEAN,
    report_id BIGINT,
    procedure_no VARCHAR(255),
    std_ver_com BIGINT,
    check_result VARCHAR(255)
);
