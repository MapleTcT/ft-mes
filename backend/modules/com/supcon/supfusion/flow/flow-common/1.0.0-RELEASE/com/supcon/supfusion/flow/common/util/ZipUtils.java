/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import lombok.extern.slf4j.Slf4j;

/**
 * @author: zhuangmh
 * @date: 2020年9月14日 下午2:01:59
 */
@Slf4j
public class ZipUtils {
    
    private ZipUtils() {
        
    }
    
    /**
     * 压缩文件
     * @param zipPath zip目标文件
     * @param files 需要压缩的文件
     * @return
     */
    public static boolean zip(String zipPath, List<File> files) {
        String folderPath = zipPath.substring(0, zipPath.lastIndexOf("/"));
        File folder = new File(folderPath);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(new File(zipPath)))) {
            for (File file : files) {
                try (FileInputStream fis = new FileInputStream(file)) {
                    ZipEntry zipEntry = new ZipEntry(file.getName());
                    zos.putNextEntry(zipEntry);
                    byte[] bytes = new byte[1024];
                    int length;
                    while((length = fis.read(bytes)) >= 0) {
                        zos.write(bytes, 0, length);
                    }
                }
            }
            return true;
        } catch (IOException e) {
            log.error("文件压缩失败,文件路径: {}", zipPath, e);
        }
        return false;
    }
    
    /**
     * zip文件解压
     * @param zipPath 待解压文件
     * @return
     */
    public static boolean unzip(File zipPath, String destFolderPath) {
        byte[] buffer = new byte[1024];
        File folderFile = new File(destFolderPath);
        if (!folderFile.exists()) {
            folderFile.mkdirs();
        }
        try (FileInputStream fis = new FileInputStream(zipPath);ZipInputStream zis = new ZipInputStream(fis)) {
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                File newFile = new File(folderFile, zipEntry.getName());
                try (FileOutputStream fos = new FileOutputStream(newFile)) {
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                    zipEntry = zis.getNextEntry();
                } 
            }
            return true;
        } catch (IOException e) {
            log.error("文件解压异常, 文件: {}", zipPath, e);
        }
        return false;
    }
    
}
