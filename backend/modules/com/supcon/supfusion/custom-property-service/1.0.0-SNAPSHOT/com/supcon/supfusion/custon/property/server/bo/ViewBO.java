package com.supcon.supfusion.custon.property.server.bo;

import com.supcon.supfusion.custon.property.dao.entity.*;
import lombok.Data;

import java.util.List;

/**
 * @author zhang yafei
 */
@Data
public class ViewBO extends View {

    private View assView;

    private View batchControlPrintSelectView;// 批量控件打印视图配置

    private ExtraQueryJson extraQuery;

    private View shadowView;


    private View reference;

    private ExtraView extraView;

    private Entity entity;

    private Model assModel;// 关联模型


    private ExtraView extraViewObj;


    private List<FastQueryJson> fastQueryJsons;


    private List<Button> buttons;

    private List<AdvQueryJson> advQueryJson;
}
