package com.supcon.supfusion.notification.admin.webapi.controller;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.generator.config.IFileCreate;
import com.microsoft.sqlserver.jdbc.SQLServerException;
import com.supcon.supfusion.framework.cloud.annotation.InternalApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.context.UserContext;
import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.common.util.DateUtil;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeMessageUnreadCount;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeMsg;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeProtocol;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeTaskProtocol;
import com.supcon.supfusion.notification.admin.dao.mappers.NoticeProtocolMapper;
import com.supcon.supfusion.notification.admin.dao.mappers.NoticeTaskProtocolMapper;
import com.supcon.supfusion.notification.admin.service.NoticeMessageRecordService;
import com.supcon.supfusion.notification.admin.service.NoticeTaskService;
import com.supcon.supfusion.notification.admin.webapi.utils.NoticeProtocolMessageWapper;
import com.supcon.supfusion.notification.admin.webapi.vo.*;
import com.supcon.supfusion.notification.sharding.context.ShardingContext;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.http.HttpStatus;
import org.mybatis.spring.MyBatisSystemException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.sql.SQLSyntaxErrorException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * ${description}
 *
 * @author huangxin2
 * @create 2020/5/7 20:03
 */
@ResponseBody
@Api(description = "NoticeMessageRecord-API", tags = {"消息记录操作API"})
@InternalApi(path = HttpConstants.URL_INTERNALAPI + HttpConstants.URL_SPLITER + "notification-admin" + HttpConstants.URL_SPLITER + "v1")
@Slf4j
public class NotcieMessageRecordController {
    @Resource(name = "adminNoticeMessageRecordServiceImpl")
    private NoticeMessageRecordService messageRecordService;
    @Resource(name = "adminNoticeProtocolMapper")
    private NoticeProtocolMapper noticeProtocolMapper;
    @Autowired
    private NoticeProtocolMessageWapper messageWapper;
    @Resource(name = "adminNoticeTaskServiceImpl")
    private NoticeTaskService noticeTaskService;
    @Resource(name = "adminNoticeTaskProtocolMapper")
    private NoticeTaskProtocolMapper noticeTaskProtocolMapper;

