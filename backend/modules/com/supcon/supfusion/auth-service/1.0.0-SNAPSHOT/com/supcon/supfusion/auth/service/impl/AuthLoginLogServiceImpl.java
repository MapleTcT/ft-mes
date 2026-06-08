package com.supcon.supfusion.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.supcon.supfusion.auth.dao.mapper.AuthLoginLogMapper;
import com.supcon.supfusion.auth.dao.po.AuthLoginLogPO;
import com.supcon.supfusion.auth.service.AuthLoginLogService;
import com.supcon.supfusion.auth.service.bo.AuthLoginLogBO;
import com.supcon.supfusion.auth.service.bo.LoginResponseBO;
import com.supcon.supfusion.framework.cloud.common.util.DateTimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class AuthLoginLogServiceImpl implements AuthLoginLogService {

    @Autowired
    private AuthLoginLogMapper authLoginLogMapper;

    @Override
    public void saveLoginLog(AuthLoginLogBO authLoginLogBO) {
		try {
			if (!Optional.ofNullable(authLoginLogBO).isPresent()) {
				return;
			}
			AuthLoginLogPO authLoginLogPO = new AuthLoginLogPO();
			BeanUtils.copyProperties(authLoginLogBO, authLoginLogPO);
			authLoginLogMapper.insert(authLoginLogPO);
		}catch (Exception e){
            log.error("save log in log error:{}",e.getMessage());
        }

    }

    @Override
    public void saveLogoutLog(String ticket, String logoutType) {
		try {
            LambdaQueryWrapper<AuthLoginLogPO> wrapper = Wrappers.lambdaQuery(AuthLoginLogPO.class);
            wrapper.eq(AuthLoginLogPO::getTicket, ticket);
            List<AuthLoginLogPO> authLoginLogPOS = authLoginLogMapper.selectList(wrapper);
            if (ObjectUtils.isEmpty(authLoginLogPOS)) {
                return;
            }
            for (AuthLoginLogPO authLoginLogPO : authLoginLogPOS) {
                authLoginLogPO.setLogoutType(logoutType);
                authLoginLogPO.setLogoutTime(DateTimeUtil.getUTC0());
                authLoginLogMapper.updateById(authLoginLogPO);
            }
        }catch (Exception e){
            log.error("save log out log error:{}",e.getMessage());
        }

    }

    @Override
    public void generateLoginLog(LoginResponseBO login, String deviceType, String realIp) {
        if (!Optional.ofNullable(login).isPresent()) {
            return;
        }
		LambdaQueryWrapper<AuthLoginLogPO> wrapper = Wrappers.lambdaQuery(AuthLoginLogPO.class);
        wrapper.eq(AuthLoginLogPO::getTicket,login.getTicket());
        Integer count = authLoginLogMapper.selectCount(wrapper);
        if(count > 0){
            return;
        }
        AuthLoginLogBO authLoginLogBO = new AuthLoginLogBO();
        authLoginLogBO.setLoginIp(realIp);
        authLoginLogBO.setUserName(login.getUsername());
        authLoginLogBO.setUserId(login.getUserId());
        authLoginLogBO.setDeviceType(deviceType);
        authLoginLogBO.setLoginTime(DateTimeUtil.getUTC0());
        authLoginLogBO.setLoginType("0".equals(login.getLoginType()) ? "supos" : login.getProtocolType());
        authLoginLogBO.setTicket(login.getTicket());
        saveLoginLog(authLoginLogBO);
    }

}
