package com.supcon.supfusion.configuration.services.service;

import com.supcon.supfusion.base.entities.MenuOperate;
import com.supcon.supfusion.configuration.services.entity.*;
import com.supcon.supfusion.configuration.services.enums.ViewType;
import com.supcon.supfusion.framework.scaffold.hibernate.page.Page;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <pre>
 * config ==> CUSTOME:{action:"",html:""}
 * </pre>
 * 
 * @author jiawei
 * 
 */
public interface ViewService {

	View getView(String viewCode, boolean full);

	/**
	 * 获取一个视图.
	 * 
	 * @param code
	 *            视图code
	 * @return 视图,不存在时返回null.
	 */
	View getView(String code);
	
	/**
	 * 获取一个视图
	 * @param id
	 * @return
	 */
	View load(String id);

	/**
	 * 保存视图的布局json
	 * @param view
	 */
	void saveViewJson(View view);

	/**
	 * 删除一个视图.<br />
	 * 并不是物理删除,而是置valid为false
	 * 
	 * @param View
	 *            对象 要删除的实体对象.
	 */

	String deleteView(View view);

	/**
	 * 保存一个视图
	 * 
	 * @param view
	 */
	void saveView(View view);
	
	/**
	 * 保存视图
	 * 
	 * @param view
	 */
	void mergeView(View view);

	/**
	 * 分页获取视图列表.
	 * 
	 * @param page
	 *            分页包裹对象
	 * @param Criterion
	 *            ...criterions 0参数或多个参数 实体类别
	 * @return 带实体列表数据的分页包裹对象.
	 */
	Page<View> findViews(Page<View> page, Criterion... criterions);
	
	/**
	 * 获取视图列表
	 * @param entity
	 * @return
	 */
	List<View> findViewList(Entity entity);


	/**
	 * 获取编辑视图列表.
	 * 
	 * @param Entity
	 *            实体
	 * @param viewTypes
	 *            视图类型
	 * @return 编辑视图列表
	 */
	List<View> findViews(Entity entity, ViewType... viewTypes);
	
	/**
	 * 
	 * 查询工程期视图的url
	 * @param entity
	 * @param viewTypes
	 * @return
	 */
	List<String> findEngineViewUrl(Entity entity, ViewType... viewTypes);

	List<View> findViews(Module module);

	List<View> findViewsByAssModelCode(String modelCode, ViewType... viewTypes);
	List<View> findProjViewsByAssModelCode(String modelCode, ViewType... viewTypes);
	
	List<View> findMobileViewsByAssModelCode(String modelCode, ViewType... viewTypes);
	
	List<View> findViewsByAssModelCode(String modelCode, int editViewType, ViewType... viewTypes);
	List<View> findProjViewsByAssModelCode(String modelCode, int editViewType, ViewType... viewTypes);
	
	void checkDataGridFildNull(List<DataGrid> dgList);

	void saveExtraView(ExtraView ev, Map argsMap);

	ExtraView getExtraView(View view);

	/**
	 * 提供默认的配置
	 * 
	 * @param view
	 * @return
	 */
	ExtraView defaultExtraView(View view);

	void publish(ExtraView ev);

	void saveButtonOperate(View view, Button button);

	void saveSql(Sql sql);

	void saveSqls(Sql... sqls);
	
	void saveFastQueryJson(FastQueryJson fastQueryJson);
	
	void saveAdvQueryJson(AdvQueryJson advQueryJson);

	BackupView getBackupView(String code);

	void restoreView(String code);

	Page<BackupView> getBackupViewList(Page<BackupView> page, String viewCode);

	void saveDataGroup(DataGroup dataGroup);

	void saveDataClassific(DataClassific dataClassific);

	List<DataGroup> findDataGroups(View view);
	
	List<DataGroup> findDataGroups(View view, String layoutName, String targetModelCode);

	Page<DataClassific> findDataClassifics(Page<DataClassific> page, DataGroup dataGroup);

