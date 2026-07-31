-- Restore the WTS qualification-person part table referenced by ticket edit grids.
CREATE TABLE IF NOT EXISTS public.wts_certificates (
    id bigint PRIMARY KEY,
    version integer,
    create_staff_id bigint,
    create_time timestamp without time zone,
    modify_staff_id bigint,
    modify_time timestamp without time zone,
    delete_staff_id bigint,
    delete_time timestamp without time zone,
    valid boolean,
    cid bigint,
    sort integer,
    create_department_id bigint,
    create_position_id bigint,
    deployment_id bigint,
    effect_staff_id bigint,
    effect_time timestamp without time zone,
    effective_state integer,
    group_id bigint,
    owner_department_id bigint,
    owner_position_id bigint,
    owner_staff_id bigint,
    position_lay_rec character varying(2000),
    process_key character varying(255),
    process_version integer,
    status integer,
    table_no character varying(255),
    table_info_id bigint,
    extra_col text,
    parent_id bigint,
    lay_rec character varying(2000),
    staff bigint,
    staff_certificate bigint,
    work_ticket bigint
);

CREATE INDEX IF NOT EXISTS idx_wts_certificate_table_id
    ON public.wts_certificates (table_info_id);
CREATE INDEX IF NOT EXISTS idx_wts_certificate_work_ticket
    ON public.wts_certificates (work_ticket);
CREATE INDEX IF NOT EXISTS idx_wts_certificate_staff
    ON public.wts_certificates (staff);

COMMENT ON TABLE public.wts_certificates IS
    'WTS ticket qualification-person rows recovered from packaged model metadata';
