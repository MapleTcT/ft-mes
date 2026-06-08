package com.supcon.supfusion.configuration.services.utils;

import com.microsoft.sqlserver.jdbc.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.StringReader;
import java.util.*;

public class XmlUtils {

	private static final Logger logger = LoggerFactory.getLogger(XmlUtils.class);

	private static final String UNDEFIND_REG_1 = "<[a-zA-z0-9]+><!\\[CDATA\\[[\\s\"]*undefined[\\s\"]*\\]\\]></[a-zA-z0-9]+>";
	private static final String UNDEFIND_REG_1_1 = "<[a-zA-z0-9]+><!\\[CDATA\\[[\\s\"]*null[\\s\"]*\\]\\]></[a-zA-z0-9]+>";
	private static final String UNDEFIND_REG_2 = "<[a-zA-z0-9]+>[ ^.]*<!\\[CDATA\\[[\\s\"]*\\]\\]>[ ^.]*</[a-zA-z0-9]+>";
	private static final String UNDEFIND_REG_3 = "<[a-zA-z0-9]+>[\\s\"]*undefined[\\s\"]*</[a-zA-z0-9]+>";
	private static final String UNDEFIND_REG_3_1 = "<[a-zA-z0-9]+>[\\s\"]*null[\\s\"]*</[a-zA-z0-9]+>";
	private static final String UNDEFIND_REG_4 = "<[a-zA-z0-9]+>[\\s\"]*</[a-zA-z0-9]+>";

	private static SAXReader reader;
	private static final ThreadLocal<SAXReader> readerLocal = new ThreadLocal<SAXReader>();
	public static Object convert(String xml) {
		if (xml == null || xml.isEmpty()) {
			return null;
		}

		Document d;
		try {
			xml = xml.replaceAll(UNDEFIND_REG_1, StringUtils.EMPTY);
			xml = xml.replaceAll(UNDEFIND_REG_1_1, StringUtils.EMPTY);
			xml = xml.replaceAll(UNDEFIND_REG_2, StringUtils.EMPTY);
			xml = xml.replaceAll(UNDEFIND_REG_3, StringUtils.EMPTY);
			xml = xml.replaceAll(UNDEFIND_REG_3_1, StringUtils.EMPTY);
			while (xml.matches(UNDEFIND_REG_4)) {
				xml = xml.replaceAll(UNDEFIND_REG_4, StringUtils.EMPTY);
			}
			d = parseText(xml);
		} catch (DocumentException e) {
			logger.error(e.getMessage(),e);
			return null;
		}
		Object obj = convert(d);
		if (obj instanceof Map) {
			Map map = (Map) obj;
			if (map != null && map.size() == 1 && map.containsKey("config")) {
				return map.get("config");
			}
		}
		// Object obj = null;
		// try {
		// xml = xml.replaceAll(UNDEFIND_REG_1, StringUtils.EMPTY);
		// xml = xml.replaceAll(UNDEFIND_REG_1_1, StringUtils.EMPTY);
		// xml = xml.replaceAll(UNDEFIND_REG_2, StringUtils.EMPTY);
		// xml = xml.replaceAll(UNDEFIND_REG_3, StringUtils.EMPTY);
		// xml = xml.replaceAll(UNDEFIND_REG_3_1, StringUtils.EMPTY);
		// while (xml.matches(UNDEFIND_REG_4)) {
		// xml = xml.replaceAll(UNDEFIND_REG_4, StringUtils.EMPTY);
		// }
		// obj = convertWithSTAX(xml);
		// } catch (XMLStreamException e) {
		// return null;
		// }
		// System.out.println("user stax total cast : " + (System.currentTimeMillis() - start));
		return obj;
	}

