/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.taskcenter.interceptor;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.supcon.supfusion.flow.common.po.PendingTaskPO;
import com.supcon.supfusion.flow.taskcenter.job.PendingTaskTotalNotificationJob;

import lombok.extern.slf4j.Slf4j;

/**
 * @author: zhuangmh
 * @date: 2020年11月13日 下午2:42:08
 */
@Component
@Aspect
@Slf4j
public class TaskNoticeInterceptor {
    
    @Autowired
    private PendingTaskTotalNotificationJob pendingTaskTotalNotificationJob;
    
    @Pointcut("execution(* com.supcon.supfusion.flow.dao.PendingTaskMapper.insert(..)) || execution(* com.supcon.supfusion.flow.dao.PendingTaskMapper.deleteTask(..))")
    public void noticePending() {
        
    }
    
    @After(value = "noticePending()")
    public void wsNotice(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        try { // 确保不会因为通知失败而导致待办创建失败
            for (Object arg : args) {
                if (arg instanceof PendingTaskPO) {
                    pendingTaskTotalNotificationJob.submit(((PendingTaskPO)arg).getUserId());
                    break;
                }
            }
        } catch (Exception e) {
            log.error("待办总数ws推送失败", e);
        }
    }
}
