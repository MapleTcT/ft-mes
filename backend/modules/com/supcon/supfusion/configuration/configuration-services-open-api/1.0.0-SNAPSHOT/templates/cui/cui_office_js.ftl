<#macro officeJs id,route="0",cabPath="",cabVersion="",pdfVersion="",executeFunc="",maxFileSize=100,requestPathPrefix="",templateRefMethod="",redRefMethod="",language="CH",openType="doc",saveType="doc",downloadDoc=false,officeShowType=1,view="edit",entityCode="",formId="",callback="",signMark="",signRefMethod="",handSignMark="",commentMark="comment",handSignRefMethod="",status=88,width="100%",height="100%",caption="",customMenuCaption="${getText('foundation.office.myMenu')}",fileOpen=true,isReadonly=false,isCreateNew=true,isNoCopy=false,isSign=false,openEmptyDoc=true,isHandSign=false,saveTemplate=false,isRevision=false,showRevision=false,hideRevision=false,getRevisions=false,acceptRevisions=false,officePrint=false,fileuploadLinkId="",fileuploadType="",propertyCode="",divClass="",divStyle="padding-top:10px",productCaption="",productKey="">
<#if getConfigProperty('bap.office.type') == "GOLDGRID">
<script type="text/javascript" src="/bap/static/office/${getCurrent('lang')}/webOfficePlugin.js"" ></script>
<script type="text/javascript" src="/bap/static/office/${getCurrent('lang')}/webPdfPlugin.js"" ></script>
<script type="text/javascript" src="/bap/static/office/${getCurrent('lang')}/webSignPlugin.js"" ></script>
<#else>
<script type="text/javascript" src="/bap/static/office/${getCurrent('lang')}/officePlugin.js"" ></script>
</#if>
<#--<#assign productPassword = (getConfigProperty("bap.office.productPassword")!'123456')?trim> -->
<div id="officeControl_${id}" style="${divStyle}" class="<#if divClass?? && divClass != "">${divClass}</#if> office-control"></div>
<#if route == "0">
<script type="text/javascript">
var ${id}_OfficeControl;
(function($){	
	YAHOO.util.Event.onDOMReady(function() {
		${id}_OfficeControl = new CUI.OfficeControl({
			<#if id??>id : "${id}",</#if>
			cabPath : "<#if cabPath != "">${cabPath}<#else>/bap/static/office/</#if>",
			cabVersion : "<#if cabVersion != "">${cabVersion}<#else>${getConfigProperty('bap.goldgrid.iWebOffice2009.version')}</#if>",
			pdfVersion : "<#if pdfVersion != "">${pdfVersion}<#else>${getConfigProperty('bap.goldgrid.iWebPDF.version')}</#if>",
			<#if height??>height : "${height}",</#if>
			<#if width??>width : "${width}",</#if>
			<#if caption != "">caption : "${caption}",</#if>
			<#if propertyCode != "">propertyCode : "${propertyCode}",</#if>
			<#if divClass != "">divClass : "${divClass}",</#if>
			<#if divStyle != "">divStyle : "${divStyle}",</#if>
			<#if view != "">view : "${view}",</#if>
			<#if entityCode != "">entityCode : "${entityCode}",</#if>
			<#if customMenuCaption != "">customMenuCaption : "${customMenuCaption}",</#if>
			currentUserName : "${(getCurrent('userName'))!'bap'}",
			currentStaffName : "${(getCurrent('staffName'))!'bap'}",
			fileuploadType : "${fileuploadType}",
			fileOpen : <#if fileOpen?? && fileOpen>true<#else>false</#if>,
			isCreateNew : <#if isCreateNew?? && isCreateNew>true<#else>false</#if>,
			isNoCopy : <#if isNoCopy?? && isNoCopy>true<#else>false</#if>,
			isRevision : <#if isRevision?? && isRevision>true<#else>false</#if>,
			showRevision : <#if showRevision?? && showRevision>true<#else>false</#if>,
			hideRevision : <#if hideRevision?? && hideRevision>true<#else>false</#if>,
			getRevisions : <#if getRevisions?? && getRevisions>true<#else>false</#if>,
			acceptRevisions : <#if acceptRevisions?? && acceptRevisions>true<#else>false</#if>,
			officePrint : <#if officePrint?? && officePrint>true<#else>false</#if>,
			isSign : <#if isSign?? && isSign>true<#else>false</#if>,
			isReadonly:<#if isReadonly?? && isReadonly>true<#else>false</#if>,
			isHandSign : <#if isHandSign?? && isHandSign>true<#else>false</#if>,
			saveTemplate : <#if saveTemplate?? && saveTemplate>true<#else>false</#if>,
			downloadDoc : <#if downloadDoc?? && downloadDoc>true<#else>false</#if>,
			productCaption : "<#if productCaption != "">${productCaption}<#else>铜陵新亚星焦化有限公司</#if>",
			productKey : "<#if productKey != "">${productKey}<#else>37196D4AC825F25A845DBEA47FBAE26FFB59747A</#if>",
			openType : "${openType}",
			saveType : "${saveType}",
			<#if executeFunc != "">executeFunc : "${executeFunc}",</#if>
			<#if signMark != "">signMark : "${signMark}",</#if>
			<#if handSignMark != "">handSignMark : "${handSignMark}",</#if>
			<#if signRefMethod != "">signRefMethod : "${signRefMethod}",</#if>
			<#if handSignRefMethod != "">handSignRefMethod : "${handSignRefMethod}",</#if>
			status : ${status}
		});
	});
})(jQuery);	

