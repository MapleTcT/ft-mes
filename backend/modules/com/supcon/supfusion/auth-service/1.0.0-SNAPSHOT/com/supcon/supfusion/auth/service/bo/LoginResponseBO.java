package com.supcon.supfusion.auth.service.bo;

import com.supcon.supfusion.framework.cloud.common.pojo.BO;
import lombok.*;

import java.io.Serializable;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseBO extends BO {


    private LoginResponseBO.User user;

    private List<LoginResponseBO.Company> companies;

    private LoginResponseBO.Company currentCompany;

    private String status;

    private String ticket;

    private String tenantId;

    private String username;

    private Long userId;

    private Integer userType;

    private String accessToken;

    private Integer expiresIn;

    private String refreshToken;

    private String clientId;

    private String code;

    private String redirectUri;

    private String state;

    private String clientAccessToken;

    private String clientRefreshToken;

    // 登录方式,默认为0,表示supOS登录;如果为1,表示竹云单点登录
    private String loginType = "0";

    private Boolean userBind;

    private String protocolType;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Company implements Serializable {

        private Long id;

        private String name;

        private String code;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class User implements Serializable {

        private Long id;

        private String userName;

        private Integer userType;
    }


}
