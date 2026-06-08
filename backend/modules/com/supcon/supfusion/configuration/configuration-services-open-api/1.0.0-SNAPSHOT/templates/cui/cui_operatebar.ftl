<#-- operatebar -->

<#macro operatebar operates="" entityCode="",menuCode="",operateType="",businessParam="",resultType="html",serviceName="",buttonCode="",buttonType="",viewEdit=false,isPermission=false,isHide=false,permissionCode='',onclick='',nameKey='' buttonId=''>
<#assign operates = operates>
	<#if resultType=="html">
	<script type="text/javascript">
	</#if>
	<#if !viewEdit> 
		function operateBarOnclickFoundation(url){
			<#if businessParam??&&businessParam!="" >
				if(url.indexOf('?')==-1){
					url+="?${businessParam}";
				}else{
					url+="&${businessParam}";
				}
			</#if>
			openFullScreen(url);
		}
	</#if>
	<#if resultType=="html">
	</script>
	</#if>
	<#if !viewEdit >
		<#if serviceName?? && serviceName?length gt 0 && operates?exists>
			${checkOperatePower(operates,menuCode,entityCode,operateType,resultType,serviceName,Parameters)!}
		<#elseif operates?exists>
			${checkOperatePower(operates,menuCode,entityCode,operateType,resultType,'','')!}
		</#if>
	<#else>
		<#if isPermission>
			<#if checkUserPermisition(permissionCode)>
				<a class='cui-btn-new' id="edit-btn-${buttonId}" style="<#if isHide>display:none;</#if>" onclick="${onclick}()"> <span class="edit-default"></span>${getHtmlText('${nameKey}')}</a>
			</#if>
		<#else>
			<a class='cui-btn-new' id="edit-btn-${buttonId}" style="<#if isHide>display:none;</#if>" onclick="${onclick}()"> <span class="edit-default"></span>${getHtmlText('${nameKey}')}</a>	
		</#if>
	</#if>
</#macro>
