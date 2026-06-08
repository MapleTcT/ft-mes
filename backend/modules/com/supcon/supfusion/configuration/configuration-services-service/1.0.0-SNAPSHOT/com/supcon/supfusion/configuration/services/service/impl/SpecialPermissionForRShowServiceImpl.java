package com.supcon.supfusion.configuration.services.service.impl;

import com.supcon.supfusion.configuration.services.dao.SpecialPermissionForRShowDaoImpl;
import com.supcon.supfusion.configuration.services.entity.SpecialPermissionForRShow;
import com.supcon.supfusion.configuration.services.service.SpecialPermissionForRShowService;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * @Description: TODO
 * @Version V1.0
 * @Auther: huning
 * @Date: 2020/7/22
 */
@Slf4j
@ServiceApiService
@Transactional
public class SpecialPermissionForRShowServiceImpl implements SpecialPermissionForRShowService {

    @Autowired
    private SpecialPermissionForRShowDaoImpl specialPermissionForRShowDao;

    @Override
    public List<SpecialPermissionForRShow> findAllShowInfo(Long roleId, Long operateId, String specialPermissionCode) {
        List<Object> list = new ArrayList<Object>();
        list.add(roleId);
        list.add(operateId);
        String hql="from  SpecialPermissionForRShow show  where show.valid=true  and  show.roleId=?  and  show.operateId=?  ";
        if(specialPermissionCode!=null&&!specialPermissionCode.isEmpty())  {
            hql=hql+" and show.specialPermission.code=? ";
            list.add(specialPermissionCode);
        }
        List<SpecialPermissionForRShow> result=this.specialPermissionForRShowDao.findByHql(hql, list.toArray(new Object[list.size()]));
        return result;
    }

    @Override
    public void deleteRoleShowHistoryData(Long roleId, Long operateId, String specialPermissionCode) {
        String hql="delete  from SpecialPermissionForRShow  s  where s.roleId=?  and s.operateId=?  and s.valid=true ";
        List<Object> list = new ArrayList<Object>();
        list.add(roleId);
        list.add(operateId);
        if(specialPermissionCode!=null&&!specialPermissionCode.isEmpty())  {
            hql=hql+" and s.specialPermission.code=? ";
            list.add(specialPermissionCode);
        }
        specialPermissionForRShowDao.bulkExecute(hql, list.toArray(new Object[list.size()]));
    }

    @Override
    public List<String> getConfigSpecialPermissonCode(Long roleId, Long operateId) {
        String hql = "SELECT distinct s.specialPermission.code FROM SpecialPermissionForRShow s where s.roleId=?  and  s.operateId=?  ";
        List<String>  result=specialPermissionForRShowDao.findByHql(hql, new Object[]{roleId,operateId});
        return result;
    }

    @Override
    public void save(SpecialPermissionForRShow specialPermissionForRShow) {
        specialPermissionForRShowDao.save(specialPermissionForRShow);
    }
}
