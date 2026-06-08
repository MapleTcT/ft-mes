package com.supcon.supfusion.framework.scaffold.auditlog.aspect;

import com.supcon.supfusion.framework.scaffold.auditlog.annotation.AuditBusinessLog;
import com.supcon.supfusion.framework.scaffold.auditlog.annotation.AuditPK;
import com.supcon.supfusion.framework.scaffold.auditlog.cache.ModelAuditLogCache;
import com.supcon.supfusion.framework.scaffold.auditlog.constant.OperateType;
import com.supcon.supfusion.framework.scaffold.auditlog.pojo.bo.AuditBusinessLogBO;
import com.supcon.supfusion.framework.scaffold.auditlog.strategy.AuditLogStrategy;
import com.supcon.supfusion.framework.scaffold.auditlog.util.ClassExUtil;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.CodeSignature;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Objects;
import java.util.Optional;

/**
 * 业务审计日志切面
 *
 * @author caokele
 */
@Aspect
public class AuditBusinessLogAspect {
    private static final Logger logger = LoggerFactory.getLogger(AuditBusinessLogAspect.class);
    private static final String FILE_SEPARATOR = "/";


    private AuditLogStrategy<AuditBusinessLogBO> auditLogStrategy;

    public AuditLogStrategy<AuditBusinessLogBO> getAuditLogStrategy() {
        return auditLogStrategy;
    }

    public void setAuditLogStrategy(AuditLogStrategy<AuditBusinessLogBO> auditLogStrategy) {
        this.auditLogStrategy = auditLogStrategy;
    }

    /**
     * 业务审计日志切入点
     */
    @Pointcut("@annotation(com.supcon.supfusion.framework.scaffold.auditlog.annotation.AuditBusinessLog)")
    public void auditBusinessLogCut() {
    }

    /**
     * 方法返回后通知，代表着业务正常结束
     */
    @AfterReturning(value = "auditBusinessLogCut()", returning = "returnObject")
    public void afterReturning(JoinPoint joinPoint, Object returnObject) {
        // 解析注解
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        AuditBusinessLog auditBusinessLog = methodSignature.getMethod().getAnnotation(AuditBusinessLog.class);
        if (!ModelAuditLogCache.isAuditLogEnabled(auditBusinessLog.modelCodes())) {
            return;
        }
        AuditBusinessLogBO auditBusinessLogBO = auditLogStrategy.buildAuditLog(auditBusinessLog);
        auditBusinessLogBO.setSuccess(true);
        auditBusinessLogBO.setOperateType(getOperateType(joinPoint, auditBusinessLog.operateType()));
        // 处理导入
        dealImport(auditBusinessLogBO, joinPoint, auditBusinessLog.operateType());
        // 发布日志
        auditLogStrategy.publishAuditLog(auditBusinessLogBO);
    }

    /**
     * 异常通知，代表业务出现异常
     */
    @AfterThrowing(pointcut = "auditBusinessLogCut()", throwing = "ex")
    public void afterThrowing(JoinPoint joinPoint, Throwable ex) {
        // 解析注解
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        AuditBusinessLog auditBusinessLog = methodSignature.getMethod().getAnnotation(AuditBusinessLog.class);
        if (!ModelAuditLogCache.isAuditLogEnabled(auditBusinessLog.modelCodes())) {
            return;
        }
        AuditBusinessLogBO auditBusinessLogBO = auditLogStrategy.buildAuditLog(auditBusinessLog);
        auditBusinessLogBO.setExceptionDescription(Optional.ofNullable(ex.getMessage()).orElse(ex.toString()));
        auditBusinessLogBO.setSuccess(false);
        auditBusinessLogBO.setOperateType(getOperateType(joinPoint, auditBusinessLog.operateType()));
        // 处理导入参数
        dealImport(auditBusinessLogBO, joinPoint, auditBusinessLog.operateType());
        // 发布日志
        auditLogStrategy.publishAuditLog(auditBusinessLogBO);
    }

    /**
     * 获取操作类型
     * 如果方法同时拥有新增或修改的功能，根据参数中是否有主键，如果存在非空主键，则代表是修改。
     *
     * @see AuditPK 通过该注解可以给申明指定主键参数
     */
    private OperateType getOperateType(JoinPoint joinPoint, OperateType operateType) {
        if (!operateType.equals(OperateType.ADD_OR_MODIFY)) {
            return operateType;
        }
        boolean isModify = false;
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();
        Parameter[] parameters = method.getParameters();
        Object[] args = joinPoint.getArgs();
        for (int i = 0; i < args.length; i++) {
            // 是否是主键字段且不为空
            boolean isPkField = parameters[i].isAnnotationPresent(AuditPK.class);
            if (Objects.nonNull(args[i]) && isPkField) {
                isModify = true;
                break;
            }
        }
        return isModify ? OperateType.MODIFY : OperateType.ADD;
    }

    /**
     * 处理导入参数
     */
    private void dealImport(AuditBusinessLogBO auditBusinessLogBO, JoinPoint joinPoint, OperateType operateType) {
        if (!OperateType.IMPORT.equals(operateType)) {
            return;
        }
        CodeSignature codeSignature = (CodeSignature) joinPoint.getSignature();
        Object[] fieldValues = joinPoint.getArgs();
        String[] fieldNames = codeSignature.getParameterNames();
        String filePath = (String) ClassExUtil.getFieldByName(fieldValues, fieldNames, ClassExUtil.FIELD_FILE_PATH);
        if (StringUtils.isEmpty(filePath)) {
            return;
        }
        String[] filePart = filePath.split(FILE_SEPARATOR);
        String fileName = filePart[filePart.length - 1];
        auditBusinessLogBO.setFileName(fileName);
        auditBusinessLogBO.setFileUrl(filePath);
    }
}
