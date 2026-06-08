package com.supcon.supfusion.file.server.api;

import com.supcon.supfusion.file.server.common.constants.Constants;
import com.supcon.supfusion.file.server.common.exception.FileServerErrorEnum;
import com.supcon.supfusion.file.server.common.exception.FileServerException;
import com.supcon.supfusion.file.server.common.utils.PathUtil;
import com.supcon.supfusion.file.server.service.FileService;
import com.supcon.supfusion.file.server.service.bo.FileInfoBO;
import com.supcon.supfusion.framework.cloud.annotation.InternalApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@InternalApi(path = HttpConstants.URL_INTERNALAPI + HttpConstants.URL_SPLITER + "${spring.application.name}" + HttpConstants.URL_SPLITER + "v1")
public class InterApiFileController {
    @Autowired
    private FileService fileService;

    @Value("${minio.fileTypes}")
    private String fileTypes;



    //上传文件
    @PostMapping(value = "/upload")
    public ListResult<String> upload(@RequestParam(value = "folder", defaultValue = Constants.DEFAULT_FOLDER) String folder,
                                     @RequestParam("files") MultipartFile[] files) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String tenantId = RpcContext.getContext().getTenantId();
        tenantId = tenantId == null || tenantId != "" ? Constants.DEFAULT_TENANT_ID : tenantId;
        List<String> filePathList = new ArrayList<String>();
        Boolean errorExist = false;
        for (MultipartFile file : files) {
            if(checkFileType(file.getOriginalFilename())){
                try {
                    String filePath = PathUtil.getFilePath(folder, timestamp, file.getOriginalFilename());
                    fileService.upLoad(tenantId, filePath, file);
                    filePathList.add(filePath.substring(1));
                }catch (Exception ex){
                    errorExist = true;
                }
            }
        }
        ListResult<String> result= new ListResult<String>(filePathList);
        if(errorExist){
            result.setCode(FileServerErrorEnum.FILE_UPLOAD_ERROR.getCode());
            result.setMessage(FileServerErrorEnum.FILE_TYPE_ERROR.getMessage());
        }else if(files.length!=filePathList.size()){
            result.setCode(FileServerErrorEnum.FILE_TYPE_ERROR.getCode());
            result.setMessage(FileServerErrorEnum.FILE_TYPE_ERROR.getMessage());
        }
        return result;
    }

    //下载文件
    @GetMapping(value = "/download")
    public void download(@RequestParam(value = "filePath") String filePath,
                         HttpServletResponse response) {
        String tenantId = RpcContext.getContext().getTenantId();
        tenantId = tenantId != null && tenantId != "" ? tenantId : Constants.DEFAULT_TENANT_ID;
        InputStream in = null;
        BufferedInputStream inBuffer = null;
        OutputStream out = null;
        BufferedOutputStream outBuffer = null;
        try {
            int slash = filePath.lastIndexOf("/");
            String returnFileName = new String(filePath.substring(slash+1).getBytes("UTF-8"), "ISO8859-1");
            response.setHeader("content-type", "application/octet-stream");
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment;filename=" + returnFileName);
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
            throw new FileServerException(FileServerErrorEnum.FILE_DOWNLOAD_ERROR);
        } finally {
            try {
                inBuffer.close();
                in.close();
                outBuffer.close();
                out.close();
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        }
    }

    //预览文件(仅限浏览器支持的文件格式)
    @GetMapping(value = "/overview")
    public void overview(@RequestParam(value = "filePath") String filePath,
                         HttpServletResponse response) {
        String tenantId = RpcContext.getContext().getTenantId();
        tenantId = tenantId != null && tenantId != "" ? tenantId : Constants.DEFAULT_TENANT_ID;
        InputStream in = null;
        BufferedInputStream inBuffer = null;
        OutputStream out = null;
        BufferedOutputStream outBuffer = null;
        try {
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
            throw new FileServerException(FileServerErrorEnum.FILE_DOWNLOAD_ERROR);
        } finally {
            try {
                inBuffer.close();
                in.close();
                outBuffer.close();
                out.close();
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        }
    }

    //删除文件
    @DeleteMapping(value = "/remove")
    public void delete(@RequestParam(value = "filePath") String filePath) {
        String tenantId = RpcContext.getContext().getTenantId();
        tenantId = tenantId != null && tenantId != "" ? tenantId : Constants.DEFAULT_TENANT_ID;
        if(filePath==null||filePath.isEmpty()){
            throw new FileServerException(FileServerErrorEnum.FILE_PARAMS_ERROR);
        }
        try {
            fileService.remove(tenantId, filePath);
        } catch (Exception e) {
            throw new FileServerException(FileServerErrorEnum.FILE_REMOVE_ERROR);
        }
    }

    //删除文件夹
    @DeleteMapping(value = "/removeFolder")
    public void deleteFolder(@RequestParam(value = "folder") String folder) {
        String tenantId = RpcContext.getContext().getTenantId();
        tenantId = tenantId != null && tenantId != "" ? tenantId : Constants.DEFAULT_TENANT_ID;
        if(folder==null||folder.isEmpty()){
            throw new FileServerException(FileServerErrorEnum.FILE_PARAMS_ERROR);
        }
        try {
            fileService.removeFolder(tenantId, folder);
        } catch (Exception e) {
            throw new FileServerException(FileServerErrorEnum.FILE_REMOVE_ERROR);
        }
    }

    //请求文件列表
    @GetMapping(value = "/listAll")
    public ListResult<FileInfoBO> listAll() {
        String tenantId = RpcContext.getContext().getTenantId();
        tenantId = tenantId != null && tenantId != "" ? tenantId : Constants.DEFAULT_TENANT_ID;
        List<FileInfoBO> result = new ArrayList<FileInfoBO>();
        try {
            result = fileService.getListAll(tenantId);
        } catch (Exception e) {
            throw new FileServerException(FileServerErrorEnum.FILE_QUERY_ERROR);
        }
        return new ListResult<FileInfoBO>(result);
    }

    //请求文件列表
    @GetMapping(value = "/list")
    public ListResult<FileInfoBO> list(@RequestParam(value = "folder", defaultValue = Constants.DEFAULT_FOLDER) String folder,
                                       HttpServletRequest request) {
        String tenantId = RpcContext.getContext().getTenantId();
        tenantId = tenantId != null && tenantId != "" ? tenantId : Constants.DEFAULT_TENANT_ID;
        List<FileInfoBO> result = new ArrayList<FileInfoBO>();
        try {
            result = fileService.getList(tenantId, folder);
        } catch (Exception e) {
            log.error("请求文件列表失败", e);
        }
        return new ListResult<FileInfoBO>(result);
    }

    //移动文件
    @PutMapping(value = "/move")
    public void move(@RequestParam(value = "oldPath") String oldPath, @RequestParam("newPath") String newPath) {
        String tenantId = RpcContext.getContext().getTenantId();
        tenantId = tenantId != null && tenantId != "" ? tenantId : Constants.DEFAULT_TENANT_ID;
        try {
            fileService.move(tenantId, oldPath, newPath);
        }catch (Exception e){
            throw new FileServerException(FileServerErrorEnum.FILE_MOVE_ERROR);
        }
    }

    public boolean checkFileType(String fileName){
        String[] types = fileTypes.split(",");
        String fileType = fileName.substring(fileName.lastIndexOf(".")+1);
        for(String type:types){
            if(type.equalsIgnoreCase(fileType)){
                return true;
            }
        }
        return false;
    }
}
