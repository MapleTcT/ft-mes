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
