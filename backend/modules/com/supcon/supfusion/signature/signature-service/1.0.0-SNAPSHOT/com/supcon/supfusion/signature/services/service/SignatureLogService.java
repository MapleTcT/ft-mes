package com.supcon.supfusion.signature.services.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.supcon.supfusion.framework.cloud.common.result.Pagination;
import com.supcon.supfusion.signature.base.enums.SignatureColumn;
import com.supcon.supfusion.signature.dao.entity.SignatureLog;
import com.supcon.supfusion.signature.dao.mappers.SignatureLogMapper;
import com.supcon.supfusion.signature.services.bo.LogQueryCondition;

import java.util.List;
import java.util.Map;

/**
 * @author zhang yafei
 */
public interface SignatureLogService  extends IService<SignatureLog> {
    List<SignatureLog> getSignaureLogs(LogQueryCondition signatureColumnCondition, Pagination pagination);
    List<SignatureLog> getSignaureLogsByIds(List<String> ids);
    SignatureLog getSignaureLogsByIds(String id);
    void saveSignatureLog(SignatureLog signatureLog);
    void batchSaveSignatureLog(List<SignatureLog> signatureLogs);
}
