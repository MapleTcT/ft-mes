package com.supcon.supfusion.rbac.openapi.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class RolesVO extends VO {
    private String code;
    private String name;
    private String description;
}
