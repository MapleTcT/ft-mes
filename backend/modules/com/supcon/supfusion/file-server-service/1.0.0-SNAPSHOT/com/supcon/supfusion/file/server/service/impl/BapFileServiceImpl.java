package com.supcon.supfusion.file.server.service.impl;

import com.alibaba.fastjson.JSON;
import com.supcon.supfusion.file.server.api.BapFileService;
import com.supcon.supfusion.file.server.api.dto.DocumentQueryDTO;
import com.supcon.supfusion.file.server.api.dto.DocumentSaveDTO;
import com.supcon.supfusion.file.server.api.dto.DocumentUpdateDTO;
import com.supcon.supfusion.file.server.api.vo.DocumentVO;
import com.supcon.supfusion.file.server.common.config.FileConfig;
import com.supcon.supfusion.file.server.common.constants.Constants;
import com.supcon.supfusion.file.server.common.exception.FileServerErrorEnum;
import com.supcon.supfusion.file.server.common.exception.FileServerException;
import com.supcon.supfusion.file.server.common.utils.DocumentUtils;
import com.supcon.supfusion.file.server.common.utils.FileUtil;
import com.supcon.supfusion.file.server.common.utils.PathUtil;
import com.supcon.supfusion.file.server.common.utils.TenantUtil;
import com.supcon.supfusion.file.server.dao.DocumentDao;
import com.supcon.supfusion.file.server.dao.DocumentDownloadInfoDao;
import com.supcon.supfusion.file.server.dao.po.DocumentPO;
import com.supcon.supfusion.file.server.service.FileConvertService;
import com.supcon.supfusion.file.server.service.FileDaoService;
import com.supcon.supfusion.file.server.service.FileServerManager;
import com.supcon.supfusion.file.server.service.FileService;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import com.supcon.supfusion.framework.cloud.common.context.UserContext;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.supcon.supfusion.file.server.common.utils.PathUtil.getFilePath;


@ServiceApiService
@Slf4j
public class BapFileServiceImpl implements BapFileService {

    @Autowired
    private FileService fileService;
    @Autowired
    private DocumentDao documentDao;
    @Autowired
    private DocumentDownloadInfoDao documentDownloadInfoDao;
    @Autowired
    private FileConfig fileConfig;
    @Autowired
    private FileConvertService fileConvertService;
    @Autowired
    private FileDaoService fileDaoService;
    @Autowired
    private FileServerManager fileServerManager;

