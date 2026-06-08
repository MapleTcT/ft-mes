package com.supcon.supfusion.configuration.services.openapi.freemark;

import com.supcon.supfusion.base.entities.SystemCode;
import com.supcon.supfusion.configuration.services.service.SystemCodeService;
import com.supcon.supfusion.configuration.services.i18n.InternationalResource;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
public class SystemCodeValueMethod implements TemplateMethodModelEx {
	@Autowired
	private SystemCodeService systemCodeService;

	private String getLanguage(Locale locale) {
		if (null != locale) {
			return locale.getLanguage() + "_" + locale.getCountry();
		}
		return null;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Object exec(List arguments) throws TemplateModelException {
		String systemCodeID="";
		if (null != arguments && !arguments.isEmpty() && null != arguments.get(0)) {
			//entityCode= arguments.get(0).toString();
			if(arguments.size() > 1){
				systemCodeID= arguments.get(0).toString() + "/" + arguments.get(1).toString();
			}else{
				systemCodeID= arguments.get(0).toString();
			}
		} else {
			return "";
		}
		if(systemCodeID.contains(",")){
			String codes[]=systemCodeID.split(",");
			String values="";
			for(String code:codes){
				if(code != null && !code.isEmpty()) {
					values+=",";
					values+= InternationalResource.get(systemCodeService.getSystemCode(code).getValue());
				}
			}
			if (values != null && !values.isEmpty()) {
				return values.substring(1);
			}
		}else {
			SystemCode sys= systemCodeService.getSystemCode(systemCodeID);
			if(null!=sys){
				return InternationalResource.get(sys.getValue());
			}
		}
		return "";
	}

}