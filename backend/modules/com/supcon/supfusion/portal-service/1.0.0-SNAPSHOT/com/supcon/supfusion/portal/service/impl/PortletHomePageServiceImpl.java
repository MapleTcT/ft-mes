package com.supcon.supfusion.portal.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.supcon.supfusion.framework.cloud.common.context.UserContext;
import com.supcon.supfusion.portal.common.constant.PortalConstants;
import com.supcon.supfusion.portal.dao.mapper.EcMyPortletMapper;
import com.supcon.supfusion.portal.dao.mapper.EcPortletMapper;
import com.supcon.supfusion.portal.dao.po.EcMyPortletPO;
import com.supcon.supfusion.portal.dao.po.EcPortletPO;
import com.supcon.supfusion.portal.manager.PortalAdapter;
import com.supcon.supfusion.portal.service.PortletHomePageService;
import com.supcon.supfusion.portal.service.bo.EcPortletBO;
import com.supcon.supfusion.portal.service.entity.MenuInfo;
import com.supcon.supfusion.portal.service.entity.MyPortlet;
import com.supcon.supfusion.rbac.api.dto.MenuInfoDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PortletHomePageServiceImpl implements PortletHomePageService {

    @Autowired
    private EcMyPortletMapper ecMyPortletMapper;
    @Autowired
    private EcPortletMapper ecPortletMapper;
    @Autowired
    private PortalAdapter portalAdapter;

    @Override
    public List<EcPortletBO> queryHomePagePortlet() {
        LambdaQueryWrapper<EcPortletPO> ecMyPortletPOLambdaQueryWrapper = Wrappers.lambdaQuery(EcPortletPO.class);
        ecMyPortletPOLambdaQueryWrapper.eq(EcPortletPO::getPowerFlag, false);
        List<MenuInfoDTO> menuInfos = portalAdapter.findPermissionMenu(UserContext.getUserContext().getUserId());
        if (ObjectUtils.isNotNull(menuInfos)) {
            ecMyPortletPOLambdaQueryWrapper.or().in(EcPortletPO::getMenuInfoId, menuInfos.stream().map(menuInfo -> menuInfo.getId()).collect(Collectors.toList()));
        }
        List<EcPortletPO> ecPortletPOS = ecPortletMapper.selectList(ecMyPortletPOLambdaQueryWrapper);
        List<EcPortletBO> ecPortletBOS = transformToBOS(ecPortletPOS);
        return ecPortletBOS;
    }

    @Override
    public List<MyPortlet> queryMyPortal() {
        LambdaQueryWrapper<EcMyPortletPO> ecMyPortletPOLambdaQueryWrapper = Wrappers.lambdaQuery(EcMyPortletPO.class);
        Long userId = UserContext.getUserContext().getUserId();
        ecMyPortletPOLambdaQueryWrapper.eq(EcMyPortletPO::getUserId, userId);
        EcMyPortletPO findEcMyPortlet = ecMyPortletMapper.selectOne(ecMyPortletPOLambdaQueryWrapper);
        if (!Optional.ofNullable(findEcMyPortlet).isPresent()) {
//            findEcMyPortlet = new EcMyPortletPO();
//            findEcMyPortlet.setConfig(PortalConstants.MY_PORTAL_CONFIG);
            ecMyPortletPOLambdaQueryWrapper.clear();
            ecMyPortletPOLambdaQueryWrapper.eq(EcMyPortletPO::getUserId, -1);
            findEcMyPortlet = ecMyPortletMapper.selectOne(ecMyPortletPOLambdaQueryWrapper);
        }
        List<MyPortlet> myPortlets = JSONArray.parseArray(findEcMyPortlet.getConfig(), MyPortlet.class);
        myPortlets.forEach(myPortlet -> {
            List<String> codes = new ArrayList<>();
            myPortlet.getPortlets().forEach(ecPortletBO -> {
                codes.add(ecPortletBO.getCode());
            });
            if (ObjectUtils.isNotNull(codes)) {
                LambdaQueryWrapper<EcPortletPO> ecPortletPOLambdaQueryWrapper = Wrappers.lambdaQuery(EcPortletPO.class);
                ecPortletPOLambdaQueryWrapper.in(EcPortletPO::getCode, codes);
                List<EcPortletPO> ecPortletPOS = ecPortletMapper.selectList(ecPortletPOLambdaQueryWrapper);
                List<EcPortletBO> ecPortletBOS = transformToBOS(ecPortletPOS);
                myPortlet.setPortlets(ecPortletBOS);
            }
        });
        return myPortlets;
    }

    @Override
    public void saveMyPortal(List<MyPortlet> myPortlet) {
        EcMyPortletPO ecMyPortletPO = new EcMyPortletPO();
        ecMyPortletPO.setUserId(UserContext.getUserContext().getUserId());
        String myPortletConfig = JSONArray.toJSONString(myPortlet);
        ecMyPortletPO.setConfig(myPortletConfig);
        LambdaQueryWrapper<EcMyPortletPO> ecMyPortletPOLambdaQueryWrapper = Wrappers.lambdaQuery(EcMyPortletPO.class);
        ecMyPortletPOLambdaQueryWrapper.eq(EcMyPortletPO::getUserId, ecMyPortletPO.getUserId());
        EcMyPortletPO findEcMyPortlet = ecMyPortletMapper.selectOne(ecMyPortletPOLambdaQueryWrapper);
        if (!Optional.ofNullable(findEcMyPortlet).isPresent()) {
            ecMyPortletMapper.insert(ecMyPortletPO);
        } else {
            ecMyPortletPO.setId(findEcMyPortlet.getId());
            ecMyPortletMapper.updateById(ecMyPortletPO);
        }
    }

    private List<EcPortletBO> transformToBOS(List<EcPortletPO> ecPortletPOS) {
        if (!ObjectUtils.isNotNull(ecPortletPOS)) {
            return new ArrayList<>();
        }
        return ecPortletPOS.stream().map(ecPortletPO -> {
            EcPortletBO ecPortletBO = new EcPortletBO();
            BeanUtils.copyProperties(ecPortletPO, ecPortletBO);
            ecPortletBO.setTitle(portalAdapter.getRemoteMessage(ecPortletBO.getTitle()));
            return ecPortletBO;
        }).collect(Collectors.toList());
    }
}
