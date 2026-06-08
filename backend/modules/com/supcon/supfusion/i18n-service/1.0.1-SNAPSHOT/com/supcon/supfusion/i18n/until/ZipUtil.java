package com.supcon.supfusion.i18n.until;

import com.supcon.supfusion.i18n.common.until.Constants;
import com.supcon.supfusion.i18n.common.until.MyFileUtils;
import com.supcon.supfusion.i18n.manager.service.I18nManagerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.*;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.*;

/**
 * ZIP操作
 */
@Slf4j
@Component
public class ZipUtil {
    private static ZipUtil zipUtil;
    @Autowired
    private I18nManagerService i18nManagerService;

    @PostConstruct
    public void init() {
        zipUtil = this;
        zipUtil.i18nManagerService = this.i18nManagerService;
    }

    /**
     * 解压到指定目录
     */
    public static void unZipFiles(String zipPath, String descDir) {
        unZipFiles(new File(zipPath), descDir);
    }

    /**
     * 解压文件到指定目录
     */
    @SuppressWarnings("rawtypes")
    public static void unZipFiles(File zipFile, String descDir) {
        File pathFile = new File(descDir);
        if (!pathFile.exists()) {
            pathFile.mkdirs();
        }
        //解决zip文件中有中文目录或者中文文件
        try (ZipFile zip = new ZipFile(zipFile, Charset.forName("UTF-8"))) {
            for (Enumeration entries = zip.entries(); entries.hasMoreElements(); ) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                String zipEntryName = entry.getName();
                String outPath = (descDir + zipEntryName).replaceAll("\\*", "/");
                //判断路径是否存在,不存在则创建文件路径
                File file = new File(outPath.substring(0, outPath.lastIndexOf('/')));
                if (!file.exists()) {
                    file.mkdirs();
                }
                //判断文件全路径是否为文件夹,如果是上面已经上传,不需要解压
                if (new File(outPath).isDirectory()) {
                    continue;
                }
                try (InputStream in = zip.getInputStream(entry);
                     OutputStream out = new FileOutputStream(outPath);) {
                    byte[] buf1 = new byte[1024];
                    int len;
                    while ((len = in.read(buf1)) > 0) {
                        out.write(buf1, 0, len);
                    }
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }


    /**
     * 解压文件到指定目录
     */
    @SuppressWarnings("rawtypes")
    public static void unZipFiles2(File zipFile, String descDir) {
        File pathFile = new File(descDir);
        if (!pathFile.exists()) {
            pathFile.mkdirs();
        }
        //解决zip文件中有中文目录或者中文文件
        try (ZipFile zip = new ZipFile(zipFile, Charset.forName("UTF-8"))) {
            for (Enumeration entries = zip.entries(); entries.hasMoreElements(); ) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                if (!entry.getName().endsWith("/")) {
                    //把是所有文件解压到同一个目录下
                    // 如果文件名中 含有下划线 “_” 再将这个文件解压到临时目录用于入库
                    if (entry.getName().contains(Constants.STR_LINE)) {
                        String filename = entry.getName().substring(entry.getName().length() - 16);
                        File f = new File(descDir + Constants.PATH + filename);
                        try (InputStream in = zip.getInputStream(entry);
                             OutputStream out = new FileOutputStream(f)) {
                            byte[] buf1 = new byte[1024];
                            int len;
                            while ((len = in.read(buf1)) > 0) {
                                out.write(buf1, 0, len);
                            }
                        } catch (Exception e) {
                            log.error(e.getMessage());
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    @SuppressWarnings("rawtypes")
    public static void unzipToI18nDir2(File zipFile, String i18nDir, Boolean useAllModuleCode) {
        File pathFile = new File(i18nDir);
        if (!pathFile.exists()) {
            pathFile.mkdirs();
        }
        try (ZipFile zip = new ZipFile(zipFile, Charset.forName("UTF-8"))) {
            for (Enumeration entries = zip.entries(); entries.hasMoreElements(); ) {
                ZipEntry zipEntry = (ZipEntry) entries.nextElement();
                if (!zipEntry.getName().endsWith("/")) {
                    //把是所有文件解压到同一个目录下
                    String filename = zipEntry.getName().substring(zipEntry.getName().indexOf(Constants.PATH, Constants.ONE_INT) + 1);
                    //获取模块名 校验模块名正确是否
                    String moduleCode = Constants.STR_NO_SPACE;
                    if (filename.contains(Constants.PATH)) {
                        String[] arr = filename.split(Constants.PATH);
                        filename = arr[arr.length - 1];
                    }
                    if (filename.contains(Constants.STR_LINE)) {
                        moduleCode = filename.substring(filename.indexOf(Constants.PATH, Constants.ONE_INT) + 1, filename.indexOf(Constants.STR_LINE, Constants.ONE_INT));
                    } else {
                        moduleCode = filename.substring(filename.indexOf(Constants.PATH, Constants.ONE_INT) + 1, filename.indexOf(Constants.STR_POINT, Constants.ONE_INT));
                    }
                    List<String> modules;
                    if (useAllModuleCode) {
                         modules = zipUtil.i18nManagerService.getAllModuleCode();
                    } else {
                         modules = zipUtil.i18nManagerService.getModuleEnumModuleCode();
                    }
                    if (modules != null && modules.contains(moduleCode)) {
                        // 遍历 i18n 下的文件夹 找到同模块名相同的 文件夹路径 如果不存在就新建  destDir
                        //String destDir = MyFileUtils.findDir(i18nDir, moduleCode);
                        //将该文件复制到 destDir 目录下
                        MyFileUtils.createDir(i18nDir + Constants.PATH);
                        //File f = new File(destDir + Constants.PATH + filename);
                        File f = new File(i18nDir + Constants.PATH + filename);
                        if (!f.exists()) {
                            if (!f.createNewFile()) {
                                log.error(f.getName() + Constants.CREATE_ERROR);
                            }
                        }
                        try (InputStream in = zip.getInputStream(zipEntry);
                             FileOutputStream out = new FileOutputStream(f)) {
                            byte[] buf = new byte[1024];
                            int len = -1;
                            while ((len = in.read(buf)) != -1) {
                                // 直到读到该条目的结尾
                                out.write(buf, 0, len);
                            }
                            out.flush();
                        } catch (IOException e) {
                            log.error(e.getMessage());
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}

