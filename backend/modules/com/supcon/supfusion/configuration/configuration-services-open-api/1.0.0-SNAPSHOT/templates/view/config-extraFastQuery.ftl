<script type="text/javascript">
	$(function(){
		$('#fast_select_elements').accordion();
		$('#fast_select_elements .accordion_pane').height(320);
	});
</script>
<@errorbar id="ec_fastQuery_edit_formDialogErrorBar" />
<div style="margin-left:2px;margin-top:8px;width: 160px;height: 473px;float:left;background-color:white;border:1px solid #3C7FB1">
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
    					var formId = 'ec_fastQuery_edit_form';
    					if(formId && formId.length > 5){
    						var pageId = formId.substring(0, formId.length - 5);
    						var datagrids = $('body').data(pageId + '_datagrids');
    					}
    					if(msg && msg.success == false){
    	        			var errorMsgs = "";
    						CUI.each(msg.items,function(index,item){
    							if(index.indexOf('.id') != -1 && index.indexOf('.id')+3 == index.length) {
    								$("#ec_fastQuery_edit_form *[name^='"+(index.substring(0,index.length -3))+"'][type!='hidden']").each(function(){
    									if($(this).parents('td[nullable=false]').length > 0) {
    										showErrorField($(this));
    									}
    								});
    							} else if($("#ec_fastQuery_edit_form input[name='"+index+"AddIds'][type='hidden']").length > 0) {
    								// 多选控件验证
    								showErrorField($("#ec_fastQuery_edit_form *[name='"+index+"AddIds']").parent('div'));
    							} else {
    								var field = CUI("#ec_fastQuery_edit_form *[name='"+index+"']");
    								if(field.prop('type') && field.prop('type').toUpperCase() == 'HIDDEN' && field.next() && field.next().length > 0) {
    			                		showErrorField(field.next());
    			                	} else {
    			                		showErrorField(field);
    			                	}
    							}
    							CUI("#ec_fastQuery_edit_form *[name='"+index+"']").first().focus();
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
    						oErrorWidget = ec_fastQuery_edit_formDialogErrorBarWidget;
    						oErrorWidget.showMessage(errorMsgs);
    						// oErrorWidget.show(errorMsgs);
    						if(CUI.Dialog){
    					    	 CUI.Dialog.toggleAllButton('ec_fastQuery_edit_form', true);
    					     }
    					}else{
    						window.onbeforeunload = null;
    						if(window.containerLoadPanelWidget) {
    							//setTimeout(function(){closeLoadPanel();}, 2800);
    						}
    						ec.fastQuery.editCallback(msg,CUI('#'+formId).serialize());
    					}
    				}
    			}
    		);
    	}

    	function submitBapForm(){//电子签名成功之后出现进度条并提交表单
    		var ecFormFlag = false;
    		var retrialFormFlag = false;
    		if(ecFormFlag && ( $('#ec_fastQuery_edit_form').parents('.ewc-dialog-blove').length > 0 || ( window != top && !window.frameElement.getAttribute( 'v3_tab_iframe' ) ) ) ) {
    			// dialog不出进度条
    			ecFormFlag = false;
    		}
    		ecFormFlag = (ecFormFlag || retrialFormFlag);

    		//前台验证通过之后出进度条
    		CUI.Dialog.toggleAllButton('ec_fastQuery_edit_form',true,ecFormFlag, true);
    	// TODO 延时600ms，让页面上已触发 未执行完成的事件处理完成，临时性解决办法
    	setTimeout(function(){

    			// 延迟保存数据, 解决onchange事件无法触发问题
    			var formId = 'ec_fastQuery_edit_form';
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
    				//headers:{'Content-Type':'application/json;charset=utf8'},
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
    							$("#ec_fastQuery_edit_form *[name^='"+(index.substring(0,index.length -3))+"'][type!='hidden']").each(function(){
    								if($(this).parents('td[nullable=false]').length > 0) {
    									showErrorField($(this));
    								}
    							});
    						} else {
    							var field = CUI("#ec_fastQuery_edit_form *[name='"+index+"']");
    							if(field.prop('type') && field.prop('type').toUpperCase() == 'HIDDEN' && field.next() && field.next().length > 0) {
    		                		showErrorField(field.next());
    		                	} else {
    		                		showErrorField(field);
    		                	}
    						}
    						CUI("#ec_fastQuery_edit_form *[name='"+index+"']").first().focus();
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
    					oErrorWidget = ec_fastQuery_edit_formDialogErrorBarWidget;
    					if ( $(oErrorWidget.errorInfoBar).closest('.ewc-dialog-el').length>0 ) {
    						oErrorWidget.show(errorMsgs);// 对话框内错误提示显示顶部横条
    					}	else {
    						oErrorWidget.showMessage(errorMsgs);// 编辑界面错误提示显示右上
    					}
    					if(CUI.Dialog){
    						CUI.Dialog.toggleAllButton('ec_fastQuery_edit_form', true);
    				    }
    				},
    				success : function(msg){
    					window.onbeforeunload = null;
    					if(window.containerLoadPanelWidget) {
    						setTimeout(function(){closeLoadPanel();}, 500);
    					}
    					ec.fastQuery.editCallback(msg,postData);
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
    	CUI('#ec_fastQuery_edit_form').unbind('submit.bapForm').bind('submit.bapForm',function(){
    		//每次提交时先隐藏报错信息
    		try{
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
    		//if(CUI.Dialog) CUI.Dialog.toggleAllButton('ec_fastQuery_edit_form',true,ecFormFlag);
    		//});
    			//if(!validateForm_ec_fastQuery_edit_form())	return false;
    		$('#ec_fastQuery_edit_form').trigger('beforeSubmit');
    		if($('#ec_fastQuery_edit_form input[name="operateType"]').val() == "submit"){
    			var deploymentId=$('#ec_fastQuery_edit_form input[name="deploymentId"]');
    			var buttonCode=$('#ec_fastQuery_edit_form input[name="buttonCode"]');
    			var namespace=$('#ec_fastQuery_edit_form input[name="namespace"]');
    			if( deploymentId.length > 0 && deploymentId.val() != undefined && deploymentId.val() != ''){
    				var signatureInfo=signatureUtil.getSignatureInfo(true,'',deploymentId.val(),'','')//判断是否需要进行电子签名
    				if(signatureInfo[0] != '') {
    					var cancelItem = $('input[name="workFlowVarStatus"]');
    					if(cancelItem.val() != "cancel") {
    						signatureUtil.showSignatureDialog(signatureInfo[1],signatureInfo[0],'ec.flow.submit','','ec_fastQuery_edit_form');
    						$('a[name="signature_signatureDialog_showDialog_0"]').unbind('click').bind('click',function(){singleSignOk('','ec_fastQuery_edit_form','ec.flow.submit',false)});
    					}
    					else {
    						signatureUtil.showSignatureDialog(signatureInfo[1],signatureInfo[0],'ec.edit.remove','','ec_fastQuery_edit_form');
    						$('a[name="signature_signatureDialog_showDialog_0"]').unbind('click').bind('click',function(){singleSignOk('','ec_fastQuery_edit_form','ec.edit.remove',false)});
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
    						parent.signatureUtil.showSignatureDialog(signatureInfo[1],signatureInfo[0],buttonCode.val(),namespace.val(),"ec_fastQuery_edit_form",false,'');
    						if(signatureInfo[0] == 'singleSign') {
    							parent.$('a[name="signature_signatureDialog_showDialog_0"]').unbind('click').bind('click',function(){singleSignOk(namespace.val(),'ec_fastQuery_edit_form',buttonCode.val(),false)});
    						}
    						else {
    							setTimeout(function(){parent.$('#secondSignerOK').unbind('click').bind('click',function(){doubleSignOK(this,namespace.val(),'ec_fastQuery_edit_form',buttonCode.val(),false)});},2000);
    						}
    					}
    					else {
    						signatureUtil.showSignatureDialog(signatureInfo[1],signatureInfo[0],buttonCode.val(),'',"ec_fastQuery_edit_form",false,'');
    						if(signatureInfo[0] == 'singleSign') {
    							$('a[name="signature_signatureDialog_showDialog_0"]').unbind('click').bind('click',function(){singleSignOk('','ec_fastQuery_edit_form',buttonCode.val(),false)});
    						}
    						else {
    							setTimeout(function(){$('#secondSignerOK').unbind('click').bind('click',function(){doubleSignOK(this,'','ec_fastQuery_edit_form',buttonCode.val(),false)});},2000);
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
	<form id="ec_fastQuery_edit_form" name="ec_fastQuery_edit_form" action="/msService/ec/view/save-fastqueryconfig" method="post" callback="ec.fastQuery.editCallback" onreset="clearErrorMessages(this);clearErrorLabels(this);">
		<input type="hidden" name="fastDelCells" name="fastDelCells" value="${fastDelCells!}"/>
		<#if fqj??>
		<input type="hidden" name="fqj.code" value="${fqj.code!}" />
		<input type="hidden" name="fqj.version" value="${fqj.version!}" />
		<#else>
		<input type="hidden" name="fqj.code" value="${fqj.code!}" />
		<input type="hidden" name="fqj.version" value="0" />
		</#if>
		<#if fqj.targetModel??>
		<input type="hidden" name="fqj.targetModel.code" value="${targetModel.code!}" />
		</#if>
		<input type="hidden" name="fqj.layoutName" value="${layoutName!}" />
		<input type="hidden" name="fqj.view.code" value="${fqj.view.code!}"/>
		<input type="hidden" name="fqj.queryConfig" value='${fqj.queryConfigEscapeHtml!}' />
		<input type="hidden" name="fieldConfig" value='${fieldConfig!}'/>
		<input type="hidden" name="isProj" value="${isProj?string}"/>
	</form>
	<!-- 左侧 -->
	<div id="fast_select_elements">
		<h2 class="current">${getHtmlText('ec.view.entityattribute')}</h2>
		<div class="accordion_pane" style="display:block;overflow:auto;height:320px;">
			<ul class="main_properties_container">
				<#if subs?? && (subs.properties)?? && (subs.properties)?size &gt; 0>
					<#assign properties = (subs.properties)>
					<#list properties as p>
						<#if p.type != "OFFICE" && p.type != "PROPERTYATTACHMENT" && p.type != "COLOR">
						<#if (p.name) != 'tableInfoId' && (p.name) != 'status' && (p.name) != 'version' && p.name != 'layNo' && p.name != 'layRec' && (p.type) != "LONGTEXT">
						<li source='main' modelDataType="<#if (p.model.dataType)?? && (p.model.dataType) == 2>tree<#else>simple</#if>" onclick='ec.addFastQueryField(this)' partDepend='common' propDefaultValue='${(p.defaultValue)!}' propertyCode='${(p.code)!}' namekey="${(p.displayName)!}" name='${(p.name)!}' columnType='${(p.type)!}' propShowFormat='${(p.format)!}' propShowType='${(p.fieldType)!}' entityCode='${(p.model.code)!}' multable='${(p.multable!false)?string('true','false')}'  nullable='${(p.nullable!true)?string('true','false')}' <#if p.isUsedForList?? && p.isUsedForList> list='true' <#else> list='false' </#if> fillContent='${(p.fillcontent)!}' layRec='${(p.name)!}' mnecode='${(p.isUsedMneCode!false)?string('true','false')}' propPrecision='${(p.decimalNum)!}'>
							${getHtmlText('${(p.displayName)!}')}
						</li>
						</#if>	
						</#if>
					</#list>
				</#if>
				<#if subs?? && (subs.associatedInfos)??>
					<#assign associatedInfos = (subs.associatedInfos)>
					<#assign i = 1>
					<#list associatedInfos as ass>
						<#if ass.originalProperty?? && ass.originalProperty.name != 'status'>
						<li source='test' parentModelDataType='<#if (ass.targetProperty.model.dataType)?? && (ass.targetProperty.model.dataType) == 2>tree<#else>simple</#if>' partDepend='common' assTar='${ass.targetProperty.code}' assPropertyName='${ass.targetProperty.name}' asscolumnName='${ass.targetProperty.columnName}' propertyCode="${ass.originalProperty.code!}" assOrg="${ass.originalProperty.code}" dbname='${(ass.targetProperty.model.modelName)!}' name='${(ass.originalProperty.name)!}' entityCode='${(ass.targetProperty.model.code)!}' relativeName='${(ass.targetProperty.model.tableName)!},${(ass.targetProperty.columnName)},${(ass.originalProperty.model.tableName)!},${(ass.originalProperty.columnName)!}'>
							<img sourceType='fast' align='absmiddle' src='/bap/static/treeview/assets/ectree_colse.gif' flag='true' hasclick='false' onclick='ec.showQueryAssProperty(this,"${(ass.targetProperty.model.code)!}","fast")'></img>
							${getHtmlText('${(ass.originalProperty.displayName)!}')} [${getHtmlText('${(ass.targetProperty.model.name)!}')}]
						</li>	
						</#if>
						<#assign i = i+1>
					</#list>
				</#if>
			</ul>
		</div>
		<h2>${getHtmlText('ec.view.one2manyattr')}</h2>
		<div class="accordion_pane" style="overflow:auto;height:320px;"><ul class="onetomany_properties_container">
			<#if subs?? && (subs.oneToManyAssociatedInfos)?? && (subs.oneToManyAssociatedInfos)?size &gt; 0>
				<#assign oneToManyAssociatedInfos = (subs.oneToManyAssociatedInfos)>
				<#assign j = 1>
					<#list oneToManyAssociatedInfos as onetoManyAss>
						<#assign one2ManyOriginalTableName = (onetoManyAss.targetProperty.model.tableName)!>
						<#assign one2ManyTargetPropertyName = (onetoManyAss.targetProperty.name)>
						<#if one2ManyOriginalTableName == 'WF_DEAL_INFO'>
							<#assign one2ManyOriginalTableName = (onetoManyAss.originalProperty.model.tableName + '_DI')>
							<#assign one2ManyTargetPropertyName = (onetoManyAss.targetProperty.name + onetoManyAss.targetProperty.model.modelName + 'Di')>
						<#elseif one2ManyOriginalTableName == 'WF_PAY_CLOSE_ATTENTION'>
							<#assign one2ManyOriginalTableName = (onetoManyAss.originalProperty.model.tableName + '_PA')>
							<#assign one2ManyTargetPropertyName = (onetoManyAss.targetProperty.name + onetoManyAss.targetProperty.model.modelName + 'Pa')>
						<#elseif one2ManyOriginalTableName == 'WF_SUPERVISION'>
							<#assign one2ManyOriginalTableName = (onetoManyAss.originalProperty.model.tableName + '_SV')>
							<#assign one2ManyTargetPropertyName = (onetoManyAss.targetProperty.name + onetoManyAss.targetProperty.model.modelName + 'Sv')>
						</#if>
						<li source='test' parentModelDataType='<#if (onetoManyAss.targetProperty.model.dataType)?? && (onetoManyAss.targetProperty.model.dataType) == 2>tree<#else>simple</#if>' partDepend='one2many' assTar='${onetoManyAss.targetProperty.code}' propertyCode="${onetoManyAss.targetProperty.code!}" assOrg="${onetoManyAss.originalProperty.code}" dbname='${(onetoManyAss.targetProperty.model.modelName)!}' name='${(one2ManyTargetPropertyName)!}' entityCode='${(onetoManyAss.targetProperty.model.code)!}' relativeName='${(one2ManyOriginalTableName)!},${(onetoManyAss.targetProperty.columnName)!},${(onetoManyAss.originalProperty.model.tableName)!},${(onetoManyAss.originalProperty.columnName)}'>
							<img sourceType='fast' align='absmiddle' src='/bap/static/treeview/assets/ectree_colse.gif' flag='true' hasclick='false' onclick='ec.showQueryAssProperty(this,"${(onetoManyAss.targetProperty.model.code)!}","fast")'></img>
							${getHtmlText('${(onetoManyAss.targetProperty.displayName)!}')}[${getHtmlText('${(onetoManyAss.targetProperty.model.name)!}')}]
						</li>	
						<#assign j = j+1>
				</#list>
			 </#if>
		</ul></div>
	</div>
</div>
<div name="fastQueryContentAndConfig" style="width:475px;height: 375px;background-color:white;border:2px solid #D4D4D4;float:left;margin: 8px 5px 0 7px;"> 	
	<div style="position:absolute;height:83%;width:360px;">
		<#if fqj?? && (fqj.queryConfigMap)??  && (fqj.queryConfigMap.fastProperty)??>
		<#assign fastProperty = fqj.queryConfigMap.fastProperty>
		</#if>
		<div style="float:right;padding-top:10px;<#if view??&&view.mobile==true> display:none;</#if>" >
			<input type="checkbox" name="isExpandAll" id="isExpandAll" onclick="ec.expandAllMethod(this);" <#if fastProperty?? && fastProperty.isExpandAll??><#if fastProperty.isExpandAll>checked="checked"</#if><#else>checked="checked"</#if>>${getHtmlText('ec.view.isExpandAll')}</input>
			<select id="expandType" name="expandType" class="cui-edit-field" style="width:100px;<#if fastProperty?? && !fastProperty.isExpandAll>display:none;</#if>">
				<option value="single">${getText("ec.view.isSingle")}</option>
				<option value="all" <#if fastProperty??><#if fastProperty.expandType?? && fastProperty.expandType == "all">selected</#if><#else>selected</#if> >${getText("ec.view.isAll")}</option>
			</select>
			<a style="border:1px solid #fff;padding-right:15px;" class="cui-btn mr10 cui-btn-setting" href="#" onclick="ec.fastFieldProperty()">${getHtmlText('ec.view.fastFieldProperty')}</a>
		</div>
		<div style="margin-left:10px;margin-top:40px;overflow-y:auto;clear:both;height:82%">
		<table cellpadding="0" id="fastColTable" style="font-size:12px;width:96%;"  isExpandAll="${((fastProperty.isExpandAll)!false)?string('true','false')}" expandType="${(fastProperty.expandType)!'single'}" cellspacing="0" align="center" class="infoTable">
			<tbody id="fastColOrder">
			<#if fqj?? && (fqj.queryConfigMap)?? && (fqj.queryConfigMap.fastQueryJson)?? >
			<#assign configMap = (fqj.queryConfigMap.fastQueryJson)>
			<#if (configMap.sections)??>
			<#list (configMap.sections) as section>
			<#if section??>
				<#if section.cells?? >
				<#list section.cells as cell>
				<#if (cell.element.namekey)??>
					<#assign ckname = getText(cell.element.namekey)! >
				<#else>
					<#assign ckname = (cell.element.checkname)! >
				</#if>
				<#if (cell.element.propertyCode)??>
					<#assign propCodes = (cell.element.propertyCode)?split('||')>
					<#assign propCode = propCodes[propCodes?size - 1]>
				<#else>
					<#assign propCode = 'null'>
				</#if>
				<#assign cellCode = cell.element.cellCode!ecCodeInit()>
				<#if (cell.element.propertyCode)?? &&propertyMap?? && propertyMap['${propCode}']?? >
					<#assign property = propertyMap['${propCode}'] >
					<#assign showType = cell.element.showType!>
					<#if !cell.element.showTypeHasChanged!false >
					<#assign showType = property.fieldType! >
					</#if>
					<#assign showFormat = cell.element.showFormat!>
					<#if !cell.element.showFormatHasChanged!false>
					<#assign showFormat = property.format! >
					</#if>
					<#if property.type?? && property.type=='BOOLEAN'>
						<#assign showType='SELECT'>
						<#assign showFormat = 'SELECT'>
					</#if>
					<tr <#if cell_index % 2 == 0>class="even-num" numType="even"<#else>class="odd-num" numType="odd"</#if> cellCode="${cellCode!}" key="${(cell.element.name)!}" partDepend="${(cell.element.partDepend)!'common'}" selfType="${(property.type)!}" ondblclick="ec.selectRow('fast',this);ec.fastFieldProperty();" onmousedown="ec.selectRow('fast',this);" defaultValue="<#if cell.element.defaultValueHasChanged!false>${(cell.element.defaultValue!)?string}<#else>${(property.defaultValue!)?string}</#if>" propDefaultValue="${(property.defaultValue!)?string}" defaultValueHasChanged="${(cell.element.defaultValueHasChanged!false)?string('true','false')}" partDepend="${(cell.element.partDepend)!'common'}" columnType="${(property.type)!''}" name="${(cell.element.name)!}" namekey="${(cell.element.namekey)!}" key="${(cell.element.key)!}" mnecode='${(property.isUsedMneCode!false)?string('true','false')}' iscrosscompany='${(cell.element.iscrosscompany!false)?string('true','false')}' isrefselect='${(cell.element.isrefselect!false)?string('true','false')}' propertyCode="${(cell.element.propertyCode)!}" layRec="${(cell.element.layRec)!}" nullable="<#if (cell.element.nullable)?? && (property.nullable)?? && (cell.element.nullable)?string("true","false") == 'true' && (property.nullable)?string("true","false") == 'true'>true<#else>false</#if>" isreadonly="${(cell.element.readonly)?string("true","false")}" exp="${(cell.element.exp)!}" entityCode="${(property.model.entity.code)!}" layRec="${(cell.element.layRec)!}" assTar="${(cell.element.assTar)!}" assOrg="${(cell.element.assOrg)!}" multable="${((property.multable)!false)?string('true','false')}" columnLong="${(property.maxLength)!}" readonly="${(cell.element.readonly)?string("true","false")}" checkname="${ckname!}"  sourcepropertyname="${(cell.sourcepropertyname)!}" callbackbody="${((cell.element.callbackbody)!)?html}" callbackname="${((cell.element.callbackname)!)?html}" funcname="${((cell.funcname)!)?html}" funcbody="${((cell.funcbody)!)?html}" selectCompType="${(cell.element.selectCompType)!}" cssstyle="${((cell.element.cssstyle)!"")?html}" referenceview="${(cell.element.referenceview)!}" modelcode="${(property.model.code)!}" columnType="${(property.type)!}" colspan="${(cell.element.colspan)!1}" rowspan="${(cell.element.rowspan)!1}"  
					propShowType="${(property.fieldType!)}" propShowFormat="${(property.format!)}" showTypeHasChanged="${(cell.element.showTypeHasChanged!false)?string('true','false')}" showFormatHasChanged="${(cell.element.showFormatHasChanged!false)?string('true','false')}" 
					precision="${cell.element.precision!}" propPrecision="${property.decimalNum!}" precisionHasChanged="${(cell.element.precisionHasChanged!false)?string('true','false')}"  
					assPropertyName="${cell.assPropertyName!}" asscolumnname="${cell.asscolumnname!}"
					showType="${showType!}" <#if (cell.element.refCondition)?has_content>refCondition="${(cell.element.refCondition)?html}"</#if> modelDataType="<#if property.model.dataType == 2>tree<#else>simple</#if>" containLower="${(cell.element.containLower!false)?string('true','false')}" showFormat="${showFormat!}" fill='${(property.fillcontent)!}' caseSensitive="${(cell.element.caseSensitive!false)?string('true','false')}" crosscol="${(cell.element.crosscol!false)?string('true','false')}">
				<#else>
					<tr <#if cell_index % 2 == 0>class="even-num" numType="even"<#else>class="odd-num" numType="odd"</#if> cellCode="${cellCode!}" partDepend="${(cell.element.partDepend)!'common'}" selfType="${(cell.element.selfType)!(cell.element.columnType)!''}" key="${(cell.element.name)!}" ondblclick="ec.selectRow('fast',this);ec.fastFieldProperty();" onmousedown="ec.selectRow('fast',this);" defaultValue="${(cell.element.defaultValue!)?string}" propDefaultValue="${(cell.element.defaultValue!)?string}" defaultValueHasChanged="${(cell.element.defaultValueHasChanged!false)?string('true','false')}" partDepend="${(cell.element.partDepend)!'common'}" columnType="${(property.type)!''}" name="${(cell.element.name)!}" namekey="${(cell.element.namekey)!}" key="${(cell.element.key)!}" mnecode='${(cell.element.mnecode!false)?string('true','false')}' iscrosscompany='${(cell.element.iscrosscompany!false)?string('true','false')}' isrefselect='${(cell.element.isrefselect!false)?string('true','false')}' propertyCode="${(cell.element.propertyCode)!}" layRec="${(cell.element.layRec)!}" nullable="${(cell.element.nullable)?string("true","false")}" isreadonly="${(cell.element.readonly)?string("true","false")}" exp="${(cell.element.exp)!}" entityCode="${(cell.element.entityCode)!}" layRec="${(cell.element.layRec)!}" assTar="${(cell.element.assTar)!}" assOrg="${(cell.element.assOrg)!}" multable="${((cell.element.multable)!false)?string('true','false')}" columnLong="${(cell.element.columnLong)!}" readonly="${(cell.element.readonly)?string("true","false")}" checkname="${ckname!}" showType="${(cell.element.showType)!}"   sourcepropertyname="${(cell.sourcepropertyname)!}" callbackbody="${((cell.element.callbackbody)!)?html}" callbackname="${((cell.element.callbackname)!)?html}" funcname="${((cell.funcname)!)?html}" funcbody="${((cell.funcbody)!)?html}" selectCompType="${(cell.element.selectCompType)!}" cssstyle="${((cell.element.cssstyle)!"")?html}" referenceview="${(cell.element.referenceview)!}" modelcode="${(cell.element.modelcode)!}" columnType="${(property.type)!}" colspan="${(cell.element.colspan)!1}" rowspan="${(cell.element.rowspan)!1}" 
					showFormat="${(cell.element.showFormat)!}" assPropertyName="${cell.assPropertyName!}" asscolumnname="${cell.asscolumnname!}" <#if (cell.element.refCondition)?has_content>refCondition="${(cell.element.refCondition)?html}"</#if> modelDataType="simple" containLower="false" caseSensitive="${(cell.element.caseSensitive!false)?string('true','false')}" crosscol="${(cell.element.crosscol!false)?string('true','false')}" fill='{<#if (cell.element.fill)?has_content><#list (cell.element.fill)?keys as fe><#if fe_index &gt; 0>,</#if>"${fe}":<#if (cell.element.fill.fillType)?has_content && (cell.element.fill.fillType?string) == '4' && fe == 'fillContent'>{<#if cell.element.fill.fillOrder?has_content><#list cell.element.fill.fillOrder?split(",") as ne><#if ne_index &gt; 0>,</#if>"${ne}":"${((cell.element.fill.fillContent)[ne])?html}"</#list><#else><#list (cell.element.fill.fillContent)?keys as ne><#if ne_index &gt; 0>,</#if>"${ne}":"${((cell.element.fill.fillContent)[ne])?html}"</#list></#if>}<#else>"${((cell.element.fill)[fe])?html}"</#if></#list></#if>}'>
				</#if>
					<td align="center" style="width:90%;">${getHtmlText('${(cell.element.namekey)!cell.element.name}')}</td>
					<td onclick="ec.deleteFastQueryField(this)"><img title="${getText('ec.view.cell.fastdel')}" style="cursor:pointer;" src="/bap/static/ec/delete.gif" onMouseOver='deleteBtnChange(this)' onMouseOut='deleteBtnChange(this)'></img></td>
					</tr>
				</#list>
				</#if>
				</#if>
			</#list>
			</#if>
			</#if>
			</tbody>
		</table>
		</div>
		<div style="margin-left:10px;padding-top:20px;clear:both;">
		<label style="float:right;" id="fastquery-tips">${getHtmlText('ec.view.fastField.tips')}</label>
		</div>
	</div>
	<div id="fastContent" style="background-color:#f8f6f7;position:absolute;right:30px;width:105px;height:87%;">
		<div style="padding-top:80px!important;" class="ec-list-btndiv"><div class="ec-list-topbtn" onclick="ec.firstRow('fast')" id="fastFirstMove" ></div></div>
		<div class="ec-list-btndiv"><div class="ec-list-prevbtn" onclick="ec.upRow('fast')" id="fastUpMove" ></div></div>
		<div class="ec-list-btndiv"><div class="ec-list-nextbtn" onclick="ec.downRow('fast')" id="fastDownMove"></div></div>
		<div class="ec-list-btndiv"><div class="ec-list-lastbtn" onclick="ec.lastRow('fast')" id="fastLastMove"></div></div>
	</div>
</div>
<script type="text/javascript">
(function(){
	//注册命名空间
	CUI.ns("ec.fastQuery");
	/**
	 * 快速查询提交
	 * @method ec.fastQuery.Callback
	 */
	ec.fastQuery.editCallback = function(msg){
		if(msg && msg.code) {
			ec_fastQuery_edit_formDialogErrorBarWidget.show("${getHtmlText('ec.dataclassific.submitsuccessful')}","s");			
			$("#ec_fastQuery_edit_form input[name='fqj.version']").val(msg.version);
			setTimeout(function(){
				fastQuerySetting_dialog && fastQuerySetting_dialog.close();
			},1500);			
		}else{
			ec_fastQuery_edit_formDialogErrorBarWidget.show("${getHtmlText('ec.dataclassific.submitfailure')}");
        }
        CUI.Dialog.toggleAllButton(null,true,true);
	}
})();
</script>