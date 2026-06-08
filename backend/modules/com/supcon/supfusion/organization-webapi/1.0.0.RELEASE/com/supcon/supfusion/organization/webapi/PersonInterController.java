package com.supcon.supfusion.organization.webapi;

import com.alibaba.fastjson.JSONObject;
import com.supcon.supfusion.framework.cloud.annotation.InternalApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;

import com.supcon.supfusion.framework.cloud.common.context.UserContext;
import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.organization.common.constants.Constants;
import com.supcon.supfusion.organization.common.exception.OrganizationErrorEnum;
import com.supcon.supfusion.organization.common.exception.OrganizationException;
import com.supcon.supfusion.organization.common.utils.ImageUtils;
import com.supcon.supfusion.organization.dao.po.person.PersonAddPO;
import com.supcon.supfusion.organization.manager.FileServerApiServiceAdapter;
import com.supcon.supfusion.organization.service.PersonService;
import com.supcon.supfusion.organization.service.bo.person.*;
import com.supcon.supfusion.organization.webapi.vo.department.DepartmentDetailVO;
import com.supcon.supfusion.organization.webapi.vo.person.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 人员管理接口
 *
 * @author
 * @date 20-5-20 上午10:42
 */
@Slf4j
@InternalApi(path = HttpConstants.URL_INTERNALAPI + HttpConstants.URL_SPLITER + "organization" + HttpConstants.URL_SPLITER + "v1")
@Validated
@Api(tags = "人员管理", description = "人员文档说明", hidden = false)
public class PersonInterController {

    @Autowired
    private PersonService personService;

    @Autowired
    private FileServerApiServiceAdapter fileServerApiService;

    /**
     * 新增人员
     * @param personAddVo
     */
    @PostMapping(value = "/person")
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(value = "新增人员", notes = "新增一个人员")
    void addPerson(@Validated @RequestBody PersonAddVO personAddVo) {
        String tenantId = RpcContext.getContext().getTenantId();
        PersonAddPO personAddPo = new PersonAddPO();
        BeanUtils.copyProperties(personAddVo, personAddPo);
        personService.addPersonAndUser(personAddPo, personAddVo.getUserName(), personAddVo.getPassword(), personAddVo.getUserDescription(), personAddVo.getRoles(), tenantId);
    }

    /**
     * 修改人员
     * @param personUpdateVo
     */
    @PutMapping(value = "/person")
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(value = "修改人员", notes = "修改一个人员")
    void updatePerson(@Validated @RequestBody PersonUpdateVO personUpdateVo) {
        String tenantId = RpcContext.getContext().getTenantId();
        PersonAddPO personAddPo = new PersonAddPO();
        BeanUtils.copyProperties(personUpdateVo, personAddPo);
        personService.updatePerson(personAddPo, tenantId);
    }

    /**
     * 删除人员
     * @param id
     */
    @DeleteMapping(value = "/person/{id}")
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(value = "删除人员", notes = "删除一个人员")
    void deletePerson(@ApiParam(value = "人员id,多个人员分号分割", required = true) @PathVariable("id") Long[] id) {
        String tenantId = RpcContext.getContext().getTenantId();
        personService.deletePerson(id, tenantId);
    }

    /**
     * 根据人员id查询修改人员的弹窗信息
     * @param personId
     * @return
     */
    @GetMapping(value = "/person/modify/page")
    @ApiOperation(value = "初始化修改人员页面弹窗的内容", notes = "初始化修改人员页面弹窗的内容")
    Result<PersonUpdatePageVO> queryDetailByPersonId(@ApiParam(value = "人员id", required = true) @RequestParam("personId") Long personId) {
        PersonUpdatePageBO personUpdatePageBO = personService.queryDetailByPersonId(personId);
        PersonUpdatePageVO personUpdatePageVO = new PersonUpdatePageVO();
        BeanUtils.copyProperties(personUpdatePageBO, personUpdatePageVO);
        return new Result<PersonUpdatePageVO>(personUpdatePageVO);
    }

