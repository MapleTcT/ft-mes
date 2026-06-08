package com.supcon.supfusion.configuration.workflow.openapi;

import com.google.common.base.Stopwatch;
import com.supcon.supfusion.base.entities.*;
import com.supcon.supfusion.base.enums.CompanyType;
import com.supcon.supfusion.base.services.*;
import com.supcon.supfusion.configuration.services.entity.CustomPropertyViewMapping;
import com.supcon.supfusion.configuration.services.enums.DbColumnType;
import com.supcon.supfusion.configuration.services.i18n.InternationalResource;
import com.supcon.supfusion.configuration.services.service.ModelService;
import com.supcon.supfusion.configuration.services.service.ViewService;
import com.supcon.supfusion.configuration.services.utils.SqlParser;
import com.supcon.supfusion.configuration.workflow.core.WorkflowBaseController;
import com.supcon.supfusion.configuration.workflow.db.RestrictionsCustomizer;
import com.supcon.supfusion.framework.scaffold.hibernate.page.Page;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.type.StandardBasicTypes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.*;


/**
 * @Description: TODO
 * @Version V1.0
 * @Auther: huning
 * @Date: 2020/8/19
 */
@Slf4j
@Controller
@RequestMapping(value = "ec/foundation")
public class CommonController extends WorkflowBaseController {

    @Autowired
    private CompanyService companyService;
    @Autowired
    private DepartmentService departmentService;
    @Autowired
    private PositionService positionService;
    @Autowired
    private RoleService roleService;
    @Autowired
    private UserService userService;
    @Autowired
    private RoleUserService roleUserService;
    @Autowired
    private PositionWorkService positionWorkService;
    @Autowired
    private ViewService viewService;
    @Autowired
    private ModelService modelService;

    @RequestMapping(value = "/user/common/userListFrameset")
    public String userListFrameset(ModelMap map, Boolean multiSelect, String callBackFuncName, String entityCode) {
        map.addAttribute("multiSelect", multiSelect);
        map.addAttribute("callBackName", callBackFuncName);
        map.addAttribute("callBackFuncName", callBackFuncName);
        map.addAttribute("crossCompanyFlag", true);
        map.addAttribute("closePage", true);
        map.addAttribute("Parameters.openType", "page");
        map.addAttribute("groupCrossCompanyFlag", true);
        return "workflow/userListFrameset";
    }

    @RequestMapping(value = "/user/common/departmentUserList")
    public String departmentUserList(ModelMap map, Boolean crossCompanyFlag, Boolean closePage, String openType,
                                     String callBackFuncName, Boolean multiSelect, Boolean enableAddToGroup) {
        List<Company> companyList = new ArrayList<Company>();
        List<Company> list = companyService.getAllCompanies();
        companyList.addAll(list);

        Map<String, Object> Parameters = new HashMap<>();
        Parameters.put("openType", openType);
        Parameters.put("showRange", "all");
        Parameters.put("multiSelect", multiSelect);
        map.addAttribute("companyList", companyList);
        map.addAttribute("crossCompanyFlag", crossCompanyFlag);
        map.addAttribute("closePage", closePage);
        map.addAttribute("multiSelect", multiSelect);
        map.addAttribute("callBackFuncName", callBackFuncName);
        map.addAttribute("crossCompanyFlag", crossCompanyFlag);
        map.addAttribute("closePage", closePage);
        map.addAttribute("enableAddToGroup", enableAddToGroup);
        map.addAttribute("Parameters", Parameters);
        return "workflow/departmentUserList";
    }

    @RequestMapping(value = "/user/common/positionUserList")
    public String positionUserList(ModelMap map, Boolean crossCompanyFlag, Boolean closePage, String openType,
                                   String callBackFuncName, Boolean multiSelect, Boolean enableAddToGroup) {
        List<Company> companyList = new ArrayList<Company>();
        List<Company> list = companyService.getAllCompanies();
        companyList.addAll(list);

        Map<String, Object> Parameters = new HashMap<>();
        Parameters.put("openType", openType);
        Parameters.put("showRange", "all");
        Parameters.put("multiSelect", multiSelect);
        map.addAttribute("companyList", companyList);
        map.addAttribute("crossCompanyFlag", crossCompanyFlag);
        map.addAttribute("closePage", closePage);
        map.addAttribute("multiSelect", multiSelect);
        map.addAttribute("callBackFuncName", callBackFuncName);
        map.addAttribute("closePage", closePage);
        map.addAttribute("enableAddToGroup", enableAddToGroup);
        map.addAttribute("Parameters", Parameters);
        return "workflow/positionUserList";
    }

    @RequestMapping(value = "/position/common/positionListFrame")
    public String positionListFrame(ModelMap map, Boolean multiSelect, String callBackFuncName,
                                    String entityCode, Boolean unassignStaffSupport,String openType) {
        List<Company> companyList = new ArrayList<Company>();
        List<Company> list = companyService.getAllCompanies();
        companyList.addAll(list);
        Map<String, Object> Parameters = new HashMap<>();
        Parameters.put("openType", openType);
        Parameters.put("showRange", "all");
        multiSelect=multiSelect==null?false:multiSelect;
        Parameters.put("multiSelect", multiSelect);
        Parameters.put("unassignStaffSupport", unassignStaffSupport);
        map.addAttribute("companyList", companyList);
        map.addAttribute("closePage", true);
        map.addAttribute("crossCompanyFlag", true);
        map.addAttribute("Parameters", Parameters);
        map.addAttribute("multiSelect", multiSelect);
        map.addAttribute("callBackFuncName", callBackFuncName);
        map.addAttribute("entityCode", entityCode);
        return "workflow/positionListFramePage";
    }

