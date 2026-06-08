package com.supcon.supfusion.rbac.webapi;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supcon.supfusion.framework.cloud.annotation.InternalApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.context.UserContext;
import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import com.supcon.supfusion.framework.scaffold.dbp.constants.DbType;
import com.supcon.supfusion.framework.scaffold.dbp.util.DataId;
import com.supcon.supfusion.framework.scaffold.dbp.util.DbStringUtil;
import com.supcon.supfusion.organization.api.dto.CompanyResultDTO;
import com.supcon.supfusion.rbac.common.Contants.Constants;
import com.supcon.supfusion.rbac.common.exception.RoleErrorEnum;
import com.supcon.supfusion.rbac.common.exception.RoleException;
import com.supcon.supfusion.rbac.common.utils.StringUtils;
import com.supcon.supfusion.rbac.dao.field.RoleField;
import com.supcon.supfusion.rbac.dao.field.TagField;
import com.supcon.supfusion.rbac.dao.po.RolePO;
import com.supcon.supfusion.rbac.dao.po.TagPO;
import com.supcon.supfusion.rbac.manager.IOrganizationAdapter;
import com.supcon.supfusion.rbac.service.IRoleMneCodeService;
import com.supcon.supfusion.rbac.service.IRoleService;
import com.supcon.supfusion.rbac.service.ITagService;
import com.supcon.supfusion.rbac.urlscan.annotation.MenuOperateCode;
import com.supcon.supfusion.rbac.webapi.vo.role.*;
import com.supcon.supfusion.rbac.webapi.vo.tag.TagVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

import org.apache.ibatis.annotations.Param;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 角色表 前端控制器
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-05
 */
@Slf4j
@InternalApi(path = HttpConstants.URL_INTERNALAPI + HttpConstants.URL_SPLITER + "rbac" + HttpConstants.URL_SPLITER + "v1")
@Validated
@Api(tags = "角色相关接口")
public class RoleInterApiController extends BaseController {

    @Autowired
    private IRoleService roleService;
    @Autowired
    private ITagService tagService;
    @Autowired
    private IOrganizationAdapter organizationAdapter;
    @Autowired
    private IRoleMneCodeService roleMneCodeService;
    @Autowired
    private DataId dataId;
    @Autowired
    private DbStringUtil dbStringUtil;

    /**
     * @description: 查询角色树不分页
     * @param: keyword 通过关键字（编码、名称）模糊查询
     * @param: tag 标签
     * @return: java.util.List<java.util.Map < java.lang.String, java.lang.Object>>
     * @author: 袁阳
     * @date: 2020/6/9
     */
    @MenuOperateCode("queryRole")
    @GetMapping("/roles/tree")
    @ApiOperation(value = "查询角色树")
    @ApiImplicitParams({
            @ApiImplicitParam(name="tag",value="标签",required=false,paramType="query"),
            @ApiImplicitParam(name="keyword",value="关键字，模糊查询角色名和角色编码",required=false,paramType="query"),
            @ApiImplicitParam(name="cid",value="公司ID",required=false,paramType="query"),
    })
    public ListResult<Map<String, Object>> findRoleTree(@RequestParam(required = false, value = "keyword") String keyword, @RequestParam(required = false, value = "tag") String tag, @RequestParam(required = false, value = "cid") Long cid) {
        log.info("GET:/roles/tree===params:tag={},keyword={}=,cid={}***********************************",tag,keyword,cid);
        List<Map<String, Object>> roleTree = roleService.getRoleTreeSingleCompany(keyword, tag,cid);
        //构建vo
        roleTree.forEach(map -> {
            List<RolePO> children = (List) map.get("children");
            //把children里的RolePO转成roleTreeSingleCompanyVO
            List<RoleTreeSingleCompanyVO> collect = children.stream().map(rolePO -> {
                RoleTreeSingleCompanyVO roleTreeSingleCompanyVO = new RoleTreeSingleCompanyVO();
                BeanUtils.copyProperties(rolePO, roleTreeSingleCompanyVO);
                return roleTreeSingleCompanyVO;
            }).collect(Collectors.toList());
            map.put("children",collect);
        });
        log.info("GET:/roles/tree===response:roleTree={}***********************************",roleTree);
        return new ListResult<>(roleTree);
    }