$(window).bind('load',function(){
 	setTimeout(function(){
		<#if fileuploadLinkId?? && fileuploadLinkId?has_content>
		${id}_OfficeControl.initalizeOfficeDoc("/foundation/workbench/download?linkId=${fileuploadLinkId!}&type=<#if saveTemplate>bapTemplate__${fileuploadType}<#else>bapOffice__${fileuploadType}</#if>&propertyCode=${propertyCode!}&entityCode=${entityCode!}&showType=${openType}", "${view}");
		if(!${id}_OfficeControl.getFileOpenedStatus()){
			${id}_OfficeControl.initalizeOfficeDoc("");
		}
		<#else>
		<#if openEmptyDoc?? && openEmptyDoc>
		${id}_OfficeControl.initalizeOfficeDoc("");
		</#if>
		</#if>
 	},800);
});

/**
 * 跨浏览器控件事件注册调用方法 ,相对于控件中的OnDocumentClosed
 */
${id}_OC_onDocumentClosed = function() {
	${id}_OfficeControl.onDocumentClosed();
}

/**
 * 跨浏览器控件事件注册调用方法 ,相对于控件中的OnDocumentOpened
 */
${id}_OC_onDocumentOpened = function(str, doc) {
	${id}_OfficeControl.onDocumentOpened(str, doc);
}

/**
 * 跨浏览器控件事件注册调用方法 ,相对于控件中的AfterOpenFromURL
 */
${id}_OC_afterOpenFromURL = function(doc) {
	${id}_OfficeControl.afterOpenFromURL(doc);
}

/**
 * 跨浏览器控件事件注册调用方法 ,相对于控件中的AfterPublishAsPDFToURL
 */
${id}_OC_afterPublishAsPDFToURL = function(ret, code) {
	${id}_OfficeControl.afterPublishAsPDFToURL(ret, code);
}

/**
 * 跨浏览器控件事件注册调用方法 ,相对于控件中的OnCustomMenuCmd2
 */
${id}_OC_onCustomMenuCmd = function(menuPos,submenuPos,subsubmenuPos,menuCaption,menuID) {
	${id}_OfficeControl.onCustomMenuCmd(menuPos,submenuPos,subsubmenuPos,menuCaption,menuID);
}

/**
 * 跨浏览器控件事件注册调用方法 ,相对于控件中的OnCustomMenuCmd2
 */
${id}_OC_onCustomButtonOnMenuCmd = function(btnPos,btnCaption,btnCmdid) {
	${id}_OfficeControl.onCustomButtonOnMenuCmd(btnPos,btnCaption,btnCmdid);
}

/**
 * 保存之后的回调（用于跨浏览器处理，返回的类似于saveFileToUrl的result）
 */
