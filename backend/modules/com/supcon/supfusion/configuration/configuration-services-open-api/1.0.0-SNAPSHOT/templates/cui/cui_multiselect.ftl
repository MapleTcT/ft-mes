<#macro multiselect id,name,url,isWrap=false,isCrossCompany=false,clickShowHeight=180,clickShowWidth=260,clickShowTitle="${getText('foundation.rolepermission.assignStaff')}",displayFieldName="name",type="",funcparam="",conditionfunc="",title="",ids="",names="",view=false,cssStyle="",cssClass="multi-input",clickedClass="cui-search-click">
	<#assign addIds = ''>
	<#assign existsIds = ids>
	<#assign existsNames = names>
	<#if name?ends_with('.supervision') && !(ids?has_content)>
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
		</#if>
	</#if>


<div id="${id}multiselectContainerDiv" class="<#if view>multi-input-readonly<#else>${cssClass}</#if> fix-search-click" onclick="if($('span',CUI('#${id}multiselectContainer')).size() == 0) {${id}_selectEvent(<#if type??>'${type}'<#else>'other'</#if>,'${url}','${title!getText('foundation.common.mneClient')}','${funcparam}')}" style="${cssStyle}">    
	<input type="hidden" name="${name}MultiIDs" id="${id}MultiIDs" value="${existsIds!}<#if existsIds != ''>,</#if>"/>
	<input type="hidden" id="${id}DeleteIds" name="${name}DeleteIds" value="" originalvalue=""/>
	<input type="hidden" id="${id}AddIds" name="${name}AddIds" value="${addIds}" originalvalue="${addIds}"/>
	<span id="${id}multiselectContainer" <#if isWrap>style="height: 18px;overflow: hidden;display: block;"</#if>>
	<#if existsIds != "" && existsNames != "">
	<#assign idArr = existsIds?split(',')>
	<#assign nameArr = existsNames?split(',')>
	<#list idArr as item>
		<#if !view>
		<span style="padding-left:8px;float:left;">${(nameArr[item_index])!}<img src="/bap/static/ec/delete.gif" style="cursor:pointer;vertical-align:middle;padding-bottom:3px;" onMouseOver='deleteBtnChange(this)' onMouseOut='deleteBtnChange(this)' onclick="${id}deleteObj(this);" objid="${(idArr[item_index])!}"/></span>
		<#else>
		<#if item_index == 0>
		<span style="padding-right:2px;">${(nameArr[item_index])!}</span>
		<#else>
		<span style="padding-right:2px;">,${(nameArr[item_index])!}</span>
		</#if>
		</#if>
	</#list>
	</#if>
	</span>
	<#if !view>
	<#if isWrap>
	<span id="${id}ellipsisBtn" class="cui-ellipsis-span">......</span>
	</#if>
	<input type="button" style="position:absolute;right:2px;top:1px;<#if isWrap>background-color:#FFF</#if>" class="${clickedClass}" />
	</#if>