    /**
     * 前端上传临时目录  后端调用 移动接口保存到正式目录
     * 接口
     *
     * @param LinkIds
     * @param filePaths
     * @param fileTypes
     * @param mainModelId
     * @param sizeDis
     * @param memo
     * @param propertyCode
     * @param showType
     * @param opener
     * @param openTime
     * @param deploymentId
     * @param activityName
     * @param taskDescription
     * @param fileIcon
     * @param isFileView
     * @param docContent
     * @param docSummary
     * @param convertStatus
     * @param reason
     * @param convertPath
     * @param name
     * @param type
     * @param size
     * @param downloadTimes
     * @param previewTimes
     */
    @Override
    public void moveFiles(Long[] LinkIds, String[] filePaths, String[] fileTypes,
                          Long[] mainModelId, String[] sizeDis, String[] memo,
                          String[] propertyCode, String[] showType, String[] opener,
                          Date[] openTime, Long[] deploymentId, String[] activityName,
                          String[] taskDescription, String[] fileIcon, Boolean[] isFileView,
                          String[] docContent, String[] docSummary, String[] convertStatus,
                          String[] reason, String[] convertPath, String[] name, String[] type,
                          String[] size, Long[] downloadTimes, Long[] previewTimes) {

        if (LinkIds != null && filePaths != null && fileTypes != null) {
            if (LinkIds.length != filePaths.length || LinkIds.length != fileTypes.length || filePaths.length != fileTypes.length) {
                throw new FileServerException(FileServerErrorEnum.FILE_PARAM_NUM_ERROR);
            }
        }
        if (LinkIds.length == 0) {
            throw new FileServerException(FileServerErrorEnum.FILE_PARAM_LOST_ERROR);
        }
        List<DocumentPO> documentPOS = new ArrayList<>();
        for (int i = 0; i < LinkIds.length; i++) {
            String filePath = filePaths[i].replace("/", "\\");
            String filename = filePath.substring(filePath.lastIndexOf("\\") + 1);
            //String filename = filePaths[i].substring(filePaths[i].lastIndexOf("\\") + 1);
            String tenantId = TenantUtil.getTenantId();
            String timestamp = String.valueOf(System.currentTimeMillis());
            String preFilePath = getFilePath(LinkIds[i] + "", timestamp, filename);
            String finalTenantId = tenantId;
            //TODO  校验文件是否存在
            log.info(filename + " 临时目录：" + filePaths[i]);
            log.info(filename + " 正式目录：" + preFilePath);
            log.info("当前租户id：" + finalTenantId);
            try {
                fileService.copy(finalTenantId, filePaths[i].replace("\\", "/"), preFilePath);
            } catch (Exception e) {
                log.error("copy:",e);
                throw new FileServerException(FileServerErrorEnum.FILE_MOVE_ERROR);
            }
            // 下载到临时目录
            InputStream input = null;
            try {
                input = fileService.downLoad(finalTenantId, preFilePath);
            } catch (Exception e) {
                log.error(e.getMessage());
            }
            //业务关联信息存入数据库
            String tempDir = PathUtil.getTempPath() + "MoveFiles/" + System.currentTimeMillis() + Constants.PATH;
            String tempFilePath = tempDir + filename;
            FileUtil.createDir(tempDir);
            FileUtil.createNewFile(tempFilePath);
            FileUtil.copyFileUsingFileStreams(input, new File(tempFilePath));
            //从临时目录文件中读取文件信息
            File file2 = new File(tempFilePath);
            DocumentPO documentPO = new DocumentPO();
            documentPO.setId(IDGenerator.newInstance().generate().longValue());
            //Long[] LinkIds, String[] filePaths, String[] fileTypes,
            documentPO.setFilePath(preFilePath);
            documentPO.setLinkId(LinkIds[i]);
            if (!ObjectUtils.isEmpty(fileTypes[i]) && !fileTypes[i].equals("null")) {
                documentPO.setFileType(fileTypes[i]);
            }
            //Long[] mainModelId, String[] sizeDis, String[] memo,
            if (mainModelId != null && mainModelId.length > 0 && mainModelId[i] != null) {
                documentPO.setMainModelId(mainModelId[i]);
            }
            if (memo != null && memo.length > 0 && memo[i] != null) {
                documentPO.setMemo(memo[i]);
            }
            //String[] propertyCode, String[] showType, String[] opener,
            if (propertyCode != null && propertyCode.length > 0 && propertyCode[i] != null) {
                documentPO.setPropertyCode(propertyCode[i]);
            }
            if (showType != null && showType.length > 0 && showType[i] != null && !showType[i].equals(Constants.STR_KONG)) {
                documentPO.setShowType(showType[i]);
            }
            if (opener != null && opener.length > 0 && opener[i] != null) {
                documentPO.setOpener(opener[i]);
            }

            //Date[] openTime, Long[] deploymentId, String[] activityName,
            if (openTime != null && openTime.length > 0 && openTime[i] != null) {
                documentPO.setOpenTime(openTime[i]);
            }
            if (deploymentId != null && deploymentId.length > 0 && deploymentId[i] != null) {
                documentPO.setDeploymentId(deploymentId[i]);
            }
            if (activityName != null && activityName.length > 0 && activityName[i] != null) {
                documentPO.setActivityName(activityName[i]);
            }

            //String[] taskDescription, String[] fileIcon, Boolean[] isFileView,
            if (taskDescription != null && taskDescription.length > 0 && taskDescription[i] != null) {
                documentPO.setTaskDescription(taskDescription[i]);
            }
            if (fileIcon != null && fileIcon.length > 0 && fileIcon[i] != null) {
                documentPO.setFileIcon(fileIcon[i]);
            }
            if (isFileView != null && isFileView.length > 0 && isFileView[i] != null) {
                documentPO.setIsFileView(isFileView[i]);
            } else {
                documentPO.setIsFileView(false);
            }

            //String[] docContent, String[] docSummary, ConvertStatus[] convertStatus,
            if (docContent != null && docContent.length > 0 && docContent[i] != null) {
                documentPO.setDocContent(docContent[i]);
            }
            if (docSummary != null && docSummary.length > 0 && docSummary[i] != null) {
                documentPO.setDocSummary(docSummary[i]);
            }
            if (convertStatus != null && convertStatus.length > 0 && convertStatus[i] != null) {
                documentPO.setConvertStatus(convertStatus[i]);
            }

            //String[] reason, String[] convertPath, String[] name, String[] type,
            if (reason != null && reason.length > 0 && reason[i] != null) {
                documentPO.setReason(reason[i]);
            }
            if (convertPath != null && convertPath.length > 0 && convertPath[i] != null) {
                documentPO.setConvertPath(convertPath[i]);
            }


            if (name != null && name.length > 0 && name[i] != null) {
                documentPO.setFileName(name[i]);
            } else {
                documentPO.setFileName(filename);
            }
            if (type != null && type.length > 0 && type[i] != null) {
                documentPO.setFileOrgType(type[i]);
            } else {
                documentPO.setFileOrgType(filename.substring(filename.lastIndexOf(".") + 1));
            }

            //String[] size, String[] downloadTimes, String[] previewTimes)
            documentPO.setFileSize(FileUtil.getFileSize(file2));

            //删除临时文件
            if (!file2.delete()) {
                log.error("删除临时文件发生错误,file2:{}",file2);
            }else {
                log.info("删除临时文件成功,file2:{}",file2);
            }
            if (sizeDis != null && sizeDis.length > 0 && sizeDis[i] != null) {
                documentPO.setSizeDis(sizeDis[i]);
            }else{
                documentPO.setSizeDis(DocumentUtils.sizeConversion(documentPO.getFileSize()));
            }
            if (downloadTimes != null && downloadTimes.length > 0 && downloadTimes[i] != null) {
                documentPO.setDownloadTimes(downloadTimes[i]);
            }
            if (previewTimes != null && previewTimes.length > 0 && previewTimes[i] != null) {
                documentPO.setPreviewTimes(previewTimes[i]);
            }
            documentPO.setValid("1");
            documentPO.setVersion(0L);
            documentPO.setCreateTime(new Date());
            documentPO.setModifyTime(new Date());
            Long staffId = UserContext.getUserContext().getStaffId();
            documentPO.setCreateStaffId(staffId);
            if (!ObjectUtils.isEmpty(staffId)) {
                documentPO.setCreator(fileServerManager.getPersonName(staffId));
            }
            documentPOS.add(documentPO);
        }
//        documentDao.saveBatch(documentPOS);
        fileDaoService.saveBatch(documentPOS);
        //附件转换
        for (DocumentPO documentPO : documentPOS) {
            fileConvertService.fileConvert(documentPO);
        }
    }

