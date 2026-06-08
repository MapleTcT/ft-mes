<style>
#fast_select_elements h2 {
margin: 1;
padding: 5px 0;
text-align: center;
font-size: 13px;
font-weight: normal;
border: 1px solid #0369A3;
cursor: pointer;
background-color: #7bb3ff;
}

#fast_select_elements .accordion_pane li {
font-size: 12px;
color: #000000;
cursor: pointer;
line-height: 18px;
z-index: 100;
margin-left: 4px;
margin-top:2px;
}

.sort-selected {
background: #A3E0FF;
}

.even-num {
background-color: #EDEDED;
}

#ec_model_editSqlProperties_tableInfo th{
	max-width: 120px;
	background-color: #f5f5f6;
	height: 28px;
    line-height: 28px;
	border-top: 1px solid #FFFFFF;
    border-bottom: 1px solid #e7e7e7;
    border-left: 0;
    border-right: 1px solid #e7e7e7;
}
#ec_model_editSqlProperties_tableInfo{
	border-left: 1px solid #e7e7e7;
	border-top: 1px solid #e7e7e7;
	color: #484848;
}
#ec_model_editSqlProperties_tableInfo td{
	max-width: 90px;
	height: 28px;
    line-height: 28px;
	border-top: 1px solid #FFFFFF;
    border-bottom: 1px solid #e7e7e7;
    border-left: 0;
    border-right: 1px solid #e7e7e7;
	overflow: hidden;
	white-space:nowrap;
    text-overflow:ellipsis;
}
#ec_model_editSqlProperties_tableInfo td input{
	max-width: 120px;
	height: 28px;
    line-height: 28px;
	border: 0;
	padding: 0px 3px;
	overflow: hidden;
	white-space:nowrap;
    text-overflow:ellipsis;
}
#ec_model_editSqlProperties_tableInfo select {
	width: 60px;
	height: 23px;
	line-height: 23px;
	margin: 0px 5px;
	border: none;
}
#ec_model_editSqlProperties_tableInfo td span{
	padding: 0px 0px 0px 2px;
}
#modelSQL {
 word-break: break-all;
}
.trSelected {
	background-color: #FFF5C2;
}   
.format_fix_input{
	min-width: 200px;
	overflow: hidden;
    border: 1px solid #C6d4e1;
	position: relative;
	height: 28px;
	box-sizing: border-box;
} 
.fix-input-readonly{
	height: 28px;
	box-sizing: border-box;
} 
.format_fix_input label {
	padding: 0px 3px;
	width:192px;
	color: #A0A0A0;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
	text-align:left;
	display: inline-block;
}

</style>
<@errorbar id="ec_model_editSql_formDialogErrorBar" />
<form  id="ec_model_editSql_form" onSubmit="javascript:return ec.model.beforesubmit();" action="/msService/ec/sqlmodel/save" validate="true" callback="ec.model.SqlCallback">
	<input type="hidden"  name="model.version" <#if model?? && model.version??>value="${model.version}"</#if> id="ec_model_editSql_form_model_version" />
	<input type="hidden"  name="model.code" <#if model?? && model.code??>value="${model.code}"</#if> id="ec_model_editSql_form_model_code"/>
	<input type="hidden"  name="model.entity.code" <#if model?? && model.entity.code??>value="${model.entity.code}"</#if> id="ec_model_editSql_form_model_entity_code" />
	<input type="hidden"  name="model.moduleCode"  <#if model?? && model.moduleCode??>value="${model.moduleCode}"</#if> id="ec_model_editSql_form_model_moduleCode" />
	<input type="hidden"  name="model.tableName"  <#if model?? && model.tableName??>value="${model.tableName}"</#if> />
	<input type="hidden"  name="model.type" value ="3" id="ec_model_editSql_form_model_type" />
	<input type="hidden"  name="model.sqlModel.properties" value="" id="ec_model_editSql_form_properties" />
	<input type="hidden"  name="model.dataType" value="1" />
	<input type="hidden"  name="model.sqlModel.version" <#if model?? && model.sqlModel?? && model.sqlModel.version??>value="${model.sqlModel.version}"</#if> />

	<input type="hidden" name="modelCode" value="${(model.code)!}" id="ec_model_editSql_form_modelCode"/>
	<div id="sqlModelDlg" >
		<@errorbar id="sqlModelDlgErrorBar"></@errorbar>
		<table id="ec_model_editSql_table" class="infoTable" cellpadding="0" cellspacing="0" border="0"  align="center" width="90%"  style="margin-top: 15px">
				<tr>
				    <td class="la"  style="color:#B30303;padding-right: 10px;" align="right">${getHtmlText('ec.model.modelName')} </td>
					<td>
						<div class="fix-input">
						    <#if model?? && (model.modelName)??>
                                <input type="text" name="model.modelName" readonly="true" maxlength="14" value="${model.modelName}" id="ec_model_edit_form_model_modelName" class="cui-noborder-input"/>
                            <#else>
                                <input type="text" name="model.modelName" maxlength="14" value="" id="ec_model_edit_form_model_modelName" class="cui-noborder-input"/>
                            </#if>
						</div>
					<span class="description">(${getHtmlText('ec.model.Modelname')})</span>
					</td>
				</tr>
				<tr>
					<td class="la"  style="color:#B30303;padding-right: 10px;" align="right">${getHtmlText('ec.model.name')}</td>
					<td>
					    <@international name="model.name" key="${(model.name)!}" moduleCode=module.artifact isNew=true maxLength=80></@international>
						<span class="description">(${getHtmlText('ec.model.modelshow')})</span>
					</td>
				</tr>
				<tr>
					<td class="la">${getHtmlText('ec.model.isMain')}</td>
					<td>
						<input type="checkbox" name="model.isMain" <#if (model?exists&&model.isMain== true) || (responseMap.firstIsMain)?? && (responseMap.firstIsMain)>checked="checked"</#if> value="true" <#if model??>disabled=true</#if> /></td>
						<input type='hidden' name='model.isMain' value='<#if (model?exists&&model.isMain== true) || (responseMap.firstIsMain)?? && (responseMap.firstIsMain)>true<#else>false</#if>'></td>
				</tr>
				<tr>
					<td class="la" style="color:#B30303;padding-right: 10px;">${getHtmlText('ec.model.sql')}
					<span id="showFunctionHelpinfo" class="baphelp-icon"></span>
