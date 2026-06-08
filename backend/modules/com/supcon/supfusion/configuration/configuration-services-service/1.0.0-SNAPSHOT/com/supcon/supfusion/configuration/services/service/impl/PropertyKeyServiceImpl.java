package com.supcon.supfusion.configuration.services.service.impl;

import com.supcon.supfusion.configuration.services.service.PropertyKeyService;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@ServiceApiService("propertyKeyService")
public class PropertyKeyServiceImpl implements PropertyKeyService,ResourceLoaderAware,InitializingBean {

	private Set<String> javaKeyWords;
	private Set<String> propertyKeyWords;
	private Set<String> dbKeyWords;
	private Set<String> bapKeyWords;
	private ResourceLoader resourceLoader;
	
	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}
	@Override
	public Boolean checkJavaKey(String key) {
		
		for (String item : javaKeyWords) {
			if (key.equalsIgnoreCase(item)) {
				return false;
			}
		}
		
		return true;
	}

	@Override
	public Boolean checkPropertyKey(String key) {
		
		for (String item : propertyKeyWords) {
			if (key.equalsIgnoreCase(item)) {
				return false;
			}
		}
		
		return true;
	}

	@Override
	public Boolean checkDBKey(String colName) {
		for (String item : dbKeyWords) {
			if (colName.equalsIgnoreCase(item)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		javaKeyWords = new HashSet<String>();
		propertyKeyWords = new HashSet<String>();
		dbKeyWords = new HashSet<String>();
		bapKeyWords = new HashSet<String>();
		loadKeyWords("java", javaKeyWords);
		loadKeyWords("propertyKey", propertyKeyWords);
		loadKeyWords("db", dbKeyWords);
		loadKeyWords("bapKey", bapKeyWords);
	}
	private void loadKeyWords(String type, Set<String> set) throws IOException {
		Resource resource = this.resourceLoader.getResource("classpath:META-INF/keyFile/keyfile-" + type + ".txt");
		if (null != resource) {
			InputStream is = null;
			try {
				is = resource.getInputStream();
				String content = IOUtils.toString(is, "utf-8");
				if (!StringUtils.isBlank(content)) {
					for (String s : content.split(",")) {
						if ("db".equals(type))
							set.add(s.trim().toUpperCase());
						else
							set.add(s.trim());
					}

				}
			} finally {
				IOUtils.closeQuietly(is);
			}
		}
	}
	
	@Override
	public Boolean checkBapKey(String key) {
		for (String item : bapKeyWords) {
			if (key.equalsIgnoreCase(item)) {
				return false;
			}
		}
		return true;
	}
	
	
}
