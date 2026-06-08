package com.supcon.supfusion.organization.service.bo.baseService;

import com.alibaba.fastjson.annotation.JSONField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;
import lombok.*;

/**
 * 公司PO类
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CompanyBaseServiceBO {

    /**
     * 公司id
     */
    @JSONField(name = "id")
    private Long id;

    /**
     * 版本号
     */
    @JSONField(name = "version")
    private Long rowVersion;

    /**
     * 公司编码
     */
    @JSONField(name = "code")
    private String code;

    /**
     * 对应旧版本的公司的name
     */
    @JSONField(serialize = false)
    private String oldId;

    /**
     * 描述
     */
    @JSONField(name = "description")
    private String description;

    /**
     * 集团或公司简称
     */
    @JSONField(name = "shortName")
    private String shortName;

    /**
     * 集团或公司全称
     */
    @JSONField(name = "name")
    private String fullName;

    /**
     * 公司全路径
     */
    @JSONField(serialize = false)
    private String fullPath;

    /**
     * 公司id全路径
     */
    @JSONField(serialize = false)
    private String layRec;

    /**
     * 集团或公司地址
     */
    @JSONField(name = "address")
    private String address;

    /**
     * 节点层级
     */
    @JSONField(serialize = false)
    private Integer layNo;

    /**
     * 同层级下节点顺序
     */
    @JSONField(name = "sort")
    private Double sort;

    /**
     * 父级节点id
     */
    @JSONField(name = "parentId")
    private Long parentId;

    /**
     * 是否有效
     */
    @JSONField(name = "valid")
    private Boolean valid = true;

}