    @Override
    public void moveFilesJson(DocumentSaveDTO documentSaveDTO) {
        moveFiles(documentSaveDTO.getLinkIds(), documentSaveDTO.getFilePaths(), documentSaveDTO.getFileTypes(),
                documentSaveDTO.getMainModelId(), documentSaveDTO.getSizeDis(), documentSaveDTO.getMemo(),
                documentSaveDTO.getPropertyCode(), documentSaveDTO.getShowType(), documentSaveDTO.getOpener(),
                documentSaveDTO.getOpenTime(), documentSaveDTO.getDeploymentId(), documentSaveDTO.getActivityName(),
                documentSaveDTO.getTaskDescription(), documentSaveDTO.getFileIcon(), documentSaveDTO.getIsFileView(),
                documentSaveDTO.getDocContent(), documentSaveDTO.getDocSummary(), documentSaveDTO.getConvertStatus(),
                documentSaveDTO.getReason(), documentSaveDTO.getConvertPath(), documentSaveDTO.getName(), documentSaveDTO.getType(),
                documentSaveDTO.getSize(), documentSaveDTO.getDownloadTimes(), documentSaveDTO.getPreviewTimes());
    }

    /**
     * 后端调用复制附件接口
     *
     * @param LinkId
     * @param fileId
     * @param type
     * @param fileType
     */
    @Override
    public void copyFile(Long LinkId, Long fileId, String type, String fileType) {
        if (!(LinkId != null && fileId != null)) {
            throw new FileServerException(FileServerErrorEnum.FILE_PARAM_LOST_ERROR);
        }
        DocumentPO documentPO = documentDao.selectByFileId(fileId);
        if (documentPO != null) {
            String filePath = documentPO.getFilePath();
            String filename = documentPO.getFileName();
            String tenantId = TenantUtil.getTenantId();
            String timestamp = String.valueOf(System.currentTimeMillis());
            String preFilePath = getFilePath(LinkId + "", timestamp, filename);
            log.info(filename + " 被拷贝文件：" + filePath);
            log.info(filename + " 需要拷贝到的目录：" + preFilePath);
            // 下载到临时目录
            //InputStream input = null;
            try {
                //input = fileService.downLoad(tenantId, filePath);
                //fileService.upLoadStream(tenantId, preFilePath, input);
                fileService.copy(tenantId, filePath, preFilePath);
            } catch (Exception e) {
                log.error(e.getMessage());
            }
            //业务关联信息存入数据库
            //String tempDir = PathUtil.getTempPath();
            //String tempFilePath = tempDir + filename;
            //FileUtil.createNewFile(tempFilePath);
            //FileUtil.copyFileUsingFileStreams(input, new File(tempFilePath));

            //Long[] LinkIds 关联ID, String[] filePaths 文档路径, String[] fileTypes 文件类型 pic：图片字段 attachment:普通附件 office:文档控件,
            DocumentPO documentPO2 = new DocumentPO();
            documentPO2.setId(IDGenerator.newInstance().generate().longValue());
            documentPO2.setLinkId(LinkId);
            if (fileType != null) {
                documentPO2.setFileType(fileType);
            } else {
                documentPO2.setFileType(documentPO.getFileType());
            }
            documentPO2.setFilePath(preFilePath);
            //Long[] mainModelId 主关联模型的ID，如是表单 则为tableInfoId, String[] sizeDis 文件大小(显示用), String[] memo 备注,
            documentPO2.setMainModelId(documentPO.getMainModelId());
            documentPO2.setSizeDis(documentPO.getSizeDis());
            documentPO2.setMemo(documentPO.getMemo());
            //String[] propertyCode 字段CODE, String[] showType显示类型, String[] opener首次打开用户,
            documentPO2.setPropertyCode(documentPO.getPropertyCode());
            documentPO2.setShowType(documentPO.getShowType());
            documentPO2.setOpener(documentPO.getOpener());
            //Date[] openTime打开时间, Long[] deploymentId流程ID, String[] activityName活动CODE,
            documentPO2.setOpenTime(null);
            documentPO2.setDeploymentId(documentPO.getDeploymentId());
            documentPO2.setActivityName(documentPO.getActivityName());
            //String[] taskDescription活动描述, String[] fileIcon文件图标类型, Boolean[] isFileView是否启用附件预览,
            documentPO2.setTaskDescription(documentPO.getTaskDescription());
            documentPO2.setFileIcon(documentPO.getFileIcon());
            documentPO2.setIsFileView(documentPO.getIsFileView());
            //String[] docContent, String[] docSummary, ConvertStatus[] convertStatus文档转换状态,
            documentPO2.setDocContent(documentPO.getDocContent());
            documentPO2.setDocSummary(documentPO.getDocSummary());
            documentPO2.setConvertStatus(null);
            //String[] reason不支持转换原因, String[] convertPath 转换后路径, String[] name  文档名称, String[] type 文档类型,
            documentPO2.setReason(documentPO.getReason());
            documentPO2.setConvertPath(null);
            documentPO2.setFileName(documentPO.getFileName());
            if (type != null) {
                documentPO2.setFileOrgType(type);
            } else {
                documentPO2.setFileOrgType(documentPO.getFileOrgType());
            }
            //String[] size 文件大小, String[] downloadTimes下载次数, String[] previewTimes浏览次数)
            documentPO2.setFileSize(documentPO.getFileSize());
            documentPO2.setDownloadTimes(0);
            documentPO2.setPreviewTimes(0);
            documentPO2.setValid("1");
            documentPO2.setVersion(documentPO.getVersion());
            documentPO2.setCreateTime(new Date());
            documentPO2.setModifyTime(new Date());
            Long staffId = UserContext.getUserContext().getStaffId();
            documentPO2.setCreateStaffId(staffId);
            documentPO2.setCreator(documentPO.getCreator());
            List<DocumentPO> documentPOS = new ArrayList<>();
            documentPOS.add(documentPO2);
//            documentDao.saveBatch(documentPOS);
            fileDaoService.saveBatch(documentPOS);
        } else {
            throw new FileServerException(FileServerErrorEnum.FILE_NO_ERROR);
        }
    }

