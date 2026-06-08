## 1.0.0
/*===================================================================*/
/* Table: 多公司模式组织架构公司表(org_company)                */
/*===================================================================*/
create table org_company (
    id number(20,0) NOT NULL,
    row_version number(20,0) DEFAULT 0,
    code varchar2(256) NOT NULL,
    description varchar2(2048) DEFAULT NULL,
    short_name varchar2(2048) NOT NULL,
    full_name varchar2(2048) NOT NULL,
    full_path varchar2(4000) NOT NULL,
    address varchar2(512) DEFAULT NULL,
    lay_no number(10,0) NOT NULL,
    lay_rec varchar2(4000) NOT NULL,
    sort number(20,6) NOT NULL,
    parent_id number(20,0) DEFAULT NULL,
    valid number(1,0) DEFAULT 1,
    old_id varchar2(256) NOT NULL,
    creator varchar2(32) DEFAULT NULL,
    modifier varchar2(32) DEFAULT NULL,
    create_time TIMESTAMP,
    modify_time TIMESTAMP,
    create_staff_id number(20,0) default null, --comment '创建者人员id',
    modify_staff_id number(20,0) default null, --comment '修改者人员id',
    PRIMARY KEY (id),
    UNIQUE (old_id)
);
MERGE INTO org_company A
    USING (SELECT 1000 AS id, 'default_org_company' AS code, '默认公司' AS short_name, '默认公司' AS full_name, 0 AS lay_no, '/默认公司' AS full_path, 0 AS sort, 'Company_default_org_company' AS old_id, '1000' AS lay_rec FROM dual) B
    ON (A.CODE = B.CODE)
    WHEN NOT MATCHED THEN INSERT(A.id, A.code, A.short_name, A.full_name, A.lay_no, A.full_path, A.sort, A.old_id, A.lay_rec)
    VALUES (B.id, B.code, B.short_name, B.full_name, B.lay_no, B.full_path, B.sort, B.old_id, B.lay_rec);
/*===================================================================*/
/* Table: 多公司模式组织架构部门表(org_department)                    */
/*===================================================================*/
create table org_department (
    id number(20,0) NOT NULL,
    row_version number(20,0) DEFAULT 0,
    code varchar2(256) NOT NULL,
    name varchar2(2048) NOT NULL,
    dept_type varchar2(64) DEFAULT 'general',
    description varchar2(2048) DEFAULT NULL,
    full_path varchar2(4000) NOT NULL,
    lay_no number(10,0) NOT NULL,
    lay_rec varchar2(4000) NOT NULL,
    sort number(20,6) NOT NULL,
    company_id number(20,0) NOT NULL,
    parent_id number(20,0) DEFAULT NULL,
    valid number(1,0) DEFAULT 1,
    sys_flag number(1,0) DEFAULT 0,
    old_id varchar2(256) NOT NULL,
    leaf number(1,0) DEFAULT 1,
    creator varchar2(32) DEFAULT NULL,
    modifier varchar2(32) DEFAULT NULL,
    create_staff_id number(20,0) default null, --comment '创建者人员id',
    modify_staff_id number(20,0) default null, --comment '修改者人员id',
    create_time TIMESTAMP,
    modify_time TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE (old_id)
);
MERGE INTO org_department A
    USING (SELECT 1 AS id, 'default_department' AS code, '虚拟部门' AS name, 'sys_department_type/general' AS dept_type, '默认公司/虚拟部门' AS full_path, 1 AS lay_no, '1' AS lay_rec, 1000 AS sort, 1000 AS company_id, 1 AS sys_flag, 'Department_1' AS old_id from dual) B
    ON (A.ID = B.ID)
    WHEN NOT MATCHED THEN INSERT(A.id, A.code, A.name, A.dept_type, A.full_path, A.lay_no, A.lay_rec, A.sort, A.company_id, A.sys_flag, A.old_id)
    VALUES (B.id, B.code, B.name, B.dept_type, B.full_path, B.lay_no, B.lay_rec, B.sort, B.company_id, B.sys_flag, B.old_id);
/*===================================================================*/
/* Table: 多公司模式组织架构岗位表(org_position)                    */
/*===================================================================*/
create table org_position (
    id number(20,0) NOT NULL,
    row_version number(20,0) DEFAULT 0,
    code varchar2(256) NOT NULL,
    name varchar2(2048) NOT NULL,
    description varchar2(2048) DEFAULT NULL,
    full_path varchar2(4000) NOT NULL,
    lay_no number(10,0) NOT NULL,
    lay_rec varchar2(4000) NOT NULL,
    sort number(20,6) NOT NULL,
    company_id number(20,0) NOT NULL,
    dep_id number(20,0) NOT NULL,
    parent_id number(20,0) DEFAULT NULL,
    valid number(1,0) DEFAULT 1,
    sys_flag number(1,0) DEFAULT 0,
    leaf number(1,0) DEFAULT 1,
    old_id varchar2(256) NOT NULL,
    creator varchar2(32) DEFAULT NULL,
    modifier varchar2(32) DEFAULT NULL,
    create_time TIMESTAMP,
    modify_time TIMESTAMP,
    create_staff_id number(20,0) default null, --comment '创建者人员id',
    modify_staff_id number(20,0) default null, --comment '修改者人员id',
    PRIMARY KEY (id),
    UNIQUE (old_id)
);
MERGE INTO org_position A
    USING (SELECT 1 AS id, 'default_position' AS code, '虚拟岗位' AS name, '默认公司/虚拟岗位' AS full_path, 1 AS lay_no, '1' AS lay_rec, 1000 AS sort, 1000 AS company_id, 1 AS dep_id, 1 AS sys_flag, 'Position_1' AS old_id from dual) B
    ON (A.ID = B.ID)
    WHEN NOT MATCHED THEN INSERT(A.id, A.code, A.name, A.full_path, A.lay_no, A.lay_rec, A.sort, A.company_id, A.dep_id, A.sys_flag, A.old_id)
    VALUES (B.id, B.code, B.name, B.full_path, B.lay_no, B.lay_rec, B.sort, B.company_id, B.dep_id, B.sys_flag, B.old_id);
