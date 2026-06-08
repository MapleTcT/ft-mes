package com.supcon.supfusion.base.services.impl;

import com.supcon.supfusion.base.dao.StaffDaoImpl;
import com.supcon.supfusion.base.entities.Department;
import com.supcon.supfusion.base.entities.DepartmentWork;
import com.supcon.supfusion.base.entities.Position;
import com.supcon.supfusion.base.entities.Staff;
import com.supcon.supfusion.base.services.StaffService;
import com.supcon.supfusion.framework.scaffold.hibernate.page.Page;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.SQLQuery;
import org.hibernate.transform.AliasToEntityMapResultTransformer;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.transform.Transformers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * @Description: TODO
 * @Version V1.0
 * @Auther: huning
 * @Date: 2020/7/21
 */
@Slf4j
@Service
@Transactional
public class StaffServiceImpl implements StaffService {

    @Autowired
    private StaffDaoImpl staffDao;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public Staff get(Long id) {
        return staffDao.get(id);
    }

    @Override
    public Staff load(Long staffId) {
        return staffDao.get(staffId);
    }

    @Override
    public Page<Map<String, Object>> findRecordPage(Page<Map<String, Object>> page, String queryResultSQL,
                                                    Object... objects) throws SQLException {

        SQLQuery query = staffDao.createNativeQuery(queryResultSQL, objects);
        List<Map<String, Object>> tlist = getResult(page, query, Transformers.ALIAS_TO_ENTITY_MAP);
        page.setResult(tlist);
        page.setTotalCount(tlist==null?0:tlist.size());
        return page;
    }

    private List<Map<String, Object>> getResult(Page<Map<String, Object>> page, SQLQuery query, ResultTransformer transformer) {
        return page.isPaging() ? query.setResultTransformer(transformer).setFirstResult(page.getFirst() - 1)
                .setMaxResults(page.getPageSize()).list() : query.setResultTransformer(transformer).list();
    }

    @Override
    public Page<DepartmentWork> deptfindstaffworkInfo(Page<DepartmentWork> departmentWorkPage) {
        List<DepartmentWork> result = departmentWorkPage.getResult();
        String sql;
        for (int i = 0; i < result.size(); i++) {
            // long mainpositionId = -1L;
            long staffId = -1L;
            long departmentId = -1L;
            // if (null != result.get(i).getStaff().getMainPositionId()) {
            // mainpositionId = result.get(i).getStaff().getMainPositionId();
            // }
            if (null != result.get(i).getStaff().getId()) {
                staffId = result.get(i).getStaff().getId();
            }
            if (null != result.get(i).getDepartment().getId()) {
                departmentId = result.get(i).getDepartment().getId();
            }

            // if (mainpositionId > -1l) {

            if (staffId > -1L && departmentId > -1L) {

                sql = "select d.name DEPARTMENTNAME,d.id DEPARTMENTID,p.name POSITIONNAME,p.id POSITIONID "
                        + "from base_position p,base_department d " + "where p.DEPARTMENT_ID = d.id and d.id =? "
                        + " and p.id in (select position_id from base_positionwork where staff_id = ? and valid=?)"; /*
                 * 删除最后的条件
                 * and
                 * rownum
                 * =
                 * 1
                 */
                log.debug("staff:{}", staffId);
                List<Map<String, Object>> workMap = jdbcTemplate.queryForList(sql, departmentId, staffId, true);
                if (null != workMap && !workMap.isEmpty()) {
                    Map<String, Object> map = workMap.get(0);
                    Position p = new Position();
                    p.setName(map.get("POSITIONNAME").toString());
                    p.setId(Long.valueOf(map.get("POSITIONID").toString()));
                    Department d = new Department();
                    d.setName(map.get("DEPARTMENTNAME").toString());
                    d.setId(Long.valueOf(map.get("DEPARTMENTID").toString()));
                    p.setDepartment(d);
                    result.get(i).setPosition(p);
                }
            }
        }
        return departmentWorkPage;

    }


}
