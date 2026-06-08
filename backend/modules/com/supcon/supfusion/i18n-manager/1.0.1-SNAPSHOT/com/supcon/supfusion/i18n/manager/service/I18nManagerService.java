package com.supcon.supfusion.i18n.manager.service;

import java.util.Collection;
import java.util.List;

import com.supcon.supfusion.module.registry.dto.ModuleDTO;

/**
 * @Description: manager service接口层
 * @Author: ShenZhiqiang
 * @Date: Create in  20:25 2020/6/19
 * @Modified:
 */
public interface I18nManagerService{

    /**
     * 获取所有模块code 包括系统模块和业务模块
     * @return
     */
    List<String> getAllModuleCode();
    /**
     * 校验模块是否存在
     * @param moduleId
     * @return
     */
    boolean moduleExists(String moduleId);
    /**
     * 获取枚举类中定义的模块code
     * @return
     */
    List<String> getModuleEnumModuleCode();

    Collection<ModuleDTO> queryModules();

    ModuleDTO getModule(String var1);

    /**
     * 获取所有系统模块code
     * @return
     */
    List<String> querySystemModules();


    /**
     * 获取所有业务模块code
     * @return
     */
    List<String> queryBIZModules();

}
