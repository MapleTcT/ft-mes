## 1.0.0
/*==============================================================*/
/* Table: 系统主题表(system_theme)                */
/*==============================================================*/
create table system_theme (
    id BIGINT NOT NULL, --COMMENT '主键ID'
    row_version BIGINT DEFAULT 0, --COMMENT '版本号'
    theme VARCHAR(500) NOT NULL, --COMMENT '主题'
    logo VARCHAR(500) DEFAULT NULL, --COMMENT '标志'
    font TINYINT DEFAULT 12, --COMMENT '字体大小, 12表示标准字体, 14表示大字体'
    status TINYINT DEFAULT 0 NULL, --COMMENT '状态, 0表示不启用, 1表示启用'
    creator VARCHAR(200) DEFAULT NULL, --COMMENT '创建者'
    modifier VARCHAR(200) DEFAULT NULL, --COMMENT '修改者'
    create_time datetime, --COMMENT '创建时间'
    modify_time datetime, --COMMENT '修改时间'
    PRIMARY KEY (id)
);

/*==============================================================*/
/* Table: 个人主题设置表(personal_theme)                */
/*==============================================================*/
create table personal_theme (
    id BIGINT NOT NULL, --COMMENT '主键ID'
    row_version BIGINT DEFAULT 0, --COMMENT '版本号'
    user_id BIGINT DEFAULT NULL, --COMMENT '用户ID'
    theme VARCHAR(500) NOT NULL, --COMMENT '主题'
    font TINYINT DEFAULT 12, --COMMENT '字体大小'
    status TINYINT DEFAULT 0 NULL, --COMMENT '状态, 0表示不启用, 1表示启用'
    type VARCHAR(500) NOT NULL, --COMMENT '类型'
    creator VARCHAR(200) DEFAULT NULL, --COMMENT '创建者'
    modifier VARCHAR(200) DEFAULT NULL, --COMMENT '修改者'
    create_time datetime, --COMMENT '创建时间'
    modify_time datetime, --COMMENT '修改时间'
    PRIMARY KEY (id)
);

insert into system_theme(id,theme,logo,font,status) select 1000,'default','/theme/logo/logo.png',12,1 where not exists(select theme from system_theme where theme='default');
insert into system_theme(id,theme,logo,font,status) select 1001,'dark','/theme/logo/logo.png',12,0 where not exists(select theme from system_theme where theme='dark');