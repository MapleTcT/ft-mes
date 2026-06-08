package com.supcon.supfusion.configuration.services.service.impl;

import com.supcon.supfusion.configuration.services.dao.SpecialPermissionForUShowDaoImpl;
import com.supcon.supfusion.configuration.services.entity.SpecialPermission;
import com.supcon.supfusion.configuration.services.entity.SpecialPermissionForUShow;
import com.supcon.supfusion.configuration.services.service.SpecialPermissionForUShowService;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import com.supcon.supfusion.framework.scaffold.hibernate.page.Page;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.SQLQuery;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.transform.Transformers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Description: TODO
 * @Version V1.0
 * @Auther: huning
 * @Date: 2020/7/22
 */
@Slf4j
@ServiceApiService
@Transactional
public class SpecialPermissionForUShowServiceImpl implements SpecialPermissionForUShowService {

    @Autowired
    private SpecialPermissionForUShowDaoImpl specialPermissionForUShowDao;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public List<SpecialPermissionForUShow> findAllShowInfo(Long userId, Long operateId, String specialPermissionCode) {
        List<Object> list = new ArrayList<Object>();
        String hql="from  SpecialPermissionForUShow show  where show.valid=true  and  show.userId=?  and  show.operateId=?  ";
        list.add(userId);
        list.add(operateId);
        if(specialPermissionCode!=null&&!specialPermissionCode.isEmpty())  {
            hql=hql+"  and show.specialPermission.code=? ";
            list.add(specialPermissionCode);
        }
        List<SpecialPermissionForUShow>  result=this.specialPermissionForUShowDao.findByHql(hql, list.toArray(new Object[list.size()]));
        return result;
    }

    @Override
    public Page<Map<String, Object>> findRecordPage(Page<Map<String, Object>> page, String queryResultSQL, String queryPageSQL, Object... objects) throws SQLException {
        SQLQuery query = specialPermissionForUShowDao.createNativeQuery(queryResultSQL, objects);
        List<Map<String, Object>> tlist = getResult(page, query, Transformers.ALIAS_TO_ENTITY_MAP);
        page.setResult(tlist);
        if (queryResultSQL != null && !queryResultSQL.isEmpty()) {
            page.setTotalCount(jdbcTemplate.queryForObject(queryResultSQL, objects, Long.class));
        }
        return page;
    }
    protected List getResult(Page page, SQLQuery query, ResultTransformer transformer) {
        if (page.isExportFlag()) {
            query.setResultTransformer(transformer);
            if (page.isAll()) {
                return query.setFirstResult(0).setMaxResults(Integer.MAX_VALUE).list();
            } else {
                List retList = new ArrayList();
                //Map<Integer, Integer> pageNos = page.getPageNos();
                Map<Integer, Integer> pageNos = new HashMap<Integer, Integer>();
                pageNos.put(page.getPageNo(), 1);
                if (pageNos != null) {
                    for (Map.Entry<Integer, Integer> entry : pageNos.entrySet()) {
                        page.setPageNo(entry.getKey());
                        if (entry.getValue() < 0) {
                            retList.addAll(query.setFirstResult(page.getFirst() - 1).setMaxResults(Integer.MAX_VALUE).list());
                        } else {
                            retList.addAll(query.setFirstResult(page.getFirst() - 1).setMaxResults(page.getPageSize() * entry.getValue())
                                    .list());
                        }
                    }
                }
                return retList;
            }
        } else {
            return page.isPaging() ? query.setResultTransformer(transformer).setFirstResult(page.getFirst() - 1)
                    .setMaxResults(page.getPageSize()).list() : query.setResultTransformer(transformer).list();
        }
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public void deleteUserShowHistoryData(Long userId, Long operateId, String specialPermissionCode) {
        List<Object> list = new ArrayList<Object>();

        StringBuilder sb=new StringBuilder();
        sb.append("delete  from SpecialPermissionForUShow  s  where 1=1 ");
        if(userId!=null) {
            list.add(userId);
            sb.append(" and  s.userId=? ");
        }
        if(operateId!=null)  {
            list.add(operateId);
            sb.append(" and  s.operateId=?  ");
        }
        if(specialPermissionCode!=null&&!specialPermissionCode.isEmpty()) {
            list.add(specialPermissionCode);
            sb.append(" and s.specialPermission.code=?");
        }
        sb.append(" and s.valid=true");
        specialPermissionForUShowDao.bulkExecute(sb.toString(), list.toArray(new Object[list.size()]));
    }

    @Override
    public List<String> getConfigSpecialPermissonCode(Long userId, Long operateId, String configedCodes) {
        String hql = "SELECT distinct s.specialPermission.code FROM SpecialPermissionForUShow s where s.userId=?  and  s.operateId=?  ";
        List<String>  result=specialPermissionForUShowDao.findByHql(hql, new Object[]{userId,operateId});
        return result;
    }

    @Override
    public SpecialPermission loadSpecialPermission(String code) {
        String hql="from  SpecialPermission  p  where p.valid=true  and  p.code=?";
        List<SpecialPermission>  sps=specialPermissionForUShowDao.findByHql(hql, new Object[]{code});
        if(sps.size()>0)  {
            return sps.get(0);
        }
        return null;
    }

    @Override
    public void save(SpecialPermissionForUShow specialPermissionForUShow) {
        specialPermissionForUShowDao.save(specialPermissionForUShow);
    }
}
