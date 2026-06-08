package com.supcon.supfusion.configuration.services.service;

import com.supcon.supfusion.configuration.services.entity.Layout;

import java.util.List;

public interface LayoutService {
	List<Layout> findAll();
	Layout get(String code);
}
