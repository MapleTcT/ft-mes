package com.supcon.supfusion.scheduler.server.service.Utils;


import com.baomidou.mybatisplus.core.toolkit.StringUtils;
/**
 * @Author kk.C
 * @Description: 特殊字符转义工具类
 * @Date 2021/1/4 9:48
 */
public class EscapeUtil {
    //mysql的模糊查询时特殊字符转义
    public static String escapeChar(String before){
        if(StringUtils.isNotBlank(before)){
//            before = before.replaceAll("/", "\\\\/") ;
            before = before.replaceAll("_", "\\\\_") ;
            before = before.replaceAll("%", "\\\\%") ;
        }
        return before ;
    }
}