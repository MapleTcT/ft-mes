package com.supcon.supfusion.i18n.common.until;

public class CacheUtils {
	
	private CacheUtils() {
		
	}
	
	/**
	 * 国际化资源缓存key的生成规则 tenant_{tenantId}_{moduleId}_{lang}
	 * @param tenantId
	 * @param moduleId
	 * @param languageCode
	 * @return
	 */
	public static String getResouceCacheKey(String tenantId, String moduleId, String languageCode) {
		return String.format("tenant_%s_%s_%s", tenantId, moduleId, languageCode);
	}
	
	/**
	 * index值用于标识该国际化资源是否被修改, 缓存key的生成规则 tenant_{tenantId}_{moduleId}
	 * @param tenantId
	 * @param moduleId
	 * @return
	 */
	public static String getIndexCacheKey(String tenantId, String moduleId) {
		return String.format("tenant_%s_%s", tenantId, moduleId);
	}
}
