package com.supcon.orchid.msgcenter.websocket;

import com.google.gson.Gson;
import com.supcon.orchid.msgcenter.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArraySet;

//import org.springframework.amqp.core.Message;

@ServerEndpoint(value = "/webSocket/{group}")
@Component
public class WebSocketServer {

    private Session session;

    public static CopyOnWriteArraySet<WebSocketServer> webSockets = new CopyOnWriteArraySet<WebSocketServer>();

    public final Logger logger = LoggerFactory.getLogger(this.getClass());

    public static Map<String, CopyOnWriteArraySet<WebSocketServer>> serverMapping = new HashMap<>();

    @OnOpen
    public void onOpen(Session session, @PathParam("group") String group) throws InterruptedException {
        this.session = session;
        CopyOnWriteArraySet<WebSocketServer> webSocketServers = new CopyOnWriteArraySet<WebSocketServer>();
        if (serverMapping.get(group) != null && serverMapping.get(group).size() > 0) {
            webSocketServers.addAll(serverMapping.get(group));
        }
        webSocketServers.add(this);
        serverMapping.put(group, webSocketServers);
        webSockets.add(this);
        this.send("新用户" + group + "加入");
        logger.info("现有用户：" + new Gson().toJson(serverMapping));
    }

    @OnClose
    public void onClose(Session s, @PathParam("group") String group) {
        logger.info("有" + group + "用户离开");
        webSockets.remove(this);
        CopyOnWriteArraySet<WebSocketServer> webSocketServers = serverMapping.get(group);
        webSocketServers.remove(this);
        serverMapping.put(group, webSocketServers);
    }

    @OnMessage
    public void onMessage(String msg, Session session) throws InterruptedException {
        if (AppConfig.websocket_PING.equals(msg)) {
            this.send(AppConfig.websocket_PONG);
        }
//        logger.info("从客户端接受的消息： " + msg);
    }

    public void send(String msg) {
        try {
            synchronized (session) {
                this.session.getBasicRemote().sendText(msg);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }
}
