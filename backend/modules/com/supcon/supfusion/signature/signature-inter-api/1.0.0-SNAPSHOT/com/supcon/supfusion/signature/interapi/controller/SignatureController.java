package com.supcon.supfusion.signature.interapi.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import com.supcon.supfusion.organization.api.PersonApiService;
import com.supcon.supfusion.organization.api.dto.PersonDTO;
import com.supcon.supfusion.organization.api.dto.PositionDetailDTO;
import com.supcon.supfusion.rbac.api.IRoleApiService;
import com.supcon.supfusion.rbac.api.dto.RoleDTO;
import com.supcon.supfusion.signature.base.constant.ButtonPowerTypeConstant;
import com.supcon.supfusion.signature.base.enums.SignatureErrorEnum;
import com.supcon.supfusion.signature.base.exception.SignatureException;
import com.supcon.supfusion.signature.base.i18n.SignatureInternationalResource;
import com.supcon.supfusion.signature.dao.entity.*;
import com.supcon.supfusion.signature.dao.enums.OperateType;
import com.supcon.supfusion.signature.dao.enums.ViewType;
import com.supcon.supfusion.signature.interapi.vo.response.ButtonResponseVO;
import com.supcon.supfusion.signature.interapi.vo.*;
import com.supcon.supfusion.signature.interapi.vo.ButtonVO;
import com.supcon.supfusion.signature.services.service.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.IOException;
import java.util.*;

/**
 * @author user
 */
@Slf4j
@RestController
@Api(tags = {"电子签名管理API"})
public class SignatureController extends BaseController {

    @Autowired
    private ModuleService moduleService;
    @Autowired
    private EntityService entityService;
    @Autowired
    SignatureService signatureService;
    @Autowired
    private ViewServiceFoundation viewServiceFoundation;
    @Autowired
    private DataGridService dataGridService;
    @Autowired
    private ButtonService buttonService;
    @Autowired
    ElectronicSignatureService electronicSignatureService;

    @Autowired
    private SignatureInternationalResource signatureInternationalResource;

    @Autowired
    private IRoleApiService iRoleApiService;

    @Autowired
    private PersonApiService personApiService;

    /**
     * @param
     * @return
     */
    @ApiOperation("菜单树API")
    @PostMapping(value = "/inter-api/signature/foundation/projectButton/viewTree")
    @ResponseBody
    public Result<List<FoundationEcTreeNode>> viewTree( ViewTreeVo viewTreeVo) {
        Locale locale = LocaleContextHolder.getLocale();
        List treeList = new ArrayList<FoundationEcTreeNode>();
        Integer level = viewTreeVo.getLevel();
        String code = viewTreeVo.getCode();
        if (level != 0 && StringUtils.isBlank(code)) {
            throw new SignatureException(SignatureErrorEnum.CODE_CANNOT_BE_EMPTY_ERROR);
        }
        switch (level) {
            case 0:
                List<Module> moduleList = moduleService.getAllModule();
                if (moduleList != null && moduleList.size() > 0) {
                    for (Module m : moduleList) {
                        if (!"sysbase_1.0".equals(m.getCode())) {
                            FoundationEcTreeNode node = new FoundationEcTreeNode();
                            node.setCode(m.getCode());
                            node.setName(signatureInternationalResource.getI18nValue(m.getName(), locale));
                            node.setIsParent(true);
                            treeList.add(node);
                        }
                    }
                }
                break;
            case 1:
                List<Entity> entityList = entityService.findEntities(code);
                if (entityList == null && entityList.size() <= 0) {
                    break;
                }
                if ("sysbase_1.0".equals(code)) {
                    for (Entity entity : entityList) {
                        if ("sysbase_1.0_position".equals(entity.getCode())) {
                            FoundationEcTreeNode node = new FoundationEcTreeNode();
                            node.setCode(entity.getCode());
                            node.setName(signatureInternationalResource.getI18nValue(entity.getName(), locale));
                            node.setIsParent(true);
                            treeList.add(node);
                        }
                    }

                } else {
                    for (Entity e : entityList) {
                        FoundationEcTreeNode node = new FoundationEcTreeNode();
                        node.setCode(e.getCode());
                        node.setName(signatureInternationalResource.getI18nValue(e.getName(), locale));
                        node.setIsParent(true);
                        treeList.add(node);
                    }
                }

                break;
            case 2:
                List<View> viewList = viewServiceFoundation.findViews(code, ViewType.EDIT, ViewType.VIEW, ViewType.LIST, ViewType.EXTRA, ViewType.TREE);
                if (viewList != null && viewList.size() > 0) {
                    for (int i = 0; i < viewList.size(); i++) {
                        View v = viewList.get(i);
                        if (!v.getMobile() && !v.getIsShadow()) {
                            FoundationEcTreeNode node = new FoundationEcTreeNode();
                            node.setCode(v.getCode());
                            node.setName(signatureInternationalResource.getI18nValue(v.getDisplayName(), locale) + "[" + v.getName() + "]");
                            node.setIsParent(false);
                            treeList.add(node);
                        }
                    }
                }
                break;
            default:
                throw new SignatureException(SignatureErrorEnum.LEVEL_UNKNOWN_TYPE_ERROR);
        }
        return Result.data(treeList);
    }