	@SuppressWarnings("unchecked")
	public static Object convertWithSTAX(String content) throws XMLStreamException {
		long start = System.currentTimeMillis();
		Stack<Tag> stack = new Stack<Tag>();
		Tag tag = null;
		XMLStreamReader r = null;
		content = content.replaceAll("&", "_hyphen_");
		// 去掉多余标签，提高性能
		XMLInputFactory factory = XMLInputFactory.newInstance();
		factory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
		r = factory.createXMLStreamReader(new StringReader(content));
		String tagName = null;
		String tagContent = null;
		int event;
		while (r.hasNext()) {
			event = r.getEventType();
			switch (event) {
			case XMLStreamConstants.START_ELEMENT:
				tagName = r.getName().getLocalPart();
				tag = new Tag(TagType.START, tagName);
				stack.push(tag);
				break;
			case XMLStreamConstants.CHARACTERS:
				if (null == tagName) {
					break;
				}
				if (r.isWhiteSpace())
					break;
				tagContent = r.getText();
				if (tagContent != null && tagContent.indexOf("_hyphen_") != -1) {
					tagContent = tagContent.replaceAll("_hyphen_", "&");
				}
				tag = new Tag(TagType.CONTENT, tagContent);
				stack.push(tag);
				break;
			case XMLStreamConstants.END_ELEMENT:
				tagName = r.getName().getLocalPart();
				tag = new Tag(TagType.END, tagName);
				stack.push(tag);
				break;
			}

			if (r.hasNext()) {
				event = getNextEvent(r);
			}
		}
		Stack<Tag> stack2 = new Stack<Tag>();
		while (!stack.isEmpty()) {
			stack2.push(stack.pop());
		}
		Map<String, Object> retMap = (Map<String, Object>) recurse(stack2);
		Object retObj = null;
		if (retMap.containsKey("config")) {
			retObj = retMap.get("config");
			if (!"".equals(retObj)) {
				return retObj;
			}
			return null;
		} else {
			if (retMap.keySet().size() == 1) {
				retObj = retMap.entrySet().iterator().next().getValue();
				if (!"".equals(retObj)) {
					return retObj;
				}
				return null;
			}
			return retMap;
		}
	}

	private static Object recurse(Stack<Tag> stack) {
		Map<String, Object> map = new HashMap<String, Object>();
		List<Object> list = null;
		Tag tag = null;
		String tagName = null;
		while (!stack.isEmpty()) {
			tag = stack.pop();
			if (tag.getType() == TagType.START) {
				tagName = tag.getContent();
				if (tagName != null && tagName.equals("list")) {
					list = new ArrayList<Object>();
				} else if (tagName != null && tagName.equals("list-item")) {
					list.add(recurse(stack));
				} else {
					map.put(tagName, recurse(stack));
				}
			} else if (tag.getType() == TagType.CONTENT) {
				if ("true".equalsIgnoreCase(tag.getContent()) || "false".equalsIgnoreCase(tag.getContent())) {
					return Boolean.valueOf(tag.getContent());
				} else if (ValidateUtils.isLong(tag.getContent())) {
					return Long.valueOf(tag.getContent());
				} else {
					return tag.getContent();
				}

			} else if (tag.getType() == TagType.END) {
				if (tag.getContent() != null && tag.getContent().equals("list")) {
					return list;
				} else if (tag.getContent() != null && tag.getContent().equals("list-item")) {
					continue;
				} else if (tagName == null) {
					// 当只有开始、结束标签，中间没有内容时,not regual
					stack.push(tag);
					return "";
				} else if (stack.isEmpty()
						|| (tagName != null && tagName.equals(tag.getContent()) && stack.peek().getType() == TagType.END)) {
					return map;
				}
			}
		}
		return map;
	}

	static enum TagType {
		START, END, CONTENT;
	}

	static class Tag {
		private TagType type = null;
		private String content = null;

		public Tag(TagType type, String content) {
			this.type = type;
			this.content = content;
		}

		public TagType getType() {
			return type;
		}

		public String getContent() {
			return content;
		}
	}