<div id="showFunctionHelpinforef" style="display:none">
<p class="baphelp-info">通过--注释的方式区别数据库类型，不区分大小写;通用SQL不需要指明数据库类型</p>
    <p class="baphelp-example">范例</p>
<div class="baphelp-code">
<pre><code>--oracle
select id, name from table1
--sqlserver
select id, name from table2
--mariadb
select id, name from table3
</code></pre>
    </div>
    <p class="baphelp-hint">注意：注释行除数据库类型外请不要有其他内容，sql请换行之后再填写</p>
</div>
                    </div>
                        <script type="text/javascript">
                            $('#showFunctionHelpinfo').helptip({refElm: "#showFunctionHelpinforef", html: true , isCustom :false, width: 400 , title :"说明"});
                        </script>
                    </div>
                    </td>
					<td>
						<#if model?? && model.sqlModel?? && (model.sqlModel.modelSql)??>
						    <textarea name="model.sqlModel.sql" value="${model.sqlModel.modelSql}" cols="" rows="" id="modelSQL" class="cui-edit-textarea" style="width:95%;height:230px;margin-top:5px;" onchange="ec.model.changesql()">${model.sqlModel.modelSql}</textarea>
						<#else>
						    <textarea name="model.sqlModel.sql" cols="" rows="" id="modelSQL" class="cui-edit-textarea" style="width:95%;height:230px;margin-top:5px;" onchange="ec.model.changesql()"></textarea>
						</#if>
					</td>
				</tr>
				<tr>
					<td class="la">${getHtmlText('ec.model.description')}</td>
					<td>
					<textarea name="model.description" value="${(model.description)!}" cols="" rows="" id="modelDescripttion" class="cui-edit-textarea" style="width:95%;height:75px;margin-top:10px"></textarea>
					</td>
				</tr>
		</table>		
	</div>
