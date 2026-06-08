package com.supcon.supfusion.auth.openapi.suposvo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class AddRoleVO extends VO {

    private String name;

    private String showName;

    private String description;
}
