<#--
	fieldcodes 为查询的条件组合
	onclick 如果不为空，则生成取消 确定按钮
	formId与queryfield的formId 必须一样
	onadvancedclick 如果不为空，则用高级查询
	unique 用于存储最后一次查询的条件 且须以LAST_QUERY开头， 如 LAST_QUERY_positionTest,unqiue值必须保证在整个系统中唯一,所以建议使用LAST_QUERY+url
-->
<#macro quickquery fieldcodes, onclick="",isExpandAll=false, datatableId="",formId="",idprefix="",onadvancedclick="",style="",unique="",divStyle="float:left;margin:10px 0px 5px 10px;",expandType="single">
<#assign codes = fieldcodes?split('||')>
<#assign _datatableId = datatableId>
<#if !(_datatableId?has_content)>
	<#assign _datatableId = idprefix + "_query">
</#if>
<div id="${formId}_div" style="${divStyle}">
	<input id="cookieId" name="cookie_Last_query_id" type="hidden" reset="false">
    <input id="cookieType" name="cookie_Last_query_type" reset="false" type="hidden" value="${unique}">
    <input id="cookieValue" name="cookie_Last_query_value" type="hidden" reset="false">
  <!--  <input id="cookieVersion" name="cookie_Last_query_version" reset="false" type="hidden">-->
    <input id="originalcookieValue" type="hidden" reset="false">
  	<input id="cookieExpandType" name="cookie_expand_type" type="hidden" reset="false" value="EXPAND_TYPE_${formId}">
  	<input id="cookieExpandTypeValue" name="cookie_expand_type_value" type="hidden" reset="false" value="${expandType}">
  	<input id="oldExpandTypeValue" name="old_expand_type_value" type="hidden" reset="false" value="${expandType}">
  	<div class="cui_quickquery" style="margin-top:5px;display:inline;">
	<select reset="false" name="queryParam" id="${formId}_queryParam"  class="list-select" onchange="${formId}_changeInput(this)" style="display:none;">
		<#if codes??>
			<#list codes as code>
				<#assign keys=code?split(':')>
				<option value="${keys[0]?replace('.', '_')}">
				<#if keys?size==1 >
					<#assign propertys=quickQuery(keys[0])>
					${getText((propertys.displayName)!)}
				<#else>
					${getText(keys[1])}
				</#if>
				</option>
			</#list>
		</#if>
  	</select>
  	</div>
  	<div id="${formId}_inputValue"  style="float:left;margin-right: 10px;width:230px;margin-top:5px;<#if style != "">${style}</#if>"> 
  	</div>
  	<div id="${formId}_allContainer" class="queryForm-allContainer clearfix" style="display:none;width:97%;float:left;">
  	
  	</div>
   <#nested>
</div>
<script type="text/javascript">

//百分比类型格式化
$(function(){
	$('#${formId}_div input[showformat="PERCENT"]')
		.on('blur', function(){
			var val = $(this).val();
			if (val !='' && isDecimal(val)) {
				$(this).attr('truevalue', val);
				$(this).val(val + '%');
			} else {
				$(this).attr('truevalue', '');
				$(this).val('');
			}
		})
		.on('focus', function(){
			var val = $(this).val();
			if (val !=''){
				var trueVal = $(this).attr('truevalue');
				if(trueVal=='' || trueVal === undefined){
					$(this).val('');
				}else{
					$(this).val(trueVal);			
				}	
			} else {
				$(this).attr('truevalue', '');
			}													
		});
});

var ${formId}orignal;
var ${formId}DefaultValueTuple = {};
function ${formId}_changeInput(obj){
	if( $( "#${formId}_allContainer" ).is( ':visible' ) ){
		 ${formId}_expandAllCondition();
	}
	var inputid="showInput${formId}_"+obj.value;
	var orignalid="showInput${formId}_"+${formId}orignal;
	var targetObj = $(document.getElementById(inputid));
	var orignalObj = $(document.getElementById(orignalid));
	var selectObj = CUI("input[type!='button'][type!='hidden'][type!='checkbox'],select",targetObj.children().children());
	var mneObj = CUI("input[mneType='mnemonic']",targetObj);
	if(targetObj != undefined && targetObj.children().length!=0){
		if(MneObj) {
			MneObj = new Object();
			_param = null;
		}
		CUI("#${formId} #${formId}_inputValue input[reset!='false'] ").val("");
		CUI("#${formId} #${formId}_inputValue select").val("");
		CUI("#${formId} #${formId}_inputValue .dropselectbox h4").html('');
	}
	if(targetObj.attr('pType') == 'DATETIME' || targetObj.attr('pType') == 'DATE' || targetObj.attr('showFormat').indexOf('YMD') == 0) {
		$("#${formId} #${formId}_inputValue").css("width","320px");
	} else {
		$("#${formId} #${formId}_inputValue").css("width","230px");
	}
	${formId}defaultValueMethod(selectObj);
	orignalObj.append($("#${formId} #${formId}_inputValue").children());
	$("#${formId} #${formId}_inputValue").append(targetObj.children());
	${formId}orignal=obj.value;
	if(mneObj.val() == "" && mneObj != undefined){
		if(mneObj.attr("name")) {
			CUI.restoreMneTips(mneObj,mneObj.attr("name").replace(/\./ig,'_') + '_mneTipLabel','${formId}');
		}
	}
	var cookievalue=$('#${formId}_queryParam option:selected').val();
	$("#${formId} #cookieValue").val(cookievalue);
}

