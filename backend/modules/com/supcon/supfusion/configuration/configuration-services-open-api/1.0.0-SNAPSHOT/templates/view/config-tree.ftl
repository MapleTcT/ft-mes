<#if (ev.configMap)?? && (ev.configMap.layout)??>
<#if ev.configMap.layout.pageConfig ??>
	<#if ev.configMap.layout.pageConfig.rootName??>
	<#assign rootName = ev.configMap.layout.pageConfig.rootName>
	</#if>
	<#if ev.configMap.layout.pageConfig.treeModelCode??>
	<#assign treeModelCode = ev.configMap.layout.pageConfig.treeModelCode>
	</#if>
	<#if ev.configMap.layout.pageConfig.onclickMethod??>
	<#assign onclickMethod = ev.configMap.layout.pageConfig.onclickMethod>
	</#if>
	<#if ev.configMap.layout.pageConfig.jsonSource??>
	<#assign jsonSource = ev.configMap.layout.pageConfig.jsonSource>
	</#if>
	<#if ev.configMap.layout.pageConfig.ondbclickMethod??>
	<#assign ondbclickMethod = ev.configMap.layout.pageConfig.ondbclickMethod>
	</#if>
	<#if ev.configMap.layout.pageConfig.beforeDrag??>
	<#assign beforeDrag = ev.configMap.layout.pageConfig.beforeDrag>
	</#if>
	<#if ev.configMap.layout.pageConfig.afterDrop??>
	<#assign afterDrop = ev.configMap.layout.pageConfig.afterDrop>
	</#if>
