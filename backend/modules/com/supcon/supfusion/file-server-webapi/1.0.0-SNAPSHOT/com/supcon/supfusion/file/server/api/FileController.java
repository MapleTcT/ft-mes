package com.supcon.supfusion.file.server.api;

import com.supcon.supfusion.file.server.common.ConvertStatus;
import com.supcon.supfusion.file.server.common.constants.Constants;
import com.supcon.supfusion.file.server.common.exception.FileServerErrorEnum;
import com.supcon.supfusion.file.server.common.exception.FileServerException;
import com.supcon.supfusion.file.server.common.utils.DocumentUtils;
import com.supcon.supfusion.file.server.common.utils.IPUtils;
import com.supcon.supfusion.file.server.common.utils.SystemUtils;
import com.supcon.supfusion.file.server.common.utils.TenantUtil;
import com.supcon.supfusion.file.server.common.vo.DocumentUploadVO;
import com.supcon.supfusion.file.server.dao.po.DocumentDownloadInfoPO;
import com.supcon.supfusion.file.server.dao.po.DocumentPO;
import com.supcon.supfusion.file.server.service.DocumentService;
import com.supcon.supfusion.file.server.service.FileConvertService;
import com.supcon.supfusion.file.server.service.FileDaoService;
import com.supcon.supfusion.file.server.service.FileService;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.context.UserContext;
import com.supcon.supfusion.framework.cloud.common.exception.BizException;
import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;
import sun.misc.BASE64Encoder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.*;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.util.*;

@Slf4j
@Api(tags = "附件inter API相关接口")
@RestController
@RequestMapping(path = HttpConstants.URL_INTERNALAPI + HttpConstants.URL_SPLITER + "${spring.application.name}" + HttpConstants.URL_SPLITER + "v1" + "/file")
public class FileController {

    @Autowired
    private FileService fileService;
    @Autowired
    private DocumentService documentService;
    @Autowired
    private FileConvertService fileConvertService;
    @Autowired
    private FileDaoService fileDaoService;

    @Value("${minio.fileTypes}")
    private String fileTypes;

    private static String PDF_URL = "/inter-api/file-server/web/viewer.html";
    private static String imageContentType = "image/";
    private static String applicationContentType = "application/";

    public String getFilePath(String folder, String timestamp, String fileName) {
        return "/" + folder + "/" + timestamp + "/" + fileName;
    }