function ${formId}defaultValueMethod(selectObj) {
	if(selectObj!=undefined && selectObj.attr("name")!=undefined){
		var objName = selectObj.attr("name");
		var hideId = objName.substring(0,objName.lastIndexOf("."))+".id";
		var tagname = selectObj.prop("tagName");
		var defaultValue = selectObj.attr("deValue");
		if(defaultValue!=undefined && defaultValue!='' && (selectObj.val() == null || selectObj.val() == "")){
			var objNamePrefix = objName.substring(0,objName.lastIndexOf("."));
			var objNameSuffix = objName.substring(objName.lastIndexOf(".") + 1);
			var viewUrl,defaultId,returnValue;
			if(defaultValue=='currentUser' || defaultValue=='currentPost' || defaultValue=='currentDepart' || defaultValue=='currentComp') {
				if(defaultValue=='currentUser'){
					viewUrl = "/foundation/staff/common/staffListFrameset";
					defaultId = "${getCurrent('staffId')}";
				}else if(defaultValue=='currentPost'){
					viewUrl = "/foundation/position/common/positionListFrame";
					defaultId = "${getCurrent('positionId')}";
				}else if(defaultValue=='currentDepart'){
					viewUrl = "/foundation/department/common/departmentListFrame";
					defaultId = "${getCurrent('departmentId')}";
				}else if(defaultValue=='currentComp'){
					viewUrl = "/foundation/company/common/companyListFrame";
					defaultId = "${getCurrent('companyId')}!";
				}
				if(${formId}DefaultValueTuple.hasOwnProperty(objName)) {
					returnValue = ${formId}DefaultValueTuple[objName];
				} else {
					returnValue = foundation.common.getObjectForQuickQuery(objNameSuffix, defaultId, viewUrl);
					${formId}DefaultValueTuple[objName] = returnValue;
				}
				selectObj.val(returnValue);
				selectObj.attr('originalValue', returnValue);
				CUI("input[id='"+hideId+"']").val(defaultId);
			}else if(defaultValue=='true' && tagname=='SELECT'){
				selectObj.val("1");
			}else if(defaultValue=='false' && tagname=='SELECT'){
				selectObj.val("0");
			}else{
				if(selectObj.length > 1){
					for(var j=0;j<selectObj.length;j++){
						var d = $(selectObj.get(j)).attr("deValue");
						if( tagname=='SELECT' ){
							$(selectObj.get(j)).setValue(d);
						}else{
							$(selectObj.get(j)).val(d);
						}
					}
				}else{
					if( tagname=='SELECT' ){
						selectObj.setValue(defaultValue);
					}else{
						selectObj.val(defaultValue);
					}
				}
				
			}
		}
	}
}

