package com.supcon.supfusion.framework.scaffold.auditlog.event;

import com.supcon.supfusion.framework.scaffold.auditlog.pojo.bo.AuditBusinessLogBO;
import com.supcon.supfusion.framework.scaffold.auditlog.pojo.bo.AuditDataLogBO;
import com.supcon.supfusion.framework.scaffold.auditlog.pojo.po.AuditBusinessLogPO;
import com.supcon.supfusion.framework.scaffold.auditlog.pojo.po.AuditDataLogPO;
import com.supcon.supfusion.framework.scaffold.auditlog.repository.AuditLogRepository;
import com.supcon.supfusion.framework.scaffold.auditlog.util.ClassExUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 审计日志监听
 *
 * @author ricky
 * @version 1.0.0
 * @date 2019-07-05 10:57
 * @copyright
 */
@Component
public class AuditLogListener {

    @Autowired
    private AuditLogRepository<AuditBusinessLogPO> auditBusinessLogRepository;
    @Autowired
    private AuditLogRepository<AuditDataLogPO> auditDataLogRepository;

    @Async
    @EventListener(AuditBusinessLogEvent.class)
    public void auditBusinessLogListener(AuditBusinessLogEvent event) {
        Collection<AuditBusinessLogBO> auditBusinessLogBOs = event.getSource();
        if (auditBusinessLogBOs.isEmpty()) {
            return;
        }
        List<AuditBusinessLogPO> auditBusinessLogPOs = auditBusinessLogBOs.stream().map(auditBusinessLogBO -> {
            AuditBusinessLogPO auditBusinessLogPO = new AuditBusinessLogPO();
            BeanUtils.copyProperties(auditBusinessLogBO, auditBusinessLogPO);
            auditBusinessLogPO.setOperateType(auditBusinessLogBO.getOperateType().toString());
            auditBusinessLogPO.setOperateTime(auditBusinessLogBO.getOperateTime().getTime());
            return auditBusinessLogPO;
        }).collect(Collectors.toList());
        auditBusinessLogRepository.batchSave(auditBusinessLogPOs);
    }

    @Async
    @EventListener(AuditDataLogEvent.class)
    public void auditDataLogListener(AuditDataLogEvent event) throws IllegalAccessException {
        Collection<AuditDataLogBO> auditDataLogBOs = event.getSource();
        if (auditDataLogBOs.isEmpty()) {
            return;
        }
        // 将集合根据模块编码分类
        Map<String, Collection<AuditDataLogPO>> modelAuditDataLogMap = new HashMap<>(auditDataLogBOs.size());
        for (AuditDataLogBO auditDataLogBO : auditDataLogBOs) {
            AuditDataLogPO auditDataLogPO = new AuditDataLogPO();
            BeanUtils.copyProperties(auditDataLogBO, auditDataLogPO);
            auditDataLogPO.setOperateType(auditDataLogBO.getOperateType().toString());
            auditDataLogPO.setOperateTime(auditDataLogBO.getOperateTime().getTime());
            Map<String, Object> model = ClassExUtil.modelToMap(auditDataLogBO.getModel());
            auditDataLogPO.setModel(model);
            Collection<AuditDataLogPO> auditDataLogPOs = modelAuditDataLogMap.computeIfAbsent(auditDataLogPO.getModelCode(), k -> new LinkedList<>());
            auditDataLogPOs.add(auditDataLogPO);
        }
        modelAuditDataLogMap.forEach((modelCode, auditDataLogs) -> {
            auditDataLogRepository.batchSave(auditDataLogs);
        });
    }
}