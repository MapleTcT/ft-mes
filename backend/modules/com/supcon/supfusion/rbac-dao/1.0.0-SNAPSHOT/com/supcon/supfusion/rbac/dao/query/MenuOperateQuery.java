package com.supcon.supfusion.rbac.dao.query;

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
@Data
public class MenuOperateQuery extends LogicDeleteBaseEntityQuery{


    /**
     * 主键ID
     */
    private Long id;

    /**
     * 版本
     */
    private Integer version;


    /**
     * 公司ID
     */
    private Long cid;

    /**
     * 是否允许委托
     */
    private Boolean isAllowProxy;

    /**
     * 是否隐藏
     */
    private Boolean isHidden;

    /**
     * 是否三员菜单
     */
    private Boolean threeRole;

    /**
     * 视图编码
     */
    private String viewCode;

    /**
     * 是否查询操作
     */
    private Boolean isQuery;

    /**
     * 该操作的其他数据限制跟岗位限制等的或.与关系标志位,默认为AND
     */
    private Boolean isOrrelation;

    /**
     * 启用数据权限
     */
    private Boolean enableDataPermission;

    /**
     * 启用自定义权限
     */
    private Boolean enableCustomPermission;


    /**
     * 启用业务权限
     */
    private Boolean forFlowPermission ;

    /**
     * 无限制
     */
    private Boolean enableNorestrict;

    /**
     * 启用处理人
     */
    private Boolean enableDealerpermission;

    /**
     * 启用指定人员
     */
    private Boolean enableAssignstaff;

    /**
     * 启用指定岗位
     */
    private Boolean enableAssignpos;

    /**
     * 岗位限制
     */
    private Boolean enablePosrestrict;

    /**
     * 指定部门
     */
    private Boolean enableAssignDept;

    /**
     * 部门限制
     */
    private Boolean enableDeptrict;

    /**
     * 启用组限制
     */
    private Boolean enableGrouprestrict;

    /**
     * 实体编码
     */
    private String entityCode;

    /**
     * 忽视权限
     */
    private Boolean ignorePermission;

    /**
     * 是否是主列表视图的查询操作
     */
    private Boolean powerFlag;

    /**
     * 工作流版本
     */
    private String flowVersion;

    /**
     * 工作流KEY
     */
    private String flowKey;

    private Integer msgAssembled;

    private Long deploymentId;

    private String menuOperateType;

    /**
     * 菜单ID
     */
    private Long menuinfoId;

    /**
     * ID
     */
    private String iconCls;


    /**
     * 排序
     */
    private Double sort;

    /**
     * 备注
     */
    private String memo;

    /**
     * 打开方式
     */
    private String target;

    /**
     * ACTION
     */
    private String action;

    /**
     * 命名空间
     */
    private String namespace;

    /**
     * 地址
     */
    private String url;

    /**
     * 中文名
     */
    private String nameZhCn;

    /**
     * 名称
     */
    private String name;

    /**
     * 编码
     */
    private String code;

    /**
     * 所属应用名
     */
    private String app;

    /**
     * 默认操作标识，默认操作不可删除
     */
    private Boolean defaultOperate;

    /**
     * 名称国际化值
     */
    private String nameDisplay;

    private Long userId;

    private List<String> operateCodes;

    private List<Long> menuInfoIds;

    private Boolean assign;

    private Integer purviewType;

    private Long roleId;

    private String menuInfoCode;

    private List<Long> cids;

    private List<String> codes;
}
