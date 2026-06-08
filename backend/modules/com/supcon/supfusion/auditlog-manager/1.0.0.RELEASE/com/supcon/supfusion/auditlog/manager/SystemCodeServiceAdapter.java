package com.supcon.supfusion.auditlog.manager;

import com.supcon.supfusion.systemcode.api.dto.SystemCodeResultDTO;

/**
 * @author caokele
 */
public interface SystemCodeServiceAdapter {
    /**
     * 获取系统编码
     * @param code entityCode/code
     * @return 系统编码
     */
    SystemCodeResultDTO getSystemCodeByCode(String code);

    /**
     * 获取系统编码
     * @param entityCode 实体编码
     * @param code 系统编码
     * @return 系统编码
     */
    SystemCodeResultDTO getSystemCode(String entityCode, String code);
}
