package com.supcon.supfusion.file.server.service.impl;

import com.supcon.supfusion.file.server.common.config.FileConfig;
import com.supcon.supfusion.file.server.common.constants.Constants;
import com.supcon.supfusion.file.server.common.exception.FileServerErrorEnum;
import com.supcon.supfusion.file.server.common.exception.FileServerException;
import com.supcon.supfusion.file.server.common.utils.DocumentUtils;
import com.supcon.supfusion.file.server.common.utils.RestTemplateUtil;
import com.supcon.supfusion.file.server.common.utils.TenantUtil;
import com.supcon.supfusion.file.server.common.vo.DocumentUploadVO;
import com.supcon.supfusion.file.server.dao.DocumentDao;
import com.supcon.supfusion.file.server.dao.DocumentDownloadInfoDao;
import com.supcon.supfusion.file.server.dao.po.DocumentDownloadInfoPO;
import com.supcon.supfusion.file.server.dao.po.DocumentPO;
import com.supcon.supfusion.file.server.service.DocumentService;
import com.supcon.supfusion.file.server.service.FileService;
import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;
import com.supcon.supfusion.organization.api.PersonApiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.*;

@Slf4j
@Service("documentService")
public class DocumentServiceImpl implements DocumentService {

    private static final String FILE_TOPIC = "topic.file.convert";

    @Autowired
    private DocumentDao documentDao;
    @Autowired
    private DocumentDownloadInfoDao documentDownloadInfoDao;
    @Autowired
    private FileConfig fileConfig;
    @Autowired
    private FileService fileService;
    @Autowired(required = false)
    private PersonApiService personApiService;

    @Autowired
    @Qualifier("restTemplateProxy1")
    private RestTemplate restTemplate;

    private static String imageContentType = "image/";
    private static String applicationContentType = "application/";


    @Override
    public String downLoadQueryById(String id) {
        if (id != null && !id.equals(Constants.STR_KONG)) {
            DocumentPO documentPO = documentDao.selectById(Long.valueOf(id));
            if (documentPO != null && documentPO.getFilePath() != null && !documentPO.getFilePath().equals(Constants.STR_KONG)) {
                DocumentPO documentPO1 = new DocumentPO();
                documentPO1.setId(documentPO.getId());
                documentPO1.setDownloadTimes(documentPO.getDownloadTimes() + 1);
                documentDao.updatedDownloadTimesByOneId(documentPO1);
                return documentPO.getFilePath();
            }
        }
        return Constants.STR_KONG;
    }

    @Override
    public DocumentPO convertPathById(String id) {
        if (id != null && !id.equals(Constants.STR_KONG)) {
            DocumentPO documentPO = documentDao.selectById(Long.valueOf(id));
            if (!ObjectUtils.isEmpty(documentPO)) {
                return documentPO;
            }
        }
        return new DocumentPO();
    }

    @Override
    public String overviewQueryById(String id) {
        if (id != null && !id.equals(Constants.STR_KONG)) {
            DocumentPO documentPO = documentDao.selectById(Long.valueOf(id));
            if (documentPO != null && documentPO.getFilePath() != null && !documentPO.getFilePath().equals(Constants.STR_KONG)) {
                DocumentPO documentPO1 = new DocumentPO();
                documentPO1.setId(documentPO.getId());
                documentPO1.setPreviewTimes(documentPO.getPreviewTimes() + 1);
                documentDao.updatedPreviewTimesByOneId(documentPO1);
                return documentPO.getFilePath();
            }
        }
        return Constants.STR_KONG;
    }

    @Override
    public void saveDownloadRecord(String id, String clinetIpByReq, Long userId, Long staffId) {
        DocumentDownloadInfoPO documentDownloadInfoPO = new DocumentDownloadInfoPO();
        documentDownloadInfoPO.setId(IDGenerator.newInstance().generate().longValue());
        documentDownloadInfoPO.setIpAddr(clinetIpByReq);
        documentDownloadInfoPO.setRecordType("download");
        documentDownloadInfoPO.setDocumentId(Long.parseLong(id));
        documentDownloadInfoPO.setDownloadTime(new Date());
        if (staffId != null)
            documentDownloadInfoPO.setDownloadStaffId(staffId.toString());
        documentDownloadInfoPO.setCreateTime(new Date());
        documentDownloadInfoPO.setValid("1");
        documentDownloadInfoDao.insert(documentDownloadInfoPO);
    }

    @Override
    public void saveOverViewRecord(String id, String clinetIpByReq, Long userId, Long staffId) {
        DocumentDownloadInfoPO documentDownloadInfoPO = new DocumentDownloadInfoPO();
        documentDownloadInfoPO.setId(IDGenerator.newInstance().generate().longValue());
        documentDownloadInfoPO.setIpAddr(clinetIpByReq);
        documentDownloadInfoPO.setRecordType("overview");
        documentDownloadInfoPO.setDocumentId(Long.parseLong(id));
        documentDownloadInfoPO.setDownloadTime(new Date());
        if (staffId != null)
            documentDownloadInfoPO.setDownloadStaffId(staffId.toString());
        documentDownloadInfoPO.setCreateTime(new Date());
        documentDownloadInfoPO.setValid("1");
        documentDownloadInfoDao.insert(documentDownloadInfoPO);
    }

