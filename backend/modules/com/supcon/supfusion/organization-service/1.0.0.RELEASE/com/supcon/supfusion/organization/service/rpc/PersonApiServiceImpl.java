package com.supcon.supfusion.organization.service.rpc;

import com.alibaba.fastjson.JSONObject;
import com.supcon.supfusion.auth.api.dto.UserDetailDTO;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import com.supcon.supfusion.organization.api.PersonApiService;
import com.supcon.supfusion.organization.api.dto.*;
import com.supcon.supfusion.organization.common.utils.ThreadPoolUtils;
import com.supcon.supfusion.organization.dao.po.company.CompanyPO;
import com.supcon.supfusion.organization.dao.po.department.DepartmentAddPO;
import com.supcon.supfusion.organization.dao.po.person.PersonAddPO;
import com.supcon.supfusion.organization.manager.OrganizationAdapter;
import com.supcon.supfusion.organization.service.CompanyService;
import com.supcon.supfusion.organization.service.DepartmentService;
import com.supcon.supfusion.organization.service.PersonService;
import com.supcon.supfusion.organization.service.PositionService;
import com.supcon.supfusion.organization.service.bo.company.CompanyBO;
import com.supcon.supfusion.organization.service.bo.department.DepartmentDetailBO;
import com.supcon.supfusion.organization.service.bo.person.PersonBO;
import com.supcon.supfusion.organization.service.bo.person.PersonDetailBO;
import com.supcon.supfusion.organization.service.bo.person.PersonLeaderBO;
import com.supcon.supfusion.organization.service.bo.person.PersonUserBO;
import com.supcon.supfusion.organization.service.bo.position.PositionDetailBO;
import io.micrometer.core.instrument.util.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * 人员rpc实现
 */
@ServiceApiService
public class PersonApiServiceImpl extends BaseController implements PersonApiService {
    /**
     * 根据id查询人员信息
     * @param personIds
     * @return
     */
    @Autowired
    private PersonService personService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private CompanyService companyService;

    @Autowired
    private OrganizationAdapter organizationAdapter;

    @Autowired
    private PositionService positionService;

    @Override
    public void addCompany(CompanyDTO companyDTO) {
        String tenantId = RpcContext.getContext().getTenantId();
        CompanyPO comPO = new CompanyPO();
        BeanUtils.copyProperties(companyDTO, comPO);
        companyService.addCompany(comPO, null, companyDTO.getUserName(), companyDTO.getPassword(), tenantId);
    }

    @Override
    public Result<CompanyResultDTO> findCompany(Long id) {
        CompanyResultDTO comDTO = new CompanyResultDTO();
        CompanyPO comPO = companyService.findCompany(id);
        Optional.ofNullable(comPO).ifPresent(com -> BeanUtils.copyProperties(com, comDTO));
        return Result.custom().data(comDTO).build();
    }

    @Override
    public void addDepartment(DepartmentAddDTO departmentAddDTO) {
        String tenantId = RpcContext.getContext().getTenantId();
        DepartmentAddPO departmentAddPO = new DepartmentAddPO();
        BeanUtils.copyProperties(departmentAddDTO, departmentAddPO);
        departmentService.addDepartment(departmentAddPO, departmentAddDTO.getManagerIds(), tenantId);
    }
    @Override
    public Map<Long, PersonDTO> queryPersonsById(Long[] personIds) {
        List<PersonDetailBO> list = personService.queryPersonsById(personIds);
        TreeMap<Long, PersonDTO> map = new TreeMap<Long, PersonDTO>();
        list.stream().forEach(person -> {
            PersonDTO personDTO = new PersonDTO();
            BeanUtils.copyProperties(person, personDTO);
            map.put(personDTO.getId(), personDTO);
        });
        return map;
    }

