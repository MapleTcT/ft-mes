package com.supcon.supfusion.module.registry.api;

import java.util.Collection;

import javax.annotation.Nullable;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.module.registry.ModuleTypeEnum;
import com.supcon.supfusion.module.registry.dto.AddModuleDTO;
import com.supcon.supfusion.module.registry.dto.ModuleDTO;

@FeignClient(name = "module-registry")
public interface ModuleRegistryApi {
    
    /**
     * 根据模块名模糊查询
     * @param fuzzyName 
     * @return
     */
    @GetMapping(HttpConstants.URL_SERVICEAPI + HttpConstants.URL_SPLITER + "module-registry/modules/fuzzy")
    @ResponseBody
    Collection<ModuleDTO> fuzzyQueryModules(@RequestParam("fuzzyName") String fuzzyName);
    
    /**
     * 查询所有模块
     * @return 返回模块列表
     */
    @GetMapping(HttpConstants.URL_SERVICEAPI + HttpConstants.URL_SPLITER + "module-registry/modules/type")
    @ResponseBody
    Collection<ModuleDTO> queryModules(@RequestParam("moduleType") ModuleTypeEnum moduleType);
    
    /**
     * 查询所有模块ID
     * @param moduleType 查询系统模块或者业务模块,如果为空则查询所有
     * @return 返回模块ID列表
     */
    @GetMapping(HttpConstants.URL_SERVICEAPI + HttpConstants.URL_SPLITER + "module-registry/moduleIds")
    @ResponseBody
    Collection<String> queryModuleIds(@Nullable@RequestParam("moduleType") ModuleTypeEnum moduleType);
    
    /**
     * 查询所有模块
     * @return 返回模块列表
     */
    @GetMapping(HttpConstants.URL_SERVICEAPI + HttpConstants.URL_SPLITER + "module-registry/modules")
    @ResponseBody
    Collection<ModuleDTO> queryModules();
    
    /**
     * 查询模块详情
     * @param moduleId 模块编号 {@link com.supcon.supfusion.module.registry.ModuleEnum}
     * @return 返回模块详情
     */
    @GetMapping(HttpConstants.URL_SERVICEAPI + HttpConstants.URL_SPLITER + "module-registry/module/{moduleId}")
    @ResponseBody
    ModuleDTO getModule(@PathVariable("moduleId") String moduleId);
    
    /**
     * 新增模块编号
     * @param moduleId
     */
    @PostMapping(HttpConstants.URL_SERVICEAPI + HttpConstants.URL_SPLITER + "module-registry/module")
    void addModule(@RequestBody AddModuleDTO addModuleRequest);
    
    /**
     * 检查模块编码是否存在
     * @param moduleId
     * @return
     */
    @GetMapping(HttpConstants.URL_SERVICEAPI + HttpConstants.URL_SPLITER + "module-registry/module/exist/{moduleId}")
    boolean checkExist(@PathVariable("moduleId") String moduleId);
    
    /**
     * 删除模块
     * @param moduleId
     * @return
     */
    @DeleteMapping(HttpConstants.URL_SERVICEAPI + HttpConstants.URL_SPLITER + "module-registry/module/{moduleId}")
    boolean deleteModule(@PathVariable("moduleId") String moduleId);
    
}