    /**
     * 根据旧路径和新路径复制附件
     *
     * @param oldPath
     * @param newPath
     */
    @Override
    public void copyFileByPath(String oldPath, String newPath) {
        String tenantId = TenantUtil.getTenantId();
        oldPath= oldPath.replace("\\", "/");
        newPath = newPath.replace("\\", "/");
        if(!oldPath.startsWith("/")){
            oldPath = "/"+oldPath;
        }
        if(!newPath.startsWith("/")){
            newPath = "/"+newPath;
        }
        try {
            fileService.copy(tenantId, oldPath, newPath);
        } catch (Exception e) {
            log.error("copyFileByPath:",e);
            throw new FileServerException(FileServerErrorEnum.FILE_MOVE_ERROR);
        }
    }
    /**
     * 更新单个 附件相关信息接口
     *
     * @param documentUpdateDTO
     */
    @Override
    public void updateOneFileMessage(DocumentUpdateDTO documentUpdateDTO) {
        if (documentUpdateDTO != null && documentUpdateDTO.getId() != null) {
            DocumentPO documentPO = documentDao.selectByFileId(documentUpdateDTO.getId());
            if (documentPO != null) {
                //private long linkId;// 关联ID
                if (documentUpdateDTO.getLinkId() != null && documentUpdateDTO.getLinkId() != 0) {
                    documentPO.setLinkId(documentUpdateDTO.getLinkId());
                }
                //private String fileType; // 文件类型 pic：图片字段 attachment:普通附件 office:文档控件
                if (documentUpdateDTO.getFileType() != null && !documentUpdateDTO.getFileType().equals("")) {
                    documentPO.setFileType(documentUpdateDTO.getFileType());
                }
                //private Long mainModelId;// 主关联模型的ID，如是表单 则为tableInfoId
                if (documentUpdateDTO.getMainModelId() != null && documentUpdateDTO.getMainModelId() != 0) {
                    documentPO.setMainModelId(documentUpdateDTO.getMainModelId());
                }
                //private String sizeDis; // 文件大小(显示用)
                if (documentUpdateDTO.getSizeDis() != null && !documentUpdateDTO.getSizeDis().equals("")) {
                    documentPO.setSizeDis(documentUpdateDTO.getSizeDis());
                }
                //private String memo;// 备注
                if (documentUpdateDTO.getMemo() != null) {
                    documentPO.setMemo(documentUpdateDTO.getMemo());
                }
                //private String propertyCode;
                if (documentUpdateDTO.getPropertyCode() != null) {
                    documentPO.setPropertyCode(documentUpdateDTO.getPropertyCode());
                }
                //private String showType;
                if (documentUpdateDTO.getShowType() != null) {
                    documentPO.setShowType(documentUpdateDTO.getShowType());
                }
                //private String opener;
                if (documentUpdateDTO.getOpener() != null) {
                    documentPO.setOpener(documentUpdateDTO.getOpener());
                }
                //private long openTime;
                if (documentUpdateDTO.getOpenTime() != null) {
                    long milliSecond = documentUpdateDTO.getOpenTime();
                    Date date = new Date();
                    date.setTime(milliSecond);
                    documentPO.setOpenTime(date);
                }
                //private Long deploymentId;// 流程ID
                if (documentUpdateDTO.getDeploymentId() != null) {
                    documentPO.setDeploymentId(documentUpdateDTO.getDeploymentId());
                }
                //private String activityName;// 活动CODE
                if (documentUpdateDTO.getActivityName() != null) {
                    documentPO.setActivityName(documentUpdateDTO.getActivityName());
                }
                //private String taskDescription;// 活动描述
                if (documentUpdateDTO.getTaskDescription() != null) {
                    documentPO.setTaskDescription(documentUpdateDTO.getTaskDescription());
                }
                //private String fileIcon;// 文件图标类型
                if (documentUpdateDTO.getFileIcon() != null) {
                    documentPO.setFileIcon(documentUpdateDTO.getFileIcon());
                }
                //private Boolean isFileView; // 是否启用附件预览
                if (documentUpdateDTO.getIsFileView() != null) {
                    documentPO.setIsFileView(documentUpdateDTO.getIsFileView());
                }
                //private String docContent;
                if (documentUpdateDTO.getDocContent() != null) {
                    documentPO.setDocContent(documentUpdateDTO.getDocContent());
                }
                //private String docSummary;
                if (documentUpdateDTO.getFileIcon() != null) {
                    documentPO.setFileIcon(documentUpdateDTO.getFileIcon());
                }
                //private String convertStatus; // 文档转换状态
                if (documentUpdateDTO.getConvertStatus() != null) {
                    documentPO.setConvertStatus(documentUpdateDTO.getConvertStatus());
                }
                //private String reason; // 不支持转换原因
                if (documentUpdateDTO.getReason() != null) {
                    documentPO.setReason(documentUpdateDTO.getReason());
                }
                // String convertPath; // 转换后路径
                if (documentUpdateDTO.getConvertPath() != null) {
                    documentPO.setConvertPath(documentUpdateDTO.getConvertPath());
                }
                //private String createStaffId;
                if (documentUpdateDTO.getCreateStaffId() != null) {
                    documentPO.setCreateStaffId(documentUpdateDTO.getCreateStaffId());
                }
                //private String modifyStaffId;
                if (documentUpdateDTO.getModifyStaffId() != null) {
                    documentPO.setModifyStaffId(documentUpdateDTO.getModifyStaffId());
                }
                documentDao.updateAllById(documentPO);
            } else {
                throw new FileServerException(FileServerErrorEnum.FILE_NO_ERROR);
            }
        } else {
            throw new FileServerException(FileServerErrorEnum.FILE_PARAM_LOST_ERROR);
        }
    }

