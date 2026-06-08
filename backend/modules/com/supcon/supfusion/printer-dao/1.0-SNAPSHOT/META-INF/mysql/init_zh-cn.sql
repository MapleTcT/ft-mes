## 1.0.0
/*===================================================================*/
/* Table: 打印日志表(printer_log)                */
/*===================================================================*/
create table if not exists printer_log (
    id BIGINT NOT NULL COMMENT '主键ID',
    row_version BIGINT DEFAULT 0 COMMENT '版本号',
    template_id BIGINT NOT NULL COMMENT '打印模板id',
    page_id VARCHAR(128) NOT NULL COMMENT '页面id',
    creator VARCHAR(32) DEFAULT NULL COMMENT '创建者',
    modifier VARCHAR(32) DEFAULT NULL COMMENT '修改者',
    create_time TIMESTAMP COMMENT '创建时间',
    modify_time TIMESTAMP COMMENT '修改时间',
    create_staff_id bigint(20) default null comment '创建者人员id',
    modify_staff_id bigint(20) default null comment '修改者人员id',
    PRIMARY KEY (id)
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8
COMMENT='打印日志表';
/*===================================================================*/
/* Table: 对象实例iframe页面注册表(printer_object_iframe)               */
/*===================================================================*/
create table if not exists printer_object_iframe (
    id BIGINT NOT NULL COMMENT '主键ID',
    name VARCHAR(128) NOT NULL COMMENT '对象实例数据源展示名',
    source TINYINT NOT NULL COMMENT '注册来源',
    url VARCHAR(1024) NOT NULL COMMENT '对象实例iframe页面url',
    valid TINYINT(1) DEFAULT 1 COMMENT '逻辑删除,是否有效0:无效 1:有效',
    creator VARCHAR(32) DEFAULT NULL COMMENT '创建者',
    modifier VARCHAR(32) DEFAULT NULL COMMENT '修改者',
    create_time TIMESTAMP COMMENT '创建时间',
    modify_time TIMESTAMP COMMENT '修改时间',
    create_staff_id bigint(20) default null comment '创建者人员id',
    modify_staff_id bigint(20) default null comment '修改者人员id',
    PRIMARY KEY (id)
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8
COMMENT='对象实例iframe页面注册表';
/*===================================================================*/
/* Table: 打印模板设计内容表(printer_design_content)               */
/*===================================================================*/
create table if not exists printer_design_content(
    template_id BIGINT NOT NULL COMMENT '模板id',
    content TEXT DEFAULT "" COMMENT '模板设计json内容',
    valid TINYINT(1) DEFAULT 1 COMMENT '逻辑删除,是否有效0:无效 1:有效',
    creator VARCHAR(32) DEFAULT NULL COMMENT '创建者',
    modifier VARCHAR(32) DEFAULT NULL COMMENT '修改者',
    create_time TIMESTAMP COMMENT '创建时间',
    modify_time TIMESTAMP COMMENT '修改时间',
    create_staff_id bigint(20) default null comment '创建者人员id',
    modify_staff_id bigint(20) default null comment '修改者人员id',
    KEY templateid_valid(template_id, valid)
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8
COMMENT='打印模板设计内容表';

/*===================================================================*/
/* Table: 打印标签表(printer_label)               */
/*===================================================================*/
create table if not exists printer_label (
    id BIGINT NOT NULL COMMENT '主键id',
    label_name VARCHAR(128) NOT NULL COMMENT '标签名称',
    creator VARCHAR(32) DEFAULT NULL COMMENT '创建者',
    modifier VARCHAR(32) DEFAULT NULL COMMENT '修改者',
    create_time TIMESTAMP COMMENT '创建时间',
    modify_time TIMESTAMP COMMENT '修改时间',
    create_staff_id bigint(20) default null comment '创建者人员id',
    modify_staff_id bigint(20) default null comment '修改者人员id',
    PRIMARY KEY (id)
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8
COMMENT='打印标签表';

/*===================================================================*/
/* Table: 数据注册表(printer_register)               */
/*===================================================================*/
create table if not exists printer_register (
    id BIGINT NOT NULL COMMENT '主键id',
    source TINYINT NOT NULL COMMENT '页面来源',
    service_url VARCHAR(1024) NOT NULL COMMENT '服务地址',
    service_type TINYINT NOT NULL COMMENT '服务类型:1：获取APP模块数据、2：获取页面数据、3：根据表单ID获取实体对象数据、4：模型列表动态加载子属性',
    call_type TINYINT NOT NULL COMMENT '请求方式：1、GET, 2、POST, 3、PUT, 4、DELETE',
    creator VARCHAR(32) DEFAULT NULL COMMENT '创建者',
    modifier VARCHAR(32) DEFAULT NULL COMMENT '修改者',
    create_time TIMESTAMP COMMENT '创建时间',
    modify_time TIMESTAMP COMMENT '修改时间',
    create_staff_id bigint(20) default null comment '创建者人员id',
    modify_staff_id bigint(20) default null comment '修改者人员id',
    PRIMARY KEY (id)
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8
COMMENT='页面注册表';

/*===================================================================*/
/* Table: 打印模板表(printer_template)               */
/*===================================================================*/
create table if not exists printer_template (
    id BIGINT NOT NULL COMMENT '主键id',
    template_name VARCHAR(128) NOT NULL COMMENT '模板名称',
    i18n_key VARCHAR(128) COMMENT '模板名称国际化编号',
    template_code VARCHAR(128) NOT NULL COMMENT '模板编码',
    app_id VARCHAR(128) NOT NULL COMMENT 'app编号',
    label_names VARCHAR(128) COMMENT '标签名称',
    template_desc VARCHAR(512) DEFAULT NULL COMMENT '模板描述',
    enabled TINYINT(1) DEFAULT 1 COMMENT '是否发布，1：已发布，2：未发布，3：修改中',
    valid TINYINT(1) DEFAULT 1 COMMENT '是否有效',
    creator VARCHAR(32) DEFAULT NULL COMMENT '创建者',
    modifier VARCHAR(32) DEFAULT NULL COMMENT '修改者',
    create_time TIMESTAMP COMMENT '创建时间',
    modify_time TIMESTAMP COMMENT '修改时间',
    create_staff_id bigint(20) default null comment '创建者人员id',
    modify_staff_id bigint(20) default null comment '修改者人员id',
    PRIMARY KEY (id)
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8
COMMENT='打印模板表';

/*===================================================================*/
/* Table: 打印模板页面关联表(printer_template_relation_page)               */
/*===================================================================*/
create table if not exists printer_template_relation_page (
    id BIGINT NOT NULL COMMENT '主键id',
    template_id BIGINT NOT NULL COMMENT '模板编号',
    page_id VARCHAR(128) NOT NULL COMMENT '页面编号',
    model_code VARCHAR(128) NOT NULL COMMENT '模型编号',
    creator VARCHAR(32) DEFAULT NULL COMMENT '创建者',
    modifier VARCHAR(32) DEFAULT NULL COMMENT '修改者',
    create_time TIMESTAMP COMMENT '创建时间',
    modify_time TIMESTAMP COMMENT '修改时间',
    create_staff_id bigint(20) default null comment '创建者人员id',
    modify_staff_id bigint(20) default null comment '修改者人员id',
    PRIMARY KEY (id)
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8
COMMENT='打印模板页面关联表';

## 1.0.2
alter table printer_log modify create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
alter table printer_log modify modify_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

alter table printer_object_iframe modify create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
alter table printer_object_iframe modify modify_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

alter table printer_design_content modify create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
alter table printer_design_content modify modify_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

alter table printer_register modify create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
alter table printer_register modify modify_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

alter table printer_template modify create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
alter table printer_template modify modify_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

alter table printer_label modify create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
alter table printer_label modify modify_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

alter table printer_template_relation_page modify create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
alter table printer_template_relation_page modify modify_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;