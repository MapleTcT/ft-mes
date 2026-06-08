package com.supcon.supfusion.auth.service.excel;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.supcon.supfusion.auth.common.constants.Constants;
import com.supcon.supfusion.auth.common.constants.UserTypeEnum;
import com.supcon.supfusion.auth.common.exception.UserErrorEnum;
import com.supcon.supfusion.auth.common.exception.UserException;
import com.supcon.supfusion.auth.common.model.ExcelProgress;
import com.supcon.supfusion.auth.common.utils.ExcelUtils;
import com.supcon.supfusion.auth.common.utils.ThreadPoolUtils;
import com.supcon.supfusion.auth.dao.mapper.ExcelOperateMapper;
import com.supcon.supfusion.auth.dao.po.AuthExcelPO;
import com.supcon.supfusion.auth.dao.po.UserPO;
import com.supcon.supfusion.auth.dao.po.UserRolePO;
import com.supcon.supfusion.auth.manager.PersonServiceAdapter;
import com.supcon.supfusion.auth.manager.RbacServiceAdapter;
import com.supcon.supfusion.auth.service.ExcelService;
import com.supcon.supfusion.auth.service.UserRoleService;
import com.supcon.supfusion.auth.service.UserService;
import com.supcon.supfusion.auth.service.bo.ExcelStatusBO;
import com.supcon.supfusion.auth.service.bo.UserBO;
import com.supcon.supfusion.auth.service.cache.UserCache;
import com.supcon.supfusion.auth.service.excel.entity.Explain;
import com.supcon.supfusion.auth.service.excel.entity.UserEntity;
import com.supcon.supfusion.auth.service.excel.entity.UserEntityListener;
import com.supcon.supfusion.auth.service.excel.entity.UserExportEntity;
import com.supcon.supfusion.framework.cloud.common.context.UserContext;
import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;
import com.supcon.supfusion.organization.api.dto.PersonDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.supcon.supfusion.auth.common.constants.Constants.EXCEL_EXPORT;
import static com.supcon.supfusion.auth.common.utils.ExcelUtils.*;

@Slf4j
@Service
public class ExcelServiceImpl implements ExcelService, ApplicationContextAware {

    @Resource
    private ExcelOperateMapper excelOperateMapper;

    @Resource
    private UserService userService;

    @Resource
    private UserRoleService userRoleService;

    @Resource
    private PersonServiceAdapter personServiceAdapter;

    @Resource
    private RbacServiceAdapter rbacServiceAdapter;

    @Resource
    private UserCache userCache;

    private ApplicationContext applicationContext;


    @Override
    public ExcelStatusBO importExcel(MultipartFile file) throws Exception {
        ExcelStatusBO excelStatusBO = new ExcelStatusBO();
        AuthExcelPO excelPO = new AuthExcelPO();
        excelPO.setId(IDGenerator.newInstance().generate().longValue());
        String name = file.getName();
        if (name.endsWith("xlsx") || name.endsWith("xls")) {
            excelPO.setStatus(ExcelProgress.FAIL.getProgress());
            excelPO.setType(Constants.EXCEL_IMPORT);
            excelPO.setErrorMessage(UserErrorEnum.EXCEL_IS_NOT.getMessage());
            excelOperateMapper.insert(excelPO);
            excelStatusBO.setId(excelPO.getId());
            excelStatusBO.setStatus(ExcelProgress.FAIL.getProgress());
            excelStatusBO.setErrorMessage(UserErrorEnum.EXCEL_IS_NOT.getMessage());
            return excelStatusBO;
        }
        if (file.getSize() < 0) {
            excelPO.setStatus(ExcelProgress.FAIL.getProgress());
            excelPO.setType(Constants.EXCEL_IMPORT);
            excelPO.setErrorMessage(UserErrorEnum.EXCEL_SIZE_EMPTY.getMessage());
            excelOperateMapper.insert(excelPO);
            excelStatusBO.setId(excelPO.getId());
            excelStatusBO.setStatus(ExcelProgress.FAIL.getProgress());
            excelStatusBO.setErrorMessage(UserErrorEnum.EXCEL_SIZE_EMPTY.getMessage());
            return excelStatusBO;
        }
        excelPO.setStatus(ExcelProgress.IN_PROGRESS.getProgress());
        excelPO.setType(Constants.EXCEL_IMPORT);
        String userName = UserContext.getUserContext().getUserName();
        excelOperateMapper.insert(excelPO);
        UserEntityListener bean = applicationContext.getBean(UserEntityListener.class);
        bean.setExcelPO(excelPO);
        bean.setLockName(userName);
        excelStatusBO.setId(excelPO.getId());
        excelStatusBO.setStatus(ExcelProgress.IN_PROGRESS.getProgress());
        try {
            InputStream inputStream = file.getInputStream();
            ThreadPoolUtils.getThreadPool().execute(() -> {
                EasyExcel.read(inputStream, UserEntity.class, bean).sheet(USER_FILE).doRead();
            });
        } catch (IOException e) {
            excelStatusBO.setId(excelPO.getId());
            excelStatusBO.setHasErrorFile(false);
            excelStatusBO.setStatus(ExcelProgress.FAIL.getProgress());
            excelStatusBO.setErrorMessage("导入文件未读到，请重试");
            excelPO.setStatus(ExcelProgress.FAIL.getProgress());
            excelOperateMapper.updateById(excelPO);
            log.error("import excel is error", e);
        }


//        redisLockUtil.unlock(userName);
        return excelStatusBO;
    }

