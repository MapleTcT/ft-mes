package com.supcon.supfusion.license.service.impl;

import com.alibaba.fastjson.JSON;
//import com.alibaba.nacos.api.config.ConfigService;
//import com.alibaba.nacos.api.exception.NacosException;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Lists;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.supcon.supfusion.framework.cloud.common.exception.BizErrorEnum;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.security.util.MD5Util;
import com.supcon.supfusion.license.api.dto.LicenseInfoDTO;
import com.supcon.supfusion.license.common.constants.Constants;
import com.supcon.supfusion.license.common.enuma.BAPLicenseKey;
import com.supcon.supfusion.license.common.exception.IdentityProviderErrorEnum;
import com.supcon.supfusion.license.common.exception.IdentityProviderException;
import com.supcon.supfusion.license.common.utils.date.DateHelper;
import com.supcon.supfusion.license.common.utils.security.Base64Util;
import com.supcon.supfusion.license.common.utils.systemutil.SupportOS;
import com.supcon.supfusion.license.common.utils.systemutil.SystemUtils;
import com.supcon.supfusion.license.dao.mapper.LicenseMapper;
import com.supcon.supfusion.license.dao.po.LicenseInfoPO;
import com.supcon.supfusion.license.service.LicenseService;
import com.supcon.supfusion.license.service.bo.LicenseInfoBO;
import com.supcon.supfusion.license.service.cache.LicenseCache;
import com.supcon.supfusion.license.service.vo.LicenseInfoVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class LicenseServiceImpl extends ServiceImpl<LicenseMapper, LicenseInfoPO> implements LicenseService {

    @Autowired
    private LicenseMapper licenseMapper;

    @Autowired
    private LicenseCache licenseCache;

//    @Autowired
//    private ConfigService configService;

    @Autowired
    @Qualifier("licenseStringRedisTemplate")
    private StringRedisTemplate redisTemplate;

    private static final ScheduledExecutorService SCHEDULED_EXECUTOR = Executors.newSingleThreadScheduledExecutor();

    private static final boolean isLinux = SystemUtils.getOS().equals(SupportOS.LINUX);

    /**
     * 获取Windows下软件狗授权结果
     */
    public interface SCDog extends Library {
        int DogPack_CheckDogSecurityEx(String key);
    }

    /**
     * 获取Linux下软件狗授权结果
     */
    public interface SCDogLinux extends Library {
        int CheckDogSecrityDirect(String key);
    }

    public static SCDog SCDOG = null;

    public static SCDogLinux SCDogLinux = null;

    @Override
    public void scheduleRefresh() {
        SCHEDULED_EXECUTOR.scheduleAtFixedRate(() -> {
            log.info("定时任务刷新授权信息开始执行 ======");
            this.refreshLicense();
        }, 100, 1 * DateHelper.MINUTE_TIME, TimeUnit.MILLISECONDS);
    }


    /**
     * 定时任务刷新授权信息
     */
    @Transactional
    private void refreshLicense() {
        try {
            //防止篡改数据
            this.preventUpdateDB();
            licenseCache.preventUpdateRedis();

            //初始化软件狗数据
            if (isLinux) {
                SCDogLinux = Native.loadLibrary("SCDog", SCDogLinux.class);
            } else {
                SCDOG = Native.loadLibrary("SCDogJavaAdapter10_x64", SCDog.class);
            }

            //初始化并发数授权
            this.initConcurrent();

            //从Nacos获取moduleCode和licenseKey
//            String config = configService.getConfig(Constants.nacosLicenseDataId, Constants.nacosLicenseGroup, 5000);
            String config = redisTemplate.opsForValue().get(Constants.licenseInitRedisKey);
            List<LicenseInfoBO> licenseInfoBOList = new ArrayList<>();
            if (!ObjectUtils.isEmpty(config)) {
                licenseInfoBOList = JSON.parseArray(config, LicenseInfoBO.class);
            }
            this.handleLicenseInfoList(licenseInfoBOList);

            //若nacos信息丢失，从数据库读取数据刷新，补偿机制
            List<LicenseInfoBO> licenseInfoListDB = this.refreshFromDB(licenseInfoBOList);
            this.handleLicenseInfoList(licenseInfoListDB);
        } catch (Exception e) {
            log.error("定时任务刷新授权信息发生错误 ======", e);
        }
    }

    /**
     * 处理授权
     */
    private void handleLicenseInfoList(List<LicenseInfoBO> licenseInfoBOList) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(Constants.licenseRedisKey);
        for (LicenseInfoBO licenseInfoBO : licenseInfoBOList) {
            String licenseKey = licenseInfoBO.getLicenseKey();
            if (!ObjectUtils.isEmpty(licenseKey)) {
                //从软件狗获取是否允许授权
                Integer valueFromSCDog = this.getValueFromSCDog(licenseKey);
                log.info("从软件狗获取授权信息,licenseInfoBO:{},valueFromSCDog:{}", JSON.toJSONString(licenseInfoBO), valueFromSCDog);
                //有授权信息
                if (!ObjectUtils.isEmpty(valueFromSCDog)) {
                    licenseInfoBO.setValue(valueFromSCDog);
                } else {
                    log.error("从软件狗获取授权信息发生错误,licenseInfoBO:{},valueFromSCDog:{}", JSON.toJSONString(licenseInfoBO), valueFromSCDog);
                    continue;
                }
                //将授权信息存入redis
                licenseCache.insertLicenseToRedis(licenseInfoBO,entries);

                //将授权信息存入数据库
                this.insertLicenseToDB(licenseInfoBO, valueFromSCDog);
            }
        }
    }

    /**
     * 防止nacos信息丢失，从数据库读取数据刷新，补偿机制
     */
    private List<LicenseInfoBO> refreshFromDB(List<LicenseInfoBO> licenseInfoBOList) {
        List<LicenseInfoBO> licenseInfoListDB = Lists.newArrayList();
        List<String> collect = licenseInfoBOList.stream().map(LicenseInfoBO::getModuleCode).collect(Collectors.toList());
        collect.add(BAPLicenseKey.BAP.getModuleCode());
        List<LicenseInfoPO> list = this.list();
        for (LicenseInfoPO licenseInfoPO : list) {
            String moduleCode = Base64Util.decode(licenseInfoPO.getModuleCode());
            String licenseKey = Base64Util.decode(licenseInfoPO.getLicenseKey());
            String time = Base64Util.decode(licenseInfoPO.getTime());
            String applicationName = licenseInfoPO.getApplicationName();
            String applicatonType = licenseInfoPO.getApplicationType();
            LicenseInfoBO licenseInfoBo = LicenseInfoBO.builder().moduleCode(moduleCode).licenseKey(licenseKey).time(time).
                    applicationName(applicationName).applicationType(applicatonType).build();
            if (!collect.contains(licenseInfoBo.getModuleCode())) {
                licenseInfoListDB.add(licenseInfoBo);
            }
        }
        return licenseInfoListDB;
    }


    /**
     * 初始化并发数授权
     */
    private void initConcurrent() throws UnsupportedEncodingException, NoSuchAlgorithmException {
        LicenseInfoPO one = this.getOne(new QueryWrapper<LicenseInfoPO>().lambda().eq(LicenseInfoPO::getModuleCode, Base64Util.encode(BAPLicenseKey.BAP.getModuleCode())));
        String moduleCode = BAPLicenseKey.BAP.getModuleCode();
        String licenseKey = BAPLicenseKey.BAP.getLicenseKey();
        Integer valueFromSCDog = getValueFromSCDog(licenseKey);
        log.info("并发数授权,从软件狗获取授权信息,licenseKey:{},valueFromSCDog:{}",licenseKey,valueFromSCDog);
        if (ObjectUtils.isEmpty(valueFromSCDog) || valueFromSCDog.equals(-1)) {
            valueFromSCDog = -1;
        } else if (valueFromSCDog == 255) {
            valueFromSCDog = 255;
        } else if (valueFromSCDog < 5) {
            valueFromSCDog = 5;
        }
        if (ObjectUtils.isEmpty(one)) {
            LicenseInfoPO licenseInfoPO = new LicenseInfoPO();
            licenseInfoPO.setModuleCode(Base64Util.encode(moduleCode));
            licenseInfoPO.setLicenseKey(Base64Util.encode(licenseKey));
            String time = String.valueOf(DateHelper.time());
            licenseInfoPO.setTime(Base64Util.encode(time));
            licenseInfoPO.setValue(Base64Util.encode(String.valueOf(valueFromSCDog)));
            licenseInfoPO.setApplicationType(BAPLicenseKey.BAP.getApplicationType());
            licenseInfoPO.setApplicationName(BAPLicenseKey.BAP.getApplicationName());
            //hash
            String result = moduleCode + licenseKey + valueFromSCDog + time + Constants.Salt;
            licenseInfoPO.setHashCode(MD5Util.encode(result));
            this.save(licenseInfoPO);
        } else {
            //并发数发生变化进行修改
            if (!one.getValue().equals(Base64Util.encode(String.valueOf(valueFromSCDog)))) {
                one.setValue(Base64Util.encode(String.valueOf(valueFromSCDog)));
                String result = moduleCode + licenseKey + valueFromSCDog + Base64Util.decode(one.getTime()) + Constants.Salt;
                one.setHashCode(MD5Util.encode(result));
                this.updateById(one);
            }
        }

        //存放至redis
        licenseCache.initRedisConcurrent(valueFromSCDog);
    }

    /**
     * 将授权信息存入数据库
     *
     * @param licenseInfoBO
     * @param valueFromSCDog
     */
    private void insertLicenseToDB(LicenseInfoBO licenseInfoBO, Integer valueFromSCDog) {
        try {
            LicenseInfoPO licenseInfoPO = new LicenseInfoPO();

            licenseInfoPO.setModuleCode(Base64Util.encode(licenseInfoBO.getModuleCode()));
            licenseInfoPO.setLicenseKey(Base64Util.encode(licenseInfoBO.getLicenseKey()));
            licenseInfoPO.setTime(Base64Util.encode(licenseInfoBO.getTime()));
            licenseInfoPO.setApplicationName(licenseInfoBO.getApplicationName());
            licenseInfoPO.setApplicationType(licenseInfoBO.getApplicationType());

            //已授权
            if (valueFromSCDog >= Constants.haveLicense) {
                licenseInfoPO.setValue(Base64Util.encode(valueFromSCDog.toString()));
            } else if (Constants.noLicense.equals(valueFromSCDog)) {
                //未授权
                //判断是否过期   实体配置试用时间12小时  其他业务模块试用时间6小时
                boolean isExpire;
                if (BAPLicenseKey.EC_MODULE.getModuleCode().equalsIgnoreCase(licenseInfoBO.getModuleCode())) {
                    isExpire = (DateHelper.time() - Long.parseLong(licenseInfoBO.getTime())) > Constants.ecEntityTime;
                } else {
                    isExpire = (DateHelper.time() - Long.parseLong(licenseInfoBO.getTime())) > Constants.moduleTime;
                }
                //过期
                if (isExpire) {
                    licenseInfoPO.setValue(Base64Util.encode("-1"));
                } else {
                    licenseInfoPO.setValue(Base64Util.encode("-2"));
                }
            }
            //hash值
            String result = licenseInfoBO.getModuleCode() + licenseInfoBO.getLicenseKey() + Base64Util.decode(licenseInfoPO.getValue()) + licenseInfoBO.getTime() + Constants.Salt;
            licenseInfoPO.setHashCode(MD5Util.encode(result));
            this.saveOrUpdate(licenseInfoPO, new UpdateWrapper<LicenseInfoPO>().lambda().eq(LicenseInfoPO::getModuleCode, licenseInfoPO.getModuleCode()));
        } catch (Exception e) {
            log.error("将授权信息存入数据库发生错误 ===== ", e);
        }
    }

    /**
     * 防止篡改数据库数据
     */
    private void preventUpdateDB() {
        //获取DB所有数据,如有篡改数据，则删除当前记录
        try {
            List<LicenseInfoPO> list = this.list();
            for (LicenseInfoPO licenseInfoPO : list) {
                String moduleCode = Base64Util.decode(licenseInfoPO.getModuleCode());
                String licenseKey = Base64Util.decode(licenseInfoPO.getLicenseKey());
                String value = Base64Util.decode(licenseInfoPO.getValue().toString());
                String time = Base64Util.decode(licenseInfoPO.getTime());
                String result = moduleCode + licenseKey + value + time + Constants.Salt;
                //非法篡改，删除当前记录
                if (!MD5Util.encode(result).equalsIgnoreCase(licenseInfoPO.getHashCode())) {
                    this.removeById(licenseInfoPO.getId());
                    log.info("当前数据库数据被篡改：moduleCode:{},licenseKey:{},value:{}", moduleCode, licenseKey, value);
                }
            }
        } catch (Exception e) {
            log.error("防止篡改数据库数据发生错误 =====", e);
        }
    }


    /**
     * 从软件狗获取是否允许授权
     */
    public synchronized Integer getValueFromSCDog(String key) {
        //初始化软件狗数据
        if (isLinux) {
            SCDogLinux = Native.loadLibrary("SCDog", SCDogLinux.class);
        } else {
            SCDOG = Native.loadLibrary("SCDogJavaAdapter10_x64", SCDog.class);
        }
        int result;
        try {
            if (isLinux) {
                result = SCDogLinux.CheckDogSecrityDirect(key);
            } else {
                result = SCDOG.DogPack_CheckDogSecurityEx(key);
            }
            log.debug("读取软件狗信息   licenseKey:" + key + ",值:" + result);
            return result;
        } catch (Exception e) {
            log.error("从软件狗获取授权信息发生错误 ", e);
            return null;
        }
    }

    /**
     * app服务启动时，向nacos注册moduleCode和软件狗key相关信息
     */
    @Override
    public void registerLicenseInfo(LicenseInfoDTO licenseInfoDTO) {
        try {
            log.info("向nacos注冊授权信息,licenseInfoDTO：{},time:{}", JSON.toJSONString(licenseInfoDTO),DateHelper.now());
            //设置app服务启动时间
            licenseInfoDTO.setTime(String.valueOf(DateHelper.time()));
//            String config = configService.getConfig(Constants.nacosLicenseDataId, Constants.nacosLicenseGroup, 5000);
            String config = redisTemplate.opsForValue().get(Constants.licenseInitRedisKey);
            List<LicenseInfoDTO> result = new ArrayList<>();
            if (ObjectUtils.isEmpty(config)) {
                result.add(licenseInfoDTO);
            } else {
                result = JSON.parseArray(config, LicenseInfoDTO.class);
                List<String> collect = result.stream().map(LicenseInfoDTO::getModuleCode).collect(Collectors.toList());
                if (collect.contains(licenseInfoDTO.getModuleCode())) {
                    //修改
                    for (LicenseInfoDTO licenseInfoNacos : result) {
                        if (licenseInfoDTO.getModuleCode().equals(licenseInfoNacos.getModuleCode())) {
                            BeanUtils.copyProperties(licenseInfoDTO, licenseInfoNacos);
                        }
                    }
                } else {
                    //新增
                    result.add(licenseInfoDTO);
                }
            }
//            configService.publishConfig(Constants.nacosLicenseDataId, Constants.nacosLicenseGroup, JSON.toJSONString(result));
            redisTemplate.opsForValue().set(Constants.licenseInitRedisKey,JSON.toJSONString(result));
        } catch (Exception e) {
            log.error("nacos 注册模块信息发生错误 =========");
        }
    }

    /**
     * 根据模块code获取授权信息
     */
    @Override
    public Result<LicenseInfoVO> getLicenseByModule(String moduleCode) {
        LicenseInfoVO licenseInfoVO = new LicenseInfoVO();
        LicenseInfoPO licenseInfoPO = this.getOne(new QueryWrapper<LicenseInfoPO>().lambda().eq(LicenseInfoPO::getModuleCode, Base64Util.encode(moduleCode)));
        if (!ObjectUtils.isEmpty(licenseInfoPO)) {
            this.handleSingleLicense(licenseInfoVO, licenseInfoPO);
        } else {
            throw new IdentityProviderException(IdentityProviderErrorEnum.MODULE_NOT_EXIST);
        }
        return new Result<>(BizErrorEnum.SYSTEM_OK.getCode(), BizErrorEnum.SYSTEM_OK.getMessage(), licenseInfoVO);
    }


    /**
     * 分页查询授权信息
     */
    @Override
    public PageResult<LicenseInfoVO> getLicensePage(Long current, Long size) {
        Page<LicenseInfoPO> page = new Page<>(current, size);
        List<LicenseInfoVO> result = new ArrayList<>();
        QueryWrapper<LicenseInfoPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("valid", true);
        IPage<LicenseInfoPO> page1 = licenseMapper.findPage(page, queryWrapper);
        if (!ObjectUtils.isEmpty(page1) && !ObjectUtils.isEmpty(page1.getRecords())) {
            List<LicenseInfoPO> licenseList = page1.getRecords();
            for (LicenseInfoPO licenseInfoPO : licenseList) {
                LicenseInfoVO licenseInfoVO = new LicenseInfoVO();
                this.handleSingleLicense(licenseInfoVO, licenseInfoPO);
                result.add(licenseInfoVO);
            }
        }
        return new PageResult<>(result, page1.getTotal(), page1.getSize(), page.getCurrent());
    }

    private void handleSingleLicense(LicenseInfoVO licenseInfoVO, LicenseInfoPO licenseInfoPO) {
        String decodeModuleCode = Base64Util.decode(licenseInfoPO.getModuleCode());
        licenseInfoVO.setModuleCode(decodeModuleCode);
        licenseInfoVO.setApplicationName(licenseInfoPO.getApplicationName());
        licenseInfoVO.setApplicationType(licenseInfoPO.getApplicationType());
        Integer value = Integer.parseInt(Base64Util.decode(licenseInfoPO.getValue()));
        //并发用户数模块
        if (licenseInfoVO.getModuleCode().equalsIgnoreCase(BAPLicenseKey.BAP.getModuleCode())) {
//            Integer valueFromSCDog = getValueFromSCDog(Base64Util.decode(licenseInfoPO.getLicenseKey()));
            Integer valueFromSCDog = value;
            if (ObjectUtils.isEmpty(valueFromSCDog) || valueFromSCDog.equals(-1)) {
                licenseInfoVO.setDescription(Constants.noLicenseDes + "(最大" + 5 + "用户数)");
            } else if (valueFromSCDog == 255) {
                licenseInfoVO.setDescription(Constants.haveLicenseDes + "(无限制用户数)");
            } else if (valueFromSCDog < 5) {
                licenseInfoVO.setDescription(Constants.haveLicenseDes + "(最大" + 5 + "用户数)");
            } else {
                licenseInfoVO.setDescription(Constants.haveLicenseDes + "(最大" + valueFromSCDog + "用户数)");
            }
        } else {
            //实体和其他模块
            if (Constants.noLicense.equals(value)) {
                licenseInfoVO.setDescription(Constants.noLicenseDes);
            } else if (Constants.trialLicense.equals(value)) {
                //试用期内
                //判断是否过期
                long surplusTime;
                long decodeTime = Long.parseLong(Base64Util.decode(licenseInfoPO.getTime()));
                if (BAPLicenseKey.EC_MODULE.getModuleCode().equalsIgnoreCase(licenseInfoVO.getModuleCode())) {
                    surplusTime = Constants.ecEntityTime - (DateHelper.time() - decodeTime);
                } else {
                    surplusTime = Constants.moduleTime - (DateHelper.time() - decodeTime);
                }
                if (surplusTime <= 0) {
                    licenseInfoVO.setDescription(Constants.noLicenseDes);
                } else {
                    String timeDes = DateHelper.millToHour((int) surplusTime);
                    licenseInfoVO.setDescription(String.format(Constants.trialLicenseDes, timeDes));
                }
            } else if (value >= Constants.haveLicense) {
                licenseInfoVO.setDescription(Constants.haveLicenseDes);
            }
        }
    }

    @Override
    public Integer getLicenseInfoByLicenseKeyFromRegistry(String licenseKey) {
        List<LicenseInfoPO> licenseInfoPOs = this.list(new QueryWrapper<LicenseInfoPO>().lambda().eq(LicenseInfoPO::getLicenseKey, Base64Util.encode(licenseKey)));
        if (ObjectUtils.isEmpty(licenseInfoPOs)) {
            throw new IdentityProviderException(IdentityProviderErrorEnum.LICENSE_KEY_NOT_EXIST);
        }
        return Integer.valueOf(Base64Util.decode(licenseInfoPOs.get(0).getValue()));
    }
}
