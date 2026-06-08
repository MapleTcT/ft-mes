<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
	<title>${getText(view.assModel.entity.name)}-${getText(view.displayName)}-${getText('ec.view.config.mobile')}-${getText('ec.view.list')}</title>
	<@ec_extraMobileTop/>
	<@loadpanel />
	<style>
	#enumerate_content_table td {
		vertical-align: middle;
		text-align: center;
		height : 20px;
	}
	#enumerate_content_table thead td {
		background: url('/bap/static/css/sprite_20120525.png') 0 -1340px repeat-x;
	}
	</style>
	<style type="text/css">
		.grid-s4m0 .main-wrap {width: 100%;float:left;height:100%;overflow:hidden;background-color:#93BFD8;border:1px dashed #E6E6E6;}
		.grid-s4m0 .col-sub { margin-left:2px;margin-top:8px;width: 220px; float:left;background-color:white;border:1px solid #3C7FB1}
		.grid-s4m0 .col-operate{ margin-right:3px;border:1px solid #a2a2a2;background-color: #d4d4d4;width: 80px;float:right;margin-top:8px;overflow:hidden;}
		.grid-s4m0 .col-top{width: 100%;height:30px;background-color:#D4D8DB;border-bottom:1px solid #ADACAB;}
		#form_select_elements{height:98%}
		#form_select_elements h2 {padding:5px 0;text-align:center;font-size:13px;font-weight:normal;border:1px solid #0369A3;cursor:pointer;background-color: #9cc0d6;}
		#form_select_elements h2.current {cursor:default;}
		#form_select_elements .accordion_pane {	border:1px solid #fff;	border-width:0 2px;	display:none;padding:0px;font-size:12px;}
		#form_select_elements .accordion_pane li {font-size:12px;color:#000000;cursor:pointer;line-height:18px;z-index:100;margin-left:4px;}	
		#form_select_elements .accordion_pane li.dragout{color:#BBBBBB;cursor:default;}
		#fast_select_elements{height:98%;}
		#fast_select_elements h2 {padding:5px 0;text-align:center;font-size:13px;font-weight:normal;border:1px solid #0369A3;cursor:pointer;background-color: #9cc0d6;}
		#fast_select_elements h2.current {cursor:default;}
		#fast_select_elements .accordion_pane {	border:1px solid #fff;	border-width:0 2px;	display:none;padding:0px;font-size:12px;height:347px;}
		#fast_select_elements .accordion_pane li {font-size:12px;color:#000000;cursor:pointer;line-height:18px;z-index:100;margin-left:4px;}	
		#fast_select_elements .accordion_pane li.dragout{color:#BBBBBB;cursor:default;}		
		#adv_select_elements{height:98%}
		#adv_select_elements h2 {padding:5px 0;text-align:center;font-size:13px;font-weight:normal;border:1px solid #0369A3;cursor:pointer;background-color: #9cc0d6;}
		#adv_select_elements h2.current {cursor:default;}
		#adv_select_elements .accordion_pane {	border:1px solid #fff;	border-width:0 2px;	display:none;padding:0px;font-size:12px;}
		#adv_select_elements .accordion_pane li {font-size:12px;color:#000000;cursor:pointer;line-height:18px;z-index:100;margin-left:4px;}	
		#adv_select_elements .accordion_pane li.dragout{color:#BBBBBB;cursor:default;}
		#main_form_tab_operate li{display:block;}
		#main_form_tab .etv-content{background-color:#F3F3F3!important}
		#main_form_tab .etv-navset{border:1px solid #2B82B2;}
		#main_design_container{padding:2px 2px 0 0;}
		.form_title{height:30px;width:99%;background-color:#d4d4d4;margin:6px 0px 0px 5px}
		.line-icon{width:70px;margin:7px 3px 2px 2px; border-color:#d4d4d4;overflow:hidden;}
		#form_area{position: relative;width:100%;height:95%;}
		#main_form_tab_operate{z-index: 1000;}
		#main_form_tab_operate li{margin-left:3px;width:80px;height:25px;cursor:pointer;display:block;overflow:hidden;}
		#editcolwidthdiv input {margin-right:5px;}
		.operateDiv{height:22px;background:#bababa;border:#CCC 1px dashed;}
		.cui-btn-leftmove {background: url('/bap/static/css/sprite_20120525.png') 0px -2050px no-repeat;}
		.cui-btn-rightmove {background: url('/bap/static/css/sprite_20120525.png') -61px -2050px no-repeat;}
		.button_design_ul_li{border:#CCC 1px dashed;float:left; height:25px;margin:1px;margin-right:10px;}
		.ec-list-topbtn{cursor:hand;}
		.ec-list-nextbtn{cursor:hand;}
		.ec-list-prevbtn{cursor:hand;}
		.ec-list-lastbtn{cursor:hand;}
		<#assign viewHeight = 500>
		<#--
		<#if view.openType == 'dialog'>
			<#if view.dialogType?? && view.dialogType != 'DIALOG_OTHER'>
				<#if view.dialogType?index_of('_1') gt 0>
					<#assign viewHeight = 164>
				<#elseif view.dialogType?index_of('_2') gt 0>
					<#assign viewHeight = 202>
				<#elseif view.dialogType?index_of('_3') gt 0>
					<#assign viewHeight = 330>
				<#elseif view.dialogType?index_of('_4') gt 0>
					<#assign viewHeight = 500>
				<#elseif view.dialogType?index_of('_5') gt 0>
					<#assign viewHeight = 600>
				</#if>
			<#else>
				<#assign viewHeight = view.height!500>
			</#if>
		</#if>
		-->
		.col-main{margin-left:5px;}
		.form_design_ul{position:relative;}
		.form_design_ul_li{border:#CCC 1px dashed;float:left; height:30px;margin:1px;margin-right:10px;position: absolute;}
		.form_design_ul_li dt{float:left;width:100px;text-align: right;padding-right:5px;padding-top:2px;line-height:25px;}
		.form_design_ul_li dd{float:left;text-align: right;padding-top:5px;width:100%;margin-top:3px}
		.form_design_ul_li dd textarea,.form_design_ul_li dd input,.form_design_ul_li dd select{width:99%;height:20px;}
		.form_design_ul_li dd textarea{border:green 1px solid;margin:0;}
		.form_design_ul_li dd input.cui-search-click{width:24px!important;}
		.form_design_ul_li dd input.cui-calpick{width:22px!important;}
		.form_design_ul_li dd input[type='checkbox']{width:20px;height:20px;}
		.form_design_ul_li dd input[type='radio']{width:20px;height:20px;}
		
		.form_design_ul_li div{display:none;position: absolute;left:5px;top:2px;cursor:move;width:20px;height:20px;background: url("/bap/static/ec/images/arrow_switch.png") center center no-repeat;}
		.div-selected{}
		.ui-selectee{}
		.ui-selecting { background: #FECA40; }
		.ui-selected { background: #F39814;}
		.couple-selected { background: #FFD27F;}
		.couple-sort-checked { background-color: #92D8FB;}
		.se-selected { background: #F39814;}
		.BlockInfoDiv {margin:2px;padding:4px;border-top:1px dashed #cccccc;}
		.editeventli {width:175px;text-align: right;}
		.datagird_th_td0 {width:5%;height:22px;overflow:hidden;text-align:center;padding:0 3px;background:url("/bap/static/css/sprite_20120525.png") repeat-x scroll 0 -1340px transparent;border-right:1px solid #FFFFFF}
		.datagird_th_td1 {width:95%;height:22px;overflow:hidden;text-align:center;padding:0 3px;background:url("/bap/static/css/sprite_20120525.png") repeat-x scroll 0 -1340px transparent;border-right:1px solid #FFFFFF}
		.datagird_tb_td0{width:5%;height:22px;text-align:center;overflow:hidden;background-color:#E7E7E7;border-top:1px solid #BABABA;border-left:1px solid #BABABA;border-bottom:1px solid #BABABA}
		.datagird_tb_td1{width:95%;height:22px;overflow:hidden;background-color:white;border:1px solid #BABABA;}
		.even-num {background-color:#EDEDED;}
		.unsort-selected{ #FFFFFF;}
		.sort-selected{background:#A3E0FF;}
		//.treeBox{border-top:1px solid #0369A3;height:30%;}
		//.treeBox h3{line-height:30px;height:30px;text-align:center;background:blanchedalmond;border-bottom:1px solid #0369A3;}
		#layoutTree{display: block;visibility: visible;z-index: 0;overflow:auto;} 
		#layout-config{height:497px;width:753px;}
		#layout-config-title{font-size:14px;height:22px;border-bottom:1px solid #e5e5e5;}
		#layout-config-title span{display:block;padding-left:2px;padding-top:2px;}
		#layout-config-choice{height:80px;}
		.layout-bt{margin:10px 25px 0 8px;padding:3px 10px;border:1px solid #e5e5e5;border-radius: 5px;}
		#layout-config-choice span{display:block;padding-top:2px;padding-left:20px;}
		.layout-configuration-title{clear:both;height:20px;font-size:14px;margin-bottom:15px;border-bottom:1px solid #e5e5e5;}
		.layout-configuration-title span{padding-bottom:2px;padding-left:2px;}
		.layout-config-border{margin-left:20px;}
		.layout-config-border input{vertical-align:middle;margin-right:5px;}
		.num-columns{font-size:12px;width:50px;height:18px;}
		.layout-options-row{height:340px;width:100%;}
		.layout-options-column{height:340px;width:100%;}
		.layout-options-column img{margin-left:60px;margin-top:5px;}
		.layout-options-row img{padding-top:55px;}
		.layout-column-selector{float:left;width:480px;height:335px;margin:0 3px;}
		.tb-fixedblock-select{height:30px;outline: none;margin-left:40px;margin-right:1px;}
		.tb-fixedblock-select + label{height:35px;display: inline-block;padding: 0 2px;vertical-align: middle;}
		.lr-fixedblock-select{height:30px;padding-top:2px;}
		.table-showing-columns{border-collapse:collapse;}
		.layout-options-row td{border: 1px solid #ccc;border-radius: 5px;margin:2px 0;vertical-align: top;}
		.layout-options-column td{border: 1px solid #ccc;border-radius: 5px;margin:2px 0;}
		.fixedblock-layout-bar{float:left;border: 1px dashed #ccc;border-radius: 5px;height:335px;width:125px;margin:0 2px;text-align:center;}
		.layout-fixedblock-select{padding-top:30px;height:30px;}
		.layout-fixedblock-select label{height:40px;display: inline-block;vertical-align: middle;}
		.optional-layout-bar{height:40px;width:98%;border:1px dashed #ccc;border-radius: 5px;margin:2px 0;}
		.layout-row-selector{width:99%;height:250px;margin:3px 0;}
		div.lr-fixed{height:99%;width:200px;float:left;border: 1px dashed #ccc;border-radius: 5px;margin:2px;}
		.row-fixed{float:left;height:99%;border: 1px solid #ccc;border-radius: 5px;margin:2px;}
		div.tb-fixed{width:99%;height:100px;border: 1px dashed #ccc;border-radius: 5px;margin:2px;display:inline-block;}
		.column-fixed{width:99%;border: 1px solid #ccc;border-radius: 5px;margin:2px;display:inline-block;}
		.layout-common{border: 1px solid #ccc;border-radius: 5px;margin: 2px;overflow:hidden;}
		div.layout{border:1px solid transparent;margin:0;border-radius:5px;}
		.layout-common div.urlBox{padding-top: 20px;text-align:center;}
		.layout-common div.searchBox{text-align:center;height:100%;}
		.layout-common .searchBox img{margin-top:20px;}
		.widget-configure-menu{background:url("/bap/static/images/icon-widget-menu.png") no-repeat 0px 0px;width: 21px;height: 21px;cursor: pointer;position: absolute;display:none;left: -1px;top: -1px;z-index: 2;}
		.layout-adjust-panel{border:1px solid #E8E8E8;box-shadow: 1px 1px 1px #888888;height:246px;width:150px;position:absolute;left:-1px;top:-2px;z-index: 9999;background-color:#F3F3F3;display:none;}
		.layout-adjust-title{width:100%;height:25px;line-height:25px;border-bottom:1px solid #ccc;}
		.layout-adjust-title span{padding-left:5px;padding-top:6px;}
		.layout-adjust-content{width:100%;}
		.layout-adjust-content ul{margin:0;padding:5px 0;}
		.layout-adjust-content li{list-style:none;line-height:1.5em;padding:1px 0px 4px 0px;cursor:pointer;}
		.layout-adjust-content li i{width:18px;height:18px;display:inline-block;vertical-align:middle;background-image:url("/bap/static/css/edit_20150318.png");background-repeat:no-repeat;}
		.layout-adjust-content li.moveL i{background-image:url("/bap/static/css/sprite_20120525.png");}
		.layout-adjust-content li.moveR i{background-image:url("/bap/static/css/sprite_20120525.png");}
		.layout-adjust-content li.over{background: #9cc0d6;}
		.layout-adjust-content span{padding:1px 0px 4px 6px;}
		.accordion_pane{height:78%;}
		.layoutMethod-selected{border-color:#0088CC;}
		div.layout-selected{border:1px solid #F39814;display:block;border-radius:5px;}
		.layout-tabs{list-style-type: none;height:33px;overflow:hidden;padding:0;margin:2px auto 0;font-size:12px;font-weight: bold;color: #0f78bc;border-bottom:1px solid #ccc;}
		.layout-tabs li{float:left;max-width:120px;min-width:70px;line-height: 29px;cursor: pointer;height:29px;position:relative;background:white;text-align: center;margin:0 1px;}
		.layout-tabs .tabs-span{width:100%;display:inline-block;white-space:nowrap;text-overflow:ellipsis;overflow:hidden;font-weight:normal;font-size:14px;}
		.layout-tabs .tab-active{background-color:#0F78BC;border: 1px solid #ddd;border-radius:5px;border-bottom-color: transparent;padding-bottom: 2px;color:white;}
		.tab-pane{display:none;}
		.layout-tabs .tabs-close{width: 15px;height: 15px;position: absolute;z-index: 5;top: 1px;right: 1px;background: url("/bap/static/tabview/assets/list_all.gif") no-repeat 1px -1117px;}
		#tabs-config {margin:10px;line-height:30px;}
		#tabs-config .tabs-Name{width:100%;}
		#tabs-config .tabs-Name tr{float:left;margin-right:5px;width:45%;}
		#tabs-config .tabs-Name tr td:first-child{padding-right:5px;width:15%;}
		#tabs-config .tabs-Name tr td:last-child{width:60%;padding-right:5px;}
		#tabs-num{font-size: 12px;width: 50px;height: 18px;}
		.layout-setting{padding:15px;}
		.layout-setting tr{line-height:30px;}
		.layout-setting td input[type="text"]{width:120px;height:20px;margin-right:5px;}
		.layout-setting .label{width:22%;text-align:left;}
		.layout-setting .val{width:75%;text-align:left;}
		.subLayout-setting{border-top:1px solid #ccc; padding-left:15px;}
		.subLayout-setting h2{font-size:14px; line-height:50px; padding-left:5px; color:#7796ab;}
		.subLayout-setting p span{margin-left:5px; text-align:right; width:20%;}
		.subLayout-setting p input{width:95px; margin:5px;margin-right:0;}
		.subLayout-setting ul{overflow:hidden; margin-top:10px;}
		.subLayout-setting ul li{float:left; margin:0;margin-right:5px; width:42%;}
		.subLayout-setting ul li input{width:95px; margin:5px 0;}
		#layout-setting-dlg .layout-style{position:relative;}
		#layout-setting-dlg .layout-style h3{position:absolute;left:10px;width: 1em;top: 40%;color:#7796ab;}
		#layout-setting-dlg .layout-style textarea{border:1px solid #ccc;height: 330px;width: 88%;margin: 5px;margin-left: 9%;}
		#layout-setting-dlg .edit-tabs{margin-top: 14px;margin-left: 14px;line-height: 36px;border-top: 5px solid #e3e3e3;background: #FFF;}
		.data-assoc-select{overflow:auto;height: 115px;}
		.data-assoc-select .data-assoc-check{margin:5px 5px 0 8px;}
		.data-echarts-select{overflow:auto;height: 115px;}
		.data-echarts-select .data-echarts-check{margin:5px 5px 0 8px;}
		.searchBox .search-config-wrapper {float:left;height:20px;width:24%;min-width:90px;margin-top: 5px;}		
		.searchBox .search-config-wrapper .search-config-button{width:90px;}
		.layout-cut{background-color: #f4f2e7;}
		a.help-link {
			display: inline-block;
			width: 15px;
			height: 15px;
			background: url(/bap/static/css/edit_20150318.png) 0px -3553px no-repeat;
		}
		.dialog_button_head_li {
			border: none;
		}
		.col-mobile-mian .dialog_button_head {
			width: 80px;
			height: 25px;
			background: url(/bap/static/tabview/assets/mview_btn.png) no-repeat;
			border: 0px;
			line-height: 25px;
			color: #328bbd;
			font-size: 13px;
			background-size: 100% 100%;
		}
		.dialog_button_head:hover {
			background: url(/bap/static/tabview/assets/mview_btn_p.png) no-repeat;
			background-size: 100% 100%;
			color: #fff;
		}
	</style>
	<script type="text/javascript">
		<#assign defaultColNum = 4>
		<#if view.openType == 'dialog'>
			<#assign defaultColNum = 4>
		</#if>
		<#-- 全局变量 -->
		var main_tab,ec;
		var _directAssosJson = ${(multSelectInfo.directAssosJson)!'{}'};
		var _indirectAssosJson = ${(multSelectInfo.indirectAssosJson)!'{}'};
		var _proj_config_flag = ${isProj?string};
		var _isReadOnlyMode = ${isReadOnlyMode?string};
		(function($){
			$(function(){
			
				<#-- 初始化手风琴下拉 -->
				$('#form_select_elements').accordion();
			//	$('#form_select_elements').tabs("#form_select_elements div.accordion_pane", {tabs:'h2',effect:'slide',initialIndex:null});
				//$('#fast_select_elements').accordion();//初始化手风琴下拉
				//$('#fast_select_elements').tabs("#fast_select_elements div.accordion_pane", {tabs:'h2',effect:'slide',initialIndex:null});//初始化手风琴下拉
				<#-- 初始化主设计区域页签 -->
				main_tab = new CUI.TabView("main_form_tab",{tabposition:'top', removable: true});
				<#-- 初始化主操作方法 -->
				ec = new CUI.EntityConfExtra(
					'${(view.code)!}',
					<#if (ev.config)?? && ev.config != ''>
						<#--<@s.property value="ev.config" escape=false/>-->{pageConfig:{colNum:${(ev.configMap.layout.pageConfig.colNum)!defaultColNum},layoutType:"${(ev.configMap.layout.pageConfig.layoutType)!}"}}
					<#else>
						{pageConfig:{colNum:${defaultColNum}}}
					</#if>,
					main_tab,
					'${(ev.code)!}',
					'${(ev.version)!0}',
					'${(isProj)?string!}',
					{
						'singlSelect' : {'SELECT':'${getText('ec.view.select')}', 'RADIO':'${getText('ec.view.radio')}'},
						'multiSelect' : {'CHECKBOX':'${getText('ec.view.checkbox')}', 'MULTSELECT':'${getText('ec.view.mulselect')}'},
						'objectSelect': {'TEXTFIELD':'${getText('ec.view.textfield')}','TEXTAREA':'${getText('ec.view.textarea')}', 'DATE':'${getText('ec.view.date')}', 'DATETIME':'${getText('ec.view.datetime')}', 'SELECTCOMP':'${getText('ec.view.selectcomp')}'},
						'normalSelect': {<#if fieldTypes??><#list fieldTypes?keys as key>'${key}':'${getText('${fieldTypes[key]!}')}'<#if key_has_next>,</#if></#list></#if>},
						'viewColumnTypes' : {<#if viewColumnTypes??><#list viewColumnTypes?keys as key>'${key}':'${getText('${viewColumnTypes[key]!}')}'<#if key_has_next>,</#if></#list></#if>},
						'viewFormats' :  {<#if viewFormats??><#list viewFormats?keys as key>'${key}':'${getText('${viewFormats[key]!}')}'<#if key_has_next>,</#if></#list></#if>}
					},
					_directAssosJson,
					_indirectAssosJson,
					'${(view.type)!}',
					'${(view.editViewType)!}',
					<#if view.mobile?? && view.mobile>1<#else>0</#if>,
					{
					'equalExp' : {'equal':'${getText('ec.view.equal')}', 'unequal':'${getText('ec.view.unequal')}'},
					'likeExp' : {'equal':'${getText('ec.view.like')}'},
					'textExp' : {'equal':'${getText('ec.view.equal')}', 'like':'${getText('ec.view.like')}', 'llike':'${getText('ec.view.llike')}','rlike':'${getText('ec.view.rlike')}'}
					}
				);
			});
		})(jQuery);
		
	</script>
</head>
<body class="ec-config-page" style="background-color:#f2f2f2" layoutCode="${(ev.configMap.layout.layoutCode)!ecCodeInit('layout')}" page_beforesave="${((ev.configMap.layout.pageConfig.beforeSave)!)?html}" ptPageInit="${((ev.configMap.layout.pageConfig.ptPageInit)!)?html}" renderOver="${((ev.configMap.layout.pageConfig.renderOver)!)?html}" page_onload="${((ev.configMap.layout.pageConfig.onload)!)?html}" page_onsave="${((ev.configMap.layout.pageConfig.onsave)!)?html}" page_aftersave="${((ev.configMap.layout.pageConfig.afterSave)!)?html}" page_beforesubmit="${((ev.configMap.layout.pageConfig.beforeSubmit)!)?html}" page_aftersubmit="${((ev.configMap.layout.pageConfig.afterSubmit)!)?html}">
	<@errorbar id="workbenchErrorBar" offsetY=83 />
		<div class="layout grid-s4m0"> 	
		<div class="col-top">
			<div>
				<h3 style="color:#3A70AA;padding-top:7px;padding-left:15px;float:left">
					${getText(view.assModel.entity.name)}-${getText(view.displayName)}-${getHtmlText('ec.project.view_setting')}-<#if view.type == 'EXTRA' || view.type == 'EDIT'>${getHtmlText('ec.view.edit')}<#else>${getHtmlText('ec.view.view')}</#if>
				</h3>
				<#if TempProjView?? && TempProjView.projFlag?? && TempProjView.projFlag>
				<h3 style="color:red;padding-top:7px;padding-left:5px;float:left">
					(${getText("ec.view.inherited")}-<#if TempProjView.projEnabled?? && TempProjView.projEnabled>${getText("ec.view.inherited.projenabled")}<#if !TempProjView.publishTime??>${getText("ec.engine.publishstste.not")}<#elseif (TempProjView.publishTime?long - TempProjView.modifyTime?long)?abs lt 1000>${getText("ec.engine.publishstste.published")}<#else>${getText("ec.engine.publishstste.waittopublish")}</#if><#else>${getText("ec.view.inherited.projenabled.not")}</#if>)
				</h3>
				</#if>
				<span style="color: #FFFFFF;font-size: 15px;font-weight: bold;margin: 8px 15px;;float:right">
            		<a href="/help/" target="_blank" title="帮助文档" class="help-link"></a>
            	</span>
			</div>
		</div>
		<div class='col-mobile-mian'>
		<div class="col-sub">
			<#-- 左侧 -->
			<div id="form_select_elements">
				<h2 class="current">${getHtmlText('ec.view.entityattribute')}</h2>
				<div class="accordion_pane" style="display:block;overflow:auto;height:97%">
					<ul class="main_properties_container">
						<#if subs?? && (subs.properties)?? && (subs.properties)?size &gt; 0>
							<#assign properties = (subs.properties)>
							<#list properties as p>
								<#if (p.name) != 'tableInfoId' && (p.name) != 'status' && (p.name) != 'id' && (p.name) != 'version' && (p.type) !='PROPERTYATTACHMENT'>
								<li  source='mobile' partDepend='common' propDefaultValue='${(p.defaultValue)!}' propertyCode='${(p.code)!}' namekey="${(p.displayName)!}" name='${(p.name)!}' columnType='${(p.type)!}' propShowFormat='${(p.format)!}' propShowType='${(p.fieldType)!}' entityCode='${(p.model.code)!}' multable='${(p.multable!false)?string('true','false')}'  nullable='${(p.nullable!true)?string('true','false')}' <#if p.isUsedForList?? && p.isUsedForList> list='true' <#else> list='false' </#if> fillContent='${(p.fillcontent)!}' layRec='${(p.name)!}' mnecode='${(p.isUsedMneCode!false)?string('true','false')}' propPrecision='${(p.decimalNum)!}'     seniorSystemCode='${(p.seniorSystemCode!false)?string('true','false')}' isCustom='${(p.isCustom!false)?string('true','false')}'>
									${getHtmlText('${(p.displayName)!}')}
								</li>
								</#if>	
							</#list>
						</#if>
						<#if subs?? && (subs.associatedInfos)??>
							<#assign associatedInfos = (subs.associatedInfos)>
							<#assign i = 1>
							<#list associatedInfos as ass>
								<li source='test' partDepend='common' assTar='${ass.targetProperty.code}' assPropertyName='${ass.targetProperty.name}' propertyCode="${ass.originalProperty.code!}" assOrg="${ass.originalProperty.code}" dbname='${(ass.targetProperty.model.modelName)!}' name='${(ass.originalProperty.name)!}' entityCode='${(ass.targetProperty.model.code)!}' relativeName='${(ass.targetProperty.model.tableName)!},${(ass.targetProperty.columnName)},${(ass.originalProperty.model.tableName)!},${(ass.originalProperty.columnName)!}' isCustom='${(ass.originalProperty.isCustom!false)?string('true','false')}'>
									<img sourceType='list' align='absmiddle' src='/bap/static/treeview/assets/ectree_colse.gif' flag='true' hasclick='false' onclick='ec.showAssPropertyList(this,"${(ass.targetProperty.model.code)!}")'></img>
									${getHtmlText('${(ass.originalProperty.displayName)!}')} [${getHtmlText('${(ass.targetProperty.model.name)!}')}]
								</li>	
								<#assign i = i+1>
							</#list>
						</#if>
					</ul>
				</div>
				<h2>${getHtmlText('ec.view.formelement')}</h2>
				<div class="accordion_pane"><ul id="form_elements_container">
				<#if fieldTypes??>
				<#list fieldTypes?keys as key>
					<#if key == 'LABEL' >
					<li source="el" showType="${key}">${getHtmlText('${fieldTypes[key]!}')}<#if key == 'DATAGRID'>(可编辑)</#if></li>
					<#-- 
					<#if view.assModel.isExtraCol?default(true)>
					<li source="el" fieldType="${key}">${fieldTypes[key]!}</li>
					<#else>
					<#if key == 'DATAGRID' || key == 'MULTSELECT'>
					<li source="el" fieldType="${key}">${fieldTypes[key]!}</li>
					</#if>
					</#if>
					-->
					</#if>
					<#if key == 'DATAGRID'>
					<li source="el" showType="DATATABLE">DataTable(可分页)</li>
					</#if>
				</#list>
				<li source="custom" isCustom="true" >${getHtmlText("foundation.common.currency.customfield")}</li>
				</#if>
				<li source="el" showtype="SEARCH">${getHtmlText('ec.view.property.search')}</li>
				</ul></div>
				
				
			</div>
		</div>
		<div class="col-main">
			<#assign viewWidth = 840>
			<#assign viewHeight = 600>
			<#if view.openType == 'dialog'>	
			<div  id="_dialogFrame" class="ewc-dialog-blove" style="visibility:visible;">
				<div class="hd" style="cursor: move;">
					<div class="hd-lc"></div>
					<div class="hd-cc"><h4>${getHtmlText('ec.project.view_setting')}</h4></div>
					<div class="dialog-handle"><a class="min" title="${getText('ec.view.mini')}" hidefocus="true" href="#"></a><a class="close" title="${getText('ec.view.accclose')}" href="javascript:window.close();"></a></div>
					<div class="hd-rc">
					</div>
				</div>
				<div class="bd" id="bd_dialogFrame" style="width:${(viewWidth)!500}px;height:${(viewHeight)!260}px;">
					<div class="bd-lb"></div>
					<div class="bd-rb"></div>
					<div class="bd-cb" style="overyflow:hidden;width:${(viewWidth-22)!478}px;">
						<div class="des"></div>
						<div class="content" style="overyflow:hidden;width:${((viewWidth)!500)-26}px;height:${(viewHeight)!260 - 4}px;">
						<div class="ewc-dialog-el">
			</#if>
							<div class="main-wrap" id="main_design_container"<#if view.openType == 'dialog'> style="overflow:hidden;width:${((viewWidth)!500)-30}px;height:${(viewHeight)!260}px;"</#if>>
							<#-- <div class="main-wrap" id="main_design_container"> 
								<div>
									<div class="form_title"></div>
								</div>-->
								<div id="form_area">
									<div id="main_form_tab" class="etv-navset" style="overflow: hidden;">
										<input type="hidden" id="viewCode" value="${(view.code)!}" />
										<input type="hidden" id="configType" value="edit" />
										<input type="hidden" id="viewType" value="${(view.type)!}" />
										<input type="hidden" id="delCellIds" name="delCellIds" />
										<input type="hidden" id="btDelCellIds" name="btDelCellIds" />
										<input type="hidden" id="delEventIds" name="delEventIds" />
										<input type="hidden" id="delValidateIds" name="delValidateIds" />
										<input type="hidden" id="delLayoutNames" name="delLayoutNames"/>
										<div id="buttonOperateDiv" style="margin:10px 10px 15px 10px;padding-bottom:5px;background:#F3F3F3!important;border:1px solid #0099CC;">
											<div class="operateDiv">
												<label for="button"><font style="font-weight:bolder">${getHtmlText('ec.view.configoperator')}</font></label>
												<div style="padding-left:20px;display:inline-block">
												<#if (view.type)?? && view.type!='REFERENCE'>
												<a class="cui-btn mr10 cui-btn-add" href="#" onclick="ec.addOperateButton()">${getText('ec.view.add')}</a>
												</#if>
												<a class="cui-btn mr10 cui-btn-edit" href="#" onclick="ec.modifyOperateButton()">${getText('common.button.edit')}</a>
												<a class="cui-btn mr10 cui-btn-del" href="#" onclick="ec.deleteOperateButton()">${getText('ec.view.delete')}</a>
												<a class="cui-btn mr10 cui-btn-leftmove" href="#" onclick="ec.leftmove('buttonOperateDiv')">${getText('ec.view.toleft')}</a>
												<a class="cui-btn mr10 cui-btn-rightmove" href="#" onclick="ec.rightmove('buttonOperateDiv')">${getText('ec.view.toright')}</a>
												<#if (view.type)?? && view.type=='REFERENCE'>
												<a class="cui-btn mr10 cui-btn-setting" href="#" onclick="ec.selectListForButton()">${getText('ec.view.refList')}</a>
												</#if>
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
																	<#if (operateButton.isHide)??>
																		isHide="${(operateButton.isHide)?string('true','false')}"
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
																	<#if (operateButton.isPublished)??>
																		isPublished="${(operateButton.isPublished)?string('true','false')}"
																	</#if>
																	<#if (operateButton.isSignatureConfig)??>
																		isSignatureConfig="${(operateButton.isSignatureConfig)?string('true','false')}"
																	</#if>
																	<#if (operateButton.permissionFromName)??>
																		permissionFromName="${((operateButton.permissionFromName)!)?html}" 
																	</#if>
																	<#if (operateButton.permissionFromCode)??>
																		permissionFromCode="${((operateButton.permissionFromCode)!)?html}" 
																	</#if>
																	 class="button_design_ul_li dialog_button_head_li" onmousedown="ec.selectListLi(this)" <#if !(operateButton.operatetype)?? || ((operateButton.operatetype)?? && (operateButton.operatetype) != 'SEPARATE')>ondblclick="ec.modifyOperateButton()"></#if><dd><#if (operateButton.operatetype)?? && (operateButton.operatetype) == 'SEPARATE'><input type="button" style="width:30px" value="|"><#else><input type="button" class="dialog_button_head btn_pointer" value="${getText('${(operateButton.namekey)!(operateButton.showname)!}')}"></#if></dd></li>
																</#list>
															</#if>
															<#break>
														</#if>
														</ul>
													</#list>
													</#if>
													<#else>
														<ul id="operateButton_ul" sectionCode="${ecCodeInit('section')}">
														</ul>
													</#if>	
												
												</div>	
											</div>
										</div>
										<hr style="margin-left:8px;margin-right:8px;display:none">
										<#if !((view.type)?? && view.type=='REFERENCE')>
										<hr style="margin-left:8px;margin-right:8px">
										</#if>
										<ul class="etv-nav">
											<#if ev?? && !(ev.config)?has_content>
												<li class="selected" namekey="ec.view.commoninfo">${getHtmlText('ec.view.commoninfo')}</li>
											<#else>
												<#assign commoninfo = getText('ec.view.commoninfo')>
												<#if (ev.configMap)?? && (ev.configMap.layout)??>
												<#list (ev.configMap.layout)['tabs'] as tab1>
													<#if !tab1.namekey?? && (tab1.name)?? && commoninfo?? && commoninfo == tab1.name>
														<li class="selected" namekey="ec.view.commoninfo">${getHtmlText('ec.view.commoninfo')}</li>
													<#else>
														<li  namekey="${(tab1.namekey!)}" paneId="home">${getHtmlText('${(tab1.namekey)!((tab1.name)!)}')}</li>
													</#if>
												</#list>
												</#if>
											</#if>
											<#if (view.project.workflowEnabled)??><li fixed="true">${getHtmlText('ec.view.procinfo')}</li></#if>
										</ul>
										<div class="etv-content-mobile-style">
										<div class="etv-mobile-bg">
											  <div class="bg-top"><span class="circle-wrap"><i></i><i></i><i></i></span></div>
											  <div class="bg-bottom"><i></i></div>
										</div>
										<div style="background-color:#e3f1f9;overflow-y: auto;" class="etv-content" >
											<#if ev?? && !(ev.config)?has_content>
											<input type="hidden" id="currentCounter" value="1"/>
											<input type="hidden" id="layoutnums" name="layoutnums" value="1" />
											<div class="tab-pane layout-common" layoutname="tabs-1" isborder="1" layno="1" tabno="1">
												<div class="confIcon" style="position:absolute;">
													<div class="widget-configure-menu">&nbsp;</div>
												</div>
											</div>
											<#else>
												<#if (ev.configMap)?? && (ev.configMap.layout)??>
													<input type="hidden" id="currentCounter" value="${ev.configMap.layout.currentCounter}"/>
													<input type="hidden" id="layoutnums" name="layoutnums" value='${ev.configMap.layout.layoutnums}'/>
													<script type="text/javascript">
													$(function(){
														$('.col-main').width($('.col-mobile-mian').width()-450);
														<#if view.openType == 'dialog'>	
														$('#form_area .etv-content').width(${(viewWidth)!500} - 42);
														$('#form_area .etv-content').height(${(viewHeight)!260} - 42 -108);
														$('#form_area .etv-content > .tab-pane').height($('#form_area .etv-content').height()-15);
														<#else>
														<#if (view.type)?? && view.type=='REFERENCE'>
															$('#form_area .etv-content').height($(window).height()-205); //参照配置界面
														<#else>
															$('#form_area .etv-content').height($(window).height()-315); //列表配置界面
														</#if>
														$('#form_area .etv-content').width($('.col-main').width()-10);
														$('#form_area .etv-content > .tab-pane').height($('#form_area .etv-content').height()-10);
														</#if>															
													});
													</script>
													<#list (ev.configMap.layout)['tabs'] as tab>
														<#if tab.layout?? && tab.layoutProperties?? &&  tab.layoutProperties.layoutmethod?? && tab.layoutProperties.layoutmethod != "container" >
														<div class="tab-pane ui-droppable layout" parlayoutname="${tab.layoutProperties.parlayoutname!}" layoutname="${tab.layoutProperties.layoutname!}" layoutmethod="${tab.layoutProperties.layoutmethod!}" nums="${tab.layoutProperties.nums!}"  isborder="${tab.layoutProperties.isborder!}" isreadonlyBak="${tab.layoutProperties.isreadonlyBak!}" <#if tab.layoutProperties.ptRealTimeLoad??>ptRealTimeLoad="${tab.layoutProperties.ptRealTimeLoad}"</#if> <#if tab.layoutProperties.tabViewIndex??>tabViewIndex="${tab.layoutProperties.tabViewIndex!}"</#if> isreadonly="${tab.layoutProperties.isreadonly!}" <#if tab.layoutProperties.cssstyle??>cssstyle="${tab.layoutProperties.cssstyle!}"</#if> layno="${tab.layoutProperties.layno!}" islefttopfixed="<#if tab.layoutProperties.islefttopfixed??>${tab.layoutProperties.islefttopfixed?string('true','false')}</#if>" isrightbottomfixed="<#if tab.layoutProperties.isrightbottomfixed??>${tab.layoutProperties.isrightbottomfixed?string('true','false')}</#if>"  style="overflow:hidden;">
															<div class="confIcon" style="position:absolute;">
																<div class="widget-configure-menu">&nbsp;</div>
															</div>
															<#list tab.layout as layouts >
																<@extraView layouts=layouts parLayoutmethod="${tab.layoutProperties.layoutmethod!}" parHaslefttopfixeds="${(tab.layoutProperties.islefttopfixed)?string}" parHasrightbottomfixeds="${(tab.layoutProperties.isrightbottomfixed)?string}" >
																</@extraView>
															</#list>
														<#elseif tab.tabs?? >
														<div class="tab-pane ui-droppable layout" parlayoutname="${tab.layoutProperties.parlayoutname!}" layoutname="${tab.layoutProperties.layoutname!}" layoutmethod="${tab.layoutProperties.layoutmethod!}" nums="${tab.layoutProperties.nums!}"  isborder="${tab.layoutProperties.isborder!}" isreadonlyBak="${tab.layoutProperties.isreadonlyBak!}" <#if tab.layoutProperties.ptRealTimeLoad??>ptRealTimeLoad="${tab.layoutProperties.ptRealTimeLoad}"</#if> <#if tab.layoutProperties.tabViewIndex??>tabViewIndex="${tab.layoutProperties.tabViewIndex!}"</#if> isreadonly="${tab.layoutProperties.isreadonly!}" <#if tab.layoutProperties.cssstyle??>cssstyle="${tab.layoutProperties.cssstyle!}"</#if> layno="${tab.layoutProperties.layno!}" islefttopfixed="<#if tab.layoutProperties.islefttopfixed??>${tab.layoutProperties.islefttopfixed?string('true','false')}</#if>" isrightbottomfixed="<#if tab.layoutProperties.isrightbottomfixed??>${tab.layoutProperties.isrightbottomfixed?string('true','false')}</#if>"  style="overflow:hidden;">
															<div class="confIcon" style="position:absolute;">
																<div class="widget-configure-menu">&nbsp;</div>
															</div>
															<ul class="layout-tabs"></ul>
															<#list tab.tabs as tabs >
																<@extraView layouts=tabs parLayoutmethod="${tab.layoutProperties.layoutmethod!}" parNums="${tab.layoutProperties.nums!}" >
																</@extraView>
															</#list>
														<#else>
														<div class="tab-pane ui-droppable layout-common" parlayoutname="${tab.layoutProperties.parlayoutname!}" layoutname="${tab.layoutProperties.layoutname!}" layoutmethod="${tab.layoutProperties.layoutmethod!}" nums="${tab.layoutProperties.nums!}"  isborder="${tab.layoutProperties.isborder!}" isreadonlyBak="${tab.layoutProperties.isreadonlyBak!}" <#if tab.layoutProperties.ptRealTimeLoad??>ptRealTimeLoad="${tab.layoutProperties.ptRealTimeLoad}"</#if> <#if tab.layoutProperties.tabViewIndex??>tabViewIndex="${tab.layoutProperties.tabViewIndex!}"</#if> isreadonly="${tab.layoutProperties.isreadonly!}" <#if tab.layoutProperties.cssstyle??>cssstyle="${tab.layoutProperties.cssstyle!}"</#if> layno="${tab.layoutProperties.layno!}" islefttopfixed="<#if tab.layoutProperties.islefttopfixed??>${tab.layoutProperties.islefttopfixed?string('true','false')}</#if>" isrightbottomfixed="<#if tab.layoutProperties.isrightbottomfixed??>${tab.layoutProperties.isrightbottomfixed?string('true','false')}</#if>" <#if tab.layoutProperties.url?? >url="${tab.layoutProperties.url!}"</#if>  <#if tab.layoutProperties.showmethod?? >showmethod="${tab.layoutProperties.showmethod!}"</#if> <#if tab.layoutProperties.layoutcontent??>layoutcontent="${tab.layoutProperties.layoutcontent}"</#if>>
															<div class="confIcon" style="position:absolute;">
																<div class="widget-configure-menu">&nbsp;</div>
															</div>
															<#if tab.layoutProperties?? && tab.layoutProperties.layoutcontent?? && tab.layoutProperties.layoutcontent == 'webFrame' >
															<div class='urlBox'><img src='/bap/static/images/urlImg.png' /></div>
															</#if>																
														</#if>														
														</div>														
													</#list>
												</#if>
											</#if>
										</div>
										</div>
									</div>
									</div>
								</div>
			<#if view.openType == 'dialog'>					
							</div>
						</div>
					</div>
				</div>
			
				<div class="ft">
					<div class="ft-lc"></div>
					<div class="ft-cc"></div>
					<div class="ft-rc"></div>
				</div>
			</div>
			</#if>
		</div>
		<div class="col-operate">
			<div>
				<ul id="main_form_tab_operate" >
					<li/>
					<li title="${getText('ec.view.pageProperty')}"><button class="Dialog_button btn_pointer" onclick="ec.cellPageProperty()" type="button">${getHtmlText('ec.view.pageProperty')}</button></li>
					<li title="${getText('ec.view.listproperty')}"><button class="Dialog_button btn_pointer" onclick="ec.listProperty()" type="button">${getHtmlText('ec.view.listproperty')}</button></li>
					<#--<li title="${getText('ec.view.addSection')}"><button class="Dialog_button btn_pointer" onclick="ec.addSection()" type="button">${getHtmlText('ec.view.addSection')}</button></li>
					<li title="${getText('ec.view.delSection')}" ><button class="Dialog_button btn_pointer" onclick="ec.delSection()" type="button">${getHtmlText('ec.view.delSection')}</button></li>-->
					<li title="${getText('ec.view.sectionattr')}"><button class="Dialog_button btn_pointer" onclick="ec.sectionProperty()" type="button">${getHtmlText('ec.view.sectionattr')}</button></li>
					<li title="${getText('ec.view.addline')}"><button class="Dialog_button btn_pointer" onclick="ec.addCell(1)" type="button">${getHtmlText('ec.view.addline')}</button></li>
					<li  title="${getText('ec.view.deleteline')}"><button class="Dialog_button btn_pointer" onclick="ec.delRow()" type="button">${getHtmlText('ec.view.deleteline')}</button></li>
					<li><hr class="line-icon"></hr></li>
					<li title="${getText('ec.view.splitcell')}"><button class="Dialog_button btn_pointer" onclick="ec.splitCell2()" type="button">${getHtmlText('ec.view.splitcellEdit')}</button></li>
					<li title="${getText('ec.view.delcell')}"><button class="Dialog_button btn_pointer" onclick="ec.delCell()" type="button">${getHtmlText('ec.view.delcell')}</button></li>
					<li title="${getText('ec.view.cellproperty')}"><button class="Dialog_button btn_pointer" onclick="ec.cellProperty()" type="button">${getHtmlText('ec.view.cellproperty')}</button></li>
					<li><hr class="line-icon"></hr></li>
					<li title="${getText('ec.view.list.dcsetclick')}"><button class="Dialog_button btn_pointer" onclick="dataClassific()" type="button">${getHtmlText('ec.view.list.dcset')}</button></li>
					<li title="${getText('ec.view.list.fastsetclick')}"><button class="Dialog_button btn_pointer" onclick="fastQuerySetting()" type="button">${getHtmlText('ec.view.list.fastset')}</button></li>
					<li title="${getText('ec.view.sortOrderBy')}"><button class="Dialog_button btn_pointer" onclick="ec.sortOrderBy(this,'${view.assModel.code}')" type="button">${getHtmlText('ec.view.sortOrderBy')}</button></li>
				</ul>
			</div>
		</div>
		</div>
		<#if !isReadOnlyMode>
		<div class="mobile-design-btn" id="design-button" style="height:30px;width:100%;overflow:hidden;clear:both;text-align:center;padding-top:10px">
			<button class="Dialog_button btn_pointer" onclick="ec.save()" type="button">${getHtmlText('common.button.save')}</button>&#160;&#160;&#160;&#160;&#160;
			<button class="Dialog_button btn_pointer" onclick="ec.save(true)" type="button">${getHtmlText('common.button.saveexit')}</button>&#160;&#160;&#160;&#160;&#160;
			<button class="Dialog_button btn_pointer" onclick="ec.publish()" type="button">${getHtmlText('ec.view.publishview')}</button>&#160;&#160;&#160;&#160;&#160;
			<#--<button class="Dialog_button btn_pointer" onclick="ec.preview()" type="button">${getText('ec.view.preview')}</button>-->
		</div>
		</#if>
	</div>
	
	<div id="form_set_item_property_dlg" style="display:none">
		<@errorbar id="EditDialogErrorBar" />
		<input type="hidden" id="form_prop_default_val" />
		<input type="hidden" id="form_property_default_val" />
		<input type="hidden" id="form_value_changed_val"  />
		<input type="hidden" id="form_showtype_changed_val"  />
		<input type="hidden" id="form_showformat_changed_val"  />
		<div id="dlg-div" class="dlg-etv-navset">
			<div class="etv-scrollbar" style="margin:0 5px;">
				<ul class="etv-nav" style="display: block;">
					<li class="selected" name="proertyTabs" id="base_attr">
						<span class="etv-nav-span">
							<em class="etv-nav-em" style="width: 90px;">${getText('ec.view.cell.property')}</em>
						</span>
						<span class="etv-nav-span-r"></span>
					</li>
					<li name="proertyTabs" id="event_defined">
						<span class="etv-nav-span">
							<em class="etv-nav-em" style="width: 90px;">${getText('ec.view.cell.event')}</em>
						</span>
						<span class="etv-nav-span-r"></span>
					</li>
					<li name="proertyTabs" id="css_defined">
						<span class="etv-nav-span">
							<em class="etv-nav-em" style="width: 90px;">${getText('ec.view.cell.style')}</em>
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
				<td class="co" width="30%"><input type="text" id="form_property_name" class="cui-edit-field" style="width:100%" /></td>
				<td class="la showname" width="20%" align="right" padding-right="10px">${getHtmlText('ec.view.showname')}</td>
				<td class="co showname" width="30%">
				<#-- <input type="text" id="form_property_show_name" class="cui-edit-field" style="width:100%" /> -->
				<#--international_propertyshowName    international_propertyshowName_showName -->
				<@international name="formProperty.showName" isNew=true isOldEdit=true moduleCode="${view.assModel.entity.module.artifact}" key="" style="width:100%;margin-right: -5px;" cssClass="cui-edit-field" />
				</td>
			</tr>
			<tr class="btn-set" style="display:none">
				<td class="la" width="20%" align="right" padding-right="10px"><label>${getHtmlText('ec.view.showvalue')}</label></td>
				<td class="co" width="30%">
					<select id="btn_lr"  class="cui-edit-field" style="width:96%">
						<option value="right">${getText('ec.view.right')}</option>
						<option value="left">${getText('ec.view.left')}</option>
					</select>
				</td>
			</tr>
			<tr id="selectDisplayTypeTR">
				<td class="la" width="20%" align="right" padding-right="10px">${getHtmlText('ec.view.cell.fieldType')}</td>
				<td class="co" width="30%">
					<select id="form_property_column_type" class="cui-edit-field" style="width:100%;display:none"></select>
					<input type="text" id="form_property_column_type_input" class="cui-edit-field cui-readonly-field" readonly="readonly" style="width:100%"/>
				</td>
				<td class="la" width="20%" name="td_showType" align="right" padding-right="10px">${getHtmlText('ec.view.cell.showType')}</td>
				<td class="co" width="30%" name="td_showType">
					<select id="form_property_field_type" class="cui-edit-field" style="width:100%"></select>
					<input type="text" id="form_property_field_type_input" class="cui-edit-field cui-readonly-field" readonly="readonly" style="width:100%;display:none" />
				</td>
			</tr>
			<tr id="tr_showFormat">
				<td class="la" width="20%" align="right" padding-right="10px">${getHtmlText('ec.view.cell.showFormat')}</td>
				<td class="co" width="30%">
					<select id="form_property_show_format" class="cui-edit-field" style="width:100%"></select>
					<input type="text" id="form_property_show_format_input" class="cui-edit-field cui-readonly-field" readonly="readonly" style="width:100%;display:none" />
				</td>
				<td class="la" width="20%" align="right" padding-right="10px"><label id="iscomplexLable">${getHtmlText('ec.view.iscomplexlable')}</label></td>
				<td class="co" width="30%">
					<select id="selectcomp_type"  class="cui-edit-field" style="display:none;width:100%">
					</select>
				</td>
			</tr>
			<tr class="iscrosscompany-tr" style="display:none;">
				<td class="la" width="20%" align="right" padding-right="10px"><label>${getHtmlText('ec.view.iscrosscompany')}</label></td>
				<td class="co" width="30%"><input type="checkbox" id="form_property_iscrosscompany" /></td>
			</tr>
			<tr id="defaultTR" style="display:none">
					<td class="la" width="20%" align="right" padding-right="10px">
						<div id="form_defaultValue_div" style="width:100%">
							${getHtmlText('ec.property.default.td')}
						</div>
					</td>
					<td class="co" width="80%" colspan="3">
						<div id="form_text_defaultValue_div" style="display:none;width:100%">
							<input type="text" id="form_text_defaultValue" class="cui-edit-field" style="width:100%" onChange="_changeDefaultValue(this)" />
						</div>
						<div id="dateRadio_div" style="display:none;width:100%">
							<input type="radio" name="dateRadio" value="today" onClick="_changeDefaultValue(this)">${getHtmlText('ec.property.default.today')}&nbsp;</input>
							<input type="radio" name="dateRadio" value="firstday" onClick="_changeDefaultValue(this)"/>${getHtmlText('ec.property.default.firstdayOfMonth')}&nbsp;
							<input type="radio" name="dateRadio" value="lastday" onClick="_changeDefaultValue(this)"/>${getHtmlText('ec.property.default.lastdayOfMonth')}&nbsp;
							<input type="radio" name="dateRadio" value="nextsevenday" onClick="_changeDefaultValue(this)"/>${getHtmlText('ec.property.default.later')}&nbsp;
						</div>
						<div id="dateTimeRadio_div" style="display:none;width:100%">
							<input type="radio" name="dateTimeRadio" value="currentTime" onClick="_changeDefaultValue(this)"/>${getHtmlText('ec.property.default.currenttime')}&nbsp;
							<input type="radio" name="dateTimeRadio" value="firstday" onClick="_changeDefaultValue(this)"/>${getHtmlText('ec.property.default.firstdayOfMonth')}&nbsp;
							<input type="radio" name="dateTimeRadio" value="lastday" onClick="_changeDefaultValue(this)"/>${getHtmlText('ec.property.default.lastdayOfMonth')}&nbsp;
							<input type="radio" name="dateTimeRadio" value="nextsevenday" onClick="_changeDefaultValue(this)"/>${getHtmlText('ec.property.default.later')}&nbsp;
						</div>
						<div id="ck_boolean_div" style="display:none;width:100%">
							<input type="checkbox" id="ck_boolean" value="true" onChange="_changeDefaultValue(this)"/>${getHtmlText('ec.property.default.yes')}
						</div>
						<div id="ck_currentUser_div" style="display:none;width:100%">
							<input type="checkbox" id="ck_currentUser" value="currentUser" onChange="_changeDefaultValue(this)"/>${getHtmlText('ec.property.default.currentStaff')}
						</div>
						<div id="ck_currentDepart_div" style="display:none;width:100%">
							<input type="checkbox" id="ck_currentDepart" value="currentDepart" onChange="_changeDefaultValue(this)"/>${getHtmlText('ec.property.default.currentDepartment')}
						</div>
						<div id="ck_currentPost_div" style="display:none;width:100%">
							<input type="checkbox" id="ck_currentPost" value="currentPost" onChange="_changeDefaultValue(this)"/>${getHtmlText('ec.property.default.currentPosition')}
						</div>
						<div id="ck_currentComp_div" style="display:none;width:100%">
							<input type="checkbox" id="ck_currentComp" value="currentComp" onChange="_changeDefaultValue(this)"/>${getHtmlText('ec.property.default.currentCompany')}
						</div>
						<div id="ck_syscode_div" style="display:none;width:100%">
							<select id="ck_syscode" name="ck_syscode" class="cui-edit-field" style="width:100%" onChange="_changeDefaultValue(this)"></select>
						</div>
						<script type="text/javascript">
							var ckradio = null;
							function _changeDefaultValue(obj){
								if(obj.type=="text"){
									CUI('#form_property_default_val').val(CUI(obj).val());
								}else if(obj.type=="radio"){
									if(obj==ckradio){
										obj.checked=false;
										ckradio = null;
										CUI('#form_property_default_val').val('');
									}else{
										ckradio = obj;
										CUI('#form_property_default_val').val(CUI(obj).val());
									}
								}else if(obj.type=="checkbox"){
									if(obj.id=="ck_boolean"){
										if(obj.checked){
											CUI('#form_property_default_val').val(CUI(obj).val());
										}else{
											CUI('#form_property_default_val').val("false");
										}
									}else if(obj.id=="ck_currentUser" || obj.id=="ck_currentDepart" || obj.id=="ck_currentPost" || obj.id=="ck_currentComp"){
										if(obj.checked){
											CUI('#form_property_default_val').val(CUI(obj).val());
										}else{
											CUI('#form_property_default_val').val("");
										}
									}
								}else{
									CUI('#form_property_default_val').val(CUI(obj).val());
								}
								if(CUI('#form_property_default_val').val()!=CUI('#form_prop_default_val').val()){
									CUI('#form_value_changed_val').val(true);
								}
							}
						</script>
					</td>
				</tr>
			<tr class="not-btn-set">
				<td class="la" width="20%" align="right" padding-right="10px"><label id="isreadOnlyLable">${getHtmlText('ec.view.isreadonly')}</label></td>
				<td class="co" width="30%"><input type="checkbox" id="form_property_is_readonly" /></td>
				<td class="la" width="20%" align="right" padding-right="10px"><label id="operator" style="display:none;">${getHtmlText('ec.flowActive.doperate')}</label></td>
				<td class="co" width="30%">
				<select id="form_property_exp_type" class="cui-edit-field" style="display:none;width:96%">
				</select></td>
			</tr>
			<tr class="case-tr">
				<td class="la" width="20%" align="right" padding-right="10px"><label>${getHtmlText('ec.view.caseSensitive')}</label></td>
				<td class="co" width="30%"><input type="checkbox" id="form_property_caseSensitive" /></td>
				<#--<td class="la" width="20%" align="right" padding-right="10px"><label>${getHtmlText('ec.view.crosscol')}</label></td>
				<td class="co" width="30%"><input type="checkbox" id="form_property_crosscol" /></td>--> 
			</tr>
			<tr id="ref_condition">
				<td class="la" width="20%" align="right" padding-right="10px"><label>${getHtmlText('ec.view.refCondition')}</label>
				<span id="refConditionHelpinfo" class="baphelp-icon"></span>
<div id="refConditionHelpinforef" style="display:none">
<p class="baphelp-info">当单元属性为对象类型，可传自定义条件给参照页面。 </p>
<p class="baphelp-example">范例</p>
<div class="baphelp-code">
<pre><code><span>return "year=2008 &amp;entity=crossCom " ;</span></code></pre>
</div>
<p class="baphelp-hint">注意：该功能需要参照列表的表格属性配置自定义条件，具体见其帮助说明。</p>
</div>
					<script type="text/javascript">
							$('#refConditionHelpinfo').helptip({refElm: "#refConditionHelpinforef", html: true , isCustom :false, width: 460 , title :"说明"});
						</script>
				</td>
				<td class="co" colspan="3">
					<textarea id="ref_condition_content" class="cui-edit-textarea" style="width:100%;height:50px;margin-top:5px"></textarea>
				</td>
			</tr>
			<tr class="isrefselect-tr not-btn-set">
				<td class="la" width="20%" align="right" padding-right="10px"><label>${getHtmlText('ec.view.isrefselect')}</label></td>
				<td class="co" width="30%"><input type="checkbox" id="form_property_isrefselect" /></td>
			</tr>
			<tr class="containLower-tr not-btn-set">
				<td class="la" width="20%" align="right" padding-right="10px"><label>${getHtmlText('ec.view.containLower')}</label></td>
				<td class="co" width="30%"><input type="checkbox" id="containLower" /></td>
			</tr>
		</table>
		<div id="div-event" style="width:100%;align:left;height:300px;overflow-y:auto;display:none">
			<table id="tab-event" class="infoTable" cellpadding="0" cellspacing="0" border="0" width="95%" align="left" style="margin-top: 10px">
				<tr>
					<td class="la" width="20%" align="right" padding-right="10px"><label>${getHtmlText('ec.view.cell.eventType')}</label></td>
					<td class="co" width="30%">
						<select id="events-select" class="cui-edit-field" style="width:100%">
						</select>
					</td>
					<td class="la" width="20%" style="text-align:left;padding-left:10px">
						<img onclick="ec.addnewevent($('#events-select'),$('#tab-event1'))" title="${getText('ec.view.addevent')}" src="/bap/static/foundation/images/icon-add.gif" /><br/>
					</td>
					<td class="la" width="30%"></td>
				</tr>
				<tr>
					<td colspan="4">
						<table id="tab-event1" class="infoTable" cellpadding="0" cellspacing="0" border="0" width="100%" align="left" style="margin-top: 10px;">
							<tr style="height:0px">
								<td class="la" style="width:15%;height:0px;text-align:right"></td>
								<td style="width:35%;height:0px" class="co"></td>
								<td class="la" style="width:45%;height:0px;text-align:right"></td>
								<td style="width:5%;height:0px" class="co"></td>
							</tr>
						</table>
					</td>
				</tr>
			</table>
		</div>
		<table id="tab-style" class="infoTable" cellpadding="0" cellspacing="0" border="0" width="95%" align="left" style="margin-top: 10px;display:none">
			<tr style="height:0px">
				<td style="width:20%;height:0px;text-align:right" class="la"></td>
				<td class="co" style="width:30%;height:0px"></td>
				<td style="width:20%;height:0px;text-align:right" class="la"></td>
				<td class="co" style="width:30%;height:0px"></td>
			</tr>
			<tr class="not-btn-set">
				<td class="la" style="width:20%" align="right" padding-right="10px">${getHtmlText('ec.view.customstyle')}
				<span id="customStyleHelpinfo" class="baphelp-icon"></span>&#160;
<div id="customStyleHelpinforef" style="display:none">
	<p class="baphelp-info">自定义样式</p>
	<p class="baphelp-example">范例</p>
<div class="baphelp-code">
<pre><code><span>background-color:red;</span>
</code></pre>
</div>
<p class="baphelp-hint">注意：直接写css样式。</p>
</div>
					<script type="text/javascript">
							$('#customStyleHelpinfo').helptip({refElm: "#customStyleHelpinforef", html: true , isCustom :false, width: 180 , title :"说明"});
						</script></td>
				<td class="co" style="width:80%"><textarea id="form_property_field_style" class="cui-edit-textarea" style="width:100%;height:50px;margin-top:5px"></textarea></td>
			</tr>
		</table>
	</div>
	
	
	<div id="page_setting_dlg" style="display:none;">
		<div>
		<table class="infoTable" cellpadding="0" cellspacing="0" border="0" width="95%" align="left" style="margin-top: 10px">
			<tr style="display:none;">
				<td class="la" width="20%" align="right" padding-right="10px">${getHtmlText('ec.view.layout')}</td>
				<td class="co" width="30%">${getHtmlText('ec.view.everyline')}<input type="text" id="layout_col_num" class="cui-edit-field" style="width:50px" /> ${getHtmlText('ec.view.colum')}</td>
				<td class="la" width="20%" title="${getText("ec.view.config.layout.description")?html}" padding-right="10px">
					<select id="layout_type" class="cui-edit-field" style="width:100px" disabled="true">
						<option value="classic">${getText("ec.view.config.layout.classic")}</option>
						<option value="mixed">${getText("ec.view.config.layout.mixed")}</option>
					</select>
				</td>
				<td class="co" width="30%"></td>
			</tr>
			<tr>
				<td class="la" width="20%" align="right" padding-right="10px">${getHtmlText('ec.view.pageload')}<br/>(onLoad)
				<span id="onLoadHelpinfo" class="baphelp-icon"></span>
<div id="onLoadHelpinforef" style="display:none">
	<p class="baphelp-info">页面加载的时候执行</p>
	<p class="baphelp-example">范例</p>
<div class="baphelp-code">
<pre><code>alert("示例");
</code></pre>
</div>
<p class="baphelp-hint">注意：在该时刻，Datagrid或Datatable可能还未加载完，不可再对这两个对象进行操作</p>
</div>
					<script type="text/javascript">
							$('#onLoadHelpinfo').helptip({refElm: "#onLoadHelpinforef", html: true , isCustom :false, width: 480 , title :"说明"});
						</script>
				</td>
				<td class="co" colspan="3"><textarea id="onloadevent" class="cui-edit-textarea" style="width:100%;height:100px;"></textarea></td>
			</tr>
			<tr style="display:none;">
				<td class="la" width="20%" align="right" padding-right="10px">${getHtmlText('ec.view.saveevent')}&#160;&#160;&#160;<br/>(onSave)<span id="onSaveHelpinfo" class="baphelp-icon"></span>
<div id="onSaveHelpinforef" style="display:none">
	<p class="baphelp-info">页面保存或提交的时候执行</p>
	<p class="baphelp-example">范例</p>
<div class="baphelp-code">
<pre><code>var type = $("#operateType").val();
if(type =='submit'){//提交
	alert("这是示例");
}else if(type =='save'){//保存
	alert("这是示例");
}
var type1=$("input[name='workFlowVarStatus']").val();
if(type1 =='reject'){//驳回
	alert("这是示例");
}else if(type1 =='cancel'){//作废
	alert("这是示例");
}
</code></pre>
</div>
<p class="baphelp-hint">注意：只有表单才有operateType属性</p>
</div>					
					<script type="text/javascript">
							$('#onSaveHelpinfo').helptip({refElm:"#onSaveHelpinforef", html: true , isCustom :false, width: 330 , title :"说明"});
						</script></td>
				<td class="co" colspan="3"><textarea id="onsaveevent" class="cui-edit-textarea" style="width:100%;height:100px;margin-top:5px;"></textarea></td>
			</tr>
		</table>
		</div>
		<div style="float:left;width:100%;display:none;">
		<fieldset style="border:1px solid #A5C7EF;width:90%;margin-left:18px;">
			<legend>Groovy ${getText('ec.view.groovyscript')}</legend>
			<table class="infoTable" cellpadding="0" cellspacing="0" border="0" width="100%" align="left" style="margin-top: 10px">
			<tr>
				<td class="la" width="20%" align="right" padding-right="10px">BeforeSave</td>
				<td class="co" width="30%">
					<@selector name="beforeSave" id="beforeSave" cssClass="cui-noborder-input" cssStyle="width:100%" value=""
						openType="page" closePage="true" pageType="script" onclick="selectScriptCode('beforeSave')"/>
				</td>
				<td class="la" width="20%" align="right" padding-right="10px">AfterSave</td>
				<td class="co" width="30%">
					<@selector name="afterSave" id="afterSave" cssClass="cui-noborder-input" cssStyle="width:100%" value=""
						openType="page" closePage="true" pageType="script" onclick="selectScriptCode('afterSave')"/>
				</td>
			</tr>
			<#if view.assModel.entity.workflowEnabled?default(false) && view.assModel.isMain?? && view.assModel.isMain>
			<tr>
				<td class="la" width="20%" align="right" padding-right="10px">BeforeSubmit</td>
				<td class="co" width="30%">
					<@selector name="beforeSubmit" id="beforeSubmit" cssClass="cui-noborder-input" cssStyle="width:100%" value=""
						openType="page" closePage="true" pageType="script" onclick="selectScriptCode('beforeSubmit')"/>
				</td>
				<td class="la" width="20%" align="right" padding-right="10px">AfterSubmit</td>
				<td class="co" width="30%">
					<@selector name="afterSubmit" id="afterSubmit" cssClass="cui-noborder-input" cssStyle="width:100%" value=""
						openType="page" closePage="true" pageType="script" onclick="selectScriptCode('afterSubmit')"/>
				</td>
			</tr>
			</#if>
			</table>
		</fieldset>
		</div>
	</div>
	<div id="layout-config" style="display:none;">
		<div id="layout-config-title">
			<span>${getText('ec.view.choicelayout')}</span>
		</div>
		<div id="layout-config-choice">
			<div class="layout-config-horiz" style="float:left;">
				<div class="layout-bt" layoutMethod="row">
					<img src="/bap/static/images/large-columns.png" />
				</div>
				<span>${getText('ec.view.horizontalLayout')}</span>
			</div>
			<div class="layout-config-vert" style="float:left;">
				<div class="layout-bt" layoutMethod="column">
					<img src="/bap/static/images/large-rows.png" />
				</div>
				<span>${getText('ec.view.verticalLayout')}</span>
			</div>			
		</div>
		<div class="layout-configuration-title">
			<span>${getText('ec.view.layoutOptions')}</span>
			<select class="row num-columns">
				<option value="1">1</option>
				<option value="2" selected="selected">2</option>
				<option value="3">3</option>
				<option value="4">4</option>
				<option value="5">5</option>
				<option value="6">6</option>
				<option value="7">7</option>
			</select>
			<label class="layout-dir">${getText('ec.view.layoutColumn')}</label>
			<label class="layout-config-border"><input type="checkbox" checked />${getText('ec.view.layoutNeedBorder')}</label>

		</div>			
		<div class="layout-options-row" nums="2">
			<div class="fixedblock-layout-bar">
				<img src="/bap/static/images/left-large.png"/>
				<div class="layout-fixedblock-select">
					<input name="left-fixedblock-select" id="left-fixedblock-select" class="lr-fixedblock-select" type="checkbox" />
					<label for="left-fixedblock-select">${getText('ec.view.layoutLeftBlock')}</label>
				</div>
			</div>
			<div class="layout-column-selector">					
				<table class="table-showing-columns" height="100%" width="100%">
					<tr>
						<td align="center"><img src="/bap/static/images/row-large.png" /></td>
						<td align="center"><img src="/bap/static/images/row-large.png" /></td>
					</tr>
				</table>
			</div>
			<div class="fixedblock-layout-bar">
				<img src="/bap/static/images/right-large.png" />
				<div class="layout-fixedblock-select">
					<input name="right-fixedblock-select" class="lr-fixedblock-select" id="right-fixedblock-select" type="checkbox" />
					<label for="right-fixedblock-select">${getText('ec.view.layoutRightBlock')}</label>
				</div>
			</div>
		</div>
		<div class="layout-options-column" style="display: none;"  nums="2">
			<div class="optional-layout-bar">
				<img src="/bap/static/images/header-large.png"/>
				<input name="top-fixedblock-select" id="top-fixedblock-select" class="tb-fixedblock-select" type="checkbox" />
				<label for="top-fixedblock-select">${getText('ec.view.layoutTopBlock')}</label>					
			</div>
			<div class="layout-row-selector">
				<table class="table-showing-columns" height="100%" width="100%">
					<tr><td><img src="/bap/static/images/column-large.png" /></td></tr>
					<tr><td><img src="/bap/static/images/column-large.png" /></td></tr>
				</table>
			</div>
			<div class="optional-layout-bar">
				<img src="/bap/static/images/bottom-large.png"/>
				<input name="bottom-fixedblock-select" id="bottom-fixedblock-select" class="tb-fixedblock-select" type="checkbox" />
				<label for="bottom-fixedblock-select">${getText('ec.view.layoutBottomBlock')}</label>
			</div>
		</div>		
	</div>
	<div id="tabs-config" style="display:none;">
		<label for="tabs-num">${getText('ec.view.choiceTabNums')}</label>
		<select id="tabs-num" name="tabs-num">
			<option value="1">1</option>
			<option value="2">2</option>
			<option value="3">3</option>
			<option value="4">4</option>
			<option value="5">5</option>
			<option value="6">6</option>
			<option value="7">7</option>
			<option value="8">8</option>
		</select>
		<table class="tabs-Name">
			<tr>
				<td>${getText('ec.view.tab')}1</td>
				<td><@international name="tab.namenew1" isNew=true isOldEdit=true moduleCode="${view.assModel.entity.module.artifact}" key="" style="width:100%;margin-right: -5px;" cssClass="cui-edit-field" /></td>
			</tr>
			<tr>
				<td>${getText('ec.view.tab')}2</td>
				<td><@international name="tab.namenew2" isNew=true isOldEdit=true moduleCode="${view.assModel.entity.module.artifact}" key="" style="width:100%;margin-right: -5px;" cssClass="cui-edit-field" /></td>
			</tr>
			<tr>
				<td>${getText('ec.view.tab')}3</td>
				<td><@international name="tab.namenew3" isNew=true isOldEdit=true moduleCode="${view.assModel.entity.module.artifact}" key="" style="width:100%;margin-right: -5px;" cssClass="cui-edit-field" /></td>
			</tr>
			<tr>
				<td>${getText('ec.view.tab')}4</td>
				<td><@international name="tab.namenew4" isNew=true isOldEdit=true moduleCode="${view.assModel.entity.module.artifact}" key="" style="width:100%;margin-right: -5px;" cssClass="cui-edit-field" /></td>
			</tr>
			<tr>
				<td>${getText('ec.view.tab')}5</td>
				<td><@international name="tab.namenew5" isNew=true isOldEdit=true moduleCode="${view.assModel.entity.module.artifact}" key="" style="width:100%;margin-right: -5px;" cssClass="cui-edit-field" /></td>
			</tr>
			<tr>
				<td>${getText('ec.view.tab')}6</td>
				<td><@international name="tab.namenew6" isNew=true isOldEdit=true moduleCode="${view.assModel.entity.module.artifact}" key="" style="width:100%;margin-right: -5px;" cssClass="cui-edit-field" /></td>
			</tr>
			<tr>
				<td>${getText('ec.view.tab')}7</td>
				<td><@international name="tab.namenew7" isNew=true isOldEdit=true moduleCode="${view.assModel.entity.module.artifact}" key="" style="width:100%;margin-right: -5px;" cssClass="cui-edit-field" /></td>
			</tr>
			<tr>
				<td>${getText('ec.view.tab')}8</td>
				<td><@international name="tab.namenew8" isNew=true isOldEdit=true moduleCode="${view.assModel.entity.module.artifact}" key="" style="width:100%;margin-right: -5px;" cssClass="cui-edit-field" /></td>
			</tr>
		</table>
	</div>
	<div id="search-config" style="display:none;">
		<div class="searchBox">
			<div style="width:100%;height:100%;">
				<div class="search-config-wrapper">
					<button id="dcbutton" class="search-config-button" onclick="ec.dataClassific(this)"  title="${getText('ec.view.list.dcsetclick')}">${getHtmlText('ec.view.list.dcset')}</button>
				</div>
				<div class="search-config-wrapper">
					<button id="fqsbutton" class="search-config-button" onclick="ec.fastQuerySetting(this,'${view.assModel.code}')" title="${getText('ec.view.list.fastsetclick')}">${getHtmlText('ec.view.list.fastset')}</button>
				</div>
				<div class="search-config-wrapper">
					<button id="advbutton" class="search-config-button" onclick="ec.advQuerySetting(this,'${view.assModel.code}')" title="${getText('ec.view.list.advsetclick')}">${getHtmlText('ec.view.list.advset')}</button>
				</div>
				<div class="search-config-wrapper">
					<button class="search-config-button" onclick="ec.dataAssocSetting(this,'${view.assModel.code}')" title="数据关联配置">数据关联配置</button>
				</div>
			</div>
		</div>
	</div>
	<div id="layout-setting-dlg" style="display:none">
		<script type="text/javascript">
			$(function(){
				$("#layout-setting-dlg>ul.edit-tabs").tabs("#layout-setting-dlg>div.edit-panes > div");
			});
		</script>
		<ul class="edit-tabs" style="width: 465px;">
			<li>${getHtmlText('ec.view.layoutProp')}</li>
			<li>${getHtmlText('ec.view.layoutStyle')}</li>
		</ul>
 		<@errorbar id="LayoutSettingErrorBar"></@errorbar>
		<div class="edit-panes edit-panes-w edit-container clearfix" style="width: 465px; height: 350px;">
			<div class="clearfix pd_bottom" style="width:100%;height:100%;overflow:auto;">
				<table class="layout-setting">
					<tr><td class="label">${getHtmlText('ec.view.layoutName')}</td><td class="val lay-name">layoutname</td></tr>
					<tr class="tab-name">
						<td class="label" style="color: rgb(179, 3, 3);">${getHtmlText('ec.view.tabNames')}</td>
						<td class="val">
						<@international name="tab.name.set" isNew=true isOldEdit=true moduleCode="${view.assModel.entity.module.artifact}" key="" style="width:300px;" cssClass="cui-edit-field" />
						</td>
					</tr>
					<tr><td class="label">${getHtmlText('ec.view.layoutMethod')}</td><td class="val lay-dir">${getHtmlText('ec.view.horizontalLayout')}</td></tr>
					<tr style="display:none;" id="assocModel"><td class="label">${getHtmlText('ec.view.assocModel')}</td><td class="val assoc-model"></td></tr>
					<tr class="tr-size"><td class="label lay-width">${getHtmlText('ec.view.layoutWidth')}</td><td class="val lay-width"><input type="text" value="80"/>px</td>
						<td class="label lay-height">${getHtmlText('ec.view.layoutHeight')}</td><td class="val lay-height"><input type="text" value="80"/>px</td>
					</tr>
					<tr class="tr-ratio"><td class="label ratio-txt">${getHtmlText('ec.view.layoutHorizontalRatio')}</td><td class="val" style="width:35%"><input class="lay-ratio" type="text" value="100" />%</td></tr>
					<tr class="tr-border"><td class="label"><label for="panel_property_is_border" >${getHtmlText('ec.view.layoutNeedBorders')}</label></td><td class="val"><input id="panel_property_is_border" class="isBorder" type="checkbox" checked /></td></tr>
					<tr class="tr-readonly"><td class="label"><label for="panel_property_is_readonly" >${getHtmlText('ec.view.isreadonly')}</label></td><td class="val"><input id="panel_property_is_readonly" type="checkbox" /></td></tr>
					<tr class="tr-ptRealTimeLoad"><td class="label"><label for="panel_property_is_ptRealTimeLoad" >${getHtmlText('ec.view.ptRealTimeLoad')}：</label></td><td class="val"><input id="panel_property_is_ptRealTimeLoad" type="checkbox" /></td></tr>
					<tr class="tr-url"><td>${getHtmlText('ec.view.LayoutCustomizationURL')}</td><td><input type="text"/></td></tr>
					<tr class="tr-showmethod"><td>${getHtmlText('ec.view.URLShowMethod')}</td><td><input name="showmethod" type="radio" checked="true" value="iframe"/>iframe <input name="showmethod" type="radio" value="div"/>div</td></tr>
				</table>
				<div class="subLayout-setting" style="display:none;">
					<h2>${getHtmlText('ec.view.subLayoutSetting')}</h2>
					<p>
						${getHtmlText('ec.view.layoutLeftBlock')}<label class="lt-fix"><input type="text" value="100" />px</label>
						${getHtmlText('ec.view.layoutRightBlock')}<label class="rb-fix"><input type="text" value="100" />px</label>
					</p>
					<ul>
						<li>${getHtmlText('ec.view.variableContainer')}<input type="text" value="20">%</li>
						<li>${getHtmlText('ec.view.variableContainer')}<input type="text" value="20">%</li>
						<li>${getHtmlText('ec.view.variableContainer')}<input type="text" value="20">%</li>
						<li>${getHtmlText('ec.view.variableContainer')}<input type="text" value="20">%</li>
						<li>${getHtmlText('ec.view.variableContainer')}<input type="text" value="20">%</li>
					</ul>
				</div>
			</div>
			<div class="clearfix pd_bottom layout-style" style="width:100%;height:100%;overflow:auto;">
				<h3>${getHtmlText('ec.view.layoutStyles')}</h3>
				<textarea id="layoutStyle"></textarea>
			</div>
		</div>	
	</div>
	<div id="listPropertyDlg" style="display:none;">
		<@errorbar id="ListPropertyDlgErrorBar"></@errorbar>
		<div id="dlg_div_public" class="dlg-etv-navset">
	 		<div class="etv-scrollbar" style="margin:0 5px;">
				<ul class="etv-nav" style="display: block;">
					<li class="selected" name="proertyTabs" id="base_attr_public">
						<span class="etv-nav-span">
							<em class="etv-nav-em" style="width: 90px;">${getText('ec.view.cell.property')}</em>
						</span>
						<span class="etv-nav-span-r"></span>
					</li>
					<li name="proertyTabs" id="event_defined_public">
						<span class="etv-nav-span">
							<em class="etv-nav-em" style="width: 90px;">${getText('ec.view.cell.event')}</em>
						</span>
						<span class="etv-nav-span-r"></span>
					</li>
				</ul>
			</div>
		</div>
		<table id="tab_common" class="infoTable" cellpadding="0" cellspacing="0" border="0" width="80%" align="left" style="margin-top: 10px">
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
<#noparse>
<div class="baphelp-code">
<pre><code><span>if(parameters.test[0]=="1"){</span>
	return "(\"p\".ID =\${deptId,Long} OR \"p\".LAY_REC LIKE \${layrec,String})";
}else{
	return "(\"p\".ID =\${deptId,String} OR \"p\".LAY_REC LIKE \${layrec,String})";
}</code></pre>
</div>
<p class="baphelp-hint">注意：test 为前台传的参数，post 或get 均可，数组可以用array或者list类型，参数默认用逗号分隔，或者额外传split参数作为分隔符，例如 pid in (${pids,array})</p>
</#noparse>
</div>

					<script type="text/javascript">
							$('#handwritingHelpinfo').helptip({refElm: "#handwritingHelpinforef", html: true , isCustom :false, width: 460 , title :"说明"});
				</script>
				</td>
				<td class="co" colspan="2">
					<div id="66387035_button_2" canClick="true" class="edit-btn btn-act" onclick="showTableDialog()"><a class="cui-btn-l">&nbsp;</a><a class="cui-btn-c">${getHtmlText('ec.view.customerCondition')}</a><a class="cui-btn-r">&nbsp;</a></div>
				</td>
			</tr>
			<tr>
				<td class="co" colspan="4" style="padding-left:30px">
					<textarea readonly="true" id="conditionArea" class="cui-edit-textarea" style="width:90%;height:180px;"></textarea>
				</td>
			</tr>
		</table>
		<table id="tab_event" class="infoTable" cellpadding="0" cellspacing="0" border="0" width="80%" align="left" style="margin-top: 10px;display:none;">
			<tr style="height:0px">
				<td style="width:20%;height:0px;text-align:right" class="la"></td>
				<td class="co" style="width:30%;height:0px"></td>
				<td style="width:20%;height:0px;text-align:right" class="la"></td>
				<td class="co" style="width:30%;height:0px"></td>
			</tr>
			<tr>
				<td class="la" style="width:25%" align="right" padding-right="10px">renderOver:
				<span id="renderOverHelpinfo" class="baphelp-icon"></span>
<div id="renderOverHelpinforef" style="display:none">
	<p class="baphelp-info">列表数据请求执行接口</p>
	<p class="baphelp-example">范例</p>
<div class="baphelp-code">
<pre><code>alert("示例");
</code></pre>
</div>
<p class="baphelp-hint">注意：每次请求数据的时候都会调用该接口</p>
</div>

					<script type="text/javascript">
							$('#renderOverHelpinfo').helptip({refElm: "#renderOverHelpinforef", html: true , isCustom :false, width: 280 , title :"说明"});
				</script>
				</td>
				<td class="co" style="width:80%"><textarea id="dataGrid_public_event" class="cui-edit-textarea" style="width:100%;height:60px;margin-top:5px"></textarea></td>
			</tr>
			<tr>
				<td class="la" style="width:20%" align="right" padding-right="10px">ptInit（${getText('ec.view.list')}&#160;&#160;<br/>${getText('ec.view.init')}）:
				<span id="ptInitHelpinfo" class="baphelp-icon"></span>
<div id="ptInitHelpinforef" style="display:none">
	<p class="baphelp-info">列表初始化函数</p>
	<p class="baphelp-example">范例</p>
<div class="baphelp-code">
<pre><code>alert("示例");
</code></pre>
</div>
<p class="baphelp-hint">注意：只在第一次初始化时执行</p>
</div>

					<script type="text/javascript">
							$('#ptInitHelpinfo').helptip({refElm: "#ptInitHelpinforef", html: true , isCustom :false, width: 220 , title :"说明"});
				</script>
				</td>
				<td class="co" style="width:80%"><textarea id="dataGrid_init_event" class="cui-edit-textarea" style="width:100%;height:60px;margin-top:5px"></textarea></td>
			</tr>
		</table>
	</div>
	<div id="section_setting_dlg" style="display:none;height:280;overflow-y:auto;">
		<table class="infoTable" cellpadding="0" cellspacing="0" border="0" width="95%" align="left" style="margin-top: 10px">
			<tr id="tr_is_custom_section">
				<td class="la" width="15%" style="text-align:right;padding-right:10px;">自定义字段节</td>
				<td class="co" width="85%"><input type="checkbox" id="is_custom_section" /></td>
			</tr>
			<tr>
				<td class="la" width="15%" align="right" padding-right="10px">${getHtmlText('ec.view.layout')}</td>
				<td class="co" width="85%">${getHtmlText('ec.view.everyline')} <input type="text" id="section_num" class="cui-edit-field" style="width:50px"/> ${getHtmlText('ec.view.colum')}</td>
			</tr>
			<tr style="display:none;">
				<td class="la" align="right" padding-right="10px">${getHtmlText('ec.view.sectionname')}</td>
				<td class="co"><input type="text" id="section_name" class="cui-edit-field" style="width:90px" /></td>
			</tr>
			<tr style="display:none;">
				<td class="la" align="right" padding-right="10px">${getHtmlText('ec.view.isborder')}</td>
				<td class="co"><input type="checkbox" id="section_border"/></td>
				
			</tr>
			<tr>
				<td class="la" align="right" padding-right="10px">${getHtmlText('ec.view.colwidth')}</td>
				<td class="co">
					<div id="editcolwidthdiv">
						1、<input type="text" id="colwidth1" class="cui-edit-field" style="width:20px" value="13"/>
						2、<input type="text" id="colwidth2" class="cui-edit-field" style="width:20px" value="20"/>
						3、<input type="text" id="colwidth3" class="cui-edit-field" style="width:20px" value="13"/>
						4、<input type="text" id="colwidth4" class="cui-edit-field" style="width:20px" value="20"/>
						5、<input type="text" id="colwidth5" class="cui-edit-field" style="width:20px" value="13"/>
						6、<input type="text" id="colwidth6" class="cui-edit-field" style="width:20px" value="20"/>
					</div>
				</td>
			</tr>
			<tr style="display:none;">
				<td class="la" align="right" padding-right="10px">${getHtmlText('ec.view.customstyle')}
				<span id="customStyleHelpinfo1" class="baphelp-icon"></span>&#160;
<div id="customStyleHelpinforef1" style="display:none">
	<p class="baphelp-info">自定义样式</p>
	<p class="baphelp-example">范例</p>
<div class="baphelp-code">
<pre><code><span>background-color:red;</span>
</code></pre>
</div>
<p class="baphelp-hint">注意：直接写css样式。</p>
</div>
					<script type="text/javascript">
							$('#customStyleHelpinfo1').helptip({refElm: "#customStyleHelpinforef1", html: true , isCustom :false, width: 180 , title :"说明"});
						</script></td>
				<td class="co"><textarea id="form_section_style" class="cui-edit-textarea" style="width:100%;height:80px;margin-top:5px"></textarea></td>
			</tr>
		</table>
	</div>
	<div id="tab_setting_dlg" style="display:none;">
 		<@errorbar id="TabDialogErrorBar"></@errorbar>
		<table class="infoTable" cellpadding="0" cellspacing="0" border="0" width="95%" align="left" style="margin-top: 10px">
			<tr>
				<td class="la" width="15%" align="right" padding-right="10px" style="color: rgb(179, 3, 3);">${getHtmlText('ec.view.tabname')}</td>
				<td class="co">
				<@international name="tab.name" isNew=true isOldEdit=true moduleCode="${view.assModel.entity.module.artifact}" key="" style="width:100%;margin-right: -5px;" cssClass="cui-edit-field" />
			</tr>
		</table>
	</div>
	<div id="datagrid_setting_dlg" style="display:none;">
		<table class="infoTable" cellpadding="0" cellspacing="0" border="0" width="95%" align="left" style="margin-top: 10px">
			<tr>
				<td class="la" width="40%" align="right" padding-right="5px">${getHtmlText('ec.view.choicemodel')}</td>
				<td class="co" width="60%">
					<select id="datagrid_model_type" class="cui-edit-field" style="width:100%">
					<#if assos?has_content>
						<#list assos as asso>
						<option value="${asso.originalProperty.code}">${getText('${asso.originalProperty.model.name}')}[${getText('${asso.originalProperty.displayName}')}]</option>
						</#list>
					</#if>
					</select>
				</td>
			</tr>
		</table>
	</div>
	<div id="datatable_setting_dlg" style="display:none;">
		<table class="infoTable" cellpadding="0" cellspacing="0" border="0" width="95%" align="left" style="margin-top: 10px">
			<tr>
				<td class="la" width="40%" align="right" padding-right="5px">${getHtmlText('ec.view.choicemodel')}</td>
				<td class="co" width="60%">
					<select id="datatable_model_type" class="cui-edit-field" style="width:100%">
					<#if models?has_content>
						<#list models as model>
						<option value="${model.code}">${getText('${model.name}')}</option>
						</#list>
					</#if>
					</select>
				</td>
			</tr>
		</table>
	</div>	
	<div id="query_setting_dlg" style="display:none;">
		<table class="infoTable" cellpadding="0" cellspacing="0" border="0" width="95%" align="left" style="margin-top: 10px">
			<tr>
				<td class="la" width="40%" align="right" padding-right="5px">${getHtmlText('ec.view.choicemodel')}</td>
				<td class="co" width="60%">
					<select id="query_model_type" class="cui-edit-field" style="width:100%">
					<#if models?has_content>
						<#list models as model>
						<option value="${model.code}" modelname="${model.name}">${getText('${model.name}')}</option>
						</#list>
					</#if>
					</select>
				</td>
			</tr>
		</table>
	</div>
	<div id="groupSelectPeople_dlg" style="display:none;OVERFLOW-Y:auto;">
		<input type="hidden" id="selectPeopleDel" />
		<table id="selectPeopleTable" class="infoTable" cellpadding="0" cellspacing="0" border="0" width="100%" align="left" style="margin-top: 10px;margin-left: 10px;">
			<tr>
				<td class="la" width="20%">
					<input name="groupname" type="text" size="4" value="分组名称"/>
				</td>
				<td class="co" width="30%">
					<div id="selectSetting" canclick="true" class="edit-btn btn-act" onclick="selectPeopleReference(this)">
						<a class="cui-btn-l">&nbsp;</a><a class="cui-btn-c"><span i18n="选择">选择</span></a>
						<a class="cui-btn-r">&nbsp;</a>
					</div>
					<input name="staffName" type="text" size="5" readonly="true" />
					<input name="selectStaffId" type="hidden"/>
					<input name="uid" type="hidden"/>
				</td>
				<td class="la" width="20%">
					<input name="sort" type="text" size="1" value="1"/>
				</td>
				<td class="co" width="30%">
					<img onclick="addSelectRow(this)" src="/bap/static/foundation/images/icon-add.gif" />
					<img onclick="delSelectRow(this)" src="/bap/static/foundation/images/icon-del.gif" />
				</td>
			</tr>
			<#-- 
			<tr>
				<td colspan="4">
					<div id="div-validate" style="width:570px;align:left;height:390px;overflow-y:auto">
						<table id="tab-validate1" cellpadding="0" cellspacing="0" border="0" width="100%" style="margin-top: 10px;">
							<tr style="height:0px">
								<td class="la" style="width:15%;height:0px;text-align:right"></td>
								<td style="width:35%;height:0px" class="co"></td>
								<td class="la" style="width:45%;height:0px;text-align:right"></td>
								<td style="width:5%;height:0px" class="co"></td>
							</tr>
						</table>
					</div>
				</td>
			</tr>
			-->
		</table>
	</div>
	<div id="multselect_setting_dlg" style="display:none;">
		<@errorbar id="mulEditDialogErrorBar"></@errorbar>
	 		<div id="muldlg-div" class="dlg-etv-navset">
				<div class="etv-scrollbar" style="margin:0 5px;">
					<ul class="etv-nav" style="display: block;">
						<li class="selected" name="proertyTabs" id="mulbase_attr">
							<span class="etv-nav-span">
								<em class="etv-nav-em" style="width: 90px;">${getText('ec.view.cell.property')}</em>
							</span>
							<span class="etv-nav-span-r"></span>
						</li>
						<li name="proertyTabs" id="mulevent_defined">
							<span class="etv-nav-span">
								<em class="etv-nav-em" style="width: 90px;">${getText('ec.view.cell.event')}</em>
							</span>
							<span class="etv-nav-span-r"></span>
						</li>
					</ul>
				</div>
			</div>
	 		<table id="multab-common" class="infoTable" cellpadding="0" cellspacing="0" border="0" width="95%" align="left" style="margin-top: 10px">
			<tr>
				<td class="la" width="30%" align="right" padding-right="5px">${getHtmlText('ec.view.assmodel')}</td>
				<td class="co" width="60%">
					<select id="multselect_model_type" class="cui-edit-field" style="width:100%">
					<#if (multSelectInfo['directAssos'])??>
						<#list multSelectInfo['directAssos'] as asso>
						<option value="-${asso.originalProperty.code}-${asso.targetProperty.code}-">
							${getText('${asso.targetProperty.model.name}')}
						</option>
						</#list>
					</#if>
					</select>
				</td>
			</tr>
			<tr id="associated_1">
				<td class="la" width="30%" align="right">${getHtmlText('ec.view.attribute')}</td>
				<td class="co" width="60%">
				<select id="ec_select_indirect_model" style="width:100%;"></select>
				</td>
			</tr>
			<tr id="staffSelectType">
				<td class="la" width="30%" align="right" style="vertical-align:middle;">选择类型</td>
				<td class="co" width="60%">
					<label for="company" class="">本公司</label><input class="" id="company" name="staffSelectTypeRadio" style="vertical-align:middle;" type="radio" value="company" onclick="staffSelectTypeRadio(this);" checked="true"/>&nbsp;&nbsp;
					<label for="crossCompany" class="">跨公司</label><input class="" id="crossCompany" name="staffSelectTypeRadio" style="vertical-align:middle;" type="radio" value="crossCompany" onclick="staffSelectTypeRadio(this);"/>&nbsp;&nbsp;
					<label for="department" class="">本部门</label><input class="" id="department" name="staffSelectTypeRadio" style="vertical-align:middle;" type="radio" value="department" onclick="staffSelectTypeRadio(this);"/>&nbsp;&nbsp;
					<label for="departmentAndSub" class="">本部门及下级</label><input class="" id="departmentAndSub" name="staffSelectTypeRadio" style="vertical-align:middle;" type="radio" value="departmentAndSub" onclick="staffSelectTypeRadio(this);"/>&nbsp;&nbsp;
					<label for="customer" class="">自定义</label><input class="" id="customer" name="staffSelectTypeRadio" style="vertical-align:middle;" type="radio" value="customer" onclick="staffSelectTypeRadio(this);"/>&nbsp;&nbsp;
				</td>
			</tr>
			<tr id="staffLimit" style="display:none;">
				<td class="la" width="30%" align="right">${getHtmlText('选择范围')}</td>
				<td class="co" width="60%">
					<div id="selectSetting" canclick="true" class="edit-btn btn-act" onclick="showSelectPeopleSettingDialog()">
						<a class="cui-btn-l">&nbsp;</a><a class="cui-btn-c"><span i18n="配置范围">配置范围</span></a>
						<a class="cui-btn-r">&nbsp;</a>
					</div>
					<#-- <@mneclient reftitle="${getText('foundation.ec.entity.view.reference')}"   name="staffLimit" id="staffLimit" type="Staff" url="/msService/ec/foundation/staff/common/staffListFrameset" displayFieldName="name"  ids="${(weituo.weituoGuanlianrenmultiselectIDs)!}" names="${(weituo.weituoGuanlianrenmultiselectNames)!}" onkeyupfuncname="getstaffLimitMultiInfo()"   clicked=true multiple=true cssStyle="padding-left: 0px; width: 100%;" mnewidth=260 isCrossCompany=false isEdit=true /> -->
				</td>
			</tr>
			<tr id="associated_2">
				<td class="la" width="30%" align="right">${getHtmlText('ec.view.choiceRef')}</td>
				<td class="co" width="60%">
				<select id="ec_reference_view" style="width:100%"></select>
				</td>
			</tr>
			<tr id="associated_2">
				<td class="la" width="30%" align="right">${getHtmlText('ec.view.showfield')}</td>
				<td class="co" width="60%">
				<div name="property" class="fix-search-click" style="width:95%">
				<input id="ec_select_property" type="hidden">
				<input size="15" id="ec_select_property_displayName" class="cui-edit-field">
				<input type="button" onclick="ec_view_select_property.selectProperty(event, this)"  value="" class="cui-search-click">
			</div>
				</td>
			</tr>
			<tr id="ec_tr_mneenable" style="display:none;">
				<td class="la" width="30%" align="right"><label for="ec_checkbox_mneenable">${getHtmlText('ec.view.mneenable')}</label></td>
				<td class="co" width="60%">
				<input type="checkbox" id="ec_checkbox_mneenable" value="true">
				</td>
			</tr>
			<tr id="ec_tr_nullable">
				<td class="la" width="30%" align="right"><label for="ec_checkbox_nullable">${getHtmlText('ec.view.isnullable')}</label></td>
				<td class="co" width="60%">
				<input type="checkbox" id="ec_checkbox_nullable" value="true">
				</td>
			</tr>
			<#-- 
			<tr>
				<td class="la" width="30%" align="right">${getHtmlText('ec.view.isgroup')}</td>
				<td class="co" width="60%">
					<input type="checkbox" id="form_property_is_group_multi" />
				</td>
			</tr>
			-->
			<tr>
				<td class="la" width="30%" align="right"><label for="ec_is_readonly">${getHtmlText('ec.view.isreadonly')}</label></td>
				<td class="co" width="60%">
					<input type="checkbox" id="ec_is_readonly" />
				</td>
			</tr>
			<tr >
				<td class="la" width="30%" align="right"><label for="multAllowView">${getHtmlText('ec.view.allowView')}</label></td>
				<td class="co" width="60%">
					<input type="checkbox" id="multAllowView" onclick="changeMultAllowView(this);"/>
				</td>
			</tr>
			<tr id="ref_condition" style="height: 30px; display: table-row;">
				<td class="la" width="20%" align="right" padding-right="7px"><label id="refConditionLabel"><span i18n="ec.view.refCondition">参照参数</span></label></td>
				<td class="co" colspan="3">
					<textarea id="ref_condition_content" class="cui-edit-textarea" style="width:100%;height:50px;margin-top:5px"></textarea>
				</td>
			</tr>
			<tr id="multAllowViewTR" style="display:none;">
				<td class="la" width="30%" align="right">${getHtmlText('ec.business.view.choose')}</td>
				<td class="co" width="60%">
					<select id="allowMultViewSelect"></select>
				</td>
			</tr>
		</table>
		<div id="muldiv-event" style="width:100%;align:left;height:340px;overflow-y:auto;display:none">
			<table id="multab-event" class="infoTable" cellpadding="0" cellspacing="0" border="0" width="95%" align="left" style="margin-top: 10px">
				<tr>
					<td class="la" width="20%" align="right" padding-right="10px"><label>${getHtmlText('ec.view.cell.eventType')}</label></td>
					<td class="co" width="30%">
						<select id="mulevents-select" class="cui-edit-field" style="width:100%">
							<option value="onBeforeClear">onBeforeClear</option>
							<option value="onAfterClear">onAfterClear</option>
							<option value="onBeforeSet">onBeforeSet</option>
							<option value="onAfterSet">onAfterSet</option>
						</select>
					</td>
					<td class="la" width="20%" style="text-align:left;padding-left:10px">
						<img onclick="ec.addnewevent($('#mulevents-select'),$('#multab-event1'))" title="${getText('ec.view.addevent')}" src="/bap/static/foundation/images/icon-add.gif" /><br/>
					</td>
					<td class="la" width="30%"></td>
				</tr>
				<tr>
					<td colspan="4">
						<table id="multab-event1" class="infoTable" cellpadding="0" cellspacing="0" border="0" width="100%" align="left" style="margin-top: 10px;">
							<tr style="height:0px">
								<td class="la" style="width:15%;height:0px;text-align:right"></td>
								<td style="width:35%;height:0px" class="co"></td>
								<td class="la" style="width:45%;height:0px;text-align:right"></td>
								<td style="width:5%;height:0px" class="co"></td>
							</tr>
						</table>
					</td>
				</tr>
			</table>
		</div>
	</div>
	<div id="supervision_setting_dlg" style="display:none;">
		<table class="infoTable" cellpadding="0" cellspacing="0" border="0" width="95%" align="left" style="margin-top: 10px">
			<tr id="associated_2">
				<td class="la" width="30%" align="right">${getHtmlText('ec.view.choiceRef')}</td>
				<td class="co" width="60%">
				<select id="supervision_ec_reference_view" style="width:100%"></select>
				</td>
			</tr>
			<tr id="supervision_ec_tr_mneenable">
				<td class="la" width="30%" align="right">${getHtmlText('ec.view.mneenable')}</td>
				<td class="co" width="60%">
				<input type="checkbox" id="supervision_ec_checkbox_mneenable" value="true" checked>
				</td>
			</tr>
			<#--
			<tr id="ec_tr_nullable">
				<td class="la" width="30%" align="right">${getHtmlText('ec.view.isnullable')}</td>
				<td class="co" width="60%">
				<input type="checkbox" id="ec_checkbox_nullable" value="true">
				</td>
			</tr>
			-->
			<tr>
				<td class="la" width="30%" align="right">${getHtmlText('ec.view.isgroup')}</td>
				<td class="co" width="60%">
					<input type="checkbox" id="supervision_form_property_is_group_multi" />
				</td>
			</tr>
			<tr>
				<td class="la" width="30%" align="right">${getHtmlText('ec.view.isreadonly')}</td>
				<td class="co" width="60%">
					<input type="checkbox" id="supervision_ec_is_readonly" />
				</td>
			</tr>
		</table>
	</div>
	<div id="datagrid_property_dlg" style="display:none;">
		<iframe id="datagird_property_frame" name="datagird_property_frame" width="100%" height="450px" style="overflow:hidden;" SCROLLING="no" MARGINHEIGHT="0" MARGINWIDTH="0" frameborder="0" border="0" framespacing="0"></iframe>
	</div>
	<table id="table_validate_example_row" style="display:none;">
		<tr id="tr_example">
			<td style="width:100%;text-align:right;" class="la">
				<table style="border: 1px solid #DDD;padding: 0 10px;margin: 10px 0 0 10px;width: 100%;">
					<tr>
						<td style="width:20%;text-align:right" class="la">
							<b class="displayLabel"</b>
						</td>
						<td class="co" style="text-align: left;width:30%;">
							<img class="deleteImg" style="cursor: pointer;" src="/bap/static/foundation/images/icon-del.gif">
						</td>
						<td style="width:15%;text-align:right" class="la"></td>
						<td class="co" style="width:35%;"></td>
					</tr>
					<tr class="rangeExampleRow">
						<td style="text-align:right" class="la">
						</td>
						<td class="co">
							<div class="fix-input"><div class="fix-search-click clearfix">
								<input class="cui-noborder-input" name="min" type="text">
							</div></div>
						</td>
						<td style="width:15%;text-align:right" class="la">
							
						</td>
						<td class="co" style="width:35%;">
							<div class="fix-input"><div class="fix-search-click clearfix">
								<input class="cui-noborder-input" name="max" type="text">
							</div></div>
						</td>
					</tr>

					<tr class="stringlengthExampleRow">
						<td style="text-align:right" class="la">
						</td>
						<td class="co">
							<div class="fix-input"><div class="fix-search-click clearfix">
								<input class="cui-noborder-input" name="minLength" type="text">
							</div></div>
						</td>
						<td style="width:15%;text-align:right" class="la">
							
						</td>
						<td class="co" style="width:35%;">
							<div class="fix-input"><div class="fix-search-click clearfix">
								<input class="cui-noborder-input" name="maxLength" type="text">
							</div></div>
						</td>
					</tr>

					<tr class="regexExampleRow">
						<td style="text-align:right" class="la">
						</td>
						<td class="co" colspan="3">
							<div class="fix-input">
								<input class="cui-noborder-input" name="regex" type="text">
							</div>
						</td>
					</tr>
					<tr class="regexExampleRow">
						<td style="width:20%;text-align:right" class="la">
						</td>
						<td class="co" colspan="3">
							<div class="fix-input">
								<#-- international_errorMsg    international_errorMsg_showName -->
								<@international name="errorMsg" isNew=true isOldEdit=true moduleCode="${view.assModel.entity.module.artifact}" key="" style="width:100%;margin-right: -5px;" cssClass="cui-noborder-input" />
							</div>
						</td>
					</tr>

					<tr class="compareExampleRow">
						<td style="text-align:right" class="la">
						</td>
						<td class="co">
							<select class="noborder" name="operator"  style="width:100%">
								<option value="==">${getText('ec.view.config.validate.equal')}</option>
								<option value="!=">${getText('ec.view.config.validate.notequal')}</option>
								<option value=">">${getText('ec.view.config.validate.great')}</option>
								<option value=">=">${getText('ec.view.config.validate.greatequal')}</option>
								<option value="<">${getText('ec.view.config.validate.little')}</option>
								<option value="<=">${getText('ec.view.config.validate.littlequal')}</option>
							</select>
						</td>
						<td style="width:15%;text-align:right" class="la">
							
						</td>
						<td class="co" style="width:35%;">
							<select class="noborder" name="another" style="width:100%">
							</select>
						</td>
					</tr>

					<tr class="customExampleRow">
						<td style="width:20%;text-align:right" class="la">
							
						</td>
						<td class="co" colspan="3">
							<div class="fix-input fix-ie7-textarea">
								<textarea style="height: 85px;width: 99.8%;" name="jsCode" class="cui-noborder-textarea" value=""></textarea>
							</div>
						</td>
					</tr>

					<tr class="customExampleRow">
						<td style="width:20%;text-align:right" class="la">
							
						</td>
						<td class="co" colspan="3">
							<div class="fix-input">
								<#-- international_errorMsg    international_errorMsg_showName -->
								<@international name="errorMsg" isNew=true isOldEdit=true moduleCode="${view.assModel.entity.module.artifact}" key="" style="width:100%;margin-right: -5px;" cssClass="cui-noborder-input" />
							</div>
						</td>
					</tr>
				</table>
			</td>
		</tr>
	</table>
	<div id="button_property_dlg" style="display:none">
		<@errorbar id="ButtonDialogErrorBar"></@errorbar>
		<table class="infoTable" cellpadding="0" cellspacing="0" border="0" width="95%" align="left" style="margin-top: 10px">
			<tr>
				<td class="la" width="20%" align="right" padding-right="10px">${getHtmlText('ec.view.identiy')}</td>
				<td class="co" width="30%"><input type="text" id="button_property_id" class="cui-edit-field" style="width:100%;box-sizing:border-box;" /></td>
				<td class="la" width="20%" align="right" padding-right="10px">${getHtmlText('ec.view.showname')}</td>
				<td class="co" width="30%">
				<#--<input type="text" id="button_property_show_name" class="cui-edit-field" style="width:100%" />-->
				<#--international_propertyshowName    international_propertyshowName_showName -->
				<@international name="buttonProperty.showName" isNew=true isOldEdit=true moduleCode="${view.assModel.entity.module.artifact}" key="" style="width:100%;margin-right: -5px;" cssClass="cui-edit-field" />
				</td>
			</tr>
			<tr>
				<td class="la" width="20%" align="right" padding-right="10px">${getHtmlText('ec.view.ishidden')}</td>
				<td class="co" width="30%"><input type="checkbox" id="isOperateHide" /></td>
				<td id="permissionFrom" style="display:none;" class="la" width="20%" align="right" padding-right="10px">${getHtmlText('ec.view.permissionFrom')}</td>
				<td id="permissionFrom_" class="co" width="30%" style="display:none;" ><input type="text" id="permissionFromName" readonly=true class="cui-edit-field" style="box-sizing:border-box;"/>
				<input type="hidden" id="permissionFromCode" readonly=true/>
				</td>
			</tr>
			<tr>
				<td class="la" width="20%" align="right" padding-right="10px">${getHtmlText('ec.view.operatetype')}</td>
				<td class="co" width="30%">
					<select id="operatetype"  class="cui-edit-field" style="width:100%">
						<option value="CUSTOM">${getText('ec.view.customoperate')}</option>
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
			<#--<tr>
				<td class="la" width="20%" align="right" padding-right="10px">Confirm</td>
				<td class="co" width="30%"><input type="checkbox" id="isconfirm" /></td>
				<td class="la" width="20%" align="right" padding-right="10px" id="confirmname">${getHtmlText('ec.view.hint')}</td>
				<td class="co confirmcontent" width="30%">
					<@international name="confirmcontent" isNew=true isOldEdit=true moduleCode="${view.assModel.entity.module.artifact}" key="" style="width:100%;margin-right: -5px;" cssClass="cui-edit-field" />
				</td>
			</tr>-->
			<tr>
				<td class="la" width="20%" align="right" padding-right="10px">${getHtmlText('ec.view.styletype')}</td>
				<td class="co" width="30%">
					<select id="buttonstyle"  class="cui-edit-field" style="width:100%">
						<option value="add">${getText('ec.view.addstyle')}</option>
						<option value="modify">${getText('ec.view.editstyle')}</option>
						<option value="del">${getText('ec.view.delstyle')}</option>
						<#--<option value="import">${getText('ec.view.importstyle')}</option>
						<option value="recover">${getText('ec.view.restorestyle')}</option>-->
					</select>
				
				</td>
				<#--<td class="la" width="20%" align="right" padding-right="10px">${getHtmlText('ec.view.useInMore')}</td>
				<td class="co" width="30%"><input type="checkbox" id="useInMore" /></td>-->
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
			<tr id="selectStypeTr" style="display:none">
				<td class="la" width="20%" align="right" padding-right="10px">${getHtmlText('ec.view.selectScript')}</td>
				<td class="co" width="30%">
					<input type="radio" name="stype" id="selectType" />		
				</td>
				<td class="la" width="20%" align="right" padding-right="10px">${getHtmlText('ec.view.customerScript')}</td>
				<td class="co" width="30%">
					<input type="radio" name="stype" id="writeType" />		
				</td>
			</tr>
			<tr id="eventTr" style="display:none;">
				<td class="la" width="30%" align="right" padding-right="10px">${getHtmlText('ec.view.customevent')}</td>
				<td class="co" width="30%">
					<div style="float:left;width:100%;height:20px;margin-bottom:5px;border:1px solid #0059B3;overflow:auto;">
						<ul id="button_event_ul">
							<li class="editeventli" style="padding-top:2px">
							<a href="#" title="${getText('ec.view.editevent')}" style="text-decoration:underline;padding-left:20px;" onclick="listec.editButtonEvent('onclick')">onclick</a>：<input type="checkbox" id="button_property_event_onclick" value="" style="padding-right:10px"/>
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
				<td class="la" width="30%" align="right" padding-right="10px">${getHtmlText('ec.view.iscallback')}
				</td>
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
									RESTORE: '<p class="baphelp-info">还原的回调函数</p><p class="baphelp-example">范例：</p><div class="baphelp-code"><pre><code>function demo(){<br/>&#160;&#160;&#160;alert(\"示例\");<br/>}</code></pre></div><p class="baphelp-hint">注意：回调函数会在保存后执行</p>'
									
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
				<td class="la" id="view_scriptCodeLabel" >${getHtmlText('js.ec.script.manager.code')}</td>
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
	<div id="operatorBar_setting_dlg" style="display:none;">
		<@errorbar id="operatorBarDialogErrorBar"></@errorbar>
		<table class="infoTable" cellpadding="0" cellspacing="0" border="0" width="80%" align="left" style="margin-top: 10px">
			<tr>
				<td class="la" width="10%" style="padding-right:10px;">${getHtmlText('ec.view.classname')}</td>
				<td class="co" width="40%"><input type="text" id="cssName" title="${getText('ec.view.whatclassnamefor')}"/></td>
			</tr>
		</table>
	</div>
	<div id="data_assoc_setting_dlg" style="display:none;">
		<div class="data-assoc-select">
			<input type="checkbox" class="data-assoc-check" id="dataAssocSelectID" /><span id="dataAssocSelectSpan" style="font-weight: bold;cursor: pointer;">PT名称</span>
		</div>
		<div class="data-echarts-select">
			<input type="checkbox" class="data-echarts-check" id="echartsSelectID" /><span id="echartsSelectSpan" style="font-weight: bold;cursor: pointer;">图表</span>
		</div>
	</div>
	
	<div id="orderby_sort_dlg" style="display:none;">
		<div style="position:absolute;left:0px;top:0px;overflow-y:auto;height:280px;width:75%;display:inline-block;">
			<table cellpadding="0" id="listColOrderTable" style="padding-left:5px;font-size:12px;padding-top:15px;" cellspacing="0" border="0" align="center" width="93%" class="infoTable">
				<tbody id="listColOrder">
				</tbody>
			</table>
		</div>
		<div id="listContent" style="background-color:#f8f6f7;position:absolute;right:0px;top:0px;width:25%;border: 1px solid #efefef; height: 280px;">
			<div class="ec-list-btndiv"><div class="ec-list-topbtn" onclick="ec.firstRow('list')" id="firstMove" ></div></div>
			<div class="ec-list-btndiv"><div class="ec-list-prevbtn" onclick="ec.upRow('list')" id="upMove" ></div></div>
			<div class="ec-list-btndiv"><div class="ec-list-nextbtn" onclick="ec.downRow('list')" id="downMove"></div></div>
			<div class="ec-list-btndiv"><div class="ec-list-lastbtn" onclick="ec.lastRow('list')" id="lastMove"></div></div>
		</div>
 	</div>
	<div id="listFieldDlg" style="display:none;">
 		<@errorbar id="ListFieldDlgErrorBar"></@errorbar>
 		<input type="hidden" id="listFieldDefault" />
 		<input type="hidden" id="precision_changed_val" />
 		<div id="dlg-div-field" class="dlg-etv-navset">
		<div class="etv-scrollbar" style="margin:0 5px;">
			<ul class="etv-nav" style="display: block;">
				<li class="selected" name="proertyTabs" id="field_base_attr">
					<span class="etv-nav-span">
						<em class="etv-nav-em" style="width: 90px;">${getText('ec.view.cell.property')}</em>
					</span>
					<span class="etv-nav-span-r"></span>
				</li>
				<li name="proertyTabs" id="field_event_defined">
					<span class="etv-nav-span">
						<em class="etv-nav-em" style="width: 90px;">${getText('ec.view.cell.event')}</em>
					</span>
					<span class="etv-nav-span-r"></span>
				</li>
				<li name="proertyTabs" id="field_css_defined">
					<span class="etv-nav-span">
						<em class="etv-nav-em" style="width: 90px;">${getText('ec.view.cell.style')}</em>
					</span>
					<span class="etv-nav-span-r"></span>
				</li>
			</ul>
		</div>

	</div>
	<table id="field-tab-common" class="infoTable" cellpadding="0" cellspacing="0" border="0" width="95%" align="left" style="margin-top: 10px">
			<tr class="config_relation_model_props config_custom_props" id="propType">
				<td class="la config_relation_model_props config_custom_props" width="20%">字段类型</td>
				<td class="co config_relation_model_props config_custom_props" width="30%">
					<select id="changePropType_select" onchange="changePropType(this);">
						<option value="0">普通字段</option>
						<option value="1">关联模型字段</option>
						<option value="2">自定义字段</option>
					</select>
					<span id="config_relationModelProps" style="margin:0 10px;"><a href="#" onclick="openConfigRelationModelPropsDlg();" style="color:blue;">配置</a></span>
				</td>
				<td class="la config_custom_props" width="20%"></td>
				<td class="co config_custom_props" width="30%"></td>
			</tr>
			<tr id="propertyCodeTr">
				<td class="la" width="20%" style="padding-right:10px;align:right;color:red">${getHtmlText('ec.view.code')}</td>
				<td class="co" width="30%"><input id="property_key" type="text" class="cui-edit-field" /></td>
				<td class="la" width="20%" style="padding-right:10px;align:right" id="decimalTd">${getHtmlText('ec.property.decimalNum')}</td>
				<td class="co" width="30%" id="decimalTextTd"><input id="property_decimal" type="text" class="cui-edit-field" /></td>
			</tr>
			<tr class="config_relation_model_props">
				<td class="la config_relation_model_props" width="20%" style="padding-right:10px;align:right">${getHtmlText('ec.view.showname')}</td>
				<td class="co config_relation_model_props" width="30%">
				<#-- <input id="property_show_name" type="text" class="cui-edit-field" /> --> 
				<#--international_propertyshowName    international_propertyshowName_showName -->
					<@international name="property.showName" isNew=true isOldEdit=true moduleCode="${view.assModel.entity.module.artifact}" key="" style="width:100%;margin-right: -5px;" cssClass="cui-edit-field" />
				</td>
				<td class="la" width="20%" style="padding-right:10px;align:right">${getHtmlText('ec.view.cell.showFormat')}</td>
				<td class="co" width="30%">
					<select id="property_show_format" class="cui-edit-field" style="width:100%"></select>
					<input type="text" id="property_show_format_input" class="cui-edit-field cui-readonly-field" readonly="readonly" style="width:100%;display:none" />
				</td>
			</tr>
			<tr class="config_relation_model_props" id="widthTr">
				<td class="la config_relation_model_props" width="20%" style="padding-right:10px;align:right">${getHtmlText('ec.view.width')}</td>
				<td class="co config_relation_model_props" width="30%"><input id="property_width" type="text" class="cui-edit-field" /></td>
				<td class="la count-total" width="20%" style="padding-right:10px;align:right"></td>
				<td class="co count-total" width="30%">
				<div id="listCount_div" style="width:100%">
					<input type="checkbox" name="isCount" id="isCount">${getHtmlText('ec.property.isCount')}&nbsp;</input>
					<input type="checkbox" name="isTotal" id="isTotal"/>${getHtmlText('ec.property.isTotal')}&nbsp;</input>
				</div>
				</td>
			</tr>
			<tr id="listFieldDefaultTR" class="config_relation_model_props">
				<td class="la config_relation_model_props" width="20%" align="right" padding-right="10px">
					<div id="listField_defaultValue_div" style="width:100%">
						${getHtmlText('ec.property.nulldefault.diaplay')}
					</div>
				</td>
				<td class="co config_relation_model_props" width="80%" colspan="3">
					<input type="text" name="displayDefaultValue" id="displayDefaultValue" class="cui-edit-field" onChange="_changeDisplayValue(this)" />
					<script type="text/javascript">
						function _changeDisplayValue(obj){
							CUI('#listFieldDefault').val(CUI(obj).val());
						}
					</script>
				</td>
			</tr>
			<tr class="config_relation_model_props">
				<td class="la config_relation_model_props" width="20%" style="padding-right:10px;align:right">${getHtmlText('ec.view.ishidden')}</td>
				<td class="co config_relation_model_props" width="30%"><input type="checkbox" id="isHide" /></td>
				<td class="la config_relation_model_props" width="20%" style="padding-right:10px;align:right">${getHtmlText('ec.view.showvalue')}</td>
				<td class="co config_relation_model_props" width="30%">
					<select id="textalign"  class="cui-edit-field" style="width:100%">
						<option value="center">${getText('ec.view.center')}</option>
						<option value="left">${getText('ec.view.left')}</option>
						<option value="right">${getText('ec.view.right')}</option>
					</select>
				</td>
			</tr>
			<tr class="isorderby">
				<td class="la" width="20%" style="padding-right:10px;align:right">${getHtmlText('ec.view.isOrderBy')}</td>
				<td class="co" width="30%"><input type="checkbox" id="isOrderBy" /><input type="hidden" id="seqNumber" /></td>
				<td class="la orderby" width="20%" style="padding-right:10px;align:right;display:none;">${getHtmlText('ec.view.sortType')}</td>
				<td class="co orderby" width="30%" style="display:none;">
					<select id="sortType"  class="cui-edit-field" style="width:100%">
						<option value="asc">${getText('ec.view.asc')}</option>
						<option value="desc">${getText('ec.view.desc')}</option>
					</select>
				</td>
			</tr>
			<tr id="formulasTr" style="display:none;">
				<td class="la" width="30%" style="padding-right:10px;">${getHtmlText('ec.view.expression')}</td>
				<td class="co" width="70%" colspan="3"><input id="formulasText" type="text" class="cui-edit-field" /></td>
			</tr>
			<#if !view.entity.isBase && view.type != "REFERENCE" && view.assModel.isMain?? && view.assModel.isMain>
			<tr class="openPending" style="display:none;">
				<td class="la" width="20%" style="padding-right:10px;align:right">${getHtmlText('ec.view.openPending')}</td>
				<td class="co" width="30%"><input type="checkbox" id="openPending" /></td>
				</td>
			</tr>
			</#if>
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
			<tr class="treeNode">
				<td class="la" width="20%" align="right" padding-right="10px"><label id="isTreeNode">${getHtmlText('ec.view.isTreeNode')}</label></td>
				<td class="co" width="30%"><input type="checkbox" id="property_is_treeNode" /></td>
			</tr>
			<tr id="formatTr">
				<td class="la" width="20%" align="right" padding-right="10px">
					<div style="width:100%">
						${getHtmlText('ec.view.cell.showFunction')}
					<span id="showFunctionHelpinfo" class="baphelp-icon"></span>
<div id="showFunctionHelpinforef" style="display:none">
	<p class="baphelp-info">在列表字段配置界面中的显示方法区域添加方法,一般用于添加超链接</p>
	<p class="baphelp-example">范例</p>
<div class="baphelp-code">
<pre><code>
	 return "&lt;div style='background:#3366CC;'&gt;"+value+"&lt;/div&gt;";
</code></pre>
</div>
<p class="baphelp-hint">注意：值参数必须是value，行参数必须是nRow。</p>
</div>
					<script type="text/javascript">
							$('#showFunctionHelpinfo').helptip({refElm: "#showFunctionHelpinforef", html: true , isCustom :false, width: 400 , title :"说明"});
						</script>
						</div>
				</td>
				<td class="co" width="80%" colspan="3">
					<textarea id="showFormatFunc" class="cui-edit-textarea" style="height:80px;"></textarea>
				</td>
			</tr>
			<tr id="tr_config_custom_props_select_model" class="config_custom_props">
				<td class="la config_custom_props" width="20%">所属模型</td>
				<td class="co config_custom_props" width="30%">
					<div name="property" class="fix-search-click" style="width:95%;">
						<input id="config_custom_props_select_model" size="15" class="cui-edit-field" readonly="readonly" />
						<input type="button" onclick="selectModel(event, this);" class="cui-search-click" />
					</div>
				</td>
				<td class="la config_custom_props" width="20%"></td>
				<td class="co config_custom_props" width="30%"></td>
			</tr>
		</table>
		<div id="field-div-event" style="width:100%;align:left;height:300px;overflow-y:auto;display:none">
		<table id="field-tab-event" class="infoTable" cellpadding="0" cellspacing="0" border="0" width="95%" align="left" style="margin-top: 10px">
			<tr>
				<td class="la" width="20%" align="right" padding-right="10px"><label>${getHtmlText('ec.view.cell.eventType')}</label></td>
				<td class="co" width="30%">
					<select id="field-events-select" class="cui-edit-field" style="width:100%">
					<option value="onclick">onclick</option>
					</select>
				</td>
				<td class="la" width="20%" style="text-align:left;padding-left:10px">
					<img onclick="ec.addnewevent($('#field-events-select'),$('#field-tab-event1'))" title="${getText('ec.view.addevent')}" src="/bap/static/foundation/images/icon-add.gif" /><br/>
				</td>
				<td class="la" width="30%"></td>
			</tr>
			<tr>
				<td colspan="4">
					<table id="field-tab-event1" class="infoTable" cellpadding="0" cellspacing="0" border="0" width="100%" align="left" style="margin-top: 10px;">
						<tr style="height:0px">
							<td class="la" style="width:15%;height:0px;text-align:right"></td>
							<td style="width:35%;height:0px" class="co"></td>
							<td class="la" style="width:45%;height:0px;text-align:right"></td>
							<td style="width:5%;height:0px" class="co"></td>
						</tr>
					</table>
				</td>
			</tr>
		</table>
	</div>
	<table id="field-tab-style" class="infoTable" cellpadding="0" cellspacing="0" border="0" width="95%" align="left" style="margin-top: 10px;display:none">
		<tr style="height:0px">
			<td style="width:20%;height:0px;text-align:right" class="la"></td>
			<td class="co" style="width:30%;height:0px"></td>
			<td style="width:20%;height:0px;text-align:right" class="la"></td>
			<td class="co" style="width:30%;height:0px"></td>
		</tr>
		<tr>
			<td class="la" style="width:22%" align="right" padding-right="10px">${getHtmlText('ec.view.tdcustomstyle')}
			<span id="tdstyleHelpinfo" class="baphelp-icon"></span>
<div id="tdstyleHelpinforef" style="display:none">
	<p class="baphelp-info">自定义TD的样式</p>
	<p class="baphelp-example">范例</p>
<div class="baphelp-code">
<pre><code><span>background-color:red;</span>
</code></pre>
</div>
<p class="baphelp-hint">注意：直接写css样式。</p>
</div>
					<script type="text/javascript">
							$('#tdstyleHelpinfo').helptip({refElm: "#tdstyleHelpinforef", html: true , isCustom :false, width: 260 , title :"说明"});
						</script></td>
			<td class="co" style="width:80%"><textarea id="form_property_field_style_td" class="cui-edit-textarea" style="width:100%;height:100px;margin-top:5px"></textarea></td>
		</tr>
	</table>
	</div>
</body>
<script type="text/javascript">	

	//自动设置中间编辑区域的宽度
	function _init_size(){
		$('.col-main').width($('.col-mobile-mian').width()-450);
		$('.col-main').height($(window).height()-83);
		$('.col-sub').height($(window).height()-83);
		var paneH= $('#form_select_elements').height()- $('#form_select_elements h2').outerHeight(true)*$('#form_select_elements h2').length;
		$('#form_select_elements .accordion_pane').height(paneH);	
		//$('#layoutTree').height($(".col-sub .treeBox").height()-46);
		$('.col-operate').height($(window).height()-83);
		<#if view.openType == 'dialog'>	
		$('#form_area .etv-navset').height(${(viewHeight)!260});
		$('#form_area .etv-navset').width(${(viewWidth)!500} - 42);
		$('#form_area .etv-content').width(${(viewWidth)!500} - 42);
		$('#form_area .etv-content').height(${(viewHeight)!260} - 42 -108);
		$('#form_area .etv-content > .tab-pane').height($('#form_area .etv-content').height()-15);
		<#else>
		$('#form_area .etv-navset').height($(window).height()-83);
		$('#form_area .etv-navset').width($('.col-main').width());
		<#if (view.type)?? && view.type=='REFERENCE'>
			$('#form_area .etv-content').height($(window).height()-205); //参照配置界面
		<#else>
			$('#form_area .etv-content').height($(window).height()-315); //列表配置界面
		</#if>
		$('#form_area .etv-content').width($('.col-main').width()-10);
		$('#form_area .etv-content > .tab-pane').height($('#form_area .etv-content').height()-10);
		</#if>
		if($('#_dialogFrame').length>0){
			$('#bd_dialogFrame').css('max-height',$('.col-main').height()-50);
			$('#bd_dialogFrame').css('max-width',$('.col-main').width()-100);
			$('#_dialogFrame').css('margin-left',($('.col-main').width()-$('#bd_dialogFrame').width())/2);
		}
		
	}
	$(function(){
		_init_size();
		CUI('#multselect_model_type').change(function(){
			ec.multselect_model_type_changed(this);
		});
		CUI('#ec_select_indirect_model').change(function(){
			ec.loadModelProperties(this);
		});
		ec.multselect_model_type_changed(CUI('#multselect_model_type')[0]);
		var liArr = $("#dlg-div").find('li');
		liArr.each(function(index){
			var objMe = $(this);
			var objId = $(objMe).attr('id');
			$(this).unbind('click').bind('click',function(){
				$(this).removeClass('selected').addClass('selected');
				liArr.each(function(ind){
					if($(this).attr('id')!=objId ){
						$(this).removeClass('selected');
					}
				});
				if(objId === 'base_attr'){
					$('#tab-common').css('display','block');
					$('#div-event').css('display','none');
					$('#tab-validate').css('display','none');
					$('#tab-style').css('display','none');
				}else if(objId === 'event_defined'){
					$('#tab-common').css('display','none');
					$('#div-event').css('display','block');
					$('#tab-validate').css('display','none');
					$('#tab-style').css('display','none');
				}else if(objId === 'validate_defined'){
					$('#tab-common').css('display','none');
					$('#div-event').css('display','none');
					$('#tab-validate').css('display','block');
					$('#tab-style').css('display','none');
				}else if(objId === 'css_defined'){
					$('#tab-common').css('display','none');
					$('#div-event').css('display','none');
					$('#tab-validate').css('display','none');
					$('#tab-style').css('display','block');          
				}
			});
		});
		ec.layoutEventBind();
	});
	$(function(){
		var liArr = $("#muldlg-div").find('li');
		liArr.each(function(index){
		    var objMe = $(this);
			var objId = $(objMe).attr('id');
		    $(this).unbind('click').bind('click',function(){
				$(this).removeClass('selected').addClass('selected');
		        liArr.each(function(ind){
		            if($(this).attr('id')!=objId ){
		                $(this).removeClass('selected');
		            }
		        });
		        if(objId === 'mulbase_attr'){
		            $('#multab-common').css('display','block');
		            $('#muldiv-event').css('display','none');
		        }else if(objId === 'mulevent_defined'){
		            $('#multab-common').css('display','none');
		            $('#muldiv-event').css('display','block');
		        }
		    });
		});
		var liArr0 = $("#dlg-div-field").find('li');
		liArr0.each(function(index){
		    var objMe = $(this);
			var objId = $(objMe).attr('id');
		    $(this).unbind('click').bind('click',function(){
		    	liArr0.removeClass('selected')
				$(this).addClass('selected');
		        if(objId === 'field_base_attr'){
		            $('#field-tab-common').css('display','block');
		            $('#field-div-event').css('display','none');
		            $('#field-tab-style').css('display','none');
		        }else if(objId === 'field_event_defined'){
		            $('#field-tab-common').css('display','none');
		            $('#field-div-event').css('display','block');
		            $('#field-tab-style').css('display','none');
		        }else if(objId === 'field_css_defined'){
		            $('#field-tab-common').css('display','none');
		            $('#field-div-event').css('display','none');
		            $('#field-tab-style').css('display','block');          
		        }
		    });
		});
		var liArr1 = $("#dlg_div_public").find('li');
		liArr1.each(function(index){
		    var objMe = $(this);
			var objId = $(objMe).attr('id');
		    $(this).unbind('click').bind('click',function(){
		    	liArr1.removeClass('selected');
				$(this).addClass('selected');
		        if(objId === 'base_attr_public'){
		            $('#tab_common').css('display','block');
		            $('#tab_event').css('display','none');
		        }else if(objId === 'event_defined_public'){
		            $('#tab_common').css('display','none');
		            $('#tab_event').css('display','block');
		        }
		    });
		});
	});
	$(window).resize(function(){
	    //等页面渲染完重新计算高度
		setTimeout(function(){
			_init_size();	
		});		
	});
	
	function selectScriptCode(code){
		var windowStyle = "width=1000,height=650,top=120,left=120,scrollbars=yes,resizable =yes,toolbar=no,menubar=no,location=no,status=yes";
		<#if view?exists&&view.entity?exists>
			var param="entityCode=${view.entity.code}";
		<#elseif entity?? && entity.code??>
			var param="entityCode=${entity.code}";
		</#if>
		foundation.common.callOpenWeb("script",windowStyle,null,true,null,code,param);
		
	}
	
	function setScript(objArr){
		var code =objArr[0].scriptCode;
		this.val(code);
		//CUI('#scriptCode').val(code);
	}
	
	function changePropType(obj){
		var type = parseInt($(obj).val());
		$('td.ui-selected', '#list_design_section').removeAttr("assoFlag");
		$('td.ui-selected', '#list_design_section').removeAttr("assoConfig");
		$('#config_custom_props_select_model').val('');
		$('#config_custom_props_select_model').removeAttr('modelcode');
		$('#config_custom_props_select_model').removeAttr('layrec');
		if (type == 1) {
			$('tr:visible,td:visible', '#listFieldDlg').removeClass('listFieldDlgInit').addClass('listFieldDlgInit');
			$('tr,td', '#listFieldDlg').hide();
			$('tr.config_relation_model_props,td.config_relation_model_props', '#listFieldDlg').show();
			$(obj).next().show();
		} else if (type == 0) {
			$(obj).next().hide();
			$('tr.config_custom_props,td.config_custom_props', "#listFieldDlg").hide();
			$('tr.listFieldDlgInit,td.listFieldDlgInit', "#listFieldDlg").show();
			$('tr.listFieldDlgInit,td.listFieldDlgInit', "#listFieldDlg").removeClass("listFieldDlgInit");
			$('#tr_config_custom_props_select_model').hide();
		} else if (type == 2) {
			$('tr:visible,td:visible', '#listFieldDlg').removeClass('listFieldDlgInit').addClass('listFieldDlgInit');
			$('tr,td', '#listFieldDlg').hide();
			$('span[id="config_relationModelProps"]').hide();
			$('tr.config_custom_props,td.config_custom_props', "#listFieldDlg").show();
		}
	}
	
	/**
	* 选择展示哪个模型的自定义字段
	*/
	function selectModel(e, obj) {
		YUE.stopPropagation(e);
		showOverLayer(obj, "/msService/ec/view/config-select-model?model.code=${(view.assModel.code)!}");
	}
	var showOverLayerDiv;
	function showOverLayer(obj,url){
		CUI('#customContent').html("");
		showOverLayerDiv = new CUI.Overlay({
			align   : obj,
	     	el      : 'customContent',
	     	title   : '选择模型',
	     	width   : 180,
	     	height  : 282,
	     	zIndex  : 9999,
	     	shadow  : false,
			buttons : [
						{	name:"${getHtmlText('foundation.workbench.mainPage.sure')}",
							handler:function(){
								if ( $('li[isselected="1"]').length == 0 ) {
									$('body').trigger('click.overlayer2');
									CUI.Dialog.alert('${getText("请先选择模型！")}');
								} else {
									propertyDblClickFunc($('li[isselected="1"]')[0]);
									$('body').trigger('click.overlayer2');
								}
							}
						},
						{	name:"${getHtmlText('calendar.common.cancal')}",
							handler:function(){$('body').trigger('click.overlayer2');}
						}
					  ]
	     	
		});
		showOverLayerDiv.render();
		$("#overlay-idcustomContent").click(
			function(e){
				YUE.stopPropagation(e);
			}
		)
		url += "&time=" + new Date();
		showOverLayerDiv.show();
		$("#customContent").html('<table style="width:100%;height:100%;text-align:center;"><tr><td><label class="datagrid-loading">${getText("common.text.loading")}</label></td></tr></table>');
		CUI('#customContent').load(url);
	}
	$(function(){
		$('body').unbind('click.overlayer2').bind('click.overlayer2', function(){
			if(showOverLayerDiv && showOverLayerDiv.isShow) {
				showOverLayerDiv.close();
				$('#overlay-shim-ie').remove();
			}
		});
	});
</script>
<script type="text/javascript">
	var ec_view_select_property = {};
	$('body').unbind('click.overlayer').bind('click.overlayer', function(){
		if(ec_view_select_property.showOverLayerDiv && ec_view_select_property.showOverLayerDiv.isShow) {
			ec_view_select_property.showOverLayerDiv.close();
		}
	});
	ec_view_select_property.selectProperty = function(e, obj){
		YUE.stopPropagation(e);
		ec_view_select_property.current = $('#ec_select_property_displayName');
		ec_view_select_property.showOverLayer(obj, '/msService/ec/property/select_property?config=true&model.code=' + $('#ec_select_indirect_model').val().split('-')[1]);
	}
	ec_view_select_property.showOverLayer = function(obj,url){
		CUI('#customContent').html("");
		ec_view_select_property.showOverLayerDiv = new CUI.Overlay({
			align:obj,
			el:'customContent',
			title:'${getText('ec.property.choicefield')}',
			width:180,
			height:282,
			zIndex:9999,
			shadow:false,
			buttons:[
					{	name:"${getHtmlText('foundation.workbench.mainPage.sure')}",
						handler:function(){
							if($('li[isSelected="1"]').length == 0) {
								alert('${getText("ec.ec_view_select_property.selectNullData")}');
							} else {
								ec_view_select_property.propertyDblClickFunc($('li[isSelected="1"]')[0]);
								$('body').trigger('click.overlayer');
							}
						}
					},
					{	name:"${getHtmlText('calendar.common.cancal')}",
						handler:function(){$('body').trigger('click.overlayer');}
					}]
			
		});
		
		
		ec_view_select_property.showOverLayerDiv.render();
		$("#overlay-idcustomContent").click(
			function(e){
				YUE.stopPropagation(e);
			}
		)
		url+="&time="+new Date();
		ec_view_select_property.showOverLayerDiv.show();
		$("#customContent").html('<table style="width:100%;height:100%;text-align:center;"><tr><td><label class="datagrid-loading">${getText("common.text.loading")}</label></td></tr></table>');
		CUI('#customContent').load(url);
	}
	ec_view_select_property.propertyDblClickFunc = function(obj){
		if(propertyClickFunc(obj)) {
			var name = $(obj).attr('name');
			name = name.substr(name.indexOf('.') + 1);
			$('#ec_select_property').val(name);
			ec_view_select_property.fillPropertiesNames(ec_view_select_property.current, $(obj).attr('name'));
		}
	}
	ec_view_select_property.fillPropertiesNames = function(obj, names){
		if(obj == null) {
			obj = $('#ec_select_property_displayName');
		}
		if(names == null) {
			names = 'xx.' + $('#ec_select_property').val();
		}
		if($('#ec_select_indirect_model') && $('#ec_select_indirect_model').val()) {
			var ret = getDisplayNamesByNamesAndModelCode(names, $('#ec_select_indirect_model').val().split('-')[1]);
			obj.val('');
			for(var v in ret.data) {
				if(obj.val().length > 0) {
					obj.val(obj.val() + '.');
				}
				obj.val(obj.val() + ret.data[v]);
			}
		}
	}
	
	function showSelectPeopleSettingDialog(){
		var selectSettingDialog = new CUI.Dialog({
			title:"选人范围配置",
			elementId:"groupSelectPeople_dlg",
			//formId:"advQueryForm",
			modal:true,
			type:3,
			dragable:true,
			buttons:[{name:"确定", handler:function(){this.close();}},
					 {name:"取消", handler:function(){this.close();}}
					]
		});
		selectSettingDialog.show();
	}
	
	var nextLable;
	var nextInput;
	var groupNameInput;
	
	function selectPeopleReference(obj){
		groupNameInput = $(obj).parent().prev().children("input[name='groupname']");
		nextLable = $(obj).next();
		nextInput = nextLable.next();
		//var windowStyle = "width=800,height=600,top=50,left=150,scrollbars=yes,resizable =yes,toolbar=no,menubar=no,location=no,status=yes";
		//foundation.common.callOpenWeb("staff",windowStyle,null,null,null,"selectPeopleReferenceCallBack","unassignStaffSupport=true");
		
		foundation.common.select({
				pageType : 'staff',
				closePage : true,
				callBackFuncName : 'selectPeopleReferenceCallBack',
				params : 'unassignStaffSupport=true&multiSelect=true'
			});
	}
	
	function selectPeopleReferenceCallBack(staff){
		for(var i = 0; i < staff.length; i++){
			$(groupNameInput).val(staff[i].groupName);
			$(nextLable).val(staff[i].name);
			$(nextInput).val(staff[i].id);
			
			if(i != staff.length-1){
				var copyElement = $('<tr><td class="la" width="20%"><input name="groupname" type="text" size="4" value="分组名称"/></td><td class="co" width="30%"><div id="selectSetting" canclick="true" class="edit-btn btn-act" onclick="selectPeopleReference(this)"><a class="cui-btn-l">&nbsp;</a><a class="cui-btn-c"><span i18n="选择">选择</span></a><a class="cui-btn-r">&nbsp;</a></div><input name="staffName" type="text" size="5" readonly="true" /><input name="selectStaffId" type="hidden"/><input name="uid" type="hidden"/></td><td class="la" width="20%"><input name="sort" type="text" size="1" value="1"/></td><td class="co" width="30%"><img onclick="addSelectRow(this)" src="/bap/static/foundation/images/icon-add.gif" /><img onclick="delSelectRow(this)" src="/bap/static/foundation/images/icon-del.gif" /></td></tr>');
				$(nextLable).parent().parent().after(copyElement);
				groupNameInput = copyElement.find("input[name='groupname']");
				nextLable = copyElement.find("input[name='staffName']");
				nextInput = copyElement.find("input[name='selectStaffId']");
			}
		}
	}
	
	function addSelectRow(obj){
		var copyElement = $('<tr><td class="la" width="20%"><input name="groupname" type="text" size="4" value="分组名称"/></td><td class="co" width="30%"><div id="selectSetting" canclick="true" class="edit-btn btn-act" onclick="selectPeopleReference(this)"><a class="cui-btn-l">&nbsp;</a><a class="cui-btn-c"><span i18n="选择">选择</span></a><a class="cui-btn-r">&nbsp;</a></div><input name="staffName" type="text" size="5" readonly="true" /><input name="selectStaffId" type="hidden"/><input name="uid" type="hidden"/></td><td class="la" width="20%"><input name="sort" type="text" size="1" value="1"/></td><td class="co" width="30%"><img onclick="addSelectRow(this)" src="/bap/static/foundation/images/icon-add.gif" /><img onclick="delSelectRow(this)" src="/bap/static/foundation/images/icon-del.gif" /></td></tr>');
		$(obj).parent().parent().after(copyElement);
	}
	function delSelectRow(obj){
		if($('tr', $(obj).parents('table')).length <=1) {
			var copyElement = $('<tr><td class="la" width="20%"><input name="groupname" type="text" size="4" value="分组名称"/></td><td class="co" width="30%"><div id="selectSetting" canclick="true" class="edit-btn btn-act" onclick="selectPeopleReference(this)"><a class="cui-btn-l">&nbsp;</a><a class="cui-btn-c"><span i18n="选择">选择</span></a><a class="cui-btn-r">&nbsp;</a></div><input name="staffName" type="text" size="5" readonly="true" /><input name="selectStaffId" type="hidden"/><input name="uid" type="hidden"/></td><td class="la" width="20%"><input name="sort" type="text" size="1" value="1"/></td><td class="co" width="30%"><img onclick="addSelectRow(this)" src="/bap/static/foundation/images/icon-add.gif" /><img onclick="delSelectRow(this)" src="/bap/static/foundation/images/icon-del.gif" /></td></tr>');
			$(obj).parent().parent().after(copyElement);
		}
		var id = $(obj).parents('tr').find('input[name="uid"]').val();
		if(id!="" && id!="-1"){
			$('#selectPeopleDel').val($('#selectPeopleDel').val() + "," + id);
		}
		$(obj).parents('tr').remove();
	}
	
	function staffSelectTypeRadio(obj){
		var value = $(obj).val();
		if(value=="customer"){
			$("#staffLimit").show();
		}else{
			$("#staffLimit").hide();
		}
	}
	
	function changeAllowView(obj){
		if($(obj).prop('checked')){
			$('#form_tr_allow_view_td1').show();
			$('#form_tr_allow_view_td2').show();
		}else{
			$('#form_tr_allow_view_td1').hide();
			$('#form_tr_allow_view_td2').hide();
		}
	}
	
	function changeMultAllowView(obj){
		if($(obj).prop('checked')){
			$('#multAllowViewTR').show();
		}else{
			$('#multAllowViewTR').hide();
		}
	}
	$(function(){
		$('ul.custom_section li.form_design_ul_li:first').children('dd').html('').html('自定义字段节');
		$('ul.custom_section li.form_design_ul_li:first').children('dd').css('text-align', 'left');
	});
	$(".layout-bt",$(".layout-config-horiz")).addClass('layoutMethod-selected');//首次默认选中行布局
	$(".layout-bt").unbind('click').bind('click',function(){
		$(".layout-bt").removeClass('layoutMethod-selected');
		$(this).addClass('layoutMethod-selected');	
		if($(this).parent().hasClass("layout-config-horiz")){
			$(".layout-options-column").css("display","none");
			$(".layout-options-row").css("display","block");
			$(".layout-configuration-title label.layout-dir").text("${getText('ec.view.layoutColumn')}");
			$(".num-columns").removeClass("column");	
			$(".num-columns").addClass("row");
			$(".num-columns").val($(".layout-options-row").attr("nums"));
		}else if($(this).parent().hasClass("layout-config-vert")){
			$(".layout-options-column").css("display","block");
			$(".layout-options-row").css("display","none");
			$(".layout-configuration-title label.layout-dir").text("${getText('ec.view.layoutRow')}");
			$(".num-columns").removeClass("row");	
			$(".num-columns").addClass("column");
			$(".num-columns").val($(".layout-options-column").attr("nums"));				
		}
	});
	$(".num-columns").unbind('change').bind('change',function(){
		var numColumns = $(".num-columns").val();
		if($(".num-columns").hasClass("row")){
			var content = '<td align="center"><img src="/bap/static/images/row-large.png" /></td>';
			$(".layout-options-row .table-showing-columns tr").empty();
			for(var i=0;i<numColumns;i++){
				$(".layout-options-row .table-showing-columns tr").append(content);
			}
			$(".layout-options-row").attr("nums",$(".num-columns").val());
		}else if($(".num-columns").hasClass("column")){
			var content = '<tr><td><img src="/bap/static/images/column-large.png" /></td></tr>';
			$(".layout-options-column .table-showing-columns").empty();
			for(var i=0;i<numColumns;i++){
				$(".layout-options-column .table-showing-columns").append(content);
			}	
			$(".layout-options-column").attr("nums",$(".num-columns").val());				
		}
	});		
	
	function changeInput(obj){
		var value;
		if(obj) {
			value = obj.value;
		} else {
			value = $('option:first', '#fast_query_select').attr('value');
		}
		var showFormat;
		$('option','#fast_query_select').each(function(){
			if($(this).attr('value') == value) {
				showFormat = $(this).attr("showFormat");
				return;
			}
		});
		if(showFormat == "SELECTCOMP") {
			$('#div_SELECTCOMP').show();
			$('#div_TEXT').hide();
			$('#div_DATE').hide();
			$('#div_SELECT').hide();
		}else if(showFormat == "SELECT" || showFormat == "CHECKBOX" || showFormat == "RADIO" || showFormat == "SYSTEMCODE" || showFormat == "ENUMERATE") {
			$('#div_SELECT').show();
			$('#div_TEXT').hide();
			$('#div_SELECTCOMP').hide();
			$('#div_DATE').hide();
		} else if (showFormat == 'Y' || showFormat == 'YM' || showFormat == 'YMD' || showFormat == 'YMD_H' || showFormat == 'YMD_HM' || showFormat == 'YMD_HMS') {
			$('#div_DATE').show();
			$('#div_TEXT').hide();
			$('#div_SELECTCOMP').hide();
			$('#div_SELECT').hide();
		} else if(showFormat != null && showFormat != undefined){
			$('#div_TEXT').show();
			$('#div_SELECTCOMP').hide();
			$('#div_DATE').hide();
			$('#div_SELECT').hide();
		}
	}
	
	function showTableDialog(){
		if($('#66387035_button_2').attr('canClick')=="true"){
			dg.advQuery.showAdv();
		}
	}
	
	function fastQuerySetting(){
		$("#fqsbutton").trigger("click");
		$("#fastQueryExpandType").hide();
	}
	
	function dataClassific(){
		$("#dcbutton").trigger("click");
	}
	
	$(function(){
		ec._init();
		$(window).resize(function(){
			// 重新刷新布局，需要等页面上所有元素渲染完成
			setTimeout(function(){
				ec.refreshLayout();
			});
		});	
		<#if view.mobile?? && view.mobile>
		<#if !(ev.config)?? || ev.config == ''>
			//初始化布局
			ec.buildLayout($("[layoutname='tabs-1']"),"column",1,true,false,0);
			ec.restoreLayoutSetting();
			//初始化查询控件
			var $queryObject = $(".tb-fixed");
			$queryObject.append($("#search-config").html());
			$queryObject.attr("layoutContent","searchWidget");
			$queryObject.attr('targetModelCode', "${view.assModel.code}");
			$queryObject.attr('targetmodelname', "${view.assModel.name}");
			$queryObject.attr('targetmodelnameText', "${getText('${view.assModel.name}')}");
			$("[layoutname='column-3']").trigger('click');
			ec.addSection();
		</#if>
		var height = $('#form_area .etv-content > .tab-pane').height();
		$("[class='ui-droppable layout-common column-fixed']").height(height - 5);
		$(".layout_h2").next("div").remove();
		$("#form_elements_container [showtype='DATAGRID']").remove();		//datagrid
		$("#form_elements_container [showtype='DATATABLE']").remove();		//datatable
		$("#form_elements_container [showtype='SEARCH']").remove();			//SEARCH
		$("#form_elements_container [showtype='line-bar']").remove();		//柱状图、折线图
		$("#form_elements_container [showtype='pie']").remove();			//饼图
		$("#form_elements_container [showtype='gauge']").remove();			//仪表盘
		$(".tb-fixed").hide();
		<#if (view.type)?? && view.type=='REFERENCE'>
		$("#buttonOperateDiv").hide();
		$('#form_area .etv-content').height($(window).height()-205);
		</#if>
	</#if>
	});
	</script>
	<@customerCondition viewCode="${view.code}"/>
</html>