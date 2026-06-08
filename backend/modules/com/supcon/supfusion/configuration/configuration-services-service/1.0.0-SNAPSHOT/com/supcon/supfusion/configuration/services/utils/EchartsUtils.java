package com.supcon.supfusion.configuration.services.utils;

import com.supcon.supfusion.configuration.services.entity.EchartsModel;
import com.supcon.supfusion.configuration.services.entity.EchartsXAxis;
import com.supcon.supfusion.configuration.services.entity.EchartsYAxis;
import com.supcon.supfusion.configuration.services.enums.ShowFormat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EchartsUtils {
	
	public static final Map<String, String> showFormats = new LinkedHashMap<String, String>();

	/**
	 * @Description: 处理X轴Y轴，Json转对象
	 *
	 * @param: 数据源列表
	 * @return: null
	 *
	 * @author: huning
	 * @date: 2019年1月17日 上午10:58:00
	 */
	public static void dealXAxisAndYAxis(List<EchartsModel> emodels) {
		for (EchartsModel emodel : emodels) {
			try {
				List<EchartsXAxis> xAxisList = (List<EchartsXAxis>) JSONUtil.generateObjectFromJson(emodel.getXaxisStr(), EchartsXAxis.class, null);
				List<EchartsYAxis> yAxisList = (List<EchartsYAxis>) JSONUtil.generateObjectFromJson(emodel.getYaxisStr(), EchartsYAxis.class, null);
				if (null != xAxisList && !xAxisList.isEmpty()) {
					emodel.setXaxis(xAxisList.get(0));
				}
				if (null != yAxisList && !yAxisList.isEmpty()) {
					emodel.setYaxis(yAxisList.get(0));
				}
			} catch (Exception e) {}
		}
	}
	
	static {
		showFormats.put(ShowFormat.Y.name(), "yyyy");
		showFormats.put(ShowFormat.YM.name(), "yyyy-MM");
		showFormats.put(ShowFormat.YMD.name(), "yyyy-MM-dd");
		showFormats.put(ShowFormat.YMD_H.name(), "yyyy-MM-dd HH");
		showFormats.put(ShowFormat.YMD_HM.name(), "yyyy-MM-dd HH:mm");
		showFormats.put(ShowFormat.YMD_HMS.name(), "yyyy-MM-dd HH:mm:ss");
	}
	
}
