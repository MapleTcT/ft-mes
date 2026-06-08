## 1.0.0
create table printer_log (
    id BIGINT NOT NULL,
    template_id BIGINT NOT NULL,
    page_id VARCHAR(128) NOT NULL,
    app_id VARCHAR(128) NOT NULL,
    creator VARCHAR(32) DEFAULT NULL,
    modifier VARCHAR(32) DEFAULT NULL,
    create_time datetime NULL,
    modify_time datetime NULL,
    create_staff_id bigint default null,
    modify_staff_id bigint default null,
    PRIMARY KEY (id)
);
create table printer_object_iframe (
    id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    source TINYINT NOT NULL,
    url VARCHAR(1024) NOT NULL,
    valid TINYINT DEFAULT 1,
    creator VARCHAR(32) DEFAULT NULL,
    modifier VARCHAR(32) DEFAULT NULL,
    create_time datetime,
    modify_time datetime,
    create_staff_id bigint default null,
    modify_staff_id bigint default null,
    PRIMARY KEY (id)
);
create table printer_design_content(
    template_id BIGINT NOT NULL,
    valid TINYINT DEFAULT 1,
    content TEXT NULL,
    creator VARCHAR(32) DEFAULT NULL,
    modifier VARCHAR(32) DEFAULT NULL,
    create_time datetime,
    modify_time datetime,
    create_staff_id bigint default null,
    modify_staff_id bigint default null
);
create table printer_label (
    id BIGINT NOT NULL,
    label_name VARCHAR(128) NOT NULL,
    creator VARCHAR(32) DEFAULT NULL,
    modifier VARCHAR(32) DEFAULT NULL,
    create_time datetime,
    modify_time datetime,
    create_staff_id bigint default null,
    modify_staff_id bigint default null,
    PRIMARY KEY (id)
);
create table printer_register (
    id BIGINT NOT NULL,
    source TINYINT NOT NULL,
    service_url VARCHAR(1024) NOT NULL,
    service_type TINYINT NOT NULL,
    call_type TINYINT NOT NULL,
    creator VARCHAR(32) DEFAULT NULL,
    modifier VARCHAR(32) DEFAULT NULL,
    create_time datetime,
    modify_time datetime,
    create_staff_id bigint default null,
    modify_staff_id bigint default null,
    PRIMARY KEY (id)
);
create table printer_template (
    id BIGINT NOT NULL,
    template_name VARCHAR(128) NOT NULL,
    template_code VARCHAR(128) NOT NULL,
    i18n_key VARCHAR(128),
    app_id VARCHAR(128) NOT NULL,
    label_names VARCHAR(128),
    template_desc VARCHAR(512) DEFAULT NULL,
    enabled TINYINT DEFAULT 1,
    valid TINYINT DEFAULT 1,
    creator VARCHAR(32) DEFAULT NULL,
    modifier VARCHAR(32) DEFAULT NULL,
    create_time datetime,
    modify_time datetime,
    create_staff_id bigint default null,
    modify_staff_id bigint default null,
    PRIMARY KEY (id)
);
create table printer_template_relation_page (
    id BIGINT NOT NULL,
    template_id BIGINT NOT NULL,
    page_id VARCHAR(128) NOT NULL,
    model_code VARCHAR(128) NOT NULL,
    creator VARCHAR(32) DEFAULT NULL,
    modifier VARCHAR(32) DEFAULT NULL,
    create_time datetime,
    modify_time datetime,
    create_staff_id bigint default null,
    modify_staff_id bigint default null,
    PRIMARY KEY (id)
)
