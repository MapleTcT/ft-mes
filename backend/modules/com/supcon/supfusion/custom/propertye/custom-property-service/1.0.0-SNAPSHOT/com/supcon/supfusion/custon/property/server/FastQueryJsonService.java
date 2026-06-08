package com.supcon.supfusion.custon.property.server;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.supcon.supfusion.custon.property.dao.entity.FastQueryJson;

import java.util.List;

/**
 * @author zhang yafei
 */
public interface FastQueryJsonService extends IService<FastQueryJson> {
    List<FastQueryJson> getFastQueryJsonByViewCode(String viewCode);
}
