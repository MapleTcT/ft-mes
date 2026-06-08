package com.supcon.supfusion.rbac.openapi.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.validation.constraints.NotEmpty;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CreateRoleVO extends VO {
    @NotEmpty(message = "角色编码不能为空")
    private String code;
    @NotEmpty(message = "角色名称不能为空")
    private String name;
    private String description;
    @NotEmpty(message = "公司编码不能为空")
    private String companyCode;
}
