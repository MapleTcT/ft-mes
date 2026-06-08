package com.supcon.supfusion.signature.services.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.supcon.supfusion.framework.cloud.common.result.Pagination;
import com.supcon.supfusion.signature.base.enums.SignatureColumn;
import com.supcon.supfusion.signature.dao.entity.SignatureExcel;
import com.supcon.supfusion.signature.dao.entity.SignatureLog;
import com.supcon.supfusion.signature.dao.mappers.SignatureExcelMapper;
import com.supcon.supfusion.signature.services.bo.LogQueryCondition;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * @author zhang yafei
 */
public interface SignatureExcelService extends IService<SignatureExcel> {

    SignatureExcel createExportTask();

    void exportExcel(SignatureExcel signatureExcel, Pagination pagination, LogQueryCondition signatureColumnCondition, String fileName, Boolean isAll, List<String> ids);

    SignatureExcel queryStatus(Long id);
}
