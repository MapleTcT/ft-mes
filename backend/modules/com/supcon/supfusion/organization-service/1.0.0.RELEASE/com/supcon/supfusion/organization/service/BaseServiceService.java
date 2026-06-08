package com.supcon.supfusion.organization.service;

import com.alibaba.fastjson.JSONObject;
import com.supcon.supfusion.organization.service.bo.person.SystemCodeBO;
import com.supcon.supfusion.systemcode.api.dto.SystemCodeResultDTO;

import java.util.List;
import java.util.Map;

public interface BaseServiceService {
    JSONObject transferToJSON(JSONObject json, String includes, String type);

    /**
     * 获取助记码
     * @param name
     * @return
     */
    List<String> generateZhujima(String name);

    /**
     * 系统编码sys_gender/male筛选系统编码
     * @param entityCodeAndCode
     * @param entityCodeMap
     * @return
     */
    SystemCodeBO findSystemCode(String entityCodeAndCode, Map<String, List<SystemCodeResultDTO>> entityCodeMap);
}
