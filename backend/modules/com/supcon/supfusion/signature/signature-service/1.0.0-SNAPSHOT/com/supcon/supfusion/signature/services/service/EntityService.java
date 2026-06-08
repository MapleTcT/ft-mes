package com.supcon.supfusion.signature.services.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.supcon.supfusion.signature.dao.entity.Entity;
import java.util.List;

/**
 * 实体(项目)操作服务.
 * 
 * @author songjiawei
 * 
 */

public interface EntityService extends IService<Entity> {
	List<Entity> findEntities(String moduleCode);

}