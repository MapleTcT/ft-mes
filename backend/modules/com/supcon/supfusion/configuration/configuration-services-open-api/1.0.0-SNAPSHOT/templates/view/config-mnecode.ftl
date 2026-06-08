<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
	<title>${getText('mnecode')}</title>
	<@mainjs /><@maincss />
	<link rel="stylesheet" type="text/css" href="/bap/static/ec/css/style.css" />
	<link rel="stylesheet" type="text/css" href="/bap/static/ec/css/view.css" />
	<@loadpanel />
	<@adpSkin />
	<style type="text/css">
		.grid-s4m0 .main-wrap { margin-left: 160px; }
		.grid-s4m0 .col-sub { width: 150px; margin-left: -100%; }
		.col-sub { margin-left:2px;margin-top:8px;width: 140px; height:98%;float:left;background-color:white;border:1px solid #3C7FB1}
		#form_select_elements{border:1px solid #3C7FB1;margin-top:10px;}
		#form_select_elements h2 {margin:0;	padding:5px 0;text-align:center;font-size:13px;font-weight:normal;border:1px solid #fff;border-bottom:1px solid #ddd;cursor:pointer;background-color: #3C7FB1;}
		#form_select_elements h2.current {cursor:default;}
		#form_select_elements .accordion_pane {	border:1px solid #fff;	border-width:0 2px;	display:none;padding:10px;font-size:12px;}
		#form_select_elements .accordion_pane li {font-size:12px;color:#999;cursor:pointer;line-height:18px;}	
		
		#main_design_container{padding:10px 10px 0 0;}
				
		.datagrid-hd{height: 23px;}
		.editeventli {width:175px;text-align: right;}
		a.normala{text-decoration: underline;padding-left:2px;cursor:pointer;color:blue; display:inline-block;height:15px;overflow:hidden;}
		
		table td{padding:0 3px;}
		a.help-link {
			display: inline-block;
			width: 15px;
			height: 15px;
			background: url(/bap/static/css/edit_20150318.png) 0px -3553px no-repeat;
		}
	</style>
	<script type="text/javascript">
		CUI(function(){
			<#--$('#main_container table').sortable({
				items : $('tr:gt(0)'),
				cursor : "move"
			});-->
			CUI.ajax({
				url : '/msService/ec/view/getMneCode?view.code=${(view.code)!}${isProj?string("&isProj=true","")}',
				type: 'post',
				success: function(ev) {
					if(ev != null) {
						$('input[name="ev.code"]').val(ev.code);
						$('input[name="ev.version"]').val(ev.version);
						if(ev.configMap != null && ev.configMap.layout != null) {
							$('#showNumber').val((ev.configMap.layout.sections)[0].mnecodeset.showNumber);
							if((ev.configMap.layout.sections)[0].mnecodeset.isMore) {
								$('#isMore').prop('checked',true);
							} else {
								$('#isMore').prop('checked',false);
							}
							if((ev.configMap.layout.sections)[0].mnecodeset.isTransCondition) {
								$('#isTransCondition').prop('checked',true);
								$('#conditionArea').prop("disabled",false);
								var conditionStr = (ev.configMap.layout.sections)[0].mnecodeset.conditionContent;
								$('#conditionArea').val(conditionStr.replace(/@@backslash_quote@@/gi, "\\").replace(/@@wrap_quote@@/gi, "\n").replace(/@@double_quote@@/gi, "\"").replace(/@@single_quote@@/gi, "'").replace(/@@space_quote@@/gi, " ").replace(/@@wcard_quote@@/gi, "%").replace(/@@equal_quote@@/gi, "="));
							} else {
								$('#isTransCondition').prop('checked',false);
								$('#conditionArea').prop("disabled",true);
								//$('#conditionArea').val('');
							}
							if((ev.configMap.layout.sections)[0] != null) {
								$.each((ev.configMap.layout.sections)[0].cells,function(index,item){
									var key = item['key'];
									var tr = $('#listTable tr[key="' + key + '"]');
									tr.attr("cellCode",item['cellCode']);
									$('input[name="checkshow"]',tr).prop('checked',true);
								});
							}
						}
					} 
				}
			});
			
			$('#isTransCondition').bind('click',function(e){
				var self = this;
				if(this.checked) {
					$('#conditionArea').prop("disabled",false);
					$('#custCond').attr('disabled', 'true');
				}else {
					if(e && e.preventDefault) {
					　　e.preventDefault();
					} else {
					　　window.event.returnValue = false;
					}
					var json = $('#customerJsonString').val();
					CUI.Dialog.confirm("${getHtmlText('ec.view.leavehandwriting')}", function(){	
						self.checked = false;
						$('#conditionArea').val('');
						$('#conditionArea').prop("disabled",true);
						$('#custCond').removeAttr('disabled');
						var datas = {"dgQueryCond" : json};
						CUI.ajax({
							url: "/msService/ec/customerCon/transtoSql",
							type: 'post',
							async: false,
							data: datas,
							success: function(msg) {
									if(msg.success==false || msg.data==null){
										dg.advQuery.showMsg('');
									}else{
										dg.advQuery.showMsg(msg.data);
									}
							}
						});
					});
				}
			});
		});
	</script>