    /**
     * @param
     * @param
     * @return
     */
    @ApiOperation("根据视图code分页获取按钮信息")
    @PostMapping(value = "/inter-api/signature/foundation/projectButton/buttonListQuery")
    @ResponseBody
    public PageResult<ButtonResponseVO> buttonListQuery(@Valid GetButtonVO getButtonVO) {
        Locale locale = LocaleContextHolder.getLocale();
        View view = viewServiceFoundation.getView(getButtonVO.getCode());
        if (view == null) {
            throw new SignatureException(SignatureErrorEnum.CODE_INVALID_ERROR);
        }

        IPage<EcButton> buttonsRequireSign = buttonService.getButtonsRequireSign(view,new Page<EcButton>(getButtonVO.getCurrent(), getButtonVO.getPageSize()));
        List<EcButton> result = buttonsRequireSign.getRecords();
        ArrayList<ButtonResponseVO> buttonResponseVOS = new ArrayList<>();
        result.forEach(button -> {
            ButtonResponseVO buttonDto = new ButtonResponseVO();
            BeanUtils.copyProperties(button, buttonDto);
            String displayName = buttonDto.getDisplayName();
            if (StringUtils.isNotBlank(displayName)) {
                buttonDto.setName(signatureInternationalResource.getI18nValue(displayName, locale));
            }
            //通过签名类型,和对应的id查询name
            Boolean signatureEnabled = buttonDto.getSignatureEnabled();
            if (signatureEnabled && "doubleSign".equals(buttonDto.getSignatureType())) {
                String powerType = buttonDto.getPowerType();
                switch (powerType.toLowerCase()) {
                    case ButtonPowerTypeConstant.STAFF:
                        String signerIds = buttonDto.getSignerId();
                        List<PersonVo> persons = getPersonNames(signerIds);
                        buttonDto.setPersons(persons);
                        break;
                    case ButtonPowerTypeConstant.POSITION:
                        String positionIds = buttonDto.getPositionId();
                        List<PositionVo> positions = getPositionNames(positionIds);
                        buttonDto.setPositions(positions);
                        break;
                    case ButtonPowerTypeConstant.ROLE:
                        String roleIds = buttonDto.getRoleId();
                        List<RoleVo> roles = getRoleNames(roleIds);
                        buttonDto.setRoles(roles);
                        break;
                }
            }
            buttonResponseVOS.add(buttonDto);
        });
        long current = buttonsRequireSign.getCurrent();
        long size = buttonsRequireSign.getSize();
        long total = buttonsRequireSign.getTotal();

        return new PageResult<ButtonResponseVO>(buttonResponseVOS,total,size,current );
    }

