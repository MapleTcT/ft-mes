package com.supcon.supfusion.organization.openapi;

import com.google.common.collect.Lists;
import com.supcon.supfusion.auth.api.UserApiService;
import com.supcon.supfusion.framework.cloud.annotation.InternalApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.organization.common.constants.Constants;
import com.supcon.supfusion.organization.common.exception.OrganizationErrorEnum;
import com.supcon.supfusion.organization.common.exception.OrganizationException;
import com.supcon.supfusion.organization.common.utils.ImageUtils;
import com.supcon.supfusion.organization.dao.po.company.CompanyPO;
import com.supcon.supfusion.organization.dao.po.company.CompanyPersonPO;
import com.supcon.supfusion.organization.dao.po.department.DepartmentPersonPO;
import com.supcon.supfusion.organization.dao.po.position.PositionPersonPO;
import com.supcon.supfusion.organization.openapi.vo.CompanyVO;
import com.supcon.supfusion.organization.openapi.vo.company.CompanyDetailInfoVO;
import com.supcon.supfusion.organization.openapi.vo.department.DepartmentDetailInfoVO;
import com.supcon.supfusion.organization.openapi.vo.person.MainPositionBaseVO;
import com.supcon.supfusion.organization.openapi.vo.person.PersonAddOpenVO;
import com.supcon.supfusion.organization.openapi.vo.person.PersonBulkOperateOpenVO;
import com.supcon.supfusion.organization.openapi.vo.person.PersonCompanyBaseVO;
import com.supcon.supfusion.organization.openapi.vo.person.PersonDepartmentBaseVO;
import com.supcon.supfusion.organization.openapi.vo.person.PersonDetailInfoVO;
import com.supcon.supfusion.organization.openapi.vo.person.PersonPositionVO;
import com.supcon.supfusion.organization.openapi.vo.person.PersonRelationVO;
import com.supcon.supfusion.organization.openapi.vo.person.PersonSynchronizationInfoVO;
import com.supcon.supfusion.organization.openapi.vo.person.PersonUpdateOpenVO;
import com.supcon.supfusion.organization.openapi.vo.person.PersonVO;
import com.supcon.supfusion.organization.openapi.vo.person.SystemCodeVO;
import com.supcon.supfusion.organization.openapi.vo.person.UserVO;
import com.supcon.supfusion.organization.openapi.vo.position.PositionDetailInfoVO;
import com.supcon.supfusion.organization.service.CompanyPersonService;
import com.supcon.supfusion.organization.service.CompanyService;
import com.supcon.supfusion.organization.service.DepartmentPersonService;
import com.supcon.supfusion.organization.service.DepartmentService;
import com.supcon.supfusion.organization.service.PersonService;
import com.supcon.supfusion.organization.service.PositionPersonService;
import com.supcon.supfusion.organization.service.PositionService;
import com.supcon.supfusion.organization.service.bo.company.CompanyBO;
import com.supcon.supfusion.organization.service.bo.company.CompanyDetailInfoBO;
import com.supcon.supfusion.organization.service.bo.person.MainPositionBaseBO;
import com.supcon.supfusion.organization.service.bo.person.PersonAddOpenBO;
import com.supcon.supfusion.organization.service.bo.person.PersonBO;
import com.supcon.supfusion.organization.service.bo.person.PersonBulkOperateOpenBO;
import com.supcon.supfusion.organization.service.bo.person.PersonCompanyBaseBO;
import com.supcon.supfusion.organization.service.bo.person.PersonDepartmentBaseBO;
import com.supcon.supfusion.organization.service.bo.person.PersonDetailBO;
import com.supcon.supfusion.organization.service.bo.person.PersonPositionBO;
import com.supcon.supfusion.organization.service.bo.person.PersonSynchronizationInfoBO;
import com.supcon.supfusion.organization.service.bo.person.PersonUpdateOpenBO;
import com.supcon.supfusion.organization.service.bo.person.SystemCodeBO;
import com.supcon.supfusion.organization.service.bo.person.UserBO;
import io.micrometer.core.instrument.util.StringUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;
import java.util.stream.Collectors;

/**
 * 人员管理接口
 *
 * @author
 * @date 20-5-20 上午10:42
 */
@Slf4j
@Setter
@Getter
@InternalApi(path = HttpConstants.URL_OPENAPI + HttpConstants.URL_SPLITER + "organization")
@Validated
@Api(tags = "人员管理openApi", description = "人员管理openApi说明", hidden = true)
public class PersonController {

