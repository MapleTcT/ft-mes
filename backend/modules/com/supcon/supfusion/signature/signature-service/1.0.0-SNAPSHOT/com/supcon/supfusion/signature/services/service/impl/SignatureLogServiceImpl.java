package com.supcon.supfusion.signature.services.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.framework.cloud.common.result.Pagination;
import com.supcon.supfusion.signature.base.enums.DatabaseType;
import com.supcon.supfusion.signature.base.enums.SignatureColumn;
import com.supcon.supfusion.signature.dao.entity.SignatureLog;
import com.supcon.supfusion.signature.dao.mappers.SignatureLogMapper;
import com.supcon.supfusion.signature.services.bo.LogQueryCondition;
import com.supcon.supfusion.signature.services.service.SignatureLogService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author zhang yafei
 */
@Service
@Slf4j
public class SignatureLogServiceImpl extends ServiceImpl<SignatureLogMapper, SignatureLog> implements SignatureLogService {

    @Autowired
    private SignatureLogMapper signatureLogMapper;

    @Override
    public List<SignatureLog> getSignaureLogs(LogQueryCondition signatureColumnCondition, Pagination pagination) {
        int pageSize = pagination.getPageSize();
        Integer count = signatureLogMapper.signaureLogsBylikeCount(signatureColumnCondition.getLikeCondition(),
                signatureColumnCondition.getInCondition(),
                signatureColumnCondition.getTimeCondition());
        pagination.setTotal(count);
        List<SignatureLog> signatureLogs = null;
        if (count > 0) {
            signatureLogs = signatureLogMapper.getSignaureLogsBylike(signatureColumnCondition.getLikeCondition(),
                    signatureColumnCondition.getInCondition(),
                    signatureColumnCondition.getTimeCondition(), (pagination.getCurrent() - 1) * pageSize, pageSize);
        }
        return signatureLogs;
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