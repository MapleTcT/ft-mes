package com.supcon.supfusion.organization.openapi.compatible;

import com.alibaba.fastjson.JSONObject;
import com.supcon.supfusion.framework.cloud.annotation.InternalApi;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;
import com.supcon.supfusion.organization.common.constants.Constants;
import com.supcon.supfusion.organization.common.exception.OrganizationErrorEnum;
import com.supcon.supfusion.organization.common.exception.OrganizationException;
import com.supcon.supfusion.organization.dao.po.person.PersonAddPO;
import com.supcon.supfusion.organization.service.PersonService;
import com.supcon.supfusion.organization.service.bo.person.PersonPositionTransferBO;
import com.supcon.supfusion.organization.service.bo.person.PersonTransferBO;
import com.supcon.supfusion.organization.service.bo.person.PersonUpdatePageBO;
import com.supcon.supfusion.organization.openapi.vo.compatible.person.PersonAddVO;
import com.supcon.supfusion.organization.openapi.vo.compatible.person.PersonBatchDeleteVO;
import com.supcon.supfusion.organization.openapi.vo.compatible.person.PersonUpdateVO;
import com.supcon.supfusion.organization.openapi.vo.person.PersonPositionTransferVO;
import com.supcon.supfusion.organization.openapi.vo.person.PersonUpdatePageVO;
import io.micrometer.core.instrument.util.StringUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * 人员管理接口
 *
 * @author zhuangmh
 * @date 20-7-3 上午10:42
 */
@Setter
@Getter
@InternalApi(path = "/open-api/api/metadata/organization/person")
@Validated
@Api(tags = "人员管理old", description = "人员文档说明(基于V1,V2架构)", hidden = true)
public class PersonMetadataOpenController {

    private static final String PERSON_PREFIX = "Person_";
    
    @Autowired
    private PersonService personService;

    /**
     * 新增人员
     *
     */
/*    @PostMapping(value = "/v1")
    @ResponseStatus(HttpStatus.CREATED)
    @ApiOperation(value = "新增人员meta", notes = "新增一个人员Meta")
    JSONObject addPersonMeta(@Validated @RequestBody PersonAddVO personAddVoMeta) {
        PersonAddPO personAddPo = buildPersonAddPO(personAddVoMeta);
        personService.addPerson(personAddPo, null, null, null, null);
        JSONObject result = new JSONObject();
        result.put("code", 200);
        result.put("message", "success");
        return result;
    }*/
    
    private PersonAddPO buildPersonAddPO(PersonAddVO personAddVo) {
        PersonAddPO personAddPo = new PersonAddPO();
        personAddPo.setCode(personAddVo.getCode());
        // 默认公司ID
        personAddPo.setDescription(personAddVo.getDescription());
        personAddPo.setEmail(personAddVo.getEmail());
        personAddPo.setGender(personAddVo.getGender() == Constants.FEMALE ? "female" : "male");
        Long uid = IDGenerator.newInstance().generate().longValue();
        personAddPo.setId(uid);
        personAddPo.setName(personAddVo.getShowName());
        personAddPo.setPhone(personAddVo.getPhone());
        personAddPo.setOldId(PERSON_PREFIX +  uid);
        personAddPo.setMainPosition(personAddVo.getMainPosition());
        personAddPo.setStatus("onWork");
        return personAddPo;
    }

    /**
     * 查询人员列表
     */
    @GetMapping("/v1")
    @ApiOperation(value = "查询人员列表")
    public JSONObject queryPersonList(@ApiParam(value = "关键字", required = false) @RequestParam(value = "keywords", required = false) String keywords,
                                      @ApiParam(value = "是否包含账户信息", required = false) @RequestParam(value = "hasAccount", required = false) Boolean hasAccount,
                                      @ApiParam(value = "是否查询全部", required = false) @RequestParam(value = "isAll", required = false) Boolean isAll,
                                      @ApiParam(value = "是否包含组织信息", required = false) @RequestParam(value = "includeOrgs", required = false) Boolean includeOrgs,
                                      @ApiParam(value = "当前页", required = false) @RequestParam(value = "page", required = false) Integer page,
                                      @ApiParam(value = "每页条数", required = false) @RequestParam(value = "per_page", required = false) Integer per_page,
                                      HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        String noBear = "";
        if (StringUtils.isNotBlank(token)) {
            noBear = token.substring(7,token.length());
        }
        String tenantId = request.getHeader("X-TENANT-ID");
        return personService.queryPersonList(keywords, hasAccount == null? false : hasAccount, isAll == null? true : isAll, includeOrgs == null? false : includeOrgs, page == null? 1 : page, per_page == null? 20 : per_page, noBear, tenantId);
    }

