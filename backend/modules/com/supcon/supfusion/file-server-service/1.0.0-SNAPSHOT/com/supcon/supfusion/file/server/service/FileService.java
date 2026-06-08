package com.supcon.supfusion.file.server.service;

import com.supcon.supfusion.file.server.service.bo.FileInfoBO;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;


public interface FileService {

    /**
     * 上传文件
     * @param tenantId 租户ID
     * @param filePath 文件待存储路径
     * @param file 待上传文件
     * @throws Exception
     */
    void upLoad(String tenantId, String filePath, MultipartFile file) throws Exception;


    /**
     * 上传文件
     * @param tenantId 租户ID
     * @param filePath 文件待存储路径
     * @param inputStream 待上传文件流
     * @throws Exception
     */
    void upLoadStream(String tenantId, String filePath, InputStream inputStream) throws Exception;

    /**
     * 上传转换文件
     * @param tenantId 租户ID
     * @param filePath 文件待存储路径
     * @param inputStream 待上传文件流
     * @param fileType
     * @throws Exception
     */
    void upLoadStreamForConvert(String tenantId, String filePath, InputStream inputStream, String fileType) throws Exception;

    /**
     * 下载文件
     * @param tenantId 租户ID
     * @param filePath 文件存储路径
     * @return
     * @throws Exception
     */
    InputStream downLoad(String tenantId, String filePath) throws Exception;

    /**
     * 删除文件
     * @param tenantId 租户ID
     * @param filePath 待删除文件路径
     * @throws Exception
     */
    void remove(String tenantId, String filePath) throws Exception;

    /**
     * 删除文件夹
     * @param tenantId 租户ID
     * @param folder 待删除文件夹
     * @throws Exception
     */
    void removeFolder(String tenantId, String folder) throws Exception;

    /**
     * 删除一天前创建的临时文件
     * @param tenantId 租户ID
     * @param folder 待删除文件夹
     * @throws Exception
     */
    void removeFolderOfOneDay(String tenantId, String folder) throws Exception;

    /**
     * 获取指定目录下文件列表
     * @param tenantId 租户ID
     * @param folder 文件夹
     * @return
     * @throws Exception
     */
    List<FileInfoBO> getList(String tenantId, String folder) throws Exception;

    /**
     * 获取该租户所有文件列表
     * @param tenantId 租户ID
     * @return
     * @throws Exception
     */
    List<FileInfoBO> getListAll(String tenantId) throws Exception;

    /**
     * 文件移动
     * @param tenantId 租户ID
     * @param oldPath 文件移动前路径
     * @param newPath 文件移动后路径
     * @throws Exception
     */
    void move(String tenantId, String oldPath, String newPath) throws Exception;

    /**
     * 文件复制
     * @param tenantId 租户ID
     * @param oldPath 文件移动前路径
     * @param newPath 文件移动后路径
     * @throws Exception
     */
    void copy(String tenantId, String oldPath, String newPath) throws Exception;

    /**
     *获取预览url
     * @param tenantId 租户ID
     * @param filePath 文件存储路径
     * @return
     */
    String getPreviewUrl(String tenantId, String filePath) throws Exception;
}