    @Override
    public Boolean getAuthentication(String Authorization, String methodType, String serverName, String url, String entityCode, String id, Long userId, Long staffId) {
        String linkId = null;
        String type = null;
        Long mainModelId = null;
        String propertyCode = null;
        String showType = null;
        DocumentPO documentPO = documentDao.selectById(Long.valueOf(id));
        if (documentPO != null) {
            if (documentPO.getLinkId() != 0) {
                linkId = documentPO.getLinkId() + Constants.STR_KONG;
            }
            if (documentPO.getFileOrgType() != null) {
                type = documentPO.getFileOrgType();
            }
            if (documentPO.getMainModelId() != null) {
                mainModelId = documentPO.getMainModelId();
            }
            if (documentPO.getPropertyCode() != null) {
                propertyCode = documentPO.getPropertyCode();
            }
            if (documentPO.getShowType() != null) {
                showType = documentPO.getShowType();
            }
        }
        Boolean isCanDownload = false;
        try {
            isCanDownload = RestTemplateUtil.getAuthentication(Authorization, restTemplate, methodType, serverName, url, entityCode, linkId, id, type, mainModelId, propertyCode, showType, userId);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new FileServerException(FileServerErrorEnum.FILE_DOWN_AUTH_GET_ERROR);
        }
        return isCanDownload;
    }

    @Override
    public List<DocumentUploadVO> queryByLindIdAndTypeAndPropertyCodeAndFileView(String linkId, String type, String fileType, String propertyCode) {
        List<DocumentPO> documentPOS = documentDao.selectListByLinkIdAndType(Long.valueOf(linkId), type, fileType, propertyCode);
        List<DocumentUploadVO> documentVOS = new ArrayList<>();
        if (documentPOS != null && documentPOS.size() > 0) {
            documentPOS.forEach(documentPO -> {
                DocumentUploadVO documentVO = new DocumentUploadVO();
                documentVO.setId(documentPO.getId());
                documentVO.setPath(documentPO.getFilePath());
                documentVO.setType(documentPO.getFileType());
                documentVO.setSize(documentPO.getFileSize());
                documentVO.setLinkId(documentPO.getLinkId());
                documentVO.setFileType(documentPO.getFileType());
                documentVO.setMainModelId(documentPO.getMainModelId());
                documentVO.setMemo(documentPO.getMemo());
                documentVO.setPropertyCode(documentPO.getPropertyCode());
                documentVO.setShowType(documentPO.getShowType());
                documentVO.setOpener(documentPO.getOpener());
                if (documentPO.getOpenTime() != null) {
                    documentVO.setOpenTime(documentPO.getOpenTime().getTime());
                }
                documentVO.setDeploymentId(documentPO.getDeploymentId());
                documentVO.setActivityName(documentPO.getActivityName());
                documentVO.setTaskDescription(documentPO.getTaskDescription());
                documentVO.setIsFileView(documentPO.getIsFileView());
                documentVO.setDocContent(documentPO.getDocContent());
                documentVO.setDocSummary(documentPO.getDocSummary());
                documentVO.setConvertStatus(documentPO.getConvertStatus());
                documentVO.setReason(documentPO.getReason());
                documentVO.setConvertPath(documentPO.getConvertPath());
                documentVO.setDownloadTimes(documentPO.getDownloadTimes());
                documentVO.setPreviewTimes(documentPO.getPreviewTimes());
                if (documentPO.getCreateTime() != null) {
                    documentVO.setCreateTime(documentPO.getCreateTime().getTime());
                }
                if (documentPO.getModifyTime() != null) {
                    documentVO.setModifyTime(documentPO.getModifyTime().getTime());
                }
                if (documentPO.getCreateStaffId() != null) {
                    documentVO.setCreateStaffId(documentPO.getCreateStaffId().toString());
                }
                if (documentPO.getModifyStaffId() != null) {
                    documentVO.setModifyStaffId(documentPO.getModifyStaffId().toString());
                }
                if (documentPO.getCreator()!=null){
                    documentVO.setCreator(documentPO.getCreator());
                }
                if (documentPO.getFileName() != null) {
                    documentVO.setName(documentPO.getFileName());
                    documentVO.setFileIcon(DocumentUtils.getIcon(documentPO.getFileName()));
                }
                if (documentPO.getSizeDis() != null) {
                    documentVO.setSizeDis(documentPO.getSizeDis());
                }
                documentVO.setIsFileView(fileConfig.getFileView());
                documentVOS.add(documentVO);
            });
        }
        return documentVOS;
    }

