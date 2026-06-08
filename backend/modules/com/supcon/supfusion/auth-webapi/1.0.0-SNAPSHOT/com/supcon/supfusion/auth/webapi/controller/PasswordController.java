package com.supcon.supfusion.auth.webapi.controller;

import com.supcon.supfusion.auth.common.exception.UserErrorEnum;
import com.supcon.supfusion.auth.common.exception.UserException;
import com.supcon.supfusion.auth.manager.PersonServiceAdapter;
import com.supcon.supfusion.auth.service.PasswordService;
import com.supcon.supfusion.auth.service.UserService;
import com.supcon.supfusion.auth.service.bo.AuthPasswdRulesBO;
import com.supcon.supfusion.auth.service.bo.UpdatePwdBO;
import com.supcon.supfusion.auth.service.bo.UserBO;
import com.supcon.supfusion.auth.webapi.vo.AuthPasswdRulesVO;
import com.supcon.supfusion.auth.webapi.vo.PersonDetailVO;
import com.supcon.supfusion.auth.webapi.vo.UpdatePwdVO;
import com.supcon.supfusion.framework.cloud.annotation.InternalApi;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import com.supcon.supfusion.organization.api.dto.PersonDTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author kk.C
 * @Date 2021/2/25 14:31
 **/
@Slf4j
@RestController
@InternalApi(path = "/inter-api/auth")
@Api(value = "用户密码相关", tags = "用户密码相关")
public class PasswordController extends BaseController {

    @Autowired
    private PasswordService passwordService;
    @Autowired
    private UserService userService;
    @Autowired
    private PersonServiceAdapter personServiceAdapter;

    @PutMapping("/v1/password/config")
    @ApiOperation(value = "修改密码配置", httpMethod = "PUT")
    public void updatePasswordConfig(@Validated @RequestBody @ApiParam(name = "body", value = "json格式", required = true) AuthPasswdRulesVO authPasswdRulesVO) {
        AuthPasswdRulesBO authPasswdRulesBO = new AuthPasswdRulesBO();
        BeanUtils.copyProperties(authPasswdRulesVO,authPasswdRulesBO);
        passwordService.updatePasswordConfig(authPasswdRulesBO);
    }

    @GetMapping("/v1/password/config")
    @ApiOperation(value = "获取密码配置", httpMethod = "GET")
    public Result<AuthPasswdRulesVO> getPasswordConfig() {
        AuthPasswdRulesVO authPasswdRulesVO = new AuthPasswdRulesVO();
        BeanUtils.copyProperties(passwordService.getPasswordConfig(),authPasswdRulesVO);
        return new Result(authPasswdRulesVO);
    }

    @PostMapping("/v1/password/config/reset")
    @ApiOperation(value = "重置密码配置", httpMethod = "POST")
    public void resetPasswordConfig() {
        passwordService.resetPasswordConfig();
    }

    @PostMapping("/v1/password/find")
    @ApiOperation(value = "找回密码发送邮箱", httpMethod = "Post")
    public Result<PersonDetailVO>  findPassword(@ApiParam(name = "email", value = "邮箱", required = true) @RequestParam(value = "email") String email
            ,@ApiParam(name = "userName", value = "用户名", required = true) @RequestParam(value = "userName") String userName) {
        UserBO userBO = userService.findByUserName(userName);
        if (StringUtils.isEmpty(userBO.getUserName())) {
            throw new UserException(UserErrorEnum.USER_NOT_EXIST);
        }
        PersonDTO personDTO;
        if (null == userBO.getPersonId()) {
            throw new UserException(UserErrorEnum.USER_PERSON_NOT_RELATION);
        } else {
            Map<Long, PersonDTO> personDTOMap = personServiceAdapter.queryPersonsById(new Long[]{(userBO.getPersonId())});
            personDTO = personDTOMap.get(userBO.getPersonId());
        }
        if(StringUtils.isEmpty(personDTO.getEmail())){
            throw new UserException(UserErrorEnum.EMAIL_NOT_EXIST);
        }
        if (!email.equals(personDTO.getEmail())) {
            throw new UserException(UserErrorEnum.EMAIL_NOT_MATCH);
        }
        personDTO.setUserId(userBO.getId());
        personDTO.setUserName(userBO.getUserName());
        passwordService.findPassword(email,personDTO.getCode());
        PersonDetailVO personDetailVO = new PersonDetailVO();
        BeanUtils.copyProperties(personDTO, personDetailVO);
        return new Result<>(personDetailVO);
    }

    @PostMapping("/v1/password/update")
    @ApiOperation(value = "更新密码", httpMethod = "Post")
    public void updatePassword(@Validated @ApiParam(name = "updatePwdVO", value = "邮箱验证码方式修改密码参数VO类", required = true) @RequestBody UpdatePwdVO updatePwdVO) {
        UpdatePwdBO updatePwdBO = new UpdatePwdBO();
        BeanUtils.copyProperties(updatePwdVO,updatePwdBO);
        passwordService.checkAndUpdatePwd(updatePwdBO);
    }

    @PostMapping("/v1/password/imageCode")
    @ApiOperation(value = "找回密码获取图形验证码", httpMethod = "Post")
    public Map<String,Object> getImageCode() {
        Map<String,Object> map = new HashMap<>();
        map = passwordService.createImageCode();
        return map;
    }

    @GetMapping("/v1/password/imageCode")
    @ApiOperation(value = "找回密码校验图形验证码", httpMethod = "Post")
    public Map<String,String> checkImageCode(@ApiParam(name = "key", value = "验证码key", required = true) @RequestParam(value = "key") String key
            ,@ApiParam(name = "code", value = "验证码文本", required = true) @RequestParam(value = "code") String code) {
        return passwordService.checkImageCode(key,code);
    }
}
