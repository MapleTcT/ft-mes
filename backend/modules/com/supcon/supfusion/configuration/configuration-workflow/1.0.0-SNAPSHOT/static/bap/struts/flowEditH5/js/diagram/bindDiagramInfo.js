/**
 * 流程展示数据绑定
 * 业务逻辑定义
 */
define(function (require, exports, module) {
    var bpmnModeler;
    var bpmnPublic=require("./bpmnPublic.js");//基本方法
    var xmlConvert = require("./xmlConvert.js");//流程图数据转换
    var lineColor=["#868686","#868686","#5FB878"];//线条颜色
    var bindDiagramInfo=function(bpmn){
        bpmnModeler=bpmn;
        this.initData();//请求并绑定流程状态
    }
    //请求并绑定流程状态
    bindDiagramInfo.prototype.initData=function(){
        var hisData=getHistoryData();//获取流程完成状态（连接线是否生效，生效即迁移线两端生效）
        if(!hisData) return;
        var canvas = bpmnModeler.get('canvas');
        var canvasEle = canvas._elementRegistry._elements;
        for (var key in canvasEle) {
            var item = canvasEle[key].element;
            var itemType = item.type;
            var itemId = item.id;
            if(itemType == "bpmn:SequenceFlow"){
            	$.each(hisData.effectLine,function(i,id){
                	if (id==itemId) {
                        setEffectStyle(item);//设置流程生效样式(迁移线)
                    }
                }); 
            }else{
            	$.each(hisData.effectNode,function(i,id){
                	if (id==itemId) {
                        setShapeEffective(item);
                    }
                });
                //当前停留节点
            	$.each(hisData.curNode,function(i,item){
            		 if(item==itemId){
                     	var eleOutline=$("g[data-element-id="+itemId+"]").find(".djs-outline");
                     	var style={
                     			visibility:"visible",
                     		    stroke: "#f00",
                                 strokeWidth: "2px",
                                 width:"46",
                                 height:"46",
                                 x:"-3",
                                 y:"-4"
                     	}
                     	eleOutline.css(style).attr("data-state","cur");
                     }
            	}); 	
            }
        }
    }
    //获取流程完成状态(记录已生效的迁移线)
    function getHistoryData(){
    	var historyXML=$("#historyXML",parent.document).val();
    	if(!historyXML||historyXML=="") return;
    	var xml=bpmnPublic.string2XML(historyXML);
    	var xmlChild =xml.childNodes;
        var xmlObj = xmlConvert.XmlToJson.parse(xmlChild[0]);
        var hisData={"effectLine":[],"effectNode":[],"curNode":[]};
        if(xmlObj.items&&xmlObj.items.length>0){
        	$.each(xmlObj.items,function(i,obj){
        		//已生效的活动迁移线
        		if(obj.outcome&&obj.outcome!=""){
        			var outLine=obj.outcome.split(",");
        			hisData.effectLine=hisData.effectLine.concat(outLine);
        		} 
        		//已生效(或当前)的流程节点
        		hisData.effectNode.push(obj.name);
        		//停留活动即当前活动
        		if(obj.status=="1") hisData.curNode.push(obj.name);
        	});
        }	
        return hisData;
    }
    //设置流程生效样式（迁移线）
    function setEffectStyle(data){
        var obj=data.businessObject;
        var id=obj.id;
        var reject=obj.reject;
        var lineEle=$("g[data-element-id="+id+"]").find("g path");
        var markEnd=lineEle.css("marker-end");
        markEnd=markEnd.replace(/#868686/, lineColor[2]);
        var newStyle={
            "stroke":lineColor[2],
            "marker-end":markEnd
        }
        lineEle.css(newStyle);
    }
    //修改为已生效样式
    function setShapeEffective(data){
        var id=data.id;
        var shapeEle=$("g[data-element-id="+id+"]").find("g.djs-visual");
        var fill=$(shapeEle).attr("fill");
        fill=fill.replace(/_gray/,"");
        shapeEle.attr("fill",fill);
        $("g[data-element-id="+id+"]").attr("data-effect","1");
    }
    //绑定流程图信息
    bindDiagramInfo.prototype.bindTool = function (zoom) {
        layui.use(['form', 'slider'], function () {
            var form = layui.form;
            var slider = layui.slider;
            window.slider=slider;
            window.viewSlider=slider.render({
                elem: '#scaleCtrl'
                , value:zoom //[40, 60] //初始值
                , step: 10 //间隔值
                , min: 20 //最小值
                , max: 400 //最大值
                , change: function (value) { //回调实时显示当前值
                    setScale(value);//设置缩放
                }
                , setTips: function (value) { //自定义提示文本
                    return value / 100;
                }
                , theme: '#cfcfcf' //主题色
            });
            //checkbox监听
            form.on('checkbox(hideDescribe)', function (data) {
                //是否隐藏描述
                var isOn = $(data.elem).is(":checked");
                showOrHideDescribe(isOn);

            });
            form.on('checkbox(hideReject)', function (data) {
                //是否隐藏驳回线
                var isOn = $(data.elem).is(":checked");
                showOrHideReject(isOn);
            });
        });
        //全屏方法定义
        $("#fullScreen").on("click", function () {
            var flag = $(this).attr("data-id");
            if (flag == "0") {
                requestFullScreen();
            } else {
                exitFull();
            }
        });
    }
    //是否隐藏描述
    function showOrHideDescribe(isHide) {
        var canvas = bpmnModeler.get('canvas');
        var canvasEle = canvas._elementRegistry._elements;
        for (var key in canvasEle) {
            var item = canvasEle[key].element.businessObject;
            var type = item.$type;
            var id = item.id;
            if (type == "bpmn:SequenceFlow") {
                var label = $(".djs-element[data-element-id=" + id + "_label]");
                $(label).css("display", isHide ? "none" : "block");
            }
        }
    }
    //是否隐藏驳回线
    function showOrHideReject(isHide) {
        var canvas = bpmnModeler.get('canvas');
        var canvasEle = canvas._elementRegistry._elements;
        for (var key in canvasEle) {
            var item = canvasEle[key].element.businessObject;
            var type = item.$type;
            var id = item.id;
            var isReject = item.reject == "1";
            if (type == "bpmn:SequenceFlow" && isReject) {
                var line = $(".djs-element[data-element-id=" + id + "]");
                var label = $(".djs-element[data-element-id=" + id + "_label]");
                $(line).css("display", isHide ? "none" : "block");
                $(label).css("display", isHide ? "none" : "block");
            }
        }
    }
    //设置缩放
    function setScale(val) {
        var canvas = bpmnModeler.get('canvas');
        canvas.zoom(val);
    }
    //全屏显示
    function requestFullScreen() {
        var element = document.documentElement;
        // 判断各种浏览器，找到正确的方法
        var requestMethod = element.requestFullScreen || //W3C
            element.webkitRequestFullScreen || //FireFox
            element.mozRequestFullScreen || //Chrome等
            element.msRequestFullscreen; //IE11
        if (requestMethod) {
            requestMethod.call(element);
        } else if (typeof window.ActiveXObject !== "undefined") { //for Internet Explorer
            var wscript = new ActiveXObject("WScript.Shell");
            if (wscript !== null) {
                wscript.SendKeys("{F11}");
            }
        }
    }
    //退出全屏 判断浏览器种类
    function exitFull() {
        // 判断各种浏览器，找到正确的方法
        var exitMethod = document.exitFullscreen || //W3C
            document.mozCancelFullScreen || //FireFox
            document.webkitCancelFullScreen || //Chrome等
            document.msExitFullscreen; //IE11
        if (exitMethod) {
            exitMethod.call(document);
        } else if (typeof window.ActiveXObject !== "undefined") { //for Internet Explorer
            var wscript = new ActiveXObject("WScript.Shell");
            if (wscript !== null) {
                wscript.SendKeys("{F11}");
            }
        }
    }
    //注：ie调用ActiveX控件，需要在ie浏览器安全设置里面把 ‘未标记为可安全执行脚本的ActiveX控件初始化并执行脚本’ 设置为启用
    //监听退出全屏事件
    window.onresize = function () {
        var isFull = window.screen.height - document.body.clientHeight <= 10;//允许误差
        $("#fullScreen").html(isFull ? getInternationalValByKey("js.ec.workflow.view.exitFullScreen") : getInternationalValByKey("js.ec.workflow.view.fullScreen")).attr("data-id", isFull ? "1" : "0"); 
        $("#resetFitView").trigger("click");//重新适应视图
    }
    return bindDiagramInfo;
});