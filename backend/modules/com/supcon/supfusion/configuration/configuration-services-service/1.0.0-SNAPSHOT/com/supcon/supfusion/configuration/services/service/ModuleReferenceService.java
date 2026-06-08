package com.supcon.supfusion.configuration.services.service;

import com.supcon.supfusion.configuration.services.entity.Module;
import com.supcon.supfusion.configuration.services.entity.ModuleReference;

import java.util.List;

public interface ModuleReferenceService {

	void save(ModuleReference entity);
	/**
	 * 根据module 查询所有valid=true的引用模块记录
	 * 
	 * @param module
	 * @return
	 */
	List<ModuleReference> getReferences(Module module);

	/**
	 * 根据module和target查询的引用模块记录
	 * 
	 * @param module
	 * @param target
	 * @return
	 */
	ModuleReference getReferenceWithNoValid(Module module, Module target);

	/**
	 * 根据module和target查询valid=true的引用模块记录
	 * 
	 * @param module
	 * @param target
	 * @return
	 */
	ModuleReference getReferenceWithValid(Module module, Module target);

	/**
	 * 物理删除ModuleReference
	 * 
	 * @param reference
	 */
	void deleteReferencePhysical(ModuleReference reference);

	/**
	 * 检查模块是否被依赖模块关联  true代表有关联，false代表不关联
	 * 
	 * @param moduleCode
	 * @param targetId
	 * @param delIds
	 * @return
	 */
	Boolean isModuleReference(String moduleCode, String targetId, String delIds);
}
