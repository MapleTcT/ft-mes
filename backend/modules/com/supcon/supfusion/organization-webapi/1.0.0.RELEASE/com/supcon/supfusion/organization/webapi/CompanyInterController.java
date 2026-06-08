package com.supcon.supfusion.organization.webapi;

import com.alibaba.fastjson.JSONObject;
import com.supcon.supfusion.auth.api.dto.UserDetailDTO;
import com.supcon.supfusion.framework.cloud.annotation.InternalApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.framework.cloud.common.context.UserContext;
import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import com.supcon.supfusion.organization.common.config.OrganizationProperties;
import com.supcon.supfusion.organization.common.constants.Constants;
import com.supcon.supfusion.organization.dao.po.company.CompanyPO;
import com.supcon.supfusion.organization.manager.OrganizationAdapter;
import com.supcon.supfusion.organization.service.CompanyService;
import com.supcon.supfusion.organization.service.CompanyTagService;
import com.supcon.supfusion.organization.service.bo.company.CompanyKeywordBO;
import com.supcon.supfusion.organization.service.bo.company.CompanyTagBO;
import com.supcon.supfusion.organization.service.bo.person.PersonDetailBO;
import com.supcon.supfusion.organization.webapi.vo.company.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


/**
 * @Description: 公司开放接口
 * @Author:     HUNING
 * @CreateDate: 2020/5/25
 */
@Slf4j
@InternalApi(path = HttpConstants.URL_INTERNALAPI + HttpConstants.URL_SPLITER + "organization" + HttpConstants.URL_SPLITER + "v1")
@Api(tags = "公司管理", description = "公司管理文档说明", hidden = true)
@Validated
public class CompanyInterController extends BaseController {

    Logger logger = LoggerFactory.getLogger(CompanyInterController.class);

    @Autowired
    private OrganizationProperties organizationProperties;

    @Autowired
    private CompanyService companyService;

    @Autowired
    private CompanyTagService companyTagService;

    @Autowired
    private OrganizationAdapter organizationAdapter;

    @GetMapping(value = {"/companies", "/companies/ref"})
    @ResponseBody
    public ListResult<CompanyVO> companies() {
        UserContext userContext = UserContext.getUserContext();
        Long companyId = null;
        if (userContext != null && userContext.getCompanyId() != null) {
            companyId = userContext.getCompanyId();
        }

        List<CompanyPO> comPOS = companyService.listCompanies();
        List comVOS = new ArrayList(comPOS.size());
        for (CompanyPO comPO : comPOS) {
            CompanyVO comVO = new CompanyVO();
            BeanUtils.copyProperties(comPO, comVO);
            List<String> tags = companyTagService.getCompanyTagById(comVO.getId());
            comVO.setTags(tags);
            if (comVO.getId().equals(Constants.DEFAULT_COMPANY_ID)) {
                comVOS.add(0, comVO);
            } else {
                comVOS.add(comVO);
            }
        }

        return new ListResult<CompanyVO>(comVOS);
    }

