package com.supcon.supfusion.notification.admin.webapi.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.microsoft.sqlserver.jdbc.SQLServerException;
import com.supcon.supfusion.framework.cloud.annotation.InternalApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeTask;
import com.supcon.supfusion.notification.admin.service.NoticeTaskService;
import com.supcon.supfusion.notification.admin.webapi.utils.NoticeTaskWapper;
import com.supcon.supfusion.notification.admin.webapi.vo.NoticeTaskVO;
import com.supcon.supfusion.notification.sharding.context.ShardingContext;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.MyBatisSystemException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.sql.SQLSyntaxErrorException;
import java.util.ArrayList;
import java.util.List;

@ResponseBody
@Api(description = "NoticeTask-API", tags = {"任务API"})
@InternalApi(path = HttpConstants.URL_INTERNALAPI + HttpConstants.URL_SPLITER + "notification-admin" + HttpConstants.URL_SPLITER + "v1")
@Slf4j
public class NotcieTaskController {
    @Resource(name = "adminNoticeTaskServiceImpl")
    private NoticeTaskService taskService;
    @Autowired
    private NoticeTaskWapper taskWapper;

    @ApiOperation(value = "分页查询任务记录")
    @GetMapping(value = "/notice/task/tasks")
    public PageResult<NoticeTaskVO> list(@ApiParam(value = "创建时间start", required = false) @RequestParam(value = "startTime", required = false) String startTime,
                                         @ApiParam(value = "创建时间end", required = false) @RequestParam(value = "endTime", required = false) String endTime,
                                         @ApiParam(value = "任务ID", required = false) @RequestParam(value = "id", required = false) String id,
                                         @ApiParam(value = "消息主题ID", required = false) @RequestParam(value = "noticeTopicId", required = false) String noticeTopicId,
                                         @ApiParam(value = "发送方编号", required = false) @RequestParam(value = "bsmodCode", required = false) String bsmodCode,
                                         @ApiParam(value = "服务名称", required = false) @RequestParam(value = "bsmodName", required = false) String bsmodName,
                                         @ApiParam(value = "页码", required = false) @RequestParam(value = "pageNo", required = false) Integer pageNo,
                                         @ApiParam(value = "分页大小", required = false) @RequestParam(value = "pageSize", required = false) Integer pageSize) throws Exception {
        if (pageNo == null || pageSize == null || pageSize < 1 || pageNo < 1) {
            pageNo = 1;
            pageSize = 20;
        }
        try {
            String[] timeRange = normalizeTimeRange(startTime, endTime);
            startTime = timeRange[0];
            endTime = timeRange[1];
            Page page = new Page<>(pageNo, pageSize);
            ShardingContext.getContext().setShardingTime(resolveShardingTime(startTime, endTime));
            Page<NoticeTask> entityPage = taskService.queryTaskPage(startTime, endTime, id, noticeTopicId == null ? null : Long.valueOf(noticeTopicId), bsmodCode, bsmodName, page);
            Page<NoticeTaskVO> wapper = taskWapper.pageCP(entityPage);
            return new PageResult(wapper.getRecords(), entityPage.getTotal(), entityPage.getSize(), entityPage.getCurrent());
        } catch (MyBatisSystemException e) {
            if (isMissingShardTable(e)) {
                log.error(e.getMessage(), e);
                return new PageResult(new ArrayList(), 0, pageSize, pageNo);
            }
            throw e;
        }
    }

    @ApiOperation(value = "关键字查询")
    @GetMapping(value = "/notice/task/keyword")
    public ListResult<NoticeTaskVO> mnemonicList(@ApiParam(value = "创建时间start", required = false) @RequestParam(value = "startTime", required = false) String startTime,
                                                 @ApiParam(value = "创建时间end", required = false) @RequestParam(value = "endTime", required = false) String endTime,
                                                 @ApiParam(value = "任务ID", required = false) @RequestParam(value = "id", required = false) String id,
                                                 @ApiParam(value = "消息主题ID", required = false) @RequestParam(value = "noticeTopicId", required = false) String noticeTopicId,
                                                 @ApiParam(value = "发送方编号", required = false) @RequestParam(value = "bsmodCode", required = false) String bsmodCode,
                                                 @ApiParam(value = "服务名称", required = false) @RequestParam(value = "bsmodName", required = false) String bsmodName) throws Exception {
        String[] timeRange = normalizeTimeRange(startTime, endTime);
        startTime = timeRange[0];
        endTime = timeRange[1];
        ShardingContext.getContext().setShardingTime(resolveShardingTime(startTime, endTime));
        List<NoticeTask> result = taskService.queryListByKeyword(startTime, endTime, id, noticeTopicId == null ? null : Long.valueOf(noticeTopicId), bsmodCode, bsmodName);
        List<NoticeTaskVO> wapper = taskWapper.listCP(result);
        return new ListResult(wapper);
    }

    private String[] normalizeTimeRange(String startTime, String endTime) {
        if (!isBlank(startTime) && !isBlank(endTime)) {
            try {
                Long.valueOf(startTime);
                Long.valueOf(endTime);
                return new String[]{startTime, endTime};
            } catch (NumberFormatException ex) {
                log.warn("notice task query time is invalid, startTime={}, endTime={}", startTime, endTime);
            }
        }

        long now = System.currentTimeMillis();
        long oneDay = 24L * 60L * 60L * 1000L;
        return new String[]{String.valueOf(now - 30L * oneDay), String.valueOf(now + oneDay)};
    }

    private Long resolveShardingTime(String startTime, String endTime) {
        try {
            if (isBlank(startTime) || isBlank(endTime)) {
                return System.currentTimeMillis();
            }
            return (Long.valueOf(startTime) + Long.valueOf(endTime)) / 2;
        } catch (NumberFormatException ex) {
            log.warn("notice task query time is invalid, startTime={}, endTime={}", startTime, endTime);
            return System.currentTimeMillis();
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean isMissingShardTable(MyBatisSystemException e) {
        if (e.getCause() == null || e.getCause().getCause() == null || e.getCause().getCause().getCause() == null) {
            return false;
        }
        Throwable throwable = e.getCause().getCause().getCause();
        if (throwable instanceof SQLSyntaxErrorException) {
            SQLSyntaxErrorException sqlSyntaxErrorException = (SQLSyntaxErrorException) throwable;
            return "42S02".equals(sqlSyntaxErrorException.getSQLState()) || "42000".equals(sqlSyntaxErrorException.getSQLState());
        }
        if (throwable instanceof SQLServerException) {
            SQLServerException sqlServerException = (SQLServerException) throwable;
            return "S0002".equals(sqlServerException.getSQLState());
        }
        return false;
    }
}