    /**
     * 保存电子签名的配置
     *
     * @param buttonVO
     * @return
     */
    @ApiModelProperty("保存数据")
    @PostMapping(value = "/inter-api/signature/foundation/projectButton/signatureConfig/save")
    @ResponseBody
    public Map<String, Boolean> siangtureConfigSave(@RequestBody ButtonVO buttonVO) {
        String buttonCode = buttonVO.getButtonCode();
        if (StringUtils.isBlank(buttonCode)) {
            throw new SignatureException(SignatureErrorEnum.BUTTON_CODE_CANNOT_BE_EMPTY_ERROR);
        }
        Button db_button = buttonService.getButtonByCode(buttonCode);
        if (db_button == null) {
            throw new SignatureException(SignatureErrorEnum.BUTTON_CODE_INVALID_ERROR);
        }
        Boolean signatureEnabled = buttonVO.getSignatureEnabled();
        db_button.setSignatureEnabled(signatureEnabled);
        if (signatureEnabled) {
            db_button.setSignatureDescrible(buttonVO.getSignatureDescrible());
            String signatureType = buttonVO.getSignatureType();
            if (StringUtils.isNotBlank(signatureType)) {
                db_button.setSignatureType(signatureType);
                if ("doubleSign".equals(signatureType)) {
                    String powerType = buttonVO.getPowerType();
                    String staffMultiIDs = multiIDsToString(buttonVO.getStaffMultiIDs());
                    String positionMultiIDs = multiIDsToString(buttonVO.getPositionMultiIDs());
                    String roleMultiIDs = multiIDsToString(buttonVO.getRoleMultiIDs());
                    //校验数据正确性
                    checkData(powerType, staffMultiIDs, positionMultiIDs, roleMultiIDs);

                    db_button.setPowerType(powerType);
                    db_button.setSignerId(staffMultiIDs);
                    db_button.setPositionId(positionMultiIDs);
                    db_button.setRoleId(roleMultiIDs);
                }
            } else {
                throw new SignatureException(SignatureErrorEnum.SIGNATURE_TYPE_CANNOT_BE_EMPTY_ERROR);
            }
        }
        buttonService.saveButton(db_button);
        Map<String, Boolean> responseMap = new HashMap<>();
        responseMap.put("dealSuccessFlag", true);
        return responseMap;
    }

    /**
     * 获取指定按钮是否启用电子签名
     *
     * @param buttonCode
     * @return
     */
    @GetMapping(value = "/inter-api/signature/buttonSignature/getSignatureEnabled")
    @ResponseBody
    @ApiOperation("获取指定按钮是否启用电子签名")
    public Result<Map<String, Object>> getButtonInfo(
            @ApiParam(value = "按钮code", required = false) @RequestParam(required = false) String buttonCode) {
        Locale locale = LocaleContextHolder.getLocale();
        Map<String, Object> responseMap = new HashMap<String, Object>();
        responseMap.put("signatureEnabled", false);
        if (buttonCode != null && !buttonCode.isEmpty()) {
            Button button = buttonService.getButtonByCode(buttonCode);
            if (null != button) {
                responseMap.put("signatureEnabled", button.getSignatureEnabled());
                responseMap.put("signatureType", button.getSignatureType());
                //国际化
                String buttonName = signatureInternationalResource.getI18nValue(button.getDisplayName(), locale);
                responseMap.put("buttonName", buttonName);
                String viewCode = null;
                if (button.getOperateType() == OperateType.ADD || button.getOperateType() == OperateType.MODIFY) {
                    viewCode = button.getViewSelectCode();
                } else {
                    viewCode = button.getViewCode();
                }
                if (StringUtils.isBlank(viewCode)){
                    String dgCode = button.getDataGridCode();
                    DataGrid dataGrid = dataGridService.getById(dgCode);
                    if (dataGrid != null) {
                        viewCode = dataGrid.getViewCode();
                    }
                }
                View view = viewServiceFoundation.getView(viewCode);
                if (view != null) {
                    String viewName = view.getTitle();
                    viewName = signatureInternationalResource.getI18nValue(viewName, locale);// internationalResource.get(viewName,this.getLocale().toString());
                    responseMap.put("viewName", viewName);
                }
            }
        }
        return Result.data(responseMap);
    }

    /**
     * 获取是否允许密码为空
     *
     * @return
     */
    @GetMapping(value = "/inter-api/signature/pwdAllowEmpty/pwdAllowEmpty")
    @ResponseBody
    public Boolean pwdAllowEmpty() {
        return true;
    }

