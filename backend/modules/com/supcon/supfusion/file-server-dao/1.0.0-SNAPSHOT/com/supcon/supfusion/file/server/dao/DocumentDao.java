package com.supcon.supfusion.file.server.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supcon.supfusion.file.server.dao.po.DocumentPO;
import org.apache.ibatis.annotations.Param;

import java.util.List;


public interface DocumentDao extends BaseMapper<DocumentPO> {

    void save(DocumentPO doc);

    void deleteByFilePath(String filepath);

    void deleteByLindId(String linkId);

    List<DocumentPO> findByByLinkId(@Param("linkId1")Long linkId1);

    List<DocumentPO> findByCreateStaffId(@Param("createStaffId")Long createStaffId);

    List<DocumentPO> findByLinkIdAndType(@Param("linkId") Long linkId, @Param("type") String type);

    List<DocumentPO> findByLinkIdAndTypeAndPropertyCode(@Param("linkId") Long linkId,
                                                        @Param("type") String type,
                                                        @Param("propertyCode") String propertyCode);

    List<DocumentPO> getPictureByLinkIdAndType(@Param("linkId") Long linkId,
                                               @Param("type") String type,
                                               @Param("propertyCode") String propertyCode);

    List<DocumentPO> findByLinkIdAndTypeAndPropertyCodeAndShowType(@Param("linkId") Long linkId,
                                                                   @Param("type") String type,
                                                                   @Param("propertyCode") String propertyCode,
                                                                   @Param("showType")String showType);
    List<DocumentPO> selectAllByValid();

    List<DocumentPO> findByLinkIdAndTypeAndPropertyCodeNoThere(@Param("linkId") Long linkId,
                                                               @Param("type") String type,
                                                               @Param("propertyCode") String propertyCode);

    List<DocumentPO> findByOpener(@Param("userName")String userName);

    List<DocumentPO> findByLinkIdAndTypesAndFileTypes(@Param("linkId") Long linkId,
                                                      @Param("type") String type);








    void saveBatch(List<DocumentPO> documentPOS);

    List<DocumentPO> selectListByLinkIdAndType(@Param("linkId")Long linkId,
                                               @Param("type")String type,
                                               @Param("fileType")String fileType,
                                               @Param("propertyCode")String propertyCode);

    List<DocumentPO> selectListByLinkIdsAndType(@Param("linkIds") List<Long> linkIds,
                                                @Param("type")String type,
                                                @Param("fileType")String fileType,
                                                @Param("propertyCode")String propertyCode);

    List<DocumentPO> selectAllByValidIsFalse();

    void deleteByDocumentPOId(long id);

    void deleteByFileId(@Param("id") Long id);

    List<DocumentPO> selectListByLinkIdAndTypeAndPropertyCode(@Param("linkIds") Long linkId,
                                                              @Param("type")String type,
                                                              @Param("propertyCode")String propertyCode);

    DocumentPO selectByFilePath(@Param("filePath")String filePath);

    void updatedDownloadTimesByOneId(DocumentPO documentPO1);

    void updatedPreviewTimesByOneId(DocumentPO documentPO1);

    DocumentPO selectByFileId(@Param("fileId")Long fileId);

    void updateAllById(DocumentPO documentPO);

    void updatedConvertReasonByOneId(DocumentPO documentPO);

    List<DocumentPO> selectAllByIsNotConvert();

}