    @GetMapping("/v1/{code}")
    @ApiOperation(value = "查询人员详情")
    public JSONObject queryPersonDetail(@PathVariable("code") String code, HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        String noBear = "";
        if (StringUtils.isNotBlank(token)) {
            noBear = token.substring(7,token.length());
        }
        String tenantId = request.getHeader("X-TENANT-ID");
        return personService.queryPersonDetail(code, noBear, tenantId);
    }

    /**
     * 修改人员
     * @param personUpdateVo
     */
/*    @PutMapping(value = "/v1/{code}")
    @ResponseStatus(HttpStatus.CREATED)
    @ApiOperation(value = "修改人员", notes = "修改一个人员")
    JSONObject updatePerson(@Validated @RequestBody PersonUpdateVO personUpdateVo, @PathVariable("code") String code) {
        PersonAddPO personUpdatePo = buildPersonUpdatePO(personUpdateVo, code);
        boolean success = personService.updatePersonByCode(personUpdatePo);
        if (!success) {
            throw new OrganizationException(404, OrganizationErrorEnum.PERSON_ID_NOT_EXISTS.getMessage(), 404);
        }
        JSONObject result = new JSONObject();
        result.put("code", 200);
        result.put("message", "success");
        return result;
    }*/
    
    private PersonAddPO buildPersonUpdatePO(PersonUpdateVO personUpdateVo, String code) {
        PersonAddPO personAddPo = new PersonAddPO();
        personAddPo.setCode(code);
        personAddPo.setDescription(personUpdateVo.getDescription());
        personAddPo.setEmail(personUpdateVo.getEmail());
        if (personUpdateVo.getGender() != null) {
            personAddPo.setGender(personUpdateVo.getGender().intValue() == Constants.FEMALE ? "female" : "male");
        }
        personAddPo.setName(personUpdateVo.getShowName());
        personAddPo.setPhone(personUpdateVo.getPhone());
        if (personUpdateVo.getStatus() != null) {
            // TODO 离职编码
            personAddPo.setStatus(personUpdateVo.getStatus().intValue() == Constants.ON_WORK ? "onWork" : "offWork");
        }
        return personAddPo;
    }

    /**
     * 删除人员
     */
    @DeleteMapping(value = "/v1/{code}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ApiOperation(value = "删除人员", notes = "删除一个人员")
    JSONObject deletePerson(@PathVariable("code") String code) {
        String tenantId = RpcContext.getContext().getTenantId();
        personService.deletePersonByCode(code, tenantId);
        JSONObject result = new JSONObject();
        result.put("code", 200);
        result.put("message", "success");
        return result;
    }
    
    /**
     * 删除人员
     */
    @PostMapping(value = "/v1/delete")
    @ResponseStatus(HttpStatus.CREATED)
    @ApiOperation(value = "批量删除人员", notes = "批量删除人员")
    JSONObject batchDeletePerson(@RequestBody PersonBatchDeleteVO personBatchDeleteVO) {
        String tenantId = RpcContext.getContext().getTenantId();
        personService.batchDeletePersonByCode(personBatchDeleteVO.getList(), tenantId);
        JSONObject result = new JSONObject();
        result.put("code", 200);
        result.put("message", "success");
        return result;
    }

    /**
     * 根据人员id查询修改人员的弹窗信息
     * @param personId
     * @return
     */
/*    @GetMapping(value = "/person/modify/page")
    @ApiOperation(value = "初始化修改人员页面弹窗的内容", notes = "初始化修改人员页面弹窗的内容")
    Result<PersonUpdatePageVO> queryDetailByPersonId(@ApiParam(value = "人员id", required = true) @RequestParam("personId") Long personId) {
        PersonUpdatePageBO personUpdatePageBO = personService.queryDetailByPersonId(personId);
        PersonUpdatePageVO personUpdatePageVO = new PersonUpdatePageVO();
        BeanUtils.copyProperties(personUpdatePageBO, personUpdatePageVO);
        return new Result<PersonUpdatePageVO>(personUpdatePageVO);
    }*/

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


}