    @GetMapping("/roles/tree/notag")
    @ApiOperation(value = "查询角色树无标签")
    @ApiImplicitParams({
            @ApiImplicitParam(name="tag",value="标签",required=false,paramType="query"),
            @ApiImplicitParam(name="keyword",value="关键字，模糊查询角色名和角色编码",required=false,paramType="query"),
            @ApiImplicitParam(name="cid",value="公司ID",required=false,paramType="query"),
    })
    public ListResult<RoleVO> findRoleTreeNoTag(@RequestParam(required = false, value = "keyword") String keyword, @RequestParam(required = false, value = "cid") Long cid) {
        log.info("GET:/roles/tree/notag=====params:keyword={}=,cid={}***********************************",keyword,cid);
        if (ObjectUtils.isEmpty(cid)){
            cid = UserContext.getUserContext().getCompanyId();
        }
        List<RolePO> roles = roleService.getRoleTreeNoTag(keyword,cid);
        List<RoleVO> roleVOS = roles.stream().map(rolePO -> {
            RoleVO roleVO = new RoleVO();
            BeanUtils.copyProperties(rolePO, roleVO);
            return roleVO;
        }).collect(Collectors.toList());
        log.info("GET:/roles/tree/notag=====response:roleVOS={}***********************************",roleVOS);
        return new ListResult<>(roleVOS);
    }

    /**
     * @description: 查询角色树不分页
     * @param: keyword 通过关键字（编码、名称）模糊查询
     * @param: tag 标签
     * @return: java.util.List<java.util.Map < java.lang.String, java.lang.Object>>
     * @author: 袁阳
     * @date: 2020/6/9
     */
    @GetMapping("/roles/treeSingleCompany")
    @ApiOperation(value = "查询角色树,查询单个公司")
    @ApiImplicitParams({
            @ApiImplicitParam(name="tag",value="标签",required=false,paramType="query"),
            @ApiImplicitParam(name="keyword",value="关键字，模糊查询角色名和角色编码",required=false,paramType="query"),
            @ApiImplicitParam(name="cid",value="公司ID",required=true,paramType="query"),
    })
    public Result<RoleTreeVO> treeSingleCompany(@RequestParam(required = false) String keyword, @RequestParam(required = false) String tag, @RequestParam("companyId") Long cid) {
        log.info("GET:/roles/treeSingleCompany==params:tag={},keyword={}=,cid={}***********************************",tag,keyword,cid);
        List<Map<String, Object>> roleTree = roleService.getRoleTreeSingleCompany(keyword, tag, cid);
        //如果单查一个公司的角色 就返回Result 如果没有限定条件 则查所有 返回ListResult
        RoleTreeVO roleTreeVO = new RoleTreeVO();
        Result<CompanyResultDTO> companyData = organizationAdapter.findCompany(cid);
        BeanUtils.copyProperties(companyData.getData(),roleTreeVO);
        //把roleTree中children的rolePO转成roleTreeSingleCompanyVO
        roleTree.forEach(map -> {
            List<RolePO> children = (List) map.get("children");
            //把children里的RolePO转成roleTreeSingleCompanyVO
            List<RoleTreeSingleCompanyVO> collect = children.stream().map(rolePO -> {
                RoleTreeSingleCompanyVO roleTreeSingleCompanyVO = new RoleTreeSingleCompanyVO();
                BeanUtils.copyProperties(rolePO, roleTreeSingleCompanyVO);
                return roleTreeSingleCompanyVO;
            }).collect(Collectors.toList());
            map.put("children",collect);
        });
        //转成VO
        roleTreeVO.setChildren(roleTree);
        log.info("GET:/roles/treeSingleCompany===response:roleTree={}***********************************",roleTree);
        return Result.custom().data(roleTreeVO).build();
    }

