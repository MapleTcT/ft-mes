package com.supcon.supfusion.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supcon.supfusion.auth.common.constants.Constants;
import com.supcon.supfusion.auth.common.exception.IpBlackWhiteErrorEnum;
import com.supcon.supfusion.auth.common.exception.IpBlackWhiteException;
import com.supcon.supfusion.auth.dao.mapper.IpBlackWhiteMapper;
import com.supcon.supfusion.auth.dao.po.IpBlackWhitePO;
import com.supcon.supfusion.auth.service.IpBlackWhiteService;
import com.supcon.supfusion.auth.service.bo.IpBlackWhiteBO;
import com.supcon.supfusion.auth.service.bo.IpBlackWhiteQueryBO;
import com.supcon.supfusion.auth.service.cache.IpBlackWhiteCache;
import com.supcon.supfusion.framework.cloud.common.context.UserContext;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Pagination;
import com.supcon.supfusion.framework.cloud.common.util.DateTimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class IpBlackWhiteServiceImpl implements IpBlackWhiteService {
    /**
     * IP格式校验正则
     */
    private static final String IP_FORMAT_REGEX = "^(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|[1-9]|\\*|\\?|[1-9]\\?|\\?[0-9]|[1]\\d\\?|[1]\\?\\d|[2]\\?[0-5]|[2][0-5]\\?|\\?\\?|[1-2]\\?\\?|\\?[0-9]\\?|\\?\\?[0-9])\\."
            + "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d|\\*|\\?|[1-9]\\?|\\?[0-9]|[1]\\d\\?|[1]\\?\\d|[2]\\?[0-5]|[2][0-5]\\?|\\?\\?|[1-2]\\?\\?|\\?[0-9]\\?|\\?\\?[0-9])\\." + "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d|\\*|\\?|[1-9]\\?|\\?[0-9]|[1]\\d\\?|[1]\\?\\d|[2]\\?[0-5]|[2][0-5]\\?|\\?\\?|[1-2]\\?\\?|\\?[0-9]\\?|\\?\\?[0-9])\\."
            + "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d|\\*|\\?|[1-9]\\?|\\?[0-9]|[1]\\d\\?|[1]\\?\\d|[2]\\?[0-5]|[2][0-5]\\?|\\?\\?|[1-2]\\?\\?|\\?[0-9]\\?|\\?\\?[0-9])$";
    private static final String ASTERISK_PATTERN = "(1\\\\d{2}|2[0-4]\\\\d|25[0-5]|[1-9]\\\\d|[1-9])";
    private static final String QUESTION_MARK_PATTERN = "([0-9])";
    @Autowired
    private IpBlackWhiteMapper ipBlackWhiteMapper;
    @Autowired
    private IpBlackWhiteCache ipBlackWhiteCache;

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public IpBlackWhiteBO createIpBlackWhite(IpBlackWhiteBO ipBlackWhiteBO, Boolean addCurrentIp) {
        // 判断ip格式
        if (!ipCheck(ipBlackWhiteBO.getIp())) {
            throw new IpBlackWhiteException(IpBlackWhiteErrorEnum.ERROR_IP_FORMAT);
        }
        ipBlackWhiteBO.setCompanyId(UserContext.getUserContext().getCompanyId());
        // 判断ip是否重复
        Integer ipCount = ipBlackWhiteMapper.selectCount(Wrappers.lambdaQuery(IpBlackWhitePO.class)
                .eq(IpBlackWhitePO::getCompanyId, ipBlackWhiteBO.getCompanyId())
                .eq(IpBlackWhitePO::getIp, ipBlackWhiteBO.getIp()));
        if (ipCount > 0) {
            throw new IpBlackWhiteException(IpBlackWhiteErrorEnum.IP_EXIST);
        }
        // 校验管控模式
        boolean isBlackControlType = Objects.equals(Constants.BLACK_CONTROL_TYPE, ipBlackWhiteBO.getControlType());
        boolean isWhiteControlType = Objects.equals(Constants.WHITE_CONTROL_TYPE, ipBlackWhiteBO.getControlType());
        if (!isBlackControlType && !isWhiteControlType) {
            // 如果所选管控模式非黑非白
            throw new IpBlackWhiteException(IpBlackWhiteErrorEnum.ERROR_CONTROL_TYPE_FORMAT);
        }
        if (isBlackControlType) {
            // 如果所选管控模式为黑名单
            handleBlackList(ipBlackWhiteBO);
        } else {
            // 如果所选管控模式为白名单
            handleWhiteList(ipBlackWhiteBO);
        }
        IpBlackWhitePO ipBlackWhitePO = new IpBlackWhitePO();
        BeanUtils.copyProperties(ipBlackWhiteBO, ipBlackWhitePO);
        ipBlackWhitePO.setCompanyId(ipBlackWhiteBO.getCompanyId());
        ipBlackWhitePO.setCreateTime(DateTimeUtil.getUTC0());
        ipBlackWhiteMapper.insert(ipBlackWhitePO);
        Set<String> ips = new HashSet<>(2);
        ips.add(ipBlackWhitePO.getIp());
        BeanUtils.copyProperties(ipBlackWhitePO, ipBlackWhiteBO);
        // 处理当前登录ip
        if (addCurrentIp && !Objects.equals(ipBlackWhiteBO.getIp(), ipBlackWhiteBO.getCurrentIp())) {
            Integer currentIpCount = ipBlackWhiteMapper.selectCount(Wrappers.lambdaQuery(IpBlackWhitePO.class)
                    .eq(IpBlackWhitePO::getCompanyId, ipBlackWhiteBO.getCompanyId())
                    .eq(IpBlackWhitePO::getIp, ipBlackWhiteBO.getCurrentIp()));
            if (currentIpCount == 0) {
                IpBlackWhitePO currentIpBlackWhitePO = new IpBlackWhitePO();
                currentIpBlackWhitePO.setIp(ipBlackWhiteBO.getCurrentIp());
                currentIpBlackWhitePO.setCompanyId(ipBlackWhiteBO.getCompanyId());
                currentIpBlackWhitePO.setControlType(ipBlackWhiteBO.getControlType());
                ipBlackWhiteMapper.insert(currentIpBlackWhitePO);
                ips.add(currentIpBlackWhitePO.getIp());
            }
        }
        // 写到redis
        ipBlackWhiteCache.add(ipBlackWhiteBO, ips);
        return ipBlackWhiteBO;
    }

    /**
     * 处理黑名单
     */
    private void handleBlackList(IpBlackWhiteBO ipBlackWhiteBO) {
        // 判断是否存在白名单ip
        Integer count = ipBlackWhiteMapper.selectCount(Wrappers.lambdaQuery(IpBlackWhitePO.class)
                .eq(IpBlackWhitePO::getCompanyId, ipBlackWhiteBO.getCompanyId())
                .eq(IpBlackWhitePO::getControlType, Constants.WHITE_CONTROL_TYPE));
        if (count > 0) {
            // 列表已存在白名单ip，但是新增的ip是黑名单
            throw new IpBlackWhiteException(IpBlackWhiteErrorEnum.CONFLICT_CONTROL_TYPE_WHITE);
        }
    }

    /**
     * 处理白名单
     */
    private void handleWhiteList(IpBlackWhiteBO ipBlackWhiteBO) {
        // 判断是否存在黑名单ip
        Integer count = ipBlackWhiteMapper.selectCount(Wrappers.lambdaQuery(IpBlackWhitePO.class)
                .eq(IpBlackWhitePO::getCompanyId, ipBlackWhiteBO.getCompanyId())
                .eq(IpBlackWhitePO::getControlType, Constants.BLACK_CONTROL_TYPE));
        if (count > 0) {
            // 列表已存在黑名单ip，但是新增的ip是白名单
            throw new IpBlackWhiteException(IpBlackWhiteErrorEnum.CONFLICT_CONTROL_TYPE_BLACK);
        }
    }

    /**
     * 校验ip格式是否合法
     *
     * @param ip ip地址
     */
    private boolean ipCheck(String ip) {
        if (StringUtils.isEmpty(ip)) {
            return false;
        }
        // 定义ip正则表达式 因支持模糊查询所以IP内会出现 ?,*模糊匹配字符 支持192.168.1?.*,192.168.??.*等格式的IP
        return ip.matches(IP_FORMAT_REGEX);
    }

    @Override
    public void removeIpBlackWhiteList(List<Long> ids, String currentIp) {
        checkIsExist(ids.toArray(new Long[0]));
        ipBlackWhiteCache.delete(ids);
        ipBlackWhiteMapper.deleteBatchIds(ids);
        Long companyId = UserContext.getUserContext().getCompanyId();
        Integer controlType = ipBlackWhiteCache.getControlType(companyId);
        // 如果是白名单，并且当前ip不在白名单内，则将当前ip加入到白名单内
        if (controlType != null && Objects.equals(Constants.WHITE_CONTROL_TYPE, controlType) && !verifyIp(currentIp, companyId)) {
            IpBlackWhiteBO ipBlackWhiteBO = new IpBlackWhiteBO();
            ipBlackWhiteBO.setIp(currentIp);
            ipBlackWhiteBO.setControlType(Constants.WHITE_CONTROL_TYPE);
            ipBlackWhiteBO.setCompanyId(companyId);
            createInterIpBlackWhite(ipBlackWhiteBO, false);
        }
    }

    @Override
    public PageResult<IpBlackWhiteBO> queryIpBlackWhiteList(IpBlackWhiteQueryBO queryParams, Pagination pagination) {
        LambdaQueryWrapper<IpBlackWhitePO> lambdaQueryWrapper = Wrappers.lambdaQuery(IpBlackWhitePO.class)
                .eq(IpBlackWhitePO::getCompanyId, UserContext.getUserContext().getCompanyId())
                .orderByDesc(IpBlackWhitePO::getIp);
        if (!StringUtils.isEmpty(queryParams.getIp())) {
            lambdaQueryWrapper.likeRight(IpBlackWhitePO::getIp, queryParams.getIp());
        }
        Page<IpBlackWhitePO> page = new Page<>();
        page.setCurrent(pagination.getCurrent()).setSize(pagination.getPageSize());
        Page<IpBlackWhitePO> ipBlackWhitePOPage = ipBlackWhiteMapper.selectPage(page, lambdaQueryWrapper);
        List<IpBlackWhiteBO> collect = ipBlackWhitePOPage.getRecords().stream().map(entity -> {
            IpBlackWhiteBO ipBlackWhiteBO = new IpBlackWhiteBO();
            BeanUtils.copyProperties(entity, ipBlackWhiteBO);
            return ipBlackWhiteBO;
        }).collect(Collectors.toList());
        return new PageResult<>(collect, ipBlackWhitePOPage.getTotal(), ipBlackWhitePOPage.getSize(), ipBlackWhitePOPage.getCurrent());
    }

    /**
     * 判断IP黑白名单是否存在
     *
     * @param ids 主键ID数组
     */
    private void checkIsExist(Long... ids) {
        Integer count = ipBlackWhiteMapper.selectCount(Wrappers.lambdaQuery(IpBlackWhitePO.class)
                .in(IpBlackWhitePO::getId, ids)
                .eq(IpBlackWhitePO::getCompanyId, UserContext.getUserContext().getCompanyId()));
        if (count != ids.length) {
            throw new IpBlackWhiteException(IpBlackWhiteErrorEnum.NOT_EXIST);
        }
    }

    @Override
    public boolean verifyIp(String ip, Long companyId) {
        if (companyId == null) {
            return true;
        }
        Integer controlType = ipBlackWhiteCache.getControlType(companyId);
        if (controlType == null) {
            return true;
        }
        Set<String> ipList = ipBlackWhiteCache.getIpSet(companyId);
        // 完善ip，将通配符变成对应的正则
        Set<String> controlIpList = new HashSet<>();
        for (String originIp : ipList) {
            if (originIp.contains("*")) {
                originIp = originIp.replaceAll("\\*", ASTERISK_PATTERN);
            }
            if (originIp.contains("?")) {
                originIp = originIp.replaceAll("\\?", QUESTION_MARK_PATTERN);
            }
            controlIpList.add(originIp);
        }
        // 是否在名单中
        boolean isContains = false;
        for (String controlIp : controlIpList) {
            if (ip.matches(controlIp)) {
                isContains = true;
                break;
            }
        }
        // 如果是黑名单管控模式，并且ip不在名单内 或者 是白名单管控模式，并且ip在名单中，则合法
        return (controlType == 0 && !isContains) || (controlType == 1 && isContains);
    }

    @Override
    public boolean checkOperateNeedTip(IpBlackWhiteBO ipBlackWhiteBO, Integer operateType) {
        // 判断ip格式
        if (!ipCheck(ipBlackWhiteBO.getIp())) {
            throw new IpBlackWhiteException(IpBlackWhiteErrorEnum.ERROR_IP_FORMAT);
        }
        Long companyId = UserContext.getUserContext().getCompanyId();
        // 校验管控模式
        boolean isBlackControlType = Objects.equals(Constants.BLACK_CONTROL_TYPE, ipBlackWhiteBO.getControlType());
        boolean isWhiteControlType = Objects.equals(Constants.WHITE_CONTROL_TYPE, ipBlackWhiteBO.getControlType());
        if (!isBlackControlType && !isWhiteControlType) {
            // 如果所选管控模式非黑非白
            throw new IpBlackWhiteException(IpBlackWhiteErrorEnum.ERROR_CONTROL_TYPE_FORMAT);
        }
        // 判断管控模式是否冲突
        Integer originControlType = ipBlackWhiteCache.getControlType(companyId);
        if (originControlType != null && !originControlType.equals(ipBlackWhiteBO.getControlType())) {
            throw new IpBlackWhiteException(IpBlackWhiteErrorEnum.ONLY_ONE_CONTROL_TYPE);
        }
        Set<String> ipList = ipBlackWhiteCache.getIpSet(companyId);
        if (ipList == null) {
            ipList = new HashSet<>();
        } else if (operateType == 0 && ipList.contains(ipBlackWhiteBO.getIp())) {
            // 判断新增时ip是否重复
            throw new IpBlackWhiteException(IpBlackWhiteErrorEnum.IP_EXIST);
        }
        // 完善ip列表
        if (operateType == 0) {
            ipList.add(ipBlackWhiteBO.getIp());
        } else {
            ipList.remove(ipBlackWhiteBO.getIp());
        }
        // 将ip中的通配符变成对应的正则
        Set<String> controlIpList = new HashSet<>();
        for (String originIp : ipList) {
            if (originIp.contains("*")) {
                originIp = originIp.replaceAll("\\*", ASTERISK_PATTERN);
            }
            if (originIp.contains("?")) {
                originIp = originIp.replaceAll("\\?", QUESTION_MARK_PATTERN);
            }
            controlIpList.add(originIp);
        }
        // 判断当前登录ip是否在名单中
        boolean isContains = false;
        for (String controlIp : controlIpList) {
            if (ipBlackWhiteBO.getCurrentIp().matches(controlIp)) {
                isContains = true;
                break;
            }
        }
        // 如果是黑名单管控模式，并且当前登录ip在名单内 或者 是白名单管控模式，并且当前登录ip在不在名单中，则需要提示
        return (isBlackControlType && isContains) || (isWhiteControlType && !isContains);
    }



    private IpBlackWhiteBO createInterIpBlackWhite(IpBlackWhiteBO ipBlackWhiteBO, Boolean addCurrentIp) {
        // 判断ip格式
        if (!ipCheck(ipBlackWhiteBO.getIp())) {
            throw new IpBlackWhiteException(IpBlackWhiteErrorEnum.ERROR_IP_FORMAT);
        }
        ipBlackWhiteBO.setCompanyId(UserContext.getUserContext().getCompanyId());
        // 判断ip是否重复
        Integer ipCount = ipBlackWhiteMapper.selectCount(Wrappers.lambdaQuery(IpBlackWhitePO.class)
                .eq(IpBlackWhitePO::getCompanyId, ipBlackWhiteBO.getCompanyId())
                .eq(IpBlackWhitePO::getIp, ipBlackWhiteBO.getIp()));
        if (ipCount > 0) {
            throw new IpBlackWhiteException(IpBlackWhiteErrorEnum.IP_EXIST);
        }
        // 校验管控模式
        boolean isBlackControlType = Objects.equals(Constants.BLACK_CONTROL_TYPE, ipBlackWhiteBO.getControlType());
        boolean isWhiteControlType = Objects.equals(Constants.WHITE_CONTROL_TYPE, ipBlackWhiteBO.getControlType());
        if (!isBlackControlType && !isWhiteControlType) {
            // 如果所选管控模式非黑非白
            throw new IpBlackWhiteException(IpBlackWhiteErrorEnum.ERROR_CONTROL_TYPE_FORMAT);
        }
        if (isBlackControlType) {
            // 如果所选管控模式为黑名单
            handleBlackList(ipBlackWhiteBO);
        } else {
            // 如果所选管控模式为白名单
            handleWhiteList(ipBlackWhiteBO);
        }
        IpBlackWhitePO ipBlackWhitePO = new IpBlackWhitePO();
        BeanUtils.copyProperties(ipBlackWhiteBO, ipBlackWhitePO);
        ipBlackWhitePO.setCompanyId(ipBlackWhiteBO.getCompanyId());
        ipBlackWhitePO.setCreateTime(DateTimeUtil.getUTC0());
        ipBlackWhiteMapper.insert(ipBlackWhitePO);
        Set<String> ips = new HashSet<>(2);
        ips.add(ipBlackWhitePO.getIp());
        BeanUtils.copyProperties(ipBlackWhitePO, ipBlackWhiteBO);
        // 处理当前登录ip
        if (addCurrentIp && !Objects.equals(ipBlackWhiteBO.getIp(), ipBlackWhiteBO.getCurrentIp())) {
            Integer currentIpCount = ipBlackWhiteMapper.selectCount(Wrappers.lambdaQuery(IpBlackWhitePO.class)
                    .eq(IpBlackWhitePO::getCompanyId, ipBlackWhiteBO.getCompanyId())
                    .eq(IpBlackWhitePO::getIp, ipBlackWhiteBO.getCurrentIp()));
            if (currentIpCount == 0) {
                IpBlackWhitePO currentIpBlackWhitePO = new IpBlackWhitePO();
                currentIpBlackWhitePO.setIp(ipBlackWhiteBO.getCurrentIp());
                currentIpBlackWhitePO.setCompanyId(ipBlackWhiteBO.getCompanyId());
                currentIpBlackWhitePO.setControlType(ipBlackWhiteBO.getControlType());
                ipBlackWhiteMapper.insert(currentIpBlackWhitePO);
                ips.add(currentIpBlackWhitePO.getIp());
            }
        }
        // 写到redis
        ipBlackWhiteCache.add(ipBlackWhiteBO, ips);
        return ipBlackWhiteBO;
    }

}