    //上传文件 到临时目录
    @ApiOperation(value="上传单个附件",notes="流上传 返回文件临时路径 附件在请求体 form-data中")
    @PostMapping(value = "/upload/file")
    public Result<Map<String, String>> uploadOneFile(HttpServletRequest request) {
        Result<Map<String, String>> result = new Result<>();
        Map<String, String> responseMap = new HashMap<>();
        boolean isMultipart = ServletFileUpload.isMultipartContent(request);
        if (!isMultipart) {
            throw new FileServerException(FileServerErrorEnum.FILE_EMPTY_ERROR);
        }
        Collection<Part> parts = null;
        try {
            parts = request.getParts();
        } catch (IOException | ServletException e) {
            log.error(e.getMessage());
        }
        String fileName = null;
        InputStream input = null;
        for (Iterator<Part> iterator = parts.iterator(); iterator.hasNext(); ) {
            Part part = iterator.next();
            log.info("单个文件上传-----类型名称------->" + part.getName());
            log.info("单个文件上传-----类型------->" + part.getContentType());
            log.info("单个文件上传-----提交的类型名称------->" + part.getSubmittedFileName());
            try {
                log.info("单个文件上传----流-------->" + part.getInputStream());

                //fileName = part.getSubmittedFileName();
                String cd = part.getHeader("Content-Disposition");
                //截取不同类型的文件需要自行判断
                fileName = cd.substring(cd.lastIndexOf("filename=")+10, cd.length()-1);
                if (fileName.contains("\\")) {
                    fileName = fileName.substring(fileName.lastIndexOf("\\") + 1);
                }

                //input = request.getInputStream();
                input = part.getInputStream();
                if (0 == input.available()) {
                    log.info("当前文件内容为空,fileName:{}", fileName);
                    throw new BizException(FileServerErrorEnum.FILE_UPLOAD_FAIL_AND_REASON, fileName, "文件为空不允许上传");
                }
                log.info("单个上传文件名 filename:" + fileName);
                String timestamp = String.valueOf(System.currentTimeMillis());
                String tenantId = TenantUtil.getTenantId();
                if (!ObjectUtils.isEmpty(input) && !ObjectUtils.isEmpty(fileName)) {
                    try {
                        String filePath = getFilePath(Constants.TEMP_FOLDER, timestamp, fileName);
                        fileService.upLoadStream(tenantId, filePath, input);
                        responseMap.put("path", filePath.substring(1));
                        responseMap.put("filename", fileName);
                        responseMap.put("fileIcon", DocumentUtils.getIcon(fileName));
                        result.setData(responseMap);
                    } catch (Exception ex) {
                        log.error(ex.getMessage());
                        throw new BizException(FileServerErrorEnum.FILE_UPLOAD_FAIL_AND_REASON, fileName, "上传minio服务器发生错误");
                    }
                }
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        }
        return result;
    }


    @ApiOperation(value="上传多个附件",notes="流上传 返回文件临时路径 附件在请求体 form-data中")
    //上传文件 到临时目录
    @PostMapping(value = "/upload/files")
    public ListResult<Map<String, String>> uploadFiles(HttpServletRequest request) {
        List<Map<String, String>> filePathList = new ArrayList<>();
        boolean isMultipart = ServletFileUpload.isMultipartContent(request);
        if (!isMultipart) {
            throw new FileServerException(FileServerErrorEnum.FILE_EMPTY_ERROR);
        }
        Collection<Part> parts = null;
        try {
            parts = request.getParts();
        } catch (IOException e) {
            log.error(e.getMessage());
        } catch (ServletException e) {
            log.error(e.getMessage());
        }
        String fileName = null;
        InputStream input = null;
        Boolean errorExist = false;
        for (Iterator<Part> iterator = parts.iterator(); iterator.hasNext(); ) {
            Part part = iterator.next();
            log.info("多个上传文件上传-----类型名称------->" + part.getName());
            log.info("多个上传文件上传-----类型------->" + part.getContentType());
            log.info("多个上传文件上传-----提交的类型名称------->" + part.getSubmittedFileName());
            try {
                log.info("多个上传文件上传----流-------->" + part.getInputStream());
                //input = request.getInputStream();
                input = part.getInputStream();
                //fileName = part.getSubmittedFileName();
                String cd = part.getHeader("Content-Disposition");
                //截取不同类型的文件需要自行判断
                fileName = cd.substring(cd.lastIndexOf("=")+2, cd.length()-1);
                if (fileName.contains("\\")) {
                    fileName = fileName.substring(fileName.lastIndexOf("\\") + 1);
                }
                log.info("上传文件名 filename:" + fileName);
                String timestamp = String.valueOf(System.currentTimeMillis());
                String tenantId = TenantUtil.getTenantId();
                if (input != null && fileName != null && !fileName.equals(Constants.STR_KONG)) {
                    try {
                        String filePath = getFilePath(Constants.TEMP_FOLDER, timestamp, fileName);
                        fileService.upLoadStream(tenantId, filePath, input);
                        Map<String, String> responseMap = new HashMap<>();
                        responseMap.put("path", filePath.substring(1));
                        responseMap.put("filename", fileName);
                        responseMap.put("fileIcon", DocumentUtils.getIcon(fileName));
                        filePathList.add(responseMap);
                    } catch (Exception ex) {
                        errorExist = true;
                        log.error(ex.getMessage());
                    }
                }
            } catch (Exception e) {
                errorExist = true;
                log.error(e.getMessage());
            }
        }
        ListResult<Map<String, String>> result = new ListResult<>(filePathList);
        if (errorExist) {
            throw new FileServerException(FileServerErrorEnum.FILE_UPLOAD_ERROR);
        }
        return result;
    }


    //下载文件
    @ApiOperation(value="下载附件",notes="需要鉴权")
    @ApiImplicitParams({
            @ApiImplicitParam(name="methodType",value="鉴权url 的请求方式",required=true,paramType="query",dataType="String"),
            @ApiImplicitParam(name="serverName",value="鉴权服务 服务名",required=true,paramType="query",dataType="String"),
            @ApiImplicitParam(name="url",value="鉴权URL",required=true,paramType="query",dataType="String"),
            @ApiImplicitParam(name="entityCode",value="实体编码",required=true,paramType="query",dataType="String"),
            @ApiImplicitParam(name="id",value="附件id",required=true,paramType="query",dataType="String")
    })
    @GetMapping(value = "/download")
    public Result download1(HttpServletRequest request,
                            @RequestParam(value = "methodType") String methodType,
                            @RequestParam(value = "serverName") String serverName,
                            @RequestParam(value = "url") String url,
                            @RequestParam(value = "entityCode") String entityCode,
                            @RequestParam(value = "id") String id,
                            HttpServletResponse response) {
        if (methodType == null || (methodType != null && methodType.equals(Constants.STR_KONG))) {
            throw new FileServerException(FileServerErrorEnum.FILE_PARAM_METHOD_TYPE_ERROR);
        }
        if (url == null || (url != null && url.equals(Constants.STR_KONG))) {
            throw new FileServerException(FileServerErrorEnum.FILE_PARAM_URL_ERROR);
        }
        if (id == null || (id != null && id.equals(Constants.STR_KONG))) {
            throw new FileServerException(FileServerErrorEnum.FILE_PARAM_LINKID_ERROR);
        }
        Boolean isCanDownload = false;
        Long userId = UserContext.getUserContext().getUserId();
        Long staffId = UserContext.getUserContext().getStaffId();
        String Authorization = request.getHeader("Authorization");
//        isCanDownload = documentService.getAuthentication(Authorization, methodType, serverName, url, entityCode, id, userId, staffId);
        isCanDownload = true;
        if (isCanDownload) {
            //根据id查询文件路径
            String filePath = documentService.downLoadQueryById(id);
            if (filePath != null && filePath.equals(Constants.STR_KONG)) {
                throw new FileServerException(FileServerErrorEnum.FILE_EMPTY_ERROR);
            }
            documentService.saveDownloadRecord(id, IPUtils.getClinetIpByReq(request), userId, staffId);
//            DocumentPO documentPO = fileDaoService.getById(id);
            String tenantId = TenantUtil.getTenantId();
            InputStream in = null;
            BufferedInputStream inBuffer = null;
            OutputStream out = null;
            BufferedOutputStream outBuffer = null;
            try {
                int slash = filePath.lastIndexOf("/");
                String returnFileName = new String(filePath.substring(slash + 1).getBytes("UTF-8"), "ISO8859-1");
                response.setHeader("content-type", "application/octet-stream");
                response.setContentType("application/octet-stream;charset=UTF-8");
                response.setCharacterEncoding("UTF-8");
                response.setHeader("Content-Disposition", "attachment;filename=\"" + returnFileName + "\"");
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
                response.setHeader("content-type", "application/json");
                response.setContentType("application/json;charset=UTF-8");
                response.setCharacterEncoding("UTF-8");
                response.setStatus(500);
                log.error("100117002:文件下载失败:",e);
                return Result.data(100117002,"文件下载失败",null);
                //throw new FileServerException(FileServerErrorEnum.FILE_DOWNLOAD_ERROR);
            } finally {
                if(inBuffer!=null){
                    try {
                        inBuffer.close();
                    } catch (IOException e) {
                        log.error(e.getMessage());
                    }
                }
                if(in!=null){
                    try {
                        in.close();
                    } catch (IOException e) {
                        log.error(e.getMessage());
                    }
                }
                if(outBuffer!=null){
                    try {
                        outBuffer.close();
                    } catch (IOException e) {
                        log.error(e.getMessage());
                    }
                }
                if(out!=null){
                    try {
                        out.close();
                    } catch (IOException e) {
                        log.error(e.getMessage());
                    }
                }
            }
        } else {
            log.error("download:100117018,无权限下载");
            response.setHeader("content-type", "application/json");
            response.setContentType("application/json;charset=UTF-8");
            response.setCharacterEncoding("UTF-8");
            response.setStatus(500);
            return Result.data(100117018,"没有下载权限",null);
            //throw new FileServerException(FileServerErrorEnum.FILE_DOWNLOAD_AUTH_ERROR);
        }
        return null;
    }

    /**
     * 查询附件
     *
     * @param linkId       不为空
     * @param type         不为空
     * @param propertyCode 可空
     * @return
     */
    @ApiOperation(value="查询附件列表",notes="根据linkId和fileType查询对应的附件列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name="linkId",value="关联ID",required=true,paramType="query",dataType="String"),
            @ApiImplicitParam(name="type",value="文档类型 辅助id",required=false,paramType="query",dataType="String"),
            @ApiImplicitParam(name="fileType",value="文件类型 pic：图片字段 attachment:普通附件 office:文档控件",required=false,paramType="query",dataType="String"),
            @ApiImplicitParam(name="propertyCode",value="字段CODE",required=false,paramType="query",dataType="String")
    })
    @GetMapping("/upload-list")
    public Result list(@RequestParam(value = "linkId") String linkId,
                       @RequestParam(value = "type", required = false) String type,
                       @RequestParam(value = "fileType", defaultValue = "attachment", required = false) String fileType,
                       @RequestParam(value = "propertyCode", required = false) String propertyCode) {
        if (linkId != null && linkId.equals(Constants.STR_KONG)) {
            throw new FileServerException(FileServerErrorEnum.FILE_LINK_ID_ERROR);
        }
        List<DocumentUploadVO> documentVOS = documentService.queryByLindIdAndTypeAndPropertyCodeAndFileView(linkId, type, fileType, propertyCode);
        return Result.data(documentVOS);
    }