</head>
<body class="ec-config-page view-mnecode" layoutCode="${(ev.configMap.layout.layoutCode)!ecCodeInit('layout')}">
<div id="design-top">
	</div>
	  <#if (getCurrent('company'))??>
	  	<#assign company = (getCurrent('company'))!>
	  </#if>	
	  <@errorbar id="workbenchErrorBar" offsetY=83 />
	  <div></div>
		<div class="col-top">
			<div>
				<h3 style="color:#3A70AA;padding-top:7px;padding-left:15px;">${(view.assModel.entity.entityName)!}-${getHtmlText('ec.project.view_setting')}-${(view.name)!}</h3>
				<#if TempProjView?? && TempProjView.projFlag?? && TempProjView.projFlag>
				<h3 style="color:red;padding-top:7px;padding-left:5px;float:left">
					(${getText("ec.view.inherited")}-<#if TempProjView.projEnabled?? && TempProjView.projEnabled>${getText("ec.view.inherited.projenabled")}<#if !TempProjView.publishTime??>${getText("ec.engine.publishstste.not")}<#elseif (TempProjView.publishTime?long - TempProjView.modifyTime?long)?abs lt 1000>${getText("ec.engine.publishstste.published")}<#else>${getText("ec.engine.publishstste.waittopublish")}</#if><#else>${getText("ec.view.inherited.projenabled.not")}</#if>)
				</h3>
				</#if>
				<span style="color: #FFFFFF;font-size: 15px;font-weight: bold;margin: 8px 15px;float:right">
            		<a href="/help/" target="_blank" title="帮助文档" class="help-link"></a>
            	</span>
			</div>
		</div>
	   <div style="border:#CCC 1px dashed;margin:5px 5px 0 5px;padding:10px;overflow: auto;height:35px;" class="view-mnecode-config">
	   		<table id="mneCodePropertySet" cellpadding="0" cellspacing="0" border="0" align="right" style="margin-top: 10px;width:100%">
			<tr>
				<input type="hidden" name="ev.code" value="" />
				<input type="hidden" name="ev.version" value="" />
				<td class="la" width="15%" align="right" padding-right="10px">${getHtmlText('ec.view.mnecode.count')}：</td>
				<td class="co" width="20%"><input type="text" id="showNumber" class="cui-edit-field" style="width:50%" value="5"/></td>
				<td class="la" width="15%" align="right" padding-right="10px">${getHtmlText('ec.view.mnecode.more')}：</td>
				<td class="co" width="20%"><input type="checkbox" id="isMore" style="margin-left:10px;"/></td>
				<td class="la" width="12%" align="right" padding-right="10px"></td>
				<td class="co" width="20%"></td>
			</tr>
			<tr style="display:none">
				<td class="la" width="12%" align="right" style="padding-right:10px;">${getHtmlText('ec.view.handwritingCondition')}：</td>
				<td class="co" width="20%" style=""><input type="checkbox"  id="isTransCondition" /></td>
				<td class="la" width="15%" align="right" style="padding-right:10px;">${getHtmlText('ec.view.conditioncontent')}：</td>
				<td class="co" width="20%">
					<textarea id="conditionArea" class="cui-edit-textarea" style="width:100%;height:40px;margin-left:10px;" disabled="disabled"></textarea>
				</td>
			</tr>
			<tr style="display:none">
				<td class="la" width="12%" align="right" style="padding-right:10px;">
					
				</td>
				<td class="co" width="20%" style="">
					<button id="custCond" class="Dialog_button btn_pointer" type="button" onclick="dg.advQuery.showAdv()" style="margin-right:0px">${getHtmlText('ec.view.customerCondition')}</button>
				</td>
			</tr>
			</table>
	   </div>
	  <div style="border:#CCC 1px dashed;margin:5px 5px 0 5px;padding:10px;overflow: auto;height:470px;" id="main_container">
		<table width="100%" align="center" bgcolor="#CCCCCC" cellspacing="1" id="listTable" sectionCode="${(ev.configMap.layout.section.sectionCode)!ecCodeInit('section')}">
			<input type="hidden" id="delCellIds" name="delCellIds" />
			<tr bgcolor="#EEEEEE" style="font-weight:bold;">
				<td height="20" align="center" width="80">${getHtmlText('ec.view.mnecode.layer')}</td>
				<td align="center" width="150">${getHtmlText('ec.view.fieldname')}</td>
				<td align="center" width="50">${getHtmlText('ec.view.mnecode.data')}</td>
			</tr>
				<#if view.assModel??>
					<#assign properties = (view.assModel.properties)![]>
					<#assign associatedInfos = (view.assModel.associatedInfos)![]>
					<#if properties??>
						<#list properties as property>
							<#if (property.type)?? && !(property.isCustom!false) && ((property.type)?string == "TEXT" || (property.type)?string == "BAPCODE") || (property.type)?string == "SUMMARY">
								<#assign key = property.name>
								<#if relativeMap?? && relativeMap[property.code]??>
								<#assign relativeName = relativeMap[property.code]>
								</#if>
									<tr class="nor" bgcolor="#FFFFFF" key="${key}" mnecode="${(property.isUsedMneCode)?string('true','false')}" propertyCode="${(property.code)!}" showType="${(property.type)!}" showFormat="${(property.format)!}" layRec="<#if (property.associatedProperty)?? && relativeName??>${relativeName}<#else>${key}</#if>">
										
										<td height="20" align="center"><#if !(property.associatedProperty)??><input style="border:none;" type="checkbox" name="checkshow" onclick="countDelCellIds(this);" /></td></#if>
										<td <#if ai??> " style=\"padding-left:20px;\"" <#else> "" </#if>>
											<#if (property.associatedProperty)?? && assMap[property.code]??>  "<a href='#' class='normala' onclick="toggleAss(this)" title="${getText('ec.view.associatepro')}"><span style="font-size:15px;width:13px;font-weight:bold;display:block;">-</span></a>" <#else> </#if>
											<span class="displayName">${getHtmlText('${property.displayName}')}</span>
										</td>
										<td align="center"><#if (property.isUsedMneCode)?? && (property.isUsedMneCode)>${getHtmlText('ec.select.yes')}<#else>${getHtmlText('ec.select.no')}</#if></td>
									</tr>
									<#if assMap?? && assMap[property.code]??>
										<#list assMap[property.code] as p>
											<#if (p.type)?? && ((p.type)?string == "TEXT" || (p.type)?string == "BAPCODE" || (p.type)?string == "SUMMARY") && !(p.isCustom!false)>
												<tr class="ass" bgcolor="#FFFFCC" key="${(property.name)!}.${(p.name)!}" mnecode="${(p.isUsedMneCode)?string('true','false')}" propertyCode="${(p.code)!}" showType="${(p.type)!}" showFormat="${(p.format)!}" assTar="${(p.associatedProperty.code)!''}" assOrg="${(property.code)!}" layRec = "<#if relativeName??>${relativeName}-${(p.name)!}<#else>${(p.name)!}</#if>">
													<td height="20" align="center"><input style="border:none;" type="checkbox" name="checkshow" onclick="countDelCellIds(this);"/></td>
													<td style="padding-left:20px;"><span class="displayName">${getHtmlText('${(p.displayName)!}')}</span></td>
													<td align="center"><#if (p.isUsedMneCode)?? && (p.isUsedMneCode)>${getHtmlText('ec.select.yes')}<#else>${getHtmlText('ec.select.no')}</#if></td>
												</tr> 
											</#if>	
										</#list>
									</#if>
							</#if>
						</#list>
						<#list properties as property>
							<#if (property.type)?? && (property.type)?string == "OBJECT" && !(property.isCustom!false)>		
								<#assign key = property.name>
								<#if relativeMap?? && relativeMap[property.code]??>
								<#assign relativeName = relativeMap[property.code]>
								</#if>
								<tr class="nor object" bgcolor="#BDBDBD" key="${key}" mnecode="${(property.isUsedMneCode)?string('true','false')}" propertyCode="${(property.code)!}" showType="${(property.type)!}" showFormat="${(property.format)!}" layRec="<#if (property.associatedProperty)?? && relativeName??>${relativeName}<#else>${key}</#if>">
									
									<td height="20" align="center"><#if !(property.associatedProperty)??><input style="border:none;" type="checkbox" name="checkshow"/></td></#if>
									<td <#if ai??> " style=\"padding-left:20px;\"" <#else> "" </#if>>
										<#if (property.associatedProperty)?? && assMap[property.code]??>  "<a href='#' class='normala' onclick="toggleAss(this)" title="${getText('ec.view.associatepro')}"><span style="font-size:15px;width:13px;font-weight:bold;display:block;">-</span></a>" <#else> </#if>
										<span class="displayName">${getHtmlText('${property.displayName}')}</span>
									</td>
									<td align="center"><#if (property.isUsedMneCode)?? && (property.isUsedMneCode)>${getHtmlText('ec.select.yes')}<#else>${getHtmlText('ec.select.no')}</#if></td>
								</tr>
								<#if assMap?? && assMap[property.code]??>
									<#list assMap[property.code] as p>
										<#if (p.type)?? && ((p.type)?string == "TEXT" || (p.type)?string == "BAPCODE" || (p.type)?string == "SUMMARY") && !(p.isCustom!false)>
											<tr class="ass" bgcolor="#FFFFCC" key="${(property.name)!}.${(p.name)!}" mnecode="${(p.isUsedMneCode)?string('true','false')}" propertyCode="${property.code}||${(p.code)!}" showType="${(p.type)!}" showFormat="${(p.format)!}" assTar="${(p.associatedProperty.code)!''}" assOrg="${(property.code)!}" layRec = "<#if relativeName??>${relativeName}-${(p.name)!}<#else>${(p.name)!}</#if>">
												<td height="20" align="center"><input style="border:none;" type="checkbox" name="checkshow"/></td>
												<td style="padding-left:20px;"><span class="displayName">${getHtmlText('${(p.displayName)!}')}</span></td>
												<td align="center"><#if (p.isUsedMneCode)?? && (p.isUsedMneCode)>${getHtmlText('ec.select.yes')}<#else>${getHtmlText('ec.select.no')}</#if></td>
											</tr> 
										</#if>	
									</#list>
								</#if>
							</#if>
						</#list>
					</#if>
				</#if>
		</table>
	</div>
	
	<#if !isReadOnlyMode>
	<div id="design-button" style="height:30px;width:100%;float:right;text-align:right;margin-top:20px">
		<button class="Dialog_button btn_pointer btn-primary" onclick="save()" type="button" style="margin-right:20px">${getHtmlText('common.button.save')}</button>
	</div>
	</#if>
	<script type="text/javascript">
		function toggleAss(obj){
			var o = $(obj);
			if(o.text() == "-"){
				$("span",o).text("+");
			}else{
				$("span",o).text("-");
			}
			o.parent().parent().nextUntil(".nor").toggle();
		};
		
		function countDelCellIds(obj) {
			if(obj.checked == false) {
				var cellIds = $('#delCellIds').val();
				if($(obj).parent().parent().attr("cellCode")) {
					cellIds += (cellIds === "") ? selected.attr("cellCode") : "," + selected.attr("cellCode");
				}
				$('#delCellIds').val(cellIds);
			}
		}
		
		function build_xml() {
			var xmlStr = '<?xml version="1.0" encoding="UTF-8"?><config>';
			var layoutCode = '';
			if($('body').attr('layoutCode')){
				layoutCode = $('body').attr('layoutCode');
			}else{
				layoutCode = "layout_" + new Date().getTime() +'_'+ Math.round(Math.random(1)*10000);
			}
			xmlStr += '<layout><layoutCode>'+layoutCode+'</layoutCode>';
			xmlStr += '<sections><list><list-item>';
			var sectionCode = '';
			if($('#listTable').attr('sectionCode')){
				sectionCode = $('#listTable').attr('sectionCode');
			}else{
				sectionCode = "section_" + new Date().getTime() +'_'+ Math.round(Math.random(1)*10000);
			}
			xmlStr += '<sectionCode>'+sectionCode+'</sectionCode>';//section code
			xmlStr += '<regionType><![CDATA[MNECODE]]></regionType>';//section的区域类型
			xmlStr += '<cells><list>';
			var fieldXml = '<fieldConfig><fields><list>';
			$('#main_container input[name="checkshow"]:checked').each(function(index){
				var o = $(this);
				var td = o.parent();
				var tr = td.parent();
				xmlStr += '<list-item>';
				var cellCode = '';
				if(tr.attr('cellCode')){
					cellCode = tr.attr('cellCode');
				}else{
					cellCode = "cell_" + new Date().getTime() +'_'+ Math.round(Math.random(1)*10000);
				}
				xmlStr += '<cellCode>'+cellCode+'</cellCode>';
				xmlStr += '<regionType><![CDATA[MNECODE]]></regionType>';
				/*xmlStr += '<key><![CDATA['+$.trim(tr.attr("key"))+']]></key><layRec><![CDATA[' + tr.attr("layRec") + ']]></layRec>';
				var showFormat =tr.attr('showFormat');
				if(showFormat){
					xmlStr += '<showFormat><![CDATA[' + showFormat + ']]></showFormat>';
				}
				var propertyCode = tr.attr('propertyCode');
				if(propertyCode){
					xmlStr += '<propertyCode><![CDATA[' + propertyCode + ']]></propertyCode>';
				}
				var showType = tr.attr('showType');
				if(showType){
					xmlStr += '<showType><![CDATA[' + showType+']]></showType>';
				}
				if(tr.hasClass('ass')){
					xmlStr += '<ass><tar><![CDATA[' + tr.attr('assTar') + ']]></tar><org><![CDATA[' + tr.attr('assOrg') + ']]></org></ass>';
				}*/
				xmlStr += '</list-item>';
				
				//分离
				fieldXml += '<list-item>';
				fieldXml += '<cellCode>'+cellCode+'</cellCode>';
				fieldXml += '<regionType><![CDATA[MNECODE]]></regionType>';
				fieldXml += '<key><![CDATA['+$.trim(tr.attr("key"))+']]></key><layRec><![CDATA[' + tr.attr("layRec") + ']]></layRec>';
				var showFormat =tr.attr('showFormat');
				if(showFormat){
					fieldXml += '<showFormat><![CDATA[' + showFormat + ']]></showFormat>';
				}
				var propertyCode = tr.attr('propertyCode');
				if(propertyCode){
					fieldXml += '<propertyCode><![CDATA[' + propertyCode + ']]></propertyCode>';
				}
				var showType = tr.attr('showType');
				if(showType){
					fieldXml += '<showType><![CDATA[' + showType+']]></showType>';
				}
				if(tr.hasClass('ass')){
					fieldXml += '<ass><tar><![CDATA[' + tr.attr('assTar') + ']]></tar><org><![CDATA[' + tr.attr('assOrg') + ']]></org></ass>';
				}
				fieldXml += '</list-item>';
			});
			fieldXml += '</list></fields></fieldConfig>';
			xmlStr += '</list></cells><mnecodeset><showNumber><![CDATA['+ $('#showNumber').val() + ']]></showNumber><isMore><![CDATA[' + $('#isMore').prop('checked')+ ']]></isMore><isTransCondition><![CDATA[' + $('#isTransCondition').prop('checked')+ ']]></isTransCondition><conditionContent><![CDATA[' + $('#conditionArea').val() + ']]></conditionContent></mnecodeset></list-item></list></sections></layout></config>';
			xmlStr += fieldXml;
			return xmlStr;
			
		}
		
		function save() {
			var flag = false;
			$('#main_container input[name="checkshow"]:checked').each(function(index){
				var o = $(this);
				var td = o.parent();
				var tr = td.parent();
				if(tr.attr('mnecode') == 'true') {
					flag = true;
					return;
				}
			});
			if(flag == false) {
				workbenchErrorBarWidget.showMessage("${getHtmlText('ec.view.mnecode.alert')}");
				return false;
			}
			CUI.Dialog.toggleAllButton('design-button',true,true);
			var config = this.build_xml();
			var index = config.indexOf("<fieldConfig><fields>");
			var mne_config = config.substring(0,index);
			var fieldConfig = config.substring(index);
			$.ajax( {
				type : 'POST',
				url : '/msService/ec/view/save-config${isProj?string("?isProj=true","")}',
				data : {
					"ev.code" : $('input[name="ev.code"]').val(),
					"ev.view.code" : '${(view.code)!}',
					"ev.version" : $('input[name="ev.version"]').val(),
					"ev.config" : mne_config,
					"fieldConfig" : fieldConfig,
					"delCellIds" : $('#delCellIds').val()
				},
				success : function(msg) {
					if(opener&&opener.ec.view.reloadDataGrid){
						opener.ec.view.reloadDataGrid();
					}
					CUI.Dialog.toggleAllButton('design-button',true,true);
					if (msg && msg.code) {
						$('input[name="ev.code"]').val(msg.code);
						$('input[name="ev.version"]').val(msg.version);
						workbenchErrorBarWidget.showMessage("${getHtmlText('ec.view.commonSaveSuccess')}", "s");
						//dg.advQuery.submitCustomerCon();
						location.reload();
					} else {
						workbenchErrorBarWidget.showMessage("${getHtmlText('common.dialog.savefailure')}");
					}
				}
			});
		}
		
		function publish() {
			
		}
		
		<#--
		$(document).ready(function(){
		var datas = {"viewCode" : "${view.code}"};
		CUI.ajax({
			url: "/msService/ec/customerCon/getCustomerCondition",
			type: 'post',
			async: false,
			data: datas,
			success: function(msg) {
				//console.log(msg);
				if(msg.success==true){
					//console.log($('#customerJsonString'));
					//console.log(document.getElementById('customerJsonString'));
					$('#customerJsonString').val(msg.data);
				}else{
					$('#customerJsonString').val('');
				}
			}
		});
		});
		
		
		(function(){
		var datas = {"viewCode" : "${view.code}"};
		
		CUI.ajax({
			url: "/msService/ec/customerCon/getShowSql",
			type: 'post',
			async: false,
			data: datas,
			success: function(msg) {
				if(msg.success==true){
					$('#isTransCondition').prop('checked', true);
					$('#conditionArea').removeAttr("readonly");
					$('#custCond').attr('disabled', 'true');
				}
				if(msg.data==null){
					$('#conditionArea').val('');
				}else{
					$('#conditionArea').val(msg.data);
				}
			}
		});
	})();
	-->

	$('#main_container').height($(window).height() - 180 );
	</script>

</body>
<#--<@customerCondition viewCode="${view.code}"/>-->
</html>