    @Override
    public List<DocumentVO> selectBusinessIdAllFile(Long linkId, String type, String fileType, String propertyCode) {
        List<DocumentVO> list = new ArrayList<>();
        List<DocumentPO> documentPOS = documentDao.selectListByLinkIdAndType(linkId, type, fileType, propertyCode);
        if (documentPOS != null && documentPOS.size() > 0) {
            documentPOS.forEach(documentPO -> {
                DocumentVO documentVO = new DocumentVO();
                documentVO.setId(documentPO.getId());
                documentVO.setPath(documentPO.getFilePath());
                documentVO.setName(documentPO.getFileName());
                documentVO.setType(documentPO.getFileType());
                documentVO.setSize(documentPO.getFileSize());
                documentVO.setLinkId(documentPO.getLinkId());
                documentVO.setFileType(documentPO.getFileType());
                documentVO.setSizeDis(documentPO.getSizeDis());
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
                documentVO.setFileIcon(documentPO.getFileIcon());
                documentVO.setIsFileView(documentPO.getIsFileView());
                documentVO.setDocContent(documentPO.getDocContent());
                documentVO.setDocSummary(documentPO.getDocSummary());
                documentVO.setConvertStatus(documentPO.getConvertStatus());
                documentVO.setReason(documentPO.getReason());
                documentVO.setConvertPath(documentPO.getConvertPath());
                documentVO.setDownloadTimes(documentPO.getDownloadTimes());
                documentVO.setPreviewTimes(documentPO.getPreviewTimes());
                if (documentPO.getCreateTime() != null) {
                    documentVO.setCreateTime(documentPO.getCreateTime());
                }
                if (documentPO.getModifyTime() != null) {
                    documentVO.setModifyTime(documentPO.getModifyTime());
                }
                if (documentPO.getCreateStaffId() != null) {
                    documentVO.setCreateStaffId(documentPO.getCreateStaffId().toString());
                }
                if (documentPO.getModifyStaffId() != null) {
                    documentVO.setModifyStaffId(documentPO.getModifyStaffId().toString());
                }
                list.add(documentVO);
            });
        }
        return list;
    }

