package com.supcon.supfusion.authkeycloak;

import com.supcon.supfusion.authkeycloak.configure.PropertiesConfigure;
import com.supcon.supfusion.authkeycloak.discovery.Registry;
import lombok.extern.jbosslog.JBossLog;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.Config;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author lifangyuan
 */
@JBossLog
public class SuposUserStorageProviderFactory implements org.keycloak.storage.UserStorageProviderFactory<SuposUserProviderImpl> {
    public static final String PROVIDER_NAME = "readonly-property-file";

    @Override
    public String getId() {
        return PROVIDER_NAME;
    }

    @Override
    public void init(Config.Scope config) {
        initProperties();
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
        if ("Linux".equalsIgnoreCase(System.getProperty("os.name"))) {
            scheduledExecutorService.schedule(()->{
                try {
                    log.info("update realm ssl");
                    Process process = Runtime.getRuntime().exec("/opt/jboss/keycloak/bin/kcadm.sh config credentials --server http://localhost:8080/auth --realm master --user admin --password admin");
                    process.waitFor();
                    Process process1 = Runtime.getRuntime().exec("/opt/jboss/keycloak/bin/kcadm.sh update realms/master -s sslRequired=NONE");
                    process1.waitFor();
                } catch (Exception e) {
                    log.error("error is =====",e);
                }
            },1, TimeUnit.MINUTES);
        }
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            try {
                log.info("register nacos");
                Enumeration interfaces = NetworkInterface.getNetworkInterfaces();
                while (interfaces.hasMoreElements()) {
                    NetworkInterface ni = (NetworkInterface) interfaces.nextElement();
                    Enumeration ipAddrEnum = ni.getInetAddresses();
                    while (ipAddrEnum.hasMoreElements()) {
                        InetAddress addr = (InetAddress) ipAddrEnum.nextElement();
                        if (addr.isLoopbackAddress() == true) {
                            continue;
                        }

                        String ip = addr.getHostAddress();
                        if (ip.indexOf(":") != -1) {
                            //skip the IPv6 addr
                            continue;
                        }
                        String port = PropertiesConfigure.getProperties().getProperty("authkeycloak.port", "8080");
                        String group = PropertiesConfigure.getProperties().getProperty("nacos.group", "DEFAULT_GROUP");
                        Registry.getNamingService().registerInstance("keycloak", group, ip, Integer.parseInt(port));
                    }
                }
            } catch (Exception e) {
                log.error("nacos is not ok,error message ", e);
                System.exit(0);
            }
        },0,1,TimeUnit.MINUTES);

    }

    @Override
    public SuposUserProviderImpl create(KeycloakSession session, ComponentModel model) {
        SuposUserProviderImpl userProvider = new SuposUserProviderImpl(session, model);
        ResteasyProviderFactory.getInstance().injectProperties(userProvider);
        return userProvider;
    }

    /**
     * 加载配置
     */
    private void initProperties() {
        try {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("auth-keycloak/application.properties");
            PropertiesConfigure.getProperties().load(inputStream);
        } catch (IOException e) {
            log.error("load configure properties is error", e);
        }

        List<String> propertiesNames = new ArrayList() {{
            add("nacos.address");
            add("nacos.group");
            add("authkeycloak.port");
            add("nationalEncry-enable");
        }};
        Properties properties = PropertiesConfigure.getProperties();
        propertiesNames.forEach(propertiesName -> {
            String value = System.getProperty(propertiesName);
            if (null != value) {
                properties.setProperty(propertiesName, value);
            }
        });
    }

}