/*===================================================================*/
/* Table: 多公司模式组织架构组表(org_group)                    */
/*===================================================================*/
create table org_group (
    id number(20,0) NOT NULL,
    row_version number(20,0) DEFAULT 0,
    code varchar2(1024) NOT NULL,
    name varchar2(1024) NOT NULL,
    description varchar2(2048) DEFAULT NULL,
    full_path varchar2(4000)  NULL,
    sort number(20,6) NOT NULL,
    company_id number(20,0) NOT NULL,
    manager_id number(20,0) DEFAULT NULL,
    manager_name varchar2(256) DEFAULT NULL,
    valid number(1,0) DEFAULT 1,
    creator varchar2(32) DEFAULT NULL,
    modifier varchar2(32) DEFAULT NULL,
    create_time TIMESTAMP,
    modify_time TIMESTAMP,
    create_staff_id number(20,0) default null, --comment '创建者人员id',
    modify_staff_id number(20,0) default null, --comment '修改者人员id',
    PRIMARY KEY (id)
);
/*===================================================================*/
/* Table: 多公司模式组织架构人员表(org_person)                          */
/*===================================================================*/
create table org_person (
    id number(20,0) NOT NULL,
    row_version number(20,0) DEFAULT 0,
    code varchar2(50) NOT NULL,
    name varchar2(512) NOT NULL,
    old_id varchar2(256) DEFAULT NULL,
    description varchar2(2048) DEFAULT NULL,
    gender varchar2(200) NULL,
    phone varchar2(256) NULL,
    email varchar2(256) NULL,
    status varchar2(200) NULL,
    classified_level varchar2(200) NULL,
    valid number(1,0) DEFAULT 1,
    sys_flag number(1,0) DEFAULT 0,
    main_position number(20,0) DEFAULT NULL,
    direct_leader_id number(20,0) DEFAULT NULL,
    grand_leader_id number(20,0) DEFAULT NULL,
    create_user number(1,0) DEFAULT 0,
    creator varchar2(32) DEFAULT NULL,
    modifier varchar2(32) DEFAULT NULL,
    create_time TIMESTAMP,
    modify_time TIMESTAMP,
    create_staff_id number(20,0) default null, --comment '创建者人员id',
    modify_staff_id number(20,0) default null, --comment '修改者人员id',
    PRIMARY KEY (id),
    UNIQUE (old_id)
);
MERGE INTO org_person A
    USING (SELECT 1 AS id, 'default_person' AS code, '虚拟人员' AS name, 'Position_1' AS old_id, 'sys_person_status/onWork' AS status, 1 AS sys_flag, 1 AS main_position from dual) B
    ON (A.ID = B.ID)
    WHEN NOT MATCHED THEN INSERT(A.id, A.code, A.name, A.old_id, A.status, A.sys_flag, A.main_position)
    VALUES (B.id, B.code, B.name, B.old_id, B.status, B.sys_flag, B.main_position);
/*===================================================================*/
/* Table: 多公司模式组织架构岗位人员关联表(org_person_position)           */
/*===================================================================*/
create table org_person_position (
    id number(20,0) NOT NULL,
    row_version number(20,0) DEFAULT 0,
    position_id number(20,0) NOT NULL ,
    person_id number(20,0) NOT NULL ,
    work_time DATE,
    off_time DATE,
    remark varchar2(1024),
    creator varchar2(32) DEFAULT NULL,
    modifier varchar2(32) DEFAULT NULL,
    valid number(1,0) DEFAULT 1,
    create_time TIMESTAMP,
    modify_time TIMESTAMP,
    create_staff_id number(20,0) default null, --comment '创建者人员id',
    modify_staff_id number(20,0) default null, --comment '修改者人员id',
    PRIMARY KEY (id),
    UNIQUE (position_id, person_id)
);
MERGE INTO org_person_position A
    USING (SELECT 1 AS id, 1 AS position_id, 1 AS person_id from dual) B
    ON (A.ID = B.ID)
    WHEN NOT MATCHED THEN INSERT(A.id, A.position_id, A.person_id)
    VALUES (B.id, B.position_id, B.person_id);
/*===================================================================*/
/* Table: 标签表(org_tag)           */
/*===================================================================*/
create table org_tag (
    id number(20,0) NOT NULL,
    row_version number(20,0) DEFAULT 0,
    tag_type varchar2(32) DEFAULT NULL ,
    name varchar2(256) NOT NULL,
    company_id number(20,0) NOT NULL,
    creator varchar2(32) DEFAULT NULL,
    modifier varchar2(32) DEFAULT NULL,
    valid number(1,0) DEFAULT 1,
    create_time TIMESTAMP,
    modify_time TIMESTAMP,
    create_staff_id number(20,0) default null, --comment '创建者人员id',
    modify_staff_id number(20,0) default null, --comment '修改者人员id',
    PRIMARY KEY (id),
    UNIQUE (company_id, name, tag_type)
);
/*===================================================================*/
/* Table: 多公司模式组织架构部门,岗位,组负责人关联表(org_manager)           */
/*===================================================================*/
create table org_manager (
    id number(20,0) NOT NULL,
    row_version number(20,0) DEFAULT 0,
    org_id number(20,0) NOT NULL ,
    manager_id number(20,0) NOT NULL ,
    manager_name varchar2(2048) NOT NULL,
    manager_type varchar2(16) NOT NULL,
    creator varchar2(32) DEFAULT NULL,
    modifier varchar2(32) DEFAULT NULL,
    create_time TIMESTAMP,
    modify_time TIMESTAMP,
    create_staff_id number(20,0) default null, --comment '创建者人员id',
    modify_staff_id number(20,0) default null, --comment '修改者人员id',
    PRIMARY KEY (id),
    UNIQUE (org_id, manager_id, manager_type)
);