${id}_OC_afterSaveFileToURL = function(type, code ,html) {
	${id}_afterSaveFileToURL(type, code ,html);
}
</script>
<#else>
<script type="text/javascript">
var ${id}_WebOfficeControl;
(function($){	
	YAHOO.util.Event.onDOMReady(function() {
		${id}_WebOfficeControl = new CUI.WebOfficeControl({
			<#if id??>id : "${id}",</#if>
			cabPath : "<#if cabPath != "">${cabPath}<#else>/bap/static/office/</#if>",
			cabVersion : "<#if cabVersion != "">${cabVersion}<#else>${getConfigProperty('bap.goldgrid.iWebOffice2009.version')}</#if>",
			<#if height??>height : "${height}",</#if>
			<#if width??>width : "${width}",</#if>
			<#if caption != "">caption : "${caption}",</#if>
			<#if propertyCode != "">propertyCode : "${propertyCode}",</#if>
			<#if divClass != "">divClass : "${divClass}",</#if>
			<#if divStyle != "">divStyle : "${divStyle}",</#if>
			<#if view != "">view : "${view}",</#if>
			<#if entityCode != "">entityCode : "${entityCode}",</#if>
			currentUserName : "${(getCurrent('userName'))!'bap'}",
			currentStaffName : "${(getCurrent('staffName'))!'bap'}",
			fileuploadType : "${fileuploadType}",
			fileOpen : <#if fileOpen?? && fileOpen>true<#else>false</#if>,
			isCreateNew : <#if isCreateNew?? && isCreateNew>true<#else>false</#if>,
			isNoCopy : <#if isNoCopy?? && isNoCopy>true<#else>false</#if>,
			isRevision : <#if isRevision?? && isRevision>true<#else>false</#if>,
			showRevision : <#if showRevision?? && showRevision>true<#else>false</#if>,
			hideRevision : <#if hideRevision?? && hideRevision>true<#else>false</#if>,
			getRevisions : <#if getRevisions?? && getRevisions>true<#else>false</#if>,
			acceptRevisions : <#if acceptRevisions?? && acceptRevisions>true<#else>false</#if>,
			officePrint : <#if officePrint?? && officePrint>true<#else>false</#if>,
			isSign : <#if isSign?? && isSign>true<#else>false</#if>,
			isHandSign : <#if isHandSign?? && isHandSign>true<#else>false</#if>,
			saveTemplate : <#if saveTemplate?? && saveTemplate>true<#else>false</#if>,
			<#if signMark != "">signMark : "${signMark}",</#if>
			<#if handSignMark != "">handSignMark : "${handSignMark}",</#if>
			<#if signRefMethod != "">signRefMethod : "${signRefMethod}",</#if>
			<#if handSignRefMethod != "">handSignRefMethod : "${handSignRefMethod}",</#if>
			<#if commentMark != "">commentMark : "${commentMark}",</#if>
			<#if templateRefMethod != "">templateRefMethod : "${templateRefMethod}",</#if>
			<#if redRefMethod != "">redRefMethod : "${redRefMethod}",</#if>
			language : "${language}",
			openType : "${openType}",
			saveType : "${saveType}",
			downloadDoc : <#if downloadDoc?? && downloadDoc>true<#else>false</#if>,
			officeShowType : ${officeShowType},
			isReadonly:<#if isReadonly?? && isReadonly>true<#else>false</#if>,
			maxFileSize : ${maxFileSize},
			productKey : "<#if productKey != "">${productKey}<#else>www.cjnf.com.cn</#if>",
			status : ${status},
			<#if pendingId??>pendingId : ${pendingId}</#if>
		});
	});
})(jQuery);	

$(window).bind('load',function(){
 	setTimeout(function(){
 		if(${id}_WebOfficeControl) {
 			var url = (YAHOO.env.ua.ie > 0) ? "/office.office" : "${requestPathPrefix}/office.office";
	 		${id}_WebOfficeControl.loadDoc(url, "${fileuploadLinkId!}");
 		}
 	},800);
});

/**
 * 跨浏览器控件事件注册调用方法 ,相对于控件中的onMenuClick
 */
${id}_WOC_onMenuClick = function(vIndex, vCaption) {
	${id}_WebOfficeControl.onMenuClick(vIndex, vCaption);
}

/**
 * 跨浏览器控件事件注册调用方法 ,相对于控件中的onToolsClick
 */
${id}_WOC_onToolsClick = function(vIndex, vCaption) {
	${id}_WebOfficeControl.onToolsClick(vIndex, vCaption);
}

<#if view != "readonly" && !isReadonly>
setInterval("${id}_WebOfficeControl.updateOpenTime('${fileuploadLinkId!}')", 300000);
</#if>
</script>
</#if>
</#macro>	