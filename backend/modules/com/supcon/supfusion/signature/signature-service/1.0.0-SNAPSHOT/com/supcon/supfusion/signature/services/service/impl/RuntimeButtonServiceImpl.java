package com.supcon.supfusion.signature.services.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.signature.dao.entity.EcButton;
import com.supcon.supfusion.signature.dao.entity.RuntimeButton;
import com.supcon.supfusion.signature.dao.mappers.RuntimeButtonMapper;
import com.supcon.supfusion.signature.services.service.RuntimeButtonService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

/**
 * @author zhang yafei
 */
@Service
public class RuntimeButtonServiceImpl extends ServiceImpl<RuntimeButtonMapper, RuntimeButton> implements RuntimeButtonService {
    @Override
    public void ecSynchronizedToRuntime(EcButton ecButton) {
        RuntimeButton runtimeButton = super.getById(ecButton.getCode());
        if (runtimeButton == null){
            return;
        }
        Long version = runtimeButton.getVersion();
        BeanUtils.copyProperties(ecButton,runtimeButton);
        runtimeButton.setVersion(version);
        super.saveOrUpdate(runtimeButton);
    }
}
