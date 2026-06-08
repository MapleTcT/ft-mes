package com.supcon.supfusion.module.registry.service;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;
import com.supcon.supfusion.framework.cloud.i18n.resource.utils.MessageResourceWrapper;
import com.supcon.supfusion.i18n.service.api.MessageResourceService;
import com.supcon.supfusion.module.registry.ModuleEnum;
import com.supcon.supfusion.module.registry.ModuleErrorEnum;
import com.supcon.supfusion.module.registry.ModuleTypeEnum;
import com.supcon.supfusion.module.registry.api.ModuleRegistryApi;
import com.supcon.supfusion.module.registry.dao.mapper.AppMapper;
import com.supcon.supfusion.module.registry.dao.mapper.ModuleMapper;
import com.supcon.supfusion.module.registry.dao.po.AppPO;
import com.supcon.supfusion.module.registry.dao.po.ModulePO;
import com.supcon.supfusion.module.registry.dto.AddModuleAppDTO;
import com.supcon.supfusion.module.registry.dto.AddModuleDTO;
import com.supcon.supfusion.module.registry.dto.ModuleDTO;
import com.supcon.supfusion.module.registry.dto.UpgradeModuleAppDTO;
import com.supcon.supfusion.module.registry.exception.ModuleException;

import lombok.extern.slf4j.Slf4j;


@ServiceApiService
@Slf4j
public class ModuleRegistryServiceImpl implements ModuleRegistryApi {

    @Autowired(required = false)
    private MessageResourceWrapper messageResource;
    @Autowired
    private MessageResourceService messageResourceService;
    @Autowired
    private ModuleMapper moduleMapper;
    @Autowired
    private AppMapper appMapper;
    
    /**
     * @see com.supcon.supfusion.module.registry.api.ModuleRegistryApi#fuzzyQueryModules(String)
     */
    @Override
    public Collection<ModuleDTO> fuzzyQueryModules(String fuzzyName) {
        Map<String, String> modulesMap = messageResourceService.messageResourceSearchModuleMatchCase(fuzzyName, ModuleEnum.MODULE_REGISTRY.getModuleId());
        log.info("模糊查询国际化接口返回: {}, 参数: {}", modulesMap, fuzzyName);
        // 对value进行ASCII码升序, 先将map转化为ModuleDTO TreeSet
        List<ModuleDTO> moduleList = new LinkedList<>();
        for (Map.Entry<String, String> entry : modulesMap.entrySet()) {
            // 根据国际化编码获取模块编号
            ModulePO module = moduleMapper.getByName(entry.getKey());
            if (module == null) {
                log.error("查询模块不存在,模块名国际化编码: {}", entry.getKey());
                continue;
            }
            int indexOf = entry.getValue().indexOf(fuzzyName);
            ModuleDTO moduleDTO = new ModuleDTO();
            moduleDTO.setModuleCode(module.getModuleCode());
            moduleDTO.setModuleId(module.getModuleId());
            moduleDTO.setModuleName(entry.getValue());
            moduleDTO.setIndexof(indexOf);
            moduleList.add(moduleDTO);
        }
        // 根据indexof排序
        Collections.sort(moduleList, (ModuleDTO arg0, ModuleDTO arg1) -> arg0.getIndexof().compareTo(arg1.getIndexof()));
        return moduleList;
    }
    
    /**
     * @see com.supcon.supfusion.module.registry.api.ModuleRegistryApi#queryModules()
     */
    @Override
    public Collection<ModuleDTO> queryModules(ModuleTypeEnum moduleType) {
        Collection<ModuleDTO> modules = new LinkedList<>();
        String name = Optional.ofNullable(moduleType).map(e -> e.name()).orElse(null);
        List<ModulePO> modulePOs = moduleMapper.selectModules(name);
        for (ModulePO modulePO : modulePOs) {
            String moduleName = messageResource.getMessageNotBlank(modulePO.getModuleName());
            ModuleDTO moduleDTO = new ModuleDTO();
            moduleDTO.setModuleCode(modulePO.getModuleCode());
            moduleDTO.setModuleId(modulePO.getModuleId());
            moduleDTO.setModuleName(moduleName);
            moduleDTO.setModuleType(modulePO.getModuleType());
            modules.add(moduleDTO);
        }
        return modules;
    }
    
    @Override
    public Collection<ModuleDTO> queryModules() {
        return queryModules(null);
    }

    /**
     * @see com.supcon.supfusion.module.registry.api.ModuleRegistryApi#getModule(java.lang.String)
     */
    @Override
    public ModuleDTO getModule(String moduleId) {
        ModulePO modulePO = moduleMapper.getOne(moduleId);
        if (modulePO == null) {
            // 应技术公司要求, 此处不抛出异常,改为返回null
            /*throw new ModuleException(ModuleErrorEnum.MODULE_NOT_FOUND_ERROR);*/
            log.error("模块不存在,模块编号: {}", moduleId);
            return null;
        }
        String moduleName = messageResource.getMessageNotBlank(modulePO.getModuleName());
        ModuleDTO moduleDTO = new ModuleDTO();
        moduleDTO.setModuleCode(modulePO.getModuleCode());
        moduleDTO.setModuleId(modulePO.getModuleId());
        moduleDTO.setModuleName(moduleName);
        moduleDTO.setModuleType(modulePO.getModuleType());
        return moduleDTO;
    }
    
