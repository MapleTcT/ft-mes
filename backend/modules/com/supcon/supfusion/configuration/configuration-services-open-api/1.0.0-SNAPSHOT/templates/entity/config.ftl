<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
        <meta http-equiv="X-UA-Compatible" content="IE=edge" />
        <title>${getText('${entity.name!}')}</title>
        <@head />
		<@adpSkin />           
    </head>
    <body class="ec-config-page"><@loadpanel ></@loadpanel>
        <@frameset id="ec_entity_config">
		<@frame id="ec_entity_config_top" region="north" height=36>
	        <div style="">
	            <span class="config_top_title">${getHtmlText('${entity.name!}')}</span>
	            <span style="color: #FFFFFF;font-size: 15px;font-weight: bold;margin-right: 20px;float:right">
	            	<a href="/help/" target="_blank" class="help-link"></a>
	            </span>
	        </div>
        </@frame>
		<@frame id="ec_entity_config_left" region="west" width=209 resize=false>
        <div id="mainMenu" style="width: 100%;height: 100%;">
	        <div class="menu_item select-item" onclick="javascript:getAjaxContent(this,'/msService/ec/entity/edit?entity.code=${entity.code}&isView=true');">
	            <div class="menu_item_content baseinfo_item">${getHtmlText('ec.configMenu.baseinfo')}</div>
	        </div>
	        <div class="menu_item mouseout" onclick="javascript:getAjaxContent(this,'/msService/ec/model/manage?entity.code=${entity.code}');">
	            <div class="menu_item_content datamodel_item">${getHtmlText('ec.configMenu.datamodel')}</div>
	        </div>
	        <div class="menu_item mouseout" onclick="javascript:getAjaxContent(this,'/msService/ec/view/manage?entity.code=${entity.code}');">
	            <div class="menu_item_content view_item">${getHtmlText('ec.configMenu.view')}</div>
	        </div>
	        <div class="menu_item mouseout" onclick="javascript:getAjaxContent(this,'/msService/ec/entity/publishMenuFrame?entityCode=${entity.code}');">
	            <div class="menu_item_content menuinfo_item">${getHtmlText('ec.configMenu.menuInfo')}</div>
	        </div>
	        <!--
	        <div class="menu_item mouseout" onclick="javascript:getAjaxContent(this,'/msService/ec/systemCode/entityConfigFrame?systemEntity.moduleCode=${entity.module.code}');">
	            <div class="menu_item_content menuinfo_item">${getHtmlText('foundation.systemCode.systemcode')}</div>
	        </div> -->
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
	        <!--
	        <#if !entity.code?starts_with("sysbase_1.0")>
	        <div class="menu_item mouseout" onclick="javascript:getAjaxContent(this,'/foundation/portlet/frameCfg?entityCode=${entity.code}');">
	            <div class="menu_item_content portlet_item">${getHtmlText('foundation.portlet.management')}</div>
	        </div>
	        </#if>

	        <#if !entity.code?starts_with("sysbase_1.0")>
	        <div class="menu_item mouseout" onclick="javascript:getAjaxContent(this,'/msService/ec/scheduler/schedulerJobConfig?entityCode=${entity.code}');">
	            <div class="menu_item_content scheduler_item">${getHtmlText('ec.configMenu.schedulerConfig')}</div>
	        </div>
	        </#if>
	         -->
	        <div class="menu_item mouseout" onclick="javascript:getAjaxContent(this,'/msService/ec/scripts/manage?entityCode=${entity.code}');">
                <div class="menu_item_content script_item">${getHtmlText('ec.configMenu.script')}</div>
            </div>
	        <div class="menu_item mouseout" onclick="javascript:getAjaxContent(this,'/msService/ec/customCode/manage?entityCode=${entity.code}');">
	            <div class="menu_item_content customcode_item">${getHtmlText('ec.configMenu.customCode')}</div>
	        </div>
	        
	        
	        
	        <#-- 
	        <div class="menu_item mouseout" onclick="javascript:getAjaxContent(this,'/msService/ec/sob/frame?entity.code=${entity.code}');">
	            <div class="menu_item_content sob_item">${getHtmlText('ec.configMenu.data')}</div>
	        </div>
	        <div class="menu_item mouseout" onclick="javascript:getAjaxContent(this,'/msService/ec/entity/publish?entity.code=${entity.code}');">
	            <div class="menu_item_content sob_item">${getText('ec.configMenu.publish')}</div>
	        </div>
	         -->
		</div>
        </@frame>
		<@frame id="ec_entity_config_main" offsetH=0>
        	<div id="ecContentDivWrap">
				<div id="ecContentDiv""></div>
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
    	        getAjaxContent(CUI('#mainMenu').children().get(0),'/msService/ec/entity/edit?entity.code=${entity.code}&isView=true');
	        })
        </script>
    </body>
</html>
