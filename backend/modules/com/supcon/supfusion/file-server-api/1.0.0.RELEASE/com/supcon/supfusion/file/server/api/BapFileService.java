package com.supcon.supfusion.file.server.api;

import com.supcon.supfusion.file.server.api.config.FeignMultipartSupportConfig;
import com.supcon.supfusion.file.server.api.dto.DocumentQueryDTO;
import com.supcon.supfusion.file.server.api.dto.DocumentSaveDTO;
import com.supcon.supfusion.file.server.api.dto.DocumentUpdateDTO;
import com.supcon.supfusion.file.server.api.vo.DocumentVO;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.List;

@Api(tags = "附件service API相关接口")
@FeignClient(name = "file-server", configuration = FeignMultipartSupportConfig.class)
public interface BapFileService {

    String API_PREFIX = HttpConstants.URL_SERVICEAPI + HttpConstants.URL_SPLITER + "file-server/v1";

    /**
     * 前端上传临时目录  后端调用 移动接口保存到正式目录 临时目录文件仍然存在
     * 4.附件接口
     */
    @ApiOperation(value="保存单个附件接口",notes="将临时目录的附件移动到正式目录之后 同业务建立关联关系存库 临时目录文件不删除")
    @ApiImplicitParams({
            @ApiImplicitParam(name="LinkIds",value="linkId 数组",required=true,paramType="query",dataType="Long[]"),
            @ApiImplicitParam(name="filePaths",value="对应的文件相对路径数组",required=true,paramType="query",dataType="String[]"),
            @ApiImplicitParam(name="fileTypes",value="文件类型数组 如果为null 数组内请传\"null\" 字符串",required=true,paramType="query",dataType="String[]")
    })
    @PostMapping(value = API_PREFIX + "/move/files")
    @ResponseBody
    void moveFiles(@RequestParam("LinkIds") Long[] LinkIds,
                   @RequestParam(value = "filePaths") String[] filePaths,
                   @RequestParam(value = "fileTypes") String[] fileTypes,
                   @RequestParam(value = "mainModelId", required = false) Long[] mainModelId,
                   @RequestParam(value = "sizeDis", required = false) String[] sizeDis,
                   @RequestParam(value = "memo", required = false) String[] memo,
                   @RequestParam(value = "propertyCode", required = false) String[] propertyCode,
                   @RequestParam(value = "showType", required = false) String[] showType,
                   @RequestParam(value = "opener", required = false) String[] opener,
                   @RequestParam(value = "openTime", required = false) Date[] openTime,
                   @RequestParam(value = "deploymentId", required = false) Long[] deploymentId,
                   @RequestParam(value = "activityName", required = false) String[] activityName,
                   @RequestParam(value = "taskDescription", required = false) String[] taskDescription,
                   @RequestParam(value = "fileIcon", required = false) String[] fileIcon,
                   @RequestParam(value = "isFileView", required = false) Boolean[] isFileView,
                   @RequestParam(value = "docContent", required = false) String[] docContent,
                   @RequestParam(value = "docSummary", required = false) String[] docSummary,
                   @RequestParam(value = "convertStatus", required = false) String[] convertStatus,
                   @RequestParam(value = "reason", required = false) String[] reason,
                   @RequestParam(value = "convertPath", required = false) String[] convertPath,
                   @RequestParam(value = "name", required = false) String[] name,
                   @RequestParam(value = "type", required = false) String[] type,
                   @RequestParam(value = "size", required = false) String[] size,
                   @RequestParam(value = "downloadTimes", required = false) Long[] downloadTimes,
                   @RequestParam(value = "previewTimes", required = false) Long[] previewTimes
    );

    /**
     * 前端上传临时目录  后端调用 移动接口保存到正式目录 临时目录文件仍然存在
     * 4.附件接口
     */
    @ApiOperation(value = "保存单个附件Json接口", notes = "将临时目录的附件移动到正式目录之后 同业务建立关联关系存库 临时目录文件不删除")
    @PostMapping(value = API_PREFIX + "/move/filesJson")
    @ResponseBody
    void moveFilesJson(@RequestBody DocumentSaveDTO documentSaveDTO);

    /**
     *  后端调用复制附件接口
     */
    @ApiOperation(value="复制附件接口",notes="传入附件id 复制附件同其他的业务id建立关联关系入库")
    @PostMapping(value = API_PREFIX + "/copy/files")
    @ResponseBody
    void copyFile(@RequestParam("linkId") Long linkId,
                   @RequestParam(value = "fileId") Long fileId,
                   @RequestParam(value = "type",required = false) String type,
                   @RequestParam(value = "fileType",required = false) String fileType
    );

    /**
     * 根据旧路径和新路径复制附件
     *
     * @param oldPath
     * @param newPath
     */
    @ApiOperation(value="复制附件接口",notes="从一个路径复制附件到指定路径")
    @ApiImplicitParams({
            @ApiImplicitParam(name="oldPath",value="附件旧路径",required=true,paramType="query",dataType="String"),
            @ApiImplicitParam(name="newPath",value="附件新的存储路径",required=true,paramType="query",dataType="String")
    })
    @PutMapping(value = API_PREFIX + "/copy/file/bypath")
    void copyFileByPath(@RequestParam(value = "oldPath") String oldPath, @RequestParam("newPath") String newPath);

