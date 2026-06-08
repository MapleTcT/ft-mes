
<script type="text/javascript">
var GloabFlag=0;
function showOverLayer(obj,url){

	showOverLayerDIv = new CUI.Overlay({
		align:obj,
     	el:'customContent',
     	title:'系统编码',
     	width:180,
     	height:220,
     	zIndex:9999,
     	shadow:true,
		buttons:[
				{	name:"确定",
					handler:function(){getsystemCodeValue()}
				},
				{	name:"取消",
					handler:function(){closeSystemWindow()}
				}]
     	
	});

	var sysId=CUI("#${parameters.code}sysId").val();
	var valueId="#"+sysId;
	var sysCode=CUI(valueId).val();
	showOverLayerDIv.render();
	showOverLayerDIv.show();

	url+="&systemCodeCode="+sysCode;
	url+="&time="+new Date();
	setTimeout(function(){
		if(GloabFlag==0){
			CUI('#customContent').load(url);
			GloabFlag=1;
		}
		
	},200);
	
}
getsystemCodeValue=function(){
var str=foundation.systemCode.getSystemCodeValue();
var arr=str.split("@|@");
if(arr.length>0){
	var sysValue=arr[0];
	var sysCode=arr[1];
	var sysId=CUI("#${parameters.code}sysId").val();
	var valueId="#"+sysId;
	var showValueId="#systemValue_"+sysId;
	CUI(valueId).val(sysCode);
	CUI(showValueId).val(sysValue);
	
}
showOverLayerDIv.hide();
}
closeSystemWindow=function(){
	showOverLayerDIv.hide();
}
</script>

<#-- 编辑 -->
<#if !parameters.view>

<#-- 列表 -->
<#if parameters.listType == 'LIST'>

	<#-- 多选 -->
	<#if parameters.multiFlag == 'true'>
		<input type="hidden" 
		<#if parameters.name??>
		 name="${parameters.name?html}"<#rt/>
		  id="${parameters.name?replace(".", "")?html}"<#rt/>
		</#if>
		
		<#if parameters.value??>
		 value="${parameters.value?html}"<#rt/>
		</#if>
		/>
		<input type="text" class="cui-edit-field" name="systeCode" readonly="true" id="systemValue_<#if parameters.name??>${parameters.name?replace(".", "")?html}</#if>"  style="padding-left:0px;width:98%;"
		<#if parameters.value??>
		 value="${parameters.showValue?html}"<#rt/>
		</#if>
		<#if parameters.cssStyle??>
		 style="${parameters.cssStyle?html}"<#rt/>
		</#if>
		<#if parameters.title??>
		 title="${parameters.title?html}"<#rt/>
		</#if>
		/>
		<input type="button" class="cui-search-click" value="&nbsp;" onclick="showOverLayer(this,'/msService/ec/systemCode/multiCodeList?sytemEntityCode=${parameters.code}')"></input>
	<#else><#-- 单选-->
	<select id="${parameters.id}" name="${parameters.name}"<#rt/>
		<#if parameters.cssStyle??>
			 style="${parameters.cssStyle?html}"<#rt/>
		<#else>
			 style="width:99%;border: 1px solid #A5C7EF"<#rt/>
		</#if>
		<#if parameters.readOnly?default(false)>
		 	disabled="true"<#rt/>
		</#if>
	>
	<option value=""></option>
	<#list parameters.dataSource?keys as key> 
		<option value="${key}"<#rt/>
	<#if parameters.value?? && parameters.value == key>
		selected="true"<#rt/>
	</#if>
	>${parameters.dataSource[key]}</option>
	</#list>
	</select>
	</#if>
<#else><#-- 树形 -->
	<#-- 多选 -->
	<#if parameters.multiFlag == 'true'>
		<input type="hidden" 
		<#if parameters.name??>
		 name="${parameters.name?html}"<#rt/>
		 id="${parameters.name?replace(".", "")?html}"<#rt/>
		</#if>
		<#if parameters.value??>
		 value="${parameters.value?html}"<#rt/>
		</#if>
		/>
		<input type="text" class="cui-edit-field" name="systeCode" readonly="true" id="systemValue_<#if parameters.name??>${parameters.name?replace(".", "")?html}</#if>"  style="padding-left:0px;width:98%;"
		<#if parameters.value??>
		 value="${parameters.showValue?html}"<#rt/>
		</#if>
		<#if parameters.cssStyle??>
		 style="${parameters.cssStyle?html}"<#rt/>
		</#if>
		<#if parameters.title??>
		 title="${parameters.title?html}"<#rt/>
		</#if>
		/>
		<input type="button" class="cui-search-click" value="&nbsp;" onclick="showOverLayer(this,'/msService/ec/systemCode/multiCodeTree?sytemEntityCode=${parameters.code}')"></input>
		
	<#else><#-- 单选-->
		<input type="hidden" 
		<#if parameters.name??>
		 name="${parameters.name?html}"<#rt/>
		</#if>
		id="${parameters.name?replace(".", "")?html}"<#rt/>
		<#if parameters.value??>
		 value="${parameters.value?html}"<#rt/>
		</#if>
		/>
		<input type="text" class="cui-edit-field" name="systeCode"  readonly="true" id="systemValue_<#if parameters.name??>${parameters.name?replace(".", "")?html}</#if>"  style="padding-left:0px;width:98%;"
		<#if parameters.value??>
		 value="${parameters.showValue?html}"<#rt/>
		</#if>
		<#if parameters.cssStyle??>
		 style="${parameters.cssStyle?html}"<#rt/>
		</#if>
		<#if parameters.title??>
		 title="${parameters.title?html}"<#rt/>
		</#if>
		/>
		<input type="button" class="cui-search-click" value="&nbsp;" onclick="showOverLayer(this,'/msService/ec/systemCode/singleCodeTree?sytemEntityCode=${parameters.code}')"></input>
		
	</#if>
</#if>

	<input type="hidden" id="${parameters.code}sysId"
	<#if parameters.value??>
		 value="${parameters.name?replace(".", "")?html}"<#rt/>
	</#if>/>
<#-- 查看 -->
<#else>
<input type="text" class="cui-readonly-field" name="systeCode" readonly="true" id="systemValue"  style="padding-left:0px;width:98%;"
		<#if parameters.value??>
		 value="${parameters.showValue?html}"<#rt/>
		</#if>
		<#if parameters.cssStyle??>
		 style="${parameters.cssStyle?html}"<#rt/>
		</#if>
		<#if parameters.title??>
		 title="${parameters.title?html}"<#rt/>
		</#if>
		/>
</#if>