    @Override
    public void deleteById(String id){
        //先查询一次 如果不存在 直接返回
        DocumentPO documentPO = documentDao.selectById(id);
        if (documentPO != null) {
            //删除minio服务器文件
            try {
                if (!ObjectUtils.isEmpty(documentPO.getFilePath())) {
                    fileService.remove(TenantUtil.getTenantId(), documentPO.getFilePath());
                }
                if (!ObjectUtils.isEmpty(documentPO.getConvertPath())) {
                    fileService.remove(TenantUtil.getTenantId(), documentPO.getConvertPath());
                }
                documentDao.deleteByFileId(Long.valueOf(id));
            } catch (Exception e) {
                throw new FileServerException(FileServerErrorEnum.FILE_REMOVE_ERROR);
            }
        } else {
            throw new FileServerException(FileServerErrorEnum.FILE_NO_ERROR);
        }
    }

    @Override
    public List<DocumentDownloadInfoPO> selectByIdPage(String id, Integer pageNo, Integer pageSize, Map ddl) {
        String recordType = "";
        Map downloadStaff = new HashMap();
        String downloadStaffId = "";
        if (ddl != null) {
            if (ddl.get("recordType") != null) {
                recordType = (String) ddl.get("recordType");
            }
            if (ddl.get("downloadStaff") != null) {
                downloadStaff = (Map) ddl.get("downloadStaff");
                if (downloadStaff != null && downloadStaff.get("id") != null) {
                    Integer staffId = (Integer) downloadStaff.get("id");
                    downloadStaffId = String.valueOf(staffId);
                }
            }
        }
        int offset = (pageNo - 1) * pageSize;
        List<DocumentDownloadInfoPO> documentDownloadInfoPOS = documentDownloadInfoDao.selectByIdAndRecordTypeAndDownloadStaffIdPage(Long.valueOf(id), offset, pageSize, recordType, downloadStaffId);
        if (downloadStaffId != null && !downloadStaffId.equals(Constants.STR_KONG)) {
            List<Long> ids = new ArrayList<>();
            ids.add(Long.valueOf(downloadStaffId));
            //ListResult<DepartmentDetailDTO> departmentDetailDTOListResult = personApiService.queryPersonsDepartmentsByPersonIds(ids);
            //Result<JSONObject> person = personApiService.getCurPerson(Long.valueOf(downloadStaffId),"name");

        }
        return documentDownloadInfoPOS;
    }

    @Override
    public void view(String id, HttpServletResponse response) {
        DocumentPO documentPO = documentDao.selectByFileId(Long.valueOf(id));
        if (ObjectUtils.isEmpty(documentPO)) {
            throw new FileServerException(FileServerErrorEnum.FILE_NO_ERROR);
        }
        showPdf(documentPO.getConvertPath(), response);
    }

    @Override
    public void updatePreviewTime(DocumentPO documentPO) {
        DocumentPO documentPO1 = new DocumentPO();
        documentPO1.setId(documentPO.getId());
        documentPO1.setPreviewTimes(documentPO.getPreviewTimes() + 1);
        documentDao.updatedPreviewTimesByOneId(documentPO1);
    }

    /**
     * 预览文件工具类
     *
     * @param response
     * @param filePath
     */
    public void showPdf(String filePath, HttpServletResponse response) {
        InputStream in = null;
        BufferedInputStream inBuffer = null;
        OutputStream out = null;
        BufferedOutputStream outBuffer = null;
        try {
            String convertFileType = filePath.substring(filePath.lastIndexOf(".") + 1);
            if ("jpg".equals(convertFileType) || "jpeg".equals(convertFileType) || "jfif".equals(convertFileType)) {
                response.setContentType(imageContentType + "jpeg");
            } else if ("gif".equals(convertFileType) || "png".equals(convertFileType) || "tiff".equals(convertFileType)) {
                response.setContentType(imageContentType + convertFileType);
            } else if ("tif".equals(convertFileType)) {
                response.setContentType(imageContentType + "tiff");
            } else if ("bmp".equals(convertFileType)) {
                response.setContentType(applicationContentType + "x-bmp");
            } else if ("wmf".equals(convertFileType)) {
                response.setContentType(applicationContentType + "x-wmf");
            } else if ("pdf".equals(convertFileType)) {
                response.setContentType(applicationContentType + "pdf");
            } else if ("html".equals(convertFileType)) {
                response.setContentType("text/html");
            }
            String tenantId = TenantUtil.getTenantId();
            in = fileService.downLoad(tenantId, filePath);
            inBuffer = new BufferedInputStream(in);
            out = response.getOutputStream();
            outBuffer = new BufferedOutputStream(out);
            int len;
            byte[] bs = new byte[1024];
            while ((len = inBuffer.read(bs)) != -1) {
                outBuffer.write(bs, 0, len);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (inBuffer != null) {
                try {
                    inBuffer.close();
                } catch (IOException e) {
                    log.error(e.getMessage());
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    log.error(e.getMessage());
                }
            }
            if (outBuffer != null) {
                try {
                    outBuffer.close();
                } catch (IOException e) {
                    log.error(e.getMessage());
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    log.error(e.getMessage());
                }
            }
        }
    }

}
