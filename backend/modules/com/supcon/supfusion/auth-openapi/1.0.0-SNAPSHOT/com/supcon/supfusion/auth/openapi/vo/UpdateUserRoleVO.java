package com.supcon.supfusion.auth.openapi.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Setter
@Getter
@ToString
public class UpdateUserRoleVO extends VO {

    private List<String> roleCodes;
}
