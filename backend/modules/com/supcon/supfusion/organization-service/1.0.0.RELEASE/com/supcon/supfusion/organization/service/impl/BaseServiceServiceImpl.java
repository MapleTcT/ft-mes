package com.supcon.supfusion.organization.service.impl;

import com.alibaba.druid.sql.visitor.functions.If;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.supcon.supfusion.framework.MneCodeGenterate;
import com.supcon.supfusion.organization.common.constants.Constants;
import com.supcon.supfusion.organization.dao.mapper.company.CompanyMapper;
import com.supcon.supfusion.organization.dao.mapper.department.DepartmentMapper;
import com.supcon.supfusion.organization.dao.mapper.person.OrganizationManagerMapper;
import com.supcon.supfusion.organization.dao.mapper.person.PersonMapper;
import com.supcon.supfusion.organization.dao.mapper.position.PositionMapper;
import com.supcon.supfusion.organization.dao.po.company.CompanyPO;
import com.supcon.supfusion.organization.dao.po.department.DepartmentAddPO;
import com.supcon.supfusion.organization.dao.po.person.OrganizationManagerPO;
import com.supcon.supfusion.organization.dao.po.person.PersonAddPO;
import com.supcon.supfusion.organization.dao.po.position.PositionAddPO;
import com.supcon.supfusion.organization.manager.OrganizationAdapter;
import com.supcon.supfusion.organization.service.BaseServiceService;
import com.supcon.supfusion.organization.service.bo.baseService.CompanyBaseServiceBO;
import com.supcon.supfusion.organization.service.bo.baseService.DepartmentBaseServiceBO;
import com.supcon.supfusion.organization.service.bo.baseService.PersonBaseServiceBO;
import com.supcon.supfusion.organization.service.bo.baseService.PositionBaseServiceBO;
import com.supcon.supfusion.organization.service.bo.person.SystemCodeBO;
import com.supcon.supfusion.systemcode.api.dto.SystemCodeResultDTO;
import io.micrometer.core.instrument.util.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class BaseServiceServiceImpl implements BaseServiceService {
    private static final String OBJ_COMPANY = "company";
    private static final String OBJ_MANAGER = "manager";
    private static final String OBJ_DEPARTMENT = "department";
    private static final String OBJ_MAINPOSITION = "mainPosition";
    @Autowired
    private CompanyMapper companyMapper;
    @Autowired
    private OrganizationManagerMapper organizationManagerMapper;
    @Autowired
    private PersonMapper personMapper;
    @Autowired
    private DepartmentMapper departmentMapper;
    @Autowired
    private PositionMapper positionMapper;

    @Autowired
    private OrganizationAdapter organizationAdapter;

    @Override
    public JSONObject transferToJSON(JSONObject json, String includes, String type) {
        JSONObject result = new JSONObject();
        String[] fields = null; if (StringUtils.isNotBlank(includes)) {
            fields = includes.split(",");
        } if (fields != null && fields.length > 0) { for (String field : fields) { if (json.get(field) != null) {
            result.put(field, json.get(field)); continue;
        } if (!field.contains(".")) { continue;
        }
            String objFieldName = field.substring(0, field.indexOf(".")); if (Constants.COMPANY.equals(type)) { continue;
            } else if (Constants.DEPARTMENT.equals(type)) {
                JSONObject interJson = handleDepartment(json, objFieldName);
                String subField = field.substring(field.indexOf(".") + 1, field.length()); if (objFieldName.equals(OBJ_COMPANY)) {
                    JSONObject comJson = transferToJSON(interJson, subField, Constants.COMPANY); if (result.getJSONObject(OBJ_COMPANY) == null) {
                        result.put(OBJ_COMPANY, comJson);
                    } else {
                        handleResult(result.getJSONObject(OBJ_COMPANY), comJson, OBJ_COMPANY);
                    }
                } else if (objFieldName.equals(OBJ_MANAGER)) {
                    JSONObject managerJson = transferToJSON(interJson, subField, Constants.PERSON); if (result.getJSONObject(OBJ_MANAGER) == null) {
                        result.put(OBJ_MANAGER, managerJson);
                    } else {
                        handleResult(result.getJSONObject(OBJ_MANAGER), managerJson, OBJ_MANAGER);
                    }
                } else { continue;
                }
            } else if (Constants.POSITION.equals(type)) {
                JSONObject interJson = handlePosition(json, objFieldName);
                String subField = field.substring(field.indexOf(".") + 1, field.length()); if (objFieldName.equals(OBJ_COMPANY)) {
                    JSONObject comJson = transferToJSON(interJson, subField, Constants.COMPANY); if (result.getJSONObject(OBJ_COMPANY) == null) {
                        result.put(OBJ_COMPANY, comJson);
                    } else {
                        handleResult(result.getJSONObject(OBJ_COMPANY), comJson, OBJ_COMPANY);
                    }
                } else if (objFieldName.equals(OBJ_DEPARTMENT)) {
                    JSONObject deptJson = transferToJSON(interJson, subField, Constants.DEPARTMENT); if (result.getJSONObject(OBJ_DEPARTMENT) == null) {
                        result.put(OBJ_DEPARTMENT, deptJson);
                    } else {
                        handleResult(result.getJSONObject(OBJ_DEPARTMENT), deptJson, OBJ_DEPARTMENT);
                    }
                } else { continue;
                }
            } else if (Constants.PERSON.equals(type)) {
                JSONObject interJson = handlePerson(json, objFieldName);
                String subField = field.substring(field.indexOf(".") + 1, field.length()); if (objFieldName.equals(OBJ_MAINPOSITION)) {
                    JSONObject posJson = transferToJSON(interJson, subField, Constants.POSITION); if (result.getJSONObject(OBJ_MAINPOSITION) == null) {
                        result.put(OBJ_MAINPOSITION, posJson);
                    } else {
                        handleResult(result.getJSONObject(OBJ_MAINPOSITION), posJson, OBJ_MAINPOSITION);
                    }
                } else { continue;
                }
            } else { continue;
            }
        }
        return result;
        }
        return json;
    }
    private void handleResult(JSONObject source, JSONObject target, String type) {
        fieldPutAll(source, target);
        if (type.equals(OBJ_MAINPOSITION)) {
            if (source.containsKey(OBJ_DEPARTMENT)) {
                if (target.containsKey(OBJ_DEPARTMENT)) {
                    handleResult(source.getJSONObject(OBJ_DEPARTMENT), target.getJSONObject(OBJ_DEPARTMENT), OBJ_DEPARTMENT);
                }
            } else { if (target.containsKey(OBJ_DEPARTMENT)) {
                source.put(OBJ_DEPARTMENT, target.getJSONObject(OBJ_DEPARTMENT));
            }
            } if (source.containsKey(OBJ_COMPANY)) {
                if (target.containsKey(OBJ_COMPANY)) {
                    handleResult(source.getJSONObject(OBJ_COMPANY), target.getJSONObject(OBJ_COMPANY), OBJ_COMPANY);
                }
            } else {
                if (target.containsKey(OBJ_COMPANY)) {
                    source.put(OBJ_COMPANY, target.getJSONObject(OBJ_COMPANY));
                }
            }
        } else if (type.equals(OBJ_DEPARTMENT)) {
            if (source.containsKey(OBJ_COMPANY)) {
                if (target.containsKey(OBJ_COMPANY)) {
                    handleResult(source.getJSONObject(OBJ_COMPANY), target.getJSONObject(OBJ_COMPANY), OBJ_COMPANY);
                }
            } else {
                if (target.containsKey(OBJ_COMPANY)) {
                    source.put(OBJ_COMPANY, target.getJSONObject(OBJ_COMPANY));
                }
            }
            if (source.containsKey(OBJ_MANAGER)) {
                if (target.containsKey(OBJ_MANAGER)) {
                    handleResult(source.getJSONObject(OBJ_MANAGER), target.getJSONObject(OBJ_MANAGER), OBJ_MANAGER);
                }
            } else {
                if (target.containsKey(OBJ_MANAGER)) {
                    source.put(OBJ_MANAGER, target.getJSONObject(OBJ_MANAGER));
                }
            }
        } else if (type.equals(OBJ_MANAGER)) {
            if (source.containsKey(OBJ_MAINPOSITION)) {
                if (target.containsKey(OBJ_MAINPOSITION)) {
                    handleResult(source.getJSONObject(OBJ_MAINPOSITION), target.getJSONObject(OBJ_MAINPOSITION), OBJ_MAINPOSITION);
                }
            } else {
                if (target.containsKey(OBJ_MAINPOSITION)) {
                    source.put(OBJ_MAINPOSITION, target.getJSONObject(OBJ_MAINPOSITION));
                }
            }
        } else if (type.equals(OBJ_COMPANY)) {
        }
    }
    private void fieldPutAll(JSONObject source, JSONObject target) {
        Set<String> keys = target.keySet();
        for (String key : keys) {
            if (target.get(key) instanceof JSONObject) {
                continue;
            } else {
                source.put(key, target.get(key));
            }
        }
    }
    private JSONObject handleDepartment(JSONObject json, String objFieldName) {
        if (OBJ_COMPANY.equals(objFieldName)) {
            Long companyId = json.getLong("cid");
            if (companyId == null) {
                return new JSONObject();
            }
            CompanyPO companyPO = companyMapper.selectById(companyId);
            if (companyPO == null) {
                return new JSONObject();
            }
            CompanyBaseServiceBO companyBaseServiceBO = new CompanyBaseServiceBO();
            BeanUtils.copyProperties(companyPO, companyBaseServiceBO);
            return (JSONObject) JSON.toJSON(companyBaseServiceBO);
        } else if (OBJ_MANAGER.equals(objFieldName)) {
            Long depId = json.getLong("id");
            if (depId == null) {
                return new JSONObject();
            }
            QueryWrapper<OrganizationManagerPO> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("org_id", depId);
            List<OrganizationManagerPO> managers = organizationManagerMapper.selectList(queryWrapper);
            if (managers == null || managers.size() == 0) {
                return new JSONObject();
            }
            PersonAddPO personAddPO = personMapper.selectById(managers.get(0).getManagerId());
            if (personAddPO == null) {
                return new JSONObject();
            }
            PersonBaseServiceBO personBaseServiceBO = new PersonBaseServiceBO();
            BeanUtils.copyProperties(personAddPO, personBaseServiceBO);
            return (JSONObject) JSON.toJSON(personBaseServiceBO);
        } else {
            return new JSONObject();
        }
}
    private JSONObject handlePosition(JSONObject json, String objFieldName) {
        if (OBJ_COMPANY.equals(objFieldName)) {
            Long companyId = json.getLong("cid");
            if (companyId == null) {
                return new JSONObject();
            }
            CompanyPO companyPO = companyMapper.selectById(companyId);
            if (companyPO == null) {
                return new JSONObject();
            }
            CompanyBaseServiceBO companyBaseServiceBO = new CompanyBaseServiceBO();
            BeanUtils.copyProperties(companyPO, companyBaseServiceBO);
            return (JSONObject) JSON.toJSON(companyBaseServiceBO);
    } else if (OBJ_DEPARTMENT.equals(objFieldName)) {
        Long depId = json.getLong("depId");
        if (depId == null) {
            return new JSONObject();
        }
        DepartmentAddPO departmentAddPO = departmentMapper.selectById(depId);
        if (departmentAddPO == null) {
            return new JSONObject();
        }
        DepartmentBaseServiceBO departmentBaseServiceBO = new DepartmentBaseServiceBO();
        BeanUtils.copyProperties(departmentAddPO, departmentBaseServiceBO);
        return (JSONObject) JSON.toJSON(departmentBaseServiceBO);
    } else {
            return new JSONObject();
    }
    }
    private JSONObject handlePerson(JSONObject json, String objFieldName) {
        if (OBJ_MAINPOSITION.equals(objFieldName)) {
            Long posId = json.getLong("mainPositionId");
            if (posId == null) {
                return new JSONObject();
            }
            PositionAddPO positionAddPO = positionMapper.selectById(posId);
            if (positionAddPO == null) {
                return new JSONObject();
            }
            PositionBaseServiceBO positionBaseServiceBO = new PositionBaseServiceBO();
            BeanUtils.copyProperties(positionAddPO, positionBaseServiceBO);
            return (JSONObject) JSON.toJSON(positionBaseServiceBO);
        } else {
            return new JSONObject();
        }
    }

    /**
     * 生成助记码
     * @param name
     * @return
     */
    @Override
    public List<String> generateZhujima(String name) {
        List<String> list = MneCodeGenterate.mneCodeTupleGenerate(name);
        return list;
    }

    @Override
    public SystemCodeBO findSystemCode(String entityCodeAndCode, Map<String, List<SystemCodeResultDTO>> entityCodeMap) {
        if (StringUtils.isEmpty(entityCodeAndCode)){
            return null;
        }
        String entityCode = entityCodeAndCode.split("/")[0];
        String genderCode = entityCodeAndCode.split("/")[1];

        SystemCodeBO systemCodeBO = new SystemCodeBO();

        if (entityCodeMap.get(entityCode) == null) {
            List<SystemCodeResultDTO> genderResult = organizationAdapter.querySystemCodesByEntityCode(entityCode);
            entityCodeMap.put(entityCode, genderResult);
        }
        List<SystemCodeResultDTO> systemCodeResultDTOS = entityCodeMap.get(entityCode);
        for (SystemCodeResultDTO systemCodeResultDTO : systemCodeResultDTOS) {
            if (systemCodeResultDTO.getCode().equals(genderCode)) {
                systemCodeBO.setCode(systemCodeResultDTO.getCode());
                systemCodeBO.setName(systemCodeResultDTO.getDisplayName());
                break;
            }
        }
        return systemCodeBO;
    }
}