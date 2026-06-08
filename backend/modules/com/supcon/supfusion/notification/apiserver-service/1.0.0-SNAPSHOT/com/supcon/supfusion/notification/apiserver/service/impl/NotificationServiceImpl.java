package com.supcon.supfusion.notification.apiserver.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.supcon.supfusion.framework.cloud.common.exception.BizHttpStatusException;
import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;
import com.supcon.supfusion.notification.apiserver.common.bean.RangeBO;
import com.supcon.supfusion.notification.apiserver.common.execption.NotificationApiServerError;
import com.supcon.supfusion.notification.apiserver.common.execption.NotificationApiServerExecption;
import com.supcon.supfusion.notification.apiserver.common.utils.NumberUtil;
import com.supcon.supfusion.notification.apiserver.dao.entities.NoticeMsg;
import com.supcon.supfusion.notification.apiserver.dao.entities.NoticeProtocol;
import com.supcon.supfusion.notification.apiserver.dao.entities.NoticeRecieveRange;
import com.supcon.supfusion.notification.apiserver.dao.entities.NoticeRecieveRangeExt;
import com.supcon.supfusion.notification.apiserver.dao.entities.NoticeTask;
import com.supcon.supfusion.notification.apiserver.dao.entities.NoticeTmpl;
import com.supcon.supfusion.notification.apiserver.dao.entities.NoticeTopic;
import com.supcon.supfusion.notification.apiserver.dao.entities.NoticeTopicTmplateRelation;
import com.supcon.supfusion.notification.apiserver.dao.mappers.NoticeProtocolMapper;
import com.supcon.supfusion.notification.apiserver.dao.mappers.NoticeReceiveRangeExtMapper;
import com.supcon.supfusion.notification.apiserver.dao.mappers.NoticeReceiveRangeMapper;
import com.supcon.supfusion.notification.apiserver.dao.mappers.NoticeTaskMapper;
import com.supcon.supfusion.notification.apiserver.dao.mappers.NoticeTmplMapper;
import com.supcon.supfusion.notification.apiserver.dao.mappers.NoticeTopicMapper;
import com.supcon.supfusion.notification.apiserver.dao.mappers.NoticeTopicTmplRelMapper;
import com.supcon.supfusion.notification.apiserver.manager.StaffService;
import com.supcon.supfusion.notification.apiserver.service.NoticeProtocolMessageService;
import com.supcon.supfusion.notification.apiserver.service.NotificationService;
import com.supcon.supfusion.notification.apiserver.service.bo.AckBO;
import com.supcon.supfusion.notification.apiserver.service.bo.MessageBO;
import com.supcon.supfusion.notification.apiserver.service.bo.SendWithMessageBO;
import com.supcon.supfusion.notification.apiserver.service.bo.SendWithMessageV1BO;
import com.supcon.supfusion.notification.apiserver.service.bo.SendWithTopicBO;
import com.supcon.supfusion.notification.common.bean.RangeType;
import com.supcon.supfusion.notification.common.util.FreeMarkUtil;
import com.supcon.supfusion.notification.kafka.MultiTenantKafKa;
import com.supcon.supfusion.notification.protocol.common.Address;
import com.supcon.supfusion.notification.protocol.common.Message;
import com.supcon.supfusion.notification.protocol.common.SendStatus;
import com.supcon.supfusion.notification.sharding.context.ShardingContext;
import com.supcon.supfusion.notification.sharding.util.ITableNameStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Slf4j
@Service("apiserverNotificationServiceImpl")
public class NotificationServiceImpl implements NotificationService {
    @Autowired
    private MultiTenantKafKa multiTenantKafKa;
    @Autowired
    private StaffService staffService;
    @Resource(name = "apiserverNoticeProtocolMessageServiceImpl")
    private NoticeProtocolMessageService noticeProtocolMessageService;
    @Resource(name = "apiserverNoticeTaskMapper")
    private NoticeTaskMapper noticeTaskMapper;
    @Resource(name = "apiserverNoticeTopicMapper")
    private NoticeTopicMapper noticeTopicMapper;
    @Resource(name = "apiserverNoticeTmplMapper")
    private NoticeTmplMapper noticeTemplateMapper;
    @Resource(name = "apiserverNoticeTopicTmplRelMapper")
    private NoticeTopicTmplRelMapper noticeTopicTmplRelMapper;
    @Resource(name = "apiserverNoticeProtocolMapper")
    private NoticeProtocolMapper noticeProtocolMapper;
    @Resource(name = "apiserverNoticeReceiveRangeMapper")
    private NoticeReceiveRangeMapper noticeReceiveRangeMapper;
    @Resource(name = "apiserverNoticeReceiveRangeExtMapper")
    private NoticeReceiveRangeExtMapper noticeReceiveRangeExtMapper;