    /**
     * 查询公司以及下级
     * @param companyId
     * @return
     */
    @GetMapping(value = {"/companies/sub", "/companies/sub/ref"})
    @ResponseBody
    @ApiOperation(value = "查询当前公司及下级公司,如果传companyId就以companyId为准,否则以登录用户选择的公司为准")
    public ListResult<CompanyResultVO> supCompanies(@ApiParam(value = "顶层公司id") @RequestParam(value = "companyId", required = false) Long companyId,
                                              @ApiParam(value = "选中的id") @RequestParam(value = "selectCompanyId", required = false) Long selectCompanyId,
                                              @ApiParam(value = "模糊匹配关键字") @RequestParam(value = "keyword", required = false) String keyword
    ) {
        UserContext userContext = UserContext.getUserContext();
        List comVOS = new ArrayList();
        if (companyId == null) {
            if (userContext == null || userContext.getCompanyId() == null) {
                return new ListResult<CompanyResultVO>(comVOS);
            } else {
                companyId = userContext.getCompanyId();
            }
         }
        List<CompanyPO> comPOS = companyService.getSubCompanies(companyId, selectCompanyId, keyword);
        for (CompanyPO comPO : comPOS) {
            CompanyResultVO comVO = new CompanyResultVO();
            BeanUtils.copyProperties(comPO, comVO);
            List<UserDetailDTO> users = organizationAdapter.queryCompanyUsers(comVO.getId());
            List<CompanyUserVO> comUsers = new ArrayList<>();
            if (users != null && users.size() > 0) {

                users.stream().forEach(user -> {
                    CompanyUserVO companyUserVO = new CompanyUserVO();
                    companyUserVO.setUserName(user.getUserName());
                    companyUserVO.setUserType(user.getUserType());
                    comUsers.add(companyUserVO);
                });
            }
            comVO.setUsers(comUsers);
            List<String> tags = companyTagService.getCompanyTagById(comVO.getId());
            comVO.setTags(tags);
            if (companyId != null && companyId.equals(comPO.getId())) {
                comVO.setParentId(null);
                comVOS.add(0, comVO);
            } else {
                comVOS.add(comVO);
            }
        }
        return new ListResult<CompanyResultVO>(comVOS);
    }
    @PostMapping("/company")
    public void addCompany(@Validated @RequestBody CompanyVO comVO) {
        String tenantId = RpcContext.getContext().getTenantId();
        CompanyPO comPO = new CompanyPO();
        BeanUtils.copyProperties(comVO, comPO);
        companyService.addCompany(comPO, comVO.getTags(), comVO.getUserName(), comVO.getPassword(), tenantId);
    }

    @PutMapping("/company")
    public void updateCompany(@Validated @RequestBody CompanyUpdateVO comVO) {
        String tenantId = RpcContext.getContext().getTenantId();
        CompanyPO comPO = new CompanyPO();
        BeanUtils.copyProperties(comVO, comPO);
        companyService.saveOrUpdateCom(comPO, comVO.getTags(), tenantId);

    }

    @GetMapping("/company/{id}")
    @ResponseBody
    public Result<CompanyResultVO> company(@PathVariable Long id) {
        CompanyResultVO comVO = new CompanyResultVO();
        CompanyPO comPO = companyService.findCompany(id);
        List<UserDetailDTO> users = organizationAdapter.queryCompanyUsers(id);
        List<CompanyUserVO> comUsers = new ArrayList<>();
        if (users != null && users.size() > 0) {

            users.stream().forEach(user -> {
                CompanyUserVO companyUserVO = new CompanyUserVO();
                companyUserVO.setUserName(user.getUserName());
                companyUserVO.setUserType(user.getUserType());
                comUsers.add(companyUserVO);
            });
        }
        List<String> tags = companyTagService.getCompanyTagById(id);
        Optional.ofNullable(comPO).ifPresent(com -> BeanUtils.copyProperties(com, comVO));
        comVO.setUsers(comUsers);
        comVO.setTags(tags);
        return Result.custom().data(comVO).build();
    }

    @DeleteMapping("/company/{id}")
    public void delCompany(@PathVariable Long id) {
        String tenantId = RpcContext.getContext().getTenantId();
        companyService.delCompany(id, tenantId);
    }

    @GetMapping(value = "/company/tag")
    @ApiOperation(value = "查询公司标签", notes = "查询公司标签")
    List<CompanyTagVO> queryCompanyTags(@ApiParam(value = "模糊匹配关键字") @RequestParam(value = "keyword", required = false) String keyword) {
        List<CompanyTagBO> results = companyTagService.getCompanyTags(keyword);
        List<CompanyTagVO> list = new ArrayList<CompanyTagVO>();
        results.stream().forEach(tag -> {
            CompanyTagVO companyTagVO = new CompanyTagVO();
            BeanUtils.copyProperties(tag, companyTagVO);
            list.add(companyTagVO);
        });
        return list;
    }

