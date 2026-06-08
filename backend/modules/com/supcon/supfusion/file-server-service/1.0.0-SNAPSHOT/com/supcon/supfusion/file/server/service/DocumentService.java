/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD.
 * All rights reserved.
 */
package com.supcon.supfusion.file.server.service;

import com.supcon.supfusion.file.server.common.vo.DocumentUploadVO;
import com.supcon.supfusion.file.server.dao.po.DocumentDownloadInfoPO;
import com.supcon.supfusion.file.server.dao.po.DocumentPO;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * @author szq
 */
public interface DocumentService {

    String downLoadQueryById(String id);

    String overviewQueryById(String id);

    DocumentPO convertPathById(String id);

    void saveDownloadRecord(String id, String clinetIpByReq, Long userId, Long staffId);

    void saveOverViewRecord(String id, String clinetIpByReq, Long userId, Long staffId);

    Boolean getAuthentication(String Authorization ,String methodType, String serverName, String url, String entityCode, String id, Long userId, Long staffId);

    List<DocumentUploadVO> queryByLindIdAndTypeAndPropertyCodeAndFileView(String linkId, String type, String fileType , String propertyCode);

    void deleteById(String id);

    List<DocumentDownloadInfoPO> selectByIdPage(String id, Integer pageNo, Integer pageSize, Map ddl);

    void view(String id, HttpServletResponse response);

    void updatePreviewTime(DocumentPO documentPO);
}
