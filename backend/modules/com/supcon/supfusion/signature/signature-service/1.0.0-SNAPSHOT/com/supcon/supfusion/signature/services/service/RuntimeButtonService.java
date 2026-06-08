package com.supcon.supfusion.signature.services.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.supcon.supfusion.signature.dao.entity.EcButton;
import com.supcon.supfusion.signature.dao.entity.RuntimeButton;

/**
 * @author zhang yafei
 */
public interface RuntimeButtonService extends IService<RuntimeButton> {
    void ecSynchronizedToRuntime(EcButton ecButton);
}
