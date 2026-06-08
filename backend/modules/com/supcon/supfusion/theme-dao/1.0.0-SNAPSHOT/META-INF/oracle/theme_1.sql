## 1.0.0
/*==============================================================*/
/* Table: 系统主题表(system_theme)                */
/*==============================================================*/
create table system_theme (
    id number(20,0) NOT NULL, --COMMENT '主键ID'
    row_version number(20,0) DEFAULT 0, --COMMENT '版本号'
    theme VARCHAR2(500) NOT NULL, --COMMENT '主题'
    logo VARCHAR2(500) DEFAULT NULL, --COMMENT '标志'
    font number(3,0) DEFAULT 12, --COMMENT '字体大小, 12表示标准字体, 16表示大字体'
    status number(3,0) DEFAULT 0, --COMMENT '状态, 0表示不启用, 1表示启用'
    creator VARCHAR2(200) DEFAULT NULL, --COMMENT '创建者'
    modifier VARCHAR2(200) DEFAULT NULL, --COMMENT '修改者'
    create_time timestamp, --COMMENT '创建时间'
    modify_time timestamp, --COMMENT '修改时间'
    PRIMARY KEY (id)
);

/*==============================================================*/
/* Table: 个人主题设置表(personal_theme)                */
/*==============================================================*/
create table personal_theme (
    id number(20,0) NOT NULL, --COMMENT '主键ID'
    row_version number(20,0) DEFAULT 0, --COMMENT '版本号'
    user_id number(20,0) DEFAULT NULL, --COMMENT '用户ID'
    theme VARCHAR2(500) NOT NULL, --COMMENT '主题'
    font number(3,0) DEFAULT 12, --COMMENT '字体大小'
    status number(3,0) DEFAULT 0, --COMMENT '状态, 0表示不启用, 1表示启用'
    type VARCHAR2(500) NOT NULL, --COMMENT '类型'
    creator VARCHAR2(200) DEFAULT NULL, --COMMENT '创建者'
    modifier VARCHAR2(200) DEFAULT NULL, --COMMENT '修改者'
    create_time timestamp, --COMMENT '创建时间'
    modify_time timestamp, --COMMENT '修改时间'
    PRIMARY KEY (id)
);

insert into system_theme(id,theme,logo,font,status) values(1000,'default','/theme/logo/logo.png',12,0);
insert into system_theme(id,theme,logo,font,status) values(1001,'dark','/theme/logo/logo.png',12,1);