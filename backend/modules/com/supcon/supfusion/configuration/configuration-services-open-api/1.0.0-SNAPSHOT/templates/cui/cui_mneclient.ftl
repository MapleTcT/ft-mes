<#macro mneclient id,name,url,showRange="",display_key="",mneTip="${getText('foundation.common.Tip')}",type="",property_type="",viewType="create",deValue="",clicked=false,searchClick="",clickedClass="cui-search-click",formId="",mnewidth=260,isEdit=false,multiple=false,isWrap=true,isCrossCompany=false,clickShowHeight=180,clickShowWidth=260,clickShowTitle="${getText('cui.mneclient.clickShowTitle')}",multiDivStyle="height:26px;overflow:hidden;",multiDivClass="",classStyle="cui-noborder-input",cssStyle="",onBeforeClick="",displayFieldName="name",ids="",names="",exp="",onkeyupfuncname="",editCustomCallback="",beforecallback="",delCustomCallback="",conditionfunc="",funcparam="",title="",reftitle="",value="",tabindex="",href="",disabled=false,iframe=false,readOnly=false,view=false,onclick="",onblur="",displayType="mne",isPrecise=false,selectionRange="",fieldCode="", deploymentId="",outcome="",selectPeople="",mneenable=true,allowView=false,allowViewFunc="",editLinkCallBack="",sourceStaff="",useDefaultVal=false,onchange="",onselect="",refViewCode="",currentViewCode="",realPermissionCode="",advresume="",caseSensitive=false,assPropertyName="",onBeforeClear="",onAfterClear="",onBeforeSet="",onAfterSet="",specialNoCrossCompany=false,json=false>
<#--onchange,onselect参数先加上，避免实体配置页面配置了onchange与onselect时报错-->
	<#assign addIds = ''>
	<#assign existsIds = ids>
	<#assign existsNames = names>
	<#if viewType = "create" && name?ends_with('.supervision') && !(ids?has_content)>
		<#assign defaultSvs = defaultSupervisions!>
		<#if defaultSvs??>
		<#list defaultSvs as item>
			<#if item_index gt 0>
				<#assign existsIds = existsIds + ",">
				<#assign existsNames = existsNames + ",">
			</#if>
			<#assign addIds = addIds + item.id + ",">
			<#assign existsIds = existsIds + item.id>
			<#assign existsNames = existsNames + item.name>
		</#list>
		<#assign addIds = addIds + ",">
		</#if>
	</#if>
	<#-- 单选 -->
	<#if !multiple>
	<#-- 编辑 -->
	<#if !view>
	
	<div class="fix-input">
	<div class="fix-search-click" id="${id}div" onmouseenter="select_${id?replace('.','_')}_clear();" onmouseleave="select_${id?replace('.','_')}_deleteImage();">
	<#assign originalValue = ''>
	<#assign valueid = ''>
	<#if advresume!="">
		<#if viewType=='create'>
			<#if value?? && value?has_content>
				<#assign originalValue = value>
			<#else>
				<#if deValue=='currentUser'>
					<#assign originalValue = getCurrent('staffName')>
					<#assign valueid = getCurrent('staffId')>
				<#elseif deValue=='currentPost'>
					<#assign originalValue = getCurrent('positionName')>
					<#assign valueid = getCurrent('positionId')>
				<#elseif deValue=='currentDepart'>
					<#assign originalValue = getCurrent('departmentName')>
					<#assign valueid = getCurrent('departmentId')>
				<#elseif deValue=='currentComp'>
					<#assign originalValue = getCurrent('companyName')>
					<#assign valueid = getCurrent('companyId')>
				</#if>
			</#if>
		<#elseif viewType=='edit' || viewType=='readonly'>
			<#assign originalValue = value>
		</#if>
	<#elseif viewType=='create'>
		<#if value?? && value?has_content>
			<#assign originalValue = value>
		</#if>
	<#elseif viewType=='edit' || viewType=='readonly'>
		<#assign originalValue = value>
	</#if>
	<input type="text" <#if mneenable?? && mneenable>mneType="mnemonic"</#if> multiable="false" class="${classStyle}"<#rt/>
	 name="${name?html}"<#rt/>
	 property_type="${property_type}"<#rt/>
	 <#if isEdit>onkeydown="if(event.keyCode==13) return false" isEdit='true' valuebak="${value?html}"<#rt/></#if>
	 <#if isPrecise>isPrecise='true'<#rt/>
 	 <#assign idIndex = name?last_index_of('.')>
 	 <#if idIndex!=-1>
 	 <#assign idStr = name?substring(0,idIndex)>
 	 <#else>
 	 <#assign idStr = name>
 	 </#if>
	 onpropertychange="CUI.iePreciseClear('${idStr!}','${formId}')"<#rt/>
	 </#if>
	<#if disabled>
	 disabled="disabled"<#rt/>
	</#if>
	<#if deValue!=''>
	deValue="${deValue!}"<#rt/>
	</#if>
	<#if formId != ''>
	formId="${formId}"<#rt/>
	</#if>
	<#if (!mneenable && isEdit) || readOnly>
	 readonly="true"<#rt/>
	</#if>
	<#if tabindex??>
	 tabindex="${tabindex?html}"<#rt/>
	</#if>
	<#if href != "">
	 href="${href}"<#rt/>
	</#if>
	 id="${name?replace('.', '_')}"<#rt/>
	 fieldCodeMne="${id}"<#rt/>
	<#if value??>
	<#if advresume!="">
	value="${originalValue?html}"
	<#else>
	value="${value?html}"
	</#if>
	originalValue="${originalValue?html}"<#rt/>
	</#if>
	<#if cssStyle??>
	 style="${cssStyle?html}"<#rt/>
	</#if>
	<#if title??>
	 title="${title?html}"<#rt/>
	</#if>
	<#if mneenable>
	 onfocus="CUI.clearMneTips('${name?replace('.', '_')}_mneTipLabel','${formId}')"<#rt/>
	</#if>
	<#if onclick??>
	 onclick="<#if mneenable>CUI.clearMneTips('${name?replace('.', '_')}_mneTipLabel','${formId}');</#if>${onclick?html}"<#rt/>
	</#if>
	<#if searchClick??>
	 searchClick="${searchClick?html}"<#rt/>
	</#if>
	<#if exp??>
	 exp="${exp?html}"<#rt/>
	</#if>
	<#if caseSensitive??>
	 caseSensitive="${caseSensitive?string}"<#rt/>
	</#if>
	<#if displayType=='select'>
	 displayType="${displayType?html}"<#rt/>
	<#else>
		<#if mneenable>
	 onblur="var that=this;setTimeout(function(){if(!window.mnePageBtnFlag){<#if onblur??>${onblur?html};</#if>cleanMneDiv(that,0,'${formId}');CUI.restoreMneTips(that,'${name?replace('.', '_')}_mneTipLabel','${formId}');}},200);"<#rt/>
		</#if>
	</#if>
	isCrossCompany="${isCrossCompany?string}" <#if specialNoCrossCompany>specialNoCrossCompany='true'</#if>  refViewCode="${refViewCode!}"   currentViewCode="${currentViewCode!}"   realPermissionCode="${realPermissionCode!}"   <#rt/>
	<#if mneenable>
	onkeyup="selectKeyEvent(event,this,'${url}','${displayFieldName}','<#if type?? && type != "">${type}<#else>other</#if>','${multiple?string}','${mnewidth}','<#if onkeyupfuncname??>${onkeyupfuncname}</#if>','<#if conditionfunc??>${conditionfunc?replace('\'', '\\\'')}</#if>','<#if onBeforeClick?has_content>${onBeforeClick}</#if>',null,null,'${showRange!}')"<#rt/>
	</#if>
	 autocomplete=off />
	 <#if displayType=='select'>
	<input type="button" class="cui-base-select-click" value="&nbsp;"  deValue="${deValue!}" 
		onclick="$(this).prev().focus();selectKeyEvent(event,$(this).prevAll( 'input[mneType=mnemonic]' )[0],'${url}','${displayFieldName}','<#if type?? && type != "">${type}<#else>other</#if>','${multiple?string}','${mnewidth}','<#if onkeyupfuncname??>${onkeyupfuncname}</#if>','<#if conditionfunc??>${conditionfunc?replace('\'', '\\\'')}</#if>','<#if onBeforeClick?has_content>${onBeforeClick}</#if>', true,null,'${showRange!}')"<#rt/>
	></input>
	</#if>
	<input type="hidden" reset="false" id="${id}FieldCode"  value="${fieldCode}"/>
	<input type="hidden" reset="false" id="${id}DeploymentId"  value="${deploymentId}"/>
	<input type="hidden" reset="false" id="${id}Outcome"  value="${outcome}"/>
	<input type="hidden" reset="false" id="${id}SelectPeople"  value="${selectPeople}"/>
	<input type="hidden" reset="false" id="${id}SourceStaff"  value="${sourceStaff}"/>
	<#if displayType != 'select' && mneenable>
	<label id="${name?replace('.', '_')}_mneTipLabel" <#if (value?? && value != "") || (deValue?? && deValue != "")>style="display:none;text-align:left;"<#else>style="text-align:left;"</#if> onclick="CUI.clearMneTips('${name?replace('.', '_')}_mneTipLabel','${formId}');$(this).siblings('input[id=${name?replace('.', '_')}]').focus();" class="mne-tip search-ft-color">${mneTip}</label>
	</#if>
	<input id="${id}_click_button" formne="${name?replace('.', '_')}" style="display:<#if displayType=='select'>none<#elseif clicked>block<#else>none</#if>;" type="button" deValue="${deValue!}"  class="${clickedClass}" value="&nbsp;" onclick="${id}_selectEvent('${name}','<#if type?? && type != "">${type?uncap_first}<#else>other</#if>','${url}','<#if reftitle?? && reftitle != "">${reftitle}<#else>${getText('foundation.common.mneClient')}</#if>','${funcparam}','<#if onBeforeClick?has_content>${onBeforeClick}</#if>','${showRange!}')"></input>
	</div></div>
	<script type="text/javascript">

		$(function(){
			$("#${id}div").bind('mouseenter',function(){
			  select_${id?replace('.','_')}_clear();
			});
			$("#${id}div").bind('mouseleave',function(){
			  select_${id?replace('.','_')}_deleteImage();
			});
			<#if !(isEdit?? && isEdit)>
			setTimeout(function(){
				var objName = "${name?html}";
				var arr = objName.split('.');
				var ${id}_prefix = '', ${id}_suffix = '';
				for(var i=0;i<arr.length-1;i++) {
					if(i!=0) {
						${id}_prefix += '.';
					}
					${id}_prefix += arr[i];
				}
				${id}_suffix = arr[arr.length -1];
				CUI('#${formId} input[name="'+${id}_prefix + '."]').unbind("change");
				CUI('#${formId} input[name="'+${id}_prefix + '.' + ${id}_suffix +'"]').bind("change",function(){
					CUI('#${formId} input[name="'+${id}_prefix + '.id"]').val('');
					<#if assPropertyName??&&assPropertyName!="">
						CUI('#${formId} input[name="'+${id}_prefix + '.${assPropertyName}"]').val('');
					</#if>
				});
			},500)
			</#if>
			
		});
		$(function(){
		if("${advresume!}"!=""){
		$("[id='${advresume}']").val("${valueid}");
		}
		});
		
	var ${id}_dialog;
	var ${id}_prefix='',${id}_suffix='';
	function ${id}_selectEvent(objName,type,url,title,refparam,onBeforeClick,showRange) {
		try {
			var beforeResult = true;
			if(onBeforeClick && onBeforeClick.length > 0) {
				eval("beforeResult=" + onBeforeClick);
			}
			if(beforeResult == false) {
				return false;
			}
		} catch (e) {
		}
		var arr = objName.split('.');
		${id}_prefix = '',
		${id}_suffix = '';
		${id}_sUrl = '';
		for(var i=0;i<arr.length-1;i++) {
			if(i!=0) {
				${id}_prefix += '.';
			}
			${id}_prefix += arr[i];
		}
		${id}_suffix = arr[arr.length -1];
		<#if conditionfunc?? && conditionfunc?has_content>
		var conditionfunc = "${conditionfunc}";
		if(eval("typeof("+conditionfunc.substring(0,conditionfunc.indexOf("("))+")!='undefined'")) {
			var conditionfuncStr = eval(conditionfunc)==null ? "" : eval(conditionfunc);
			if(refparam != null && refparam != "") {
				refparam += '&';
			} else {
			    refparam = '';
			}
			refparam += "condition=" +  encodeURIComponent(conditionfuncStr);
		}
		</#if>
		
		if(refparam != null && refparam.length > 0) {
			refparam += "&crossCompanyFlag=<#if isCrossCompany?? && isCrossCompany?string=='true'>true<#else>false</#if>";
		} else {
			refparam = "&crossCompanyFlag=<#if isCrossCompany?? && isCrossCompany?string=='true'>true<#else>false</#if>";
		}
		<#if multiple??>
		refparam+="&multiSelect=${multiple?string}";
		</#if>
				
		if(type=="userRange"){
		
		<#if outcome?? && outcome?has_content>
			var outCome = "${outcome}";
		<#else >
			var outCome = $('input[name="workFlowVar.outcome"]:checked').val();
		</#if>
			
			$('#${id}Outcome').val(outCome);
			refparam += "&outcome=" + outCome;
			refparam += "&deploymentId=${deploymentId}"
			<#if selectPeople??>
			refparam += "&selectPeople=${selectPeople}"
			</#if>
		}
		<#if type == 'MenuInfo' || type == 'UserMenuInfo'>
		<#if type == 'MenuInfo'>
			var open_url= "/foundation/menuInfo/common/menuInfoTreeFrame?callBackFuncName=_callback_${id!}&closePage=true";
		<#else>
			var open_url= "/foundation/menuInfo/userpermission/menuInfoTreeFrame?callBackFuncName=_callback_${id!}&closePage=true";
		</#if>
		var window_height = 550;
		var window_width  = 280;
		var ShowStyle = "width = " + window_width + ",height=" + window_height + ",scrollbars=yes,resizable =yes,toolbar=no,menubar=no,location=no,status=yes";
		window.open(open_url,"",ShowStyle);
		<#else>
		${id}_dialog = foundation.common.select({
			pageType : type,
			closePage : true,
			
			<#if iframe>
			iframe : '${id}',
			dialogName : '${id}' + objName,
			</#if>
			
			callBackFuncName : '_callback_${id!}',
			url : url,
			title : title,
			params : refparam,
			showRange:"${showRange!}"
		});
		</#if>
	}
	
	function _callback_${id!}(obj) {
		<#if beforecallback?? && beforecallback != "">
			var flag = ${beforecallback};
			if(flag === false){
				return flag;
			}
		</#if>
		<#if isEdit?? && isEdit>
		var arr = '${name}'.split('.');
		obj[0] = foundation.common.getObject('${formId}',obj[0], arr[0]+'.'+arr[1], '${url}');
		if(null != obj[0].id && obj[0].id != ""){
			CUI.commonFills('${formId}',(arr.length == 2) ? arr[0] : arr[0]+'.'+arr[1],obj[0]);
			//自定义字段回填
			CUI.commonFills_CP(obj[0].id,'${url}');
		} else {
			CUI.clearInput('${name}');
		}
		<#--如果有自定义回调函数，执行以上内容后，再执行自定义的函数-->
		<#if editCustomCallback?? && editCustomCallback != "">
			${editCustomCallback}
		</#if>

		// 编辑页面中，当有值输入时，触发页面的resize事件，重新计算高度，避免换行时，页面变形
		try{
			$('body').trigger('resize');
			$('body').trigger('dialog.resize');
		} catch(e) {
			if(${(isDev!false)?string}) {throw e;}
		}
		<#else>
		CUI('#${formId} input[name="'+${id}_prefix + '.' + ${id}_suffix +'"]').val(obj[0][${id}_suffix]);
		CUI('#${formId} input[name="'+${id}_prefix + '."]').unbind("change");
		CUI('#${formId} input[name="'+${id}_prefix + '.' + ${id}_suffix +'"]').bind("change",function(){
			CUI('#${formId} input[name="'+${id}_prefix + '.id"]').val('');
			<#if assPropertyName??&&assPropertyName!="">
			CUI('#${formId} input[name="'+${id}_prefix + '.${assPropertyName}"]').val('');
			</#if>
		});
		
		CUI('#${formId} input[name="'+${id}_prefix + '.id"]').val(obj[0].id);
		<#if assPropertyName??&&assPropertyName!="">
		CUI('#${formId} input[name="'+${id}_prefix + '.${assPropertyName}"]').val(obj[0].${assPropertyName});
		</#if>
		</#if>
		if(${id}_dialog) {
			${id}_dialog.close();
		}
		<#if mneenable>
		CUI.clearMneTips('${name?replace('.', '_')}_mneTipLabel','${formId}');
		</#if>
		
		<#if editLinkCallBack?? && editLinkCallBack != "">
			eval("${editLinkCallBack}(obj)");
		</#if>
	}
	function ${id}_delete(){
	  <#if isEdit?? && isEdit>
	  	//组织删除的信息
		var deleteObj = {"id" :  CUI('#${formId} input[name="${name?replace('_', '.')?replace('.name', '.id')}"]').val() ,"name" : CUI("#${name?replace('.', '_')}").val() };
	  	<#if (onBeforeClear?length>0)>
		if (typeof ${onBeforeClear?replace('()', '')} === 'function'){
			if (false === ${onBeforeClear?replace('()', '(deleteObj)')}) return;
		}
		</#if>
	   ${id}_prefix = '',
	   CUI.clearInput("${name?html}");
	   CUI("#${formId} #${name?replace('.', '_')}").attr('valuebak','');
	   CUI('#${formId} input[name="'+${id}_prefix + '.id"]').attr('valuebak','');
	   <#if mneenable>
	   CUI.restoreMneTips($("#${formId} #${name?replace('.', '_')}"),'${name?replace('.', '_')}_mneTipLabel','${formId}');
	   </#if>
	   <#if delCustomCallback?? && delCustomCallback != "">
	   ${delCustomCallback}
	   </#if>
	   <#if (onAfterClear?length>0)>
		typeof ${onAfterClear?replace('()', '')} === 'function' && ${onAfterClear?replace('()', '(deleteObj)')}
		</#if>
	 </#if>
	}
	
	select_${id?replace('.','_')}_clear = function(){
	<#if isEdit?? && isEdit && displayType!='select'>
		var length=$.trim($("#${formId} #${name?replace('.', '_')}").val()).length;
		if(length>0 && $('#${id}Image').length == 0){ // 如果已经有清除图标，不要重复添加
			var newSpan = $("<input  type='button' class='cui-edit-clear' id='${id}Image' onclick='${id}_delete();'/>");
			$(newSpan).insertAfter($("#${formId} #${name?replace('.', '_')}"));
		}
	</#if>
	};
	select_${id?replace('.','_')}_deleteImage = function(){
	<#if isEdit?? && isEdit && displayType!='select'>
		$(YUD.get("${id}Image")).remove();
	</#if>
	};
	</script>
	<#-- 查看 -->
	<#else>
	<div class="fix-input-readonly">
	<input name="${name}" property_type="${property_type}" class="${classStyle}" readonly="true" <#rt/>
	<#if cssStyle??>
	 style="${cssStyle}"<#rt/>
	</#if>
	<#if value??>
	 value="${value}"<#rt/>
	</#if>
	<#include "/template/simple/dynamic-attributes.ftl" />
	/></div>
	</#if>
	<#-- 多选  -->
	<#else>
	<div class="fix-input<#if view>-readonly</#if>">
	<div id="${id}MultiIDsContainerDiv" <#if view>class="multi-container-readonly ${multiDivClass}" style="height:auto!important;min-height:26px;"<#else>class="multi-container ${multiDivClass}" style="${multiDivStyle};height:26px;"</#if> >
	<input type="hidden" id="${id}MultiIDs" name="${name}MultiIDs" value="${existsIds!}<#if existsIds != ''>,</#if>"/>
	<input type="hidden" id="${id}DeleteIds" name="${name}DeleteIds" value="" originalvalue=""/>
	<input type="hidden" id="${id}AddIds" name="${name}AddIds" value="${addIds}" originalvalue="${addIds}"/>
	<input type="hidden" reset="false" id="${id}FieldCode"  value="${fieldCode}"/>
	<input type="hidden" reset="false" id="${id}DeploymentId"  value="${deploymentId}"/>
	<input type="hidden" reset="false" id="${id}Outcome"  value="${outcome}"/>
	<input type="hidden" reset="false" id="${id}SelectPeople"  value="${selectPeople}"/>
	<input type="hidden" reset="false" id="${id}SourceStaff"  value="${sourceStaff}"/>
	<#if isWrap>
	<#if !view>
	<span style="float:left;" id="${id}MultiSelect">
		<#if mneenable>
		<span style="margin-right:2px;position: relative;">
			<input<#if json> json="true"</#if> display_key="${display_key}" id="${id}MneInput" name="${name?html}" multiable="true" <#if formId != ''>formId="${formId}"</#if> onfocus="CUI.clearMneTips('${name?replace('.', '_')}_mneTipLabel','${formId}')" onclick="CUI.clearMneTips('${name?replace('.', '_')}_mneTipLabel','${formId}');<#if isWrap>CUI.focusMneInput('${id}');</#if>" value="${value!''}" isCrossCompany="${isCrossCompany?string}"  <#if specialNoCrossCompany>specialNoCrossCompany='true'</#if>  refViewCode="${refViewCode!}"  currentViewCode="${currentViewCode!}"  realPermissionCode="${realPermissionCode!}"   <#if isEdit>onkeydown="if(event.keyCode==13) return false" isEdit='true'</#if>  autocomplete=off style="border:0;width:95px;"<#rt/>
			onkeyup="selectKeyEvent(event,this,'${url}','${displayFieldName}','<#if type?? && type != "">${type}<#else>other</#if>','${multiple?string}','${mnewidth}','<#if onkeyupfuncname??>${onkeyupfuncname}</#if>','<#if conditionfunc??>${conditionfunc?replace('\'', '\\\'')}</#if>','',null,null,'${showRange!}')"<#rt/>
			onblur="var that=this;setTimeout(function(){if(!window.mnePageBtnFlag){cleanMneDiv(that,0,'${formId}');CUI.restoreMneTips(that,'${name?replace('.', '_')}_mneTipLabel','${formId}');}},200);"<#rt/>
		/>
		<label id="${name?replace('.', '_')}_mneTipLabel" <#if (value?? && value != "") || (deValue?? && deValue != "")>style="display:none"</#if> onclick="CUI.clearMneTips('${name?replace('.', '_')}_mneTipLabel','${formId}');CUI('#${id}MneInput').focus();" class="mne-tip search-ft-color">${mneTip}</label>
		</span>
		</#if>
	</span>
	</#if>
	</#if>
	<span id="${id}MultiIDsContainer" <#if isWrap>class="mneMultiContainerWrap"</#if> style="<#if mneenable>left:97px;</#if><#if isWrap>height:26px;<#elseif mneenable><#if !view>position:relative;top:-1px;top:-2px \9;*top:-5px;</#if><#else>position:relative;top:5px;</#if>" >
	<#if existsIds != "" && existsNames != "">
	<#assign idArr = existsIds?split(',')>
	<#assign nameArr = existsNames?split(',')>
	<#list idArr as item>
		<#if !view>
			<#if editLinkCallBack?? && editLinkCallBack != "">
				<span class="mne-select-span" style="padding-right:8px;"><span class="edit-table-symbol-span" objId="${item}" onclick='${id}_editLinkCallBack(this)'>${(nameArr[item_index])!}</span><img src="/bap/static/ec/delete.gif" style="cursor:pointer;vertical-align:middle;padding-bottom:3px;" onMouseOver='deleteBtnChange(this)' onMouseOut='deleteBtnChange(this)' mneobjid="${(idArr[item_index])!}"/></span>
			<#else>
				<span class="mne-select-span" style="padding-right:8px;">${(nameArr[item_index])!}<img src="/bap/static/ec/delete.gif" style="cursor:pointer;vertical-align:middle;padding-bottom:3px;" onMouseOver='deleteBtnChange(this)' onMouseOut='deleteBtnChange(this)' mneobjid="${(idArr[item_index])!}"/></span>
			</#if>
		<#else>
			<#if editLinkCallBack?? && editLinkCallBack != "">
				<span class="edit-table-symbol-span" objId="${item}" onclick='${id}_editLinkCallBack(this)'>${(nameArr[item_index])!}</span><#if item_index != idArr?size-1>,</#if>
			<#else>
				${(nameArr[item_index])!}<#if item_index != idArr?size-1>,</#if>
			</#if>
		</#if>
	</#list>
	<#else>
	<#if view>
	<span style='width:auto;display:inline-block;'>&nbsp;</span>
	</#if>
	</#if>
	</span>
	<#if !view>
	<#if !isWrap>
	<span style="position:relative" id="${id}MultiSelect">
		<#if mneenable>
		<span style="position:relative">
			<input id="${id}MneInput" name="${name?html}" multiable="true" <#if formId != ''>formId="${formId}"</#if> onfocus="CUI.clearMneTips('${name?replace('.', '_')}_mneTipLabel','${formId}')" isCrossCompany="${isCrossCompany?string}" <#if specialNoCrossCompany>specialNoCrossCompany='true'</#if>  refViewCode="${refViewCode!}" currentViewCode="${currentViewCode!}"  realPermissionCode="${realPermissionCode!}"    <#if isEdit>onkeydown="if(event.keyCode==13) return false" isEdit='true'</#if> autocomplete=off style="border:0;width:95px;"<#rt/>
			onkeyup="selectKeyEvent(event,this,'${url}','${displayFieldName}','<#if type?? && type != "">${type}<#else>other</#if>','${multiple?string}','${mnewidth}','<#if onkeyupfuncname??>${onkeyupfuncname}</#if>','<#if conditionfunc??>${conditionfunc?replace('\'', '\\\'')}</#if>','',null,null,'${showRange!}')"<#rt/>
			onblur="var that=this;setTimeout(function(){if(!window.mnePageBtnFlag){cleanMneDiv(that,0,'${formId}');CUI.restoreMneTips(that,'${name?replace('.', '_')}_mneTipLabel','${formId}');}},200);"<#rt/>
		/>
		<label id="${name?replace('.', '_')}_mneTipLabel" <#if (value?? && value != "") || (deValue?? && deValue != "")>style="display:none"</#if> onclick="CUI.clearMneTips('${name?replace('.', '_')}_mneTipLabel','${formId}');CUI('#${id}MneInput').focus();" class="mne-tip search-ft-color">${mneTip}</label>
		</span>
		</#if>
	</span>
	</#if>
	<#if clicked> 
	<#if isWrap>
	<span id="${id}ellipsisBtn" class="cui-ellipsis-span">......</span>
	</#if>
	<input type="button" formne="$${name?replace('.', '_')}" <#if isWrap>style="background-color:#FFF"</#if> class="${clickedClass}"></input>
	</#if>
	</div>
	</div>
	<script type="text/javascript">
		
		<#if existsIds != '' && useDefaultVal>
		$(function(){
			$("#${id}AddIds").val($("#${id}MultiIDs").val());
		});
		</#if>
		
		//多选删除单个的函数
		function delete${id}Multi(imgObj, noCallback) {
			//组织删除的信息
			var deleteObj = {"id" : imgObj.getAttribute("mneobjid") ,"name" : $(imgObj).parent().text() };
			<#if (onBeforeClear?length>0)>
			if (typeof ${onBeforeClear?replace('()', '')} === 'function'){
				if (false === ${onBeforeClear?replace('()', '(deleteObj)')}) return;
			}
			</#if>
			var id = imgObj.getAttribute("mneobjid");
			var addInupt = CUI("#${id}AddIds");
			var addIDs = addInupt.val();
			var delInupt = CUI("#${id}DeleteIds");	
			var delIDs = delInupt.val();
			// 当前所有id
			var allmnemultiselectids = CUI("#${id}MultiIDs").val() || '';
			var re = new RegExp('(.*,|^^)('+ id +',)(.*)'); 
			// 判断是否为新增数据
			if(addIDs.indexOf(',' + id + ',') != -1 || addIDs.indexOf(id + ',') == 0){
				addIDs = addIDs.replace(re,'$1$3');
				addInupt.val(addIDs);
				// 设置隐藏字段值
			}else{
				// 增加
				delIDs += (id + ',');
				delInupt.val(delIDs);
				// 设置隐藏字段值
			}
			$(imgObj).parent().remove();
			// 更新当前所有id
			allmnemultiselectids = allmnemultiselectids.replace(re,'$1$3');
			CUI("#${id}MultiIDs").val(allmnemultiselectids);
			<#if isWrap>
			${id}CalWidth();
			var size = $('span', "#${id}ShowSpanContainer").length;
			$('span', ${id}ClickShow._header).html(size);
			</#if>
			<#if delCustomCallback?? && delCustomCallback != "">
			${delCustomCallback}
			</#if>
			if( $('#${id}MultiIDsContainerDiv').height() == 28 && ( $.browser.msie7 || $.browser.msie6 ) ){
				$("#${id}MultiIDsContainer").css( 'top', '-5px' );
			}
			<#if (onAfterClear?length>0)>
			typeof ${onAfterClear?replace('()', '')} === 'function' && ${onAfterClear?replace('()', '(deleteObj)')}
			</#if>
		};
		
		
		//多选回调函数默认的对象MneObj，默认的隐藏IDs名称为MultiIDs，存放人名显示的span名称为MultiIDsContainer
		function get${id}MultiInfo() {
			if (MneObj == null || MneObj == undefined || MneObj.id == null || MneObj.id == undefined) {
				return false;
			}
			<#if (onBeforeSet?length>0)>
			if (typeof ${onBeforeSet?replace('()', '')} === 'function'){
				if (false === ${onBeforeSet?replace('()', '(MneObj)')}) return;
			}
			</#if>
			// 当前所有id
			var allmnemultiselectids = CUI("#${id}MultiIDs").val() || '';
			// 判断是否已经存在
			if(allmnemultiselectids.indexOf(',' + MneObj.id + ',') != -1 || allmnemultiselectids.indexOf(MneObj.id + ',') == 0){
				return;
			}
			var addInupt = CUI("#${id}AddIds");
			var addIDs = addInupt.val();
			var delInupt = CUI("#${id}DeleteIds");	
			var delIDs = delInupt.val();
			// 原有数据被删除
			if(delIDs.indexOf(',' + MneObj.id + ',') != -1 || delIDs.indexOf(MneObj.id + ',') == 0){
				var re = new RegExp('(.*,|^^)('+ MneObj.id +',)(.*)'); 
				delIDs = delIDs.replace(re,'$1$3');
				delInupt.val(delIDs);
			}else{
				// 增加
				addIDs += (MneObj.id + ',');
				addInupt.val(addIDs);
			}
			// 更新当前所有id
			allmnemultiselectids += (MneObj.id + ',');
			CUI("#${id}MultiIDs").val(allmnemultiselectids);
			<#if editLinkCallBack?? && editLinkCallBack != "">
			var newSpan = $("<span class='mne-select-span'><span class='edit-table-symbol-span' objId='" + MneObj.id + "' onclick='${id}_editLinkCallBack(this)'>"+MneObj.${displayFieldName}+"</span><img src='/bap/static/ec/delete.gif' class='multi-mne-img' onMouseOver='deleteBtnChange(this)' onMouseOut='deleteBtnChange(this)' mneobjid='"+MneObj.id+"'/></span>");
			<#else>
			var newSpan = $("<span class='mne-select-span'>"+MneObj.${displayFieldName}+"<img src='/bap/static/ec/delete.gif' class='multi-mne-img' onMouseOver='deleteBtnChange(this)' onMouseOut='deleteBtnChange(this)' mneobjid='"+MneObj.id+"'/></span>");
			</#if>
			CUI("#${id}MultiIDsContainer").append(newSpan);
			$('img[mneobjid="'+MneObj.id+'"]',$('#${id}MultiIDsContainer')).click(function(e){
				stopBubble(e);
            	delete${id}Multi(this);
			});
			<#if isWrap>
			${id}CalWidth();
			</#if>
			if( $('#${id}MultiIDsContainerDiv').height() > 28 && ( $.browser.msie7 || $.browser.msie6 ) ){
				$("#${id}MultiIDsContainer").css( 'top', 0 );
			}
			<#if (onAfterSet?length>0)>
			if (typeof ${onAfterSet?replace('()', '')} === 'function'){
				${onAfterSet?replace('()', '(MneObj)')};
			}
			</#if>
		}
		
		<#if clicked>
		var ${id}_dialog;
		
		function ${id}_selectEvent(type,url,title,refparam,showRange){
			<#if conditionfunc?? && conditionfunc?has_content>
			var conditionfunc = "${conditionfunc}";
			if(eval("typeof("+conditionfunc.substring(0,conditionfunc.indexOf("("))+")!='undefined'")) {
				var conditionfuncStr = eval(conditionfunc)==null ? "" : eval(conditionfunc);
				if(refparam != null && refparam.length > 0) {
					refparam += "&";
				} else {
					refparam = "";
				}
				refparam += "condition=" +  encodeURIComponent(conditionfuncStr);
			}
			</#if>
			<#if isCrossCompany??&&isCrossCompany?string=='true'>
			refparam+="&crossCompanyFlag=true";
			</#if>
			<#if multiple??>
			refparam+="&multiSelect=${multiple?string}";
			</#if>
		
			if(type=="userRange"){
				<#if outcome?? && outcome?has_content>
					var outCome = "${outcome}";
				<#else >
					var outCome = $('input[name="workFlowVar.outcome"]:checked').val();
				</#if>
				$('#${id}Outcome').val(outCome);
				refparam += "&outcome=" + outCome;
				refparam += "&deploymentId=${deploymentId}";
				<#if selectPeople??>
				refparam += "&selectPeople=${selectPeople}"
				</#if>
			}
			if(type=="staffRange"){
				<#if selectPeople??>
				refparam += "&selectPeople=${selectPeople}"
				</#if>
			}
			${id}_dialog = foundation.common.select({
				pageType : type,
				closePage : false,
				
				<#if iframe>
				iframe : '${id}',
				dialogName : '${id}' + '${name}',
				</#if>
				
				callBackFuncName : '_callback_${id!}',
				url : url,
				title : title,
				params : refparam,
				showRange:"${showRange!}"
			});
		}
	
		function _callback_${id!}(objs) {
			if (objs == null || objs == undefined || objs.length <= 0) {
				return false;
			}
			<#if (onBeforeSet?length>0)>
			if (typeof ${onBeforeSet?replace('()', '')} === 'function'){
				if (false === ${onBeforeSet}) return;
			}
			</#if>
			for(var o=0 ; o < objs.length; o++) {
				var id = objs[o].id;
				// 当前所有id
				var allmnemultiselectids = CUI("#${id}MultiIDs").val() || '';
				// 判断是否已经存在
				if(allmnemultiselectids.indexOf(',' + id + ',') != -1 || allmnemultiselectids.indexOf(id + ',') == 0){
					continue;
				}
				var addInupt = CUI("#${id}AddIds");
				var addIDs = addInupt.val();
				var delInupt = CUI("#${id}DeleteIds");	
				var delIDs = delInupt.val();
				// 原有数据被删除
				if(delIDs.indexOf(',' + id + ',') != -1 || delIDs.indexOf(id + ',') == 0){
					var re = new RegExp('(.*,|^^)(' + id + ',)(.*)'); 
					delIDs = delIDs.replace(re,'$1$3');
					delInupt.val(delIDs);
				}else{
					// 增加
					addIDs += (id + ',');
					addInupt.val(addIDs);
				}
				// 更新当前所有id
				allmnemultiselectids += (id + ',');
				CUI("#${id}MultiIDs").val(allmnemultiselectids);
				<#if editLinkCallBack?? && editLinkCallBack != "">
				var newSpan = $("<span class='mne-select-span'><span class='edit-table-symbol-span' objId='" + objs[o].id + "' onclick='${id}_editLinkCallBack(this)'>"+objs[o].${displayFieldName}+"</span><img src='/bap/static/ec/delete.gif' class='multi-mne-img' onMouseOver='deleteBtnChange(this)' onMouseOut='deleteBtnChange(this)' mneobjid='"+id+"'/></span>");
				<#else>
				var newSpan = $("<span class='mne-select-span'>"+objs[o].${displayFieldName}+"<img src='/bap/static/ec/delete.gif' class='multi-mne-img' onMouseOver='deleteBtnChange(this)' onMouseOut='deleteBtnChange(this)' mneobjid='"+id+"'/></span>");
				</#if>
				CUI("#${id}MultiIDsContainer").append(newSpan);
				$('img[mneobjid="'+id+'"]',$('#${id}MultiIDsContainer')).click(function(e){
					stopBubble(e);
		        	delete${id}Multi(this);
				});
			}
			<#if isWrap>
			${id}CalWidth();
			</#if>
			if( $('#${id}MultiIDsContainerDiv').height() > 28 && ( $.browser.msie7 || $.browser.msie6 ) ){
				$("#${id}MultiIDsContainer").css( 'top', 0 );
			}
			<#if (onAfterSet?length>0)>
			if (typeof ${onAfterSet?replace('()', '')} === 'function'){
				${onAfterSet};
			}
			</#if>
		}
	</#if>
	<#if isWrap>
		$( window ).resize( function(){
			${id}CalWidth();
		})
		
		function ${id}CalWidth() {
			// 不可见时不进行计算
			if( CUI("#${id}MultiIDsContainerDiv").is( ':hidden' ) ){
				return;
			}
			var mneInputWidth = <#if mneenable>parseInt(CUI("#${id}MneInput").css('width'),10)<#else>CUI("#${id}MultiIDsContainer").position().left</#if>;
			var trueWidth = parseInt(CUI("#${id}MultiIDsContainerDiv").css('width'),10) - 30;
			var spanWidth = 0;
			$('span.mne-select-span',CUI("#${id}MultiIDsContainer")).each(function(index){
				spanWidth += parseInt($(this).css('width'),10) + 8;
			});
			if(parseInt(spanWidth,10) + mneInputWidth - trueWidth > 0) {
				$('#${id}ellipsisBtn').show();
			} else {
				$('#${id}ellipsisBtn').hide();
			}
		}
		if (typeof ${id}ClickShow === 'undefined' || !$('${id}ellipsisBtn').is(${id}ClickShow.dTargetElement) ) {
			var ${id}ClickShow = new CUI.Clickshow({
				overlayId:'${id}' + Math.round(Math.random()*10000),
				needDrag: true,
				titleText:'${clickShowTitle}（<span>0</span>）',
				needMask:true,
				width:${clickShowWidth},
				height:${clickShowHeight},
				dTargetElement:'${id}ellipsisBtn',
				layBodyContent: '',
				zIndex:400
			});
				
			${id}ClickShow.afterShow.subscribe(function(){
				$(${id}ClickShow._body).empty();
				var showDiv = $('<span id="${id}ShowSpan" style="height:${clickShowHeight-50}px;padding:5px 10px;display:block;overflow:auto"></span>');
				var strContent = CUI("#${id}MultiIDsContainer").clone(true);
				strContent.attr('id','${id}ShowSpanContainer');
				strContent.appendTo(showDiv);
				${id}ClickShow.setContent(showDiv[0]);
				var size = $('span.mne-select-span', "#${id}ShowSpanContainer").length;
				$('span', ${id}ClickShow._header).html(size);
			});
			${id}ClickShow.afterHidden.subscribe(function(){
				$('#${id}MultiIDsContainer').empty();
				var showSpanContainer = CUI("#${id}ShowSpanContainer",'#' + ${id}ClickShow._overlay.id);
				$('span.mne-select-span',showSpanContainer).appendTo($('#${id}MultiIDsContainer'));
				CUI("#${id}ShowSpan",${id}ClickShow._overlay).remove();
				${id}CalWidth();
				
				// 编辑页面有业务中心时  弹出层关闭后会有样式问题
				if ( $("#edit_sidebar").length > 0 ){
					$( 'div.edit-foot' ).hide();
					setTimeout( function(){
						$( 'div.edit-foot' ).show()
					}, 50)
				}
				
			});
		}
		$(function(){
			setTimeout(${id}CalWidth, 0);
		})
	</#if>
	<#if !mneenable>
	$("#${id}MultiIDsContainerDiv").click(function(e){
		if($('span.mne-select-span',$('#${id}MultiIDsContainerDiv')).size() == 0) {
			stopBubble(e);
			${id}_selectEvent('<#if type?? && type != "">${type?uncap_first}<#else>other</#if>','${url}','<#if reftitle?? && reftitle != "">${reftitle}<#else>${getText("foundation.common.mneClient")}</#if>','${funcparam}','${showRange!}');
		}
	});
	</#if>
	$('input[type="button"]',$("#${id}MultiIDsContainerDiv")).click(function(e){
		stopBubble(e);
    	${id}_selectEvent('<#if type?? && type != "">${type?uncap_first}<#else>other</#if>','${url}','<#if reftitle?? && reftitle != "">${reftitle}<#else>${getText("foundation.common.mneClient")}</#if>','${funcparam}','${showRange!}');
    	$("body").trigger("click.select");
	});
	$('img[mneobjid]',$('#${id}MultiIDsContainerDiv')).click(function(e){
		stopBubble(e);
    	delete${id}Multi(this);
	});
	</script>					
	</#if>
	</#if>
	
	<#if editLinkCallBack?? && editLinkCallBack != "">
	<script type="text/javascript">
		function ${id}_editLinkCallBack(obj){
			var id = $(obj).attr("objId");
			eval("${editLinkCallBack}(" + id + ")");
		}
	</script>
	</#if>
</#macro>