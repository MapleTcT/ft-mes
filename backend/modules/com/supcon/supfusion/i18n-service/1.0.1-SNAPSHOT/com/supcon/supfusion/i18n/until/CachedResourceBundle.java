package com.supcon.supfusion.i18n.until;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.supcon.supfusion.i18n.common.dto.KeyValuePairCollection;
import com.supcon.supfusion.i18n.common.until.CacheUtils;
import com.supcon.supfusion.i18n.common.until.Constants;
import com.supcon.supfusion.i18n.dao.I18nIndexDao;
import com.supcon.supfusion.i18n.dao.I18nResourceDao;
import com.supcon.supfusion.i18n.dao.po.I18nIndexPO;
import com.supcon.supfusion.i18n.dao.po.I18nResourcePO;
import com.supcon.supfusion.module.registry.ModuleEnum;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StopWatch;


/**
 * 自定义资源集合
 *
 * @author
 * @version 1.0.0
 * @date 2020-06-04 14:24
 * @copyright
 */
@Component
@Slf4j
public class CachedResourceBundle {

	// 默认1年左右时间过期
	private static final int DEFAULT_TIMEOUT_DAY = 2 << 8;
	
	@Autowired
	@Qualifier("i18nRedisTemplate")
	private RedisTemplate<String, Object> redisTemplate;
	@Autowired
	private I18nResourceDao i18nResourceDao;
	@Autowired
    private I18nIndexDao i18nIndexDao;

	/**
	 * 查询租户模块下所有语言的国际化资源
	 * <ul>
	 * 	<li>优先查询缓存, 缓存没有再查询数据库</li>
	 *  <li>国际化资源有可能存在租户和系统的数据, 需要将两者数据进行合并取并集, key冲突时, 租户数据覆盖系统默认</li>
	 * </ul>
	 * @param tenantId
	 * @param moduleId 模块编号(等同于moduleCode)
	 * @param languages
	 * @return Map<language, KeyValuePairCollection>
	 */
	public Map<String, KeyValuePairCollection> getModuleResourceForMultipleTenant(String tenantId, String moduleId, Set<String> languages) {
		Map<String, KeyValuePairCollection> i18nResources = new HashMap<>();
		for (String language : languages) {
			// 先查询默认租户的国际化资源, 如果租户有数据则需要以租户数据为准, 需要做一次合并
			KeyValuePairCollection resource = getSingleResourceForMultipleTenant(tenantId, moduleId, language);
			if (resource != null) {
				i18nResources.put(language, resource);
			}
		}
		return i18nResources;
	}
	
	/**
	 * 通过租户ID, 模块编号, 语言三个条件查询国际化资源
	 * <p>
	 * 	优先从缓存中读取, 如果缓存不存在则从数据库中查询, 将查询结果放入缓存并返回, 如果数据库也没有数据则返回null
	 *  在多租户环境下, 如果租户数据不存在需要再查询系统数据
	 * </p>
	 * @param tenantId
	 * @param moduleId
	 * @param language
	 * @return
	 */
	public KeyValuePairCollection getSingleResourceForMultipleTenant(String tenantId, String moduleId, String language) {
		KeyValuePairCollection systemResource = getResourceForSingleTenant(Constants.DEFAULT_TENANT, moduleId, language);
		if (Constants.DEFAULT_TENANT.equals(tenantId)) {
			return systemResource;
		}
		KeyValuePairCollection tenantResource = getResourceForSingleTenant(tenantId, moduleId, language);
		if (tenantResource != null) {
			if (systemResource == null) {
				return tenantResource;
			}
			systemResource.getKvs().putAll(tenantResource.getKvs());
		}
		return systemResource;
	}
	
	/**
	 * 获取单个租户数据, 只在当前租户查找
	 * @param tenantId
	 * @param moduleId
	 * @param language
	 * @return
	 */
	public KeyValuePairCollection getResourceForSingleTenant(String tenantId, String moduleId, String language) {
		String cacheKey = CacheUtils.getResouceCacheKey(tenantId, moduleId, language);
		KeyValuePairCollection kvs = null;
		try { // 防止Redis服务异常导致系统不可用
			kvs = (KeyValuePairCollection)redisTemplate.opsForValue().get(cacheKey);
		} catch (Exception e) {
			log.error("redis获取缓存异常", e);
			log.info("============ 接下来将继续从数据库获取 ============");
		}
		// 缓存不存在去查询数据库
		if (kvs == null) {
			LambdaQueryWrapper<I18nResourcePO> queryWrapper = new QueryWrapper<I18nResourcePO>().lambda()
					.eq(I18nResourcePO::getTenantId, tenantId)
					.eq(I18nResourcePO::getModuleCode, moduleId)
					.eq(I18nResourcePO::getLanguCode, language);
			StopWatch stopWatch = new StopWatch();
			List<I18nResourcePO> resourcesInDB = null;
			try {
				stopWatch.start();
				resourcesInDB = i18nResourceDao.selectList(queryWrapper);
				log.debug("<-------------remoteMessage 数据库查询时间：{} ----------------->", stopWatch.getTotalTimeMillis());
			} finally {
				stopWatch.stop();
			}
			if (resourcesInDB != null && !resourcesInDB.isEmpty()) {
				Map<String, String> i18nResourceMap = resourcesInDB.stream().collect(
						Collectors.toMap(I18nResourcePO::getI18nKey, p -> p.getI18nValue() == null ? "" : p.getI18nValue(), (key1, key2) -> {
									return key1;
								}));
				kvs = new KeyValuePairCollection(i18nResourceMap);
				// 再次刷到缓存
				try {
					flushResourceCacheByKeyAndValue(cacheKey, kvs);
				} catch (Exception ignor) {
					
				}
			}
		}
		return kvs;
	}
	
