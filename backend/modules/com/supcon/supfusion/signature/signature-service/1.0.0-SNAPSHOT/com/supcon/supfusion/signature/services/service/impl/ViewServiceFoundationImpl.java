package com.supcon.supfusion.signature.services.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.supcon.supfusion.signature.dao.entity.Entity;
import com.supcon.supfusion.signature.dao.entity.View;
import com.supcon.supfusion.signature.dao.enums.ViewType;
import com.supcon.supfusion.signature.dao.mappers.ViewMapper;
import com.supcon.supfusion.signature.services.service.ViewServiceFoundation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * @author zhang yafei
 */
@Service
public class ViewServiceFoundationImpl implements ViewServiceFoundation {
    @Autowired
    private ViewMapper viewMapper;

    @Override
    public List<View> findViews(String entityCode, ViewType... viewTypes) {
        LambdaQueryWrapper<View> viewLambdaQueryWrapper = new LambdaQueryWrapper<>();
        viewLambdaQueryWrapper.eq(View::getEntityCode, entityCode).in(View::getType, viewTypes);
        return viewMapper.selectList(viewLambdaQueryWrapper);
    }

    @Override
    public View getView(String code) {
        return viewMapper.selectById(code);

    }
}
