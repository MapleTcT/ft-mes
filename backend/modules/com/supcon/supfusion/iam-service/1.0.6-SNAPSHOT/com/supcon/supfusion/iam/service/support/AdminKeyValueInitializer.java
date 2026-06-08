package com.supcon.supfusion.iam.service.support;

import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.framework.cloud.common.events.TenantInfo;
import com.supcon.supfusion.framework.cloud.common.events.TenantInfoLocalStorage;
import com.supcon.supfusion.iam.dao.entity.AccountPO;
import com.supcon.supfusion.iam.service.AccountService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author tomcat
 * @date 21-3-22 下午4:34
 */
@Setter
@Getter
@Slf4j
@Component
public class AdminKeyValueInitializer implements CommandLineRunner {

    @Autowired
    private AccountService accountService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void run(String... args) throws Exception {
        //put admin to redis
        try {
            stringRedisTemplate.opsForValue().setIfAbsent(AdminKeyValue.AK, AdminKeyValue.SK);
        } catch (Exception e) {
            log.error("put admin ak/sk to redis failed, system will exit", e);
            System.exit(0);
        }
        //load from db and put to redis
        try {
            Set<TenantInfo> tenantInfos = TenantInfoLocalStorage.getAll();
            tenantInfos.forEach(t -> {
                RpcContext.getContext().setTenantId(t.getId());
                List<AccountPO> accounts = accountService.findAll();
                if (!CollectionUtils.isEmpty(accounts)) {
                    Map<String, String> akskMap = accounts.stream().collect(Collectors.toMap(AccountPO::getAccessKey, AccountPO::getSecretKey));
                    stringRedisTemplate.opsForValue().multiSet(akskMap);
                }
            });
        } catch (Exception e) {
            log.error("put tenant all ak/sk to redis failed, system will exit", e);
            System.exit(0);
        }
    }
}
