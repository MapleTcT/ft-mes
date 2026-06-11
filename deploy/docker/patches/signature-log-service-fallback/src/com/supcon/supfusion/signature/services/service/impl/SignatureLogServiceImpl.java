package com.supcon.supfusion.signature.services.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.framework.cloud.common.result.Pagination;
import com.supcon.supfusion.signature.base.enums.SignatureColumn;
import com.supcon.supfusion.signature.dao.entity.SignatureLog;
import com.supcon.supfusion.signature.dao.mappers.SignatureLogMapper;
import com.supcon.supfusion.signature.services.bo.LogQueryCondition;
import com.supcon.supfusion.signature.services.service.SignatureLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class SignatureLogServiceImpl extends ServiceImpl<SignatureLogMapper, SignatureLog> implements SignatureLogService {

    @Autowired
    private SignatureLogMapper signatureLogMapper;

    @Override
    public List<SignatureLog> getSignaureLogs(LogQueryCondition signatureColumnCondition, Pagination pagination) {
        QueryWrapper<SignatureLog> wrapper = buildQueryWrapper(signatureColumnCondition);
        Integer count = signatureLogMapper.selectCount(wrapper);
        pagination.setTotal(count == null ? 0 : count);
        if (count == null || count <= 0) {
            return java.util.Collections.emptyList();
        }

        int current = pagination.getCurrent() <= 0 ? 1 : pagination.getCurrent();
        int pageSize = pagination.getPageSize() <= 0 ? 20 : pagination.getPageSize();
        Page<SignatureLog> page = new Page<>(current, pageSize);
        wrapper.orderByDesc(SignatureColumn.FIRST_SIGN_TIME.name());
        return signatureLogMapper.selectPage(page, wrapper).getRecords();
    }

    private QueryWrapper<SignatureLog> buildQueryWrapper(LogQueryCondition condition) {
        QueryWrapper<SignatureLog> wrapper = new QueryWrapper<>();
        if (condition == null) {
            return wrapper;
        }

        appendLikeConditions(wrapper, condition.getLikeCondition());
        appendInConditions(wrapper, condition.getInCondition());
        appendTimeConditions(wrapper, condition.getTimeCondition());
        return wrapper;
    }

    private void appendLikeConditions(QueryWrapper<SignatureLog> wrapper, Map<SignatureColumn, List<String>> conditions) {
        if (conditions == null) {
            return;
        }
        for (Map.Entry<SignatureColumn, List<String>> entry : conditions.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
            final String column = entry.getKey().name();
            final List<String> values = nonBlankValues(entry.getValue());
            if (values.isEmpty()) {
                continue;
            }
            wrapper.and(group -> {
                boolean first = true;
                for (String value : values) {
                    if (first) {
                        group.like(column, value);
                        first = false;
                    } else {
                        group.or().like(column, value);
                    }
                }
            });
        }
    }

    private void appendInConditions(QueryWrapper<SignatureLog> wrapper, Map<SignatureColumn, List<String>> conditions) {
        if (conditions == null) {
            return;
        }
        for (Map.Entry<SignatureColumn, List<String>> entry : conditions.entrySet()) {
            List<String> values = entry.getValue() == null ? java.util.Collections.emptyList() : nonBlankValues(entry.getValue());
            if (entry.getKey() != null && !values.isEmpty()) {
                wrapper.in(entry.getKey().name(), values);
            }
        }
    }

    private void appendTimeConditions(QueryWrapper<SignatureLog> wrapper, Map<SignatureColumn, List<String>> conditions) {
        if (conditions == null) {
            return;
        }
        for (Map.Entry<SignatureColumn, List<String>> entry : conditions.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            String column = entry.getKey().name();
            List<String> values = entry.getValue();
            if (values.size() > 0 && values.get(0) != null && !values.get(0).trim().isEmpty()) {
                wrapper.ge(column, values.get(0));
            }
            if (values.size() > 1 && values.get(1) != null && !values.get(1).trim().isEmpty()) {
                wrapper.le(column, values.get(1));
            }
        }
    }

    private List<String> nonBlankValues(List<String> values) {
        List<String> result = new ArrayList<>();
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                result.add(value);
            }
        }
        return result;
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
