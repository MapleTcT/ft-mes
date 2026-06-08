package com.supcon.supfusion.auth.webapi.vo;

import com.supcon.supfusion.auth.service.bo.LoginResponseBO;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.*;

import java.util.List;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseVO extends VO {


    private LoginResponseBO.User user;

    private List<LoginResponseBO.Company> companies;

    private LoginResponseBO.Company currentCompany;

    private String status;

    private String ticket;

    private String tenantId;

    private String username;

    private Long userId;

    private Integer userType;

    private String redirectUri;

    private String loginType;
}
