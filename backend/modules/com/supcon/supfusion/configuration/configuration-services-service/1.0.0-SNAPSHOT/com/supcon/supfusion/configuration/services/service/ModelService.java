package com.supcon.supfusion.configuration.services.service;

import com.supcon.supfusion.configuration.services.entity.*;
import com.supcon.supfusion.configuration.services.entity.AssociatedInfo;
import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueCodeEntity;
import org.hibernate.criterion.Criterion;
import com.supcon.supfusion.framework.scaffold.hibernate.page.Page;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ModelService {

	/**
	 * 获取模型对象.
	 * 
	 * @param code
	 *            模型Code
	 * @return 模型实例
	 */
	Model getModel(String code);

	/**
	 * 获取模型对象及对象下面的属性
	 * 
	 * @param code
	 *            模型CODE
	 * @return 模型实例
	 */
	Model getModelWithProperties(String code);

	/**
	 * 新增/修改模型.
	 * 
	 * @param model
	 *            模型实例
	 */
	void saveModel(Model model);

	String deleteModel(Model model);

	/**
	 * 获取实体下模型列表.
	 * 
	 * @param entity
	 *            实体
	 */
	List<Model> findModels(Entity entity);
	List<Model> findModels(Module module);

	/**
	 * 获取实体下模型分页列表
	 * 
	 * @param page
	 *            分页包裹对象
	 * @param entity
	 *            实体
	 * @return 模型列表分页包裹对象
	 */
	Page<Model> findModels(Page<Model> page, Entity entity);

	// ==============================================================

	/**
	 * 获取模型内属性对象.
	 * 
	 * @param code
	 *            属性CODE
	 * @return 属性实例
	 */
	Property getProperty(String code);

	/**
	 * 新增/修改模型属性.
	 * 
	 * @param property
	 *            模型属性实例
	 */

	void saveProperty(Property property);

	String deleteProperty(Property property);
	/**
	 * 获取模型下模型属性列表.
	 * 
	 * @param model
	 *            模型实体
	 * @return 模型属性列表
	 */
	List<Property> findProperties(Model model);
	Property findMainDisplayProperty(Model model);

	/**
	 *  查找本模型的关联属性以及关联本模型的关联属性
	 * 
	 * @param model
	 * @return 
	 */
	List<AssociatedInfo> findAssociatedInfos(Model model, int... associatedTypes);
	List<AssociatedInfo> findAssociatedInfoNotIncludeBackAsso(Model model, int... associatedTypes);
	List<AssociatedInfo> findAssociatedInfos(List<Model> models, int... associatedTypes);
	List<AssociatedInfo> findAssociatedInfos(Property property);
	List<AssociatedInfo> findInherentAssociatedInfos(int associatedType);
	/**
	 * 查找本模型的关联属性以及关联本模型的关联属性（不包含自定义字段）
	 * @param model
	 * @param associatedTypes
	 * @return
	 */
	List<AssociatedInfo> findAssociatedInfosForTemplate(Model model, Boolean showCustom, int... associatedTypes);

	Page<Property> findProperties(Page<Property> page, Model mode, Boolean showInherentl, Boolean showCustom);
	/**
	 * 生成固有字段
	 * 
	 * @param page
	 *            
	 * @param property
	 *            
	 * @return 
	 */
	void createInherentProperties(Model model);

	List<Property> findByUpdateProperties();

	String deleteModelPhysical(String modelCode, Boolean deleteType);

	String deletePropertyPhysical(String propertyCode, Boolean deleteType);
	/**
	 * get property has bussinessKey by model
	 * @see Property
	 * @param modelCode
	 * @return
	 */
	Property getBussinessProperty(Model model);
	/**
	 * get property has bussinessKey by modelCode
	 * @see Property
	 * @param modelCode
	 * @return
	 */
	Property getBussinessProperty(String modelCode);

	/**
	 * 处理EC_PROPERTY 添加显示格式与显示类型
	 * 处理后显示类型与显示格式都恢复为默认
	 * @throws Exception
	 */
	void modifyPropertyFieldType(String moduleCode) throws Exception;
	/**
	 * 处理业务主键 如模型中没有业务主键则设主键为业务主键
	 * @throws Exception
	 */
	void modifyBusinessKey() throws Exception;

	/**
	 * 处理业务主键 如模型中没有业务主键则设主键为业务主键
	 * @throws Exception
	 */
	void modifyMainAsso() throws Exception;

	/**
	 * @return
	 */
	boolean firstIsMain(Entity entity);
	
	/**
	 * 根据property获取property和model
	 * @param code
	 * @return
	 */
	Property getPropertyWithModel(String code);

	/**
	 * 根据条件查询Model
	 * @param criterions
	 * @return
	 */
	List<Model> findModels(Criterion... criterions);
	/**
	 * 根据条件查询Property
	 * @param criterions
	 * @return
	 */
	List<Property> findProperties(Criterion... criterions);
	/**
	 * 根据条件查询Property
	 * @return
	 */
	List<Property> findProperties(String propertiesNames, String modelCode);
	/**
	 * 查询模型主键
	 * @param modelCode
	 * @return
	 */
	Property findPKProperty(String modelCode);

	/**
	 * 查找关联模型
	 * @param modelCode
	 * @param associatedTypes
	 * @return
	 */
	Set<Model> findRelationModels(String modelCode, int... associatedTypes);
	
	
	/**
	 * 得到主显示字段
	 * @param modelCode
	 * @return
	 */
	Property getMainDisplayProperty(String modelCode) ;
	/**
	 * 得到业务主键字段
	 * @param modelCode
	 * @return
	 */
	Property getBussinessKeyProperty(String modelCode);
	/**
	 * 得到主键字段
	 * @param modelCode
	 * @return
	 */
	Property getPKProperty(String modelCode);
	/**
	 * 检查是否是树形系统编码
	 * @param p
	 * @return
	 */
	Boolean checkWhetherIsTreeSystemCode(Property p);
    
	/**
	 * @author mkp
	 * 保存自定义字段顺序
	 * @param orderModeltCol
	 * 
	 */
	public void saveOrderModeltCol(String orderModeltCol);
	/**
	 * 	生成自定义字段
	 * @param modelCode 模型code
	 * @param charParamAmount 字符串
	 * @param intParamAmount 整数
	 * @param floatParamAmount 浮点数
	 * @param dateParamAmount 日期
	 * @param codeParamAmount 系统编码
	 * @param objParamAmount 对象
	 * @param colPrefix 列名前缀 
	 * @param staffId 
	 */
	void createCustomProps(String modelCode, Integer charParamAmount, Integer intParamAmount, Integer floatParamAmount, Integer dateParamAmount,
                           Integer codeParamAmount, Integer objParamAmount, String colPrefix, Long staffId);
	/**
	 * 获取模型下模型属性列表.
	 * 
	 * @param model
	 *            模型实体
	 * @return 模型属性列表
	 */
	List<Property> findPropertiesByModel(Model model);

	/**
	 * 检查字段是否可以删除 </br>
	 * 检查范围：视图、其它字段
	 * @param property 字段
	 * @param includeModule  是否包含字段所在模块
	 * @return 提示信息
	 */
	Set<String> checkDeleteProperty(Property property);

	/**
	 * 检查模型是否可被删除
	 * @param model
	 * @return
	 */
	Set<String> checkDeleteModel(Model model);

	String deletePropertyPhysical(String propertyCode, Boolean deleteType, Boolean ignoreCheck);

	/**
	 * 删除相应文件
	 * @param ecEntity
	 */
	void deleteModuleFile(AbstractAuditUniqueCodeEntity ecEntity);

	/**
	 * 根据模型名称获取id字段
	 * @param modelCode
	 * @return property || null
	 */
	Property getIdProperty(String modelCode);

	Boolean getCustomProptyNullAble(String propertyCode);

	Property findPropertyByCode(String propertyCode);
	/**
	 * @param entityCode 实体code
	 * @param tableName 表名
	 * @param propertyName 字段名称
	 * @return
	 */
	String getPropertyColumnNameByTableName(String entityCode, String tableName, String propertyName, Boolean isObjectType);

	List<AssociatedInfo> findAssociatedInfos2(Model model, int... associatedTypes);

	List<String> getRunningCustomProperties(String entityCode);

    String getMainDisplayValue(String code, Object obj);

	void checkModelInfo(Model model);
	List<Entity> getEntities(String moduleCode);
	/**
	 * 获取所有模块信息.
	 *
	 * @return 所有模块信息
	 */
	List<Module> getAllModule(String... env);

	/**
	 * 根据实体code获取实体
	 */
	Entity getEntity(String entityCode);
	Module getModule(String moduleCode);
	void deleteCustomPropertyModelMappingsForImport(String moduleCode);

	void saveCustomPropertyModelMapping(CustomPropertyModelMapping item);

	List<CustomPropertyModelMapping> findCustomPropertyModelMappingsForExport(String code);
	
	/**
	 * 查询已关联的对象类型自定义字段
	 */
	CustomPropertyModelMapping getAssociatedCustomPropertyModelMapping(String propertyCode);


}