    /***
     * 分页查询对象
     * @param startTime
     * @param endTime
     * @param noticeProtocolId
     * @param noticeTaskId
     * @param sendStatus
     * @param readStatus
     * @param pageNo
     * @param pageSize
     * @return
     * @throws Exception
     */
    @ApiOperation(value = "分页查询消息记录")
    @GetMapping(value = "/notice/message/record/messages")
    public PageResult<NoticeProtocolMessageVO> list(@ApiParam(value = "创建时间start", required = false) @RequestParam(required = false) String startTime,
                                                    @ApiParam(value = "创建时间end", required = false) @RequestParam(required = false) String endTime,
                                                    @ApiParam(value = "通知方式ID", required = true) @RequestParam(required = true) String noticeProtocolId,
                                                    @ApiParam(value = "接收人名称", required = false) @RequestParam(required = false) String staffName,
                                                    @ApiParam(value = "任务ID", required = false) @RequestParam(required = false) String noticeTaskId,
                                                    @ApiParam(value = "发送状态", required = false) @RequestParam(required = false) String sendStatus,
                                                    @ApiParam(value = "阅读状态", required = false) @RequestParam(required = false) String readStatus,
                                                    @ApiParam(value = "页码", required = false) @RequestParam(required = false) Integer pageNo,
                                                    @ApiParam(value = "分页大小", required = false) @RequestParam(required = false) Integer pageSize) throws Exception {
        if (pageNo == null || pageSize == null || pageSize < 1 || pageNo < 1) {
            pageNo = 0;
            pageSize = 20;
        }
        try {
            if (pageNo != null && pageSize != null && pageNo > -1 && pageSize > -1) {
                NoticeProtocol protocol = noticeProtocolMapper.selectOne(Wrappers.<NoticeProtocol>query().eq(NoticeProtocol.getIdFieldName(), noticeProtocolId));
                ShardingContext.getContext().setShardingTime((Long.valueOf(startTime) + Long.valueOf(endTime)) / 2);
                ShardingContext.getContext().setProtocol(protocol.getProtocol());

                Page page = new Page<>(pageNo, pageSize);
                Page<NoticeMsg> entityPage = messageRecordService.queryPageList(startTime == null ? null : Long.valueOf(startTime), endTime == null ? null : Long.valueOf(endTime), noticeProtocolId == null ? null : Long.valueOf(noticeProtocolId), staffName, noticeTaskId, sendStatus, readStatus, page);
                Page<NoticeProtocolMessageVO> wapper = messageWapper.pageCP(entityPage);
                return new PageResult(wapper.getRecords(), entityPage.getTotal(), entityPage.getSize(), entityPage.getCurrent());
            }
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
        return new PageResult<>();
    }

    /**
     * 关键字查询
     *
     * @param startTime
     * @param endTime
     * @param noticeTaskId
     * @param noticeProtocolId
     * @param staffName
     * @return
     * @throws Exception
     */
    @ApiOperation(value = "关键字查询")
    @GetMapping(value = "/notice/message/keyword")
    public ListResult<NoticeProtocolMessageVO> mnemonicList(@ApiParam(value = "创建时间start", required = false) @RequestParam(required = false) String startTime,
                                                            @ApiParam(value = "创建时间end", required = false) @RequestParam(required = false) String endTime,
                                                            @ApiParam(value = "通知方式ID", required = true) @RequestParam(required = true) String noticeProtocolId,
                                                            @ApiParam(value = "接收人名称", required = false) @RequestParam(required = false) String staffName,
                                                            @ApiParam(value = "任务ID", required = false) @RequestParam(required = false) String noticeTaskId) throws Exception {
        NoticeProtocol protocol = noticeProtocolMapper.selectOne(Wrappers.<NoticeProtocol>query().eq(NoticeProtocol.getIdFieldName(), noticeProtocolId));
        ShardingContext.getContext().setShardingTime((Long.valueOf(startTime) + Long.valueOf(endTime)) / 2);
        ShardingContext.getContext().setProtocol(protocol.getProtocol());
        List<NoticeMsg> result = messageRecordService.queryListByKeyword(startTime == null ? null : Long.valueOf(startTime), endTime == null ? null : Long.valueOf(endTime), noticeProtocolId == null ? null : Long.valueOf(noticeProtocolId), staffName, noticeTaskId);
        List<NoticeProtocolMessageVO> wapper = messageWapper.listCP(result);
        return new ListResult(wapper);
    }

    @ApiOperation(value = "站内信管理消息记录")
    @GetMapping(value = "/notice/message/record/stattionletter")
    public PageResult<NoticeProtocolMessageVO> stattionletter(@ApiParam(value = "发送时间start", required = false) @RequestParam(required = false) String startTime,
                                                              @ApiParam(value = "发送时间end", required = false) @RequestParam(required = false) String endTime,
                                                              @ApiParam(value = "阅读状态", required = false) @RequestParam(required = false) String readStatus,
                                                              @ApiParam(value = "页码", required = false) @RequestParam(required = false) Integer pageNo,
                                                              @ApiParam(value = "分页大小", required = false) @RequestParam(required = false) Integer pageSize) throws Exception {

        if (pageNo == null || pageSize == null || pageSize < 1 || pageNo < 1) {
            pageNo = 0;
            pageSize = 20;
        }
        try {
            String staffCode = UserContext.getUserContext().getStaffCode();
            log.info("staffCode: {}", staffCode);
            if (StringUtils.isBlank(staffCode)) {
                /**
                 * 该用户没有绑定人员,为系统管理员。系统管理员没有个人站内信
                 */
                return new PageResult(new ArrayList(), 0, pageSize, pageNo);
            }
            ShardingContext.getContext().setShardingTime((Long.valueOf(startTime) + Long.valueOf(endTime)) / 2);
            ShardingContext.getContext().setProtocol("stationLetter");
            Page page = new Page<>(pageNo, pageSize);
            Page<NoticeMsg> entityPage = messageRecordService.queryStationLetterPage(startTime == null ? null : Long.valueOf(startTime), endTime == null ? null : Long.valueOf(endTime), readStatus, staffCode, page);
            Page<NoticeProtocolMessageVO> wapper = messageWapper.pageCP(entityPage);
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

    @ApiOperation(value = "全部已读")
    @PutMapping(value = {"/notice/message/record/stattionletter/all",
                        "/notice/message/record/month/all"})
    public Result ackAllStationLetter(@ApiParam(value = "消息记录ID", required = false) @RequestBody ReadStationLetterVO readStationLetterVO) throws Exception {
        messageRecordService.ackAllStationLetter(
                readStationLetterVO.getStartTime(),
                readStationLetterVO.getEndTime(),
                readStationLetterVO.getProtocol()
        );
        return new Result(HttpStatus.SC_OK, "");
    }

    @ApiOperation(value = "根据消息id标记已读(适用于所有id在同一个月)")
    @PutMapping(value = {"/notice/message/record/stattionletter"
            , "/notice/message/record/month/ids"})
    public Result ackStationLetter(@ApiParam(value = "消息记录ID", required = false) @Valid @RequestBody ReadStationLetterVO readStationLetterVO) throws Exception {
        messageRecordService.ackStationLetter(
                readStationLetterVO.getStartTime(),
                readStationLetterVO.getEndTime(),
                readStationLetterVO.getMessageIds(),
                readStationLetterVO.getProtocol()
        );
        return new Result(HttpStatus.SC_OK, "");
    }

    @ApiOperation(value = "根据消息id标记已读(适用于跨月批量标记已读)")
    @PutMapping(value = {"/notice/message/record/stattionletters",
            "/notice/message/record/ids"})
    public Result ackStationLetters(@ApiParam(value = "消息记录ID", required = false) @Valid @NotNull @RequestBody BatchReadStationLetterVO batchReadStationLetterVO) throws Exception {
        for (BatchReadStationLetterDataVO batchReadStationLetterDataVO : batchReadStationLetterVO.getDatas()) {
            messageRecordService.ackStationLetters(
                    batchReadStationLetterDataVO.getShardingTime(),
                    Long.valueOf(batchReadStationLetterDataVO.getId()),
                    batchReadStationLetterDataVO.getProtocol()
            );
        }
        return new Result(HttpStatus.SC_OK, "");
    }

    @ApiOperation(value = "查询消息详情")
    @GetMapping(value = "/notice/message/record")
    public Result<NoticeTaskProtocolVO> queryRecord(@ApiParam(value = "消息记录ID", required = false) @RequestParam(required = true) String messageId,
                                                    @ApiParam(value = "协ID", required = false) @RequestParam(required = true) String protocolId,
                                                    @ApiParam(value = "消息发送时间戳", required = false) @RequestParam(required = false) String shardingTime) throws Exception {
        NoticeProtocol protocol = noticeProtocolMapper.selectOne(Wrappers.<NoticeProtocol>query().eq(NoticeProtocol.getIdFieldName(), protocolId));
        if (protocol == null) {
            return new Result();
        }


        ShardingContext.getContext().setShardingTime(Long.valueOf(shardingTime));
        ShardingContext.getContext().setProtocol(protocol.getProtocol());
        NoticeMsg noticeProtocolMessage;
        try {
            noticeProtocolMessage = messageRecordService.queryEntity(Long.valueOf(messageId));
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
                        new Result();
                    }
                } else if (throwable instanceof SQLServerException) {
                    SQLServerException sqlServerException = (SQLServerException) throwable;
                    if ("S0002".equals(sqlServerException.getSQLState())) {
                        log.error(e.getMessage(), e);
                        new Result();
                    }
                }
            }
            throw e;
        }
        if (noticeProtocolMessage == null) {
            return new Result();
        }

        Long noticeTaskProtocolId = noticeProtocolMessage.getNoticeTaskProtocolId();
        NoticeTaskProtocol noticeTaskProtocol = noticeTaskProtocolMapper.selectById(noticeTaskProtocolId);
        if (noticeTaskProtocol == null) {
            return new Result();
        }

        String content = noticeTaskProtocol.getContent();
        String text = "";
        String url = "";
        try {
            /**
             * 尝试将content转化为JSONObject，以获取消息的正文内容。如果转化失败或者消息正文内容的key不为text，则将content直接展示
             */
            JSONObject jsonObject = JSONObject.parseObject(content);
            text = jsonObject.getString("text");
            if (StringUtils.isBlank(text)) {
                text = content;
            }

            url = jsonObject.getString("url");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            text = content;
        }

        NoticeTaskProtocolVO noticeTaskProtocolVO = new NoticeTaskProtocolVO();
        noticeTaskProtocolVO.setContent(text);
        noticeTaskProtocolVO.setUrl(url);
        return new Result(noticeTaskProtocolVO);
    }

