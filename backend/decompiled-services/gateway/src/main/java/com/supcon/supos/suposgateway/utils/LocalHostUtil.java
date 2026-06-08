/*
 * Decompiled with CFR 0.152.
 */
package com.supcon.supos.suposgateway.utils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;

public class LocalHostUtil {
    public static String getHostName() throws UnknownHostException {
        return InetAddress.getLocalHost().getHostName();
    }

    public static String getLocalIP() throws UnknownHostException {
        return InetAddress.getLocalHost().getHostAddress();
    }

    public static String[] getLocalIPs() throws SocketException {
        ArrayList<String> list = new ArrayList<String>();
        Enumeration<NetworkInterface> enumeration = NetworkInterface.getNetworkInterfaces();
        while (enumeration.hasMoreElements()) {
            NetworkInterface intf = enumeration.nextElement();
            if (intf.isLoopback() || intf.isVirtual()) continue;
            Enumeration<InetAddress> inets = intf.getInetAddresses();
            while (inets.hasMoreElements()) {
                InetAddress addr = inets.nextElement();
                if (addr.isLoopbackAddress() || !addr.isSiteLocalAddress() || addr.isAnyLocalAddress()) continue;
                list.add(addr.getHostAddress());
            }
        }
        return list.toArray(new String[0]);
    }

    public static boolean isWindowsOS() {
        boolean isWindowsOS = false;
        String osName = System.getProperty("os.name");
        if (osName.toLowerCase().indexOf("windows") > -1) {
            isWindowsOS = true;
        }
        return isWindowsOS;
    }

    public static void main(String[] args) {
        try {
            System.out.println("\u4e3b\u673a\u662f\u5426\u4e3aWindows\u7cfb\u7edf\uff1a" + LocalHostUtil.isWindowsOS());
            System.out.println("\u4e3b\u673a\u540d\u79f0\uff1a" + LocalHostUtil.getHostName());
            System.out.println("\u7cfb\u7edf\u9996\u9009IP\uff1a" + LocalHostUtil.getLocalIP());
            System.out.println("\u7cfb\u7edf\u6240\u6709IP\uff1a" + String.join((CharSequence)",", LocalHostUtil.getLocalIPs()));
        }
        catch (UnknownHostException unknownHostException) {
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}

