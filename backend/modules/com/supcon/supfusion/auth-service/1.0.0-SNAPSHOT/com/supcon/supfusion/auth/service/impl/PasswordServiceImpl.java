package com.supcon.supfusion.auth.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.supcon.supfusion.auth.common.constants.Constants;
import com.supcon.supfusion.auth.common.exception.UserErrorEnum;
import com.supcon.supfusion.auth.common.exception.UserException;
import com.supcon.supfusion.auth.dao.mapper.AuthPasswdRulesMapper;
import com.supcon.supfusion.auth.dao.po.AuthPasswdRulesPO;
import com.supcon.supfusion.auth.manager.NotificationAdapter;
import com.supcon.supfusion.auth.manager.bo.LoginConfigBO;
import com.supcon.supfusion.auth.service.PasswordService;
import com.supcon.supfusion.auth.service.UserService;
import com.supcon.supfusion.auth.service.bo.AuthPasswdRulesBO;
import com.supcon.supfusion.auth.service.bo.UpdatePwdBO;
import com.supcon.supfusion.auth.service.bo.UserBO;
import com.supcon.supfusion.notification.apiserver.api.dto.RangeDTO;
import com.supcon.supfusion.notification.apiserver.api.dto.SendWithMessageContentDTO;
import com.supcon.supfusion.notification.apiserver.api.dto.SendWithMessageRequestDTO;
import com.supcon.supfusion.notification.common.bean.RangeType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static com.supcon.supfusion.auth.common.exception.UserErrorEnum.PASSWD_REGREX_WRONG;

@Slf4j
@Service
public class PasswordServiceImpl implements PasswordService {

    @Autowired
    private AuthPasswdRulesMapper authPasswdRulesMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private NotificationAdapter notificationAdapter;
    @Autowired
    private UserService userService;

    @Autowired
    private DefaultKaptcha defaultKaptcha;

    private static SecureRandom random = new SecureRandom();

    @Override
    public void updatePasswordConfig(AuthPasswdRulesBO authPasswdRulesBO) {
        AuthPasswdRulesPO authPasswdRulesPO = new AuthPasswdRulesPO();

        String regularExpression = authPasswdRulesBO.getRegularExpression();
        if (StringUtils.isNotEmpty(regularExpression)){
            try {
                Pattern.compile(regularExpression);
            }catch (PatternSyntaxException e){
                throw new UserException(PASSWD_REGREX_WRONG);
            }

        }

        BeanUtils.copyProperties(authPasswdRulesBO, authPasswdRulesPO);
        authPasswdRulesMapper.updateById(authPasswdRulesPO);
    }

    @Override
    public AuthPasswdRulesBO getPasswordConfig() {
        AuthPasswdRulesPO authPasswdRulesPO = authPasswdRulesMapper.selectById(1);
        AuthPasswdRulesBO authPasswdRulesBO = new AuthPasswdRulesBO();
        BeanUtils.copyProperties(authPasswdRulesPO, authPasswdRulesBO);
        return authPasswdRulesBO;
    }

    @Override
    public LoginConfigBO getLoginConfig() {
        AuthPasswdRulesPO authPasswdRulesPO = authPasswdRulesMapper.selectById(1);
        LoginConfigBO loginConfigBO = new LoginConfigBO();
        loginConfigBO.setMax(authPasswdRulesPO.getMaxLength());
        loginConfigBO.setMin(authPasswdRulesPO.getMinLength());
        if (null != authPasswdRulesPO.getContainLetterCase()) {
            loginConfigBO.setBigSmall(authPasswdRulesPO.getContainLetterCase());
        }
        if (null != authPasswdRulesPO.getContainNumbers()) {
            loginConfigBO.setNumber(authPasswdRulesPO.getContainNumbers());
        }
        if (null != authPasswdRulesPO.getContainSpecialChar()) {
            loginConfigBO.setSpecialChar(authPasswdRulesPO.getContainSpecialChar());
        }
        if (null != authPasswdRulesPO.getRegularExpression()) {
            loginConfigBO.setRegularExpression(authPasswdRulesPO.getRegularExpression());
        }
        if (null != authPasswdRulesPO.getHint()) {
            loginConfigBO.setHint(authPasswdRulesPO.getHint());
        }
        if (null != authPasswdRulesPO.getRuleType()) {
            loginConfigBO.setRuleType(authPasswdRulesPO.getRuleType());
        }
        return loginConfigBO;
    }

