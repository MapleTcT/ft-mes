/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.engine.server.delegate;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.supcon.supfusion.flow.common.enumeration.FlowErrorEnum;
import com.supcon.supfusion.flow.common.exception.FormatException;
import com.supcon.supfusion.framework.cloud.common.util.DateTimeUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * @author: zhuangmh
 * @date: 2020年12月26日 下午4:51:41
 */
@Component("formatDelegate")
@Slf4j
public class DateFormatDelegate {
    
    public String formatLongDate(String dateStr, String format) {
        Date date = null;
        try {
            if (StringUtils.isEmpty(dateStr)) {
                // http://jira.bluetron.cn/browse/SUP-9708
                date = new Date();
            } else {
                if (StringUtils.isEmpty(format)) {
                    date = new Date(Long.parseLong(dateStr));
                } else {
                    format = format.replace("YYYY", "yyyy").replace("DD", "dd");
                    date = new SimpleDateFormat(format).parse(dateStr);
                    // 减掉8小时(默认处理,否则前端需要传时间戳)
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(date);
                    cal.add(Calendar.HOUR_OF_DAY, -8);
                    date = cal.getTime();
                }
            }
            //引擎解析需要ISO时间格式
            LocalDateTime utc = LocalDateTime.ofInstant(date.toInstant(), ZoneId.of("UTC"));
            ZonedDateTime utc0 = ZonedDateTime.of(utc, ZoneId.of("UTC"));
            String utcTime = utc0.format(DateTimeUtil.UTC0_FORMAT);
            log.info("解析前时间为：{}，解析后时间为：{}", dateStr, utcTime);
            return utcTime;
        } catch (Exception e) {
            log.error("解析长整时间异常, 入参：{}", dateStr, e);
            throw new FormatException(FlowErrorEnum.TIME_FORMAT_ERROR, e);
        }
        
    }

}
