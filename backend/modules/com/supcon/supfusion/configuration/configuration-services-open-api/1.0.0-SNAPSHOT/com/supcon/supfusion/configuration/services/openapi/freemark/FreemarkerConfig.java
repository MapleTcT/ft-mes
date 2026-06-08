package com.supcon.supfusion.configuration.services.openapi.freemark;

import freemarker.template.TemplateModelException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.ArrayList;

/**
 * @Description: TODO
 * @Version V1.0
 * @Auther: huning
 * @Date: 2020/7/1
 */
@Configuration
public class FreemarkerConfig {


    @Autowired
    private GetUserThemePropertyMethod getUserThemePropertyMethod;
    @Autowired
    private I18nMethod i18nMethod;
    @Autowired
    private EnvMethod envMethod;
    @Autowired
    private GetConfigPropertyMethod getConfigPropertyMethod;
    @Autowired
    private GetUserFontPropertyMethod getUserFontPropertyMethod;
    @Autowired
    private HtmlI18nMethod htmlI18nMethod;
    @Autowired
    private CheckOperatePowerMethod checkOperatePowerMethod;
    @Autowired
    private GetEnableFieldsPermissionConfigMethod getEnableFieldsPermissionConfigMethod;
    @Autowired
    private OperatePowerMethod operatePowerMethod;
    @Autowired
    private JsI18nMethod jsI18nMethod;
    @Autowired
    private HtmlUrlMethod htmlUrlMethod;
    @Autowired
    private SerializerMethod serializerMethod;
    @Autowired
    private DateTimeMethod dateTimeMethod;
    @Autowired
    private SystemCodeValueMethod systemCodeValueMethod;
    @Autowired
    private SystemCodeMethod systemCodeMethod;
    @Autowired
    private SystemCodeListMethod systemCodeListMethod;
    @Autowired
    private GetDataClassificMethod getDataClassificMethod;
    @Autowired
    private GetFlowStartUrlMethod getFlowStartUrlMethod;
    @Autowired
    private InfosetMethod infosetMethod;
    @Autowired
    private ColumnizeMethod columnizeMethod;
    @Autowired
    private TableizeMethod tableizeMethod;
    @Autowired
    private FormatDateMethod formatDateMethod;
    @Autowired
    private FileIconMethod fileIconMethod;
    @Autowired
    private GenerateNormalOperatePowerCodeMethod generateNormalOperatePowerCodeMethod;
    @Autowired
    private CheckUserOperatePowerMethod checkUserOperatePowerMethod;
    @Autowired
    private CheckFieldPermissionMethod checkFieldPermissionMethod;
    @Autowired
    private GetPermissionFieldsMapMethod getPermissionFieldsMapMethod;
    @Autowired
    private FormatDateTimeMethod formatDateTimeMethod;
    @Autowired
    private EntityConfigCodeInitMethod entityConfigCodeInitMethod;
    @Autowired
    private GetShowCustomPropsMethod getShowCustomPropsMethod;
    @Autowired
    private GetPropDisplayNameMethod getPropDisplayNameMethod;
    @Autowired
    private CustomPropertyMethod customPropertyMethod;
    @Autowired
    private GetTabViewMethod getTabViewMethod;
    @Autowired
    private AuditCheckMethod auditCheckMethod;
    @Autowired
    private DataAuditCheckMethod dataAuditCheckMethod;
    @Autowired
    private GetShowCustomSecretMethod getShowCustomSecretMethod;
    @Autowired
    private IsContainsWorkflow isContainsWorkflow;
    @Autowired
    private LanguageMethod languageMethod;
    @Autowired
    private InternationalResourceTemplateModel internationalResourceTemplateModel;
    @Autowired
    private QuickQueryMethod quickQueryMethod;
    @Autowired
    private FindLastQueryFieldMethod findLastQueryFieldMethod;
    @Autowired
    private ViewMethod viewMethod;
    @Autowired
    private CurrentMethod currentMethod;

