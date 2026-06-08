package com.supcon.supfusion.configuration.services.service;

import com.supcon.supfusion.configuration.services.entity.Property;

import java.io.IOException;
import java.util.List;

public interface PropertyService {
	Property getProperty(String propertyCode);
	List<Property> getProperties(String modelCode);
	void addCounterRule(Property property);
	void deleteCounter(Property property);
	void save(Property property);
}
