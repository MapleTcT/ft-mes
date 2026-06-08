<#macro tagselect id,name,url,mneTip="${getText('foundation.common.Tip')}",type="",property_type="",viewType="create",deValue="",clicked=false,searchClick="",clickedClass="cui-search-click",formId="",mnewidth=260,isEdit=false,multiple=false,isWrap=true,isCrossCompany=false,clickShowHeight=180,clickShowWidth=260,clickShowTitle="${getText('cui.mneclient.clickShowTitle')}",multiDivStyle="height:26px;overflow:hidden;",multiDivClass="",classStyle="cui-noborder-input",cssStyle="",onBeforeClick="",displayFieldName="name",ids="",names="",exp="",onkeyupfuncname="",editCustomCallback="",beforecallback="",delCustomCallback="",conditionfunc="",funcparam="",title="",reftitle="",value="",tabindex="",href="",disabled=false,iframe=false,readOnly=false,view=false,onclick="",onblur="",displayType="mne",isPrecise=false,selectionRange="",fieldCode="", deploymentId="",outcome="",selectPeople="",mneenable=true,allowView=false,allowViewFunc="",editLinkCallBack="",sourceStaff="",useDefaultVal=false,onchange="",onselect="",refViewCode="",currentViewCode="",realPermissionCode="",advresume="",caseSensitive=false,assPropertyName="">
<#--onchange,onselect参数先加上，避免实体配置页面配置了onchange与onselect时报错-->
	<#assign addIds = ''>
	<#assign existsIds = ids>
	<#assign existsNames = names>
	<#assign hiddenId = name?replace(".","") >
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
	<#-- 多选  -->
	<#else>
	<div class="fix-input<#if view>-readonly</#if>">
	<div id="${id}MultiIDsContainerDiv" <#if view>class="multi-container-readonly ${multiDivClass}" style="height:auto!important;min-height:26px;"<#else>class="multi-container ${multiDivClass}" style="${multiDivStyle};height:26px;"</#if> >
	<input type="hidden" id="${hiddenId}id" name="${name}.id" />
	<input type="hidden" id="${hiddenId}" name="${name}" />
	<#if isWrap>
	<#if !view>
	<span style="float:left;" id="${id}MultiSelect">
		<#if mneenable>
		<span style="margin-right:2px;position: relative;">
			<input id="${id}MneInput" name="${name?html}" multiable="true" <#if formId != ''>formId="${formId}"</#if> onfocus="CUI.clearMneTips('${name?replace('.', '_')}_mneTipLabel','${formId}')" onclick="CUI.clearMneTips('${name?replace('.', '_')}_mneTipLabel','${formId}');<#if isWrap>CUI.focusMneInput('${id}');</#if>" value="${value!''}" isCrossCompany="${isCrossCompany?string}"  refViewCode="${refViewCode!}"  currentViewCode="${currentViewCode!}"  realPermissionCode="${realPermissionCode!}"   <#if isEdit>onkeydown="if(event.keyCode==13) return false" isEdit='true'</#if>  autocomplete=off style="border:0;width:95px;"<#rt/>
			onkeyup="selectKeyEvent(event,this,'${url}','${displayFieldName}','<#if type?? && type != "">${type}<#else>other</#if>','${multiple?string}','${mnewidth}','<#if onkeyupfuncname??>${onkeyupfuncname}</#if>','<#if conditionfunc??>${conditionfunc?replace('\'', '\\\'')}</#if>')"<#rt/>
			onblur="var that=this;setTimeout(function(){if(!window.mnePageBtnFlag){cleanMneDiv(that,0,'${formId}');CUI.restoreMneTips(that,'${name?replace('.', '_')}_mneTipLabel','${formId}');}},200);"<#rt/>
		/>
		<label id="${name?replace('.', '_')}_mneTipLabel" <#if (value?? && value != "") || (deValue?? && deValue != "")>style="display:none"</#if> onclick="CUI.clearMneTips('${name?replace('.', '_')}_mneTipLabel','${formId}');CUI('#${id}MneInput').focus();" class="mne-tip search-ft-color">${mneTip}</label>
		</span>
		</#if>
	</span>
	</#if>
	</#if>
	<span id="${id}MultiIDsContainer" <#if isWrap>class="mneMultiContainerWrap"</#if> style="<#if isWrap>height:26px;<#elseif mneenable><#if !view>position:relative;top:-1px;top:-2px \9;*top:-5px;</#if><#else>position:relative;top:5px;</#if>" >
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
			<input id="${id}MneInput" name="${name?html}" multiable="true" <#if formId != ''>formId="${formId}"</#if> onfocus="CUI.clearMneTips('${name?replace('.', '_')}_mneTipLabel','${formId}')" isCrossCompany="${isCrossCompany?string}"  refViewCode="${refViewCode!}" currentViewCode="${currentViewCode!}"  realPermissionCode="${realPermissionCode!}"    <#if isEdit>onkeydown="if(event.keyCode==13) return false" isEdit='true'</#if> autocomplete=off style="border:0;width:95px;"<#rt/>
			onkeyup="selectKeyEvent(event,this,'${url}','${displayFieldName}','<#if type?? && type != "">${type}<#else>other</#if>','${multiple?string}','${mnewidth}','<#if onkeyupfuncname??>${onkeyupfuncname}</#if>','<#if conditionfunc??>${conditionfunc?replace('\'', '\\\'')}</#if>')"<#rt/>
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
		function delete${id}Multi(imgObj) {
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
			
		};
		
		
		//多选回调函数默认的对象MneObj，默认的隐藏IDs名称为MultiIDs，存放人名显示的span名称为MultiIDsContainer
		function get${id}MultiInfo() {
			if (MneObj == null || MneObj == undefined || MneObj.id == null || MneObj.id == undefined) {
				return false;
			}
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
		}
		
		<#if clicked>
		var ${id}_dialog;
		
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
			<#if isCrossCompany??&&isCrossCompany?string=='true'>
			refparam+="&crossCompanyFlag=true";
			</#if>
			<#if multiple?? && multiple>
			refparam+="&multiSelect=true";
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
				closePage : true,
				
				<#if iframe>
				iframe : '${id}',
				dialogName : '${id}' + '${name}',
				</#if>
				
				callBackFuncName : '_callback_${id!}',
				url : url,
				title : title,
				params : refparam
			});
		}
	
		function _callback_${id!}(objs) {
			if (objs == null || objs == undefined || objs.length <= 0) {
				return false;
			}
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
			//console.log(11);
		}
		//console.log(22);
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
			var mneInputWidth = <#if mneenable>parseInt(CUI("#${id}MneInput").css('width'),10)<#else>0</#if>;
			var trueWidth = parseInt(CUI("#${id}MultiIDsContainerDiv").css('width'),10) - 30;
			var spanWidth = 0;
			$('span.mne-select-span',CUI("#${id}MultiIDsContainer")).each(function(index){
				spanWidth += parseInt($(this).css('width'),10);
			});
			if(parseInt(spanWidth,10) + mneInputWidth - trueWidth > 0) {
				$('#${id}ellipsisBtn').show();
			} else {
				$('#${id}ellipsisBtn').hide();
			}
		}
		
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
	</#if>
	<#if !mneenable>
	$("#${id}MultiIDsContainerDiv").click(function(e){
		if($('span.mne-select-span',$('#${id}MultiIDsContainerDiv')).size() == 0) {
			stopBubble(e);
			${id}_selectEvent('<#if type?? && type != "">${type?uncap_first}<#else>other</#if>','${url}','<#if reftitle?? && reftitle != "">${reftitle}<#else>${getText("foundation.tagselect.view")}</#if>','${funcparam}');
		}
	});
	</#if>
	$('input[type="button"]',$("#${id}MultiIDsContainerDiv")).click(function(e){
		stopBubble(e);
    	${id}_selectEvent('<#if type?? && type != "">${type?uncap_first}<#else>other</#if>','${url}','<#if reftitle?? && reftitle != "">${reftitle}<#else>${getText("foundation.tagselect.view")}</#if>','${funcparam}');
	});
	$('img[mneobjid]',$('#${id}MultiIDsContainerDiv')).click(function(e){
		stopBubble(e);
    	delete${id}Multi(this);
	});
	$.setSelectTagValue=function(str){
		CUI("#${id}MultiIDsContainer").html(str);
		CUI("#cheShitagnumberid").val(str);
		CUI("#cheShitagnumber").val(str);
	};
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