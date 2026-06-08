package com.supcon.supfusion.configuration.services.dao;

import com.supcon.supfusion.configuration.services.entity.DataGrid;
import com.supcon.supfusion.framework.scaffold.hibernate.dao.impl.ExtGenDaoImpl;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class DataGridDaoImpl extends ExtGenDaoImpl<DataGrid, String>{

	public long countByModel(String viewCode, String tagmodelCode) {
		String hql = "select count(code) from DataGrid dg where dg.valid=true and dg.view.code = ? and dg.targetModel.code=?";
		return ((Long)findEntityByHql(hql, viewCode, tagmodelCode)).longValue();
	}

	public List<DataGrid> getDataGridByViewCode(String viewCode, boolean noValid) {
		String hql = noValid ? "from DataGrid dg where dg.view.code = ?0" : "from DataGrid dg where dg.valid=true and dg.view.code = ?0 order by dg.createTime";
		return findByHql(hql, viewCode);
	}

	public DataGrid getDataGridByModel(String viewCode, String tagmodelCode) {
		String hql = "from DataGrid dg where dg.valid=true and dg.view.code = ? and dg.targetModel.code=?";
		return findEntityByHql(hql, viewCode, tagmodelCode);
	}
}