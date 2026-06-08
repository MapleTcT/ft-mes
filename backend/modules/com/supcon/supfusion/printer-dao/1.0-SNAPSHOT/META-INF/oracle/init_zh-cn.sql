## 1.0.0
create table printer_log (
    id number(20,0) NOT NULL,
    template_id number(20,0) NOT NULL,
    page_id varchar2(128) NOT NULL,
    app_id varchar2(128) NOT NULL,
    creator varchar2(32) DEFAULT NULL,
    modifier varchar2(32) DEFAULT NULL,
    create_time TIMESTAMP,
    modify_time TIMESTAMP,
    create_staff_id number(20,0) default null, --comment '创建者人员id',
    modify_staff_id number(20,0) default null, --comment '修改者人员id',
    PRIMARY KEY (id)
);
create table printer_object_iframe (
    id number(20,0) NOT NULL,
    name varchar2(128) NOT NULL,
    source number(4,0) NOT NULL,
    url varchar2(1024) NOT NULL,
    valid number(1,0) DEFAULT 1,
    creator varchar2(32) DEFAULT NULL,
    modifier varchar2(32) DEFAULT NULL,
    create_time TIMESTAMP,
    modify_time TIMESTAMP,
    create_staff_id number(20,0) default null, --comment '创建者人员id',
    modify_staff_id number(20,0) default null, --comment '修改者人员id',
    PRIMARY KEY (id)
);
create table printer_design_content (
    template_id number(20,0) NOT NULL,
    valid number(1,0) DEFAULT 1,
    content CLOB DEFAULT NULL,
    creator varchar2(32) DEFAULT NULL,
    modifier varchar2(32) DEFAULT NULL,
    create_time TIMESTAMP,
    modify_time TIMESTAMP,
    create_staff_id number(20,0) default null,
    modify_staff_id number(20,0) default null
);
tablespace INSIGHTDATA3_TS
  pctfree 10
  initrans 1
  maxtrans 255
  storage
  (
    initial 160K
    next 1M
    minextents 1
    maxextents unlimited
  );
create table printer_label (
    id number(20,0) NOT NULL,
    label_name varchar2(128) NOT NULL,
    creator varchar2(32) DEFAULT NULL,
    modifier varchar2(32) DEFAULT NULL,
    create_time TIMESTAMP,
    modify_time TIMESTAMP,
    create_staff_id number(20,0) default null,
    modify_staff_id number(20,0) default null
    PRIMARY KEY (id)
);
create table printer_register (
    id number(20,0) NOT NULL,
    source number(4,0) NOT NULL,
    service_url varchar2(1024) NOT NULL,
    service_type number(4,0) NOT NULL,
    call_type number(4,0) NOT NULL,
    creator varchar2(32) DEFAULT NULL,
    modifier varchar2(32) DEFAULT NULL,
    create_time TIMESTAMP,
    modify_time TIMESTAMP,
    create_staff_id number(20,0) DEFAULT NULL,
    modify_staff_id number(20,0) DEFAULT NULL,
    PRIMARY KEY (id)
);
create table printer_template (
    id number(20,0) NOT NULL,
    template_name varchar2(128) NOT NULL,
    template_code varchar2(128) NOT NULL,
    i18n_key varchar2(128),
    app_id varchar2(128) NOT NULL,
    label_names varchar2(128),
    template_desc varchar2(512),
    enabled number(1,0) DEFAULT 1,
    valid number(1,0) DEFAULT 1,
    creator varchar2(32) DEFAULT NULL,
    modifier varchar2(32) DEFAULT NULL,
    create_time TIMESTAMP,
    modify_time TIMESTAMP,
    create_staff_id number(20,0) DEFAULT NULL,
    modify_staff_id number(20,0) DEFAULT NULL,
    PRIMARY KEY (id)
);
create table printer_template_relation_page (
    id number(20,0) NOT NULL,
    template_id number(20,0) NOT NULL,
    page_id varchar2(128),
    model_code varchar2(128),
    creator varchar2(32) DEFAULT NULL,
    modifier varchar2(32) DEFAULT NULL,
    create_time TIMESTAMP,
    modify_time TIMESTAMP,
    create_staff_id number(20,0) DEFAULT NULL,
    modify_staff_id number(20,0) DEFAULT NULL,
    PRIMARY KEY (id)
);
CREATE TABLE PRINTER_LABEL (
    "ID" NUMBER(20,0) NOT NULL,
	"LABEL_NAME" VARCHAR2(128) NOT NULL,
	"CREATOR" VARCHAR2(128),
	"MODIFIER" VARCHAR2(128),
	"CREATE_TIME" TIMESTAMP (6),
	"MODIFY_TIME" TIMESTAMP (6),
	"CREATE_STAFF_ID" NUMBER(20,0),
	"MODIFY_STAFF_ID" NUMBER(20,0),
	 PRIMARY KEY ("ID")
);