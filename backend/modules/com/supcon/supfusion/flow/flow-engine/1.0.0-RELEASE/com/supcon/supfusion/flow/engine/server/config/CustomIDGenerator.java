/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.engine.server.config;

import org.flowable.common.engine.impl.cfg.IdGenerator;

import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;

/**
 * @author: zhuangmh
 * @date: 2020年5月29日 下午9:20:09
 */
public class CustomIDGenerator extends IDGenerator implements IdGenerator {

    /**
     * 提供工作流引擎UUID
     */
    @Override
    public String getNextId() {
        return super.generate().toString();
    }

}
