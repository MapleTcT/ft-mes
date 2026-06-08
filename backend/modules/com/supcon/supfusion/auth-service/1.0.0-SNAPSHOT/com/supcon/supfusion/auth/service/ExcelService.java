package com.supcon.supfusion.auth.service;

import com.supcon.supfusion.auth.dao.po.AuthExcelPO;
import com.supcon.supfusion.auth.service.bo.ExcelStatusBO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public interface ExcelService {

    /**
     * 导入Excel文件
     *
     * @param file Excel文件
     */
    ExcelStatusBO importExcel(MultipartFile file) throws Exception;

    void downlowdExcelTemplate(HttpServletResponse response) throws IOException;

    ExcelStatusBO checkStatus(Long id);

    void downlowdExcel(Long id, HttpServletResponse response) throws IOException;

    ExcelStatusBO exportExcelData(List<Long> ids, Boolean all, String keyword, HttpServletResponse response);

    void excuteExcelState(AuthExcelPO excelPO);

}
