package com.supcon.supfusion.organization.service.impl;

import com.supcon.supfusion.organization.common.exception.*;
import com.supcon.supfusion.organization.common.utils.ExcelUtils;
import com.supcon.supfusion.organization.common.utils.ThreadPoolUtils;
import com.supcon.supfusion.organization.dao.mapper.excel.ExcelMapper;
import com.supcon.supfusion.organization.dao.po.excel.ExcelPO;
import com.supcon.supfusion.organization.service.DepartmentService;
import com.supcon.supfusion.organization.service.ExcelManageService;
import com.supcon.supfusion.organization.service.PersonService;
import com.supcon.supfusion.organization.service.PositionService;
import com.supcon.supfusion.organization.service.bo.excel.ExcelStatusBO;
import com.supcon.supfusion.organization.service.bo.person.PersonDetailBO;
import io.micrometer.core.instrument.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class ExcelManageServiceImpl implements ExcelManageService {

    @Autowired
    private PersonService personService;

    @Autowired
    private PositionService positionService;

    @Autowired
    private ExcelMapper excelMapper;

    @Autowired
    private DepartmentService departmentService;

    /**
     * Excel导入实现
     * @param companyId 公司id
     * @param type 导入类型
     * @param file Excel文件
     * @param tenantId
     * @param timeZone
     */
    @Override
    public ExcelStatusBO importExcel(Long companyId, String type, MultipartFile file, String tenantId, String timeZone) throws IOException {

        XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream());
        ExcelStatusBO excelStatusBO = new ExcelStatusBO();
/*        if (workbook == null) {
            throw new OrganizationException(OrganizationErrorEnum.EXCEL_SIZE_EMPTY);
        }*/
        ExcelPO excelPO = new ExcelPO();
        excelPO.setStatus(0);
        excelPO.setType("import");
        excelMapper.insert(excelPO);
        int sheetNum = workbook.getNumberOfSheets();
        if (sheetNum < 2) {
            excelPO.setStatus(3);
            excelPO.setErrorMessage(OrganizationErrorEnum.EXCEL_SHEET_ERROR.getMessage());
            excelMapper.updateById(excelPO);
            excelStatusBO.setId(excelPO.getId());
            excelStatusBO.setStatus(3);
            excelStatusBO.setErrorMessage(OrganizationErrorEnum.EXCEL_SHEET_ERROR.getMessage());
            return excelStatusBO;
        }
        Sheet sheet = workbook.getSheetAt(1);
        if (sheet == null) {
            excelPO.setStatus(3);
            excelPO.setErrorMessage(OrganizationErrorEnum.EXCEL_SHEET_ERROR.getMessage());
            excelMapper.updateById(excelPO);
            excelStatusBO.setId(excelPO.getId());
            excelStatusBO.setStatus(3);
            excelStatusBO.setErrorMessage(OrganizationErrorEnum.EXCEL_SHEET_ERROR.getMessage());
            return excelStatusBO;
            //throw new OrganizationException(OrganizationErrorEnum.EXCEL_SHEET_ERROR);
        }



        if (sheet.getLastRowNum() < 1) {
            excelStatusBO.setStatus(3);
            excelStatusBO.setId(excelPO.getId());
            excelStatusBO.setErrorMessage(OrganizationErrorEnum.EXCEL_IMPORT_FAILED_ROWS_EMPTY.getMessage());

            excelPO.setStatus(3);
            excelPO.setErrorMessage(OrganizationErrorEnum.EXCEL_IMPORT_FAILED_ROWS_EMPTY.getMessage());
            excelMapper.updateById(excelPO);
            return excelStatusBO;
        }

        String fileName = ExcelUtils.createExcelFile(workbook, file.getOriginalFilename(), excelPO.getId());
        excelPO.setFileName(fileName);
        excelMapper.updateById(excelPO);
        excelStatusBO.setId(excelPO.getId());
        excelStatusBO.setStatus(0);
        if ("Department".equals(type)) {

            ThreadPoolUtils.getThreadPool().execute(() -> {
                departmentService.importExcel(workbook, excelPO.getId(), companyId, file.getOriginalFilename(), tenantId);
            });

        } else if ("Position".equals(type)) {
            //岗位导入
            //部门导入
            ThreadPoolUtils.getThreadPool().execute(() -> {
                positionService.importExcel(workbook, excelPO.getId(), companyId, file.getOriginalFilename(), fileName, tenantId);
            });
        } else if ("Person".equals(type)) {
            //人员导入
            ThreadPoolUtils.getThreadPool().execute(() -> {
                personService.importExcel(workbook, excelPO.getId(), companyId, file.getOriginalFilename(), tenantId, timeZone);
            });
        } else {
            throw new OrganizationException(OrganizationErrorEnum.EXCEL_IMPORT_TYPE_ERROR);
        }
        return excelStatusBO;
    }

    /**
     * 下载模板
     * @param type
     * @param response
     */
    @Override
    public void downlowdExcelTemplate(String type, HttpServletResponse response) throws IOException {
        File dir = new File(ExcelUtils.EXCEL_FILE_TEMPLATE_PATH);
        if (!dir.exists()) {
            boolean mkFlag = dir.mkdirs();
            if (!mkFlag) {
                throw new OrganizationException(OrganizationErrorEnum.EXCEL_FILE_CREATE_ERROR);
            }
        }

        //response.setContentType("application/octet-stream;charset=UTF-8");

        /*response.setContentType("application/x-download");
        response.setCharacterEncoding("utf-8");*/

        String fileName = "";
        File file = null;
        if ("Department".equals(type)) {
            //部门模板
            fileName = ExcelUtils.DEPARTMENT_FILE;
            ExcelUtils.createFolder(ExcelUtils.EXCEL_FILE_TEMPLATE_PATH);
            file = ExcelUtils.createFile(ExcelUtils.EXCEL_FILE_TEMPLATE_PATH + ExcelUtils.DEPARTMENT_FILE);
           departmentService.downlowdExcelTemplate(file);
        } else if ("Position".equals(type)) {
            //岗位模板
            fileName = ExcelUtils.POSITION_FILE;
            ExcelUtils.createFolder(ExcelUtils.EXCEL_FILE_TEMPLATE_PATH);
            file = ExcelUtils.createFile(ExcelUtils.EXCEL_FILE_TEMPLATE_PATH + ExcelUtils.POSITION_FILE);
            positionService.downlowdExcelTemplate(file);
        } else if ("Person".equals(type)) {
            fileName = ExcelUtils.PERSON_FILE;
            ExcelUtils.createFolder(ExcelUtils.EXCEL_FILE_TEMPLATE_PATH);
            file = ExcelUtils.createFile(ExcelUtils.EXCEL_FILE_TEMPLATE_PATH + ExcelUtils.PERSON_FILE);
            //人员模板
            personService.downlowdExcelTemplate(file);

        } else if ("Relation".equals(type)) {

        } else {
            throw new OrganizationException(OrganizationErrorEnum.EXCEL_TYPE_TEMPLATE_NOT_EXISTS);
        }
        //response.setHeader("Content-Disposition", "attachment;fileName=" + fileName);
        response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(fileName, "UTF-8"));
        BufferedInputStream inputStream = null;
        ServletOutputStream out = null;
        try {
            inputStream = new BufferedInputStream(new FileInputStream(file));
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

    /**
     * 查询导入状态
     * @param id
     * @return
     */
    @Override
    public ExcelStatusBO checkStatus(Long id) {
        ExcelPO excelPO = excelMapper.selectById(id);
        if (excelPO == null) {
            throw new OrganizationException(OrganizationErrorEnum.EXCEL_IMPORT_TASH_NOT_EXISTS_ERROR);
        }
        ExcelStatusBO excelStatusBO = new ExcelStatusBO();
        excelStatusBO.setId(excelPO.getId());
        excelStatusBO.setStatus(excelPO.getStatus());
        excelStatusBO.setErrorFile(excelPO.getErrorFile());
        excelStatusBO.setErrorMessage(excelPO.getErrorMessage());
        if (StringUtils.isNotBlank(excelPO.getErrorFile())) {
            excelStatusBO.setHasErrorFile(true);
        }
        return excelStatusBO;
    }

    /**
     * 下载错误文件
     * @param id
     * @param response
     */
    @Override
    public void downlowdExcel(Long id, HttpServletResponse response) throws IOException {
        ExcelPO excelPO = excelMapper.selectById(id);
        if (excelPO == null) {
            throw new OrganizationException(OrganizationErrorEnum.EXCEL_IMPORT_TASH_NOT_EXISTS_ERROR);
        }
        //response.setContentType("application/octet-stream;charset=UTF-8");

        /*response.setContentType("application/x-download");
        response.setCharacterEncoding("utf-8");*/

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日HH时mm分ss秒");
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        String strDate = excelPO.getCreateTime();
        Date date = new Date();
        if (StringUtils.isNotBlank(strDate)) {
            try {
                date= df.parse(strDate);
            } catch (ParseException e) {
                date = new Date();
            }
        }
        String filePath = "";
        String fileName = "";
        if ("import".equals(excelPO.getType())) {
            filePath = excelPO.getErrorFile();
            fileName = filePath.substring(filePath.lastIndexOf("_") + 1, filePath.indexOf(".xlsx")) + sdf.format(date) + ".xlsx";
        } else {
            filePath = excelPO.getFileName();
            fileName = filePath.substring(filePath.lastIndexOf("_") + 1, filePath.length());
        }
        //response.setHeader("Content-Disposition", "attachment;fileName=" + filePath.substring(filePath.lastIndexOf("_") + 1, filePath.length()));
        response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(fileName, "UTF-8").replace("+","%20"));
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

    /**
     * 导出数据
     * @param ids
     * @param all
     * @param type
     * @param response
     * @param companyId
     * @param conditionQuery
     * @param keyword
     * @param orgId
     */
    @Override
    public ExcelStatusBO exportExcelData(List<Long> ids, Boolean all, String type, HttpServletResponse response, Long companyId, PersonDetailBO conditionQuery, String keyword, Long orgId) {

        if (!all && (ids == null || ids.size() == 0)) {
            throw new OrganizationException(OrganizationErrorEnum.EXCEL_EXPORT_IDS_EMPTY);
        }
        ExcelStatusBO excelStatusBO = new ExcelStatusBO();

        ExcelPO excelPO = new ExcelPO();
        excelPO.setStatus(0);
        excelPO.setType("export");
        excelMapper.insert(excelPO);
        excelStatusBO.setId(excelPO.getId());
        excelStatusBO.setStatus(0);
        if ("Department".equals(type)) {
            //部门模板
            ThreadPoolUtils.getThreadPool().execute(() -> {
                departmentService.exportExcelData(ids, all, excelPO.getId(), companyId);
            });
        } else if ("Position".equals(type)) {
            //岗位模板
            ThreadPoolUtils.getThreadPool().execute(() -> {
                positionService.exportExcelData(ids, all, excelPO.getId(), companyId);
            });
        } else if ("Person".equals(type)) {
            ThreadPoolUtils.getThreadPool().execute(() -> {
                personService.exportExcelData(ids, all, excelPO.getId(), companyId, conditionQuery, keyword, orgId);
            });

        } else if ("Position-Relation".equals(type)) {
            if (orgId == null) {
                throw new PositionException(PositionErrorEnum.POSITION_ID_NOT_EXISTS);
            }
            //岗位模板
            ThreadPoolUtils.getThreadPool().execute(() -> {
                positionService.exportPersonExcelData(ids, all, excelPO.getId(), orgId);
            });
        } else if ("Department-Relation".equals(type)) {
            if (orgId == null) {
                throw new DepartmentException(DepartmentErrorEnum.DEPARTMENT_ID_NOT_EXISTS);
            }
            //岗位模板
            ThreadPoolUtils.getThreadPool().execute(() -> {
                departmentService.exportPersonExcelData(ids, all, excelPO.getId(), orgId);
            });
        } else {
            throw new OrganizationException(OrganizationErrorEnum.EXCEL_TYPE_TEMPLATE_NOT_EXISTS);
        }
        return excelStatusBO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public void excuteExcelState(ExcelPO excelPO) {
        excelMapper.updateById(excelPO);
    }

}