/*===================================================================*/
/* Table: 多公司模式组织架构组管理人员关联表(org_group_person)           */
/*===================================================================*/
create table org_group_person (
    id number(20,0) NOT NULL,
    row_version number(20,0) DEFAULT 0,
    group_id number(20,0) NOT NULL ,
    person_id number(20,0) NOT NULL ,
    creator varchar2(32) DEFAULT NULL,
    modifier varchar2(32) DEFAULT NULL,
    valid number(1,0) DEFAULT 1,
    create_time TIMESTAMP,
    modify_time TIMESTAMP,
    create_staff_id number(20,0) default null, --comment '创建者人员id',
    modify_staff_id number(20,0) default null, --comment '修改者人员id',
    PRIMARY KEY (id),
    UNIQUE (group_id, person_id)
);
/*===================================================================*/
/* Table: Excel导入记录表(org_excel)           */
/*===================================================================*/
create table org_excel (
    id number(20,0) NOT NULL,
    status number(1,0) NOT NULL ,
    file_name varchar2(256) DEFAULT NULL,
    error_file varchar2(256) DEFAULT NULL,
    error_message varchar2(512) DEFAULT NULL,
    excel_type varchar2(64) DEFAULT NULL,
    creator varchar2(32) DEFAULT NULL,
    modifier varchar2(32) DEFAULT NULL,
    create_time TIMESTAMP,
    modify_time TIMESTAMP,
    create_staff_id number(20,0) default null, --comment '创建者人员id',
    modify_staff_id number(20,0) default null, --comment '修改者人员id',
    PRIMARY KEY (id)
);
/*===================================================================*/
/* Table: 岗位角色表(org_position_role)           */
/*===================================================================*/
create table org_position_role (
    id number(20,0) NOT NULL,
    position_id number(20,0) NOT NULL ,
    role_id number(20,0) NOT NULL,
    creator varchar2(32) DEFAULT NULL,
    modifier varchar2(32) DEFAULT NULL,
    create_time TIMESTAMP,
    modify_time TIMESTAMP,
    create_staff_id number(20,0) default null, --comment '创建者人员id',
    modify_staff_id number(20,0) default null, --comment '修改者人员id',
    PRIMARY KEY (id),
    UNIQUE (position_id, role_id)
);

/*===================================================================*/
/* Table: 多公司模式组织架构部门人员关联表(org_person_department)           */
/*===================================================================*/
create table org_person_department (
    id number(20,0) NOT NULL,
    row_version number(20,0) DEFAULT 0,
    dept_id number(20,0) NOT NULL,
    person_id number(20,0) NOT NULL,
    position_id number(20,0) NOT NULL,
    creator VARCHAR2(32) DEFAULT NULL,
    modifier VARCHAR2(32) DEFAULT NULL,
    valid number(1,0) DEFAULT 1,
    create_time TIMESTAMP,
    modify_time TIMESTAMP,
    create_staff_id number(20,0) default null, --comment '创建者人员id',
    modify_staff_id number(20,0) default null, --comment '修改者人员id',
    PRIMARY KEY (id)
);

MERGE INTO org_person_department A
    USING (SELECT 1 AS id, 1 AS position_id, 1 AS person_id, 1 AS dept_id from dual) B
    ON (A.ID = B.ID)
    WHEN NOT MATCHED THEN INSERT(A.id, A.position_id, A.person_id, A.dept_id)
    VALUES (B.id, B.position_id, B.person_id, B.dept_id);

/*===================================================================*/
/* Table: 多公司模式组织架构公司人员关联表(org_person_company)           */
/*===================================================================*/
create table org_person_company (
    id number(20,0) NOT NULL,
    row_version number(20,0) DEFAULT 0,
    company_id number(20,0) NOT NULL,
    person_id number(20,0) NOT NULL,
    position_id number(20,0) NOT NULL,
    creator VARCHAR2(32) DEFAULT NULL,
    modifier VARCHAR2(32) DEFAULT NULL,
    valid number(1,0) DEFAULT 1,
    create_time TIMESTAMP,
    modify_time TIMESTAMP,
    create_staff_id number(20,0) default null, --comment '创建者人员id',
    modify_staff_id number(20,0) default null, --comment '修改者人员id',
    PRIMARY KEY (id)
);

MERGE INTO org_person_company A
    USING (SELECT 1 AS id, 1 AS position_id, 1 AS person_id, 1000 AS company_id from dual) B
    ON (A.ID = B.ID)
    WHEN NOT MATCHED THEN INSERT(A.id, A.position_id, A.person_id, A.company_id)
    VALUES (B.id, B.position_id, B.person_id, B.company_id);
/*==============================================================*/
/* Table:组织架构助记码(org_mnecode)                */
/*==============================================================*/
CREATE TABLE org_mnecode
(
    ID                  NUMBER(20, 0) NOT NULL ,
    row_version         NUMBER(20) DEFAULT 0 NOT NULL  ,
    LANGUAGE            VARCHAR(510),
    ORG_ID           NUMBER(20, 0) ,
    MNE_CODE            VARCHAR(510),
    creator VARCHAR2(32) DEFAULT NULL,
    modifier VARCHAR2(32) DEFAULT NULL,
    create_time TIMESTAMP,
    modify_time TIMESTAMP,
    create_staff_id number(20,0) default null, --comment '创建者人员id',
    modify_staff_id number(20,0) default null, --comment '修改者人员id',
    PRIMARY KEY(ID)
);
COMMENT ON TABLE org_mnecode IS '组织助记码';
COMMENT ON COLUMN org_mnecode.ID IS '主键ID';
COMMENT ON COLUMN org_mnecode.row_version IS '版本';
COMMENT ON COLUMN org_mnecode.LANGUAGE IS '语言';
COMMENT ON COLUMN org_mnecode.MNE_CODE IS '助记码';
COMMENT ON COLUMN org_mnecode.ORG_ID IS '组织ID';
/*===================================================================*/
/* Table: 视图        */
/*===================================================================*/
CREATE OR REPLACE FORCE VIEW "BASE_COMPANY" ("ID", "VERSION", "CODE", "DESCRIPTION", "SHORT_NAME", "NAME", "ADDRESS", "SORT", "PARENT_ID", "LAY_REC", "VALID", "CREATE_STAFF_ID", "MODIFY_STAFF_ID", "DELETE_STAFF_ID", "CREATE_TIME", "MODIFY_TIME", "DELETE_TIME", "IS_DEFAULT", "CUSTOMER_FIELD2", "CUSTOMER_FIELD1", "UUID", "SC_NATURE", "EMAIL", "SITE", "FAX", "TELEPHONE", "POST_CODE", "TYPE") AS
  (
select
	id,
	row_version as version,
	code,
	description,
	short_name,
	full_name as name,
	address,
	sort,
	parent_id,
	lay_rec,
	valid,
	NULL as create_staff_id,
	NULL as modify_staff_id,
	NULL as delete_staff_id,
	create_time,
	modify_time,
	NULL as delete_time,
	1 AS IS_DEFAULT,
	NULL AS CUSTOMER_FIELD2,
	NULL AS CUSTOMER_FIELD1,
	NULL AS UUID,
	NULL AS SC_NATURE,
	NULL AS EMAIL,
	NULL AS SITE,
	NULL AS FAX,
	NULL AS TELEPHONE,
	NULL AS POST_CODE,
	'UNIT' AS TYPE
from
	org_company
);


