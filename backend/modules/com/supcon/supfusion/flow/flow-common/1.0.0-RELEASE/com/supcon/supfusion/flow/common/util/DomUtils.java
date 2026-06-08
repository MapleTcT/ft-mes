/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: zhuangmh
 * @Date: 2020年5月21日 上午10:42:17
 */
@Slf4j
public class DomUtils {
    
    private DomUtils() {
        throw new IllegalStateException("DomUtils is utility class, do not instantiate");
    }
    
    private static SAXReader reader;
    
    static {
        reader = new SAXReader();
        Map<String, String> namespaceMap = new HashMap<>(4);  
        namespaceMap.put("uri", "http://www.omg.org/spec/BPMN/20100524/MODEL");  
        namespaceMap.put("flowable", "http://flowable.org/bpmn"); 
        reader.getDocumentFactory().setXPathNamespaceURIs(namespaceMap);
        reader.setEncoding(StandardCharsets.UTF_8.name());
    }
    
    public static Document getDocument(String xml) throws UnsupportedEncodingException, DocumentException {
        return reader.read(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8.name())));
    }
    
    public static Document getDocument(InputStream inputStream) throws DocumentException {
        return reader.read(inputStream);
    }
    
    public static String getAttributeValue(Document document, String elementId, String attrName) {
        Node targetNode = document.selectSingleNode(String.format("//*[@id='%s']", elementId));
        if (targetNode instanceof Element) {
            return ((Element)targetNode).attributeValue(attrName);
        }
        log.warn("Target Node do not find, attrName = {}, elementId = {}", attrName, elementId);
        return "";
    }
    
    public static String getAttributeValue(String xml, String xpath, String attrName) {
        try {
            Document document = getDocument(xml);
            Node targetNode = document.selectSingleNode(xpath);
            if (targetNode instanceof Element) {
                return ((Element)targetNode).attributeValue(attrName);
            }
        } catch (Exception e) {
            log.error("xpath({}) 解析错误", xpath, e);
        }
        return "";
    }
}
