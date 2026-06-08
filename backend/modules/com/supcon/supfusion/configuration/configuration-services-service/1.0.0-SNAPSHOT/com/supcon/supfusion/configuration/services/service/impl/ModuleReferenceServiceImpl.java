package com.supcon.supfusion.configuration.services.service.impl;

import com.supcon.supfusion.configuration.services.dao.ModuleReferenceDao;
import com.supcon.supfusion.configuration.services.entity.Module;
import com.supcon.supfusion.configuration.services.entity.ModuleReference;
import com.supcon.supfusion.configuration.services.service.ModuleReferenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ModuleReferenceServiceImpl implements ModuleReferenceService {

	@Autowired
	private ModuleReferenceDao moduleReferenceDao;

	@Override
	public ModuleReference getReferenceWithNoValid(Module module, Module target) {
		List<ModuleReference> references = moduleReferenceDao.findByHql(
				"From ModuleReference where module = ? and target = ?", module,
				target);
		if (references != null && references.size() == 1) {
			return references.get(0);
		} else {
			return new ModuleReference();
		}
	}

	@Override
	public ModuleReference getReferenceWithValid(Module module, Module target) {
		List<ModuleReference> references = moduleReferenceDao
				.findByHql(
						"From ModuleReference where module = ? and target = ? and valid = true",
						module, target);
		if (references != null && references.size() == 1) {
			return references.get(0);
		} else {
			return new ModuleReference();
		}
	}

	@Override
	public List<ModuleReference> getReferences(Module module) {
		return moduleReferenceDao.findByHql(
				"From ModuleReference where module = ? and valid = true",
				module);
	}

	@Override
	public void deleteReferencePhysical(ModuleReference reference) {
		moduleReferenceDao.deletePhysical(reference);
	}

	@Override
	public Boolean isModuleReference(String moduleCode, String targetId, String delIds) {
//		List<ModuleReference> moduleReferences = moduleReferenceDao.findByHql(
//				"From ModuleReference where target.code = ? and module.code = ? and valid = true",
//				new Object[] {targetId, moduleCode});
//		if (null != moduleReferences && moduleReferences.size() > 0) {
//			for (ModuleReference reference : moduleReferences) {
//				if (null != targetId
//						&& delIds.indexOf(reference.getTarget().getCode() + ",") == -1) {	//不在这次操作的引用模块的删除列表里面
//					return true;
//				}
//			}
//		}		
		List<ModuleReference> moduleReferences = moduleReferenceDao.findByHql(
				"From ModuleReference where  module.code = ? and valid = true",
				targetId);
		if (null != moduleReferences && moduleReferences.size() > 0) {
			for (ModuleReference reference : moduleReferences) {
				if (null != targetId
						&& moduleCode.endsWith(reference.getTarget().getCode())) {
					return true;
				}
			}
		}
		return false;
	}


	@Override
	public void save(ModuleReference entity) {
		moduleReferenceDao.save(entity);
	}

	@Autowired
	public void setModuleReferenceDao(ModuleReferenceDao moduleReferenceDao) {
		this.moduleReferenceDao = moduleReferenceDao;
	}

}