    /**
     *  更新单个 附件相关信息接口
     */
    @ApiOperation(value="更新单个附件信息接口",notes="传入附件documentUpdateDTO 修改附件关联信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name="linkId",value="linkId",required=true,paramType="query",dataType="Long")
    })
    @PutMapping(value = API_PREFIX + "/update/one/file")
    @ResponseBody
    void updateOneFileMessage(@RequestBody DocumentUpdateDTO documentUpdateDTO);


    /**
     * 1.查询附件列表
     *
     * @param linkId
     * @return
     */
    @ApiOperation(value="查询业务关联附件接口",notes="根据linkID查询关联的附件 单个业务id")
    @ApiImplicitParams({
            @ApiImplicitParam(name="linkId",value="linkId",required=true,paramType="query",dataType="Long"),
            @ApiImplicitParam(name="type",value="文档类型 类型",required=false,paramType="query",dataType="String"),
            @ApiImplicitParam(name="fileType",value="附件文件类型",required=false,paramType="query",dataType="String"),
            @ApiImplicitParam(name="propertyCode",value="字段CODE",required=false,paramType="query",dataType="String")
    })
    @GetMapping(value = API_PREFIX + "/select")
    @ResponseBody
    List<DocumentVO> selectBusinessIdAllFile(@RequestParam(value = "linkId") Long linkId,
                                             @RequestParam(value = "type", required = false) String type,
                                             @RequestParam(value = "fileType", required = false) String fileType,
                                             @RequestParam(value = "propertyCode", required = false) String propertyCode);


    /**
     * 1.查询附件列表
     *
     * @param linkIds
     * @return
     */
    @ApiOperation(value="查询业务关联附件接口",notes="根据linkID查询关联的附件 多个业务id")
    @ApiImplicitParams({
            @ApiImplicitParam(name="linkIds",value="linkId集合",required=true,paramType="query",dataType="List<Long>"),
            @ApiImplicitParam(name="type",value="文档类型 类型",required=false,paramType="query",dataType="String"),
            @ApiImplicitParam(name="fileType",value="附件文件类型",required=false,paramType="query",dataType="String"),
            @ApiImplicitParam(name="propertyCode",value="字段CODE",required=false,paramType="query",dataType="String")
    })
    @GetMapping(value = API_PREFIX + "/select/files")
    @ResponseBody
    List<DocumentVO> selectBusinessIdAllFiles(@RequestParam(value = "linkIds") List<Long> linkIds,
                                              @RequestParam(value = "type", required = false) String type,
                                              @RequestParam(value = "fileType", required = false) String fileType,
                                              @RequestParam(value = "propertyCode", required = false) String propertyCode);

    /**
     * 查询附件列表，入参为json
     *
     * @return
     */
    @ApiOperation(value="查询业务关联附件接口,入参为json",notes="根据linkID查询关联的附件 多个业务id")
    @GetMapping(value = API_PREFIX + "/select/filesJson")
    @ResponseBody
    List<DocumentVO> selectBusinessIdAllFilesJson(@RequestBody DocumentQueryDTO documentQueryDTO);

    /**
     * 删除单个id 所有附件接口
     */
    @ApiOperation(value="删除附件",notes="单个id 删除附件")
    @ApiImplicitParams({
            @ApiImplicitParam(name="id",value="附件id",required=true,paramType="query",dataType="Long")
    })
    @DeleteMapping(value = API_PREFIX + "/delete")
    void deleteOneFile(@RequestParam("id") Long id);

    /**
     * 删除单个id 所有附件接口
     */
    @ApiOperation(value="删除附件",notes="批量 删除附件")
    @ApiImplicitParams({
            @ApiImplicitParam(name="ids",value="附件id集合",required=true,paramType="query",dataType="List<Long>")
    })
    @DeleteMapping(value = API_PREFIX + "/delete/ids")
    void deleteFilesByIdArr(@RequestParam("ids") List<Long> ids);


    /**
     * 移动附件
     *
     * @param oldPath
     * @param newPath
     */
    @ApiOperation(value="移动附件",notes="从一个路径移动附件到指定路径")
    @ApiImplicitParams({
            @ApiImplicitParam(name="oldPath",value="附件旧路径",required=true,paramType="query",dataType="String"),
            @ApiImplicitParam(name="newPath",value="附件新的存储路径",required=true,paramType="query",dataType="String")
    })
    @PutMapping(value = API_PREFIX + "/move")
    void move(@RequestParam(value = "oldPath") String oldPath, @RequestParam("newPath") String newPath);


    /**
     * 1.查询单个
     *
     * @param id
     * @return
     */
    @ApiOperation(value="查询附件",notes="根据附件id查询附件")
    @ApiImplicitParams({
            @ApiImplicitParam(name="id",value="附件id",required=true,paramType="query",dataType="Long")
    })
    @GetMapping(value = API_PREFIX + "/select/one/file")
    @ResponseBody
    DocumentVO selectFileByFileId(@RequestParam(value = "id") Long id);


    @ApiOperation(value="下载附件",notes="根据附件相对路径下载附件")
    @ApiImplicitParams({
            @ApiImplicitParam(name="filePath",value="附件相对路径",required=true,paramType="query",dataType="String")
    })
    @RequestMapping(value = API_PREFIX + "/downloadFile", method = {RequestMethod.GET})
    @ResponseBody
    ResponseEntity<byte[]> downloadFile(@RequestParam(value = "filePath") String filePath);


    /**
     *  附件 上传
     *
     * @param uploadFile
     * @return
     */
    @ApiOperation(value="附件上传",notes="MultipartFile 对象上传附件")
    @ApiImplicitParams({
            @ApiImplicitParam(name="file",value="MultipartFile对象",required=true,paramType="query",dataType="MultipartFile")
    })
    @PostMapping(value = API_PREFIX + "/upload/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    Result fileUpload(@RequestPart("file") MultipartFile uploadFile);
}