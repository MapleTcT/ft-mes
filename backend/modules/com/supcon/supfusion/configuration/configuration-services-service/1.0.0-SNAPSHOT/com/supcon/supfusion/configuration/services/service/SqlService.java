package com.supcon.supfusion.configuration.services.service;

import com.supcon.supfusion.configuration.services.entity.Sql;
import org.hibernate.criterion.Criterion;

import java.util.List;

public interface SqlService {
	void save(Sql sql);
	Sql get(String code);
	Sql getSql(String code);
	Sql getSql(String viewCode, int type);
	List<Sql> getSqls(String viewCode);
	void deleteSql(Sql sql);
	List<Sql> getSqls(Criterion... criterions);
	Sql getSql(String viewCode, String datagridCode, int type);
}
