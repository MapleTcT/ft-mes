<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
        <meta http-equiv="X-UA-Compatible" content="IE=edge" />
        <title>${getText('${entity.name!}')}-${getText('ec.engine.cfg')}</title>
		 <@head/>
        <#-- BAP-XA-DBZY zhanghd start -->
        <link href="/bap/static/foundation/css/advquery.css" rel="stylesheet" type="text/css" />
		<!--<script type="text/javascript" src="/bap/static/foundation/js/${getCurrent('lang')!}/advquery.js"></script>-->
		<link rel="stylesheet" type="text/css" href="/bap/static/ec/css/purchase.css">
	    <link rel="stylesheet" type="text/css" href="/bap/static/treeview/assets/treeview.css">
		<!--<script type="text/javascript" src="/bap/static/ec/js/${getCurrent('lang')!}/ec_list.js"></script>
		<script type="text/javascript" src="/bap/static/print/${getCurrent('lang')!}/printControl.js"></script>-->
		<#-- BAP-XA-DBZY zhanghd end -->
        <@adpSkin />
        <style type="text/css">
            /* .infoset_top {
                height: 30px;
                background: url('/bap/static/foundation/infoset/topbg.gif') left top repeat-x;
            }

            .infoset_top div ul {
                margin: 0;
                padding: 0;
                list-style: none;
            }

            .infoset_top div ul li {
                float: left;
                color: #FFF;
                line-height: 30px;
                margin-left: 5px;
            }

            .infoset_top div ul li a {
                color: #FFF;
                text-decoration: none;
                padding: 4px 6px 2px;
            }

            .infoset_top div ul li a:hover {
                background-color: #FFF6C3;
                color: #000;
                border: 1px solid #FDC51A;
                padding: 3px 5px 2px;
            }

            .infoset_top div ul li.dot {
                line-height: 30px;
            }

            .infoTable {
                width: 98%;
                font-size: 12px;
            }

            .infoTable td {
                height: 28px;
            }

            .infoTable td.la {
                text-align: right;
                width: 13%;
                padding-right: 5px;
            }

            .infoTable td.co {
                width: 20%;
            }

            .infoTable .textfield {
                border: 1px solid #A5C7EF;
                height: 18px;
                width: 98%;
                line-height: 18px;
            }

            .infoTable .textfield_disabled {
                border: 1px solid #909090;
                height: 18px;
                width: 98%;
                line-height: 18px;
            }

            .infoTable .textarea {
                border: 1px solid #A5C7EF;
                width: 99%;
            }

            .infoTable .textfield_readonly {
                border: 1px solid #909090;
                height: 18px;
                width: 98%;
                line-height: 18px;
            }

            .infoTable .textarea_readonly {
                border: 1px solid #909090;
                height: 60px;
                width: 99%;
                line-height: 20px;
            }

            .infoTable.co.textfield_required {
                color: #FF0000;
            }

            .cui-btn {
                border: 1px solid #E3F1F9;
            }

            .noborder input {
                border: none;
            }

            .infoTable .textfield_err {
                border: #DB0000 1px solid;
                background-color: #FCD6D6;
            }

            .cui-main-title {
                background-color: #E3F1F9;
            }

            #ec_project_datamodel_container_left {
                border: #0269A3 1px solid;
                border: none;
            }

            #ec_project_datamodel_table, #ec_project_datamodel_ass_table {
                padding-left: 5px;
            }

            div.menu_item {
                width: 100%;
                height: 29px;
                padding: 8px 0 1px 0;
                cursor: pointer;
                border-bottom: solid 1px #4891BC;
                background: url('/bap/static/ec/images/entity/background.gif');
            }

            div.select-item {
                background: url('/bap/static/ec/images/entity/background_over.gif');
            }

            div.mouseover {
                background: url('/bap/static/ec/images/entity/background_over.gif');
            }

            div.menu_item_content {
                height: 100%;
                margin-left: 15px;
                padding: 5px 0 0 33px;
            }

            div.baseinfo_item {
                background: url('/bap/static/ec/images/entity/baseinfo.gif') no-repeat;
            }
            div.custfield_item {
                background: url('/bap/static/ec/images/entity/custfield.png') no-repeat;
            }

            div.view_item {
                background: url('/bap/static/ec/images/entity/view.gif') no-repeat;
            }

            div.custpower_item {
                background: url('/bap/static/ec/images/entity/custpower.png') no-repeat;
            }

            div.BAPCode_item {
                background: url('/bap/static/ec/images/entity/BAPCode.png') no-repeat;
            }

            div.workbussiness_item {
                background: url('/bap/static/ec/images/entity/wbs.gif') no-repeat;
            }

            div.workflow_item {
                background: url('/bap/static/ec/images/entity/workflow.gif') no-repeat;
            }

            div.script_item {
                background: url('/bap/static/ec/images/entity/script.gif') no-repeat;
            }

            div.printtemplate_item {
                background: url('/bap/static/ec/images/entity/printtemplate.png') no-repeat;
            }
            div.audit_item {
                background: url('/bap/static/ec/images/entity/auditlog.png') no-repeat;
            }

            div.auditlog_item {
                background: url('/bap/static/ec/images/entity/auditlog.png') no-repeat;
            }
            #ec_entity_config_top a.help-link {
				display: inline-block;
				width: 15px;
				height: 15px;
				background: url(/bap/static/css/edit_20150318.png) 0px -3553px no-repeat;
			}

            div.import_item {
            	background: url('/bap/static/ec/images/entity/import.png') no-repeat;
            }

            div.scheduler_item {
            	background: url('/bap/static/ec/images/entity/scheduler.png') no-repeat;
            } */
        </style>
    </head>
    <body class="ec-config-page"><@loadpanel ></@loadpanel>
        <@frameset id="ec_entity_config">
		<@frame id="ec_entity_config_top" region="north" height=35>
	        <div style="">
	            <span class="config_top_title">${getHtmlText('${entity.name!}')}</span>
	        	<span style="color: #FFFFFF;font-size: 15px;font-weight: bold;margin-right: 20px;float:right">
	            	<a href="/help/" target="_blank" title="帮助文档" class="help-link"></a>
	            </span>
	        </div>
        </@frame>
		<@frame id="ec_entity_config_left" region="west" width=209 resize=false>
        <div id="mainMenu" style="width: 100%;height: 100%;">
        	<div class="menu_item select-item" onclick="javascript:getAjaxContent(this,'/msService/ec/engine/entityInfo?entity.code=${entity.code}&isView=true');">
	            <div class="menu_item_content baseinfo_item">${getHtmlText('ec.configMenu.baseinfo')}</div>
	        </div>
	        <#if !(entity.module.isInherentedBase!false)>
				<div class="menu_item mouseout" onclick="javascript:getAjaxContent(this,'/msService/ec/engine/viewList?entity.code=${entity.code}');">
					<div class="menu_item_content view_item">${getHtmlText('ec.engine.configMenu.view')}</div>
				</div>
				<#if !entity.module.type?? || "Mis" != entity.module.type>
				<div class="menu_item mouseout" onclick="javascript:getAjaxContent(this,'/ec/engine/bapCodeList.action?entity.code=${entity.code}');">
					<div class="menu_item_content BAPCode_item">${getHtmlText('ec.engine.configMenu.BAPCode')}</div>
				</div>
				</#if>
			</#if>
			<#if entity.workflowEnabled>
            	 <div class="menu_item mouseout" onclick="javascript:getAjaxContent(this,'/msService/ec/entity/wf?entityCode=${entity.code}');">
                <div class="menu_item_content workflow_item">${getHtmlText('ec.configMenu.workflow')}</div>
                </div>
            </#if>
            <#if !entity.code?starts_with("sysbase_1.0")>
            	 <div class="menu_item mouseout" onclick="javascript:getAjaxContent(this,'/msService/ec/import/init?entityCode=${entity.code}');">
            	      <div class="menu_item_content import_item">${getHtmlText('ec.configMenu.importTemplateConfig')}</div>
            	 </div>
            </#if>
            <div class="menu_item mouseout" onclick="javascript:getAjaxContent(this,'/msService/ec/scripts/manage?entityCode=${entity.code}');">
                 <div class="menu_item_content script_item">${getHtmlText('ec.configMenu.script')}</div>
            </div>
	        <!--<div class="menu_item mouseout" onclick="javascript:getAjaxContent(this,'/ec/engine/main.action?entity.code=${entity.code}');">
	            <div class="menu_item_content audit_item">${getHtmlText('ec.engine.configMenu.audit')}</div>
	        </div>

	        <#if entity.workflowEnabled>
            <div class="menu_item mouseout" onclick="javascript:getAjaxContent(this,'/ec/engine/engineWfconfig.action?entity.code=${entity.code}&showHistoryVersion=true');">
                <div class="menu_item_content workflow_item">${getHtmlText('ec.entity.management.ec_wfconfig')}</div>
            </div>
            </#if>
            <#if entity.module.code != "sysbase_1.0">
            <div class="menu_item mouseout" id="printTemplateId" onclick="javascript:getAjaxContent(this,'/ec/printTemplate/viewList.action?entity.code=${entity.code}');">
	            <div class="menu_item_content printtemplate_item">${getHtmlText('ec.engine.configMenu.printtemplate')}</div>
	        </div>
	        </#if>
	        <#if !entity.code?starts_with("sysbase_1.0") || entity.code=="sysbase_1.0_user" || entity.code=="sysbase_1.0_department" || entity.code=="sysbase_1.0_position">
	        <div class="menu_item mouseout" onclick="javascript:getAjaxContent(this,'/ec/import/init.action?entityCode=${entity.code}&isProj=true');">
	            <div class="menu_item_content import_item">${getHtmlText('ec.configMenu.importTemplateConfig')}</div>
	        </div>
	        </#if>
	        <div class="menu_item mouseout" onclick="javascript:getAjaxContent(this,'/ec/scheduler/schedulerConfig.action?entityCode=${entity.code}&isProj=true');">
	            <div class="menu_item_content scheduler_item">${getHtmlText('ec.configMenu.schedulerConfig')}</div>
	        </div>-->
		</div>
        </@frame>
		<@frame id="ec_entity_config_main">
        	<div style="height: 100%;">
				<div id="ecContentDiv" style="width: 100%;height: 100%;background: #fff;"></div>
			</div>
        </@frame>
		</@frameset>
        <script type="text/javascript">
			function checkImportTemplateModified(){
				var isModified = false;
		   		if($('#export-model-wrap','#ecContentDiv').size() == 1) {
       				isModified = ec.model.exportObj.isModified();
	       		}
	       		return isModified;
			}

	       	function getAjaxContent(obj, url){
	       		// 当前页签在Excel导入模板配置，未保存须提示
	       		if(checkImportTemplateModified()){
	       			CUI.Dialog.confirm(
					    '当前模板配置未保存，是否继续？',
					    function(){loadAjaxContent(obj, url);}
					);
	       		} else {
	       			loadAjaxContent(obj, url);
	       		}
            }

            function loadAjaxContent(obj, url){
            	var ieEnable = (!!window.ActiveXObject || "ActiveXObject" in window);
	        	if($(obj).attr("id") == "printTemplateId" && !ieEnable) {
					CUI.Dialog.alert("${getText('ec.printTemplate.isIEbrowserCheck')}");
				}
            	if($('#ec_customcode_Tree','#ecContentDiv').size() == 1) {
            		if($('.ico_docu_edit','#ec_customcode_Tree').size() == 1) {
            			if(!confirm(ec.customcode.node.name + "${getText('ec.custom.save.update')}")){
	            			return false;
            			}
            		}
            	}
                try {
                	if(window.createLoadPanel){createLoadPanel(false);}
                    itemClickFunction(obj);
                } catch (e) {}

                $('#ecContentDiv').load(url, null, function(){
                    try {
                        closeLoadPanel();
                    } catch (e) {
                    }
                });
            }

            function itemClickFunction(obj){
                CUI('.menu_item').removeClass('select-item');
                CUI(obj).addClass('select-item');
            }
            $(document).ready(function(){
	            CUI('.menu_item').bind('mouseenter', function(){
	                CUI(this).addClass('mouseover');
	            }).bind('mouseleave', function(){
	                CUI(this).removeClass('mouseover');
	            });
    	        getAjaxContent(CUI('#mainMenu').children().get(0),'/msService/ec/engine/entityInfo?entity.code=${entity.code}&isView=true');
        	})
        </script>
    </body>
</html>
