## 1.0.0
/*===================================================================*/
/* Table: 授权信息表(license_info)                */
/*===================================================================*/
create TABLE if NOT EXISTS license_info (
    id              bigint(20)      not null        comment '主键ID',
    module_code  varchar(200)      not null        comment '模块code',
    license_key    varchar(256)    not null        comment '授权key',
    value      varchar(50)     not null        comment '授权值  0已授权 -1未授权 -2未授权 但在试用期 可用',
    description       varchar(256)    default null        comment '描述',
    application_name       varchar(256)    default null        comment '模块名称',
    application_type       varchar(256)    default null        comment '模块型号',
    time   varchar(256)    not null    comment 'APP启动时的时间',
    hash_code varchar(256)    not null    comment 'moduleCode licenseKey value time salt md5加密后',
    valid           tinyint(1)      default 1       comment '是否删除 0 删除 1 未删除',
    creator         varchar(32)     default null    comment '创建者',
    modifier        varchar(32)     default null    comment '修改者',
    create_time     timestamp       not null default current_timestamp	comment '创建时间',
    modify_time     timestamp       not null default current_timestamp  comment '修改时间',
    create_staff_id bigint(20)      default null comment '创建者人员id',
    modify_staff_id bigint(20)      default null comment '修改者人员id',
     primary key (id),
    unique(module_code)
) ENGINE = InnoDB  DEFAULT CHARSET = utf8 COMMENT ='授权信息表';

## 1.0.1
alter table license_info modify modify_time timestamp not null default current_timestamp;