    /**
     * 电子签名验证
     *
     * @return
     */
    @ApiOperation("电子签名验证")
    @PostMapping(value = "/inter-api/signature/checkUserPassword/check")
    @ResponseBody
    public Result<Map<String, Object>> check(@Validated @RequestBody CheckUserPasswordVO checkUserPasswordVO) {

        Map<String, Object> responseMap = electronicSignatureService.signatureAuthenticate(checkUserPasswordVO.getIsFirstSigner(),
                checkUserPasswordVO.getUsername(), checkUserPasswordVO.getPassword(), checkUserPasswordVO.getButtonCode());
        return Result.data(responseMap);
    }

    /**
     * 获取基础外所有模块
     *
     * @return
     */
    @ApiOperation("获取基础外所有模块")
    @GetMapping(value = "/inter-api/signature/signatureLogQuery/signatureLogQuery", produces = "application/json")
    @ResponseBody
    public Result<Map<String, Object>> signatureLogs() {
        Map<String, Object> responseMap = new HashMap<String, Object>();
        List<Module> moduleList = moduleService.getAllModule();
        for (int i = 0; i < moduleList.size(); i++) {
            Module module = moduleList.get(i);
            String artifact = module.getArtifact();
            if ("foundation".equals(artifact)) {
                moduleList.remove(i);
                break;
            }
        }
        responseMap.put("moduleList", moduleList);
        return Result.data(responseMap);
    }

    /**
     * 根据流程id拿到是否签名属性
     *
     * @param deploymentId  已生效单据不传
     * @param activityName  总是需要传
     * @param fvTableInfoId 已生效单据才传
     * @param modelCode     已生效单据才传
     * @return
     */
    @ApiOperation("根据流程id拿到是否签名属性")
    @GetMapping(value = "/inter-api/signature/workflow/getSignatureEnable")
    @ResponseBody
    public Result<Map<String, Object>> getSignatureByDeplomentId(
            HttpServletRequest request,
            @RequestParam(value = "deploymentId", required = false) @ApiParam(value = "流程id") Long deploymentId,
            @RequestParam(value = "fvTableInfoId", required = false) @ApiParam(value = "fvTableInfoId") Long fvTableInfoId,
            @RequestParam(value = "modelCode", required = false) @ApiParam(value = "模型编码") String modelCode,
            @RequestParam(value = "activityName") @ApiParam(value = "活动名称", required = true) String activityName
    ) throws IOException {
        Locale locale = LocaleContextHolder.getLocale();

        Map<String, Object> responseMap = new HashMap<String, Object>();
        if (null != deploymentId) {
            //调度id获取调度信息
            Boolean signature = signatureService.getDeployment(deploymentId);
            if (signature) {
                activityName = request.getParameter("activityName");
                String transitionCode = request.getParameter("transitionCode");
                if (null != activityName) {
                    //通过活动名称和调度id,获取任务
                    WfTask task = signatureService.getTask(activityName, deploymentId);
                    if (null != task) {
                        String taskName = signatureInternationalResource.getI18nValue(task.getName(), locale);
                        responseMap.put("taskName", taskName);
                    }

                }
                if (null != transitionCode) {
                    WfTransition transition = signatureService.getTransition(transitionCode, deploymentId);
                    if (null != transition) {
                        String transitionName = signatureInternationalResource.getI18nValue(transition.getName(), locale);
                        responseMap.put("transitionName", transitionName);
                    }
                }

            }
            responseMap.put("isSignature", signature);
        } else {
            Object[] result = signatureService.getSignatureEnable(fvTableInfoId, modelCode);
            Boolean signature = (Boolean) result[0];
            String processName = (String) result[1];
            responseMap.put("isSignature", signature);
            if (null != activityName && activityName.equals("retrial") && null != processName) {
                processName = signatureInternationalResource.getI18nValue(processName, locale);
                responseMap.put("processName", processName);
            }
        }
        return Result.data(responseMap);
    }


    private void checkData(String powerType, String staffMultiIDs, String positionMultiIDs, String roleMultiIDs) {
        Map<String, List> nameListMap = queryNameById(powerType, staffMultiIDs, positionMultiIDs, roleMultiIDs);
        if (nameListMap == null) {
            //powerType类型的数据不存在
            throw new SignatureException(SignatureErrorEnum.DATA_NOT_EXIST_ERROR, powerType);
        }
    }