    @ApiOperation(value = "查询站内信未读消息总数")
    @GetMapping(value = "/notice/message/stationLetter/unreadnum")
    public Result<StationLetterUnReadNumVO> stationLetterUnreadNum() throws Exception {
        long count = messageRecordService.stationLetterUnreadNum();
        StationLetterUnReadNumVO stationLetterUnReadNumVO = new StationLetterUnReadNumVO();
        stationLetterUnReadNumVO.setCount(count);
        return new Result(stationLetterUnReadNumVO);
    }

    @ApiOperation(value = "根据协议查询未读消息总数(目前支持 stationLetter、mobile)")
    @GetMapping(value = "/notice/message/unreadnum")
    public Result<StationLetterUnReadNumVO> getUnreadNumByProtocol(
            @ApiParam(value = "协议", required = true) @RequestParam(required = true) String protocol
    ) throws Exception {
        NoticeProtocol noticeProtocol = noticeProtocolMapper.selectOne(new QueryWrapper<NoticeProtocol>().eq(NoticeProtocol.getProtocolFieldName(), protocol));
        long count = messageRecordService.getUnreadNum(noticeProtocol.getId());
        StationLetterUnReadNumVO stationLetterUnReadNumVO = new StationLetterUnReadNumVO();
        stationLetterUnReadNumVO.setCount(count);
        return new Result(HttpStatus.SC_OK, "", stationLetterUnReadNumVO);
    }