    /**
     * 查询公司模式,是但公司模式还是多公司模式
     * @return
     */
    @GetMapping("/company/model")
    Result<CompanyModelVO> queryCompanyModel() {
        CompanyModelVO companyModelVO = new CompanyModelVO();
        companyModelVO.setModel(organizationProperties.getOrgType());
        return new Result<CompanyModelVO>(companyModelVO);
    }

    @GetMapping(value = "/company/keyword")
    @ResponseBody
    @ApiOperation(value = "公司模糊查询列表", notes = "公司模糊查询列表")
    ListResult<CompanyKeywordVO> queryPositionByKeyword (@ApiParam(value = "模糊匹配关键字") @RequestParam(value = "keyword", required = false) String keyword,
                                                         @ApiParam(value = "顶层公司id") @RequestParam(value = "companyId", required = true) Long companyId) {
        List<CompanyKeywordBO> list = companyService.queryCompaniesByKeyword(keyword, companyId);
        List<CompanyKeywordVO> results = new ArrayList<CompanyKeywordVO>();
        if (list == null || list.size() == 0) {
            return new ListResult<CompanyKeywordVO>(results);
        }

        list.stream().forEach(bo -> {
            CompanyKeywordVO companyKeywordVO = new CompanyKeywordVO();
            BeanUtils.copyProperties(bo, companyKeywordVO);
            results.add(companyKeywordVO);
        });
        return new ListResult<CompanyKeywordVO>(results);
    }
    
    /**
     * 根据公司id查询公司信息
     * @param id
     * @param includes
     * @return
     */
    @GetMapping(value = "/company/common/get")
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(value = "根据公司id查询公司信息", notes = "根据公司id查询公司信息", tags = {"baseService"})
    public Result<JSONObject> getCompanyById(@ApiParam(value = "公司id", required = true) @NotNull(message = Constants.COM_PARAM_ID_NOTNULL) @RequestParam(value = "id", required = true) Long id,
                                         @ApiParam(value = "返回哪些字段,逗号分割", required = false)  @RequestParam(value = "includes", required = false) String includes) {
        JSONObject companyInfo = companyService.getCompanyById(id, includes);
        return new Result<JSONObject>(companyInfo);
    }

    /**
     * 分页加载公司列表
     * @param current
     * @param pageSize
     * @return
     */
    @GetMapping(value = "/companies/pages")
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(value = "加载公司列表", notes = "加载公司列表")
    public Result<PageResult<CompanyResultVO>> loadCompanies(@ApiParam(value = "当前页码", required = true) @Min(value = -1, message = Constants.PAGE_CURRENT_ERROR) @RequestParam("current") Integer current,
                                                             @ApiParam(value = "每页条数", required = true) @Min (value = -1, message = Constants.PAGE_PAGESIZE_ERROR) @RequestParam("pageSize") Integer pageSize,
                                                             @ApiParam(value = "时间", required = false) @RequestParam(value = "fromTime", required = false) Long fromTime) {
        PageResult<CompanyPO> pageResult = companyService.loadCompanies(current, pageSize, fromTime);
        if (pageResult == null || pageResult.getPagination().getTotal() == 0) {
            return new Result<>(new PageResult<>(null, 0, pageSize, current));
        }
        List<CompanyResultVO> vos = new ArrayList<>();
        pageResult.getList().stream().forEach(comPo -> {
            CompanyResultVO companyResultVO = new CompanyResultVO();
            BeanUtils.copyProperties(comPo, companyResultVO);
            vos.add(companyResultVO);
        });
        return new Result<>(new PageResult<>(vos, pageResult.getPagination().getTotal(), pageResult.getPagination().getPageSize(), pageResult.getPagination().getCurrent()));
    }

