package com.supcon.supfusion.custon.property.server.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.supcon.supfusion.custon.property.dao.entity.Button;
import com.supcon.supfusion.custon.property.dao.mappers.ButtonMapper;
import com.supcon.supfusion.custon.property.server.ButtonService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author zhang yafei
 */
@Slf4j
@Service
public class ButtonServiceImpl implements ButtonService {

    @Autowired
    private ButtonMapper buttonMapper;

    @Override
    public List<Button> getButtons(String viewCode) {
        LambdaQueryWrapper<Button> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Button::getViewCode, viewCode);
        List<Button> buttons = buttonMapper.selectList(wrapper);

        return buttons;
    }

    @Override
    public List<Button> getButtonsByDataGridCode(String dataGridCode) {
        LambdaQueryWrapper<Button> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Button::getDataGridCode, dataGridCode);
        List<Button> buttons = buttonMapper.selectList(wrapper);

        return buttons;
    }
}
