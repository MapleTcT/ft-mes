package com.supcon.supfusion.configuration.services.openapi.freemark;

import com.supcon.supfusion.base.entities.Staff;
import com.supcon.supfusion.base.services.*;
import com.supcon.supfusion.framework.cloud.common.context.UserContext;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
public class CurrentMethod implements TemplateMethodModelEx {

	@Autowired
	private CompanyService companyService;
	@Autowired
	private StaffService staffService;
	@Autowired
	private PositionService positionService;
	@Autowired
	private DepartmentService departmentService;
	@Autowired
	private UserService userService;

	@SuppressWarnings("rawtypes")
	@Override
	public Object exec(List arguments) throws TemplateModelException {
		String type = "";
		if (null != arguments && !arguments.isEmpty() && null != arguments.get(0)) {
			type= arguments.get(0).toString();
		}
		switch (type) {
			case "staffId": return UserContext.getUserContext().getStaffId() !=null ? UserContext.getUserContext().getStaffId() : "";
			case "positionId": return UserContext.getUserContext().getPositionId() !=null ? UserContext.getUserContext().getPositionId() : "";
			case "companyId": return UserContext.getUserContext().getCompanyId() !=null ? UserContext.getUserContext().getCompanyId() : "";
			case "departmentId": return UserContext.getUserContext().getDepartmentId() !=null ? UserContext.getUserContext().getDepartmentId() : "";
			case "staffName": return UserContext.getUserContext().getStaffName() !=null ? UserContext.getUserContext().getStaffName() : "";
			case "userName": return UserContext.getUserContext().getUserName() !=null ? UserContext.getUserContext().getUserName() : "";
			case "positionName": return UserContext.getUserContext().getPositionName() !=null ? UserContext.getUserContext().getPositionName() : "";
			case "departmentName": return UserContext.getUserContext().getDepartmentName() !=null ? UserContext.getUserContext().getDepartmentName() : "";
			case "companyName": return UserContext.getUserContext().getCompanyName() !=null ? UserContext.getUserContext().getCompanyName() : "";
			case "companyCode": return UserContext.getUserContext().getCompanyCode() !=null ? UserContext.getUserContext().getCompanyCode() : "";
			case "language":
				String language = LocaleContextHolder.getLocale().toString();
				if (!"zh_CN".equals(language) && !"zh_HK".equals(language)  && !"en_US".equals(language)) {
					return Locale.SIMPLIFIED_CHINESE.toString();
				}
				return language;
			case "lang":
				String lang = LocaleContextHolder.getLocale().toString();
				if (!"zh_CN".equals(lang) && !"zh_HK".equals(lang)  && !"en_US".equals(lang)) {
					return Locale.SIMPLIFIED_CHINESE.toString().toLowerCase();
				}
				return lang.toLowerCase();
			case "staff":
				Long staffId = UserContext.getUserContext().getStaffId();
				if (staffId != null) {
					return staffService.load(staffId);
				}
			case "position":
				Long positionId = UserContext.getUserContext().getPositionId();
				if (positionId != null) {
					return positionService.load(positionId);
				}
			case "company":
				Long companyId = UserContext.getUserContext().getCompanyId();
				if (companyId != null) {
					return companyService.get(companyId);
				}
		}
		return "";
	}

	public static void main(String[] args) {
		System.out.println(Locale.SIMPLIFIED_CHINESE.toString());
	}
}
