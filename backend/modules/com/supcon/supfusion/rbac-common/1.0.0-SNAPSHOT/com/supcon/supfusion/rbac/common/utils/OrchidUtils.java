/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 
 * All rights reserved.
 */
package com.supcon.supfusion.rbac.common.utils;

public class OrchidUtils {
	

	/**
	 * Encodes the powercode using  with the encoding
	 * specified in the configuration.
	 *
	 * @param input
	 * @return
	 */
	public static byte[] encode(byte[] input) {
		return BAPUrlBase64.encode(input);
	}
}