    /**
     * 根据人员id查询修改人员的弹窗信息,需要校验是否是本人
     */
    @GetMapping(value = "/person/modify/ref")
    @ApiOperation(value = "初始化修改人员页面弹窗的内容,需要校验是否是本人", notes = "初始化修改人员页面弹窗的内容,需要校验是否是本人")
    Result<PersonUpdatePageVO> queryDetailValidatePerson(@ApiParam(value = "人员id", required = true) @RequestParam("personId") Long personId) {
        UserContext userContext = UserContext.getUserContext();
        if (null != userContext && !personId.equals(userContext.getStaffId())) {
            throw new OrganizationException(OrganizationErrorEnum.NO_QUERY_PERSON_PERMISSION);
        }
        PersonUpdatePageBO personUpdatePageBO = personService.queryDetailByPersonId(personId);
        PersonUpdatePageVO personUpdatePageVO = new PersonUpdatePageVO();
        BeanUtils.copyProperties(personUpdatePageBO, personUpdatePageVO);
        return new Result<PersonUpdatePageVO>(personUpdatePageVO);
    }

    /**
     * 岗位调入
     * @param personPositionTransferVO
     */
    @PostMapping(value = "/person/transfer/position")
    @ApiOperation(value = "岗位调入", notes = "给人员调入岗位")
    void transferPosition(@Validated @RequestBody PersonPositionTransferVO personPositionTransferVO) {
        PersonPositionTransferBO personPositionTransferBO = new PersonPositionTransferBO();
        BeanUtils.copyProperties(personPositionTransferVO, personPositionTransferBO);
        List<PersonTransferBO> list = new ArrayList<PersonTransferBO>();
        if (personPositionTransferVO.getPersons() != null) {
            personPositionTransferVO.getPersons().stream().forEach(person -> {
                PersonTransferBO personTransferBO = new PersonTransferBO();
                BeanUtils.copyProperties(person, personTransferBO);
                list.add(personTransferBO);
            });
        }
        personPositionTransferBO.setPersons(list);
        String tenantId = RpcContext.getContext().getTenantId();
        personService.transferPosition(personPositionTransferBO, tenantId);
    }

    /**
     * 根据人员id查询人员详情
     * @param ids
     * @return
     */
    @GetMapping("/persons/ids")
    @ResponseBody
    @ApiOperation(value = "根据部门id批量查询岗位信息", notes = "根据部门id批量查询岗位信息")
    public ListResult<PersonDetailVO> queryDeptInfoByIds(@ApiParam(value = "岗位ids") @RequestParam(value = "ids", required = true) List<Long> ids) {
        List<PersonDetailBO> list = personService.queryPersonInfoByIds(ids);
        if (list == null || list.size() == 0) {
            return new ListResult<PersonDetailVO>(new ArrayList<PersonDetailVO>());
        }
        List<PersonDetailVO> vos = new ArrayList<PersonDetailVO>();
        list.stream().forEach(bo -> {
            PersonDetailVO personDetailVO = new PersonDetailVO();
            BeanUtils.copyProperties(bo, personDetailVO);
            vos.add(personDetailVO);
        });
        return new ListResult<PersonDetailVO>(vos);
    }
    @GetMapping(value = "/person/position")
    @ApiOperation(value = "查询人员关联的岗位")
    public ListResult<PersonPositionVO> queryPersonPosition(@ApiParam(value = "人员id") @RequestParam(value = "id", required = true) Long id,
                                                            @ApiParam(value = "公司id") @RequestParam(value = "companyId", required = false) Long companyId) {
        List<PersonPositionBO> list = personService.queryPersonPosition(id, companyId);
        if (list == null || list.size() == 0) {
            return new ListResult<PersonPositionVO>(new ArrayList<PersonPositionVO>());
        }
        List<PersonPositionVO> results = new ArrayList<>();
        list.stream().forEach(bo -> {
            PersonPositionVO personPositionVO = new PersonPositionVO();
            BeanUtils.copyProperties(bo, personPositionVO);
            results.add(personPositionVO);
        });
        return new ListResult<PersonPositionVO>(results);
    }

