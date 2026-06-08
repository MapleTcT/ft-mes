package com.supcon.supfusion.rbac.service.upgrade;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.supcon.supfusion.rbac.dao.po.MenuAppDesignerRelPO;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.springframework.util.CollectionUtils;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class GetAppTreeFromOODMThread extends Thread {
    private static volatile boolean isExit = false;
    private int count = 50;
    private final GetAppTreeCallback callback;
    private final String appId;
    private final String tenantId;
    private final CountDownLatch latch;

    GetAppTreeFromOODMThread(GetAppTreeCallback callback, String tenantId, String appId, CountDownLatch latch) {
        this.callback = callback;
        this.tenantId = tenantId;
        this.appId = appId;
        this.latch = latch;
        setDaemon(true);
        setName("upgrade-find-app-menus-from-oodm-" + appId);
    }

    @Override
    public void run() {
        while (!isExit && count > 0) {
            count--;
            List<MenuAppDesignerRelPO> menuAppDesignerRelPOS = null;
            try {
                menuAppDesignerRelPOS = findMenuTreeListFromOODM();
            } catch (Exception e) {
                try {
                    Thread.sleep(3000L);
                } catch (InterruptedException ignore) {
                }
                continue;
            }
            count = 0;
            if (!CollectionUtils.isEmpty(menuAppDesignerRelPOS)) {
                this.callback.callback(menuAppDesignerRelPOS);
            }
        }
        this.latch.countDown();
    }

    private List<MenuAppDesignerRelPO> findMenuTreeListFromOODM() throws Exception {
        // 请求元数据
        String url = "http://compose-manage:8080/api/compose/manage/folders?parentId=" + appId;
        HttpClient client = new DefaultHttpClient();
        // 定义请求的参数
        URI uri = new URIBuilder(url).build();
        // 创建http GET请求
        HttpGet request = new HttpGet(uri);
        request.setHeader("X-Tenant-Id", tenantId);
        HttpResponse response = client.execute(request);
        /**请求发送成功，并得到响应**/
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            /**读取服务器返回过来的json字符串数据**/
            String result = EntityUtils.toString(response.getEntity());
            JSONObject jsonObject = JSON.parseObject(result);
            // 解析数据
            return rbacDataUpgred(jsonObject, new LinkedList<>());
        }
        return new LinkedList<>();
    }

    private List<MenuAppDesignerRelPO> rbacDataUpgred(JSONObject jsonObject, List<MenuAppDesignerRelPO> menuAppDesignerPOList) {
        if (null == jsonObject) {
            return null;
        }
        // 解析数据
        MenuAppDesignerRelPO menuAppDesignerPO = new MenuAppDesignerRelPO();
        menuAppDesignerPO.setCode(jsonObject.getString("id"));
        menuAppDesignerPO.setParentCode(jsonObject.getString("parentId"));
        menuAppDesignerPO.setAppId(jsonObject.getString("appId"));
        menuAppDesignerPOList.add(menuAppDesignerPO);
        JSONArray childrenList = JSONArray.parseArray(jsonObject.getString("children"));
        if (!CollectionUtils.isEmpty(childrenList)) {
            for (int i = 0, size = childrenList.size(); i < size; i++) {
                JSONObject itemJson = childrenList.getJSONObject(i);
                rbacDataUpgred(itemJson, menuAppDesignerPOList);
            }
        }
        return menuAppDesignerPOList;
    }

    public static void exit() {
        GetAppTreeFromOODMThread.isExit = true;
    }
}