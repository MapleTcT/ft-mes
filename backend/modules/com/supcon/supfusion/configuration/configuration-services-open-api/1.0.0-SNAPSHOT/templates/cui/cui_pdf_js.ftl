<#macro pdfJs id,route="0",cabPath="",cabVersion="",maxFileSize=100,requestPathPrefix="",openType="pdf",view="edit",width="100%",height="100%",divClass="",divStyle="padding-top:10px",propertyCode="",entityCode="",fileType="",fileLinkId="",isSign=false,isHandSign=false,downloadDoc=false,officePrint=false,productKey="">
<#if getConfigProperty('bap.office.type') == "GOLDGRID">
<script type="text/javascript" src="/bap/static/office/${getCurrent('lang')}/webOfficePlugin.js"" ></script>
<script type="text/javascript" src="/bap/static/office/${getCurrent('lang')}/webPdfPlugin.js"" ></script>
<script type="text/javascript" src="/bap/static/office/${getCurrent('lang')}/webSignPlugin.js"" ></script>
<#else>
<script type="text/javascript" src="/bap/static/office/${getCurrent('lang')}/officePlugin.js"" ></script>
</#if>
<div id="pdfControl_${id}" style="${divStyle}" <#if divClass?? && divClass != "">class="${divClass}"</#if>></div>
<#if route == "0">
<#else>
<script type="text/javascript">
var ${id}_WebPdfControl;
/**
 * BAP-XA-DBZY 张慧东 2017-05-10
 */
var ${id}_WPC_onPrint;
var ${id}_twoDimension;
(function($){	
	YAHOO.util.Event.onDOMReady(function() {
		${id}_WebPdfControl = new CUI.WebPdfControl({
			<#if id??>id : "${id}",</#if>
			cabPath : "<#if cabPath != "">${cabPath}<#else>/bap/static/office/</#if>",
			cabVersion : "<#if cabVersion != "">${cabVersion}<#else>${getConfigProperty('bap.goldgrid.iWebPDF.version')}</#if>",
			<#if height??>height : "${height}",</#if>
			<#if width??>width : "${width}",</#if>
			<#if propertyCode != "">propertyCode : "${propertyCode}",</#if>
			<#if entityCode != "">entityCode : "${entityCode}",</#if>
			<#if view != "">view : "${view}",</#if>
			isSign : <#if isSign?? && isSign>true<#else>false</#if>,
			isHandSign : <#if isHandSign?? && isHandSign>true<#else>false</#if>,
			officePrint : <#if officePrint?? && officePrint>true<#else>false</#if>,
			downloadDoc : <#if downloadDoc?? && downloadDoc>true<#else>false</#if>,
			fileType : "${fileType}",
			openType : "${openType}",
			maxFileSize : ${maxFileSize},
			productKey : "<#if productKey != "">${productKey}<#else>www.cjnf.com.cn</#if>",
			currentStaffName : "${(getCurrent('staffName'))!'bap'}",
			currentUserName : "${(getCurrent('userName'))!'bap'}",
			cus_twoDimension : ${id}_twoDimension,
			cus_printFn : ${id}_WPC_onPrint,
			activityName:"${activityName!}"
		});
		window.onbeforeunload = function(){
			${id}_WebPdfControl.OnClose();
		}
	});
})(jQuery);	

$(window).bind('load',function(){
 	setTimeout(function(){
 		if(${id}_WebPdfControl) {
 			var url = (YAHOO.env.ua.ie > 0) ? "/pdf.office" : "${requestPathPrefix}/pdf.office";
	 		${id}_WebPdfControl.loadPdf(url, "${fileLinkId!}");
 		}
 	},700);
});

/**
 * 跨浏览器控件事件注册调用方法 ,相对于控件中的onMenuClick
 */
${id}_WPC_onMenuClick = function(vIndex, vCaption) {
	${id}_WebPdfControl.onMenuClick(vIndex, vCaption);
}

/**
 * 跨浏览器控件事件注册调用方法 ,相对于控件中的onToolsClick
 */
${id}_WPC_onToolsClick = function(vIndex, vCaption) {
	${id}_WebPdfControl.onToolsClick(vIndex, vCaption);
}

/**
 * 跨浏览器控件事件注册调用方法 ,相对于控件中的onOpen
 */
${id}_WPC_onOpen = function() {
	${id}_WebPdfControl.onOpen();
}

/**
 * 跨浏览器控件事件注册调用方法 ,相对于控件中的onClose
 */
${id}_WPC_onClose = function() {
	${id}_WebPdfControl.onClose();
}
</script>
</#if>
</#macro>	