    @Autowired
    private PersonService personService;
    @Autowired
    private CompanyService companyService;
    @Autowired
    private DepartmentService departmentService;
    @Autowired
    private PositionService positionService;
    @Autowired
    private DepartmentPersonService departmentPersonService;
    @Autowired
    private PositionPersonService positionPersonService;
    @Autowired
    private CompanyPersonService companyPersonService;
    @Autowired
    private DepartmentController departmentController;
    @Autowired
    private PositionController positionController;
    @Autowired
    private UserApiService userApiService;
    /**
     * 查询所有人员信息
     * @return
     */
    @GetMapping("/v1/person/all")
    @ApiOperation(value = "查询所有人员", notes = "查询所有人员")
    ListResult<PersonVO> queryAllPersons() {
        List<PersonBO> list = personService.queryAllPersons();
        List<PersonVO> vos = new ArrayList<PersonVO>();
        list.stream().forEach(personBO -> {
            PersonVO personVO = new PersonVO();
            BeanUtils.copyProperties(personBO, personVO);
            vos.add(personVO);
        });
        return new ListResult<PersonVO>(vos);
    }

    /**
     * 根据人员code查询所属公司
     * @param personCode
     * @return
     */
    @GetMapping("/v1/person/codes/companies")
    @ApiOperation(value = "根据人员编码查询公司列表", notes = "根据人员编码查询公司列表")
    ListResult<CompanyVO> queryCompanyIdByPersonCode(@ApiParam(value = "人员code", required = true) @RequestParam("personCode") String personCode) {
        List<CompanyVO> list = new ArrayList<CompanyVO>();
        if (StringUtils.isBlank(personCode)) {
            return new ListResult<CompanyVO>(list);
        }
        List<CompanyBO> companies = personService.queryCompanIdByPersonCode(personCode);

        companies.stream().forEach(company -> {
            CompanyVO companyVO = new CompanyVO();
            BeanUtils.copyProperties(company, companyVO);
            list.add(companyVO);
        });
        return new ListResult<CompanyVO>(list);
    }



    @GetMapping("/v2/persons")
    @ApiOperation(value = "批量查询人员列表")
    PageResult<PersonSynchronizationInfoVO> getPersons(@ApiParam(value = "最后修改时间", required = false) @RequestParam(value = "modifyTime", required = false) String modifyTime,
                                                       @ApiParam(value = "当前页码", required = false) @Min(value = 1, message = Constants.PAGE_CURRENT_ERROR) @NotNull(message = Constants.PAGE_CURRENT_ERROR) @RequestParam(value = "current", required = false) Integer current,
                                                       @ApiParam(value = "每页条数", required = false) @Min (value = 1, message = Constants.PAGE_PAGESIZE_ERROR) @Max(value = 500, message = Constants.PAGE_PAGESIZE_MAX_ERROR) @RequestParam(value = "pageSize", required = false, defaultValue = "20") Integer pageSize) {
        Date modifyDate = null;
        /**
         * 时间格式校验,转换
         * 时间格式: 2021-01-31T10:30:20.000+1000
         */
        if (StringUtils.isNotBlank(modifyTime)) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
            try {
                modifyDate = format.parse(modifyTime);
                SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                sdf1.setTimeZone(TimeZone.getTimeZone("UTC"));
                modifyTime = sdf1.format(modifyDate);
            } catch (ParseException e) {
                throw new OrganizationException(OrganizationErrorEnum.DATE_FORMAT_ERROR);
            }
        }
        PageResult<PersonSynchronizationInfoBO> pageResult = personService.getPersons(modifyTime, current, pageSize);