    @Override
    public void downlowdExcelTemplate(HttpServletResponse response) throws IOException {
//        response.setContentType("application/vnd.ms-excel");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-disposition", "attachment;filename=" + URLEncoder.encode(USER_FILE, "UTF-8") + ".xlsx");
        ExcelWriter excelWriter = EasyExcel.write(response.getOutputStream()).build();
        WriteSheet writeSheet1 = EasyExcel.writerSheet(0, EXPLIAN).head(Explain.class).build();
        WriteSheet writeSheet2 = EasyExcel.writerSheet(1, USER_FILE).head(UserEntity.class).build();
        excelWriter.write(getData(), writeSheet1);
        excelWriter.write(Lists.newArrayList(), writeSheet2);
        excelWriter.finish();
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
     * 查询导入状态
     *
     * @param id
     * @return
     */
    @Override
    public ExcelStatusBO checkStatus(Long id) {
        AuthExcelPO excelPO = excelOperateMapper.selectById(id);
        if (excelPO == null) {
            throw new UserException(UserErrorEnum.EXCEL_IMPORT_TASH_NOT_EXISTS_ERROR);
        }
        ExcelStatusBO excelStatusBO = new ExcelStatusBO();
        excelStatusBO.setId(excelPO.getId());
        excelStatusBO.setStatus(excelPO.getStatus());
        excelStatusBO.setAddNum(excelPO.getAddNum());
        excelStatusBO.setUpdateNum(excelPO.getUpdateNum());
        if (excelPO.getStatus().equals(ExcelProgress.FAIL.getProgress())) {
            if (StringUtils.isNotEmpty(excelPO.getFileName())) {
                excelStatusBO.setHasErrorFile(true);
            }
            excelStatusBO.setErrorMessage(excelPO.getErrorMessage());
        }
        return excelStatusBO;
    }

    @Override
    public void downlowdExcel(Long id, HttpServletResponse response) throws IOException {
        AuthExcelPO excelPO = excelOperateMapper.selectById(id);
        if (excelPO == null) {
            throw new UserException(UserErrorEnum.EXCEL_IMPORT_TASH_NOT_EXISTS_ERROR);
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日HH时mm分ss秒");
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        String strDate = excelPO.getCreateTime();
        Date date = new Date();
        if (StringUtils.isNotBlank(strDate)) {
            try {
                date = df.parse(strDate);
            } catch (ParseException e) {
                date = new Date();
            }
        }

        String filePath = "";
        String fileName = "";
        if (EXCEL_EXPORT.equals(excelPO.getType()) && !excelPO.getStatus().equals(ExcelProgress.FAIL.getProgress())) {
            filePath = EXCEL_FILE_EXPORT_PATH + excelPO.getFileName();
            fileName = filePath.substring(filePath.lastIndexOf("_") + 1, filePath.length());
        } else {
            filePath = EXCEL_ERROR_FILE_PATH + excelPO.getFileName();
            fileName = filePath.substring(filePath.lastIndexOf("_") + 1, filePath.indexOf(".xlsx")) + sdf.format(date) + ".xlsx";
        }
        response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(fileName, "UTF-8"));
        BufferedInputStream inputStream = null;
        ServletOutputStream out = null;
        try {
            inputStream = new BufferedInputStream(new FileInputStream(filePath));
            out = response.getOutputStream();
            int b = 0;
            byte[] buffer = new byte[1024];
            while ((b = inputStream.read(buffer)) != -1) {
                //写到输出流(out)中
                out.write(buffer, 0, b);
            }
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }

            if (out != null) {
                out.flush();
                out.close();
            }
        }
    }

