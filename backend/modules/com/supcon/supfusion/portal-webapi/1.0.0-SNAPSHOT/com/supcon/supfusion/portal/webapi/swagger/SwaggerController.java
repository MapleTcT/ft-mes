package com.supcon.supfusion.portal.webapi.swagger;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * 请求转发api-docs
 * 
 * @author fjh
 *
 */
@Controller
public class SwaggerController {

	@RequestMapping(value = "/*/v1/api-docs",method = RequestMethod.GET)
	public String apiForward() {
		return "forward:/v1/api-docs";
	}
}
