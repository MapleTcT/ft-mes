package com.supcon.supfusion.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supcon.supfusion.auth.common.dto.LdapDTO;
import com.supcon.supfusion.auth.common.exception.UserDirectoryErrorEnum;
import com.supcon.supfusion.auth.common.exception.UserDirectoryException;
import com.supcon.supfusion.auth.dao.mapper.UserDirectoryMapper;
import com.supcon.supfusion.auth.dao.mapper.UserMapper;
import com.supcon.supfusion.auth.dao.po.UserDirectoryPO;
import com.supcon.supfusion.auth.dao.po.UserPO;
import com.supcon.supfusion.auth.manager.LdapServiceAdapter;
import com.supcon.supfusion.auth.service.OnlineUserService;
import com.supcon.supfusion.auth.service.UserDirectoryService;
import com.supcon.supfusion.auth.service.UserService;
import com.supcon.supfusion.auth.service.bo.UserDirectoryBO;
import com.supcon.supfusion.auth.service.bo.UserDirectoryQueryBO;
import com.supcon.supfusion.framework.cloud.common.context.UserContext;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Pagination;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.supcon.supfusion.auth.common.exception.UserDirectoryErrorEnum.AD_DELETE_NOT_SUPPORT;
import static com.supcon.supfusion.auth.common.exception.UserDirectoryErrorEnum.EMPTY_AD_ENABLE;

/**
 * @author caokele
 */
@Slf4j
@Service
public class UserDirectoryServiceImpl implements UserDirectoryService {
    public static final Double SORT_INTERVAL = 1000D;
    @Autowired
    private UserDirectoryMapper userDirectoryMapper;
    @Autowired
    private LdapServiceAdapter ldapServiceAdapter;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private OnlineUserService onlineUserService;

    @Resource
    private UserService userService;

    @Override
    public UserDirectoryBO createUserDirectory(UserDirectoryBO userDirectoryBO) {
        UserDirectoryPO userDirectoryPO = new UserDirectoryPO();
        BeanUtils.copyProperties(userDirectoryBO, userDirectoryPO);
        userDirectoryPO.setCompanyId(UserContext.getUserContext().getCompanyId());
        // 获取最大的一个sort
        Double maxSort = Optional.ofNullable(userDirectoryMapper.selectMaxSort())
                .map(sort -> sort + SORT_INTERVAL)
                .orElse(SORT_INTERVAL);
        userDirectoryPO.setSort(maxSort);
        userDirectoryMapper.insert(userDirectoryPO);
        BeanUtils.copyProperties(userDirectoryPO, userDirectoryBO);
        return userDirectoryBO;
    }