    /**
     * 查询公司关联的用户信息
     */
    @GetMapping(value = "/companies/users")
    @ResponseBody
    @ApiOperation(value = "查询公司关联的用户信息", notes = "查询公司关联的用户信息")
    public PageResult<CompanyUserDetailVO> getCompanyUsers(@ApiParam(value = "公司id", required = false) @RequestParam(value = "companyId", required = false) String companyId,
                                                           @ApiParam(value = "模糊匹配关键词（支持对用户名或人员名称模糊查询）", required = false) @RequestParam(value = "keyword", required = false) String keyword,
                                                           @ApiParam(value = "是否只查询包含用户的人员") @RequestParam(value = "onlyUser", required = false) Boolean onlyUser,
                                                           @ApiParam(value = "当前页码", required = false) @Min(value = 1, message = Constants.PAGE_CURRENT_ERROR) @RequestParam(value = "current", defaultValue = "1", required = false) Integer current,
                                                           @ApiParam(value = "每页条数", required = false) @Min(value = 1, message = Constants.PAGE_PAGESIZE_ERROR) @Max(value = 500, message = Constants.PAGE_PAGESIZE_MAX_ERROR) @RequestParam(value = "pageSize", defaultValue = "20", required = false) Integer pageSize) {
        PageResult<PersonDetailBO> result = companyService.getCompanyUsers(companyId, keyword, onlyUser, current, pageSize);
        List<CompanyUserDetailVO> companyUserDetailVOS = new ArrayList<>();
        for (PersonDetailBO personDetailBO : result.getList()) {
            CompanyUserDetailVO companyUserDetailVO = new CompanyUserDetailVO();
            BeanUtils.copyProperties(personDetailBO, companyUserDetailVO);
            companyUserDetailVO.setId(personDetailBO.getUserId());
            companyUserDetailVO.setPersonId(personDetailBO.getId());
            companyUserDetailVOS.add(companyUserDetailVO);
        }
        return new PageResult<>(companyUserDetailVOS, result.getPagination().getTotal(),
                result.getPagination().getPageSize(), result.getPagination().getCurrent());
    }

    /**
     * 查询公司关联的人员信息
     */
    @GetMapping(value = "/companies/persons")
    @ResponseBody
    @Validated
    @ApiOperation(value = "查询公司关联的人员信息", notes = "查询公司关联的人员信息")
    public PageResult<CompanyPersonDetailVO> getCompanyPersons(@ApiParam(value = "公司id", required = false) @RequestParam(value = "companyId", required = false) String companyId,
                                                               @ApiParam(value = "模糊匹配关键词（支持对用户名或人员名称模糊查询）", required = false) @RequestParam(value = "keyword", required = false) String keyword,
                                                               @ApiParam(value = "当前页码", required = true) @Min(value = 1, message = Constants.PAGE_CURRENT_ERROR) @RequestParam(value = "current", defaultValue = "1", required = true) Integer current,
                                                               @ApiParam(value = "每页条数", required = false) @Min(value = 1, message = Constants.PAGE_PAGESIZE_ERROR) @Max(value = 500, message = Constants.PAGE_PAGESIZE_MAX_ERROR) @RequestParam(value = "pageSize", defaultValue = "20", required = false) Integer pageSize) {
        PageResult<PersonDetailBO> result = companyService.getCompanyUsers(companyId, keyword, false, current, pageSize);
        List<CompanyPersonDetailVO> companyPersonDetailVOS = new ArrayList<>();
        for (PersonDetailBO personDetailBO : result.getList()) {
            CompanyPersonDetailVO companyPersonDetailVO = new CompanyPersonDetailVO();
            BeanUtils.copyProperties(personDetailBO, companyPersonDetailVO);
            companyPersonDetailVOS.add(companyPersonDetailVO);
        }
        return new PageResult<>(companyPersonDetailVOS, result.getPagination().getTotal(),
                result.getPagination().getPageSize(), result.getPagination().getCurrent());
    }

}