function ${formId}_expandAllCondition(){
	var queryForm = $( "#${formId}" );
	var queryFormDiv = $('#${formId}_div');
	var container = $( "#${formId}_allContainer" );
	var dataClassifyContainer = $( '#${formId}_data_classify' );
	var btnToggle =  $( "#${formId}_btn_toggle" );
	var inputContainer = $( "#${formId}_inputValue");
	var dcSpan = $("#${formId}_dcSpan");
	var quickquery = $( ".cui_quickquery", queryForm );
	var buttonbar = $( 'div.quick-query-buttonbar', queryForm );
	var parentLayout = $(queryForm).closest(".extra-layout");
	if( container.css( "display" ) == "block" ) {
		buttonbar.css( { 'visibility': 'hidden', 'margin-top': 0 } );
		if( YAHOO.env.ua.ie != 6 ){
			queryFormDiv.css( 'float', 'left' ).removeClass( 'clearfix' );
		}
		if( YAHOO.env.ua.ie == 6 ){
			if( dataClassifyContainer.length > 0 ) {
				quickquery.css( 'margin-left', 0 );
			}
			buttonbar.css( { 'margin-left': 0, 'margin-top': 0 } ) ;
		} 
		if( YAHOO.env.ua.ie && YAHOO.env.ua.ie != 6 ){
			buttonbar.css( { 'float': 'none', 'margin-top': '0', 'margin-bottom': '6px' } );
		}
		queryForm.css( "float", "left" );
		var queryContents = $( "div.quick_query_content", queryForm );
		for ( var i = 0, l = queryContents.length; i < l; i++ ) {
			var item = queryContents.eq( i );
			var son = item.children( );
			if( son.eq( 0 ).attr( "currentObj" ) == "true" ) {
				var labelText = $( 'label.quick_query_label', queryForm ).eq( i ).text();
				inputContainer.append( son );
				$( 'div.dropselectbox h4' , quickquery ).text( labelText ).attr( 'title', labelText );
			}else{
				$( "#" + item.attr( "pDivID" ) , queryForm ).append( son );
			}
		}
		dataClassifyContainer.css( 'visibility', 'hidden' ).css('width', "");
		if( dataClassifyContainer.length > 0 ) {
			queryFormDiv.before( dataClassifyContainer );
			//$( '.cui-fast-split' ).show();
		}
		dataClassifyContainer.css( { 'margin': '15px 0 0 12px', 'position': 'static' });
		dcSpan.attr('bakspantext', '${getText("ec.view.dataclassific")}');
		if($('#${idprefix}_query_selectClassify').val() == '') {
			dcSpan.text('${getText("ec.view.dataclassific")}');
		}
		container.slideToggle(50, function(){
			dataClassifyContainer.css( 'visibility', 'visible' );
			buttonbar.css( 'visibility', 'visible' );
			//buttonbar.fadeIn();
			container.children().remove();
			// btnToggle.addClass( "quick-query-btn-expand" ).removeClass( "quick-query-btn-collapse" ).css( 'position', 'static' ).attr( 'title', '展开' );;
			$( '#${formId}_btn_collapse' ).hide();
			inputContainer.show();
			quickquery.show();
			CUI("input[reset!='false'],select",$("div:hidden[id^='showInput']")).val("");
			CUI(".dropselectbox h4",$("div:hidden[id^='showInput']")).html('');	
			$("#${formId} #cookieExpandTypeValue").val('single');
			try{
				var pt = ${_datatableId}Widget || ${_datatableId}Widget._DT;
				if( ${_datatableId}Widget && ${_datatableId}Widget._initDomElements ) {
					${_datatableId}Widget._initDomElements();
				}else{
					// 控件版触发 resize 事件
					$(window).trigger('resize');
				}
			}catch(e){
			
			}
			//增强型布局下，快速查询块自适应
			/*
			if(parentLayout.length == 1){
				if(undefined != $(parentLayout).attr("heightBak") && "" != $(parentLayout).attr("heightBak") && "0" != $(parentLayout).attr("heightBak")){
					$(parentLayout).height($(parentLayout).attr("heightBak"));	
				}else{
					$(parentLayout).height($(queryForm).height());
				}
	
			}
			*/
			$(window).trigger('resize');	
		})
	} else {
		$( 'div.dropselectbox h4' , quickquery ).text( '显示全部' ).attr('title','显示全部');
		if( YAHOO.env.ua.ie != 6 ){
			quickquery.hide();
			queryFormDiv.css( 'float', 'none' ).addClass( 'clearfix' );
		}else if( dataClassifyContainer.length > 0 ) {
			quickquery.css( 'margin-left', '15px' );
		}
		var winWidth = queryForm.parents( 'div[region]' ).eq(0).width() - 12 - ( YAHOO.env.ua.ie == 6 ? 133 : 0  );
		if(winWidth <= 0){
			<#if isExtra?? && isExtra>
			if(queryForm.parents( 'div.extra-layout' ).length > 0){
				winWidth = calWidthInPercent(queryForm.parents( 'div.extra-layout' ).eq(0)[0]) - 12 - ( YAHOO.env.ua.ie == 6 ? 133 : 0  );
			}			
			</#if>
		}
		if ( YAHOO.env.ua.ie == 6 && queryForm.parents( 'div.ewc-dialog-el' ).length > 0 ) {
			winWidth -= 10;
		}
		//if( YAHOO.env.ua.ie == 6 || YAHOO.env.ua.ie == 7 ){
			container.width( winWidth );
		//}
		// 有日期控件时需要保证最小宽度 否则会显示不全
		var nodeWidth = winWidth > ( $( 'input.cui-calpick', queryForm ).length > 0 ? 1185 : 1024 ) ? "33%" : "49%";
		queryForm.css( 'float', 'none' );
		var options = document.getElementById( '${formId}_queryParam' ).options;
		var inputId = '';
		var cssInputid = '';
		var inputName = '';
		
		var df = document.createDocumentFragment();
		if( dataClassifyContainer.length > 0 ) {
			var dataClassifyNode = $( '<div class="quick_query_node" style="position:relative;z-index:3;width:' + nodeWidth + '"><label class="quick_query_label"><span class="quick_query_text" style="padding-right: 5px;">${getText("ec.view.dataclassific")}</span></label><div class="quick_query_content"></div></div>'  );
			dataClassifyContainer.css( { 'margin': '0', 'position': 'relative', width: '96%'} );
			dcSpan.attr('bakspantext', '${getText("ec.MenuInfo.Choice")}');
			if($('#${idprefix}_query_selectClassify').val() == '') {
				dcSpan.text('${getText("ec.MenuInfo.Choice")}');
			}
			$( dataClassifyNode[0].lastChild ).append( dataClassifyContainer );
			df.appendChild( dataClassifyNode[0] );
			//$( '.cui-fast-split' ).hide();
		}
		for( var i = 0, l = options.length; i < l; i++ ) {
			inputId = "showInput${formId}_" + options[i].value;
			cssInputid = "showInput${formId}_" + options[i].value + "_css";
			inputName = options[i].text;
			var inputObj;
			if ( $( "#" + inputId, queryForm ).children().length > 0 ) {
				inputObj = $( "#" + inputId, queryForm ).children();
				inputObj.attr( "currentObj", "false" );
			}else{
				inputObj = inputContainer.children();
				inputObj.attr( "currentObj", "true" );
			}
			var queryFiledCss = $( "#" + cssInputid, queryForm ).val();
			var selectObj = CUI("input[type!='button'][type!='hidden'][type!='checkbox'],select",inputObj.children());
			${formId}defaultValueMethod(selectObj);
			var node = $( '<div class="quick_query_node" style="width:' + nodeWidth + ';' + queryFiledCss + '"><label class="quick_query_label" style="'+queryFiledCss+'"><span class="quick_query_text">' + inputName + '</span></label><div class="quick_query_content"></div></div>');
			$( node[0].lastChild ).append( inputObj).attr( "pDivID", inputId );
			df.appendChild( node[0] );
		}
		df.appendChild( $('<div style="clear:both"></div>')[0] );
		container[0].appendChild(df);
		inputContainer.hide();
	
		buttonbar.css( 'visibility', 'hidden' );
		$("#${formId} #cookieExpandTypeValue").val('all');
		if( YAHOO.env.ua.ie == 6 ){
			buttonbar.css( { 'margin-left': winWidth - buttonbar.outerWidth() - 50, 'margin-top': '5px' } ) ;
		}
		if( YAHOO.env.ua.ie && YAHOO.env.ua.ie != 6 ){
			buttonbar.css( { 'float': 'right', 'margin-top': '5px', 'margin-bottom': 0 } );
		}	
		if( YAHOO.env.ua.ie != 6 && $( 'div.quick_query_node', container ).length % ( winWidth > ( $( 'input.cui-calpick', queryForm ).length > 0 ? 1185 : 1024 ) ? 3 : 2 ) != 0 ){
			buttonbar.css( 'margin-top', '-38px' );
		}
		container.slideToggle(50, function(){
			if ( $( '#${formId}_btn_collapse' ).length == 0 ) {
				buttonbar.prepend( '<a class="v3-btn-collapse" id="${formId}_btn_collapse" style="display:none;" onclick="${formId}_expandAllCondition()" title="${getText("common.button.packup")}"></a>' );
			}
			$( '#${formId}_btn_collapse' ).show();
			buttonbar.css( 'visibility', 'visible' );
			if( YAHOO.env.ua.ie == 6 || YAHOO.env.ua.ie == 7 ){
				$( 'span.quick_query_text', container ).each( function(){
					var that = $( this );
					that.css( 'line-height', that.height() > 28 ? '14px' : '28px'  );
					setTimeout( function(){
				    	that.css( 'line-height', that.height() > 28 ? '14px' : '28px'  );
				   	},100)
				})
			}
			try{
				var pt = ${_datatableId}Widget || ${_datatableId}Widget._DT;
				if( ${_datatableId}Widget && ${_datatableId}Widget._initDomElements ) {
					${_datatableId}Widget._initDomElements();
				}else{
					// 控件版触发 resize 事件
					$(window).trigger('resize');
				}
			}catch(e){
			
			}	
			//增强型布局下，快速查询块自适应
			if($(parentLayout).length == 1){
			/*
				if($(parentLayout).height() > 0){
					$(parentLayout).attr("heightBak",$(parentLayout).height());
				}				
				$(parentLayout).height($(queryForm).outerHeight(true) + $(parentLayout).outerHeight(true) - $(parentLayout).height() + 2);
			*/
				$(parentLayout).css("overflow","visible");
			}
			$(window).trigger('resize');
	});
		
		
		
	}

}

