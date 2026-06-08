package com.supcon.supfusion.license.service.cache;

import com.alibaba.fastjson.JSON;
import com.supcon.supfusion.framework.cloud.security.util.MD5Util;
import com.supcon.supfusion.license.common.constants.Constants;
import com.supcon.supfusion.license.common.enuma.BAPLicenseKey;
import com.supcon.supfusion.license.common.utils.date.DateHelper;
import com.supcon.supfusion.license.common.utils.security.Base64Util;
import com.supcon.supfusion.license.service.bo.LicenseInfoBO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Set;

@Component
@Slf4j
public class LicenseCache {

    @Autowired
    @Qualifier("licenseStringRedisTemplate")
    private StringRedisTemplate redisTemplate;

    private static final String SPLIT = "/";

    /**
     * 将授权信息存入redis
     */
    public void insertLicenseToRedis(LicenseInfoBO licenseInfoBO,Map<Object, Object> entries) {
        try {
            if (!ObjectUtils.isEmpty(entries)) {
                for (Map.Entry<Object, Object> redisEntry : entries.entrySet()) {
                    String[] keySplit = ((String) redisEntry.getKey()).split(SPLIT);
                    String moduleCodeRedis = Base64Util.decode(keySplit[0]);
                    String licenseKeyRedis = Base64Util.decode(keySplit[1]);
                    if (moduleCodeRedis.equals(licenseInfoBO.getModuleCode()) && !licenseKeyRedis.equals(licenseInfoBO.getLicenseKey())) {
                        log.info("当前模块licenseKey发生变化，将原来licenseKey该条数据删除=== moduleCodeRedis：{},licenseKeyRedis:{},licenseInfoBO:{}",
                                JSON.toJSONString(moduleCodeRedis), JSON.toJSONString(licenseKeyRedis), JSON.toJSONString(licenseInfoBO));
                        redisTemplate.opsForHash().delete(Constants.licenseRedisKey, redisEntry.getKey());
                    }
                }
            }
            //将授权信息存入redis
            //已授权
            if (licenseInfoBO.getValue() >= Constants.haveLicense) {
                this.setRedis(licenseInfoBO);
            } else if (Constants.noLicense.equals(licenseInfoBO.getValue())) {
                //未授权
                //判断是否过期   实体配置试用时间12小时  其他业务模块试用时间6小时
                String time = licenseInfoBO.getTime();
                boolean isExpire;
                if (BAPLicenseKey.EC_MODULE.getModuleCode().equalsIgnoreCase(licenseInfoBO.getModuleCode())) {
                    isExpire = (DateHelper.time() - Long.parseLong(time)) > Constants.ecEntityTime;
                } else {
                    isExpire = (DateHelper.time() - Long.parseLong(time)) > Constants.moduleTime;
                }
                //过期
                if (isExpire) {
                    licenseInfoBO.setValue(-1);
                } else {
                    licenseInfoBO.setValue(-2);
                }
                this.setRedis(licenseInfoBO);
            }

        } catch (Exception e) {
            log.error("将授权信息存入redis错误 ======", e);
        }
    }

    private void setRedis(LicenseInfoBO licenseInfoBO) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        String hKey = Base64Util.encode(licenseInfoBO.getModuleCode()) + SPLIT + Base64Util.encode(licenseInfoBO.getLicenseKey());
        String initHash = licenseInfoBO.getModuleCode() + licenseInfoBO.getLicenseKey()
                + licenseInfoBO.getValue() + licenseInfoBO.getTime() + Constants.Salt;
        String hashCode = MD5Util.encode(initHash);

        String hValue = Base64Util.encode(licenseInfoBO.getValue().toString()) + SPLIT +
                Base64Util.encode(licenseInfoBO.getTime()) + SPLIT + hashCode;
        redisTemplate.opsForHash().put(Constants.licenseRedisKey, hKey, hValue);
    }

    /**
     * 防止篡改redis数据
     */
    public void preventUpdateRedis() {
        try {
            //获取redis所有数据,如有篡改数据，则删除当前记录
            Set<Map.Entry<Object, Object>> entrySet = redisTemplate.opsForHash().entries(Constants.licenseRedisKey).entrySet();
            for (Map.Entry<Object, Object> redisEntry : entrySet) {
                String[] keySplit = ((String) redisEntry.getKey()).split(SPLIT);
                String key = Base64Util.decode(keySplit[0]) + Base64Util.decode(keySplit[1]);
                String[] split = ((String) redisEntry.getValue()).split(SPLIT);
                String value = Base64Util.decode(split[0]);
                String time = Base64Util.decode(split[1]);
                String hashCode = split[2];
                String result = key + value + time + Constants.Salt;
                String md5String = MD5Util.encode(result);
                //非法篡改，删除当前记录
                if (!hashCode.equalsIgnoreCase(md5String)) {
                    redisTemplate.opsForHash().delete(Constants.licenseRedisKey, redisEntry.getKey());
                    log.info("当前redis数据被篡改：key:{},value:{}", Base64Util.decode(keySplit[0]) + Base64Util.decode(keySplit[1]), redisEntry.getValue());
                }
            }
        } catch (Exception e) {
            log.error("防止篡改redis数据发生错误 ======", e);
        }
    }

    /**
     * 初始化redis并发数授权
     *
     * @param valueFromSCDog
     */
    public void initRedisConcurrent(Integer valueFromSCDog) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        String moduleCode = BAPLicenseKey.BAP.getModuleCode();
        String licenseKey = BAPLicenseKey.BAP.getLicenseKey();
        String hKey = Base64Util.encode(moduleCode) + SPLIT + Base64Util.encode(licenseKey);
        String hValueFromRedis = (String) redisTemplate.opsForHash().get(Constants.licenseRedisKey, hKey);
        String time;
        if (!ObjectUtils.isEmpty(hValueFromRedis)) {
            String[] split = hValueFromRedis.split(SPLIT);
            time = Base64Util.decode(split[1]);
        } else {
            time = String.valueOf(DateHelper.time());
        }
        String initHash = moduleCode + licenseKey
                + valueFromSCDog + time + Constants.Salt;
        String hashCode = MD5Util.encode(initHash);

        String hValue = Base64Util.encode(valueFromSCDog.toString()) + SPLIT +
                Base64Util.encode(time) + SPLIT + hashCode;
        redisTemplate.opsForHash().put(Constants.licenseRedisKey, hKey, hValue);
    }
}
