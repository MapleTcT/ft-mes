/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.supcon.orchid.entityconf.entities.DeploymentLog
 *  com.supcon.orchid.entityconf.services.StartProgressService
 *  com.supcon.orchid.entityconf.services.impl.ModuleDeployService
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.stereotype.Service
 */
package com.supcon.orchid.entityconf.service.imps;

import com.supcon.orchid.entityconf.entities.DeploymentLog;
import com.supcon.orchid.entityconf.services.LogListenService;
import com.supcon.orchid.entityconf.services.StartProgressService;
import com.supcon.orchid.entityconf.services.impl.ModuleDeployService;
import java.io.File;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LogListenServiceImpl
implements LogListenService {
    @Value(value="${logPath:''}")
    private String codeLogPath;
    public static long pointer = 0L;
    @Autowired
    private StartProgressService startProgressService;
    @Autowired
    private ModuleDeployService moduleDeployService;

    @Override
    public Map<String, Object> getProgressiveLog() {
        DeploymentLog deploymentLog = this.moduleDeployService.getDeploymentLog();
        if (null == deploymentLog) {
            return null;
        }
        String taskId = deploymentLog.getDeploymentId();
        String path = this.codeLogPath + File.separator + "publish" + File.separator + taskId + ".log";
        HashMap<String, Object> jsonMap = new HashMap<String, Object>(8);
        jsonMap.put("success", true);
        StringBuilder sb = new StringBuilder();
        int hybird = 0;
        pointer = 0L;
        try {
            File file = new File(path);
            if (file.exists()) {
                RandomAccessFile raf = new RandomAccessFile(file, "r");
                raf.seek(pointer);
                String line = null;
                boolean isError = false;
                while ((line = raf.readLine()) != null) {
                    if (line.contains("FAILURE_")) {
                        jsonMap.put("success", false);
                        isError = true;
                    }
                    if (!isError) {
                        sb.append(line + "<br>");
                        continue;
                    }
                    sb.append("<p style=\"color:red\">" + line + "</p><br> ");
                    break;
                }
                hybird = this.startProgressService.getFinishFlag() != false ? 100 : this.startProgressService.getCurrentProgress();
                pointer = raf.getFilePointer();
                raf.close();
                String message = new String(sb.toString().getBytes("iso-8859-1"), "UTF-8");
                jsonMap.put("message", message);
                jsonMap.put("userName", deploymentLog.getUserName());
                jsonMap.put("point", hybird);
                jsonMap.put("msName", deploymentLog.getServiceName());
                jsonMap.put("taskId", taskId);
            } else {
                jsonMap.put("message", "");
                jsonMap.put("point", 0);
            }
        }
        catch (Exception e) {
            jsonMap.put("success", false);
            jsonMap.put("message", e.getMessage());
        }
        return jsonMap;
    }
}