    /**
     * 查询附件
     *
     * @param id 不为空
     * @return
     */
    @ApiOperation(value="删除附件",notes="根据附件id删除附件")
    @ApiImplicitParams({
            @ApiImplicitParam(name="id",value="附件id",required=true,paramType="query",dataType="String")
    })
    @DeleteMapping("/delete")
    public Map<String, Object> delete(@RequestParam(value = "id") String id) {
        documentService.deleteById(id);
        Map<String, Object> map = new HashMap<>();
        Map<String, Boolean> map2 = new HashMap<>();
        map.put("code", 200);
        map.put("success", true);
        map.put("msg", "操作成功");
        map2.put("dealSuccess", true);
        map.put("data", map2);
        return map;
    }

    /**
     * 查询下载详情
     *
     * @param id
     * @param pageNo
     * @param pageSize
     * @param ddl
     */
    @ApiOperation(value="查询附件下载详情",notes="根据附件id 和其他参数模糊搜索下载详情")
    @ApiImplicitParams({
            @ApiImplicitParam(name="id",value="附件id",required=true,paramType="query",dataType="Integer"),
            @ApiImplicitParam(name="pageNo",value="页数 默认1",required=true,paramType="query",dataType="Integer"),
            @ApiImplicitParam(name="pageSize",value="每页显示条数 默认20",required=true,paramType="query",dataType="Integer"),
            @ApiImplicitParam(name="ddl",value="参数map",required=false,paramType="body",dataType="Map")
    })
    @GetMapping(value = "/file-download-info-list", produces = "application/json")
    @ResponseBody
    public Result<List<DocumentDownloadInfoPO>> selectDownloadInfo(String id,
                                                                   @RequestParam(defaultValue = "1") Integer pageNo,
                                                                   @RequestParam(defaultValue = "20") Integer pageSize,
                                                                   @RequestBody(required = false) Map ddl) {
        if (id != null && id.equals(Constants.STR_KONG)) {
            throw new FileServerException(FileServerErrorEnum.FILE_FILE_ID_ERROR);
        }
        Result<List<DocumentDownloadInfoPO>> result = new Result<>();
        List<DocumentDownloadInfoPO> documentDownloadInfoPOs = documentService.selectByIdPage(id, pageNo, pageSize, ddl);
        result.setData(documentDownloadInfoPOs);
        return result;
    }


