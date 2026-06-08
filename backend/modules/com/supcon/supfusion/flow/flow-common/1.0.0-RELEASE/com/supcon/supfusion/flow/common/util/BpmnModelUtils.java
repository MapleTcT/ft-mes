/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.util;

import java.io.UnsupportedEncodingException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.Node;
import org.dom4j.QName;

import com.supcon.supfusion.flow.common.enumeration.FlowErrorEnum;
import com.supcon.supfusion.flow.common.exception.InvalidBpmnModelException;

/**
 * @author: zhuangmh
 * @date: 2020年5月21日 下午5:43:00
 */
public class BpmnModelUtils {
    
    private static final String[] ELEMENT_LIST = {"sequenceFlow", "userTask", "serviceTask", "parallelGateway", "inclusiveGateway", "exclusiveGateway"};
    
    private BpmnModelUtils() {
        throw new IllegalStateException("BpmnModelUtils is utility class, do not instantiate");
    }
    
    /**
     * 判断两个流程图的结构是否一致 一致标准: 1. 元素个数相同, 2. 迁移线指向相同
     * @param bpmnXml1
     * @param bpmnXml2
     * @return 结构一致返回 true, 否者返回 false
     * @throws UnsupportedEncodingException
     * @throws DocumentException
     */
    public static boolean bpmnStructureEquals(String bpmnXml1, String bpmnXml2) throws UnsupportedEncodingException, DocumentException {
        Document document1 = DomUtils.getDocument(bpmnXml1);
        Document document2 = DomUtils.getDocument(bpmnXml2);
        return bpmnElementNumberEquals(document1, document2) 
                || bpmnStructureEquals(document1, document2);
    }
    
    // 判断元素个数是否一致
    private static boolean bpmnElementNumberEquals(Document document1, Document document2) {
        for (String element : ELEMENT_LIST) {
            List<?> node1List = document1.selectNodes(String.format("//uri:%s", element));
            List<?> node2List = document2.selectNodes(String.format("//uri:%s", element));
            if (node1List.size() != node2List.size()) {
                return false;
            }
        }
        return true;
    }
    
