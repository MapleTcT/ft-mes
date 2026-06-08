package com.supcon.supfusion.auth.service.excel.entity;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.collect.Lists;
import com.supcon.supfusion.auth.common.constants.Constants;
import com.supcon.supfusion.auth.common.constants.UserTypeEnum;
import com.supcon.supfusion.auth.common.model.ExcelProgress;
import com.supcon.supfusion.auth.common.utils.BCryptUtil;
import com.supcon.supfusion.auth.common.utils.ExcelUtils;
import com.supcon.supfusion.auth.dao.po.AuthExcelPO;
import com.supcon.supfusion.auth.dao.po.UserPO;
import com.supcon.supfusion.auth.dao.po.UserRolePO;
import com.supcon.supfusion.auth.manager.PersonServiceAdapter;
import com.supcon.supfusion.auth.manager.RbacServiceAdapter;
import com.supcon.supfusion.auth.manager.bo.LoginConfigBO;
import com.supcon.supfusion.auth.service.PasswordService;
import com.supcon.supfusion.auth.service.UserRoleService;
import com.supcon.supfusion.auth.service.UserService;
import com.supcon.supfusion.auth.service.bo.UserBO;
import com.supcon.supfusion.auth.service.cache.UserCache;
import com.supcon.supfusion.framework.cloud.common.context.UserContext;
import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;
import com.supcon.supfusion.organization.api.dto.CompanyDTO;
import com.supcon.supfusion.organization.api.dto.PersonDetailDTO;
import com.supcon.supfusion.organization.api.dto.PersonUserDTO;
import com.supcon.supfusion.rbac.api.dto.RoleFRDTO;
import com.supcon.supfusion.rbac.api.dto.RoleUserFRDTO;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.File;
import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.supcon.supfusion.auth.common.exception.UserErrorEnum.EXCEL_IMPORT_FAILED_ROWS_EMPTY;
import static com.supcon.supfusion.auth.common.exception.UserErrorEnum.EXCEL_TITLE_WRONG;
import static com.supcon.supfusion.auth.common.utils.ExcelUtils.*;

/**
 * @author lifangyuan
 */
@Slf4j
@Component
@Scope("prototype")
public class UserEntityListener extends AnalysisEventListener<UserEntity> {

    private static Pattern pattern = Pattern.compile("^\\w+$");

    private static String bigSmall = "(?=.*[a-z])(?=.*[A-Z])";

    private static String number = "(?=.*[0-9])";

    private static String special = "(?=.*[!@#$%^&*();'?.,])";

    private static String all = "[A-Za-z0-9!@#$%^&*();'?.,]";


    CustomCellWriteHandler customCellWriteHandler = new CustomCellWriteHandler();

    @Resource
    private UserService userService;

    @Resource
    private UserRoleService userRoleService;

    @Resource
    private UserCache userCache;

    @Getter
    private AuthExcelPO authExcelPO;

    private Boolean success = true;

    private Boolean hasContent = false;

    @Resource
    private PersonServiceAdapter personServiceAdapter;

    @Resource
    private RbacServiceAdapter rbacServiceAdapter;

    List<UserPO> userPOS = new ArrayList<>();

    List<UserRolePO> userRolePOS = new ArrayList<>();

    List<UserEntity> userEntities = new ArrayList<>();

    Set<String> usernames = new HashSet<>();

    Set<String> perdonCodes = new HashSet<>();

    List<RoleUserFRDTO> roleUserAddBatchDTOS = new ArrayList<>();

    int row = 0;

    int update = 0;

    int add = 0;

    @Getter
    @Setter
    private String lockName;

    private String passwordRemind;

    private Pattern passwordRex;


    @Autowired
    private PasswordService passwordService;

