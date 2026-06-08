package com.supcon.supfusion.notification.engine.dispatcher;

import com.supcon.supfusion.notification.engine.dispatcher.protocol.RemoteProtocol;
import com.supcon.supfusion.notification.protocol.Protocol;
import com.supcon.supfusion.notification.protocol.model.Ack;
import com.supcon.supfusion.notification.protocol.model.Notice;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class ProtocolCache implements ApplicationContextAware {
    private Map<String, Protocol> localCache = new HashMap<>();
    private Object lock = new Object();
    private ApplicationContext applicationContext;

    @Autowired
    private RemoteProtocol remoteProtocol;

    /**
     * 往通知APP分发消息
     *
     * @param protocol
     * @param notice
     */
    @Transactional
    public Ack send(String protocol, Notice notice) {
        Protocol localProtocol = localCache.get(protocol);
        if (localProtocol != null) {
            return localProtocol.send(notice);
        } else {
            return remoteProtocol.send(notice);
        }

        /**
         * mock data
         */
//        Ack ack = new Ack();
//        List<AckResult> ackResults = new ArrayList();
//        AckResult ackResult = new AckResult();
//        ackResult.setMessageId(notice.getReceivers().get(0).getMessageId());
//        ackResult.setReadStatus(ReadStatus.READ);
//        ackResult.setSendStatus(SendStatus.FAIL);
//        ackResult.setErrorMessage("xxxxxxxxxxxxxxxxxxxx");
//        ackResults.add(ackResult);
//        ack.setResults(ackResults);
//        return ack;
    }

    public void addLocalProtocol(String protocol) {
        Protocol protocolBean = applicationContext.getBean(protocol, Protocol.class);
        synchronized (lock) {
            localCache.put(protocol, protocolBean);
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }


}
