package com.supcon.supfusion.auth.manager.Impl;

import com.supcon.supfusion.auth.manager.SystemCodeServiceAdapter;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.systemcode.api.SystemCodeApiService;
import com.supcon.supfusion.systemcode.api.dto.SystemCodeResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
}
