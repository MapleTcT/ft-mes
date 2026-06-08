package com.supcon.supfusion.rbac.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.rbac.dao.TagMapper;
import com.supcon.supfusion.rbac.dao.po.TagPO;
import com.supcon.supfusion.rbac.service.ITagService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 * 标签表 服务实现类
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-10
 */
@Slf4j
@Service
@Transactional
public class TagServiceImpl extends ServiceImpl<TagMapper, TagPO> implements ITagService {

}
