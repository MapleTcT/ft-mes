package com.supcon.supfusion.configuration.services.utils;

import com.supcon.supfusion.configuration.services.exceptions.EcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class XPathUtil {
	private static final Logger logger = LoggerFactory.getLogger(XPathUtil.class);
	
	private static DocumentBuilder builder;
	private static TransformerFactory tf;

	/**
	 * 根据xpath获取对应节点列表
	 * 
	 * @param config
	 *            配置文件
	 * @param path
	 *            xpath路径
	 * @return NodeList
	 */
	@SuppressWarnings("rawtypes")
	public static NodeList getNodeListByXPath(String config, String path) {
		NodeList nodeList = null;
		if (config != null && config.indexOf("<?xml version=\"1.0\" encoding=\"UTF-8\"?>") == -1) {
			config = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + config;
		}
		if (config != null && config.length() > 0) {
			XPath xpath = XPathFactory.newInstance().newXPath();
			xpath.setNamespaceContext(new NamespaceContext() {

				@SuppressWarnings("rawtypes")
				@Override
				public Iterator getPrefixes(String namespaceURI) {
					return null;
				}

				@Override
				public String getPrefix(String namespaceURI) {
					return null;
				}

				@Override
				public String getNamespaceURI(String prefix) {
					if ("ec".equals(prefix))
						return "http://bap.supcon.com/xml/module/config";
					else
						return null;
				}
			});
			try {
				if (config.contains("xmlns=\"http://bap.supcon.com/xml/module/config\"")) {
					path = "//ec:" + path.substring(2);
				} else {
					path = "//" + path.substring(2);
				}
				nodeList = (NodeList) xpath.evaluate(path, new InputSource(new StringReader(config)), XPathConstants.NODESET);
			} catch (XPathExpressionException e) {
				throw new EcException(e.getMessage());
			}
		}
		return nodeList;
	}

	/**
	 * 根据节点名称获取节点
	 * 
	 * @param nodeList
	 * @param nodeName
	 * @return
	 */
	public static Node getNodeByName(NodeList nodeList, String nodeName) {
		Node node = null;
		if (nodeList != null && nodeList.getLength() > 0) {
			for (int i = 0; i < nodeList.getLength(); i++) {
				Node nd = nodeList.item(i);
				if (nd.getNodeName().equals(nodeName)) {
					node = nd;
					break;
				}
			}
		}
		return node;
	}

	
	/**
	 * 根据节点名称获取节点MAP
	 * 
	 * @param nodeList
	 * @param names
	 * @return
	 */
	public static Map<String, Node> getNodeByName(NodeList nodeList, List<String> names) {
		Map<String, Node> map = new LinkedHashMap<String, Node>();
		map = getNodeByName(nodeList, names.toArray());
		return map;
	}

	/**
	 * 根据节点名称获取节点MAP
	 * 
	 * @param nodeList
	 * @param names
	 * @return
	 */
	public static Map<String, Node> getNodeByName(NodeList nodeList, Object... names) {
		Map<String, Node> map = new LinkedHashMap<String, Node>();
		if (names != null && names.length > 0 && nodeList != null && nodeList.getLength() > 0) {
			for (int i = 0; i < nodeList.getLength(); i++) {
				Node nd = nodeList.item(i);
				for (Object name : names) {
					if (nd.getNodeName().equals(name.toString())) {
						map.put(name.toString(), nd);
						break;
					}
				}
			}
		}
		return map;
	}

	/**
	 * 根据xpath获取对应节点列表
	 * 
	 * @param xmlStr
	 *            配置文件
	 * @param name
	 *            name节点名
	 * @return NodeList
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 */
	@SuppressWarnings("rawtypes")
	public static NodeList getNodeListByName(String xmlStr, String name) throws ParserConfigurationException, SAXException, IOException {
		Document document = newDocument(xmlStr);
		return document.getDocumentElement().getElementsByTagName(name);
	}

	/**
	 * 根据xml生成document
	 * 
	 * @param xmlStr
	 * @return
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	public static Document newDocument(String xmlStr){
		InputStream is = new ByteArrayInputStream(xmlStr.getBytes());
		Document document;
		try {
			builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			document = builder.parse(is);
			return document;
		} catch (ParserConfigurationException | SAXException | IOException e) {
			logger.error(e.getMessage(), e);
		}
		return null;
	}

	/**
	 * 根据document生成字符串
	 * 
	 * @param document
	 * @return
	 * @throws TransformerException
	 * @throws IOException
	 */
	public static String getXmlString(Document document) throws TransformerException, IOException {
		tf = TransformerFactory.newInstance();
		Transformer t = tf.newTransformer();
		//t.setOutputProperty("encoding", "UTF-8");// 解决中文问题，试过用GBK不行
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		t.transform(new DOMSource(document), new StreamResult(bos));
		String xmlStr = bos.toString();
		if(xmlStr.contains("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>")){
			xmlStr = xmlStr.replace("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>", "");
		}
		bos.close();
		return xmlStr;
	}

	/**
	 * 根据节点名与文本生成节点 如 <element><![CDATA[data]]></element>
	 * 
	 * @param doc
	 * @param elName
	 * @param data
	 * @return
	 */
	public static Element getElement(Document doc, String elName, String data) {
		Element element = doc.createElement(elName);
		CDATASection section = doc.createCDATASection(data);
		element.appendChild(section);
		return element;
	}
}
