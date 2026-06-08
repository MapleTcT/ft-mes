package com.supcon.supfusion.organization.service.init;

import com.alibaba.fastjson.JSON;
import com.supcon.supfusion.auth.api.dto.UserDetailDTO;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.framework.cloud.common.events.TenantInfo;
import com.supcon.supfusion.framework.cloud.common.events.TenantInfoLocalStorage;
import com.supcon.supfusion.organization.common.constants.Constants;
import com.supcon.supfusion.organization.common.utils.SystemUtil;
import com.supcon.supfusion.organization.common.utils.ThreadPoolUtils;
import com.supcon.supfusion.organization.dao.mapper.person.PersonMapper;
import com.supcon.supfusion.organization.manager.OrganizationAdapter;
import com.supcon.supfusion.organization.service.PersonService;
import com.supcon.supfusion.organization.service.bo.person.PersonUserBO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
@Slf4j
public class InitPersonUser implements ApplicationRunner {
    @Autowired
    private OrganizationAdapter organizationAdapter;
    @Autowired
    private PersonService personService;
    @Autowired
    private PersonMapper personMapper;

    @Override
    public void run(ApplicationArguments args) {
        ThreadPoolUtils.getThreadPool().execute(() -> {
            Set<TenantInfo> tenantInfoSet = TenantInfoLocalStorage.getAll();
            log.info("全部租户信息,tenantInfoSet:{}", JSON.toJSONString(tenantInfoSet));
            if (Constants.LINUX.equalsIgnoreCase(SystemUtil.getOS())) {
                tenantInfoSet.forEach(tenantInfo -> {
                    RpcContext rpcContext = RpcContext.getContext();
                    rpcContext.setTenantId(tenantInfo.getId());
                    this.commonInit();
                });
            } else {
                this.commonInit();
            }
        });
    }

    public void commonInit() {
        Integer count = personMapper.getCountOfUser();
        Integer personUserCount = personService.countPersonUser();
        List<UserDetailDTO> userDetailDTOS = organizationAdapter.getAllPersonsUsers();
        if (count == 0 || userDetailDTOS.size() - personUserCount > 1) {
            log.info("初始化用户信息开始 ===== ");
            try {
                int i = 0;
                while (i == 0) {
                    try {
                        log.info("初始化用户信息userDetailDTOS:{}", JSON.toJSONString(userDetailDTOS));
                        i = 1;
                        for (UserDetailDTO userDetailDTO : userDetailDTOS) {
                            PersonUserBO personUserBO = new PersonUserBO();
                            personUserBO.setPersonId(userDetailDTO.getPersonId());
                            personUserBO.setUserId(userDetailDTO.getId());
                            personUserBO.setUserName(userDetailDTO.getUserName());
                            personService.saveOrUpdateUserByPersonId(personUserBO);
                        }
                        log.info("初始化用户信息成功 ======");
                    } catch (Exception e) {
                        log.info("初始化用户信息失败 =======");
                    }
                }
            } catch (Exception e) {
                log.info("初始化用户信息 =====");
            }
        }
    }
}