	DataGroup getDataGroup(String code);

	DataClassific getDataClassific(String code);
	
	List<DataClassific> getDataClassificInView(DataClassific dataClassific, View view);

	void deleteDataGroup(DataGroup DataGroup);

	void deleteDataClassific(DataClassific DataClassific);

	List<DataClassific> findDataClassifics(DataGroup dataGroup);

	Sql getSql(String viewCode, int type);

	Sql getSql(String viewCode, String datagridCode, int type);
	
	/**
	 * 删除备份视图
	 * 
	 * @param bvCode
	 */
	void deleteBackupView(String bvCode);

	/**
	 * 历史数据处理
	 * 在{@link <code>ExtraView</code>}中的config中添加propertyCode属性，并在LABEL节点中添加key
	 * 
	 * @throws Exception
	 */
	void modifyExtraViewPropertyCode(Page<View> page, String modelCode) throws Exception;


	/**
	 * 根据config的propertyCode获取propertyMap
	 * 
	 * @param ev
	 * @return
	 */
	Map<String, Property> getPropertyMap(String config);

	String deleteViewPhysical(String viewCode, Boolean deleteType);
	/**
	 * 对EC_EXTRA_VIEW 与EC_DATA_GRID进行数据处理 添加字段显示类型和显示格式等
	 * @param modelCode 处理单独模块时可传入modelCode 模块code
	 * @throws TransformerException
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws Exception
	 */
	void modifyExtraViewField(String modelCode) throws ParserConfigurationException, SAXException, IOException, TransformerException, Exception;

	String getExtraViewFullConfig(View view);

	/**
	 * @param backupView
	 */
	void saveBackupView(BackupView backupView);
	/**
	 * 保存backupView
	 * @param view
	 */
	void saveBackupView(View view);

	List<View> findViewsByModuleCode(String moduleCode);

	List<View> findAllViews(Criterion... criterions);

	/**
	 * 根据提供的条件，查询视图
	 * @param entity
	 * @param valid 0只返回作废视图，1只返回有效视图，3全部
	 * @param showType 显示类型，0片断，1布局，2独立的页面,3非片断，4全部
	 * @param mobile 0返回非移动配置视图，1返回移动配置视图，3全部
	 * @param viewTypes 视图类型
	 * @return
	 */
	List<View> findViews(Entity entity, int valid, int showType, int mobile, ViewType... viewTypes);
	/**
	 * 备份EC_EXTRA_VIEW与EC_DATA_GRID
	 */
	void backupViewConfig();
	/**
	 * 备份EC_FIELD
	 */
	void backupField();
	
	void transformCustomerCondition(String moduleCode);

	/**
	 * 视图拷贝
	 * @param srcView
	 * @param view
	 */
	void copyView(View srcView, View view, boolean needCopyExtraView);


	/**
	 * 获取主列表视图
	 * @param entity
	 * @return
	 */
	View getMainListView(Entity entity);

	/**
	 * 根据操作code与CID查询操作
	 * @param operateCode
	 * @param cid
	 * @return
	 */
	MenuOperate getMenuOperateByCode(String operateCode, Long cid);
	/**
	 * 处理布局视图对应片段的按钮操作  将操作前加上布局code
	 */
	void dealLayoutMenuOperate();

	
   /**
    * 通过viewCode和layoutName查询FQJ
    * @param view
    * @param layoutName
    * @return
    */
   FastQueryJson getFastQueryJsonByViewCodeAndLayoutName(View view, String layoutName);

   /**
    * 通过viewCode和layoutName查询FQJ
    * @param view
    * @param layoutName
    * @return
    */
   AdvQueryJson getAdvQueryJsonByViewCodeAndLayoutName(View view, String layoutName);

   /**
    * 通过view 查询所有的 FastQueryJson
    * @param view
    * @return
    */
   Map<String, FastQueryJson> getFastQueryJsonFromView(View view);

