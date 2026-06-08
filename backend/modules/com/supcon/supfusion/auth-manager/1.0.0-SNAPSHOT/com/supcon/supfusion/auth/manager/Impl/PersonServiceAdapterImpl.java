package com.supcon.supfusion.auth.manager.Impl;

import com.alibaba.fastjson.JSONObject;
import com.supcon.supfusion.auth.manager.PersonServiceAdapter;
import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.organization.api.PersonApiService;
import com.supcon.supfusion.organization.api.dto.*;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PersonServiceAdapterImpl implements PersonServiceAdapter {


    @Resource
    private PersonApiService personApiService;

    @Override
    public Map<Long, PersonDTO> queryPersonsById(Long[] personIds) {
        return personApiService.queryPersonsById(personIds);
    }

    @Override
    public ListResult<CompanyDTO> queryCompanyIdByPersonId(Long personId) {
        return personApiService.queryCompanyIdByPersonId(personId);
    }

    @Override
    public ListResult<PersonDetailDTO> queryPersonByCodes(List<String> codes) {
        return personApiService.queryPersonByCodes(codes);
    }

    @Override
    public JSONObject getPersonById(Long id, String includes) {
        return personApiService.getCurPerson(id, includes).getData();
    }

    @Override
    public Result<CompanyResultDTO> findCompany(Long id) {
        return personApiService.findCompany(id);
    }

    @Override
    public Result<Boolean> updatePerson(PersonUpdateDTO personUpdateDTO) {
        return personApiService.updatePerson(personUpdateDTO);
    }

    @Override
    public ListResult<Long> queryRoleIdByPersonId(Long personId) {
        return personApiService.queryRoleIdByPersonId(personId);
    }

    @Override
    public ListResult<Long> queryPersonsByCompanyId(Long id) {
        return personApiService.queryPersonsByCompanyId(id);
    }

    @Override
    public ListResult<Long> queryMultiCompanyPersonsByCompanyId(Long id) {
        return personApiService.queryMultiCompanyPersonsByCompanyId(id);
    }

    @Override
    public Result<Long> addVirtualPerson(String userName, Long companyId) {
        return personApiService.addVirtualPerson(userName, companyId);
    }

    @Override
    public Result<CompanyResultDTO> findCompanyByCode(String code) {
        return personApiService.findCompanyByCode(code);
    }

    @Override
    public Result<Boolean> deletePersonById(Long personId) {
        return personApiService.deletePersonById(personId);
    }


    @Override
    public ListResult<PersonDetailDTO> queryPersonDetailByIds(List<Long> ids) {
        return personApiService.queryPersonByIds(ids);
    }
    @Override
    public void saveOrUpdateUsers(List<PersonUserDTO> personUserDTOS) {
        personApiService.saveOrUpdateUsers(personUserDTOS);
    }

    @Override
    public void deleteUsersByPersonIds(List<Long> personIds) {
        personApiService.deleteUsersByPersonIds(personIds);
    }

    @Override
    public Long getCompanyIdByCode(String companyCode) {
        Result<CompanyResultDTO> companyResult = personApiService.findCompanyByCode(companyCode);
        return Optional.ofNullable(companyResult).map(Result::getData).map(CompanyResultDTO::getId).orElse(null);
    }
}