</form>
<div id="sqlPropertiesDlg" style="display:none;">
</div>
<script type="text/javascript">
(function(){
		//注册命名空间
		CUI.ns("ec.model", "ec.ass");
		//获取字段列表
		ec.model.loadSqlProperties = function() {
			var url = "/msService/ec/sqlmodel/editSqlProperties";//?model.modelName=" + $("[name='model.modelName']").val() + "&model.isMain=" + $("[name='model.isMain']").prop('checked') + "&model.sqlModel.sql=" + encodeURIComponent($("[name='model.sqlModel.sql']").val()) + "&model.entity.code=" + $("[name='model.entity.code']").val();
			var params = {
					"model.code":$("[name='model.code']").val(),
					"model.modelName":$("[name='model.modelName']").val(),
					"model.entity.code":$("[name='model.entity.code']").val(),
					"model.sqlModel.sql":$("[name='model.sqlModel.sql']").val(),
					"model.isMain":$("[name='model.isMain']").prop('checked'),
					"model.type":$("[name='model.type']").val()
				};
				
			CUI('#sqlPropertiesDlg').load(url, params, function(){
				try {
					$("#sqlModelDlg").css('display','none'); // 隐藏SQL模型页面
					$("#sqlPropertiesDlg").css('display','block'); // 展示字段列表页面
					ec.model.loadSystemCodeInit();
					$("#buttonNext").hide();
					$("#buttonSave").show();
					$("#buttonBack").show();
				}catch(e){}
			});
		}
		
		ec.model.beforesubmit = function() {
            if (!ec.model.checkProperties()) {
                return false;
            }
            if (!ec.model.setProperties()) {
                return false;
            }
			return true;
		}
		
		// 验证名称和关联字段
		ec.model.checkProperties = function() {
		    var result = true;
			var displayNameFlag = true;
			var assFlag = true;
			var isHaveBussinessKey = false;
			var isHaveMainDisplay = false;
			var tab = document.getElementById("ec_model_editSqlProperties_tableInfo");
			for(var i = 1; i < tab.rows.length; i++){
				var displayName = tab.rows[i].cells[1].getElementsByTagName("INPUT")[0].value;
				var type = tab.rows[i].cells[2].getElementsByTagName("select")[0].value;
				var associatedPropertyCode = tab.rows[i].cells[3].getElementsByTagName("label")[0].getAttribute('associatedPropertyCode');
				var isBussinessKey = tab.rows[i].cells[4].getElementsByTagName("INPUT")[0].checked;
				var isMainDisplay = tab.rows[i].cells[5].getElementsByTagName("INPUT")[0].checked;
				if (displayName == "") {
					tab.rows[i].cells[1].getElementsByTagName("div")[0].style.border = "1px solid red";
					displayNameFlag = false;
				} else {
					tab.rows[i].cells[1].getElementsByTagName("div")[0].style.border = "0px";
				}
				if (type == 'OBJECT' && associatedPropertyCode == "") {
					tab.rows[i].cells[3].getElementsByTagName("div")[0].style.border = "1px solid red";
					assFlag = false;
				} else {
					tab.rows[i].cells[3].getElementsByTagName("div")[0].style.border = "0px";
				}
				if (isBussinessKey) {
				    isHaveBussinessKey = true;
				}
				if (isMainDisplay) {
                    isHaveMainDisplay = true;
                }
			}
			if (!displayNameFlag) {
				ec_model_editSql_formDialogErrorBarWidget.show("${getHtmlText('ec.property.displayname.isnotnull')}", "f");
				result = false;
			}
            if (!assFlag) {
                ec_model_editSql_formDialogErrorBarWidget.show("${getHtmlText('ec.model.notChoiceAssProperty')}", "f");
                result = false;
            }
            if (!isHaveBussinessKey) {
                ec_model_editSql_formDialogErrorBarWidget.show("${getHtmlText('ec.exceptions.NOBUSSINESSKEY')}", "f");
                result = false;
            }
            if (!isHaveMainDisplay) {
                ec_model_editSql_formDialogErrorBarWidget.show("${getHtmlText('ec.exceptions.NOMAINDISPLAY')}", "f");
                result = false;
            }
			return result;
		}

		//将字段参数拼接成JSON串
		ec.model.setProperties = function() {
			var properties = [];
			var tab = document.getElementById("ec_model_editSqlProperties_tableInfo");
			for(var i = 1; i < tab.rows.length; i++){
				var name = tab.rows[i].cells[0].getElementsByTagName("INPUT")[0].value;
				var displayName = tab.rows[i].cells[1].getElementsByTagName("INPUT")[0].value;
				var type = tab.rows[i].cells[2].getElementsByTagName("select")[0].value;
				var associatedPropertyCode = tab.rows[i].cells[3].getElementsByTagName("label")[0].getAttribute('associatedPropertyCode');
				var associatedType = tab.rows[i].cells[3].getElementsByTagName("label")[0].getAttribute('associatedType');
				var isMainAssociated = tab.rows[i].cells[3].getElementsByTagName("label")[0].getAttribute('isMainAssociated');
				var fetchMode = tab.rows[i].cells[3].getElementsByTagName("label")[0].getAttribute('fetchMode');
				var systemcodeValue = tab.rows[i].cells[3].getElementsByTagName("select")[0].value;
				var fillcontent;
				if (type == "SYSTEMCODE" && systemcodeValue != "" && systemcodeValue != null && systemcodeValue != undefined) {
					fillcontent = '{"fillType":"3","fillName":"${getText('foundation.infoSetCol.systemCode')}","fillContent":"' + systemcodeValue + '"}';
				} else {
					fillcontent = undefined;
				}
				var isBussinessKey = tab.rows[i].cells[4].getElementsByTagName("INPUT")[0].checked;
				var isMainDisplay = tab.rows[i].cells[5].getElementsByTagName("INPUT")[0].checked;
				var isInherent = tab.rows[i].cells[6].getElementsByTagName("INPUT")[0].checked;
				var code = tab.rows[i].cells[7].getElementsByTagName("INPUT")[0].value;
				var fieldType = tab.rows[i].cells[7].getElementsByTagName("INPUT")[1].value;
				var format = tab.rows[i].cells[7].getElementsByTagName("INPUT")[2].value;
				var columnName = tab.rows[i].cells[7].getElementsByTagName("INPUT")[3].value;
				properties.push({
					"code": code,
					"columnName": columnName,
					"displayName": displayName,
					"name":name,
					"type": type,
					"associatedProperty.code": associatedPropertyCode,
					"associatedType": associatedType,
					"isMainAssociated": isMainAssociated,
					"fetchMode": fetchMode,
					"fillcontent": fillcontent,
					"isBussinessKey": isBussinessKey,
					"isMainDisplay": isMainDisplay,
					"isInherent": isInherent
				});
			}
			var json = JSON.stringify(properties);
			$("input[name='model.sqlModel.properties']").val(json);
			return true;
		}

		//关联对象参数设置
		ec.model.formatDialog = function(obj) {
			var id = CUI(obj)[0].id;
			ec.model.dialog = new CUI.Dialog({
				title: "关联对象",
				modal: true,
				width : 360,
				height : 280,
				elementId: 'ec_model_format_dialog',
				buttons:[
					{	
						name:"${getHtmlText('ec.common.confirm')}",
						handler:function(){
							var module = $('#ec_select_module_sql:eq(0) option:selected').text();
							var entity = $('#ec_select_entity_sql:eq(0) option:selected').text();
							var model = $('#ec_select_model_sql:eq(0) option:selected').text();
							var property = $('#ec_select_property_sql:eq(0) option:selected').text();
							if (property == "" || property == "--") {
								ec_model_format_dialogDialogErrorBarWidget.show("${getHtmlText('ec.model.notChoiceAssProperty')}", "f");
								return false;
							}
							var splitWord = module + "-" + entity + "-" + model + "-" + property;
							$("#"+ id).prev().text(splitWord);
							$("#"+ id).prev().val(splitWord);
							$("#"+ id).prev().attr('title',splitWord);

							var moduleVal = $("select[id='ec_select_module_sql']").val();
							var entityVal = $("select[id='ec_select_entity_sql']").val();
							var modelVal = $("select[id='ec_select_model_sql']").val();
							var associatedPropertyCode = $("select[name='associatedProperty.code']").val();
							var associatedType = $("select[name='associatedType']").val();
							var isMainAssociated = $("input[name='isMainAssociated']").prop('checked');
							var fetchMode = $("select[name='fetchMode']").val();
							$("#"+ id).prev().attr({
								'judgeVal':'1',
								'moduleVal':moduleVal,
								'entityVal':entityVal,
								'modelVal':modelVal,
								'associatedPropertyCode':associatedPropertyCode,
								'associatedType':associatedType,
								'isMainAssociated':isMainAssociated,
								'fetchMode':fetchMode
							});
							this.close();							
						}
					},
					{	
						name:"${getHtmlText('ec.common.cancel')}",
						handler:function(){this.close()}
					}
				]
			});
			ec.model.dialog.show();
		    if ($("#"+ id).prev().attr('moduleVal') !== null && $("#"+ id).prev().attr('moduleVal') !== "") {
				var $label = $("#"+ id).prev();
				var moduleVal = $("#"+ id).prev().attr('moduleVal');
				var entityVal = $("#"+ id).prev().attr('entityVal');
				var modelVal = $("#"+ id).prev().attr('modelVal');
				var associatedPropertyCode = $("#"+ id).prev().attr('associatedPropertyCode');
				ec.ass.init(CUI(obj)[0]);
				$("select[id='ec_select_module_sql']").val(moduleVal);
				ec.ass.selectChange('entity',$("select[id='ec_select_module_sql']")[0],'module.code');
				$("select[id='ec_select_entity_sql']").val(entityVal);
				ec.ass.selectChange('model',CUI('#ec_select_entity_sql')[0],'entity.code');
				$("select[id='ec_select_model_sql']").val(modelVal);
				ec.ass.selectChange('property',CUI('#ec_select_model_sql')[0],'model.code');
				$("select[name='associatedProperty.code']").val(associatedPropertyCode);
			} else{
				var $select = $("#"+ec.model.dialog.id).find('select[class="cui-edit-field selectsql"]');
				$select.each(function(){this.options.length = 0;});
				ec.model.typeChanged($("#"+ id).parent().parent().parent().find('select[name="property.type"]')[0]);
			}
			var associatedType = $("#"+ id).prev().attr('associatedType');
			var isMainAssociated = $("#"+ id).prev().attr('isMainAssociated');
			var fetchMode = $("#"+ id).prev().attr('fetchMode');
			$("select[name='associatedType']").val(associatedType);
			if(isMainAssociated == 'true'){
				$("input[name='isMainAssociated']").prop('checked',true);
			}else{
				$("input[name='isMainAssociated']").removeAttr("checked");
			}
			$("select[name='fetchMode']").val(fetchMode);
		}
		
		/* 编码规则验证
		* @method ec.model.typeChanged
		* @public
		*/
		ec.model.typeChanged = function(obj){
			if('OBJECT' == CUI(obj).val()) {
				ec.ass.init(obj);
			} else if('SYSTEMCODE' == CUI(obj).val()) {
				ec.model.systemCodeInit(obj, null);
			} else {
				var $Element = $(obj.parentElement.parentElement);
				$Element.find('select[name="systemcode_select"]').css('display','none');
				$Element.find('div[class="format_fix_input"]').css('display', 'block');
				$Element.find('div[class="format_fix_input"]').css({'background-color': '#eee','cursor': 'not-allowed'});
				$Element.find('label').css({'cursor':'not-allowed', 'color':'#A0A0A0'});
				$Element.find('label').attr('associatedPropertyCode','');
				$Element.find('label').text("选择目标字段");
				$Element.find('input[class="cui-search-click"]').css('cursor', 'not-allowed');
				$Element.find('input[class="cui-search-click"]').prop('disabled',true);
			}
		};
		
		ec.model.loadSystemCodeInit = function(){
			var tab = document.getElementById("ec_model_editSqlProperties_tableInfo");
			for(var i = 1; i < tab.rows.length; i++){
				var typeObject = tab.rows[i].cells[2].getElementsByTagName("select")[0];
				if (typeObject.value == "SYSTEMCODE") {
					var value = tab.rows[i].cells[3].getElementsByTagName("select")[0].getAttribute('value');
					if (value!=null&&value!=""&&value!=undefined) {
						var fill = jQuery.parseJSON(value);
						value = fill['fillContent'];
					}
					ec.model.systemCodeInit(typeObject, value);
				}
			}
		}
		
		ec.model.systemCodeInit = function(obj, value){
			var $Element = $(obj.parentElement.parentElement);
			$Element.find('div[class="format_fix_input"]').css('display', 'none');
			$Element.find('select[name="systemcode_select"]').css('display','block');
			var objSelect = $Element.find('select[name="systemcode_select"]');
			CUI.ajax({
    			url: "/msService/ec/view/system-code-types?moduleCode=${module.code!}",
    			type: 'post',
    			async: false,
    			success: function(msg) {
    				$.each(msg, function(key, value) {
    					var objOptionGroup = document.createElement("optgroup");
    					if(key==='others'){
							objOptionGroup.setAttribute('label','${getText("ec.view.else")}');
						}else{
							objOptionGroup.setAttribute('label',key);
						}
    					CUI(objOptionGroup).appendTo(objSelect);
    					$.each(value,function(k,v){
							var objOption=document.createElement("option");
							objOption.innerHTML = v.name;
							objOption.value = k;
							objOption.id = v.id;
		                    objOption.setAttribute('companytype', v.companyType );
							CUI(objOption).appendTo(objOptionGroup);
						});
    				});
    			}
    		});
    		if(value!=null&&value!=""&&value!=undefined) {
				objSelect.val(value);
			}
		}
		
		ec.ass.init = function(obj){
			var $Element = $(obj.parentElement.parentElement);
			$Element.find('div[class="format_fix_input"]').css('display', 'block');
			$Element.find('select[name="systemcode_select"]').css('display','none');
			$Element.find('div[class="format_fix_input"]').css({'background-color': '#FFFFFF','cursor': 'default'});
			$Element.find('label').css({'cursor':'default', 'color':'#484848'});
			$Element.find('input[class="cui-search-click"]').css('cursor', 'pointer');
			$Element.find('input[class="cui-search-click"]').prop('disabled',false);
			$.ajax({
				async : false,
				type : "POST",
				url : "/msService/ec/module/list-select?module.code=${module.code!}",
				success : function(msg){
					var se = $('#ec_select_module_sql')[0];
					$('#ec_select_module_sql').empty();
					se.options.add(new Option('--',''));
					for(var i = 0 ; i < msg.length ; i ++){
						se.options.add(new Option(msg[i].nameInternational,msg[i].code));
					}
				}
			});
		}
		
		ec.ass.selectChange = function(type,obj,param){
			if(obj && $(obj).val()){
				var data = {};
				data[param] = $(obj).val();
				data["modelCode"]=$("[name='modelCode']").val();
				$.ajax({
					async: false,
					type : "POST",
					url : "/msService/ec/" + type + "/list-select",
					data : data,
					success : function(msg){
						var nextSelects = $(obj).parent().parent().nextAll().find("select[class='cui-edit-field selectsql']");
						nextSelects.each(function(){this.options.length = 0;});
						var n = $(obj).parent().parent().next().find("select[class='cui-edit-field selectsql']");
						var se = n[0];
						se.options.add(new Option('--',''));
						for(var i = 0 ; i < msg.length ; i ++){
							if(type == 'module' && (msg[i].code != 'sysbase_1.0' || msg[i].code != '${module.code!}')) {
								continue;
							}
							if(msg[i].nameInternational) {
								se.options.add(new Option(msg[i].nameInternational,msg[i].code));
							} else {
								se.options.add(new Option(msg[i].displayNameInternational,msg[i].code));
							}
							if(se.id=='ec_select_property_sql'){
								if(msg[i].name=='id'){
									se.options[se.options.length - 1].selected="selected";
								}
							}
						}
					}
				});
			} else {
				var nextSelects = $(obj).parent().parent().nextAll().find("select[class='cui-edit-field selectsql']");
				nextSelects.each(function(){this.options.length = 0;});
			}
		};
		
		ec.model.checkSqlExecutable = function() {

			return true;
		}
		
		/* 编码规则验证
		 * @method ec.model.codeValidate
		 * @public
		 */
		ec.model.codeValidate = function() {
			var returnFlag = false;
			//模型名
		 	var reg = /^[A-Z]{1}[a-z]{1}[a-zA-Z0-9]+$/;
		 	var $ModelName = $("#ec_model_editSql_form input[name='model.modelName']");
		 	$ModelName.val($ModelName.val().trim());
		 	if ($ModelName.val() == "" || $ModelName.val() == null || $ModelName.val() == undefined) {
		 		ec_model_editSql_formDialogErrorBarWidget.show("${getHtmlText('ec.model.modelname.required')}", "f");
				return false;
		 	}
			if (!reg.test($ModelName.val())) {
				ec_model_editSql_formDialogErrorBarWidget.show("${getHtmlText('ec.model.formatmessage')}", "f");
				return false;
			}

			//显示名称
			var $Name = $("#ec_model_editSql_form input[name='model.name']");
			$Name.val($Name.val().trim());
			if (!($Name.val() !== "" && $Name.val() !== null && $Name.val() !== undefined)) {
				ec_model_editSql_formDialogErrorBarWidget.show("${getHtmlText('ec.model.name.required')}", "f");
				return false;
			}

			//新建SQL进行校验
			var $SQLName = $("#ec_model_editSql_form textarea[name='model.sqlModel.sql']").val();
			if($SQLName != "" && $SQLName != null && $SQLName != undefined) {
				$.ajax({
					type: "POST",
					async: false,
					url: "/msService/ec/sqlmodel/checkSqlModel",
					data: {
						"model.code":$("[name='model.code']").val(),
						"model.modelName":$("[name='model.modelName']").val(),
						"model.isMain":$("[name='model.isMain']").prop('checked'),
						"model.sqlModel.sql":$("[name='model.sqlModel.sql']").val(),
						"model.entity.code":$("[name='model.entity.code']").val(),
						"model.type": $("[name='model.type']").val()
					},
					dataType: "json",
					success: function(result) {
						if (result.success == true) {
							returnFlag = true;
						}
					},
					error: function(xhr) {
						ec_model_editSql_formDialogErrorBarWidget.show(JSON.parse(xhr.responseText).exceptionMsg, "f");
						returnFlag = false;
					}
				});
			} else {
				ec_model_editSql_formDialogErrorBarWidget.show("${getHtmlText('ec.model.sqlNotNull')}", "f");
				return false;
			}

			return returnFlag;
		}
		
		function upperFirstLetter(str){
			return str.replace(/\b\w+\b/g,function(word){
				return word.substring(0,1).toUpperCase()+word.substring(1);
			});
		}
		$("[name='model.modelName']").change(function(){
			var modelName = $("[name='model.modelName']").val();
			var entityCode = "${entity.code}";
			if(null != modelName && "" != modelName){
				modelName = upperFirstLetter(modelName);
				$("[name='model.modelName']").val(modelName);
				$.ajax({
					type : "POST",
					url : "/msService/ec/model/formatTableName",
					data : {"model.modelName":$("[name='model.modelName']").val(),"entity.code":entityCode},
					success : function(msg){
						if(null != msg.tableName && "" != msg.tableName){
							$("[name='model.tableName']").val(msg.tableName);
						}
					}
				});
			}
		});
		//表格选中行添加背景色
		$("#ec_model_editSqlProperties_tableInfo tbody tr").click(function() {
			$(this).addClass('trSelected').siblings().removeClass('trSelected');
		});
		$("select[name='associatedType']").change(function(){
			var associatedType = $("select[name='associatedType']").val();
			if(associatedType == "1"){
				$("select[name='fetchMode'] option[value='SUBSELECT']").remove();
			}else if(associatedType == "2" && $("select[name='fetchMode'] option[value='SUBSELECT']").length == 0){
				$("select[name='fetchMode']").append('<option value="SUBSELECT">${getText('ec.property.fetchMode.SUBSELECT')}</option>');
			}
		});
		$("select[name='associatedType']").trigger('change');
		
		ec.model.checkReceive = function(obj) {
			var inputName = $(obj).attr("name");
		    $("input[name='" + inputName + "']").each(function () {
		        if (this != obj) {
		            $(this).attr("checked", false);
		        } else {
		            if ($(this).prop("checked")) {
		                $(this).attr("checked", true);
		            } else {
		            	$(this).attr("checked", false);
		            }
		        }
		    });
		};
		
		ec.model.changesql = function() {
			var sql = $("[name='model.sqlModel.sql']").val();
			sql = sql.replace(/^\s+|\s+$/g,'');
			$("[name='model.sqlModel.sql']").val(sql);
		}
		
})();
CUI(function(){
	function submitBapForm(){//电子签名成功之后出现进度条并提交表单
		var ecFormFlag = false;
		var retrialFormFlag = false;
		if(ecFormFlag && ( $('#ec_model_editSql_form').parents('.ewc-dialog-blove').length > 0 || ( window != top && !window.frameElement.getAttribute( 'v3_tab_iframe' ) ) ) ) {
			// dialog不出进度条
			ecFormFlag = false;
		}
		ecFormFlag = (ecFormFlag || retrialFormFlag);
		
		//前台验证通过之后出进度条
		CUI.Dialog.toggleAllButton('ec_model_editSql_form',true,ecFormFlag, true);
	// TODO 延时600ms，让页面上已触发 未执行完成的事件处理完成，临时性解决办法
	setTimeout(function(){
		
			// 延迟保存数据, 解决onchange事件无法触发问题
			var formId = 'ec_model_editSql_form';
			var ecformflag = false;
			
			
		    $('input[type="text"]','#'+formId).each(function(){
		        var v=$.trim($(this).val());
		        $(this).val(v);
		    });
			var files = $('input[type="file"]', '#' + formId);
			if(ecformflag || (files!=null&&files.length>0)) {
				ajaxFileUpload(CUI('#'+formId).attr('action'),formId);
			} else {
			
			var postData = CUI('#'+formId).serialize();
			CUI.ajax({
				url : CUI('#'+formId).attr('action'),
				type : 'POST',
				dataType : 'json',
				data : postData,
				error : function(XMLHttpRequest, textStatus, errorThrown){
					//console.log("jqXHR=%o,textStatus=%o,errorThrown=%o", XMLHttpRequest, textStatus, errorThrown );
					if (XMLHttpRequest.status==401) {
						//showLoginDialog();
						return ;
					}
					var msg = CUI.parseJSON(XMLHttpRequest.responseText);
					var errorMsgs = "";
					CUI.each(msg.items,function(index,item){
						if(index.indexOf('.id') != -1 && index.indexOf('.id')+3 == index.length) {
							$("#ec_model_editSql_form *[name^='"+(index.substring(0,index.length -3))+"'][type!='hidden']").each(function(){
								if($(this).parents('td[nullable=false]').length > 0) {
									showErrorField($(this));
								}
							});
						} else {
							var field = CUI("#ec_model_editSql_form *[name='"+index+"']");
							if(field.prop('type') && field.prop('type').toUpperCase() == 'HIDDEN' && field.next() && field.next().length > 0) {
		                		showErrorField(field.next());
		                	} else {
		                		showErrorField(field);
		                	}
						}
						CUI("#ec_model_editSql_form *[name='"+index+"']").first().focus();
						for(var i = 0 ; i < item.length ; i++){
							errorMsgs += item[i] + '<br/>';
						}
					});
					CUI.each(msg.actionErrors,function(index,item){
						errorMsgs += item + '<br/>';
					});
					if(msg.exceptionMsg!=null&&msg.exceptionMsg!=""){
						errorMsgs += msg.exceptionMsg + '<br/>';
					}
					var oErrorWidget = null;
					oErrorWidget = ec_model_editSql_formDialogErrorBarWidget;
					if ( $(oErrorWidget.errorInfoBar).closest('.ewc-dialog-el').length>0 ) {
						oErrorWidget.show(errorMsgs);// 对话框内错误提示显示顶部横条
					}	else {
						oErrorWidget.showMessage(errorMsgs);// 编辑界面错误提示显示右上
					}					
					if(CUI.Dialog){
						CUI.Dialog.toggleAllButton('ec_model_editSql_form', true);
				    }
				},
				success : function(msg){
					window.onbeforeunload = null;
					if(window.containerLoadPanelWidget) {
						setTimeout(function(){closeLoadPanel();}, 500);
					}
					ec.model.SqlCallback(msg,postData);
				}
			});
		}
	}, 600);
		return false;
	}


	CUI('#ec_model_editSql_form').unbind('submit.bapForm').bind('submit.bapForm',function(){
		//每次提交时先隐藏报错信息
		try{
		}catch(e){
		
		}
		// 清除错误标红
		try{clearErrorLabels();}catch(e){}
		var ecFormFlag = false;
		var retrialFormFlag = false;
		if(ecFormFlag && ( $(this).parents('.ewc-dialog-blove').length > 0 || ( window != top && !window.frameElement.getAttribute( 'v3_tab_iframe' ) ) ) ) {
			// dialog不出进度条
			ecFormFlag = false;
		}
		ecFormFlag = (ecFormFlag || retrialFormFlag);
		
		
		//禁用所有按钮
		//CUI("body").one("click", function(event){
		//if(CUI.Dialog) CUI.Dialog.toggleAllButton('ec_model_editSql_form',true,ecFormFlag);
		//});
			//if(!validateForm_ec_model_editSql_form())	return false;
		$('#ec_model_editSql_form').trigger('beforeSubmit');
		if($('#ec_model_editSql_form input[name="operateType"]').val() == "submit"){
			var deploymentId=$('#ec_model_editSql_form input[name="deploymentId"]');
			var buttonCode=$('#ec_model_editSql_form input[name="buttonCode"]');
			var namespace=$('#ec_model_editSql_form input[name="namespace"]');
			if( deploymentId.length > 0 && deploymentId.val() != undefined && deploymentId.val() != ''){
				var signatureInfo=signatureUtil.getSignatureInfo(true,'',deploymentId.val(),'','')//判断是否需要进行电子签名
				if(signatureInfo[0] != '') {
					var cancelItem = $('input[name="workFlowVarStatus"]');
					if(cancelItem.val() != "cancel") {
						signatureUtil.showSignatureDialog(signatureInfo[1],signatureInfo[0],'ec.flow.submit','','ec_model_editSql_form');
						$('a[name="signature_signatureDialog_showDialog_0"]').unbind('click').bind('click',function(){singleSignOk('','ec_model_editSql_form','ec.flow.submit',false)});
					}
					else {
						signatureUtil.showSignatureDialog(signatureInfo[1],signatureInfo[0],'ec.edit.remove','','ec_model_editSql_form');
						$('a[name="signature_signatureDialog_showDialog_0"]').unbind('click').bind('click',function(){singleSignOk('','ec_model_editSql_form','ec.edit.remove',false)});
					}
					
				}
				else {
					submitBapForm();
				}
			}
			else if( buttonCode.length > 0 && buttonCode.val() != undefined && buttonCode.val() != ''){
				var signatureInfo=signatureUtil.getSignatureInfo(false,buttonCode.val());
				if(signatureInfo[0] != '') {
					if(namespace.length > 0 && namespace.val() != undefined && namespace.val() != '') {
						parent.signatureUtil.showSignatureDialog(signatureInfo[1],signatureInfo[0],buttonCode.val(),namespace.val(),"ec_model_editSql_form",false,'');
						if(signatureInfo[0] == 'singleSign') {
							parent.$('a[name="signature_signatureDialog_showDialog_0"]').unbind('click').bind('click',function(){singleSignOk(namespace.val(),'ec_model_editSql_form',buttonCode.val(),false)});
						}
						else {
							setTimeout(function(){parent.$('#secondSignerOK').unbind('click').bind('click',function(){doubleSignOK(this,namespace.val(),'ec_model_editSql_form',buttonCode.val(),false)});},2000);
						}	
					}
					else {
						signatureUtil.showSignatureDialog(signatureInfo[1],signatureInfo[0],buttonCode.val(),'',"ec_model_editSql_form",false,'');
						if(signatureInfo[0] == 'singleSign') {
							$('a[name="signature_signatureDialog_showDialog_0"]').unbind('click').bind('click',function(){singleSignOk('','ec_model_editSql_form',buttonCode.val(),false)});
						}
						else {
							setTimeout(function(){$('#secondSignerOK').unbind('click').bind('click',function(){doubleSignOK(this,'','ec_model_editSql_form',buttonCode.val(),false)});},2000);
						}
					}
				}
				else {
					submitBapForm();	
				}
			}
			else {
				submitBapForm();
			}
		}
		else {
			submitBapForm();
		}
		return false;
	});


});
</script>