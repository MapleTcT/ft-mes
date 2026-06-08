<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
        <title>${getText('js.ec.script.manager')}</title>
        <link rel="stylesheet" type="text/css" href="/bap/static/res/main.css" />
        <link type="text/css" rel="stylesheet" href="/bap/static/scripts/syntaxhighlighter/styles/shCoreDefault.css" />
        <link rel='stylesheet' type='text/css' href='/bap/static/css/defaultSkin.css' />
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
    <body>
        <body style="background: #93BFD8;">
            <div style="background: #FFF; position: absolute; top: 10px; left: 10px; right: 10px; bottom: 10px;">
                <div style="height: 30px; line-height: 30px; background: #E3F1F9; border-bottom: #000 1px dotted; padding: 0 20px;">
                    <span style="color: green; font-weight: bold; font-size: 14px;">${getHtmlText('js.ec.script.manager.scriptCode')}</span>
                </div>
                <div>
                    <table width="100%" cellpadding="0" cellspacing="0" border="0">
                        <tr>
                            <td valign="top" style="padding-left: 5px;">
                                <textarea name="code" id="code" style="border: none; width: 100%; height: 430px;"></textarea>
                                <div id="design-button" style="height: 30px; width: 100%; overflow: hidden; clear: both; text-align: right; margin-top: 20px">
                                    <button class="Dialog_button" onclick="save(false)" type="button">
                                       ${getHtmlText('foundation.common.save')}
                                    </button>
                                </div>
                            </td>
                            <td width="200">
                            </td>
                        </tr>
                    </table>
                </div>
            </div>
            <@mainjs />
            <script type="text/javascript" src="/bap/static/scripts/editarea/edit_area_full.js"></script>
            <script type="text/javascript" src="/bap/static/scripts/editarea/reg_syntax/groovy.js"></script>
            <script type="text/javascript">
            	(function(){
            		if(opener&&opener.getHiddenExtraParamScript){
               			$("#code").val(opener.getHiddenExtraParamScript());
               		}
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
	            })();
            	
               	
               	function save_callback(){
               		save();
               	}
               	function save(){
               		if(opener&&opener.setHiddenExtraParamScript){
               			opener.setHiddenExtraParamScript(editAreaLoader.getValue("code"));
               			window.opener= null;
						window.open("","_self"); 
						window.close();
               		}
               	}
            </script>
    </body>
</html>