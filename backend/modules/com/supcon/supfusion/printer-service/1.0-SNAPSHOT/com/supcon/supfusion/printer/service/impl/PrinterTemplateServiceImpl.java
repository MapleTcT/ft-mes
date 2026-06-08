package com.supcon.supfusion.printer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.file.server.api.BapFileService;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;
import com.supcon.supfusion.printer.common.Constants;
import com.supcon.supfusion.printer.config.InternationalResource;
import com.supcon.supfusion.printer.dao.mapper.PrinterDesignContentMapper;
import com.supcon.supfusion.printer.dao.mapper.PrinterTemplateMapper;
import com.supcon.supfusion.printer.dao.mapper.PrinterTemplateRelationPageMapper;
import com.supcon.supfusion.printer.dao.po.PrinterDesignContentPO;
import com.supcon.supfusion.printer.dao.po.PrinterRegisterPO;
import com.supcon.supfusion.printer.dao.po.PrinterTemplatePO;
import com.supcon.supfusion.printer.dao.po.PrinterTemplateRelationPagePO;
import com.supcon.supfusion.printer.service.PrinterTemplateService;
import com.supcon.supfusion.printer.service.bo.*;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@Transactional(rollbackFor = Exception.class)
public class PrinterTemplateServiceImpl extends ServiceImpl<PrinterTemplateMapper, PrinterTemplatePO> implements PrinterTemplateService {
    @Autowired
    private PrinterTemplateRelationPageMapper printerTemplateRelationPageMapper;

    @Autowired
    private PrinterDesignContentMapper printerDesignContentMapper;

    @Autowired
    BapFileService bapFileService;

    @Override
    public Integer templateCodeCount(String templateCode) {
        QueryWrapper<PrinterTemplatePO> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(PrinterTemplatePO::getTemplateCode, templateCode)
                .eq(PrinterTemplatePO::getValid, Constants.VALID);
        return count(wrapper);
    }

    @Override
    public Long addPrinterTemplate(PrinterTemplateAddBO printerTemplateAddBO) {
        PrinterTemplatePO printerTemplatePO = new PrinterTemplatePO();
        BeanUtils.copyProperties(printerTemplateAddBO, printerTemplatePO);
        // 默认不启用
        printerTemplatePO.setEnabled(2);
        save(printerTemplatePO);
        InternationalResource.initiativeRefreshCache();
        return printerTemplatePO.getId();
    }

    @Override
    public void updatePrinterTemplate(PrinterTemplateUpdateBO printerTemplateUpdateBO) {
        PrinterTemplatePO printerTemplatePO = new PrinterTemplatePO();
        BeanUtils.copyProperties(printerTemplateUpdateBO, printerTemplatePO);
        updateById(printerTemplatePO);
        List<PrinterTemplateRelationPageBO> pageDatas = printerTemplateUpdateBO.getPageDatas();
        LambdaUpdateWrapper updateWrapper = new UpdateWrapper<PrinterTemplateRelationPagePO>().lambda().eq(PrinterTemplateRelationPagePO :: getTemplateId ,printerTemplateUpdateBO.getId());
        printerTemplateRelationPageMapper.delete(updateWrapper);
        if(!CollectionUtils.isEmpty(pageDatas)){
            pageDatas.forEach(p -> {
                PrinterTemplateRelationPagePO printerTemplateRelationPagePO = new PrinterTemplateRelationPagePO();
                BeanUtils.copyProperties(p, printerTemplateRelationPagePO);
                printerTemplateRelationPageMapper.insert(printerTemplateRelationPagePO); });
        }
        InternationalResource.initiativeRefreshCache();
    }

    @Override
    public void copyPrinterTemplate(PrinterTemplateUpdateBO printerTemplateUpdateBO) {
        //toDO copy printer_design_content

        PrinterTemplatePO printerTemplatePO = new PrinterTemplatePO();
        BeanUtils.copyProperties(printerTemplateUpdateBO, printerTemplatePO);

        QueryWrapper<PrinterDesignContentPO> designWrapper = new QueryWrapper<>();
        designWrapper.lambda().eq(PrinterDesignContentPO::getTemplateId, printerTemplateUpdateBO.getId()).eq(PrinterDesignContentPO::getValid, true);
        PrinterDesignContentPO printerDesignContentPO = printerDesignContentMapper.selectOne(designWrapper);

        printerTemplatePO.setId(null);
        save(printerTemplatePO);

        if (printerDesignContentPO != null) {
            printerDesignContentPO.setTemplateId(printerTemplatePO.getId());
            printerDesignContentMapper.insert(printerDesignContentPO);
        }

        List<PrinterTemplateRelationPageBO> pageDatas = printerTemplateUpdateBO.getPageDatas();
        if(!CollectionUtils.isEmpty(pageDatas)){
            pageDatas.forEach(p -> {
                PrinterTemplateRelationPagePO printerTemplateRelationPagePO = new PrinterTemplateRelationPagePO();
                printerTemplateRelationPagePO.setPageId(p.getPageId());
                printerTemplateRelationPagePO.setTemplateId(printerTemplatePO.getId());
                printerTemplateRelationPagePO.setModelCode(p.getModelCode());
                printerTemplateRelationPageMapper.insert(printerTemplateRelationPagePO);
            });
        }
        InternationalResource.initiativeRefreshCache();
    }

