## 1.0.0
/*===================================================================*/
/* table: 配置项         sqlservice       */
/*===================================================================*/
-- 创建systemconfig_config_info表，创建索引（app_code,code）
create table systemconfig_config_info(
    id              bigint                     not null primary key,      --主键
    catalog_id      bigint                     default null ,    -- 配置中心分类ID
    sort            decimal(20,6)              default null ,    -- 顺序
    code            varchar(256)               default null ,    -- 业务唯一性标识
    name            varchar(256)               default null ,    -- 国际化编码
    app_code        varchar(256)               default null ,    -- APP标识
    module_code     varchar(256)               default null ,    -- 模块code
    widget_type     tinyint                    default null ,    -- 控件类型(0:默认输入框,1:单选checkbox,2:单选radio,3:下拉多选,4:下拉单选,5:时间框,6:自定义控件)
    default_value   varchar(4000)              default null ,    -- 缺省值
    widget_value    varchar(4000)              default null ,    -- 值
    max_value       int                        default null ,    -- 最大值
    min_value       int                        default null ,    -- 最小值
    reg_format      varchar(256)               default null ,    -- 格式校验
    reg_message     varchar(256)               default null ,    -- 提示语
    has_require     tinyint                    default null ,    -- 是否必填
    custom          varchar(256)               default null ,    -- json字符串  保存自定义控件渲染内容
    description     varchar (256)              default null ,    -- 配置项说明
    creator         varchar(256)               not null default 'system',
    create_time     datetime                   not null default getutcdate(),
    modifier        varchar(256)               default null,
    modify_time     datetime                   null,
    create_staff_id bigint                     default null, -- 创建者人员id,
    modify_staff_id bigint                     default null -- 修改者人员id,
) ;
-- 创建索引
 create unique index udx_sysconf_code on systemconfig_config_info (app_code,code);

insert into systemconfig_config_info(id, catalog_id, sort, code, name, app_code, module_code, widget_type, default_value, widget_value, max_value, min_value, reg_format, reg_message, has_require, custom, description )
values(1, 4, 1, 'spreadjs.licence', 'spreadjs.licence', 'printer', 'printer', 7, '', '', null, null, null, null, 1, null, '');

alter table systemconfig_config_info alter column widget_value varchar(4000) null;
alter table systemconfig_config_info alter column default_value varchar(4000) null;

create table  systemconfig_config_version (
    id              bigint                     not null primary key ,  -- 主键
    config_version  varchar(256)         	   not null ,          -- 配置版本
    tid_module_key  varchar(256)        	   not null,           -- 租户模块key
    creator         varchar(256)               not null default 'system',
    create_time     datetime                   not null default getutcdate(),
    modifier        varchar(256)               default null,
    modify_time     datetime                   null,
    create_staff_id bigint                     default null, -- 创建者人员id,
    modify_staff_id bigint                     default null -- 修改者人员id,
);

-- 创建systemconfig_config_option表
create table  systemconfig_config_option(
    id              bigint                     not null primary key,        -- 主键
    config_id       bigint                     default null ,      -- 系统配置ID
    sort            decimal(20,0)              default null ,      -- 顺序
    label           varchar(256)               default null ,      -- 国际化编码
    select_value    varchar(256)               default null ,      -- 值
    creator         varchar(256)               not null default 'system',
    create_time     datetime                   not null default getutcdate(),
    modifier        varchar(256)               default null,
    modify_time     datetime                   null,
    create_staff_id bigint                     default null, -- 创建者人员id,
    modify_staff_id bigint                     default null -- 修改者人员id,
) ;

