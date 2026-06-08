package com.supcon.supfusion.signature.services.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.framework.cloud.common.result.Pagination;
import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;
import com.supcon.supfusion.signature.base.constant.SinatureConstants;
import com.supcon.supfusion.signature.base.i18n.SignatureInternationalResource;
import com.supcon.supfusion.signature.base.untils.SignatureType;
import com.supcon.supfusion.signature.dao.entity.SignatureExcel;
import com.supcon.supfusion.signature.dao.entity.SignatureLog;
import com.supcon.supfusion.signature.dao.mappers.SignatureExcelMapper;
import com.supcon.supfusion.signature.services.bo.LogQueryCondition;
import com.supcon.supfusion.signature.services.service.SignatureExcelService;
import com.supcon.supfusion.signature.services.service.SignatureLogService;
import com.supcon.supfusion.signature.services.utils.callback.ExcelCallback;
import com.supcon.supfusion.signature.services.utils.excel.ExcelUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author zhang yafei
 */
@Service
@Slf4j
public class SignatureExcelServiceImpl extends ServiceImpl<SignatureExcelMapper,SignatureExcel> implements SignatureExcelService {
    @Autowired
    private SignatureLogService signatureLogService;
    @Autowired
    SignatureExcelMapper signatureExcelMapper;
    @Autowired
    private SignatureInternationalResource signatureInternationalResource;

    @Override
    @Transactional
    @Async
    public void exportExcel(SignatureExcel signatureExcel, Pagination pagination, LogQueryCondition signatureColumnCondition, String fileName, Boolean isAll, List<String> ids) {
        //开始创建Excel
        String path = SinatureConstants.EXCEL_PATH + fileName;
        try {
            AtomicBoolean sign = new AtomicBoolean(true);
            ExcelCallback excelCallback = new ExcelCallback() {
                @Override
                public List<SignatureLog> getData(int pageIndex) {
                    List<SignatureLog> signatureLogs = null;
                    if (isAll) {
                        pagination.setCurrent(pageIndex);
                        signatureLogs = signatureLogService.getSignaureLogs(signatureColumnCondition,pagination);
                    } else {
                        if (sign.get()) {
                            List<SignatureLog> signaureLogs = signatureLogService.getSignaureLogsByIds(ids);
                            sign.set(false);
                            signatureLogs = signaureLogs;
                        }

                    }
                    if (signatureLogs != null){
                        signatureLogs.forEach(signatureLog -> {
                            signatureLog.setSignatureType(SignatureType.getType(signatureLog.getSignatureType()));
                        });
                    }
                    return signatureLogs;
                }
            };
            ExcelUtils.createSignatureLogExcelFile(excelCallback,path);
            signatureExcel.setStatus(2);
        }catch (Exception e){
            String message = e.getMessage();
            signatureExcel.setStatus(3);
            signatureExcel.setErrorMessage(message);
            log.error("导出失败:" + message);
        }
        super.saveOrUpdate(signatureExcel);
    }


    @Override
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public SignatureExcel queryStatus(Long id) {
        return signatureExcelMapper.selectById(id);
    }

    @Override
    @Transactional
    public SignatureExcel createExportTask() {
        SignatureExcel signatureExcel = new SignatureExcel();
        long id = IDGenerator.newInstance().generate().longValue();
        String fileName = id + SinatureConstants.STR_POINT +SinatureConstants.XLSX_LOW;
        signatureExcel.setId(id);
        signatureExcel.setStatus(1);
        signatureExcel.setValid(1);
        signatureExcel.setFileName(fileName);
        signatureExcel.setOperateType("export");
        Date date = new Date();
        signatureExcel.setCreateTime(date);
        signatureExcel.setModifyTime(date);
        super.save(signatureExcel);

        return signatureExcel;
    }


}
