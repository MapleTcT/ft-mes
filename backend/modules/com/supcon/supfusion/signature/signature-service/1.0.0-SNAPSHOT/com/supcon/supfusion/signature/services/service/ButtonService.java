package com.supcon.supfusion.signature.services.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.supcon.supfusion.signature.dao.entity.Button;
import com.supcon.supfusion.signature.dao.entity.EcButton;
import com.supcon.supfusion.signature.dao.entity.View;
import com.supcon.supfusion.signature.dao.mappers.EcButtonMapper;

import java.util.List;

/**
 * @author zhang yafei
 */
public interface ButtonService extends IService<EcButton> {

    IPage<EcButton> getButtonsRequireSign(View view , IPage<EcButton> page);
    Button getButtonByCode(String code);
    void updateButton(EcButton button);
    void saveButton(Button button);
    void copySignatureInfoToProject(Button button);
}