    @ApiOperation(value = "根据协议查询未读数量,按topic分组(目前支持 stationLetter、mobile)")
    @GetMapping(value = "/notice/message/group/unreadnum")
    public ListResult<NoticeMessageUnreadCount> getUnreadNumByProtocolGroup(
            @ApiParam(value = "协议", required = true) @RequestParam(required = true) String protocol
    ) throws Exception {
        NoticeProtocol noticeProtocol = noticeProtocolMapper.selectOne(new QueryWrapper<NoticeProtocol>().eq(NoticeProtocol.getProtocolFieldName(), protocol));
        List<NoticeMessageUnreadCount> noticeMessageUnreadCounts = messageRecordService.getUnreadNumGroup(noticeProtocol.getId());
        ListResult listResult = new ListResult(noticeMessageUnreadCounts);
        listResult.setCode(HttpStatus.SC_OK);
        return listResult;
    }

    @ApiOperation(value = "根据协议查询未读数量,按topic分组、并获取每组最新消息(目前支持 stationLetter、mobile)")
    @GetMapping(value = "/notice/message/group/unreadnumAndMessages")
    public ListResult<UnreadCountMessageVO> getUnreadnumAndMessages(
            @ApiParam(value = "协议", required = true) @RequestParam(required = true) String protocol
    ) throws Exception {
        NoticeProtocol noticeProtocol = noticeProtocolMapper.selectOne(new QueryWrapper<NoticeProtocol>().eq(NoticeProtocol.getProtocolFieldName(), protocol));
        List<NoticeMessageUnreadCount> noticeMessageUnreadCounts = messageRecordService.getUnreadNumGroup(noticeProtocol.getId());
        ArrayList<UnreadCountMessageVO> unreadCountMessageVOs = new ArrayList<>();
        noticeMessageUnreadCounts.forEach(noticeMessageUnreadCount -> {
            UnreadCountMessageVO unreadCountMessageVO = new UnreadCountMessageVO();
            BeanUtils.copyProperties(noticeMessageUnreadCount, unreadCountMessageVO);
            //"yyyy-MM-dd'T'HH:mm:ss.SSSZ"
            unreadCountMessageVO.setModifyTime(StringUtils.isBlank(noticeMessageUnreadCount.getModifyTime()) ? null : DateUtil.parse(noticeMessageUnreadCount.getModifyTime(), DateUtil.DATETIME_FORMAT).getTime());
            unreadCountMessageVOs.add(unreadCountMessageVO);
            String modifyTime = noticeMessageUnreadCount.getModifyTime();
            Date createTime = null;
            if (!StringUtils.isBlank(modifyTime)) {
                createTime = DateUtil.parse(noticeMessageUnreadCount.getModifyTime(), DateUtil.DATETIME_FORMAT);
            }
            Page<NoticeMsg> latestNewsByTopic = messageRecordService.getLatestNewsByTopic(protocol, noticeMessageUnreadCount.getTopicId(), createTime, null, noticeMessageUnreadCount.getStaffCode(), null, null);
            if (latestNewsByTopic != null && latestNewsByTopic.getRecords() != null && latestNewsByTopic.getRecords().size() > 0) {
                NoticeMsg noticeMsg = latestNewsByTopic.getRecords().get(0);
                NoticeProtocolMessageVO noticeProtocolMessageVO = messageWapper.entityCP(noticeMsg);
                noticeProtocolMessageVO.setContent(noticeTaskService.getContent(noticeMsg.getNoticeTaskProtocolId()));
                unreadCountMessageVO.setNoticeMsg(noticeProtocolMessageVO);
            }
        });
        ListResult listResult = new ListResult(unreadCountMessageVOs);
        listResult.setCode(HttpStatus.SC_OK);
        return listResult;
    }