CREATE OR REPLACE FORCE VIEW "BASE_COMPANYSTAFF" ("ID", "VERSION", "CID", "STAFF_ID", "VALID", "TRANSFER_OUT_DEAL_TIME", "TRANSFER_IN_DEAL_TIME", "TRANSFER_OUT_DEALER_ID", "TRANSFER_IN_DEALER_ID", "EDIT_DATE", "END_TIME", "START_TIME") AS
  (
select
	id,
	row_version as version,
	company_id as cid,
	person_id as staff_id,
	valid,
	NULL AS TRANSFER_OUT_DEAL_TIME,
	NULL AS TRANSFER_IN_DEAL_TIME,
	NULL AS TRANSFER_OUT_DEALER_ID,
	NULL AS TRANSFER_IN_DEALER_ID,
	NULL AS EDIT_DATE,
	NULL AS END_TIME,
	NULL AS START_TIME
from org_person_company
);


CREATE OR REPLACE FORCE VIEW "BASE_CUSTOM_GROUP" ("ID", "VERSION", "CODE", "NAME_CN", "DESCRIPTION", "SORT", "CID", "VALID", "CREATE_STAFF_ID", "MODIFY_STAFF_ID", "DELETE_STAFF_ID", "CREATE_TIME", "MODIFY_TIME", "DELETE_TIME") AS
  ( select id, row_version as version, code, name as name_cn, description, sort, company_id as cid, valid, NULL as create_staff_id, NULL as modify_staff_id, NULL as delete_staff_id, create_time, modify_time, TO_TIMESTAMP('1970-01-01','yyyy-MM-dd') AS delete_time from org_group );


CREATE OR REPLACE FORCE VIEW "BASE_CUSTOM_GROUPMEMBER" ("ID", "VERSION", "RELATED_CUSTOM_GROUP_ID", "RELATED_STAFF_ID", "VALID", "CREATE_STAFF_ID", "MODIFY_STAFF_ID", "DELETE_STAFF_ID", "CREATE_TIME", "MODIFY_TIME", "CID", "DELETE_TIME") AS
  ( select gp.id as id, gp.row_version as version, gp.group_id as related_custom_group_id, gp.person_id as related_staff_id, gp.valid as valid, CAST (gp.creator AS int) as create_staff_id, CAST (gp.modifier AS int) as modify_staff_id, null as delete_staff_id, gp.create_time, gp.modify_time, g.company_id as cid, NULL AS delete_time from org_group_person gp left join org_group g on gp.group_id = g.id );


 CREATE OR REPLACE FORCE VIEW "BASE_DEPARTMENT" ("ID", "VERSION", "CODE", "NAME", "DESCRIPTION", "FULL_PATH_NAME", "LAY_NO", "LAY_REC", "SORT", "CID", "PARENT_ID", "VALID", "LEAF", "CREATE_STAFF_ID", "MODIFY_STAFF_ID", "DELETE_STAFF_ID", "CREATE_TIME", "MODIFY_TIME", "DELETE_TIME", "STAFF_ID", "OBJPARAME", "OBJPARAMD", "OBJPARAMC", "OBJPARAMB", "OBJPARAMA", "RES_SCPARAME", "RES_SCPARAMD", "RES_SCPARAMC", "RES_SCPARAMB", "RES_SCPARAMA", "CHARPARAME", "CHARPARAMD", "CHARPARAMC", "CHARPARAMB", "CHARPARAMA", "NUMBERPARAME", "NUMBERPARAMD", "NUMBERPARAMC", "NUMBERPARAMB", "NUMBERPARAMA", "DATEPARAME", "DATEPARAMD", "DATEPARAMC", "DATEPARAMB", "DATEPARAMA", "INTEGERPARAME", "INTEGERPARAMD", "INTEGERPARAMC", "INTEGERPARAMB", "INTEGERPARAMA", "CUSTOMER_FIELD2", "CUSTOMER_FIELD1", "SC_NATURE", "UUID", "IS_VIRTUAL") AS
  (
select
	id,
	row_version as version,
	code,
	name,
	description,
	full_path as full_path_name,
	lay_no,
	lay_rec,
	sort,
	company_id as cid,
	parent_id,
	valid,
	leaf,
	NULL as create_staff_id,
	NULL as modify_staff_id,
	NULL as delete_staff_id,
	create_time,
	modify_time,
	NULL as delete_time,
	NULL as staff_id,
	NULL AS OBJPARAME,
	NULL AS OBJPARAMD,
	NULL AS OBJPARAMC,
	NULL AS OBJPARAMB,
	NULL AS OBJPARAMA,
	NULL AS RES_SCPARAME,
	NULL AS RES_SCPARAMD,
	NULL AS RES_SCPARAMC,
	NULL AS RES_SCPARAMB,
	NULL AS RES_SCPARAMA,
	NULL AS CHARPARAME,
	NULL AS CHARPARAMD,
	NULL AS CHARPARAMC,
	NULL AS CHARPARAMB,
	NULL AS CHARPARAMA,
	NULL AS NUMBERPARAME,
	NULL AS NUMBERPARAMD,
	NULL AS NUMBERPARAMC,
	NULL AS NUMBERPARAMB,
	NULL AS NUMBERPARAMA,
	NULL AS DATEPARAME,
	NULL AS DATEPARAMD,
	NULL AS DATEPARAMC,
	NULL AS DATEPARAMB,
	NULL AS DATEPARAMA,
	NULL AS INTEGERPARAME,
	NULL AS INTEGERPARAMD,
	NULL AS INTEGERPARAMC,
	NULL AS INTEGERPARAMB,
	NULL AS INTEGERPARAMA,
	NULL AS CUSTOMER_FIELD2,
	NULL AS CUSTOMER_FIELD1,
	NULL AS SC_NATURE,
	NULL AS UUID,
	SYS_FLAG AS IS_VIRTUAL
from
	org_department);

