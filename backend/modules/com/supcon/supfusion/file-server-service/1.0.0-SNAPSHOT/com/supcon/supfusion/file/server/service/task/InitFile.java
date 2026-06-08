package com.supcon.supfusion.file.server.service.task;

import com.alibaba.fastjson.JSON;
import com.supcon.supfusion.file.server.common.constants.Constants;
import com.supcon.supfusion.file.server.common.utils.SystemUtil;
import com.supcon.supfusion.file.server.common.utils.ThreadPoolUtils;
import com.supcon.supfusion.file.server.dao.DocumentDao;
import com.supcon.supfusion.file.server.dao.po.DocumentPO;
import com.supcon.supfusion.file.server.service.FileConvertService;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.framework.cloud.common.events.TenantInfo;
import com.supcon.supfusion.framework.cloud.common.events.TenantInfoLocalStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class InitFile implements ApplicationRunner {

    @Autowired
    private DocumentDao documentDao;
    @Autowired
    private FileConvertService fileConvertService;

    @Override
    public void run(ApplicationArguments args) {
        ThreadPoolUtils.getThreadPool().execute(() -> {
            log.info("------------>开始查询待附件预览转换记录");
            if (Constants.LINUX.equalsIgnoreCase(SystemUtil.getOS())) {
                Set<TenantInfo> tenantInfoSet = TenantInfoLocalStorage.getAll();
                log.info("当前租户信息,tenantInfoSet:{}", JSON.toJSONString(tenantInfoSet));
                for (TenantInfo tenantInfo : tenantInfoSet) {
                    RpcContext rpcContext = RpcContext.getContext();
                    rpcContext.setTenantId(tenantInfo.getId());
                    commonFileConvertTask();
                }
            } else {
                commonFileConvertTask();
            }
        });
    }

    public void commonFileConvertTask() {
        log.info("初始化租户,tenantId:{}", RpcContext.getContext().getTenantId());
        List<DocumentPO> documentPOS = documentDao.selectAllByIsNotConvert();
        if (documentPOS != null && documentPOS.size() > 0) {
            documentPOS.forEach(documentPO -> {
                try {
                    Thread.sleep(100L);
                } catch (InterruptedException e) {
                    log.error("初始转换发生错误:{}", e.getMessage());
                }
                fileConvertService.fileConvert(documentPO);
            });
        }
        log.info("租户tenantId:{},初始化转换完成", RpcContext.getContext().getTenantId());
    }

}