    @PostConstruct
    public void init() {
        StringBuilder str = new StringBuilder();
        StringBuilder remind = new StringBuilder();
        LoginConfigBO loginConfig = passwordService.getLoginConfig();
        if (Constants.COMBINATION_PWD_RULE.compareTo(loginConfig.getRuleType())==0) {
            if (loginConfig.getBigSmall() != null && loginConfig.getBigSmall()) {
                str.append(bigSmall);
                remind.append("大小写、");
            }
            if (loginConfig.getNumber() != null && loginConfig.getNumber()) {
                str.append(number);
                remind.append("数字、");
            }
            if (loginConfig.getSpecialChar() != null && loginConfig.getSpecialChar()) {
                str.append(special);
                remind.append("特殊字符（仅支持!@#$%^&*();'?.,)、");
            }
        } else {
            str.append(loginConfig.getRegularExpression());
        }
        if (loginConfig.getMin() != null && loginConfig.getMax() != null) {
            str.append(".{").append(loginConfig.getMin()).append(",").append(loginConfig.getMax()).append("}");
        }
        if (Constants.COMBINATION_PWD_RULE.compareTo(loginConfig.getRuleType())==0) {
            String composeStr = !StringUtils.isEmpty(remind.toString()) ? remind.substring(0, remind.length() - 1) : "大小写、数字、特殊字符（仅支持!@#$%^&*();'?.,)";
            passwordRemind = String.format(Constants.RULE_ERROR, composeStr, loginConfig.getMin(), loginConfig.getMax());
        } else {
            passwordRemind = loginConfig.getHint();
        }
        passwordRex = Pattern.compile("^" + str.toString() + "$");
    }

    @Override
    public void invoke(UserEntity userEntity, AnalysisContext analysisContext) {
        try {
            hasContent = true;
            int currentRow = ++row;
            userEntities.add(userEntity);
            String randomId = UUID.randomUUID().toString();
            log.info("uuid====>"+randomId);
            if (success) {
                long now = System.currentTimeMillis();
                boolean validateEntity = EasyExcelValiHelper.validateEntity(userEntity, currentRow, customCellWriteHandler, passwordRex, passwordRemind);
                UserOrgRbacEntity temp = verify(currentRow, userEntity, validateEntity);
                addUserPO(temp);
                if (temp != null) {
                    if (temp.getUpdate()) {
                        update++;
                    } else {
                        add++;
                    }
                }
                log.info("cost total====>" + (System.currentTimeMillis() - now));
            }
        } catch (Exception e) {
            log.error("data is error", e);
//            authExcelPO.setStatus(ExcelProgress.FAIL.getProgress());
//            authExcelPO.setErrorMessage(ExcelProgress.FAIL.getDescription());
        }

    }

    private UserPO addUserPO(UserOrgRbacEntity temp) {
        UserPO userPO = new UserPO();
        long now1 = System.currentTimeMillis();
        try {
            BeanUtils.copyProperties(temp.getUserBO(), userPO);
            if (!temp.getUpdate()) {
                ListResult<Long> result = personServiceAdapter.queryRoleIdByPersonId(userPO.getPersonId());
                log.info("cost get role by id=====>" + (System.currentTimeMillis() - now1));
                if (result.getList() != null) {
                    List<UserRolePO> collect = result.getList().stream().map(t -> {
                        UserRolePO userRolePO = new UserRolePO();
                        userRolePO.setUserId(userPO.getId());
                        userRolePO.setRoleId(t);
                        userRolePO.setRoleType(Constants.ROLE_ORG);
                        userRolePO.setId(IDGenerator.newInstance().generate().longValue());
                        return userRolePO;
                    }).collect(Collectors.toList());
                    userRolePOS.addAll(collect);
                    addUserRolesToRbac(userPO, collect, temp.getUserBO().getPersonCode(), temp.getUserBO().getPersonName());
                }
            }
        } catch (Exception e) {
            log.error("erroe is =====> " + e);
            success = false;
        }
        userPOS.add(userPO);
        log.info("add cost total=====>" + (System.currentTimeMillis() - now1));
        return userPO;
    }