CREATE OR REPLACE FORCE VIEW "BASE_DEPARTMENTWORK" ("ID", "VERSION", "DEPARTMENT_ID", "STAFF_ID", "VALID", "TRANSFER_OUT_DEAL_TIME", "TRANSFER_IN_DEAL_TIME", "TRANSFER_OUT_DEALER_ID", "TRANSFER_IN_DEALER_ID", "EDIT_DATE", "END_TIME", "START_TIME") AS
  (
select
	id,
	row_version as version,
	dept_id as department_id,
	person_id as staff_id,
	valid,
	NULL AS TRANSFER_OUT_DEAL_TIME,
	NULL AS TRANSFER_IN_DEAL_TIME,
	NULL AS TRANSFER_OUT_DEALER_ID,
	NULL AS TRANSFER_IN_DEALER_ID,
	NULL AS EDIT_DATE,
	NULL AS END_TIME,
	NULL AS START_TIME
from org_person_department
);


CREATE OR REPLACE FORCE VIEW "BASE_POSITION" ("ID", "VERSION", "CODE", "NAME", "DESCRIPTION", "FULL_PATH_NAME", "LAY_NO", "LAY_REC", "SORT", "CID", "DEPARTMENT_ID", "PARENT_ID", "VALID", "LEAF", "CREATE_STAFF_ID", "MODIFY_STAFF_ID", "DELETE_STAFF_ID", "CREATE_TIME", "MODIFY_TIME", "DELETE_TIME", "OBJPARAME", "OBJPARAMD", "OBJPARAMC", "OBJPARAMB", "OBJPARAMA", "RES_SCPARAME", "RES_SCPARAMD", "RES_SCPARAMC", "RES_SCPARAMB", "RES_SCPARAMA", "CHARPARAME", "CHARPARAMD", "CHARPARAMC", "CHARPARAMB", "CHARPARAMA", "NUMBERPARAME", "NUMBERPARAMD", "NUMBERPARAMC", "NUMBERPARAMB", "NUMBERPARAMA", "DATEPARAME", "DATEPARAMD", "DATEPARAMC", "DATEPARAMB", "DATEPARAMA", "INTEGERPARAME", "INTEGERPARAMD", "INTEGERPARAMC", "INTEGERPARAMB", "INTEGERPARAMA", "UUID", "IS_VIRTUAL", "SC_NATURE") AS
  (
select
	id,
	row_version as version,
	code,
	name,
	description,
	full_path as full_path_name,
	lay_no,
	lay_rec,
	sort,
	company_id as cid,
	dep_id as department_id,
	parent_id,
	valid,
	leaf,
	NULL as create_staff_id,
	NULL as modify_staff_id,
	NULL as delete_staff_id,
	create_time,
	modify_time,
	NULL as delete_time,
	NULL AS OBJPARAME,
	NULL AS OBJPARAMD,
	NULL AS OBJPARAMC,
	NULL AS OBJPARAMB,
	NULL AS OBJPARAMA,
	NULL AS RES_SCPARAME,
	NULL AS RES_SCPARAMD,
	NULL AS RES_SCPARAMC,
	NULL AS RES_SCPARAMB,
	NULL AS RES_SCPARAMA,
	NULL AS CHARPARAME,
	NULL AS CHARPARAMD,
	NULL AS CHARPARAMC,
	NULL AS CHARPARAMB,
	NULL AS CHARPARAMA,
	NULL AS NUMBERPARAME,
	NULL AS NUMBERPARAMD,
	NULL AS NUMBERPARAMC,
	NULL AS NUMBERPARAMB,
	NULL AS NUMBERPARAMA,
	NULL AS DATEPARAME,
	NULL AS DATEPARAMD,
	NULL AS DATEPARAMC,
	NULL AS DATEPARAMB,
	NULL AS DATEPARAMA,
	NULL AS INTEGERPARAME,
	NULL AS INTEGERPARAMD,
	NULL AS INTEGERPARAMC,
	NULL AS INTEGERPARAMB,
	NULL AS INTEGERPARAMA,
	NULL AS UUID,
	SYS_FLAG AS IS_VIRTUAL,
	NULL AS SC_NATURE
from
	org_position);

CREATE OR REPLACE FORCE VIEW "BASE_POSITIONWORK" ("ID", "VERSION", "POSITION_ID", "STAFF_ID", "START_TIME", "VALID", "TRANSFER_OUT_DEAL_TIME", "TRANSFER_IN_DEAL_TIME", "TRANSFER_OUT_DEALER_ID", "TRANSFER_IN_DEALER_ID", "EDIT_DATE", "MAIN_POSI_FLAG", "PARENT_PW_ID", "TRANSFER_OUT_DES", "TRANSFER_IN_DES", "END_TIME") AS
  (
select
	id,
	row_version as version,
	position_id,
	person_id as staff_id,
	work_time as start_time,
	valid,
	NULL AS TRANSFER_OUT_DEAL_TIME,
	NULL AS TRANSFER_IN_DEAL_TIME,
	NULL AS TRANSFER_OUT_DEALER_ID,
	NULL AS TRANSFER_IN_DEALER_ID,
	NULL AS EDIT_DATE,
	NULL AS MAIN_POSI_FLAG,
	NULL AS PARENT_PW_ID,
	NULL AS TRANSFER_OUT_DES,
	NULL AS TRANSFER_IN_DES,
	NULL AS END_TIME
from org_person_position
);


