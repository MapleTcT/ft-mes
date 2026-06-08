<#if (Parameters.openType)?default('dialog') != 'dialog'>
<!DOCTYPE html>
<html  xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="X-UA-Compatible" content="IE=edge" />
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
	<@head/>

<link href="/bap/static/foundation/css/style.css" rel="stylesheet" type="text/css" charset="utf-8"/>
<title>${getText('foundation.basic.internationalizationManagement')}</title>
</head>
<body id="dialog_page">
</#if>
<@frame class="international-editlistframe" id="center_in" region="center" height=300 style="height:500px;overflow-y:auto;">
<input type="hidden" name="callBackFuncName" value="${callBackFuncName}" id="callBackFuncName"/>
<input type="hidden" name="moduleCode" value="${moduleCode}" id="internationalModuleCode"/>
<input type="hidden" name="isNew" value="${(isNew)?string('true','false')}" id="isNew"/><div id="international_edit_formDialogErrorBar"></div>
	<@errorbar id="international_edit_formDialogErrorBar"/>
	<div>
	<script type="text/javascript">
    String.prototype.trim=function(){
    	return this.replace(/^\s*/,'').replace(/\s*$/,'');
    }
    CUI(function(){
    	function ajaxFileUploadCommon(s) {
    		// TODO introduce global settings, allowing the client to modify
    		// them for all requests, not only timeout
    		s = jQuery.extend({}, jQuery.ajaxSettings, s);
    		var id = new Date().getTime();
    		var form = createUploadForm(id, s.formId);
    		var io = createUploadIframe(id, s.secureuri);
    		var frameId = 'jUploadFrame' + id;
    		var formId = 'jUploadForm' + id;
    		// Watch for a new set of requests
    		if (s.global && !jQuery.active++) {
    			jQuery.event.trigger("ajaxStart");
    		}
    		var requestDone = false;
    		// Create the request object
    		var xml = {}
    		if (s.global)
    			jQuery.event.trigger("ajaxSend", [ xml, s ]);
    		// Wait for a response to come back
    		var uploadCallback = function(isTimeout) {
    			var io = document.getElementById(frameId);
    			try {
    				if (io.contentWindow) {
    					xml.responseText = io.contentWindow.document.body ? io.contentWindow.document.body.innerHTML : null;
    					xml.responseXML = io.contentWindow.document.XMLDocument ? io.contentWindow.document.XMLDocument : io.contentWindow.document;

    				} else if (io.contentDocument) {
    					xml.responseText = io.contentDocument.document.body ? io.contentDocument.document.body.innerHTML : null;
    					xml.responseXML = io.contentDocument.document.XMLDocument ? io.contentDocument.document.XMLDocument : io.contentDocument.document;
    				}
    				if(foundation.common._errorCode && foundation.common._errorCode != 404) {
    					foundation.common._errorCode = null;
    					foundation.common.showErrorMsg({"_errorCode" : foundation.common._errorCode});
    				}
    			} catch (e) {
    				 workbenchErrorBarWidget.showMessage(e,"f",null,"提交出错");
    				//jQuery.handleError(s, xml, null, e);
    			}
    			if (xml || isTimeout == "timeout") {
    				requestDone = true;
    				var status;
    				try {
    					status = isTimeout != "timeout" ? "success" : "error";
    					// Make sure that the request was successful or
    					// notmodified
    					if (status != "error") {
    						// process the data (runs the xml through
    						// httpData regardless of callback)
    						var data = uploadHttpData(xml, s.dataType);
    						// If a local callback was specified, fire it
    						// and pass it the data
    						if (s.success)
    							s.success(data, status);

    						// Fire the global callback
    						if (s.global)
    							jQuery.event.trigger("ajaxSuccess", [ xml, s ]);
    					} else
    						 workbenchErrorBarWidget.showMessage(status,"f",null,"提交出错");
    						//jQuery.handleError(s, xml, status);
    				} catch (e) {
    					//add by yubo20180119
    					if(e.name == 'SyntaxError') {
    					 	if(xml && xml.responseText) {
    					 		$("#load_mask").remove();
    							$("#load_iframe_ie").remove();
    							$("#loading_wrap").remove();
    					 		var res = xml.responseText.toLowerCase();
    						 	var arr1 = res.match('<span[\\s\\S^/]*>.+</span>');
    						 	var msg = arr1[0].replace(/<span[^/>]*>/,'').replace('</span>','');
    						 	//workbenchErrorBarWidget.showMessage(msg, 'f',null,"提交出错");
    						 	//showLoginDialog();

    					 	}
    						//jQuery.handleError(s, xml, status, e);
    					} else if(e.name == 'TypeError' && e.message == "Cannot read property 'dealSuccessFlag' of undefined") {
    						status = "error";
    					 	workbenchErrorBarWidget.showMessage("网络出错，请检查网络连接后再提交","f",null,"提交出错");
    					} else {
    						status = "error";
    					 	workbenchErrorBarWidget.showMessage(e,"f",null,"提交出错");
    					}
    				}

    				// The request was completed
    				if (s.global)
    					jQuery.event.trigger("ajaxComplete", [ xml, s ]);

    				// Handle the global AJAX counter
    				if (s.global && !--jQuery.active)
    					jQuery.event.trigger("ajaxStop");

    				// Process result
    				if (s.complete)
    					s.complete(xml, status);

    				jQuery(io).unbind();

    				setTimeout(function() {
    					try {
    						jQuery(io).remove();
    						jQuery(form).remove();

    					} catch (e) {
    						 workbenchErrorBarWidget.showMessage(e,"f",null,"提交出错");
    					}
    				}, 100);
    				xml = null;
    				//closeLoadPanel();
    			}
    		}
    		// Timeout checker
    		if (s.timeout > 0) {
    			setTimeout(function() {
    				// Check to see if the request is still happening
    				if (!requestDone)
    					uploadCallback("timeout");
    			}, s.timeout);
    		}
    		try {

    			var form = jQuery('#' + formId);
    			jQuery(form).attr('action', s.url);
    			jQuery(form).attr('method', 'POST');
    			jQuery(form).attr('target', frameId);
    			if (form.encoding) {
    				jQuery(form).attr('encoding', 'multipart/form-data');
    			} else {
    				jQuery(form).attr('enctype', 'multipart/form-data');
    			}

    			//ceshi by yubo20180119
    			//window.onbeforeunload = null;


    			$.ajax({
    	   		 //data:{username:username,password:password},// 参数
    	   		 dataType:"json", //一般就给json， 以json格式传递参数，包括返回数据
    	   		 type:"post", //post， get，一般就用post好了
    	   		 url:'/sso-auth2.action',// 调用的url
    	   		 async:false, //是否异步调用，如果true，则表示是异步调用，如果是false，则表示是同步调用。
    	   		 success:function(re){
    	   		 		jQuery(form).submit();
    	   		 		setTimeout(function(){
    						var allfiles = jQuery('#' + s.formId + ' input[type="file"]');
    						jQuery('input[type="file"]', form).each(function(ind){
    							jQuery(allfiles[ind]).before(jQuery(this));
    							jQuery(allfiles[ind]).remove();
    						});
    					}, 2000);
    	   			 	//checkFlag=re;
    	   		 }, //正确返回时，调用的函数
    	   		 error : function(XMLHttpRequest,
    						textStatus, errorThrown) {
    						if(XMLHttpRequest.status == 401){
    							//alert(2);
    						}
    						//alert(3);

    				}
    			});

    		} catch (e) {
    			 workbenchErrorBarWidget.showMessage(e,"f");
    			//jQuery.handleError(s, xml, null, e);
    		}
    		jQuery('#' + frameId).bind('load', function() {
    			uploadCallback();
    		});
    		return {
    			abort : function() {
    			}
    		};

    	}

    	//add by yubo20180119
    	function showLoginDialog(fun) {

    		if( $('#dialog_login').length > 0 ){
    			return;
    		}
    		new CUI.Dialog({
    			elementId:"login",
    			title : '请先登录',
    			modal : true,
    			height : 150,
    			width : 320,
    			url : CUI_CONFIG_URL.loginDialog + '?lang=zh_cn',
    			dragable : true,
    			buttons : [
    					{
    						name : "登录",
    						handler : function() {
    							checkLogin(this);
    						}
    					}, {
    						name : "取消",
    						handler : function() {
    							this.close()
    						}
    					} ]
    		}).show();
    	};

    	function uploadHttpData(r, type) {
    		var data = !type;
    		data = type == "xml" || data ? r.responseXML : r.responseText;
    		// If the type is "script", eval it in global context
    		if (type == "script")
    			jQuery.globalEval(data);
    		// Get the JavaScript object, if JSON is used.
    		if (type == "json")
    			eval("data = " + data);
    		// evaluate scripts within html
    		if (type == "html")
    			jQuery("<div>").html(data).evalScripts();
    		return data;
    	}
    	function ajaxFileUpload(sUrl,formId) {
    		ajaxFileUploadCommon({
    				url:sUrl,
    				formId:formId,
    				secureuri:false,
    				dataType: 'json',
    				beforeSend:function() {
    					$("#loading").show();
    				},
    				complete:function() {
    					$("#loading").hide();
    				},
    				success : function(msg) {
    					var formId = 'international_edit_form';
    					if(formId && formId.length > 5){
    						var pageId = formId.substring(0, formId.length - 5);
    						var datagrids = $('body').data(pageId + '_datagrids');
    					}
    					if(msg && msg.success == false){
    	        			var errorMsgs = "";
    						CUI.each(msg.items,function(index,item){
    							if(index.indexOf('.id') != -1 && index.indexOf('.id')+3 == index.length) {
    								$("#international_edit_form *[name^='"+(index.substring(0,index.length -3))+"'][type!='hidden']").each(function(){
    									if($(this).parents('td[nullable=false]').length > 0) {
    										showErrorField($(this));
    									}
    								});
    							} else if($("#international_edit_form input[name='"+index+"AddIds'][type='hidden']").length > 0) {
    								// 多选控件验证
    								showErrorField($("#international_edit_form *[name='"+index+"AddIds']").parent('div'));
    							} else {
    								var field = CUI("#international_edit_form *[name='"+index+"']");
    								if(field.prop('type') && field.prop('type').toUpperCase() == 'HIDDEN' && field.next() && field.next().length > 0) {
    			                		showErrorField(field.next());
    			                	} else {
    			                		showErrorField(field);
    			                	}
    							}
    							CUI("#international_edit_form *[name='"+index+"']").first().focus();
    							for(var i = 0 ; i < item.length ; i++){
    								if(index.indexOf('[') != -1 && index.indexOf('List[') != -1){
    									var tmpIndex = index.substring(0, index.indexOf('List['));
    									var dgwidget = null;
    									if(datagrids && tmpIndex) {
    										for(var firstInd = 0; firstInd < datagrids.length ; firstInd++) {
    											if(datagrids[firstInd]) {
    												for(var secondInd = 0; secondInd < datagrids.length ; secondInd++) {
    													if(datagrids[firstInd][secondInd] && datagrids[firstInd][secondInd].endsWith(tmpIndex)) {
    														var dgwidget = eval(datagrids[firstInd][secondInd]+'Widget');
    														break;
    													}
    												}
    												if(dgwidget) {
    													break;
    												}
    											}
    										}
    									}
    									if(dgwidget) {
    										var json = eval(dgwidget.parseEditedData());
    										var num = parseInt(index.substring(index.indexOf('[')+1, index.indexOf(']')));
    										if(json[num] && json[num].rowIndex) {
    											num = parseInt(json[num].rowIndex) + 1;
    										}
    										dgwidget.addErrorStyle(num - 1, index.substring(index.indexOf('].')+2));
    										errorMsgs += item[i].split('：')[0] + '：第<b>' + num + '</b>行' + item[i].split('：')[1] + '<br/>';
    									} else {
    										var num = parseInt(index.substring(index.indexOf('[')+1, index.indexOf(']'))) + 1;
    										errorMsgs += item[i].split('：')[0] + '：第<b>' + num + '</b>行' + item[i].split('：')[1] + '<br/>';
    									}
    								}else{
    									errorMsgs += item[i] + '<br/>';
    								}
    							}
    						});
    						CUI.each(msg.actionErrors,function(index,item){
    							errorMsgs += item + '<br/>';
    						});
    						if(msg.exceptionMsg!=null&&msg.exceptionMsg!=""){
    							errorMsgs += msg.exceptionMsg + '<br/>';
    						}
    						var oErrorWidget = null;
    						oErrorWidget = international_edit_formDialogErrorBarWidget;
    						oErrorWidget.showMessage(errorMsgs);
    						// oErrorWidget.show(errorMsgs);
    						if(CUI.Dialog){
    					    	 CUI.Dialog.toggleAllButton('international_edit_form', true);
    					     }
    					}else{
    						window.onbeforeunload = null;
    						if(window.containerLoadPanelWidget) {
    							//setTimeout(function(){closeLoadPanel();}, 2800);
    						}
    						foundation.international.addCallbackmodulename(msg,CUI('#'+formId).serialize());
    					}
    				}
    			}
    		);
    	}

    	function submitBapForm(){//电子签名成功之后出现进度条并提交表单
    		var ecFormFlag = false;
    		var retrialFormFlag = false;
    		if(ecFormFlag && ( $('#international_edit_form').parents('.ewc-dialog-blove').length > 0 || ( window != top && !window.frameElement.getAttribute( 'v3_tab_iframe' ) ) ) ) {
    			// dialog不出进度条
    			ecFormFlag = false;
    		}
    		ecFormFlag = (ecFormFlag || retrialFormFlag);

    		//前台验证通过之后出进度条
    		CUI.Dialog.toggleAllButton('international_edit_form',true,ecFormFlag, true);
    	// TODO 延时600ms，让页面上已触发 未执行完成的事件处理完成，临时性解决办法
    	setTimeout(function(){

    			// 延迟保存数据, 解决onchange事件无法触发问题
    			var formId = 'international_edit_form';
    			var ecformflag = false;


    		    $('input[type="text"]','#'+formId).each(function(){
    		        var v=$.trim($(this).val());
    		        $(this).val(v);
    		    });
    			var files = $('input[type="file"]', '#' + formId);
    			if(ecformflag || (files!=null&&files.length>0)) {
    				ajaxFileUpload(CUI('#'+formId).attr('action'),formId);
    			} else {

    			var postData = CUI('#'+formId).serialize();
    			CUI.ajax({
    				url : CUI('#'+formId).attr('action'),
    				type : 'POST',
    				dataType : 'json',
    				data : postData,
    				error : function(XMLHttpRequest, textStatus, errorThrown){
    					//console.log("jqXHR=%o,textStatus=%o,errorThrown=%o", XMLHttpRequest, textStatus, errorThrown );
    					if (XMLHttpRequest.status==401) {
    						//showLoginDialog();
    						return ;
    					}
    					var msg = CUI.parseJSON(XMLHttpRequest.responseText);
    					var errorMsgs = "";
    					CUI.each(msg.items,function(index,item){
    						if(index.indexOf('.id') != -1 && index.indexOf('.id')+3 == index.length) {
    							$("#international_edit_form *[name^='"+(index.substring(0,index.length -3))+"'][type!='hidden']").each(function(){
    								if($(this).parents('td[nullable=false]').length > 0) {
    									showErrorField($(this));
    								}
    							});
    						} else {
    							var field = CUI("#international_edit_form *[name='"+index+"']");
    							if(field.prop('type') && field.prop('type').toUpperCase() == 'HIDDEN' && field.next() && field.next().length > 0) {
    		                		showErrorField(field.next());
    		                	} else {
    		                		showErrorField(field);
    		                	}
    						}
    						CUI("#international_edit_form *[name='"+index+"']").first().focus();
    						for(var i = 0 ; i < item.length ; i++){
    							errorMsgs += item[i] + '<br/>';
    						}
    					});
    					CUI.each(msg.actionErrors,function(index,item){
    						errorMsgs += item + '<br/>';
    					});
    					if(msg.exceptionMsg!=null&&msg.exceptionMsg!=""){
    						errorMsgs += msg.exceptionMsg + '<br/>';
    					}
    					var oErrorWidget = null;
    					oErrorWidget = international_edit_formDialogErrorBarWidget;
    					if ( $(oErrorWidget.errorInfoBar).closest('.ewc-dialog-el').length>0 ) {
    						oErrorWidget.show(errorMsgs);// 对话框内错误提示显示顶部横条
    					}	else {
    						oErrorWidget.showMessage(errorMsgs);// 编辑界面错误提示显示右上
    					}
    					if(CUI.Dialog){
    						CUI.Dialog.toggleAllButton('international_edit_form', true);
    				    }
    				},
    				success : function(msg){
    					window.onbeforeunload = null;
    					if(window.containerLoadPanelWidget) {
    						setTimeout(function(){closeLoadPanel();}, 500);
    					}
    					foundation.international.addCallbackmodulename(msg,postData);
    				}
    			});
    		}
    	}, 600);
    		return false;
    	}
    	function singleSign_callback(res,namespace,formId,buttonCode,isTree,custom_callback,isReplace,isIgnore){
    		if(res.isSuccess) {
    			var form=CUI('#'+formId);
    			if($('#signature_username1').length == 0) {
    	    		form.append("<input type='hidden' id='signature_username1' name='signatureLog.firstUserName' value=''>");
    	   	 	}
    	   	 	if($('#signature_reason1').length == 0) {
    	    		form.append("<input type='hidden' id='signature_reason1' name='signatureLog.firstReason' value=''>");
    	   	 	}
    			if($('#signature_signatureType').length == 0) {
    				form.append("<input type='hidden' id='signature_signatureType' name='signatureLog.signatureType' value='singleSign'>");
    	    	}
    	    	if($('#signature_buttonCode').length == 0) {
    	    		form.append("<input type='hidden' id='signature_buttonCode' name='signatureLog.buttonCode' value='"+buttonCode+"'>");
    	    	}
    	    	if($('#signature_password1').length == 0) {
    	    		form.append("<input type='hidden' id='signature_password1' name='signature.password1' value=''>");
    	    	}
    	    	if($('#signature_remark1').length == 0) {
    	    		form.append("<input type='hidden' id='signature_remark1' name='signatureLog.firstRemark' value=''>");
    	    	}
    	    	if($('#signature_signTime1').length == 0) {
    	    		form.append("<input type='hidden' id='signature_signTime1' name='signatureLog.firstSignTime' value=''>");
    	    	}
    	    	if($('#signatureEnabled').length == 0) {
    	    		form.append("<input type='hidden' id='signatureEnabled' name='signatureEnabled' value='true'>");
    	    	}
    	    	if(namespace != undefined && namespace != "") {
    				eval("$('#signature_reason1').val(parent.$('#foundation_signature_reason1').val())");
    				eval("$('#signature_username1').val(parent.$('#foundation_signature_username1').val())");
    				eval("$('#signature_password1').val(parent.$('#foundation_signature_password1').val())");
    				eval("$('#signature_remark1').val(parent.$('#foundation_signature_remark1').val())");
    				eval("$('#signature_signTime1').val(new Date().getTime())");
    			}
    			else {
    				$('#signature_reason1').val($('#foundation_signature_reason1').val());
    				$('#signature_username1').val($('#foundation_signature_username1').val());
    				$('#signature_password1').val($('#foundation_signature_password1').val());
    				$('#signature_remark1').val($('#foundation_signature_remark1').val());
    				$('#signature_signTime1').val(new Date().getTime());
    			}
    			if(namespace != undefined && namespace != "") {
    				parent.signatureDialog.close();
    			}
    			else {
    				signatureDialog.close();
    			}
    			if(formId == "ImportForm") {
    				foundation.importExcel.submitForm(isReplace,isIgnore);
    			}
    			else {
    				if($('#workflow_comments').length > 0) {
    					 var comments=$('#workflow_comments').val("【电子签名】"+$('#signature_remark1').val());
    				}
    	 			submitBapForm();
    	 		}
    		}
    		else {
    			if(namespace != undefined && namespace != "") {
    				parent.signature_singleSign_formDialogErrorBarWidget.showMessage("认证失败");
    			}
    			else {
    				signature_singleSign_formDialogErrorBarWidget.showMessage("认证失败");
    			}
    		}
    	}
    	function doubleSign_callback(res,namespace,formId,buttonCode,isTree,custom_callback,isReplace,isIgnore) {
    	var isSuccess=res.isSuccess;
    	if(isSuccess) {
    		if(!res.checkPowerSuccess) {
    			if(namespace != '') {
    				parent.signature_doubleSign_formDialogErrorBarWidget.showMessage('签名失败，该用户无权签名');
    			}
    			else {
    				signature_doubleSign_formDialogErrorBarWidget.showMessage('签名失败，该用户无权签名');
    			}
    			return;
    		}
    		if(namespace != '') {
    			parent.signature_doubleSign_formDialogErrorBarWidget.showMessage('签名成功','s');
    		}
    		else {
    			signature_doubleSign_formDialogErrorBarWidget.showMessage('签名成功','s');
    		}
    		try {
    			if(namespace != '') {
    				namespace=eval(namespace);
    			}
    			else {
    				namespace = null;
    			}
    		}
    		catch(e){}
    		if(namespace != null ) {
    			var username1=parent.$('#foundation_signature_username1').val();
    			var reason1=parent.$('#foundation_signature_reason1').val();
    			var remark1=parent.$('#foundation_signature_remark1').val();
    			var password1=parent.$('#foundation_signature_password1').val();
    			var username2=parent.$('#foundation_signature_username2').val();
    			var reason2=parent.$('#foundation_signature_reason2').val();
    			var remark2=parent.$('#foundation_signature_remark2').val();
    			var password2=parent.$('#foundation_signature_password2').val();
    			var signTime1=parent.$('#signature_signTime1').val();
    		}
    		else {
    			var username1=$('#foundation_signature_username1').val();
    			var reason1=$('#foundation_signature_reason1').val();
    			var remark1=$('#foundation_signature_remark1').val();
    			var password1=$('#foundation_signature_password1').val();
    			var username2=$('#foundation_signature_username2').val();
    			var reason2=$('#foundation_signature_reason2').val();
    			var remark2=$('#foundation_signature_remark2').val();
    			var password2=$('#foundation_signature_password2').val();
    			var signTime1=$('#signature_signTime1').val();
    		}
    		var form=$("#"+formId);
    		if($('#signature_username1').length == 0) {
        		form.append("<input type='hidden' id='signature_username1' name='signatureLog.firstUserName' value=''>");
        	}
    		if($('#signature_reason1').length == 0) {
        		form.append("<input type='hidden' id='signature_reason1' name='signatureLog.firstReason' value=''>");
       	 	}
       	 	if($('#signature_remark1').length == 0) {
        		form.append("<input type='hidden' id='signature_remark1' name='signatureLog.firstRemark' value=''>");
       	 	}
    		if($('#signature_password1').length == 0) {
        		form.append("<input type='hidden' id='signature_password1' name='signature.password1' value=''>");
       	 	}
    		if($('#signature_signatureType').length == 0) {
        		form.append("<input type='hidden' id='signature_signatureType' name='signatureLog.signatureType' value='doubleSign'>");
        	}
        	if($('#signature_buttonCode').length == 0) {
        		form.append("<input type='hidden' id='signature_buttonCode' name='signatureLog.buttonCode' value='"+buttonCode+"'>");
        	}
    		if($('#signature_username2').length == 0) {
        		form.append("<input type='hidden' id='signature_username2' name='signatureLog.secondUserName' value=''>");
        	}
    		if($('#signature_reason2').length == 0) {
        		form.append("<input type='hidden' id='signature_reason2' name='signatureLog.secondReason' value=''>");
       	 	}
       	 	if($('#signature_remark2').length == 0) {
        		form.append("<input type='hidden' id='signature_remark2' name='signatureLog.secondRemark' value=''>");
       	 	}
    		if($('#signature_password2').length == 0) {
        		form.append("<input type='hidden' id='signature_password2' name='signature.password2' value=''>");
       	 	}
       	 	if($('#signatureEnabled').length == 0) {
       	 		form.append("<input type='hidden' id='signatureEnabled' name='signatureEnabled' value='true'>");
       	 	}
       	 	if(namespace != null ){
       	 		if(parent.$('#signature_signTime1').length > 0) {
    	    		form.append(parent.$('#signature_signTime1'));
    	    	}
       	 	}
       	 	else {
       	 		if($('#signature_signTime1').length > 0) {
    	    		form.append($('#signature_signTime1'));
    	    	}
       	 	}

        	if($('#signature_signTime2').length == 0) {
        		form.append("<input type='hidden' id='signature_signTime2' name='signatureLog.secondSignTime' value=''>");
        	}
    		$('#signature_reason1').val(reason1);
    		$('#signature_remark1').val(remark1);
    		$('#signature_username1').val(username1);
    		$('#signature_password1').val(password1)
        	$('#signature_username2').val(username2);
        	$('#signature_reason2').val(reason2);
        	$('#signature_remark2').val(remark2);
        	$('#signature_password2').val(password2);
        	$('#signature_signTime2').val(new Date().getTime());
    		setTimeout(function(){
    			if(namespace != null ) {
    				parent.signatureDialog.close();
    			}
    			else {
    				signatureDialog.close();
    			}
    			if(custom_callback != "undefined" && custom_callback != '') {
    				var signatureInfo="signatureLog.firstUserName="+encodeURIComponent(username1)+"&signatureLog.firstReason="+encodeURIComponent(reason1)+"&signatureLog.firstSignTime="+signTime1+"&signatureLog.firstRemark="+encodeURIComponent(remark1)+"&signatureLog.buttonCode="+encodeURIComponent(buttonCode)+"&signatureLog.signatureType=doubleSign";
    				signatureInfo+="&signatureLog.secondUserName="+encodeURIComponent(username2)+"&signatureLog.secondReason="+encodeURIComponent(reason2)+"&signatureLog.secondSignTime="+new Date().getTime()+"&signatureLog.secondRemark="+encodeURIComponent(remark2)+"&signature.password1="+password1+"&signature.password2="+password2+"&signatureEnabled=true";
    				eval(custom_callback+"('"+signatureInfo+"')");
    			}
    			if(formId == "ImportForm") {
    				//var isReplace="";
    				//var isIgnore="";
    				//foundation.importExcel.submitForm(isReplace,isIgnore);
    			}
    			else {
    				submitBapForm();
    			}
    			},500);
    	}
    	else {
    		if(namespace != undefined && namespace != "") {
    			parent.signature_doubleSign_formDialogErrorBarWidget.showMessage('认证失败');
    		}
    		else {
    			signature_doubleSign_formDialogErrorBarWidget.showMessage('认证失败');
    		}
    	}
    }
    	function singleSignOk (namespace,formId,buttonCode,isTree,custom_callback,isReplace,isIgnore){
    		if(namespace != '') {
    			var username=parent.$('#foundation_signature_username1').val();
    			var password=parent.$('#foundation_signature_password1').val();
    			var reason=parent.$('#foundation_signature_reason1').val();
    			var remark=parent.$('#foundation_signature_remark1').val();
    		}
    		else {
    			var username=$('#foundation_signature_username1').val();
    			var password=$('#foundation_signature_password1').val();
    			var reason=$('#foundation_signature_reason1').val();
    			var remark=$('#foundation_signature_remark1').val();
    		}
    		var errorMsg='';
    		if(!signatureUtil.pwdAllowEmpty() && signatureUtil.isEmpty(password.trim())){
    			errorMsg+="密码不能为空<br/>";
    		}

    		if(signatureUtil.isEmpty(reason.trim())) {
    			errorMsg+="签名原因不能为空";
    		}
    		if(!signatureUtil.isEmpty(reason.trim()) && reason.length > 500) {
    			errorMsg+="签名原因不能超过500个字符";
    		}
    		if(!signatureUtil.isEmpty(remark.trim()) && remark.length > 300) {
    			errorMsg+="签名备注不能超过300个字符";
    		}
    		if(errorMsg != '') {
    			if(namespace != '') {
    				parent.signature_singleSign_formDialogErrorBarWidget.showMessage(errorMsg);
    			}
    			else {
    				signature_singleSign_formDialogErrorBarWidget.showMessage(errorMsg);
    			}
    			return ;
    		}
    		CUI.post("/signature/checkUserPassword/check.action?isFirstSigner=true&username="+encodeURIComponent(username)+"&password="+password,function(res){singleSign_callback(res,namespace,formId,buttonCode,isTree,custom_callback,isReplace,isIgnore)},"json");
    	}
    	function doubleSignOK(obj,namespace,formId,buttonCode,isTree,custom_callback,isReplace,isIgnore){
    	var canclick=$(obj).attr('canclick');
    	if(canclick == 'false') {
    		return;
    	} else {
    		if(namespace != '') {
    			var username=parent.$('#foundation_signature_username2').val();
    			if(!parent.isIE() || parent.isNotIE8()) {
    				parent.$('#foundation_signature_password2').val(parent.$('#foundation_signature_password2').next().val());
    			}
    			var password=parent.$('#foundation_signature_password2').val();
    			var reason=parent.$('#foundation_signature_reason2').val();
    			var remark=parent.$('#foundation_signature_remark2').val();
    		} else {
    			var username=$('#foundation_signature_username2').val();
    			if(!isIE() || isNotIE8()) {
    				$('#foundation_signature_password2').val($('#foundation_signature_password2').next().val());
    			}
    			var password=$('#foundation_signature_password2').val();
    			var reason=$('#foundation_signature_reason2').val();
    			var remark=$('#foundation_signature_remark2').val();
    		}
    		var errorMsg='';
    		if(signatureUtil.isEmpty(username.trim())) {
    			errorMsg+='用户名不允许为空<br/>';
    		}
    		if(namespace != '' ) {
    			if(username == parent.$('#foundation_signature_username1').val()) {
    				errorMsg+='用户2不能与用户1相同<br/>';
    			}
    		}
    		else {
    			if(username == $('#foundation_signature_username1').val()) {
    				errorMsg+='用户2不能与用户1相同<br/>';
    			}
    		}
    		if(!signatureUtil.pwdAllowEmpty() && signatureUtil.isEmpty(password.trim())){
    			errorMsg+='密码不能为空<br/>';
    		}
    		if(signatureUtil.isEmpty(reason.trim())) {
    			errorMsg+='签名原因不能为空';
    		}
    		if(!signatureUtil.isEmpty(reason.trim()) && reason.length > 500) {
    			errorMsg+='签名原因不能超过500个字符';
    		}
    		if(!signatureUtil.isEmpty(remark.trim()) && remark.length > 300) {
    			errorMsg+='签名备注不能超过300个字符';
    		}
    		if(errorMsg != '') {
    			if(namespace != '' ) {
    				parent.signature_doubleSign_formDialogErrorBarWidget.showMessage(errorMsg);
    			}
    			else {
    				signature_doubleSign_formDialogErrorBarWidget.showMessage(errorMsg);
    			}
    			return ;
    		}
    		var buttonCode='';
    		CUI.post("/signature/checkUserPassword/check.action?isFirstSigner=false&buttonCode="+encodeURIComponent(buttonCode)+"&username="+encodeURIComponent(username)+"&password="+password,function(res){doubleSign_callback(res,namespace,formId,buttonCode,false,'')},"json");
    	}
    }
    	CUI('#international_edit_form').unbind('submit.bapForm').bind('submit.bapForm',function(){
    		//每次提交时先隐藏报错信息
    		try{
    			international_edit_formDialogErrorBarWidget.close();
    		}catch(e){

    		}
    		// 清除错误标红
    		try{clearErrorLabels();}catch(e){}
    		var ecFormFlag = false;
    		var retrialFormFlag = false;
    		if(ecFormFlag && ( $(this).parents('.ewc-dialog-blove').length > 0 || ( window != top && !window.frameElement.getAttribute( 'v3_tab_iframe' ) ) ) ) {
    			// dialog不出进度条
    			ecFormFlag = false;
    		}
    		ecFormFlag = (ecFormFlag || retrialFormFlag);


    		//禁用所有按钮
    		//CUI("body").one("click", function(event){
    		//if(CUI.Dialog) CUI.Dialog.toggleAllButton('international_edit_form',true,ecFormFlag);
    		//});
    			//if(!validateForm_international_edit_form())	return false;
    		$('#international_edit_form').trigger('beforeSubmit');
    		if($('#international_edit_form input[name="operateType"]').val() == "submit"){
    			var deploymentId=$('#international_edit_form input[name="deploymentId"]');
    			var buttonCode=$('#international_edit_form input[name="buttonCode"]');
    			var namespace=$('#international_edit_form input[name="namespace"]');
    			if( deploymentId.length > 0 && deploymentId.val() != undefined && deploymentId.val() != ''){
    				var signatureInfo=signatureUtil.getSignatureInfo(true,'',deploymentId.val(),'','')//判断是否需要进行电子签名
    				if(signatureInfo[0] != '') {
    					var cancelItem = $('input[name="workFlowVarStatus"]');
    					if(cancelItem.val() != "cancel") {
    						signatureUtil.showSignatureDialog(signatureInfo[1],signatureInfo[0],'ec.flow.submit','','international_edit_form');
    						$('a[name="signature_signatureDialog_showDialog_0"]').unbind('click').bind('click',function(){singleSignOk('','international_edit_form','ec.flow.submit',false)});
    					}
    					else {
    						signatureUtil.showSignatureDialog(signatureInfo[1],signatureInfo[0],'ec.edit.remove','','international_edit_form');
    						$('a[name="signature_signatureDialog_showDialog_0"]').unbind('click').bind('click',function(){singleSignOk('','international_edit_form','ec.edit.remove',false)});
    					}

    				}
    				else {
    					submitBapForm();
    				}
    			}
    			else if( buttonCode.length > 0 && buttonCode.val() != undefined && buttonCode.val() != ''){
    				var signatureInfo=signatureUtil.getSignatureInfo(false,buttonCode.val());
    				if(signatureInfo[0] != '') {
    					if(namespace.length > 0 && namespace.val() != undefined && namespace.val() != '') {
    						parent.signatureUtil.showSignatureDialog(signatureInfo[1],signatureInfo[0],buttonCode.val(),namespace.val(),"international_edit_form",false,'');
    						if(signatureInfo[0] == 'singleSign') {
    							parent.$('a[name="signature_signatureDialog_showDialog_0"]').unbind('click').bind('click',function(){singleSignOk(namespace.val(),'international_edit_form',buttonCode.val(),false)});
    						}
    						else {
    							setTimeout(function(){parent.$('#secondSignerOK').unbind('click').bind('click',function(){doubleSignOK(this,namespace.val(),'international_edit_form',buttonCode.val(),false)});},2000);
    						}
    					}
    					else {
    						signatureUtil.showSignatureDialog(signatureInfo[1],signatureInfo[0],buttonCode.val(),'',"international_edit_form",false,'');
    						if(signatureInfo[0] == 'singleSign') {
    							$('a[name="signature_signatureDialog_showDialog_0"]').unbind('click').bind('click',function(){singleSignOk('','international_edit_form',buttonCode.val(),false)});
    						}
    						else {
    							setTimeout(function(){$('#secondSignerOK').unbind('click').bind('click',function(){doubleSignOK(this,'','international_edit_form',buttonCode.val(),false)});},2000);
    						}
    					}
    				}
    				else {
    					submitBapForm();
    				}
    			}
    			else {
    				submitBapForm();
    			}
    		}
    		else {
    			submitBapForm();
    		}
    		return false;
    	});
    });

    </script>
    <form id="international_edit_form" name="international_edit_form"  >
			<table cellspacing="0" cellpadding="0" width="90%" class="cui-fd-infotable">
				<tbody>
					<tr>
						<td style="width:12%;" class="lab">${getHtmlText('foundation.module.code')}</td>
						<td width="14%">
							<div class="fix-input-readonly"><input type="text" class="cui-noborder-input" readonly="true" id="languageModuleCode" name="moduleCode" value="${moduleCode!}" />
							<!--<span class="description">${getText('foundation.international.title')}</span>-->
						</td>
						<td style="width:13%; " class="lab">
						${getHtmlText('foundation.international.key')}
						</td>
						<td width="12%" colspan="3">
							<div class="fix-input"><input type="text" class="cui-noborder-input" readonly="true" id="languageKey"  value="${key!}" name="key"/></div>
							<input type="hidden"  id="languageKeyHidden" name="oldKey" value="${key!}" />
							<span class="description">${getHtmlText('foundation.international.codeRequired')}</span>
						</td>
						
					</tr>
					<#if languages?? && languages?size gte 3>
					<#list languages as language>
						<#assign x=0>
						<#if (language_index+1)%3==1>
						<tr>
						</#if>
							<td style="width: 10%;" class="lab">${getHtmlText('${language.displayName!}')}</td>
							<td width="15%">
							<#if internationalList?exists>
								<#list internationalList as international>
								<#if international.languageKey==language.key>
								<#assign x=1>
									<input type="hidden" name="ids" value="${international.id!}"/>
									<div class="fix-input"><input type="text" readonly="true" class="cui-noborder-input internationalvalue-input" id="internationalKey_${language.key}" name="values" value="${(international.value)?default('')}" ></input></div>

								</#if>
								</#list>
							</#if>
							<#if x==0>
								<input type="hidden" name="ids" ></input>
								<div class="fix-input"><input type="text" readonly="true" class="cui-noborder-input internationalvalue-input" id="internationalKey_${language.key}" name="values"   ></input></div>
							</#if>
							</td>
						<#if (language_index+1)%3==0>
						</tr>
						</#if>
					</#list>
					<#else>
					<tr>
					<#list languages as language>
						<#assign x=0>
						<td style="width: 10%;" class="lab">${getHtmlText('${language.displayName!}')}</td>
						<td width="15%">
						<#if internationalList?exists>
							<#list internationalList as international>
							<#if international.languageKey==language.key>
							<#assign x=1>
								<input type="hidden" name="ids" value="${international.id!}"/>
								<div class="fix-input"><input type="text" readonly="true" class="cui-noborder-input internationalvalue-input" id="internationalKey_${language.key}" name="values" value="${(international.value)?default('')}" ></input></div>
							</#if>
							</#list>
						</#if>
						<#if x==0>
							<input type="hidden" name="ids" ></input>
							<div class="fix-input"><input type="text" readonly="true" class="cui-noborder-input internationalvalue-input" id="internationalKey_${language.key}" name="values"   ></input></div>
						</#if>
						</td>
					</#list>
					<#list 1..(3-languages?size) as i>
						<td style="width: 10%;" class="lab"></td>
						<td width="15%"></td>
					</#list>
					</tr>
					</#if>
				</tbody>
			</table>
		</form>
	</div>
	
	<div class="divideLittleLine"></div>
	<div style="height: 35px">
		<form id="international_queryForm" onsubmit="return false;">
			
		
			<table cellpadding="0" cellspacing="0" class="cui-fd-infotable">
				<tr>
					<td width="12%" class="lab">${getHtmlText('foundation.international.key')}</td>
					<td width="15%"><div class="fix-input"><input type="text" name="international.key" id="internationalKey" class="cui-noborder-input" /></div></td>
					<td width="8%" class="lab">${getHtmlText('foundation.userset.language')}</td>
					<td width="15%"><select id="internationalLangKey" class="cui-edit-field" name="international.languageKey">
					
					<option value=""></option>
					<#list languages as l>
					<option value="${l.key}">${getHtmlText(l.displayName!)}</option>
					</#list>
					</select></td>
					<td width="12%" class="lab">${getHtmlText('foundation.userset.internationalvalue')}</td>
					<td width="15%"><div class="fix-input"><input type="text"  name="international.value" id="internationalenValue" value="" class="cui-noborder-input"></input></div></td>
					<td width="5%"></td>
					<td>
					    <@querybutton formId="international_queryForm" type="search" onclick="foundation.international.queryinternationals()"/>
						<@querybutton formId="international_queryForm" type="clear"  isCustomize=true customizeName="common.button.reset" onclick="foundation.international.clear();"/>
					</td>
				</tr>
			</table>
		</form>
	</div>
	<@datatable firstLoad=false dtPage="page" formId="international_queryForm" height=265 hidekey="['id','key']" dblclick="foundation.international.select${name!}International" style="margin:5px 17px;" editable=false transMethod="post" id="internationalListTable" dataUrl="/msService/ec/foundation/international/queryList">
		<@datacolumn key="key" label="${getHtmlText('foundation.international.key')}" width="300"/>
		<#list languages as l>
		<@datacolumn key="${l.key}" label="${getText(l.displayName)}" width="200"/>
		</#list>
	</@datatable>
