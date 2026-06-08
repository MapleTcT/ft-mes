package com.supcon.supfusion.file.server.service.config;

import com.supcon.supfusion.file.server.common.utils.SystemUtils;
import com.supcon.supfusion.file.server.service.FileService;
import com.supcon.supfusion.file.server.service.impl.MinIOServiceImpl;
import io.minio.MinioClient;
import io.minio.errors.InvalidEndpointException;
import io.minio.errors.InvalidPortException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @ClassName: MinioConfig
 * @Description:
 * @author: sunmingming
 * @Date: 2020/9/15 13:26
 * @Version: 1.0
 */
@Configuration
@Slf4j
public class FileServerConfig {

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.accessKey}")
    private String accessKey;

    @Value("${minio.secretKey}")
    private String secretKey;

    @Bean
    public MinioClient minioClient() throws InvalidPortException, InvalidEndpointException, UnknownHostException {
        boolean linux = SystemUtils.getOS().equals("LINUX");
        log.info("osName:{}", System.getProperty("os.name"));
        log.info("linux:{}",linux);
        //windows环境下
        if (!linux) {
            InetAddress addr = InetAddress.getLocalHost();
            String hostAddress = addr.getHostAddress();
            String endpoint = "http://" + hostAddress + ":30200";
            return new MinioClient(endpoint, accessKey, secretKey);
        }
        return new MinioClient(endpoint, accessKey, secretKey);
    }

    @Bean
    public FileService fileService() {
        return new MinIOServiceImpl();
    }

}
