package com.supcon.supfusion.organization.openapi.compatible;

import com.alibaba.fastjson.JSONObject;
import com.supcon.supfusion.framework.cloud.annotation.InternalApi;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import com.supcon.supfusion.organization.common.constants.Constants;
import com.supcon.supfusion.organization.common.exception.OrganizationErrorEnum;
import com.supcon.supfusion.organization.common.exception.OrganizationException;
import com.supcon.supfusion.organization.service.CompanyService;
import com.supcon.supfusion.organization.service.DepartmentService;
import com.supcon.supfusion.organization.service.PositionService;
import io.micrometer.core.instrument.util.StringUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * @Description: 公司开放接口
 * @Author:     HUNING
 * @CreateDate: 2020/5/25
 */
@Slf4j
@InternalApi(path = "/open-api/api/metadata/organization")
@Api(tags = "旧版本组织管理", description = "旧版本组织管理文档说明", hidden = true)
public class OrganizationOldOpenController extends BaseController {


    @Autowired
    private CompanyService companyService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private PositionService positionService;

    /**
     * 批量查询组织树(公司根节点)信息
     * @param keywords
     * @param page
     * @param per_page
     * @return
     */
    @GetMapping("/orgs/v1")
    @ApiOperation(value = "批量查询组织树(公司根节点)信息")
    public JSONObject listCompanies(@ApiParam(value = "匹配查询关键字", required = false) @RequestParam(value = "keywords", required = false) String keywords,
                                    @ApiParam(value = "页码", required = false) @RequestParam(value = "page", required = false) Integer page,
                                    @ApiParam(value = "每页条数", required = false) @RequestParam(value = "per_page", required = false) Integer per_page) {
        return companyService.listCompanies(keywords, page, per_page);
    }

    /**
     * 根据别名查询公司详情
     * @param orgName
     * @return
     */
    @GetMapping("/orgs/v1/{orgName}")
    @ApiOperation(value = "查询指定唯一标识别名的公司详情")
    public JSONObject queryCompanyDetail(@ApiParam("别名对应oldId") @PathVariable(value = "orgName") String orgName) {
        return companyService.queryCompanyDetail(orgName);
    }


    /**
     * 修改公司信息
     * @param orgName
     * @param body
     */
    @PutMapping("/orgs/v1/{orgName}")
    @ResponseStatus(HttpStatus.CREATED)
    @ApiOperation(value = "修改指定公司信息")
    public JSONObject updateCompany(@ApiParam("别名对应oldId") @PathVariable(value = "orgName") String orgName,
                              @RequestBody JSONObject body) {
        String tenantId = RpcContext.getContext().getTenantId();
        companyService.updateCompany(orgName, body, tenantId);
        JSONObject result = new JSONObject();
        result.put("code", 200);
        result.put("message", "success");
        return result;
    }

    /**
     * 新增部门或者岗位
     * @param body
     */
    @PostMapping("/orgs/v1/{orgName}/nodes")
    @ResponseStatus(HttpStatus.CREATED)
    @ApiOperation(value = "新增组织节点:部门,岗位")
    public JSONObject addDeptOrPosition(@RequestBody JSONObject body, @PathVariable("orgName") String orgName) {
        String tenantId = RpcContext.getContext().getTenantId();
        if (body == null || body.size() == 0) {
            throw new OrganizationException(400, Constants.PARAM_NECESSARY);
        }
        if (StringUtils.isBlank(body.getString("code")) || StringUtils.isBlank(body.getString("showName"))
                || StringUtils.isBlank(body.getString("orgType"))) {
            throw new OrganizationException(400, Constants.PARAM_NECESSARY);
        }
        body.put("root", orgName);
        if ("Department".equals(body.getString("orgType"))) {
            departmentService.addOldDepartment(body, tenantId);
        } else if ("Position".equals(body.getString("orgType"))) {
            positionService.addOldPosition(body, tenantId);
        }
        JSONObject result = new JSONObject();
        result.put("code", 200);
        result.put("message", "success");
        return result;
    }

    /**
     * 批量查询组织树所有节点(公司,部门,岗位),结果为平铺结构
     * @return
     */
    @GetMapping("/orgs/v1/{orgName}/nodes")
    @ApiOperation(value = "批量查询组织树所有节点(公司,部门,岗位),结果为平铺结构")
    public JSONObject queryOrganizationTileStruct(@ApiParam(value = "组织树别名", required = true) @PathVariable(value = "orgName", required = true) String orgName,
                                                  @ApiParam(value = "类型", required = false) @RequestParam(value = "orgType", required = false) String orgType,
                                                  @ApiParam(value = "匹配查询关键字", required = false) @RequestParam(value = "keywords", required = false) String keywords,
                                                  @ApiParam(value = "页码", required = false) @RequestParam(value = "page", required = false) Integer page,
                                                  @ApiParam(value = "每页条数", required = false) @RequestParam(value = "per_page", required = false) Integer per_page) {
        return companyService.queryOrganizationTileStruct(orgName, orgType, keywords, page, per_page);
    }