</@frame>
<#if (Parameters.openType)?default('dialog') != 'dialog'>
</body>
</html>
</#if>
<script type="text/javascript" charset="UTF-8" language="javascript">
(function(){
	//注册命名空间
	CUI.ns("foundation.international");
	
	//隐藏多余的取消按钮
	$('#international_edit_form .cui-btn-blue').eq(1).hide();
	
	foundation.international.getInternationalValueInput=function(){
		var obj = {};
		$('.internationalvalue-input').each(function(){
			obj[this.getAttribute('id').slice(17)] = $.trim(this.value);
		});
		return obj;
	}
	
	//回填上层页面的打开值
	foundation.international.setInternationalValueInput=function(){
		var obj = foundation.international.currentEditValObj;
		if (typeof obj === 'object'){
			for (var key in obj){
				$('#internationalKey_' + key).val(obj[key]);
			}
		}
	}
		
	foundation.international.setInternationalValueInput();
	
	foundation.international.clear=function (){
		CUI.resetForm('international_queryForm');
	};
	$("#internationalLangKey").mSelect();
	/**
	 * 查询
	 * @method foundation.international.queryinternationals
	 */
	//当国际化的key是汉字时自动帮其生成国际化key并将其国际化值显示到相应语言框中
	foundation.international.initKeyValue=function(){
		var currentLanguageValue="${getCurrent('language')}";
		var keyValue=CUI("#languageKey").val();
		var regex=/^[\u4E00-\uFA29]+$/;
		var moduleValue=CUI("#languageModuleCode").val();
		if(regex.test(keyValue)){
			var d=new Date();
			var radion=d.getTime();
			var key=moduleValue+'.'+'${name?default("common")}'+'.radion'+radion;
			CUI("#languageKey").val(key);
			CUI("#languageKeyHidden").val(key);
			//获取当前语言所对应的国际化输入框，并将keyValue存入该input中
			var valueId="internationalKey_"+currentLanguageValue;
			CUI("#"+valueId).val(keyValue);
			
		}
	}
	foundation.international.initKeyValue();
	foundation.international.queryinternationals = function(){
		var dataPost="";
		var url = "/msService/ec/foundation/international/queryList?a=1";

		var internationalKey =encodeURIComponent(CUI.trim(CUI("#internationalKey").val())) ;
		var internationalLangKey =encodeURIComponent(CUI.trim(CUI('#internationalLangKey').val())) ;
		var internationalenValue =encodeURIComponent(CUI.trim(CUI('#internationalenValue').val())) ;
		if(internationalKey){
			 dataPost += "&international.key=" + internationalKey;
		}
		if(internationalLangKey){
			dataPost += "&international.languageKey=" + internationalLangKey;
		}
		if(internationalenValue){
			dataPost += "&international.value=" + internationalenValue;
		}  

		var pageSize=CUI('input[name="internationalListTable_PageLink_PageCount"]').val();
		dataPost += "&pageSize="+encodeURIComponent(pageSize);	
	    internationalListTableWidget.setRequestDataUrl(url,dataPost);

		return false;
		
	};
	foundation.international.select${name!}International=function(event,oRow){
		var arrObj = new Array();
	
		var oRows = new Array();
		if(event == undefined){
			oRows = internationalListTableWidget.selectedRows;
		}else{
			oRows.push(oRow);
		}	
		if(oRows.length == 0){
			CUI.Dialog.alert("${getHtmlText('foundation.international.checkselected')}");
			return false;
		}
		
		for(var i=0; i<oRows.length; i++){
			var obj = $.extend({}, oRows[i], {});
			delete obj.rowHtmlObj;
			arrObj.push(obj);
		}
		try{
			<#if (Parameters.openType)?default('dialog') != 'dialog'>
				if(CUI("#callBackFuncName").val() != ""){
					eval("opener." +CUI("#callBackFuncName").val() + "(arrObj)");
				}else{
					opener.getInternational(arrObj);
				}
				try{
					window.close();
				}catch(e){}
			<#else>
				if(CUI("#callBackFuncName").val() != ""){
					eval(CUI("#callBackFuncName").val() + "(arrObj)");
				}else{
					getInternational(arrObj);
				}
			</#if>
			
			
		}catch(e){
			CUI.Dialog.alert("${getHtmlText('foundation.inter.alert')}");
		}
	}
	<#if (Parameters.openType)?default('dialog') != 'dialog'>
		foundation.international.addCallback${name!}=function(res){
			if(res.dealSuccessFlag){
				eval("opener.foundation.international.addCallback${name!}(res)");
				
				international_edit_formDialogErrorBarWidget.showMessage("${getHtmlText('foundation.workbench.mainFrame.dealSuccessed')}","s");
				setTimeout(function(){
					try{
						window.close();
					}catch(e){}
					
				},1000);
			} else {
				CUI.showErrorInfos(res);
			}
	};
	</#if>
	
})();	
</script>