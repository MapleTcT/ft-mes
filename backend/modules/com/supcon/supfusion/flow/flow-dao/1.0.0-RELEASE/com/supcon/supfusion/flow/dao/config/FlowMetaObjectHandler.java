/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.dao.config;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.supcon.supfusion.framework.cloud.common.util.DateTimeUtil;
import com.supcon.supfusion.framework.scaffold.mybatis.handler.BaseEntityMetaObjectHandler;

/**
 * @author: zhuangmh
 * @date: 2021年2月19日 下午4:11:17
 */
@Component
@Primary
public class FlowMetaObjectHandler extends BaseEntityMetaObjectHandler {

    /**
     * @see com.baomidou.mybatisplus.core.handlers.MetaObjectHandler#insertFill(org.apache.ibatis.reflection.MetaObject)
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        super.insertFill(metaObject);
        String curTs = getUtc0Str();
        this.strictInsertFill(metaObject, "startTime", String.class, curTs);
        this.strictInsertFill(metaObject, "endTime", String.class, curTs);
    }
    
    private String getUtc0Str(){
        LocalDateTime utc = LocalDateTime.ofInstant(new Date().toInstant(), ZoneId.of("UTC"));
        ZonedDateTime utc0 = ZonedDateTime.of(utc, ZoneId.of("UTC"));
        return utc0.format(DateTimeUtil.UTC0_FORMAT);
    }


}