    /**
     * 批量删除组织节点(例如：公司，部门，岗位)
     * @param body
     */
    @PostMapping("/orgs/v1/{orgName}/nodes/delete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ApiOperation(value = "批量删除组织节点(例如：公司，部门，岗位)")
    public JSONObject batchDeleteOrg(@RequestBody JSONObject body) {
        String tenantId = RpcContext.getContext().getTenantId();
        companyService.batchDeleteOrg(body, tenantId);
        JSONObject result = new JSONObject();
        result.put("code", 200);
        result.put("message", "success");
        return result;
    }

    /**
     * 查询组织详情
     * @param orgName
     * @param nodeName
     * @return
     */
    @GetMapping("/orgs/v1/{orgName}/nodes/{nodeName}")
    @ApiOperation(value = "查询指定组织节点(例如：公司，部门，岗位)详细信息")
    public JSONObject queryOrgDetail(@ApiParam(value = "公司别名", required = true)@PathVariable("orgName") String orgName,
                                     @ApiParam(value = "部门或岗位别名", required = true)@PathVariable("nodeName") String nodeName) {
        return companyService.queryOrgDetail(orgName, nodeName);
    }


    /**
     * 查询组织详情根据编码
     * @param code
     * @param type
     * @return
     */
    @GetMapping("/orgs/v1/{code}/node")
    @ApiOperation(value = "查询指定组织节点(例如：公司，部门，岗位)详细信息")
    public JSONObject queryOrgDetailByCode(@ApiParam(value = "编码", required = true)@PathVariable("code") String code,
                                     @ApiParam(value = "部门或岗位别名", required = true)@RequestParam("type") String type) {
        return companyService.queryOrgDetailByCode(code, type);
    }

    /**
     * 修改组织信息
     * @param orgName
     * @param nodeName
     * @param body
     */
    @PutMapping("/orgs/v1/{orgName}/nodes/{nodeName}")
    @ResponseStatus(HttpStatus.CREATED)
    @ApiOperation(value = "修改指定组织节点(例如：公司，部门，岗位)")
    public JSONObject updateOrg(@ApiParam(value = "公司别名", required = true)@PathVariable("orgName") String orgName,
                          @ApiParam(value = "部门或岗位别名", required = true)@PathVariable("nodeName") String nodeName,
                          @RequestBody JSONObject body) {
        String tenantId = RpcContext.getContext().getTenantId();
        companyService.updateOrg(orgName, nodeName, body, tenantId);
        JSONObject result = new JSONObject();
        result.put("code", 200);
        result.put("message", "success");
        return result;
    }

    /**
     * 删除组织
     * @param orgName
     * @param nodeName
     */
    @DeleteMapping("/orgs/v1/{orgName}/nodes/{nodeName}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ApiOperation(value = "删除指定组织节点(例如：公司，部门，岗位)")
    public JSONObject deleteOrg(@ApiParam(value = "公司别名", required = true)@PathVariable("orgName") String orgName,
                          @ApiParam(value = "部门或岗位别名", required = true)@PathVariable("nodeName") String nodeName, HttpServletRequest request) {

        String token = request.getHeader("Authorization");
        String noBear = "";
        if (StringUtils.isNotBlank(token)) {
            noBear = token.substring(7,token.length());
        } else {
            throw new OrganizationException(OrganizationErrorEnum.NO_LOGIN);
        }
        String tenantId = request.getHeader("X-TENANT-ID");
        companyService.deleteOrg(orgName, nodeName, noBear, tenantId);
        JSONObject result = new JSONObject();
        result.put("code", 200);
        result.put("message", "success");
        return result;
    }


    /**
     * 查询关联节点
     * @param nodeName
     * @return
     */
    @GetMapping("/orgs/v1/{nodeName}/correlation")
    @ApiOperation(value = "查询关联节点")
    public JSONObject queryOrgCorrelation(@ApiParam(value = "部门或岗位别名", required = true)@PathVariable("nodeName") String nodeName,
                                          @ApiParam(value = "页码", required = false) @RequestParam(value = "page", required = false) Integer page,
                                          @ApiParam(value = "每页条数", required = false) @RequestParam(value = "per_page", required = false) Integer per_page) {
        return companyService.queryOrgCorrelation(nodeName, page, per_page);
    }

    @PostMapping(value = "/orgs/v1/{orgName}/nodes/{nodeName}")
    @ResponseStatus(HttpStatus.CREATED)
    @ApiOperation(value = "关联节点")
    public JSONObject addCorNode(@ApiParam(value = "公司别名", required = true)@PathVariable("orgName") String orgName,
                                 @ApiParam(value = "部门或岗位别名", required = true)@PathVariable("nodeName") String nodeName,
                                 @RequestBody JSONObject body) {
        companyService.addCorNode(nodeName, body);
        JSONObject result = new JSONObject();
        result.put("code", 200);
        result.put("message", "success");
        return result;
    }

    @PostMapping(value = "/orgs/v1/{orgName}/nodes/{nodeName}/delete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ApiOperation(value = "关联节点")
    public JSONObject delCorNode(@ApiParam(value = "公司别名", required = true)@PathVariable("orgName") String orgName,
                                 @ApiParam(value = "部门或岗位别名", required = true)@PathVariable("nodeName") String nodeName,
                                 @RequestBody JSONObject body) {
        JSONObject result = new JSONObject();
        result.put("code", 200);
        result.put("message", "success");
        return result;
    }
    /**
     * 查询组织关联人员
     * @param nodeName
     * @return
     */
    @GetMapping("/orgs/v1/{nodeName}/person")
    @ApiOperation(value = "批量查询组织(例如：公司，部门，岗位)关联的人员")
    public JSONObject queryCorrelationPerson(@ApiParam(value = "部门或岗位别名", required = true)@PathVariable("nodeName") String nodeName,
                                             @ApiParam(value = "模糊匹配", required = false) @RequestParam(value = "keywords", required = false) String keywords,
                                             @ApiParam(value = "页码", required = false) @RequestParam(value = "page", required = false) Integer page,
                                             @ApiParam(value = "每页条数", required = false) @RequestParam(value = "per_page", required = false) Integer per_page) {
        return companyService.queryCorrelationPerson(nodeName, keywords, page, per_page);
    }

    @PostMapping("/orgs/v1/{nodeName}/person")
    @ResponseStatus(HttpStatus.CREATED)
    @ApiOperation(value = "为组织(例如：公司，部门，岗位)新增关联的人员")
    public void addCorrelationForOrg(@ApiParam(value = "部门或岗位别名", required = true)@PathVariable("nodeName") String nodeName, @RequestBody JSONObject body) {
        if (StringUtils.isBlank(nodeName) || !"Position".equals(nodeName)) {
            return;
        }
        companyService.addCorrelationForOrg(nodeName, body);
    }

    @PostMapping("/orgs/v1/{nodeName}/person/delete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ApiOperation(value = "批量删除组织(例如：公司，部门，岗位)与人员的关联关系")
    public JSONObject deleteCorrelationPerson(@ApiParam(value = "部门或岗位别名", required = true)@PathVariable("nodeName") String nodeName, @RequestBody JSONObject body) {
        JSONObject result = new JSONObject();
        result.put("code", 200);
        result.put("message", "success");
        if (StringUtils.isBlank(nodeName) || !"Position".equals(nodeName)) {
            return result;
        }
        companyService.deleteCorrelationPerson(nodeName, body);

        return result;
    }

    /**
     * 查询组织树
     * @param orgName
     * @param path
     * @param deep
     * @param nodeName
     * @return
     */
    @GetMapping("/orgs/v1/{orgName}/tree")
    @ApiOperation(value = "查询树形结构")
    public JSONObject getOrgTree(@ApiParam(value = "公司别名", required = true)@PathVariable("orgName") String orgName,
                                 @ApiParam(value = "路径", required = false) @RequestParam(value = "path", required = false) String path,
                                 @ApiParam(value = "深度", required = false) @RequestParam(value = "deep", required = false) String deep,
                                 @ApiParam(value = "部门或岗位", required = false) @RequestParam(value = "nodeName", required = false) String nodeName,
                                 @ApiParam(value = "查询的组织类型", required = false) @RequestParam(value = "orgType", required = false) String orgType,
                                 HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        String noBear = "";
        if (StringUtils.isNotBlank(token)) {
            noBear = token.substring(7,token.length());
        }
        String tenantId = request.getHeader("X-TENANT-ID");
        JSONObject treeJson = companyService.getOrgTree(orgName, path, deep, nodeName, noBear, orgType, tenantId);
        return treeJson;
    }

    @GetMapping("/orgs/v1/{nodeName}/persons/chose")
    @ApiOperation(value = "人员选择器")
    public JSONObject getPersonChose(@ApiParam(value = "部门或岗位别名", required = true)@PathVariable("nodeName") String nodeName,
                                     @ApiParam(value = "类型", required = true) @RequestParam(value = "type", required = true) String type,
                                     @ApiParam(value = "是否查询全部", required = false) @RequestParam(value = "isAll", required = false) Boolean isAll,
                                     @ApiParam(value = "模糊匹配", required = false) @RequestParam(value = "keywords", required = false) String keywords,
                                     @ApiParam(value = "查询当前关联的人员", required = true) @RequestParam(value = "curNodeName", required = true) String curNodeName,
                                     @ApiParam(value = "页码", required = false) @RequestParam(value = "page", required = false) Integer page,
                                     @ApiParam(value = "每页条数", required = false) @RequestParam(value = "per_page", required = false) Integer per_page, HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        String noBear = "";
        if (StringUtils.isNotBlank(token)) {
            noBear = token.substring(7,token.length());
        }
        String tenantId = request.getHeader("X-TENANT-ID");
        JSONObject choseJson = companyService.getPersonChose(nodeName, curNodeName, type, isAll, keywords, page, per_page, noBear, tenantId);
        return choseJson;
    }

}

