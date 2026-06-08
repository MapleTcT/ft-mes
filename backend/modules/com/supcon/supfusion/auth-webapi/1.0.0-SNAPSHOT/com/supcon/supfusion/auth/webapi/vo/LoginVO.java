package com.supcon.supfusion.auth.webapi.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.*;


@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class LoginVO extends VO {

    private String userName;

    private String password;

    private String clientId;

    private Long companyId;

    private String state;

}