    @Override
    public ListResult<PersonDetailDTO> queryPersonByCodes(List<String> codes) {
        if (codes == null || codes.size() == 0) {
            return new ListResult<PersonDetailDTO>(new ArrayList<PersonDetailDTO>());
        }
        List<PersonDetailBO> list = personService.queryPersonsByCodes(codes);
        if (list == null || list.size() == 0) {
            return new ListResult<PersonDetailDTO>(new ArrayList<PersonDetailDTO>());
        }
        List<PersonDetailDTO> results = new ArrayList<PersonDetailDTO>();
        list.stream().forEach(person -> {
            PersonDetailDTO personDetailDTO = new PersonDetailDTO();
            BeanUtils.copyProperties(person, personDetailDTO);
            UserDetailDTO userDetailDTO = organizationAdapter.getUserDetailByPerson(person.getId());
            if (userDetailDTO != null) {
                personDetailDTO.setUserName(userDetailDTO.getUserName());
            }
            results.add(personDetailDTO);
        });
        return new ListResult<PersonDetailDTO>(results);
    }

    @Override
    public ListResult<DepartmentDetailDTO> queryDepartmentByCodes(List<String> codes) {
        if (codes == null || codes.size() == 0) {
            return new ListResult<DepartmentDetailDTO>(new ArrayList<DepartmentDetailDTO>());
        }
        List<DepartmentDetailBO> list = departmentService.queryDepartmentByCodes(codes);
        if (list == null || list.size() == 0) {
            return new ListResult<DepartmentDetailDTO>(new ArrayList<DepartmentDetailDTO>());
        }
        List<DepartmentDetailDTO> results = new ArrayList<DepartmentDetailDTO>();
        list.stream().forEach(dept -> {
            DepartmentDetailDTO deptDetailDTO = new DepartmentDetailDTO();
            BeanUtils.copyProperties(dept, deptDetailDTO);
            results.add(deptDetailDTO);
        });
        return new ListResult<DepartmentDetailDTO>(results);
    }

    @Override
    public ListResult<PositionDetailDTO> queryPositionByCodes(List<String> codes) {
        if (codes == null || codes.size() == 0) {
            return new ListResult<PositionDetailDTO>(new ArrayList<PositionDetailDTO>());
        }
        List<PositionDetailBO> list = positionService.queryPositionByCodes(codes);
        if (list == null || list.size() == 0) {
            return new ListResult<PositionDetailDTO>(new ArrayList<PositionDetailDTO>());
        }
        List<PositionDetailDTO> results = new ArrayList<PositionDetailDTO>();
        list.stream().forEach(pos -> {
            PositionDetailDTO posDetailDTO = new PositionDetailDTO();
            BeanUtils.copyProperties(pos, posDetailDTO);
            results.add(posDetailDTO);
        });
        return new ListResult<PositionDetailDTO>(results);
    }

    @Override
    public ListResult<CompanyDTO> queryCompanyIdByPersonId(Long personId) {
        if (personId == null) {
            return new ListResult<CompanyDTO>();
        }
        List<CompanyBO> companies = personService.queryCompanIdByPersonIds(personId);
        List<CompanyDTO> list = new ArrayList<CompanyDTO>();
        companies.stream().forEach(company -> {
            CompanyDTO companyDTO = new CompanyDTO();
            BeanUtils.copyProperties(company, companyDTO);
            list.add(companyDTO);
        });
        return new ListResult<CompanyDTO>(list);
    }

    @Override
    public ListResult<PersonDTO> queryPersonByNotification(List<String> roleCodes, List<String> positionCodes, List<String> departmentCodes, List<String> personCodes) {
        List<PersonDTO> list = personService.queryPersonByNotification(roleCodes, positionCodes, departmentCodes, personCodes);
        return new ListResult<PersonDTO>(list);
    }