CREATE OR REPLACE FORCE VIEW "BASE_ROLEPOSITION" ("ID", "VERSION", "POSITION_ID", "ROLE_ID", "VALID") AS ( select id, 0 as version, position_id, role_id, 1 as valid from org_position_role );

CREATE OR REPLACE FORCE VIEW "BASE_STAFF" ("ID", "VERSION", "CODE", "NAME", "SEX", "MOBILE", "EMAIL", "WORK_STATUS", "SECURITY_CLASS", "VALID", "USER_ID", "MAIN_POSITION_ID", "CREATE_STAFF_ID", "MODIFY_STAFF_ID", "DELETE_STAFF_ID", "CREATE_TIME", "MODIFY_TIME", "DELETE_TIME", "MEMO", "CUSTOMER_FIELD2", "CUSTOMER_FIELD1", "UUID", "SORT", "SYS_FLAG", "HIGHER_LEADER_STAFF_ID", "LEADER_STAFF_ID", "YGXS", "HUKOUXINGZHI", "HUKOUDI", "DANGANDI", "DEGREE", "EDUCATIONAL", "PROFESSION", "BIYE_TIME", "SCHOOL", "FOREIGN_LANGUAGE", "COMPUTE_LEVEL", "HEIGHT", "VIRTUAL_MOBILE", "POLITICS_INFO", "NATION", "MARRIAGE", "ID_CARD", "NATIVE_PLACE", "BIRTHDAY") AS
  (
select
	op.id as id,
	row_version as version,
	code,
	name,
	gender as SEX,
	phone as mobile,
	email,
	status as work_status,
	classified_level as security_class,
	op.valid as valid,
	au.id as user_id,
	main_position as main_position_id,
	NULL as create_staff_id,
	NULL as modify_staff_id,
	NULL as delete_staff_id,
	op.create_time AS create_time,
	op.modify_time AS modify_time,
	NULL as delete_time,
	op.description  as memo,
	NULL AS CUSTOMER_FIELD2,
	NULL AS CUSTOMER_FIELD1,
	NULL AS UUID,
	NULL AS SORT,
	op.sys_flag AS SYS_FLAG,
	op.grand_leader_id AS HIGHER_LEADER_STAFF_ID,
	op.direct_leader_id AS LEADER_STAFF_ID,
	NULL AS YGXS,
	NULL AS HUKOUXINGZHI,
	NULL AS HUKOUDI,
	NULL AS DANGANDI,
	NULL AS DEGREE,
	NULL AS EDUCATIONAL,
	NULL AS PROFESSION,
	NULL AS BIYE_TIME,
	NULL AS SCHOOL,
	NULL AS FOREIGN_LANGUAGE,
	NULL AS COMPUTE_LEVEL,
	NULL AS HEIGHT,
	NULL AS VIRTUAL_MOBILE,
	NULL AS POLITICS_INFO,
	NULL AS NATION,
	NULL AS MARRIAGE,
	NULL AS ID_CARD,
	NULL AS NATIVE_PLACE,
	NULL AS BIRTHDAY
from
	org_person op
left outer join
	auth_user au
on op.id=au.person_id and au.valid = 1
);
create or replace view base_linkinfo as (
select
  id AS base_staff_id,
  phone AS mobile,
  NULL AS jabber,
  email
from org_person
);

create or replace VIEW base_workinfo AS (
SELECT
  NULL AS worktime,
  NULL AS JZKST,
  NULL AS JGSSJ,
  NULL AS LRSJ,
  NULL AS RZSJ,
  NULL AS ZZSJ,
  NULL AS CONPANYNAME,
  NULL AS BIGDEPARTMENT,
  NULL AS DEPARTMENT,
  NULL AS POSITIONNAME,
  NULL AS POSITIONTYPE,
  NULL AS ZWMC,
  NULL AS GSEMAIL,
  NULL AS ZKXNH,
  NULL AS ZXHM,
  NULL AS FJH,
  NULL AS TXLBZ,
  NULL AS FJHM,
  op.id AS BASE_STAFF_ID,
  NULL AS YJZZSJ
FROM ORG_PERSON op
);

CREATE OR REPLACE FORCE VIEW base_staff_mnecode AS (
SELECT
	m.id AS id,
	m.row_version AS version,
	m.org_id AS staff,
	m.mne_code
FROM
	org_mnecode m
INNER JOIN org_person p ON
	m.org_id = p.id );

CREATE OR REPLACE force VIEW base_department_mnecode AS (
SELECT
	m.id AS id,
	m.row_version AS version,
	m.org_id AS department,
	m.mne_code
FROM
	org_mnecode m
INNER JOIN org_department d ON
	m.org_id = d.id );

CREATE OR REPLACE force VIEW base_position_mnecode AS (
SELECT
	m.id AS id,
	m.row_version AS version,
	m.org_id AS position,
	m.mne_code
FROM
	org_mnecode m
INNER JOIN org_position p ON
	m.org_id = p.id );

CREATE OR REPLACE force VIEW base_custom_group_mnecode AS (
SELECT
	m.id AS id,
	m.row_version AS version,
	m.org_id AS custom_group,
	m.mne_code
FROM
	org_mnecode m
INNER JOIN org_group g ON
	m.org_id = g.id );

