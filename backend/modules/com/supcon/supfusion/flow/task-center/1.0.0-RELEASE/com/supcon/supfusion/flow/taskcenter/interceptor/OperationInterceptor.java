/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.taskcenter.interceptor;

import java.util.ArrayList;
import java.util.List;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import com.supcon.supfusion.flow.common.dto.AssigneeDTO;
import com.supcon.supfusion.flow.common.enumeration.OperationTypeEnum;
import com.supcon.supfusion.flow.common.util.Constants;
import com.supcon.supfusion.flow.common.util.LocalContext;
import com.supcon.supfusion.flow.common.vo.webapi.AssigneeRequestVO;
import com.supcon.supfusion.flow.common.vo.webapi.AuditVO;

/**
 * @author: zhuangmh
 * @date: 2020年8月19日 上午9:28:15
 */
@Component
@Aspect
public class OperationInterceptor {
    
    @Pointcut("execution(* com.supcon.supfusion.flow.taskcenter.service.TaskCenterService.submit(..))")
    public void pointcut() {
    }
    
    @Pointcut("execution(* com.supcon.supfusion.flow.taskcenter.service.TaskCenterService.revoke(..))")
    public void revokeTaskPointcut() {
        
    }
    
    @Around("pointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            Object[] args = joinPoint.getArgs();
            setContext(args);
            return joinPoint.proceed();
        } finally {
            clearContext();
        }
    }
    
    @Around("revokeTaskPointcut()")
    public Object aroundRevokeTask(ProceedingJoinPoint joinPoint) throws Throwable {
        LocalContext.getContext().setOperationType(OperationTypeEnum.REVOKE);
        try {
            return joinPoint.proceed();
        } finally {
            clearContext();
        }
    }
    
    private void setContext(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof AuditVO) {
                if (((AuditVO)arg).getType() == Constants.REJECT) {
                    LocalContext.getContext().setOperationType(OperationTypeEnum.REJECT);
                    break;
                }
            } else if (arg instanceof List) {
                List<?> assignments = ((List<?>) arg);
                if (assignments != null && !assignments.isEmpty() && assignments.get(0) instanceof AssigneeRequestVO) {
                    LocalContext.getContext().setOperationType(OperationTypeEnum.APPOINT);
                    List<AssigneeDTO> assignDtos = vodtoTransfer((List<AssigneeRequestVO>) arg);
                    LocalContext.getContext().setAssigns(assignDtos);
                    break;
                }
            } 
        }
    }
    
    private List<AssigneeDTO> vodtoTransfer(List<AssigneeRequestVO> vos) {
        List<AssigneeDTO> dtos = new ArrayList<>();
        for (AssigneeRequestVO vo : vos) {
            dtos.add(new AssigneeDTO("", vo.getName(), vo.getTaskDefKey(), vo.getUsers()));
        }
        return dtos;
    }
    
    private void clearContext() {
        LocalContext.getContext().setOperationType(null);
        LocalContext.getContext().setAssigns(null);
    }
}
