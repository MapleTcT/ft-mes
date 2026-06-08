<#-- systemcode -->
<#---macro systemcode code,name="",value="",showValue="",cssStyle="",title="",
view=false,listType="",multiFlag="",readOnly=false-->
<#macro systemcode code,name="", istreesystem="", property_type="",advquery=false,viewType="create",deValue="",onchange="",value="",classStyle="edit-input-text",cssStyle="",title="",view=false,readOnly=false,formId="",showType="default",iscustom=false,ecFlag=false,multable=false,seniorSystemCode=false,isOnlySelectLeaf=false>

<#assign systemEntity=getSystemCode(code)>
<#assign listType=systemEntity.listType>
<#if ecFlag>
	<#assign multiFlag=multable>
<#else>
	<#assign multiFlag=systemEntity.multiFlag>
</#if>
<#if seniorSystemCode>
<#assign showValue=getSystemCodeValue(code, value)>
<#else>
<#assign showValue=getSystemCodeValue(value)>
</#if>
<#--<#assign valueList=getSystemCodeList(code)>-->

<#assign systemcodeNames = name?split('.')>
<#assign systemcodeValueName = ''>
<#if systemcodeNames??>
	<#list systemcodeNames as item>
		<#if item_has_next>
			<#if item_index gt 0>
			<#assign systemcodeValueName = systemcodeValueName + '.' + item>
			<#else>
			<#assign systemcodeValueName = systemcodeValueName + item>
			</#if>
		<#elseif iscustom>
			<#assign systemcodeValueName = systemcodeValueName + '.' +item+'.value'>
		<#else>
			<#assign systemcodeValueName = systemcodeValueName + '.value'>
		</#if>
	</#list>
</#if>
<#assign selectValue = "">
<#if viewType?? && viewType=='create'>
	<#----说明：系统编码默认值优先级  视图中配置则以视图为准，其次为模型字段的默认值，最后为系统编码本身默认值----->
	<#if deValue?? && deValue?has_content>
		<#assign selectValue = deValue>
	<#elseif value?? && value?has_content>
		<#assign selectValue = value>
	<#else>
		<#list systemEntity.systemCodes?keys as key>
			<#if (systemEntity.systemCodes[key].defaultFlag)?? && systemEntity.systemCodes[key].defaultFlag>
				<#assign selectValue = systemEntity.systemCodes[key].id><#-- 系统编码管理中设置的默认值-->
				<#break>
			</#if>
		</#list>
	</#if>
<#elseif viewType=='edit' || viewType=='readonly'>
	<#if value??>
		<#assign selectValue = value><#-- 查看、修改时与默认值无关-->
	</#if>
</#if>

