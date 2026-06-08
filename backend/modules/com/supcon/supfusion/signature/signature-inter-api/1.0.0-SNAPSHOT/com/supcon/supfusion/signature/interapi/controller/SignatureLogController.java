package com.supcon.supfusion.signature.interapi.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Pagination;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.common.util.DateUtil;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import com.supcon.supfusion.signature.base.constant.SinatureConstants;
import com.supcon.supfusion.signature.base.enums.SignatureColumn;
import com.supcon.supfusion.signature.base.enums.SignatureErrorEnum;
import com.supcon.supfusion.signature.base.exception.SignatureException;
import com.supcon.supfusion.signature.base.i18n.SignatureInternationalResource;
import com.supcon.supfusion.signature.base.untils.DateUntils;
import com.supcon.supfusion.signature.base.untils.SignatureType;
import com.supcon.supfusion.signature.dao.entity.SignatureExcel;
import com.supcon.supfusion.signature.dao.entity.SignatureLog;
import com.supcon.supfusion.signature.interapi.vo.SignatureLogConditionQueryVo;
import com.supcon.supfusion.signature.services.bo.LogQueryCondition;
import com.supcon.supfusion.signature.services.service.SignatureExcelService;
import com.supcon.supfusion.signature.services.service.SignatureLogService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.util.*;

/**
 * @author user
 */
@Slf4j
@RestController
@Api(tags = {"电子签名日志管理API"})
@RequestMapping("/inter-api/signature")
public class SignatureLogController extends BaseController {


    @Autowired
    private SignatureLogService signatureLogService;

    @Autowired
    private SignatureExcelService signatureExcelService;

    @Autowired
    private SignatureInternationalResource signatureInternationalResource;

    /**
     * 根据查询条件获取签名日志
     *
     * @param signatureLogConditionQueryVo
     * @return
     */
    @ApiOperation("根据条件分页获取签名日志")
    @PostMapping(value = "/signatureLogListQuery")
    @ResponseBody
    public PageResult<SignatureLog> signatureLogListQuery(@RequestBody SignatureLogConditionQueryVo signatureLogConditionQueryVo) {
        LogQueryCondition signatureColumnCondition = getSignatureColumnCondition(signatureLogConditionQueryVo);
        int pageSize = signatureLogConditionQueryVo.getPageSize();
        int current = signatureLogConditionQueryVo.getCurrent();
        Pagination pagination = new Pagination();
        if (pageSize > 0) {
            pagination.setCurrent(current);
        }
        if (current > 0) {
            pagination.setPageSize(pageSize);
        }
        List<SignatureLog> result = signatureLogService.getSignaureLogs(signatureColumnCondition, pagination);
        if (result != null) {
            result.forEach(signatureLog -> {
                signatureLog.setSignatureType(SignatureType.getType(signatureLog.getSignatureType()));
            });
        }
        PageResult<SignatureLog> signatureLogPageResult = new PageResult<>();
        signatureLogPageResult.setList(result);
        signatureLogPageResult.setPagination(pagination);
        return signatureLogPageResult;
    }

    /**
     * 根据uuid获取签名日志
     *
     * @param page
     * @param uuid
     * @param request
     * @return
     * @throws IOException
     */
    @ApiOperation("根据日志id获取日志详细内容")
    @PostMapping(value = "/signatureLogQuery/openTableInfo")
    @ResponseBody
    public Map<String, Object> openTableInfo(@RequestBody Page<SignatureLog> page, String uuid,
                                             HttpServletRequest request) throws IOException {

        if (StringUtils.isBlank(uuid)) {
            throw new SignatureException(SignatureErrorEnum.BE_EMPTY_ERROR, "uuid");
        }

        SignatureLog signaureLogs = signatureLogService.getSignaureLogsByIds(uuid);
        Map<String, Object> responseMap = new HashMap<String, Object>();
        responseMap.put("signatureLog", signaureLogs);
        return responseMap;
    }

