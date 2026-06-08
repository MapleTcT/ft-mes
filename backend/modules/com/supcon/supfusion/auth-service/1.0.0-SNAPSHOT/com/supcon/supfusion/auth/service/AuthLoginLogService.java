package com.supcon.supfusion.auth.service;

import com.supcon.supfusion.auth.service.bo.AuthLoginLogBO;
import com.supcon.supfusion.auth.service.bo.LoginResponseBO;

/**
 * @Author kk.C
 * @Description: 登录日志
 * @Date 2021/5/26 14:19
 */
public interface AuthLoginLogService {

    void saveLoginLog(AuthLoginLogBO authLoginLogBO);

    void saveLogoutLog(String ticket,String logoutType);

    void generateLoginLog(LoginResponseBO login, String deviceType, String realIp);
}