    @PostMapping(value = "/person/off/position")
    @ApiOperation(value = "岗位调离", notes = "给人员调离岗位")
    public void offPosition(@Validated @RequestBody PersonOffPositionVO personOffPositionVO) {
        if (personOffPositionVO == null) {
            return;
        }
        PersonOffPositionBO personOffPositionBO = new PersonOffPositionBO();
        BeanUtils.copyProperties(personOffPositionVO, personOffPositionBO);
        String tenantId = RpcContext.getContext().getTenantId();
        personService.offPosition(personOffPositionBO, tenantId);
    }
    
    @GetMapping(value = "/staff/common/get")
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(value = "根据id查询人员", notes = "根据id查询人员", tags = {"baseService"})
    public Result<JSONObject> getStaffById(@ApiParam(value = "人员id", required = true) @NotNull(message = Constants.PERSON_PARAM_ID_NECESSARY) @RequestParam(value = "id", required = true) Long id,
                                           @ApiParam(value = "返回哪些字段,逗号分割", required = false)  @RequestParam(value = "includes", required = false) String includes) {
        JSONObject personInfo = personService.getStaffById(id, includes);
        return new Result<JSONObject>(personInfo);
    }

    /**
     * 根据人员id查询主岗对应部门，以及该部门的顶级部门
     * @param id
     * @return
     */
    @GetMapping(value = "/person/main/department")
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(value = "根据人员id查询主岗的部门和顶层部门", notes = "根据人员id查询主岗的部门和顶层部门")
    public Result<PersonDepartmentVO> queryMainDepartmentByPersonId(@ApiParam(value = "人员id", required = true) @NotNull(message = Constants.PERSON_PARAM_ID_NECESSARY) @RequestParam(value = "id", required = true) Long id) {
        PersonDepartmentBO personDepartmentBO = personService.queryMainDepartmentByPersonId(id);
        PersonDepartmentVO result = new PersonDepartmentVO();
        if (personDepartmentBO == null) {
            return new Result<>(result);
        }
        DepartmentDetailVO mainDept = new DepartmentDetailVO();
        BeanUtils.copyProperties(personDepartmentBO.getDepartment(), mainDept);
        DepartmentDetailVO rootDept = new DepartmentDetailVO();
        BeanUtils.copyProperties(personDepartmentBO.getRootDepartment(), rootDept);
        result.setDepartment(mainDept);
        result.setRootDepartment(rootDept);
        return new Result<>(result);
    }

    /**
     * 分页加载人员列表
     * @param current
     * @param pageSize
     * @return
     */
    @GetMapping(value = "/persons/pages")
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(value = "加载人员列表", notes = "加载人员列表")
    public Result<PageResult<PersonDetailVO>> loadPersons(@ApiParam(value = "当前页码", required = true) @Min(value = -1, message = Constants.PAGE_CURRENT_ERROR) @RequestParam("current") Integer current,
                                                    @ApiParam(value = "每页条数", required = true) @Min (value = -1, message = Constants.PAGE_PAGESIZE_ERROR) @RequestParam("pageSize") Integer pageSize,
                                                    @ApiParam(value = "时间", required = false) @RequestParam(value = "fromTime", required = false) Long fromTime) {
        PageResult<PersonDetailBO> pageResult = personService.loadPersons(current, pageSize, fromTime);
        if (pageResult == null || pageResult.getPagination().getTotal() == 0) {
            return new Result<>(new PageResult<>(null, 0, pageSize, current));
        }
        List<PersonDetailVO> vos = new ArrayList<>();
        pageResult.getList().stream().forEach(personPO -> {
            PersonDetailVO personResultVO = new PersonDetailVO();
            BeanUtils.copyProperties(personPO, personResultVO);
            vos.add(personResultVO);
        });
        return new Result<>(new PageResult<>(vos, pageResult.getPagination().getTotal(), pageResult.getPagination().getPageSize(), pageResult.getPagination().getCurrent()));
    }