    @GetMapping("/roles/treeSingleCompany/notag")
    @ApiOperation(value = "查询角色树,查询单个公司,无标签")
    @ApiImplicitParams({
            @ApiImplicitParam(name="tag",value="标签",required=false,paramType="query"),
            @ApiImplicitParam(name="keyword",value="关键字，模糊查询角色名和角色编码",required=false,paramType="query"),
            @ApiImplicitParam(name="cid",value="公司ID",required=true,paramType="query"),
    })
    public Result<RoleTreeVO> treeSingleCompanyNoTag(@RequestParam(required = false) String keyword, @RequestParam("companyId") Long cid) {
        log.info("GET:/roles/treeSingleCompany/notag====params:keyword={}=,cid={}***********************************",keyword,cid);
        List<RolePO> roles = roleService.getRoleTreeNoTag(keyword, cid);
        //如果单查一个公司的角色 就返回Result 如果没有限定条件 则查所有 返回ListResult
        RoleTreeNoTagVO roleTreeNoTagVO = new RoleTreeNoTagVO();
        Result<CompanyResultDTO> companyData = organizationAdapter.findCompany(cid);
        BeanUtils.copyProperties(companyData.getData(),roleTreeNoTagVO);
        //把roleTree中children的rolePO转成roleTreeSingleCompanyVO
        List<RoleVO> roleVOS = roles.stream().map(rolePO -> {
            RoleVO roleVO = new RoleVO();
            BeanUtils.copyProperties(rolePO, roleVO);
            return roleVO;
        }).collect(Collectors.toList());
        //转成VO
        roleTreeNoTagVO.setChildren(roleVOS);
        log.info("GET:/roles/treeSingleCompany/notag===response:roleTreeNoTagVO={}***********************************",roleTreeNoTagVO);
        return Result.custom().data(roleTreeNoTagVO).build();
    }

    /**
     * @description: 保存
     * @param: role 前台传过来的角色 parentId、code、name、roleType、description、tags
     * @return: boolean
     * @author: 袁阳
     * @date: 2020/6/9
     */
    @PostMapping("/role")
    @ApiOperation(value = "新增角色")
    public void save(@RequestBody RoleVO role) {
        log.info("POST:/role===requestbody:role={}***********************************",role);
        if (null == role.getId()) {
            if (role.getCode().endsWith("_ADMINISTRATOR") || role.getCode().endsWith("_SECURITY_ADMIN") || role.getCode().endsWith("_SECURITY_AUDITOR")) {
                throw new RoleException(RoleErrorEnum.CODE_KEYWORD_ERROR);
            }
        }
        RolePO rolePO = new RolePO();
        BeanUtils.copyProperties(role, rolePO);
        roleService.saveRole(rolePO, role.getTags());
    }

    /**
     * @description: 角色修改
     * @param: role
     * @return: void
     * @author: 袁阳
     * @date: 2020/6/9
     */
    @PutMapping("/role")
    @ApiOperation(value = "修改角色")
    public void update(@RequestBody RoleVO role){
        log.info("PUT:/role===requestbody:role={}***********************************",role);
        RolePO rolePO = new RolePO();
        BeanUtils.copyProperties(role,rolePO);
        roleService.update(rolePO,role.getDeleteIds(),role.getTags());
    }

