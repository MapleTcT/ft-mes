/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.taskcenter.service;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.supcon.supfusion.flow.common.vo.webapi.HelperResponseVO;
import com.supcon.supfusion.flow.common.vo.webapi.KeyValuePair;
import com.supcon.supfusion.framework.cloud.i18n.resource.utils.MessageResourceWrapper;

import lombok.extern.slf4j.Slf4j;

/**
 * @author: zhuangmh
 * @date: 2020年6月17日 上午10:16:20
 */
@Service
@Slf4j
public class HelperService {
    
    @Autowired(required = false)
    private MessageResourceWrapper messageSource;
    
    /**
     * 
     * @return
     */
    public HelperResponseVO getHelpDoc() {
        List<KeyValuePair<String>> helpers = new LinkedList<>();
        Properties properties = new Properties();
        try {
            properties.load(this.getClass().getClassLoader().getResourceAsStream("i18n/workflow/workflow.properties"));
        } catch (IOException e) {
            log.error("读取帮助文档本地资源失败", e);
            return new HelperResponseVO(helpers);
        }
        Set<Object> keySet = properties.keySet();
        for (Object key : keySet) {
            String message = messageSource.getMessageNotBlank(key.toString());
            helpers.add(new KeyValuePair<String>(key.toString(), message));
        }
        return new HelperResponseVO(helpers);
    }
}