    @GetMapping(value = "/persons/flow/codes")
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(value = "根据人员编码查询岗位id信息", notes = "根据人员编码查询岗位id信息")
    public ListResult<PersonFlowSimpleVO> queryPersonIdByCodes(@ApiParam(value = "人员编码", required = true) @RequestParam(value = "codes", required = true) List<String> codes) {
        List<PersonFlowSimpleBO> personFlowSimpleBOList = personService.queryPersonIdByCodes(codes);

        List<PersonFlowSimpleVO> personFlowSimpleVOList = new ArrayList<>();
        if (personFlowSimpleBOList == null) {
            return new ListResult<>(personFlowSimpleVOList);
        }
        personFlowSimpleBOList.stream().forEach(personFlowSimpleBO -> {
            PersonFlowSimpleVO personFlowSimpleVO = new PersonFlowSimpleVO();
            BeanUtils.copyProperties(personFlowSimpleBO, personFlowSimpleVO);
            personFlowSimpleVOList.add(personFlowSimpleVO);
        });
        return new ListResult<>(personFlowSimpleVOList);
    }

    /**
     * 根据人员编号查询修改人员的弹窗信息
     * @param code
     * @return
     */
    @GetMapping(value = "/persons/code")
    @ApiOperation(value = "初始化修改人员页面弹窗的内容", notes = "初始化修改人员页面弹窗的内容")
    Result<PersonUpdatePageVO> queryDetailByPersonCode(@ApiParam(value = "人员编号", required = true) @RequestParam("code") String code) {
        PersonUpdatePageBO personUpdatePageBO = personService.queryDetailByPersonCode(code);

        PersonUpdatePageVO personUpdatePageVO = new PersonUpdatePageVO();
        BeanUtils.copyProperties(personUpdatePageBO, personUpdatePageVO);
        return new Result<PersonUpdatePageVO>(personUpdatePageVO);
    }

    /**
     * 上载人员 签名、头像
     * @param uploadFile
     * @return
     */
    @PostMapping(value = "/persons/image")
    @ApiOperation(value = "上载人员签名或头像", notes = "上载人员签名或头像")
    Result uploadPersonImage(@ApiParam(value = "人员头像（或签名）文件", required = true) @RequestPart("file") MultipartFile uploadFile) throws IOException {
        String tenantId = RpcContext.getContext().getTenantId();
        if (null != uploadFile){
            // 文件后缀名校验
            boolean isImg = ImageUtils.checkSuffix(uploadFile.getOriginalFilename());
            if (!isImg){
                throw new OrganizationException(OrganizationErrorEnum.PERSON_IMAGE_NOT_FORMATE);
            }
        }
        //Result result = fileServerApiService.fileUpload(uploadFile);
        Result result = personService.fileUpload(uploadFile, tenantId);
        return result;
    }

    /**
     * 下载人员 签名、头像(附件服务不支持)
     * @param100000001
     * @return
     */
    @PostMapping(value = "/persons/downloadImage")
    @ApiOperation(value = "下载人员签名或头像", notes = "下载人员签名或头像")
    public Result<Map<String, String>> downloadPersonImage(@ApiParam(value = "人员头像（或签名）文件路径", required = true) @RequestParam(value = "filePaths[]") String[] filePaths) {
        if (ArrayUtils.isEmpty(filePaths)){
            throw new OrganizationException(OrganizationErrorEnum.FILE_PARAM_FILEPATH_ERROR);
        }
        Map<String, String> res = personService.downloadFile(filePaths);
        return new Result<>(res);
    }

    /**
     * 获取当前登录人信息（头像地址）（根据staff的Id获取）
     * @param
     * @return
     */
    @GetMapping(value = "/persons/currentLoginHeadImage")
    @ApiOperation(value = "获取当前登录人信息", notes = "获取当前登录人信息")
    Result<String> getLoginHeadImg() {
        UserContext userContext = UserContext.getUserContext();
        Long staffId = userContext.getStaffId();
        PersonLoginInfoBO personLoginInfoBO = personService.getHeadImgById(staffId);

        if (null == personLoginInfoBO){
            return new Result<>();
        }
        return new Result<>(personLoginInfoBO.getAvatarUrl());
    }

}
