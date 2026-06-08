package com.supcon.supfusion.rbac.dao.field;

/**
 * <p>
 * 角色表
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-05
 */
public class RoleField{


    /**
     * 主键ID
     */
    public static String id="ID";


    /**
     * 公司ID
     */
    public static String cid="CID";

    /**
     * 是否叶子
     */
    public static String leaf="LEAF";

    /**
     * 层级全路径
     */
    public static String fullPathName="FULL_PATH_NAME";

    /**
     * 上级节点ID
     */
    public static String parentId="PARENT_ID";

    /**
     * 层级
     */
    public static String layNo="LAY_NO";

    /**
     * 层级结构
     */
    public static String layRec="LAY_REC";

    /**
     * 用于软件公司同步接口
     */
    public static String uuid="UUID";

    /**
     * 三员类型:1系统管理员,2安全保密员 ,3安全审计员
     */
    public static String threeRoleType="THREE_ROLE_TYPE";

    /**
     * 角色类型
     * SystemCode:
     *  ROLE_TYPE/roletype 1 默认公司
     */
    public static String roleType="ROLE_TYPE";

    /**
     * 排序
     */
    public static String sort="SORT";

    /**
     * 描述
     */
    public static String description="DESCRIPTION";

    /**
     * 名称
     */
    public static String name="NAME";

    /**
     * 编码
     */
    public static String code="CODE";

    /**
     * 版本
     */
    public static String version="VERSION";
}
