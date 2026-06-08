<#--
clearFunc=清除函數，如果為空值，则只清除当前文本框
-->
<#macro selector id,value,pageType,showRange="",onclick="",showRange="",isCrossCompany=false,viewType="create",deValue="",cssClass="",onBeforeClick="",parameters="",tabindex="",href="",funcparam="",callBackFuncName="",title="",cssStyle="",view=false,name="",disabled=false,readOnly=true,windowName="",multiSelect="false",closePage="",openwindowCss="",open_url="",openType="page",clickedClass="cui-search-click",displayType="selectComp",clearFunc="",delCustomCallback="">
	<#if !view>
	<div class="fix-input<#if (viewType?? && viewType == 'readonly')>-readonly</#if>">
	<div class="fix-search-click" id="${id}div" onmouseenter="select_${id?replace('.','_')}_clear();" onmouseleave="select_${id?replace('.','_')}_deleteImage();">
		<#assign originalValue = ''>
		<#if viewType=='create'>
			<#if value?? && value?has_content>
				<#assign originalValue = value>
			<#else>
				<#if deValue=='currentUser'>
					<#assign originalValue = getCurrent('staffName')>
				<#elseif deValue=='currentPost'>
					<#assign originalValue = getCurrent('positionName')>
				<#elseif deValue=='currentDepart'>
					<#assign originalValue = getCurrent('departmentName')>
				<#elseif deValue=='currentComp'>
					<#assign originalValue = getCurrent('companyName')>
				</#if>
			</#if>
		<#elseif viewType=='edit' || viewType=='readonly'>
			<#assign originalValue = value>
		</#if>
		<input type="text" selectType="select"<#rt/>
		 name="${name?html}"<#rt/>
		<#if disabled>
		 disabled="disabled"<#rt/>
		</#if>
		<#if readOnly>
		 readonly="true"<#rt/>
		</#if>
		<#if deValue!=''>
		deValue="${deValue!}"<#rt/> 
		</#if>
		<#if tabindex??>
		 tabindex="${tabindex?html}"<#rt/>
		</#if>
		<#if href?if_exists != "">
		 href="${href}"<#rt/>
		</#if>
		<#if id??>
		 id="${id?html}"<#rt/>
		</#if>
		<#if value??>
		 value="${value?html}" originalValue="${originalValue?html}"<#rt/>
		</#if>
		<#if cssStyle??>
		 style="${cssStyle?html} <#if !(value?if_exists != "")> ;cursor: pointer;" </#if><#rt/>
		</#if>
		<#if title??>
		 title="${title?html}"<#rt/>
		</#if>
		<#if cssClass?if_exists != "">
		 class="${cssClass?html}"<#rt/>
		</#if> 
		<#if (readOnly?? && readOnly) && !(value?if_exists != "")>
		 onclick="select_${id?replace('.','_')}_callOpenWeb();"<#rt/>
		 </#if>
		  
		   />
		
		<input type="button" class="${clickedClass}" value="&nbsp;"  onclick="select_${id?replace('.','_')}_callOpenWeb();"></input>
		</div>
		</div>
		<script type="text/javascript">
		$(function(){
		//chrome下使用
			$(YUD.get("${id}div")).bind('mouseenter',function(){
			  select_${id?replace('.','_')}_clear();
			});
			$(YUD.get("${id}div")).bind('mouseleave',function(){
			  select_${id?replace('.','_')}_deleteImage();
			});
		})
			select_${id?replace('.','_')}_clear = function(){
				var length=$.trim(YUD.get("${id?html}").value).length;
				if(length>0){
					var newSpan = $("<input  type='button' class='cui-edit-clear' id='${id}Image' onclick='select_${id?replace('.','_')}_delete();'/>");
					$(newSpan).insertAfter(YUD.get("${id?html}"));
				}
			};
			select_${id?replace('.','_')}_deleteImage = function(){
				$(YUD.get("${id}Image")).remove();
			};
			select_${id?replace('.','_')}_callOpenWeb = function(){
			<#if onBeforeClick?has_content>
				try {
					var beforeResult=${onBeforeClick};
					if(beforeResult == false) {
						return false;
					}
				} catch (e) {
				}
			</#if>
			<#if onclick != "">
				${onclick}(<#if funcparam??>${funcparam}</#if>);
				return true;
			<#else>
				var refparam = "${parameters}";
				<#if isCrossCompany?? && isCrossCompany>
				if(refparam != null && refparam.length > 0) {
					refparam += "&crossCompanyFlag=true";
				} else {
					refparam = "crossCompanyFlag=true";
				}
				</#if>
				<#if openType == 'page'>
						<#if openwindowCss?? && openwindowCss != ''>
						var windowStyle = "${openwindowCss},scrollbars=yes,resizable =yes,toolbar=no,menubar=no,location=no,status=yes";
						<#else>
						var windowStyle = "width=800,height=600,scrollbars=yes,resizable =yes,toolbar=no,menubar=no,location=no,status=yes";
						</#if>
						foundation.common.callOpenWeb("${pageType}",windowStyle,"${windowName}","${multiSelect}","${closePage}","${callBackFuncName}",refparam,"${open_url}","${showRange!}");
				<#else>
						foundation.common.select({
							pageType : "${pageType}",
							multiSelect: "${multiSelect}",
							closePage:"${closePage}",
							width : 800,
							height : 600,
							callBackFuncName : "${callBackFuncName}",
							title : "${getText('js.cui.js.option')}",
							params : refparam,
							url : "${open_url}",
							showRange:"${showRange!}"
						});
				</#if>
			</#if>
			};
			
			select_${id?replace('.','_')}_delete = function(){
				<#if clearFunc!="">
					eval('${clearFunc}');
				<#else>
					// YUD.get("${id?html}").value='';
					CUI.clearInput("${name?html}");
				</#if>
				<#if delCustomCallback?? && delCustomCallback!="">
					eval('${delCustomCallback}()');
				</#if>
			};
		</script>
	<#-- 查看 -->
	<#else>
	<div class="fix-input<#if (readOnly?? && readOnly)||view==true>-readonly</#if>">
	<div class="fix-search-click" >
		<input name="${name}" class="cui-noborder-input" readonly="true" <#rt/>
		<#if cssStyle??>
		 style="${cssStyle?html}"<#rt/>
		</#if>
		<#if value??>
		 value="${value?html}"<#rt/>
		</#if>
		/>
	</div>
	</div>
	</#if>

</#macro>
