package com.supcon.supfusion.base.services.impl;

import com.supcon.supfusion.base.dao.DepartmentDaoImpl;
import com.supcon.supfusion.base.entities.Department;
import com.supcon.supfusion.base.services.BaseServiceImpl;
import com.supcon.supfusion.base.services.DepartmentService;
import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.framework.scaffold.hibernate.page.Page;
import com.supcon.supfusion.organization.api.PersonApiService;
import com.supcon.supfusion.organization.api.dto.DepartmentDetailDTO;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.criterion.DetachedCriteria;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @Description: TODO
 * @Version V1.0
 * @Auther: huning
 * @Date: 2020/7/21
 */
@Slf4j
@Service
@Transactional
public class DepartmentServiceImpl extends BaseServiceImpl implements DepartmentService {

    @Autowired
    private PersonApiService personApiService;
    @Autowired
    private DepartmentDaoImpl departmentDao;
    @Override
    public List<String> getDepartmentChildren(String deptName, Long companyId) {

        String hql = "select   d.layRec   from  Department as d where  d.valid=true  and d.name like :name";

        if (companyId != null && !companyId.equals(1L)) {
            hql += "  and d.cid=:cid";
            List<String> listly = departmentDao.createQuery(hql).setString("name", '%' + deptName + '%').setParameter("cid", companyId).list();

            return listly;
        }
        List<String> listly = departmentDao.createQuery(hql).setString("name", '%' + deptName + '%').list();
        return listly;
    }

    @Override
    public Department load(Long id) {
        return departmentDao.get(id);
    }

    @Override
    public List<Department> getTreeChildren(Long id, Long companyId) {
        if (companyId == null) {
            companyId = getCurrentCompanyId();
        }
        ListResult<DepartmentDetailDTO> listResult = personApiService.querySubDepartmentByParentId(id, false, companyId);
        if (listResult.getList() == null || listResult.getList().size() <= 0) {
            return null;
        }
        List<Department> list = new ArrayList<>(listResult.getList().size());
        listResult.getList().forEach(departmentDetailDTO -> {
            Department d = new Department();
            BeanUtils.copyProperties(departmentDetailDTO, d);
            list.add(d);
        });
        return list;
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Page<Department> getByPage(Page<Department> page, DetachedCriteria detachedCriteria) {
        return departmentDao.findByPage(page, detachedCriteria);
    }
}
