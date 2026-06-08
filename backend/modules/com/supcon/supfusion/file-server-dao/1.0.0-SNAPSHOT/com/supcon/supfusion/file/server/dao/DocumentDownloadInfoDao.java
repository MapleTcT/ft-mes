package com.supcon.supfusion.file.server.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supcon.supfusion.file.server.dao.po.DocumentDownloadInfoPO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface DocumentDownloadInfoDao extends BaseMapper<DocumentDownloadInfoPO> {

    List<DocumentDownloadInfoPO> selectByIdAndRecordTypeAndDownloadStaffIdPage(@Param("id") Long id,
                                                                               @Param("offset")Integer offset,
                                                                               @Param("limit")Integer limit,
                                                                               @Param("recordType")String recordType,
                                                                               @Param("downloadStaffId")String downloadStaffId);
}