    @Override
    @Transactional
    public String sendWithMessageV1(SendWithMessageV1BO sendWithMessageV1BO) {
        Map<String, List<Address>> addresses = staffService.getUserAddress(sendWithMessageV1BO.getUserIds(), sendWithMessageV1BO.getProtocols());

        Long shardingTime = LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli();
        Long taskId = IDGenerator.newInstance().generate().longValue();
        String taskCode = ITableNameStrategy.tableNameSuffix(shardingTime) + taskId.toString();
        NoticeTask noticeTask = new NoticeTask();
        noticeTask.setId(taskId);
        noticeTask.setCode(taskCode);
        noticeTask.setBsmodCode(sendWithMessageV1BO.getBsmodCode());
        noticeTask.setBsmodName(sendWithMessageV1BO.getBsmodName());
        noticeTask.setTaskType(1);
        noticeTask.setShardingTime(shardingTime);
        ShardingContext.getContext().setShardingTime(shardingTime);
        noticeTaskMapper.insert(noticeTask);
        sendWithMessageV1BO.getProtocols().stream().forEach(protocol -> {
            Message message = new Message();
            message.setTaskId(taskId);
            message.setBsmodCode(sendWithMessageV1BO.getBsmodCode());
            message.setBsmodName(sendWithMessageV1BO.getBsmodName());
            message.setProtocol(protocol);
            message.setAddresses(addresses.get(protocol));
            message.setContent(sendWithMessageV1BO.getContent());
            message.setShardingTime(shardingTime);
            multiTenantKafKa.send(protocol, (JSONObject) JSON.toJSON(message));
        });

        return taskCode;
    }

    @Override
    public String sendWithMessage(SendWithMessageBO sendWithMessageBO) {
        List<MessageBO> contents = sendWithMessageBO.getContents();
        Map<String, String> protocolMessages = new HashMap<>();
        contents.forEach(content -> {
            protocolMessages.put(content.getProtocol(), content.getContent());
        });
        if (sendWithMessageBO.getContents() != null && sendWithMessageBO.getContents().size() > 0) {
            for (MessageBO messageBO : sendWithMessageBO.getContents()) {
                Integer count = noticeProtocolMapper.selectCount(Wrappers.<NoticeProtocol>query().eq(NoticeProtocol.getProtocolFieldName(), messageBO.getProtocol()).eq(NoticeProtocol.getValidFieldName(), 1));
                if (count == null || count == 0) {
                    throw new BizHttpStatusException(NotificationApiServerError.ERROR_PROTOCOL_DONT_EXIST, 400);
                }
            }
        }

        Map<String, List<Address>> protocolAddresses = staffService.getStaffAddress(sendWithMessageBO.getReceivers(), protocolMessages.keySet());
        return pushMessgae(sendWithMessageBO.getBsmodCode(), sendWithMessageBO.getBsmodName(), 1, null, null, protocolAddresses, protocolMessages, null);
    }

