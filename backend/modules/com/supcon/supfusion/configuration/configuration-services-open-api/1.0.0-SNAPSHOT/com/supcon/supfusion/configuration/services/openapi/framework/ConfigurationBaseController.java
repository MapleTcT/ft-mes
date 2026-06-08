package com.supcon.supfusion.configuration.services.openapi.framework;

import com.supcon.supfusion.base.entities.Company;
import com.supcon.supfusion.base.entities.Staff;
import com.supcon.supfusion.base.entities.User;
import com.supcon.supfusion.configuration.services.i18n.InternationalResource;
import com.supcon.supfusion.base.enums.CompanyType;
import com.supcon.supfusion.framework.cloud.common.context.UserContext;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;

public class ConfigurationBaseController extends BaseController {

    public Locale getLocale() {
        Locale locale=LocaleContextHolder.getLocale();
        if("zh".equals(locale.getLanguage())){
            locale=new Locale("zh", "CN");
        }
        return locale;
    }

    public String getLang() {
        return getUserLanguage().toLowerCase();

    }

    public String getUserLanguage() {
        return InternationalResource.getDefaultLanguage();
    }

    public Staff getCurrentStaff() {
        Staff staff = new Staff();
        staff.setId(UserContext.getUserContext().getStaffId());
        staff.setCode(UserContext.getUserContext().getStaffCode());
        staff.setName(UserContext.getUserContext().getStaffName());
        return staff;
    }

    public User getCurrentUser() {
        User user = new User();
        user.setId(UserContext.getUserContext().getUserId());
        user.setName(UserContext.getUserContext().getUserName());
        user.setStaff(getCurrentStaff());
        return user;
    }

    public Company getCurrentCompany() {
        Company company = new Company();
        company.setId(UserContext.getUserContext().getCompanyId());
        company.setCode(UserContext.getUserContext().getCompanyCode());
        company.setName(UserContext.getUserContext().getCompanyName());
        return company;
    }

    public Long getCurrentCompanyId() {
        Company company = getCurrentCompany();
        if(null != company){
            return getCurrentCompany().getId();
        }else{
            return null;
        }
    }

}