-- 创建systemconfig_config_catalog表 ，创建索引（app_code,code）
create table  systemconfig_config_catalog(
    id              bigint                     not null primary key,        -- 主键
    parent_id       bigint                     default null ,        -- 父配置ID
    sort            decimal(20,0)              default null ,        -- 顺序
    code            varchar(256)               default null ,        -- 业务唯一性标识
    name            varchar(256)               default null ,        -- 国际化编码
    app_code        varchar(256)               default null ,        -- APP标识
    catalog_type    tinyint                    default null ,        -- 0:系统配置，1：app配置
    has_hide        tinyint                    default 0,
    creator         varchar(256)               not null default 'system',
    create_time     datetime                   not null default getutcdate(),
    modifier        varchar(256)               default null,
    modify_time     datetime                   null,
    create_staff_id bigint                     default null, -- 创建者人员id,
    modify_staff_id bigint                     default null -- 修改者人员id,
);
-- 创建索引
create unique index udx_sysconf_code on systemconfig_config_catalog (app_code,code);

-- 初始化数据 插入前先判断是否存在
insert into systemconfig_config_catalog(id,sort,code,name,app_code,catalog_type,has_hide) values(1, 1,'system','systemConfig.system','system',0,0);
insert into systemconfig_config_catalog(id,sort,code,name,app_code,catalog_type,has_hide) values (2,2,'app','systemConfig.app','app',0,0) ;
insert into systemconfig_config_catalog(id,sort,code,parent_id,name,app_code,catalog_type,has_hide) values(3, 2,'userAD',1,'systemConfig.userAD','system',1,0);
insert into systemconfig_config_catalog(id,sort,code,parent_id,name,app_code,catalog_type,has_hide) values(4, 3,'printer',1,'systemConfig.printer','printer',1,0);
insert into systemconfig_config_catalog(id,sort,code,parent_id,name,app_code,catalog_type,has_hide) values(5, 4,'AKSK',1,'systemConfig.AKSK','AKSK',1,0);
insert into systemconfig_config_catalog(id,sort,code,parent_id,name,app_code,catalog_type,has_hide) values(6, 5,'baseImages',1,'systemConfig.baseImages','baseImages',1,0);
insert into systemconfig_config_catalog(id,sort,code,parent_id,name,app_code,catalog_type,has_hide) values(7, 6,'passwordConfig',1,'systemConfig.passwordConfig','passwordConfig',1,0);

## 1.0.1
update systemconfig_config_info set default_value='192.*|10.*,615382821284924#B0vEDu3Wex5Wd6hFWLB5Z5U4KpRTUpFGSXdkMwFGOlJDcXZ4Mv3SSEJXOv96KHhmR7s6V8oGarkmRI5UZ7xUUxdGUPVXV8l4MFVmehlkQSJ6THN6Q7RDSSRkSPtSYWl7TxJWSyAjN0BDe9EXc4cUbDd5czhGSYBDNXZDMhh7N8cDWuNVTol7MOpENGBzT0RkRx56YxEjMVtUY5Nja424RhpFMNNFNtpnRlpnaOFDND56TrYVNZVnU6Z5RzZWT6AjVwlGcPFHRWpHMMtEWxc6blFnV8I4LsV7c0NVQPhTO9gHMDp6a5hjUrUXVWhzaF36ST5kVHVmI0IyUiwiI7QzMFZkRyEjI0ICSiwiM6gDM8MTM9ETM0IicfJye&Qf35VfiU5TzYjI0IyQiwiIzEjL6ByUKBCZhVmcwNlI0IiTis7W0ICZyBlIsIyMyIzMxADIxMjMxAjMwIjI0ICdyNkIsIiKuATMsoiLykTMiojIz5GRiwiI8+Y9sWY9QmZ0Jyp93uL9hKI0vyp9Aqo9n0o9tiL9fGr9ZWr9iojIh94QiwiI4ITO4gjMxIDOygzM5EjNiojIklkIs4XZzxWYmpjIyNHZisnOiwmbBJye0ICRiwiI34TQwh7QV94bwVEbE3URFJlUDB7dXJ5MB9Wd9Q4M854U7YGUBNDdwh5UPVVb4InUshzMI3ya6IGUYVFOzlnWrgWRlZGezolYhZEZQJTZ58mezk7R7JTQjxWWBx4cZN7LLwFjn'
,description='默认授权key只适用于192/10网段，如果其它网段的需求，请独立申请。' where id=1;