    // 根据每根迁移线的来源和去向是否一致, 即可知道整体结构是否一致
    private static boolean bpmnStructureEquals(Document document1, Document document2) {
        List<Node> flowNodes = document1.selectNodes("//uri:sequenceFlow");
        for (Node flowNode1 : flowNodes) {
            String nodeId = ((Element)flowNode1).attributeValue("id");
            // 根据前者的ID找到后者对应ID的迁移线, 如果把线删了再画上去, 此时后者的ID已经发生变化,因此也是判断结构发生改变, 有点傻...
            Node flowNode2 = document2.selectSingleNode(String.format("//uri:sequenceFlow[@id='%s']", nodeId));
            if (flowNode2 == null) {
                return false;
            }
            String sourceRef2 = ((Element)flowNode2).attributeValue("sourceRef", "");
            String sourceRef1 = ((Element)flowNode1).attributeValue("sourceRef", "");
            if (!sourceRef1.equals(sourceRef2)) {
                return false;
            }
            String targetRef2 = ((Element)flowNode2).attributeValue("targetRef", "");
            String targetRef1 = ((Element)flowNode1).attributeValue("targetRef", "");
            if (!targetRef1.equals(targetRef2)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * 验证bpmn格式是否符合规范
     * @param bpmnXml
     * @return 返回修改过的bpmn字符串
     * @throws UnsupportedEncodingException
     * @throws DocumentException
     */
    public static String validateBpmnModel(String bpmnXml) throws UnsupportedEncodingException, DocumentException {
        Document document = DomUtils.getDocument(bpmnXml);
        List<Node> startNodes = document.selectNodes("//uri:process/uri:startEvent");
        validateStartEvent(startNodes);
        List<?> endNodes = document.selectNodes("//uri:endEvent");
        if (endNodes.isEmpty()) {
            throw new InvalidBpmnModelException(FlowErrorEnum.END_NODE_NOT_EXIST);
        }
        List<Node> userTaskNodes = document.selectNodes("//uri:userTask");
        validateUserTask(userTaskNodes); // 验证人工节点
        List<Node> exclusiveGatewayNodes = document.selectNodes("//uri:exclusiveGateway");
        validateExclusiveGateway(document, exclusiveGatewayNodes); // 验证互斥网关
        List<Node> serviceTaskNodes = document.selectNodes("//uri:serviceTask");
        validateServiceTask(serviceTaskNodes); // 验证自动服务节点
        List<Node> timerNodes = document.selectNodes("//uri:timerEventDefinition");
        validateTimer(document, timerNodes); // 验证定时器节点
        List<Node> signalNodes = document.selectNodes("//uri:signalEventDefinition");
        validateSignal(document, signalNodes); // 验证信号节点
        setConditionForMultipleOutgoing(document, userTaskNodes);
        return document.asXML();
    }
    
    private static void validateStartEvent(List<Node> startNodes) {
        if (startNodes.isEmpty() || startNodes.size() > 1) {
            throw new InvalidBpmnModelException(FlowErrorEnum.ONLY_ONE_START_NODE_IS_ALLOWED_ERROR);
        }
       /* Element ele = (Element)startNodes.get(0);
        String pageUrl = ele.attributeValue(Constants.PAGE_URL);
        if (StringUtils.isEmpty(pageUrl)) {
            throw new InvalidBpmnModelException(FlowErrorEnum.START_NODE_PAGE_NOT_SET_ERROR);
        }*/
    }
    
    private static void validateUserTask(List<Node> userTaskNodes) {
        for (Node userTaskNode : userTaskNodes) {
            Element userTaskEle = (Element)userTaskNode;
            if (StringUtils.isEmpty(userTaskEle.attributeValue(Constants.NAME))) {
                throw new InvalidBpmnModelException(FlowErrorEnum.USERTASK_NAME_NOT_EMPTY_ERROR);
            }
            String url = userTaskEle.attributeValue(Constants.PAGE_URL);
            if (StringUtils.isEmpty(url)) {
                throw new InvalidBpmnModelException(FlowErrorEnum.USERTASK_PAGE_NOT_EMPTY_ERROR);
            }
        }
    }
    
    private static void validateExclusiveGateway(Document document, List<Node> exclusiveGatewayNodes) {
        for (Node exclusiveGatewayNode : exclusiveGatewayNodes) {
            Element exclusiveGatewayEle = (Element)exclusiveGatewayNode;
            String elementId = exclusiveGatewayEle.attributeValue(Constants.ID);
            List<Node> gatewayConditionNodes = document.selectNodes(String.format("//uri:sequenceFlow[@sourceRef='%s']/uri:conditionExpression", elementId));
            if (gatewayConditionNodes.isEmpty()) {
                throw new InvalidBpmnModelException(FlowErrorEnum.MULTIPLE_SEQUENCE_CONDITION_NOT_SET_ERROR);
            }
            for (Node exclusiveFlowNode : gatewayConditionNodes) {
                if (StringUtils.isEmpty(exclusiveFlowNode.getText())) {
                    throw new InvalidBpmnModelException(FlowErrorEnum.MULTIPLE_SEQUENCE_CONDITION_NOT_SET_ERROR);
                }
            }
        }
    }
    
    private static void validateServiceTask(List<Node> serviceTaskNodes) {
        for (Node serviceTaskNode : serviceTaskNodes) {
            Element serviceTaskEle = (Element)serviceTaskNode;
            String name = serviceTaskEle.attributeValue(Constants.NAME);
            if (StringUtils.isEmpty(name)) {
                throw new InvalidBpmnModelException(FlowErrorEnum.SERVICETASK_NAME_NOT_EMPTY_ERROR);
            }
            String expression = serviceTaskEle.attributeValue("expression");
            if (StringUtils.isEmpty(expression)) {
                throw new InvalidBpmnModelException(FlowErrorEnum.SERVICETASK_EXECUTOR_NOT_SET_ERROR);
            }
        }
    }
    
    private static void validateTimer(Document document, List<Node> timerNodes) {
        for (Node timerNode : timerNodes) {
            Element parent = timerNode.getParent();
            Node timeExpressionNode = document.selectSingleNode(String.format("//*[@id='%s']/uri:timerEventDefinition/uri:timeDate", parent.attributeValue("id")));
            Element timeExpressionEle = (Element)timeExpressionNode;
            String timerVar = timeExpressionEle.attributeValue("showTimer");
            if (StringUtils.isEmpty(timerVar)) {
                throw new InvalidBpmnModelException(FlowErrorEnum.TIMER_VARIABLE_NOT_SET_ERROR);
            }
        }
    }
    
    private static void validateSignal(Document document, List<Node> signalNodes) {
        for (Node signalNode : signalNodes) {
            Element signalEle = (Element)signalNode;
            String signalRef = signalEle.attributeValue("signalRef"); // 引用信号源对象
            if (StringUtils.isEmpty(signalRef)) {
                throw new InvalidBpmnModelException(FlowErrorEnum.SIGNEL_SOURCE_NOT_SET_ERROR);
            }
            Node signalDef = document.selectSingleNode(String.format("//uri:signal[@id='%s']", signalRef)); // 定义信号源对象
            if (signalDef == null) {
                throw new InvalidBpmnModelException(FlowErrorEnum.SIGNEL_SOURCE_NOT_SET_ERROR);
            }
        }
    }
    
    /**
     *  设置多分支流转条件, 例: 有2个分支的条件表达式分别为${audit==0}  ${audit==1}
     */
    private static void setConditionForMultipleOutgoing(Document document, List<Node> userTaskNodes) {
        for (Node userTaskNode : userTaskNodes) {
            String id = ((Element)userTaskNode).attributeValue(Constants.ID);
            List<Node> outgoingFlows = document.selectNodes(String.format("//uri:sequenceFlow[@sourceRef='%s']", id));
            int outgoingSize = outgoingFlows.size();
            int go = 1, back = -1;
            // 只有当用户任务存在多个输出才需要设置输出条件
            for (int i = 0; i < outgoingSize && outgoingSize > 1; i++) {
                Element sequenceElement = (Element)outgoingFlows.get(i);
                if (StringUtils.isEmpty(sequenceElement.attributeValue(Constants.NAME))) {
                    throw new InvalidBpmnModelException(FlowErrorEnum.MULTIPLE_SEQUENCE_NAME_NOT_EMPTY_ERROR);
                }
                String sequenceId = sequenceElement.attributeValue(Constants.ID);
                String rejectStr = sequenceElement.attributeValue(Constants.REJECT_SEQUENCE);
                Node sequenceFlow = document.selectSingleNode(String.format("//uri:sequenceFlow[@id='%s']", sequenceId));
                Node conditionNode = document.selectSingleNode(String.format("//uri:sequenceFlow[@id='%s']/uri:conditionExpression", sequenceId));
                // 迁移线条件表达式
                String expression = "";
                if (Boolean.valueOf(rejectStr)) {
                    expression = String.format("${%s==%d}", Constants.OUTPUT_CONDITION_VARIABLE, back--);
                } else {
                    expression = String.format("${%s==%d}", Constants.OUTPUT_CONDITION_VARIABLE, go++);
                }
                if (conditionNode != null) {
                    conditionNode.setText(expression);
                } else {
                    Element conditionExpressionEntry = ((Element)sequenceFlow).addElement("conditionExpression");
                    conditionExpressionEntry.setText(expression);
                    conditionExpressionEntry.addAttribute(new QName("type", new Namespace("xsi", "http://www.w3.org/2001/XMLSchema-instance")), "tFormalExpression");
                }
            }
        }
    }
    
}
