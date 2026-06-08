package com.supcon.supfusion.organization.webapi.vo.company;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

/**
 * 公司模式
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CompanyModelVO extends VO {

    @ApiModelProperty(value = "组织架构模式,single为单公司模式, multi为多公司")
    private String model;
}
