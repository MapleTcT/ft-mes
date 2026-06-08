/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.core.io.ClassPathResource
 *  org.springframework.core.io.FileSystemResource
 *  org.springframework.util.ResourceUtils
 */
package com.supcon.greendill.base.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.util.ResourceUtils;

public class JSONHelper {
    private static final Logger log = LoggerFactory.getLogger(JSONHelper.class);

    public String ResolveJsonFileToString(String filename) throws IOException {
        BufferedReader br = null;
        String result = null;
        br = new BufferedReader(new InputStreamReader(this.getResFileStream(filename), "UTF-8"));
        StringBuffer message = new StringBuffer();
        String line = null;
        while ((line = br.readLine()) != null) {
            message.append(line);
        }
        if (br != null) {
            br.close();
        }
        String defaultString = message.toString();
        result = defaultString.replace("\r\n", "").replaceAll(" +", "");
        return result;
    }

    private File getResFile(String filename) throws FileNotFoundException {
        FileSystemResource resource;
        File file = new File(filename);
        if (!file.exists()) {
            log.debug("\u4e0d\u5728\u540c\u7ea7\u76ee\u5f55\uff0c\u8fdb\u5165config\u76ee\u5f55\u67e5\u627e");
            file = new File("config/" + filename);
        }
        if (!(resource = new FileSystemResource(file)).exists()) {
            log.debug("\u4e0d\u5728config\u76ee\u5f55\uff0c\u8fdb\u5165classpath\u76ee\u5f55\u67e5\u627e");
            file = ResourceUtils.getFile((String)("classpath:" + filename));
        }
        return file;
    }

    private InputStream getResFileStream(String filename) throws IOException {
        Object fin = null;
        ClassPathResource fileRource = new ClassPathResource(filename);
        return fileRource.getInputStream();
    }
}