    @Override
    public String sendWithTopic(SendWithTopicBO sendWithTopicBO) {
        log.info("获得发送指令对象sendWithMessageBO：{}", sendWithTopicBO.toString());
        List<NoticeTopicTmplateRelation> topicTmplateRelationList;
        Map<String, String> protocolTemplates = new HashMap<>();
        Map<String, String> presentRequestProtocolTemplates;

        //根据topicCode获取消息主题对象
        NoticeTopic noticeTopic = noticeTopicMapper.selectOne(Wrappers.<NoticeTopic>query().eq(NoticeTopic.getCodeFieldName(), sendWithTopicBO.getTopicCode()));
        //根据主题id获取主题-模板映射表的数据从而获取模板和通知方式对象
        if (noticeTopic != null) {
            sendWithTopicBO.setTopicId(noticeTopic.getId());
            topicTmplateRelationList = noticeTopicTmplRelMapper.selectList(Wrappers.<NoticeTopicTmplateRelation>query().eq(NoticeTopicTmplateRelation.getTopicIdName(), noticeTopic.getId()));
        } else {
            throw new BizHttpStatusException(NotificationApiServerError.ERROR_TOPIC_NOT_EXIST, 400);
        }

        //获取协议和对应模板
        if (topicTmplateRelationList != null && topicTmplateRelationList.size() > 0) {
            int size = topicTmplateRelationList.size();
            for (NoticeTopicTmplateRelation noticeTopicTmplateRelation : topicTmplateRelationList) {
                NoticeTmpl template = noticeTemplateMapper.selectById(noticeTopicTmplateRelation.getTemplate());
                NoticeProtocol protocol = noticeProtocolMapper.selectOne(Wrappers.<NoticeProtocol>query().eq(NoticeProtocol.getIdFieldName(), noticeTopicTmplateRelation.getProtocol()).eq(NoticeProtocol.getValidFieldName(), 1));
                if (protocol == null) {
                    throw new BizHttpStatusException(NotificationApiServerError.ERROR_PROTOCOL_DONT_EXIST, 400);
                }
                if (template != null) {
                    protocolTemplates.put(protocol.getProtocol(), template.getTemplate());
                } else {
                    log.error("template {}, protocl {} dont exist", noticeTopicTmplateRelation.getTemplate(), noticeTopicTmplateRelation.getProtocol());
                }
            }
        } else {
            throw new BizHttpStatusException(NotificationApiServerError.ERROR_TOPIC_HAS_NO_TEMPLATE, 400);
        }

        if (protocolTemplates.size() == 0) {
            throw new BizHttpStatusException(NotificationApiServerError.ERROR_TOPIC_HAS_NO_TEMPLATE, 400);
        }

        //需要根据当前请求中指定的protocol，来发选择协议发送。如不存在throw异常
        if (sendWithTopicBO.getProtocols() != null && sendWithTopicBO.getProtocols().size() > 0) {
            presentRequestProtocolTemplates = new HashMap<>();
            sendWithTopicBO.getProtocols().forEach(protocol -> {
                String template = protocolTemplates.get(protocol);
                if (template == null) {
                    throw new BizHttpStatusException(NotificationApiServerError.ERROR_TOPIC_EXCLUSIVE_PROTOCOL, 400);
                } else {
                    presentRequestProtocolTemplates.put(protocol, template);
                }
            });
        } else {
            presentRequestProtocolTemplates = protocolTemplates;
        }

        //获取发送地址
        List<RangeBO> receivers;
        if (sendWithTopicBO.getReceivers() != null && sendWithTopicBO.getReceivers().size() > 0) {
            receivers = sendWithTopicBO.getReceivers();
        } else {
            receivers = new ArrayList<>();
            List<NoticeRecieveRange> noticeRecieveRanges = noticeReceiveRangeMapper.selectList(Wrappers.<NoticeRecieveRange>query().eq(NoticeRecieveRange.getTopicIdFieldName(), noticeTopic.getId()));
            if (noticeRecieveRanges == null || noticeRecieveRanges.size() == 0) {
                throw new BizHttpStatusException(NotificationApiServerError.ERROR_NO_RECEIVERS, 400);
            } else {
                noticeRecieveRanges.stream().forEach(noticeRecieveRange -> {
                    RangeBO rangeBO = new RangeBO();
                    List<String> codes = new ArrayList<>();
                    rangeBO.setCodes(codes);
                    rangeBO.setUrl(noticeRecieveRange.getBizModuleAddr());
                    rangeBO.setRangeType(RangeType.byValueOf(noticeRecieveRange.getRangeType()));
                    List<NoticeRecieveRangeExt> noticeRecieveRangeExts = noticeReceiveRangeExtMapper.selectList(Wrappers.<NoticeRecieveRangeExt>query().eq(NoticeRecieveRangeExt.getRangeIdFieldName(), noticeRecieveRange.getId()));
                    if (noticeRecieveRangeExts == null || noticeRecieveRangeExts.size() == 0) {
                        log.error("there is no receiver in {}", noticeRecieveRange.getId());
                    } else {
                        noticeRecieveRangeExts.stream().forEach(noticeRecieveRangeExt -> {
                            codes.add(noticeRecieveRangeExt.getReceiverCode());
                        });
                    }
                    receivers.add(rangeBO);
                });
            }
        }
        Map<String, List<Address>> protocolAddresses = staffService.getStaffAddress(receivers, presentRequestProtocolTemplates.keySet());

        //解析模板，获取消息内容
        Map<String, String> protocolMessages = new HashMap<>();
        for (String protocol : protocolAddresses.keySet()) { //用protocolAddresses的keyset，接收地址不存在的协议无需解析模板
            String template = presentRequestProtocolTemplates.get(protocol);
            try {
                /**
                 * 如果模板中的占位符数量大于请求传入的数量，则没有传入的占位符值设为空
                 */
                JSONObject paramData;
                if (sendWithTopicBO.getParam() != null) {
                    paramData = sendWithTopicBO.getParam();
                } else {
                    paramData = new JSONObject();
                }
                List<String> paramKeys = FreeMarkUtil.getVariableWithoutDefaultValue(template);
                for (String paramKey : paramKeys) {
                    if (!paramData.containsKey(paramKey)) {
                        paramData.put(paramKey, "");
                    }
                }
                String message = FreeMarkUtil.buildTemplate(template, paramData);
                protocolMessages.put(protocol, message);
            } catch (Exception e) {
                log.error("freemark build fail:{}, {} ,{}", protocol, template, sendWithTopicBO.getParam() != null ? sendWithTopicBO.getParam().toJSONString() : null);
                log.error(e.getMessage(), e);
                throw new NotificationApiServerExecption(NotificationApiServerError.ERROR_FREEMARK_BUILD_FAIL);
            }
        }
        JSONObject param = sendWithTopicBO.getParam();
        return pushMessgae(sendWithTopicBO.getBsmodCode(), sendWithTopicBO.getBsmodName(), 0, noticeTopic.getName(), noticeTopic.getId(), protocolAddresses, protocolMessages, param);
    }


