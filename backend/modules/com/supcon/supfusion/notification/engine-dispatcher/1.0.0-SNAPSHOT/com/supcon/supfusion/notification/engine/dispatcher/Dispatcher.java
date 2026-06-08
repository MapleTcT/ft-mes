package com.supcon.supfusion.notification.engine.dispatcher;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.supcon.supfusion.framework.boot.scaffold.dbp.DataSourceConnectionProperties;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.framework.cloud.common.events.TenantDatabaseInfo;
import com.supcon.supfusion.framework.cloud.common.events.TenantEventTypeEnum;
import com.supcon.supfusion.framework.cloud.common.events.TenantInfo;
import com.supcon.supfusion.framework.cloud.common.events.TenantInfoLocalStorage;
import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;
import com.supcon.supfusion.notification.common.constants.Constants;
import com.supcon.supfusion.notification.engine.common.NumberUtil;
import com.supcon.supfusion.notification.engine.dao.entities.NoticeMessageUnreadCount;
import com.supcon.supfusion.notification.engine.dao.entities.NoticeMsg;
import com.supcon.supfusion.notification.engine.dao.entities.NoticeProtocol;
import com.supcon.supfusion.notification.engine.dao.entities.NoticeTaskProtocol;
import com.supcon.supfusion.notification.engine.dao.mappers.NoticeMessageUnreadCountMapper;
import com.supcon.supfusion.notification.engine.dao.mappers.NoticeProtocolMapper;
import com.supcon.supfusion.notification.engine.dispatcher.service.NoticeProtocolMessageService;
import com.supcon.supfusion.notification.engine.dispatcher.service.NoticeTaskProtocolService;
import com.supcon.supfusion.notification.kafka.MultiTenantKafKa;
import com.supcon.supfusion.notification.protocol.common.Message;
import com.supcon.supfusion.notification.protocol.common.ReadStatus;
import com.supcon.supfusion.notification.protocol.common.SendStatus;
import com.supcon.supfusion.notification.protocol.config.ProtocolConfig;
import com.supcon.supfusion.notification.protocol.model.Ack;
import com.supcon.supfusion.notification.protocol.model.AckResult;
import com.supcon.supfusion.notification.protocol.model.Notice;
import com.supcon.supfusion.notification.protocol.model.Receiver;
import com.supcon.supfusion.notification.sharding.context.ShardingContext;
import com.supcon.supfusion.tenant.api.TenantManagerService;
import com.supcon.supfusion.tenant.api.dto.TenantDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.*;

@Service
@Slf4j
public class Dispatcher implements CommandLineRunner {
    private Set<String> protocoles;
    @Autowired
    private TenantManagerService tenantManagerService;
    @Autowired
    private MultiTenantKafKa multiTenantKafKa;
    @Autowired
    private ProtocolCache protocolCache;
    @Autowired
    private DataSourceConnectionProperties dataSourceConnectionProperties;
    @Resource(name = "engineNoticeProtocolMapper")
    private NoticeProtocolMapper noticeProtocolMapper;
    @Resource(name = "engineNoticeTaskProtocolServiceImpl")
    private NoticeTaskProtocolService noticeTaskProtocolService;
    @Resource(name = "engineNoticeProtocolMessageServiceImpl")
    private NoticeProtocolMessageService noticeProtocolMessageService;
    @Resource(name = "engineNoticeMessageUnreadCountMapper")
    private NoticeMessageUnreadCountMapper noticeMessageUnreadCountMapper;

    /**
     * 消费消息队列消息．记录消息记录，往通知APP分发消息，记录Ack
     *
     * @param topic
     */
    public void consume(String topic) {
        multiTenantKafKa.consume(topic, "SUPFUSION_NOTIFICATION", jsonObject -> {
            consumerMessage(jsonObject);
        });
    }