    @Override
    public List<DocumentVO> selectBusinessIdAllFiles(List<Long> linkIds, String type, String fileType, String propertyCode) {
        List<DocumentVO> list = new ArrayList<>();
//        List<DocumentPO> documentPOS = documentDao.selectListByLinkIdsAndType(linkIds, type, fileType, propertyCode);
        List<DocumentPO> documentPOS = new ArrayList<>();;
            int batch = linkIds.size() / 1000;
            if (0 == batch) {
                documentPOS.addAll(documentDao.selectListByLinkIdsAndType(linkIds, type, fileType, propertyCode));
            } else {
                for (int i = 0; i < batch; i++) {
                    documentPOS.addAll(documentDao.selectListByLinkIdsAndType(linkIds.subList(i * 1000, i * 1000 + 1000), type, fileType, propertyCode));
                }
                if (linkIds.size() % 1000 != 0) {
                    documentPOS.addAll(documentDao.selectListByLinkIdsAndType(linkIds.subList(batch * 1000, linkIds.size()), type, fileType, propertyCode));
                }
            }

        if (documentPOS != null && documentPOS.size() > 0) {
            documentPOS.forEach(documentPO -> {
                DocumentVO documentVO = new DocumentVO();
                documentVO.setId(documentPO.getId());
                documentVO.setPath(documentPO.getFilePath());
                documentVO.setName(documentPO.getFileName());
                documentVO.setType(documentPO.getFileType());
                documentVO.setSize(documentPO.getFileSize());
                documentVO.setLinkId(documentPO.getLinkId());
                documentVO.setFileType(documentPO.getFileType());
                documentVO.setSizeDis(documentPO.getSizeDis());
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
                documentVO.setFileIcon(documentPO.getFileIcon());
                documentVO.setIsFileView(documentPO.getIsFileView());
                documentVO.setDocContent(documentPO.getDocContent());
                documentVO.setDocSummary(documentPO.getDocSummary());
                documentVO.setConvertStatus(documentPO.getConvertStatus());
                documentVO.setReason(documentPO.getReason());
                documentVO.setConvertPath(documentPO.getConvertPath());
                documentVO.setDownloadTimes(documentPO.getDownloadTimes());
                documentVO.setPreviewTimes(documentPO.getPreviewTimes());
                if (documentPO.getCreateTime() != null) {
                    documentVO.setCreateTime(documentPO.getCreateTime());
                }
                if (documentPO.getModifyTime() != null) {
                    documentVO.setModifyTime(documentPO.getModifyTime());
                }
                if (documentPO.getCreateStaffId() != null) {
                    documentVO.setCreateStaffId(documentPO.getCreateStaffId().toString());
                }
                if (documentPO.getModifyStaffId() != null) {
                    documentVO.setModifyStaffId(documentPO.getModifyStaffId().toString());
                }
                list.add(documentVO);
            });
        }
        return list;
    }

