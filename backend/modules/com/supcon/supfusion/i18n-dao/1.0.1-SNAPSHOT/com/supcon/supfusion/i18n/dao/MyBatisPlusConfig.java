package com.supcon.supfusion.i18n.dao;

import com.baomidou.mybatisplus.extension.plugins.PaginationInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Description: MybatisPlus配置类
 * @Author: ShenZhiqiang
 * @Date: Create in  9:28 2020/6/13
 * @Modified:
 */
@Configuration("i18nMyBatisPlusConfig")
public class MyBatisPlusConfig {

    /**
     * 分页插件
     * @return
     */
    @ConditionalOnMissingBean(PaginationInterceptor.class)
    @Bean("i18nPaginationInterceptor")
    public PaginationInterceptor paginationInterceptor() {
        return new PaginationInterceptor();
    }

}