    /**
     * 将用户和角色的关联发送给rbac
     */
    void addUserRolesToRbac(UserPO userBo, List<UserRolePO> role, String personCode, String personName) {
        RoleUserFRDTO roleUserAddBatchDTO = new RoleUserFRDTO();
        roleUserAddBatchDTO.setUserId(userBo.getId());
        roleUserAddBatchDTO.setUserName(userBo.getUserName());
        roleUserAddBatchDTO.setCid(userBo.getCompanyId());
        roleUserAddBatchDTO.setPersonCode(personCode);
        roleUserAddBatchDTO.setPersonName(personName);
        if (role != null && !role.isEmpty()) {
            List<RoleFRDTO> roleFRDTOS = new ArrayList<>();
            role.forEach(t -> {
                RoleFRDTO roleFRDTO = new RoleFRDTO();
                roleFRDTO.setId(t.getRoleId());
                roleFRDTO.setFromPosition(t.getRoleType());
                roleFRDTOS.add(roleFRDTO);
            });
            roleUserAddBatchDTO.setRoles(roleFRDTOS);
        } else {
            roleUserAddBatchDTO.setRoles(new ArrayList<>());
        }
        roleUserAddBatchDTOS.add(roleUserAddBatchDTO);
    }


    private UserOrgRbacEntity verify(int row, UserEntity userEntity, boolean validateEntity) {
        UserOrgRbacEntity entity = new UserOrgRbacEntity();
        try {
            long now2 = System.currentTimeMillis();
            if (!validateEntity) {
                success = false;
                return entity;
            }
            if (!usernames.add(userEntity.getUserName())) {
                customCellWriteHandler.put(row, 0, "用户名重复");
                success = false;
                return entity;
            }
            if (!perdonCodes.add(userEntity.getPersonCode())) {
                customCellWriteHandler.put(row, 3, "人员编码重复");
                success = false;
                return entity;
            }
            UserBO userBO = userService.selectUserName(userEntity.getUserName());
            log.info("userName=======>" + (System.currentTimeMillis() - now2));
            if (userBO.getValid() != null && !userBO.getValid()) {
                customCellWriteHandler.put(row, 0, "用户已被删除，该用户名不可使用");
                success = false;
                return entity;
            } else {
                userBO.setCreateTime(null);
                userBO.setModifyTime(null);
                entity.setUpdate(userBO.getId() != null);
                entity.setUserBO(userBO);
            }
            entity.getUserBO().setUserName(userEntity.getUserName());
            entity.getUserBO().setPassword(BCryptUtil.encode(userEntity.getPassword()));
            if (StringUtils.isNotEmpty(userEntity.getDescription())) {
                entity.getUserBO().setDescription(userEntity.getDescription());
            }
            if (entity.getUserBO().getId() == null) {
                entity.getUserBO().setId(IDGenerator.newInstance().generate().longValue());
                UserContext userContext = UserContext.getUserContext();
                Long companyId = userContext.getCompanyId();
                entity.getUserBO().setCompanyId(companyId);
                entity.getUserBO().setUserType(UserTypeEnum.COMMON_USER.getCode());
            }
            String personCode = userEntity.getPersonCode();
            ArrayList<String> arrayList = Lists.newArrayList(personCode);
            ListResult<PersonDetailDTO> personList = personServiceAdapter.queryPersonByCodes(arrayList);
            log.info("cost get personByCode=======>" + (System.currentTimeMillis() - now2));
            ArrayList<PersonDetailDTO> list = (ArrayList<PersonDetailDTO>) personList.getList();
            if (list == null || list.isEmpty()) {
                customCellWriteHandler.put(row, 3, "该人员编码不存在");
                success = false;
                return entity;
            } else {
                PersonDetailDTO personDetailDTO = list.get(0);
                if (!personDetailDTO.getValid()) {
                    customCellWriteHandler.put(row, 3, "该人员已被删除");
                    success = false;
                    return entity;
                }
                ListResult<CompanyDTO> companys = personServiceAdapter.queryCompanyIdByPersonId(personDetailDTO.getId());
                log.info("cost get person company=======>" + (System.currentTimeMillis() - now2));
                boolean exit = companys.getList().stream().anyMatch(t -> t.getId().compareTo(entity.getUserBO().getCompanyId()) == 0);
                if (!exit) {
                    customCellWriteHandler.put(row, 3, "该人员不属于当前用户所属公司");
                    success = false;
                    return entity;
                }
                if (entity.getUpdate()) {
                    if (entity.getUserBO().getPersonId().compareTo(personDetailDTO.getId()) != 0) {
                        customCellWriteHandler.put(row, 3, "不可修改用户关联的人员");
                        success = false;
                        return entity;
                    }
                }
                List<UserPO> userPOS = userService.lambdaQuery().getBaseMapper().selectList(Wrappers.lambdaQuery(UserPO.class).eq(UserPO::getPersonId, personDetailDTO.getId()));
                log.info("personID=======>" + (System.currentTimeMillis() - now2));
                if (userPOS != null && userPOS.size() > 0) {
                    if (!userPOS.get(0).getUserName().equals(userEntity.getUserName())) {
                        customCellWriteHandler.put(row, 3, "该人员已被用户绑定，不可使用");
                        success = false;
                        return entity;
                    }
                }
                if (!personDetailDTO.getName().equals(userEntity.getPersonName())) {
                    customCellWriteHandler.put(row, 2, "人员名称不匹配");
                    success = false;
                    return entity;
                }
                entity.getUserBO().setPersonName(list.get(0).getName());
                entity.getUserBO().setPersonCode(list.get(0).getCode());
                entity.getUserBO().setPersonId(list.get(0).getId());
                entity.setPersonDetailDTO(list.get(0));
                log.info("verify cost=======>" + (System.currentTimeMillis() - now2));
            }

        } catch (Exception e) {
            success = false;
        }
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
        try {
            if (!hasContent) {
                authExcelPO.setStatus(ExcelProgress.FAIL.getProgress());
                authExcelPO.setErrorMessage(EXCEL_IMPORT_FAILED_ROWS_EMPTY.getMessage());
                return;
            }
            if (success) {
                userService.saveOrUpdateBatch(userPOS, 100);

                //同步人员表中冗余的用户信息
                List<PersonUserDTO> personUserDTOS = userPOS.stream().map(userPO ->
                     new PersonUserDTO(userPO.getPersonId(), userPO.getId(), userPO.getUserName())
                ).collect(Collectors.toList());
                personServiceAdapter.saveOrUpdateUsers(personUserDTOS);

                userRoleService.saveOrUpdateBatch(userRolePOS, 100);
                authExcelPO.setStatus(ExcelProgress.FINISH.getProgress());
                authExcelPO.setUpdateNum(update);
                authExcelPO.setAddNum(add);
                List<RoleUserFRDTO> roleUserFRDTOS = new ArrayList<>();
                for (int i = 0; i < roleUserAddBatchDTOS.size(); i++) {
                    roleUserFRDTOS.add(roleUserAddBatchDTOS.get(i));
                    if (roleUserFRDTOS.size() == 10) {
                        long now = System.currentTimeMillis();
                        rbacServiceAdapter.batchSaveOneUserFR(roleUserFRDTOS);
                        log.info("sysnc role cost time====>" + (System.currentTimeMillis() - now));
                        roleUserFRDTOS.clear();
                    }
                    if (i == roleUserAddBatchDTOS.size() - 1) {
                        rbacServiceAdapter.batchSaveOneUserFR(roleUserFRDTOS);
                        roleUserFRDTOS.clear();
                    }
                }

            } else {
                File dir = new File(EXCEL_ERROR_FILE_PATH);
                if (!dir.exists()) {
                    boolean mkdirs = dir.mkdirs();
                }
                String filePath = EXCEL_ERROR_FILE_PATH + authExcelPO.getId() + "_" + USER_FILE + ".xlsx";
                ExcelWriter writer = EasyExcel.write(filePath).build();
                WriteSheet writeSheet1 = EasyExcel.writerSheet(0, EXPLIAN).head(Explain.class).build();
                WriteSheet writeSheet = EasyExcel.writerSheet(1, USER_FILE).head(UserEntity.class).registerWriteHandler(customCellWriteHandler).build();
                writer.write(getData(), writeSheet1);
                writer.write(userEntities, writeSheet);
                authExcelPO.setStatus(ExcelProgress.FAIL.getProgress());
                authExcelPO.setFileName(authExcelPO.getId() + "_" + USER_FILE + ".xlsx");
                authExcelPO.setErrorMessage(ExcelProgress.FAIL.getDescription());
                writer.finish();
            }
        } catch (Exception e) {
            log.error("doAfterAllAnalysed error is",e);
//            authExcelPO.setStatus(ExcelProgress.FAIL.getProgress());
//            authExcelPO.setErrorMessage(ExcelProgress.FAIL.getDescription());
        } finally {
            userService.excuteExcelState(authExcelPO);
        }
    }

