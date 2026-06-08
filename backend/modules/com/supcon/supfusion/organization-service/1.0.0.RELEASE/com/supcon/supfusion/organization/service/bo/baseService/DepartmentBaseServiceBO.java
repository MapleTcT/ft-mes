package com.supcon.supfusion.organization.service.bo.baseService;

import com.alibaba.fastjson.annotation.JSONField;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;
import lombok.*;

/**
 * 部门PO类
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentBaseServiceBO {

    /**
     * 部门id
     */
    @JSONField(name = "id")
    private Long id;
    /**
     * 部门编码
     */
    @JSONField(name = "code")
    private String code;

    /**
     * 部门名称
     */
    @JSONField(name = "name")
    private String name;

    /**
     * 老版本的别名name
     */
    @JSONField(serialize = false)
    private String oldId;
    /**
     * 部门类型
     */
    @TableField(value = "dept_type")
    private String type;

    /**
     * 所属公司id
     */
    @JSONField(name = "cid")
    private Long companyId;

    /**
     * 上级部门id（如果上级是公司则为空）
     */
    @JSONField(name = "parentId")
    private Long parentId;

    /**
     * 描述
     */
    @JSONField(name = "description")
    private String description;


    /**
     * 部门层级
     */
    @JSONField(name = "layNo")
    private Integer layNo;

    /**
     * 部门全路径
     */
    @JSONField(name = "fullPathName")
    private String fullPath;

    /**
     * 部门id全路径
     */
    @JSONField(name = "layRec")
    private String layRec;

    /**
     * 顺序
     */
    @JSONField(name = "sort")
    private Double sort;

    /**
     * 是否是叶子节点０不是，１是
     */
    @JSONField(name = "leaf")
    private Boolean leaf;

    @JSONField(name = "valid")
    private Boolean valid;
}