## 1.0.2
CREATE OR REPLACE FORCE VIEW "BASE_POSITION" ("ID", "VERSION", "CODE", "NAME", "DESCRIPTION", "FULL_PATH_NAME", "LAY_NO", "LAY_REC", "SORT", "CID", "DEPARTMENT_ID", "PARENT_ID", "VALID", "LEAF", "CREATE_STAFF_ID", "MODIFY_STAFF_ID", "DELETE_STAFF_ID", "CREATE_TIME", "MODIFY_TIME", "DELETE_TIME", "OBJPARAME", "OBJPARAMD", "OBJPARAMC", "OBJPARAMB", "OBJPARAMA", "RES_SCPARAME", "RES_SCPARAMD", "RES_SCPARAMC", "RES_SCPARAMB", "RES_SCPARAMA", "CHARPARAME", "CHARPARAMD", "CHARPARAMC", "CHARPARAMB", "CHARPARAMA", "NUMBERPARAME", "NUMBERPARAMD", "NUMBERPARAMC", "NUMBERPARAMB", "NUMBERPARAMA", "DATEPARAME", "DATEPARAMD", "DATEPARAMC", "DATEPARAMB", "DATEPARAMA", "INTEGERPARAME", "INTEGERPARAMD", "INTEGERPARAMC", "INTEGERPARAMB", "INTEGERPARAMA", "UUID", "IS_VIRTUAL", "SC_NATURE") AS
  (
select
	id,
	row_version as version,
	code,
	name,
	description,
	full_path as full_path_name,
	lay_no,
	lay_rec,
	sort,
	company_id as cid,
	dep_id as department_id,
	parent_id,
	valid,
	leaf,
	NULL as create_staff_id,
	NULL as modify_staff_id,
	NULL as delete_staff_id,
	create_time,
	modify_time,
	NULL as delete_time,
	NULL AS OBJPARAME,
	NULL AS OBJPARAMD,
	NULL AS OBJPARAMC,
	NULL AS OBJPARAMB,
	NULL AS OBJPARAMA,
	NULL AS RES_SCPARAME,
	NULL AS RES_SCPARAMD,
	NULL AS RES_SCPARAMC,
	NULL AS RES_SCPARAMB,
	NULL AS RES_SCPARAMA,
	NULL AS CHARPARAME,
	NULL AS CHARPARAMD,
	NULL AS CHARPARAMC,
	NULL AS CHARPARAMB,
	NULL AS CHARPARAMA,
	NULL AS NUMBERPARAME,
	NULL AS NUMBERPARAMD,
	NULL AS NUMBERPARAMC,
	NULL AS NUMBERPARAMB,
	NULL AS NUMBERPARAMA,
	NULL AS DATEPARAME,
	NULL AS DATEPARAMD,
	NULL AS DATEPARAMC,
	NULL AS DATEPARAMB,
	NULL AS DATEPARAMA,
	NULL AS INTEGERPARAME,
	NULL AS INTEGERPARAMD,
	NULL AS INTEGERPARAMC,
	NULL AS INTEGERPARAMB,
	NULL AS INTEGERPARAMA,
	NULL AS UUID,
	SYS_FLAG AS IS_VIRTUAL,
	NULL AS SC_NATURE
from
	org_position);


## 1.0.3

/*==============================================================*/
/* Table:公司助记码(org_company_mnecode)                */
/*==============================================================*/
CREATE TABLE ORG_COMPANY_MNECODE
(
    ID                  NUMBER(20, 0) NOT NULL ,
    ROW_VERSION         NUMBER(20) DEFAULT 0 NOT NULL  ,
    LANGUAGE            VARCHAR(510),
    MNE_CODE            VARCHAR(510),
    COMPANY_ID           NUMBER(20, 0) ,
    COMPANY_SHORT_NAME     VARCHAR(510),
    CREATOR VARCHAR2(32) DEFAULT NULL,
    MODIFIER VARCHAR2(32) DEFAULT NULL,
    CREATE_TIME TIMESTAMP,
    MODIFY_TIME TIMESTAMP,
    CREATE_STAFF_ID NUMBER(20,0) DEFAULT NULL, --COMMENT '创建者人员ID',
    MODIFY_STAFF_ID NUMBER(20,0) DEFAULT NULL, --COMMENT '修改者人员ID',
    PRIMARY KEY(ID)
);
COMMENT ON TABLE ORG_COMPANY_MNECODE IS '公司助记码';
COMMENT ON COLUMN ORG_COMPANY_MNECODE.ID IS '主键ID';
COMMENT ON COLUMN ORG_COMPANY_MNECODE.ROW_VERSION IS '版本';
COMMENT ON COLUMN ORG_COMPANY_MNECODE.LANGUAGE IS '语言';
COMMENT ON COLUMN ORG_COMPANY_MNECODE.MNE_CODE IS '助记码';
COMMENT ON COLUMN ORG_COMPANY_MNECODE.COMPANY_ID IS '公司id';
COMMENT ON COLUMN ORG_COMPANY_MNECODE.COMPANY_SHORT_NAME IS '公司简称';

/*==============================================================*/
/* Table:部门助记码(org_department_mnecode)                */
/*==============================================================*/
CREATE TABLE ORG_DEPARTMENT_MNECODE
(
    ID                  NUMBER(20, 0) NOT NULL ,
    ROW_VERSION         NUMBER(20) DEFAULT 0 NOT NULL  ,
    LANGUAGE            VARCHAR(510),
    MNE_CODE            VARCHAR(510),
    DEPT_ID           NUMBER(20, 0) ,
    DEPT_NAME     VARCHAR(510),
    CREATOR VARCHAR2(32) DEFAULT NULL,
    MODIFIER VARCHAR2(32) DEFAULT NULL,
    CREATE_TIME TIMESTAMP,
    MODIFY_TIME TIMESTAMP,
    CREATE_STAFF_ID NUMBER(20,0) DEFAULT NULL, --COMMENT '创建者人员ID',
    MODIFY_STAFF_ID NUMBER(20,0) DEFAULT NULL, --COMMENT '修改者人员ID',
    PRIMARY KEY(ID)
);
COMMENT ON TABLE ORG_DEPARTMENT_MNECODE IS '部门助记码';
COMMENT ON COLUMN ORG_DEPARTMENT_MNECODE.ID IS '主键ID';
COMMENT ON COLUMN ORG_DEPARTMENT_MNECODE.ROW_VERSION IS '版本';
COMMENT ON COLUMN ORG_DEPARTMENT_MNECODE.LANGUAGE IS '语言';
COMMENT ON COLUMN ORG_DEPARTMENT_MNECODE.MNE_CODE IS '助记码';
COMMENT ON COLUMN ORG_DEPARTMENT_MNECODE.DEPT_ID IS '部门id';
COMMENT ON COLUMN ORG_DEPARTMENT_MNECODE.DEPT_NAME IS '部门名称';

