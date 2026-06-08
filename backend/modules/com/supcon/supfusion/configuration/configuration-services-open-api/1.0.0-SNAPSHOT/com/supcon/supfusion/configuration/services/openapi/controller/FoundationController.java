package com.supcon.supfusion.configuration.services.openapi.controller;

import com.supcon.supfusion.base.entities.*;
import com.supcon.supfusion.base.services.InternationalService;
import com.supcon.supfusion.configuration.services.entity.SelectionRange;
import com.supcon.supfusion.configuration.services.exceptions.EcException;
import com.supcon.supfusion.configuration.services.utils.SqlParser;
import com.supcon.supfusion.base.services.CompanyService;
import com.supcon.supfusion.base.services.MenuInfoService;
import com.supcon.supfusion.base.services.MenuOperateService;
import com.supcon.supfusion.configuration.services.service.SystemCodeService;
import com.supcon.supfusion.configuration.services.i18n.InternationalResource;
import com.supcon.supfusion.configuration.services.openapi.framework.ConfigurationBaseController;
import com.supcon.supfusion.configuration.services.openapi.vo.MenuInfoVO;
import com.supcon.supfusion.configuration.services.openapi.vo.MenuOperateVO;
import com.supcon.supfusion.configuration.services.openapi.wrapper.MenuInfoWrapper;
import com.supcon.supfusion.configuration.services.openapi.wrapper.MenuOperateWrapper;
import com.supcon.supfusion.configuration.services.utils.StringUtils;
import com.supcon.supfusion.configuration.services.utils.ValidateUtils;
import com.supcon.supfusion.framework.scaffold.hibernate.page.Page;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.xml.ws.Action;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @Description: TODO
 * @Version V1.0
 * @Auther: huning
 * @Date: 2020/7/30
 */
@Slf4j
@Controller
@RequestMapping(value = "ec/foundation")
public class FoundationController extends ConfigurationBaseController {

    private static final MenuInfoWrapper menuInfoWrapper = new MenuInfoWrapper();
    private static final MenuOperateWrapper menuOperateWrapper = new MenuOperateWrapper();

    @ResponseBody
    @RequestMapping("/workbench/getDataGirdCookie")
    public String dataGirdCookie() {
        return null;
    }

    @ResponseBody
    @RequestMapping(value = "/international/common/getContent")
    public String getContent(String key) throws Exception {
        String textValue = null;
        if (null != key) {
            String[] keys = key.split(",");
            for (int i = 0; i < keys.length; i++) {
                String value = InternationalResource.get(keys[i]);
                if (i == 0) {
                    textValue = value;
                } else {
                    textValue += "." + value;
                }
            }
        }
        return textValue;
    }

    @ResponseBody
    @RequestMapping(value = "/international/getLanguageText")
    public Map<String, Object> getLanguageText(String key) {
        Map<String, Object> responseMap = new HashMap<>();
        String textValue = null;
        if (null != key) {
            String[] keys = key.split(",");
            for (int i = 0; i < keys.length; i++) {
                String value = InternationalResource.get(keys[i]);
                if (i == 0) {
                    textValue = value;
                } else {
                    textValue += "." + value;
                }
            }
            responseMap.put("value",textValue);
        }
        return responseMap;
    }

    @Autowired
    private SystemCodeService systemCodeService;
    @Autowired
    private CompanyService companyService;

    @ResponseBody
    @RequestMapping(value = "/systemCode/systemCodeJson")
    public Map systemCodeJson(String systemEntityCode) throws Exception {
        Map<String, String> systemCodeMap = null;
        if (systemEntityCode != null && !systemEntityCode.equals("")) {
            systemCodeMap = systemCodeService.getSystemCodeList(systemEntityCode, false);
        }

        return systemCodeMap;
    }

