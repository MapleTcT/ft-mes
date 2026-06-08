package com.supcon.supfusion.configuration.workflow.script;

import com.supcon.supfusion.configuration.services.exceptions.EcException;
import com.supcon.supfusion.base.services.impl.ProcessServiceImpl;
import com.supcon.supfusion.configuration.workflow.activity.NoticeTransit;
import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;


public class TextExecutor {
	private transient static final Logger LOGGER = LoggerFactory.getLogger(TextExecutor.class);
//	 private static final TemplateEngine TEMPLATE_ENGINE = new SimpleTemplateEngine();
	private static final Configuration cfg = new Configuration();
	private static final StringTemplateLoader TEMPLATE_LOADER = new StringTemplateLoader();
	private static final Map<String, Template> templates = new HashMap<String, Template>(4);// 只缓存默认模板
	
	public static void updateTemplates(){
		try {
			templates.put(NoticeTransit.DEFAULT_EMAIL_TITLE_KEY, new Template(null, new StringReader(ProcessServiceImpl.DEFAULT_EMAIL_TITLE), cfg));
			templates.put(NoticeTransit.DEFAULT_EMAIL_CONTENT_KEY, new Template(null, new StringReader(ProcessServiceImpl.DEFAULT_EMAIL_CONTENT), cfg));
			templates.put(NoticeTransit.DEFAULT_JABBER_KEY, new Template(null, new StringReader(ProcessServiceImpl.DEFAULT_JABBER_CONTENT), cfg));
			templates.put(NoticeTransit.DEFAULT_SMS_KEY, new Template(null, new StringReader(ProcessServiceImpl.DEFAULT_SMS_CONTENT), cfg));
			templates.put(NoticeTransit.DEFAULT_APP_KEY, new Template(null, new StringReader(ProcessServiceImpl.DEFAULT_APP_CONTENT), cfg));
		} catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
		}
	}
	@PostConstruct
	static void init()  {
		try {
			cfg.setObjectWrapper(new DefaultObjectWrapper());
			cfg.setDefaultEncoding("UTF-8");
			cfg.setOutputEncoding("UTF-8");
			cfg.setTemplateLoader(TEMPLATE_LOADER);
			cfg.setNumberFormat("#");
			templates.put(NoticeTransit.DEFAULT_EMAIL_TITLE_KEY, new Template(null, new StringReader(NoticeTransit.DEFAULT_EMAIL_TITLE), cfg));
			templates.put(NoticeTransit.DEFAULT_EMAIL_CONTENT_KEY, new Template(null, new StringReader(NoticeTransit.DEFAULT_EMAIL_CONTENT), cfg));
			templates.put(NoticeTransit.DEFAULT_JABBER_KEY, new Template(null, new StringReader(NoticeTransit.DEFAULT_JABBER_CONTENT), cfg));
			templates.put(NoticeTransit.DEFAULT_SMS_KEY, new Template(null, new StringReader(NoticeTransit.DEFAULT_SMS_CONTENT), cfg));
			templates.put(NoticeTransit.DEFAULT_APP_KEY, new Template(null, new StringReader(NoticeTransit.DEFAULT_APP_CONTENT), cfg));
		} catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
		}
	}

	public static String execute(String name, String text, Map<String, Object> model) {
		try {
			Template template = null;
			if (null != name) {
				template = templates.get(name);
			}
			if (null == template) {
				template = new Template(null, new StringReader(text), cfg);
			}
			StringWriter writer = new StringWriter();
			template.process(model, writer);
			return writer.toString();
		} catch (Exception e) {
			throw new EcException("can not execute the script.", e);
		}
	}

	public static String execute(String text, Map<String, Object> model) {
		return execute(null, text, model);
	}
	//
	// public static class StringTemplateLoader implements TemplateLoader {
	//
	// @Override
	// public Object findTemplateSource(String name) throws IOException {
	// return name;
	// }
	//
	// @Override
	// public long getLastModified(Object templateSource) {
	// return 0;
	// }
	//
	// @Override
	// public Reader getReader(Object templateSource, String encoding) throws IOException {
	// return new StringReader((String) templateSource);
	// }
	//
	// @Override
	// public void closeTemplateSource(Object templateSource) throws IOException {
	// }
	//
	// }
}