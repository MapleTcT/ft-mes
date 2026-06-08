package com.supcon.supfusion.configuration.services.openapi.wrapper;

import com.supcon.supfusion.configuration.services.entity.UploadInfo;
import com.supcon.supfusion.configuration.services.openapi.vo.UploadInfoVO;
import com.supcon.supfusion.framework.scaffold.hibernate.utils.BeanUtil;
import com.supcon.supfusion.framework.scaffold.hibernate.wrapper.BaseLocalWrapper;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class UploadInfoWrapper extends BaseLocalWrapper<UploadInfo, UploadInfoVO> {

    @Override
    public UploadInfoVO e2v(UploadInfo entity) {
       return BeanUtil.copy(entity, UploadInfoVO.class);
    }

    @Override
    public UploadInfo v2e(UploadInfoVO vo) {
        return BeanUtil.copy(vo, UploadInfo.class);
    }
}
