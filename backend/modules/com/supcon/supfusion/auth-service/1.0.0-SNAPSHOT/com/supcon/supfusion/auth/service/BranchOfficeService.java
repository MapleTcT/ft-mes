package com.supcon.supfusion.auth.service;

import com.supcon.supfusion.auth.service.bo.LoginBO;
import com.supcon.supfusion.auth.service.bo.LoginResponseBO;
import org.springframework.web.client.RestTemplate;

public interface BranchOfficeService {
    void logout(String ticket);

    boolean refreshToken(String ticket);

    LoginResponseBO login(LoginBO loginBO, String realIp, String deviceType, String quatoName);
}
