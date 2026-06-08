/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.openapi.interceptor;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.supcon.supfusion.auth.api.dto.UserOrgDetailDTO;
import com.supcon.supfusion.flow.common.enumeration.FlowErrorEnum;
import com.supcon.supfusion.flow.common.exception.NotExistException;
import com.supcon.supfusion.flow.common.util.Constants;
import com.supcon.supfusion.flow.common.vo.openapi.BaseRequestVO;
import com.supcon.supfusion.flow.taskcenter.rpc.UserServiceAdapter;
import com.supcon.supfusion.framework.cloud.common.context.UserContext;

/**
 * @author: zhuangmh
 * @date: 2020年11月11日 下午1:46:06
 */
@Component
@Aspect
public class OpenapiInterceptor {
    
    @Pointcut("execution(* com.supcon.supfusion.flow.openapi.*.*(..))")
    public void controllerPointcut() {
    }
    
    @Autowired
    private UserServiceAdapter userServiceAdapter;
    
    /**
     * 处理GET DELETE请求
     * @param joinPoint
     * @return
     * @throws Throwable
     */
    @Around("controllerPointcut() && (@annotation(org.springframework.web.bind.annotation.DeleteMapping) || @annotation(org.springframework.web.bind.annotation.GetMapping))")
    public Object handleGetPointcut(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            Object[] args = joinPoint.getArgs();
            for (Object arg : args) {
                if (arg instanceof HttpServletRequest) {
                    String username = ((HttpServletRequest)arg).getParameter(Constants.PARAMETER_USER_NAME);
                    if (StringUtils.isNotEmpty(username)) {
                        setUserContext(username);
                    }
                    break;
                }
            }
            return joinPoint.proceed();
        } finally {
            clearUserContext();
        }
    }
    
    private void setUserContext(String username) {
        UserOrgDetailDTO user = userServiceAdapter.getUserByName(username);
        if (user == null || user.getId() == null) {
            throw new NotExistException(FlowErrorEnum.USER_NOT_EXIST_ERROR);
        }
        UserContext.getUserContext().setUserId(user.getId());
        UserContext.getUserContext().setUserName(user.getUserName());
        UserContext.getUserContext().setCompanyId(user.getCompanyId());
        UserContext.getUserContext().setStaffId(user.getPersonId());
        UserContext.getUserContext().setStaffName(user.getPersonName());
    }
    
    private void clearUserContext() {
        UserContext.getUserContext().setUserId(null);
        UserContext.getUserContext().setUserName(null);
        UserContext.getUserContext().setCompanyId(null);
        UserContext.getUserContext().setStaffId(null);
    }
    
    /**
     * 处理POST PUT请求
     * @param joinPoint
     * @return
     * @throws Throwable
     */
    @Around("controllerPointcut() && (@annotation(org.springframework.web.bind.annotation.PostMapping) || @annotation(org.springframework.web.bind.annotation.PutMapping))")
    public Object handlePostPointcut(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            Object[] args = joinPoint.getArgs();
            for (Object arg : args) {
                if (arg instanceof BaseRequestVO) {
                    String username = ((BaseRequestVO)arg).getUsername();
                    if (StringUtils.isNotEmpty(username)) {
                        setUserContext(username);
                    }
                    break;
                }
            }
            return joinPoint.proceed();
        } finally {
            clearUserContext();
        }
    }

}
