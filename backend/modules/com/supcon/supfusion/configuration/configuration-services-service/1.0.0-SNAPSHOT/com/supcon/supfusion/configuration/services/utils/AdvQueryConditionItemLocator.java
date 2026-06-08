package com.supcon.supfusion.configuration.services.utils;

import com.supcon.supfusion.configuration.services.entity.AdvQueryConditionItem;
import flexjson.ClassLocator;
import flexjson.ObjectBinder;
import flexjson.Path;

public class AdvQueryConditionItemLocator implements ClassLocator {
	/**
	 * @param conditionUtil
	 */
	AdvQueryConditionItemLocator() {
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Class locate(ObjectBinder context, Path currentPath) throws ClassNotFoundException {
		return AdvQueryConditionItem.class;
	}
	
}