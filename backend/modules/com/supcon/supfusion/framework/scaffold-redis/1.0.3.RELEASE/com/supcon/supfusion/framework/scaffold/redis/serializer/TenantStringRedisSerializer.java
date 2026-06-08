package com.supcon.supfusion.framework.scaffold.redis.serializer;

import com.supcon.supfusion.framework.scaffold.redis.util.KeyUtil;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.nio.charset.StandardCharsets;

/**
 * @author chenlizhong
 * @date 2020/5/18下午1:44
 * @description
 */
public class TenantStringRedisSerializer extends StringRedisSerializer {

    @Override
    public String deserialize(byte[] bytes) {
        String key = new String(bytes, StandardCharsets.UTF_8);
        key = key.replaceFirst(KeyUtil.getCurrTenantKey() + ":", "");
        return super.deserialize(key.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public byte[] serialize(String string) {
        return (string == null ? null : super.serialize(KeyUtil.getCurrTenantKey() + ":" + string));
    }


}
