package com.supcon.supfusion.configuration.services.openapi.wrapper;

import com.supcon.supfusion.configuration.services.entity.Entity;
import com.supcon.supfusion.configuration.services.openapi.vo.EntityVO;
import com.supcon.supfusion.framework.scaffold.hibernate.utils.BeanUtil;
import com.supcon.supfusion.framework.scaffold.hibernate.wrapper.BaseLocalWrapper;
import lombok.AllArgsConstructor;

/**
 * @author ricky
 * @version 1.0.0
 * @date 2019-07-23 14:30
 * @copyright
 */
@AllArgsConstructor
public class EntityWrapper extends BaseLocalWrapper<Entity, EntityVO> {

    @Override
    public EntityVO e2v(Entity entity) {
        return BeanUtil.copy(entity, EntityVO.class);
    }

    @Override
    public Entity v2e(EntityVO vo) {
        return BeanUtil.copy(vo, Entity.class);
    }

    /**
     * 查询对象转换
     * @param pageQuery
     * @return
     */
//    public List<Object> query2Params(PageQuery<EntityVO, SampleQuery> pageQuery) {
//        List<Object> paramsList = new ArrayList<Object>();
//        if(null != pageQuery && null != pageQuery.getQuery()) {
//            SampleQuery sampleQuery = pageQuery.getQuery();
//            if(null != sampleQuery.getEndTime()) {
//                paramsList.add(sampleQuery.getEndTime());
//            }
//            if(null != sampleQuery.getStartTime()) {
//                paramsList.add(sampleQuery.getStartTime());
//            }
//        }
//        return paramsList;
//    }

}