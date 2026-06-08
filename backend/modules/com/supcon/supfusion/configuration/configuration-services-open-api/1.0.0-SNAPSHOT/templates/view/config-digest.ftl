<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
	<title>${getText('${(view.assModel.name)!}')}-${getText('${(view.displayName)!}')}-<#if view.type == 'VIEW'>${getText('ec.view.view')}<#else>${getText('ec.view.edit')} </#if>${getText('ec.project.view_setting')}</title>
	<@ec_digestTop />
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
		.cui-btn-leftmove {
			background: url('/bap/static/css/sprite_20120525.png') 0px -2050px no-repeat;
		}
		.cui-btn-rightmove {
			background: url('/bap/static/css/sprite_20120525.png') -61px -2050px no-repeat;
		}
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
		a.help-link {
			display: inline-block;
			width: 15px;
			height: 15px;
			background: url(/bap/static/css/edit_20150318.png) 0px -3553px no-repeat;
		}
	</style>
</head>
<body class="ec-config-page" style="background-color:#f2f2f2" layoutCode="${(ev.configMap.layout.layoutCode)!ecCodeInit('layout')}">
	<@errorbar id="digestErrorBar"/>
	<span style="color: #FFFFFF;font-size: 15px;font-weight: bold;margin: 8px 3px;float:right;">
		<a href="/help/" target="_blank" title="帮助文档" class="help-link"></a>
	</span>
	<div style="margin-left:2px;margin-top:8px;width: 160px; height:96%;float:left;background-color:white;border:1px solid #3C7FB1">
			<!-- 左侧 -->
			<div id="form_select_elements">
				<h2 class="current">${getHtmlText('ec.view.entityattribute')}</h2>
				<div class="accordion_pane" style="display:block;overflow:auto;height:95%">
					<ul class="main_properties_container">
						<#if subs?? && (subs.properties)?? && (subs.properties)?size &gt; 0>
							<#assign properties = (subs.properties)>
							<#list properties as p>
								<#if (p.name) != 'tableInfoId' && (p.name) != 'status' && (p.name) != 'version'>
								<li  onclick='digestec.addFastQueryField(this)' source='main' partDepend='common'  propertyCode='${(p.code)!}' namekey="${(p.displayName)!}" name='${(p.name)!}' columnType='${(p.type)!}'  entityCode='${(p.model.code)!}'  nullable='${(p.nullable!true)?string('true','false')}'>
									${getHtmlText('${(p.displayName)!}')}
								</li>
								</#if>	
							</#list>
						</#if>
						<#if subs?? && (subs.associatedInfos)??>
							<#assign associatedInfos = (subs.associatedInfos)>
							<#assign i = 1>
							<#list associatedInfos as ass>
								<li source='test'  partDepend='common' assTar='${ass.targetProperty.code}' assPropertyName='${ass.targetProperty.name}' propertyCode="${ass.originalProperty.code!}" assOrg="${ass.originalProperty.code}" dbname='${(ass.targetProperty.model.modelName)!}' name='${(ass.originalProperty.name)!}' entityCode='${(ass.targetProperty.model.code)!}' relativeName='${(ass.targetProperty.model.tableName)!},${(ass.targetProperty.columnName)},${(ass.originalProperty.model.tableName)!},${(ass.originalProperty.columnName)!}'>
									<img sourceType='digest' align='absmiddle' src='/bap/static/treeview/assets/ectree_colse.gif' flag='true' hasclick='false' onclick='digestec.showAssProperty(this,"${(ass.targetProperty.model.code)!}")'></img>
									${getHtmlText('${(ass.originalProperty.displayName)!}')} [${getHtmlText('${(ass.targetProperty.model.name)!}')}]
								</li>	
								<#assign i = i+1>
							</#list>
						</#if>
					</ul>
				</div>
			</div>
		</div>
		<div style="width:490px;background-color:white;border:2px solid #D4D4D4;float:left;height:96%;margin: 20px 5px 0 7px;">
			<input type="hidden" id="viewCode" value="${(view.code)!}" />
			<input type="hidden" id="viewType" value="${(view.type)!}" />
			<input type="hidden" id="delCellIds" name="delCellIds" />	
			<div style="position:absolute;height:89%;width:56%;">
				<div style="margin-left:10px;margin-top:40px;overflow-y:auto;clear:both;height:82%">
				<#if ev?? && (ev.configMap)??  && (ev.configMap.layout)??>
					<#assign configMap = (ev.configMap.layout)>
					<#if (configMap.sections)??>
					<#list (configMap.sections) as secatt>
					<#if (secatt.regionType)?? && (secatt.regionType)=='DIGEST'>
					<#assign sectionCode = secatt.sectionCode!ecCodeInit('section')>
					</#if>
					</#list>
					</#if>
				</#if>
				<table cellpadding="0" id="fastColTable" style="font-size:12px;width:96%;" sectionCode="${sectionCode!}" cellspacing="0" align="center" class="infoTable">
					<tbody id="fastColOrder">
					<#if ev?? && (ev.configMap)??  && (ev.configMap.layout)??>
					<#assign configMap = (ev.configMap.layout)>
					<#if (configMap.sections)??>
					<#list (configMap.sections) as secatt>
						<#if (secatt.regionType)?? && (secatt.regionType)=='DIGEST'>
							<#assign sectionCode = secatt.sectionCode!ecCodeInit('section')>
								<#list (secatt.cells) as element>
								<#assign cellCode = element.cellCode!ecCodeInit()>
								<#if (element.element)??>
									<#if (element.element.namekey)??>
										<#assign ckname = getText(element.element.namekey)! >
									<#else>
										<#assign ckname = (element.element.checkname)! >
									</#if>
									<#if (element.element.propertyCode)??>
										<#assign propCodes = (element.element.propertyCode)?split('||')>
										<#assign propCode = propCodes[propCodes?size - 1]>
									<#else>
										<#assign propCode = 'null'>
									</#if>
										<#if (element.element.propertyCode)?? &&propertyMap?? && propertyMap['${propCode}']?? >
											<#assign property = propertyMap['${propCode}'] >
											<tr <#if element_index % 2 == 0>class="even-num" numType="even"<#else>class="odd-num" numType="odd"</#if> groupname="digestRow" cellCode="${cellCode!}" key="${(element.element.key)!}" assPropertyName=${element.element.assPropertyName!} rowIndex="${(element.element.rowIndex)!}" ondblclick="digestec.selectRow('fast',this);digestec.digestFieldProperty();" onmousedown="digestec.selectRow('fast',this);"  columnType="${(property.type)!''}" name="${(element.element.name)!}" namekey="${(element.element.namekey)!}"  propertyCode="${(element.element.propertyCode)!}"  modelcode="${(property.model.code)!}">
										<#else>
											<tr <#if element_index % 2 == 0>class="even-num" numType="even"<#else>class="odd-num" numType="odd"</#if> groupname="digestRow" cellCode="${cellCode!}" key="${(element.element.name)!}" assPropertyName=${element.element.assPropertyName!} rowIndex="${(element.element.rowIndex)!}" ondblclick="digestec.selectRow('fast',this);digestec.digestFieldProperty();" onmousedown="digestec.selectRow('fast',this);" columnType="${(property.type)!''}" name="${(element.element.name)!}" namekey="${(element.element.namekey)!}"  propertyCode="${(element.element.propertyCode)!}"  modelcode="${(element.element.modelcode)!}">
										</#if>
											<td align="center" style="width:90%;">${getHtmlText('${(element.element.namekey)!element.element.name}')}</td>
											<td onclick="digestec.deleteFastQueryField(this)"><img title="${getText('ec.view.cell.fastdel')}" style="cursor:pointer;" src="/bap/static/ec/delete.gif" onMouseOver='deleteBtnChange(this)' onMouseOut='deleteBtnChange(this)'></img></td>
											</tr>
								</#if>
								</#list>
							<#break>
						</#if>
					</#list>
					</#if>
					</#if>
					</tbody>
				</table>
				</div>
			</div>
			<div id="fastContent" style="background-color:#f8f6f7;position:absolute;right:15px;width:15%;height:89%">
				<div style="padding-top:80px!important;" class="ec-list-btndiv"><div class="ec-list-topbtn" onclick="digestec.firstRow('fast')" id="firstMove" ></div></div>
				<div class="ec-list-btndiv"><div class="ec-list-prevbtn" onclick="digestec.upRow('fast')" id="upMove" ></div></div>
				<div class="ec-list-btndiv"><div class="ec-list-nextbtn" onclick="digestec.downRow('fast')" id="downMove"></div></div>
				<div class="ec-list-btndiv"><div class="ec-list-lastbtn" onclick="digestec.lastRow('fast')" id="lastMove"></div></div>
			</div>
		</div>	
	<div id="design-button" style="height:30px;width:100%;float:right;text-align:right;margin-top:20px">
		<button class="Dialog_button btn_pointer" onclick="digestec.save()" type="button" style="margin-right:20px">${getHtmlText('common.button.save')}</button>
		<button class="Dialog_button btn_pointer" type="button" onclick="javascript:window.close();" style="margin-right:20px">${getHtmlText('common.button.cancel')}</button>
	</div>
	<div id="form_set_item_property_dlg" style="display:none">
		<@errorbar id="DigestDialogErrorBar" />
		<div id="dlg-div" class="dlg-etv-navset">
			<div class="etv-scrollbar" style="margin:0 5px;">
				<ul class="etv-nav" style="display: block;">
					<li class="selected" name="proertyTabs" id="base_attr">
						<span class="etv-nav-span">
							<em class="etv-nav-em" style="width: 90px;">${getText('ec.view.cell.property')}</em>
						</span>
						<span class="etv-nav-span-r"></span>
					</li>
				</ul>
			</div>
		</div>
		<table id="tab-common" class="infoTable" cellpadding="0" cellspacing="0" border="0" width="95%" align="left" style="margin-top: 10px">
			<input type="hidden" id="form_property_field_val" />
			<tr>
				<td class="la" width="20%" align="right" padding-right="10px">${getHtmlText('ec.entity.name')}</td>
				<td class="co" width="70%"><input type="text" id="form_property_name" readonly="readonly" class="cui-edit-field" style="width:100%" /></td>
			</tr>
			<tr>
				<td class="la showname" width="20%" align="right" padding-right="10px">${getHtmlText('ec.view.showname')}</td>
				<td class="co showname" width="70%">
				<@international name="digestProperty.showName" isNew=true isOldEdit=true moduleCode="${view.assModel.entity.module.artifact}" key="" style="width:100%;margin-right: -5px;" cssClass="cui-edit-field" />
				</td>
			</tr>
		</table>
	</div>
	<script type="text/javascript">
		var main_tab,digestec;//全局变量
		$(function(){
			$('#form_select_elements').tabs("#form_select_elements div.accordion_pane", {tabs:'h2',effect:'slide',initialIndex:null});//初始化手风琴下拉
			//$('#fast_select_elements').tabs("#fast_select_elements div.accordion_pane", {tabs:'h2',effect:'slide',initialIndex:null});//初始化手风琴下拉
			//main_tab = new CUI.TabView("main_form_tab",{tabposition:'top', removable: true});//初始化主设计区域页签
			digestec = new CUI.EntityConfDigest('${(view.code)!}','${(ev.code)!}','${(ev.version)!0}');
			digestec.fastFieldColumnTroggle();
		});
	</script>
</body>