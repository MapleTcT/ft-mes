<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
        <title>${getText('js.ec.script.manager')}</title>
        <@maincss/>
		<@mainjs/>
		<@adpSkin />
        <style type="text/css">
            .col-operate {
                border: 1px solid #a2a2a2;
                background-color: #d4d4d4;
                width: 35px;
                overflow: hidden;
            }
            
            #main_form_tab_operate {
                z-index: 1000;
            }
            
            #main_form_tab_operate li {
                width: 30px;
                height: 30px;
                margin: 6px;
                cursor: pointer;
                display: block;
                overflow: hidden;
            }
            
            #main_form_tab_operate .p-set-icon {
                margin: 10px 10px 0px 10px;
                background: url('/bap/static/css/sprite_20120525.png') 0 -1631px no-repeat;
            }
            
            #main_form_tab_operate .s-add-icon {
                background: url('/bap/static/css/sprite_20120525.png') 0 -1511px no-repeat;
            }
            
            #main_form_tab_operate .s-del-icon {
                background: url('/bap/static/css/sprite_20120525.png') 0 -1751px no-repeat;
            }
            
            #main_form_tab_operate .s-set-icon {
                background: url('/bap/static/css/sprite_20120525.png') 0 -1841px no-repeat;
            }
            
            #main_form_tab_operate .l-add-icon {
                background: url('/bap/static/css/sprite_20120525.png') 0 -1541px no-repeat;
            }
            
            #main_form_tab_operate .l-del-icon {
                background: url('/bap/static/css/sprite_20120525.png') 0 -1781px no-repeat;
            }
            
            #main_form_tab_operate .c-pro-icon {
                background: url('/bap/static/css/sprite_20120525.png') 0 -1691px no-repeat;
            }
            
            #main_form_tab_operate .c-del-icon {
                background: url('/bap/static/css/sprite_20120525.png') 0 -1721px no-repeat;
            }
            
            #main_form_tab_operate .c-split-icon {
                background: url('/bap/static/css/sprite_20120525.png') 0 -1661px no-repeat;
            }
            
            #main_form_tab_operate .t-add-icon {
                background: url('/bap/static/css/sprite_20120525.png') 0 -1571px no-repeat;
            }
            
            #main_form_tab_operate .t-mod-icon {
                background: url('/bap/static/css/sprite_20120525.png') 0 -1601px no-repeat;
            }
            
            #main_form_tab_operate .t-del-icon {
                background: url('/bap/static/css/sprite_20120525.png') 0 -1811px no-repeat;
            }
            
            .logtable {
                margin-top: 10px;
            }
            
            .logtable td {
                padding: 0 2px;
            }
            
            #svnhistory {
                border: #0369A3 1px solid;
                width: 350px;
                height: 500px;
                position: absolute;
                right: 38px;
                top: 10px;
                z-index: 1000;
                background: #FFF;
                overflow: auto;
                display: none;
            }
        </style>
    </head>
    <body class="ec-config-page">
		<@errorbar id="workbenchErrorBar" offsetY=0 />


            <div>
				<div id="ec_entity_config_top" region="north" height="36" style="height: 36px; width: 1920px;">
					<div style="">
						<span class="config_top_title">${getHtmlText('js.ec.script.manager.scriptCode')}</span>
					</div>
				</div>

                <div>
                    <table width="100%" cellpadding="0" cellspacing="0" border="0">
                        <tr>
                            <td valign="top" style="padding-left: 16px;">
                                <div style="padding: 10px 0" id="script-info-wrap">
                                   
								   <div id="required-field">
								   ${getHtmlText('js.ec.script.manager.name')}
								   <input type="text" id="name" style="width: 250px; border: #e7e7e7 1px solid; height: 32px;" value="${(script.name)!''}" />
                                	
									
     								${getHtmlText('js.ec.script.manager.code')}
									 <input type="text" id="scriptCode" style="width: 250px; border: #e7e7e7 1px solid; height: 32px;" value="${(script.scriptCode)!''}" <#if script?? && script.id??>readonly="readonly"</#if>  />
									</div>
                                    

                             		<div class="clear">${getHtmlText('js.ec.script.manager.descript')}
							 		<textarea name="description" id="description" style="padding: 2px;width: 90%; height: 64px; border: #e7e7e7 1px solid;">${(script.description)!''}</textarea>
									 </div>

                                </div>

                                <textarea name="code" id="code" style="border: none; width: 100%; height: 430px;">${(script.code)!''} </textarea>
                                <input type="hidden" id="log" name="log" />
                                <input type="hidden" id="id" name="id" value="${(script.id)!''}" />
                                <input type="hidden" id="version" name="version" value="${(script.version)!''}" />
                                <div id="design-button" style="height: 30px; width: 100%; overflow: hidden; clear: both; text-align: right; margin-top: 20px; padding-right: 0;">
                                    <button class="Dialog_button btn-primary" onclick="save(false)" type="button">
                                       ${getHtmlText('foundation.common.save')}并新建
                                    </button>
                                    <button class="Dialog_button" type="button" onclick="save(true)">
                                      ${getHtmlText('common.button.saveexit')}
                                    </button>
                                </div>
                            </td>
                            <td align="right" valign="top" width="16" style="padding-top: 30px;">
                                <div id="svnhistory">
                                </div>
                                <div class="col-operate"  style="display:none">
                                    <ul id="main_form_tab_operate" style="height: 500px;">
                                        <li class="p-set-icon" title="">
                                        </li>
                                        <li>
                                            <hr class="line-icon"/>
                                        </li>
                                        <li class="s-add-icon" onclick="showhistory()" title="${getText('js.ec.script.manager.history')}">
                                        </li>
                                        <li class="s-set-icon" onclick="submit()" title="${getText('js.ec.script.manager.submit')}">
                                        </li>
                                        <li>
                                            <hr class="line-icon"/>
                                        </li>
                                    </ul>
                                </div>
                            </td>
                        </tr>
                    </table>
                </div>
            </div>
			<script>
			// fix:zen143957
			$('#required-field span').addClass('supplant-required-field');
			$('#code').height($(window).height() - 235 );
			</script>
            <script type="text/javascript" src="/bap/static/scripts/editarea/edit_area_full.js"></script>
            <script type="text/javascript" src="/bap/static/scripts/editarea/reg_syntax/groovy.js"></script>
            <script type="text/javascript">				
            	var language='${getCurrent('language')}';
            	var langJS='';
            	if(language=='zh_CN'){
            		langJS='zh';
            	}else if(language=='zh_TW'){
            		langJS='tw';
            	}else if(language=='en_US'){
            		langJS='en';
            	}
				editAreaLoader.init({
					id : "code",
					syntax: "groovy",
					start_highlight: true ,
					language:langJS,
					allow_toggle:false,
					save_callback : "save_callback",
					toolbar :  "save,|,search, go_to_line, fullscreen, |, undo, redo, |, select_font,|, change_smooth_selection, highlight, reset_highlight, word_wrap, |, help"
				});
               	
               	function showhistory(){
               		history();
               		$('#svnhistory').toggle();
               	}
               	
               	function history(data){
               		if(data == null || data == undefined){
               			data = {}
               		}
               		data["id"] = "${id!''}";
               		$.ajax({
               			url : "/msService/ec/history",
               			type : "POST",
               			data : data,
               			success : function(res){
               				if(res && res == -2){
               					showauth(function(){
               						history({"svnusername" : $('#svnusername').val(),"svnpassword" : $('#svnpassword').val(),"svnrem" :  $('#svnrem').val()});					
               					});
               					return;
               				}
               				var html = '<table class="logtable" width="98%" align="center" cellspacing="1" cellpadding="0" bgcolor="#CCCCCC" border="0">';
               				html += '<tr bgcolor="#EEEEEE"><td height="20" align="center">提交人</td><td align="center">备注</td><td align="center">提交时间</td></tr>';
               				for(var i = 0 ; i < res.length ; i++){
               					var e = res[i];
               					html += '<tr bgcolor="#FFFFFF"><td height="20">' + e.author + '</td><td>' + e.message + '</td><td>' + timestamptostr(e.date) + '</td></tr>'
               				}
               				html += '</table>';
               				$('#svnhistory').html(html);
               			}
               		});
               	}
               	function getCode(){
               		return editAreaLoader.getValue("code");
               	}
               	function save_callback(){
               		save(false);
               	}
               	function save(exit){
               		var data = {"script.name" : $('#name').val().trim(),"script.scriptCode" : $('#scriptCode').val().trim(),"script.description":$('#description').val(),"script.code":getCode(),"script.entityCode" : "${entityCode}"};
               		if($("#id").val()){
               			data["script.id"] = $("#id").val();
               		}
               		if($("#version").val()){
               			data["script.version"] = $("#version").val();
               		}
               		var message = '';
               		if(!$('#name').val().trim()){
               			message += ',脚本名称'
               		}
               		if(!$('#scriptCode').val().trim()){
               			message += ',脚本编码'
               		}
               		if(message){
               			workbenchErrorBarWidget.showMessage(message.substring(1)+"必填!", "f");
               			return;
               		}
               		$.ajax({
               			type : "POST",
               			url : "/msService/ec/scripts/save",
               			data : data,
               			success : function(msg){
	               			if(msg && msg.message != undefined){
		                        workbenchErrorBarWidget.showMessage("脚本编码重复，请重新填写!", "f");
		                        return ;
	                      	}
               				if(msg && msg.id > 0){
               					$("#id").val(msg.id);
               					$("#version").val(msg.version);
               					if(exit){
               						try{
					 					window.opener.refreshScriptList();
									}catch(e){
										try{
											window.opener.reload();
										}catch(e2){}
									}
               						window.close();
               					}else{
	               					try{
					 					window.opener.refreshScriptList();
									}catch(e){
										try{
											window.opener.reload();
										}catch(e2){}
									}
               						new CUI.loading({  
				                      "head": "数据处理中, 请稍等",  
				                      "opacity":50,  
				                      "bgColor":"#666666",  
				                      "show": true                  
				                  });  
                  				   setTimeout(function(){
		                              var index = window.location.href.indexOf('&id=')
		                              var url = window.location.href;
		                              if(index>-1){
		                                  url = window.location.href.substring(0,index);
		                              }
		                              window.location.href=url;},500)	
               					}
               				}
               			}
               		});
               	}
               	var svnlogdl;
               	function submit(){
               		svnlogdl = new CUI.Dialog({
               			title : "${getHtmlText('js.ec.script.manager.SVNDemo')}",
               			width : 500,
               			height : 200,
               			modal:true,
               			html : "<textarea style='width:470px;height:150px;margin-left:1px;' id='svnlog'></textarea>",
               			buttons:[
               				{	name:"${getHtmlText('ec.flow.submit')}",
               					handler:function(){$('#log').val($('#svnlog').val());doSubmit();this.close();}
               				},
               				{	name:"${getHtmlText('ec.flow.cancal')}",
               					handler:function(){this.close()}
               				}]
               		});
               		svnlogdl.show();
               	}
               	
               	var svnauthdl;
               	function showauth(callback){
               		svnauthdl= new CUI.Dialog({
               			title : "${getHtmlText('js.ec.srcipt.manager.SVNCheck')}",
               			width : 200,
               			height : 120,
               			modal:true,
               			html : "<table width='100%' align='center' cellspacing='5'><tr><td align='right'>${getHtmlText('ec.script.user')}</td><td><input type='text' id='svnusername' style='width: 150px; border: #999 1px solid; height: 20px;' /></td></tr><tr><td align='right'>${getHtmlText('ec.script.password')}</td><td><input type='password' id='svnpassword' style='width: 150px; border: #999 1px solid; height: 20px;' /></td></tr><tr><td align='right'>${getHtmlText('ec.script.rememberPassword')}</td><td><input type='checkbox' id='svnrem' value='true' /></td></tr></table>",
               			buttons:[
               				{	name:"${getHtmlText('js.ec.script.manager.ok')}",
               					handler:function(){if(callback)callback();this.close();}
               				},
               				{	name:"${getHtmlText('js.ec.script.manager.cancel')}",
               					handler:function(){this.close()}
               				}]
               		});
               		svnauthdl.show();
               	}
               	
               	function doSubmit(){
               		var data = {"script.name" : $('#name').val(),"script.scriptCode" : $('#scriptCode').val(),"script.code":getCode(),"script.description":$('#description').val(),"script.entityCode" : "${entityCode}","script.log":$('#log').val()};
               		if($("#id").val()){
               			data["script.id"] = $("#id").val();
               		}
               		if($("#version").val()){
               			data["script.version"] = $("#version").val();
               		}
               		$.ajax({
               			type : "POST",
               			url : "/msService/ec/scripts/submit",
               			data : data,
               			success : function(msg){
               				if(msg && msg == -2){
               					showauth();
               				}
               				else if(msg && msg.id > 0){
               					$("#id").val(msg);
               					$("#version").val(msg.version);
               					CUI.Dialog.alert("${getHtmlText('ec.model.submitsuccessful')}");
               					/*
               					if(svnlogdl)
               						svnlogdl.close();
               					*/
               				}
               			}
               		});
               		
               	}
               	function timestamptostr(timestamp) {
               		var　d = new Date(timestamp);
                    var jstimestamp = (d.getFullYear()) + "-" + (d.getMonth() + 1) + "-" + (d.getDate()) + " " + (d.getHours()) + ":" + (d.getMinutes()) + ":" + (d.getSeconds());
               		return jstimestamp;
               	}
            </script>
    </body>
</html>