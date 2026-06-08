package com.supcon.supfusion.i18n.until;


import com.supcon.supfusion.framework.cloud.common.supports.Charsets;
import com.supcon.supfusion.i18n.common.until.Constants;
import com.supcon.supfusion.i18n.manager.service.I18nManagerService;
import lombok.extern.slf4j.Slf4j;

import java.io.*;

import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipOutputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * ZIP操作
 */
@Slf4j
@Component
public class ResourceZipWrapper {

    private static ResourceZipWrapper resourceZipWrapper;
    @Autowired
    private I18nManagerService i18nManagerService;

    @PostConstruct
    public void init() {
        resourceZipWrapper = this;
        resourceZipWrapper.i18nManagerService = this.i18nManagerService;
    }

    /**
     * 写zip文件
     *
     * @param sourceFile
     * @param zipFullName
     * @return
     */
    public static File createZip(File sourceFile, String zipFullName) {
        try (FileOutputStream fos = new FileOutputStream(zipFullName);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            zos.setEncoding(Charsets.UTF_8_NAME);
            writeZip(sourceFile, "", zos);
            File zipFile = new File(zipFullName);
            if (zipFile.exists()) {
                return zipFile;
            }
        } catch (FileNotFoundException e) {
            log.error("create zip file fail! ", e);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        return null;
    }

    /**
     * 递归遍历目录压缩文件
     *
     * @param file
     * @param parentDir
     * @param zos
     */
    private static void writeZip(File file, String parentDir, ZipOutputStream zos) {
        if (file.exists()) {
            if (file.isDirectory()) {
                parentDir += file.getName();
                if (!parentDir.endsWith(File.separator)) {
                    parentDir += File.separator;
                }
                File[] files = file.listFiles();
                if (files.length != 0) {
                    for (File f : files) {
                        writeZip(f, parentDir, zos);
                    }
                } else {
                    try {
                        zos.putNextEntry(new ZipEntry(parentDir));
                    } catch (IOException e) {
                        log.error(e.getMessage());
                    }
                }
            } else {
                try (FileInputStream fis = new FileInputStream(file)) {
                    ZipEntry ze = new ZipEntry(parentDir + file.getName());
                    zos.putNextEntry(ze);
                    byte[] content = new byte[1024];
                    int len;
                    while ((len = fis.read(content)) != -1) {
                        zos.write(content, 0, len);
                        zos.flush();
                    }
                } catch (FileNotFoundException e) {
                    log.error(Constants.ZIP_CREATE_FAIL, e);
                } catch (IOException e) {
                    log.error(Constants.ZIP_CREATE_FAIL, e);
                }
            }
        }
    }
}

