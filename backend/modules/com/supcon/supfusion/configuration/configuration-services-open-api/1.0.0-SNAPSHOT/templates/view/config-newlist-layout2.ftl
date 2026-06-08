<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
		<title>${(subs.mainEntityName)!}-${getText(view.displayName)}-${getText('ec.project.view_setting')}-<#if view.type == 'LIST'>${getText('ec.view.list')}<#else>${getText('ec.view.ref')}</#if></title>
		<@head />
		<link rel="stylesheet" type="text/css" href="/bap/static/jquery/assets/jquery-ui.css" />
		<script type="text/javascript" src="/bap/static/jquery/jquery.tools.js"></script>
		<script type="text/javascript" src="/bap/static/jquery/jquery.ui.js"></script>
		<@adpSkin />
		<style type="text/css">
			ul{margin:0;clear:both;padding-left:40px;}
			li{padding:5px;list-style:none;float:left;margin-right:15px;}
			#layarea{position:absolute;z-index:9999;top:-60px;left:0;right:0;padding:5px;background:#000080;}
			#area{border:#0080ff 1px solid;margin:0px;}
			.t{background:#FFFFFF;cursor:pointer;}
			.t tr{background:#000080;}
			.selected{background:#f2f200;}
			.la{height:30px;}
			.layoutpart{overflow:auto;}
			.drag_li{cursor:pointer;}
			.elm-layout-doc-in-wrap .elm-layout-wrap-in-north{border-bottom:1px solid #0269A3;}
			.elm-layout-doc-in-wrap .elm-layout-wrap-in-west {border-right:1px solid #0269A3;}
			a.help-link {
				display: inline-block;
				width: 15px;
				height: 15px;
				background: url(/bap/static/css/edit_20150318.png) 0px -3553px no-repeat;
			}
		</style>
    </head>
    <body class="ec-config-page" id="config-newlist-layout2">	
    	<@errorbar id="workbenchErrorBar" offsetY=83 />
		<@loadpanel />
		
    	<div id="area">	
    	</div>
    	
    	<#if !isReadOnlyMode>
    	<!--<span style="color: #FFFFFF;font-size: 15px;font-weight: bold;margin: 0px 15px 0px 15px;">
			<a href="/help/" target="_blank" title="帮助文档" class="help-link"></a>
		</span>-->
    	<div id="design-button" style="height:zpx;width:100%;overflow:hidden;clear:both;text-align:right;margin-top:20px">
			<button class="Dialog_button btn_pointer btn-primary" onclick="save()" type="button">${getText('common.button.save')}</button>
			<button class="Dialog_button btn_pointer" onclick="save(true)" type="button">${getText('common.button.saveexit')}</button>
			<button class="Dialog_button btn_pointer" onclick="publish()" type="button">${getText('ec.view.publishview')}</button>
			<#--
			<button class="Dialog_button btn_pointer" onclick="preview()" type="button">${getText('ec.view.preview')}</button>		
			-->
		</div>
		</#if>
		<script type="text/javascript">
			var _proj_config_flag = ${isProj?string};
			var basicProp = new Array('id','vcode','url','width','height','ctype','treeView','tree_isPermission','tree_crossCompanyFlag','tree_model','tree_property','tree_cond','tree_root','tree_source','tree_click','tree_dblclick');
			$(function(){
				initSize();
	            $(window).resize(function(){
	                initSize();
	            });
				initLayout();
			});
			function initLayout(){
				/* 画出布局 */
				var id = "layout_${view.name}";
            	var l = new Layout(id);
				$("#area").html(l.build());
				l.create();
				/* 单元格双击事件 */
				$('.layoutpart').dblclick(function(){
					var _this = this;
                	var id = $(_this).attr('id');
                	var dlg = _dialog("${getText('ec.view.layoutattr')}","layout-property2?view.code=${view.code}&isProj=${isProj?string}&part=" + id,function(){
                		$("#lp_ctype").attr("disabled",false);
						var arr = $("#layoutpartprop form").serializeArray();
						$.each(arr,function(index,item){
							if(item.name == "treeView"){
								$('#'+id).attr(item.name,item.value);
							}else{
							    var itemName = item.name.substring(3);
							    var itemValue = item.value;
							    if(itemName=="tree_click" || itemName=="tree_dblclick"){
							    	itemValue = customJSValidateAndReplace(itemValue);
							    }
								$('#'+id).attr(itemName,itemValue);
							}
						});
						this.close();
					},600,460);
					dlg.show();
				});
			}

			/* 前台输入js代码验证替换禁止输入的字符串："1","TRUE","FALSE","true","false","undefined"*/
			function customJSValidateAndReplace(jsStr){
				if(jsStr!=null) {
					if (jsStr.indexOf("\"1\"")!=-1 ) {
						jsStr = jsStr.replace("\"1\"","\'1\'");
					} 
					if (jsStr.indexOf("\"TRUE\"")!=-1) {
						jsStr = jsStr.replace("\"TRUE\"","\'TRUE\'");
					} 
					if (jsStr.indexOf("\"true\"")!=-1) {
						jsStr = jsStr.replace("\"true\"","\'true\'");
					} 
					if (jsStr.indexOf("\"FALSE\"")!=-1 ) {
						jsStr = jsStr.replace("\"FALSE\"","\'FALSE\'");
					} 
					if (jsStr.indexOf("\"false\"")!=-1) {
						jsStr = jsStr.replace("\"false\"","\'false\'");
					} 
					if (jsStr.indexOf("\"undefined\"")!=-1){
						jsStr = jsStr.replace("\"undefined\"","\'undefined\'");
					}
				}
				return jsStr;
			}
			
			/* common */
            function initSize(){
                $("#area").height($(window).height() - 80);
            }
            function _dialog(title,url, handler, width, height){
	            var params = {
	                title: title,
	                width: width ? width : 550,
	                height: height ? height : 220,
	                modal: true,
					url : url,
	                buttons: [{
	                    name: "${getText('common.button.save')}",
	                    handler: handler
	                }, {
	                    name: "${getText('common.button.cancel')}",
	                    handler: function(){
	                        this.close();
	                    }
	                }]
	            };
	            return new CUI.Dialog(params);
	        };
			function publish(){
				save(false, 'publish');
			}
			function save(closeFlag, type){
				if(type == null) {
					type = 'save';
				}
				if(type == "publish" && !checkHasConfigFinsh('layout_${view.name}')){
					return;
				}
				CUI.Dialog.toggleAllButton('design-button',true,true);
				var xml = buildXml();
				$.post("/msService/ec/view/" + type + "-config"+(window._proj_config_flag?'?isProj=true':''),{"ev.code":"${ev.code!''}","ev.version":${ev.version!0},"ev.config":xml,"ev.view.code":"${view.code}"},function(msg){
					CUI.Dialog.toggleAllButton('design-button',true,true);
					if (msg && msg.code) {
					    if(type == 'save'){
					        workbenchErrorBarWidget.showMessage("${getText('ec.view.commonSaveSuccess')}", "s");
					    }else if(type == 'publish'){
					        workbenchErrorBarWidget.showMessage("${getText('js.ec.release.successed')}", "s");
					    }
						if(opener&&opener.ec.view.reloadDataGrid){
							opener.ec.view.reloadDataGrid();
						}
						if(closeFlag && closeFlag == true) {
							setTimeout(function(){window.close();}, 1000);
						}else{
							location.reload();
						}
					} else {
						workbenchErrorBarWidget.showMessage("${getText('common.dialog.savefailure')}");
					}
				},"json");
			}
			
			var xmlStr = "";
			function buildXml(){
				xmlStr = '<?xml version="1.0" encoding="UTF-8"?><config><layout>';
				resBuildXml('layout_${view.name}');
				xmlStr += '</layout></config>';
				return xmlStr;
			}
			
			function resBuildXml(obj) {
				if($('div[parentDiv="'+obj+'"]',$('#'+obj)).size() > 0) {
					$('div[parentDiv="'+obj+'"]',$('#'+obj)).each(function(){
						xmlStr += '<' + $(this).attr('region') + '>';
						if($('div[parentDiv="layout_'+this.id+'"]',$('#layout_'+this.id)).size() > 0) {
							xmlStr +="<layout>";
							resBuildXml('layout_' + this.id);
							xmlStr +="</layout>";
						} else {
							 for (var i = 0; i < basicProp.length; i++) {
							 	xmlStr += '<' + basicProp[i] + '><![CDATA[';
							 	if($(this).attr(basicProp[i]) != undefined && $(this).attr(basicProp[i]) != null && $(this).attr(basicProp[i]) != "") {
							 		xmlStr += $(this).attr(basicProp[i]);
							 	}
							 	xmlStr += ']]></' + basicProp[i] + '>';
							 }
						}
						xmlStr += '</' + $(this).attr('region') + '>';
					});
				}
			}
			
			//检查是否已经配置好了
			function checkHasConfigFinsh(obj){
				if($('div[parentDiv="'+obj+'"]',$('#'+obj)).size() > 0) {
					var errorMsg = "";
					if($('#west1',$('#'+obj)).attr("ctype") == undefined || $('#west1',$('#'+obj)).attr("ctype") == null || $('#west1',$('#'+obj)).attr("ctype") == ""){
						errorMsg += "${getText('ec.view.leftTreePart')}";			
					}
					if($('#center1',$('#'+obj)).attr("vcode") == undefined || $('#center1',$('#'+obj)).attr("vcode") == null || $('#center1',$('#'+obj)).attr("vcode") == ""){
						errorMsg += " "+ "${getText('ec.view.rightListPart')}";			
					}
					if(errorMsg != ""){
						errorMsg += " " + "${getText('ec.view.needConfig')}";
						workbenchErrorBarWidget.showMessage(errorMsg, "f");
						return false;
					}else{
						return true;
					}			
				}
			}
			
            var count = 0;
            var co = 0;
            function Layout(id){
                var _this = this;
                this.html = "";
                this.arr = new Array();
                this.doLayout = function(){
                	<#if ev?? && (ev.configMap)??>
                		<#assign config = ev.configMap>
                		<#assign layoutconfig = config.layout>
                	<#else>
                	<#if layout?? && layout.configMap??>
						<#assign config = layout.configMap>
						<#assign layoutconfig = config.layout>
					</#if>	
					</#if>	
					_this.html += '<div id ="layout_${view.name}">';
					_this.html += '<div id="ec_entity_config_top" region="north" height="36"><div style=""><span class="config_top_title"><span>${(subs.mainEntityName)!}-${getText(view.displayName)}-${getText('ec.project.view_setting')}-<#if view.type == 'LIST'>${getText('ec.view.list')}<#else>${getText('ec.view.ref')}</#if></span></span></div></div>';
					_this.arr.push('layout_${view.name}');
					<@layoutDiv layoutConfig=layoutconfig count=0 parent="layout_${view.name}"/>
					_this.html += '</div>';
					
                };
                this.build = function(){
                    _this.doLayout();
                    return _this.html;
                };
                this.create = function(){
                    for (var i = 0; i < _this.arr.length; i++) 
                        new CUI.Layout(_this.arr[i], {gutter:0});
                };
            }
		</script>
    </body>
</html>

<#macro layoutDiv layoutConfig count parent>
	<#assign keys = layoutConfig?keys>
	<#list keys as key>
		<#if key != 'layout'>
			<#assign newcount = count + 1>
			<#assign props = layoutConfig[key]?keys>
			<#assign newId = key + newcount>
			<#list props as prop>
				<#if prop == 'id'>
					<#assign newId = layoutConfig[key][prop]>
				</#if>
			</#list>
			_this.html += '<div id ="${newId!}" parentDiv="${parent}" region="${key}"';
			<#list props as prop>
				<#if prop != 'id' && prop != 'layout'>
					_this.html += ' ${prop}="${layoutConfig[key][prop]?js_string?html}" ';
				</#if>
			</#list>
			_this.html += ' class="layoutpart">';
			<#list props as prop>
				<#if prop == 'layout'>
					_this.html += '<div id ="layout_${newId}">';
					_this.arr.push('layout_${newId}');
					<@layoutDiv layoutConfig=layoutConfig[key][prop] count=newcount parent="layout_${newId}"/>
					_this.html += '</div>';
				</#if>
			</#list>
		_this.html += '</div>';
		<#else>
		</#if>
	</#list> 
</#macro>