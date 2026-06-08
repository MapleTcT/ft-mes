package com.supcon.supfusion.printer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;
import com.supcon.supfusion.printer.dao.mapper.EntityPageUrlMapper;
import com.supcon.supfusion.printer.dao.mapper.PrinterRegisterMapper;
import com.supcon.supfusion.printer.dao.po.EntityPageUrlPO;
import com.supcon.supfusion.printer.dao.po.PrinterRegisterPO;
import com.supcon.supfusion.printer.service.PrinterRegisterService;
import com.supcon.supfusion.printer.service.bo.EntityPageUrlBO;
import com.supcon.supfusion.printer.service.bo.PrinterRegisterBO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PrinterRegisterServiceImpl extends ServiceImpl<PrinterRegisterMapper, PrinterRegisterPO> implements PrinterRegisterService {

    @Autowired
    private EntityPageUrlMapper entityPageUrlMapper;

    @Override
    public void addPrinterRegister(PrinterRegisterBO printerRegisterBO) {
        PrinterRegisterPO printerRegisterPO = new PrinterRegisterPO();
        BeanUtils.copyProperties(printerRegisterBO, printerRegisterPO);
        LambdaUpdateWrapper<PrinterRegisterPO> updateWrapper = new UpdateWrapper<PrinterRegisterPO>().lambda()
                .eq(PrinterRegisterPO :: getSource, printerRegisterBO.getSource())
                .eq(PrinterRegisterPO :: getServiceType, printerRegisterBO.getServiceType());
        saveOrUpdate(printerRegisterPO, updateWrapper);
    }

    @Override
    public PrinterRegisterBO queryPrinterRegister(PrinterRegisterBO printerRegisterBO) {
        LambdaQueryWrapper<PrinterRegisterPO> queryWrapper = new QueryWrapper<PrinterRegisterPO>().lambda()
                .eq(PrinterRegisterPO :: getSource, printerRegisterBO.getSource())
                .eq(PrinterRegisterPO :: getServiceType, printerRegisterBO.getServiceType());
        PrinterRegisterPO printerRegisterPO = getOne(queryWrapper);
        PrinterRegisterBO printerRegisterBOResult = new PrinterRegisterBO();
        BeanUtils.copyProperties(printerRegisterPO, printerRegisterBOResult);
        return printerRegisterBOResult;
    }

    /**
     * 实体对象注册页面地址接口
     * @param entityPageUrlBO
     */
    @Override
    public void registerEntityPageUrl(EntityPageUrlBO entityPageUrlBO) {
        EntityPageUrlPO entityPageUrlPO = new EntityPageUrlPO();
        BeanUtils.copyProperties(entityPageUrlBO, entityPageUrlPO);
        Long uid = IDGenerator.newInstance().generate().longValue();
        entityPageUrlPO.setId(uid);
        entityPageUrlMapper.insert(entityPageUrlPO);
    }
}