</div>
<#if !view>
<script type="text/javascript">
	$('input[type="button"]',$("#${id}multiselectContainerDiv")).click(function(e){
		stopBubble(e);
    	${id}_selectEvent(<#if type??>'${type}'<#else>'other'</#if>,'${url}','${title!getText('foundation.common.mneClient')}','${funcparam}');
	});

	var _dialog;
	function ${id}_selectEvent(type,url,title,refparam){
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
		<#if isCrossCompany?? && isCrossCompany>
		if(refparam != null && refparam.length > 0) {
			refparam += "&crossCompanyFlag=true";
		} else {
			refparam = "crossCompanyFlag=true";
		}
		</#if>
		_dialog = foundation.common.select({
			pageType : type,
			closePage : true,
			callBackFuncName : '_callback_${id!''}',
			url : url,
			title : title,
			params : refparam
		});
	}
	
	function _callback_${id!''}(objs) {
		if (objs == undefined || objs.length <= 0) {
			return false;
		}
		for(var o=0 ; o < objs.length; o++) {
			// 当前所有id
			var allmnemultiselectids = CUI("#${id}MultiIDs").val() || '';
			// 判断是否已经存在
			if(allmnemultiselectids.indexOf(',' + objs[o].id + ',') != -1 || allmnemultiselectids.indexOf(objs[o].id + ',') == 0){
				continue;
			}
			var addInupt = CUI("#${id}AddIds");
			var addIDs = addInupt.val();
			var delInupt = CUI("#${id}DeleteIds");	
			var delIDs = delInupt.val();
			// 原有数据被删除
			if(delIDs.indexOf(',' + objs[o].id + ',') != -1 || delIDs.indexOf(objs[o].id + ',') == 0){
				var re = new RegExp('(.*,|^^)('+ objs[o].id +',)(.*)'); 
				delIDs = delIDs.replace(re,'$1$3');
				delInupt.val(delIDs);
			}else{
				// 增加
				addIDs += (objs[o].id + ',');
				addInupt.val(addIDs);
			}
			// 更新当前所有id
			allmnemultiselectids += (objs[o].id + ',');
			CUI("#${id}MultiIDs").val(allmnemultiselectids);
		
			var newSpan = $("<span spanid='" + objs[o].id + "' class='multi-select-span'>"+objs[o].${displayFieldName}+"<img src='/bap/static/ec/delete.gif' class='multi-mne-img' onMouseOver='deleteBtnChange(this)' onMouseOut='deleteBtnChange(this)' objid='"+objs[o].id+"'/></span>");
			CUI("#${id}multiselectContainer").append(newSpan);
			$('img[objid="'+objs[o].id+'"]',$('#${id}multiselectContainer')).click(function(e){
				stopBubble(e);
	        	${id}deleteObj(this);
			});
			// 编辑页面中，当有值输入时，触发页面的resize事件，重新计算高度，避免换行时，页面变形
			$('body').trigger('resize');
			$('body').trigger('dialog.resize');
		}
		<#if isWrap>
			var trueWidth = parseInt(CUI("#${id}multiselectContainerDiv").css('width'),10) - 16;
			var spanWidth = 0;
			$('span',CUI("#${id}multiselectContainer")).each(function(index){
				spanWidth += parseInt($(this).css('width'),10);
			});
			if(parseInt(spanWidth,10) - trueWidth > 0) {
				$('#${id}ellipsisBtn').show();
			}
		</#if>
	}
	
	//多选删除单个的函数
	function ${id}deleteObj(imgObj) {
		var id = imgObj.getAttribute("objid");
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
		}else{
			// 增加
			delIDs += (id + ',');
			delInupt.val(delIDs);
		}
		$(imgObj).parent().remove();
		// 更新当前所有id
		allmnemultiselectids = allmnemultiselectids.replace(re,'$1$3');
		CUI("#${id}MultiIDs").val(allmnemultiselectids);
		if(CUI('span[spanid="'+id+'"]',$('#${id}multiselectContainer'))) {
			CUI('span[spanid="'+id+'"]',$('#${id}multiselectContainer')).remove();
		}
		<#if isWrap>
			var trueWidth = parseInt(CUI("#${id}multiselectContainerDiv").css('width'),10) - 16;
			var spanWidth = 0;
			$('span',CUI("#${id}multiselectContainer")).each(function(index){
				spanWidth += parseInt($(this).css('width'),10);
			});
			if(parseInt(spanWidth,10) - trueWidth > 0) {
				$('#${id}ellipsisBtn').show();
			} else {
				$('#${id}ellipsisBtn').hide();
			}
		</#if>
		
		
	}
	
	
	<#if isWrap>
	
	var ${id}ClickShow = new CUI.Clickshow({
		needDrag: true,
		titleText:'${clickShowTitle}',
		needMask:true,
		width:${clickShowWidth},
		height:${clickShowHeight},
		dTargetElement:'${id}ellipsisBtn',
		layBodyContent: '',
		zIndex:400
	});
	${id}ClickShow.afterShow.subscribe(function(){
		var strContent = CUI("#${id}multiselectContainer").html();
		var strContentDiv = '<div id="${id}ShowDiv" style="height:${clickShowHeight-50}px;padding:5px 10px;overflow:auto">' + strContent + '</div>';
		${id}ClickShow.setContent(strContentDiv);
	});
	</#if>
	
</script>
</#if>
</#macro>
