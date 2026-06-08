package com.supcon.supfusion.signature.services.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.framework.cloud.common.result.Pagination;
import com.supcon.supfusion.signature.dao.entity.SignatureLog;
import com.supcon.supfusion.signature.dao.mappers.SignatureLogMapper;
import com.supcon.supfusion.signature.services.bo.LogQueryCondition;
import com.supcon.supfusion.signature.services.service.SignatureLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class SignatureLogServiceImpl extends ServiceImpl<SignatureLogMapper, SignatureLog> implements SignatureLogService {

    @Autowired
    private SignatureLogMapper signatureLogMapper;

    @Override
    public List<SignatureLog> getSignaureLogs(LogQueryCondition signatureColumnCondition, Pagination pagination) {
        try {
            int pageSize = pagination.getPageSize();
            Integer count = signatureLogMapper.signaureLogsBylikeCount(
                    signatureColumnCondition.getLikeCondition(),
                    signatureColumnCondition.getInCondition(),
                    signatureColumnCondition.getTimeCondition());
            pagination.setTotal(count);
            if (count != null && count > 0) {
                return signatureLogMapper.getSignaureLogsBylike(
                        signatureColumnCondition.getLikeCondition(),
                        signatureColumnCondition.getInCondition(),
                        signatureColumnCondition.getTimeCondition(),
                        (pagination.getCurrent() - 1) * pageSize,
                        pageSize);
            }
        } catch (RuntimeException ex) {
            log.warn("signature log mapper query failed, returning empty page", ex);
        }
        pagination.setTotal(0);
        return Collections.emptyList();
    }

    @Override
    public List<SignatureLog> getSignaureLogsByIds(List<String> ids) {
        return signatureLogMapper.selectBatchIds(ids);
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public SignatureLog getSignaureLogsByIds(String id) {
        return signatureLogMapper.selectById(id);
    }

    @Override
    @Transactional
    public void saveSignatureLog(SignatureLog signatureLog) {
        save(signatureLog);
    }

    @Override
    @Transactional
    public void batchSaveSignatureLog(List<SignatureLog> signatureLogs) {
        saveBatch(signatureLogs);
    }
}
