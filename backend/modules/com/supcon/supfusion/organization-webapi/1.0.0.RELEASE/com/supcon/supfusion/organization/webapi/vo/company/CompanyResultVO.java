package com.supcon.supfusion.organization.webapi.vo.company;


import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
public class CompanyResultVO extends VO {

    /**
     * 公司id
     */
    @ApiModelProperty(value = "公司id")
    private Long id;

    /**
     * 公司编码
     */
    @ApiModelProperty(value = "编码")
    private String code;

    /**
     * 集团或公司简称
     */
    @ApiModelProperty(value = "公司简称")
    private String shortName;

    /**
     * 集团或公司全称
     */
    @ApiModelProperty(value = "公司全称")
    private String fullName;

    @ApiModelProperty(value = "公司管理员用户")
    private List<CompanyUserVO> users;


    /**
     * 公司全路径
     */
    @ApiModelProperty(value = "公司全路径")
    private String fullPath;

    /**
     * 节点层级
     */
    @ApiModelProperty(value = "公司层级")
    private Integer layNo;

    /**
     * 同层级下节点顺序
     */
    @ApiModelProperty(value = "公司顺序")
    private Double sort;

    /**
     * 父级节点id
     */
    @ApiModelProperty(value = "上级公司id")
    private Long parentId;

    /**
     * 描述
     */
    @ApiModelProperty(value = "公司描述")
    private String description;

    /**
     * 标签
     */
     private List<String> tags;

     @ApiModelProperty(value = "id层级")
     private String layRec;

    @ApiModelProperty(value = "删除标识，是否有效")
    private Boolean valid;

}
