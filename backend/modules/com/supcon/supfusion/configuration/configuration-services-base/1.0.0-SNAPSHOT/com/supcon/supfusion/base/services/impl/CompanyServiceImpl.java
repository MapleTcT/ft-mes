package com.supcon.supfusion.base.services.impl;

import com.supcon.supfusion.base.dao.CompanyDaoImpl;
import com.supcon.supfusion.base.entities.Company;
import com.supcon.supfusion.base.enums.CompanyType;
import com.supcon.supfusion.base.services.CompanyService;
import com.supcon.supfusion.framework.scaffold.hibernate.page.Page;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
public class CompanyServiceImpl implements CompanyService {

    @Autowired
    private CompanyDaoImpl companyDao;

    @Override
    public Company getCompanyByCode(String code) {
        if (StringUtils.isEmpty(code)) {
            return null;
        }
        List<Company> list = companyDao.findByHql("From Company where code = ? and valid = true", code);
        if(list!=null && list.size()>0){
            return list.get(0);
        }else{
            return null;
        }
    }

    @Override
    public Company get(Long id) {
        return companyDao.get(id);
    }

    @Override
    public List<Company> getAllCompanies() {
        List<Company> groupCompanies = companyDao.createCriteria(
                Restrictions.eq("valid", true)).addOrder(Order.asc("id")).list();
        return groupCompanies;
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Page<Company> getByPage(Page<Company> page, DetachedCriteria detachedCriteria) {
        return companyDao.findByPage(page,detachedCriteria);
    }
}
