package com.supcon.supfusion.configuration.workflow.script;

import groovy.lang.*;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ScriptExecutor {

	private static final Logger logger = LoggerFactory.getLogger(ScriptExecutor.class);
	
	private volatile static ScriptExecutor instance;
//	private static final Object monitor=new Object();
	
	public static ScriptExecutor getInstance() {
		if(null == instance) {
			synchronized (ScriptExecutor.class) {
				if(null == instance) {
					instance = new ScriptExecutor();
				}
					
			}
		}
		return instance;
	}

	private ScriptExecutor() {
		config.setDebug(false);
		final ClassLoader parentLoader = ScriptExecutor.class.getClassLoader();
		loader = AccessController.doPrivileged(new PrivilegedAction<GroovyClassLoader>() {
			public GroovyClassLoader run() {
				return new GroovyClassLoader(parentLoader, config);
			}
		});
	}
	

	private static AtomicInteger ai = new AtomicInteger();

	public static Object eval(String script, Map<String, Object>... variables) {
		Map<String, Object> vars = new HashMap<String, Object>();
		if (null != variables) {
			for (Map<String, Object> v : variables) {
				vars.putAll(v);
			}
		}
		// Binding binding = new Binding(vars);
		return eval(script, vars);
	}

	private CompilerConfiguration config = CompilerConfiguration.DEFAULT;
	private GroovyClassLoader loader;
	@SuppressWarnings("rawtypes")
	private ConcurrentMap<String, Class> map = new ConcurrentHashMap<String, Class>();
	private ConcurrentMap<Long, Set<String>> bundleMap =  new ConcurrentHashMap<>();
	
	public ConcurrentMap<String, Class> getMap() {
		return map;
	}
	
	public ConcurrentMap<Long, Set<String>> getBundleMap() {
		return bundleMap;
	}
	
	public GroovyClassLoader getLoader() {
		return loader;
	}
	
	public CompilerConfiguration getConfig() {
		return config;
	}

	@SuppressWarnings("rawtypes")
	public static void preCompile(final String script) {
		if (logger.isDebugEnabled())
			logger.debug("预编译脚本：\n\n***************************************************************************************************\n" + script
					+ "\n***************************************************************************************************\n");
		Class clz = getInstance().getMap().get(script);
		if (null == clz) {
			GroovyCodeSource gcs = AccessController.doPrivileged(new PrivilegedAction<GroovyCodeSource>() {
				public GroovyCodeSource run() {
					return new GroovyCodeSource(script, "Script" + ai.getAndIncrement(), GroovyShell.DEFAULT_CODE_BASE);
				}
			});
			clz = getInstance().getLoader().parseClass(gcs, false);
			getInstance().getMap().put(script, clz);
		}
	}

}