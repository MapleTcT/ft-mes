package com.supcon.supfusion.notification.sms.service.impl;

import com.supcon.supfusion.notification.common.CachabledOptional;
import com.supcon.supfusion.notification.protocol.common.ReadStatus;
import com.supcon.supfusion.notification.protocol.common.SendStatus;
import com.supcon.supfusion.notification.protocol.model.Ack;
import com.supcon.supfusion.notification.protocol.model.AckResult;
import com.supcon.supfusion.notification.protocol.model.Notice;
import com.supcon.supfusion.notification.protocol.model.Receiver;
import com.supcon.supfusion.notification.sms.dao.entities.SmsEntity;
import com.supcon.supfusion.notification.sms.dao.mappers.SmsEntityMapper;
import com.supcon.supfusion.notification.sms.service.SmsService;
import com.supcon.supfusion.notification.sms.service.runner.ConfigRunner;
import lombok.extern.slf4j.Slf4j;
import net._139130.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @Author kk.C
 * @Description 金仓短信接口实现
 * @Date 2021/4/22 9:58
 * @Param
 * @return
 **/
@Service
@Slf4j
public class SmsServiceImpl implements SmsService {

    @Autowired
    private SmsEntityMapper smsEntityMapper;
    //    private static final ObjectFactory obf = new ObjectFactory();
    //    Optional<Pair<String, WebServiceSoap>> wsdlChangeAbleWs = Optional.empty();
    //    CachabledOptional<Triple<String, String, String>> wsdlCacheAbleConfig = CachabledOptional.<Triple<String, String, String>>empty().setExpireMs(10 * 1000L);

    @Override
    public Ack send(Notice notice) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            List<AckResult> ackResults = new ArrayList<>();
            SmsEntity sms = new SmsEntity();
            String sender;
            for (Receiver receiver : notice.getReceivers()) {
                AckResult ackResult = new AckResult();
                if (!StringUtils.isEmpty(receiver.getAddress())) {
                    sms.setReceiver(receiver.getAddress());
                    sms.setContent(notice.getContent());
                    sms.setInpool("0");
                    sms.setSendTime(sdf.format(new Date()));
                    sms.setInsertTime(sms.getSendTime());
                    sms.setModuleId("COM1");
                    sms.setSendmode("2");
                    if (null != notice.getBsmodName()) {
                        sender = notice.getBsmodName();
                    } else {
                        sender = "ADP消息中心";
                    }
                    sms.setSender(sender);
                    log.info("save sms entity record to db");
                    smsEntityMapper.insert(sms);
                    log.info("save sms entity record to db success");
                    ackResult.setMessageId(receiver.getMessageId());
                    ackResult.setReadStatus(ReadStatus.UNKOWN);
                    ackResult.setSendStatus(SendStatus.SUCCESS);
                } else {
                    ackResult.setMessageId(receiver.getMessageId());
                    ackResult.setReadStatus(ReadStatus.UNKOWN);
                    ackResult.setSendStatus(SendStatus.FAIL);
                    ackResult.setErrorMessage("mobile is not exit");
                }
                ackResults.add(ackResult);
            }
            return Ack.builder().results(ackResults).build();
        } catch (Exception e) {
            log.info(e.getMessage(), e);
            return Ack.buildFail(notice, "send mobile failed", ReadStatus.UNKOWN, SendStatus.FAIL);
        }

    }

    //    @Override
    //    public Ack send(Notice notice) {
    //
    //        ArrayOfMessageData messageDatas = new ArrayOfMessageData();
    //        MTPacks pack = new MTPacks();
    //
    //        MessageData messageData = null;
    //        List<AckResult> ackResults = new ArrayList<>();
    //        for (Receiver receiver : notice.getReceivers()) {
    //            messageData = new MessageData();
    //            messageData.setContent(obf.createMessageDataContent(notice.getContent()));
    //            messageData.setPhone(obf.createMessageDataPhone(receiver.getAddress()));
    //            messageData.setVipFlag(true);
    //            messageDatas.getItem().add(messageData);
    //
    //            if (StringUtils.isEmpty(receiver.getAddress())) {
    //                AckResult ackResult = new AckResult();
    //                ackResult.setMessageId(receiver.getMessageId());
    //                ackResult.setReadStatus(ReadStatus.UNREAD);
    //                ackResult.setSendStatus(SendStatus.FAIL);
    //                ackResult.setErrorMessage("短信发送，手机号未绑定");
    //                ackResults.add(ackResult);
    //            } else {
    //                AckResult ackResult = new AckResult();
    //                ackResult.setMessageId(receiver.getMessageId());
    //                ackResult.setReadStatus(ReadStatus.UNKOWN);
    //                ackResult.setSendStatus(SendStatus.SUCCESS);
    //                ackResult.setErrorMessage("");
    //                ackResults.add(ackResult);
    //            }
    //
    //        }
    //
    //
    //        pack.setMsgs(messageDatas);
    //
    //        String batchId = UUID.randomUUID().toString();
    //        //1短信发送 2彩信发送
    //        pack.setMsgType(1);
    //        //0群发 1 组发      ***********确定是群发还是组发
    //        pack.setSendType(0);
    //        pack.setMsgs(messageDatas);
    //        //是否过滤重复号码
    //        pack.setDistinctFlag(true);
    //        pack.setBatchID(batchId);
    //        //扩展号
    //        pack.setCustomNum("13801");
    //
    //        Triple<String, String, String> config = wsdlCacheAbleConfig.orElseGet(() -> {
    //            Triple<String, String, String> c = configRunner.getSmsConfig();
    //            log.info("get config from remote {}", c);
    //            return c;
    //        });
    //        if (Objects.isNull(config)) {
    //        }
    //        WebServiceSoap soap = null;
    //        if (wsdlChangeAbleWs.isPresent()) {
    //            if (Objects.equals(config.getLeft(), wsdlChangeAbleWs.get().getLeft())) {
    //                soap = wsdlChangeAbleWs.get().getRight();
    //            }
    //        } else {
    //            URL wsdlLocation = null;
    //            try {
    //                wsdlLocation = new URL(config.getLeft());
    //            } catch (MalformedURLException e) {
    //                log.error("Invalid WSDL url {}", config.getLeft());
    //            }
    //            WebService webService = new WebService(wsdlLocation);
    //            wsdlChangeAbleWs = Optional.ofNullable(Pair.of(config.getLeft(), soap = webService.getWebServiceSoap()));
    //
    //        }
    //        if (soap == null) {
    //            soap = new WebService().getWebServiceSoap();
    //            log.warn("use default soap... {}");
    //        }
    //
    //        try {
    //            GsmsResponse unusedResponse = soap
    //                    .post(config.getMiddle(), config.getRight(), pack);
    //        } catch (Exception e) {
    //            log.error("sms send fail e={}", ExceptionUtils.getStackTrace(e));
    //        }
    //        return Ack.builder().results(ackResults).build();
    //    }

}
