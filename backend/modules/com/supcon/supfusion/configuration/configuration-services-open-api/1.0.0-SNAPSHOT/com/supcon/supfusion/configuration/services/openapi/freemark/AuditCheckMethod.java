package com.supcon.supfusion.configuration.services.openapi.freemark;

import com.supcon.supfusion.configuration.services.utils.DbUtils;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

@Component
public class AuditCheckMethod implements TemplateMethodModelEx {

	@Override
	public Object exec(List arguments) throws TemplateModelException {
		String code = arguments.get(0).toString();
		Map<String, Object> rs = new HashMap<String, Object>();
		List<Map<String, Object>> list = DbUtils.getJdbcTemplate().queryForList("select ENABLE_OPERATION_AUDIT FROM RUNTIME_MODEL WHERE CODE = ?", code);
		if (null != list && list.size() > 0) {
			rs = list.get(0);
			if (rs != null && !rs.isEmpty()) {
				Iterator<Entry<String, Object>> it = rs.entrySet().iterator();
				Entry<String, Object> item = it.next();
				return item.getValue().toString();
			}
		}
		return "";
	}

}