    @Override
    public ExcelStatusBO exportExcelData(List<Long> ids, Boolean all, String keyword, HttpServletResponse response) {
        ExcelStatusBO excelStatusBO = new ExcelStatusBO();
        AuthExcelPO excelPO = new AuthExcelPO();
        excelPO.setId(IDGenerator.newInstance().generate().longValue());
        excelPO.setStatus(ExcelProgress.IN_PROGRESS.getProgress());
        excelPO.setType(EXCEL_EXPORT);
        excelOperateMapper.insert(excelPO);
        Long companyId = UserContext.getUserContext().getCompanyId();
        ThreadPoolUtils.getThreadPool().execute(() -> {
            try {
                ArrayList<UserExportEntity> userExportEntities = new ArrayList<>();
                if (StringUtils.isNotEmpty(keyword) && all) {
                    List<UserBO> userBOS = searchUser(keyword,companyId);
                    if (!userBOS.isEmpty()) {
                        for (UserBO userBO : userBOS) {
                            UserExportEntity userExportEntity = new UserExportEntity();
                            userExportEntity.setUserName(userBO.getUserName());
                            userExportEntity.setDescription(userBO.getDescription());
                            if (userBO.getUserType().compareTo(UserTypeEnum.SYSTEM_USER.getCode()) == 0) {
                                userExportEntity.setUserType(UserTypeEnum.SYSTEM_USER.getName());
                            } else {
                                userExportEntity.setUserType(UserTypeEnum.COMMON_USER.getName());
                            }
                            userExportEntity.setPersonName(userBO.getPersonName());
                            List<UserRolePO> list = userRoleService.list(Wrappers.lambdaQuery(UserRolePO.class).eq(UserRolePO::getUserId, userBO.getId()).ne(UserRolePO::getRoleType,Constants.ROLE_ORG));
                            if (!list.isEmpty()) {
                                List<Long> collect = list.stream().map(UserRolePO::getRoleId).collect(Collectors.toList());
                                Map<Long, String> batchName = rbacServiceAdapter.findBatchName(Joiner.on(",").join(collect));
                                String join = Joiner.on(",").join(batchName.values());
                                userExportEntity.setRole(join);
                            }
                            userExportEntities.add(userExportEntity);
                        }
                    }
                } else if (!all && !ids.isEmpty()) {
                    List<UserPO> userPOS = userService.list(Wrappers.lambdaQuery(UserPO.class).in(UserPO::getId, ids).eq(UserPO::getCompanyId, companyId).orderByDesc(UserPO::getCreateTime));
                    for (UserPO userPO : userPOS) {
                        UserExportEntity userExportEntity = new UserExportEntity();
                        userExportEntity.setUserName(userPO.getUserName());
                        userExportEntity.setDescription(userPO.getDescription());
                        if (userPO.getUserType().compareTo(UserTypeEnum.SYSTEM_USER.getCode()) == 0) {
                            userExportEntity.setUserType(UserTypeEnum.SYSTEM_USER.getName());
                        } else {
                            userExportEntity.setUserType(UserTypeEnum.COMMON_USER.getName());
                        }
                        if (userPO.getPersonId() != null) {
                            Map<Long, PersonDTO> map = personServiceAdapter.queryPersonsById(new Long[]{userPO.getPersonId()});
                            PersonDTO personDTO = map.get(userPO.getPersonId());
                            userExportEntity.setPersonName(personDTO.getName());
                        }
                        List<UserRolePO> list = userRoleService.list(Wrappers.lambdaQuery(UserRolePO.class).eq(UserRolePO::getUserId, userPO.getId()).ne(UserRolePO::getRoleType, Constants.ROLE_ORG));
                        if (list != null && !list.isEmpty()) {
                            List<Long> collect = list.stream().map(UserRolePO::getRoleId).collect(Collectors.toList());
                            Map<Long, String> batchName = rbacServiceAdapter.findBatchName(Joiner.on(",").join(collect));
                            String join = Joiner.on(",").join(batchName.values());
                            userExportEntity.setRole(join);
                        }
                        userExportEntities.add(userExportEntity);
                    }
                } else {
                    LambdaQueryWrapper<UserPO> lambdaQueryWrapper = Wrappers.lambdaQuery(UserPO.class).eq(UserPO::getCompanyId, companyId);
                    ListResult<Long> personIds = personServiceAdapter.queryMultiCompanyPersonsByCompanyId(UserContext.getUserContext().getCompanyId());
                    if (personIds != null && personIds.getList() != null && !personIds.getList().isEmpty()) {
                        lambdaQueryWrapper.or(items -> {
                            for (Long personId : personIds.getList()) {
                                items.or().eq(UserPO::getPersonId, personId);
                            }
                            items.ne(UserPO::getCompanyId, UserContext.getUserContext().getCompanyId());
                        });

                    }
                    lambdaQueryWrapper.orderByDesc(UserPO::getCreateTime);

                    List<UserPO> userPOS = userService.list(lambdaQueryWrapper);
                    for (UserPO userPO : userPOS) {
                        UserExportEntity userExportEntity = new UserExportEntity();
                        userExportEntity.setUserName(userPO.getUserName());
                        userExportEntity.setDescription(userPO.getDescription());
                        if (userPO.getUserType().compareTo(UserTypeEnum.SYSTEM_USER.getCode()) == 0) {
                            userExportEntity.setUserType(UserTypeEnum.SYSTEM_USER.getName());
                        } else {
                            userExportEntity.setUserType(UserTypeEnum.COMMON_USER.getName());
                        }
                        if (userPO.getPersonId() != null) {
                            Map<Long, PersonDTO> map = personServiceAdapter.queryPersonsById(new Long[]{userPO.getPersonId()});
                            PersonDTO personDTO = map.get(userPO.getPersonId());
                            userExportEntity.setPersonName(personDTO.getName());
                        }
                        List<UserRolePO> list = userRoleService.list(Wrappers.lambdaQuery(UserRolePO.class).eq(UserRolePO::getUserId, userPO.getId()).ne(UserRolePO::getRoleType, Constants.ROLE_ORG));
                        if (list != null && !list.isEmpty()) {
                            List<Long> collect = list.stream().map(UserRolePO::getRoleId).collect(Collectors.toList());
                            Map<Long, String> batchName = rbacServiceAdapter.findBatchName(Joiner.on(",").join(collect));
                            String join = Joiner.on(",").join(batchName.values());
                            userExportEntity.setRole(join);
                        }
                        userExportEntities.add(userExportEntity);
                    }
                }
                File dir = new File(EXCEL_FILE_EXPORT_PATH);
                if (!dir.exists()) {
                    boolean mkFlag = dir.mkdirs();
                }
                String filePath = EXCEL_FILE_EXPORT_PATH + excelPO.getId() + "_" + USER_FILE + ".xlsx";
                ExcelWriter writer = EasyExcel.write(filePath).build();
                WriteSheet writeSheet1 = EasyExcel.writerSheet(0, EXPLIAN).head(Explain.class).build();
                WriteSheet writeSheet2 = EasyExcel.writerSheet(1, USER_FILE).head(UserExportEntity.class).build();
                writer.write(getData(), writeSheet1);
                writer.write(userExportEntities, writeSheet2);
                writer.finish();
                excelPO.setAddNum(userExportEntities.size());
                excelPO.setFileName(excelPO.getId() + "_" + USER_FILE + ".xlsx");
                excelPO.setStatus(ExcelProgress.FINISH.getProgress());
                excelOperateMapper.updateById(excelPO);
            } catch (Exception e) {
                log.error("export excel is error", e);
            }
        });
        excelStatusBO.setId(excelPO.getId());
        excelStatusBO.setStatus(ExcelProgress.IN_PROGRESS.getProgress());
        return excelStatusBO;
    }

    private List<UserBO> searchUser(String keyword,Long companyId) {
        List<UserPO> userPOS = userService.list(Wrappers.lambdaQuery(UserPO.class).eq(UserPO::getCompanyId, companyId).and(q -> {
            q.like(UserPO::getUserName, keyword).or().like(UserPO::getDescription, keyword).or().like(UserPO::getPersonName, keyword);
        }));
        List<UserBO> collect = userPOS.stream().map(t -> {
            UserBO userBO = new UserBO();
            BeanUtils.copyProperties(t, userBO);
            return userBO;
        }).collect(Collectors.toList());
        return collect;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public void excuteExcelState(AuthExcelPO excelPO) {
        excelOperateMapper.updateById(excelPO);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
