package com.supcon.supfusion.configuration.services.dao;

import com.supcon.supfusion.configuration.services.entity.*;
import com.supcon.supfusion.framework.scaffold.hibernate.dao.impl.ExtGenDaoImpl;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ViewDaoImpl extends ExtGenDaoImpl<View, String> {

	public void saveExtraView(ExtraView ev) {
		getSession().saveOrUpdate(ev);
	}

	public void mergeExtraView(ExtraView ev) {
		getSession().merge(ev);
	}

	public ExtraView getExtraView(String code) {
		return getSession().load(ExtraView.class, code);
	}

	public List<View> queryByEntityAndType(String entityCode) {
		return findByHql(" FROM View v WHERE v.entity.code = ? " +
				"AND  v.valid = true ORDER BY v.code DESC", (Object)entityCode);
	}

	public void saveFastQueryJson(FastQueryJson fqj) {
		getSession().merge(fqj);
	}
	/**
	 * 保存高级查询Json
	 * 
	 * @param
	 * @return
	 */
	public void saveAdvQueryJson(AdvQueryJson aqj) {
		getSession().merge(aqj);
	}

	public void saveExtraQueryJson(ExtraQueryJson extraQueryJson) {
		getSession().saveOrUpdate(extraQueryJson);
	}

	public List<FastQueryJson> getFastQueryJsons(String viewCode) {
		return findByHql(" From FastQueryJson fqj where fqj.view.code = ?0 ", viewCode);
	}

	public FastQueryJson getFastQueryJson(String viewCode, String layoutName) {
		return (FastQueryJson) getSession().createQuery("From FastQueryJson fqj where fqj.view.code =:code and fqj.layoutName =:layoutName ").setParameter("code", viewCode).setParameter("layoutName", layoutName).uniqueResult();
	}
	
	public AdvQueryJson getAdvQueryJson(String viewCode, String layoutName) {
		return (AdvQueryJson) getSession().createQuery("From AdvQueryJson adv where adv.view.code =:code and adv.layoutName =:layoutName ").setParameter("code", viewCode).setParameter("layoutName", layoutName).uniqueResult();
	}
}