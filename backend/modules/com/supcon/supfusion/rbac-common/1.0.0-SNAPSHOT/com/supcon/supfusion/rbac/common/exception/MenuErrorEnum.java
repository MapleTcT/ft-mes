package com.supcon.supfusion.rbac.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;

public enum MenuErrorEnum implements ErrorDefinition {

    MENU_IS_EXISTS(100105200, "菜单编码已存在，请重新输入！","rbac.EMPTY_DATA"),
    MENU_DELETE_DATA_IS_NOT_EMPTY(100105201, "请选择菜单！","rbac.MENU_DELETE_DATA_IS_NOT_EMPTY"),
    NO_AUTHORITY_EDIT(100105202, "只能修改本公司菜单！","rbac.NO_AUTHORITY_EDIT"),
    CHILD_COMPANY_MORE_THAN_PARENT(100105203, "因为子菜单适用范围比父菜单适用范围大，移动失败！","rbac.CHILD_COMPANY_MORE_THAN_PARENT"),
    ONLY_THE_CREATION_COMPANY_HAS_PERMISSION_TO_MODIFY(100105204, "只有所属公司有权限修改！","rbac.ONLY_THE_CREATION_COMPANY_HAS_PERMISSION_TO_MODIFY"),
    THE_CREATION_COMPANY_MUST_IN_COMPANYS (100105205, "适用范围必须包含所属公司！","rbac.THE_CREATION_COMPANY_MUST_IN_COMPANYS"),
    CAN_NOT_DELETE_MENU_MANAGE (100105206, "不能停用菜单配置及其父级菜单！","rbac.CAN_NOT_DELETE_MENU_MANAGE"),
    MENU_HAS_GRANT_PERMISSION (100105207, "存在菜单已被分配权限，删除失败！","rbac.MENU_HAS_GRANT_PERMISSION"),
    CANT_FIND_I18N (100105208, "国际化值找不到！","rbac.CANT_FIND_I18N"),
    CODE_UNQUALIFIED (100105209, "编码由字母、数字、小数点、下划线组成且必须以字母开头且不以小数点、下划线结尾！","rbac.CODE_UNQUALIFIED"),
    CANT_GENERATE_ROOT_MENU (100105210, "菜单配置中无法生成根节点！","rbac.CANT_GENERATE_ROOT_MENU"),
    CHILD_COMPANY_MORE_THAN_PARENT_UPDATE(100105211, "因为子菜单适用范围比父菜单适用范围大，修改失败！","rbac.CHILD_COMPANY_MORE_THAN_PARENT_UPDATE"),
    CAN_NOT_FOUND_TENANT(100105212, "未找到租户信息!", "未找到租户信息!"),
    MENUCODE_IS_EXISTS(100105213, "菜单编码已存在！","rbac.MENU_IS_EXISTS"),
    MENUCODE_NOT_FOUND(100105214, "菜单编码不存在！","rbac.MENU_NOT_FOUND"),
    CODE_CAN_NOT_EMPTY(100105215, "菜单编码不能为空!","rbac.CODE_CAN_NOT_EMPTY"),
    PARENT_CON_NOT_FIND(100105216, "父菜单找不到!","rbac.PARENT_CON_NOT_FIND"),
    ROOT_CON_NOT_REMOVE_TO_ROOT(100105217, "根目录不允许移动到根目录!","rbac.ROOT_CON_NOT_REMOVE_TO_ROOT"),
    APP_CON_NOT_REMOVE_TO_FOLDER_OR_PAGE(100105218, "APP目录不允许移动到文件夹或者页面下!","rbac.APP_CON_NOT_REMOVE_TO_FOLDER_OR_PAGE"),
    FOLDER_CON_NOT_REMOVE_TO__PAGE(100105219, "文件夹目录不允许移动到页面下!","rbac.FOLDER_CON_NOT_REMOVE_TO__PAGE"),
    MENU_NAME_DISPLAY_EMPTY(100105220, "菜单展示名为空!","rbac.MENU_NAME_DISPLAY_IS_EMPTY"),
    MENU_APP_REF_EMPTY(100105221, "未找到appid与菜单的关系!","rbac.MENU_APP_REF_EMPTY_IS_EMPTY"),
    APPID_EMPTY(100105222, "appid为空!","rbac.APPID_IS_EMPTY"),
    MENU_SOURCE_EMPTY(100105223, "菜单来源为空!","rbac.MENU_SOURCE_IS_EMPTY")
    ;

    /**
     * 异常码
     */
    private Integer code;
    /**
     * 默认异常信息
     */
    private String defaultMessage;

    private String key;

    MenuErrorEnum(Integer code, String defaultMessage,String key) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.key = key;
    }

    @Override
    public Integer getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return defaultMessage;
    }

    @Override
    public String getInfo() {
        return key;
    }

    @Override
    public String getSimpleMessage() {
        return defaultMessage;
    }
}
