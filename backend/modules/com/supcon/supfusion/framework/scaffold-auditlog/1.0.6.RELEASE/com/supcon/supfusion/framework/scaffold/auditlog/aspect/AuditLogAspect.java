package com.supcon.supfusion.framework.scaffold.auditlog.aspect;

import com.supcon.supfusion.framework.scaffold.auditlog.annotation.AuditLog;
import com.supcon.supfusion.framework.scaffold.auditlog.cache.ModelAuditLogCache;
import com.supcon.supfusion.framework.scaffold.auditlog.constant.OperateType;
import com.supcon.supfusion.framework.scaffold.auditlog.pojo.bo.AuditBusinessLogBO;
import com.supcon.supfusion.framework.scaffold.auditlog.pojo.bo.AuditDataLogBO;
import com.supcon.supfusion.framework.scaffold.auditlog.strategy.AuditLogStrategy;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 通用审计日志切面
 *
 * @author caokele
 */
@Aspect
public class AuditLogAspect {
    private static final Logger logger = LoggerFactory.getLogger(AuditLogAspect.class);

    /**
     * 审计业务日志策略
     */
    private AuditLogStrategy<AuditBusinessLogBO> auditBusinessLogStrategy;
    /**
     * 审计数据日志策略
     */
    private AuditLogStrategy<AuditDataLogBO> auditDataLogStrategy;

    public AuditLogStrategy<AuditBusinessLogBO> getAuditBusinessLogStrategy() {
        return auditBusinessLogStrategy;
    }

    public void setAuditBusinessLogStrategy(AuditLogStrategy<AuditBusinessLogBO> auditBusinessLogStrategy) {
        this.auditBusinessLogStrategy = auditBusinessLogStrategy;
    }

    public AuditLogStrategy<AuditDataLogBO> getAuditDataLogStrategy() {
        return auditDataLogStrategy;
    }

    public void setAuditDataLogStrategy(AuditLogStrategy<AuditDataLogBO> auditDataLogStrategy) {
        this.auditDataLogStrategy = auditDataLogStrategy;
    }

    /**
     * 通用审计日志切入点
     */
    @Pointcut("@annotation(com.supcon.supfusion.framework.scaffold.auditlog.annotation.AuditLog)")
    public void auditLogCut() {
    }

    /**
     * 方法返回后通知，代表着业务正常结束
     */
    @AfterReturning(value = "auditLogCut()", returning = "returnObject")
    public void afterReturning(JoinPoint joinPoint, Object returnObject) {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        AuditLog auditLog = methodSignature.getMethod().getAnnotation(AuditLog.class);
        if (!ModelAuditLogCache.isAuditLogEnabled(auditLog.modelCodes())) {
            return;
        }
        // 解析参数获取数据日志
        Object[] args = joinPoint.getArgs();
        List<AuditDataLogBO> auditDataLogs = extractAuditDataLogs(args, auditLog.operateType());
        // 发布数据日志
        if (!auditDataLogs.isEmpty()) {
            auditDataLogStrategy.publishAuditLogs(auditDataLogs);
        }
        // 解析注解
        AuditBusinessLogBO auditBusinessLogBO = auditBusinessLogStrategy.buildAuditLog(auditLog);
        auditBusinessLogBO.setSuccess(true);
        // 发布业务日志
        auditBusinessLogStrategy.publishAuditLog(auditBusinessLogBO);
    }

    /**
     * 异常通知，代表业务出现异常
     */
    @AfterThrowing(pointcut = "auditLogCut()", throwing = "ex")
    public void afterThrowing(JoinPoint joinPoint, Throwable ex) {
        // 解析注解
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        AuditLog auditLog = methodSignature.getMethod().getAnnotation(AuditLog.class);
        if (!ModelAuditLogCache.isAuditLogEnabled(auditLog.modelCodes())) {
            return;
        }
        // 解析参数获取数据日志
        Object[] args = joinPoint.getArgs();
        List<AuditDataLogBO> auditDataLogs = extractAuditDataLogs(args, auditLog.operateType());
        // 发布数据日志
        if (!auditDataLogs.isEmpty()) {
            auditDataLogStrategy.publishAuditLogs(auditDataLogs);
        }
        // 解析注解
        AuditBusinessLogBO auditBusinessLogBO = auditBusinessLogStrategy.buildAuditLog(auditLog);
        auditBusinessLogBO.setExceptionDescription(Optional.ofNullable(ex.getMessage()).orElse(ex.toString()));
        auditBusinessLogBO.setSuccess(false);
        // 发布业务日志
        auditBusinessLogStrategy.publishAuditLog(auditBusinessLogBO);
    }

    /**
     * 解析参数获取数据日志
     */
    private List<AuditDataLogBO> extractAuditDataLogs(Object[] args, OperateType operateType) {
        List<Object> auditModels = new LinkedList<>();
        for (Object arg : args) {
            if (Objects.isNull(arg)) {
                continue;
            }
            Class<?> argClazz = arg.getClass();
            // 如果参数模型有审计日志注解
            if (auditDataLogStrategy.isModelNeedAudit(argClazz)) {
                auditModels.add(arg);
            }
            // 如果参数是数组并且item有审计日志注解
            if (argClazz.isArray()) {
                Object[] objects = (Object[]) arg;
                for (Object object : objects) {
                    if (Objects.nonNull(object) && auditDataLogStrategy.isModelNeedAudit(object.getClass())) {
                        auditModels.add(object);
                    }
                }
            }
            // 如果参数是Collection并且item有审计日志注解
            if (arg instanceof Collection) {
                Collection<?> objects = (Collection<?>) arg;
                for (Object object : objects) {
                    if (Objects.nonNull(object) && auditDataLogStrategy.isModelNeedAudit(object.getClass())) {
                        auditModels.add(object);
                    }
                }
            }
        }
        if (auditModels.isEmpty()) {
            return Collections.emptyList();
        }
        // 构建审计日志对象
        List<AuditDataLogBO> auditDataLogs = auditModels.stream().map(model -> {
            AuditDataLogBO auditDataLogBO = auditDataLogStrategy.buildAuditLog(model);
            auditDataLogBO.setOperateType(operateType);
            return auditDataLogBO;
        }).collect(Collectors.toList());
        return auditDataLogs;
    }
}
