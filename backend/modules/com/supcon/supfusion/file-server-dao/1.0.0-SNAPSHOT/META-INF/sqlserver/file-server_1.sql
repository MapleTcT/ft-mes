## 1.0.0
-- file_server_document definition
create table file_server_document(
    id bigint not null ,-- '主键',
 	  file_path varchar(1024) not null ,-- '路径',
 	  file_name varchar(255) not null ,-- '名称',
 	  link_id bigint not null ,-- '对应表单id',
      file_org_type varchar(1024)  null ,-- 'linkid 辅助字段',
 	  file_size bigint not null ,-- '大小',
 	  file_type varchar(510)  null ,-- '文件类型   pic：图片字段 attachment:普通附件  office:文档控件',
 	  main_model_id bigint  null ,-- '主关联模型的id，如是表单 则为tableinfoid',
 	  size_dis varchar(256)  null ,-- '记录类型',
 	  memo varchar(510)  null ,-- '备注',
 	  property_code varchar(510) null ,-- '字段code',
 	  show_type varchar(510)  null ,-- '显示类型',
 	  opener varchar(510)  null ,-- '首次打开用户',
 	  open_time datetime null ,-- '上次打开时间',
 	  deployment_id bigint  null ,-- '流程id',
 	  activity_name varchar(510)  null ,-- '活动名称',
 	  task_description varchar(510)  null ,-- '活动描述',
 	  file_icon varchar(510)  null ,-- '记录类型',
 	  is_file_view tinyint default 0 ,-- '是否启用附件预览  0 否',
 	  doc_content varchar(510)  null ,-- '',
 	  doc_summary varchar(510)  null ,-- '',
 	  convert_status varchar(510)  null ,-- '附件转换状态',
 	  reason varchar(510)  null ,-- '不支持转换的原因',
 	  convert_path varchar(510)  null ,-- '转换后的文件路径',
 	  download_times bigint  null ,-- '下载次数',
 	  preview_times bigint  null ,-- '预览时间',
      version bigint default 0,
      valid  tinyint default 1 ,-- '逻辑删除,是否有效0:无效 1:有效',
      creator  varchar(32) default 'system' ,-- '创建者',
      create_time  datetime default getutcdate() ,-- '创建时间',
      create_staff_id  bigint  default 0 ,-- '创建者人员id',
      modifier  varchar(32)  null ,-- '修改者',
      modify_time  datetime  default getutcdate() ,-- '修改时间',
      modify_staff_id  bigint  null ,-- '修改者人员id',
      delete_staff_id bigint  null ,-- '删除人员id',
      delete_time datetime  null ,-- '删除时间',
);

-- FILE_SERVER_DOCUMENT_DOWN_INFO definition
create table file_server_document_down_info (
   id  bigint not null ,-- '主键id',
   document_id   bigint not null ,-- '附件id',
   record_type  varchar(255) not null ,-- '记录类型',
   ip_addr  varchar(255) default null ,-- '下载ip',
   download_time  datetime default null ,-- '下载时间',
   download_staff_id  bigint  null ,-- '下载人',
   version bigint default 0,
   valid  tinyint default 1 ,-- '逻辑删除,是否有效0:无效 1:有效',
   creator  varchar(32) default 'system' ,-- '创建者',
   create_time  datetime default getutcdate() ,-- '创建时间',
   create_staff_id  bigint  default 0 ,-- '创建者人员id',
   modifier  varchar(32)  null ,-- '修改者',
   modify_time  datetime   default getutcdate() ,-- '修改时间',
   modify_staff_id  bigint  null ,-- '修改者人员id',
   delete_time datetime null ,-- '删除时间',
);

## 1.0.1
select id from file_server_document;

## 1.0.2
alter table file_server_document add tenant_id varchar(256);
update file_server_document set tenant_id='dtbucket' where tenant_id is null;

## 1.0.3
alter table file_server_document drop column tenant_id;

## 1.0.4
update file_server_document set version = 0 where version is null ;
update file_server_document_down_info set version = 0 where version is null ;