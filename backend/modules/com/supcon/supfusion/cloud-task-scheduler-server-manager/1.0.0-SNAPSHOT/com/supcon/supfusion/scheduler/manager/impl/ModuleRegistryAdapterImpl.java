package com.supcon.supfusion.scheduler.manager.impl;

import com.supcon.supfusion.module.registry.ModuleTypeEnum;
import com.supcon.supfusion.module.registry.api.ModuleRegistryApi;
import com.supcon.supfusion.module.registry.dto.ModuleDTO;
import com.supcon.supfusion.scheduler.manager.ModuleRegistryAdapter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;

/**
 * @author caokele
 */
@Service
public class ModuleRegistryAdapterImpl implements ModuleRegistryAdapter {
    @Autowired
    private ModuleRegistryApi moduleRegistryApi;


    @Override
    public Collection<ModuleDTO> queryModules() {
        Collection<ModuleDTO> moduleDTOS = moduleRegistryApi.queryModules(ModuleTypeEnum.BIZ);
        for (ModuleDTO moduleDTO : moduleDTOS) {
            if(StringUtils.isEmpty(moduleDTO.getModuleCode())){
                moduleDTO.setModuleCode(moduleDTO.getModuleId());
            }
            moduleDTO.setModuleCode(moduleDTO.getModuleCode().split("_")[0]);
        }
        return moduleDTOS;
    }
}
