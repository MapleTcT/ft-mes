package com.supcon.supfusion.auth.webapi.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.supcon.supfusion.auth.dao.po.PwdRulesPO;
import com.supcon.supfusion.auth.service.PwdRulesService;
import com.supcon.supfusion.auth.webapi.vo.LoginConfigVO;
import com.supcon.supfusion.framework.cloud.annotation.InternalApi;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.regex.Pattern;

/**
 * @author lifangyuan
 */
@Slf4j
@RestController
@InternalApi(path = "/inter-api/auth")
@Validated
public class PasswordConfigController extends BaseController {

    private static Pattern pattern = Pattern.compile("(?<=\\()(.+?)(?=\\))");

//    @Resource
//    private KeycliandAdminClient keycloakAdminClient;

    @Resource
    private PwdRulesService pwdRulesService;

//    @PostMapping("/v1/password-config")
//    @ResponseStatus(HttpStatus.OK)
//    public void create(@Validated @RequestBody LoginConfigVO loginConfigVO) {
////        RealmRepresentation supos = keycloakAdminClient.getKeycloak().realm(RpcContext.getContext().getTenantId()).partialExport(false, false);
////        String str = JSON.toJSONString(loginConfigVO);
////        supos.setPasswordPolicy("supos password policy(" + str + ")");
////        keycloakAdminClient.getKeycloak().realm("supos").update(supos);
//    }


    @GetMapping("/v1/password-config")
    @ResponseStatus(HttpStatus.OK)
    public Result<LoginConfigVO> getLoginConfig() {
        PwdRulesPO one = pwdRulesService.getOne(Wrappers.lambdaQuery(PwdRulesPO.class).eq(PwdRulesPO::getId, 1L));
        LoginConfigVO loginConfigVO = new LoginConfigVO();
        loginConfigVO.setBigSmall(one.getContainLetterCase());
        loginConfigVO.setNumber(one.getContainNumbers());
        loginConfigVO.setSpecialChar(one.getContainSpecialChar());
        loginConfigVO.setMin(one.getMinLength());
        loginConfigVO.setMax(one.getMaxLength());
        return data(loginConfigVO);
    }

    @GetMapping("/v1/password-config/reset")
    @ResponseStatus(HttpStatus.OK)
    public void resetPasswordConfig() {
        PwdRulesPO pwdRulesPO = new PwdRulesPO();
        pwdRulesPO.setId(1L);
        pwdRulesPO.setContainLetterCase(true);
        pwdRulesPO.setContainNumbers(true);
        pwdRulesPO.setContainSpecialChar(true);
        pwdRulesPO.setMinLength(8L);
        pwdRulesPO.setMaxLength(32L);
        pwdRulesService.updateById(pwdRulesPO);
    }

    @PutMapping("/v1/password-config")
    public void updateLoginConfig(@Valid @RequestBody LoginConfigVO loginConfigVO) {
//        RealmRepresentation supos = keycloakAdminClient.getKeycloak().realm(RpcContext.getContext().getTenantId()).partialExport(false, true);
//        String str = JSON.toJSONString(loginConfigVO);
//        supos.setPasswordPolicy("supos password policy(" + str + ")");
//        keycloakAdminClient.getKeycloak().realm(RpcContext.getContext().getTenantId()).update(supos);
        PwdRulesPO pwdRulesPO = new PwdRulesPO();
        pwdRulesPO.setId(1L);
        pwdRulesPO.setContainLetterCase(loginConfigVO.getBigSmall());
        pwdRulesPO.setContainNumbers(loginConfigVO.getNumber());
        pwdRulesPO.setContainSpecialChar(loginConfigVO.getSpecialChar());
        pwdRulesPO.setMinLength(loginConfigVO.getMin());
        pwdRulesPO.setMaxLength(loginConfigVO.getMax());
        pwdRulesService.updateById(pwdRulesPO);
    }

}