    /***
     * 分页查询对象
     * @param endTime
     * @param protocol
     * @param readStatus
     * @param pageSize
     * @return
     * @throws Exception
     */
    @ApiOperation(value = "移动端分页查询消息记录")
    @GetMapping(value = "/notice/message/mobile/record/messages")
    public PageResult<NoticeProtocolMessageVO> list(
            @ApiParam(value = "创建时间结束时间戳", required = false) @RequestParam(required = false) String endTime,
            @ApiParam(value = "通知方式", required = true) @RequestParam(required = true) String protocol,
            @ApiParam(value = "主题ID", required = true) @RequestParam(required = true) Long topiclId,
            @ApiParam(value = "接收人名称", required = false) @RequestParam(required = false) String staffName,
            @ApiParam(value = "接收人code", required = false) @RequestParam(required = false) String staffCode,
            @ApiParam(value = "阅读状态", required = false) @RequestParam(required = false) String readStatus,
            @ApiParam(value = "分页大小", required = false) @RequestParam(required = false) Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            pageSize = 50;
        }
        Page<NoticeMsg> page = new Page<>(1, pageSize);
        Date createTime = null;
        if (!StringUtils.isBlank(endTime)) {
            createTime = new Date(Long.valueOf(endTime));
        }
        Page<NoticeMsg> entityPage = messageRecordService.getLatestNewsByTopic(protocol, topiclId, createTime, staffName, staffCode, readStatus, page);
        Page<NoticeProtocolMessageVO> wapper = messageWapper.pageCP(entityPage);
        List<NoticeProtocolMessageVO> records = wapper.getRecords();
        records.forEach(noticeProtocolMessageVO -> {
            noticeProtocolMessageVO.setContent(noticeTaskService.getContent(Long.valueOf(noticeProtocolMessageVO.getNoticeTaskProtocolId())));
        });
        PageResult pageResult = new PageResult(wapper.getRecords(), entityPage.getTotal(), entityPage.getSize(), entityPage.getCurrent());
        pageResult.setCode(HttpStatus.SC_OK);
        return pageResult;

    }

    /**
     * @param startTime
     * @param endTime
     * @param noticeTaskId
     * @param noticeProtocolId
     * @param staffName
     * @return
     * @throws Exception
     */
    @ApiOperation(value = "根据接收人和消息类型统计指定时间内的消息数量")
    @GetMapping(value = "/notice/message/count")
    public Result<CountVO> countByStaffName(@ApiParam(value = "创建时间start", required = true) @RequestParam(required = true) String startTime,
                                            @ApiParam(value = "创建时间end", required = true) @RequestParam(required = true) String endTime,
                                            @ApiParam(value = "通知方式ID", required = true) @RequestParam(required = true) String noticeProtocolId,
                                            @ApiParam(value = "接收人名称", required = false) @RequestParam(required = false) String staffName,
                                            @ApiParam(value = "任务ID", required = false) @RequestParam(required = false) String noticeTaskId) throws Exception {
        NoticeProtocol protocol = noticeProtocolMapper.selectOne(Wrappers.<NoticeProtocol>query().eq(NoticeProtocol.getIdFieldName(), noticeProtocolId));
        ShardingContext.getContext().setShardingTime((Long.valueOf(startTime) + Long.valueOf(endTime)) / 2);
        ShardingContext.getContext().setProtocol(protocol.getProtocol());
        Integer count = messageRecordService.countByStaffName(startTime == null ? null : Long.valueOf(startTime), endTime == null ? null : Long.valueOf(endTime), noticeProtocolId == null ? null : Long.valueOf(noticeProtocolId), staffName, noticeTaskId);
        CountVO countVO = new CountVO();
        countVO.setCount(count);
        return new Result(countVO);
    }

}