function ${formId}_initinput(){
	//var value=$('#${formId}_queryParam option:selected').val();
	var value=$("#${formId} #originalcookieValue").val();
	if(value==undefined || value==""){
		value=$('#${formId}_queryParam option:selected').val();
	} else {
		var flag = false;
		$.each($('option','#${formId}_queryParam'), function(item){
			if($(this).val() == value) {
				flag = true;
				return;
			}
		});
		if(!flag) {
			value = $('#${formId}_queryParam option:selected').val();
		}
	}
	${formId}orignal=value;
	var inputid="showInput${formId}_"+value;
	var targetObj = $(document.getElementById(inputid));
	if(targetObj.attr('pType') == 'DATETIME' || targetObj.attr('pType') == 'DATE' || targetObj.attr('showFormat').indexOf('YMD') == 0) {
		$("#${formId} #${formId}_inputValue").css("width","320px");
	} else {
		$("#${formId} #${formId}_inputValue").css("width","230px");
	}
	$("#${formId} #${formId}_inputValue").append(targetObj.children());
	var selectObj = CUI("input[type!='button'][type!='hidden'][type!='checkbox'],select",$("#${formId} #${formId}_inputValue").children().children());
	${formId}defaultValueMethod(selectObj);
	var expandAll = $('#${formId} #cookieExpandTypeValue').val();			//缩小查找范围以 解决增强型视图出现多个查询组件  id选择器的问题    by  fukun
	if(expandAll==undefined || expandAll == ""){
		expandAll = "${isExpandAll?string}";
	}
	if(expandAll == undefined || expandAll == "" || expandAll == "single"){
		CUI("input[reset!='false'],select",$("div:hidden[id^='showInput']")).val("");
		CUI(".dropselectbox h4",$("div:hidden[id^='showInput']")).html('');	
	}
}

