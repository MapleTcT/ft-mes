<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
		<title>${(subs.mainEntityName)!}-${getText(view.displayName)}-${getText('ec.project.view_setting')}-<#if view.type == 'LIST'>${getText('ec.view.list')}<#else>${getText('ec.view.ref')}</#if></title>
		<@head />
		<link rel="stylesheet" type="text/css" href="/bap/static/jquery/assets/jquery-ui.css" />
		<script type="text/javascript" src="/bap/static/jquery/jquery.tools.js"></script>
		<script type="text/javascript" src="/bap/static/jquery/jquery.ui.js"></script>
		<style type="text/css">
			ul{margin:0;clear:both;padding-left:40px;}
			li{padding:5px;list-style:none;float:left;margin-right:15px;}
			#layarea{position:absolute;z-index:9999;top:-60px;left:0;right:0;padding:5px;background:#000080;}
			#area{border:#0080ff 1px solid;margin:10px;}
			.t{background:#FFFFFF;cursor:pointer;}
			.t tr{background:#000080;}
			.selected{background:#f2f200;}
			.la{height:30px;}
			.layoutpart{overflow:auto;}
			.drag_li{cursor:pointer;}
			.elm-layout-doc-in-wrap .elm-layout-wrap-in-north{border-bottom:1px solid #0269A3;}
			.elm-layout-doc-in-wrap .elm-layout-wrap-in-west {border-right:1px solid #0269A3;}
		</style>
    </head>
    <body class="ec-config-page">	
    	<@errorbar id="workbenchErrorBar" offsetY=83 />
		<@loadpanel />
    	<div id="area"></div>
    	<#if !isReadOnlyMode>
    	<div id="design-button" style="height:30px;width:100%;overflow:hidden;clear:both;text-align:right;margin-top:20px">
			<button class="Dialog_button btn_pointer" onclick="save()" type="button">${getHtmlText('common.button.save')}</button>&#160;&#160;&#160;&#160;&#160;
			<button class="Dialog_button btn_pointer" onclick="save(true)" type="button">${getHtmlText('common.button.saveexit')}</button>&#160;&#160;&#160;&#160;&#160;
			<button class="Dialog_button btn_pointer" onclick="publish()" type="button">${getHtmlText('ec.view.publishview')}</button>&#160;&#160;&#160;&#160;&#160;
			<#--
			<button class="Dialog_button btn_pointer" onclick="preview()" type="button">${getHtmlText('ec.view.preview')}</button>
			-->		
		</div>
		</#if>
		<script type="text/javascript">
			var _proj_config_flag = ${isProj?string};
			var basicProp = new Array('id','vcode','url','width','height','ctype','tree_isPermission','tree_crossCompanyFlag','tree_model','tree_property','tree_cond','tree_root','tree_source','tree_click','tree_dblclick');
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
					var dlg = _dialog("${getText('ec.view.layoutattr')}","layout-property?view.code=${view.code}&&part=" + id,function(){
						if($('#lp_ctype','form').val() == 'tree') {
							if($('#lp_tree_model','form').val() == undefined || $('#lp_tree_model','form').val() == '' || $('#lp_tree_model','form').val() == null) {
								layoutpartpropErrorBarWidget.showMessage("${getHtmlText('ec.view.treeModelIsNull')}");
								$('#lp_tree_model','form').focus();
								return false;
							}
							if($('#lp_tree_root','form').val() == undefined || $('#lp_tree_root','form').val() == '' || $('#lp_tree_root','form').val() == null) {
								layoutpartpropErrorBarWidget.showMessage("${getHtmlText('ec.view.treeRootIsNull')}");
								$('#lp_tree_root','form').focus();
								return false;
							}
						}
						var arr = $("#layoutpartprop form").serializeArray();
						$.each(arr,function(index,item){
							$('#'+id).attr(item.name.substring(3),item.value);
						});
						this.close();
					},600,460);
					dlg.show();
				});
			}
			/* common */
            function initSize(){
                $("#area").height($(window).height() - 100);
            }
            function _dialog(title,url, handler, width, height){
	            var params = {
	                title: title,
	                width: width ? width : 550,
	                height: height ? height : 220,
	                modal: true,
					url : url,
	                buttons: [{
	                    name: "${getHtmlText('common.button.save')}",
	                    handler: handler
	                }, {
	                    name: "${getHtmlText('common.button.cancel')}",
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
				CUI.Dialog.toggleAllButton('design-button',true,true);
				var xml = buildXml();
				$.post("/msService/ec/view/" + type + "-config",{"ev.code":"${ev.code!''}","ev.version":${ev.version!0},"ev.config":xml,"ev.view.code":"${view.code}"},function(msg){
					CUI.Dialog.toggleAllButton('design-button',true,true);
					if (msg && msg.code) {
					    if(type == 'save'){
					        workbenchErrorBarWidget.showMessage("${getText('ec.view.commonSaveSuccess')}", "s");
					    }else if(type == 'publish'){
					        workbenchErrorBarWidget.showMessage("${getText('js.ec.release.successed')}", "s");
					    }
						if(closeFlag && closeFlag == true) {
							setTimeout(function(){window.close();}, 1000);
						}else{
							location.reload();
						}
					} else {
						workbenchErrorBarWidget.showMessage("${getHtmlText('common.dialog.savefailure')}");
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
			
            var count = 0;
            var co = 0;
            function Layout(id){
                var _this = this;
                this.html = "";
                this.arr = new Array();
                this.doLayout = function(){
                	<#if ev?? && (ev.configMap)??>
                		<#assign config = ev.configMap>
                		<#assign layoutconfig = config.get('layout')>
                	<#else>
                	<#if layout?? && layout.configMap??>
						<#assign config = layout.configMap>
						<#assign layoutconfig = config.get('layout')>
					</#if>	
					</#if>	
					_this.html += '<div id ="layout_${view.name}">';
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
                        new CUI.Layout(_this.arr[i], {});
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