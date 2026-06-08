package com.supcon.supfusion.iam.service.support;

import org.apache.commons.codec.digest.HmacUtils;

/**
 * @author tomcat - <huangjianbo@supos.com>
 * @date 20-5-11 下午4:03
 */
public final class AKSKGenerator {

    private static final String DEFAULT_KEY = "#C6ZntcKMP5!lfS$";

    /**
     * 生成AK/SK
     *
     * @param username
     * @return
     */
    public static KeyValue generate(String username) {
        //AK
        String ak = HmacUtils.hmacMd5Hex(DEFAULT_KEY, username);
        //SK
        String sk = HmacUtils.hmacMd5Hex(DEFAULT_KEY, ak);

        return new KeyValue(ak, sk);
    }
}