    /**
     * 根据人员编码查询公司
     * @param personCode
     * @return
     */
    @Override
    public ListResult<CompanyDTO> queryCompanyIdByPersonCode(String personCode) {
        if (StringUtils.isBlank(personCode)) {
            return new ListResult<CompanyDTO>();
        }
        List<CompanyBO> companies = personService.queryCompanIdByPersonCode(personCode);
        List<CompanyDTO> list = new ArrayList<CompanyDTO>();
        companies.stream().forEach(company -> {
            CompanyDTO companyDTO = new CompanyDTO();
            BeanUtils.copyProperties(company, companyDTO);
            list.add(companyDTO);
        });
        return new ListResult<CompanyDTO>(list);
    }

    /**
     * 查询所有人
     * @return
     */
    @Override
    public ListResult<PersonDTO> queryAllPersons() {
        List<PersonBO> list = personService.queryAllPersons();
        List<PersonDTO> vos = new ArrayList<PersonDTO>();
        list.stream().forEach(personBO -> {
            PersonDTO personDTO = new PersonDTO();
            BeanUtils.copyProperties(personBO, personDTO);
            vos.add(personDTO);
        });
        return new ListResult<PersonDTO>(vos);
    }

    @Override
    public ListResult<Long> querySubPositionIdsByPositionId(List<Long> ids) {
        List<Long> list = positionService.querySubPositionIdsByPositionId(ids);
        return new ListResult<Long>(list);
    }

    @Override
    public ListResult<Long> querySubDepartmentIdsByDepartmentId(List<Long> ids) {
        List<Long> list = departmentService.querySubDepartmentIdsByDepartmentId(ids);
        return new ListResult<Long>(list);
    }

    @Override
    public ListResult<DepartmentDetailDTO> queryPersonsDepartmentsByPersonIds(List<Long> ids) {
        List<DepartmentDetailBO> departmentDetailBOs = personService.queryPersonsDepartmentsByPersonIds(ids);
        if (departmentDetailBOs == null || departmentDetailBOs.size() == 0) {
            return new ListResult<DepartmentDetailDTO>(new ArrayList<>());
        }
        List<DepartmentDetailDTO> results = new ArrayList<>();
        departmentDetailBOs.stream().forEach(dept -> {
            DepartmentDetailDTO departmentDetailDTO = new DepartmentDetailDTO();
            BeanUtils.copyProperties(dept, departmentDetailDTO);
            results.add(departmentDetailDTO);
        });
        return new ListResult<DepartmentDetailDTO>(results);
    }

    @Override
    public ListResult<PositionDetailDTO> queryPersonsPositionsByPersonIds(List<Long> ids) {
        List<PositionDetailBO> positionDetailBOS = personService.queryPersonsPositionsByPersonIds(ids);
        if (positionDetailBOS == null || positionDetailBOS.size() == 0) {
            return new ListResult<PositionDetailDTO>(new ArrayList<>());
        }
        List<PositionDetailDTO> results = new ArrayList<>();
        positionDetailBOS.stream().forEach(pos -> {
            PositionDetailDTO positionDetailDTO = new PositionDetailDTO();
            BeanUtils.copyProperties(pos, positionDetailDTO);
            results.add(positionDetailDTO);
        });
        return new ListResult<PositionDetailDTO>(results);
    }

    @Override
    public Result<JSONObject> getCurPerson(Long id, String includes) {
        JSONObject staffInfo = personService.getCurrentLoginInfo(id, includes);
        return new Result<JSONObject>(staffInfo);
    }

    @Override
    public Result<JSONObject> getCompanyById(Long id, String includes) {
        JSONObject companyInfo = companyService.getCompanyById(id, includes);
        return new Result<JSONObject>(companyInfo);
    }

