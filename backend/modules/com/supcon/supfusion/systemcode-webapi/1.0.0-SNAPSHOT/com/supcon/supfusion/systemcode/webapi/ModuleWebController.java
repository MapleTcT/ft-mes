package com.supcon.supfusion.systemcode.webapi;

import com.supcon.supfusion.framework.cloud.annotation.InternalApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import com.supcon.supfusion.module.registry.dto.ModuleDTO;
import com.supcon.supfusion.systemcode.service.ModuleService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Collection;

/**
 * 模块控制类
 *
 * @author
 * @date 20-5-11 下午14:15
 */
@Slf4j
@Setter
@Getter
@Validated
@InternalApi(path = HttpConstants.URL_INTERNALAPI + HttpConstants.URL_SPLITER + "systemcode" + HttpConstants.URL_SPLITER + "v1")
public class ModuleWebController extends BaseController {

    @Autowired
    ModuleService moduleService;

    @GetMapping(value = "/modules")
    @ResponseBody
    ListResult<ModuleDTO> queryModuleList(){
        Collection<ModuleDTO> moduleDTOList = moduleService.queryModuleList();
        return new ListResult<>(moduleDTOList);
    }
}
