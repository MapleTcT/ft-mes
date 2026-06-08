package com.supcon.supfusion.configuration.services.dao;

import com.supcon.supfusion.configuration.services.entity.EchartsModel;
import com.supcon.supfusion.framework.scaffold.hibernate.dao.impl.ExtGenDaoImpl;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class EchartsModelDaoImpl extends ExtGenDaoImpl<EchartsModel, String> {

	public List<EchartsModel> findEchartsModels(String echartsCode) {
		return findByHql("from EchartsModel where echartsCode=?0 and valid=true", echartsCode);
	}

	public void delEchartsModelsByEcode(String echartsCode) {
		String hql = "delete from EchartsModel em where em.echartsCode = ?0";
		bulkExecute(hql, echartsCode);
	}
}
