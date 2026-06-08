package com.supcon.supfusion.base.services.impl;

import com.supcon.supfusion.base.dao.UserPermissionDaoImpl;
import com.supcon.supfusion.base.services.UserFieldPermissionService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * @Description: TODO
 * @Version V1.0
 * @Auther: huning
 * @Date: 2020/7/21
 */
@Slf4j
@Service
@Transactional
public class UserFieldPermissionServiceImpl implements UserFieldPermissionService {


    @Override
    public int findFieldPermission(String modelCode, String propertyKey, String propertyCode) {
        return 3;
    }

    @Override
    public Map<String, Integer> getNoPermissionFieldMap(String modelCode, String keys) {
        return null;
    }

    @Autowired
    private UserPermissionDaoImpl userPermissionDao;

    public boolean enableFieldsPermissionConf(String modelCode) {
        boolean result = false;
        String permssionHql = "select e.code, e.enableFieldsPermissionConf from Model m, Entity e where m.entity.code = e.code and m.code = ? and e.enableFieldsPermissionConf = 1";
        List<Object[]> res = userPermissionDao.findByHql(permssionHql, modelCode);
        if (null == res || res.size() == 0) {
            result = false;
        } else {
            result = true;
        }
        return result;
    }


}
