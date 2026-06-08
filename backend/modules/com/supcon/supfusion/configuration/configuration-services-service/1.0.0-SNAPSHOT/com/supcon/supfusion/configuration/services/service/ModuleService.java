package com.supcon.supfusion.configuration.services.service;

import com.supcon.supfusion.configuration.services.exceptions.EcException;
import com.supcon.supfusion.base.entities.MenuInfo;
import com.supcon.supfusion.configuration.services.entity.Entity;
import com.supcon.supfusion.configuration.services.entity.Module;
import com.supcon.supfusion.configuration.services.entity.View;
import com.supcon.supfusion.configuration.services.entity.*;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ModuleService {

	Module getModule(String code, boolean full);

	/**
	 * 获取模块对象.
	 * 
	 * @param code
	 *            模块CODE
	 * @return 模块实例
	 */

	Module getModule(String code);

	/**
	 * 新增/修改模块.
	 * 
	 * @param module
	 *            模型模块
	 */

	void saveModule(Module module);
	
	/**
	 * 新增/修改模块.
	 * 
	 * @param module
	 * @param map
	 *  保存、修改模块
	 */

	void saveModule(Module module, Map map);

	/**
	 * 保存依赖模块
	 * @param module
	 * @param map
	 */
	void saveModuleRelation(Module module, Map map);
	
	/**
	 * 保存引用模块
	 * @param module
	 */
	void saveModuleReference(Module module);

	/**
	 * 获取模块列表.
	 * 
	 * @return 模块列表
	 */

	public List<Module> findAllModules();

	/**
	 * 获取微服务模块列表.
	 * 
	 * @return 模块列表
	 */
	public List<Module> findAllMsModules(String proto);

	void publishMenu(View listView, Long parentMenuId, String menuName);

	Module importXml(String xml, Boolean uploadWorkFlow, boolean filter);
	

	public String reallyDeleteModule(Module module);

	/**
	 * 助记码同步
	 * @param moduleCode
	 */
	void dealMneCodeByModule(String moduleCode);
	
	/**
	 * 保存模块之间关联
	 * @param relation
	 */
	public void saveRelation(ModuleRelation relation);
	
	/**
	 * 获取依赖的模块
	 * @param module
	 * @return
	 */
	public List<ModuleRelation> getRelations(Module module);
	
	/**
	 * 获取模块之间的依赖关系
	 * @param module
	 * @param target
	 * @return
	 */
	public ModuleRelation getRelation(Module module, Module target);
	
	/**
	 * 获取模块之间的依赖关系
	 * @param module
	 * @param target
	 * @return
	 */
	public ModuleRelation getRelation(Module module, Module target, boolean valid);
	
	public ModuleRelation getRelationWithNoValid(Module module, Module target);
	
	/**
	 * 删除模块之间的依赖关系
	 * @param relation
	 */
	public void deleteRelation(ModuleRelation relation);
	
	/**
	 * 获取所有关联
	 * @param relation
	 */
	public List<ModuleRelation> getAllRelation();
	
	/**
	 * 获取被那些模块关联
	 * @param needValid TODO
	 * @return
	 */
	public List<ModuleRelation> getBeAssociated(Module target, boolean needValid);
	
	/**
	 * 获取被直接或间接关联的模块
	 * @param target集合
	 * @param needValid
	 * @return
	 */
	public Set<Module> getAllAssociated(Set<Module> target, boolean needValid);
	/**
	 * 获取关联了那些模块
	 * @param module
	 * @return
	 */
	public List<ModuleRelation> getAssociated(Module module, boolean needValid);
	
	public void deleteRelationPhysical(ModuleRelation relation);

	/**
	 * 将参照视图权限操作发布到实体主列表视图菜单下
	 * @param mainListView
	 * @param editViews
	 */
	void publishRefMenu(View mainListView, List<View> refViews);
	/**
	 * 将参照视图权限操作发布到实体主列表视图菜单下
	 * @param mainListView
	 * @param editViews
	 */
	void publishRefMenu(View refView, MenuInfo menuInfo);
    /**
     * 将列表pt视图权限发布到实体主列表视图菜单下
     * @param dataGrid
     * @param menuInfo
     */
    void publishListPtMenu(DataGrid dataGrid, MenuInfo menuInfo);

	/**
	 * 实体复制临时文件解析
	 * @param file
	 * @param code 
	 * @return
	 * @throws Exception
	 */
	public String saveFile(File file, String code) throws Exception;
		
	/**
	 * 实体复制上载实体列表
	 * @param module
	 * @param filePath
	 * @return
	 * @throws Exception
	 */
	public List<Entity> listEntity(Module module, String filePath) throws Exception;

	List<Module> getModuleByCatetorys(List<String> values);
	List<Module> getModuleByCatetory(String catetory,List<Module> modules);
	List<Module> getModuleByCatetorys(List<String> values,List<Module> modules);

	List<Module> getMsModuleByCatetory(String catetory);
	List<Module> getMsModuleByCatetorys(List<String> values);
	List<Module> getMsModuleByCatetory(String catetory,List<Module> modules);
	List<Module> getMsModuleByCatetorys(List<String> values,List<Module> modules);
	/**
	 * 根据模块code查询模块
	 * @param codes
	 * @return
	 */
	List<Module> findModule(List<String> codes);
	
	/**
	 * 根据传入的两个上载list进行排序合并
	 * @param module
	 * @return
	 */
	List<UploadInfo> sortUploads(List<UploadInfo> firstimportUploadInfos,
                                 List<UploadInfo> uploadInfos);

	List<DeployInfo> getDeployListBytaskId(String taskId);

	String executeUploadBatch(UploadInfo up) throws IOException, InterruptedException, XMLStreamException;

	String executeUploadBatchExist(UploadInfo up) throws IOException , InterruptedException,
			XMLStreamException, EcException;

	List<Module> getModuleByArtifact(String artifact);

    String checkModifyModulesState(String moduleCodes);

    void saveModuleCompanyRef(Module module);

    void saveModuleCompanyRefAllCompany(Module module);

    boolean existCompanyRef(Module module);

    List<Long> findCompaniesByModuleCode(String moduleCode);
    
    List<Long> getModuleCompanyRef(String moduleId);

	List<String> findModuleRelationAndReferenceCode(String moduleCode);
}
