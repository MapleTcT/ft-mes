package com.supcon.supfusion.printer.common;

/**
 * 通信服务封装类
 * @author liyiming
 * @date 2020/12/4 8:58 上午
 */
public class RpcUtil {

    /**
     * 判断通信请求是否成功
     * @param jb
     * @return
     */
    public static boolean isSuccess(JSONObject jb) {
        try {
            if (null != jb && (int)jb.get("code") == 200) {
                return true;
            }
        } catch(Exception e) {
            return false;
        }
        return false;
    }
}