function ${formId}_initCookie(){
	var currentquery=$("#${formId} #cookieType").val();
	if(currentquery!=undefined&&currentquery!=''){
		<#assign lastQuery = findLastQueryFieldMethod(unique)!>
		<#if lastQuery??>
			<#if !(lastQuery.global?? && lastQuery.global) && lastQuery.id??>
			$("#${formId} #cookieId").val('${lastQuery.id!}');
			</#if>
			/* qc10303在ie9浏览器下给select value设置为空，select返回值不会默认取第一个而是返回undefined */ 
			<#if lastQuery.value?? && lastQuery.value != ''>
				$("#${formId}_queryParam").val('${lastQuery.value!}');
				$("#${formId} #originalcookieValue").val('${lastQuery.value!}');
				$("#${formId} #cookieValue").val('${lastQuery.value!}');
			</#if>
			${formId}_initinput();
			$("#${formId} .list-select").mSelect({type:'list'<#if isExpandAll>, expandFunc:${formId}_expandAllCondition</#if> });
			${formId}_initExpandTypeCookie();
		</#if>
	}else{
	
		${formId}_initinput();
		$("#${formId} .list-select").mSelect({type:'list'<#if isExpandAll>, expandFunc:${formId}_expandAllCondition </#if>});
		${formId}_initExpandTypeCookie();
	}
}

function ${formId}_initExpandTypeCookie(){
	<#if !isExpandAll>
	return;
	</#if>
	var cookieExpandType = $("#${formId} #cookieExpandType").val();
	if(cookieExpandType != undefined && cookieExpandType != ''){

	}
}
	
function ${formId}_getCookieParam(){
	var param="";
	var cookievalue=$("#${formId}_queryParam").val();
	var originalcookieValue=$("#${formId} #originalcookieValue").val();
	if(cookievalue!=undefined && cookievalue!=originalcookieValue){
		var cookieId=$("#${formId} #cookieId").val();
		var cookieType=	$("#${formId} #cookieType").val();
		var cookieVersion=$("#${formId} #cookieVersion").val();
		param="cookie_Last_query_value="+cookievalue;
		if(cookieType!=undefined && cookieType!=""){
			param+="&cookie_Last_query_type="+cookieType;
		}
		if(cookieId!=undefined &&cookieId!=""){
			param+="&cookie_Last_query_id="+cookieId;
		}
		if(cookieVersion!=undefined &&cookieVersion!=""){
			param+="&cookie_Last_query_version="+cookieVersion;
		}
		$("#${formId} #originalcookieValue").val(cookievalue);
	}
	return param;
}
$(function(){
	${formId}_initCookie();
})

</script>
</#macro>


<#-- 
isCustomize 如果true，自定义html代码
isTimeSpan 如果true，则使用两个时间控件，用于查询某段时间
mneurl 助记码url
refurl 选择框url
mneclick 如果true 助记码中使用弹出参照dialog
isCrossCompany用于助记码跨公司查询
ishavingDown 用于查询是否包含下级 如部门和岗位查询
formId与quickquery的formId 必须一样
exp 匹配格式 by fangzhibin
defaultValue 默认值
key 用于name
errorWidget 用于验证开始时间 小于结束时间 的提醒
style	label的样式
-->
<#macro queryfield code,divCode="",showRange="",isCustomize=false,key="",type="other",showFormat="",isTimeSpan=false,mneurl="",refurl="",formId="",defaultValue="",exp="",deploymentId="",mneclick=true,isCrossCompany=false,ishavingDown=false,isPrecise=true,searchClick="",errorWidget="",selectPeople="",fieldCode="",outcome="",sourceStaff="",style="">
<#assign newcode=code?replace('.', '_')>
<#if divCode?? && divCode != "">
<#assign newcode=divCode?replace('.', '_')>
</#if>
<#if quickQuery(code)??>
<#assign property = quickQuery(code)>
</#if>
<input type="hidden" id="showInput${formId}_${newcode}_css" value="${style!}" />
<div id="showInput${formId}_${newcode}" pType="${(property.type)!}" showFormat="${showFormat!}" style="display: none;">	
<#if isCustomize>
	<#nested>
<#else>
	<#if property.type == "DATETIME" || property.type == "DATE">
		<#if isTimeSpan>
			<div style="float:left;width:45%">
			<@datepicker name="${key}_start" id="${key}_start" cssClass="cui-noborder-input" ></@datepicker>
			</div>
			<div style="float:left;margin:1px 3px 0px;*margin-top:3px;">${getHtmlText('foundation.permissionQuery.to')}</div>
			<div style="float:left;width:45%">
			<@datepicker name="${key}_end" id="${key}_end" cssClass="cui-noborder-input" ></@datepicker>
			</div>
		<script type="text/javascript">
			function ${formId}checkTime(){
				if(CUI.trim(CUI("input[name='${key}_start']",CUI("#${formId}")).val()) != "" && CUI.trim(CUI("input[name='${key}_end']",CUI("#${formId}")).val()) != "" && CUI.trim(CUI("input[name='${key}_start']",CUI("#${formId}")).val()) > CUI.trim(CUI("input[name='${key}_end']",CUI("#${formId}")).val())) {
			 		<#if errorWidget!="">
			 		${errorWidget}.show("${getHtmlText('foundation.common.timeCheck')}","f");
			 		<#else>
			 		if(typeof(workbenchErrorBarWidget) != 'undefined'){
			 			workbenchErrorBarWidget.showMessage("${getHtmlText('foundation.common.timeCheck')}","f");
			 		}else {
			 			CUI.Dialog.alert(${getHtmlText("foundation.common.timeCheck")});	
			 		}
			 		</#if>
			 		return false;
	 			}
	 		}
		</script>
		<#else>
			<@datepicker name="${key}" id="${key}" cssClass="cui-noborder-input" ></@datepicker>
		</#if>
	<#elseif property.type=="SYSTEMCODE">
		<@systemcode name="${key}" code="${property.fillcontent}"/>
	<#elseif property.type=="ENUMERATE">
		<select id="${key?replace(".", "")}" name="${key}" class="edit-select ">
		<option value=""></option>
		<#assign fillcontentMap=property.fillcontentMap>
		<#assign maps=fillcontentMap["fillContent"]>
		<#list maps?keys as key>
			<option value="${key}">${maps[key]}</option>
		</#list>
		</select>
	<#else>
		<#if mneurl!="">
			<#assign splitkey=key?split('.')>
			<#assign size=(splitkey?size-1)>
			
			<input type="hidden" id="<#if size==0>${splitkey[0]}.<#else><#list 0..(size-1) as i>${splitkey[i]}.</#list></#if>id" name="<#if size==0>${splitkey[0]}.<#else><#list 0..(size-1) as i>${splitkey[i]}.</#list></#if>id" />
			<#if ishavingDown>
				<div style="float:left;_width:65%;">
				<@mneclient name="${key}" isCrossCompany=isCrossCompany onkeyupfuncname="${formId}_${newcode}_keyUpCallback(obj)" showRange="${showRange!}" 
				     formId="${formId}" id="${formId}_${newcode}_${property.name}" url="${mneurl}" clicked=mneclick iframe=true type="${type}" multiple=false isPrecise=isPrecise searchClick="${searchClick!}" deploymentId="${deploymentId}" selectPeople="${selectPeople}" sourceStaff="${sourceStaff}" fieldCode="${fieldCode}" outcome="${outcome}" cssStyle="width:147px;"/>
				</div>
				<div style="float:left;margin-top:5px"><input type="checkbox" style="border:none;vertical-align: middle; margin: 0 3px 0 10px;" reset="false" id="${property.model.modelName}Lower" checked="true" name="${property.model.modelName}Down" value="yes" /><label class="cui-font-label"><@s.text name="${getText('foundation.staff.havingDownPosition')}"/></label></div>
			<#else>
				<@mneclient name="${key}" isCrossCompany=isCrossCompany onkeyupfuncname="${formId}_${newcode}_keyUpCallback(obj)" showRange="${showRange!}" 
			     formId="${formId}" id="${formId}_${newcode}_${property.name}" url="${mneurl}" clicked=mneclick iframe=true type="${type}" multiple=false isPrecise=isPrecise searchClick="${searchClick!}" deploymentId="${deploymentId}" selectPeople="${selectPeople}" sourceStaff="${sourceStaff}" fieldCode="${fieldCode}" outcome="${outcome}"/>
			</#if>
			<script type="text/javascript">
			function ${formId}_${newcode}_keyUpCallback(obj){
				if(obj != null && obj != undefined && obj[0].id != null && obj[0].id != undefined){
					$("input[id='<#if size==0>${splitkey[0]}.<#else><#list 0..(size-1) as i>${splitkey[i]}.</#list></#if>id']","#${formId}").val(obj[0].id);
        		}				
			}
			</script>
		</#if>
		<#if refurl!="">
			<#if ishavingDown>
				<div style="float:left;_width:65%;">
				<@selector name="${key}"  cssClass="cui-noborder-input" id="${key}" showRange="${showRange!}"  callBackFuncName="${formId}_${newcode}_selectorCallback" pageType="${type}"openType="dialog" open_url="${refurl}" value="" closePage="true" pageType="other" />
				</div>
				<div style="float:left;"><input type="checkbox"  reset="false" style="border:none;vertical-align: middle; margin: 0 3px 0 10px;"  id="positionLower" name="${property.model.modelName}Down" checked="true" value="yes" /><label class="cui-font-label"><@s.text name="${getText('foundation.staff.havingDownPosition')}"/></label></div>
			<#else>
				<@selector name="${key}"  cssClass="cui-noborder-input" id="${key}" showRange="${showRange!}"  callBackFuncName="${formId}_${newcode}_selectorCallback" pageType="${type}"openType="dialog" open_url="${refurl}" value="" closePage="true" pageType="other" />
			</#if>
		
		<script type="text/javascript">
			function ${formId}_${newcode}_selectorCallback(obj){
				if(obj != null && obj != undefined && obj[0].name != null && obj[0].name != undefined){
					$("input[id='${key}']","${formId}").val(obj[0].name);
        		 }				
			}
			</script>
		</#if>
		
		<#if  mneurl==""&&refurl=="">
		<div class="fix-input"><input id="${key}" name="${key}" type="text" class="cui-noborder-input" /></div>
		</#if>
	</#if>

</#if>
</div>
</#macro>
<#macro advsqueryfield code,divCode="",isCustomize=false,key="",type="other",showFormat="",isTimeSpan=false,mneurl="",refurl="",formId="",defaultValue="",exp="",deploymentId="",mneclick=true,isCrossCompany=false,ishavingDown=false,isPrecise=true,searchClick="",errorWidget="",selectPeople="",fieldCode="",outcome="",sourceStaff="">
<#assign newcode=code?replace('.', '_')>
<#if divCode?? && divCode != "">
<#assign newcode=divCode?replace('.', '_')>
</#if>
<#if quickQuery(code)??>
<#assign property = quickQuery(code)>
</#if>
<#if isCustomize>
	<#nested>
<#else>
	<#if property.type == "DATETIME" || property.type == "DATE">
		<#if isTimeSpan>
			<div style="float:left;width:45%">
			<@datepicker name="${key}_start" id="${key}_start" cssClass="cui-noborder-input" ></@datepicker>
			</div>
			<div style="float:left;margin:1px 3px 0px;*margin-top:3px;">${getHtmlText('foundation.permissionQuery.to')}</div>
			<div style="float:left;width:45%">
			<@datepicker name="${key}_end" id="${key}_end" cssClass="cui-noborder-input" ></@datepicker>
			</div>
		<script type="text/javascript">
			function ${formId}checkTime(){
				if(CUI.trim(CUI("input[name='${key}_start']",CUI("#${formId}")).val()) != "" && CUI.trim(CUI("input[name='${key}_end']",CUI("#${formId}")).val()) != "" && CUI.trim(CUI("input[name='${key}_start']",CUI("#${formId}")).val()) > CUI.trim(CUI("input[name='${key}_end']",CUI("#${formId}")).val())) {
			 		<#if errorWidget!="">
			 		${errorWidget}.show("${getHtmlText('foundation.common.timeCheck')}","f");
			 		<#else>
			 		if(typeof(workbenchErrorBarWidget) != 'undefined'){
			 			workbenchErrorBarWidget.showMessage("${getHtmlText('foundation.common.timeCheck')}","f");
			 		}else {
			 			CUI.Dialog.alert(${getHtmlText("foundation.common.timeCheck")});	
			 		}
			 		</#if>
			 		return false;
	 			}
	 		}
		</script>
		<#else>
			<@datepicker name="${key}" id="${key}" cssClass="cui-noborder-input" ></@datepicker>
		</#if>
	<#elseif property.type=="SYSTEMCODE">
		<@systemcode name="${key}" code="${property.fillcontent}"/>
	<#elseif property.type=="ENUMERATE">
		<select id="${key?replace(".", "")}" name="${key}" class="edit-select ">
		<option value=""></option>
		<#assign fillcontentMap=property.fillcontentMap>
		<#assign maps=fillcontentMap["fillContent"]>
		<#list maps?keys as key>
			<option value="${key}">${maps[key]}</option>
		</#list>
		</select>
	<#else>
		<#if mneurl!="">
			<#assign splitkey=key?split('.')>
			<#assign size=(splitkey?size-1)>
			
			<input type="hidden" id="<#if size==0>${splitkey[0]}.<#else><#list 0..(size-1) as i>${splitkey[i]}.</#list></#if>id" name="<#if size==0>${splitkey[0]}.<#else><#list 0..(size-1) as i>${splitkey[i]}.</#list></#if>id" />
			<#if ishavingDown>
				<div style="float:left;_width:65%;">
				<@mneclient name="${key}" isCrossCompany=isCrossCompany onkeyupfuncname="${formId}_${newcode}_keyUpCallback(obj)"
				     formId="${formId}" id="${formId}_${newcode}_${property.name}" url="${mneurl}" clicked=mneclick iframe=true type="${type}" multiple=false isPrecise=isPrecise searchClick="${searchClick!}" deploymentId="${deploymentId}" selectPeople="${selectPeople}" sourceStaff="${sourceStaff}" fieldCode="${fieldCode}" outcome="${outcome}" cssStyle="width:147px;"/>
				</div>
				<div style="float:left;margin-top:5px"><input type="checkbox" style="border:none;vertical-align: middle; margin: 0 3px 0 10px;" reset="false" id="${property.model.modelName}Lower" checked="true" name="${property.model.modelName}Down" value="yes" /><label class="cui-font-label"><@s.text name="${getText('foundation.staff.havingDownPosition')}"/></label></div>
			<#else>
				<@mneclient name="${key}" isCrossCompany=isCrossCompany onkeyupfuncname="${formId}_${newcode}_keyUpCallback(obj)"
			     formId="${formId}" id="${formId}_${newcode}_${property.name}" url="${mneurl}" clicked=mneclick iframe=true type="${type}" multiple=false isPrecise=isPrecise searchClick="${searchClick!}" deploymentId="${deploymentId}" selectPeople="${selectPeople}" sourceStaff="${sourceStaff}" fieldCode="${fieldCode}" outcome="${outcome}"/>
			</#if>
			<script type="text/javascript">
			function ${formId}_${newcode}_keyUpCallback(obj){
				if(obj != null && obj != undefined && obj[0].id != null && obj[0].id != undefined){
					$("input[id='<#if size==0>${splitkey[0]}.<#else><#list 0..(size-1) as i>${splitkey[i]}.</#list></#if>id']","#${formId}").val(obj[0].id);
        		}				
			}
			</script>
		</#if>
		<#if refurl!="">
			<#if ishavingDown>
				<div style="float:left;_width:65%;">
				<@selector name="${key}"  cssClass="cui-noborder-input" id="${key}" callBackFuncName="${formId}_${newcode}_selectorCallback" pageType="${type}"openType="dialog" open_url="${refurl}" value="" closePage="true" pageType="other" />
				</div>
				<div style="float:left;"><input type="checkbox"  reset="false" style="border:none;vertical-align: middle; margin: 0 3px 0 10px;"  id="positionLower" name="${property.model.modelName}Down" checked="true" value="yes" /><label class="cui-font-label"><@s.text name="${getText('foundation.staff.havingDownPosition')}"/></label></div>
			<#else>
				<@selector name="${key}"  cssClass="cui-noborder-input" id="${key}" callBackFuncName="${formId}_${newcode}_selectorCallback" pageType="${type}"openType="dialog" open_url="${refurl}" value="" closePage="true" pageType="other" />
			</#if>
		
		<script type="text/javascript">
			function ${formId}_${newcode}_selectorCallback(obj){
				if(obj != null && obj != undefined && obj[0].name != null && obj[0].name != undefined){
					$("input[id='${key}']","${formId}").val(obj[0].name);
        		 }				
			}
			</script>
		</#if>
		
		<#if  mneurl==""&&refurl=="">
		<div class="fix-input"><input id="${key}" name="${key}" type="text" class="cui-noborder-input" /></div>
		</#if>
	</#if>

</#if>
</#macro>
