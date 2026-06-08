package com.supcon.supfusion.rbac.common.utils;

import com.supcon.supfusion.rbac.common.Contants.Constants;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.List;

import static com.supcon.supfusion.rbac.common.Contants.Constants.*;

public class StringUtils {

    public static String getStringDb(String str, String db) {
        if (!ObjectUtils.isEmpty(str) && !ObjectUtils.isEmpty(db)) {
            if(db.equals(Constants.DB_TYPE_MARIADB)){
                str = getStringByDb(str.toCharArray());
            }else if(db.equals(Constants.DB_TYPE_MYSQL)){
                str = getStringByDb(str.toCharArray());
            }else if(db.equals(Constants.DB_TYPE_ORACLE)){
                str = getStringByDbOracle(str.toCharArray());
            }else if(db.equals(Constants.DB_TYPE_SQLSERVER)){
                str = getStringByDbSqlServer(str.toCharArray());
            }
            str = str.replaceAll(MNE_WILD_CARD, SQL_WILD_CARD);
        }
        return str;
    }

    private static String getStringByDb(char[] i18nKeyChar) {
        if (i18nKeyChar != null && i18nKeyChar.length > 0) {
            List<String> i18nKeyCharList = new ArrayList<>();
            for (int i = 0; i < i18nKeyChar.length; i++) {
                if (Constants.I18N_VALUE_REGEX.contains(String.valueOf(i18nKeyChar[i]))) {
                    i18nKeyCharList.add(Constants.SQL_CHECK);
                    i18nKeyCharList.add(String.valueOf(i18nKeyChar[i]));
                } else {
                    i18nKeyCharList.add(String.valueOf(i18nKeyChar[i]));
                }
            }
            StringBuilder sb1 = new StringBuilder();
            for (String s : i18nKeyCharList) {
                sb1.append(s);
            }
            return sb1.toString();
        }
        return null;
    }
    private static String getStringByDbOracle(char[] i18nKeyChar) {
        if (i18nKeyChar != null && i18nKeyChar.length > 0) {
            List<String> i18nKeyCharList = new ArrayList<>();
            for (int i = 0; i < i18nKeyChar.length; i++) {
                if (Constants.I18N_VALUE_REGEX_ORACLE.contains(String.valueOf(i18nKeyChar[i]))) {
                    i18nKeyCharList.add(Constants.SQL_CHECK);
                    i18nKeyCharList.add(String.valueOf(i18nKeyChar[i]));
                } else {
                    i18nKeyCharList.add(String.valueOf(i18nKeyChar[i]));
                }
            }
            StringBuilder sb1 = new StringBuilder();
            for (String s : i18nKeyCharList) {
                sb1.append(s);
            }
            return sb1.toString();
        }
        return null;
    }

    private static String getStringByDbSqlServer(char[] i18nKeyChar) {
        if (i18nKeyChar != null && i18nKeyChar.length > 0) {
            List<String> i18nKeyCharList = new ArrayList<>();
            for (int i = 0; i < i18nKeyChar.length; i++) {
                if (Constants.I18N_VALUE_REGEX_SQL_SERVER.contains(String.valueOf(i18nKeyChar[i]))) {
                    i18nKeyCharList.add(Constants.SQL_SERVER_PREFIX);
                    i18nKeyCharList.add(String.valueOf(i18nKeyChar[i]));
                    i18nKeyCharList.add(Constants.SQL_SERVER_SUFFIX);
                } else if(String.valueOf(i18nKeyChar[i]).equals("'")){
                    i18nKeyCharList.add("''");
                }else{
                    i18nKeyCharList.add(String.valueOf(i18nKeyChar[i]));
                }
            }
            StringBuilder sb1 = new StringBuilder();
            for (String s : i18nKeyCharList) {
                sb1.append(s);
            }
            return sb1.toString();
        }
        return null;
    }


    /**
     * 转义字符串  (%作为普通字符 * 为通配符)
     *
     * @param str
     * @return
     */
    public static String escape(String str) {
        if(null != str && str.trim().length() > 0){
            // 将字符串中所有&转义
            str = str.replaceAll(SQL_ESCAPE_CHAR, SQL_CHECK + SQL_ESCAPE_CHAR);
            // 将字符串中的所有%转义
            str = str.replaceAll(SQL_WILD_CARD, SQL_CHECK + SQL_WILD_CARD);
            // 包含 * 则将所有 * 替换为 %
            str = str.replaceAll(MNE_WILD_CARD, SQL_WILD_CARD);
        }
        return str;
    }



    public static <E> boolean listContainsElement(List<E> sourceList, E element) {
        if (CollectionUtils.isEmpty(sourceList) || element == null){
            return false;
        }
        for (E tip : sourceList){
            if(element.equals(tip)){
                return true;
            }
        }
        return false;
    }
}
