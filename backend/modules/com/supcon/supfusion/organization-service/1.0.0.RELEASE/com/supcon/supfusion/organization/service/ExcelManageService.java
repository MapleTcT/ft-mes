package com.supcon.supfusion.organization.service;

import com.supcon.supfusion.organization.dao.po.excel.ExcelPO;
import com.supcon.supfusion.organization.service.bo.excel.ExcelStatusBO;
import com.supcon.supfusion.organization.service.bo.person.PersonDetailBO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * Excel操作服务
 */
public interface ExcelManageService {

    /**
     * 导入Excel文件
     * @param companyId 公司id
     * @param type 导入类型
     * @param file Excel文件
     * @param tenantId
     * @param timeZone
     */
    ExcelStatusBO importExcel(Long companyId, String type, MultipartFile file, String tenantId, String timeZone) throws IOException;

    void downlowdExcelTemplate(String type, HttpServletResponse response) throws IOException;

    ExcelStatusBO checkStatus(Long id);

    void downlowdExcel(Long id, HttpServletResponse response) throws IOException;

    ExcelStatusBO exportExcelData(List<Long> ids, Boolean all, String type, HttpServletResponse response, Long companyId, PersonDetailBO conditionQuery, String keyword, Long orgId);

    void excuteExcelState(ExcelPO excelPO);
}
