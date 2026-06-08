package com.supcon.supfusion.configuration.services.openapi.wrapper;

import com.supcon.supfusion.configuration.services.entity.UploadInfoBatch;
import com.supcon.supfusion.configuration.services.openapi.vo.UploadInfoBatchVO;
import com.supcon.supfusion.framework.scaffold.hibernate.utils.BeanUtil;
import com.supcon.supfusion.framework.scaffold.hibernate.wrapper.BaseLocalWrapper;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class UploadInfoBatchWrapper extends BaseLocalWrapper<UploadInfoBatch, UploadInfoBatchVO> {

    @Override
    public UploadInfoBatchVO e2v(UploadInfoBatch entity) {
       return BeanUtil.copy(entity, UploadInfoBatchVO.class);
    }

    @Override
    public UploadInfoBatch v2e(UploadInfoBatchVO vo) {
        return BeanUtil.copy(vo, UploadInfoBatch.class);
    }
}
