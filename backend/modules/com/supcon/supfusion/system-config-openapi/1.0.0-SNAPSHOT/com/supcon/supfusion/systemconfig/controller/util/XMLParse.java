package com.supcon.supfusion.systemconfig.controller.util;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import com.google.common.collect.Lists;
import com.supcon.supfusion.systemconfig.controller.vo.CatalogVO;
import com.supcon.supfusion.systemconfig.controller.vo.ConfigVO;
import org.springframework.util.ObjectUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class XMLParse {

    public static InputStream getStringStream(String sInputString) {
        if (sInputString != null && !sInputString.trim().equals("")) {
            try {
                ByteArrayInputStream tInputStringStream = new ByteArrayInputStream(sInputString.getBytes());
                return tInputStringStream;
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return null;
    }

    public static void main(String[] args) throws Exception {
        //1-获取XML-IO流
//        InputStream xmlInputStream = getXmlInputStream("D:\\a.xml.txt");
        String a = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<metatype:MetaData xmlns:metatype=\"http://www.osgi.org/xmlns/metatype/v1.1.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                " <OCD description=\"不安全行为配置\" name=\"不安全行为配置\" id=\"platform.bap.SESWssUB\" type=\"flat\" isWs=\"true\" moduleCode=\"SESWssUB_1.0.0\">\n" +
                "\t<AD isWs=\"true\" name=\"行为指标周类型提醒\" id=\"SESWssUB.indicatorWeek\" required=\"true\" order=\"1\" cardinality=\"1\" type=\"String\" default=\"5\" >\n" +
                "\t\t<Option label=\"周一\" value=\"1\"/>\n" +
                "\t\t<Option label=\"周二\" value=\"2\"/>\n" +
                "\t\t<Option label=\"周三\" value=\"3\"/>\n" +
                "\t\t<Option label=\"周四\" value=\"4\"/>\n" +
                "\t\t<Option label=\"周五\" value=\"5\"/>\n" +
                "\t\t<Option label=\"周六\" value=\"6\"/>\n" +
                "\t\t<Option label=\"周日\" value=\"7\"/>\n" +
                "\t</AD>\n" +
                "\t<AD isWs=\"true\" name=\"行为指标月类型提醒\" id=\"SESWssUB.indicatorMonth\" required=\"true\" order=\"2\" type=\"int\" description=\"行为指标时间类型为月的提醒日期\" default=\"25\" />\n" +
                " </OCD>\n" +
                "</metatype:MetaData>";

        InputStream stringStream = getStringStream(a);
        //2-解析XML-IO流 ，获取Document 对象，以及Document对象 的根节点
        Element rootElement = getRootElementFromIs(stringStream);
        //3~5-从根元素解析得到元素
        parseElementFromRoot(rootElement);
        //控制台输出：　　　　//name == HelloWorld　　　　//className == com.huishe.HelloWord　　　　//propertyEle: name == textone　　　　//propertyEle: value == Hello World!　　　　//propertyEle: name == texttwo　　　  //propertyEle: value == Hello SUN!
    }

    public static CatalogVO getConfigVOByXml(String xmlString) throws Exception {
        InputStream stringStream = getStringStream(xmlString);
        //2-解析XML-IO流 ，获取Document 对象，以及Document对象 的根节点
        Element rootElement = getRootElementFromIs(stringStream);
        //3~5-从根元素解析得到元素
        List<CatalogVO> catalogVOList = parseElementFromRoot(rootElement);
        return catalogVOList.get(0);

    }

    //1-获取XML-IO流
    private static InputStream getXmlInputStream(String xmlPath) {
        InputStream inputStream = null;
        try {
            //1-把要解析的 XML 文档转化为输入流，以便 DOM 解析器解析它
            inputStream = new FileInputStream(xmlPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return inputStream;
    }

    //2-解析XML-IO流 ，获取Document 对象，以及Document对象 的根节点
    private static Element getRootElementFromIs(InputStream inputStream) throws Exception {
        if (inputStream == null) {
            return null;
        }
        /*
         * javax.xml.parsers 包中的DocumentBuilderFactory用于创建DOM模式的解析器对象 ，
         * DocumentBuilderFactory是一个抽象工厂类，它不能直接实例化，但该类提供了一个newInstance方法 ，
         * 这个方法会根据本地平台默认安装的解析器，自动创建一个工厂的对象并返回。
         */
        //2-调用 DocumentBuilderFactory.newInstance() 方法得到创建 DOM 解析器的工厂
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        //3-调用工厂对象的 newDocumentBuilder方法得到 DOM 解析器对象。
        DocumentBuilder docBuilder = factory.newDocumentBuilder();
        //4-调用 DOM 解析器对象的 parse() 方法解析 XML 文档，得到代表整个文档的 Document 对象，进行可以利用DOM特性对整个XML文档进行操作了。
        Document doc = docBuilder.parse(inputStream);
        //5-得到 XML 文档的根节点
        Element root = doc.getDocumentElement();
        //6-关闭流
        if (inputStream != null) {
            inputStream.close();
        }
        return root;
    }

    //3-从根元素解析得到元素
    private static List<CatalogVO> parseElementFromRoot(Element root) {
        List<CatalogVO> catalogVOS = new ArrayList<>();
        NodeList nl = root.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (node instanceof Element) {
                Element ele = (Element) node;
//                //4-从元素解析得到属性值
//                getDataFromElement(ele);
                //5-从元素解析特定子元素并解析(以property为例)
                CatalogVO catalogVO = getCertainElementFromParentElement(ele);
                catalogVOS.add(catalogVO);
            }
        }
        return catalogVOS;
    }

    //4-从元素解析得到属性值
    private static void getDataFromElement(Element ele) {
        String name = ele.getAttribute("name");//根据属性名称读取属性值
        String description = ele.getAttribute("description");
        String id = ele.getAttribute("id");
        String type = ele.getAttribute("type");
        String isWs = ele.getAttribute("isWs");
        String moduleCode = ele.getAttribute("moduleCode");

    }

    //5-从元素解析特定子元素并解析(以property为例)
    private static CatalogVO getCertainElementFromParentElement(Element ele) {
        CatalogVO catalogVO = new CatalogVO();
        String description1 = ele.getAttribute("description");
        String name1 = ele.getAttribute("name");
        String id1 = ele.getAttribute("id");
        String type1 = ele.getAttribute("type");
        String isWs1 = ele.getAttribute("isWs");
        String moduleCode = ele.getAttribute("moduleCode");
        if (moduleCode.contains("_")) {
            String[] s = moduleCode.split("_");
            moduleCode = s[0];
        }
        catalogVO.setName(name1);
        catalogVO.setCode(moduleCode);
        catalogVO.setAppCode(moduleCode);
        catalogVO.setOrder(1D);

        List<ConfigVO> configVOList = new ArrayList<>();
        NodeList propertyEleList = ele.getElementsByTagName("AD");//根据标签名称获取标签元素列表
        for (int i = 0; i < propertyEleList.getLength(); i++) {
            Node node = propertyEleList.item(i);
            if (node instanceof Element) {
                Element propertyEle = (Element) node;
                String isWs = propertyEle.getAttribute("isWs");
                String name = propertyEle.getAttribute("name");
                String id = propertyEle.getAttribute("id");
                String required = propertyEle.getAttribute("required");
                String order = propertyEle.getAttribute("order");
                String cardinality = propertyEle.getAttribute("cardinality");
                String type = propertyEle.getAttribute("type");
                String description = propertyEle.getAttribute("description");
                String aDefault = propertyEle.getAttribute("default");

                ConfigVO configVO = new ConfigVO();
                configVO.setName(name);
                if (!ObjectUtils.isEmpty(order)) {
                    configVO.setOrder(Double.valueOf(order));
                } else {
                    configVO.setOrder(1D);
                }
                configVO.setCode(id);
                configVO.setOrder(Double.parseDouble(order));
                configVO.setDefaultValue(Lists.newArrayList(aDefault));


                List<ConfigVO.TypeConfig.OptionalValue> optionList = Lists.newArrayList();
                NodeList optionEleList = propertyEle.getElementsByTagName("Option");//根据标签名称获取标签元素列表

                /**
                 * todo
                 */
                if (!ObjectUtils.isEmpty(optionEleList) && optionEleList.getLength() > 0) {
                    if ("String".equalsIgnoreCase(type)) {
                        configVO.setType(3);
                    } else if ("Boolean".equalsIgnoreCase(type)) {
                        configVO.setType(2);
                    }
                } else {
                    configVO.setType(7);
                }
                for (int i1 = 0; i1 < optionEleList.getLength(); i1++) {
                    Node item = optionEleList.item(i1);
                    if (item instanceof Element) {
                        Element optionEle = (Element) item;

                        String lable = optionEle.getAttribute("label");
                        String value = optionEle.getAttribute("value");
                        String orderOptional = optionEle.getAttribute("order");

                        ConfigVO.TypeConfig.OptionalValue optionalValue = new ConfigVO.TypeConfig.OptionalValue();
                        optionalValue.setLabel(lable);
                        optionalValue.setValue(value);
                        if (!ObjectUtils.isEmpty(orderOptional)) {
                            optionalValue.setOrder(Double.valueOf(orderOptional));
                        } else {
                            optionalValue.setOrder(1D);
                        }
                        optionList.add(optionalValue);
                    }
                }

                //typeConfig
                ConfigVO.TypeConfig typeConfig = new ConfigVO.TypeConfig();
                typeConfig.setOptionalValue(optionList);
                typeConfig.setRemind(description);
                configVO.setTypeConfig(typeConfig);

                //verify
                List<ConfigVO.Verify> verifyList = new ArrayList<>();
                ConfigVO.Verify verify = new ConfigVO.Verify();
                verify.setIsRequire(Boolean.valueOf(required));
                verifyList.add(verify);
                configVO.setVerify(verifyList);

                configVOList.add(configVO);

            }
        }

        catalogVO.setConfig(configVOList);
        return catalogVO;
    }


}