    @Override
    public UserDirectoryBO updateUserDirectory(UserDirectoryBO userDirectoryBO) {
        checkIsExist(userDirectoryBO.getId());
        UserDirectoryPO userDirectoryPO = new UserDirectoryPO();
        BeanUtils.copyProperties(userDirectoryBO, userDirectoryPO);
        userDirectoryMapper.updateById(userDirectoryPO);
        BeanUtils.copyProperties(userDirectoryPO, userDirectoryBO);
        return userDirectoryBO;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void removeUserDirectories(List<Long> ids) {
        List<UserDirectoryPO> userDirectoryPOS = userDirectoryMapper.selectList(Wrappers.lambdaQuery(UserDirectoryPO.class).in(UserDirectoryPO::getId, ids)
                .eq(UserDirectoryPO::getCompanyId, UserContext.getUserContext().getCompanyId()));
        boolean isEnable = userDirectoryPOS.stream().anyMatch(t -> t.getEnabled() == true);
        if(isEnable){
            throw new UserDirectoryException(AD_DELETE_NOT_SUPPORT);
        }
        if (userDirectoryPOS.size() != ids.size()) {
            throw new UserDirectoryException(UserDirectoryErrorEnum.USER_NOT_EXIST);
        }
        UserDirectoryPO userDirectoryPO = new UserDirectoryPO();
        userDirectoryPO.setValid(false);
        userDirectoryPO.setEnabled(false);
        LambdaUpdateWrapper<UserDirectoryPO> userDirectoryUpdateWrapper = Wrappers.lambdaUpdate(UserDirectoryPO.class).in(UserDirectoryPO::getId, ids);
        userDirectoryMapper.update(userDirectoryPO, userDirectoryUpdateWrapper);
//        LambdaQueryWrapper<UserPO> userQueryWrapper = Wrappers.lambdaQuery(UserPO.class).in(UserPO::getUserDirectoryId, ids).eq(UserPO::getValid, true);
//        List<UserPO> users = userMapper.selectList(userQueryWrapper);
//        if (!users.isEmpty()) {
//            // 删除用户目录下用户
//            UserPO userPO = new UserPO();
//            userPO.setValid(false);
//            userMapper.update(userPO, Wrappers.lambdaUpdate(UserPO.class).in(UserPO::getUserDirectoryId, ids));
//            // 登出在线用户
//            List<Long> userIds = users.stream().map(UserPO::getId).collect(Collectors.toList());
//            onlineUserService.logoutOnlineUsersByUserIds(userIds);
//        }

    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void enableUserDirectory(Long id, Boolean enabled) {
        checkIsExist(id);
        List<UserDirectoryPO> userDirectoryPOS = userDirectoryMapper.selectList(Wrappers.lambdaQuery(UserDirectoryPO.class).eq(UserDirectoryPO::getEnabled, true));
        if(enabled && userDirectoryPOS.size()>=1){
            throw new UserDirectoryException(EMPTY_AD_ENABLE);
        }
        UserPO userPO = new UserPO();
        UserDirectoryPO userDirectoryPO = new UserDirectoryPO();
        userDirectoryPO.setId(id);
        userDirectoryPO.setEnabled(enabled);
        userDirectoryMapper.updateById(userDirectoryPO);
//        if (enabled) {
//            UserDirectoryPO userDirectoryPO1 = userDirectoryMapper.selectById(id);
//            if(userDirectoryPO1.getSyncFirst()){
//                LdapDTO ldapDTO = new LdapDTO();
//                ldapDTO.setHostname(userDirectoryPO1.getHostname());
//                ldapDTO.setPort(userDirectoryPO1.getPort());
//                ldapDTO.setPassword(userDirectoryPO1.getPassword());
//                ldapDTO.setUserName(userDirectoryPO1.getUserName());
//                LdapContext ldapContext = ldapServiceAdapter.getLdapContext(ldapDTO);
//                try {
//                    NamingEnumeration<NameClassPair> list = ldapContext.list(userDirectoryPO1.getBaseDn());
//                    ThreadPoolUtils.getThreadPool().execute(()->{
//                        userService.loadAllUser(userDirectoryPO1.getId(),UserContext.getUserContext().getCompanyId(),list);
//                    });
//                    userDirectoryPO.setSyncFirst(false);
//                    userDirectoryMapper.updateById(userDirectoryPO);
//                } catch (NamingException e) {
//                   log.error("enableUserDirectory error is ",e);
//                }
//            }
//            userPO.setHasLock(false);
//            // 将因为用户目录禁用而导致锁定的账户解锁
//            LambdaUpdateWrapper<UserPO> userUpdateWrapper = Wrappers.lambdaUpdate(UserPO.class).eq(UserPO::getValid, true)
//                    .eq(UserPO::getUserDirectoryId, id)
//                    .eq(UserPO::getLockReason, UserLockReasonEnum.USER_DIRECTORY.getCode());
//            userMapper.update(userPO, userUpdateWrapper);
//        } else {
//            userPO.setHasLock(true);
//            userPO.setLockReason(UserLockReasonEnum.USER_DIRECTORY.getCode());
//            // 将用户目录下还没有锁定的用户进行锁定
//            LambdaUpdateWrapper<UserPO> userUpdateWrapper = Wrappers.lambdaUpdate(UserPO.class).eq(UserPO::getValid, true)
//                    .eq(UserPO::getUserDirectoryId, id)
//                    .and(wrapper -> wrapper.isNull(UserPO::getHasLock).or(w -> w.eq(UserPO::getHasLock, false)));
//            userMapper.update(userPO, userUpdateWrapper);
//            // 登出在线用户
//            List<UserPO> users = userMapper.selectList(Wrappers.lambdaQuery(UserPO.class).eq(UserPO::getUserDirectoryId, id).eq(UserPO::getValid, true));
//            if (!users.isEmpty()) {
//                List<Long> userIds = users.stream().map(UserPO::getId).collect(Collectors.toList());
//                onlineUserService.logoutOnlineUsersByUserIds(userIds);
//            }
//        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void sortUserDirectory(Long id, Integer direction) {
        UserDirectoryPO userDirectoryPO = queryById(id);
        LambdaQueryWrapper<UserDirectoryPO> queryWrapper = Wrappers.lambdaQuery(UserDirectoryPO.class)
                .eq(UserDirectoryPO::getCompanyId, UserContext.getUserContext().getCompanyId())
                .eq(UserDirectoryPO::getValid, true);
        if (direction == 0) {
            // 如果是向上排序，获取上一位与上上一位
            queryWrapper.lt(UserDirectoryPO::getSort, userDirectoryPO.getSort()).orderByDesc(UserDirectoryPO::getSort, UserDirectoryPO::getId);
        } else {
            // 如果是向下排序，获取下一位与下下一位
            queryWrapper.gt(UserDirectoryPO::getSort, userDirectoryPO.getSort()).orderByAsc(UserDirectoryPO::getSort, UserDirectoryPO::getId);
        }
        queryWrapper.last("limit 2");
        List<UserDirectoryPO> userDirectoryPOS = userDirectoryMapper.selectList(queryWrapper);
        if (userDirectoryPOS.size() == 0) {
            // 已经在要排序的列表顶端了
            return;
        }
        // 计算排序值
        Double sort = null;
        if (userDirectoryPOS.size() == 1) {
            Double oneSort = userDirectoryPOS.get(0).getSort();
            sort = direction == 0 ? oneSort - SORT_INTERVAL : oneSort + SORT_INTERVAL;
        } else {
            double sum = userDirectoryPOS.stream().mapToDouble(UserDirectoryPO::getSort).sum();
            sort = sum / userDirectoryPOS.size();
        }
        UserDirectoryPO userDirectory = new UserDirectoryPO();
        userDirectory.setId(id);
        userDirectory.setSort(sort);
        userDirectoryMapper.updateById(userDirectory);
    }

    @Override
    public void connectUserDirectory(UserDirectoryBO userDirectoryBO) {
        LdapDTO ldapDTO = new LdapDTO();
        BeanUtils.copyProperties(userDirectoryBO, ldapDTO);
        ldapServiceAdapter.ldapTestConnect(ldapDTO);
    }

    @SuppressWarnings("unchecked")
    @Override
    public PageResult<UserDirectoryBO> queryUserDirectories(UserDirectoryQueryBO queryParams, Pagination pagination) {
        LambdaQueryWrapper<UserDirectoryPO> lambdaQueryWrapper = Wrappers.lambdaQuery(UserDirectoryPO.class)
                .eq(UserDirectoryPO::getValid, true)
                .eq(UserDirectoryPO::getCompanyId, UserContext.getUserContext().getCompanyId())
                .orderByDesc(UserDirectoryPO::getSort, UserDirectoryPO::getId);
        if (!StringUtils.isEmpty(queryParams.getDirectoryName())) {
            lambdaQueryWrapper.likeRight(UserDirectoryPO::getDirectoryName, queryParams.getDirectoryName());
        }
        if (!StringUtils.isEmpty(queryParams.getDirectoryType())) {
            lambdaQueryWrapper.eq(UserDirectoryPO::getDirectoryType, queryParams.getDirectoryType());
        }
        if (queryParams.getScreenDirectoryNames() != null && !queryParams.getScreenDirectoryNames().isEmpty()) {
            lambdaQueryWrapper.in(UserDirectoryPO::getDirectoryName, queryParams.getScreenDirectoryNames());
        }
        if (queryParams.getScreenDirectoryTypes() != null && !queryParams.getScreenDirectoryTypes().isEmpty()) {
            lambdaQueryWrapper.in(UserDirectoryPO::getDirectoryType, queryParams.getScreenDirectoryTypes());
        }
        Page<UserDirectoryPO> page = new Page<>();
        page.setCurrent(pagination.getCurrent()).setSize(pagination.getPageSize());
        Page<UserDirectoryPO> directoryPOPage = userDirectoryMapper.selectPage(page, lambdaQueryWrapper);
        List<UserDirectoryBO> collect = directoryPOPage.getRecords().stream().map(entity -> {
            UserDirectoryBO userDirectoryBO = new UserDirectoryBO();
            BeanUtils.copyProperties(entity, userDirectoryBO);
            return userDirectoryBO;
        }).collect(Collectors.toList());
        return new PageResult<>(collect, directoryPOPage.getTotal(), directoryPOPage.getSize(), directoryPOPage.getCurrent());
    }

    @Override
    public void exportUserDirectories(List<Long> ids) {
        log.info("exportUserDirectories");
    }

    @Override
    public void authenticateUserDirectory(String userName, String password) {
       UserDirectoryPO userDirectoryPO = userDirectoryMapper.selectOne(Wrappers.lambdaQuery(UserDirectoryPO.class).eq(UserDirectoryPO::getEnabled, true));
        LdapDTO ldapDTO = new LdapDTO();
        BeanUtils.copyProperties(userDirectoryPO, ldapDTO);
        ldapDTO.setAdName(userName);
        ldapDTO.setPassword(password);
        ldapServiceAdapter.ldapAuthenticate(ldapDTO);
    }

    @Override
    public UserDirectoryBO queryUserDirectory(Long id) {
        UserDirectoryPO userDirectoryPO = queryById(id);
        UserDirectoryBO userDirectoryBO = new UserDirectoryBO();
        BeanUtils.copyProperties(userDirectoryPO, userDirectoryBO);
        return userDirectoryBO;
    }

    /**
     * 通过ID获取用户目录
     *
     * @param id 主键ID
     */
    private UserDirectoryPO queryById(Long id) {
        LambdaQueryWrapper<UserDirectoryPO> queryWrapper = Wrappers.lambdaQuery(UserDirectoryPO.class)
                .eq(UserDirectoryPO::getId, id)
                .eq(UserDirectoryPO::getValid, true);
        UserDirectoryPO userDirectoryPO = userDirectoryMapper.selectOne(queryWrapper);
        if (userDirectoryPO == null) {
            throw new UserDirectoryException(UserDirectoryErrorEnum.USER_NOT_EXIST);
        }
        return userDirectoryPO;
    }

    /**
     * 判断用户目录是否存在
     *
     * @param ids 主键ID数组
     */
    private void checkIsExist(Long... ids) {
        LambdaQueryWrapper<UserDirectoryPO> queryWrapper = Wrappers.lambdaQuery(UserDirectoryPO.class)
                .in(UserDirectoryPO::getId, ids)
                .eq(UserDirectoryPO::getCompanyId, UserContext.getUserContext().getCompanyId())
                .eq(UserDirectoryPO::getValid, true);
        Integer count = userDirectoryMapper.selectCount(queryWrapper);
        if (count != ids.length) {
            throw new UserDirectoryException(UserDirectoryErrorEnum.USER_NOT_EXIST);
        }
    }

    @Override
    public Set<String> queryUserDirectoryFieldValues(String fieldName, String fieldValue) {
        LambdaQueryWrapper<UserDirectoryPO> lambdaQueryWrapper = Wrappers.lambdaQuery(UserDirectoryPO.class)
                .eq(UserDirectoryPO::getValid, true)
                .eq(UserDirectoryPO::getCompanyId, UserContext.getUserContext().getCompanyId());
        boolean isDirectoryNameField = "directoryName".equals(fieldName);
        boolean isDirectoryTypeField = "directoryType".equals(fieldName);
        if (isDirectoryNameField) {
            lambdaQueryWrapper.groupBy(UserDirectoryPO::getDirectoryName).orderByAsc(UserDirectoryPO::getDirectoryName);
            if (!StringUtils.isEmpty(fieldValue)) {
                lambdaQueryWrapper.likeRight(UserDirectoryPO::getDirectoryName, fieldValue);
            }
        } else if (isDirectoryTypeField) {
            lambdaQueryWrapper.groupBy(UserDirectoryPO::getDirectoryType).orderByAsc(UserDirectoryPO::getDirectoryType);
            if (!StringUtils.isEmpty(fieldValue)) {
                lambdaQueryWrapper.eq(UserDirectoryPO::getDirectoryType, fieldValue);
            }
        } else {
            throw new UserDirectoryException(UserDirectoryErrorEnum.FIELD_NOT_SUPPORT);
        }

        List<UserDirectoryPO> userDirectories = userDirectoryMapper.selectList(lambdaQueryWrapper);
        Set<String> values = null;
        if (isDirectoryNameField) {
            values = userDirectories.stream().map(UserDirectoryPO::getDirectoryName).collect(Collectors.toSet());
        } else if (isDirectoryTypeField) {
            values = userDirectories.stream().map(UserDirectoryPO::getDirectoryType).collect(Collectors.toSet());
        }
        return values == null ? Collections.emptySet() : values;
    }
}