    @ApiOperation("根据过滤条件导出数据")
    @PostMapping(value = "/log/export")
    @ResponseBody
    public Result signatureLogCreateExportTask(@RequestBody SignatureLogConditionQueryVo signatureLogVo) {
        List<String> ids = signatureLogVo.getIds();
        //创建导出任务
        SignatureExcel signatureExcel = signatureExcelService.createExportTask();
        if (null == signatureLogVo) {
            signatureLogVo = new SignatureLogConditionQueryVo();
        }
        LogQueryCondition signatureColumnCondition = getSignatureColumnCondition(signatureLogVo);

        Pagination pagination = new Pagination();
        Boolean isAll = signatureLogVo.getIsAll();

        pagination.setPageSize(5000);


        signatureExcelService.exportExcel(signatureExcel, pagination, signatureColumnCondition, signatureExcel.getFileName(), isAll, ids);

        Result result = new Result();
        result.setData(signatureExcel.getId());
        return result;
    }

    @ApiOperation("查询导出状态")
    @GetMapping(value = "/log/export/status")
    @ResponseBody
    public Result<SignatureExcel> querySignatureExcelStatusById(@RequestParam("id") Long id) {
        SignatureExcel signatureExcel = signatureExcelService.queryStatus(id);
        return Result.data(signatureExcel);
    }