    @Override
    public ListResult<PositionDetailDTO> queryPersonPositionsByPersonId(Long id) {
        List<PositionDetailBO> positionDetailBOS = personService.queryPersonPositionsByPersonId(id);
        if (positionDetailBOS == null || positionDetailBOS.size() == 0) {
            return new ListResult<PositionDetailDTO>(new ArrayList<>());
        }
        List<PositionDetailDTO> results = new ArrayList<>();
        positionDetailBOS.stream().forEach(pos -> {
            PositionDetailDTO positionDetailDTO = new PositionDetailDTO();
            BeanUtils.copyProperties(pos, positionDetailDTO);
            results.add(positionDetailDTO);
        });
        return new ListResult<PositionDetailDTO>(results);
    }

    /**
     *
     * @param id 岗位id
     * @param all 是否查询全部下级岗位true，还是只查询直接下级岗位false（默认为false）
     * @param cid
     * @return
     */
    @Override
    public ListResult<PositionDetailDTO> querySubPositionByParentId(Long id, Boolean all, Long cid) {
        List<PositionDetailBO> list = positionService.querySubPositionByParentId(id, all, cid);
        if (list == null || list.size() == 0) {
            return new ListResult<>(new ArrayList<>());
        }
        List<PositionDetailDTO> positionDetailDTOS = new ArrayList<>();
        list.stream().forEach(pos -> {
            PositionDetailDTO positionDetailDTO = new PositionDetailDTO();
            BeanUtils.copyProperties(pos, positionDetailDTO);
            positionDetailDTOS.add(positionDetailDTO);
        });
        return new ListResult<>(positionDetailDTOS);
    }

    /**
     *
     * @param id 部门id，为null时则，则代表
     * @param all 是否查询全部下级部门true，还是只查询直接下级部门false（默认为false）
     * @param cid 公司id
     * @return
     */
    @Override
    public ListResult<DepartmentDetailDTO> querySubDepartmentByParentId(Long id, Boolean all, Long cid) {
        List<DepartmentDetailBO> list = departmentService.querySubDepartmentByParentId(id, all, cid);
        if (list == null || list.size() == 0) {
            return new ListResult<>(new ArrayList<>());
        }
        List<DepartmentDetailDTO> departmentDetailDTOS = new ArrayList<>();
        list.stream().forEach(dept -> {
            DepartmentDetailDTO departmentDetailDTO = new DepartmentDetailDTO();
            BeanUtils.copyProperties(dept, departmentDetailDTO);
            departmentDetailDTOS.add(departmentDetailDTO);
        });
        return new ListResult<>(departmentDetailDTOS);
    }

    /**
     *
     * @return
     */
    @Override
    public ListResult<CompanyResultDTO> queryAllCompanies() {
        List<CompanyBO> list = companyService.queryAllCompanies();
        if (list == null || list.size() == 0) {
            return new ListResult<>(new ArrayList<>());
        }
        List<CompanyResultDTO> companyResultDTOS = new ArrayList<>();
        list.stream().forEach(com -> {
            CompanyResultDTO companyResultDTO = new CompanyResultDTO();
            BeanUtils.copyProperties(com, companyResultDTO);
            companyResultDTOS.add(companyResultDTO);
        });
        return new ListResult<>(companyResultDTOS);
    }

    @Override
    public ListResult<PositionDetailDTO> queryPositionsByIds(List<Long> ids) {
        List<PositionDetailBO> list = positionService.queryPosInfoByIds(ids);
        if (list == null || list.size() == 0) {
            return new ListResult<PositionDetailDTO>();
        }
        List<PositionDetailDTO> detailDTOS = new ArrayList<>();
        list.stream().forEach(bo -> {
            PositionDetailDTO positionDetailDTO = new PositionDetailDTO();
            BeanUtils.copyProperties(bo, positionDetailDTO);
            detailDTOS.add(positionDetailDTO);
        });
        return new ListResult<>(detailDTOS);
    }

    @Override
    public Result<Boolean> updatePerson(PersonUpdateDTO personUpdateDTO) {
        String tenantId = RpcContext.getContext().getTenantId();
        PersonAddPO personAddPO = personService.queryPersonPOById(personUpdateDTO.getId());
        personAddPO.setEmail(personUpdateDTO.getEmail());
        personAddPO.setPhone(personUpdateDTO.getPhone());
        personService.updatePerson(personAddPO, tenantId);
        return new Result<>(true);
    }

