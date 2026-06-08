package com.supcon.supfusion.base.services.impl;

import com.supcon.supfusion.base.services.ModuleRegistryService;
import com.supcon.supfusion.module.registry.ModuleTypeEnum;
import com.supcon.supfusion.module.registry.api.ModuleRegistryApi;
import com.supcon.supfusion.module.registry.dto.AddModuleAppDTO;
import com.supcon.supfusion.module.registry.dto.AddModuleDTO;
import com.supcon.supfusion.module.registry.dto.ModuleDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
public class ModuleRegistryServiceImpl implements ModuleRegistryService {

    @Autowired
    private ModuleRegistryApi moduleRegistryApi;

    @Override
    public void registryModule(String moduleCode) {
        try {
            AddModuleDTO moduleDTO = new AddModuleDTO();
            moduleDTO.setModuleId(moduleCode.split("_")[0]);
            moduleDTO.setModuleCode(moduleCode);
            moduleDTO.setNameOfI18nCode("reg.moduleName." + moduleCode.split("_")[0]);
            log.info("调用ModuleRegistry注册模块: " + moduleDTO);
            moduleRegistryApi.addModule(moduleDTO);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        log.info("ModuleRegistry注册成功");
    }

    @Override
    public void deleteModule(String moduleCode) {
        moduleRegistryApi.deleteModule(moduleCode.split("_")[0]);
    }

}
