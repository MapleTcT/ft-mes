package com.supcon.orchid.QCS.utils;

import com.supcon.supfusion.systemconfig.api.tenantconfig.annotation.ClassSystemConfigAnno;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@ClassSystemConfigAnno
public class QCSConfigureUtil {
    @Value("${QCS/QCS.reportShowIndexRange:}")
    private String reportShowIndexRange;

    @Value("${QCS/QCS.autoReportStaff:}")
    private String autoReportStaff;

    @Value("${QCS/QCS.autoReport:}")
    private String autoReport;

    @Value("${QCS/QCS.autoUnQlfDeal:}")
    private String autoUnQlfDeal;

    @Value("${QCS/QCS.dataPermission:}")
    private Boolean dataPermission;

    private static String textValue(String currentValue, String propertyName, String envName, String defaultValue) {
        if (currentValue != null && currentValue.trim().length() > 0) {
            return currentValue;
        }
        String propertyValue = System.getProperty(propertyName);
        if (propertyValue != null && propertyValue.trim().length() > 0) {
            return propertyValue;
        }
        String envValue = System.getenv(envName);
        if (envValue != null && envValue.trim().length() > 0) {
            return envValue;
        }
        return defaultValue;
    }

    public String getReportShowIndexRange() {
        return textValue(reportShowIndexRange, "QCS/QCS.reportShowIndexRange", "QCS_REPORT_SHOW_INDEX_RANGE", "qualityStd");
    }

    public void setReportShowIndexRange(String reportShowIndexRange) {
        this.reportShowIndexRange = reportShowIndexRange;
    }

    public String getAutoReportStaff() {
        return textValue(autoReportStaff, "QCS/QCS.autoReportStaff", "QCS_AUTO_REPORT_STAFF", "currentUser");
    }

    public void setAutoReportStaff(String autoReportStaff) {
        this.autoReportStaff = autoReportStaff;
    }

    public String getAutoReport() {
        return textValue(autoReport, "QCS/QCS.autoReport", "QCS_AUTO_REPORT", "manuCheck");
    }

    public void setAutoReport(String autoReport) {
        this.autoReport = autoReport;
    }

    public String getAutoUnQlfDeal() {
        return textValue(autoUnQlfDeal, "QCS/QCS.autoUnQlfDeal", "QCS_AUTO_UNQLF_DEAL", "");
    }

    public void setAutoUnQlfDeal(String autoUnQlfDeal) {
        this.autoUnQlfDeal = autoUnQlfDeal;
    }

    public Boolean getDataPermission() {
        if (dataPermission != null) {
            return dataPermission;
        }
        return Boolean.valueOf(textValue(null, "QCS/QCS.dataPermission", "QCS_DATA_PERMISSION", "false"));
    }

    public void setDataPermission(Boolean dataPermission) {
        this.dataPermission = dataPermission;
    }
}