</#if>
</#if>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
	<title>${(subs.mainEntityName)!}-${getText('ec.project.view_setting')}-${getText('ec.view.tree')}</title>
	<@ec_treeTop />
	<@loadpanel />
	<style type="text/css">
		.layout{height:88%;}
		.grid-s4m0 .main-wrap {width: 100%;float:left;height:100%;overflow:hidden;background-color:#93BFD8;border:1px dashed #E6E6E6;}
		.grid-s4m0 .col-sub { margin-left:2px;margin-top:8px;width: 140px; height:98%;float:left;background-color:white;border:1px solid #3C7FB1}
		.grid-s4m0 .col-top{width: 100%;height:30px;background-color:#D4D8DB;border-bottom:1px solid #ADACAB;}
		#form_select_elements{height:98%}
		#form_select_elements h2 {margin:1;	padding:5px 0;text-align:center;font-size:13px;font-weight:normal;border:1px solid #0369A3;cursor:pointer;background-color: #9cc0d6;}
		#form_select_elements h2.current {cursor:default;}
		#form_select_elements .accordion_pane {	border:1px solid #fff;	border-width:0 2px;	display:none;padding:0px;font-size:12px;}
		#form_select_elements .accordion_pane li {font-size:12px;color:#000000;cursor:pointer;line-height:18px;z-index:100;margin-left:4px;}	
		#form_select_elements .accordion_pane li.dragout{color:#BBBBBB;cursor:default;}	
		#form_select_elements .accordion_pane li.uselist{color:#40E0D0;}		
		#fast_select_elements{height:98%}
		#fast_select_elements h2 {margin:1;	padding:5px 0;text-align:center;font-size:13px;font-weight:normal;border:1px solid #0369A3;cursor:pointer;background-color: #9cc0d6;}
		#fast_select_elements h2.current {cursor:default;}
		#fast_select_elements .accordion_pane {	border:1px solid #fff;	border-width:0 2px;	display:none;padding:0px;font-size:12px;}
		#fast_select_elements .accordion_pane li {font-size:12px;color:#000000;cursor:pointer;line-height:18px;z-index:100;margin-left:4px;}	
		#fast_select_elements .accordion_pane li.dragout{color:#BBBBBB;cursor:default;}		
		#main_form_tab_operate li{display:block;}
		#main_form_tab .etv-content{background-color:#F3F3F3!important}
		#main_form_tab .etv-navset{border:1px solid #2B82B2;}
		#main_design_container{padding:2px 2px 0 0;}
		.form_title{height:30px;width:99%;background-color:#d4d4d4;margin:6px 0px 0px 5px}
		.line-icon{width:110px;margin:2px 3px 2px 2px; border-color:#d4d4d4;overflow:hidden;}
		#form_area{position: relative;height:98%;}
		#main_form_tab_operate{z-index: 1000;}
		#main_form_tab .etv-content{min-height:500px;}
		#listSection{height:46px;background:#bababa;overflow-x:scroll;overflow-y:hidden;}	
		.list_design_ul_li{border:#CCC 1px dashed;height:25px;width:180px;border-right:1px solid #FFF;margin-top:1px;margin-bottom:1px;}
		.list_design_ul_li dd{text-align: center;padding-top:2px;width:100%;margin-top:3px;}
		.form_design_ul_li{border:#CCC 1px dashed;float:left; height:25px;margin:1px;margin-right:10px;position: absolute;}
		.button_design_ul_li{border:#CCC 1px dashed;float:left; height:25px;margin:1px;margin-right:10px;}
		.form_design_ul_li dt{float:left;width:100px;text-align: right;padding-right:5px;padding-top:2px;line-height:25px;}
		.form_design_ul_li dd{float:left;text-align: right;padding-top:5px;width:100%;margin-top:3px}
		.form_design_ul_li dd textarea,.form_design_ul_li dd input,.form_design_ul_li dd select{width:100%;height:20px;}
		.form_design_ul_li dd textarea{border:green 1px solid;margin:0;}
		.form_design_ul_li dd input.cui-search-click{width:24px!important;}
		.form_design_ul_li dd input.cui-calpick{width:22px!important;}
		.form_design_ul_li dd input[type='checkbox']{width:20px;height:20px;}
		.form_design_ul_li dd input[type='radio']{width:20px;height:20px;}
		.form_design_ul_li div{display:none;position: absolute;left:5px;top:2px;cursor:move;width:20px;height:20px;background: url("/bap/static/ec/images/arrow_switch.png") center center no-repeat;}
		.div-selected{}
		.ui-selectee{}
		.ui-selecting { background: #FECA40; }
		.ui-selected{background:#F39814;}
		.BlockInfoDiv {margin:2px;padding:4px;border-top:1px dashed #cccccc;}
		.editeventli {width:175px;text-align: right;}
		a.normala{text-decoration: underline;padding-left:2px;cursor:pointer;color:blue; display:inline-block;height:15px;overflow:hidden;}
		.operateDiv{height:22px;background:#bababa;border:#CCC 1px dashed;}
		.unsort-selected{ #FFFFFF;}
		.sort-selected{background:#A3E0FF;}
		.ec-list-btndiv{padding-top:25px;padding-left:30px;}
		.even-num {background-color:#EDEDED;}
		.data-classify {margin-top:0px;}
		.dc-menu-box{top:39px;left:135px;_left:185px;}
		.dc-menu-iframe {top:39px;left:135px;_left:185px;}
		#div_SELECT .dropselectbox {width:134px;height:18px;border:1px solid #C0D8F0}
		.ec-list-topbtn{cursor:hand;}
		.ec-list-nextbtn{cursor:hand;}
		.ec-list-prevbtn{cursor:hand;}
		.ec-list-lastbtn{cursor:hand;}
		.cui-btn-leftmove {
			background: url('/bap/static/css/sprite_20120525.png') 0px -2050px no-repeat;
		}
		.cui-btn-rightmove {
			background: url('/bap/static/css/sprite_20120525.png') -61px -2050px no-repeat;
		}
		a.help-link {
			display: inline-block;
			width: 15px;
			height: 15px;
			background: url(/bap/static/css/edit_20150318.png) 0px -3553px no-repeat;
		}
	</style>
</head>
<body class="ec-config-page" style="background-color:#f2f2f2" layoutCode="${(ev.configMap.layout.layoutCode)!ecCodeInit('layout')}" id="ec-config-tree">
	<@errorbar id="treeErrorBar"/>
	<div class="main-wrap" id="main_design_container">
	
	<div class="layout grid-s4m0" style="padding-left:5px;width:100%;"> 	
		<div class="col-top">
			<div>
				<h3 style="color:#3A70AA;padding-top:7px;padding-left:15px;float:left">
					${(subs.mainEntityName)!}-${getText('ec.project.view_setting')}-${getText('ec.view.tree')}
				</h3>
			</div>
		</div>

		<div class="col-main"> 
			<div class="main-wrap">	
			<div id="buttonOperateDiv" style="margin:10px 270px 2px 10px;padding-bottom:5px;background:#F3F3F3!important;border:1px solid #0099CC;">
				<div class="operateDiv">
					<label for="button"><font style="font-weight:bolder">${getText('ec.view.configoperator')}</font></label>
					<div style="padding-left:20px;display:inline-block">
					<a class="cui-btn mr10 cui-btn-add" href="#" onclick="tree.addOperateButton()">${getText('ec.view.add')}</a>
					<a class="cui-btn mr10 cui-btn-edit" href="#" onclick="tree.modifyOperateButton()">${getText('common.button.edit')}</a>
					<a class="cui-btn mr10 cui-btn-del" href="#" onclick="tree.deleteOperateButton()">${getText('ec.view.delete')}</a>
					<a class="cui-btn mr10 cui-btn-add" href="#" onclick="tree.addSeparate()">${getText('ec.view.separate')}</a>
					<a class="cui-btn mr10 cui-btn-leftmove" href="#" onclick="tree.leftmove('buttonOperateDiv')">${getText('ec.view.toleft')}</a>
					<a class="cui-btn mr10 cui-btn-rightmove" href="#" onclick="tree.rightmove('buttonOperateDiv')">${getText('ec.view.toright')}</a>
					<a class="cui-btn mr10 cui-btn-setting" href="#" onclick="tree.listProperty()">${getHtmlText('自定义条件')}</a>
					</div>
				</div>
				<div style="border:#CCC 1px solid;height:45px;margin:5px 5px 0 5px;">
					<div style="padding-top:10px;padding-left:10px;">
						<#if (ev.configMap)??  && (ev.configMap.layout)??>
						<#assign configMap = (ev.configMap.layout)>
						<#if (configMap.sections)??>
						<#list (configMap.sections) as section>
							<ul id="operateButton_ul" sectionCode="${(section.sectionCode)!ecCodeInit('section')}">
							<#if (section.regionType)?? && (section.regionType)=='BUTTON'>
								<#assign serviceName = section.serviceName!''>
								<#if section.cells??>
									<#list (section.cells) as operateButton>
										<#if (operateButton.operatetype)?? && operateButton.operatetype != "MOVE">
										<li cellCode="${operateButton.cellCode!ecCodeInit()}" id="${(operateButton.id)!}" namekey="${(operateButton.namekey)!}" showname="${((operateButton.showname)!)?html}" buttonstyle="${((operateButton.buttonstyle)!)?html}"
										<#if (operateButton.useInMore)??>
											useInMore="${(operateButton.useInMore)?string('true','false')}" 
										<#else>
											useInMore="false" 
										</#if>
										<#if (operateButton.funcname)??>
											funcname="${((operateButton.funcname)!)?html}" 
										</#if>
										<#if (operateButton.funcbody)??>
											funcbody="${((operateButton.funcbody)!)?html}"
										</#if>
										<#if (operateButton.operatetype)??>
											operatetype="${((operateButton.operatetype)!)?html}"
										</#if>
										<#if (operateButton.viewselect)??>
											viewselect="${((operateButton.viewselect)!)?html}"
										</#if>
										<#if (operateButton.opentype)??>
											opentype="${((operateButton.opentype)!)?html}"
										</#if>
										<#if (operateButton.iscallback)??>
											iscallback="${(operateButton.iscallback)?string('true','false')}"
										</#if>
										<#if (operateButton.iscustomfunc)??>
											iscustomfunc="${(operateButton.iscustomfunc)?string('true','false')}"
										</#if>
										<#if (operateButton.ispermission)??>
											ispermission="${(operateButton.ispermission)?string('true','false')}"
										</#if>
										<#if (operateButton.operateurl)??>
											operateurl="${((operateButton.operateurl)!)?html}"
										</#if>
										<#if (operateButton.isconfirm)??>
											isconfirm="${(operateButton.isconfirm)?string('true','false')}"
										</#if>
										<#if (operateButton.confirmcontent)??>
											confirmcontent="${((operateButton.confirmcontent)!)?html}"
										</#if>
										<#if (operateButton.selectType)??>
											selectType="${((operateButton.selectType)!false)?string('true','false')}"
										</#if>
										<#if (operateButton.scriptCode)??>
											scriptCode="${((operateButton.scriptCode)!)?html}"
										</#if>
										<#if (operateButton.isSignatureConfig)??>
											isSignatureConfig="${(operateButton.isSignatureConfig)?string('true','false')}"
										</#if>
										class="button_design_ul_li" onmousedown="tree.selectListLi(this)" <#if (operateButton.operatetype)?? && (operateButton.operatetype) != 'SEPARATE'>ondblclick="tree.modifyOperateButton()"></#if><dd><#if (operateButton.operatetype)?? && (operateButton.operatetype) == 'SEPARATE'><input type="button" style="width:30px" value="|"><#else><input type="button" class="Dialog_button btn_pointer" value="${getText('${(operateButton.namekey)!(operateButton.showname)!}')}"></#if></dd></li>
										</#if>
									</#list>
								</#if>
								<#-- <#break> -->
							</#if>
								<!--<li cellcode="" id="move" namekey="移动" showname="移动" buttonstyle="" useinmore="false" operatetype="MOVE" viewselect="" iscallback="false" iscustomfunc="false" ispermission="true" isconfirm="false" class="button_design_ul_li" onmousedown="" ondblclick="" style="display:none"><dd><input type="button" class="Dialog_button btn_pointer" value="移动"></dd></li>-->
							</ul>
						</#list>
						</#if>
						<#else>
							<ul id="operateButton_ul" sectionCode="${ecCodeInit('section')}">
								<!--<li cellcode="" id="move" namekey="移动" showname="移动" buttonstyle="" useinmore="false" operatetype="MOVE" viewselect="" iscallback="false" iscustomfunc="false" ispermission="true" isconfirm="false" class="button_design_ul_li" onmousedown="" ondblclick="" style="display:none"><dd><input type="button" class="Dialog_button btn_pointer" value="移动"></dd></li>-->
							</ul>
						</#if>	
					
					</div>	
				</div>
			</div>

			<div id="form_area" style="padding-left:5px;">
				<input type="hidden" id="btDelCellIds" name="btDelCellIds" />
				<div id="main_form_tab" class="etv-navset" style="height:500px;overflow-y:auto;">
					<table class="infoTable" cellpadding="0" cellspacing="0" border="0" width="70%" align="left" style="margin-top: 10px">
						<tr>
							<td class="la" width="10%" style="padding-right:10px;">
							${getText('根节点名')}
							</td>
							<td class="co" width="40%">
							<!--QC-8189:选中根节点后可以弹出修改根节点名称的界面，同时修改API接口-->
							<@international name="treeRootName" isNew=true isOldEdit=true moduleCode="${(view.assModel.entity.module.artifact)!''}" modelName="${view.code!}" key="${rootName!''}" style="width:100%;margin-right: -5px;" cssClass="cui-edit-field" usedInTree=true/>
							</td>
							<td class="la" width="10%" style="padding-right:10px;">
							${getText('显示字段')}
							</td>
							<td class="co" width="40%">
							<select id="treeModelCode" name="view.assModel.code" onchange="ec.view.assModelChange()" style="width:100%">
								<#if subs?? && (subs.properties)?? && (subs.properties)?size &gt; 0>
									<#assign properties = (subs.properties)>
									<#list properties as p>
										<#if (p.name) != 'tableInfoId' && (p.name) != 'status' && (p.name) != 'id' && (p.name) != 'version' && (p.name) != 'STATUS'>
										<option value="${p.name}"
										<#if  treeModelCode ?? && treeModelCode = p.name>
											selected
										</#if>
										>${getText('${(p.displayName)!}')}</option>
										</#if>	
									</#list>
								</#if>
							</select>
							</td>
						</tr>
						<tr>
							<td class="la" width="10%" style="padding-right:10px;">JSON${getText('ec.view.datasource')} </td>
							<td class="co" width="40%" colspan="3">
								<input type="text" class="cui-edit-field" id="jsonSource" name="jsonSource" value="${jsonSource!''}"/>
							</td>
						</tr>
						<tr>
							<td class="la" width="10%" style="padding-right:10px;">
							${getText('onclick')}
							<span id="onclickHelpinfo" class="baphelp-icon"></span>
							<div id="onclickHelpinforef" style="display:none">
	<p class="baphelp-info">单击事件，点击框内时触发</p>
	<p class="baphelp-example">范例</p>
<div class="baphelp-code">
<pre><code>function onclickPosition(){
	console.log("onclick");
}</code></pre>
</div>
</div>

					<script type="text/javascript">
							$('#onclickHelpinfo').helptip({refElm: "#onclickHelpinforef", html: true , isCustom :false, width: 220 , title :"说明"});
				</script>
							</td>
							<td class="co" width="40%" colspan="3">
							<textarea id="onclick" class="cui-edit-textarea" style="width:100%;height:150px;">${onclickMethod!''}</textarea>
							</td>
						</tr>
						<tr>
							<td class="la" width="10%" style="padding-right:10px;">
							${getText('ondbclick')}
							<span id="ondbclickHelpinfo" class="baphelp-icon"></span>
							<div id="ondbclickHelpinforef" style="display:none">
	<p class="baphelp-info">双击事件，点击框内时触发</p>
	<p class="baphelp-example">范例</p>
<div class="baphelp-code">
<pre><code>function ondbclickPosition(){
	console.log("ondbclick");
}</code></pre>
</div>
</div>

					<script type="text/javascript">
							$('#ondbclickHelpinfo').helptip({refElm: "#ondbclickHelpinforef", html: true , isCustom :false, width: 220 , title :"说明"});
				</script>
							</td>
							<td class="co" width="40%" colspan="3">
							<textarea id="ondbclick" class="cui-edit-textarea" style="width:100%;height:150px;">${ondbclickMethod!''}</textarea>
							</td>
						</tr>
						<tr style="display: none;">
							<td class="la" width="10%" style="padding-right:10px;">
							${getText('是否允许拖动')}
							</td>
							<td class="co" width="40%">
							<input type="checkbox" id="canDrag" onclick="dragChange()"/>
							</td>
							<td class="la" width="10%" style="padding-right:10px;">
							${getText('是否自动展开')}
							</td>
							<td class="co" width="40%">
							<input type="checkbox" id="dragOpen" />
							</td>
						</tr>
						<tr>
							<td class="la" width="10%" style="padding-right:10px;">
							${getText('是否开启记忆')}
							</td>
							<td class="co" width="40%">
							<input type="checkbox" id="canMemory"/>
							</td>
						</tr>
						<#--
						<tr>
							<td class="la" width="10%" style="padding-right:10px;">
							${getText('是否需要提示')}
							</td>
							<td class="co" width="40%">
							<input type="checkbox" id="needConfirm" />
							</td>
						</tr>
						-->
						<tr style="display: none;">
							<td class="la" width="10%" style="padding-right:10px;">
							${getText('beforeDrag')}
							</td>
							<td class="co" width="40%" colspan="3">
							<textarea id="beforeDrag" class="cui-edit-textarea" style="width:100%;height:150px;">${beforeDrag!''}</textarea>
							</td>
						</tr>
						<tr style="display: none;">
							<td class="la" width="10%" style="padding-right:10px;">
							${getText('afterDrop')}
							</td>
							<td class="co" width="40%" colspan="3">
							<textarea id="afterDrop" class="cui-edit-textarea" style="width:100%;height:150px;">${afterDrop!''}</textarea>
							</td>
						</tr>
					</table>
				</div>
			</div>
			</div>
		</div>		
	</div>	
	<#if !isReadOnlyMode>
	<div id="design-button" style="height:30px;width:100%;float:right;text-align:right;margin-top:20px">
		<button class="Dialog_button btn_pointer" onclick="tree.save()" type="button" style="margin-right:20px">${getText('common.button.save')}</button>
		<button class="Dialog_button btn_pointer btn-primary" type="button" onclick="tree.publish()" style="margin-right:20px">${getText('ec.view.publish')}</button>
	</div>
	</#if>
	<div id="button_property_dlg" style="display:none">
		<@errorbar id="ButtonDialogErrorBar"></@errorbar>
		<table class="infoTable" cellpadding="0" cellspacing="0" border="0" width="95%" align="left" style="margin-top: 10px">
			<tr>
				<td class="la supplant-required-field" width="20%" align="right" padding-right="10px">${getText('ec.view.identiy')}</td>
				<td class="co" width="30%"><input type="text" id="button_property_id" class="cui-edit-field" style="width:100%" /></td>
				<td class="la supplant-required-field" width="20%" align="right" padding-right="10px">${getText('ec.view.showname')}</td>
				<td class="co" width="30%">
				<#--<input type="text" id="button_property_show_name" class="cui-edit-field" style="width:100%" />-->
				<#--international_propertyshowName    international_propertyshowName_showName -->
				<@international name="buttonProperty.showName" isNew=true isOldEdit=true moduleCode="${view.assModel.entity.module.artifact}" key="" style="width:100%;margin-right: -5px;" cssClass="cui-edit-field" />
				</td>
			</tr>
			<tr>
				<td class="la" width="20%" align="right" padding-right="10px">${getText('ec.view.operatetype')}</td>
				<td class="co" width="30%">
					<select id="operatetype"  class="cui-edit-field" style="width:100%">
						<option value="CUSTOM">${getText('ec.view.customoperate')}</option>
						<option value="ADD">${getText('ec.view.addoperator')}</option>
						<option value="MODIFY">${getText('ec.view.editoperator')}</option>
						<option value="DELETE">${getText('ec.view.deloperator')}</option>
						<option value="SORT">${getText('排序')}</option>
					</select>
				
				</td>
				<td class="la" width="20%" align="right" padding-right="10px" id="viewSelectLabel"></td>
				<td class="co" width="30%" style="display:none;" id="viewSelectTd">
					<select id="viewselect"  class="cui-edit-field" style="width:100%">
						<option value="">${getText('ec.view.none')}</option>
						<#list viewList as viewlist>
							<option value="${viewlist.code}">${viewlist.name}</option>
						</#list>
					</select>
				</td>
			</tr>
			<tr>
				<td class="la" width="20%" align="right" padding-right="10px">Confirm</td>
				<td class="co" width="30%"><input type="checkbox" id="isconfirm" /></td>
				<td class="la" width="20%" align="right" padding-right="10px" id="confirmname">${getText('ec.view.hint')}</td>
				<td class="co confirmcontent" width="30%">
					<#--<input type="text" id="confirmcontent" class="cui-edit-field" style="width:100%" />-->
					<@international name="confirmcontent" isNew=true isOldEdit=true moduleCode="${view.assModel.entity.module.artifact}" key="" style="width:100%;margin-right: -5px;" cssClass="cui-edit-field" />
				</td>
			</tr>
			<tr>
				<td class="la" width="20%" align="right" padding-right="10px">${getText('ec.view.styletype')}</td>
				<td class="co" width="30%">
					<select id="buttonstyle"  class="cui-edit-field" style="width:100%">
						<option value="add">${getText('ec.view.addstyle')}</option>
						<option value="modify">${getText('ec.view.editstyle')}</option>
						<option value="del">${getText('ec.view.delstyle')}</option>
					</select>
				
				</td>
				<td class="la" width="20%" align="right" padding-right="10px">${getText('ec.view.useInMore')}</td>
				<td class="co" width="30%"><input type="checkbox" id="useInMore" /></td>
			</tr>
			<tr id="callbackTr">
				<td class="la" width="30%" align="right" padding-right="10px">${getText('ec.view.refresh')}</td>
				<td class="co" width="30%">
					<input type="checkbox" id="iscallback" />			
				</td>
				<td class="la" width="30%" align="right" padding-right="10px">${getText('ec.view.ispower')}</td>
				<td class="co" width="30%">
					<input type="checkbox" id="ispower" />			
				</td>
			</tr>
			<tr id="permissionTr">
				<td class="la" width="20%" align="right" padding-right="10px">${getText('ec.view.ispower')}</td>
				<td class="co" width="30%"><input type="checkbox" id="ispermission" /></td>
			</tr>
			<tr id="urlTr">
				<td class="la" width="30%" align="right" padding-right="10px">${getText('ec.view.powerurl')}</td>
				<td class="co" colspan="3">
					<input type="text" id="operateurl" class="cui-edit-field" style="width:100%" />				
				</td>
			</tr>
			<tr id="selectStypeTr" style="display:none">
				<td class="la" width="20%" align="right" padding-right="10px">${getText('ec.view.selectScript')}</td>
				<td class="co" width="30%">
					<input type="radio" name="stype" id="selectType" />		
				</td>
				<td class="la" width="20%" align="right" padding-right="10px">${getText('ec.view.customerScript')}</td>
				<td class="co" width="30%">
					<input type="radio" name="stype" id="writeType" />		
				</td>
			</tr>
			<tr id="eventTr" style="display:none;">
				<td class="la supplant-required-field" width="30%" align="right" padding-right="10px">${getText('ec.view.customevent')}</td>
				<td class="co" width="30%">
					<div style="float:left;width:100%;height:20px;margin-bottom:5px;border:1px solid #0059B3;overflow:auto;">
						<ul id="button_event_ul">
							<li class="editeventli" style="padding-top:2px">
							<a href="#" title="${getText('ec.view.editevent')}" style="text-decoration:underline;padding-left:20px;" onclick="tree.editButtonEvent('onclick')">onclick</a>：<input type="checkbox" id="button_property_event_onclick" value="" style="padding-right:10px"/>
							<span id="functionHelpinfo" class="baphelp-icon"></span>
<div id="functionHelpinforef" style="display:none">
	<p class="baphelp-info">自定义按钮的单击函数</p>
	<p class="baphelp-example">范例</p>
<div class="baphelp-code">
<pre><code>function customClick(event){
	console.info(event);
}</code></pre>
</div>
<p class="baphelp-hint">注意：event为事件对象，可不传</p>
</div>
					<script type="text/javascript">
							$('#functionHelpinfo').helptip({refElm: "#functionHelpinforef", html: true , isCustom :false, width: 220 , title :"说明"});
				</script>
							</li>
						</ul>
					</div>
					<br/>					
				</td>
			</tr>
			<tr id="iscustomfuncTr">
				<td class="la" width="30%" align="right" padding-right="10px">${getText('ec.view.iscallback')}</td>
				<td class="co" width="30%">
					<input type="checkbox" id="iscustomfunc" />
					<span id="callBackHelpinfo" class="baphelp-icon"></span>
					<script type="text/javascript">
					$(function(){
							var sel = $('#operatetype');
								var cbElm = $('#callBackHelpinfo');
								var cbIns;
								var cbM = {
									ADD: '<p class="baphelp-info">新增的回调函数</p><p class="baphelp-example">范例：</p><div class="baphelp-code"><pre><code>function demo(){<br/>&#160;&#160;&#160;alert(\"示例\");<br/>}</code></pre></div><p class="baphelp-hint">注意：回调函数会在保存后执行</p>',
									MODIFY: '<p class="baphelp-info">修改的回调函数</p><p class="baphelp-example">范例：</p><div class="baphelp-code"><pre><code>function demo(){<br/>&#160;&#160;&#160;alert(\"示例\");<br/>}</code></pre></div><p class="baphelp-hint">注意：回调函数会在保存后执行</p>',
									DELETE: '<p class="baphelp-info">删除的回调函数</p><p class="baphelp-example">范例：</p><div class="baphelp-code"><pre><code>function demo(){<br/>&#160;&#160;&#160;alert(\"示例\");<br/>}</code></pre></div><p class="baphelp-hint">注意：回调函数会在删除后执行</p>',									
								};		
								cbElm.on('click', function(){
									cbIns.setContent( cbM[sel.val()] );
								});				
								cbIns = cbElm.helptip({
									content: cbM[sel.val()], 
									html: true , 
									isCustom :false, 
									width: 200 , 
									title :"说明"
								}).data('cui.tooltip');
							});	
						</script>				
				</td>
			</tr>
			<tr id="customfuncTr">
				<td class="co" colspan="4">
					<textarea  id="customfuncarea" class="cui-edit-textarea" style="width:100%;height:55px;margin-left:10px;"></textarea>
				</td>	
			</tr>
			<tr id="selectGScript" style="display:none">
				<td class="la" id="view_scriptCodeLabel" >${getText('js.ec.script.manager.code')}</td>
				<td class="co" id="view_scriptCodeTd" >
				<@selector name="view.scriptCode" id="scriptCode" cssClass="cui-noborder-input" cssStyle="width:100%" value="${(view.scriptCode)!}"
					openType="page" closePage="true" pageType="script" onclick="selectScriptCode"/>
				</td>
			</tr>
			<tr id="textareaTr" style="display:none;">
				<td class="co" colspan="4">
					<textarea  id="buttonEventarea" class="cui-edit-textarea" style="width:100%;height:55px;margin-left:10px;"></textarea>
				</td>	
			</tr>
		</table>
	</div>
	<div id="listPropertyDlg" style="display:none;">
		<@errorbar id="ListPropertyDlgErrorBar"></@errorbar>
		<table class="infoTable" cellpadding="0" cellspacing="0" border="0" width="80%" align="left" style="margin-top: 10px">
			<tr>
				<td class="la" width="10%" style="padding-right:10px;">
					<input type="checkbox" id="isTransCondition" />
				</td>
				<td class="co" width="40%">
					${getHtmlText('ec.view.handwritingCondition')}
					<span id="handwritingHelpinfo" class="baphelp-icon"></span>	
<div id="handwritingHelpinforef" style="display:none">
	<p class="baphelp-info">可动态传递参数拼接到条件中，并根据变量进行逻辑判断</p>
	<p class="baphelp-example">范例</p>
<div class="baphelp-code">
<pre><code><span>if(test==1){</span>
	return '(P.ID =\$\{{deptId,Long} OR P.LAY_REC LIKE \\'\$\{layrec,String}\\')';
}else{
	return '(P.ID = \$\{deptId,String} OR P.LAY_REC LIKE \\'\$\{layrec,String}\\')';
}</code></pre>
</div>
<p class="baphelp-hint">注意：test 为前台传的参数，post 或get 即可</p>
</div>

					<script type="text/javascript">
							$('#handwritingHelpinfo').helptip({refElm: "#handwritingHelpinforef", html: true , isCustom :false, width: 460 , title :"说明"});
				</script>
				</td>
				<td class="co" colspan="2">
					<div id="66387035_button_2" canClick="true" class="edit-btn btn-act" onclick="showDialog()"><a class="cui-btn-l">&nbsp;</a><a class="cui-btn-c">${getHtmlText('ec.view.customerCondition')}</a><a class="cui-btn-r">&nbsp;</a></div>
				</td>
			</tr>
			<tr>
				<td class="co" colspan="4" style="padding-left:30px">
					<textarea readonly="true" id="conditionArea" class="cui-edit-textarea" style="width:90%;height:150px;"></textarea>
				</td>
			</tr>
		</table>
	</div>
</body>
<script type="text/javascript">
	var tree;
	
	var _proj_config_flag = ${isProj?string};
	$(function(){
		tree = new CUI.EntityConfTree('${(view.code)!}', '${(ev.code)!}', '${(ev.version)!0}');
		<#if ev.configMap?? && ev.configMap.layout.pageConfig.canDrag?? && ev.configMap.layout.pageConfig.canDrag == true>
		$('#canDrag').prop('checked', false);//禁用拖拽功能，全都默认false
			<#if ev.configMap?? && ev.configMap.layout.pageConfig.dragOpen?? && ev.configMap.layout.pageConfig.dragOpen == true>
		$('#dragOpen').prop('checked', false);
			</#if>
		<#--
			<#if ev.configMap.layout.pageConfig.needConfirm?? && ev.configMap.layout.pageConfig.needConfirm == true>
		$('#needConfirm').prop('checked', true);
			</#if>
		-->
		</#if>
		<#if ev.configMap?? && ev.configMap.layout.pageConfig.canMemory?? && ev.configMap.layout.pageConfig.canMemory == true>
			$('#canMemory').prop('checked', true);
		</#if>
		dragChange();
	});
	
	function dragChange(){
		if($('#canDrag').prop('checked')){
			$('#dragOpen').removeAttr("disabled");
			//$('#needConfirm').removeAttr("disabled");
		}else{
			$('#dragOpen').prop('checked', false);
			$('#needConfirm').prop('checked', false);
			$('#dragOpen').attr("disabled","disabled");
			//$('#needConfirm').attr("disabled","disabled");
		}
	}
	
	function showDialog(){
		if($('#66387035_button_2').attr('canClick')=="true"){
			dg.advQuery.showAdv();
		}
	}

	function resizeMainContentHeight(){
		var h = $(window).height() - 247;
		$('#form_area').height(h);
	}

	resizeMainContentHeight();
	$(window).resize(resizeMainContentHeight);
</script>
<@customerCondition viewCode="${view.code}"/>
</html>