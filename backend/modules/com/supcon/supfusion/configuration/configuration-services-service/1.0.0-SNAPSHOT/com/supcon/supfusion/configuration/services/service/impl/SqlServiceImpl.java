package com.supcon.supfusion.configuration.services.service.impl;

import com.supcon.supfusion.base.utils.ProjectFlagHolder;
import com.supcon.supfusion.configuration.services.dao.SqlDaoImpl;
import com.supcon.supfusion.configuration.services.entity.Sql;
import com.supcon.supfusion.configuration.services.service.SqlService;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class SqlServiceImpl implements SqlService {
	@Autowired
	private SqlDaoImpl sqlDao;
	
	@Override
	public void save(Sql sql) {
		if(Boolean.TRUE.equals(ProjectFlagHolder.getInstance().getProjFlag().get())){
			sql.setProjFlag(true);
		}
		sqlDao.merge(sql);
		sqlDao.flush();
	}
	
	@Override
	public void deleteSql(Sql sql) {
		sqlDao.deletePhysical(sql);
	}

	@Override
	public Sql get(String code) {
		return sqlDao.load(code);
	}
	
	@Override
	public Sql getSql(String code) {
		return sqlDao.load(code);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public Sql getSql(String viewCode, int type) {
		List<Sql> sqls = sqlDao.findByCriteria(Restrictions.eq("viewCode", viewCode), Restrictions.eq("type", type));
		if(!sqls.isEmpty()) {
			return sqls.get(0);
		}
		return null;
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public Sql getSql(String viewCode, String datagridCode, int type){
		List<Sql> sqls = sqlDao.findByCriteria(Restrictions.eq("viewCode", viewCode), Restrictions.eq("type", type), Restrictions.eq("dataGridCode", datagridCode));
		if(!sqls.isEmpty()) {
			return sqls.get(0);
		}
		return null;
	}
	
	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<Sql> getSqls(String viewCode) {
		return sqlDao.findByCriteria(Restrictions.eq("viewCode", viewCode));
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<Sql> getSqls(Criterion... criterions) {
		return sqlDao.findByCriteria(criterions);
	}


}