    /**
     * @description: 角色查找 单个
     * @param: code 角色code
     * @return: com.supcon.supfusion.framework.cloud.common.result.Result
     * @author: 袁阳
     * @date: 2020/6/9
     */
    @MenuOperateCode("queryRole")
    @GetMapping(value= {"/role/findOne","/role/findOne/ref"})
    @ApiOperation(value = "根据编码查询角色")
    @ApiImplicitParams({
            @ApiImplicitParam(name="code",value="角色编码",required=true,paramType="query"),
    })
    public Result<RoleFindOneVO> findOne(@RequestParam String code) {
        log.info("GET:/role/findOne,/role/findOne/ref===params:code={}***********************************",code);
        QueryWrapper<RolePO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(RoleField.code, code);
        RolePO rolePO = roleService.getOne(queryWrapper);
        RoleFindOneVO roleVO = new RoleFindOneVO();
        //获取父节点信息
        //重复利用 queryWrapper
        if (!ObjectUtils.isEmpty(rolePO)){
            BeanUtils.copyProperties(rolePO, roleVO);
            QueryWrapper<TagPO> tagPOQueryWrapper = new QueryWrapper<>();
            tagPOQueryWrapper.eq(TagField.objectid, rolePO.getId());
            tagPOQueryWrapper.orderByAsc(TagField.id);
            List<TagPO> tagPOS = tagService.list(tagPOQueryWrapper);
            roleVO.setTags(tagPOS.stream().map(tagPO -> {
                TagVO tagVO = new TagVO();
                BeanUtils.copyProperties(tagPO,tagVO);
                return tagVO;
            }).collect(Collectors.toList()));
            log.info("GET:/role/findOne,/role/findOne/ref===response:roleVO={}***********************************",roleVO);
            return Result.custom().data(roleVO).build();
        }
        return Result.data(null);
    }

    /**
     * @description: 删除角色
     * @param: codes 角色codes
     * @return: List<Object> 删除的角色id
     * @author: 袁阳
     * @date: 2020/6/8
     */
    @MenuOperateCode("deleteRole")
    @DeleteMapping("/role/{codes}")
    @ApiOperation(value = "批量删除角色")
    @ApiImplicitParams({
            @ApiImplicitParam(name="codes",value="角色编码，逗号分隔",required=true,paramType="path"),
    })
    public void delete(@PathVariable String codes) {
        log.info("DELETE:/role/{codes}====params:codes={}***********************************",codes);
        roleService.deleteRoles(codes);
    }

    /**
     * @description: 根据关键字查询角色
     * @param: keyword
     * @return: com.supcon.supfusion.framework.cloud.common.result.ListResult<com.supcon.supfusion.rbac.webapi.vo.role.RoleVO>
     * @author: 袁阳
     * @date: 2020/6/11
     */
    @MenuOperateCode("queryRole")
    @GetMapping({"/roles/findRoleByKeyword","/roles/findRoleByKeyword/ref"})
    @ApiOperation(value = "根据关键字查询角色")
    @ApiImplicitParams({
            @ApiImplicitParam(name="keyword",value="关键字，模糊查询角色编码和角色名",required=true,paramType="query"),
    })
    public ListResult<RoleFindByKeywordVO> findRoleByKeyword(@RequestParam(value = "keyword",required = false) String keyword){
        log.info("GET:/roles/findRoleByKeyword,/roles/findRoleByKeyword/ref==params:keyword={}***********************************",keyword);

        QueryWrapper<RolePO> queryWrapper = new QueryWrapper<>();
        queryWrapper.and(rolePOQueryWrapper -> rolePOQueryWrapper.like(!ObjectUtils.isEmpty(keyword),RoleField.name,keyword));
        queryWrapper.eq(RoleField.cid,UserContext.getUserContext().getCompanyId());
        List<RolePO> rolePOS = roleService.list(queryWrapper);
        QueryWrapper<TagPO> tagPOQueryWrapper = new QueryWrapper<>();
        //如果有 则返回 没有 则返回空数组
        if (!ObjectUtils.isEmpty(rolePOS)){
            tagPOQueryWrapper.in(TagField.objectid,rolePOS.stream().map(RolePO::getId).collect(Collectors.toList()));
            tagPOQueryWrapper.select(TagField.id,TagField.name,TagField.objectid,TagField.cid);
            tagPOQueryWrapper.groupBy(TagField.id,TagField.name,TagField.objectid,TagField.cid);
            tagPOQueryWrapper.orderByAsc(TagField.id);
            List<TagPO> tagPOS = tagService.list(tagPOQueryWrapper);
            List<RoleFindByKeywordVO> collect = rolePOS.stream().map(rolePO -> {
                RoleFindByKeywordVO roleVO = new RoleFindByKeywordVO();
                BeanUtils.copyProperties(rolePO, roleVO);
                //找到对应的tag 设置tag
                if (!ObjectUtils.isEmpty(tagPOS)) {
                    List<TagPO> tagPOList = tagPOS.stream().filter(tagPO -> tagPO.getObjectid().equals(rolePO.getId())).collect(Collectors.toList());
                    List<TagVO> tagVOS = JSONArray.parseArray(JSON.toJSONString(tagPOList), TagVO.class);
                    roleVO.setTags(tagVOS);
                }
                return roleVO;
            }).collect(Collectors.toList());
            log.info("GET:/roles/findRoleByKeyword,/roles/findRoleByKeyword/ref==params:collect={}***********************************",collect);
            return new ListResult<>(collect);
        }else {
            return new ListResult<>(new ArrayList<>());
        }
    }

