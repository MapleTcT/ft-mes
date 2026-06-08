/**
 * 公共方法定义
 * 1.实例拷贝
 * 2.是否存在开始事件
 * 3.获取初始值定义
 * 4.时间格式化
 * 5.显示提示信息
 * 6.string类型转xml
 * 7.保存国际化编码
 * 8.控制台输出提示
 * 9.绑定默认属性面板
 */
define(function (require, exports, module) {
    var diConfig=require("./diagramConfig.js");//配置信息
    var typeControl=diConfig.typeControl;//类型配置信息
    var layer;
    layui.use('layer', function () {
        layer = layui.layer;
    });
    //实例拷贝
    var initCopyInstance=function(parent,id){
        var type=parent.$type.split(":")[1];
        var parentName=parent.name;
        var parentId=parent.id;
        var parentData=getElementDataById(parentId,type);//复制对象的属性配置
        var newObjData=JSON.parse(JSON.stringify(parentData));//使用深拷贝的属性配置作为新元素的初始属性数据
        if(parentName&&parentName!="") newObjData.desc=getCopyName(parentId,parentName);//获取拷贝实例的命名
        //国际化编码不能复制
        saveInternationalKey(null,newObjData.desc,function(key){
        	newObjData.internationalKey=key;
        	window.processAttribute[id]=newObjData;//存入公共变量
        });
        return newObjData.desc;
    }
    //拷贝实例命名规则
    function getCopyName(id,name){
        var counter;
        var recordCopy=$("#copyRule");
        if(recordCopy.length==0){
        	$(".g_container").prepend("<input id='copyRule' type='hidden' value='{}'/>");
        }
        var copyRule=JSON.parse($("#copyRule").val());
        if(!copyRule[id]){
            counter=1;
        }else{
            counter=Number(copyRule[id])+1;
        } 
        copyRule[id]=counter;
        $("#copyRule").val(JSON.stringify(copyRule));
        return name+counter;
    }
    //只允许绘制一个开始事件
    function startVerification(data){
        var flag=false;
        $.each(data,function(i,item){
            if(item.type=="bpmn:StartEvent"){
                flag=true;
                showNotice(getInternationalValByKey("js.ec.workflow.config.alert.startNodeExist"));
                return false;
            }
        });
        return flag;
    }
    //获取初始值定义
    function getElementDataById(id,type){
        if(!window.processAttribute) window.processAttribute={};
        if(!window.processAttribute[id]) window.processAttribute[id]=getDefaultAttributeByType(type);
        var getObj=window.processAttribute[id];
        //有视图的流程节点添加viewText属性（内容为视图名+视图编码，该属性只做显示不存入数据）
        if(getObj.viewName&&getObj.viewCode){
        	getObj["viewText"]="";
        	var hasView=getObj.viewName!=""&&getObj.viewCode!="";
        	if(hasView){
        		getObj["viewText"]=getObj.viewName+"（"+getObj.viewCode+"）";
        	}
        }
        return getObj;
    }
    //获取流程图默认属性
    function getDefaultAttributeByType(type){
        var processDefault;
        $.each(typeControl,function(i,item){
            if(item.tagBpmn==type||item.lowercase==type){
                processDefault=item.defaultAttr;
                return false;
            }
        });
        return JSON.parse(JSON.stringify(processDefault));//使用深拷贝保证属性值不被改变
    }
    //时间格式化  
    function dateFtt(fmt, date) { 
        var o = {
            "M+": date.getMonth() + 1,                 //月份   
            "d+": date.getDate(),                    //日   
            "h+": date.getHours(),                   //小时   
            "m+": date.getMinutes(),                 //分   
            "s+": date.getSeconds(),                 //秒   
            "q+": Math.floor((date.getMonth() + 3) / 3), //季度   
            "S": date.getMilliseconds()             //毫秒   
        };
        if (/(y+)/.test(fmt))
            fmt = fmt.replace(RegExp.$1, (date.getFullYear() + "").substr(4 - RegExp.$1.length));
        for (var k in o)
            if (new RegExp("(" + k + ")").test(fmt))
                fmt = fmt.replace(RegExp.$1, (RegExp.$1.length == 1) ? (o[k]) : (("00" + o[k]).substr(("" + o[k]).length)));
        return fmt;
    } 
    //显示提示信息
    function showNotice(msg,callback){
        layer.alert(msg,{
            title:getInternationalValByKey("js.ec.workflow.config.notice"),
            btn:getInternationalValByKey("js.ec.workflow.config.confirmFlag"),
            end:function(){
            	if(typeof callback=="function"){
            		callback();
            	}
            },
            
        });
    }
    //string类型转xml类型
    function string2XML(xmlString) {
        // for IE(IE浏览器)
        if (window.ActiveXObject) {
          var xmlObject = new ActiveXObject("Microsoft.XMLDOM");
          xmlObject.async = "false";
          xmlObject.loadXML(xmlString);
          return xmlObject;
        }
        // for other browsers(火狐,谷歌浏览器等等)
        else {
          var parser = new DOMParser();
          var xmlObject = parser.parseFromString(xmlString, "text/xml");
          return xmlObject;
        }
      }
    //保存国际化编码
    function saveInternationalKey(key,name,callback){
    	//延迟10毫秒避免遍历间隔过短，时间戳重复
    	setTimeout(function(){
    		// 如果数据不以.flag结尾则重新生成key且key添加.flag
			if (!key||key==""||!key.endsWith('.flag')) {
				// 生成key
				key=getInternationalKey();
			}
			saveLocalMessage(key,name,"zh_CN"); // 更新国际化编码对照关系
    		if(typeof callback=="function"){
    			callback(key);
    		}
    	},10);
    }
    //国际化编码规则
    function getInternationalKey(){
    	var randon="randon"+new Date().getTime();
		var moduleCode=$("#moduleCode").val();
		key=moduleCode+'.workflow.'+randon+'.flag';
		return key;
    }
    //控制台输出提示
    function formConsoleOut(data){
        var time=dateFtt("yyyy-MM-dd hh:mm:ss",new Date());//当前时间
//        var code=getRand(3);//获取三位随机数
        var headInfo=time+"&nbsp;&nbsp;";
        var error="";
        $.each(data,function(i,item){
            error+=headInfo+item.info+"<br>";
        });
        $("#consoleTab").prepend(error);
    }
    //获取随机数
    function getRand(num){
        var rand="";
        for(var i = 0; i < num; i++){
            var r = Math.floor(Math.random() * 10);
            rand += r;
        }
        return rand;
    }
    //绑定默认属性面板
    function bindDefaultAttr(){
    	var diagramAttr=[{
            "key":getInternationalValByKey("js.ec.workflow.config.flowName"),
            "value":$("#flowName").val()
        },{
            "key":getInternationalValByKey("js.ec.workflow.config.flowVersion"),
            "value":$("#processVersion").val()
        }];
        $("#diagramAttrBox").html(template("attrTemp", diagramAttr));
    }
    /***
     * 获取当前浏览器类型
     */
    function getBrowserType() {
        var userAgent = navigator.userAgent; //取得浏览器的userAgent字符串
        var isOpera = userAgent.indexOf("Opera") > -1;
        if (isOpera) { //判断是否Opera浏览器
            return "Opera"
        }
        ;
        if (userAgent.indexOf("Firefox") > -1) { //判断是否Firefox浏览器
            return "FF";
        }
        ;
        if (userAgent.indexOf("Chrome") > -1) {
            return "Chrome";
        }
        ;
        if (userAgent.indexOf("Safari") > -1) { //判断是否Safari浏览器
            return "Safari";
        }
        ;
        if (userAgent.indexOf("compatible") > -1 && userAgent.indexOf("MSIE") > -1 && !isOpera) { //判断是否IE浏览器
            return "IE";
        }
        ;
    }
    return {
        initCopyInstance:initCopyInstance,
        startVerification:startVerification,
        getElementDataById:getElementDataById,
        dateFtt:dateFtt,
        showNotice:showNotice,
        string2XML:string2XML,
        saveInternationalKey:saveInternationalKey,
        formConsoleOut:formConsoleOut,
        bindDefaultAttr:bindDefaultAttr,
        getBrowserType:getBrowserType
    }
});