    @Override
    public ListResult<Long> queryRoleIdByPersonId(Long personId) {
        List<Long> roleIds = personService.queryRoleIdByPersonId(personId);
        if (roleIds == null || roleIds.size() == 0) {
            return new ListResult<>();
        }

        return new ListResult<>(roleIds);
    }

    @Override
    public ListResult<Long> queryPersonsByCompanyId(Long id) {
        Set<Long> personIds = personService.queryPersonsByCompanyId(id);
        if (personIds == null) {
            return new ListResult<>();
        }

        return new ListResult<>(personIds);
    }

    @Override
    public Result<CompanyResultDTO> findCompanyByCode(String code) {
        CompanyResultDTO comDTO = new CompanyResultDTO();
        CompanyPO comPO = companyService.findCompanyByCode(code);
        Optional.ofNullable(comPO).ifPresent(com -> BeanUtils.copyProperties(com, comDTO));
        return Result.custom().data(comDTO).build();
    }

    @Override
    public Result<Long> addVirtualPerson(String userName, Long companyId) {
        String tenantId = RpcContext.getContext().getTenantId();
        Long depId = departmentService.addVirtualDept(companyId, tenantId);
        Long posId = positionService.addVirtualPos(companyId, depId, tenantId);
        Long id = personService.addVirtualPerson(userName, posId, tenantId);
        return new Result<>(id);
    }

    @Override
    public ListResult<PersonDetailDTO> queryPersonsByPositionId(Long positionId) {
        List<PersonDetailBO> bos = personService.queryPersonsByPositionId(positionId);
        if (bos == null || bos.size() == 0) {
            return new ListResult<>();
        }
        List<PersonDetailDTO> dtos = new ArrayList<>();
        bos.stream().forEach(bo -> {
            PersonDetailDTO personDetailDTO = new PersonDetailDTO();
            BeanUtils.copyProperties(bo, personDetailDTO);
            dtos.add(personDetailDTO);
        });
        return new ListResult<>(dtos);
    }

    @Override
    public ListResult<PersonDetailDTO> queryPersonsByDepartmentId(Long departmentId) {
        List<PersonDetailBO> bos = personService.queryPersonsByDepartmentId(departmentId);
        if (bos == null || bos.size() == 0) {
            return new ListResult<>();
        }
        List<PersonDetailDTO> dtos = new ArrayList<>();
        bos.stream().forEach(bo -> {
            PersonDetailDTO personDetailDTO = new PersonDetailDTO();
            BeanUtils.copyProperties(bo, personDetailDTO);
            dtos.add(personDetailDTO);
        });
        return new ListResult<>(dtos);
    }

    @Override
    public Result<Boolean> checkPersonSupAndSub(Long supPersonId, Long subPersonId, Long companyId) {
        Boolean result = personService.checkPersonSupAndSub(supPersonId, subPersonId, companyId);
        return new Result<>(result);
    }

    @Override
    public Map<Long, PersonDetailDTO> queryPersonsByIds(List<Long> personIds) {
        List<PersonDetailBO> list = personService.queryPersonsById(personIds);
        TreeMap<Long, PersonDetailDTO> map = new TreeMap<Long, PersonDetailDTO>();
        list.stream().forEach(person -> {
            PersonDetailDTO personDTO = new PersonDetailDTO();
            BeanUtils.copyProperties(person, personDTO);
            map.put(personDTO.getId(), personDTO);
        });
        return map;
    }

    @Override
    public List<Long> querySupCompaniesById(Long companyId) {

        return companyService.querySupCompaniesById(companyId);
    }