    /**
     * @see com.supcon.supfusion.module.registry.api.ModuleRegistryApi#addModule(com.supcon.supfusion.module.registry.dto.AddModuleDTO)
     */
    @Override
    public void addModule(AddModuleDTO addModuleRequest) {
        boolean exist = checkExist(addModuleRequest.getModuleId());
        if (exist) {
            //throw new ModuleException(ModuleErrorEnum.MODULE_HAS_EXIST_ERROR);
            String moduleId = addModuleRequest.getModuleId();
        	String moduleCode = addModuleRequest.getModuleCode();
        	String moduleName = addModuleRequest.getNameOfI18nCode();
        	moduleMapper.update(moduleId, moduleCode, moduleName);
        	return;
        }
        createNewModule(addModuleRequest);
    }

    private void createNewModule(AddModuleDTO addModuleRequest) {
        ModulePO module = new ModulePO();
        module.setId(IDGenerator.newInstance().generate().longValue());
        module.setModuleId(addModuleRequest.getModuleId());
        module.setModuleCode(addModuleRequest.getModuleCode());
        module.setModuleName(addModuleRequest.getNameOfI18nCode());
        module.setModuleType(ModuleTypeEnum.BIZ.name());
        moduleMapper.insert(module);
    }
    
    /**
     * @see com.supcon.supfusion.module.registry.api.ModuleRegistryApi#checkExist(java.lang.String)
     */
    @Override
    public boolean checkExist(String moduleId) {
        ModulePO modulePO = moduleMapper.getOne(moduleId);
        return modulePO != null;
    }

    /**
     * @see com.supcon.supfusion.module.registry.api.ModuleRegistryApi#deleteModule(java.lang.String)
     */
    @Override
    public boolean deleteModule(String moduleId) {
        int result = moduleMapper.deleteOne(moduleId);
        return result > 0;
    }
    
    /**
     * @see com.supcon.supfusion.module.registry.api.ModuleRegistryApi#queryModuleIds(com.supcon.supfusion.module.registry.ModuleTypeEnum)
     */
    @Override
    public Collection<String> queryModuleIds(ModuleTypeEnum moduleType) {
        Collection<String> moduleIds = new LinkedList<>();
        String name = Optional.ofNullable(moduleType).map(ModuleTypeEnum::name).orElse(null);
        List<ModulePO> modulePOs = moduleMapper.selectModules(name);
        for (ModulePO modulePO : modulePOs) {
            moduleIds.add(modulePO.getModuleId());
        }
        return moduleIds;
    }
    
    @Override
    @Transactional
    public void addModuleApp(AddModuleAppDTO addModuleRequest) {
    	String appId = addModuleRequest.getAppId();
    	List<AddModuleDTO> modules = addModuleRequest.getModules();
    	for (int i = 0; i < modules.size(); i++) {
    		AddModuleDTO addModuleDTO = modules.get(i);
    		String moduleId = addModuleDTO.getModuleId();
    		boolean exist = checkExist(moduleId);
            if (exist) {
            	String moduleCode = addModuleDTO.getModuleCode();
            	String moduleName = addModuleDTO.getNameOfI18nCode();
            	moduleMapper.update(moduleId, moduleCode, moduleName);
            } else {
            	createNewModule(addModuleDTO);
            }
            AppPO app = new AppPO();
            app.setId(IDGenerator.newInstance().generate().longValue());
            app.setAppId(appId);
            app.setModuleId(moduleId);
            appMapper.insertapp(app);
    	}
    }
    
    @Override
    @Transactional
    public void deleteModuleApp(String appId) {
    	appMapper.deleteApp(appId);
    	appMapper.deleteAppModule(appId);
    }
    
    @Override
    @Transactional
    public void upgradeModuleApp(UpgradeModuleAppDTO upgradeModuleAppDTO) {
    	String appId = upgradeModuleAppDTO.getAppId();
    	deleteModuleApp(appId);
    	List<AddModuleDTO> modules = upgradeModuleAppDTO.getModules();
    	for (int i = 0; i < modules.size(); i++) {
    		AddModuleDTO addModuleDTO = modules.get(i);
    		String moduleId = addModuleDTO.getModuleId();
    		boolean exist = checkExist(moduleId);
            if (exist) {
            	String moduleCode = addModuleDTO.getModuleCode();
            	String moduleName = addModuleDTO.getNameOfI18nCode();
            	moduleMapper.update(moduleId, moduleCode, moduleName);
            } else {
            	createNewModule(addModuleDTO);
            }
            AppPO app = new AppPO();
            app.setId(IDGenerator.newInstance().generate().longValue());
            app.setAppId(appId);
            app.setModuleId(moduleId);
            appMapper.insertapp(app);
    	}
    }
    
    @Override
    public List<String> singleApp(String appId) {
    	List<String> singleApp = appMapper.singleApp(appId);
    	return singleApp;
    }
    
}