    @Override
    @Transactional
    public void ack(List<AckBO> ackBOS) {
        if (ackBOS == null || ackBOS.size() == 0) {
            return;
        }
        ackBOS.forEach(ackBO -> {
            List<NoticeMsg> noticeProtocolMessages = new ArrayList<>();
            if (!NumberUtil.validString(ackBO.getTime())) {
                throw new NotificationApiServerExecption(NotificationApiServerError.ERROR_TIME_FORMAT);
            }
            ShardingContext.getContext().setProtocol(ackBO.getProtocol());
            ShardingContext.getContext().setShardingTime(Long.valueOf(ackBO.getTime()));

            NoticeMsg noticeProtocolMessage = new NoticeMsg();
            String messageId = ackBO.getMessageId();
            if (!NumberUtil.validString(messageId)) {
                return;
            }
            noticeProtocolMessage.setId(Long.valueOf(messageId));

            if (ackBO.getReadStatus() != null) {
                noticeProtocolMessage.setReadStatus(ackBO.getReadStatus().ordinal());
            }
            if (ackBO.getSendStatus() != null) {
                noticeProtocolMessage.setSendStatus(ackBO.getSendStatus().ordinal());
            }
            if (ackBO.getSendStatus() != null && ackBO.getSendStatus() == SendStatus.FAIL && StringUtils.hasText(ackBO.getErrorMessage())) {
                noticeProtocolMessage.setErrorResult(ackBO.getErrorMessage());
            }

            noticeProtocolMessages.add(noticeProtocolMessage);
            noticeProtocolMessageService.updateBatchById(noticeProtocolMessages);
        });
    }

//    /***
//     * 通过模板信息执行消息转发到发送消息引擎
//     * @param sendWithTmplate 包含通讯方式，模板id等
//     */
//    @Override
//    public void sendWithTemplate(SendWithTmplateDTO sendWithTmplate) {
//        log.info("获得发送指令对象SendWithTmplateDTO：{}", sendWithTmplate.toString());
//        //通知方式--消息内容模板Map
//        Map<String, String> protcolTemplates = new HashMap<>();
//        //获取模板id
//        List<Long> list = sendWithTmplate.getTmplateId();
//        if (list != null && list.size() > 0) {
//            for (Long tmpId : list) {
//                //根据模板id获取模板对象
//                NoticeTmpl template = noticeTemplateMapper.selectById(tmpId);
//                NoticeProtocol protocol = noticeProtocolMapper.selectById(template.getId());
//                if (protocol != null && template != null) {
//                    protcolTemplates.put(protocol.getProtocol(), template.getTemplate());
//                } else {
//                    log.error("template {}, protocl {} dont exist", template.getCode(), protocol.getId());
//                }
//            }
//        } else {
//            throw new NotificationApiServerExecption(NotificationApiServerError.ERROR_TOPIC_HAS_NO_TEMPLATE);
//        }
//
//        if (protcolTemplates.size() == 0) {
//            throw new NotificationApiServerExecption(NotificationApiServerError.ERROR_TOPIC_HAS_NO_TEMPLATE);
//        }
//        //获取发送地址
//        List<RangeBO> receivers = null;
//        RangeBO rangeBO = new RangeBO();
//        rangeBO.setRangeType(RangeType.STAFF);
//        rangeBO.setCodes(sendWithTmplate.getReceivers());
//        //添加传过来的人员codes
//        receivers.add(rangeBO);
//        Map<String, List<Address>> protocolAddresses = staffService.getStaffAddress(receivers, protcolTemplates.keySet());
//
//        //解析模板，获取消息内容
//        Map<String, String> protocolMessages = new HashMap<>();
//        for (String protocol : protocolAddresses.keySet()) { //用protocolAddresses的keyset，接收地址不存在的协议无需解析模板
//            String template = protcolTemplates.get(protocol);
//            try {
//                String message = FreeMarkUtil.buildTemplate(template, sendWithTmplate.getParam());
//                protocolMessages.put(protocol, message);
//            } catch (Exception e) {
//                log.error("freemark build fail:{}, {} ,{}", protocol, template, sendWithTmplate.getParam().toJSONString());
//                throw new NotificationApiServerExecption(NotificationApiServerError.ERROR_FREEMARK_BUILD_FAIL);
//            }
//        }
//        pushMessgae(sendWithTmplate.getBsmodCode(), sendWithTmplate.getBsmodName(), 1, null, protocolAddresses, protocolMessages);
//    }

