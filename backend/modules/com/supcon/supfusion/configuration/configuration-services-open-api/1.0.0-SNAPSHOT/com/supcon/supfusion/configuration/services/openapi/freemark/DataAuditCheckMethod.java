package com.supcon.supfusion.configuration.services.openapi.freemark;

import com.supcon.supfusion.configuration.services.utils.DbUtils;
import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModelException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataAuditCheckMethod implements TemplateMethodModel{

	@Override
	public Object exec(List arguments) throws TemplateModelException {
		String code = arguments.get(0).toString();
		int rs = DbUtils.getJdbcTemplate().queryForObject("SELECT IS_AUDIT FROM RUNTIME_VIEW WHERE CODE = ?", new String[]{code}, int.class);
		return rs;
	}

}