    @Override
    public PersonLeaderDTO getPersonLeader(Long personId) {

        PersonLeaderBO personLeaderBO = personService.getPersonLeader(personId);
        if (personLeaderBO == null) {
            return new PersonLeaderDTO();
        }
        PersonLeaderDTO personLeaderDTO = new PersonLeaderDTO();
        if (personLeaderBO.getDirectLeader() != null) {
            PersonDTO personDTO = new PersonDTO();
            BeanUtils.copyProperties(personLeaderBO.getDirectLeader(), personDTO);
            personLeaderDTO.setDirectLeader(personDTO);
        }
        if (personLeaderBO.getGrandLeader() != null) {
            PersonDTO personDTO = new PersonDTO();
            BeanUtils.copyProperties(personLeaderBO.getGrandLeader(), personDTO);
            personLeaderDTO.setGrandLeader(personDTO);
        }
        return personLeaderDTO;
    }

    @Override
    public OpenapiVersionDTO getOpenapiVersion() {
        OpenapiVersionDTO openapiVersionDTO = new OpenapiVersionDTO();
        openapiVersionDTO.setVersion("2.0.0");
        openapiVersionDTO.setService("organization");
        return openapiVersionDTO;
    }

    @Override
    public Result<Boolean> checkPositionSupAndSub(Long supPositionId, Long subPositionId) {
        Boolean result = positionService.checkPositionSupAndSub(supPositionId, subPositionId);
        return new Result<>(result);
    }

    @Override
    public Result<Boolean> deletePersonById(Long personId) {
        personService.deletePersonById(personId);
        return new Result<>(true);
    }

    @Override
    public Result<Boolean> checkRolesExistPosition(RoleIdDTO roleIdDTO) {
        if (roleIdDTO == null || roleIdDTO.getRoleIds() == null || roleIdDTO.getRoleIds().size() == 0) {
            return new Result<>(false);
        }
        Boolean flag = positionService.checkRolesExistPosition(roleIdDTO.getRoleIds());
        return new Result<>(flag);
    }


    @Override
    public ListResult<PersonDetailDTO> queryPersonByIds(List<Long> ids) {
        if (ids == null || ids.size() == 0) {
            return new ListResult<PersonDetailDTO>(new ArrayList<PersonDetailDTO>());
        }
        List<PersonDetailBO> list = personService.queryPersonsById(ids);
        if (list == null || list.size() == 0) {
            return new ListResult<PersonDetailDTO>(new ArrayList<PersonDetailDTO>());
        }
        List<PersonDetailDTO> results = new ArrayList<PersonDetailDTO>();
        list.stream().forEach(person -> {
            PersonDetailDTO personDetailDTO = new PersonDetailDTO();
            BeanUtils.copyProperties(person, personDetailDTO);
            UserDetailDTO userDetailDTO = organizationAdapter.getUserDetailByPerson(person.getId());
            if (userDetailDTO != null) {
                personDetailDTO.setUserName(userDetailDTO.getUserName());
            }
            results.add(personDetailDTO);
        });
        return new ListResult<PersonDetailDTO>(results);
    }

    @Override
    public void saveOrUpdateUsers(List<PersonUserDTO> personUserDTOS) {
        ThreadPoolUtils.getThreadPool().execute(() -> {
            for (PersonUserDTO personUserDTO : personUserDTOS) {
                PersonUserBO personUserBO = new PersonUserBO();
                BeanUtils.copyProperties(personUserDTO, personUserBO);
                personService.saveOrUpdateUserByPersonId(personUserBO);
            }
        });
    }

    @Override
    public void deleteUsersByPersonIds(List<Long> personIds) {
        ThreadPoolUtils.getThreadPool().execute(() -> {
            personService.deleteUserByPersonIds(personIds);
        });
    }

    @Override
    public ListResult<Long> queryMultiCompanyPersonsByCompanyId(Long companyId) {

        List<Long> personIds = personService.queryMultiCompanyPersonsByCompanyId(companyId);

        return new ListResult<>(personIds);
    }
}