    @RequestMapping(value = "/menuInfo/pageFrame")
    public String pageFrame(ModelMap map, Boolean multiSelect, String callBackFuncName, String moduleArtifact) {
        map.addAttribute("multiSelect", multiSelect);
        map.addAttribute("callBackFuncName", callBackFuncName);
        map.addAttribute("moduleArtifact", moduleArtifact);
        return "foundation/menuInfo/pageFrame";
    }

    @RequestMapping(value = "/menuInfo/frame")
    public String frame(ModelMap map, Boolean configPage, String moduleArtifact) {
        map.addAttribute("configPage", configPage);
        map.addAttribute("moduleArtifact", moduleArtifact);
        return "foundation/menuInfo/frame";
    }

    @Autowired
    private MenuInfoService menuInfoService;
    @Autowired
    private MenuOperateService menuOperateService;

    @Value("${bap.company.single:false}")
    private Boolean isSingleMode = false;

    @ResponseBody
    @RequestMapping(value = "/menuInfo/listChildren")
    public List<MenuInfoVO> listChildren(Long id) {
        MenuInfo menuInfo = null;
        if (null != id) {
            menuInfo = new MenuInfo();
            menuInfo.setId(id);
        }
        List<MenuInfo> list = new ArrayList<MenuInfo>();
        List<Company> companyList = new ArrayList<Company>();
        Company company = getCurrentCompany();
        Long cid = company.getId();
        companyList.add(company);
        list = (List<MenuInfo>) menuInfoService.getChildren(menuInfo, companyList.toArray(new Company[]{}));
        List<MenuInfo> menuInfoList = null;
        if (null != list) {
            menuInfoList = new ArrayList<MenuInfo>();
            Iterator<MenuInfo> iter = list.iterator();
            while (iter.hasNext()) {
                MenuInfo m = (MenuInfo) iter.next();

                menuInfoList.add(m);

            }
        }
        return menuInfoWrapper.e2vList(menuInfoList);
    }

    @ResponseBody
    @RequestMapping(value = "/menuInfo/getInfo")
    public MenuInfoVO getinfo(@RequestParam("menuInfo.id") Long menuInfoId) {
        MenuInfo menuInfo = menuInfoService.load(menuInfoId);
        return menuInfoWrapper.e2v(menuInfo);
    }

    private SqlParser filterString = new SqlParser();

    @ResponseBody
    @RequestMapping(value = "/menuInfo/queryListFeatures")
    public Page<MenuOperateVO> queryListFeatures(@RequestParam("menuInfo.id") Long menuInfoId,
                                                 @Nullable String queryParam, @Nullable String queryValue,
                                                Integer pageNo, Integer pageSize) {
        MenuInfo menuInfo = menuInfoService.load(menuInfoId);

        String hql = "select * from " + MenuOperate.TABLE_NAME + " mo where mo.valid=1";
        Map<String, Object> parameterMap = new LinkedHashMap<String, Object>();
        if (queryParam != null && queryValue != null && !queryValue.equals("") && !queryParam.equals("")) {
            List<Object> values = new ArrayList<Object>();
            if (queryParam.equals("queryopName") || queryParam.equals("querymenuName")) {
                values.add(filterString.filtrateSQLLike(queryValue));
                values.addAll(InternationalResource.getMessageKeys(filterString.filtrateSQLLike(queryValue), InternationalResource.getDefaultLanguage()));
            }
            if (queryParam.equals("queryopName")) {
                hql += " and mo.name in (:monames)";
                parameterMap.put("monames", values);
            } else if (queryParam.equals("querymenuName")) {
                hql += " and mo.menuinfo_id in (select mi.id from base_menuinfo mi where mi.valid=1 and mi.name in (:minames))";
                parameterMap.put("minames", values);
            } else if (queryParam.equals("queryopCode")) {
                hql += " and mo.code like '%" + SqlParser.filtrateSQLLike(queryValue) + "%' escape '&'";
            }
        }
        Company company = getCurrentCompany();
        hql += " and mo.cid=" + company.getId();

        if (menuInfoId != null) {
            hql += " and mo.menuInfo_id=" + menuInfoId;
        }
        Page<MenuOperate> page = new Page(pageNo,pageSize);
        Page<MenuOperate> pageMenuOperate = menuOperateService.getByPage(page, hql, parameterMap);
        // 格式化输出
        if (!pageMenuOperate.getResult().isEmpty()) {
            Iterator<MenuOperate> iter = pageMenuOperate.getResult().iterator();
            while (iter.hasNext()) {
                MenuOperate menuOperate = iter.next();
                menuOperate.setMenuInfo(menuInfo);
            }
        }

        return menuOperateWrapper.e2vPage(pageMenuOperate);
    }