    /**
     * 预览图片 需要鉴权  返回base64编码之后的图片数据
     */
    @ApiOperation(value="图片下载接口",notes="根据附件id和实体编码鉴权下载图片")
    @ApiImplicitParams({
            @ApiImplicitParam(name="methodType",value="鉴权url 的请求方式",required=true,paramType="query",dataType="String"),
            @ApiImplicitParam(name="serverName",value="鉴权服务 服务名",required=true,paramType="query",dataType="String"),
            @ApiImplicitParam(name="url",value="鉴权URL",required=true,paramType="query",dataType="String"),
            @ApiImplicitParam(name="entityCode",value="实体编码",required=true,paramType="query",dataType="String"),
            @ApiImplicitParam(name="id",value="附件id",required=true,paramType="query",dataType="String")
    })
    @GetMapping(value = "/auth/overview/pic")
    public Result<Map<String, String>> overview2(HttpServletRequest request,
                                                 @RequestParam(value = "methodType") String methodType,
                                                 @RequestParam(value = "serverName") String serverName,
                                                 @RequestParam(value = "url") String url,
                                                 @RequestParam(value = "entityCode") String entityCode,
                                                 @RequestParam(value = "id") String id,
                                                 HttpServletResponse response) {
        if (methodType == null || (methodType != null && methodType.equals(Constants.STR_KONG))) {
            throw new FileServerException(FileServerErrorEnum.FILE_PARAM_METHOD_TYPE_ERROR);
        }
        if (url == null || (url != null && url.equals(Constants.STR_KONG))) {
            throw new FileServerException(FileServerErrorEnum.FILE_PARAM_URL_ERROR);
        }
        if (id == null || (id != null && id.equals(Constants.STR_KONG))) {
            throw new FileServerException(FileServerErrorEnum.FILE_PARAM_LINKID_ERROR);
        }
        Boolean isCanOverview = false;
        Long userId = UserContext.getUserContext().getUserId();
        Long staffId = UserContext.getUserContext().getStaffId();
        String Authorization = request.getHeader("Authorization");
//        isCanOverview = documentService.getAuthentication(Authorization, methodType, serverName, url, entityCode, id, userId, staffId);
        isCanOverview = true;
        if (isCanOverview) {
            Result<Map<String, String>> result = new Result<>();
            //根据id查询文件路径
            String filePath = documentService.overviewQueryById(id);
            if (filePath != null && filePath.equals(Constants.STR_KONG)) {
                throw new FileServerException(FileServerErrorEnum.FILE_EMPTY_ERROR);
            }
            documentService.saveOverViewRecord(id, IPUtils.getClinetIpByReq(request), userId, staffId);
//            DocumentPO documentPO = fileDaoService.getById(id);
            String tenantId = TenantUtil.getTenantId();
            InputStream in = null;
            BufferedInputStream inBuffer = null;
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            log.info("====overview 鉴权  filepath:" + filePath);
            try {
                in = fileService.downLoad(tenantId, filePath);
                inBuffer = new BufferedInputStream(in);
                int len = -1;
                byte[] bs = new byte[1024];
                while ((len = inBuffer.read(bs)) != -1) {
                    bos.write(bs, 0, len);
                }
                byte[] fileByte = bos.toByteArray();
                //以上为读取图片变成字节数组
                //进行base64位加密
                BASE64Encoder encoder = new BASE64Encoder();
                String imageData = encoder.encode(fileByte);
                //{"filePath":{"/文件存储路径"},"image":"base64位加密字节数组}
                Map<String, String> map = new HashMap<>();
                map.put("filePath", filePath);
                map.put("image", imageData);
                result.setData(map);
                return result;
            } catch (Exception e) {
                log.error("download:",e);
                throw new FileServerException(FileServerErrorEnum.FILE_DOWNLOAD_ERROR);
            } finally {
                try {
                    assert inBuffer != null;
                    inBuffer.close();
                    in.close();
                } catch (IOException e) {
                    log.error(e.getMessage());
                }
            }
        } else {
            throw new FileServerException(FileServerErrorEnum.FILE_OVERVIEW_AUTH_ERROR);
        }
    }

