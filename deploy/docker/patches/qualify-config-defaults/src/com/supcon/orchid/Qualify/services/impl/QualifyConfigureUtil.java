package com.supcon.orchid.Qualify.services.impl;

import com.supcon.supfusion.systemconfig.api.tenantconfig.annotation.ClassSystemConfigAnno;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@ClassSystemConfigAnno
public class QualifyConfigureUtil {
    @Value("${Qualify/Qualify.setDefaultLevel:false}")
    private Boolean setDefaultLevel;

    public Boolean getSetDefaultLevel() {
        return setDefaultLevel != null ? setDefaultLevel : Boolean.FALSE;
    }

    public void setSetDefaultLevel(Boolean setDefaultLevel) {
        this.setDefaultLevel = setDefaultLevel;
    }
}
