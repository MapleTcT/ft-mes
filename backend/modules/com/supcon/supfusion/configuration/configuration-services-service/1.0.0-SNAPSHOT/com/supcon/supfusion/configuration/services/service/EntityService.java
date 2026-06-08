package com.supcon.supfusion.configuration.services.service;

import com.supcon.supfusion.configuration.services.entity.Entity;
import com.supcon.supfusion.configuration.services.entity.Module;
import com.supcon.supfusion.framework.scaffold.hibernate.page.Page;
import org.hibernate.criterion.Criterion;

import java.io.PrintWriter;
import java.util.List;
import java.util.Set;

/**
 * 实体(项目)操作服务.
 * 
 * @author songjiawei
 * 
 */
public interface EntityService {
	/**
	 * 新建/修改一个实体(项目).
	 * 
	 * @param entity
	 *            实体.
	 */
	void saveEntity(Entity entity);

	/**
	 * 启用关注之后，需要视图数据重新同步一遍
	 * @param entity
	 */
	public void dealPayCloseAttention(Entity entity);
	
	/**
	 * 删除一个实体(项目).<br />
	 * 并不是物理删除,而是置
	 * 
	 * @param entity
	 *            要删除的实体对象.
	 */
	String deleteEntity(Entity entity);
	/**
	 * 删除一个实体(项目).<br />
	 * 并不是物理删除,而是置valid 为false
	 * 
	 * @param id
	 *            要删除的实体ID.
	 */
	void deleteEntity(String entityCode);

	Entity getEntity(String code);

	/**
	 * 分页获取实体(项目)列表.
	 * 
	 * @param page
	 *            分页包裹对象
	 * @param ec
	 *            实体类别
	 * @param company
	 *            公司条件,一般从HttpSession获取
	 * @return 带实体列表数据的分页包裹对象.
	 */
	Page<Entity> findEntities(Page<Entity> page, Criterion... criterions);
	/**
	 * 分页获取实体(项目)列表.
	 * 
	 * @param page
	 *            分页包裹对象
	 * @param module
	 *            实体类别
	 * @return 带实体列表数据的分页包裹对象.
	 */

	Page<Entity> findEntities(Page<Entity> page, Module module);
	List<Entity> findEntities(Module module);
	String deleteEntityPhysical(String entityCode, Boolean deleteType);
	/**
	 * 更新实体中所有流程的移动标记
	 * @param entityCode
	 */
	void updateEntityProcessMobile(String entityCode);
	
	/**
	 * 跨模块实体迁移
	 * @param entities 迁移实体
	 * @param targetModule 目标模块
	 */
	void migrateEntity(PrintWriter out, List<Entity> entities, Module targetModule) throws Exception;

	Set<String> checkDelete(Entity e);

}