    @ResponseBody
    @RequestMapping(value = "/international/copyInternationalValue")
    public void copyInternationalValue (String key, String oldKey) {

        return;
    }

    @ResponseBody
    @RequestMapping(value = "/international/common/getSingleLanguageContent")
    public String getSingleLanguageContent(HttpServletRequest request) {
        String key= request.getParameter("key");
        String singleLanguage= request.getParameter("singleLanguage");
        return internationalService.getI18nValue(key,null,singleLanguage);
    }

    @ResponseBody
    @RequestMapping(value = "/international/saveSingleLanguage")
    public Map saveSingleLanguage(String key, String value, String singleLanguage, @Nullable String moduleCode) {
        // languages = internationalService.findLanguages();
        Pattern p = Pattern.compile("^(?!_)(?!.*?_$)[a-zA-Z0-9_.]+$");
        Boolean b;
        String message="";
        if(null!=key&&key.length()>0&&null!=moduleCode&&moduleCode.length()>0){
            //当moduleCode为foundation时不再验证国际化key是否以模块开头，原因是以ec开头的key其实也是属于foundation
            if(!moduleCode.equals("foundation")){
                String[] m = moduleCode.split("_");
                if(key.indexOf(m[0], 0)==-1){
                    throw new EcException(EcException.Code.NO_INTERNATION_KEY);
                }
            }
            b=p.matcher(key).matches();
            if(!b)
                throw new EcException(EcException.Code.CONTRAINT_CODE);
        }
        if (null != value && null != key) {
            b=p.matcher(key).matches();
            if(!b)
                throw new EcException(EcException.Code.CONTRAINT_CODE);
            // 遍历语言，保存特定语言
            if (null == singleLanguage) {
                singleLanguage = getUserLanguage();
            }
            internationalService.addInternational("key=" + key + "$&#" + singleLanguage + "=" + value);
        }
        Map<String, Object> responseMap = new HashMap<String, Object>();
        responseMap.put("dealSuccessFlag", true);
        responseMap.put("operateType", "save");
        responseMap.put("key", key);
        responseMap.put("value", value);
        return responseMap;
    }

    /**
     * 删除 特定语言国际化
     *
     * @return
     * @throws Exception
     */
    @ResponseBody
    @RequestMapping(value = "/international/delSingleLanguage")
    public void delSingleLanguage(String singleLanguage, String key) {
        internationalService.addInternational("key=" + key + "$&#" + singleLanguage + "=" + "");
    }

    @Autowired
    private InternationalService internationalService;

    @RequestMapping(value = "/international/editListFrame")
    public String editListFrame(ModelMap map, String moduleCode, String name, String key, String callBackFuncName, String requstObjectType) throws Exception {
        boolean isNew = true;
        List<International> internationalList = null;
        List<Language> languages;
        if(null!=key&&!"".equals(key)){
            internationalList=internationalService.getInternationals(key);
            if(null != internationalList && !internationalList.isEmpty()){
                for(International international : internationalList){
                    if(international.getValue().length() > 0){
                        isNew = false;
                        break;
                    }
                }
            }
        }
        languages = internationalService.getAllLanguage();
        map.put("isNew", isNew);
        map.put("internationalList", internationalList);
        map.put("languages", languages);
        map.put("moduleCode", moduleCode);
        map.put("key", key);
        map.put("name", name);
        map.put("callBackFuncName", callBackFuncName);
        map.put("requstObjectType", requstObjectType);
        return "international/editListFrame";
    }

