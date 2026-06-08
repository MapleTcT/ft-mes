package com.supcon.supfusion.configuration.services.service;

import com.supcon.supfusion.configuration.services.entity.*;
import org.hibernate.criterion.Criterion;
import com.supcon.supfusion.framework.scaffold.hibernate.page.Page;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface DataGridService {

	void deleteDataGrid(String code);

	DataGrid getDataGrid(String code);
	void save(DataGrid dg);
	void save(DataGrid datagrid, View view, String tagmodelCode);

	void publish(DataGrid datagrid, Map<String, Object> argsMap) throws IOException;

	DataGrid getFullDataGrid(String code);

	Map<String, String> getAsskeyByTagmodelCode(Model model);

	List<DataGrid> getDataGridByView(View view, boolean noValid);

	Map<String, DataGrid> getDataGridMapByView(View view);

	List<DataGrid> getDataGridByViewCode(String viewCode);

	List<DataGrid> getDataGridByViews(List<View> views);
	
	/**
	 * 分页获取datagrid列表.
	 * 
	 * @param page
	 *            分页包裹对象
	 * @param Criterion
	 *            ...criterions 0参数或多个参数 实体类别
	 * @return 带实体列表数据的分页包裹对象.
	 */
	Page<DataGrid> findDataGrids(Page<DataGrid> page, Criterion... criterions);

	/**
	 * 根据Property查找关联的DataGrid
	 * @param property
	 */
	List<DataGrid> findDataGridsByProperty(Property property);

	/**
	 * 删除DataGrid，物理删除
	 * @param dataGrid
	 */
	void deleteDataGridPhysical(DataGrid dataGrid);

	String getDataGridFullConfig(DataGrid dataGrid);

	Map getDataGridFullConfigMap(DataGrid dataGrid);

	void deleteDataGridPhysical(String code);

	/**
	 * @param backupDataGrid
	 */
	void saveBackupDataGrid(BackupDataGrid backupDataGrid);

	/**
	 * @param bvCode
	 */
	void deleteBackupDataGridByBackupView(String bvCode);

	/**
	 * @param bvCode
	 * @param dgCode
	 * @return
	 */
	BackupDataGrid getBackupDataGrid(String dgCode, String bvCode);

	List<BackupDataGrid> getBackupDataGridByBackupViewCode(String bvCode);

	void deleteDataGrid(DataGrid dataGrid);

	/**
	 * @param code
	 * @return
	 */
	List<DataGrid> getDataGridByTargetModel(String code);

	/**
	 * 查询缓存中的DataGrid
	 * @param code
	 * @return
	 */
	DataGrid loadDataGrid(String code);
	/**
	 * 根据条件查询DataGrid
	 * @param criterions
	 * @return
	 */
	List<DataGrid> findDataGrids(Criterion... criterions);
}