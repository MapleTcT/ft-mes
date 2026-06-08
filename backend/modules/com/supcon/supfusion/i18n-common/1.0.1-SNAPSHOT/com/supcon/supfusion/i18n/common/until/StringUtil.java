package com.supcon.supfusion.i18n.common.until;

import java.util.ArrayList;
import java.util.List;

public class StringUtil {

    //mysql    mariadb   *.?+$^[](){}|\/%&=-_;.,~`@!'" 都需要转义  在进入数据库查询之前处理 在前面添加 \
    //public static final String I18N_VALUE_REGEX= "*.?+$^[](){}|\\/%&=-_;,~`@!'\"";
    //oracle     特殊字符   *.?+$^[](){}|%&=-;,~`@!'"/&  不用转义  \%_ 需要转义  在进入数据库查询之前处理  需要动sql
    //public static final String I18N_VALUE_REGEX_ORACLE= "\\%_";
    //sqlserver  特殊字符    ^ ] 不需要转义  *.?+$[(){}|\/%&=-_;,~`@!" 需要转义 在进入数据库查询之前处理 加上[]  单引号需要单独处理 再加一个单引号
    //public static final String I18N_VALUE_REGEX_SQL_SERVER= "*.?+$[(){}|\\/%&=-_;,~`@!\"";
    public static String getString( char[] i18nKeyChar, String i18nKey, String dbType) {
        if(dbType !=null && !dbType.equals(Constants.STR_NO_SPACE)){
            if(dbType.equals(Constants.DB_TYPE_MARIADB)){
                i18nKey = getStringByDb(i18nKeyChar, i18nKey);
            }else if(dbType.equals(Constants.DB_TYPE_MYSQL)){
                i18nKey = getStringByDb(i18nKeyChar, i18nKey);
            }else if(dbType.equals(Constants.DB_TYPE_ORACLE)){
                i18nKey = getStringByDbOracle(i18nKeyChar, i18nKey);
            }else if(dbType.equals(Constants.DB_TYPE_SQLSERVER)){
                i18nKey = getStringByDbSqlServer(i18nKeyChar, i18nKey);
            }
        }else{
            i18nKey = getStringByDb(i18nKeyChar, i18nKey);
        }
        return i18nKey;
    }

    private static String getStringNoChange(char[] i18nKeyChar, String i18nKey) {
        if (i18nKeyChar != null && i18nKeyChar.length > 0) {
            List<String> i18nKeyCharList = new ArrayList<>();
            for (int i = 0; i < i18nKeyChar.length; i++) {
                i18nKeyCharList.add(String.valueOf(i18nKeyChar[i]));
            }
            StringBuilder sb1 = new StringBuilder();
            for (String s : i18nKeyCharList) {
                sb1.append(s);
            }
            i18nKey = sb1.toString();
        }
        return i18nKey;
    }

    private static String getStringByDb(char[] i18nKeyChar, String i18nKey) {
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
            i18nKey = sb1.toString();
        }
        return i18nKey;
    }
    private static String getStringByDbOracle(char[] i18nKeyChar, String i18nKey) {
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
            i18nKey = sb1.toString();
        }
        return i18nKey;
    }

    private static String getStringByDbSqlServer(char[] i18nKeyChar, String i18nKey) {
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
            i18nKey = sb1.toString();
        }
        return i18nKey;
    }


}
