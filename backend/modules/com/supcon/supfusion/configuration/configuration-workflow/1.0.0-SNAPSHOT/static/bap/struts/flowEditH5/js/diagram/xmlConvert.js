/**
 * xml与json数据格式间的相互转换
 * 旧的流程图数据转化为能被bpmn2.0解析的流程图数据
 * Created in 2018/12/19.
 */
define(function (require, exports, module) {
    var bpmnPublic=require("./bpmnPublic.js");//基本方法
    var diConfig=require("./diagramConfig.js");//配置信息
    var typeControl=diConfig.typeControl;//类型配置信息
    var shapeSize=diConfig.shape;//实例大小
    //xml转json
    function XmlToJson() {
    }
    XmlToJson.prototype.setXml = function (xml) {
        if (xml && typeof xml == "string") {
            this.xml = document.createElement("div");
            this.xml.innerHTML = xml;
            this.xml = this.xml.getElementsByTagName("*")[0];
        }
        else if (typeof xml == "object") {
            this.xml = xml;
        }
//      jq_1.7.2版本下兼容IE9对xml的解析
        if(this.xml.nextSibling&&this.xml.nodeType!=1){
        	this.xml=this.xml.nextSibling;
        }    
    };
    XmlToJson.prototype.getXml = function () {
        return this.xml;
    };
    XmlToJson.prototype.parse = function (xml) {
        this.setXml(xml);
        return this.convert(this.xml);
    };
    XmlToJson.prototype.convert = function (xml) {
        if (xml.nodeType != 1) {
            return null;
        }
        var obj = {};
        obj.xtype = xml.nodeName.toLowerCase();
        var nodeValue = (xml.textContent || "").replace(/(\r|\n)/g, "").replace(/^\s+|\s+$/g, "");

        if (nodeValue && xml.childNodes.length == 1) {
            obj.text = nodeValue;
        }
        if (xml.attributes.length > 0) {
            for (var j = 0; j < xml.attributes.length; j++) {
                var attribute = xml.attributes.item(j);
                obj[attribute.nodeName] = attribute.nodeValue;
            }
        }
        if (xml.childNodes.length > 0) {
            var items = [];
            for (var i = 0; i < xml.childNodes.length; i++) {
                var node = xml.childNodes.item(i);
                var item = this.convert(node);
                if (item) {
                    items.push(item);
                }
            }
            if (items.length > 0) {
                obj.items = items;
            }
        }
        return obj;
    };
    //json转xml
    function JsonToXml() {
        this.result = [];
        this.isNoWrap=false;//是否不换行
    }
    JsonToXml.prototype.spacialChars = ["&", "<", ">", "\"", "'"];
    JsonToXml.prototype.validChars = ["&", "<", ">", "", "'"];
    JsonToXml.prototype.toString = function () {
        return this.result.join("");
    }
    JsonToXml.prototype.replaceSpecialChar = function (s) {
        for (var i = 0; i < this.spacialChars.length; i++) {
            s = s.replace(new RegExp(this.spacialChars[i], "g"), this.validChars[i]);
        }
        return s;
    };
    JsonToXml.prototype.appendText = function (s) {
        s = this.replaceSpecialChar(s);
        this.result.push(s);
    };
    JsonToXml.prototype.appendAttr = function (key, value) {
        this.result.push(" " + key + "=\"" + value + "\"");
    };
    JsonToXml.prototype.appendFlagBeginS = function (s,space) {
    	if(this.isNoWrap) space="";//不换行情况
        this.result.push(space+"<" + s);
    };
    JsonToXml.prototype.appendFlagBeginE = function () {
    	var wrap='\n';
    	if(this.isNoWrap) wrap="";//不换行情况
        this.result.push(">"+wrap);
    };
    JsonToXml.prototype.appendFlagEnd = function (s,space) {
    	var wrap='\n';
    	if(this.isNoWrap){
    		//不换行情况
    		space="";
    		wrap="";
    	} 
        this.result.push(space+"</" + s + ">"+wrap);
    };
    JsonToXml.prototype.parse = function (json,isNoWrap) {
        this.result=[];
        this.isNoWrap=isNoWrap;//是否不换行
        this.convert(json,0);
        return this.toString();
    };
    JsonToXml.prototype.convert = function (obj,count) {
        count++;
        var nodeName = obj.xtype || "item";
        var space="";
        for(var i=0;i<count;i++){
            space+=" ";
        }
        this.appendFlagBeginS(nodeName,space);
        var arrayMap = {};
        for (var key in obj) {
            var item = obj[key];
            if(!item) item="";
            if (key == "xtype") {
                continue;
            }
            if (item==""||item.constructor == String) {
                this.appendAttr(key, item);
            }
            if (item.constructor == Array) {
                arrayMap[key] = item;
            }
        }
        this.appendFlagBeginE(space);
        for (var key in arrayMap) {
            var items = arrayMap[key];
            for (var i = 0; i < items.length; i++) {
                this.convert(items[i],count);
            }
        }
        this.appendFlagEnd(nodeName,space);
    };
    //--------------------------------------------旧的流程图数据转化为能被bpmn2.0解析的流程图数据--------------------------------------------
    function xmlConvertRorBpmn() {};
    xmlConvertRorBpmn.prototype.XmlToJson = new XmlToJson();
    xmlConvertRorBpmn.prototype.JsonToXml = new JsonToXml();
    /*
     * xml数据转换
     * @param {xml}   流程图xml数据
     */
    xmlConvertRorBpmn.prototype.dealXMLData = function (xml) {
        var xmlChild =xml.childNodes;
        var xmlObj = this.XmlToJson.parse(xmlChild[0]);
        var initData = changeToBpmnData(xmlObj);
        getFalshAttribute(xmlObj);//xml属性设置读取;
        var initXml = '<?xml version="1.0" encoding="UTF-8"?>/n' + this.JsonToXml.parse(initData,false);
        $("#lastModify").val(xmlObj.lastModify);//最后修改时间
        return initXml;
    }
     /*
     * 读取流程节点的属性信息，存入全局变量
     * @param {data}   对转换xml后生成的的json数据
     */
    function getFalshAttribute(data){
    	window.processAttribute={};//清空保存实体属性全局变量
        $.each(data.items, function (i, obj) {
            if (obj.xtype != "description") {
                //获取节点属性信息
                if(obj.xtype=="notification"){
                    obj.xtype="task";//通知活动情况，遵循人工活动实例绘制规则
                    obj.taskType="通知活动";//补充属性
                } 
                getNodeAttribute(obj);
            }
        });
    }
    //获取节点属性信息
    function getNodeAttribute(obj){
        var id=obj.name;
        var type=getInstanceTypeForBpmn(obj.xtype);
        var nodeData=bpmnPublic.getElementDataById(id,type);//获取默认的属性值定义
        for(var key in nodeData){
            var getObjAttr=obj[key];
            var isVaild=getObjAttr&&getObjAttr!=""&&getObjAttr!="undefined";//属性值是否有效
            if(nodeData[key]==true&&!isVaild) nodeData[key]=false;//属性值为布尔型且为默认选中情况下，标签上没有存属性值即代表该属性值为未选中
            if(isVaild){
                //存储在节点标签上的属性值,参数是否为布尔型，是则做转换
                nodeData[key]=booleanTransform(getObjAttr,nodeData[key]);
            }
            //会签活动的循环会签（是否选中取决于是否定义选人范围）
            if(type=="CountersignEvent"&&key=="loop"&&isVaild){
                //勾选循环会签
                nodeData["loopSign"]=true;
            }
            //迁移线的是否可选人（是否选中取决于是否定义选人范围）
            if(type=="SequenceFlow"&&key=="selectStaff"&&isVaild){
                //勾选是否可选人
                nodeData["ableSelectStaff"]=true;
            }
            //迁移线必填、默认选择上次选择人员值为"1"时需转化为true
            var transformKey=["requiredStaff","defaultSelectStaff"];//迁移线必填、默认选择上次选择人员
            if(transformKey.indexOf(key)>-1&&getObjAttr=="1") nodeData[key]=true;      
        }
        if(obj.items&&obj.items.length>0){
            var addArr=getAddtionAttr(obj.items,nodeData);//存储在节点子元素的属性值
            nodeData=$.extend(true,nodeData,addArr);//对象扩展
        }
        window.processAttribute[id]=nodeData;
    }
    //获取中英文随机数
    function getEngNumRandom(chars,n){
    	var res = "";
    	for(var i = 0; i < n ; i ++) {
    	   var id = Math.ceil(Math.random()*35);
    	   res += chars[id];
    	}
        return res;
    }
    //获取存储在节点子元素的属性值
    function getAddtionAttr(data,nodeData){
        var addAttr={};
        var paramSet=[];//参数设置
        var event=[];//触发事件
        $.each(data,function(i,item){
            //子节点类型
            switch (item.xtype) {
                case "transition":
                    //存迁移线属性数据；迁移线作为流程实例保存
                    getNodeAttribute(item);
                    break;
                case "condition":
                    //迁移线条件
                    addAttr["expr"] = item.expr;
                    break;
                case "assignment-handler":
                    //子存值域
                    var field = item.items;
                    for (var k = 0; k < field.length; k++) {
                        var fieldItem = field[k];//xtype==field
                        var key = fieldItem.name;
                        var value = fieldItem.items[0].value;//xtype==string
                        addAttr[key] = booleanTransform(value,nodeData[key]);//参数是否为布尔型，是则做转换
                    }
                    break;
                case "open-action":
                    //调用视图
                    addAttr["viewUrl"] = item.url;
                    addAttr["viewName"] = item.name;
                    addAttr["viewCode"] = item.viewCode;
                    addAttr["viewType"] = item.viewType;
                    addAttr["target"] = item.target;
                    break;
                case "notice":
                    //消息提醒
                    var notice = item.items;
                    for (var k = 0; k < notice.length; k++) {
                        var noticeItem = notice[k];
                        var key = noticeItem.xtype;
                        var value = noticeItem.enabled;
                        addAttr[key] = booleanTransform(value,nodeData[key]);//参数是否为布尔型，是则做转换
                    }
                    break;
                case "auto-script":
                    //自动活动
                    addAttr["scriptName"] = item.name;
                    addAttr["scriptCode"] = item.code;
                    break;
                case "parameter-in":
                	//参数设置(输入)
                	paramSet.push({"var":item["var"],"subvar":item["subvar"],"type":"in"});
                	break;
                case "parameter-out":
                	//参数设置（输出）
                	paramSet.push({"var":item["var"],"subvar":item["subvar"],"type":"out"});
                	break;
                case "on":
                	//触发事件
                	event.push({"name":item.name,"event":item.event,"triggerName":item.items[0].class})
                	break;
            }
        });
        if(paramSet.length>0)addAttr["paramSet"]=paramSet;
        if(event.length>0) addAttr["event"]=event;
        return addAttr;
    }
    //布尔型转换
    function booleanTransform(value,source){
        if(typeof source=="boolean"){
            value=value=="true";//转化为布尔型
        }
        return value;
    }
    /*
     * 对转换后的json数据进行改造重组，转换数据使旧数据能被bpmn解析
     * @param {data}   对转换xml后生成的的json数据
     */
    function changeToBpmnData(data) {
        var initData = {
            "xtype": "definitions",
            "xmlns": "http://www.omg.org/spec/BPMN/20100524/MODEL",
            "xmlns:bpmndi": "http://www.omg.org/spec/BPMN/20100524/DI",
            "xmlns:omgdc": "http://www.omg.org/spec/DD/20100524/DC",
            "xmlns:omgdi": "http://www.omg.org/spec/DD/20100524/DI",
            "xmlns:xsi": "http://www.w3.org/2001/XMLSchema-instance",
            "items": []
        };
        var transformData = transformProcessForBpmn(data);
        initData.items.push(transformData.process);
        //连接线及文本定义
        var labelStyle1 = {
            xtype: "bpmndi:BPMNLabelStyle",
            id: "sid-e0502d32-f8d1-41cf-9c4a-cbb49fecf581",
            items: [{
                isBold: "false",
                isItalic: "false",
                isStrikeThrough: "false",
                isUnderline: "false",
                name: "Arial",
                size: "11",
                xtype: "omgdc:Font"
            }]
        }
        var labelStyle2 = {
            xtype: "bpmndi:BPMNLabelStyle",
            id: "sid-84cb49fd-2f7c-44fb-8950-83c3fa153d3b",
            items: [{
                isBold: "false",
                isItalic: "false",
                isStrikeThrough: "false",
                isUnderline: "false",
                name: "Arial",
                size: "12",
                xtype: "omgdc:Font"
            }]
        }
        var diafram = {
            xtype: "bpmndi:BPMNDiagram",
            id: "sid-74620812-92c4-44e5-949c-aa47393d3830",
            items: [transformData.position, labelStyle1, labelStyle2]
        }
        initData.items.push(diafram);
        return initData;
    }
    /*
     * process转换
     * @param {data}   process数据重构
     */
    function transformProcessForBpmn(data) {
        var iTemp ={"xtype":"process","id":data.name,"items":[]};//实例定义
        var iPosition = { "xtype": "bpmndi:BPMNPlane", "id": "sid-cdcae759-2af7-4a6d-bd02-53f3352a731d", "bpmnElement": data.name, "items": [] };//实例位置关系定义
        $.each(data.items, function (i, obj) {
            if (obj.xtype != "description") {
                //流程对象实例
            	if(!obj.name) obj.name=getRandomNodeId(obj.xtype);//id为空的时候（空流程配置的开始活动与结束活动），设置id随机数
                getInstanceChildForBpmn(obj, function (child) {
                    iTemp.items.push(child);
                });
                //流程位置关系
                getInstancePositionForBpmn(obj, function (position) {
                    iPosition.items.push(position);
                }, data.items);
            }
        });
        return { "process": iTemp, "position": iPosition };
    }
    //id为空的时候（空流程配置的开始活动与结束活动），设置id随机数
    function getRandomNodeId(type){
    	var chars = ['0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z'];
    	var random=getEngNumRandom(chars,7);
    	var id=type+"_"+random;
    	return id;
    }
    /*
     * 流程对象实例
     * @param {obj}        流程图节点实例数据
     * @param {callback}   回调
     */
    function getInstanceChildForBpmn(obj, callback) {
        var child = {};
        child.xtype=getInstanceTypeForBpmn(obj.xtype);//获取类型
        child.id = obj.name;
        child.name = obj.desc;
        //连接线逻辑
        if (obj.items && obj.items.length > 0) {
            child.items = [];
            for (var k = 0; k < obj.items.length; k++) {
                var link = obj.items[k];
                var childLine = {};
                if (link.xtype == "transition") {
                    //连接关系定义
                    var deepLink = {
                        xtype: "outgoing",
                        text: link.name
                    }
                    child.items.push(deepLink);
                    //线条实例
                    var childLine = {
                        xtype: "sequenceFlow",
                        id: link.name,
                        sourceRef: obj.name,
                        targetRef: link.to,
                        name: (link.desc=="undefined"||link.desc=="null")?"":link.desc,
                        reject:link.reject&&link.reject=="1"?"1":"0"
                    }
                    if (typeof callback == "function") {
                        callback(childLine);
                    }
                }
            }
        }
        if (typeof callback == "function") {
            callback(child);
        }
    };
    /*
     * 类型对照关系
     * @param {type}        类型
     */
    function getInstanceTypeForBpmn(type){
        var bpmnName;
        if(type=="notification"){
             //通知活动情况，遵循人工活动实例绘制规则
            bpmnName="TaskEvent";
        }else{
            $.each(typeControl,function(i,item){
                if(item.tagName==type){
                    bpmnName=item.tagBpmn;
                    return false;
                }
            });
        }
        return bpmnName;
    }
    /*
     * 流程位置关系
     * @param {obj}        流程图节点实例数据
     * @param {callback}   回调
     * @param {dataSource} 数据源
     */
    function getInstancePositionForBpmn(obj, callback, dataSource) {
        //实例位置关系
        var position = {
            xtype: "bpmndi:BPMNShape",
            id: obj.name + "_di",
            bpmnElement: obj.name,
            items: []
        };
        var pos = obj.g.split(",");
        var shapePos = {
            xtype: "omgdc:Bounds",
            x: pos[0],
            y: pos[1],
            width:shapeSize.width,
            height:shapeSize.height
        }
        var labelPos = {
            xtype: "bpmndi:BPMNLabel",
            items: [{
                xtype: "omgdc:Bounds",
                x: pos[0],
                y: (Number(pos[1]) + Number(shapeSize.height) + 5).toString(),
                width:shapeSize.width,
                height:shapeSize.height
            }]
        }
        position.items = [shapePos, labelPos];
        //连接线位置关系
        if (obj.items && obj.items.length > 0) {
            for (var k = 0; k < obj.items.length; k++) {
                var link = obj.items[k];
                if (link.xtype == "transition") {
                    //线条实例
                    var positionLine = {
                        xtype: "bpmndi:BPMNEdge",
                        bpmnElement: link.name,
                        id: link.name + "_di",
                        items: []
                    };
                    //起始点
                    var startLine = {
                        xtype: "omgdi:waypoint",
                        x: pos[0],
                        y: pos[1]
                    }
                    positionLine.items.push(startLine);
                    //为折线时，需添加中间点
                    if (link.g && link.g != "") {
                        linePoint = link.g.split(",");
                        for (var i = 0; i < linePoint.length / 2; i++) {
                            var pointX = Number(linePoint[2 * i])-Number(shapeSize.width)/2;//考虑偏移量
                            var pointY = linePoint[2 * i + 1];
                            var centerLine = {
                                xtype: "omgdi:waypoint",
                                x: pointX.toString(),
                                y: pointY.toString()
                            }
                            positionLine.items.push(centerLine);
                        }
                    }
                    //结束点
                    var toPos=getElementLinkPos(link.to,dataSource);
                    var toLine = {
                        xtype: "omgdi:waypoint",
                        x: toPos[0],
                        y:toPos[1]
                    }
                    positionLine.items.push(toLine);
                    positionLine.items=tranformElePos(positionLine.items);//连线坐标转换（首尾两点减去节点半径）
                    //线条文本
                    if (link.desc && link.desc != ""&&link.px&&link.px!="") {
                        var lpos = link.px.split(",");
                        var lablePos = {
                            xtype: "bpmndi:BPMNLabel",
                            items: [{
                                xtype: "omgdc:Bounds",
                                width: "60",
                                height: "14",
                                x:(Number(lpos[0])-Number(shapeSize.width)).toString(),
                                y:lpos[1]
                            }]
                        }
                        positionLine.items.push(lablePos);
                    }
                    if (typeof callback == "function") {
                        callback(positionLine);
                    }
                }
            }
        }
        if (typeof callback == "function") {
            callback(position);
        }
    };
    //获取连线结束节点坐标
    function getElementLinkPos(id,data){
        var pos = [];
        $.each(data, function (i, item) {
            if (item.name == id) {
                pos = item.g.split(",");
                return false;
            }
        });
        return pos;
    }
    //连线坐标转换（首尾两点减去节点半径）
    function tranformElePos(data){
        if(!data||data.length<2) return;
        var startPoint=getRadiusCut(data[0],data[1]);
        var endPoint=getRadiusCut(data[data.length-1],data[data.length-2]);
        data[0].x=startPoint.x.toString();
        data[0].y=startPoint.y.toString();
        data[data.length-1].x=endPoint.x.toString();
        data[data.length-1].y=endPoint.y.toString();
        return data;
    }
     /*
     * 坐标偏移计算;获取箭头指向元素坐标(保证箭头不被图形覆盖，需减去半径)
     * @param {transPos}   转化点
     * @param {referPos}   参考点
     */
    function getRadiusCut(transPos,referPos){
        var endX = Number(transPos.x)+ Number(shapeSize.width) / 2;
        var endY = Number(transPos.y)+Number(shapeSize.height) / 2;
        var prevX = Number(referPos.x);
        var prevY = Number(referPos.y);
        var radius = Number(shapeSize.width) / 2;
        var getX, getY;
        var count = (prevX - endX) * (prevX - endX) + (prevY - endY) * (prevY - endY)
        getX = (prevX - endX) / Math.sqrt(count) * radius;
        getY = (prevY - endY) / Math.sqrt(count) * radius;
        var result = { x: (endX + getX).toFixed(2), y: (endY + getY).toFixed(2) };
        return result;
    }
    //--------------------------------------------新的流程图数据转化为旧的flash流程图格式数据--------------------------------------------
    function xmlConvertRorFlash() {};
    xmlConvertRorFlash.prototype.XmlToJson = new XmlToJson();
    xmlConvertRorFlash.prototype.JsonToXml = new JsonToXml();
    /*
        * xml数据转换
        * @param {xml}   流程图xml数据
        * @param {isUpdate} 是否更新修改时间
        */
    xmlConvertRorFlash.prototype.dealXMLData = function (xml,isUpdate) {
        var xmlChild = xml.childNodes;
        var xmlObj = this.XmlToJson.parse(xmlChild[0]);
        var initData = changeToFlashData(xmlObj,isUpdate);
        var initXml = '<?xml version="1.0" encoding="UTF-8"?>' + this.JsonToXml.parse(initData,false);
        return initXml;
    }
    /*
     * 对转换后的json数据进行改造重组，转换数据使bpmn流程图数据能够按旧格式（flash）保存
     * 转换成旧版flash数据
     * @param {data}   对转换xml后生成的的json数据
     */
    function changeToFlashData(data,isUpdate){
    	var lastModify=isUpdate?bpmnPublic.dateFtt("yyyy-MM-dd hh:mm",new Date()):$("#lastModify").val();//修改时间
    	var systemVersion=$("#systemVersion").val();//系统版本
    	var flowKey=$("#flowKey").val();//流程id
        var initData = {
            "xtype": "process",
            "name":flowKey,
            "systemVersion":systemVersion,//系统版本
            "xmlns": "http://jbpm.org/4.4/jpdl",
            "lastModify":lastModify,//上一次修改时间
            "items": []
        };
        //流程图描述
        var description = {
            text: $("#flowName").val(),
            xtype: "description"
        }
        initData.items.push(description);
        var transformData=transformProcessForFlash(data);//process数据转换
        initData.items=initData.items.concat(transformData);
        return initData;
    }
    //process转换(flash)
    function transformProcessForFlash(data){
        var instance=data.items[0].items;
        var posResource=data.items[1].items[0].items;
        var process=[];
        var linkSort=sortLinkRelationForFlash(posResource,instance);
        $.each(instance,function(i,item){
            if(item.xtype!="sequenceflow"){
                //非连接线
                var child={
                    xtype:getInstanceTypeForFlash(item.xtype),
                    name:item.id,
                    desc:item.name&&transSpecialCharForXML(item.name),//对特殊字符进行转义处理
                    g:getElementPosForFlash(item.id,posResource)
                }
                var extendAttr=getExtendAttribute(item.id,item.xtype);//获取额外的属性信息
                $.extend(true,child,extendAttr);//扩展属性
                //迁移线信息
                if(linkSort[item.id]&&linkSort[item.id].length!=0) {
                    if(!child.items) child["items"]=[];
                    child.items=child.items.concat(linkSort[item.id]);
                }
                process.push(child);
            }
        });
        return process;
    }
      /*
     * 类型对照关系
     * @param {type}        类型
     */
    function getInstanceTypeForFlash(type){
        var flashName;
        $.each(typeControl,function(i,item){
            if(item.tagBpmn==type||item.lowercase==type){
                flashName=item.tagName;
                return false;
            }
        });
        return flashName;
    }
    //连线位置关系归类
    function sortLinkRelationForFlash(posResource,instance){
        var relation={};
        $.each(posResource,function(i,item){
            if(item.xtype=="bpmndi:bpmnedge"){
                var info=getLinkInfoForFlash(item.bpmnElement,instance);//获取连线信息
                var pos=getLinkPosForFlash(item.items);//获取连线位置信息
                if(pos.labelPos.indexOf("NAN")>-1) pos.labelPos="";
                var link={
                    xtype:"transition",
                    name:item.bpmnElement,
                    desc:info.name?transSpecialCharForXML(info.name):"",//对特殊字符进行
                    to:info.targetRef,
                    px:pos.labelPos,//连线文本位置
                    reject:info.reject&&info.reject=="1"?"1":"0"
                }
                var extendAttr=getExtendAttribute(item.bpmnElement,"sequenceflow");//获取额外的属性信息
                $.extend(true,link,extendAttr);//扩展属性
                if(pos.linePos&&pos.linePos!="") link["g"]=pos.linePos;//线条位置
                if(!relation[info.sourceRef]) relation[info.sourceRef]=[];//未定义
                if(info.cancel=="1") link["cancel"]=info.cancel;//指向作废的迁移线需添加cancel属性
                if(info.notificationType=="1") link.notificationType=info.notificationType;//指向通知的迁移线需添加notificationType属性
                relation[info.sourceRef].push(link);
            }
        });
        return relation;
    }
    //获取连线位置信息
    function getLinkPosForFlash(data){
        var linePos=[],labelPos="";
        $.each(data,function(i,item){
            if(item.xtype=="omgdi:waypoint"){
                var transX=Number(item.x)+Number(shapeSize.width)/2;//考虑偏移量
                linePos.push(transX+","+item.y);
            }else if(item.xtype=="bpmndi:bpmnlabel"){
                var temp=item.items[0];
                var tempX=Number(temp.x)+(Number(temp.width)-60)/2+Number(shapeSize.width);//考虑偏移量
                var tempY=Number(temp.y);
                labelPos=tempX+","+tempY;
            }
        });
        //剔除开始节点坐标、结束节点坐标
        linePos=linePos.slice(1,linePos.length-1).join(",");
        var pos={linePos:linePos,labelPos:labelPos};
        return pos;
    }
    //获取连线信息
    function getLinkInfoForFlash(id,data){
        var info={};
        $.each(data,function(i,item){
            if(id==item.id){
                info=item;
                if(item.targetRef.indexOf("EndCancelEvent")>-1||item.targetRef.indexOf("cancel")>-1){
                	//指向作废的迁移线cancel属性值设置为1
                	info.cancel="1";
                }
                var targetInfo=bpmnPublic.getElementDataById(item.targetRef);//迁移线指向节点的信息
                if(item.targetRef.indexOf("TaskEvent")>-1||item.targetRef.indexOf("task")>-1){
                	//指向通知活动的迁移线notificationType属性值设置为1
                	var targetInfo=bpmnPublic.getElementDataById(item.targetRef);//迁移线指向节点的信息
                	if(targetInfo.taskType=="通知活动") info.notificationType="1";	
                }
                return false;
            }
        });
        return info;
    }
    //获取实例位置信息
    function getElementPosForFlash(id,data){
        var pos;
        $.each(data,function(i,item){
            if(id==item.bpmnElement){
                var temp=item.items[0];
                pos=temp.x+","+temp.y+","+shapeSize.width+","+shapeSize.height;
                return false;
            }
        });
        return pos;
    }
    //获取附加的属性值信息
    function getExtendAttribute(id,type){
        var processAttr=bpmnPublic.getElementDataById(id,type);//获取存储的节点属性信息，存入xml
        var tagAttr=getTagAttribute(processAttr,id,type);//获取标签上的属性值
        var childItem=getTagChildAttribute(processAttr,type);
        if(childItem.length>0){
            tagAttr["items"]=childItem;
        }
        return tagAttr;
    }
    //获取需要附加在标签上的属性值
    function getTagAttribute(data,id,type){
        var attr=["internationalKey"];
        switch (type) {
            case "startevent":
            	attr = attr.concat(["doURL", "viewName", "viewCode", "ignorePermission"]);
                break;
            case "endevent":
            	attr = attr.concat(["effectFlag"]);
                break;
            case "taskevent":
            	attr = attr.concat(["sequence", "bulkDealFlag", "dealSet", "webSignetFalg", "recallAble", "customParam", "showInSimpleDealInfo", "mobileApprove", "ignorePermission", "isAllowProxy", "requiredTime", "overdueReminders"]);
                break;
            case "countersignevent":
                //会签（当循环会签未选中时清空选人范围选项）
                if(!data.loopSign) data.loop="";
                attr = attr.concat(["sequence", "bulkDealFlag", "loop", "dealSet", "webSignetFalg", "recallAble", "customParam", "showInSimpleDealInfo", "mobileApprove", "ignorePermission", "isAllowProxy", "requiredTime", "overdueReminders"]);
                break;
            case "joinevent":
            	attr = attr.concat(["multiplicity"]);
                break;
            case "decisionevent":
            	attr = attr.concat(["expr"]);
                break;
            case "infoevent":
            	attr = attr.concat(["topic"]);
                break;
            case "subprocessevent":
            	attr = attr.concat(["sub-process-key"]);
                break;
            case "sequenceflow":
                //会签（当是否可选人未选中时清空选人范围选项）
                if(!data.ableSelectStaff) data.selectStaff="";
                attr = attr.concat(["encode", "selectStaff", "requiredStaff", "defaultSelectStaff", "sequence","cancel","description"]);
                break;
        }
        //取属性值(若值为空或为无效值false则不显示该属性值)
        var tagAttr={};
        $.each(attr,function(i,key){
        	//默认值为选中状态的属性修改为false也需要存值
        	//（如人工、会签的showInSimpleDealInfo【简易版处理意见显示】、recallAble【是否可撤回】、mobileApprove【支持移动端审批】、isAllowProxy【是否允许委托】以及结束的effectFlag【流程是否生效】）
            //开始活动的ignorePermission【不需要分配权限】
        	var speAttr=["ignorePermission","showInSimpleDealInfo","recallAble","mobileApprove","isAllowProxy","effectFlag"];
        	if((data[key]||speAttr.indexOf(key)>-1)&&data[key]!==""){
            	if(key=="expr"||key=="customParam"||key=="topic"){
            		tagAttr[key]=transSpecialCharForXML(data[key]);//逻辑表达式、自定义参数、消息主题特殊字符需替换为转义字符
            	}else{
            		tagAttr[key]=booleanToString(data[key]);//存值布尔型需转字符串
            	}  
            }
        	var transformKey=["requiredStaff","defaultSelectStaff"];//迁移线必填、默认选择上次选择人员
        	if(transformKey.indexOf(key)>-1&&tagAttr[key]=="true") tagAttr[key]="1";
        });
        //人工活动需区分普通活动和通知活动
        if(type=="taskevent"&&data.taskType=="通知活动") tagAttr["xtype"]="notification";
        return tagAttr;
    }
    //获取附加在流程子节点的属性值
    function getTagChildAttribute(data,type){
        var childItem=[];
        if(type=="taskevent"||type=="countersignevent"){
            //人工活动、会签活动有参与者信息（部分存xml）、视图、触发事件
            var assignObj=getAssignmentAttr(data);//参与者
            var openActionObj=getOpenActionAttr(data);//视图
            var eventArr=getEventAttr(data.event);//触发事件
            childItem.push(assignObj);
            childItem.push(openActionObj);
            childItem=childItem.concat(eventArr);//触发事件返回格式为数组，使用连接方式而非追加
        }
        if(type=="taskevent"||type=="countersignevent"||type=="autoevent"||type=="endevent"){
            //人工活动、会签活动、自动活动、结束活动有消息提醒信息
            var noticeObj=getNoticeAttr(data);
            if(noticeObj&&noticeObj.xtype) childItem.push(noticeObj);
            
        }
        if(type=="autoevent"){
            //自动活动自动脚本信息
            var autoObj=getAutoScriptAttr(data);
            if(autoObj&&autoObj.xtype) childItem.push(autoObj);
        }
        if(type=="sequenceflow"){
            //迁移线补充逻辑条件、触发事件
            if(data.expr&&data.expr!=""){
                var obj={
                    xtype:"condition",
                    expr:transSpecialCharForXML(data.expr)
                }
                childItem.push(obj);
            }
            var eventArr=getEventAttr(data.event);//触发事件
            childItem=childItem.concat(eventArr);//触发事件返回格式为数组，使用连接方式而非追加
        }
        if(type=="subprocessevent"){
        	//子流程参数设置
        	var paramArr=getParamterAttr(data.paramSet);//参数设置
        	childItem=childItem.concat(paramArr);//参数设置返回格式为数组，使用连接方式而非追加
        }
        return childItem;
    }
    //获取参与者候选人属性信息
    function getAssignmentAttr(data){
        var assignmentKey=["inputorFlag","leaderFlag","bigLeaderFlag","activityDealFlag","flowDealFlag","attentFlag","staffIds"];//参与者+候选人
        var fieldArr=[];
        $.each(assignmentKey,function(i,key){
        	var getVal=booleanToString(data[key])//存值布尔型需转字符串
        	if(key=="staffIds") getVal=transSpecialCharForXML(data[key]);//候选人需对特殊字符进行过滤处理
            fieldObj={
                xtype:"field",
                name:key,
                items:[{
                    xtype:"string",
                    value:getVal
                }]
            }
            fieldArr.push(fieldObj);
        });
        var assignObj={
            xtype:"assignment-handler",
            class:"com.supcon.orchid.workflow.engine.handlers.assignment.GetUserListHandler",
            items:fieldArr
        }
        return assignObj;
    }
    //获取调用视图属性信息
    function getOpenActionAttr(data){
        var openActionObj={
            xtype:"open-action",
            url:data.viewUrl,
            name:data.viewName,
            viewType:data.viewType,
            viewCode:data.viewCode,
            target:data.target
        }
        return openActionObj;
    }
    //获取消息提醒属性信息
    function getNoticeAttr(data){
        var noticeKey=["email","jabber","sms","app"];//消息提醒
        var noticeArr=[],noticeObj={};
        $.each(noticeKey,function(i,key){
            if(data[key]){
                var obj={
                    xtype:key,
                    enabled:"true",
                    items:[{
                        xtype:"receiver"
                    },{
                        xtype:"subject"
                    },{
                        xtype:"content"
                    }]
                }
                noticeArr.push(obj);
            }
        });
        //至少选中一项
        if(noticeArr.length>0){
            noticeObj={
                xtype:"notice",
                items:noticeArr
            }
        }
        return noticeObj;
    }
    //获取自动脚本属性信息
    function getAutoScriptAttr(data){
        var autoObj={};
        var name=data.scriptName;
        var code=data.scriptCode;
        var isVaildName=name&&name!="";
        var isVaildCode=code&&code!="";
        if(isVaildName||isVaildCode){
            autoObj={
                xtype:"auto-script",
                name:name,
                code:code
            }
        }
        return autoObj; 
    }
    //获取触发事件属性信息
    function getEventAttr(event){
    	var eventArr=[];
    	if(event&&event.length>0){
    		//存在触发事件 	
    		for(var i=0;i<event.length;i++){
    			var item=event[i];
    			var eventObj={
        				"xtype":"on",
                        "name":item.name,
                        "event":item.triggerPoint,
                        "items":[{
                        	"xtype":"event-listener",
                            "class":item.triggerName
                        }]
        		}
    			eventArr.push(eventObj);
    		}	
    	}	
    	return eventArr;
    }
    //获取参数设置属性信息
    function getParamterAttr(param){
    	var paramArr=[];
    	if(param&&param.length>0){
    	   //存在参数设置
    	   for(var i=0;i<param.length;i++){
    		   var item=param[i];
    		   var paramObj={
    				   "xtype":"parameter-"+item.type,
    				   "var":item["var"],
    				   "subvar":item["subvar"]
    		   }
    		   paramArr.push(paramObj);
    	   }
    	}
    	return paramArr;
    }
    //布尔型转字符串
    function booleanToString(data){
        if(typeof data =="boolean"){
            data=JSON.stringify(data);
        }
        return data;
    }
    //xml特殊字符转义
    function transSpecialCharForXML(str){
    	str=str.replace(new RegExp("&",'gm'),'&amp;');//"&"需替换为转义字符
    	str=str.replace(new RegExp("<",'gm'),'&lt;');//"<"需替换为转义字符
    	str=str.replace(new RegExp('"','gm'),'&quot;');//双引号需替换为转义字符
    	str=str.replace(new RegExp('\n','gm'),'&#xD;');//换行符需替换为转义字符
//    	str=str.replace(new RegExp(">",'gm'),'&gt;');//"<"需替换为转义字符
    	return str;
    }
    //xml转义字符转化为html内容
    function transSpecialCharForHTML(str){
    	str=str.replace(new RegExp("&lt;",'gm'),'<');
    	str=str.replace(new RegExp('&quot;','gm'),'"');
    	str=str.replace(new RegExp('&#xD;','gm'),'');
    	str=str.replace(new RegExp("&amp;",'gm'),'&');
//    	str=str.replace(new RegExp("&gt;",'gm'),'>');
    	return str;
    }
    /**
         * 函数说明
         * @XmlToJson   XML转JSON格式
         * @JsonToXml   JSON转XML格式
         * @xmlConvertRorBpmn  旧数据转换为能被bpmn解析的xml数据
         * @xmlConvertRorFlash bpmn数据转化为旧版flash数据
         */
    return {
        XmlToJson: new XmlToJson(),
        JsonToXml: new JsonToXml(),
        xmlConvertRorBpmn: new xmlConvertRorBpmn(),
        xmlConvertRorFlash:new xmlConvertRorFlash()
    }
});