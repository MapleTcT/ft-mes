package com.supcon.supfusion.rbac.dao.field;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.LogicDeleteBaseEntity;
import com.supcon.supfusion.rbac.dao.po.MenuOperateCodeUrlRefPO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * <p>
 * 操作表
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-05
 */
public class MenuOperateField extends LogicDeleteBaseEntityField{


    /**
     * 主键ID
     */
    public static String id="ID";

    /**
     * 版本
     */
    public static String version="ROW_VERSION";


    /**
     * 公司ID
     */
    public static String cid="CID";

    /**
     * 是否允许委托
     */
    public static String isAllowProxy="IS_ALLOW_PROXY";

    /**
     * 是否隐藏
     */
    public static String isHidden="IS_HIDDEN";

    /**
     * 是否三员菜单
     */
    public static String threeRole="THREE_ROLE";

    /**
     * 视图编码
     */
    public static String viewCode="VIEW_CODE";

    /**
     * 是否查询操作
     */
    public static String isQuery="IS_QUERY";

    /**
     * 该操作的其他数据限制跟岗位限制等的或.与关系标志位,默认为AND
     */
    public static String isOrrelation="IS_ORRELATION";

    /**
     * 启用数据权限
     */
    public static String enableDataPermission="ENABLE_DATAPERMISSION";

    /**
     * 启用自定义权限
     */
    public static String enableCustomPermission="ENABLE_CUSTOMPERMISSION";


    /**
     * 启用业务权限
     */
    public static String forFlowPermission="FOR_FLOW_PERMISSION" ;

    /**
     * 无限制
     */
    public static String enableNorestrict="ENABLE_NORESTRICT";

    /**
     * 启用处理人
     */
    public static String enableDealerpermission="ENABLE_DEALERPERMISSION";

    /**
     * 启用指定人员
     */
    public static String enableAssignstaff="ENABLE_ASSIGNSTAFF";

    /**
     * 启用指定岗位
     */
    public static String enableAssignpos="ENABLE_ASSIGNPOS";

    /**
     * 岗位限制
     */
    public static String enablePosrestrict="ENABLE_POSRESTRICT";

    /**
     * 指定部门
     */
    public static String enableAssignDept="ENABLE_ASSIGNDEPT";

    /**
     * 部门限制
     */
    public static String enableDeptrict="ENABLE_DEPTRICT";

    /**
     * 启用组限制
     */
    public static String enableGrouprestrict="ENABLE_GROUPRESTRICT";

    /**
     * 实体编码
     */
    public static String entityCode="ENTITY_CODE";

    /**
     * 忽视权限
     */
    public static String ignorePermission="IGNORE_PERMISSION";

    /**
     * 是否是主列表视图的查询操作
     */
    public static String powerFlag="POWER_FLAG";

    /**
     * 工作流版本
     */
    public static String flowVersion="FLOW_VERSION";

    /**
     * 工作流KEY
     */
    public static String flowKey="FLOW_KEY";

    public static String msgAssembled="MSG_ASSEMBLED";

    public static String deploymentId="DEPLOYMENT_ID";

    public static String menuOperateType="MENUOPERATETYPE";

    /**
     * 菜单ID
     */
    public static String menuinfoId="MENUINFO_ID";

    /**
     * ID
     */
    public static String iconCls="ICON_CLS";


    /**
     * 排序
     */
    public static String sort="SORT";

    /**
     * 备注
     */
    public static String memo="MEMO";

    /**
     * 打开方式
     */
    public static String target="TARGET";

    /**
     * ACTION
     */
    public static String action="ACTION_URL";

    /**
     * 命名空间
     */
    public static String namespace="NAMESPACE";

    /**
     * 地址
     */
    public static String url="URL";

    /**
     * 中文名
     */
    public static String nameZhCn="NAME_ZH_CN";

    /**
     * 名称
     */
    public static String name="NAME";

    /**
     * 编码
     */
    public static String code="CODE";

    /**
     * 所属应用名
     */
    public static String app="APP";

    /**
     * 默认操作标识，默认操作不可删除
     */
    public static String defaultOperate="DEFAULT_OPERATE";

    /**
     * 名称国际化值
     */
    public static String nameDisplay="NAME_DISPLAY";


}
