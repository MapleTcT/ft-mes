package com.supcon.supfusion.auth.service.cache;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supcon.supfusion.auth.common.constants.Constants;
import com.supcon.supfusion.auth.dao.mapper.IpBlackWhiteMapper;
import com.supcon.supfusion.auth.dao.po.IpBlackWhitePO;
import com.supcon.supfusion.auth.service.bo.IpBlackWhiteBO;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ip黑白名单缓存
 *
 * @author caokele
 */
@Component
@Slf4j
public class IpBlackWhiteCache {
    /**
     * 默认分页数量值
     */
    private static final long DEFAULT_PAGE_SIZE = 50L;

    private ExecutorService executorService;

    @Autowired
    private IpBlackWhiteMapper ipBlackWhiteMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    //@PostConstruct
    void init() {
        executorService = Executors.newFixedThreadPool(1);
        executorService.submit(() -> {
            try {
                initializeToRedis();
            } catch (Exception e) {
                log.error("Write ipBlackWhiteList to cache failed!", e);
            }
        });
    }

    /**
     * 将数据库数据写到Redis
     */
    private void initializeToRedis() {
        Integer total = ipBlackWhiteMapper.selectCount(null);
        double maxPages = Math.ceil(total * 1.0 / DEFAULT_PAGE_SIZE);
        for (int i = 1; i <= maxPages; i++) {
            Page<IpBlackWhitePO> page = new Page<>(i, DEFAULT_PAGE_SIZE, total);
            Page<IpBlackWhitePO> ipBlackWhitePOPage = ipBlackWhiteMapper.selectPage(page, new LambdaQueryWrapper<IpBlackWhitePO>().orderByAsc(IpBlackWhitePO::getId));
            List<IpBlackWhitePO> ipBlackWhitePOs = ipBlackWhitePOPage.getRecords();
            // 企业ID与IP列表的关系
            Map<Long, Set<String>> companyIpsMap = new HashMap<>(ipBlackWhitePOs.size());
            // 企业ID与批控模式的关系
            Map<String, String> companyControlTypeMap = new HashMap<>(ipBlackWhitePOs.size());
            for (IpBlackWhitePO ipBlackWhitePO : ipBlackWhitePOs) {
                Long companyId = ipBlackWhitePO.getCompanyId();
                if (!companyIpsMap.containsKey(companyId)) {
                    companyIpsMap.put(companyId, new HashSet<>(ipBlackWhitePOs.size()));
                }
                Set<String> ips = companyIpsMap.get(companyId);
                ips.add(ipBlackWhitePO.getIp());
                companyControlTypeMap.put(companyId.toString(), ipBlackWhitePO.getControlType().toString());
            }
            companyIpsMap.forEach((companyId, ips) -> {
                String key = String.format(Constants.AUTH_IWB_CID, companyId);
                stringRedisTemplate.opsForSet().add(key, ips.toArray(new String[0]));
            });
            stringRedisTemplate.opsForHash().putAll(Constants.AUTH_IWB_CID_CONTROL_TYPE, companyControlTypeMap);
        }
    }

    /**
     * 新增黑白名单
     */
    public void add(IpBlackWhiteBO ipBlackWhiteBO, Set<String> ips) {
        String tenantId = RpcContext.getContext().getTenantId();
        String key = String.format(Constants.AUTH_IWB_CID, tenantId, ipBlackWhiteBO.getCompanyId());
        String tenantType = String.format(Constants.AUTH_IWB_CID_CONTROL_TYPE, tenantId);
        stringRedisTemplate.opsForSet().add(key, ips.toArray(new String[0]));
        stringRedisTemplate.opsForHash().put(tenantType, ipBlackWhiteBO.getCompanyId().toString(), ipBlackWhiteBO.getControlType().toString());
    }

    /**
     * 删除黑白名单
     */
    public void delete(List<Long> ids) {
        String tenantId = RpcContext.getContext().getTenantId();
        List<IpBlackWhitePO> ipBlackWhitePOs = ipBlackWhiteMapper.selectBatchIds(ids);
        // 企业ID与IP列表的关系
        Map<Long, Set<String>> companyIpsMap = new HashMap<>(ipBlackWhitePOs.size());
        for (IpBlackWhitePO ipBlackWhitePO : ipBlackWhitePOs) {
            Long companyId = ipBlackWhitePO.getCompanyId();
            if (!companyIpsMap.containsKey(companyId)) {
                companyIpsMap.put(companyId, new HashSet<>(ipBlackWhitePOs.size()));
            }
            Set<String> ips = companyIpsMap.get(companyId);
            ips.add(ipBlackWhitePO.getIp());
        }
        try {
            companyIpsMap.forEach((companyId, ips) -> {
                String key = String.format(Constants.AUTH_IWB_CID, tenantId, companyId);
                String tenantType = String.format(Constants.AUTH_IWB_CID_CONTROL_TYPE, tenantId);
                stringRedisTemplate.opsForSet().remove(key, ips.toArray(new String[0]));
                if (Optional.ofNullable(stringRedisTemplate.opsForSet().size(key)).map(count -> count == 0).orElse(true)) {
                    stringRedisTemplate.delete(key);
                    stringRedisTemplate.opsForHash().delete(tenantType, companyId.toString());
                }
            });
        } catch (Exception e) {
            log.error("Delete ipBlackWhiteList From Redis failed!", e);
            throw e;
        }
    }

    /**
     * 根据企业id获取管控模式
     *
     * @param companyId 企业ID
     */
    public Integer getControlType(Long companyId) {
        String tenantId = RpcContext.getContext().getTenantId();
        String tenantType = String.format(Constants.AUTH_IWB_CID_CONTROL_TYPE, tenantId);
        String controlTypeStr = (String) stringRedisTemplate.opsForHash().get(tenantType, companyId.toString());
        return Optional.ofNullable(controlTypeStr)
                .map(Integer::valueOf)
                .orElse(null);
    }

    /**
     * 根据企业id获取黑白名单列表
     *
     * @param companyId 企业ID
     */
    public Set<String> getIpSet(Long companyId) {
        String tenantId = RpcContext.getContext().getTenantId();
        String key = String.format(Constants.AUTH_IWB_CID, tenantId, companyId);
        return stringRedisTemplate.opsForSet().members(key);
    }
}