    public void consumerMessage(JSONObject jsonObject) {
        /**
         * 记录消息记录
         */
        Message message = jsonObject.toJavaObject(Message.class);
        String protocol = message.getProtocol();

        Notice notice = new Notice();
        notice.setProtocol(message.getProtocol());
        notice.setContent(message.getContent());
        notice.setBsmodCode(message.getBsmodCode());
        notice.setBsmodName(message.getBsmodName());
        notice.setTopic(message.getTopic());
        notice.setTopicId(message.getTopicId());
        notice.setParam(message.getParam());
        List<Receiver> receivers = new ArrayList<>();
        notice.setReceivers(receivers);

        NoticeProtocol noticeProtocol = noticeProtocolMapper.selectOne(Wrappers.<NoticeProtocol>query().eq(NoticeProtocol.getProtocolFieldName(), protocol));
        long taskProtocolId = IDGenerator.newInstance().generate().longValue();
        Long protocolId = noticeProtocol.getId();
        Long taskId = message.getTaskId();
        Long shardingTime = message.getShardingTime();
        String bsmodName = message.getBsmodName();
        String bsmodCode = message.getBsmodCode();
        String param = message.getParam();
        /**
         * set 分表字段
         */
        ShardingContext.getContext().setProtocol(protocol);
        ShardingContext.getContext().setShardingTime(shardingTime);
        /**
         * long to string 用于传输
         */
        notice.setTime(shardingTime);

        NoticeTaskProtocol noticeTaskProtocol = new NoticeTaskProtocol();
        noticeTaskProtocol.setContent(message.getContent());
        noticeTaskProtocol.setId(taskProtocolId);
        noticeTaskProtocol.setNoticeTaskId(taskId);
        noticeTaskProtocol.setNoticeProtocolId(protocolId);
        noticeTaskProtocolService.save(noticeTaskProtocol);

        List<NoticeMsg> noticeProtocolMessages = new ArrayList<>();
        message.getAddresses().forEach(address -> {
            long messageId = IDGenerator.newInstance().generate().longValue();
            NoticeMsg noticeProtocolMessage = new NoticeMsg();
            noticeProtocolMessage.setId(messageId);
            noticeProtocolMessage.setNoticeProtocolId(protocolId);
            noticeProtocolMessage.setNoticeTaskId(taskId);
            noticeProtocolMessage.setNoticeTaskProtocolId(taskProtocolId);
            noticeProtocolMessage.setReadStatus(2);
            noticeProtocolMessage.setSendStatus(2);
            noticeProtocolMessage.setShardingTime(shardingTime);
            noticeProtocolMessage.setStaffCode(address.getStaffCode());
            noticeProtocolMessage.setStaffName(address.getStaffName());
            noticeProtocolMessage.setBsmodName(bsmodName);
            noticeProtocolMessage.setBsmodCode(bsmodCode);
            noticeProtocolMessage.setTopicName(message.getTopic());
            /**
             * 630版本没有人员，暂时冗余一个userName
             */
            noticeProtocolMessage.setUserName(address.getUserName());
            /**
             * 存储用户传进来的原始参数
             */
            noticeProtocolMessage.setParam(param);
            noticeProtocolMessage.setTopicId(message.getTopicId());
            noticeProtocolMessages.add(noticeProtocolMessage);


            Receiver receiver = new Receiver();
            receiver.setAddress(address.getAddress());
            if (address.getStaffCode() != null) {
                receiver.setStaffCode(address.getStaffCode());
            }
            /**
             * long to string 用于传输
             */
            receiver.setMessageId(messageId + "");
            receivers.add(receiver);
        });
        noticeProtocolMessageService.saveBatch(noticeProtocolMessages);

        /**
         * 往通知APP分发消息
         */
        log.info("send notice :{}", notice.toString());
        Ack ack = protocolCache.send(protocol, notice);
        if (ack == null)
            return;

        /**
         * 记录Ack
         */
        List<NoticeMsg> ackMessages = new ArrayList<>();
        List<AckResult> results = ack.getResults();
        results.stream().forEach(result -> {
            NoticeMsg noticeProtocolMessage = new NoticeMsg();
            ackMessages.add(noticeProtocolMessage);

            String messageId = result.getMessageId();
            if (!NumberUtil.validString(messageId)) {
                return;
            }
            noticeProtocolMessage.setId(Long.valueOf(messageId));
            noticeProtocolMessage.setReadStatus(result.getReadStatus() != null ? result.getReadStatus().ordinal() : ReadStatus.UNKOWN.ordinal());
            noticeProtocolMessage.setSendStatus(result.getSendStatus() != null ? result.getSendStatus().ordinal() : SendStatus.UNKOWN.ordinal());
            if (result.getSendStatus() != null && result.getSendStatus() == SendStatus.FAIL && StringUtils.hasText(result.getErrorMessage())) {
                noticeProtocolMessage.setErrorResult(result.getErrorMessage().length() > 200 ? result.getErrorMessage().substring(0, 200) : result.getErrorMessage());
            }
            //如果是mobile就统计数量
            if ("mobile".equals(protocol)) {
                NoticeMsg noticeMsg = noticeProtocolMessageService.getById(Long.valueOf(messageId));
                try {
                    long id = IDGenerator.newInstance().generate().longValue();
                    NoticeMessageUnreadCount noticeMessageUnreadCount = new NoticeMessageUnreadCount();
                    noticeMessageUnreadCount.setId(id);
                    noticeMessageUnreadCount.setNoticeProtocolId(noticeMsg.getNoticeProtocolId());
                    noticeMessageUnreadCount.setStaffCode(noticeMsg.getStaffCode());
                    noticeMessageUnreadCount.setTopicId(noticeMsg.getTopicId());
                    noticeMessageUnreadCount.setUnreadCount(1L);
                    noticeMessageUnreadCountMapper.insert(noticeMessageUnreadCount);
                } catch (Exception e) {
                    if (e instanceof DuplicateKeyException) {
                        log.info("{}, stationLetter已存在记录", noticeMsg.getStaffCode(), e);
                        noticeMessageUnreadCountMapper.increase(noticeMsg.getStaffCode(),
                                noticeMsg.getNoticeProtocolId(),
                                noticeMsg.getTopicId(),
                                new Date());
                    } else {
                        log.error(e.getMessage(), e);
                    }
                }

            }
        });
        noticeProtocolMessageService.updateBatchById(ackMessages);
    }


