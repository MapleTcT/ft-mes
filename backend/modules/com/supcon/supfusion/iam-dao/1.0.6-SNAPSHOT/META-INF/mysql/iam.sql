## 1.0.0
/*==============================================================*/
/* Table: IAM_ACCOUNT                                           */
/*==============================================================*/
create table if not exists iam_account
(
   id                   bigint(20) not null comment 'ID',
   access_key           varchar(32) not null comment 'AK',
   secret_key           varchar(32) not null comment 'SK',
   username             varchar(64) not null comment '用户名，用于识别账户所有者的标识，如appid',
   description          varchar(256) comment '用户说明',
   creator              varchar(32) not null default 'system' comment '创建人',
   create_time          timestamp not null default current_timestamp comment '创建时间',
   create_staff_id      bigint(20) comment '创建者人员ID',
   modifier             varchar(32) comment '修改人',
   modify_time          timestamp not null default current_timestamp comment '修改时间',
   modify_staff_id      bigint(20) comment '修改人人员ID',
   constraint pk_account_id primary key (id),
   constraint unique index udx_account_username(username),
   constraint unique index udx_account_ak(access_key)
)
engine=InnoDB
default charset=utf8
comment='账户表';

alter table iam_account add column system tinyint default 1 comment '是否为app安装时生成，0 页面创建 1 app安装时生成' after description;
alter table iam_account add column download_mark tinyint default 0 comment '下载标志' after system;