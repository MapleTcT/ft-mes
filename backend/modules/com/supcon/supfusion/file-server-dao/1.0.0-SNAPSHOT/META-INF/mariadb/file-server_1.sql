## 1.0.0
-- file_server_document definition
 create table file_server_document(
    id bigint(20) not null comment '主键',
    file_path varchar(1024) not null comment '路径',
    file_name varchar(255) not null comment '名称',
    link_id bigint(20) not null comment '对应表单id',
    file_org_type varchar(1024)  null comment 'linkid 辅助字段',
    file_size bigint(20) not null comment '大小',
    file_type varchar(510)  null comment '文件类型   pic：图片字段 attachment:普通附件  office:文档控件',
    main_model_id bigint(20)  null comment '主关联模型的id，如是表单 则为tableinfoid',
    size_dis varchar(256)  null comment '记录类型',
    memo varchar(510)  null comment '备注',
    property_code varchar(510) null comment '字段code',
    show_type varchar(510)  null comment '显示类型',
    opener varchar(510)  null comment '首次打开用户',
    open_time timestamp null comment '上次打开时间',
    deployment_id bigint(20)  null comment '流程id',
    activity_name varchar(510)  null comment '活动名称',
    task_description varchar(510)  null comment '活动描述',
    file_icon varchar(510)  null comment '记录类型',
    is_file_view tinyint(1) default 0 comment '是否启用附件预览  0 否',
    doc_content varchar(510)  null comment '',
    doc_summary varchar(510)  null comment '',
    convert_status varchar(510)  null comment '附件转换状态',
    reason varchar(510)  null comment '不支持转换的原因',
    convert_path varchar(510)  null comment '转换后的文件路径',
    download_times bigint(20)  null comment '下载次数',
    preview_times bigint(20)  null comment '预览时间',
    `version` bigint(20) default 0,
    valid  tinyint(1) default 1 comment '逻辑删除,是否有效0:无效 1:有效',
    creator  varchar(32) default 'system' comment '创建者',
    create_time  timestamp not null default current_timestamp comment '创建时间',
    create_staff_id  bigint(20)  default 0 comment '创建者人员id',
    modifier  varchar(32)  null comment '修改者',
    modify_time  timestamp not null default current_timestamp comment '修改时间',
    modify_staff_id  bigint(20)  null comment '修改者人员id',
    delete_staff_id bigint(20)  null comment '删除人员id',
    delete_time timestamp  null comment '删除时间',
 	 primary key (id)
)engine=innodb default charset=utf8 comment='附件表';

-- FILE_SERVER_DOCUMENT_DOWN_INFO definition
create table file_server_document_down_info (
   id  bigint(20) not null comment '主键id',
   document_id   bigint(20) not null comment '附件id',
   record_type  varchar(255) not null comment '记录类型',
   ip_addr  varchar(255) default null comment '下载ip',
   download_time  timestamp comment '下载时间',
   download_staff_id  bigint(20)  null comment '下载人',
   `version` bigint(20) default 0,
   valid  tinyint(1) default 1 comment '逻辑删除,是否有效0:无效 1:有效',
   creator  varchar(32) default 'system' comment '创建者',
   create_time  timestamp not null default current_timestamp comment '创建时间',
   create_staff_id  bigint(20)  default 0 comment '创建者人员id',
   modifier  varchar(32)  null comment '修改者',
   modify_time  timestamp not null  default current_timestamp comment '修改时间',
   modify_staff_id  bigint(20)  null comment '修改者人员id',
   delete_time timestamp null comment '删除时间',
   primary key ( id )
) engine=innodb default charset=utf8 comment='附件下载信息';

## 1.0.1
select id from file_server_document;

## 1.0.2
alter table file_server_document add tenant_id varchar(256) comment '租户id';
update file_server_document set tenant_id='dtbucket' where tenant_id is null;

## 1.0.3
alter table file_server_document drop column tenant_id;

## 1.0.4
alter table file_server_document alter column version set default 0;
alter table file_server_document_down_info alter column version set default 0;
update file_server_document set version = 0 where version is null ;
update file_server_document_down_info set version = 0 where version is null ;