    @Override
    public void onException(Exception exception, AnalysisContext context) throws Exception {
        log.error("import excel listener error =====>", exception);
        authExcelPO.setStatus(ExcelProgress.FAIL.getProgress());
        authExcelPO.setErrorMessage(ExcelProgress.FAIL.getDescription());
        userService.excuteExcelState(authExcelPO);
    }

    @Override
    public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
        super.invokeHeadMap(headMap, context);
        customCellWriteHandler.put(headMap);
        try {
            Map<Integer, String> indexNameMap = getIndexNameMap(UserEntity.class);
            Set<Integer> keySet = indexNameMap.keySet();
            for (Integer key : keySet) {
                if (StringUtils.isEmpty(headMap.get(key))) {
                    customCellWriteHandler.put(row, key, EXCEL_TITLE_WRONG.getMessage());
                    success = false;
                }
                if (!headMap.get(key).equals(indexNameMap.get(key))) {
                    customCellWriteHandler.put(row, key, EXCEL_TITLE_WRONG.getMessage());
                    success = false;
                }
            }
        } catch (NoSuchFieldException e) {
            log.error("error ===>",e);
        }
    }

    public void setExcelPO(AuthExcelPO excelPO) {
        this.authExcelPO = excelPO;
    }

    /**
     * 构造假数据，实际上应该从数据库查出来
     *
     * @return List<UserEntity>
     */
    private List<Explain> getData() {
        return ExcelUtils.DEPARTMENT_TEMPLATE_EXPLAIN.stream().map(t -> {
            Explain explain = new Explain();
            explain.setString(t);
            return explain;
        }).collect(Collectors.toList());
    }

    /**
     * @param clazz
     * @return java.util.Map<java.lang.Integer, java.lang.String>
     * @throws
     * @description: 获取注解里ExcelProperty的value，用作校验excel
     * @author zhy
     * @date 2019/12/24 19:21
     */
    @SuppressWarnings("rawtypes")
    public Map<Integer, String> getIndexNameMap(Class clazz) throws NoSuchFieldException {
        Map<Integer, String> result = new HashMap<>();
        Field field;
        Field[] fields = clazz.getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            field = clazz.getDeclaredField(fields[i].getName());
            field.setAccessible(true);
            ExcelProperty excelProperty = field.getAnnotation(ExcelProperty.class);
            if (excelProperty != null) {
                int index = excelProperty.index();
                String[] values = excelProperty.value();
                StringBuilder value = new StringBuilder();
                for (String v : values) {
                    value.append(v);
                }
                result.put(index, value.toString());
            }
        }
        return result;
    }

}
