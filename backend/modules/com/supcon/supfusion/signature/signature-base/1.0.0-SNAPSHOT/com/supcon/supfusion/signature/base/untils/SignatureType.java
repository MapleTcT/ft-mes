package com.supcon.supfusion.signature.base.untils;

import org.apache.commons.lang3.StringUtils;

/**
 * @author zhang yafei
 */
public class SignatureType {
    public static String getType(String type){
        if (StringUtils.isBlank(type)){
            return type;
        }
        switch (type){
            case "singleSign" :
                return "单签";
            case "doubleSign" :
                return "双签";
            default:
                return type;
        }
    }
}
