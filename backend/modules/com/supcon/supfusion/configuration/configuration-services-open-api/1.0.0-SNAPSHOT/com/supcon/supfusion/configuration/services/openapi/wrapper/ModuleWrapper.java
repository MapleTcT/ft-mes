package com.supcon.supfusion.configuration.services.openapi.wrapper;

import com.supcon.supfusion.configuration.services.entity.Module;
import com.supcon.supfusion.configuration.services.openapi.vo.ModuleVO;
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
public class ModuleWrapper extends BaseLocalWrapper<Module, ModuleVO> {

    @Override
    public ModuleVO e2v(Module entity) {
        return BeanUtil.copy(entity, ModuleVO.class);
    }

    @Override
    public Module v2e(ModuleVO vo) {
        return BeanUtil.copy(vo, Module.class);
    }

    /**
     * 查询对象转换
     * @param pageQuery
     * @return
     */
//    public List<Object> query2Params(PageQuery<ModuleVO, SampleQuery> pageQuery) {
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