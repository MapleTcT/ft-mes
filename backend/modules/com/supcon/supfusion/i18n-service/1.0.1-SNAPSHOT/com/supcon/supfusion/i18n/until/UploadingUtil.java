package com.supcon.supfusion.i18n.until;

import com.supcon.supfusion.framework.cloud.i18n.context.utils.TenantUtil;
import org.apache.commons.lang3.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.i18n.common.execption.I18nErrorEnum;
import com.supcon.supfusion.i18n.common.execption.I18nException;
import com.supcon.supfusion.i18n.common.until.Constants;
import com.supcon.supfusion.i18n.dao.ExcelDao;
import com.supcon.supfusion.i18n.dao.I18nTokenDao;
import com.supcon.supfusion.i18n.dao.po.ExcelPO;
import com.supcon.supfusion.i18n.dao.po.I18nTokenPO;

public class UploadingUtil {


    public static void getUpLoadingState(ExcelDao excelDao, I18nTokenDao i18nTokenDao) {
    	String tenantId = TenantUtil.getTenantId();
    	LambdaQueryWrapper<ExcelPO> queryWrapper = new QueryWrapper<ExcelPO>().lambda()
								.eq(ExcelPO::getStatus, Constants.ONE_INT)
								.eq(ExcelPO::getOperateType, Constants.STR_IMPORT)
								.eq(ExcelPO::getValid, Constants.ONE_STR)
								.isNotNull(ExcelPO::getFileName)
								.eq(ExcelPO::getTenantId, tenantId);
        //当前是否有人正在上传excel 有则直接返回不让操作
        Integer uploadExcelNum = excelDao.selectCount(queryWrapper);
		//当前其他模块是否有正在上传资源
		Integer uploadFileNum = i18nTokenDao.selectCount(new QueryWrapper<I18nTokenPO>());
		if ((uploadExcelNum!=null && uploadExcelNum > 0) || (uploadFileNum!=null && uploadFileNum>0)) {
			throw new I18nException(I18nErrorEnum.FILE_UPLOADING_ERROR);
		}
    }

    // 针对messageResourceAddOrUpdateOne 使用, 添加新增时的token 校验
	public static void getUpLoadingStateForAddOrUpdateOne(ExcelDao excelDao, I18nTokenDao i18nTokenDao, String moduleCode) {
		String tenantId = TenantUtil.getTenantId();
		LambdaQueryWrapper<ExcelPO> queryWrapper = new QueryWrapper<ExcelPO>().lambda()
				.eq(ExcelPO::getStatus, Constants.ONE_INT)
				.eq(ExcelPO::getOperateType, Constants.STR_IMPORT)
				.eq(ExcelPO::getValid, Constants.ONE_STR)
				.isNotNull(ExcelPO::getFileName)
				.eq(ExcelPO::getTenantId, tenantId);
		//当前是否有人正在上传excel 有则直接返回不让操作
		Integer uploadExcelNum = excelDao.selectCount(queryWrapper);
		//当前其他模块是否有正在上传资源
		//Integer uploadFileNum = i18nTokenDao.selectCount(new QueryWrapper<I18nTokenPO>());
		Integer uploadFileNum = i18nTokenDao.selectCount(new QueryWrapper<I18nTokenPO>().lambda().eq(I18nTokenPO::getModuleCode, moduleCode));
		if ((uploadExcelNum!=null && uploadExcelNum > 0) || (uploadFileNum!=null && uploadFileNum>0)) {
			throw new I18nException(I18nErrorEnum.FILE_UPLOADING_ERROR);
		}
	}

    public static void getExcelUpLoadingState(ExcelDao excelDao) {
    	LambdaQueryWrapper<ExcelPO> queryWrapper = new QueryWrapper<ExcelPO>().lambda()
    			.eq(ExcelPO::getStatus, Constants.ONE_INT)
    			.eq(ExcelPO::getOperateType, Constants.STR_IMPORT)
    			.eq(ExcelPO::getValid, Constants.ONE_STR)
    			.isNotNull(ExcelPO::getFileName);
    	String tenantId = TenantUtil.getTenantId();
    	if (StringUtils.isNotEmpty(tenantId)) {
    		queryWrapper.eq(ExcelPO::getTenantId, tenantId);
    	}
        //当前是否有人正在上传excel 有则直接返回不让操作
        Integer uploadExcelNum = excelDao.selectCount(queryWrapper);
        if (uploadExcelNum!=null && uploadExcelNum > 0) {
            throw new I18nException(I18nErrorEnum.FILE_UPLOADING_ERROR);
        }
    }
}