        List<PersonSynchronizationInfoVO> positionSynchronizationInfoVOS = new ArrayList<>();
        for (PersonSynchronizationInfoBO personSynchronizationInfoBO : pageResult.getList()) {
            PersonSynchronizationInfoVO personSynchronizationInfoVO = new PersonSynchronizationInfoVO();
            BeanUtils.copyProperties(personSynchronizationInfoBO, personSynchronizationInfoVO);

            UserBO userBO = personSynchronizationInfoBO.getUser();
            UserVO userVO = new UserVO();
            BeanUtils.copyProperties(userBO, userVO);
            personSynchronizationInfoVO.setUser(userVO);

            SystemCodeBO genderBO = personSynchronizationInfoBO.getGender();
            if (Objects.nonNull(genderBO)) {
                SystemCodeVO genderVO = new SystemCodeVO();
                BeanUtils.copyProperties(genderBO, genderVO);
                personSynchronizationInfoVO.setGender(genderVO);
            }

            SystemCodeBO statusBO = personSynchronizationInfoBO.getStatus();
            if (Objects.nonNull(statusBO)) {
                SystemCodeVO statusVO = new SystemCodeVO();
                BeanUtils.copyProperties(statusBO, statusVO);
                personSynchronizationInfoVO.setStatus(statusVO);
            }

            SystemCodeBO titleBO = personSynchronizationInfoBO.getTitle();
            if (Objects.nonNull(titleBO)) {
                SystemCodeVO titleVO = new SystemCodeVO();
                BeanUtils.copyProperties(titleBO, titleVO);
                personSynchronizationInfoVO.setTitle(titleVO);
            }

            SystemCodeBO educationBO = personSynchronizationInfoBO.getEducation();
            if (Objects.nonNull(educationBO)) {
                SystemCodeVO educationVO = new SystemCodeVO();
                BeanUtils.copyProperties(educationBO, educationVO);
                personSynchronizationInfoVO.setEducation(educationVO);
            }

            MainPositionBaseBO mainPositionBaseBO = personSynchronizationInfoBO.getMainPosition();
            MainPositionBaseVO mainPositionBaseVO = new MainPositionBaseVO();
            BeanUtils.copyProperties(mainPositionBaseBO, mainPositionBaseVO);
            personSynchronizationInfoVO.setMainPosition(mainPositionBaseVO);

            List<PersonCompanyBaseVO> personCompanyBaseVOList = new ArrayList<>();
            for (PersonCompanyBaseBO personCompanyBaseBO : personSynchronizationInfoBO.getCompanies()) {
                PersonCompanyBaseVO personCompanyBaseVO = new PersonCompanyBaseVO();
                BeanUtils.copyProperties(personCompanyBaseBO, personCompanyBaseVO);
                personCompanyBaseVOList.add(personCompanyBaseVO);
            }
            personSynchronizationInfoVO.setCompanies(personCompanyBaseVOList);

            List<PersonDepartmentBaseVO> personDepartmentBaseVOList = new ArrayList<>();
            for (PersonDepartmentBaseBO personDepartmentBaseBO : personSynchronizationInfoBO.getDepartments()) {
                PersonDepartmentBaseVO personDepartmentBaseVO = new PersonDepartmentBaseVO();
                BeanUtils.copyProperties(personDepartmentBaseBO, personDepartmentBaseVO);
                personDepartmentBaseVOList.add(personDepartmentBaseVO);
            }
            personSynchronizationInfoVO.setDepartments(personDepartmentBaseVOList);

            List<MainPositionBaseVO> positionBaseVOList = new ArrayList<>();
            for (MainPositionBaseBO positionBaseBO : personSynchronizationInfoBO.getPositions()) {
                MainPositionBaseVO positionBaseVO = new MainPositionBaseVO();
                BeanUtils.copyProperties(positionBaseBO, positionBaseVO);
                positionBaseVOList.add(positionBaseVO);
            }
            personSynchronizationInfoVO.setPositions(positionBaseVOList);

            positionSynchronizationInfoVOS.add(personSynchronizationInfoVO);
        }
        return new PageResult<>(positionSynchronizationInfoVOS, pageResult.getPagination().getTotal(), pageSize, current);
    }

    /**
     * 查询人员详情openapi
     * @param personCode
     * @return
     */
    @GetMapping("/v2/persons/{personCode}")
    @ApiOperation(value = "查询指定人员编号详细信息")
    PersonDetailInfoVO queryPersonByPersonCode(@ApiParam(value = "人员编码", required = true) @PathVariable("personCode") String personCode) {
        PersonDetailInfoVO personDetailInfoVO = new PersonDetailInfoVO();
        Result<PersonSynchronizationInfoBO> personSycBO = personService.getPersonDetailByPersonCode(personCode);
        BeanUtils.copyProperties(personSycBO.getData(), personDetailInfoVO);

        SystemCodeBO genderBO = personSycBO.getData().getGender();
        if (Objects.nonNull(genderBO)) {
            SystemCodeVO genderVO = new SystemCodeVO();
            BeanUtils.copyProperties(genderBO, genderVO);
            personDetailInfoVO.setGender(genderVO);
        }

        SystemCodeBO statusBO = personSycBO.getData().getStatus();
        if (Objects.nonNull(statusBO)) {
            SystemCodeVO statusVO = new SystemCodeVO();
            BeanUtils.copyProperties(statusBO, statusVO);
            personDetailInfoVO.setStatus(statusVO);
        }

        SystemCodeBO titleBO = personSycBO.getData().getTitle();
        if (Objects.nonNull(titleBO)) {
            SystemCodeVO titleVO = new SystemCodeVO();
            BeanUtils.copyProperties(titleBO, titleVO);
            personDetailInfoVO.setTitle(titleVO);
        }

        SystemCodeBO educationBO = personSycBO.getData().getEducation();
        if (Objects.nonNull(educationBO)) {
            SystemCodeVO educationVO = new SystemCodeVO();
            BeanUtils.copyProperties(educationBO, educationVO);
            personDetailInfoVO.setEducation(educationVO);
        }

        //todo 后续人员表会冗余用户信息,此处需要修改
        UserBO userBO = personSycBO.getData().getUser();
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(userBO, userVO);
        personDetailInfoVO.setUser(userVO);
      /*  UserBO userBO = personSycBO.getData().getUser();
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(userBO, userVO);
        personDetailInfoVO.setUser(userVO);*/

        MainPositionBaseBO mainPositionBaseBO = personSycBO.getData().getMainPosition();
        MainPositionBaseVO mainPositionBaseVO = new MainPositionBaseVO();
        BeanUtils.copyProperties(mainPositionBaseBO, mainPositionBaseVO);
        personDetailInfoVO.setMainPosition(mainPositionBaseVO);

        List<PersonCompanyBaseVO> personCompanyBaseVOS = new ArrayList<>();
        List<PersonCompanyBaseBO> personCompanyBaseBOS = personSycBO.getData().getCompanies();
        if (!CollectionUtils.isEmpty(personCompanyBaseBOS)) {
            for (PersonCompanyBaseBO personCompanyBaseBO : personCompanyBaseBOS) {
                PersonCompanyBaseVO personCompanyBaseVO = new PersonCompanyBaseVO();
                BeanUtils.copyProperties(personCompanyBaseBO, personCompanyBaseVO);
                personCompanyBaseVOS.add(personCompanyBaseVO);
            }
            personDetailInfoVO.setCompanies(personCompanyBaseVOS);
        }

        List<PersonDepartmentBaseVO> personDepartmentBaseVOS = new ArrayList<>();
        List<PersonDepartmentBaseBO> personDepartmentBaseBOS = personSycBO.getData().getDepartments();
        if (!CollectionUtils.isEmpty(personDepartmentBaseBOS)) {
            for (PersonDepartmentBaseBO personDepartmentBaseBO : personDepartmentBaseBOS) {
                PersonDepartmentBaseVO personDepartmentBaseVO = new PersonDepartmentBaseVO();
                BeanUtils.copyProperties(personDepartmentBaseBO, personDepartmentBaseVO);
                personDepartmentBaseVOS.add(personDepartmentBaseVO);
            }
            personDetailInfoVO.setDepartments(personDepartmentBaseVOS);
        }

        List<MainPositionBaseVO> positionBaseVOList = new ArrayList<>();
        List<MainPositionBaseBO> positions = personSycBO.getData().getPositions();
        if (!CollectionUtils.isEmpty(positions)) {
            for (MainPositionBaseBO positionBaseBO : personSycBO.getData().getPositions()) {
                MainPositionBaseVO positionBaseVO = new MainPositionBaseVO();
                BeanUtils.copyProperties(positionBaseBO, positionBaseVO);
                positionBaseVOList.add(positionBaseVO);
            }
            personDetailInfoVO.setPositions(positionBaseVOList);
        }

        return new Result<>(personDetailInfoVO).getData();

    }

    @GetMapping(value = "/v2/persons/{personCode}/positions")
    @ApiOperation(value = "查询人员关联的岗位")
    public ListResult<PersonPositionVO> queryPersonPosition(@ApiParam(value = "人员编码", required = true) @NotBlank(message = Constants.PERSON_PARAM_CODE_NECESSARY) @PathVariable(value = "personCode") String code,
                                                            @ApiParam(value = "公司编码") @RequestParam(value = "companyCode", required = false) String companyCode) {
        List<PersonDetailBO> personDetailBOS = personService.queryPersonsByCodes(Lists.newArrayList(code));
        if (ObjectUtils.isEmpty(personDetailBOS)) {
            throw new OrganizationException(OrganizationErrorEnum.PERSON_ID_NOT_EXISTS);
        }
        Long companyId = null;
        if (!ObjectUtils.isEmpty(companyCode)) {
            CompanyPO companyPOByCode = companyService.findCompanyByCode(companyCode);
            if (!ObjectUtils.isEmpty(companyPOByCode)) {
                companyId = companyPOByCode.getId();
            } else {
                throw new OrganizationException(OrganizationErrorEnum.COMPANY_PARAM_ID_NECESSARY);
            }
        }
        List<PersonPositionBO> list = personService.queryPersonPosition(personDetailBOS.get(0).getId(), companyId);
        if (list == null || list.size() == 0) {
            return new ListResult<>(new ArrayList<>());
        }
        List<PersonPositionVO> results = new ArrayList<>();
        list.stream().forEach(bo -> {
            PersonPositionVO personPositionVO = new PersonPositionVO();
            BeanUtils.copyProperties(bo, personPositionVO);
            results.add(personPositionVO);
        });
        return new ListResult<>(results);
    }

    @GetMapping(value = "/v2/persons/{personCode}/orgRelations")
    @ApiOperation(value = "查询人员组织关系（包括公司、部门、岗位）")
    public PersonRelationVO queryPersonPosition(@ApiParam(value = "人员编码", required = true) @NotBlank(message = Constants.PERSON_PARAM_CODE_NECESSARY) @PathVariable(value = "personCode") String code) {
        List<PersonDetailBO> personDetailBOS = personService.queryPersonsByCodes(Lists.newArrayList(code));
        if (ObjectUtils.isEmpty(personDetailBOS)) {
            throw new OrganizationException(OrganizationErrorEnum.PERSON_ID_NOT_EXISTS);
        }
        Long personId = personDetailBOS.get(0).getId();

        //人员
        PersonDetailInfoVO personDetailInfoVOResult = queryPersonByPersonCode(code);

        //根据人员查询公司
        List<CompanyPersonPO> companyPersonPOS = companyPersonService.getCompanyPersonByPersonId(personId);
        List<Long> companyIds = companyPersonPOS.stream().map(CompanyPersonPO::getCompanyId).distinct().collect(Collectors.toList());
        //根据公司id查询公司信息
        List<CompanyDetailInfoVO> companyDetailInfoVOS = Lists.newArrayList();
        for (Long companyId : companyIds) {
            CompanyPO companyPO = companyService.findCompany(companyId);
            Result<CompanyDetailInfoBO> companyDetailInfoBO = companyService.getCompanyByCode(companyPO.getCode());
            CompanyDetailInfoVO companyDetailInfoVO = new CompanyDetailInfoVO();
            BeanUtils.copyProperties(companyDetailInfoBO.getData(), companyDetailInfoVO);
            companyDetailInfoVOS.add(companyDetailInfoVO);
        }

        //部门
        List<DepartmentPersonPO> departmentPersonPOS = departmentPersonService.getdepartmentPersonByPersonId(personId);
        List<Long> deptIds = departmentPersonPOS.stream().map(DepartmentPersonPO::getDeptId).distinct().collect(Collectors.toList());

        List<DepartmentDetailInfoVO> departmentDetailInfoVOS= Lists.newArrayList();
        for (Long deptId : deptIds) {
            String departmentCode = departmentService.getDepartmentCodeById(deptId);
            DepartmentDetailInfoVO departmentDetailInfoVO = departmentController.getDeptByCode(departmentCode);
            departmentDetailInfoVOS.add(departmentDetailInfoVO);
        }

        //岗位
        List<PositionPersonPO> positionPersonPOS = positionPersonService.getByPersonId(personId);
        List<Long> positionIds = positionPersonPOS.stream().map(PositionPersonPO::getPositionId).distinct().collect(Collectors.toList());

        List<PositionDetailInfoVO> positionDetailInfoVOS = Lists.newArrayList();
        for (Long positionId : positionIds) {
            String positionCode = positionService.getPositionCodeById(positionId);
            PositionDetailInfoVO positionDetailInfoVO = positionController.getPositionByCode(positionCode);
            positionDetailInfoVOS.add(positionDetailInfoVO);
        }

        PersonRelationVO personRelationVO = new PersonRelationVO();
        BeanUtils.copyProperties(personDetailInfoVOResult,personRelationVO);
        personRelationVO.setCompanies(companyDetailInfoVOS);
        personRelationVO.setDepartments(departmentDetailInfoVOS);
        personRelationVO.setPositions(positionDetailInfoVOS);
        return personRelationVO;
    }

    @GetMapping(value = "/v2/users/{username}/person")
    @ApiOperation(value = "根据用户名查询关联的人员信息")
    public PersonDetailInfoVO getPersonInfo(@ApiParam(value = "用户名", required = true) @PathVariable("username") String userName) {
        PersonBO personBO = personService.getPersonByUserName(userName);
        if (ObjectUtils.isEmpty(personBO)) {
            throw new OrganizationException(OrganizationErrorEnum.USER_NOT_EXISTS);
        }
        return queryPersonByPersonCode(personBO.getCode());
    }

    @PostMapping(value = "/v2/persons/bulk")
    @ApiOperation(value = "批量新增,修改,删除人员")
    public void bulkPersonOperate(@Validated @RequestBody PersonBulkOperateOpenVO personBulkOperateOpenVO) {
        String tenantId = RpcContext.getContext().getTenantId();
        if (personBulkOperateOpenVO == null) {
            return;
        }
        PersonBulkOperateOpenBO personBulkOperateOpenBO = new PersonBulkOperateOpenBO();
        if (personBulkOperateOpenVO.getAddPersons() != null && personBulkOperateOpenVO.getAddPersons().size() > 0) {
            List<PersonAddOpenBO> addPersons = new LinkedList<>();
            for (PersonAddOpenVO personAddOpenVO : personBulkOperateOpenVO.getAddPersons()) {
                PersonAddOpenBO personAddOpenBO = new PersonAddOpenBO();
                BeanUtils.copyProperties(personAddOpenVO, personAddOpenBO);
                addPersons.add(personAddOpenBO);
            }
            personBulkOperateOpenBO.setAddPersons(addPersons);
        }
        if (personBulkOperateOpenVO.getUpdatePersons() != null && personBulkOperateOpenVO.getUpdatePersons().size() > 0) {
            List<PersonUpdateOpenBO> updatePersons = new LinkedList();
            for (PersonUpdateOpenVO personUpdateOpenVO : personBulkOperateOpenVO.getUpdatePersons()) {
                PersonUpdateOpenBO personUpdateOpenBO = new PersonUpdateOpenBO();
                BeanUtils.copyProperties(personUpdateOpenVO, personUpdateOpenBO);
                updatePersons.add(personUpdateOpenBO);
            }
            personBulkOperateOpenBO.setUpdatePersons(updatePersons);
        }
        personBulkOperateOpenBO.setDeletePersons(personBulkOperateOpenVO.getDeletePersons());

        personService.bulkOperate(personBulkOperateOpenBO, tenantId);
    }

    /**
     * 上载人员 签名、头像
     * @param uploadFile
     * @return
     */
    @PostMapping(value = "/v2/persons/{personCode}/image")
    @ApiOperation(value = "上载人员签名或头像", notes = "上载人员签名或头像")
    void uploadPersonImage(@ApiParam(value = "人员头像（或签名）文件", required = true) @RequestPart("file") MultipartFile uploadFile,
                             @ApiParam(value = "人员编号", required = true) @PathVariable("personCode") String personCode,
                             @ApiParam(value = "图片类型", required = true) @RequestParam(value = "imageType", required = true) String imageType) throws IOException {
        String tenantId = RpcContext.getContext().getTenantId();
        if (null != uploadFile){
            // 文件后缀名校验
            boolean isImg = ImageUtils.checkSuffix(uploadFile.getOriginalFilename());
            if (!isImg){
                throw new OrganizationException(OrganizationErrorEnum.PERSON_IMAGE_NOT_FORMATE);
            }
        }
        if (StringUtils.isBlank(imageType) || !"avatar".equals(imageType)) {
            throw new OrganizationException(OrganizationErrorEnum.IMAGE_PARAM_ERROR);
        }
        Result result = personService.fileUpload(uploadFile, tenantId);
        personService.updateAvatarUrl(personCode, result.getData().toString(), tenantId);
    }
}
