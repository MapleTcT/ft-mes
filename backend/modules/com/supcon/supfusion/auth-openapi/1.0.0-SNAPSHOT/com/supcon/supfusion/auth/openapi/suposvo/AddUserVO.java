package com.supcon.supfusion.auth.openapi.suposvo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;


@Setter
@Getter
@ToString
public class AddUserVO extends VO {

    private String username;

    private String password;

    private String email;

    private Integer accountType = 0;

    private String userDesc;

    private String timeZone;

    private List<String> roleNameList;

}