	/**
	 * 修改影子视图的hasCustomSection属性
	 * @param viewCode 父视图code
	 * @param hasCustomSection
	 */
	void modifyShadowViewCustomSection(String viewCode, Boolean hasCustomSection);

	/**
	 * 视图迁移（不同实体间的视图复制）
	 * @param srcView
	 * @param view
	 * @param needCopyExtraView
	 * @param viewCodeReplaceMap TODO
	 * @param dgCodeReplaceMap TODO
	 */
	void migrateView(View srcView, View view, Map<Property,Property> hashProperty, boolean needCopyExtraView, Map<String, String> viewCodeReplaceMap, Map<String, String> dgCodeReplaceMap);
	
	/**
	 * 列表视图发布时重建输出字段
	 * @param view
	 */
	void rebuildQueryResult(View view, List<Object> newActions);
	
	void rebuildTreeDataResult(View view, List<Object> newActions);

	/**
    * 保存增强型视图快速查询条件
    * @param fqj
    * @param full
    */
   void saveExtraFastQueryJson(FastQueryJson fqj, boolean full);

   /**
    * 保存增强型视图高级查询条件
    * @param aqj
    * @param full
    */
   void saveExtraAdvQueryJson(AdvQueryJson aqj, boolean full);

   void changeViewProjFlag(View view, Boolean projFlag);

   void updatePublishTime(View view);
   
   void updatePublishTimeToNull(View view);
   void updatePublishTimeEnableFalse(View view);

   void viewPublish(View view, ExtraView ev) throws Exception;

   /**
    * 通过viewCode批量调整关联快速查询、高级查询等对象的layoutName
    * @param viewCode
    * @param layoutName
    */
   void changeLayoutName(String viewCode, String oldLayoutName, String newLayoutName);
   

   /**
    * 查询最新的backupView
    * @param view
    * @return
    */
   BackupView getBackupView(View view);

   /**
	 * 检查视图是否可被删除
	 * @param views
	 * @return
	 */
   Set<String> checkDeleteView(View view);

   /**
	 * 处理模块中视图的打印、批量打印按钮
	 * @param moduleCode
	 * @param views
	 */
   void dealPrintButton(String moduleCode, List<View> views);
   
   /**
	 * 处理模块中视图的打印、批量打印按钮
	 * @param view
	 */
   void dealPrintButton(View view);
   
   /**
    * 通过hql查询view
    * @param hql
    * @param param
    * @return
    */
   View getViewByHql(String hql, String param);
   
   List<View> getViewsByHql(View view);
   /**
    * 离线条件查询
    * @param detachedCriteria
    * @return
    */
   public List<View> findByCriteria(DetachedCriteria detachedCriteria);
	/**
	 * ExtraView拷贝
	 * @param srcView
	 * @param targetView
	 */
	void copyExtraView(View srcView, View targetView);
   
	String getListViewCodeByView(String viewCode, Long cid);

	Object getDefaultDataClassific(String viewCode);

	List getDataClassificByViewCode(String viewCode);

	List getDataClassificByViewCode(String viewCode,String layoutName);

	List findShowedCustomProps(String modelCode, String associatedCode, String viewType, String propertyLayRec);

	List<CustomPropertyModelMapping> findCustomPropertyForAsso(String modelCode ,String property);

	List<Property> getEnabledCustomProps(String modelCode);

	String findPropDisplayName(String propLayRec,String modelCode,String... env);

	List<CustomPropertyViewMapping> findCustomPropertyForSecret(String modelCode, String associatedCode, String viewType, String propertyLayRec);

	void saveConfig(View view, ExtraView ev, Map<String, Object> argsMap);

	List<View> findViewByUrl(String url);
	void saveEntity(Entity entity);
	void deleteCustomPropertyViewMappingsForImport(String moduleCode);

	void saveCustomPropertyViewMapping(CustomPropertyViewMapping item);

	List<CustomPropertyViewMapping> findCustomPropertyViewMappingsForExport(String code);

	void saveViewConfig(ExtraView ev, Map map);
}