<#-- 编辑 -->
<#if view!=true>
<#-- 列表 -->
<#if listType == 'LIST'>

	<#-- 多选 -->
	<#if multiFlag>
	<div class="fix-input"><div class="fix-search-click" id="<#if name!=""><#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if></#if>_container">
		<input type="hidden" 
			validateType="SystemCode"
			<#if iscustom>
			iscustom="true"
			</#if>
			<#if name!="">
			name="${name}"<#rt/>
			id="<#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if>"<#rt/>
			</#if>
			exp="like" <#rt/>
			originalValue="<#if selectValue??>${selectValue!}</#if>" 
			value="<#if selectValue??>${selectValue!}</#if>" 
			deValue="${deValue!}"
			<#if onchange?? && onchange?has_content>
				onchange="${onchange}"<#rt/>
			</#if>
		/>
		<input type="text" class="cui-noborder-input" name="${systemcodeValueName}" istreesystem="1" property_type="${property_type}" readonly="true" id="systemValue_<#if name!=""><#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if></#if>"  style="padding-left:0px;width:100%;margin-right:-5px;"
		<#if selectValue??>
			value="${getText(getSystemCodeValue(selectValue))}"<#rt/>
		</#if>
		<#if cssStyle!="">
		 style="${cssStyle}"<#rt/>
		</#if>
		<#if title!="">
		 title="${title}"<#rt/>
		</#if>
		/>
		<input type='button' class='cui-edit-clear' style="display:none;" id='<#if name!=""><#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if></#if>_clearbtn' />
		<input  id="systemValue_search_click<#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if>${formId}"  type="button" class="cui-search-click systemValue_search_click" value="&nbsp;"></input>
	</div>
	<#else><#-- 单选-->
	<#--<input type="hidden" value="type:${type!}||value:${value!}||deValue:${deValue!}||selectValue:${selectValue!}" />-->
	<div class="fix-search-click">
	<#if showType?? && showType == 'RADIO'>
		<#list systemEntity.systemCodes?keys as key> 
		<span class="checkbox-radio-span">
			<input name="${name}" type="radio" value="${systemEntity.systemCodes[key].id}" property_type="${property_type}"
				<#if selectValue?? && selectValue?has_content && selectValue==systemEntity.systemCodes[key].id>checked</#if>
				<#if onchange?? && onchange?has_content>
					onclick="${onchange}"<#rt/>
				</#if>
			>${getText(systemEntity.systemCodes[key].value)}
		</span>
		</#list>
	<#else>
	<select validateType="SystemCode" id="<#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if>" originalValue="<#if selectValue??>${selectValue!}</#if>" value="<#if selectValue??>${selectValue!}</#if>" deValue="${deValue!}" class="edit-select ${classStyle}" name="${name}" istreesystem="0" property_type="${property_type}"<#rt/>
		<#if readOnly==true>
		 	disabled="true"<#rt/>
		</#if>
		<#if onchange?? && onchange?has_content>
			onchange="${onchange}"<#rt/>
		</#if>
	>
	<option value=""></option>
	<#if seniorSystemCode>
	<#list systemEntity.systemCodes?keys as key> 
		<option value="${systemEntity.systemCodes[key].code}"<#rt/>
	<#if selectValue?? && selectValue?has_content && selectValue==systemEntity.systemCodes[key].code>
		selected="true"<#rt/>
	</#if>
	>${getText(systemEntity.systemCodes[key].value)}</option>
	</#list>
	<#else>
	<#list systemEntity.systemCodes?keys as key> 
		<option value="${systemEntity.systemCodes[key].id}"<#rt/>
	<#if selectValue?? && selectValue?has_content && selectValue==systemEntity.systemCodes[key].id>
		selected="true"<#rt/>
	</#if>
	>${getText(systemEntity.systemCodes[key].value)}</option>
	</#list>
	</#if>
	<#--
	<#list valueList?keys as key> 
		<option value="${key}"<#rt/>
	<#if (value?has_content && value == key)>
		selected="true"<#rt/>
	</#if>
	>${valueList[key]}</option>
	</#list>
	-->
	</select>
	</#if>
	</#if>
	<input validateType="SystemCode" type="hidden" id="<#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if>${formId}sysId"
	<#if name!="">
		 value="<#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if>"<#rt/>
		 title="<#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if>"<#rt/>
	</#if>/>
	</div>
<#else><#-- 树形 -->
	<#-- 多选 -->
	<div class="fix-input"><div class="fix-search-click" id="<#if name!=""><#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if></#if>_container">
	<#if multiFlag>
		<input type="hidden" 
			validateType="SystemCode"
			<#if iscustom>
			iscustom="true"
			</#if>
			<#if name!="">
			name="${name}"<#rt/>
			id="<#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if>"<#rt/>
			</#if>
			exp="like" <#rt/>
			originalValue="<#if selectValue??>${selectValue!}</#if>" 
			value="<#if selectValue??>${selectValue!}</#if>" 
			deValue="${deValue!}"
			<#if onchange??>
				onchange="${onchange}"<#rt/>
			</#if>
		/>
		<input type="text" listType="" class="cui-noborder-input" name="${systemcodeValueName}" istreesystem="1" property_type="${property_type}" readonly="true" id="systemValue_<#if name!=""><#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if></#if>"  style="padding-left:0px;width:100%;margin-right:-5px;"
		<#if selectValue??>
			value="${getText(getSystemCodeValue(selectValue))}"<#rt/>
		</#if>
		<#if cssStyle!="">
		 style="${cssStyle}"<#rt/>
		</#if>
		<#if title!="">
		 title="${title}"<#rt/>
		</#if>
		/>
		<input type='button' class='cui-edit-clear' style="display:none;" id='<#if name!=""><#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if></#if>_clearbtn' />
		<input  id="systemValue_search_click<#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if>${formId}"  type="button" class="cui-search-click systemValue_search_click" value="&nbsp;"></input>
		
	<#else><#-- 单选-->
		<input type="hidden" 
			validateType="SystemCode"
			<#if iscustom>
			iscustom="true"
			</#if>
			<#if name!="">
			name="${name}"<#rt/>
			</#if>
			id="<#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if>"<#rt/>
			originalValue="<#if selectValue??>${selectValue!}</#if>" 
			value="<#if selectValue??>${selectValue!}</#if>" 
			deValue="${deValue!}" 
			<#if onchange??>
				onchange="${onchange}"<#rt/>
			</#if>
		/>
		<input type="text" class="cui-noborder-input" name="${systemcodeValueName }" istreesystem="1" property_type="${property_type}" readonly="true" id="systemValue_<#if name!=""><#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if></#if>" 
			<#if selectValue??>
				value="${getText(getSystemCodeValue(selectValue))}"<#rt/>
			</#if>
			<#if cssStyle!="">
			style="${cssStyle}"<#rt/>
			</#if>
			<#if title!="">
			title="${title}"<#rt/>
			</#if>
		/>
		<input type='button' class='cui-edit-clear' style="display:none;" id='<#if name!=""><#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if></#if>_clearbtn' />
		<input  id="systemValue_search_click<#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if>${formId}"  type="button" class="cui-search-click systemValue_search_click" value="&nbsp;"></input>
		
	</#if>
	<input type="hidden" id="<#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if>${formId}sysId"
	<#if name!="">
		 value="<#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if>"<#rt/>
		 title="<#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if>"<#rt/>
	</#if>/>
		</div>
