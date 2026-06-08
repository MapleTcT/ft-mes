package com.supcon.supfusion.signature.services.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.signature.dao.entity.*;
import com.supcon.supfusion.signature.dao.enums.OperateType;
import com.supcon.supfusion.signature.dao.enums.ViewType;
import com.supcon.supfusion.signature.dao.mappers.DataGridMapper;
import com.supcon.supfusion.signature.dao.mappers.EcButtonMapper;
import com.supcon.supfusion.signature.services.service.ButtonService;
import com.supcon.supfusion.signature.services.service.RuntimeButtonService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * @author zhang yafei
 */
@Service
public class ButtonServiceImpl extends ServiceImpl<EcButtonMapper,EcButton> implements ButtonService {
    @Autowired
    private EcButtonMapper ecButtonMapper;
    @Autowired
    private RuntimeButtonService runtimeButtonService;
    @Autowired
    private DataGridMapper dataGridMapper;

    @Override
    public IPage<EcButton> getButtonsRequireSign(View view,IPage<EcButton> page) {
        LambdaQueryWrapper<EcButton> ecButtonLambdaQueryWrapper = new LambdaQueryWrapper<>();
        ecButtonLambdaQueryWrapper.ne(Button::getOperateType,OperateType.RESTORE)
                .eq(Button::getViewCode,view.getCode())
                .eq(Button::getIsSignatureConfig,true);

        if ((view.getType() == ViewType.EDIT || view.getType() == ViewType.VIEW) || view.getType() == ViewType.EXTRA) {
            LambdaQueryWrapper<DataGrid> eq = new LambdaQueryWrapper<DataGrid>().eq(DataGrid::getViewCode, view.getCode());
            List<DataGrid> dataGrids = dataGridMapper.selectList(eq);
            List<String> dataGridCodes = new ArrayList<>();
            dataGrids.forEach(dataGrid -> {
                if (view.getEditViewType() == 1 || view.getType() == ViewType.EDIT){
                    Integer integer = ecButtonMapper.selectCount(new LambdaQueryWrapper<EcButton>().eq(Button::getDataGridCode, dataGrid.getCode())
                            .eq(Button::getOperateType, OperateType.IMPORT));
                    if (integer == null || integer <= 0){
                        return;
                    }

                }
                dataGridCodes.add(dataGrid.getCode());
                if (dataGridCodes.size()>0){
                    ecButtonLambdaQueryWrapper.in(Button::getDataGridCode,dataGridCodes);
                }
            });

        } else if (view.getType() == ViewType.TREE) {
            List<String> strings = new ArrayList<>();
            strings.add(OperateType.SORT.name());
            strings.add(OperateType.MOVE.name());
            ecButtonLambdaQueryWrapper.notIn(Button::getOperateType,strings);
        }
        IPage<EcButton> ecButtonIPage = ecButtonMapper.selectPage(page, ecButtonLambdaQueryWrapper);
        int count = super.count(ecButtonLambdaQueryWrapper);
        ecButtonIPage.setTotal(count);
        return ecButtonIPage;
    }

    @Override
    public Button getButtonByCode(String code) {
        return ecButtonMapper.selectById(code);
    }

    @Override
    @Transactional
    public void saveButton(Button button) {
        EcButton ecButton = new EcButton();
        BeanUtils.copyProperties(button,ecButton);
        updateButton(ecButton);

        //同步数据到runtime
        runtimeButtonService.ecSynchronizedToRuntime(ecButton);
        //工程期暂不支持
//        copySignatureInfoToProject(button);
    }

    @Override
    @Transactional
    public void updateButton(EcButton ecButton) {
        super.saveOrUpdate(ecButton);
    }

    /**
     * 电子签名配置修改后由runtime同步到project，避免project与runtime不一致，造成模块或工程期视图发布时造成数据不一致的问题
     *
     * @param button
     */
    @Override
    @Transactional
    public void copySignatureInfoToProject(Button button) {

    }

}