    @RequestMapping(value = "/position/common/assignPosition")
    public String assignPosition(ModelMap map, Boolean multiSelect, String callBackFuncName, String assignPositonIds,
                                 String entityCode, Boolean unassignStaffSupport) {
        List<Company> companyList = new ArrayList<Company>();
        List<Company> list = companyService.getAllCompanies();
        companyList.addAll(list);
        List<Position> positionList = null;
        if (assignPositonIds != null && !assignPositonIds.equals("")) {
            positionList = positionService.getAssignPositions(assignPositonIds);
        }
        map.addAttribute("companyList", companyList);
        map.addAttribute("multiSelect", multiSelect);
        map.addAttribute("callBackFuncName", callBackFuncName);
        map.addAttribute("assignPositonIds", assignPositonIds);
        map.addAttribute("closePage", true);
        map.addAttribute("crossCompanyFlag", false);
        map.addAttribute("viewFlag", false);
        map.addAttribute("entityCode", entityCode);
        map.addAttribute("unassignStaffSupport", unassignStaffSupport);
        map.addAttribute("positionList", positionList);
        return "workflow/assignPositionListFrame";
    }

    @RequestMapping(value = "/role/common/roleListFrame")
    public String roleListFrame(ModelMap map, Boolean multiSelect, String callBackFuncName,
                                String entityCode, Boolean unassignStaffSupport,Boolean crossCompanyFlag,Boolean isUserRef) {
        List<Company> companyList = new ArrayList<Company>();
        List<Company> list = companyService.getAllCompanies();
        companyList.addAll(list);
        Map<String, Object> Parameters = new HashMap<>();
        Parameters.put("openType", "page");
        Parameters.put("showRange", "all");
        Parameters.put("multiSelect", multiSelect);
        map.addAttribute("companyList", companyList);
        map.addAttribute("multiSelect", multiSelect);
        map.addAttribute("callBackFuncName", callBackFuncName);
        map.addAttribute("closePage", true);
        map.addAttribute("crossCompanyFlag", true);
        map.addAttribute("entityCode", entityCode);
        map.addAttribute("unassignStaffSupport", unassignStaffSupport);
        map.addAttribute("Parameters", Parameters);
        if(null!=isUserRef){
            map.addAttribute("isUserRef", isUserRef);
        }else{
            map.addAttribute("isUserRef", false);
        }

        return "workflow/roleListFrame";
    }

    @RequestMapping(value = "/department/common/departmentListFrame")
    public String departmentListFrame(ModelMap map, Boolean multiSelect, String callBackFuncName,
                                      String entityCode, Boolean unassignStaffSupport,String openType) {
        List<Company> companyList = new ArrayList<Company>();
        List<Company> list = companyService.getAllCompanies();
        companyList.addAll(list);
        Map<String, Object> Parameters = new HashMap<>();
        Parameters.put("openType", openType);
        Parameters.put("showRange", "all");
        multiSelect=multiSelect==null?false:multiSelect;
        Parameters.put("multiSelect", multiSelect);
        map.addAttribute("companyList", companyList);
        map.addAttribute("multiSelect", multiSelect);
        map.addAttribute("callBackFuncName", callBackFuncName);
        map.addAttribute("closePage", true);
        map.addAttribute("crossCompanyFlag", true);
        map.addAttribute("entityCode", entityCode);
        map.addAttribute("unassignStaffSupport", unassignStaffSupport);
        map.addAttribute("Parameters", Parameters);
        return "workflow/departmentListFrame";
    }