    @Override
    public void deleteBatchPrinterTemplates(List<Long> templateIds) {
        if (!templateIds.isEmpty()) {
            UpdateWrapper<PrinterTemplatePO> updateWrapper = new UpdateWrapper<>();
            int i = 0;
            for (Long templateId : templateIds) {
                if (i == 0) {
                    updateWrapper.lambda().eq(PrinterTemplatePO::getId, templateId);
                    continue;
                }
                updateWrapper.lambda().or().eq(PrinterTemplatePO::getId, templateId);
                i ++;
            }
            PrinterTemplatePO printerTemplatePO = new PrinterTemplatePO();
            printerTemplatePO.setValid(Constants.INVALID);
            this.update(printerTemplatePO, updateWrapper);
        }
    }

    @Override
    public Page queryPrinterTemplateListByAppId(PrinterTemplatePageQueryBO printerTemplatePageQueryBO) {
        LambdaQueryWrapper queryWrapper = new QueryWrapper<PrinterTemplatePO>().lambda()
                .eq(PrinterTemplatePO :: getAppId ,printerTemplatePageQueryBO.getAppId()).eq(PrinterTemplatePO::getValid, Constants.VALID)
                .orderByDesc(PrinterTemplatePO::getCreateTime);
//        List<PrinterTemplatePO>  printerTemplatePOList = list(queryWrapper);
        Page page = new Page<>(printerTemplatePageQueryBO.getPageNum(), printerTemplatePageQueryBO.getPageSize());
        page(page, queryWrapper);
        List<PrinterTemplatePO> printerTemplatePOList = page.getRecords();
        List<PrinterTemplateUpdateBO> printerTemplateUpdateBOList = new ArrayList<>();
        if(!CollectionUtils.isEmpty(printerTemplatePOList)){
            printerTemplatePOList.forEach(p -> {
                PrinterTemplateUpdateBO printerTemplateUpdateBO = new PrinterTemplateUpdateBO();
                BeanUtils.copyProperties(p, printerTemplateUpdateBO);
                LambdaQueryWrapper queryWrapperRelation = new QueryWrapper<PrinterTemplateRelationPagePO>().lambda().eq(PrinterTemplateRelationPagePO :: getTemplateId ,p.getId());
                List<PrinterTemplateRelationPagePO> printerTemplateRelationPagePOList = printerTemplateRelationPageMapper.selectList(queryWrapperRelation);
                List<PrinterTemplateRelationPageBO> printerTemplateRelationPageBOList = new ArrayList<>();
                if(!CollectionUtils.isEmpty(printerTemplateRelationPagePOList)){
                    printerTemplateRelationPagePOList.forEach(r -> {
                        PrinterTemplateRelationPageBO printerTemplateRelationPageBO = new PrinterTemplateRelationPageBO();
                        BeanUtils.copyProperties(r, printerTemplateRelationPageBO);
                        printerTemplateRelationPageBOList.add(printerTemplateRelationPageBO);
                    });
                }
                printerTemplateUpdateBO.setPageDatas(printerTemplateRelationPageBOList);
                printerTemplateUpdateBOList.add(printerTemplateUpdateBO);
            });
        }
        page.setRecords(printerTemplateUpdateBOList);
        return page;
    }

    @Override
    public PrinterTemplateUpdateBO queryPrinterTemplateListByTemplateId(Long templateId) {
        LambdaQueryWrapper queryWrapperTemplate = new QueryWrapper<PrinterTemplatePO>().lambda()
                .eq(PrinterTemplatePO :: getId ,templateId).eq(PrinterTemplatePO::getValid, Constants.VALID);
        PrinterTemplatePO printerTemplatePO = getOne(queryWrapperTemplate);
        PrinterTemplateUpdateBO printerTemplateUpdateBO = new PrinterTemplateUpdateBO();
        BeanUtils.copyProperties(printerTemplatePO, printerTemplateUpdateBO);

        LambdaQueryWrapper queryWrapperRelation = new QueryWrapper<PrinterTemplateRelationPagePO>().lambda().eq(PrinterTemplateRelationPagePO :: getTemplateId ,templateId);
        List<PrinterTemplateRelationPagePO> printerTemplateRelationPagePOList = printerTemplateRelationPageMapper.selectList(queryWrapperRelation);
        List<PrinterTemplateRelationPageBO> printerTemplateRelationPageBOList = new ArrayList<>();
        if(!CollectionUtils.isEmpty(printerTemplateRelationPagePOList)){
            printerTemplateRelationPagePOList.forEach(p -> {
                PrinterTemplateRelationPageBO printerTemplateRelationPageBO = new PrinterTemplateRelationPageBO();
                BeanUtils.copyProperties(p, printerTemplateRelationPageBO);
                printerTemplateRelationPageBOList.add(printerTemplateRelationPageBO);
            });
        }
        printerTemplateUpdateBO.setPageDatas(printerTemplateRelationPageBOList);
        return printerTemplateUpdateBO;
    }