    /**
     * 导出电子签名日志
     *
     * @param id
     * @return
     */
    @ApiOperation("下载excel")
    @GetMapping(value = "/log/export")
    @ResponseBody
    public Result signatureLogListQuery(HttpServletResponse httpResponse, @RequestParam("id") Long id) {
        if (id == null) {
            return Result.fail("id不能为空");
        }
        SignatureExcel signatureExcel = signatureExcelService.queryStatus(id);
        String fileName = signatureExcel.getFileName();
        String excleFilePath = SinatureConstants.EXCEL_PATH + fileName;
        httpResponse.setHeader("content-type", "application/octet-stream");
        httpResponse.setContentType("application/octet-stream");
        // 下载文件能正常显示中文
        try {
            httpResponse.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode("电子签名日志.xlsx", "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            log.error(e.getMessage());
        }

        // 实现文件下载
        byte[] buffer = new byte[1024];
        try (
                FileInputStream fis = new FileInputStream(excleFilePath);
                BufferedInputStream bis = new BufferedInputStream(fis);
        ) {

            OutputStream os = httpResponse.getOutputStream();
            int i = bis.read(buffer);
            while (i != -1) {
                os.write(buffer, 0, i);
                i = bis.read(buffer);
            }
            if (StringUtils.isNotBlank(excleFilePath)) {
                File exportFile = new File(excleFilePath);
                exportFile.delete();
            }
        } catch (Exception e) {
            log.error("Download the song failed!");
            throw new RuntimeException("Download the song failed!");
        }
        return Result.success();
    }

    private LogQueryCondition getSignatureColumnCondition(SignatureLogConditionQueryVo signatureLogVo) {
        LogQueryCondition logQueryCondition = new LogQueryCondition();

        if (signatureLogVo == null || signatureLogVo.getIsAll()) {
            return logQueryCondition;
        }
        //模糊查询
        Map<SignatureColumn, List<String>> likeCondition = new HashMap();

        setCondition(likeCondition, SignatureColumn.MODULE_NAME, signatureLogVo.getModuleName());
        setCondition(likeCondition, SignatureColumn.ENTITY_NAME, signatureLogVo.getEntityName());
        setCondition(likeCondition, SignatureColumn.MODEL_NAME, signatureLogVo.getModelName());
        setCondition(likeCondition, SignatureColumn.BUSINESS_KEY, signatureLogVo.getBusinessKey());
        setCondition(likeCondition, SignatureColumn.FIRST_REASON, signatureLogVo.getFirstReason());
        setCondition(likeCondition, SignatureColumn.SECOND_REASON, signatureLogVo.getSecondReason());
        setCondition(likeCondition, SignatureColumn.SECOND_USER_NAME, signatureLogVo.getSecondUserName());
        setCondition(likeCondition, SignatureColumn.FIRST_USER_NAME, signatureLogVo.getFirstUserName());
        setCondition(likeCondition, SignatureColumn.SECOND_STAFF_NAME, signatureLogVo.getSecondStaffName());
        setCondition(likeCondition, SignatureColumn.FIRST_STAFF_NAME, signatureLogVo.getFirstStaffName());
        if (likeCondition.size() > 0) {
            logQueryCondition.setLikeCondition(likeCondition);
        }

        //多选查询
        Map<SignatureColumn, List<String>> inCondition = new HashMap();

        setCondition(inCondition, SignatureColumn.FIRST_STAFF_ID, signatureLogVo.getFirstStaffId());
        setCondition(inCondition, SignatureColumn.SECOND_STAFF_ID, signatureLogVo.getSecondStaffId());
        setCondition(inCondition, SignatureColumn.SIGNATURE_TYPE, signatureLogVo.getSignatureType());
        setCondition(inCondition, SignatureColumn.MODULE_CODE, signatureLogVo.getModuleCode());
        setCondition(inCondition, SignatureColumn.ENTITY_CODE, signatureLogVo.getEntityCode());
        setCondition(inCondition, SignatureColumn.MODEL_CODE, signatureLogVo.getModelCode());
        setCondition(inCondition, SignatureColumn.BUTTON_CODE, signatureLogVo.getButtonCode());
        if (inCondition.size() > 0) {
            logQueryCondition.setInCondition(inCondition);
        }

        //时间区间
        Map<SignatureColumn, List<String>> timeCondition = new HashMap();

        setCondition(timeCondition, SignatureColumn.FIRST_SIGN_TIME, getStartAndEndTime(signatureLogVo.getFirstSignTimeStr()));
        setCondition(timeCondition, SignatureColumn.SECOND_SIGN_TIME, getStartAndEndTime(signatureLogVo.getSecondSignTimeStr()));
        if (timeCondition.size() > 0) {
            logQueryCondition.setTimeCondition(timeCondition);
        }
        return logQueryCondition;
    }

    private List<String> getStartAndEndTime(List<String> times) {
        if (times == null || times.size() <= 0) {
            return null;
        }
        //时间格式转换,解决24:00:00报错的bug
        ArrayList<String> strings = new ArrayList<>();
        for (String time : times) {
            if (StringUtils.isBlank(time)) {
                time = null;
            } else {
                //校验格式是否合法
                if (!(time.matches(DateUntils.DATA_TIME_FORMAT_REG) || time.matches(DateUntils.DATA_FORMAT_REG))) {
                    //校验时间是不是24:00:00
                    if (time.matches(DateUntils.DATA_TIME_FORMAT_24_REG)) {
                        time = DateUntils.getDateSrt(time, Calendar.DATE, 0);
                    }
                }
            }
            //时间格式校验
            strings.add(time);
        }
        if (strings.size() > 1) {
            return strings;
        }

        String s = strings.get(0);
        if (StringUtils.isNotBlank(s)) {
            if (DateUntils.formatCheck(s)) {
                return strings;
            }
            int i = Integer.parseInt(s);
            String previousTime = DateUntils.getPreviousTime(i);
            ArrayList<String> newTimes = new ArrayList<>();
            newTimes.add(previousTime);
            newTimes.add(DateUtil.format(new Date(), DateUtil.PATTERN_DATETIME));
            return newTimes;
        }
        return null;
    }

    private void setCondition(Map<SignatureColumn, List<String>> params, SignatureColumn key, List<String> value) {
        if (value != null && value.size() > 0) {
            params.put(key, value);
        }
    }


}