    @Override
    public List<DocumentVO> selectBusinessIdAllFilesJson(DocumentQueryDTO documentQueryDTO) {
        return selectBusinessIdAllFiles(documentQueryDTO.getLinkIds(), documentQueryDTO.getType(),
                documentQueryDTO.getFileType(), documentQueryDTO.getPropertyCode());
    }

    @Override
    public void deleteOneFile(Long id){
        if (id != null) {
            DocumentPO documentPO = documentDao.selectById(id);
            if (!ObjectUtils.isEmpty(documentPO)) {
                try {
                    String tenantId = TenantUtil.getTenantId();
                    //删除minio服务器文件
                    if (!ObjectUtils.isEmpty(documentPO.getFilePath())) {
                        fileService.remove(tenantId, documentPO.getFilePath());
                    }
                    if (!ObjectUtils.isEmpty(documentPO.getConvertPath())) {
                        fileService.remove(tenantId, documentPO.getConvertPath());
                    }
                    documentDao.deleteByFileId(id);
                } catch (Exception e) {
                    log.error("当前文件删除发生错误，documentPO：{}", JSON.toJSONString(documentPO), e);
                }
            }
        }
    }

    @Override
    public void deleteFilesByIdArr(List<Long> idArr) {
        if (idArr != null && idArr.size() > 0) {
            idArr.forEach(this::deleteOneFile);
        }
    }


    @Override
    public void move(String oldPath, String newPath) {
        String tenantId = TenantUtil.getTenantId();
        try {
            fileService.move(tenantId, oldPath, newPath);
        } catch (Exception e) {
            log.error("move:",e);
            throw new FileServerException(FileServerErrorEnum.FILE_MOVE_ERROR);
        }
    }

