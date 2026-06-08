package com.supcon.supfusion.auth.service;

import com.alibaba.fastjson.JSONObject;
import com.supcon.supfusion.auth.service.bo.IdentityCenterConfigBO;
import com.supcon.supfusion.auth.service.bo.LoginResponseBO;
import com.supcon.supfusion.auth.service.impl.ThirdAuthServiceImpl;

public interface ThirdAuthService {

    LoginResponseBO authorize(String code, String protocolType,String realIp,String state);

    LoginResponseBO queryCurrentUserInfo();

    ThirdAuthServiceImpl.ThirdToken refreshToken(String protocolType, String clientId, String clientSecret, String clientRefreshToken, String refreshTokenUrl);

    void logout(String protocolType,String clientId, String clientSecret, String token, String logoutUrl);

    JSONObject checkTokenValid(String clientAccessToken, String checkTokenUrl);

    IdentityCenterConfigBO queryClientIdentityConfigInfo(String protocolType);

    LoginResponseBO thirdIdentityUserBind(String userName, String password, String realIp, String state);
}
