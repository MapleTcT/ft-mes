package com.supcon.supfusion.systemcode.webapi;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.supcon.supfusion.framework.cloud.annotation.InternalApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import com.supcon.supfusion.systemcode.dao.po.SystemCodeDetailPO;
import com.supcon.supfusion.systemcode.dao.po.SystemCodePO;
import com.supcon.supfusion.systemcode.service.SystemCodeService;
import com.supcon.supfusion.systemcode.webapi.vo.CodeValueResultVO;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Setter
@Getter
@Validated
@InternalApi(path = "baseService" + HttpConstants.URL_SPLITER + "systemCode")
public class SysBaseServiceController extends BaseController {

    @Autowired
    private SystemCodeService systemCodeService;

    @GetMapping(value = "/systemCodeJson")
    @ResponseBody
    Map queryEntityByCode(@RequestParam("systemEntityCode") String systemEntityCode) {
        List<SystemCodePO> systemCodePOList = systemCodeService.queryValueListNoPage(systemEntityCode, "", "", "");
        HashMap<String, String> map = new HashMap<>();
        for (SystemCodePO systemCodePO : systemCodePOList) {
            String code = systemCodePO.getEntityCode() + "/" + systemCodePO.getCode();
            map.put(code, systemCodeService.queryDisplayName(systemCodePO));
        }
        HashMap result = new HashMap();
        result.put("code", 200);
        result.put("message","操作成功");
        result.put("success", true);
        result.put("data", map);
        return result;
    }

    @GetMapping(value = "/codeValueManager/valueTreeList")
    @ResponseBody
    Map valueTreeList(@RequestParam("systemEntityCode") String systemEntityCode, String id) {
        List<SystemCodeDetailPO> systemCodeDetailPOList = systemCodeService.queryCodeValueBaseTree(systemEntityCode, id);
        List<CodeValueResultVO> codeValueResultVOList = JSONArray.parseArray(JSON.toJSONString(systemCodeDetailPOList), CodeValueResultVO.class);
        HashMap result = new HashMap();
        result.put("code", 200);
        result.put("message","操作成功");
        result.put("success", true);
        result.put("data", codeValueResultVOList);
        return result;
    }
}