    /**
     * 1.查询单个
     *
     * @param id
     * @return
     */
    @Override
    public DocumentVO selectFileByFileId(Long id) {
        DocumentVO documentVO = new DocumentVO();
        if (id != null) {
            DocumentPO documentPO = documentDao.selectById(id);
            if (documentPO != null) {
                documentVO.setId(documentPO.getId());
                documentVO.setPath(documentPO.getFilePath());
                documentVO.setName(documentPO.getFileName());
                documentVO.setType(documentPO.getFileType());
                documentVO.setSize(documentPO.getFileSize());
                documentVO.setLinkId(documentPO.getLinkId());
                documentVO.setFileType(documentPO.getFileType());
                documentVO.setSizeDis(documentPO.getSizeDis());
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
                documentVO.setFileIcon(documentPO.getFileIcon());
                documentVO.setIsFileView(documentPO.getIsFileView());
                documentVO.setDocContent(documentPO.getDocContent());
                documentVO.setDocSummary(documentPO.getDocSummary());
                documentVO.setConvertStatus(documentPO.getConvertStatus());
                documentVO.setReason(documentPO.getReason());
                documentVO.setConvertPath(documentPO.getConvertPath());
                documentVO.setDownloadTimes(documentPO.getDownloadTimes());
                documentVO.setPreviewTimes(documentPO.getPreviewTimes());
                if (documentPO.getCreateTime() != null) {
                    documentVO.setCreateTime(documentPO.getCreateTime());
                }
                if (documentPO.getModifyTime() != null) {
                    documentVO.setModifyTime(documentPO.getModifyTime());
                }
                if (documentPO.getCreateStaffId() != null) {
                    documentVO.setCreateStaffId(documentPO.getCreateStaffId().toString());
                }
                if (documentPO.getModifyStaffId() != null) {
                    documentVO.setModifyStaffId(documentPO.getModifyStaffId().toString());
                }
            }
        } else {
            log.error("file is null");
        }
        return documentVO;
    }


    /**
     * 文件（二进制数据）下载
     *
     * @param filePath 文件路径
     * @return
     */
    @Override
    public ResponseEntity<byte[]> downloadFile(String filePath) {
        log.info("文件下载,方法 downloadFile---参数路径--filePath: " + filePath);
        HttpHeaders headers = new HttpHeaders();
        ResponseEntity<byte[]> entity = null;
        InputStream in = null;
        File file2 = null;
        InputStream inputStreamFromMinio = null;
        try {
            String filePath1 = filePath.replace("\\", "/");
            if (!filePath1.startsWith("/")) {
                filePath1 = "/" + filePath1;
            }
            String filename = filePath1.substring(filePath1.lastIndexOf("/") + 1);
            // 下载到临时目录
            String tenantId = TenantUtil.getTenantId();
            try {
                inputStreamFromMinio = fileService.downLoad(tenantId, filePath1);
            } catch (Exception e) {
                log.error(e.getMessage());
            }
            //业务关联信息存入数据库
            String tempDir = PathUtil.getTempPath() + "BinaryDownload/" + System.currentTimeMillis() + Constants.PATH;
            FileUtil.createDir(tempDir);
            String tempFilePath = tempDir + filename;
            FileUtil.createNewFile(tempFilePath);
            FileUtil.copyFileUsingFileStreams(inputStreamFromMinio, new File(tempFilePath));
            //从临时目录文件中读取文件信息
             file2 = new File(tempFilePath);
            if (!file2.exists()) {
                throw new FileServerException(FileServerErrorEnum.FILE_NO_ERROR);
            }
            in = new FileInputStream(file2);
            byte[] bytes = new byte[in.available()];
            in.read(bytes);
            headers.add("Content-Disposition", "attachment;filename=" + filename);
            entity = new ResponseEntity<byte[]>(bytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    log.error(e.getMessage());
                }
            }
            if (inputStreamFromMinio != null) {
                try {
                    inputStreamFromMinio.close();
                } catch (IOException e) {
                    log.error(e.getMessage());
                }
            }
            //删除临时文件
            if (null != file2) {
                if (!file2.delete()) {
                    log.error("删除临时文件发生错误,file2:{}",file2);
                }else {
                    log.info("删除临时文件成功,file2:{}",file2);
                }
            }
        }
        return entity;
    }

    /**
     * 附件 上传
     *
     * @param uploadFile
     * @return 返回 模块索引
     */
    @Override
    public Result fileUpload(MultipartFile uploadFile) {
        log.info("fileServer-----------保存文件名：" + uploadFile.getName());
        String filename = uploadFile.getOriginalFilename();
        String timestamp = String.valueOf(System.currentTimeMillis());
        String filePath = getFilePath(Constants.DEFAULT_FOLDER, timestamp, filename);
        String tenantId = TenantUtil.getTenantId();
        try {
            InputStream inputStream = uploadFile.getInputStream();
            fileService.upLoadStream(tenantId, filePath, inputStream);
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new FileServerException(FileServerErrorEnum.FILE_UPLOAD_ERROR);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new FileServerException(FileServerErrorEnum.FILE_UPLOAD_ERROR);
        }
        return Result.data(filePath);
    }
}