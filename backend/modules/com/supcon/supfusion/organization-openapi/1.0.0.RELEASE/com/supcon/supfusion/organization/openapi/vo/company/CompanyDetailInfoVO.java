package com.supcon.supfusion.organization.openapi.vo.company;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import java.util.List;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CompanyDetailInfoVO extends VO {

    @ApiModelProperty(value = "公司编码")
    private String code;

    @ApiModelProperty(value = "上级公司编码")
    private String parentCode;

    @ApiModelProperty(value = "公司全称")
    private String fullName;

    @ApiModelProperty(value = "公司简称")
    private String shortName;

    @ApiModelProperty(value = "公司描述")
    private String description;

    @ApiModelProperty(value = "公司标签")
    private List<String> tags;

    @ApiModelProperty(value = "公司全路径")
    private String fullPath;

    @ApiModelProperty(value = "公司层级")
    private String layNo;

    @ApiModelProperty(value = "顺序号")
    private String sort;

    @ApiModelProperty(value = "是否有效")
    private Integer valid;

    @ApiModelProperty(value = "最后修改时间")
    private String modifyTime;
}