    /**
     * 预览附件 需要鉴权  返回minio的url
     */
    @ApiOperation(value="附件预览接口",notes="根据附件id和实体编码鉴权下载图片")
    @ApiImplicitParams({
            @ApiImplicitParam(name="methodType",value="鉴权url 的请求方式",required=true,paramType="query",dataType="String"),
            @ApiImplicitParam(name="serverName",value="鉴权服务 服务名",required=true,paramType="query",dataType="String"),
            @ApiImplicitParam(name="url",value="鉴权URL",required=true,paramType="query",dataType="String"),
            @ApiImplicitParam(name="entityCode",value="实体编码",required=true,paramType="query",dataType="String"),
            @ApiImplicitParam(name="id",value="附件id",required=true,paramType="query",dataType="String")
    })
    @GetMapping(value = "/auth/overview/file")
    public Result<Map<String, String>> overviewFile(HttpServletRequest request,
                                                 @RequestParam(value = "methodType") String methodType,
                                                 @RequestParam(value = "serverName") String serverName,
                                                 @RequestParam(value = "url") String url,
                                                 @RequestParam(value = "entityCode") String entityCode,
                                                 @RequestParam(value = "id") String id,
                                                 @RequestParam(value = "address") String address,
                                                 HttpServletResponse response) {
        if (ObjectUtils.isEmpty(methodType)) {
            throw new FileServerException(FileServerErrorEnum.FILE_PARAM_METHOD_TYPE_ERROR);
        }
        if (ObjectUtils.isEmpty(url)) {
            throw new FileServerException(FileServerErrorEnum.FILE_PARAM_URL_ERROR);
        }
        if (ObjectUtils.isEmpty(id)) {
            throw new FileServerException(FileServerErrorEnum.FILE_PARAM_LINKID_ERROR);
        }
        Boolean isCanOverview = false;
        Long userId = UserContext.getUserContext().getUserId();
        Long staffId = UserContext.getUserContext().getStaffId();
        String Authorization = request.getHeader("Authorization");
//        isCanOverview = documentService.getAuthentication(Authorization, methodType, serverName, url, entityCode, id, userId, staffId);
        isCanOverview = true;
        if (isCanOverview) {
            Result<Map<String, String>> result = new Result<>();
            //根据id查询文件路径
            DocumentPO documentPO = documentService.convertPathById(id);
            if (ObjectUtils.isEmpty(documentPO.getFilePath())) {
                throw new FileServerException(FileServerErrorEnum.FILE_EMPTY_ERROR);
            }
            String convertStatus = documentPO.getConvertStatus();
            if (!ObjectUtils.isEmpty(convertStatus)) {
                Map<String, String> map = new HashMap<>();
                result.setCode(200);
                if (convertStatus.equals(ConvertStatus.FAILURE.toString())) {
                    //再次转换
                    fileConvertService.fileConvert(documentPO);
                    map.put("status", "false");
                    result.setData(map);
                    result.setMessage("附件转换失败:" + documentPO.getReason());
                    return result;
//                    throw new FileServerException(FileServerErrorEnum.FILE_OVERVIEW__ERROR);
                } else if (convertStatus.equals(ConvertStatus.CONVERTING.toString())) {
                    map.put("status", "convert");
                    result.setData(map);
                    result.setMessage("附件正在转换，请稍候");
                    return result;
//                    throw new FileServerException(FileServerErrorEnum.FILE_OVERVIEW_CONVERT);
                } else if (convertStatus.equals(ConvertStatus.UNSUPPORTED.toString())) {
                    map.put("status", "false");
                    result.setData(map);
                    result.setMessage("当前附件不支持预览,原因："+documentPO.getReason());
                    return result;
//                    throw new FileServerException((FileServerErrorEnum.FILE_OVERVIEW_NO_SUPPORT));
                }
            }else {
                Map<String, String> map = new HashMap<>();
                result.setCode(200);
                map.put("status", "retry");
                result.setData(map);
                result.setMessage("当前附件还未进行转换，请重试");
                return result;
            }
            String convertPath = documentPO.getConvertPath();
            documentService.saveOverViewRecord(id, IPUtils.getClinetIpByReq(request), userId, staffId);
            log.info("====overview 鉴权  filepath:" + convertPath);
            try {
                String hostAddress = InetAddress.getLocalHost().getHostAddress();
                String endpoint = "http://" + hostAddress + ":" + "8080";

                String codeConvertPath = URLEncoder.encode(documentPO.getConvertPath(), "utf-8");
                String codeUrl = URLEncoder.encode("/inter-api/file-server/v1/file/pdfStreamHandeler?id=" + id + "&filePath=" + codeConvertPath, "utf-8");
                String fileType = convertPath.substring(convertPath.lastIndexOf(".") + 1).toLowerCase();
                String previewUrl;
                if ("pdf".equals(fileType)) {
                    previewUrl = address + PDF_URL + "?file=" + codeUrl;
                } else if (/*"html".equals(fileType) ||*/ "m3u8".equals(fileType)) {
                    if (SystemUtils.getOS().equalsIgnoreCase(Constants.LINUX)) {
                        previewUrl = address + Constants.VIDEO_CONVERT_PATH + TenantUtil.getTenantId() + documentPO.getConvertPath();
                    } else {
                        previewUrl = address + "/file/" + TenantUtil.getTenantId() + documentPO.getConvertPath();
                    }
                } else {
                    previewUrl = address + "/inter-api/file-server/v1/file/pdfStreamHandeler?id=" + id + "&filePath=" + codeConvertPath;
                }
                //预览次数加1
                documentService.updatePreviewTime(documentPO);
//                String previewUrl = fileService.getPreviewUrl(tenantId, convertPath);
                Map<String, String> map = new HashMap<>();
                map.put("filePath", documentPO.getFilePath());
                map.put("convertPath", convertPath);
                map.put("previewUrl", previewUrl);
                map.put("status", "true");
                result.setData(map);
                result.setCode(200);
                result.setMessage("success");
                return result;
            } catch (Exception e) {
                log.error("附件预览发生错误:",e);
                throw new FileServerException(FileServerErrorEnum.FILE_OVERVIEW__ERROR);
            }
        } else {
            throw new FileServerException(FileServerErrorEnum.FILE_OVERVIEW_AUTH_ERROR);
        }
    }

