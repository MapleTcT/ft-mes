/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.scheduling.annotation.Async
 */
package com.supcon.supos.suposgateway.filter.analysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;

public class SaveFileWorker {
    private static final Logger LOGGER = LoggerFactory.getLogger(SaveFileWorker.class);
    @Value(value="${url-analysis.root-path}")
    private String rootPath;
    private String[] arrHead = new String[]{"url", "count"};

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Async
    public void saveFile(String tenantId, String filename, Map<String, Integer> dataMap) {
        BufferedWriter bufferedWriter = null;
        try {
            File file = new File(this.rootPath + tenantId + File.separator + filename);
            if (file.exists()) {
                System.out.println("\u5df2\u6709\u540c\u540d\u6587\u4ef6");
                return;
            }
            File parent = file.getParentFile();
            if (!parent.exists() && !parent.mkdirs()) {
                LOGGER.error("tenant url analysis mkdirs error, tenantId: " + tenantId);
                return;
            }
            if (!file.createNewFile()) {
                LOGGER.error("tenant url analysis create new file error, tenantId: " + tenantId);
                return;
            }
            bufferedWriter = new BufferedWriter(new OutputStreamWriter((OutputStream)new FileOutputStream(file), "GB2312"), 1024);
            bufferedWriter.write(String.join((CharSequence)",", this.arrHead));
            bufferedWriter.newLine();
            for (Map.Entry<String, Integer> entry : dataMap.entrySet()) {
                bufferedWriter.write(String.format("%s,%s", entry.getKey(), entry.getValue()));
                bufferedWriter.newLine();
            }
            bufferedWriter.flush();
        }
        catch (Exception e) {
            LOGGER.error("tenant url analysis error, tenantId: " + tenantId, (Throwable)e);
        }
        finally {
            try {
                if (bufferedWriter != null) {
                    bufferedWriter.close();
                }
            }
            catch (Exception e) {
                LOGGER.error("tenant url analysis close bufferedWriter error, tenantId: " + tenantId, (Throwable)e);
            }
        }
    }
}

