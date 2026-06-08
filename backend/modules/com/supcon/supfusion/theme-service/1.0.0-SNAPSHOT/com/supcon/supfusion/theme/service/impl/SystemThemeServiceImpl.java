package com.supcon.supfusion.theme.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.theme.common.exception.ThemeErrorEnum;
import com.supcon.supfusion.theme.common.exception.ThemeException;
import com.supcon.supfusion.theme.common.util.FileTypeUtils;
import com.supcon.supfusion.theme.dao.SystemThemeMapper;
import com.supcon.supfusion.theme.dao.po.SystemThemePO;
import com.supcon.supfusion.theme.service.SystemThemeService;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
@Service
public class SystemThemeServiceImpl extends ServiceImpl<SystemThemeMapper, SystemThemePO> implements SystemThemeService {

    @Autowired
    private HttpServletResponse response;

    @Override
    public List<SystemThemePO> querySystemThemeList() {
        List<SystemThemePO> systemThemePOList = list();
        return systemThemePOList;
    }

    @Override
    public SystemThemePO querySystemThemePO() {
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq(SystemThemePO.getStatusFieldName(), 1);
        SystemThemePO systemThemePO = getOne(queryWrapper);
        return systemThemePO;
    }

    @Override
    public SystemThemePO querySystemThemePOByTheme(String theme) {
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq(SystemThemePO.getThemeFieldName(), theme);
        SystemThemePO systemThemePO = getOne(queryWrapper);
        return systemThemePO;
    }

    @Override
    @Transactional
    public void updateSystemTheme(SystemThemePO systemThemePO) {
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq(SystemThemePO.getStatusFieldName(), 1);
        List<SystemThemePO> systemThemePOList = list(queryWrapper);
        for(SystemThemePO systemTheme : systemThemePOList){
            systemTheme.setStatus(0);
            UpdateWrapper updateWrapper = new UpdateWrapper();
            updateWrapper.eq(SystemThemePO.getThemeFieldName(), systemTheme.getTheme());
            update(systemTheme, updateWrapper);
        }

        systemThemePO.setStatus(1);
        UpdateWrapper updateWrapper = new UpdateWrapper();
        updateWrapper.eq(SystemThemePO.getThemeFieldName(), systemThemePO.getTheme());
        update(systemThemePO, updateWrapper);
    }

    @Override
    public void uploadLogo(MultipartFile imgFile, String theme) {
        String originalFilename = imgFile.getOriginalFilename();
        // 获取文件的格式
        String fileType = originalFilename.substring(originalFilename.lastIndexOf(".") + 1, originalFilename.length());
        if (FileTypeUtils.toFileType(fileType)) {
            if (!imgFile.isEmpty()) {
                String generateFilePath = "/var/images/";
                File targetImg = new File(generateFilePath);
                // 判断文件夹是否存在
                if (!targetImg.exists()) {
                    targetImg.mkdirs();    //级联创建文件夹
                }
                try {
                    // 开始保存图片
                    // 保存到服务器中
                    targetImg.createNewFile();
                    imgFile.transferTo(Paths.get(generateFilePath + originalFilename));
                } catch (IOException e) {
                    throw new ThemeException(ThemeErrorEnum.UPLOAD_ERROR);
                }
                String reUrl = "/theme/logo/";
                response.setHeader("Access-Control-Allow-Origin", "*");
                response.setHeader("Content-type", "application/json;charset=UTF-8");
                response.setStatus(HttpStatus.SC_OK);
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("filePath", reUrl + originalFilename);

                try {
                    response.getWriter().write(jsonObject.toJSONString());
                } catch (IOException e) {
                    throw new ThemeException(ThemeErrorEnum.UPLOAD_ERROR);
                }

//                QueryWrapper queryWrapper = new QueryWrapper();
//                queryWrapper.eq(SystemThemePO.getThemeFieldName(), theme);
//                SystemThemePO systemThemePO = getOne(queryWrapper);
//                systemThemePO.setLogo(reUrl + originalFilename);
//                updateById(systemThemePO);
            }
        } else {
            response.setHeader("Access-Control-Allow-Origin", "*");
            response.setHeader("Content-type", "application/json;charset=UTF-8");
            response.setStatus(HttpStatus.SC_BAD_REQUEST);
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("message", "上传文件格式错误!");
            try {
                response.getWriter().write(jsonObject.toJSONString());
            } catch (IOException e) {
                log.error("Upload Failed:{}", e);
            }
        }
    }
}
