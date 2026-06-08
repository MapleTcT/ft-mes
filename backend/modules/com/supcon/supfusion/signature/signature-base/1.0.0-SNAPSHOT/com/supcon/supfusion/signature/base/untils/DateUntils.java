package com.supcon.supfusion.signature.base.untils;

import com.supcon.supfusion.framework.cloud.common.util.DateUtil;
import com.supcon.supfusion.signature.base.enums.SignatureErrorEnum;
import com.supcon.supfusion.signature.base.exception.SignatureException;
import org.apache.commons.lang3.StringUtils;

import java.util.Calendar;
import java.util.Date;

/**
 * @author zhang yafei
 */
public class DateUntils {
    //时间格式 yyyy-MM-dd HH:mm:ss
    public static String DATA_TIME_FORMAT_REG = "^((([0-9]{3}[1-9]|[0-9]{2}[1-9][0-9]{1}|[0-9]{1}[1-9][0-9]{2}|[1-9][0-9]{3})-(((0[13578]|1[02])-(0[1-9]|[12][0-9]|3[01]))|((0[469]|11)-(0[1-9]|[12][0-9]|30))|(02-(0[1-9]|[1][0-9]|2[0-8]))))|((([0-9]{2})(0[48]|[2468][048]|[13579][26])|((0[48]|[2468][048]|[3579][26])00))-02-29))\\s+([0-1]?[0-9]|2[0-3]):([0-5][0-9]):([0-5][0-9])$";
    //校验时间是不是24整点
    public static String DATA_TIME_FORMAT_24_REG = "^((([0-9]{3}[1-9]|[0-9]{2}[1-9][0-9]{1}|[0-9]{1}[1-9][0-9]{2}|[1-9][0-9]{3})-(((0[13578]|1[02])-(0[1-9]|[12][0-9]|3[01]))|((0[469]|11)-(0[1-9]|[12][0-9]|30))|(02-(0[1-9]|[1][0-9]|2[0-8]))))|((([0-9]{2})(0[48]|[2468][048]|[13579][26])|((0[48]|[2468][048]|[3579][26])00))-02-29))\\s+(24):([0][0]):([0][0])$";
    //时间格式 yyyy-MM-dd
    public static String DATA_FORMAT_REG = "^((([0-9]{3}[1-9]|[0-9]{2}[1-9][0-9]{1}|[0-9]{1}[1-9][0-9]{2}|[1-9][0-9]{3})-(((0[13578]|1[02])-(0[1-9]|[12][0-9]|3[01]))|((0[469]|11)-(0[1-9]|[12][0-9]|30))|(02-(0[1-9]|[1][0-9]|2[0-8]))))|((([0-9]{2})(0[48]|[2468][048]|[13579][26])|((0[48]|[2468][048]|[3579][26])00))-02-29))$";

    public static boolean formatCheck(String date) {
        if (StringUtils.isBlank(date)) {
            return false;
        }
        return date.matches(DATA_TIME_FORMAT_REG);
    }

    public static String getPreviousTime(int i) {
        switch (i) {
            case 1:
                return getDateSrt(Calendar.DATE, -1);
            case 2:
                return getDateSrt(Calendar.DATE, -3);
            case 3:
                return getDateSrt(Calendar.DATE, -7);
            case 4:
                return getDateSrt(Calendar.MONTH, -1);
            case 5:
                return getDateSrt(Calendar.MONTH, -3);
            case 6:
                return getDateSrt(Calendar.MONTH, -6);
            case 7:
                return getDateSrt(Calendar.YEAR, -1);
            default:
                throw new SignatureException(SignatureErrorEnum.TIME_UNKNOWN_TYPE_ERROR);
        }
    }

    private static String getDateSrt(int calendar, int value) {
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.add(calendar, value);
        return DateUtil.format(c.getTime(), DateUtil.PATTERN_DATETIME);
    }
    public static String getDateSrt(String dataTime,int calendar, int value) {
        Date parse = DateUtil.parse(dataTime, DateUtil.PATTERN_DATETIME);
        Calendar c = Calendar.getInstance();
        c.setTime(parse);
        c.add(calendar, value);
        return DateUtil.format(c.getTime(), DateUtil.PATTERN_DATETIME);
    }
    public static void main(String[] args) {
        String s = "2020-10-23 24:00:00";
        boolean b = s.matches(DATA_TIME_FORMAT_24_REG);
        System.out.println("结果:" + b);
//        Date parse = DateUtil.parse(s, DateUtil.PATTERN_DATETIME);
//        System.out.println("结果:" + parse);
        String dateSrt = getDateSrt(s, Calendar.DATE, 1);
        System.out.println("结果:" + dateSrt);
    }

}