    /**
     * key的粒度到语言(tenant_{tenantId}_{moduleId}_{languageCode}), 即每个租户模块语言作为key值, 默认1年时间过期
     * @param tenantId 
     * @param moduleId 模块编号
     * @param languageCode 
     */
    public void flushResourceCacheByUpdate(String tenantId, String moduleId, String languageCode) {
		LambdaQueryWrapper<I18nResourcePO> queryWrapper = new QueryWrapper<I18nResourcePO>().lambda()
				.eq(I18nResourcePO::getTenantId, tenantId)
				.eq(I18nResourcePO::getModuleCode, moduleId)
				.eq(I18nResourcePO::getLanguCode, languageCode);
		List<I18nResourcePO> resources = i18nResourceDao.selectList(queryWrapper);
		Map<String, String> kvs = new HashMap<>();
		for (I18nResourcePO resource : resources) {
			kvs.put(resource.getI18nKey(), resource.getI18nValue());
		}
    	String cacheKey = CacheUtils.getResouceCacheKey(tenantId, moduleId, languageCode);
		flushResourceCacheByKeyAndValue(cacheKey, new KeyValuePairCollection(kvs));
    }
    
    /**
     * 删除多个国际化key时更新缓存
     * @param i18nKeys
     * @param languages
     * @param allModuleIds
     * @param tenantId
     */
    public void flushResourceCacheByDelete(List<String> i18nKeys, Set<String> languages, List<String> allModuleIds, String tenantId) {
		for (String i18nKey : i18nKeys) {
			String moduleId = i18nKey.split(Constants.STR_POINT1)[0];
			if (!allModuleIds.contains(moduleId)) {
				moduleId = ModuleEnum.DEFAULT.getModuleId();
			}
			flushResourceCacheByDelete(moduleId, languages, tenantId);
		}
	}
    
    /**
     * 删除指定模块的缓存
     * @param moduleId
     * @param languages
     * @param tenantId
     */
    public void flushResourceCacheByDelete(String moduleId, Set<String> languages, String tenantId) {
		for (String language : languages) {
			final String cacheKey = CacheUtils.getResouceCacheKey(tenantId, moduleId, language);
			redisTemplate.delete(cacheKey);
		}
    }
    
    private <T> void flushResourceCacheByKeyAndValue(final String i18nCacheKey, T value) {
    	int randomFactor = new Random().nextInt(100); // 生成100以内的随机数
    	int timeoutDay = DEFAULT_TIMEOUT_DAY + randomFactor; // 避免所有缓存在同一时间失效
    	redisTemplate.opsForValue().set(i18nCacheKey, value, timeoutDay, TimeUnit.DAYS);
    }
    
    /**
     * 查询模块对应的索引
     * @param moduleId
     * @param tenantId
     * @return
     */
    public String getModuleIndex(String moduleId, String tenantId) {
    	final String indexCacheKey = CacheUtils.getIndexCacheKey(tenantId, moduleId);
    	String moduleIndex = null;
    	try { // 防止Redis服务异常导致系统不可用
    		moduleIndex = (String)redisTemplate.opsForValue().get(indexCacheKey);
		} catch (Exception e) {
			log.error("redis获取缓存索引异常", e);
			log.info("============= 接下来将继续从数据库获取缓存索引 =============");
		}
    	if (moduleIndex == null) {
    		LambdaQueryWrapper<I18nIndexPO> queryWrapper = new QueryWrapper<I18nIndexPO>().lambda()
    				.eq(I18nIndexPO::getModuleCode, moduleId)
    				.eq(I18nIndexPO::getTenantId, tenantId);
    		I18nIndexPO indexPO = i18nIndexDao.selectOne(queryWrapper);
    		if (indexPO != null) {
    			flushModuleIndexCache(moduleId, tenantId, indexPO.getModuleIndexCode());
    			moduleIndex = indexPO.getModuleIndexCode();
    		}
    	}
    	return moduleIndex;
    }

    /**
     * key的粒度到模块, 即每个租户模块作为key值, 默认半年时间过期
     * @param moduleIndex 索引-用于标记租户模块数据是否被修改
     */
    public void flushModuleIndexCache(String moduleId, String tenantId, String moduleIndex) {
    	final String indexCacheKey = CacheUtils.getIndexCacheKey(tenantId, moduleId);
    	flushResourceCacheByKeyAndValue(indexCacheKey, moduleIndex);
    }
    
}