    private String multiIDsToString(List<String> ids) {
        if (ids == null || ids.size() <= 0) {
            return null;
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            if (i != 0) {
                stringBuilder.append(",");
            }
            stringBuilder.append(ids.get(i));
        }
        return stringBuilder.toString();
    }

    private Map<String, List> queryNameById(String powerType, String signerIds, String positionIds, String roleIds) {
        if (StringUtils.isBlank(powerType)) {
            return null;
        }
        Map<String, List> stringListHashMap = new HashMap<>();
        switch (powerType.toLowerCase()) {
            case ButtonPowerTypeConstant.STAFF:
                List<PersonVo> personNames = getPersonNames(signerIds);
                if (personNames == null) {
                    return null;
                }
                stringListHashMap.put(ButtonPowerTypeConstant.STAFF, personNames);
                return stringListHashMap;
            case ButtonPowerTypeConstant.POSITION:
                List<PositionVo> positionNames = getPositionNames(positionIds);
                if (positionNames == null) {
                    return null;
                }
                stringListHashMap.put(ButtonPowerTypeConstant.POSITION, positionNames);
                return stringListHashMap;
            case ButtonPowerTypeConstant.ROLE:
                List<RoleVo> roleNames = getRoleNames(roleIds);
                if (roleNames == null) {
                    return null;
                }
                stringListHashMap.put(ButtonPowerTypeConstant.ROLE, roleNames);
                return stringListHashMap;
            default:
                throw new SignatureException(SignatureErrorEnum.UNKNOWN_TYPE_ERROR, "powerType", powerType);
        }
    }

    //查询岗位
    private List<PositionVo> getPositionNames(String positionIdStr) {
        String[] positionIds = positionIdStr.split(",");
        ArrayList<Long> ids = new ArrayList<>();
        for (int i = 0; i < positionIds.length; i++) {
            ids.add(Long.valueOf(positionIds[i]));
        }
        ListResult<PositionDetailDTO> positionDetailDTOs = personApiService.queryPositionsByIds(ids);

        if (positionDetailDTOs == null) {
            return null;
        }
        Collection<PositionDetailDTO> list = positionDetailDTOs.getList();
        if (list == null ||list.size() <= 0) {
            return null;
        }
        List<PositionVo> positionVos = new ArrayList<>();
        for (PositionDetailDTO position : list) {
            PositionVo positionVo = new PositionVo();
            BeanUtils.copyProperties(position, positionVo);
            positionVos.add(positionVo);
        }
        return positionVos;
    }

    //查询角色
    private List<RoleVo> getRoleNames(String roleId) {
        String[] roleIds = roleId.split(",");
        ArrayList<Long> ids = new ArrayList<>();
        for (int i = 0; i < roleIds.length; i++) {
            ids.add(Long.valueOf(roleIds[i]));
        }
        List<RoleDTO> roleByIds = iRoleApiService.findRoleByIds(ids);
        if (roleByIds == null || roleByIds.size() <= 0) {
            return null;
        }
        List<RoleVo> roleVos = new ArrayList<>();
        for (RoleDTO roleDTO : roleByIds) {
            RoleVo roleVo = new RoleVo();
            BeanUtils.copyProperties(roleDTO, roleVo);
            roleVos.add(roleVo);
        }
        return roleVos;
    }

    //查询人员
    private List<PersonVo> getPersonNames(String signerId) {
        String[] persons = signerId.split(",");
        Long[] personIds = new Long[persons.length];
        for (int i = 0; i < persons.length; i++) {
            personIds[i] = Long.valueOf(persons[i]);
        }
        Map<Long, PersonDTO> personDtos = personApiService.queryPersonsById(personIds);
        if (personDtos == null || personDtos.size() <= 0) {
            return null;
        }
        List<PersonVo> personVos = new ArrayList<>();
        personDtos.forEach((k, personDTO) -> {
            PersonVo personVo = new PersonVo();
            BeanUtils.copyProperties(personDTO, personVo);
            personVos.add(personVo);
        });
        return personVos;
    }

    private Long[] toLongArray(String signerId) {
        String[] ids = signerId.split(",");
        Long[] longs = new Long[ids.length];
        for (int i = 0; i < ids.length; i++) {
            longs[i] = Long.valueOf(ids[i]);
        }
        return longs;
    }

}
