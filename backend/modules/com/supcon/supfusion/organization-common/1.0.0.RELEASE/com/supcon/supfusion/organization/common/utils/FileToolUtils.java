package com.supcon.supfusion.organization.common.utils;

import com.supcon.supfusion.organization.common.exception.OrganizationErrorEnum;
import com.supcon.supfusion.organization.common.exception.OrganizationException;

import java.io.File;
import java.io.IOException;

public class FileToolUtils {

    /**
     * 创建文件夹
     * @param newPath
     */
    public static void createDir(String newPath) {
        File target = new File(newPath);
        if (!target.exists()) {
            target.mkdirs();
        }
    }

    /**
     * 创建文件
     * @param filePath
     * @return
     */
    public static File createFile(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            try {
                // 解决多级目录创建失败的问题
                File parentFile = file.getParentFile();
                if (!parentFile.exists()){
                    parentFile.mkdirs();
                }
                if (!file.exists()){
                    file.createNewFile();
                }
            } catch (IOException e) {
                throw new OrganizationException(OrganizationErrorEnum.FILE_CREATE_ERROR);
            }
        }
        return file;
    }
}
