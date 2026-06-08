package com.supcon.supfusion.file.server.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.supcon.supfusion.file.server.service.FileServerManager;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.organization.api.PersonApiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class FileServerManagerImpl implements FileServerManager {
    @Autowired
    private PersonApiService personApiService;

    public String getPersonName(Long staffId) {
        String creator = null;
        try {
            Result<JSONObject> person = personApiService.getCurPerson(Long.valueOf(staffId), "name");
            creator = (String) ((Map) person.getData().get("staff")).get("name");
            return creator;
        } catch (Exception e) {
            log.error("获取创建人发生错误,原因：{}", e.getMessage());
            return creator;
        }
    }

}
