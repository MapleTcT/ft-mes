package com.supcon.supfusion.iam.webapi;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supcon.supfusion.framework.cloud.annotation.InternalApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Pagination;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.common.util.DateTimeUtil;
import com.supcon.supfusion.iam.common.bean.Order;
import com.supcon.supfusion.iam.dao.entity.AccountPO;
import com.supcon.supfusion.iam.service.AKSKService;
import com.supcon.supfusion.iam.service.bo.AccountBO;
import com.supcon.supfusion.iam.webapi.vo.AKSKAddVO;
import com.supcon.supfusion.iam.webapi.vo.AKSKBatchDeleteVO;
import com.supcon.supfusion.iam.webapi.vo.AKSKUpdateVO;
import com.supcon.supfusion.iam.webapi.vo.AKSKVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@ResponseBody
@Api(description = "AK/SK-API", tags = {"AK/SK管理API"})
@InternalApi(path = HttpConstants.URL_INTERNALAPI + HttpConstants.URL_SPLITER + "supos-iam" + HttpConstants.URL_SPLITER + "v1")
public class AKSKController {
    @Autowired
    private AKSKService akskService;

    private static final String TITLE = "密钥ID,密钥key";

    /**
     * 关键字查询
     */
    @ApiOperation(value = "关键字查询")
    @GetMapping(value = "/iam/aksks")
    public PageResult<AKSKVO> queryListByKeyword(@ApiParam(value = "order", required = false) @RequestParam(required = false) Order order,
                                                 @ApiParam(value = "current", required = false) @RequestParam(required = false) Integer current,
                                                 @ApiParam(value = "pageSize", required = false) @RequestParam(required = false) Integer pageSize) {
        Integer num = 1;
        Integer size = 20;
        if (current != null) {
            num = current;
        }
        if (pageSize != null) {
            size = pageSize;
        }
        Page<AccountPO> page = akskService.queryListByKeyword(null, order, num, size);

        List<AKSKVO> akskvos = new ArrayList<>();
        Pagination pagination = new Pagination();
        pagination.setPageSize(size);
        pagination.setCurrent(num);
        if (page != null && page.getRecords() != null) {
            pagination.setTotal((int) page.getTotal());
            List<AccountPO> accountPOS = page.getRecords();
            for (AccountPO accountPO : accountPOS) {
                AKSKVO akskvo = new AKSKVO();
                akskvo.setId(accountPO.getId());
                akskvo.setAppId(accountPO.getUsername());
                akskvo.setDescription(accountPO.getDescription());
                akskvo.setCreateTime(ZonedDateTime.parse(accountPO.getCreateTime(), DateTimeUtil.UTC0_FORMAT).toInstant().atZone(ZoneId.of("UTC")).toInstant().toEpochMilli());
                akskvo.setSystem(accountPO.getSystem());
                akskvo.setDownLoadMark(accountPO.getDownloadMark());
                akskvos.add(akskvo);
            }
        } else {
            pagination.setTotal(0);
        }

        PageResult<AKSKVO> pageResult = new PageResult<>();
        pageResult.setList(akskvos);
        pageResult.setPagination(pagination);
        return pageResult;
    }


    /**
     * AKSK新增
     */
    @ApiOperation(value = "AKSK新增")
    @PostMapping(value = "/iam/aksk")
    public Result<AKSKVO> add(@ApiParam(value = "AKSK对象", required = true) @RequestBody @Valid @NotNull AKSKAddVO akskAddVO) {
        long id = akskService.add(akskAddVO.getAppId(), akskAddVO.getDescription());
        AKSKVO akskvo = new AKSKVO();
        akskvo.setId(id);
        return new Result<>(akskvo);
    }

    /**
     * AKSK更新
     */
    @ApiOperation(value = "AKSK更新")
    @PutMapping(value = "/iam/aksk/{id}")
    public void update(@ApiParam(value = "AKSK对象", required = true) @RequestBody @Valid @NotNull AKSKUpdateVO akskUpdateVO,
                       @PathVariable("id") Long id) {
        akskService.update(id, akskUpdateVO.getDescription());
    }

    /**
     * AKSK批量删除
     */
    @ApiOperation(value = "AKSK删除")
    @PostMapping(value = "/iam/aksk/ids")
    public void batchDelete(@ApiParam(value = "AKSK对象", required = true) @RequestBody @Valid @NotNull AKSKBatchDeleteVO akskBatchDeleteVO) {
        akskService.batchDelete(akskBatchDeleteVO.getIds());
    }

    /**
     * AKSK下载
     */
    @ApiOperation(value = "AKSK下载")
    @GetMapping(value = "/iam/aksk/file")
    public void download(@ApiParam(value = "id", required = true) @RequestParam(required = false) @NotEmpty Long id,
                         HttpServletResponse httpServletResponse) throws IOException {
        AccountBO accountBO = akskService.download(id);

        httpServletResponse.setHeader("Content-Disposition", "attachment;filename=" + new String("AKSK.csv".getBytes("utf-8"), "ISO8859-1"));
        httpServletResponse.setContentType("text/csv; charset=utf-8");
        ServletOutputStream outputStream = httpServletResponse.getOutputStream();
        try (BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream, "utf-8"))) {
            bufferedWriter.append(TITLE);
            bufferedWriter.newLine();
            bufferedWriter.append(accountBO.getAccessKey() + "," + accountBO.getSecretKey());
            bufferedWriter.flush();
        }
    }
}
