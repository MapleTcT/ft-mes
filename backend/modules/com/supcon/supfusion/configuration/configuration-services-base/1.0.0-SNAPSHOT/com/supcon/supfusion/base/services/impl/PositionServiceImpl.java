package com.supcon.supfusion.base.services.impl;

import com.supcon.supfusion.base.dao.PositionDaoImpl;
import com.supcon.supfusion.base.entities.Position;
import com.supcon.supfusion.base.services.BaseServiceImpl;
import com.supcon.supfusion.base.services.PositionService;
import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.framework.scaffold.hibernate.page.Page;
import com.supcon.supfusion.organization.api.PersonApiService;
import com.supcon.supfusion.organization.api.dto.PositionDetailDTO;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * @Description: TODO
 * @Version V1.0
 * @Auther: huning
 * @Date: 2020/7/21
 */
@Slf4j
@Service
@Transactional
public class PositionServiceImpl extends BaseServiceImpl implements PositionService {

    @Autowired
    private PersonApiService personApiService;
    @Autowired
    private PositionDaoImpl positionDao;
    @Override
    public Position load(Long id) {
        return positionDao.get(id);
    }

    @Override
    public List<Position> getTreeChildren(Long positionId, Long companyId) {
        if (companyId == null) {
            companyId = getCurrentCompanyId();
        }
        ListResult<PositionDetailDTO> result = personApiService.querySubPositionByParentId(positionId, true, companyId);
        if (result.getList() == null || result.getList().size() <= 0) {
            return null;
        }

        List<Long> ids = new ArrayList<>(result.getList().size());
        Collection<PositionDetailDTO> plist = result.getList();
        plist.forEach(pdto -> {
            ids.add(pdto.getId());
        });

        List<Position> list = positionDao.findByCriteria(
                Restrictions.in("id", ids), Restrictions.eq("valid", true));
        return list;
    }

    @Override
    public List<Position> getAssignPositions(String assignPositions) {
        if (null == assignPositions || "".equals(assignPositions)) {
            return Collections.emptyList();
        }
        String[] positons = assignPositions.split(";");
        Long[] posIdArr = new Long[positons.length];
        DetachedCriteria detachedCriteria = DetachedCriteria
                .forClass(Position.class);
        detachedCriteria.add(Restrictions.eq("valid", true));
        Map<String, String> positionMap = new LinkedHashMap<String, String>();
        for (int i = 0; i < positons.length; i++) {
            String[] pos = positons[i].split(",");
            positionMap.put(pos[0], pos[1]);
            posIdArr[i] = Long.valueOf(pos[0]);
        }
        if (posIdArr.length > 999) {
            int i = posIdArr.length;
            int p = i / 999 + (i % 999 > 0 ? 1 : 0);
            Long[][] idss = new Long[p][999];
            for (int j = 0; j < p; j++) {
                System.arraycopy(posIdArr, 999 * j, idss[j], 0, j < p - 1 ? 999
                        : i % 999);
            }
            Criterion rc = null, c;
            for (int j = 0; j < p; j++) {
                Long[] ids = idss[j];
                c = Restrictions.in("id", ids);
                if (j > 0)
                    rc = Restrictions.or(rc, c);
                else
                    rc = c;
            }
            detachedCriteria.add(rc);
        }

        else {
            detachedCriteria.add(Restrictions.in("id", posIdArr));
        }
        List<Position> list = new ArrayList<Position>();
        if (posIdArr.length > 0) {
            list = positionDao.findByCriteria(detachedCriteria);
        }
        return list;
    }

    @Override
    public List<Position> getAllParents(String positionLayRec) {
        String[] positionIds = positionLayRec.split("-");
        Long[] ids = new Long[positionIds.length];
        for (int i = 0; i < positionIds.length; i++) {
            ids[i] = Long.parseLong(positionIds[i]);
        }

        List<Position> parents = positionDao.findByCriteria(
                Restrictions.in("id", ids), Restrictions.eq("valid", true));
        return parents;
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Page<Position> getByPage(Page page, DetachedCriteria detachedCriteria) {
        return positionDao.findByPage(page, detachedCriteria);
    }
}
