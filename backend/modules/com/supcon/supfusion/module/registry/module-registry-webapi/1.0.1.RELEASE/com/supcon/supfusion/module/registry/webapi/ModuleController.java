package com.supcon.supfusion.module.registry.webapi;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.supcon.supfusion.framework.cloud.annotation.InternalApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import com.supcon.supfusion.module.registry.api.ModuleRegistryApi;
import com.supcon.supfusion.module.registry.dto.ModuleDTO;
import com.supcon.supfusion.module.registry.webapi.vo.ModuleVO;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * @Description: 模块管理接口
 * @CreateDate: 2020/07/22
 */
@InternalApi(path = HttpConstants.URL_INTERNALAPI + HttpConstants.URL_SPLITER + "module-registry" + HttpConstants.URL_SPLITER + "v1")
@Api(tags = "模块管理", value = "模块管理文档说明", hidden = true)
public class ModuleController extends BaseController {

    @Autowired
    private ModuleRegistryApi moduleRegistryApi;
    
    /**
     * 查询模块列表, 支持根据模块名模糊查询
     * @param request
     * @return
     */
    @GetMapping("/modules")
    @ResponseBody
    @ApiOperation(value="获取模块列表V1接口", httpMethod="GET")
    public ListResult<ModuleVO> queryModules(HttpServletRequest request) {
        String fuzzyName = request.getParameter("fuzzyName");
        Collection<ModuleDTO> moduleDtos = StringUtils.isEmpty(fuzzyName) 
                ? moduleRegistryApi.queryModules()
                : moduleRegistryApi.fuzzyQueryModules(fuzzyName);
        List<ModuleVO> modules = new LinkedList<>();
        for (ModuleDTO moduleDto : moduleDtos) {
            modules.add(new ModuleVO(moduleDto.getModuleId(), moduleDto.getModuleCode(), moduleDto.getModuleName(), moduleDto.getModuleType()));
        }
        return new ListResult<>(modules);
    }
    
    @GetMapping("/module")
    @ResponseBody
    @ApiOperation(value="获取模块详情V1接口", httpMethod="GET")
    public ModuleVO getModule(HttpServletRequest request) {
        String moduleId = request.getParameter("moduleId");
        ModuleDTO moduleDto = moduleRegistryApi.getModule(moduleId);
        return new ModuleVO(moduleDto.getModuleId(), moduleDto.getModuleCode(), moduleDto.getModuleName(), moduleDto.getModuleType());
    }
}

