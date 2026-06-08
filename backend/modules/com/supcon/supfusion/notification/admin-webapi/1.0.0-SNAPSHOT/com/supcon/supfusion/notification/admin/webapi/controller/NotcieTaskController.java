package com.supcon.supfusion.notification.admin.webapi.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.microsoft.sqlserver.jdbc.SQLServerException;
import com.supcon.supfusion.framework.cloud.annotation.InternalApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeProtocol;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeTask;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeTopicTree;
import com.supcon.supfusion.notification.admin.service.NoticeTaskService;
import com.supcon.supfusion.notification.admin.webapi.utils.NoticeTaskWapper;
import com.supcon.supfusion.notification.admin.webapi.vo.NoticeTaskVO;
import com.supcon.supfusion.notification.admin.webapi.vo.NoticeTopicTreeVO;
import com.supcon.supfusion.notification.sharding.context.ShardingContext;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import java.sql.SQLSyntaxErrorException;
import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.MyBatisSystemException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;

/**
 * ${description}
 *
 * @author huangxin2
 * @create 2020/5/7 20:03
 */
@ResponseBody
@Api(description = "NoticeTask-API", tags = {"任务API"})
@InternalApi(path = HttpConstants.URL_INTERNALAPI + HttpConstants.URL_SPLITER + "notification-admin" + HttpConstants.URL_SPLITER + "v1")
@Slf4j
public class NotcieTaskController {
    @Resource(name = "adminNoticeTaskServiceImpl")
    private NoticeTaskService taskService;
    @Autowired
    private NoticeTaskWapper taskWapper;

    /**
     * 分页查询对象
     *
     * @param startTime
     * @param endTime
     * @param noticeTopicId
     * @param bsmodCode
     * @param bsmodName
     * @param pageNo
     * @param pageSize
     * @return
     * @throws Exception
     */

    @ApiOperation(value = "分页查询任务记录")
    @GetMapping(value = "/notice/task/tasks")
    public PageResult<NoticeTaskVO> list(@ApiParam(value = "创建时间start", required = false) @RequestParam(required = false) String startTime,
                                         @ApiParam(value = "创建时间end", required = false) @RequestParam(required = false) String endTime,
                                         @ApiParam(value = "任务ID", required = false) @RequestParam(required = false) String id,
                                         @ApiParam(value = "消息主题ID", required = false) @RequestParam(required = false) String noticeTopicId,
                                         @ApiParam(value = "发送方编号", required = false) @RequestParam(required = false) String bsmodCode,
                                         @ApiParam(value = "服务名称", required = false) @RequestParam(required = false) String bsmodName,
                                         @ApiParam(value = "页码", required = false) @RequestParam(required = false) Integer pageNo,
                                         @ApiParam(value = "分页大小", required = false) @RequestParam(required = false) Integer pageSize) throws Exception {
        if (pageNo == null || pageSize == null || pageSize < 1 || pageNo < 1) {
            pageNo = 0;
            pageSize = 20;
        }
        try {
            Page page = new Page<>(pageNo, pageSize);
            ShardingContext.getContext().setShardingTime((Long.valueOf(startTime) + Long.valueOf(endTime)) / 2);
            Page<NoticeTask> entityPage = taskService.queryTaskPage(startTime, endTime, id, noticeTopicId == null ? null : Long.valueOf(noticeTopicId), bsmodCode, bsmodName, page);
            Page<NoticeTaskVO> wapper = taskWapper.pageCP(entityPage);
            return new PageResult(wapper.getRecords(), entityPage.getTotal(), entityPage.getSize(), entityPage.getCurrent());
        } catch (MyBatisSystemException e) {
            /**
             * 查询不存在月份的分表，返回空数据
             */
            if (e.getCause() != null
                    && e.getCause().getCause() != null
                    && e.getCause().getCause().getCause() != null) {
                Throwable throwable = e.getCause().getCause().getCause();
                if (throwable instanceof SQLSyntaxErrorException) {
                    SQLSyntaxErrorException sqlSyntaxErrorException = (SQLSyntaxErrorException) throwable;
                    if ("42S02".equals(sqlSyntaxErrorException.getSQLState()) || "42000".equals(sqlSyntaxErrorException.getSQLState())) {
                        log.error(e.getMessage(), e);
                        return new PageResult(new ArrayList(), 0, pageSize, pageNo);
                    }
                } else if (throwable instanceof SQLServerException) {
                    SQLServerException sqlServerException = (SQLServerException) throwable;
                    if ("S0002".equals(sqlServerException.getSQLState())) {
                        log.error(e.getMessage(), e);
                        return new PageResult(new ArrayList(), 0, pageSize, pageNo);
                    }
                }
            }
            throw e;
        }
    }

    /**
     * 关键字查询
     *
     * @param startTime
     * @param endTime
     * @param id
     * @param noticeTopicId
     * @param bsmodCode
     * @return
     * @throws Exception
     */
    @ApiOperation(value = "关键字查询")
    @GetMapping(value = "/notice/task/keyword")
    public ListResult<NoticeTaskVO> mnemonicList
    (@ApiParam(value = "创建时间start", required = false) @RequestParam(required = false) String startTime,
     @ApiParam(value = "创建时间end", required = false) @RequestParam(required = false) String endTime,
     @ApiParam(value = "任务ID", required = false) @RequestParam(required = false) String id,
     @ApiParam(value = "消息主题ID", required = false) @RequestParam(required = false) String noticeTopicId,
     @ApiParam(value = "发送方编号", required = false) @RequestParam(required = false) String bsmodCode,
     @ApiParam(value = "服务名称", required = false) @RequestParam(required = false) String bsmodName) throws
            Exception {
        ShardingContext.getContext().setShardingTime((Long.valueOf(startTime) + Long.valueOf(endTime)) / 2);
        List<NoticeTask> result = taskService.queryListByKeyword(startTime, endTime, id, noticeTopicId == null ? null : Long.valueOf(noticeTopicId), bsmodCode, bsmodName);
        List<NoticeTaskVO> wapper = taskWapper.listCP(result);
        return new ListResult(wapper);
    }

}
