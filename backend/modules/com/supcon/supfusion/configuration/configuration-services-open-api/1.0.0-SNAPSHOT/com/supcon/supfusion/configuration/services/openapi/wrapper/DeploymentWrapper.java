package com.supcon.supfusion.configuration.services.openapi.wrapper;

import com.supcon.supfusion.base.entities.Deployment;
import com.supcon.supfusion.configuration.services.openapi.vo.DeploymentVO;
import com.supcon.supfusion.framework.scaffold.hibernate.utils.BeanUtil;
import com.supcon.supfusion.framework.scaffold.hibernate.wrapper.BaseLocalWrapper;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class DeploymentWrapper extends BaseLocalWrapper<Deployment, DeploymentVO> {

    @Override
    public DeploymentVO e2v(Deployment entity) {

        DeploymentVO copy = BeanUtil.copy(entity, DeploymentVO.class);
        // DeploymentVO与Deployment时间属性类型不一致处理
        if (null != entity.getCreateTime()) {
            copy.setCreateTime(entity.getCreateTime().getTime());
        }
        if (null != entity.getModifyTime()) {
            copy.setModifyTime(entity.getModifyTime().getTime());
        }
        if (null != entity.getPublishTime()) {
            copy.setPublishTime(entity.getPublishTime().getTime());
        }
        return copy;
    }

    @Override
    public Deployment v2e(DeploymentVO vo) {
        return BeanUtil.copy(vo,Deployment.class);
    }
}