/*==============================================================*/
/* Table:岗位助记码(org_position_mnecode)                */
/*==============================================================*/
CREATE TABLE ORG_POSITION_MNECODE
(
    ID                  NUMBER(20, 0) NOT NULL ,
    ROW_VERSION         NUMBER(20) DEFAULT 0 NOT NULL  ,
    LANGUAGE            VARCHAR(510),
    MNE_CODE            VARCHAR(510),
    POSITION_ID           NUMBER(20, 0) ,
    POSITION_NAME     VARCHAR(510),
    CREATOR VARCHAR2(32) DEFAULT NULL,
    MODIFIER VARCHAR2(32) DEFAULT NULL,
    CREATE_TIME TIMESTAMP,
    MODIFY_TIME TIMESTAMP,
    CREATE_STAFF_ID NUMBER(20,0) DEFAULT NULL, --COMMENT '创建者人员ID',
    MODIFY_STAFF_ID NUMBER(20,0) DEFAULT NULL, --COMMENT '修改者人员ID',
    PRIMARY KEY(ID)
);
COMMENT ON TABLE ORG_POSITION_MNECODE IS '岗位助记码';
COMMENT ON COLUMN ORG_POSITION_MNECODE.ID IS '主键ID';
COMMENT ON COLUMN ORG_POSITION_MNECODE.ROW_VERSION IS '版本';
COMMENT ON COLUMN ORG_POSITION_MNECODE.LANGUAGE IS '语言';
COMMENT ON COLUMN ORG_POSITION_MNECODE.MNE_CODE IS '助记码';
COMMENT ON COLUMN ORG_POSITION_MNECODE.POSITION_ID IS '岗位id';
COMMENT ON COLUMN ORG_POSITION_MNECODE.POSITION_NAME IS '岗位名称';

/*==============================================================*/
/* Table:人员助记码(org_person_mnecode)                */
/*==============================================================*/
CREATE TABLE ORG_PERSON_MNECODE
(
    ID                  NUMBER(20, 0) NOT NULL ,
    ROW_VERSION         NUMBER(20) DEFAULT 0 NOT NULL  ,
    LANGUAGE            VARCHAR(510),
    MNE_CODE            VARCHAR(510),
    PERSON_ID           NUMBER(20, 0) ,
    PERSON_NAME     VARCHAR(510),
    CREATOR VARCHAR2(32) DEFAULT NULL,
    MODIFIER VARCHAR2(32) DEFAULT NULL,
    CREATE_TIME TIMESTAMP,
    MODIFY_TIME TIMESTAMP,
    CREATE_STAFF_ID NUMBER(20,0) DEFAULT NULL, --COMMENT '创建者人员ID',
    MODIFY_STAFF_ID NUMBER(20,0) DEFAULT NULL, --COMMENT '修改者人员ID',
    PRIMARY KEY(ID)
);
COMMENT ON TABLE ORG_PERSON_MNECODE IS '人员助记码';
COMMENT ON COLUMN ORG_PERSON_MNECODE.ID IS '主键ID';
COMMENT ON COLUMN ORG_PERSON_MNECODE.ROW_VERSION IS '版本';
COMMENT ON COLUMN ORG_PERSON_MNECODE.LANGUAGE IS '语言';
COMMENT ON COLUMN ORG_PERSON_MNECODE.MNE_CODE IS '助记码';
COMMENT ON COLUMN ORG_PERSON_MNECODE.PERSON_ID IS '人员id';
COMMENT ON COLUMN ORG_PERSON_MNECODE.PERSON_NAME IS '人员名称';


CREATE OR REPLACE FORCE VIEW base_staff_mnecode AS (
SELECT
	m.id AS id,
	m.row_version AS version,
	m.person_id AS staff,
	m.mne_code
FROM
	ORG_PERSON_MNECODE m);

CREATE OR REPLACE force VIEW base_department_mnecode AS (
SELECT
	m.id AS id,
	m.row_version AS version,
	m.dept_id AS department,
	m.mne_code
FROM
	ORG_DEPARTMENT_MNECODE m);

CREATE OR REPLACE force VIEW base_position_mnecode AS (
SELECT
	m.id AS id,
	m.row_version AS version,
	m.position_id AS position,
	m.mne_code
FROM
	ORG_POSITION_MNECODE m);


## 1.0.4
alter table org_person add user_id number(20,0) default null;
alter table org_person add user_name varchar2(256) default null;

## 1.0.5
CREATE INDEX ORG_COMPANY_LAY_REC_IDX ON ORG_COMPANY (LAY_REC);

## 1.0.6
update org_position set name = '虚拟岗位' where sys_flag=1;
update org_department set name = '虚拟部门' where sys_flag=1;
update org_person set name = "虚拟人员", gender = null where sys_flag=1;

## 1.0.7
alter table org_person add entry_date varchar2(64) default null;
alter table org_person add title varchar2(64) default null;
alter table org_person add qualification varchar2(256) default null;
alter table org_person add education varchar2(64) default null;
alter table org_person add major varchar2(256) default null;
alter table org_person add id_number varchar2(32) default null;
create index id_number_index on org_person(id_number,valid);

## 1.0.8
alter table org_person add image_url varchar2(2048) default null;
alter table org_person add sign_pic_url varchar2(2048) default null;
alter table org_person modify qualification varchar2(2048) default null;
alter table org_person modify major varchar2(2048) default null;
alter table org_person modify id_number varchar2(2048) default null;

## 1.0.9
alter table org_person rename column image_url to avatar_url;
alter table org_person add entry_date varchar2(64) default null;

## 1.0.10
update org_position set name = '虚拟岗位' where sys_flag=1;
update org_department set name = '虚拟部门' where sys_flag=1;
update org_person set gender = null where sys_flag=1;

## 1.0.11
create index code_index on org_person(code,valid);
create index position_id_index on org_person_position(position_id);