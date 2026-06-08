package com.supcon.supfusion.auth.common.utils;

import flexjson.JSONSerializer;

import java.util.StringTokenizer;

/**
 * @author caokele
 */
public class JSONPlainSerializer extends JSONSerializer {

    public JSONPlainSerializer excludeClass() {
        this.exclude("*.Class");
        return this;
    }

    public JSONPlainSerializer excludeAudit() {
        this.exclude(
                "*.createStaffID,*.modifyStaffID,*.deleteStaffID,*.createTime,*.modifyTime,*.deleteTime,*.manager,*.modifyStaff,*.createStaff,*.deleteStaff");
        return this;
    }

    public JSONPlainSerializer excludeTree() {
        this.exclude("*.layRec,*.layNo");
        return this;
    }

    public JSONPlainSerializer excludeCid() {
        this.exclude("*.Cid");
        return this;
    }

    /**
     * 对象序列化为JSON
     *
     * @param target
     * @param include
     * @return
     */
    public static String serializeAsJSON(Object target, String include) {
        if (target == null) {
            return null;
        }
        if (include == null || include.isEmpty()) {
            return null;
        }
        JSONPlainSerializer serializer = new JSONPlainSerializer();
        StringTokenizer st = new StringTokenizer(include, ",");
        if (st != null) {
            while (st.hasMoreTokens()) {
                serializer.include(st.nextToken());
            }
            serializer.exclude("*");
        }
        return serializer.serialize(target);
    }

}
