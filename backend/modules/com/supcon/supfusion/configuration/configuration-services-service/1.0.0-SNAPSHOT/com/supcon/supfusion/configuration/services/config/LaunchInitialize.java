package com.supcon.supfusion.configuration.services.config;

import com.supcon.supfusion.configuration.services.entity.Module;
import com.supcon.supfusion.configuration.services.service.ModuleService;
import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.IdGenerator;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class LaunchInitialize implements ApplicationListener<ApplicationStartedEvent> {

    @Autowired
    private ModuleService moduleService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        List<Module> modules = moduleService.findAllModules();
        if (ObjectUtils.isEmpty(modules)){
            return;
        }
        List<String> moduleCodes = modules.stream().map(Module::getCode).collect(Collectors.toList());
        List<String> appids = jdbcTemplate.queryForList("select MODULE_CODE from module_company_ref group by MODULE_CODE", String.class);
        List<String> filter_moduleCodes = moduleCodes.stream().filter(a -> !appids.contains(a)).collect(Collectors.toList());
        if (ObjectUtils.isEmpty(filter_moduleCodes)){
            return;
        }
        String sql = "insert into module_company_ref (ID,MODULE_CODE,COMPANY_ID) values (?,?,?)";
        List<Object[]> objects = filter_moduleCodes.stream().map(moduleCode -> new Object[]{IDGenerator.newInstance().generate().longValue(), moduleCode, -1L}).collect(Collectors.toList());
        jdbcTemplate.batchUpdate(sql,objects);
        String updateSql = "update rbac_menuinfo_company_ref set appid = (select app from rbac_menuinfo rm where rm.id = rbac_menuinfo_company_ref.menuinfo_id and rm.valid = 1) where appid is null";
        jdbcTemplate.update(updateSql);
    }
}
