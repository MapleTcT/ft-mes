package com.supcon.supfusion.auditlog.manager.impl;

import com.supcon.supfusion.auditlog.manager.SystemCodeServiceAdapter;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.systemcode.api.SystemCodeApiService;
import com.supcon.supfusion.systemcode.api.dto.SystemCodeResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author caokele
 */
@Slf4j
@Service
public class SystemCodeServiceAdapterImpl implements SystemCodeServiceAdapter {
    private static final String SEPARATE_CHAR = "/";
    @Autowired
    private SystemCodeApiService systemCodeApiService;

    @Override
    public SystemCodeResultDTO getSystemCodeByCode(String code) {
        String[] codeArray = code.split(SEPARATE_CHAR);
        Result<SystemCodeResultDTO> result = systemCodeApiService.queryValueByCode(codeArray[0], codeArray[1]);
        return result.getData();
    }

    @Override
    public SystemCodeResultDTO getSystemCode(String entityCode, String code) {
        Result<SystemCodeResultDTO> result = systemCodeApiService.queryValueByCode(entityCode, code);
        return result.getData();
    }
}
