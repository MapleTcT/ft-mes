/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package com.supcon.supfusion.ws;

import com.supcon.supfusion.ws.service.registry.Registry;
import com.supcon.supfusion.ws.service.util.ClassScanner;
import com.supcon.supfusion.ws.service.util.PropertiesManager;
import com.supcon.supfusion.ws.service.websocket.WebsocketServer;
import java.util.concurrent.CountDownLatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Application {
    private static final Logger log = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        try {
            CountDownLatch countDownLatch = new CountDownLatch(1);
            new WebsocketServer().init(countDownLatch);
            ClassScanner classScanner = new ClassScanner();
            classScanner.httpScan("com.supcon.supfusion.ws.service.http.controller");
            classScanner.wsScan("com.supcon.supfusion.ws.service.websocket.controller");
            int wsPort = PropertiesManager.getInt("ws.port", 30135);
            String nacosGroup = PropertiesManager.getNacosGroup();
            Registry.registry("ws", nacosGroup, wsPort);
            countDownLatch.await();
        }
        catch (Exception e) {
            log.error("server start error", (Throwable)e);
        }
    }
}