    /**
     * 保存设计模板
     * @param printerDesignContentBO 设计模板内容
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveTemplateDesignContent(PrinterDesignContentBO printerDesignContentBO) throws IOException {
        //Long uid = IDGenerator.newInstance().generate().longValue();
        MultipartFile multipartFile = getMultipartFile(printerDesignContentBO.getTemplateId(), printerDesignContentBO.getContent());
        log.info("saveTemplateDesignContent multipartFile:{}", multipartFile);
        Result result = bapFileService.fileUpload(multipartFile);
        log.info("saveTemplateDesignContent fileUpload Result:{}", result.getData());
        printerDesignContentBO.setContent(result.getData().toString());
        PrinterDesignContentPO printerDesignContentPO = new PrinterDesignContentPO();
        BeanUtils.copyProperties(printerDesignContentBO, printerDesignContentPO);
        //printerDesignContentPO.setId(uid);
        QueryWrapper<PrinterDesignContentPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(PrinterDesignContentPO::getTemplateId, printerDesignContentBO.getTemplateId()).eq(PrinterDesignContentPO::getValid, 1);
        int count = printerDesignContentMapper.selectCount(queryWrapper);
        if (count == 0) {
            printerDesignContentMapper.insert(printerDesignContentPO);
        } else {
            UpdateWrapper<PrinterDesignContentPO> updateWrapper = new UpdateWrapper<>();
            updateWrapper.lambda().eq(PrinterDesignContentPO::getTemplateId, printerDesignContentBO.getTemplateId()).eq(PrinterDesignContentPO::getValid, 1);
            printerDesignContentMapper.update(printerDesignContentPO, updateWrapper);
        }

        PrinterTemplatePO printerTemplatePO = getById(printerDesignContentBO.getTemplateId());
        if (printerTemplatePO == null) {
            return;
        }
        printerTemplatePO.setEnabled(printerDesignContentBO.getEnabled());
        updateById(printerTemplatePO);
    }

    private MultipartFile getMultipartFile(Long templateId, String templateJson) throws IOException {
        String fileName = templateId + ".txt";
        File file = new File(fileName);
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        fileOutputStream.write(templateJson.getBytes("UTF-8"));
        final DiskFileItem item = new DiskFileItem("file", MediaType.MULTIPART_FORM_DATA_VALUE, true, fileName, 100000000, file.getParentFile());
        try {
            OutputStream os = item.getOutputStream();
            os.write(FileUtils.readFileToByteArray(file));
        } catch (IOException e) {
            e.printStackTrace(); // do nothing!
        }
        return new CommonsMultipartFile(item);
    }
    /**
     * 加载设计模板json内容
     * @param templateId 模板id
     * @return
     */
    @Override
    public PrinterDesignContentBO loadTemplateDesignContent(Long templateId) throws UnsupportedEncodingException {
        PrinterTemplatePO printerTemplatePO = getById(templateId);
        if (printerTemplatePO == null) {
            return null;
        }
        QueryWrapper<PrinterDesignContentPO> designWrapper = new QueryWrapper<>();
        designWrapper.lambda()
                .eq(PrinterDesignContentPO::getTemplateId, templateId)
                .eq(PrinterDesignContentPO::getValid, true);
        PrinterDesignContentPO printerDesignContentPO = printerDesignContentMapper.selectOne(designWrapper);
        if (printerDesignContentPO == null) {
            return null;
        }
        PrinterDesignContentBO printerDesignContentBO = new PrinterDesignContentBO();
        BeanUtils.copyProperties(printerDesignContentPO, printerDesignContentBO);
        printerDesignContentBO.setEnabled(printerTemplatePO.getEnabled());
        ResponseEntity<byte[]> responseEntity =  bapFileService.downloadFile(printerDesignContentBO.getContent());
        if (responseEntity == null) {
            log.info("loadTemplateDesignContent responseEntity is null!");
        } else {
            log.info("loadTemplateDesignContent responseEntity status:{}", responseEntity.getStatusCode());
            byte[] bs = responseEntity.getBody();
            String content = new String(bs, "UTF-8");
            printerDesignContentBO.setContent(content);
        }
        return printerDesignContentBO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateTemplateStatus(PrinterTemplateBatchUpdateBO printerTemplateBatchUpdateBO) {
        UpdateWrapper<PrinterTemplatePO> updateWrapper = new UpdateWrapper<>();
        updateWrapper.lambda().set(PrinterTemplatePO::getEnabled, printerTemplateBatchUpdateBO.getEnabled()).in(PrinterTemplatePO::getId, printerTemplateBatchUpdateBO.getTemplateIds());
        update(updateWrapper);
    }
}