    @GetMapping({"/roles/findRoleByKeyword/notag","/roles/findRoleByKeyword/notag/ref"})
    @ApiOperation(value = "根据关键字查询角色")
    @ApiImplicitParams({
            @ApiImplicitParam(name="keyword",value="关键字，模糊查询角色编码和角色名",required=true,paramType="query"),
    })
    public ListResult<RoleFindByKeywordVO> findRoleByKeywordNoTag(@RequestParam(value = "keyword",required = false) String keyword){
        log.info("GET:/roles/findRoleByKeyword/notag,/roles/findRoleByKeyword/notag/ref===params:keyword={}***********************************",keyword);
        QueryWrapper<RolePO> queryWrapper = new QueryWrapper<>();
        if (DbType.ORACLE.equals(dataId.getDataId())){
            queryWrapper.apply("name like {0} escape '\\'", Constants.SQL_LIKE_CHAR + dbStringUtil.getString(keyword) + Constants.SQL_LIKE_CHAR);
        }else{
            queryWrapper.like("name",dbStringUtil.getString(keyword));
        }
        queryWrapper.and(rolePOQueryWrapper -> rolePOQueryWrapper.like(!ObjectUtils.isEmpty(keyword),RoleField.name,keyword));
        queryWrapper.eq(RoleField.cid,UserContext.getUserContext().getCompanyId());
        List<RolePO> rolePOS = roleService.list(queryWrapper);
        log.info("GET:/roles/findRoleByKeyword/notag,/roles/findRoleByKeyword/notag/ref===response:rolePOS={}***********************************",rolePOS);
        //如果有 则返回 没有 则返回空数组
        if (!ObjectUtils.isEmpty(rolePOS)){
            return new ListResult<>(rolePOS.stream().map(rolePO -> {
                RoleFindByKeywordVO roleVO = new RoleFindByKeywordVO();
                BeanUtils.copyProperties(rolePO,roleVO);
                return roleVO;
            }).collect(Collectors.toList()));
        }else {
            return new ListResult<>(new ArrayList<>());
        }
    }

