package com.supcon.supfusion.auth.openapi.suposvo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Setter
@Getter
@ToString
public class UpdateUserVO extends VO {

    private String userDesc;

    private String timeZone;

    private String email;

    private List<String> roleNameList;
}
