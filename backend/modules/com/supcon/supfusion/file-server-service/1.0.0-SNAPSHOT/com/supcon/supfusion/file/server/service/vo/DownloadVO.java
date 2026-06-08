package com.supcon.supfusion.file.server.service.vo;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@ApiModel(description = "下载详情")
public class DownloadVO {

   private Integer first;
   private Boolean hasNext;
   private Boolean hasPre;
   private Integer nextPage;
   private Integer pageNo;
   private Integer pageSize;
   private List<DocumentDownloadInfoVO> result = new ArrayList<>();
   private Integer totalCount;
   private Integer totalPages;

}
