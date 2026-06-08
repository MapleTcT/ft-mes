package com.supcon.supfusion.configuration.services.openapi.vo;

import com.supcon.supfusion.base.entities.Company;
import com.supcon.supfusion.base.entities.MenuInfo;
import com.supcon.supfusion.base.enums.MenuOperateType;
import com.supcon.supfusion.base.enums.OperateTarget;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel(value = "MenuOperateVO", description = "MenuOperateVO")
public class MenuOperateVO extends VO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private int version;
    private String code; // 编码
    private String name; // 名称
    private String url; // 程序URL
    private String namespace;
    private String action;
    private String memo;// 备注
    private Integer sort = 0;// 排序
    private String module;// 模块信息：bundle的SymbolicName+Version组成
    private String iconCls;// 小图标
    private MenuInfo menuInfo;
    private MenuOperateType menuOperateType;
    private Long deploymentId;
    private Integer msgAssembled;
    private String flowKey;
    private String flowVersion;
    private String flowName;
    private Boolean powerFlag = false;
    private String entityCode;
    private String moduleCode;
    private Boolean ignorePermission = false;
    private Boolean isAllowProxy = true;//是否允许委托
    private OperateTarget target;
    /**
     * true时  权限管理中强制隐藏
     */
    private Boolean isHidden = false;
    /**
     * 启用菜单操作的限制方式
     *
     * @author tangjie
     */
    private Boolean enableGroupRestrict = false;
    private Boolean enablePosRestrict = false;
    private Boolean enableAssignPos = false;
    private Boolean enableAssignStaff = false;
    private Boolean enableDealerPermission = false;
    private Boolean enableNoRestrict = true;
    private Boolean forDataPermission = false;
    private Boolean enableOtherRestrict = false;
    private Boolean enableSpecialPermission = false;
    //该操作的其他数据限制跟岗位限制等的或.与关系标志位,默认为AND
    private Boolean isOrRelation = false;
    private String viewCode;
    private Boolean isQuery = false;
    private Boolean threeRole = false;
    private Company company;
    private Long cid;
}
