package com.supcon.supfusion.configuration.services.dao;

import com.supcon.supfusion.configuration.services.entity.Echarts;
import com.supcon.supfusion.framework.scaffold.hibernate.dao.impl.ExtGenDaoImpl;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class EchartsDaoImpl extends ExtGenDaoImpl<Echarts, String> {

	public List<Echarts> getListByViewCode(String viewCode) {
		String sql = "from Echarts where code like ?0 and valid=true";
		return findByHql(sql, viewCode + "%");
	}
}
