/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package com.supcon.supos.suposgateway.filter.analysis;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZipUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(ZipUtil.class);

    public static void toZip(List<File> srcFiles, File zipFile) throws Exception {
        if (zipFile == null) {
            LOGGER.error("\u538b\u7f29\u5305\u6587\u4ef6\u540d\u4e3a\u7a7a\uff01");
            return;
        }
        if (!zipFile.getName().endsWith(".zip")) {
            LOGGER.error("\u538b\u7f29\u5305\u6587\u4ef6\u540d\u5f02\u5e38\uff0czipFile={}", (Object)zipFile.getPath());
            return;
        }
        FileOutputStream out = new FileOutputStream(zipFile);
        ZipOutputStream zos = new ZipOutputStream(out);
        for (File srcFile : srcFiles) {
            int len;
            byte[] buf = new byte[1024];
            zos.putNextEntry(new ZipEntry(srcFile.getName()));
            FileInputStream in = new FileInputStream(srcFile);
            while ((len = in.read(buf)) != -1) {
                zos.write(buf, 0, len);
            }
            zos.closeEntry();
            in.close();
        }
        zos.close();
    }
}

