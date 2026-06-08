package com.supcon.supfusion.custon.property.server;

import com.baomidou.mybatisplus.extension.service.IService;
import com.supcon.supfusion.custon.property.dao.entity.Validate;

import java.util.Set;

/**
 * @author zhang yafei
 */
public interface ValidateService extends IService<Validate> {
    Set<Validate> getByFieldCode(String fieldCode);
}
