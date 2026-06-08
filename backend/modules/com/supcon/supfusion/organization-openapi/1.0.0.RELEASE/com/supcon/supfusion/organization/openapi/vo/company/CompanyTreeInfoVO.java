package com.supcon.supfusion.organization.openapi.vo.company;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CompanyTreeInfoVO extends VO {
    @JsonIgnore
    private Long id;

    @JsonIgnore
    private Long parentId;

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

    @ApiModelProperty(value = "子公司")
    private List<CompanyTreeInfoVO> children;

    public List<CompanyTreeInfoVO> getChildren() {
        if (children == null) {
            children = new ArrayList<>();
        }
        return children;
    }
}