    @Override
    public void findPassword(String email, String personCode) {
        String verificationCode = randomCode();
        String emailMsg = String.format(Constants.EMAIL_MSG, verificationCode);
        stringRedisTemplate.opsForValue().set(personCode + "email", verificationCode, 15, TimeUnit.MINUTES);
        sendEmail(emailMsg, personCode);

    }

    @Override
    public void checkAndUpdatePwd(UpdatePwdBO updatePwdBO) {
        String realVerificationCode = stringRedisTemplate.opsForValue().get(updatePwdBO.getPersonCode() + "email");
        if (StringUtils.isEmpty(realVerificationCode) ) {
            throw new UserException(UserErrorEnum.VERIFICATION_CODE_EXPIRE);
        }else if (!Objects.equals(realVerificationCode, updatePwdBO.getVerificationCode())) {
            throw new UserException(UserErrorEnum.VERIFICATION_CODE_WRONG);
        }
        UserBO userBO = new UserBO();
        userBO.setId(updatePwdBO.getUserId());
        userBO.setPassword(updatePwdBO.getPassword());
        userBO.setLoginFirst(false);

        userService.updateUser(userBO, null);
    }

    @Override
    public Map<String, Object> createImageCode() {
        Map<String, Object> codeMap = new HashMap<>();
        String uuid = UUID.randomUUID().toString();
        String createText = defaultKaptcha.createText();
        BufferedImage image = defaultKaptcha.createImage(createText);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", outputStream);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        byte imageByte[] = outputStream.toByteArray();
        Base64 base64 = new Base64();
        stringRedisTemplate.opsForValue().set(uuid, createText, 60, TimeUnit.SECONDS);
        codeMap.put("uuid", uuid);
        codeMap.put("image",base64.encodeToString(imageByte));
        return codeMap;
    }

    @Override
    public Map<String, String> checkImageCode(String key, String code) {
        Map<String,String> resultMap = new HashMap<>();
        String imageCode = stringRedisTemplate.opsForValue().get(key);
        if (StringUtils.isEmpty(imageCode) || !imageCode.equals(code)) {
            resultMap.put("isApproved","false");
            resultMap.put("message","验证码错误或已过期");
        }else{
            resultMap.put("isApproved","true");
            resultMap.put("message","验证码正确");
        }
        return resultMap;
    }

    @Override
    public void resetPasswordConfig() {
        AuthPasswdRulesPO authPasswdRulesPO = new AuthPasswdRulesPO();
        authPasswdRulesPO.setId(1L);
        authPasswdRulesPO.setMinLength(8);
        authPasswdRulesPO.setMaxLength(32);
        authPasswdRulesPO.setRuleType(0);
        authPasswdRulesPO.setContainLetterCase(true);
        authPasswdRulesPO.setContainSpecialChar(true);
        authPasswdRulesPO.setContainSpecialChar(true);
        authPasswdRulesPO.setFindPwdSwitch(true);
        authPasswdRulesMapper.updateById(authPasswdRulesPO);
    }

    private void sendEmail(String emailMsg, String personCode) {
        SendWithMessageRequestDTO sendWithMessageRequestDTO = new SendWithMessageRequestDTO();
        List<RangeDTO> rangeDTOS = new ArrayList<>();
        List<String> personCodeList = new ArrayList<>();
        List<SendWithMessageContentDTO> contentDTOS = new ArrayList<>();
        SendWithMessageContentDTO sendWithMessageContentDTO = new SendWithMessageContentDTO();
        personCodeList.add(personCode);
        RangeDTO rangeDTO = new RangeDTO();
        rangeDTO.setRangeType(RangeType.STAFF);
        rangeDTO.setCodes(personCodeList);
        rangeDTOS.add(rangeDTO);
        sendWithMessageContentDTO.setProtocol("email");
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("subject", Constants.EMAIL_TITLE);
        jsonObject.put("text", emailMsg);
        sendWithMessageContentDTO.setContent(jsonObject.toJSONString());
        contentDTOS.add(sendWithMessageContentDTO);
        sendWithMessageRequestDTO.setBsmodCode(Constants.FEATURE);
        sendWithMessageRequestDTO.setBsmodName(Constants.FEATURE);
        sendWithMessageRequestDTO.setReceivers(rangeDTOS);
        sendWithMessageRequestDTO.setContents(contentDTOS);
        //todo
        notificationAdapter.sendMessage(sendWithMessageRequestDTO);

    }

    /**
     * @return java.lang.String
     * @Author kk.C
     * @Description 生成找回密码验证码
     * @Date 2021/3/2 10:17
     * @Param []
     **/
    public static String randomCode() {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            str.append(random.nextInt(10));
        }
        return str.toString();
    }
}