    /**
     * @description: 角色联想查询
     * @param: keyword
     * @param: size
     * @param: cid
     * @return: com.supcon.supfusion.framework.cloud.common.result.ListResult<com.supcon.supfusion.rbac.webapi.vo.role.RoleFindByKeywordVO>
     * @author: 袁阳
     * @date: 2020/8/31
     */
    @MenuOperateCode("queryRole")
    @GetMapping("/roles/associate")
    @ApiOperation(value = "联想查询")
    @ApiImplicitParams({
            @ApiImplicitParam(name="keyword",value="关键字，模糊查询角色编码和角色名",required=false,paramType="query"),
            @ApiImplicitParam(name="size",value="返回条数,默认10条",required=false,paramType="query"),
            @ApiImplicitParam(name="cid",value="公司ID",required=false,paramType="query"),
    })
    public ListResult<RoleFindByKeywordVO> fuzzySearch(@RequestParam(value = "keyword", required = false) String keyword, @RequestParam(value = "size", required = false) Integer size,@RequestParam(value = "cid", required = false) Long cid) {
        log.info("GET:/roles/associate===params:keyword={},size={},cid={}***********************************",keyword,size,cid);
        QueryWrapper<RolePO> queryWrapper = new QueryWrapper<>();
        queryWrapper.select(RoleField.code, RoleField.name,RoleField.id);
        if (!ObjectUtils.isEmpty(keyword)) {
            if (DbType.ORACLE.equals(dataId.getDataId())){
                queryWrapper.apply(RoleField.name + " like {0} escape '\\'", Constants.SQL_LIKE_CHAR + dbStringUtil.getString(keyword) + Constants.SQL_LIKE_CHAR);
            }else{
                queryWrapper.like(RoleField.name,dbStringUtil.getString(keyword));
            }
        }
        if (!ObjectUtils.isEmpty(cid)){
            queryWrapper.eq(RoleField.cid,cid);
        }else{
            queryWrapper.eq(RoleField.cid, UserContext.getUserContext().getCompanyId());
        }
        Page<RolePO> page = new Page<>(1, ObjectUtils.isEmpty(size) ? 10 : size);
        Page<RolePO> pageResult = roleService.page(page, queryWrapper);
        log.info("GET:/roles/associate===response:pageResult={}***********************************",pageResult.getRecords());
        return new ListResult<>(pageResult.getRecords().stream().map(rolePO -> {
            RoleFindByKeywordVO roleFindByKeywordVO = new RoleFindByKeywordVO();
            BeanUtils.copyProperties(rolePO, roleFindByKeywordVO);
            return roleFindByKeywordVO;
        }).collect(Collectors.toList()));
    }

    /**
    *
    *
    * @param
    * @return
    */
   @GetMapping(value = "/role/common/get", produces = "application/json")
   public Result getRole(@Param("id") Long id,@Param("includes") String includes){
       log.info("GET:/role/common/get===params:id={},includes={}***********************************",id,includes);
       QueryWrapper<RolePO> queryWrapper = new QueryWrapper<>();
       queryWrapper.select(includes.split(","));
       queryWrapper.eq(RoleField.id,id);
       queryWrapper.eq(RoleField.cid, UserContext.getUserContext().getCompanyId());
       RolePO rolePO = roleService.getOne(queryWrapper);
       RoleCommonVO roleCommonVO = new RoleCommonVO();
       BeanUtils.copyProperties(rolePO,roleCommonVO);
       log.info("GET:/role/common/get===response:roleCommonVO={}***********************************",roleCommonVO);
       Result result = Result.data(roleCommonVO);
       result.setCode(200);
       result.setMessage("操作成功");
       return result;
   }


    @MenuOperateCode("queryByCodes")
    @GetMapping("/roles/queryByCodes")
    public ListResult<RoleVO> queryByCodes(@RequestParam(value = "codes", required = false) @NotNull @Size(min = 1) List<String> codes) {
        log.info("GET:/roles/queryByCodes===params:codes={}***********************************", codes);
        QueryWrapper<RolePO> queryWrapper = new QueryWrapper<>();
        queryWrapper.select(RoleField.code, RoleField.name, RoleField.id);
        queryWrapper.in(RoleField.code, codes);
        List<RolePO> rolePOS = roleService.list(queryWrapper);

        log.info("GET:/roles/queryByCodes===response:rolePOS={}***********************************",rolePOS);
        if (rolePOS == null || rolePOS.size() == 0) {
            return new ListResult<>();
        } else {
            List<RoleVO> roleVOS = new ArrayList<>();
            rolePOS.forEach(rolePO -> {
                        RoleVO roleVO = new RoleVO();
                        roleVO.setCode(rolePO.getCode());
                        roleVO.setId(rolePO.getId());
                        roleVO.setName(rolePO.getName());
                        roleVOS.add(roleVO);
                    }
            );
            return new ListResult<>(roleVOS);
        }
    }
}