</div>
</#if>
	

<#-- 查看 -->
<#else>
	<input type="hidden" name="${name}" value="${selectValue!}" />
	<div class="fix-input-readonly"><input type="text" class="${classStyle}" name="${systemcodeValueName }" istreesystem="1" property_type="${property_type}" readonly="true" id="systemValue"
		<#if selectValue!="">
		<#if seniorSystemCode>
		<#assign showValue=getSystemCodeValue(code, selectValue)>
		<#else>
		<#assign showValue=getSystemCodeValue(selectValue)>
		</#if>
		 value="${getText(showValue!)}"<#rt/>
		</#if>
		<#if cssStyle!="">
		 style="${cssStyle!};padding-left:0px;border:0;width:100%;"<#rt/>
		</#if>
		<#if title!="">
		 title="${title!}"<#rt/>
		</#if>
		/>
	</div>
</#if>

<script type="text/javascript">
	var showOverLayerDIv_<#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if>${formId};
	<#if listType == 'LIST'>
		<#-- 多选 -->
			<#if multiFlag>		
				$( '#<#if name!=""><#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if></#if>_clearbtn' ).bind( 'click', function(){
					$('#<#if name!=""><#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if></#if>').val( '' );
					$('#systemValue_<#if name!=""><#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if></#if>').val( '' );
					$("input[name^='${code}']").attr("checked", false); // 清除系统编码多选框勾选
					$(this).hide();
				});
				
				$( '#<#if name!=""><#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if></#if>_container' ).hover(function(){
					if( $('#<#if name!=""><#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if></#if>').val() ){
						$( '#<#if name!=""><#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if></#if>_clearbtn' ).show();
					}
				},function(){
					$( '#<#if name!=""><#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if></#if>_clearbtn' ).hide();
				})
				$('#systemValue_search_click<#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if>${formId}').unbind('click.showOverlay').bind('click.showOverlay', function(e){
					ev = e || window.event;
					if (ev) { // 停止事件冒泡
    					ev.stopPropagation();
            		} else {
            			window.event.cancelBubble = true;
            		}
            		showOverLayer_<#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if>${formId}(this,"/msService/ec/systemCode/multiCodeList?systemEntityCode=${code}")
				})
				
			<#else><#-- 单选-->
					$('#systemValue_search_click<#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if>${formId}').unbind('click.showOverlay').bind('click.showOverlay', function(e){
					ev = e || window.event;
					if (ev) { // 停止事件冒泡
    					ev.stopPropagation();
            		} else {
            			window.event.cancelBubble = true;
            		}
            		showOverLayer_<#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if>${formId}(this,"/msService/ec/systemCode/multiCodeList?systemEntityCode=${code}")
				})
			</#if>
	<#else><#-- 树形 -->
			$( '#<#if name!=""><#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if></#if>_clearbtn' ).bind( 'click', function(){
				$('#<#if name!=""><#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if></#if>').val( '' );
				$('#systemValue_<#if name!=""><#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if></#if>').val( '' );
				$('#<#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if>').val( '' );
				<#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if>
				var tree = $.fn.zTree.getZTreeObj('singleCodeTree${code}<#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if>${formId}') || $.fn.zTree.getZTreeObj('multiCodeTree${code}<#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if>${formId}');
				if (tree) {
					foundation.systemCode.uncheckAllSystemCode${code}<#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if>${formId}()//20161220同时取消已选中ztree值
				}
				$(this).hide();
			});
			
			$( '#<#if name!=""><#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if></#if>_container' ).hover(function(){
				if( $('#<#if name!=""><#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if></#if>').val() ){
					$( '#<#if name!=""><#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if></#if>_clearbtn' ).show();
				}
			},function(){
				$( '#<#if name!=""><#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if></#if>_clearbtn' ).hide();
			})
			<#if multiFlag><#-- 多选 -->
				//url = "/msService/ec/systemCode/multiCodeTree?systemEntityCode=${code}";
				$('#systemValue_search_click<#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if>${formId}').unbind('click.showOverlay').bind('click.showOverlay', function(e){
					ev = e || window.event;
					if (ev) { // 停止事件冒泡
    					ev.stopPropagation();
            		} else {
            			window.event.cancelBubble = true;
					}
					var value = $('#<#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if>').val();
            		showOverLayer_<#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if>${formId}(this,"/msService/ec/systemCode/multiCodeTree?systemEntityCode=${code}&systemCode.id=" + value);
				})
			<#else><#-- 单选-->
			//	url = "/msService/ec/systemCode/singleCodeTree?systemEntityCode=${code}";
					$('#systemValue_search_click<#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if>${formId}').unbind('click.showOverlay').bind('click.showOverlay', function(e){
					ev = e || window.event;
					if (ev) { // 停止事件冒泡
    					ev.stopPropagation();
            		} else {
            			window.event.cancelBubble = true;
					}
					var value = $('#<#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if>').val();
            		showOverLayer_<#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if>${formId}(this,"/msService/ec/systemCode/singleCodeTree?isOnlySelectLeaf=${isOnlySelectLeaf?string('true','false')}&systemEntityCode=${code}&systemCode.id=," + value + ",");
				})			
			</#if>
	</#if>
	
		 $('body').click(
			function(){
				if(showOverLayerDIv_<#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if>${formId} && showOverLayerDIv_<#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if>${formId}.isShow) {
					showOverLayerDIv_<#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if>${formId}.hide();
					$(showOverLayerDIv_<#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if>${formId}._overlay).prev().css("visibility", "hidden");
				}
				
			}
		);
			
	function showOverLayer_<#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if>${formId}(obj,url){
		//新增弹框里面有系统编码，勾选状态不对，屏蔽此代码
		// if( showOverLayerDIv_<#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if>${formId} ){
		// 	showOverLayerDIv_<#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if>${formId}.setPosition();
		// 	showOverLayerDIv_<#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if>${formId}.show();
		// 	return;
		// }
		showOverLayerDIv_<#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if>${formId} = new CUI.Overlay({
			// align:obj,
	     	el:'sysContent_systemValue_search_click<#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if>${formId}',
	     	title:"${getText('foundation.systemCode.systemcode')}",
	     	width:200,
	     	height:267,
	     	zIndex:9999,
	     	shadow:false,
			buttons:[
					{	name:"${getHtmlText('foundation.workbench.mainPage.sure')}",
						id:"systemValue_search_click<#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if>${formId}_overlay_btn_select",
						handler:function(){
							getsystemCodeValue_<#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if>${formId}();
						}
					},
					{	name:"${getHtmlText('calendar.common.cancal')}",
						id:"systemValue_search_click<#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if>${formId}_overlay_btn_cancel",
						handler:function(){
							closeSystemWindow_<#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if>${formId}();
						}
					}]
	     	
		});
		
		
		var sysId=CUI("#<#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if>${formId}sysId").val();
		var valueId="#"+sysId;
		var sysCode=CUI(valueId).val();
		var layObj = showOverLayerDIv_<#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if>${formId};
		
		layObj.render();
		
		$("#overlay-idsysContent_systemValue_search_click<#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if>${formId}").click(function(e){
			ev = e || window.event;
			if (ev) { // 停止事件冒泡
				ev.stopPropagation();
			} else {
				window.event.cancelBubble = true;
			}
			//绑定系统编码树形点击事件	
			var tree = $.fn.zTree.getZTreeObj('singleCodeTree${code}<#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if>${formId}') || $.fn.zTree.getZTreeObj('multiCodeTree${code}<#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if>${formId}');
			if (tree && layObj) {
				CUI.changeSystemCodeBox(tree, layObj);
				layObj.setPosition(obj);
			}
		})

		url+="&fileName=<#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if>";
		url+="&formId=${formId}";
		url+="&systemCodeCode="+sysCode;
		url+="&entityCode=${code}";
		url+="&time="+new Date();

		$("#sysContent_systemValue_search_click<#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if>${formId}").html('<table style="width:100%;height:100%;text-align:center;"><tr><td><label class="datagrid-loading">${getText("common.text.loading")}</label></td></tr></table>');
		CUI('#sysContent_systemValue_search_click<#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if>${formId}').load(url,function(){
			var existsValue = $('input:hidden', $(obj).parent()).val();
			var arr = existsValue.split(',');
			for(var x = 0; x < arr.length; x++) {
				if(arr[x] && arr[x].length > 0) {
					$('#sysContent_systemValue_search_click<#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if>${formId} input[name="' + arr[x] + '"]').prop('checked', true);
				}
			}
		});

		setTimeout(function() {
			var tree = $.fn.zTree.getZTreeObj('singleCodeTree${code}<#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if>${formId}') || $.fn.zTree.getZTreeObj('multiCodeTree${code}<#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if>${formId}');			
			if (tree && layObj) {
				$(layObj._overlay.children[1]).css("position", "relative");
				tree.setting.treeObj.css("position", "absolute");
				CUI.changeSystemCodeBox(tree, layObj);
			}
			layObj.setPosition(obj);
			layObj.show();
		}, 700);
	}
	getsystemCodeValue_<#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if>${formId}=function(){
		var str=foundation.systemCode.getSystemCodeValue${code}<#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if>${formId}();
		var arr='';
		if(str.indexOf("#|#")!=-1){
			arr=str.split("#|#"); //树形多选
		}else{
			arr=str.split("@|@");
		}
		
		if(arr.length>0){
			var sysValue=arr[0];
			var sysCode=arr[1];

			//给系统编码列表多选、系统编码树形多选和单选添加onchange事件
			var input = document.getElementById('<#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if>');
			var regex = /^,*|,*$/;
			var reg=/,$/gi;
			var inputVal;
			if (input.value) {
				inputVal = input.value.replace(regex, "");
				inputVal = inputVal.replace(reg, "");
			}
			if (inputVal != sysCode) {
				if (input && input.onchange) {
					eval(input.onchange());				
				}
			}
			
			//var sysId=CUI("#<#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if>${formId}sysId").val();
			var sysId=CUI("#<#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if>${formId}sysId").attr("title");
			var valueId="#"+sysId;
			var showValueId="#systemValue_"+sysId;
			var formId='${formId}';
			if(formId!="" && sysCode){
				<#if seniorSystemCode>
				CUI('#' + formId + " " + valueId).val(arr[2]);
				<#else>
				if(str.indexOf("#|#")!=-1){
					CUI('#' + formId + " " + valueId).val(","+sysCode+",");
				}else{
					CUI('#' + formId + " " + valueId).val(sysCode);
				}
				</#if>
				
				CUI('#' + formId + " " + showValueId).val(sysValue);
			} else {
			<#if seniorSystemCode>
				CUI(valueId).val(arr[2]);
			<#else>
				CUI(valueId).val(sysCode);
			</#if>
				
				CUI(showValueId).val(sysValue);
			}
			
		}
		showOverLayerDIv_<#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if>${formId}.hide();
		$(showOverLayerDIv_<#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if>${formId}._overlay).prev().css("visibility", "hidden");
	}
	closeSystemWindow_<#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if>${formId}=function(){
		showOverLayerDIv_<#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if>${formId}.hide();
		$(showOverLayerDIv_<#if !advquery>${name?replace(".", "")}<#else>adv_${name?replace(".", "")}</#if>${formId}._overlay).prev().css("visibility", "hidden");
	}

</script>
</#macro>