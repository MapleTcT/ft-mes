package com.supcon.supfusion.file.server.service.impl;

import com.supcon.supfusion.file.server.common.utils.BaseUtil;
import com.supcon.supfusion.file.server.service.FileService;
import com.supcon.supfusion.file.server.service.bo.FileInfoBO;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.messages.Item;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class MinIOServiceImpl implements FileService {
    private static Map<String, String> FILE_TYPE_IMAGE = BaseUtil.FILE_TYPE_IMAGE;
    private static String imageContentType = "image/";
    private static String applicationContentType = "application/";

    private String contentType = "application/octet-stream";

    @Autowired
    private MinioClient minioClient;

    @Override
    public void upLoad(String tenantId, String filePath, MultipartFile file) throws Exception {
        createBucket(tenantId);
        minioClient.putObject(tenantId, filePath, file.getInputStream(), contentType);
    }

    @Override
    public void upLoadStream(String tenantId, String filePath, InputStream inputStream) throws Exception {
        createBucket(tenantId);
        minioClient.putObject(tenantId, filePath, inputStream, contentType);
    }

    @Override
    public void upLoadStreamForConvert(String tenantId, String filePath, InputStream inputStream, String fileType) throws Exception {
        createBucket(tenantId);
        //根据文件类型设置contentType
        StringBuilder contentType = new StringBuilder();
        if ("image".equals(FILE_TYPE_IMAGE.get(fileType))) {
            if ("jpg".equals(fileType) || "jpeg".equals(fileType) || "jfif".equals(fileType)) {
                contentType.append(imageContentType).append("jpeg");
            } else if ("gif".equals(fileType) || "png".equals(fileType) || "tiff".equals(fileType)) {
                contentType.append(imageContentType).append(fileType);
            } else if ("tif".equals(fileType)) {
                contentType.append(imageContentType).append("tiff");
            } else if ("bmp".equals(fileType)) {
                contentType.append(imageContentType).append("bmp");
            } else if ("wmf".equals(fileType)) {
                contentType.append(applicationContentType).append("x-wmf");
            }
        } else if ("excel".equals(FILE_TYPE_IMAGE.get(fileType))) {
//            fileName = docuName + ".html";
//            contentType.append("text/html");
            contentType.append(applicationContentType).append("pdf");
        } else if ("video".equals(FILE_TYPE_IMAGE.get(fileType))) {
//            fileName = docuName + ".m3u8";
        } else if ("word".equals(FILE_TYPE_IMAGE.get(fileType)) || "word2".equals(FILE_TYPE_IMAGE.get(fileType)) || "ppt".equals(FILE_TYPE_IMAGE.get(fileType))
                || "pdf".equals(FILE_TYPE_IMAGE.get(fileType))) {
            contentType.append(applicationContentType).append("pdf");
        }
        minioClient.putObject(tenantId, filePath, inputStream, contentType.toString());
        inputStream.close();
    }

    @Override
    public InputStream downLoad(String tenantId, String filePath) throws Exception {
        return minioClient.getObject(tenantId, filePath);
    }

    @Override
    public void remove(String tenantId, String filePath) throws Exception {
        minioClient.removeObject(tenantId, filePath);
    }

    @Override
    public void removeFolder(String tenantId, String folder) throws Exception {
        Iterable<Result<Item>> items = minioClient.listObjects(tenantId, folder, true);
        for (Result<Item> item : items) {
            minioClient.removeObject(tenantId, item.get().objectName());
        }
    }

    @Override
    public void removeFolderOfOneDay(String tenantId, String folder) throws Exception {
        Iterable<Result<Item>> items = minioClient.listObjects(tenantId, folder, true);
        for (Result<Item> item : items) {
            //判断是否为一天内临时文件
            String objectName = item.get().objectName();
            String date = objectName.substring(objectName.indexOf("/") + 1, objectName.lastIndexOf("/"));
            if (System.currentTimeMillis() - Long.parseLong(date) >= 24 * 60 * 60 * 1000) {
                minioClient.removeObject(tenantId, objectName);
            }
        }
    }

    @Override
    public List<FileInfoBO> getList(String tenantId, String folder) throws Exception {
        Iterable<Result<Item>> items = minioClient.listObjects(tenantId, folder, true);
        List<FileInfoBO> result = new ArrayList<FileInfoBO>();
        for (Result<Item> item : items) {
            Item t = item.get();
            int point = t.objectName().lastIndexOf(".");
            int slash = t.objectName().lastIndexOf("/");
            FileInfoBO fileInfoBO = new FileInfoBO(t.objectName().substring(slash + 1), t.objectName(),
                    t.objectName().substring(point + 1).toUpperCase(), t.objectSize(), t.lastModified().getTime());
            result.add(fileInfoBO);
        }
        return result;
    }

    @Override
    public List<FileInfoBO> getListAll(String tenantId) throws Exception {
        Iterable<Result<Item>> items = minioClient.listObjects(tenantId);
        List<FileInfoBO> result = new ArrayList<FileInfoBO>();
        for (Result<Item> item : items) {
            Item t = item.get();
            int point = t.objectName().lastIndexOf(".");
            int slash = t.objectName().lastIndexOf("/");
            FileInfoBO fileInfoBO = new FileInfoBO(t.objectName().substring(slash + 1), t.objectName(),
                    t.objectName().substring(point + 1).toUpperCase(), t.objectSize(), t.lastModified().getTime());
            result.add(fileInfoBO);
        }
        return result;
    }

    @Override
    public void move(String tenantId, String oldPath, String newPath) throws Exception {
        minioClient.copyObject(tenantId, oldPath, tenantId, newPath);
        minioClient.removeObject(tenantId, oldPath);
    }

    @Override
    public void copy(String tenantId, String oldPath, String newPath) throws Exception {
        minioClient.copyObject(tenantId, oldPath, tenantId, newPath);
    }

    @Override
    public String getPreviewUrl(String tenantId, String filePath) throws Exception {
        return minioClient.presignedGetObject(tenantId, filePath);
    }

    //判断bucket是否存在，不存在则创建
    public void createBucket(String tenantId) throws Exception {
        if (!minioClient.bucketExists(tenantId)) {
            minioClient.makeBucket(tenantId);
        }
    }
}