    private String pushMessgae(String bsmodCode, String bsmodName, int messageType, String topicName, Long topicId, Map<String, List<Address>> protocolAddresses, Map<String, String> protocolMessages, JSONObject param) {
        Long shardingTime = LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli();
        ShardingContext.getContext().setShardingTime(shardingTime);
        Long taskId = IDGenerator.newInstance().generate().longValue();
        String taskCode = ITableNameStrategy.tableNameSuffix(shardingTime) + taskId.toString();

        NoticeTask noticeTask = new NoticeTask();
        noticeTask.setId(taskId);
        noticeTask.setCode(taskCode);
        noticeTask.setBsmodCode(bsmodCode);
        noticeTask.setBsmodName(bsmodName);
        noticeTask.setTaskType(messageType);
        noticeTask.setShardingTime(shardingTime);
        noticeTask.setNoticeTopicId(topicId);
        noticeTaskMapper.insert(noticeTask);
        //构建Message用于投入消息队列
        protocolAddresses.forEach((protocol, addresses) -> {
            Message message = new Message();
            message.setTaskId(taskId);
            message.setBsmodCode(bsmodCode);
            message.setBsmodName(bsmodName);
            message.setProtocol(protocol);
            message.setAddresses(addresses);
            message.setShardingTime(shardingTime);
            message.setContent(protocolMessages.get(protocol));
            message.setTopic(topicName);
            message.setTopicId(topicId);
            if (param != null) {
                message.setParam(param.toJSONString());
            }
            multiTenantKafKa.send(protocol, (JSONObject) JSON.toJSON(message));
        });
        return taskCode;
    }

}
