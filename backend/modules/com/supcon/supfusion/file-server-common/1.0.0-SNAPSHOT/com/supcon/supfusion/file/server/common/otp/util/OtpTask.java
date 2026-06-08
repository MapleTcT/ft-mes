package com.supcon.supfusion.file.server.common.otp.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.*;

@Slf4j
@Component
public class OtpTask {
    //定时任务-删除没有用的验证码文件
//    @Scheduled(cron = "0 0/15 *  *  * ?")
    public void scheduledDeleteOtpTempFile() {
        log.info("=====执行otp code file 清理任务");
        String path = System.getProperty(OtpConstants.USER_DIR) + OtpConstants.PATH;
        File file = new File(path);
        if (file != null && file.listFiles().length > 0) {
            File[] files = file.listFiles();
            for (File file1 : files) {
                if (file1 != null) {
                    Map map = PropertiesFileUtil.readValue(file1.getAbsolutePath());
                    if (map != null && map.size() > 0) {
                        if (System.currentTimeMillis() - OtpConstants.TIME * 1000 > Long.valueOf(PropertiesFileUtil.getKeyOrNull(map))) {
                            file1.delete();
                            if (file1.exists()) {
                                log.error(file1.getName() + "file： delete is fail");
                            }
                        }
                    }
                }
            }
        }
    }

}
