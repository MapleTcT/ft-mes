package com.supcon.supfusion.auth.manager;

import com.supcon.supfusion.systemcode.api.dto.SystemCodeResultDTO;

/**
 * @author caokele
 */
public interface SystemCodeServiceAdapter {
    SystemCodeResultDTO getSystemCodeByCode(String code);
}