    @ResponseBody
    @RequestMapping(value = "/international/queryList")
    public Page queryList() {
        International international = getInternational(getRequest());
        Page<Map<String, Object>> page = getPage(getRequest());
        internationalService.internationalPage(page, international);
        return page;
    }

    private Page getPage(HttpServletRequest request) {
        Page<Map<String, Object>> page = new Page<>();
        page.setPageSize(Integer.valueOf(request.getParameter("page.pageSize")));
        page.setMaxPageSize(Integer.valueOf(request.getParameter("page.maxPageSize")));
        if(request.getParameter("page.pageNo")!=null) {
        	page.setPageNo(Integer.valueOf(request.getParameter("page.pageNo")));
        }
        return page;
    }

    private International getInternational(HttpServletRequest request) {
        String key = request.getParameter("international.key");
        String language = request.getParameter("international.languageKey");
        String value = request.getParameter("international.value");
        International international = new International();
        if (!StringUtils.isEmpty(key)) {
            international.setKey(key);
        }
        if (!StringUtils.isEmpty(language)) {
            international.setLanguage(language);
        }
        if (!StringUtils.isEmpty(value)) {
            international.setValue(value);
        }
        return international;
    }

    @ResponseBody
    @RequestMapping(value = "/workbench/common/mneClient")
    public String mneClient(HttpServletRequest request) {
        return null;
    }
    /*{
        long start = System.currentTimeMillis();
        String type = null;
        String searchContent = null;
        String conditionStr = "";
        String isCrossCompany = "";
        String[] params;
        String[] types = request.getParameterValues("type");
        String showRange = request.getParameter("showRange");
        boolean flag = false;
        if (null != types && types.length > 0) {
            type = types[0];
        }
        String[] searchContents = request.getParameterValues("searchContent");
        boolean customerRule = false;
        if (null != searchContents && searchContents.length > 0) {
            searchContent = searchContents[0];
            if(searchContent.isEmpty()){
                searchContent = "*";
            }
            if(searchContent.contains("*")){
                customerRule = true;
            }
            searchContent = StringUtils.escape(searchContent);
            if(searchContent!=null){
                searchContent=searchContent.toLowerCase();
            }
        }
        Object[] oparams = new Object[5];
        params = request.getParameterValues("conditionParams");
        if (null != params && params.length > 0) {
            if(!"".equals(params[0]) && params[0].trim().length() > 0) {
                conditionStr += " AND (" + params[0] + ")";
            }
        }
        String[] isCrossCompanys = request.getParameterValues("isCrossCompany");
        String[] isSpecialNoCrossCompany = request.getParameterValues("specialNoCrossCompany");//这个属性特例用来在多组织情况下禁止用户分配权限时能选到其他公司，正常情况下这个参数为空
        //如果是多组织那么就默认放开查跨公司数据
        if(!isSingleMode&&isSpecialNoCrossCompany==null){
            oparams[1] = "true";
        }
        else{
            if (null != isCrossCompanys && isCrossCompanys.length > 0) {
                isCrossCompany = isCrossCompanys[0];
                oparams[1] = isCrossCompany;
            } else {
                oparams[1] = "false";
            }
        }
        String showNumber = getRequest().getParameter("showNumber");
        if (showNumber != null && showNumber.length() > 0 && ValidateUtils.isInt(showNumber)) {
            int showNum = Integer.valueOf(showNumber);
            if (showNum <= 0) {
                oparams[2] = 6;
            } else {
                oparams[2] = showNum;
            }
        } else {
            oparams[2] = 6;
        }
        MneClientService mcs = null;
        if("Staff".equals(type)) {
            mcs = (MneClientService) staffService;
        } else if("staffRange".equals(type)){
            oparams[1] = "true";
            mcs = (MneClientService) staffService;
            String fieldCode = request.getParameter("fieldCode");
            String selectPeople = request.getParameter("selectPeople");
            if(selectPeople!=null && selectPeople.equals("3")){
                type="userRange";
                flag = true;
            }else{
                if(fieldCode!=null && fieldCode.length()>0){
                    List<SelectionRange> ranges = fieldService.getRangeByFieldCode(fieldCode);
                    StringBuffer sb = new StringBuffer();
                    for(SelectionRange range : ranges){
                        sb.append(range.getRangeId()).append(",");
                    }
                    conditionStr += " AND BASE_STAFF.ID IN (" + sb.deleteCharAt(sb.length()-1).toString() + ")";
                }else{
                    type="userRange";
                    flag = true;
                }
            }

        } else if("userRange".equals(type)){
            oparams[1] = "true";
            mcs = (MneClientService) userService;
            if(flag){
                mcs = (MneClientService) staffService;
            }

            String selectPeople = request.getParameter("selectPeople");

            if(selectPeople!=null && selectPeople.length()>0){
                try{
                    int sp = Integer.parseInt(selectPeople);
                    if(sp == 5){
                        String deploymentId = request.getParameter("deploymentId");
                        String outcome = request.getParameter("outcome");
                        Set<Long> ids = userService.getSelectUserIds(Long.parseLong(deploymentId), outcome);
                        StringBuffer sb = new StringBuffer();
                        if(ids!=null && ids.size()>0){
                            if(!flag){
                                Iterator<Long> iter = ids.iterator();
                                while(iter.hasNext()){
                                    sb.append(iter.next());
                                    if(iter.hasNext()){
                                        sb.append(",");
                                    }
                                }
                                conditionStr += " AND BASE_USERINFO.ID IN (" + sb.toString() + ")";
                            }else{
                                Iterator<Long> iter = ids.iterator();
                                while(iter.hasNext()){
                                    User u = (User) userService.getUserById(iter.next());
                                    sb.append(u.getStaff().getId());
                                    if(iter.hasNext()){
                                        sb.append(",");
                                    }
                                }
                                conditionStr += " AND BASE_STAFF.ID IN (" + sb.toString() + ")";
                            }

                        }else{
                            conditionStr += " AND BASE_STAFF.ID = -1";
                        }
                    }else if(sp == 3){
                        //Staff staff = (Staff) getCurrentStaff();

                        Set<Staff> staffs = new HashSet<Staff>();
                        staffs.add((Staff) getCurrentStaff());
                        String sourceStaff = request.getParameter("sourceStaff");
                        StringBuffer sb = new StringBuffer(" and (");
                        if(sourceStaff!=null && sourceStaff.length()>0){
                            String[] ids = sourceStaff.split(",");
                            for(String id : ids){
                                if(id!=null && id.length()>0){
                                    User user = userService.load(Long.parseLong(id));
                                    staffs.add(user.getStaff());
                                }
                            }
                        }
                        long corssCompanyId = -1l;
                        for(Staff staff : staffs){
                            Page<DepartmentWork> page = new Page<DepartmentWork>();
                            List<Department> departments = new ArrayList<Department>();
                            page.setPageSize(Integer.MAX_VALUE);
                            page = staffService.getDepartmentWorks(page, Restrictions.eq("valid", true), Restrictions.eq("staff", staff));
                            if(page.getResult()!=null && page.getResult().size()>0){
                                for(DepartmentWork dw : page.getResult()){
                                    //if(dw.getDepartment().getCid().longValue() == getCurrentCompanyId()){
                                    departments.add(dw.getDepartment());
                                    //}
                                }
                            }

                            if(page.getResult()!=null && page.getResult().size()>0){
                                for(DepartmentWork dw : page.getResult()){
                                    //if(dw.getDepartment().getCid().longValue() == getCurrentCompanyId()){
                                    departments.add(dw.getDepartment());
                                    if(sb.length()!=6){
                                        sb.append(" or ");
                                    }
                                    //sb.append("dept.lay_rec = '").append(dw.getDepartment().getLayRec()).append("'");
                                    sb.append("dept.lay_rec like '").append(dw.getDepartment().getLayRec()).append("-%'");
                                    sb.append(" or ");
                                    sb.append("dept.lay_rec = '").append(dw.getDepartment().getLayRec()).append("'");
                                    //}
                                    if(corssCompanyId == -1){
                                        corssCompanyId = dw.getDepartment().getCid();
                                    }else{
                                        if("false".equals(oparams[1]) && corssCompanyId != dw.getDepartment().getCid()){
                                            oparams[1] = "true";
                                        }
                                    }
                                }
                            }
                        }
                        sb.append(")");
                        if(!flag){
                            String sql = "select userinfo.id from base_departmentwork dw, base_staff staff, base_userinfo userinfo, base_department dept where dw.staff_id = staff.id and staff.user_id = userinfo.id and dw.valid = 1 and dw.department_id = dept.id " + sb.toString();
                            conditionStr += " AND BASE_USERINFO.ID IN (" + sql + ")";
                        }else{
                            String sql = "select staff.id from base_departmentwork dw, base_staff staff, base_userinfo userinfo, base_department dept where dw.staff_id = staff.id and staff.user_id = userinfo.id and dw.valid = 1 and dw.department_id = dept.id " + sb.toString();
                            conditionStr += " AND BASE_STAFF.ID IN (" + sql + ")";
                        }
                    }else if(sp == 4){
                        //Staff staff = (Staff) getCurrentStaff();

                        Set<Staff> staffs = new HashSet<Staff>();
                        staffs.add((Staff) getCurrentStaff());
                        String sourceStaff = request.getParameter("sourceStaff");
                        StringBuffer sb = new StringBuffer(" and (");
                        if(sourceStaff!=null && sourceStaff.length()>0){
                            //oparams[1] = "true";
                            String[] ids = sourceStaff.split(",");
                            for(String id : ids){
                                if(id!=null && id.length()>0){
                                    User user = userService.load(Long.parseLong(id));
                                    staffs.add(user.getStaff());
                                }
                            }
                        }
                        long corssCompanyId = -1l;
                        for(Staff staff : staffs){
                            Page<DepartmentWork> page = new Page<DepartmentWork>();
                            List<Department> departments = new ArrayList<Department>();
                            page.setPageSize(Integer.MAX_VALUE);
                            page = staffService.getDepartmentWorks(page, Restrictions.eq("valid", true), Restrictions.eq("staff", staff));
                            if(page.getResult()!=null && page.getResult().size()>0){
                                for(DepartmentWork dw : page.getResult()){
                                    //if(dw.getDepartment().getCid().longValue() == getCurrentCompanyId()){
                                    departments.add(dw.getDepartment());
                                    if(sb.length()!=6){
                                        sb.append(" or ");
                                    }
                                    sb.append("dept.lay_rec like '").append(dw.getDepartment().getLayRec()).append("-%'");
                                    sb.append(" or ");
                                    sb.append("dept.lay_rec = '").append(dw.getDepartment().getLayRec()).append("'");
                                    //}
                                    if(corssCompanyId == -1){
                                        corssCompanyId = dw.getDepartment().getCid();
                                    }else{
                                        if("false".equals(oparams[1]) && corssCompanyId != dw.getDepartment().getCid()){
                                            oparams[1] = "true";
                                        }
                                    }
                                }
                            }
                        }
                        sb.append(")");
                        if(!flag){
                            String sql = "select userinfo.id from base_departmentwork dw, base_staff staff, base_userinfo userinfo, base_department dept where dw.staff_id = staff.id and staff.user_id = userinfo.id and dw.valid = 1 and dw.department_id = dept.id " + sb.toString();
                            conditionStr += " AND BASE_USERINFO.ID IN (" + sql + ")";
                        }else{
                            String sql = "select staff.id from base_departmentwork dw, base_staff staff, base_userinfo userinfo, base_department dept where dw.staff_id = staff.id and staff.user_id = userinfo.id and dw.valid = 1 and dw.department_id = dept.id " + sb.toString();
                            conditionStr += " AND BASE_STAFF.ID IN (" + sql + ")";
                        }

                    }
                }catch(Exception e){
                    log.info(e.getMessage());
                }
            }else{
                String deploymentId = request.getParameter("deploymentId");
                String outcome = request.getParameter("outcome");
                Set<Long> ids = userService.getSelectUserIds(Long.parseLong(deploymentId), outcome);
                StringBuffer sb = new StringBuffer();
                if(ids!=null && ids.size()>0){
                    if(!flag){
                        Iterator<Long> iter = ids.iterator();
                        while(iter.hasNext()){
                            sb.append(iter.next());
                            if(iter.hasNext()){
                                sb.append(",");
                            }
                        }
                        conditionStr += " AND BASE_USERINFO.ID IN (" + sb.toString() + ")";
                    }else{
                        Iterator<Long> iter = ids.iterator();
                        while(iter.hasNext()){
                            User u = (User) userService.getUserById(iter.next());
                            sb.append(u.getStaff().getId());
                            if(iter.hasNext()){
                                sb.append(",");
                            }
                        }
                        conditionStr += " AND BASE_STAFF.ID IN (" + sb.toString() + ")";
                    }

                }
            }
        } else if("Position".equals(type)) {
            mcs = (MneClientService) positionService;
        } else if("Department".equals(type)) {
            mcs = (MneClientService) departmentService;
        } else if("User".equals(type)) {
            mcs = (MneClientService) userService;
        } else if("Role".equals(type)) {
            mcs = (MneClientService) roleService;
        } else if("MenuInfo".equals(type)) {
            mcs = (MneClientService) menuInfoService;
        }
        if (null != mcs) {
            if(null != showRange && showRange.length() > 0){
                oparams[3] = showRange;
            }
            List<Map<String, Object>> list=null;
            //如输入字符串中包含 * 则不自动添加%
            if(customerRule){
                oparams[0] = searchContent;
                list = mcs.search(oparams , conditionStr);
            } else {
                int showNum=Integer.valueOf(oparams[2].toString());
                oparams[0] = searchContent;
                list = mcs.search(oparams , conditionStr);
                if(showNum>list.size()){
                    oparams[2]=showNum-list.size();
                    oparams[0] = searchContent+'%';
                    oparams[4] = getMneIDs(list);
                    list.addAll(mcs.search(oparams , conditionStr));
                    if(showNum>list.size()){
                        oparams[2]=showNum-list.size();
                        oparams[0] = '%'+searchContent+'%';
                        oparams[4] = getMneIDs(list);
                        list.addAll(mcs.search(oparams , conditionStr));
                    }
                }
            }

            if (!list.isEmpty()) {
                StringBuilder json = new StringBuilder();
                json.append('[');
                for (int i = 0; i < list.size(); i++) {
                    if (i > 0) {
                        json.append(',');
                    }
                    json.append('"');
                    int count = 0;
                    for (Map.Entry<String, Object> entry : list.get(i).entrySet()) {
                        if (count++ > 0) {
                            json.append("$$BAP$$");
                        }
                        json.append(entry.getKey().toLowerCase());
                        json.append("@@BAP@@");
                        json.append(entry.getValue());
                    }
                    json.append('"');
                }
                json.append(']');
                return json.toString();
            }
        }
        return null;
    }*/

    private String getMneIDs(List<Map<String, Object>> list){
        if(list.size()==0){
            return null;
        }else{
            String res="";
            for(Map<String, Object> m:list){
                if(m.get("ID")!=null){
                    res+=","+m.get("ID").toString();
                }
            }
            return res.substring(1);
        }

    }

}