    @RequestMapping(value = "/staff/common/assignStaffFrame")
    public String assignStaffFrame(ModelMap map, Boolean multiSelect, String callBackFuncName, String assignStaffs,
                                   String entityCode, Boolean unassignStaffSupport) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("openType", "page");
        parameters.put("showRange", "all");
        parameters.put("multiSelect", multiSelect);
        map.addAttribute("assignStaffs", assignStaffs);
        map.addAttribute("multiSelect", multiSelect);
        map.addAttribute("callBackFuncName", callBackFuncName);
        map.addAttribute("closePage", true);
        map.addAttribute("crossCompanyFlag", false);
        map.addAttribute("entityCode", entityCode);
        map.addAttribute("viewFlag", false);
        map.addAttribute("unassignStaffSupport", unassignStaffSupport);
        map.addAttribute("Parameters", parameters);
        return "workflow/assignStaffFrame";
    }

    @RequestMapping(value = "/company/common/companyListFrame")
    public String companyListFrame(ModelMap map,String multiSelect, Boolean closePage, String callBackFuncName, String openType,
                                    String requstObjectType) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("openType", openType);
        parameters.put("showRange", "all");
        closePage=closePage==null?false:closePage;
        multiSelect=multiSelect==null?"false":multiSelect;
        parameters.put("closePage", closePage);
        map.addAttribute("Parameters", parameters);
        map.addAttribute("multiSelect", multiSelect);
        map.addAttribute("callBackFuncName", callBackFuncName);
        map.addAttribute("requstObjectType", requstObjectType);
        map.addAttribute("closePage", true);
        map.addAttribute("crossCompanyFlag", false);
        map.addAttribute("isSingleMode", false);
        map.addAttribute("defaultFilterNoUserStaff", true);
        return "workflow/companyListFrame";
    }
    @ResponseBody
    @RequestMapping(value = "/company/queryList")
    public Page<Company> companyQueryList(HttpServletRequest request) {
        Boolean isSingleMode=true;
        Integer pageNo=1;
        Integer pageSize=20;
        if(null!=request.getParameter("page.pageSize")){
            pageSize=Integer.valueOf(request.getParameter("page.pageSize"));
        }
        String companyName=request.getParameter("companyName");
        String companyCode=request.getParameter("companyCode");
        if(null!=request.getParameter("page.pageNo")){
            pageNo=Integer.valueOf(request.getParameter("page.pageNo"));
        }
        if(null!=request.getParameter("isSingleMode")) {
            isSingleMode = Boolean.valueOf(request.getParameter("isSingleMode"));
        }
        Page<Company> page = new Page<Company>(pageNo,pageSize);

        DetachedCriteria detachedCriteria = DetachedCriteria
                .forClass(Company.class);
        detachedCriteria.add(Restrictions.eq("valid", true));
        /*if(isSingleMode){
            detachedCriteria.add(Restrictions.eq("type", CompanyType.UNIT));
        }else{
            detachedCriteria.add(Restrictions.or(Restrictions.eq("type", CompanyType.UNIT), Restrictions.eq("type", CompanyType.GROUP)));
        }*/

        if (companyCode != null && !companyCode.equals("")) {
            detachedCriteria.add(Restrictions.sqlRestriction(
                    " lower({alias}.code) like '%"+filterString.filtrateSQLLike(companyCode.toLowerCase())+"%' escape '&'"));
        }
        if (companyName != null && !companyName.equals("")) {
            detachedCriteria.add(Restrictions.sqlRestriction(
                    " lower({alias}.SHORT_NAME) like '%"+filterString.filtrateSQLLike(companyName).toLowerCase()+"%' escape '&'"));
        }
        /*if(ids!=null&&!"".equals(ids)){
            String[] idsArr=ids.split(",");
            Long[] idArrs=new Long[idsArr.length];
            for(int i=0;i<idsArr.length;i++){
                idArrs[i]=Long.valueOf(idsArr[i]);
            }

            detachedCriteria.add(Restrictions.in("id", idArrs));
        }*/
        page = companyService.getByPage(page, detachedCriteria);

        return page;
    }

    @RequestMapping(value = "/staff/common/staffListFrameset")
    public String staffListFrameset(ModelMap map, Boolean multiSelect, String callBackFuncName, String openType,
                                    String requstObjectType) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("openType", openType);
        parameters.put("showRange", "all");
        multiSelect=multiSelect==null?false:multiSelect;
        parameters.put("multiSelect", multiSelect);
        map.addAttribute("Parameters", parameters);
        map.addAttribute("callBackFuncName", callBackFuncName);
        map.addAttribute("requstObjectType", requstObjectType);
        map.addAttribute("closePage", true);
        map.addAttribute("crossCompanyFlag", false);
        map.addAttribute("isSingleMode", false);
        map.addAttribute("defaultFilterNoUserStaff", true);
        return "workflow/staffListFrameset";
    }

    @RequestMapping(value = "/staff/common/departmentStaffList")
    public String departmentStaffList(ModelMap map, String openFrom, Boolean multiSelect, String callBackFuncName, String openType,
                                      Boolean enableAddToGroup, Boolean closePage,
                                      Boolean defaultFilterNoUserStaff, Long companyId) {
        List<Company> companyList = new ArrayList<Company>();
        List<Company> list = companyService.getAllCompanies();
        companyList.addAll(list);
        Map<String, Object> Parameters = new HashMap<>();
        Parameters.put("openType", openType);
        Parameters.put("showRange", "all");
        Parameters.put("multiSelect", multiSelect);
        map.addAttribute("Parameters", Parameters);
        map.addAttribute("companyList", companyList);
        map.addAttribute("callBackFuncName", callBackFuncName);
        map.addAttribute("closePage", closePage);
        map.addAttribute("companyId", companyId==null?1000L:companyId);
        map.addAttribute("openFrom", openFrom);
        map.addAttribute("enableAddToGroup", enableAddToGroup);
        map.addAttribute("defaultFilterNoUserStaff", defaultFilterNoUserStaff);
        return "workflow/departmentStaffList";
    }

    @RequestMapping(value = "/staff/common/positionStaffList")
    public String positionStaffList(ModelMap map, String openFrom, Boolean multiSelect, String callBackFuncName,
                                    Boolean enableAddToGroup, Boolean closePage, String openType,
                                    Boolean defaultFilterNoUserStaff, Long companyId) {
        List<Company> companyList = new ArrayList<Company>();
        List<Company> list = companyService.getAllCompanies();
        companyList.addAll(list);
        Map<String, Object> Parameters = new HashMap<>();
        Parameters.put("openType", openType);
        Parameters.put("showRange", "all");
        Parameters.put("multiSelect", multiSelect);
        map.addAttribute("Parameters", Parameters);
        map.addAttribute("companyList", companyList);
        map.addAttribute("callBackFuncName", callBackFuncName);
        map.addAttribute("closePage", closePage);
        map.addAttribute("companyId", companyId==null?1000L:companyId);
        map.addAttribute("openFrom", openFrom);
        map.addAttribute("enableAddToGroup", enableAddToGroup);
        map.addAttribute("defaultFilterNoUserStaff", defaultFilterNoUserStaff);
        return "workflow/positionStaffList";
    }

    @Autowired
    private StaffService staffService;

    @ResponseBody
    @RequestMapping(value = "/staff/common/assignListData")
    public Page assignListData(String assignStaffs, Integer pageNo, @RequestParam("records.pageSize") Integer pageSize) throws SQLException {

        Page<Map<String, Object>> records = new Page<Map<String, Object>>(pageNo, pageSize);
        // 根据queryConditionJson 生成SQL
        StringBuffer sqlSB = new StringBuffer();
        List<Object> parameters = new LinkedList<Object>();
        sqlSB.append("select DISTINCT s.code CODE,s.name NAME,s.id STAFFID from base_staff s where s.valid=1 ");

        // 指定人员assignStaffs
        if (!StringUtils.isEmpty(assignStaffs.trim())) {
            String[] assignStaffArr = assignStaffs.split(",");
            String assignQueryStr = "";
            for (String assignStaffId : assignStaffArr) {
                assignQueryStr += " or  s.id=?";
                parameters.add(Long.valueOf(assignStaffId));
            }
            if (!"".equals(assignQueryStr)) {
                assignQueryStr = assignQueryStr.substring(3);
                sqlSB.append(" and (" + assignQueryStr + ")");
            }
        } else {
            return records;
        }

        Object[] params = new Object[parameters.size()];
        sqlSB.append(" order by s.code ASC");
        records = staffService.findRecordPage(records, sqlSB.toString(), parameters.toArray(params));
        return records;
    }


    @ResponseBody
    @RequestMapping(value = "/department/listChildren")
    public List listDepartmentChildren(@Nullable Long id, @Nullable Long companyId) {
        if (id == -1L) {
            id = null;
        }
        List<Department> departmentList = departmentService.getTreeChildren(id, companyId);
        return departmentList;
    }

    @ResponseBody
    @RequestMapping(value = "/position/listChildren")
    public List listPositionChildren(@Nullable Long id, @Nullable Long companyId) {
        if (id == -1L) {
            id = null;
        }
        List<Position> positionList = positionService.getTreeChildren(id, companyId);
        return positionList;
    }

    @ResponseBody
    @RequestMapping(value = "/role/listChildren")
    public List listRoleChildren(@Nullable Long id, @Nullable Long comId) {
        if (id == -1L) {
            id = null;
        }
        List<Role> roleList = roleService.getTreeChildren(id, comId);
        return roleList;
    }

    @ResponseBody
    @RequestMapping(value = "/user/common/getDepartmentUserList")
    public Page getDepartmentUserList(HttpServletRequest request) {
        Long departmentId=null;
        Long companyId=null;
        Integer pageNo=1;
        Integer pageSize=20;
        if(null!=request.getParameter("departmentId")){
            departmentId=Long.valueOf(request.getParameter("departmentId"));
        }
        if(null!=request.getParameter("companyId")){
            companyId=Long.valueOf(request.getParameter("companyId"));
        }
        if(null!=request.getParameter("page.pageSize")){
            pageSize=Integer.valueOf(request.getParameter("page.pageSize"));
        }
        if(null!=request.getParameter("page.pageNo")){
            pageNo=Integer.valueOf(request.getParameter("page.pageNo"));
        }
        String staffName=request.getParameter("staff.name");
        String userName=request.getParameter("user.name");
        List<Object> parameters = new LinkedList<Object>();
        StringBuilder hql = new StringBuilder();
        Company company = getCurrentCompany();
        if (companyId != null) {
            company = companyService.get(companyId);
        }
        hql.append("select ui from User ui where ui.id in (select u from User u join u.staff.positionWorks pw join pw.position p "
                + " where pw.valid=true and p.valid=true and p.company=?0 and u.staff in (select cs.staff from CompanyStaff cs where cs.company=?1 and cs.valid=true) and u.valid=true ");
        parameters.add(company);
        parameters.add(company);
        if (departmentId != null) {
            hql.append(" and u.staff.id in (select dw.staff.id from DepartmentWork dw where dw.valid=true and dw.department=?2 )");
            parameters.add(departmentService.load(departmentId));
        }
        if (userName != null && userName.length() > 0) {
            hql.append(" and upper(u.name) like upper(?"+parameters.size()+") ");
            hql.append(SqlParser.ESCAPE);
            parameters.add("%" + SqlParser.afterEscape(userName) + "%");
        }
        if (staffName != null && staffName.length() > 0) {
            hql.append(" and upper(u.staff.name) like upper(?"+parameters.size()+") ");
            hql.append(SqlParser.ESCAPE);
            parameters.add("%" + SqlParser.afterEscape(staffName) + "%");

        }
        hql.append(") order by ui.staff.id");
        Page<User> userPage = new Page<User>(pageNo,pageSize);
        userPage = userService.getByPage(userPage, hql.toString(), parameters.toArray());
//        userPage = staffService.findstaffworkInfotouser(userPage);// 列表查找出人员主岗对应的部门和岗位信息
        return userPage;
    }

    @ResponseBody
    @RequestMapping(value = "/user/common/getPositionUserList")
    public Page getPositionUserList(HttpServletRequest request) {
        Long positionId=null;
        Long companyId=null;
        Integer pageNo=1;
        Integer pageSize=20;
        List<Object> parameters = new LinkedList<Object>();
        StringBuilder hql = new StringBuilder();
        if(null!=request.getParameter("positionId")){
            positionId=Long.valueOf(request.getParameter("positionId"));
        }
        if(null!=request.getParameter("companyId")){
            companyId=Long.valueOf(request.getParameter("companyId"));
        }
        Company company = getCurrentCompany();
        if (companyId != null) {
            company = companyService.get(companyId);
        }

        if(null!=request.getParameter("page.pageSize")){
            pageSize=Integer.valueOf(request.getParameter("page.pageSize"));
        }
        if(null!=request.getParameter("page.pageNo")){
            pageNo=Integer.valueOf(request.getParameter("page.pageNo"));
        }
        String staffName=request.getParameter("staff.name");
        String userName=request.getParameter("user.name");
        hql.append("select ui from User ui where ui.id in (select u from User u join u.staff.positionWorks pw join pw.position p "
                + " where pw.valid=true and p.valid=true and p.company=?0 and u.staff in (select cs.staff from CompanyStaff cs where cs.company=?1 and cs.valid=true) and u.valid=true ");

        parameters.add(company);
        parameters.add(company);
        if (positionId != null) {
            hql.append(" and u.staff.id in ( select st.id from Staff st  where st.valid=true and st.mainPosition=?2 )");
            parameters.add(positionService.load(positionId));
        }
        if (staffName != null && staffName.length() > 0) {
            hql.append(" and upper(u.staff.name) like upper(?"+parameters.size()+")");
            parameters.add("%" + staffName + "%");
        }
        if (userName != null && userName.length() > 0) {
            hql.append(" and upper(u.name) like upper(?"+parameters.size()+")");
            parameters.add("%" + userName + "%");
        }
        hql.append(") order by ui.staff.id");
        Page<User> userPage = new Page<User>(pageNo,pageSize);
        userPage = userService.getByPage(userPage, hql.toString(), parameters.toArray());
        return userPage;
    }

    @ResponseBody
    @RequestMapping(value = "/staff/common/getPositionWorkList")
    public Page getPositionWorkList(Long companyId, @Nullable Long positionId,
                                    @Nullable @RequestParam("staff.name") String staffName,
                                    @Nullable @RequestParam("staff.id") String staffId,
                                    @Nullable @RequestParam("staff.code") String staffCode,
                                    @Nullable Boolean filterNoUserStaff) {

        String pageNo = getRequest().getParameter("pageNo");
        String pageSize = getRequest().getParameter("pageSize");
        Page positionWorkPage = new Page<PositionWork>(StringUtils.isEmpty(pageNo) ? 1 : Integer.valueOf(pageNo), StringUtils.isEmpty(pageSize) ? 20 : Integer.valueOf(pageSize));

        DetachedCriteria detachedCriteria = DetachedCriteria.forClass(PositionWork.class).createAlias("staff", "s")
                .createAlias("position", "p");
        detachedCriteria.add(Restrictions.eq("valid", true));
        if (companyId != null) {
            detachedCriteria.add(Restrictions.eq("p.company", companyService.get(companyId)));
        } else {
            detachedCriteria.add(Restrictions.eq("p.company", getCurrentCompany()));
        }

        if (null != positionId && !positionId.toString().equals("")) {
            detachedCriteria.add(Restrictions.eq("position", positionService.load(positionId)));
        }

        if (staffCode != null && !staffCode.equals("")) {
            detachedCriteria.add(Restrictions.ilike("s.code", SqlParser.filtrateSQLLike(staffCode), MatchMode.ANYWHERE));
        }
        if (staffId != null) {
            detachedCriteria.add(Restrictions.eq("s.id", staffId));
        } else {
            if (staffName != null && !staffName.equals("")) {
                detachedCriteria.add(Restrictions.like("s.name", SqlParser.filtrateSQLLike(staffName), MatchMode.ANYWHERE));
            }
        }
        if (filterNoUserStaff != null && filterNoUserStaff) {
            detachedCriteria.add(Restrictions.isNotNull("s.user"));
        }

        detachedCriteria.addOrder(Order.asc("s.name"));
        positionWorkPage = positionWorkService.getByPage(positionWorkPage, detachedCriteria);
        return positionWorkPage;
    }

    @ResponseBody
    @RequestMapping(value = "/role/common/showRoleList")
    public Page<RoleUser> showRoleList(HttpServletRequest request) {
        Long companyId=null;
        //Long id=null;
        Boolean isThreeRoles=false;
        Integer pageNo=1;
        Integer pageSize=20;
        if(null!=request.getParameter("companyId")){
            companyId=Long.valueOf(request.getParameter("companyId"));
        }else{
            companyId=getCurrentCompanyId();
        }
        /*if(null!=request.getParameter("id")){
            id=Long.valueOf(request.getParameter("id"));
        }*/
        String roleCode=request.getParameter("roleCode");
        String roleName=request.getParameter("roleName");
        if(null!=request.getParameter("isThreeRoles")){
            isThreeRoles=Boolean.valueOf(request.getParameter("isThreeRoles"));
        }
        if(null!=request.getParameter("roleUserPage.pageSize")){
            pageSize=Integer.valueOf(request.getParameter("roleUserPage.pageSize"));
        }
        if(null!=request.getParameter("roleUserPage.pageNo")){
            pageNo=Integer.valueOf(request.getParameter("roleUserPage.pageNo"));
        }
        DetachedCriteria detachedCriteria = DetachedCriteria
                .forClass(Role.class);
        detachedCriteria.add(Restrictions.eq("valid", true));
        if(companyId!=null&&!companyId.toString().equals("")){
            detachedCriteria.add(Restrictions.eq("cid", companyId));
        }
        /*if(id != null){

            detachedCriteria.add(Restrictions.or(Restrictions.eq("id", id)));
        }*/
        if (roleCode != null && !roleCode.equals("")) {
            detachedCriteria.add(Restrictions.ilike("code",filterString.filtrateSQLLike(roleCode) ,MatchMode.ANYWHERE));
        }
        if (roleName != null && !roleName.equals("")) {
            detachedCriteria.add(Restrictions.like("name", filterString.filtrateSQLLike(roleName),MatchMode.ANYWHERE));
        }

        if(isThreeRoles){
            if(null == request.getParameter("showRange") || request.getParameter("showRange").length() == 0 || !request.getParameter("showRange").equals("all")){
                detachedCriteria.add(Restrictions.not(Restrictions.in("code", new String[]{"SYSTEM_ADMINISTRATOR","SECURITY_ADMINISTRATOR","SECURITY_AUDITOR"})));
                detachedCriteria.add(Restrictions.not(Restrictions.like("code","%_ADMINISTRATOR")));
                detachedCriteria.add(Restrictions.not(Restrictions.like("code","%_SECURITY_ADMIN")));
                detachedCriteria.add(Restrictions.not(Restrictions.like("code","%_SECURITY_AUDITOR")));
            }
        }
        Page<RoleUser> roleUserPage = new Page<RoleUser>(pageNo,pageSize);
        roleUserPage = roleUserService.getByPageFilterRole(roleUserPage, detachedCriteria);
        return roleUserPage;
    }

    private SqlParser filterString = new SqlParser();

    @ResponseBody
    @RequestMapping(value = "/position/common/showPositionList")
    public Page showPositionList(Integer pageNo, Integer pageSize, Long companyId, @Nullable Long id, @Nullable String positionCode, @Nullable String positionName) {
        Company currentCompany;
        Page page = new Page<Position>(pageNo, pageSize);

        if (companyId == null || companyId.toString().equals("")) {
            currentCompany = getCurrentCompany();
        } else {
            currentCompany = companyService.get(companyId);
        }

        DetachedCriteria detachedCriteria = DetachedCriteria.forClass(Position.class);
        detachedCriteria.add(Restrictions.eq("valid", true));
        detachedCriteria.add(Restrictions.eq("company", currentCompany));
        if (id != null) {
            detachedCriteria.add(Restrictions.or(Restrictions.eq("id", id),Restrictions.eq("parentId", id)));
        }
        if (positionCode != null && !positionCode.equals("")) {
            detachedCriteria.add(Restrictions.sqlRestriction(" {alias}.code like '%" + filterString.filtrateSQLLike(positionCode)
                    + "%' escape '&'"));

        }
        if (positionName != null && !positionName.equals("")) {
            detachedCriteria.add(Restrictions.sqlRestriction(" {alias}.name like '%" + filterString.filtrateSQLLike(positionName)
                    + "%' escape '&'"));
        }
        page = positionService.getByPage(page, detachedCriteria);

        List<Position> result=page.getResult();
        List<CustomPropertyViewMapping> viewMappings = null;
        if (!result.isEmpty()) {
            viewMappings = viewService.findCustomPropertyForSecret(
                    "sysbase_1.0_position_base_position", "sysbase_1.0_position_edit", "LIST", "");
        }
        updatePositionResult(result, viewMappings);

        return page;
    }

    @ResponseBody
    @RequestMapping(value = "/position/queryList")
    public Page queryList(Integer pageNo, Integer pageSize, Long companyId, @Nullable @RequestParam("position.id") Long positionId,
                            @Nullable @RequestParam("position.code") String positionCode, @Nullable @RequestParam("position.name") String positionName,
                            @Nullable @RequestParam("department.name") String departmentName, @Nullable @RequestParam("department.id") String departmentId,
                            @Nullable @RequestParam("position.category") String positionCategory) {
        Company currentCompany;
        Page page = new Page<Position>(pageNo, pageSize);
        if (companyId == null || companyId.toString().equals("")) {
            currentCompany = getCurrentCompany();
        } else {
            currentCompany = companyService.get(companyId);
        }
        DetachedCriteria detachedCriteria = DetachedCriteria.forClass(Position.class).createAlias("department", "dept");
        detachedCriteria.add(Restrictions.eq("valid", true));
        detachedCriteria.add(Restrictions.eq("company", currentCompany));
        if (positionCode != null && !positionCode.equals("")) {
            detachedCriteria.add(Restrictions.sqlRestriction(" lower({alias}.code) like '%" + filterString.filtrateSQLLike(positionCode).toLowerCase()
                    + "%' escape '&'"));
        }
        /**当岗位id不为空时则岗位id优先，否则岗位名称positionName优先，再则岗位类型*/
        if(positionId != null){
            detachedCriteria.add(Restrictions.eq("id", positionId));
        }else if (!StringUtils.isEmpty(positionName)) {
            detachedCriteria.add(Restrictions.sqlRestriction(" lower({alias}.name) like '%" + filterString.filtrateSQLLike(positionName).toLowerCase()
                    + "%' escape '&'"));
        }else if(positionCategory != null && !positionCategory.isEmpty()){
            if(!"BLANK".equals(positionCategory)){
                detachedCriteria.add(Restrictions.sqlRestriction("{alias}.SC_NATURE = '"+positionCategory+"'"));
            }
        }
        /**当部门id不为空时则部门id优先，否则岗位名称departmentName优先*/
        if(!StringUtils.isEmpty(departmentId)){
            detachedCriteria.add(Restrictions.eq("dept.id", departmentId));
        } else {
            if (!StringUtils.isEmpty(departmentName)) {
                detachedCriteria.add(Restrictions.ilike("dept.name", filterString.filtrateSQLLike(departmentName), MatchMode.ANYWHERE));
            }
        }
        detachedCriteria.addOrder(Order.asc("layNo"));
        detachedCriteria.addOrder(Order.asc("code"));
        page = positionService.getByPage(page, detachedCriteria);
        List<Position> result=page.getResult();

        List<CustomPropertyViewMapping> viewMappings = null;
        if (!result.isEmpty()) {
            viewMappings = viewService.findCustomPropertyForSecret(
                    "sysbase_1.0_position_base_position", "sysbase_1.0_position_edit", "LIST", "");
        }
        updatePositionResult(result, viewMappings);
        return page;
    }

    private void updatePositionResult(final List<Position> result, List<CustomPropertyViewMapping> viewMappings) {
        Stopwatch sw = Stopwatch.createStarted();

        for (Position position : result) {
            if (position.getParentId() != null && position.getParentId() != -1) {
                position.setParent(positionService.load(position.getParentId()));
            }
            getPositionCustomProperty(position, viewMappings);
        }
        log.info("Position handling cost {}.", sw);
    }

    public void getPositionCustomProperty(Position p, List<CustomPropertyViewMapping> viewMappings) {
        Field[] fields = p.getClass().getDeclaredFields();
        Map<String,Object> attrMap = new HashMap<>();
        for(CustomPropertyViewMapping viewMapping : viewMappings){
            for(Field f:fields){
                Object obj = null;
                try {
                    f.setAccessible(true);
                    obj = f.get(p);
                    if(null != viewMapping.getProperty() && (viewMapping.getProperty().getType().equals(DbColumnType.OBJECT) || viewMapping.getProperty().getType().equals(DbColumnType.SYSTEMCODE)) && viewMapping.getProperty().getCode().contains(f.getName()) && null != obj){
                        String displayValue = modelService.getMainDisplayValue(viewMapping.getProperty().getCode(), obj);
                        attrMap.put(f.getName(), InternationalResource.get(displayValue));
                        break;
                    }else if(null != viewMapping.getProperty() && viewMapping.getProperty().getCode().contains(f.getName())){
                        attrMap.put(f.getName(), obj);
                        break;
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(),e);
                }

            }
        }
        p.setAttrMap(attrMap);
    }

    @ResponseBody
    @RequestMapping(value = "/workbench/saveDataGrid")
    public String saveDataGrid(String dataGridCookieType, String dataGrid, Boolean adminCookie) {
        return String.valueOf(true);
    }

    @ResponseBody
    @RequestMapping(value = "/department/common/showDepartmentList")
    public Page showDepartmentList(Long id, Long companyId) {
        DetachedCriteria detachedCriteria = DetachedCriteria.forClass(Department.class);
        detachedCriteria.add(Restrictions.eq("valid", true));
        Company currentCompany;
        if (companyId == null || companyId.toString().equals("")) {
            currentCompany = getCurrentCompany();
        } else {
            currentCompany = companyService.get(companyId);
        }
        detachedCriteria.add(Restrictions.eq("company", currentCompany));
        Department load = departmentService.load(id);
        if (load != null) {
            detachedCriteria.add(Restrictions.or(Restrictions.eq("id", id),
                    Restrictions.like("layRec", load.getLayRec() + "-", MatchMode.START)));
        }

        Page page = departmentService.getByPage(new Page<Department>((20)), detachedCriteria);

        return page;
    }

    @Autowired
    private DepartmentWorkService departmentWorkService;

    @ResponseBody
    @RequestMapping(value = "/staff/common/getDepartmentWorkList")
    public Page getDepartmentWorkList(@Nullable String staffCode,
                                      @Nullable Long staffId,
                                      @Nullable String staffName,
                                      @RequestParam(value = "staff.code", required = false) String staffCode2,
                                      @RequestParam(value = "staff.id", required = false) Long staffId2,
                                      @RequestParam(value = "staff.name", required = false) String staffName2,
                                      Long departmentId,
                                      Long companyId,
                                      Boolean filterNoUserStaff) {
        // 参数兼容
        if (staffCode == null && staffCode2 != null) {
            staffCode = staffCode2;
        }
        if (staffId == null && staffId2 != null) {
            staffId = staffId2;
        }
        if (staffName == null && staffName2 != null) {
            staffName = staffName2;
        }

        DetachedCriteria detachedCriteria = DetachedCriteria.forClass(DepartmentWork.class)
                .createAlias("department", "d")
                .createAlias("staff", "s")
                .createAlias("s.mainPosition", "p");
        detachedCriteria.add(Restrictions.eq("valid", true));
        if (companyId != null) {
            detachedCriteria.add(Restrictions.eq("d.company", companyService.get(companyId)));
        } else {
            detachedCriteria.add(Restrictions.eq("d.company.id", getCurrentCompanyId()));
        }
        if (null != departmentId && !departmentId.toString().equals("")) {

            detachedCriteria.add(Restrictions.eq("department", departmentService.load(departmentId)));
        }
        if (staffCode != null && !staffCode.equals("")) {
            detachedCriteria.add(RestrictionsCustomizer.sqlRestriction("{s}.code like '%" + SqlParser.afterEscape(staffCode) + "%' " + SqlParser.ESCAPE));
        }
        if (staffId != null) {
            detachedCriteria.add(Restrictions.eq("s.id", staffId));
        } else {
            if (staffName != null && !staffName.equals("")) {
                detachedCriteria.add(RestrictionsCustomizer.sqlRestriction("{s}.name like '%" + SqlParser.afterEscape(staffName) + "%' " + SqlParser.ESCAPE));
            }
        }

        detachedCriteria.add(Restrictions.eq("s.valid", true));
        detachedCriteria.add(Restrictions.or(Restrictions.eq("p.valid", true), Restrictions.eq("p.id", -1L)));
        if (filterNoUserStaff != null && filterNoUserStaff) {
            detachedCriteria.add(Restrictions.isNotNull("s.user"));
        }
        detachedCriteria.addOrder(Order.asc("s.name"));

        Page<DepartmentWork>  departmentWorkPage = new Page<DepartmentWork>(1, 20);
        HttpServletRequest request=getRequest();
        departmentWorkPage.setPageSize(Integer.valueOf(request.getParameter("departmentWorkPage.pageSize")));
        if(request.getParameter("departmentWorkPage.pageNo")!=null) {
        	departmentWorkPage.setPageNo(Integer.valueOf(request.getParameter("departmentWorkPage.pageNo")));
        }
        departmentWorkPage = departmentWorkService.getByPage(departmentWorkPage, detachedCriteria);
        departmentWorkPage = staffService.deptfindstaffworkInfo(departmentWorkPage);

        return departmentWorkPage;
    }
    
    private Page getPage(HttpServletRequest request) {
        Page<Map<String, Object>> page = new Page<>();
        page.setPageSize(Integer.valueOf(request.getParameter("pageSize")));
        if(request.getParameter("pageNo")!=null) {
        	page.setPageNo(Integer.valueOf(request.getParameter("pageNo")));
        }
        return page;
    }

    @ResponseBody
    @RequestMapping(value = "/department/queryList")
    public Page queryList(HttpServletRequest request) {
        Long companyId=null;
        Integer pageNo=1;
        Integer pageSize=20;
        if(null!=request.getParameter("page.pageSize")){
            pageSize=Integer.valueOf(request.getParameter("page.pageSize"));
        }
        String departmentName=request.getParameter("departmentName");
        String departmentCode=request.getParameter("departmentCode");
        if(null!=request.getParameter("page.pageNo")){
            pageNo=Integer.valueOf(request.getParameter("page.pageNo"));
        }
        if(null!=request.getParameter("companyId")){
            companyId=Long.valueOf(request.getParameter("companyId"));
        }else{
            companyId=getCurrentCompanyId();
        }
        DetachedCriteria detachedCriteria = DetachedCriteria.forClass(Department.class);
        detachedCriteria.add(Restrictions.eq("valid", true));
        Company currentCompany;
        if (companyId == null || companyId.toString().equals("")) {
            currentCompany = getCurrentCompany();
        } else {
            currentCompany = companyService.get(companyId);
        }
        detachedCriteria.add(Restrictions.eq("company", currentCompany));
        if (departmentCode != null && departmentCode.length() > 0) {
            // 这样的话查询的是 MatchMode.ANYWHERE 相当于 '%vule%' 这样的话特殊字符处理不了
            detachedCriteria.add(Restrictions.sqlRestriction(" lower({alias}.code) like ? escape '&'",
                    "%" + SqlParser.filtrateSQLLike(departmentCode).toLowerCase() + "%", StandardBasicTypes.STRING));
            // hibernate 生成的 前缀都是this的不知道具体的表明 查询不到数据 控制不了
            // 安主表查询 别名生成的是 this_
        }
        if (departmentName != null && departmentName.length() > 0) {

            detachedCriteria.add(Restrictions.sqlRestriction(" lower({alias}.name) like ? escape '&'",
                    "%" + SqlParser.filtrateSQLLike(departmentName).toLowerCase() + "%", StandardBasicTypes.STRING));
        }
        /*Department load = departmentService.load(id);
        if (load != null) {
            detachedCriteria.add(Restrictions.or(Restrictions.eq("id", id),
                    Restrictions.like("layRec", load.getLayRec() + "-", MatchMode.START)));
        }*/

        Page page = departmentService.getByPage(new Page<Department>(pageNo,pageSize), detachedCriteria);

        return page;
    }

    @ResponseBody
    @RequestMapping(value = "/position/info")
    public Position info(Long id) {
        Company company = getCurrentCompany();
        Position position = null;
        if (id == null) {
            position = new Position();
        } else {
            position = positionService.load(id);
        }
        return position;
    }

}
