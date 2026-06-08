package com.supcon.supfusion.i18n.manager.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.supcon.supfusion.i18n.manager.service.I18nManagerService;
import com.supcon.supfusion.module.registry.ModuleEnum;
import com.supcon.supfusion.module.registry.ModuleTypeEnum;
import com.supcon.supfusion.module.registry.api.ModuleRegistryApi;
import com.supcon.supfusion.module.registry.dto.ModuleDTO;

/**
 * @Description:
 * @Author: ShenZhiqiang
 * @Date: Create in  9:22 2020/6/22
 * @Modified:
 */
@Service
public class I18nManagerServiceImpl implements I18nManagerService {

    @Autowired
    private ModuleRegistryApi moduleRegistryApi;

    @Override
    public List<String> getAllModuleCode() {
        Set<String> moduleCodeSets = new HashSet<String>();
        ModuleEnum[] module = ModuleEnum.values();
        for (ModuleEnum moduleCode : module) {
            moduleCodeSets.add(moduleCode.getModuleId());
        }
        Collection<String> moduleIds = moduleRegistryApi.queryModuleIds(null);
        if (moduleIds != null && moduleIds.size() > 0) {
            moduleIds.forEach(moduleCode -> {
                moduleCodeSets.add(moduleCode);
            });
        }
        List<String> moduleCodes = new ArrayList<>();
        if (moduleCodeSets.size() > 0) {
            moduleCodeSets.forEach(moduleCode -> {
                moduleCodes.add(moduleCode);
            });
        }
        return moduleCodes;
    }

    @Override
    public List<String> getModuleEnumModuleCode() {
        List<String> moduleCodes = new ArrayList<>();
        ModuleEnum[] module = ModuleEnum.values();
        for (ModuleEnum moduleCode : module) {
            moduleCodes.add(moduleCode.getModuleId());
        }
        return moduleCodes;
    }

    @Override
    public Collection<ModuleDTO> queryModules() {
        return moduleRegistryApi.queryModules();
    }

    @Override
    public ModuleDTO getModule(String var1) {
        return moduleRegistryApi.getModule(var1);
    }

    @Override
    public List<String> querySystemModules() {
        List<String> moduleCodes = new ArrayList<>();
        Collection<ModuleDTO> ModuleDTOs = moduleRegistryApi.queryModules(ModuleTypeEnum.SYSTEM);
        if (ModuleDTOs != null && ModuleDTOs.size() > 0) {
            ModuleDTOs.forEach(ModuleDTO -> {
                moduleCodes.add(ModuleDTO.getModuleId());
            });
        }
        return moduleCodes;
    }

    @Override
    public List<String> queryBIZModules() {
        List<String> moduleCodes = new ArrayList<>();
        Collection<ModuleDTO> ModuleDTOs = moduleRegistryApi.queryModules(ModuleTypeEnum.BIZ);
        if (ModuleDTOs != null && ModuleDTOs.size() > 0) {
            ModuleDTOs.forEach(ModuleDTO -> {
                moduleCodes.add(ModuleDTO.getModuleId());
            });
        }
        return moduleCodes;
    }

	@Override
	public boolean moduleExists(String moduleId) {
		return moduleRegistryApi.checkExist(moduleId);
	}
}