    @Override
    public void run(String... args) throws Exception {
        this.protocoles = new LinkedHashSet();
        Boolean useSystem = dataSourceConnectionProperties.getUseSystem();

        /**
         * 当前已初始化consumer的协议列表。因为可能其他先安装当前协议的租户已经完成对应consumer的初始化，后续安装该协议的租户无需初始化consumer（共用一个topic）。
         */
        if (useSystem == null || !useSystem) {
            useTenant();
        } else {
            useSystem();
        }

    }

    private void useSystem() {
        Set<TenantInfo> tenantInfos = TenantInfoLocalStorage.getAll();
        if (tenantInfos == null || tenantInfos.isEmpty()) {
            log.info("there is no tenant");
        }

        //获取所有的protocol
        for (TenantInfo tenantInfo : tenantInfos) {
            String tenantId = tenantInfo.getId();
            String dbType = tenantInfo.getDatabaseInfo().getDbType();
            if (dbType == null || "".equals(dbType)) {
                log.info("there is no datasource in {}", tenantId);
                continue;
            }

            RpcContext rpcContext = RpcContext.getContext();
            rpcContext.setTenantId(tenantId);
            log.info("get {} datasource", tenantId);

            List<NoticeProtocol> noticeProtocols = noticeProtocolMapper.selectList(Wrappers.query());
            if (noticeProtocols == null || noticeProtocols.size() == 0) {
                log.info("there is no protocol");
                continue;
            }
            noticeProtocols.forEach(protocol -> protocoles.add(protocol.getProtocol()));
        }
        initConsumer();
    }

    private void useTenant() {
        ListResult<TenantDTO> tenantDTOListResult = tenantManagerService.find(null);
        Collection<TenantDTO> tenantDTOS = tenantDTOListResult.getList();
        if (tenantDTOS == null || tenantDTOS.size() == 0) {
            log.info("there is no tenant");
        }
        //获取所有的protocol
        for (TenantDTO tenantDTO : tenantDTOS) {
            String tenantId = tenantDTO.getId();
            String dbType = "";
            /**
             *　全量初始化租户数据源，防止数据源缺失
             */
            for (TenantDTO.DatabaseDTO databaseDTO : tenantDTO.getDatabaseInfos()) {
                log.info("get databaseDTO: {} ", databaseDTO.toString());
                if (databaseDTO.getMajor() == null || !databaseDTO.getMajor()) {
                    continue;
                }
                dbType = databaseDTO.getDbType();
                TenantDatabaseInfo tenantDatabaseInfo = new TenantDatabaseInfo(databaseDTO.getHost(), databaseDTO.getPort(), databaseDTO.getUsername(), databaseDTO.getPassword(), databaseDTO.getDbName(), databaseDTO.getDbType(), null);
                TenantInfo tenantInfo = new TenantInfo(TenantEventTypeEnum.ADD, tenantDTO.getId(), tenantDTO.getInstanceId(), tenantDTO.getDescription(), tenantDatabaseInfo);
                log.info("init {} datasource, tenantInfo:{}", tenantId, tenantInfo.toString());
                TenantInfoLocalStorage.add(tenantInfo);
            }
            if (dbType == null || "".equals(dbType)) {
                log.info("there is no datasource in {}", tenantId);
                continue;
            }

            RpcContext rpcContext = RpcContext.getContext();
            rpcContext.setTenantId(tenantId);
            log.info("get {} datasource", tenantId);

            List<NoticeProtocol> noticeProtocols = noticeProtocolMapper.selectList(Wrappers.<NoticeProtocol>query().eq(NoticeProtocol.getValidFieldName(), 1));
            if (noticeProtocols == null || noticeProtocols.size() == 0) {
                log.info("there is no protocol");
                continue;
            }
            noticeProtocols.forEach(protocol -> protocoles.add(protocol.getProtocol()));
        }
        initConsumer();
    }

    private void initConsumer() {
        //初始化所有协议的kafka consumer
        protocoles.forEach(protocol -> {
            consume(protocol);
        });
        //初始化本地协议处理器
        protocolCache.addLocalProtocol("email");
        protocolCache.addLocalProtocol("stationLetter");

        //初始协议注册监听器。监听协议注册消息，初始化对应协议的consumer。随机的groupId保证，不同节点的consumer都能收到消息。
        multiTenantKafKa.consume(Constants.ADD_PROTOCOL, UUID.randomUUID().toString(), jsonObject -> {
            ProtocolConfig protocolConfig = jsonObject.toJavaObject(ProtocolConfig.class);
            String protocol = protocolConfig.getProtocol();
            if (protocoles.contains(protocol)) {
                log.info("protocol {} consumer has already init, tenant {}", protocol, RpcContext.getContext().getTenantId());
            } else {
                protocoles.add(protocol);
                consume(protocol);
            }
        });
    }
}
