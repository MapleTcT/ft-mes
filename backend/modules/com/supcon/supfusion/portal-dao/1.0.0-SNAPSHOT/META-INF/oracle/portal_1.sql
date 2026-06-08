## 1.0.0
/*====================================================*/
/* TABLE: 门户信息表                                       */
/*====================================================*/
CREATE TABLE EC_PORTLET (
    CODE VARCHAR(510) NOT NULL,
    MEMO VARCHAR(4000),
    HEIGHT NUMBER(22,0),
    RESIZE_FUNC CLOB,
    ONLOAD_FUNC CLOB,
    IFRAME_FLAG NUMBER(22,0),
    MENU_INFO_ID NUMBER(19,0),
    MENU_OPERATE_ID NUMBER(19,0),
    MENU_CODE VARCHAR(510),
    OPERATE_CODE VARCHAR(510),
    CID NUMBER(19,0),
    MODULE_CODE VARCHAR(510),
    IS_HIDDEN NUMBER(22,0),
    POWER_FLAG NUMBER(22,0),
    SCOPE_NUM NUMBER(22,0),
    VERSION NUMBER(22,0) DEFAULT 0,
    IS_DEFAULT NUMBER(22,0),
    TITLE_COLOR VARCHAR(510),
    TITLE_KEY VARCHAR(510),
    TITLE VARCHAR(510),
    SIZE_NUM NUMBER(22,0),
    MORE_TARGET VARCHAR(510),
    MORE_URL VARCHAR(510),
    URL VARCHAR(510),
    CREATOR VARCHAR(32) DEFAULT NULL,
    MODIFIER VARCHAR(32) DEFAULT NULL,
    TERMINATOR VARCHAR(32) DEFAULT NULL,
    CREATE_TIME TIMESTAMP,
    MODIFY_TIME TIMESTAMP,
    DELETE_TIME TIMESTAMP,
    CREATE_STAFF_ID NUMBER(20, 0) ,
    MODIFY_STAFF_ID NUMBER(20, 0) ,
    PRIMARY KEY (CODE)
);
COMMENT ON TABLE EC_PORTLET IS '门户信息表';
COMMENT ON COLUMN EC_PORTLET.CODE IS '编码主键';
COMMENT ON COLUMN EC_PORTLET.CID IS '公司ID';
COMMENT ON COLUMN EC_PORTLET.MENU_INFO_ID IS '关联菜单ID';
COMMENT ON COLUMN EC_PORTLET.MENU_CODE IS '关联菜单CODE';
COMMENT ON COLUMN EC_PORTLET.HEIGHT IS '高度,IFRAMEFLAG为TRUE时有效';
COMMENT ON COLUMN EC_PORTLET.IFRAME_FLAG IS '是否适用IFRAME';
COMMENT ON COLUMN EC_PORTLET.MEMO IS '备注';
COMMENT ON COLUMN EC_PORTLET.POWER_FLAG IS '是否启用权限';
COMMENT ON COLUMN EC_PORTLET.SCOPE_NUM IS '所属范围   0所有公司   1本公司';
COMMENT ON COLUMN EC_PORTLET.CREATOR IS '创建者';
COMMENT ON COLUMN EC_PORTLET.MODIFIER IS '修改者';
COMMENT ON COLUMN EC_PORTLET.TERMINATOR IS '删除者';
COMMENT ON COLUMN EC_PORTLET.CREATE_TIME IS '创建时间';
COMMENT ON COLUMN EC_PORTLET.MODIFY_TIME IS '修改时间';
COMMENT ON COLUMN EC_PORTLET.DELETE_TIME IS '删除时间';
COMMENT ON COLUMN EC_PORTLET.CREATE_STAFF_ID IS '创建者人员ID';
COMMENT ON COLUMN EC_PORTLET.MODIFY_STAFF_ID IS '修改者人员ID';
CREATE UNIQUE INDEX SYS_C00155238 ON EC_PORTLET(CODE);

CREATE TABLE "EC_MY_PORTLET"
   (	"ID" NUMBER(19,0),
	"VERSION" NUMBER(*,0) DEFAULT 0,
	"CONFIG" CLOB,
	"USER_ID" NUMBER(19,0),
	 PRIMARY KEY ("ID") ENABLE
   ) ;

## 1.0.1
INSERT INTO EC_PORTLET
(CODE, HEIGHT, IFRAME_FLAG,POWER_FLAG, CID, MODULE_CODE, TITLE_KEY, TITLE, URL)
VALUES('myProcess', 350, 1,0, 0, 'portal','portal.homepage.myProcess','portal.homepage.myProcess','/license/#/myProcess');

INSERT INTO EC_PORTLET
(CODE, HEIGHT, IFRAME_FLAG,POWER_FLAG, CID, MODULE_CODE, TITLE_KEY, TITLE, URL)
VALUES('pendingNotice', 110, 1,0, 0, 'portal','portal.homepage.pendingNotice','portal.homepage.pendingNotice','/license/#/pendingNotice');

## 1.0.2
UPDATE EC_PORTLET SET url='/supplant/#/myProcess' WHERE CODE = 'myProcess';
UPDATE EC_PORTLET SET url='/supplant/#/pendingNotice' WHERE CODE = 'pendingNotice';

## 1.0.3
INSERT INTO EC_MY_PORTLET
(ID ,CONFIG ,USER_ID)
VALUES(1,'[{"portlets":[{"cid":0,"code":"myProcess","default":false,"hidden":false,"iframeFlag":false,"menuInfoId":0,"powerFlag":false,"scopeNum":0,"title":"我的流程","titleKey":"portal.homepage.myProcess","url":"/supplant/#/myProcess?__t__=1616033155820"}],"width":"33%"},{"portlets":[{"cid":0,"code":"pendingNotice","default":false,"hidden":false,"iframeFlag":false,"menuInfoId":0,"powerFlag":false,"scopeNum":0,"title":"待办提醒","titleKey":"portal.homepage.pendingNotice","url":"/supplant/#/pendingNotice?__t__=1616033156792"}],"width":"33%"},{"portlets":[],"width":"33%"}]',-1);
