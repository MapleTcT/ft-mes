## 1.0.0
-- 添加电子签名描述字段
alter table "EC_BUTTON" add(SIGNATURE_DESCRIBLE VARCHAR2(512 CHAR));

alter table "PROJECT_BUTTON" add(SIGNATURE_DESCRIBLE VARCHAR2(512 CHAR));

alter table "RUNTIME_BUTTON" add(SIGNATURE_DESCRIBLE VARCHAR2(512 CHAR));

CREATE TABLE  SIGNATURE_EXCEL  (
     id              number(20,0)       NOT NULL primary key,         --  '主键ID',
     status          number(11,0)       NOT NULL ,         --  '导入/导出状态,1进行中, 2成功, 3失败',
     file_name       varchar(255)       NOT NULL ,         --  '导入/导出文件名',
     error_message   varchar(255)       DEFAULT NULL ,         --  '错误消息',
     operate_type            varchar(100)       NOT NULL ,         --  '类型,import 或 export',
     valid           number(1,0)        DEFAULT 1 ,         --  '逻辑删除,是否有效0:无效 1:有效',
     creator         varchar(32)        DEFAULT NULL ,         --  '创建者',
     modifier        varchar(32)        DEFAULT NULL ,         --  '修改者',
     create_time     timestamp           DEFAULT sysdate ,         --  '创建时间',
     modify_time     timestamp           DEFAULT NULL         --  '修改时间',
) ;