    /**
     * 获取图片 不需要鉴权  参数 文件临时路径 返回图片流
     */
    @ApiOperation(value="图片下载接口",notes="根据附件相对路径直接下载图片 不需要鉴权")
    @ApiImplicitParams({
            @ApiImplicitParam(name="filePath",value="附件附件相对路径",required=true,paramType="query",dataType="String")
    })
    @GetMapping(value = "/overview/pic")
    public void overview1(@RequestParam(value = "filePath") String filePath, HttpServletResponse response) {
        String tenantId = TenantUtil.getTenantId();
        if (filePath != null && filePath.equals(Constants.STR_KONG)) {
            throw new FileServerException(FileServerErrorEnum.FILE_PARAM_FILEPATH_ERROR);
        }
        InputStream in = null;
        BufferedInputStream inBuffer = null;
        OutputStream out = null;
        BufferedOutputStream outBuffer = null;
        filePath = filePath.replace("\\", "/");
        if (!filePath.startsWith("/")) {
            filePath = "/" + filePath;
        }
        log.info("====overview 不鉴权  filepath:" + filePath);
        try {
            response.setContentType("image/png");
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
            log.error("download:",e);
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

    /**
     * 获取图片列表 不需要鉴权  参数 文件临时路径 返回base64加密后图片流
     * 使用场景：人员头像
     */
    @ApiOperation(value="图片列表下载接口",notes="根据附件相对路径直接下载图片列表 不需要鉴权，base64加密")
    @ApiImplicitParams({
            @ApiImplicitParam(name="filePath",value="附件附件相对路径",required=true,paramType="query",dataType="String[]")
    })
    @PostMapping(value = "/overview/base64pic")
    public Result<Map<String, String>> overviewBase64(@RequestParam(value = "filePaths[]") String[] filePaths) {
        String tenantId = TenantUtil.getTenantId();
        if(ArrayUtils.isEmpty(filePaths)){
            throw new FileServerException(FileServerErrorEnum.FILE_PARAM_FILEPATH_ERROR);
        }

        // key:filePath,value:base64图片数据
        Map<String, String> res = new HashMap<>();
        for (int i = 0; i < filePaths.length; i++){
            InputStream in = null;
            BufferedInputStream inBuffer = null;
            ByteArrayOutputStream bos = new ByteArrayOutputStream();

            String filePath = filePaths[i];
            filePath = filePath.replace("\\", "/");
            if (!filePath.startsWith("/")) {
                filePath = "/" + filePath;
            }
            log.info("====base64 图片数据 不鉴权  filepath:" + filePath);
            try {
                in = fileService.downLoad(tenantId, filePath);
                inBuffer = new BufferedInputStream(in);

                IOUtils.copy(inBuffer, bos);
                byte[] fileByte = bos.toByteArray();
                // base64
                BASE64Encoder encoder = new BASE64Encoder();
                String imageData = encoder.encode(fileByte);
                res.put(filePath, imageData);
            } catch (Exception e) {
                log.error("download:",e);
                throw new FileServerException(FileServerErrorEnum.FILE_DOWNLOAD_ERROR);
            } finally {
                try {
                    inBuffer.close();
                    in.close();
                    bos.close();
                } catch (IOException e) {
                    log.error(e.getMessage());
                }
            }
        }
        return new Result<>(res);
    }

    /**
     * 根据附件id返回可直接预览的流
     *
     * @param id
     * @param response
     */
    @ApiOperation(value = "附件预览接口返回流", notes = "根据附件id返回可直接预览的流")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "附件id", required = true, paramType = "query", dataType = "String")
    })
    @GetMapping(value = "/view")
    public String view(@RequestParam(value = "id") String id, HttpServletResponse response) throws IOException {
        if (ObjectUtils.isEmpty(id)) {
            throw new FileServerException(FileServerErrorEnum.FILE_PARAM_LINKID_ERROR);
        }
            Result<Map<String, String>> result = new Result<>();
            //根据id查询文件路径
            DocumentPO documentPO = documentService.convertPathById(id);
            if (ObjectUtils.isEmpty(documentPO.getFilePath())) {
                throw new FileServerException(FileServerErrorEnum.FILE_EMPTY_ERROR);
            }
        String filePath = documentPO.getConvertPath();
        String codeUrl = URLEncoder.encode("/inter-api/file-server/v1/file/pdfStreamHandeler?filePath=" + URLEncoder.encode(documentPO.getConvertPath(), "utf-8"), "utf-8");
        String fileType = filePath.substring(filePath.lastIndexOf(".") + 1).toLowerCase();
//        if ("pdf".equals(fileType)) {
//            response.sendRedirect(PDF_URL + "?file=" + codeUrl);
//        } else {
//            response.sendRedirect("/inter-api/file-server/v1/file/pdfStreamHandeler?filePath=" + documentPO.getConvertPath());
//        }
        return "redirect:" + PDF_URL + "?file=" + codeUrl;
    }




    /***
     * url转码获取文件流
     * @param request
     * @param response
     * @param filePath
     */
    @RequestMapping(value = "/pdfStreamHandeler", method = RequestMethod.GET)
    public void pdfStreamHandeler(HttpServletRequest request, HttpServletResponse response, String id,String filePath) {
        response.reset();
        String fileType = filePath.substring(filePath.lastIndexOf(".") + 1).toLowerCase();
        if ("pdf".equals(fileType)) {
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment;");
        } if ("jpg".equals(fileType) || "jpeg".equals(fileType) || "jfif".equals(fileType)) {
            response.setContentType(imageContentType + "jpeg");
        } else if ("gif".equals(fileType) || "png".equals(fileType) || "tiff".equals(fileType)) {
            response.setContentType(imageContentType + fileType);
        } else if ("tif".equals(fileType)) {
            response.setContentType(imageContentType + "tiff");
        } else if ("bmp".equals(fileType)) {
            response.setContentType(imageContentType + "bmp");
        } else if ("wmf".equals(fileType)) {
            response.setContentType(applicationContentType + "x-wmf");
        } else if ("pdf".equals(fileType)) {
            response.setContentType(applicationContentType + "pdf");
        } else if ("html".equals(fileType)) {
            response.setContentType("text/html");
        }

        response.setCharacterEncoding("utf-8");

        byte[] buff = new byte[1024];
        BufferedInputStream bis = null;
        OutputStream os = null;
        try {
            os = response.getOutputStream();
            //获得PDF文件流
//            InputStream is = this.getClass().getResourceAsStream(urlPath);
//            DocumentPO documentPO = fileDaoService.getById(id);
            InputStream is = fileService.downLoad(TenantUtil.getTenantId(), filePath);
            log.info("获取流结束。。。。");
            bis = new BufferedInputStream(is);
            int i = 0;
            while ((i = bis.read(buff)) != -1) {
                os.write(buff, 0, i);
                os.flush();
            }
        } catch (Exception e) {
            log.error("pdf处理出现异常：" + e.getMessage() + "; ");
        } finally {
            try {
                bis.close();
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    /**
     * 预览图片 需要鉴权
     */
    @ApiOperation(value="图片预览接口",notes="根据附件id和实体编码鉴权预览图片")
    @ApiImplicitParams({
            @ApiImplicitParam(name="methodType",value="鉴权url 的请求方式",required=true,paramType="query",dataType="String"),
            @ApiImplicitParam(name="serverName",value="鉴权服务 服务名",required=true,paramType="query",dataType="String"),
            @ApiImplicitParam(name="url",value="鉴权URL",required=true,paramType="query",dataType="String"),
            @ApiImplicitParam(name="entityCode",value="实体编码",required=true,paramType="query",dataType="String"),
            @ApiImplicitParam(name="id",value="附件id",required=true,paramType="query",dataType="String")
    })
    @GetMapping(value = "/auth/overview/image")
    public Result<Map<String, String>> overviewImage(HttpServletRequest request,
                                                    @RequestParam(value = "methodType") String methodType,
                                                    @RequestParam(value = "serverName") String serverName,
                                                    @RequestParam(value = "url") String url,
                                                    @RequestParam(value = "entityCode") String entityCode,
                                                    @RequestParam(value = "id") String id,
                                                    @RequestParam(value = "address") String address,
                                                    HttpServletResponse response) throws IOException {
        Result<Map<String, String>> mapResult = this.overviewFile(request, methodType, serverName, url, entityCode, id, address, response);
        if ("true".equals(mapResult.getData().get("status"))) {
            String previewUrl = mapResult.getData().get("previewUrl");
            response.sendRedirect(previewUrl);
        }

        return mapResult;
    }
    /**
     * 预览图片 需要鉴权
     */
    @ApiOperation(value="图片预览接口",notes="根据附件id和实体编码鉴权预览图片")
    @ApiImplicitParams({
            @ApiImplicitParam(name="methodType",value="鉴权url 的请求方式",required=true,paramType="query",dataType="String"),
            @ApiImplicitParam(name="serverName",value="鉴权服务 服务名",required=true,paramType="query",dataType="String"),
            @ApiImplicitParam(name="url",value="鉴权URL",required=true,paramType="query",dataType="String"),
            @ApiImplicitParam(name="entityCode",value="实体编码",required=true,paramType="query",dataType="String"),
            @ApiImplicitParam(name="id",value="附件id",required=true,paramType="query",dataType="String")
    })
    @GetMapping(value = "/auth/overview/imageRelativePath")
    public Result<Map<String, String>> overviewImageRelativePath(HttpServletRequest request,
                                                     @RequestParam(value = "methodType") String methodType,
                                                     @RequestParam(value = "serverName") String serverName,
                                                     @RequestParam(value = "url") String url,
                                                     @RequestParam(value = "entityCode") String entityCode,
                                                     @RequestParam(value = "id") String id,
                                                     HttpServletResponse response) throws IOException {
        Result<Map<String, String>> mapResult = this.overviewFile(request, methodType, serverName, url, entityCode, id, null, response);
        if ("true".equals(mapResult.getData().get("status"))) {
            String previewUrl = mapResult.getData().get("previewUrl");
            String finalUrl = previewUrl.substring(previewUrl.indexOf("/inter-api/file-server/v1/file"));
            //设置状态码,设置为重定向方式
            response.setStatus(302);
            //添加响应头
            response.addHeader("Authorization",  request.getHeader("Authorization"));
            response.addDateHeader("time", (new Date()).getTime());
            //设置重定向
//            response.setHeader("refresh", "3;"+previewUrl);
            response.sendRedirect(finalUrl);
        }

        return mapResult;
    }

}