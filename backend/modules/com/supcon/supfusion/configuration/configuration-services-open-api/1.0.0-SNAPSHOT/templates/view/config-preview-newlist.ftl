<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
	<title>${(subs.mainEntityName)!}-<#if view.type == 'LIST'>${getText('ec.view.list')}<#else>${getText('ec.view.ref')}</#if>${getText('ec.project.view_setting')}</title>
	<@head />
	<link rel="stylesheet" type="text/css" href="/bap/static/ec/css/style.css" />
	<link rel="stylesheet" type="text/css" href="/bap/static/ec/css/view.css" />
	<link rel="stylesheet" type="text/css" href="/bap/static/jquery/assets/jquery-ui.css" />
	<link rel="stylesheet" type="text/css" href="/bap/static/ec/css/purchase.css">
    <link rel="stylesheet" type="text/css" href="/bap/static/treeview/assets/treeview.css">
	<script type="text/javascript" src="/bap/static/core/jquery.js"></script>
	<script type="text/javascript" src="/bap/static/jquery/jquery.tools.js"></script>
	<script type="text/javascript" src="/bap/static/jquery/jquery.ui.js"></script>
	<script type="text/javascript" src="/bap/static/ec/js/${getCurrent('lang')}/ec_common.js"></script>
	<script type="text/javascript" src="/bap/static/ec/js/${getCurrent('lang')}/ec_list.js"></script>
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
		#main_form_tab_operate li{display:block;}
		#main_form_tab .etv-content{background-color:#F3F3F3!important}
		#main_design_container{padding:2px 2px 0 0;}
		.form_title{height:30px;width:99%;background-color:#d4d4d4;margin:6px 0px 0px 5px}
		.line-icon{width:110px;margin:2px 3px 2px 2px; border-color:#d4d4d4;overflow:hidden;}
		#form_area{position: relative;height:95%;padding-top:10px;}
		#main_form_tab_operate{z-index: 1000;}
		#main_form_tab .etv-content{min-height:500px;}
		#listSection{height:46px;background:#bababa;overflow-x:scroll;overflow-y:hidden;}	
		.list_design_ul_li{border:#CCC 1px dashed;height:25px;width:97px;border-right:1px solid #FFF;margin-top:1px;margin-bottom:1px;}
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
		.se-selected { background: #F39814;}
		.BlockInfoDiv {margin:2px;padding:4px;border-top:1px dashed #cccccc;}
		.editeventli {width:175px;text-align: right;}
		a.normala{text-decoration: underline;padding-left:2px;cursor:pointer;color:blue; display:inline-block;height:15px;overflow:hidden;}
		.operateDiv{height:22px;background:#bababa;border:#CCC 1px dashed;}
	</style>
</head>
<body class="ec-config-page" style="background-color:#f2f2f2">
	<@errorbar id="workbenchErrorBar" offsetY=83 />
	<div class="layout grid-s4m0"> 	
		<div class="col-top">
			<div><h3 style="color:#3A70AA;padding-top:7px;padding-left:15px;">${(view.assModel.entity.entityName)!}-${getHtmlText('ec.project.view_setting')}-<#if view.type == 'LIST'>${getHtmlText('ec.view.list')}<#else>${getHtmlText('ec.view.ref')}</#if></h3></div>
		</div>
		<div class="col-sub">
			<!-- 左侧 -->
			<div id="form_select_elements">
				<h2 class="current">${getHtmlText('ec.view.entityattribute')}</h2>
				<div class="accordion_pane" style="display:block;overflow:auto;height:92%">
					<ul class="main_properties_container">
						<#--
							Map map = (Map)request.getAttribute("subs");
							List<Property> properties = (List)map.get("properties");
							if(!properties.isEmpty()) {
								for(Property p : properties){
									out.print("<li source='main' name='" + map.get("mainEntityName") + "." + p.getName() + "' columnType='" + p.getType() + "' columnLong='" + p.getMaxLength() + "' nullable='true' fillContent='" + p.getFillcontent() + "' layRec='" + p.getName() + "'>");
									out.print("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" + p.getDisplayName());
									out.print("</li>");
								}
							}
							List<AssociatedInfo> associatedInfos = (List)map.get("associatedInfos");
							if(!associatedInfos.isEmpty()) {
								int i = 1;
								for(AssociatedInfo ass : associatedInfos) {
									out.print("<li source='test' dbname='" + ass.getTargetProperty().getModel().getModelName() + "' name='" + map.get("mainEntityName")+"."+ ass.getOriginalProperty().getName() + "' modelcode='"+ ass.getTargetProperty().getModel().getCode() +"' relativeName='"+ ass.getTargetProperty().getModel().getTableName() +","+ ass.getTargetProperty().getColumnName()+","+ ass.getOriginalProperty().getModel().getTableName()+","+ ass.getOriginalProperty().getColumnName()+"'>");
									out.print("<img align='absmiddle' src='/bap/static/treeview/assets/ectree_colse.gif' flag='true' hasclick='false' onclick='listec.showAssProperty(this,"+ ass.getTargetProperty().getModel().getCode() + ")'></img>");
									out.print(ass.getOriginalProperty().getDisplayName() + '[' + ass.getTargetProperty().getModel().getName() + ']');
									out.print("</li>");
									i++;
								}
							}
							
						-->	
						<#if subs?? && (subs.properties)?? && (subs.properties)?size &gt; 0>
							<#assign properties = (subs.properties)>
							<#list properties as p>
								<li source='main' propertyCode='${(p.code)!}' namekey="${(p.displayName)!}" name='${(p.name)!}' columnType='${(p.type)!}' columnLong='${(p.maxLength)!}' multable='${(p.multable!false)?string('true','false')}' decimalNum='${(p.decimalNum)!}' nullable='true' <#if p.isUsedForList?? && p.isUsedForList> list='true' <#else> list='false' </#if> fillContent='${(p.fillcontent)!}' layRec='${(p.name)!}' mnecode='${(p.isUsedMneCode!false)?string('true','false')}'>
									<#if p.isUsedForList?? && p.isUsedForList><font style="color:#40E0D0;"><#else><font style="color:#000000;"></#if>${getHtmlText('${(p.displayName)!}')}</font>
								</li>	
							</#list>
						</#if>
						<#if subs?? && (subs.associatedInfos)??>
							<#assign associatedInfos = (subs.associatedInfos)>
							<#assign i = 1>
							<#list associatedInfos as ass>
								<li source='test' assTar='${ass.targetProperty.code}' assOrg="${ass.originalProperty.code}" dbname='${(ass.targetProperty.model.modelName)!}' name='${(ass.originalProperty.name)!}' entityCode='${(ass.targetProperty.model.code)!}' relativeName='${(ass.targetProperty.model.tableName)!},${(ass.targetProperty.columnName)},${(ass.originalProperty.model.tableName)!},${(ass.originalProperty.columnName)!}'>
									<img align='absmiddle' src='/bap/static/treeview/assets/ectree_colse.gif' flag='true' hasclick='false' onclick='listec.showAssProperty(this,"${(ass.targetProperty.model.code)!}")'></img>
									${getHtmlText('${(ass.originalProperty.displayName)!}')} [${getHtmlText('${(ass.targetProperty.model.name)!}')}]
								</li>	
								<#assign i = i+1>
							</#list>
						</#if>
					</ul>
				</div>
				<#-- 
				<h2>${getText('ec.view.one2manyattr')}</h2>
				<div class="accordion_pane" style="overflow:auto;height:82%"><ul class="onetomany_properties_container">
				
						List<AssociatedInfo> oneToManyAssociatedInfos = (List)map.get("oneToManyAssociatedInfos");
						if(!oneToManyAssociatedInfos.isEmpty()) {
							int i = 1;
							for(AssociatedInfo onetoManyAss : oneToManyAssociatedInfos) {
								out.print("<li source='test' dbname='${(onetoManyAss.targetProperty.model.modelName)}' name='${map.get("mainEntityName")+"."+ (onetoManyAss.targetProperty.name) + "' entityCode='${(onetoManyAss.targetProperty.model.code)}' relativeName='${(onetoManyAss.targetProperty.model.tableName) +","+ (onetoManyAss.targetProperty.columnName)+","+ (onetoManyAss.originalProperty.model.tableName)+","+ (onetoManyAss.originalProperty.columnName)}'>
								out.print("<img align='absmiddle' src='/bap/static/treeview/assets/ectree_colse.gif' flag='true' hasclick='false' onclick='listec.showAssProperty(this,""${(onetoManyAss.targetProperty.model.code)} ")'></img>
								out.print(onetoManyAss.getTargetProperty().getModel().getName() + '[' + onetoManyAss.getTargetProperty().getDisplayName() + ']');
								out.print("</li>");
								i++;
							}
						}
					
					 
					 <#if subs?? && (subs.oneToManyAssociatedInfos)?? && (subs.oneToManyAssociatedInfos)?size &gt; 0>
					 	<#assign oneToManyAssociatedInfos = (subs.oneToManyAssociatedInfos)>
					 	<#assign j = 1>
							<#list oneToManyAssociatedInfos as onetoManyAss>
								<li source='test' dbname='${(onetoManyAss.targetProperty.model.modelName)!}' name='${(onetoManyAss.originalProperty.name)!}' entityCode='${(onetoManyAss.targetProperty.model.code)!}' relativeName='${(onetoManyAss.targetProperty.model.tableName)!},${(onetoManyAss.targetProperty.columnName)},${(onetoManyAss.originalProperty.model.tableName)!},${(onetoManyAss.originalProperty.columnName)!}'>
									<img align='absmiddle' src='/bap/static/treeview/assets/ectree_colse.gif' flag='true' hasclick='false' onclick='listec.showAssProperty(this,"${(onetoManyAss.targetProperty.model.code)!}")'></img>
									${(onetoManyAss.targetProperty.model.name)!}[${(onetoManyAss.targetProperty.displayName)!}]
								</li>	
								<#assign j = j+1>
						</#list>
					 </#if>
				</ul></div>
				<h2>${getText('ec.view.convertattri')}</h2>
				<div class="accordion_pane" style="overflow:auto;height:82%"><ul class="onetomany_properties_container">
				<#-- 
					List<AssociatedInfo> reverseAssociatedInfos = (List)map.get("reverseAssociatedInfos");
						if(!reverseAssociatedInfos.isEmpty()) {
							int i = 1;
							for(AssociatedInfo reverseAss : reverseAssociatedInfos) {
								out.print("<li source='test' dbname='" + reverseAss.getTargetProperty().getModel().getModelName() + "' name='" + map.get("mainEntityName")+"."+ reverseAss.getTargetProperty().getName() + "' entityCode='"+ reverseAss.getTargetProperty().getModel().getId() +"' relativeName='"+ reverseAss.getTargetProperty().getModel().getTableName() +","+ reverseAss.getTargetProperty().getColumnName()+","+ reverseAss.getOriginalProperty().getModel().getTableName()+","+ reverseAss.getOriginalProperty().getColumnName()+"'>");
								out.print("<img align='absmiddle' src='/bap/static/treeview/assets/ectree_colse.gif' flag='true' hasclick='false' onclick='listec.showAssProperty(this,"+ reverseAss.getTargetProperty().getModel().getId() + ")'></img>");
								out.print(reverseAss.getTargetProperty().getModel().getName() + '[' + reverseAss.getTargetProperty().getDisplayName() + ']');
								out.print("</li>");
								i++;
							}
						}
				
					<#if subs?? && (subs.reverseAssociatedInfos)?? && (subs.reverseAssociatedInfos)?size &gt; 0>
					 	<#assign reverseAssociatedInfos = (subs.reverseAssociatedInfos)>
					 	<#assign m = 1>
							<#list reverseAssociatedInfos as reverseAss>
								<li source='test' dbname='${(reverseAss.targetProperty.model.modelName)!}' name='${(reverseAss.targetProperty.name)!}' entityCode='${(reverseAss.targetProperty.model.code)!}' relativeName='${(reverseAss.targetProperty.model.tableName)!},${(reverseAss.targetProperty.columnName)},${(reverseAss.originalProperty.model.tableName)!},${(reverseAss.originalProperty.columnName)!}'>
									<img align='absmiddle' src='/bap/static/treeview/assets/ectree_colse.gif' flag='true' hasclick='false' onclick='listec.showAssProperty(this,"${(reverseAss.targetProperty.model.code)!}")'></img>
									${(reverseAss.targetProperty.model.name)!}[${(reverseAss.targetProperty.displayName)!}]
								</li>	
								<#assign m = m+1>
						</#list>
					 </#if>
				</ul></div>
				-->
				<h2>${getHtmlText('ec.view.searchbutton')}</h2>
				<div class="accordion_pane"><ul id="form_button_container">
					<li source='button' fieldType="button" name="query" namekey="ec.common.query">${getHtmlText('ec.common.query')}</li>
					<#--<li source='button' fieldType="button" name="advQuery" namekey="ec.common.advQuery">${getText('ec.view.advQuery')}</li>-->
				</ul></div>
			</div>
		</div>	
		<div class="col-main"> 			
			<div class="main-wrap" id="main_design_container">
				<div id="buttonOperateDiv" style="margin:10px 10px 15px 10px;padding-bottom:5px;background:#F3F3F3!important;border:1px solid #0099CC;">
					<div class="operateDiv">
						<label for="button"><font style="font-weight:bolder">${(view.assModel.modelName)!}_${getHtmlText('ec.view.configoperator')}</font></label>
						<div style="padding-left:20px;display:inline-block">
						<a class="cui-btn mr10 cui-btn-add" href="#" onclick="listec.addOperateButton()">${getHtmlText('ec.view.add')}</a>
						<a class="cui-btn mr10 cui-btn-edit" href="#" onclick="listec.modifyOperateButton()">${getHtmlText('common.button.edit')}</a>
						<a class="cui-btn mr10 cui-btn-del" href="#" onclick="listec.deleteOperateButton()">${getHtmlText('ec.view.delete')}</a>
						<a class="cui-btn mr10 cui-btn-leftmove" href="#" onclick="listec.leftmove('buttonOperateDiv')">${getHtmlText('ec.view.toleft')}</a>
						<a class="cui-btn mr10 cui-btn-rightmove" href="#" onclick="listec.rightmove('buttonOperateDiv')">${getHtmlText('ec.view.toright')}</a>
						</div>
					</div>
					<div style="border:#CCC 1px solid;height:45px;margin:5px 5px 0 5px;">
						<div style="padding-top:10px;padding-left:10px;">
							<ul id="operateButton_ul">
							<#if (ev.configMap)??>
							<#assign configMap = (ev.configMap)>
							<#if configMap.get("operateButtons")??>
							<#assign list = configMap.get("operateButtons")>
							<#if list??>
								<#list list as operateButton>
									<li id="${(operateButton.id)!}" namekey="${(operateButton.namekey)!}" showname="${(operateButton.showname)!}" buttonstyle="${(operateButton.buttonstyle)!}"
									<#if (operateButton.useInMore)??>
										useInMore="${(operateButton.useInMore)!}" 
									<#else>
										useInMore="false" 
									</#if>
									<#if (operateButton.funcname)??>
										funcname="${(operateButton.funcname)!}" 
									</#if>
									<#if (operateButton.funcbody)??>
										funcbody="${(operateButton.funcbody)!}"
									</#if>
									<#if (operateButton.operatetype)??>
										operatetype="${(operateButton.operatetype)!}"
									</#if>
									<#if (operateButton.viewselect)??>
										viewselect="${(operateButton.viewselect)!}"
									</#if>
									<#if (operateButton.opentype)??>
										opentype="${(operateButton.opentype)!}"
									</#if>
									<#if (operateButton.iscallback)??>
										iscallback="${(operateButton.iscallback)!}"
									</#if>
									<#if (operateButton.iscustomfunc)??>
										iscustomfunc="${(operateButton.iscustomfunc)!}"
									</#if>
									<#if (operateButton.ispermission)??>
										ispermission="${(operateButton.ispermission)!}"
									</#if>
									<#if (operateButton.operateurl)??>
										operateurl="${(operateButton.operateurl)!}"
									</#if>
									<#if (operateButton.isconfirm)??>
										isconfirm="${(operateButton.isconfirm)!}"
									</#if>
									<#if (operateButton.confirmcontent)??>
										confirmcontent="${(operateButton.confirmcontent)!}"
									</#if>
									 class="button_design_ul_li" onmousedown="listec.selectListLi(this)" ondblclick="listec.modifyOperateButton()"><dd><input type="button" class="Dialog_button btn_pointer" value="${(operateButton.showname)!}"></dd></li>
								</#list>
							</#if>
							</#if>
							</#if>	
							</ul>
						</div>	
					</div>
				</div>
				<hr style="margin-left:8px;margin-right:8px">
				<div class="operateDiv" style="margin:5px 8px 0 8px;">
					<label for="button" style="padding-left:15px;"><font style="font-weight:bolder">${(view.assModel.modelName)!}_${getHtmlText('ec.view.asscfg')}</font></label>
					<div style="padding-left:20px;display:inline-block">
						<a class="cui-btn mr10 cui-btn-add" href="#" onclick="listec.addLine(1)">${getHtmlText('ec.view.addline')}</a>
						<a class="cui-btn mr10 cui-btn-del" href="#" onclick="listec.delRow()">${getHtmlText('ec.view.deleteline')}</a>
						<a class="cui-btn mr10 cui-btn-del" href="#" onclick="listec.delCell()">${getHtmlText('ec.model.propertyDel')}</a>
						<a class="cui-btn mr10 cui-btn-setting" href="#" onclick="listec.cellProperty()">${getHtmlText('ec.view.fieldattribute')}</a>
						<a class="cui-btn mr10 cui-btn-setting" href="#" onclick="listec.sectionProperty()">${getHtmlText('ec.view.layout')}</a>
						<a class="cui-btn mr10 cui-btn-move" href="#" onclick="listec.splitCell()">${getHtmlText('ec.view.splitcell')}</a>
					</div>
				</div>
				<div id="form_area" style="padding-left:3px;">
					<div id="main_form_tab" class="etv-navset">
						<ul class="etv-nav">
							<li class="selected">${getHtmlText('ec.view.selectd')}</li>
						</ul>
						<div style="background-color:#e3f1f9;" class="etv-content" >
							<input type="hidden" id="mainEntityName" value="${(subs.mainEntityName)!}"/>
							<#if !(ev.configMap)??>
								<div class="div-selected">
									<ul class="form_design_ul se-selected" id="designarea" colNum="6">
										<li class="form_design_ul_li" ondblclick="listec.cellProperty()" onmousedown="listec.selectListLi(this)" colspan="1" rowspan="1"><dd></dd><div></div></li>
										<li class="form_design_ul_li" ondblclick="listec.cellProperty()" onmousedown="listec.selectListLi(this)" colspan="1" rowspan="1"><dd></dd><div></div></li>
										<li class="form_design_ul_li" ondblclick="listec.cellProperty()" onmousedown="listec.selectListLi(this)" colspan="1" rowspan="1"><dd></dd><div></div></li>
										<li class="form_design_ul_li" ondblclick="listec.cellProperty()" onmousedown="listec.selectListLi(this)" colspan="1" rowspan="1"><dd></dd><div></div></li>
										<li class="form_design_ul_li" ondblclick="listec.cellProperty()" onmousedown="listec.selectListLi(this)" colspan="1" rowspan="1"><dd></dd><div></div></li>
										<li class="form_design_ul_li" ondblclick="listec.cellProperty()" onmousedown="listec.selectListLi(this)" colspan="1" rowspan="1"><dd></dd><div></div></li>
										<li class="form_design_ul_li" ondblclick="listec.cellProperty()" onmousedown="listec.selectListLi(this)" colspan="1" rowspan="1"><dd></dd><div></div></li>
										<li class="form_design_ul_li" ondblclick="listec.cellProperty()" onmousedown="listec.selectListLi(this)" colspan="1" rowspan="1"><dd></dd><div></div></li>
										<li class="form_design_ul_li" ondblclick="listec.cellProperty()" onmousedown="listec.selectListLi(this)" colspan="1" rowspan="1"><dd></dd><div></div></li>
										<li class="form_design_ul_li" ondblclick="listec.cellProperty()" onmousedown="listec.selectListLi(this)" colspan="1" rowspan="1"><dd></dd><div></div></li>
										<li class="form_design_ul_li" ondblclick="listec.cellProperty()" onmousedown="listec.selectListLi(this)" colspan="2" rowspan="1" name="${getText('ec.common.query')}" fieldType="BUTTON"><dd><input type="button" style="width:70px!important" value="${getText('ec.common.query')}"/></dd><div></div></li>
									</ul>
							<#else>
							<#assign configMap = (ev.configMap)>
							<#if !configMap.get("fastsections")??>
								<div class="div-selected">
									<ul class="form_design_ul se-selected" id="designarea" colNum="6">
									<li class="form_design_ul_li" ondblclick="listec.cellProperty()" onmousedown="listec.selectListLi(this)" colspan="1" rowspan="1"><dd></dd><div></div></li>
										<li class="form_design_ul_li" ondblclick="listec.cellProperty()" onmousedown="listec.selectListLi(this)" colspan="1" rowspan="1"><dd></dd><div></div></li>
										<li class="form_design_ul_li" ondblclick="listec.cellProperty()" onmousedown="listec.selectListLi(this)" colspan="1" rowspan="1"><dd></dd><div></div></li>
										<li class="form_design_ul_li" ondblclick="listec.cellProperty()" onmousedown="listec.selectListLi(this)" colspan="1" rowspan="1"><dd></dd><div></div></li>
										<li class="form_design_ul_li" ondblclick="listec.cellProperty()" onmousedown="listec.selectListLi(this)" colspan="1" rowspan="1"><dd></dd><div></div></li>
										<li class="form_design_ul_li" ondblclick="listec.cellProperty()" onmousedown="listec.selectListLi(this)" colspan="1" rowspan="1"><dd></dd><div></div></li>
										<li class="form_design_ul_li" ondblclick="listec.cellProperty()" onmousedown="listec.selectListLi(this)" colspan="1" rowspan="1"><dd></dd><div></div></li>
										<li class="form_design_ul_li" ondblclick="listec.cellProperty()" onmousedown="listec.selectListLi(this)" colspan="1" rowspan="1"><dd></dd><div></div></li>
										<li class="form_design_ul_li" ondblclick="listec.cellProperty()" onmousedown="listec.selectListLi(this)" colspan="1" rowspan="1"><dd></dd><div></div></li>
										<li class="form_design_ul_li" ondblclick="listec.cellProperty()" onmousedown="listec.selectListLi(this)" colspan="1" rowspan="1"><dd></dd><div></div></li>
										<li class="form_design_ul_li" ondblclick="listec.cellProperty()" onmousedown="listec.selectListLi(this)" colspan="2" rowspan="1" name="${getText('ec.common.query')}" fieldType="BUTTON"><dd><input type="button" style="width:70px!important" value="${getText('ec.common.query')}"/></dd><div></div></li>
									</ul>
							<#else>
								<div class="div-selected">
									<#list configMap.get('fastsections') as secatt>
										<ul class="form_design_ul se-selected" id="designarea" colwidth="${(secatt.pageConfig.colwidth)!}" colNum="${(secatt.pageConfig.colNum)!6}" name="${(secatt.name)!}" isborder="${(secatt.isborder)!1}">
											<#if (secatt.content)['form']??>
											<#list (secatt.content)['form'] as element>
											<#if (element.element)??>
											<li class="form_design_ul_li" ondblclick="listec.cellProperty()" onmousedown="listec.selectListLi(this)" name="${(element.element.name)!}" namekey="${(element.element.namekey)!}" mnecode='${(element.element.mnecode!false)?string('true','false')}' propertyCode="${(element.element.propertyCode)!}" layRec="${(element.element.layRec)!}" nullable="${(element.element.nullable)?string("true","false")}" isreadonly="${(element.element.readonly)?string("true","false")}" exp="${(element.element.exp)!}" entityCode="${(element.element.entityCode)!}" layRec="${(element.element.layRec)!}" assTar="${(element.element.assTar)!}" assOrg="${(element.element.assOrg)!}" multable="${((element.element.multable)!false)?string('true','false')}" columnLong="${(element.element.columnLong)!}" readonly="${(element.element.readonly)?string("true","false")}" checkname="${(element.element.checkname)!}" fieldType="${(element.element.fieldType)!}"  fillType="${(element.element.fillType)!}" fillContent="${(element.element.fillContent)!}" sourcepropertyname="${(element.sourcepropertyname)!}" callbackbody="${(element.callbackbody)!}" callbackname="${(element.callbackname)!}" funcname="${(element.funcname)!}" funcbody="${(element.funcbody)!}" selectCompType="${(element.element.selectCompType)!}" cssstyle="${(element.cssstyle)!""}" referenceview="${(element.element.referenceview)!}" modelcode="${(element.element.modelcode)!}" columnType="${(element.element.columnType)!}" colspan="${(element.colspan)!1}" rowspan="${(element.rowspan)!1}" 
											fill='{<#if (element.element.fill)?has_content><#list (element.element.fill)?keys as fe><#if fe_index &gt; 0>,</#if>"${fe}":<#if (element.element.fill.fillType)?has_content && (element.element.fill.fillType?string) == '4' && fe == 'fillContent'>{<#if element.element.fill.fillOrder?has_content><#list element.element.fill.fillOrder?split(",") as ne><#if ne_index &gt; 0>,</#if>"${ne}":"${(element.element.fill.fillContent)[ne]}"</#list><#else><#list (element.element.fill.fillContent)?keys as ne><#if ne_index &gt; 0>,</#if>"${ne}":"${(element.element.fill.fillContent)[ne]}"</#list></#if>}<#else>"${(element.element.fill)[fe]}"</#if></#list></#if>}'>
												<#if (element.element.fieldType) == 'LABEL'>
													<dd >${getText('${(element.element.namekey)!element.element.name}')}</dd>
												<#elseif (element.element.fieldType) == 'BUTTON'>
													<dd><input type="button" style="width:70px!important" value="${getText('${(element.namekey)!element.name}')}"/></dd>
												<#else>
													<dd>
														<#if (element.element.fieldType) == 'DATE' || (element.element.fieldType) == 'DATETIME'>
															<input type="text" style="color:gray" value="${(element.element.checkname)!''}"/><input type='button' class='cui-calpick' />
														<#elseif (element.element.fieldType) == 'TEXTFIELD'>
															<input type="text" style="color:gray" value="${(element.element.checkname)!''}"/>
														<#elseif (element.element.fieldType) == 'SELECTCOMP'>
															<input type="text" style="color:gray;margin-right:-5px;" value="${(element.element.checkname)!''}"/><input type='button' class='cui-search-click' />
														<#elseif (element.element.fieldType) == 'SELECT'>
															<select style="color:gray"><option>${(element.element.checkname)!''}</option></select>
														<#elseif (element.element.fieldType) == 'RADIO'>
															<input type="radio"/><font color="gray">${(element.element.checkname)!}</font>
														<#elseif (element.element.fieldType) == 'CHECKBOX'>
															<input type="checkbox" /><font color="gray">${(element.element.checkname)!''}</font>
														</#if>
													</dd>
												</#if>
												<div></div>
											</li>
											<#else>
												<li class="form_design_ul_li" ondblclick="listec.cellProperty()" onmousedown="listec.selectListLi(this)" colspan="1" rowspan="1"><dd></dd><div></div></li>
											</#if>
											</#list>
											</#if>
										</ul>
									</#list>
								</#if>
							</#if>
								<div style="height:135px"></div>
								<hr>
								<div class="operateDiv">
									<label for="button"><font style="font-weight:bolder">${(view.assModel.modelName)!}_${getHtmlText('ec.view.cfgfield')}</font></label>
									<div style="padding-left:20px;display:inline-block">
										<a class="cui-btn mr10 cui-btn-add" href="#" onclick="listec.addCustomListField()">${getHtmlText('ec.view.addCol')}</a>
										<a class="cui-btn mr10 cui-btn-del" href="#" onclick="listec.delListField()">${getHtmlText('ec.view.delcol')}</a>
										<a class="cui-btn mr10 cui-btn-edit" href="#" onclick="listec.fieldProperty()">${getHtmlText('ec.view.colattribute')}</a>
										<a class="cui-btn mr10 cui-btn-setting" href="#" onclick="listec.listProperty()">${getHtmlText('ec.view.listproperty')}</a>
										<a class="cui-btn mr10 cui-btn-setting" href="#" onclick="listec.dataClassific()">${getHtmlText('ec.view.dataclass')}</a>
										<a class="cui-btn mr10 cui-btn-leftmove" href="#" onclick="listec.leftmove('listSection')">${getHtmlText('ec.view.toleft')}</a>
										<a class="cui-btn mr10 cui-btn-rightmove" href="#" onclick="listec.rightmove('listSection')">${getHtmlText('ec.view.toright')}</a>
									</div>
								</div>
								<table cellspacing="0" cellpadding="0" class="ec_table_bottom">									
									<tr>
										<td class="bottom_head" colspan="6">
											<div style="float:right;height:23px;padding-right:3px;">
									        	<a href="#"><span class="disabled firstPage floatspan" style="width:8px"></span></a>
									            <a href="#"><span class="disabled prevlink floatspan" style="width:8px"></span></a>
												<a href="#"><span class="disabled nextPage floatspan" style="width:8px"></span></a>
												<a href="#"><span class="disabled lastPage floatspan" style="width:8px"></span></a>  
									            <span class="floatspan" style="margin-top:3px;">${getHtmlText('ec.view.count')}</span>
									            <span class="floatspan" style="margin-top:3px;">           
									                <span class="floatspan" ></span>
									                <select id="PageLink_PageSelect" class="PageLink-PageSelect" style="width:40px;font-size: 7.8pt; font-family: verdana;" existoptionflag="0" pagecount="30">
									                <option value="1">1</option>
									                <option value="2">2</option>
									                <option value="3">3</option>
									                <option value="4">4</option>
									                <option value="5">5</option>
									                </select>
									                <span>/10${getHtmlText('ec.view.page')}</span>
									            </span>           
									        </div>
							        	</td>
							        </tr>
									<tr>
										<td class="ec_head">${getHtmlText('ec.view.num')}</td>
										<td>
											<div id="listSection" class="listSection">
											<table id="listTable" class="listTable" style="table-layout:fixed">
											<#if !(ev.configMap)??>
												<tr class="list_design_ul" id="list_design_section" isCheckBox="false" isFirstLoad="false" pendingDeal="false" isExportExcel="true" isTransCondition="false" conditionContent=""><tr>
											<#else>
												<#assign configMap = (ev.configMap)>	
												<#if configMap.get("columns")?? && (configMap.get("columns"))?size <= 0>
													<tr class="list_design_ul" id="list_design_section" isCheckBox="false" isFirstLoad="false" pendingDeal="false" isExportExcel="true" isTransCondition="false" conditionContent=""><tr>
												<#else>
													<#if (configMap.listProperty)??>
													<#assign listProperty = (configMap.listProperty)>
													<tr class="list_design_ul" id="list_design_section" isCheckBox="${listProperty.isCheckBox?string("true","false")}" isExportExcel="<#if listProperty.isExportExcel??>${listProperty.isExportExcel?string("true","false")}<#else>true</#if>" isFirstLoad="${listProperty.isFirstLoad?string("true","false")}" pendingDeal="${listProperty.pendingDeal?string("true","false")}" isTransCondition="<#if listProperty.isTransCondition??>${listProperty.isTransCondition?string("true","false")}<#else>false</#if>" conditionContent="${(listProperty.conditionContent)!''}">
													<#else>
													<tr class="list_design_ul" id="list_design_section" isCheckBox="false" isFirstLoad="false" pendingDeal="false" isExportExcel="true" isTransCondition="false" conditionContent="">
													</#if>
														<#list configMap.get('columns') as column>
															<#if !(column.none)??>
																<td class="list_design_ul_li" name='${(column.key)!}' namekey="${(column.namekey)!}" key='${(column.key)!}' <#if column.multable??>multable=${(column.multable)?string("true","false")}<#else>multable=false</#if> <#if column.isLink??>isLink=${(column.isLink)?string("true","false")}</#if> assModels='${(column.assModels)!}' linkView='${(column.linkView)!}' assModelCode='${(column.assModelCode)!}' modelCode='${(column.modelCode)!}' mnecode='${(column.mnecode!false)?string('true','false')}' format='${(column.format)!}' dbtype='${(column.type)!}' layRec='${(column.layRec)!}' formulas="${(column.formulas)!''}" textalign="${(column.textalign)!'center'}" decimalNum='${(column.decimalNum)!}'
																	fill='{<#if (column.fill)?has_content><#list (column.fill)?keys as fe><#if fe_index &gt; 0>,</#if>"${fe}":<#if (column.format) == 'ENUMERATE' && fe == 'fillContent'>{<#list (column.fill)[fe]?keys as ne><#if ne_index &gt; 0>,</#if>"${ne}":"${(column.fill)[fe][ne]}"</#list>}<#else>"${(column.fill)[fe]}"</#if></#list></#if>}'
																 	<#if (column.ass)?? && column.ass?has_content><#list column.ass?keys as asstar><#if asstar == 'tar'>assTar<#else>assOrg</#if>='${(column.ass)[asstar]!}'</#list></#if>
																 propertyCode='${(column.propertyCode)!}' checkname='${(column.label)!}' hide=${(column.hide)?string("true","false")} width=${(column.width)!100} ondblclick="listec.fieldProperty()" onmousedown="listec.selectListLi(this)"><dd>${(column.label)!}</dd></td>
															</#if>	 
														</#list>
													</tr>
												</#if>
											</#if>
											</table>
											</div>
										</td>
									</tr>
									<tr><td class="ec_left">01</td>
									<td class="ec_right">
									</td>
									</tr>
									<tr><td class="ec_left">02</td>
									<td class="ec_right">
									</td>
									</tr>
								</table>
							</div>
						</div>						
					</div>
				</div>
			</div>
		</div>		
	</div>	
	<div id="design-button" style="height:30px;width:100%;float:right;text-align:right;margin-top:20px">
		<#--<button class="Dialog_button btn_pointer" onclick="listec.save()" type="button" style="margin-right:20px">${getText('common.button.save')}</button>-->
		<#--<button class="Dialog_button btn_pointer" type="button" onclick="listec.publish()" style="margin-right:20px">${getText('ec.view.publish')}</button>-->
	</div>
	
	<div id="form_set_item_property_dlg" style="display:none">
		<@errorbar id="EditDialogErrorBar" />
		<table class="infoTable" cellpadding="0" cellspacing="0" border="0" width="95%" align="left" style="margin-top: 10px">
			<input type="hidden" id="form_property_field_val" />
			<tr>
				<td class="la" width="20%" align="right" padding-right="10px">${getHtmlText('ec.entity.name')}</td>
				<td class="co" width="30%"><input type="text" id="form_property_name" class="cui-edit-field" style="width:100%" /></td>
				<td class="la" width="20%" align="right" padding-right="10px">${getHtmlText('ec.view.showname')}</td>
				<td class="co" width="30%">
				<#-- <input type="text" id="form_property_show_name" class="cui-edit-field" style="width:100%" /> -->
				<#--international_propertyshowName    international_propertyshowName_showName -->
				<@international name="formProperty.showName" isNew=true isOldEdit=true moduleCode="${view.assModel.entity.module.artifact}" key="" style="width:100%;margin-right: -5px;" cssClass="cui-edit-field" />
				</td>
			</tr>
			<tr id="selectDisplayTypeTR"><td class="la" width="20%" align="right" padding-right="10px">${getHtmlText('ec.view.type')}</td>
				<td class="co" width="30%">
				<select id="form_property_field_type" class="cui-edit-field" style="width:100%">
					<#if fastfieldtypes??>
					<#list fastfieldtypes?keys as key>
					<option value="${key}">${fastfieldtypes[key]}</option>
					</#list>
					</#if>
				</select></td>
				<td class="la" width="20%" align="right" padding-right="10px"><label id="iscomplexLable"></label></td>
				<td class="co" width="30%">
					<select id="selectcomp_type"  class="cui-edit-field" style="display:none;width:96%">
					</select>
				</td>
			</tr>
			<tr>
				<td class="la" width="20%" align="right" padding-right="10px"><label id="isreadOnlyLable">${getHtmlText('ec.view.isreadonly')}</label></td>
				<td class="co" width="30%"><input type="checkbox" id="form_property_is_readonly" /></td>
				<td class="la" width="20%" align="right" padding-right="10px"><label id="operator" style="display:none;">${getHtmlText('ec.flowActive.doperate')}</label></td>
				<td class="co" width="30%">
				<select id="form_property_exp_type" class="cui-edit-field" style="display:none;width:96%">
				</select></td>
			</tr>
			<tr>
				<td class="la" width="20%" align="right" padding-right="10px">${getHtmlText('ec.view.customevent')}</td>
				<td class="co" colspan="3">
					<div style="float:left;width:45%;height:60px;margin-bottom:5px;border:1px solid #0059B3;overflow:auto;">
						<ul id="event_ul">
							<li class="editeventli"><a href="#" title="${getText('ec.view.editevent')}" style="text-decoration:underline" onclick="listec.editEvent('onchange')">onchange</a>：<input type="checkbox" id="li_property_event_onchange" value=""/>&#160;&#160;&#160;</li><li class="editeventli"><a title="${getText('ec.view.editevent')}" style="text-decoration:underline" href="#" onclick="listec.editEvent('onpropertychange')">onpropertychange</a>：<input type="checkbox" id="li_property_event_onpropertychange" value=""/>&#160;&#160;&#160;</li>
							<li class="editeventli"><a href="#" title="${getText('ec.view.editevent')}" style="text-decoration:underline" onclick="listec.editEvent('onclick')">onclick</a>：<input type="checkbox" id="li_property_event_onclick" value=""/>&#160;&#160;&#160;</li><li class="editeventli"><a title="${getText('ec.view.editevent')}" style="text-decoration:underline" href="#" onclick="listec.editEvent('ondbclick')">ondbclick</a>：<input type="checkbox" id="li_property_event_ondbclick" value=""/>&#160;&#160;&#160;</li>
							<li class="editeventli"><a href="#" title="${getText('ec.view.editevent')}" style="text-decoration:underline" onclick="listec.editEvent('onblur')">onblur</a>：<input type="checkbox" id="li_property_event_onblur"  value=""/>&#160;&#160;&#160;</li>
						</ul>
						<li class="editeventli"><a title="${getText('ec.view.editevent')}" style="text-decoration:underline" href="#" onclick="listec.editEvent('callback')">callback</a>：<input type="checkbox" id="li_property_event_callback" value=""/>&#160;&#160;&#160;</li>
					</div>
					<!-- <textarea  id="form_property_field_event" class="cui-edit-textarea" style="width:100%;height:120px;"></textarea> -->
					<div  style="float:right;width:50%;height:60px;margin-right:5px">
						${getHtmlText('ec.view.customeventtype')}&#160;&#160;&#160;<br/>
						<input type="text" id="editEventtype" class="cui-edit-field" style="width:150px;margin-top:10px" value=""/>&#160;&#160;&#160;&#160;&#160;&#160;<img onclick="listec.addnewevent()" title="${getText('ec.view.addevent')}" src="/bap/static/foundation/images/icon-add.gif" /><br/>
					</div>
					<br/>					
					<textarea  id="editEventarea" class="cui-edit-textarea" style="width:100%;height:75px;"></textarea>
				</td>
			</tr>
			<tr>
				<td class="la" width="20%" align="right" padding-right="10px">${getHtmlText('ec.view.customstyle')}</td>
				<td class="co" colspan="3"><textarea id="form_property_field_style" class="cui-edit-textarea" style="width:100%;height:50px;margin-top:5px"></textarea></td>
			</tr>
		</table>
	</div>
 	<div id="section_setting_dlg" style="display:none;">
 		<table class="infoTable" cellpadding="0" cellspacing="0" border="0" width="95%" align="left" style="margin-top: 10px">
			<tr>
				<td class="la" width="10%" align="right" padding-right="10px">${getHtmlText('ec.view.layout')}</td>
				<td class="co" width="90%">${getHtmlText('ec.view.everyline')} <input type="text" id="section_num" class="cui-edit-field" style="width:50px;margin-right:5px;"/> ${getHtmlText('ec.view.colum')}</td>
			</tr>
			<tr>
				<td class="la" width="10%" align="right" padding-right="10px">${getHtmlText('ec.view.colwidth')}</td>
				<td class="co" colspan="3">
					<div id="editcolwidthdiv">
						1、<input type="text" id="colwidth1" class="cui-edit-field" style="width:20px;margin-right:5px;" value="10"/>
						2、<input type="text" id="colwidth2" class="cui-edit-field" style="width:20px;margin-right:5px;" value="18"/>
						3、<input type="text" id="colwidth3" class="cui-edit-field" style="width:20px;margin-right:5px;" value="17"/>
						4、<input type="text" id="colwidth4" class="cui-edit-field" style="width:20px;margin-right:5px;" value="18"/>
						5、<input type="text" id="colwidth5" class="cui-edit-field" style="width:20px;margin-right:5px;" value="18"/>
						6、<input type="text" id="colwidth6" class="cui-edit-field" style="width:20px;margin-right:5px;" value="19"/>
					</div>
				</td>
			</tr>
		</table>
 	</div>
 	<div id="listFieldDlg" style="display:none;">
 		<@errorbar id="ListFieldDlgErrorBar"></@errorbar>
		<table class="infoTable" cellpadding="0" cellspacing="0" border="0" width="95%" align="left" style="margin-top: 10px">
			<tr>
				<td class="la" width="20%" style="padding-right:10px;align:right;color:red">${getHtmlText('ec.view.code')}</td>
				<td class="co" width="30%"><input id="property_key" type="text" class="cui-edit-field" /></td>
				<td class="la" width="20%" style="padding-right:10px;align:right" id="decimalTd">${getHtmlText('ec.view.decimal')}</td>
				<td class="co" width="30%" id="decimalTextTd"><input id="property_decimal" type="text" class="cui-edit-field" /></td>
			</tr>
			<tr>
				<td class="la" width="20%" style="padding-right:10px;align:right">${getHtmlText('ec.view.showname')}</td>
				<td class="co" width="30%">
				<#-- <input id="property_show_name" type="text" class="cui-edit-field" /> -->
				<#--international_propertyshowName    international_propertyshowName_showName -->
				<@international name="property.showName" isNew=true isOldEdit=true moduleCode="${view.assModel.entity.module.artifact}" key="" style="width:100%;margin-right: -5px;" cssClass="cui-edit-field" />
				</td>
				<td class="la" width="20%" style="padding-right:10px;align:right">${getHtmlText('ec.view.width')}</td>
				<td class="co" width="30%"><input id="property_width" type="text" class="cui-edit-field" /></td>
			</tr>
			<tr>
				<td class="la" width="20%" style="padding-right:10px;align:right">${getHtmlText('ec.view.ishidden')}</td>
				<td class="co" width="30%"><input type="checkbox" id="isHide" /></td>
				<td class="la" width="20%" style="padding-right:10px;align:right">${getHtmlText('ec.view.showvalue')}</td>
				<td class="co" width="30%">
					<select id="textalign"  class="cui-edit-field" style="width:100%">
						<option value="center">${getText('ec.view.center')}</option>
						<option value="left">${getText('ec.view.left')}</option>
						<option value="right">${getText('ec.view.right')}</option>
					</select>
				</td>
			</tr>
			<tr id="formulasTr" style="display:none;">
				<td class="la" width="30%" style="padding-right:10px;">${getHtmlText('ec.view.expression')}</td>
				<td class="co" width="70%" colspan="3"><input id="formulasText" type="text" class="cui-edit-field" /></td>
			</tr>
			<tr id="islinkViewTr">
				<td class="la" width="20%" style="padding-right:10px;align:right">${getHtmlText('ec.view.isLink')}</td>
				<td class="co" width="30%"><input type="checkbox" id="isLink" /></td>
				
			</tr>
			<tr id="linkViewTr">
				<td class="la" width="20%" style="padding-right:10px;align:right" id="assModelTd">${getHtmlText('ec.view.assModel')}</td>
				<td class="co" width="30%">
					<select id="assModels"  class="cui-edit-field" style="width:100%">
						<option value=""></option>
					</select>
				</td>
				<td class="la" width="20%" style="padding-right:10px;align:right" id="linkViewTd">${getHtmlText('ec.view.linkView')}</td>
				<td class="co" width="30%">
					<select id="linkView"  class="cui-edit-field" style="width:100%">
						<option value=""></option>
					</select>
				</td>
			</tr>
		</table>
	</div>
	<div id="listPropertyDlg" style="display:none;">
		<@errorbar id="ListPropertyDlgErrorBar"></@errorbar>
		<table class="infoTable" cellpadding="0" cellspacing="0" border="0" width="80%" align="left" style="margin-top: 10px">
			<tr>
				<td class="la" width="20%" style="padding-right:10px;"><input type="checkbox" id="isCheckBox" /></td>
				<td class="co" width="80%">${getHtmlText('ec.view.isCheckbox')}Checkbox</td>
			</tr>
			<tr>
				<td class="la" width="20%" style="padding-right:10px;"><input type="checkbox" id="isFirstLoad" /></td>
				<#if view.entity.isBase>
					<td class="co" width="80%">${getHtmlText('ec.view.firstsearch')}</td>
				<#else>
					<td class="co" width="80%">${getHtmlText('ec.view.firstselect')}</td>
				</#if>
				
			</tr>
			<#if !view.entity.isBase>
			<tr>
				<td class="la" width="20%" style="padding-right:10px;"><input type="checkbox" id="pendingDeal" /></td>
				<td class="co" width="80%">${getHtmlText('ec.view.noproxy')}</td>
			</tr>
			</#if>
			<tr>
				<td class="la" width="20%" style="padding-right:10px;"><input type="checkbox" id="isExportExcel" /></td>
				<td class="co" width="80%">${getHtmlText('ec.view.exportexcel')}</td>
			</tr>
			<tr>
				<td class="la" width="20%" style="padding-right:10px;"><input type="checkbox" id="isTransCondition" /></td>
				<td class="co" width="80%">${getHtmlText('ec.view.iscondition')}</td>
			</tr>
			<tr id="conditionAreaTr" style="display:none;">
				<td class="co" colspan="2">
					<textarea  id="conditionArea" class="cui-edit-textarea" style="width:95%;height:55px;margin-left:10px;"></textarea>
				</td>
			</tr>
		</table>
	</div>
	<div id="button_property_dlg" style="display:none">
		<@errorbar id="ButtonDialogErrorBar"></@errorbar>
		<table class="infoTable" cellpadding="0" cellspacing="0" border="0" width="95%" align="left" style="margin-top: 10px">
			<tr>
				<td class="la" width="20%" align="right" padding-right="10px">${getHtmlText('ec.view.identiy')}</td>
				<td class="co" width="30%"><input type="text" id="button_property_id" class="cui-edit-field" style="width:100%" /></td>
				<td class="la" width="20%" align="right" padding-right="10px">${getHtmlText('ec.view.showname')}</td>
				<td class="co" width="30%">
				<#--<input type="text" id="button_property_show_name" class="cui-edit-field" style="width:100%" />-->
				<#--international_propertyshowName    international_propertyshowName_showName -->
				<@international name="buttonProperty.showName" isNew=true isOldEdit=true moduleCode="${view.assModel.entity.module.artifact}" key="" style="width:100%;margin-right: -5px;" cssClass="cui-edit-field" />
				</td>
			</tr>
			<tr>
				<td class="la" width="20%" align="right" padding-right="10px">${getHtmlText('ec.view.operatetype')}</td>
				<td class="co" width="30%">
					<select id="operatetype"  class="cui-edit-field" style="width:100%">
						<option value="custom">${getText('ec.view.customoperate')}</option>
						<option value="add">${getText('ec.view.addoperator')}</option>
						<option value="edit">${getText('ec.view.editoperator')}</option>
						<option value="del">${getText('ec.view.deloperator')}</option>
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
				<td class="la" width="20%" align="right" padding-right="10px">confirm${getHtmlText('ec.view.config')}</td>
				<td class="co" width="30%"><input type="checkbox" id="isconfirm" /></td>
				<td class="la" width="20%" align="right" padding-right="10px" id="confirmname">${getHtmlText('ec.view.hint')}</td>
				<td class="co" width="30%">
					<input type="text" id="confirmcontent" class="cui-edit-field" style="width:100%" />
				</td>
			</tr>
			<tr>
				<td class="la" width="20%" align="right" padding-right="10px">${getHtmlText('ec.view.styletype')}</td>
				<td class="co" width="30%">
					<select id="buttonstyle"  class="cui-edit-field" style="width:100%">
						<option value="add">${getText('ec.view.addstyle')}</option>
						<option value="edit">${getText('ec.view.editstyle')}</option>
						<option value="del">${getText('ec.view.delstyle')}</option>
					</select>
				
				</td>
				<td class="la" width="20%" align="right" padding-right="10px">${getHtmlText('ec.view.useInMore')}</td>
				<td class="co" width="30%"><input type="checkbox" id="useInMore" /></td>
			</tr>
			<tr id="callbackTr">
				<td class="la" width="30%" align="right" padding-right="10px">${getHtmlText('ec.view.refresh')}</td>
				<td class="co" width="30%">
					<input type="checkbox" id="iscallback" />			
				</td>
				<td class="la" width="30%" align="right" padding-right="10px">${getHtmlText('ec.view.ispower')}</td>
				<td class="co" width="30%">
					<input type="checkbox" id="ispower" />			
				</td>
			</tr>
			<tr id="permissionTr">
				<td class="la" width="20%" align="right" padding-right="10px">${getHtmlText('ec.view.ispower')}</td>
				<td class="co" width="30%"><input type="checkbox" id="ispermission" /></td>
			</tr>
			<tr id="urlTr">
				<td class="la" width="30%" align="right" padding-right="10px">${getHtmlText('ec.view.powerurl')}</td>
				<td class="co" colspan="3">
					<input type="text" id="operateurl" class="cui-edit-field" style="width:100%" />				
				</td>
			</tr>
			<tr id="eventTr">
				<td class="la" width="30%" align="right" padding-right="10px">${getHtmlText('ec.view.customevent')}</td>
				<td class="co" width="30%">
					<div style="float:left;width:100%;height:20px;margin-bottom:5px;border:1px solid #0059B3;overflow:auto;">
						<ul id="button_event_ul">
							<li class="editeventli" style="padding-top:2px">
							<a href="#" title="${getText('ec.view.editevent')}" style="text-decoration:underline;padding-left:20px;" onclick="listec.editButtonEvent('onclick')">onclick</a>：<input type="checkbox" id="button_property_event_onclick" value="" style="padding-right:10px"/>
							</li>
						</ul>
					</div>
					<br/>					
				</td>
			</tr>
			<tr id="iscustomfuncTr">
				<td class="la" width="30%" align="right" padding-right="10px">${getHtmlText('ec.view.iscallback')}</td>
				<td class="co" width="30%">
					<input type="checkbox" id="iscustomfunc" />			
				</td>
			</tr>
			<tr id="customfuncTr">
				<td class="co" colspan="4">
					<textarea  id="customfuncarea" class="cui-edit-textarea" style="width:100%;height:55px;margin-left:10px;"></textarea>
				</td>	
			</tr>
			<tr id="textareaTr">
				<td class="co" colspan="4">
					<textarea  id="buttonEventarea" class="cui-edit-textarea" style="width:100%;height:55px;margin-left:10px;"></textarea>
				</td>	
			</tr>
		</table>
	</div>
	<#--
	<div id="list_property_dlg" style="display:none;">
 		<div style="border:#CCC 1px dashed;margin:5px 5px 0 5px;padding:10px;overflow: auto;height:400px;" id="main_container">
			<table width="100%" align="center" bgcolor="#CCCCCC" cellspacing="1" id="listTable">
				<tr bgcolor="#EEEEEE" style="font-weight:bold;">
					<td height="20" align="center" width="80">${getText('ec.property.dafult')}</td>
					<td align="center" width="150">${getText('ec.view.name')}</td>
				</tr>
				<#list view.assModel.properties as property>
					<#if (property.isUsedForList)?? && (property.isUsedForList)>
						<tr class="nor" bgcolor="#FFFFFF" key="${(property.name)!}" propertyCode="${(property.code)!}" dbtype="${(property.type)!}" format="${(property.format)!}" <#if (property.format) == "SYSTEMCODE" || (property.format) == "ENUMERATE">  fill='${(property.fillcontent)!}' <#else> fill='' </#if>>
							<td height="20" align="center"><input style="border:none;" type="checkbox" name="checkshow" <#if (property.associatedProperty)?? && assMap[property.code]??>disabled="disabled"</#if>/></td>
							<td <#if (property.associatedProperty)?? && assMap[property.code]??> style='padding-left:20px;' <#else> </#if>>
							<#if (property.associatedProperty)?? && assMap[property.code]??>  "<a href='#' class='normala' onclick="toggleAss(this)" title="${getText('ec.view.associatepro')}"><span style="font-size:15px;width:13px;font-weight:bold;display:block;">-</span></a>" <#else> </#if>
							<span class="displayName">${(property.displayName)!}</span>
							</td>
						</tr>
						<#if assMap?? && assMap[property.code]??>
							<#list assMap[property.code] as p>
							<#if (p.isUsedForList)?? && (p.isUsedForList)>
								<tr class="ass" bgcolor="#FFFFCC" key="${(property.name)!}.${(p.name)!}" propertyCode="${(p.code)!}" dbtype="${(p.type)!}" format="${(property.format)!}" assTar="${(property.associatedProperty.code)!}" assOrg="${(property.code)!}"
								<#if (p.format) == "SYSTEMCODE" || (p.format) == "ENUMERATE" > 
									fill='${(p.fillcontent)!}'
								<#else> 
									fill='' 
								</#if> >
									<td height="20" align="center"><input style="border:none;" type="checkbox" name="checkshow"/></td>
									<td style="padding-left:20px;"><span class="displayName">${(p.displayName)!}</span></td>
								</tr> 
							</#if>	
							</#list>
						</#if>
					</#if>
				</#list>
			</table>
		</div>
	</div>
	-->
	<script type="text/javascript">
		var main_tab,listec;//全局变量
		$(function(){
			$('#form_select_elements').tabs("#form_select_elements div.accordion_pane", {tabs:'h2',effect:'slide',initialIndex:null});//初始化手风琴下拉
			main_tab = new CUI.TabView("main_form_tab",{tabposition:'top'});//初始化主设计区域页签
			listec = new CUI.EntityConfList(
					'${(view.code)!}',
					<#if ev?? && (ev.config)?? && (ev.config) != '' && (ev.config.PageConfig)??>
						<@s.property value="(ev.config)" escape=false/>
					<#else>
						<#noparse>{pageConfig:{colNum:6}}</#noparse>
					</#if>,
					main_tab,
					'${(ev.code)!}',
					'${(ev.version)!0}',
					'${(ev.view.fastQueryJson.code)!}',
					'${(ev.view.fastQueryJson.version)!0}',
					{'singlSelect' : {'SELECT':'${getText('ec.view.select')}'},
					 'multiSelect' : {'CHECKBOX':'${getText('ec.view.checkbox')}', 'MULTSELECT':'${getText('ec.view.multselect')}'},
					 'objectSelect' : {'TEXTFIELD':'${getText('ec.view.textfield')}', 'SELECTCOMP':'${getText('ec.view.selectcomp')}'},
					 'normalSelect' : {<#if fastfieldtypes??><#list fastfieldtypes?keys as key>'${key}':'${fastfieldtypes[key]}',</#list></#if>}
					},
					{
					'equalExp' : {'equal':'${getText('ec.view.equal')}'},
					'likeExp' : {'equal':'${getText('ec.view.like')}'},
					'textExp' : {'equal':'${getText('ec.view.equal')}', 'like':'${getText('ec.view.like')}', 'llike':'${getText('ec.view.llike')}','rlike':'${getText('ec.view.rlike')}'},
					'otherExp' : {'equal':'${getText('ec.view.equal')}', 'gequal':'${getText('ec.view.ge')}${getText('ec.view.equal')}', 'lequal':'${getText('ec.view.lt')}${getText('ec.view.equal')}', 'gthan':'${getText('ec.view.ge')}', 'lthan':'${getText('ec.view.lt')}'}
					}
					);
		});
	</script>	
</body>
<script type="text/javascript">	
	//自动设置中间编辑区域的宽度
	function _init_size(){
		$('.col-main').width($(window).width()-167);
		$('#form_area').width($(window).width()-169);
		$('#listSection').width($(window).width()-248);
		$('#listTable').width($(window).width()-248);
		$('#main_form_tab').width($(window).width());
		$('.etv-navset').height($(window).height()-250);
		$('.col-sub').height($(window).height()-83);
	}
	$(function(){
		_init_size();
		$('#assModels').change(function(){
			listec.modelViewChange(this);
		});
	});
	$(window).resize(function(){
		_init_size();				
	});
	setTimeout(function(){
		$('.etv-content').width($(window).width()-191);
	},200);
</script>
</html>