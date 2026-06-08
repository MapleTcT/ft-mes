package com.supcon.supfusion.framework.scaffold.kafka.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.framework.cloud.common.message.MessageHeaderKey;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.SmartMessageConverter;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * @author tomcat
 * @date 20-5-23 下午4:31
 */
public class SupMappingJackson2MessageConverter implements SmartMessageConverter {

    private MappingJackson2MessageConverter messageConverter;

    public SupMappingJackson2MessageConverter(ObjectMapper objectMapper) {
        messageConverter = new MappingJackson2MessageConverter();
        messageConverter.setObjectMapper(objectMapper);
    }

    private void setRpcContext(Message<?> message) {
        MessageHeaders messageHeaders = message.getHeaders();
        if (messageHeaders.containsKey(MessageHeaderKey.TENANT_ID)) {
            RpcContext.getContext().setTenantId(Objects.toString(messageHeaders.get(MessageHeaderKey.TENANT_ID), ""));
        }
        if (messageHeaders.containsKey(MessageHeaderKey.TRACE_ID)) {
            RpcContext.getContext().setTraceId(Long.valueOf(Objects.toString(messageHeaders.get(MessageHeaderKey.TRACE_ID), "0")));
        }
        if (messageHeaders.containsKey(MessageHeaderKey.LANGUAGE)) {
            String language = Objects.toString(messageHeaders.get(MessageHeaderKey.LANGUAGE));
            String lang = language.split("_")[0];
            String country = language.split("_")[1];
            RpcContext.getContext().setLanguage(new Locale(lang, country));
        }
        if (messageHeaders.containsKey(MessageHeaderKey.FROM_SERVICE)) {
            RpcContext.getContext().setFromServiceName(Objects.toString(messageHeaders.get(MessageHeaderKey.FROM_SERVICE), ""));
        }
    }

    private MessageHeaders getMessageHeaders(MessageHeaders headers) {
        Map<String, Object> temp = new HashMap<>();
        headers.forEach((k,v) -> temp.put(k, v));

        RpcContext rpcContext = RpcContext.getContext();
        if (!StringUtils.isEmpty(rpcContext.getTenantId())) {
            temp.put(MessageHeaderKey.TENANT_ID, rpcContext.getTenantId());
        }
        if (rpcContext.getTraceId() != null && rpcContext.getTraceId() > 0) {
            temp.put(MessageHeaderKey.TRACE_ID, rpcContext.getTraceId());
        }
        String language = rpcContext.getLanguage() == null ? Locale.getDefault().toString() : rpcContext.getLanguage().toString();
        temp.put(MessageHeaderKey.LANGUAGE, language);
        if (!StringUtils.isEmpty(rpcContext.getFromServiceName())) {
            temp.put(MessageHeaderKey.FROM_SERVICE, rpcContext.getFromServiceName());
        }

        return new MessageHeaders(temp);
    }

    @Override
    public Object fromMessage(Message<?> message, Class<?> targetClass) {
        setRpcContext(message);
        return messageConverter.fromMessage(message, targetClass);
    }

    @Override
    public Message<?> toMessage(Object payload, MessageHeaders headers) {
        return messageConverter.toMessage(payload, getMessageHeaders(headers));
    }

    @Override
    public Object fromMessage(Message<?> message, Class<?> targetClass, Object conversionHint) {
        setRpcContext(message);
        return messageConverter.fromMessage(message, targetClass, conversionHint);
    }

    @Override
    public Message<?> toMessage(Object payload, MessageHeaders headers, Object conversionHint) {
        return messageConverter.toMessage(payload, getMessageHeaders(headers), conversionHint);
    }
}
