/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.boot.ApplicationArguments
 *  org.springframework.boot.ApplicationRunner
 *  org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
 *  org.springframework.stereotype.Component
 *  org.springframework.util.ObjectUtils
 */
package com.supcon.supos.suposgateway.task;

import com.supcon.supos.suposgateway.feign.dto.LicenseInfoDTO;
import com.supcon.supos.suposgateway.repository.LicenseDao;
import com.supcon.supos.suposgateway.utils.AESUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Component
@ConditionalOnProperty(name={"license.enabled"}, havingValue="false")
public class LicenseServiceTask
implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(LicenseServiceTask.class);
    public static ConcurrentHashMap<String, LicenseInfoDTO> licenseMap = new ConcurrentHashMap();
    private static final ScheduledExecutorService SCHEDULED_EXECUTOR = Executors.newSingleThreadScheduledExecutor();
    @Autowired
    private LicenseDao licenseDao;

    public void run(ApplicationArguments args) {
        log.info("\u5b9a\u65f6\u4efb\u52a1\u5237\u65b0\u6388\u6743\u4fe1\u606f\u5230\u7f13\u5b58 \u5f00\u59cb\u6267\u884c ======");
        SCHEDULED_EXECUTOR.scheduleAtFixedRate(this::refreshLicense, 100L, 60000L, TimeUnit.MILLISECONDS);
    }

    private void refreshLicense() {
        try {
            this.licenseDao.getLicenseFromRedis();
        }
        catch (Exception e) {
            log.error("\u5b9a\u65f6\u4efb\u52a1\u5237\u65b0\u6388\u6743\u4fe1\u606f\u5230\u7f13\u5b58\u53d1\u751f\u9519\u8bef ======= ", (Throwable)e);
        }
    }

    public List<String> getModuleCodeFromProperties() {
        String moduleCode = AESUtil.decrypt("L7CPWDHUYnbe3QNlp9QM61hUK8BCKp3C8ydDD+W1iZBvO78s3nn4VVJiEvequYimoI46V+se6gEUhlKXcTYFkB2hk21vZ005idCOaalIUKWwQxOlhjPpAdIKZQBxpiZ3lz+QBwXKGpDL0HYalM/yvkGsJbXamjSrdpyYqaOqnOjqQ+yerUiMvAG+gHR7tssmusjGDzdo28sP9gCNfZrqZeNFUHg/d9SX+Z81bixM56HMg3NFAT2LH4cRiLc/ftDq1OhEOpak/vIGkfzxUiYLBhDJurTB6lLA24+NkSgxiaKxJIB9fKumyEfY0MR/tERj4A5QZOJ6JE95brfxpka8fpG45tvH5Z0cz+30gK/6NMCicOIYQxbmcAsA3Luqs1kDO+D0r0Q2ZzL/AL0BxOs4uDuUhBzVQcjrDq9pfV1GxxyiFX3BFBLhFGllFU99YzORUP5H2OotxBowbeLOIsVvU6SFNgijpAa6NsbhLIQA3mxQaniJ8vMk0eh7GapkpW25Un+FvA4H0V81GcDPisJtcmpV8BcJ6fNjTaAjb20IAu+MdEsIthEC6NTX3J5x0pox6GLmlucXUAAtU/6l8EjP06eAnBFxVBoNzPIZ2nHdSx1DuoNqCK7bryzCtCVcHhmUKLYaKqxy7mjAKLp6AetzhnbQZ+DEdSxfeiKIqIgKASoGLpxcavlfDpcnaog909wLYhpQfUQZI/JmmJ910VUlrRvkjz1/e6uNE4/vIic9A4ktKz5TP5TjyGxAhtM9P499HvrUJVe6vhBOzlZFt6G2dEOIe09vMtuNctfeEUocDsofOQ7r2aWO21VirDSF/jmcTkgXyvfzi5KtWGPV+lGEKmLRhNAN3YyggiIxKjnrB7fVIXpzSwShGUgUkHumXE+QseVk3No/xCtnCMjYMe6aoA3vR08WK4kpCiDXFU10bpdONbb7GlxtnDpGpQIGlPOu", "baseset1unvrectb");
        if (!ObjectUtils.isEmpty((Object)moduleCode)) {
            String[] split = moduleCode.split(",");
            return Arrays.asList(split);
        }
        return new ArrayList<String>();
    }
}

