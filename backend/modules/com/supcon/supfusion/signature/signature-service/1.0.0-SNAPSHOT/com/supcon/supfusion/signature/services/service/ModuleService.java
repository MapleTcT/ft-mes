package com.supcon.supfusion.signature.services.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.supcon.supfusion.signature.dao.entity.Model;
import com.supcon.supfusion.signature.dao.entity.Module;

import java.util.List;

public interface ModuleService extends IService<Module> {

	List<Module> getAllModule();

}
