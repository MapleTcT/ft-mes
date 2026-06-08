package com.supcon.supfusion.auth.service.bo.bap;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * bap部门
 *
 * @author caokele
 */
@Data
@Accessors(chain = true)
public class BapDepartmentBO {
    /**
     * 主键id
     */
    private Long id;
    /**
     * 版本号
     */
    private Integer version = 0;
    /**
     * 编码
     */
    private String code;
    /**
     * 名称
     */
    private String name;
    /**
     * 描述
     */
    private String description;
    /**
     * 名称全路径，以'/'分隔
     */
    private String fullPathName;
    /**
     * 层级
     */
    private Integer layNo;
    /**
     * id全路径，以'-'分隔
     */
    private String layRec;
    /**
     * 公司id
     */
    private Long cid;
    /**
     * 公司对象
     */
    private BapCompanyBO company;
    /**
     * 人员对象
     */
    private BapStaffBO manager;
    /**
     * 上级部门id
     */
    private Long parentId;
    /**
     * 是否有效
     */
    private Boolean valid;
    /**
     * 是否叶子节点
     */
    private Boolean leaf;
}
