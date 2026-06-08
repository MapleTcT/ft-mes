package com.supcon.supfusion.file.server.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.file.server.dao.FileDaoMapper;
import com.supcon.supfusion.file.server.dao.po.DocumentPO;
import com.supcon.supfusion.file.server.service.FileDaoService;
import org.springframework.stereotype.Service;

@Service
public class FileDaoServiceImpl extends ServiceImpl<FileDaoMapper, DocumentPO> implements FileDaoService {
}
