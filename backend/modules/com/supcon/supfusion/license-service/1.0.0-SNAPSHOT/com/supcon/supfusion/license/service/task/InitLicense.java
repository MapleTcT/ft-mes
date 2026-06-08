package com.supcon.supfusion.license.service.task;

import com.supcon.supfusion.license.common.utils.systemutil.SupportOS;
import com.supcon.supfusion.license.common.utils.systemutil.SystemUtils;
import com.supcon.supfusion.license.service.LicenseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 定时任务从nacos获取moduleCode以及licenseKey
 * 根据licenseKey从软件狗获取授权信息，存放至redis
 */
@Component
@Slf4j
public class InitLicense implements ApplicationRunner {

    @Autowired
    private LicenseService licenseService;

    @Value("${license.ip:127.0.0.1}")
    private String ip;

    @Override
    public void run(ApplicationArguments args) throws FileNotFoundException {
        //动态写入网路狗ip,仅在linux系统才需要写入
        boolean isLinux = SystemUtils.getOS().equals(SupportOS.LINUX);
        if (isLinux) {
            initLicenseConfig();
        }
        log.info("CURRENT_SYSTEM:{},LICENSE_CONFIG_IP:{} ====", SystemUtils.getOS(), ip);
        //定时任务刷新授权信息
        licenseService.scheduleRefresh();
    }

    private void initLicenseConfig() {
        FileOutputStream fos = null;
        try {
            //1. 文件夹的路径  文件名
            String directory = "/root/.hasplm/";
            String filename = "hasp_75190.ini";

            //2.  创建文件夹对象     创建文件对象
            File file = new File(directory);
            //如果文件夹不存在  就创建一个空的文件夹
            if (!file.exists()) {
                file.mkdirs();
            }
            File file2 = new File(directory, filename);
            //如果文件不存在  就创建一个空的文件
            if (!file2.exists()) {
                try {
                    file2.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            //3.写入数据
            //创建文件字节输出流
            fos = new FileOutputStream(directory + filename);
            //开始写
            String str = "broadcastsearch=0\n" +
                    "errorlog=1\n" +
                    "serveraddr=" + ip;
            byte[] bytes = str.getBytes();
            //将byte数组中的所有数据全部写入
            fos.write(bytes);
        } catch (IOException e) {
            log.error("写入授权配置文件发生错误", e);
        } finally {
            if (null != fos) {
                //关闭流
                try {
                    fos.close();
                } catch (IOException e) {
                    log.error(e.getMessage());
                }
            }
        }
    }
}