	/**
	 * Note:此方法并不通用,只适用于配置引擎配置字符的转换.
	 * 
	 * @param e
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Object convert(Element e) {
		Map map = new HashMap();
		String content = null;
		if (null == e) {
			return null;
		} else if (e.isTextOnly()) {
			content = e.getText();
			if ("true".equalsIgnoreCase(content) || "false".equalsIgnoreCase(content)) {
				map.put(e.getName(), Boolean.valueOf(e.getText()));
			} else {
				if (content != null && content.trim().length() > 0) {
					map.put(e.getName(), e.getText());
				}
			}
		} else {
			List elements = e.elements();
			if (elements != null && elements.size() == 1 && "list".equals(((Element) elements.get(0)).getName())) {
				elements = ((Element) elements.get(0)).elements();
				List list = new ArrayList(elements.size());
				for (int i = 0; i < elements.size(); i++) {
					Element el = (Element) elements.get(i);
					list.add(convert(el));
				}
				return list;
			} else {
				// return map
				Map subMap = new HashMap();
				if(elements!=null){
				for (int i = 0; i < elements.size(); i++) {
					Element el = (Element) elements.get(i);
					if (el.isTextOnly()) {
						content = el.getText().trim();
						if ("true".equalsIgnoreCase(content) || "false".equalsIgnoreCase(content)) {
							subMap.put(el.getName(), Boolean.valueOf(el.getText()));
						} else if (ValidateUtils.isLong(content)) {
							subMap.put(el.getName(), Long.valueOf(content));
						} else {
							if (content != null && content.trim().length() > 0) {
								subMap.put(el.getName(), content);
							}
						}
					} else {
						subMap.put(el.getName(), convert(el));
					}
				}}
				map = subMap;// map.put(e.getName(), subMap);
			}
		}
		return map;
	}

	public static Object convert(Document d) {
		if (null == d)
			return null;
		return convert(d.getRootElement());
	}

	public static String getTagContent(String content, String tagName) throws Exception {
		String startTag = "<" + tagName + ">";
		String endTag = "</" + tagName + ">";
		String cdataStart = "<!\\[CDATA\\[";
		String cdataEnd = "\\]\\]>";
		String returnContent = StringUtils.EMPTY;

		String str = content;
		if (str == null) {
			return null;
		}
		int index = str.indexOf(startTag);
		if (index >= 0) {
			str = str.substring(index + startTag.length());
			index = str.indexOf(endTag);
			if (index >= 0) {
				returnContent = str.substring(0, index).trim();
				returnContent = returnContent.replaceAll(cdataStart, StringUtils.EMPTY);
				returnContent = returnContent.replaceAll(cdataEnd, StringUtils.EMPTY);
			}
		}
		return returnContent;
	}

	private static int getNextEvent(XMLStreamReader r) throws XMLStreamException {
		try {
			return r.next();
		} catch (Exception e) {
			return r.nextTag();
		}
	}

	/**
	 * 除去 xml的头部信息与<config>节点
	 * @param config
	 * @return
	 */
	public static String getPartConfig(String config) {
		if (config != null && config.length() > 0) {
			if (config.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")) {
				config = config.replace("<?xml version=\"1.0\" encoding=\"UTF-8\"?>", "");
			}
			if (config.startsWith("<config>")) {
				config = config.replace("<config>","");
			}
			if (config.endsWith("</config>")) {
				config = config.replace("</config>","");
			}
			return config.trim();
		}
		return null;
	}

	
	/**
     * <p>
     * <code>parseText</code> parses the given text as an XML document and
     * returns the newly created Document.
     * </p>
     * 
     * @param text
     *            the XML text to be parsed
     * 
     * @return a newly parsed Document
     * 
     * @throws DocumentException
     *             if the document could not be parsed
     */
    public synchronized static Document parseText(String text) throws DocumentException {
        Document result = null;
        
        reader = readerLocal.get();
        if(null == reader){
        	reader = new SAXReader();
        	readerLocal.set(reader);
        }
        String encoding = getEncoding(text);

        InputSource source = new InputSource(new StringReader(text));
        source.setEncoding(encoding);

        result = reader.read(source);

        // if the XML parser doesn't provide a way to retrieve the encoding,
        // specify it manually
        if (result.getXMLEncoding() == null) {
            result.setXMLEncoding(encoding);
        }

        return result;
    }
	
    private static String getEncoding(String text) {
        String result = null;

        String xml = text.trim();

        if (xml.startsWith("<?xml")) {
            int end = xml.indexOf("?>");
            String sub = xml.substring(0, end);
            StringTokenizer tokens = new StringTokenizer(sub, " =\"\'");

            while (tokens.hasMoreTokens()) {
                String token = tokens.nextToken();

                if ("encoding".equals(token)) {
                    if (tokens.hasMoreTokens()) {
                        result = tokens.nextToken();
                    }

                    break;
                }
            }
        }

        return result;
    }
	
}