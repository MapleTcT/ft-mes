package com.supcon.supfusion.i18n.common.until;

import com.supcon.supfusion.i18n.common.config.I18nProperties;
import lombok.extern.slf4j.Slf4j;

/**
 * @Description:
 * @Author: ShenZhiqiang
 * @Date: Create in  10:42 2020/9/21
 * @Modified:
 */
@Slf4j
public class FilePathUtil {

    public static String getFilePath(I18nProperties i18nProperties) {
        String path = null;
        if(System.getProperty("os.name").toLowerCase().contains("win")){
            path = System.getProperty(Constants.USER_DIR) + Constants.PATH;
        }else {
            if (i18nProperties != null && i18nProperties.getFileStoragePath() != null && !i18nProperties.getFileStoragePath().equals(Constants.STR_NO_SPACE)) {
                String fileStoragePath = i18nProperties.getFileStoragePath();
                if (fileStoragePath != null) {
                    fileStoragePath.replace("\\", Constants.PATH);
                }
                if (fileStoragePath != null && !fileStoragePath.endsWith(Constants.PATH)) {
                    fileStoragePath = fileStoragePath + Constants.PATH;
                }
                path = fileStoragePath;
            } else {
                path = System.getProperty(Constants.USER_DIR) + Constants.PATH;
            }
        }
        return path;
    }
}
