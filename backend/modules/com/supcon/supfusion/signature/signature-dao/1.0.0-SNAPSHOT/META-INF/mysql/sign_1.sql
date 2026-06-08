## 1.0.0
-- 添加电子签名描述字段
alter table "EC_BUTTON" add COLUMN  SIGNATURE_DESCRIBLE VARCHAR(512);

alter table "PROJECT_BUTTON" add COLUMN  SIGNATURE_DESCRIBLE VARCHAR(512);

alter table "RUNTIME_BUTTON" add COLUMN  SIGNATURE_DESCRIBLE VARCHAR(512);


CREATE TABLE signature_excel (
   id  bigint(20) NOT NULL COMMENT '主键ID',
   status  int NOT NULL COMMENT '导入/导出状态,1进行中, 2成功, 3失败',
   file_name  varchar(255) NOT NULL COMMENT '导入/导出文件名',
   error_message  varchar(255) DEFAULT NULL COMMENT '错误消息',
   operate_type  varchar(100) NOT NULL COMMENT '类型,import 或 export',
   valid  tinyint(1) DEFAULT 1 COMMENT '逻辑删除,是否有效0:无效 1:有效',
   creator  varchar(32) DEFAULT NULL COMMENT '创建者',
   modifier  varchar(32) DEFAULT NULL COMMENT '修改者',
   create_time  timestamp  NULL COMMENT '创建时间',
   modify_time  timestamp  NULL COMMENT '修改时间',
   PRIMARY KEY ( id )
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='execl导入导出状态实体类';