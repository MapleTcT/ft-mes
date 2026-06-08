package com.supcon.supfusion.notification.app.config.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;

/**
 * ${description}
 *
 * @author chenweinan
 * @create 2020/7/13 14:30
 */
@Slf4j
public class HttpClient {

    private static final Log log = LogFactory.getLog(HttpClient.class);
    public static final String DEFAULT_QUESTION_MARK = "?";

    /**
     * 发送GET请求
     *
     * @param url   请求地址
     * @param param 请求参数
     * @author Abin 2017-06-30
     */
    public static String sendGet(String url, String param) {
        BufferedReader in = null;
        String result = "";
        try {
            log.info("send get request url:" + url);
            log.info("send get request param:" + param);
            URL realUrl = new URL(url + DEFAULT_QUESTION_MARK + param);
            // 打开和URL之间的连接
            URLConnection conn = realUrl.openConnection();
            // 设置通用的请求属性
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("connection", "Keep-Alive");
            conn.setRequestProperty("user-agent",
                    "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            //conn.setRequestProperty("Content-Type", "application/json");
            //conn.setRequestProperty("Charsert", "UTF-8");

            conn.connect();
            in = new BufferedReader(new InputStreamReader(
                    conn.getInputStream(), "UTF-8"));
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
        } catch (Exception e) {
            log.error("faild to send get request url:" + url + ",param:" + param, e);
        }
        // 关闭输出、输入流
        finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                log.error(ex.getMessage(), ex);
            }
        }
        log.info("send get request ,response info" + result);
        return result;
    }

    /**
     * 发送POST请求
     *
     * @param url   请求地址
     * @param param 请求参数
     * @author Abin 2017-06-30
     */
    public static String sendPost(String url, String param) {
        PrintWriter out = null;
        BufferedReader in = null;
        String result = "";
        try {
            log.info("send post request url:" + url);
            log.info("send post request param:" + param);
            URL realUrl = new URL(url);
            // 打开和URL之间的连接
            URLConnection conn = realUrl.openConnection();
            // 设置通用的请求属性
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("connection", "Keep-Alive");
            conn.setRequestProperty("user-agent",
                    "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            //conn.setRequestProperty("contentType", "UTF-8");
            // 发送POST请求设置
            //conn.setRequestProperty("Content-Type","application/json;charset=UTF-8");
            conn.setRequestProperty("Accept-Charset", "UTF-8");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            // 获取URLConnection对象对应的输出流
            out = new PrintWriter(conn.getOutputStream());
            // 发送请求参数
            out.print(param);
            // flush输出流的缓冲
            out.flush();
            // 定义BufferedReader输入流来读取URL的响应
            in = new BufferedReader(new InputStreamReader(
                    conn.getInputStream(), "UTF-8"));
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
        } catch (Exception e) {
            log.error("faild to send post request url:" + url + ",param:" + param, e);
        }
        // 关闭输出、输入流
        finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                log.error(ex.getMessage(), ex);
            }
        }
        log.info("send post request ,response info" + result);
        return result;
    }
}
