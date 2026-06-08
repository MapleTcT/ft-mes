/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.baomidou.mybatisplus.core.conditions.Wrapper
 *  com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
 *  com.supcon.supfusion.scheduler.server.dao.SchedulerJobMapper
 *  com.supcon.supfusion.scheduler.server.service.SchedulerJobService
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.boot.CommandLineRunner
 *  org.springframework.core.annotation.Order
 *  org.springframework.stereotype.Component
 *  org.springframework.util.ObjectUtils
 */
package com.supcon.supfusion.scheduler.runner;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.supcon.supfusion.scheduler.server.dao.SchedulerJobMapper;
import com.supcon.supfusion.scheduler.server.service.SchedulerJobService;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Component
@Order(value=1)
public class ClearRunner
implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(ClearRunner.class);
    @Autowired
    private SchedulerJobMapper schedulerJobMapper;
    @Autowired
    private SchedulerJobService schedulerJobService;

    public void run(String ... args) {
        QueryWrapper qryWrapper = new QueryWrapper();
        qryWrapper.isNull((Object)"module_code");
        List schedulerJobPos = this.schedulerJobMapper.selectList((Wrapper)qryWrapper);
        if (ObjectUtils.isEmpty((Object)schedulerJobPos)) {
            return;
        }
        ArrayList ids = new ArrayList();
        schedulerJobPos.forEach(job -> ids.add(job.getId()));
        this.schedulerJobService.scheduleDelete(ids);
    }
}

