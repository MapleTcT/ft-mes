package com.supcon.supfusion.organization.service.bo.baseService;

import com.alibaba.fastjson.annotation.JSONField;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;
import lombok.*;

/**
 * 岗位PO类
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PositionBaseServiceBO {


    /**
     * 岗位id
     */
    @JSONField(name = "id")
    private Long id;
    /**
     * 岗位编码
     */
    @JSONField(name = "code")
    private String code;

    /**
     * 岗位名称
     */
    @JSONField(name = "name")
    private String name;

    /**
     * 对应老版本的name别名
     */
    @JSONField(serialize = false)
    private String oldId;


    /**
     * 所属公司id
     */
    @JSONField(name = "cid")
    private Long companyId;

    /**
     * 关联的部门id
     */
    //@JSONField(serialize = false)
    @JSONField(name = "depId")
    private Long depId;

    /**
     * 上级岗位id（如果上级是公司则为空）
     */
    @JSONField(name = "parentId")
    private Long parentId;

    /**
     * 描述
     */
    @JSONField(name = "description")
    private String description;

    /**
     * 岗位层级
     */
    @JSONField(name = "layNo")
    private Integer layNo;

    /**
     * 岗位全路径
     */
    @JSONField(name = "fullPathName")
    private String fullPath;

    /**
     * 岗位id全路径
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
    private Integer valid;
}
