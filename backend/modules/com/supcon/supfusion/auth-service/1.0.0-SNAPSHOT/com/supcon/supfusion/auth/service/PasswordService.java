package com.supcon.supfusion.auth.service;

import com.supcon.supfusion.auth.manager.bo.LoginConfigBO;
import com.supcon.supfusion.auth.service.bo.AuthPasswdRulesBO;
import com.supcon.supfusion.auth.service.bo.UpdatePwdBO;

import java.util.Map;

/**
 * @Author kk.C
 * @Description 密码相关service
 * @Date 2021/2/25 16:13
 * @Param
 * @return
 **/
public interface PasswordService {

    void updatePasswordConfig(AuthPasswdRulesBO authPasswdRulesBO);

    AuthPasswdRulesBO getPasswordConfig();

    LoginConfigBO getLoginConfig();

    void findPassword(String email, String personCode);

    void resetPasswordConfig();

    void checkAndUpdatePwd(UpdatePwdBO updatePwdBO);

    Map<String,Object> createImageCode();

    Map<String, String> checkImageCode(String key, String code);
}