    @PostConstruct
    public void setVariableConfiguration() throws TemplateModelException {

        configuration.setSharedVariable("InternationalResource", internationalResourceTemplateModel);
        configuration.setDateTimeFormat("yyyy-MM-dd HH:mm:ss");
        configuration.setDateFormat("yyyy-MM-dd");
        configuration.setSharedVariable("languageMethod", languageMethod);
        configuration.setSharedVariable("getJsText", jsI18nMethod);
        configuration.setSharedVariable("getHtmlText", htmlI18nMethod);
        configuration.setSharedVariable("getHtmlUrl", htmlUrlMethod);
        configuration.setSharedVariable("getText", i18nMethod);
        configuration.setSharedVariable("serialize", serializerMethod);
        configuration.setSharedVariable("datetime", dateTimeMethod);
        configuration.setSharedVariable("getSystemCodeValue", systemCodeValueMethod);
        configuration.setSharedVariable("getSystemCode", systemCodeMethod);
        configuration.setSharedVariable("getSystemCodeList", systemCodeListMethod);
        configuration.setSharedVariable("getDataClassific", getDataClassificMethod);
        configuration.setSharedVariable("checkOperatePower", operatePowerMethod);
        configuration.setSharedVariable("getFlowStartUrl", getFlowStartUrlMethod);
        configuration.setSharedVariable("getInfoset", infosetMethod);
        configuration.setSharedVariable("columnize", columnizeMethod);
        configuration.setSharedVariable("tableize", tableizeMethod);
        configuration.setSharedVariable("formatDate", formatDateMethod);
        configuration.setSharedVariable("fileIcon",fileIconMethod);
        configuration.setSharedVariable("getPowerCode", generateNormalOperatePowerCodeMethod);
        configuration.setSharedVariable("checkUserPermisition", checkOperatePowerMethod);
        configuration.setSharedVariable("checkUserOperatePower", checkUserOperatePowerMethod);
        configuration.setSharedVariable("checkFieldPermission", checkFieldPermissionMethod);
        configuration.setSharedVariable("getPermissionFieldsMap", getPermissionFieldsMapMethod);
        configuration.setSharedVariable("getEnableFieldsPermissionConfig", getEnableFieldsPermissionConfigMethod);
        configuration.setSharedVariable("getDefaultDateTime", formatDateTimeMethod);
        configuration.setSharedVariable("getConfigProperty", getConfigPropertyMethod);
        configuration.setSharedVariable("getUserThemeProperty", getUserThemePropertyMethod);
        configuration.setSharedVariable("getUserFontProperty", getUserFontPropertyMethod);
        configuration.setSharedVariable("ecCodeInit", entityConfigCodeInitMethod);
        configuration.setSharedVariable("getTimestamp", entityConfigCodeInitMethod);
        configuration.setSharedVariable("isDev", envMethod);
        configuration.setSharedVariable("getShowCustomProps", getShowCustomPropsMethod);
        configuration.setSharedVariable("getPropDisplayName", getPropDisplayNameMethod);
        configuration.setSharedVariable("findCPByBusinessValue", customPropertyMethod);
        configuration.setSharedVariable("getTabViews", getTabViewMethod);
        configuration.setSharedVariable("auditCheck", auditCheckMethod);
        configuration.setSharedVariable("dataAuditCheck", dataAuditCheckMethod);
        configuration.setSharedVariable("getShowCustomSecret", getShowCustomSecretMethod);
        configuration.setSharedVariable("getViewByCode", viewMethod);
        configuration.setSharedVariable("isContainsWorkflow", isContainsWorkflow);
        configuration.setSharedVariable("quickQuery", quickQueryMethod);
        configuration.setSharedVariable("findLastQueryFieldMethod", findLastQueryFieldMethod);
        configuration.setSharedVariable("getCurrent", currentMethod);



        configuration.setAutoIncludes(new ArrayList() {
            {
                add("*/cui/cui.ftl");
            }
        });
    }

    @Autowired
    private freemarker.template.Configuration configuration;


}
