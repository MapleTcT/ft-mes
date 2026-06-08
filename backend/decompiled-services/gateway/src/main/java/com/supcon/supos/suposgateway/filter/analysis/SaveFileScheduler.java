/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.redisson.api.RKeys
 *  org.redisson.api.RMap
 *  org.redisson.api.RedissonClient
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.scheduling.annotation.Scheduled
 */
package com.supcon.supos.suposgateway.filter.analysis;

import com.supcon.supos.suposgateway.filter.analysis.SaveFileWorker;
import com.supcon.supos.suposgateway.filter.analysis.ZipUtil;
import java.io.File;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Map;
import org.redisson.api.RKeys;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;

public class SaveFileScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(SaveFileScheduler.class);
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private SaveFileWorker saveFileWorker;
    @Value(value="${url-analysis.root-path:/var/log/supos-gateway/url-analysis/}")
    private String rootPath;

    @Scheduled(fixedRate=60000L)
    public void onTimer() {
        long now = System.currentTimeMillis();
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(now);
        if (calendar.get(11) != 23 || calendar.get(12) != 58) {
            return;
        }
        if (calendar.get(5) == 1) {
            LOGGER.info("\u5f00\u59cb\u538b\u7f29\u5f52\u6863...");
            this.compressZipFile(calendar);
        }
        boolean needToPullData = false;
        boolean bl = needToPullData = calendar.get(7) == 1;
        if (!needToPullData) {
            calendar.add(5, 1);
            if (calendar.get(5) == 1) {
                needToPullData = true;
            }
            calendar.add(5, -1);
        }
        if (!needToPullData) {
            return;
        }
        LOGGER.info("\u5f00\u59cb\u8bb0\u5f55\u6587\u4ef6...");
        int year = calendar.get(1);
        int month = calendar.get(2) + 1;
        String dateStr = year + "_" + month;
        int day = calendar.get(5);
        String filename = dateStr + "_" + day + ".csv";
        RKeys rKeys = this.redissonClient.getKeys();
        Iterable iterable = rKeys.getKeysByPattern("gateway:url:analysis:" + dateStr + ":*");
        for (String key : iterable) {
            RMap rMap = this.redissonClient.getMap(key);
            Map dataMap = rMap.readAllMap();
            if (dataMap.size() <= 0) continue;
            int index = key.lastIndexOf(58);
            String tenantId = key.substring(index + 1);
            this.saveFileWorker.saveFile(tenantId, filename, dataMap);
        }
    }

    private void compressZipFile(Calendar calendar) {
        calendar.add(5, -1);
        int y = calendar.get(1);
        int m = calendar.get(2) + 1;
        calendar.add(5, 1);
        String ds = y + "_" + m;
        File root = new File(this.rootPath);
        File[] tenantFolders = root.listFiles(File::isDirectory);
        if (tenantFolders == null) {
            return;
        }
        for (File tenantFolder : tenantFolders) {
            File zipFile;
            File[] files = tenantFolder.listFiles(f -> {
                String filename = f.getName();
                return f.isFile() && filename.contains(ds) && filename.endsWith(".csv");
            });
            if (files == null || (zipFile = new File(tenantFolder.getAbsolutePath() + File.separator + ds + ".zip")).exists()) continue;
            try {
                ZipUtil.toZip(Arrays.asList(files), zipFile);
                for (File file : files) {
                    file.delete();
                }
            }
            catch (Exception e) {
                LOGGER.error("tenant url analysis toZip error", (Throwable)e);
                zipFile.delete();
            }
        }
    }
}

