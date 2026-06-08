package com.supcon.supfusion.auth.manager;

import com.alibaba.fastjson.JSONObject;
import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.organization.api.dto.*;

import java.util.List;
import java.util.Map;

public interface PersonServiceAdapter {

    Map<Long, PersonDTO> queryPersonsById(Long[] personIds);

    ListResult<CompanyDTO> queryCompanyIdByPersonId(Long personId);



    ListResult<PersonDetailDTO> queryPersonByCodes(List<String> codes);

    JSONObject getPersonById(Long id, String includes);

    Result<CompanyResultDTO> findCompany(Long id);

    ListResult<Long> queryRoleIdByPersonId(Long personId);

    Result<Boolean> updatePerson(PersonUpdateDTO personUpdateDTO);

    ListResult<Long> queryPersonsByCompanyId(Long id);

    ListResult<Long> queryMultiCompanyPersonsByCompanyId(Long id);

    Result<Long> addVirtualPerson(String userName, Long companyId);

    Result<CompanyResultDTO> findCompanyByCode(String code);

    Result<Boolean> deletePersonById(Long personId);

    ListResult<PersonDetailDTO> queryPersonDetailByIds(List<Long> ids);

    void saveOrUpdateUsers(List<PersonUserDTO> personUserDTOS);

    void deleteUsersByPersonIds(List<Long> personIds);

    Long getCompanyIdByCode(String companyCode);
}
