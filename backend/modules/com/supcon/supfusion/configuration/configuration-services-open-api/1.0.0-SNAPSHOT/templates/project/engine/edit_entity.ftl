<@errorbar id="ec_entity_edit_formDialogErrorBar" />
<div id="ec_entity_edit_formDialogErrorBar"></div>
<script type="text/javascript">
	var ec_entity_edit_formDialogErrorBarWidget = null;
	YAHOO.util.Event.onDOMReady(function() {
		ec_entity_edit_formDialogErrorBarWidget = new CUI.ErrorBar('ec_entity_edit_formDialogErrorBar',{




		});
	});
</script>

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
					 	closeLoadPanel();
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
					var formId = 'ec_entity_edit_form';
					if(formId && formId.length > 5){
						var pageId = formId.substring(0, formId.length - 5);
						var datagrids = $('body').data(pageId + '_datagrids');
					}
					if(msg && msg.success == false){
	        			var errorMsgs = "";
						CUI.each(msg.items,function(index,item){
							if(index.indexOf('.id') != -1 && index.indexOf('.id')+3 == index.length) {
								$("#ec_entity_edit_form *[name^='"+(index.substring(0,index.length -3))+"'][type!='hidden']").each(function(){
									if($(this).parents('td[nullable=false]').length > 0) {
										showErrorField($(this));
									}
								});
							} else if($("#ec_entity_edit_form input[name='"+index+"AddIds'][type='hidden']").length > 0) {
								// 多选控件验证
								showErrorField($("#ec_entity_edit_form *[name='"+index+"AddIds']").parent('div'));
							} else {
								var field = CUI("#ec_entity_edit_form *[name='"+index+"']");
								if(field.prop('type') && field.prop('type').toUpperCase() == 'HIDDEN' && field.next() && field.next().length > 0) {
			                		showErrorField(field.next());
			                	} else {
			                		showErrorField(field);
			                	}
							}
							CUI("#ec_entity_edit_form *[name='"+index+"']").first().focus();
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
						oErrorWidget = ec_entity_edit_formDialogErrorBarWidget;
						oErrorWidget.showMessage(errorMsgs);
						// oErrorWidget.show(errorMsgs);
						if(CUI.Dialog){
					    	 CUI.Dialog.toggleAllButton('ec_entity_edit_form', true);
					     }
					}else{
						window.onbeforeunload = null;
						if(window.containerLoadPanelWidget) {
							//setTimeout(function(){closeLoadPanel();}, 2800);
						}
						runtime.entity.addCallback(msg,CUI('#'+formId).serialize());
					}
				}
			}
		);
	}

	function submitBapForm(){//电子签名成功之后出现进度条并提交表单
		var ecFormFlag = false;
		var retrialFormFlag = false;
		if(ecFormFlag && ( $('#ec_entity_edit_form').parents('.ewc-dialog-blove').length > 0 || ( window != top && !window.frameElement.getAttribute( 'v3_tab_iframe' ) ) ) ) {
			// dialog不出进度条
			ecFormFlag = false;
		}
		ecFormFlag = (ecFormFlag || retrialFormFlag);

		//前台验证通过之后出进度条
		CUI.Dialog.toggleAllButton('ec_entity_edit_form',true,ecFormFlag, true);
	// TODO 延时600ms，让页面上已触发 未执行完成的事件处理完成，临时性解决办法
	setTimeout(function(){

			// 延迟保存数据, 解决onchange事件无法触发问题
			var formId = 'ec_entity_edit_form';
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
							$("#ec_entity_edit_form *[name^='"+(index.substring(0,index.length -3))+"'][type!='hidden']").each(function(){
								if($(this).parents('td[nullable=false]').length > 0) {
									showErrorField($(this));
								}
							});
						} else {
							var field = CUI("#ec_entity_edit_form *[name='"+index+"']");
							if(field.prop('type') && field.prop('type').toUpperCase() == 'HIDDEN' && field.next() && field.next().length > 0) {
		                		showErrorField(field.next());
		                	} else {
		                		showErrorField(field);
		                	}
						}
						CUI("#ec_entity_edit_form *[name='"+index+"']").first().focus();
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
					oErrorWidget = ec_entity_edit_formDialogErrorBarWidget;
					if ( $(oErrorWidget.errorInfoBar).closest('.ewc-dialog-el').length>0 ) {
						oErrorWidget.show(errorMsgs);// 对话框内错误提示显示顶部横条
					}	else {
						oErrorWidget.showMessage(errorMsgs);// 编辑界面错误提示显示右上
					}
					if(CUI.Dialog){
						CUI.Dialog.toggleAllButton('ec_entity_edit_form', true);
				    }
				},
				success : function(msg){
					window.onbeforeunload = null;
					if(window.containerLoadPanelWidget) {
						setTimeout(function(){closeLoadPanel();}, 500);
					}
					runtime.entity.addCallback(msg,postData);
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
	CUI('#ec_entity_edit_form').unbind('submit.bapForm').bind('submit.bapForm',function(){
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
		//if(CUI.Dialog) CUI.Dialog.toggleAllButton('ec_entity_edit_form',true,ecFormFlag);
		//});
			//if(!validateForm_ec_entity_edit_form())	return false;
		$('#ec_entity_edit_form').trigger('beforeSubmit');
		if($('#ec_entity_edit_form input[name="operateType"]').val() == "submit"){
			var deploymentId=$('#ec_entity_edit_form input[name="deploymentId"]');
			var buttonCode=$('#ec_entity_edit_form input[name="buttonCode"]');
			var namespace=$('#ec_entity_edit_form input[name="namespace"]');
			if( deploymentId.length > 0 && deploymentId.val() != undefined && deploymentId.val() != ''){
				var signatureInfo=signatureUtil.getSignatureInfo(true,'',deploymentId.val(),'','')//判断是否需要进行电子签名
				if(signatureInfo[0] != '') {
					var cancelItem = $('input[name="workFlowVarStatus"]');
					if(cancelItem.val() != "cancel") {
						signatureUtil.showSignatureDialog(signatureInfo[1],signatureInfo[0],'ec.flow.submit','','ec_entity_edit_form');
						$('a[name="signature_signatureDialog_showDialog_0"]').unbind('click').bind('click',function(){singleSignOk('','ec_entity_edit_form','ec.flow.submit',false)});
					}
					else {
						signatureUtil.showSignatureDialog(signatureInfo[1],signatureInfo[0],'ec.edit.remove','','ec_entity_edit_form');
						$('a[name="signature_signatureDialog_showDialog_0"]').unbind('click').bind('click',function(){singleSignOk('','ec_entity_edit_form','ec.edit.remove',false)});
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
						parent.signatureUtil.showSignatureDialog(signatureInfo[1],signatureInfo[0],buttonCode.val(),namespace.val(),"ec_entity_edit_form",false,'');
						if(signatureInfo[0] == 'singleSign') {
							parent.$('a[name="signature_signatureDialog_showDialog_0"]').unbind('click').bind('click',function(){singleSignOk(namespace.val(),'ec_entity_edit_form',buttonCode.val(),false)});
						}
						else {
							setTimeout(function(){parent.$('#secondSignerOK').unbind('click').bind('click',function(){doubleSignOK(this,namespace.val(),'ec_entity_edit_form',buttonCode.val(),false)});},2000);
						}
					}
					else {
						signatureUtil.showSignatureDialog(signatureInfo[1],signatureInfo[0],buttonCode.val(),'',"ec_entity_edit_form",false,'');
						if(signatureInfo[0] == 'singleSign') {
							$('a[name="signature_signatureDialog_showDialog_0"]').unbind('click').bind('click',function(){singleSignOk('','ec_entity_edit_form',buttonCode.val(),false)});
						}
						else {
							setTimeout(function(){$('#secondSignerOK').unbind('click').bind('click',function(){doubleSignOK(this,'','ec_entity_edit_form',buttonCode.val(),false)});},2000);
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
<form id="ec_entity_edit_form" action="/msService/ec/engine/saveEntity" validate="true" callback="runtime.entity.addCallback" onreset="clearErrorMessages(this);clearErrorLabels(this);">
	<input type="hidden" name="entity.version" <#if entity?? && entity.version??>value="${entity.version}" <#else>value="0" </#if> id="ec_entity_edit_form_entity_version"/>
    <input type="hidden" name="entity.code" <#if entity?? && entity.code??>value="${entity.code}" </#if> id="ec_entity_edit_form_entity_code"/>
    <input type="hidden" name="entity.module.code" <#if entity?? && entity.module.code??>value="${entity.module.code}" </#if> id="ec_entity_edit_form_entity_module_code" />
	<input type="hidden" name="entity.isControl" id="isControl" <#if entity?exists&&entity.isControl?exists&&entity.isControl== true>value="true"<#else>value="false"</#if>/>
	<input type="hidden" name="entity.payCloseAttention" id="payCloseAttention" <#if entity?exists&&entity.payCloseAttention?exists&&entity.payCloseAttention== true>value="true"<#else>value="false"</#if>/>
	<input type="hidden" name="entity.crossCompanyFlag" id="crossCompanyFlag" <#if entity?exists&&entity.crossCompanyFlag?exists&&entity.crossCompanyFlag== true>value="true"<#else>value="false"</#if>/>
	<input type="hidden" name="entity.mobile" id="mobile" <#if entity?exists&&entity.mobile?exists&&entity.mobile== true>value="true"<#else>value="false"</#if>/>
	<input type="hidden" name="entity.enableFieldsPermissionConf" id="enableFieldsPermissionConf" <#if entity?exists&&entity.enableFieldsPermissionConf?exists&&entity.enableFieldsPermissionConf== true>value="true"<#else>value="false"</#if>/>
	<table class="infoTable" cellpadding="0" cellspacing="0" border="0" align="center" width="90%" style="margin-top: 5px">
		<tr>
			<td class="la" style="width: 20%;padding-right: 3px;" align="right">${getHtmlText('ec.entity.entityCode')}</td>
			<td class="co" colspan="3">
			<#if isView?? && isView>
            		<input type="text" name="entity.entityName" maxlength="28" <#if entity?? && entity.entityName??>value="${entity.entityName}" readonly="true"</#if> id="ec_entity_edit_form_entity_entityName" class="cui-edit-field"/>
            <#else>
            	<#if entity?? && (entity.code)??>
            		<input type="text" name="entity.entityName" maxlength="28" <#if entity?? && entity.entityName??>value="${entity.entityName}" readonly="true"</#if> id="ec_entity_edit_form_entity_entityName" class="cui-edit-field"/>
            	<#else>
            		<input type="text" name="entity.entityName" maxlength="28" value="" id="ec_entity_edit_form_entity_entityName" class="cui-edit-field"/>
            	</#if>
            </#if>
			</td>
		</tr>
		<tr>
			<td class="la" style="width: 20%;padding-right: 3px;" align="right">${getHtmlText('ec.entity.prefix')}</td>
			<td class="co" colspan="3">
				<#if isView?? && isView>
                	<input type="text" name="entity.prefix" <#if entity?? && entity.prefix??>value="${entity.prefix}" </#if> id="ec_entity_edit_form_entity_prefix" class="cui-edit-field"/>
                <#else>
                	<input type="text" name="entity.prefix" <#if entity?? && entity.prefix??>value="${entity.prefix}"  </#if> id="ec_entity_edit_form_entity_prefix" class="cui-edit-field"/>
                </#if>
				<div><span class="description">(${getHtmlText('ec.entityEdit.entityPrefix')})</span></div>
			</td>
		</tr>
		<tr>
			<td class="la"  style="width: 20%;padding-right: 3px;" align="right">${getHtmlText('ec.entity.name')}</td>
			<td class="co" width="30%">
            	<input type="text" name="entity.name" disabled="disabled" id="ec_entity_edit_form_entity_name" cssClass="cui-readonly-field" style="display:none;">
            	<#if isView?? && isView>
            		<@international name="entity.name" moduleCode="${(module.artifact)!}" key="${entity.name!}" cssClass="cui-readonly-field"  view=true></@international>
            	<#else>
            		<#if !entity?exists>
            			<@international name="entity.name" key=(entity.name)!'' moduleCode=module.artifact isNew=true maxLength=80></@international>
            		<#else>
            			<#if (responseMap.isRead)>
            				<@international name="entity.name" moduleCode="${(module.artifact)!}" key="${entity.name!}" view=true cssClass="cui-readonly-field"></@international>
            			<#else>
            				<@international name="entity.name" key=(entity.name)! moduleCode=module.artifact isNew=true maxLength=80 cssClass="cui-readonly-field"></@international>
            			</#if>
            		</#if>
            	</#if>
            </td>
			<td class="la"  style="width: 20%;padding-right: 3px;" align="right">${getHtmlText('ec.entity.type')}</td>
		    <td class="co">
		    	<#if entity?? && entity.code??>
		    		<input type="hidden" id="entity_isBase" name="entity.isBase" value="${(entity.isBase!false)?string('true','false')}"/>
		    		<input type="text" name="entity_isBase" width="100%" class="cui-readonly-field" readonly="readonly" value="<#if entity.isBase==false>${getText('ec.entity.formtype')}<#else>${getText('ec.entity.isBase')}</#if>" />
		    	<#else>
			    	<select id="entity_isBase" name="entity.isBase" width="100%" onchange="runtime.entity.isBaseCheckChoose(this)">
			    		<option value="true" >${getText('ec.entity.isBase')}</option>
			    		<option value="false" <#if entity?? && entity.isBase==false>selected="selected"</#if>>${getText('ec.entity.formtype')}</option>
			    	</select>
		    	</#if>
		    </td>
		</tr>
		<#-- 增加实体类别：BAP、S2 -->
		<tr>
			<td class="la"  style="width: 20%;padding-right: 3px;" align="right">${getHtmlText('ec.entity.entityType')}</td>
			<td>
				<#if bapS2integration!false>
				<div class="fix-input">
					<@systemcode name="entity.entityType.id" code="SYSTEM" value="${(entity.entityType.id)!'SYSTEM/BAP'}"></@systemcode>
				</div>
				<#else>
				<div class="fix-input-readonly">
					<input type="text" id="entity_entityType" readOnly="true" cssClass="cui-noborder-input" value="${getText((entity.entityType.value)!'BAP')}" />
					<input type="hidden" name="entity.entityType.id" value="${(entity.entityType.id)!'SYSTEM/BAP'}" />
				</div>
				</#if>
			</td>
			<!-- 默认提供日志功能
			<td class="la"  style="width: 20%;padding-right: 3px;" align="right">${getHtmlText('ec.entity.enableAudit')}</td>
			<td class="co">
				<input type="hidden" name="entity.enableAudit" value="<#if (entity.enableAudit)?? && entity.enableAudit>true<#else>false</#if>" />
				<input type="checkbox" onclick="runtime.entity.setAudit()" id="entityAudit" <#if (entity.enableAudit)?? && entity.enableAudit>checked="checked"</#if>/>
			</td> -->
		</tr>
		<tr class="tr-workflow" style="*height:20px;">
			<td class="la"  style="width: 20%;padding-right: 3px;" align="right">${getHtmlText('ec.entity.groupEnabled')}</td>
			<td class="co"><input type="checkbox" name="entity.groupEnabled" <#if entity?exists&&entity.groupEnabled== true>checked="checked"</#if> value="true" <#if (isView)?? && (isView)>disabled="true"</#if> /></td>
			<td class="la" style="width: 20%;padding-right: 3px;" align="right" name="td_workflowEnabled">${getHtmlText('ec.entity.workflowEnabled')}</td>
			<td class="co" name="td_workflowEnabled">
				<input type="hidden" id="entity_workflowEnabled" name="entity.workflowEnabled" <#if entity?exists&&entity.workflowEnabled== true>value="true"</#if>/>
				<input type="checkbox" id="entity_workflowEnabled_ck" <#if entity?exists&&entity.workflowEnabled== true>checked="checked"</#if> onclick="javascript:$('#entity_workflowEnabled').val(this.checked);" <#if (isView)?? && (isView)>disabled="true"</#if>/>
			</td>
		</tr>
		<tr style="*height:20px;">
			<td class="la td-workflow" style="width: 20%;padding-right: 3px;" align="right">${getHtmlText('ec.entity.payCloseAttention')}</td>
			<td class="co td-workflow"><input type="checkbox" onclick="runtime.entity.changePayCloseAttention(this)" id="payCloseAttention" <#if entity?exists&&entity.payCloseAttention?exists&&entity.payCloseAttention== true>checked="checked"</#if> value="true" <#if (isView)?? && (isView)>disabled="true"</#if>/></td>

			<td class="la"  style="width: 20%;padding-right: 3px;" align="right">${getHtmlText('ec.entity.crossCompanyFlag')}</td>
			<td class="co"><input type="checkbox" onclick="runtime.entity.changeCrossCompanyFlag(this)" id="entityCrossCompanyFlag" <#if entity?exists&&entity.crossCompanyFlag?exists&&entity.crossCompanyFlag== true>checked="checked"</#if> value="true" /></td>
		</tr>
		<tr style="*height:20px;">
			<td class="la"  style="width: 20%;padding-right: 3px;" align="right">${getHtmlText('ec.entity.mobile')}</td>
			<td class="co"><input type="checkbox" onclick="runtime.entity.changeMobile(this)" id="entityMobile" <#if entity?exists&&entity.mobile?exists&&entity.mobile== true>checked="checked"</#if> value="true" <#if (isView)?? && (isView)>disabled="true"</#if> /></td>
			<td class="la" style="width:20%;padding-right:3px;text-align:right">${getHtmlText('ec.entity.enableAclRestrict')}</td>
			<td class="co">
				<input type="hidden" name="entity.enableAclRestrict" value="<#if (entity.enableAclRestrict)?? && entity.enableAclRestrict>true<#else>false</#if>" />
				<input type="checkbox" onclick="runtime.entity.setAclRestrict()" id="entityAclRestrict" <#if (entity.enableAclRestrict)?? && entity.enableAclRestrict>checked="checked"</#if> <#if (isView)?? && (isView)>disabled="true"</#if>/>
			</td>
		</tr>
		<tr	style="*height:20px;">
 			<td class="la " style="width: 20%;padding-right: 3px;" align="right">${getHtmlText('ec.entity.enableFieldsPermissionConf')}</td>
			<td class="co"><input type="checkbox" onclick="runtime.entity.changeEnableFieldsPermissionConf(this)" id="changeEnableFieldsPermissionConf" <#if entity?exists&&entity.enableFieldsPermissionConf?exists&&entity.enableFieldsPermissionConf== true>checked="checked"</#if> value="true" <#if (isView)?? && (isView)>disabled="true"</#if> /></td>
 		</tr>
		<tr>
			<td class="la" style="width: 20%;padding-right: 3px;" align="right">${getHtmlText('ec.entity.description')}</td>
			<td class="co" colspan="3">
			<#if (isView)?? && (isView)>
				<textarea name="entity.description"  readonly="true" cols="" rows="" id="ec_entity_edit_form_entity_description" class="cui-edit-textarea" style="width:95%;height:80px">${entity.description!}</textarea>
			<#else>
				<textarea name="entity.description" cols="" rows=""  id="ec_entity_edit_form_entity_description" class="cui-edit-textarea" style="width:95%;height:80px"><#if entity?? && entity.description??>${entity.description!} </#if></textarea>
			</#if>
			</td>
		</tr>
	</table>
</form>
<script type="text/javascript">
(function(){  //onload事件，自动调用
	var form = CUI('#ec_entity_edit_form');
})();

    function validateForm_ec_entity_edit_form() {
        form = document.getElementById("ec_entity_edit_form");
        // 内容前后去空格
    	$('select,input[type="text"],textarea', '#ec_entity_edit_form').each(function(){
    		this.value = $.trim(this.value);
    	});
        clearErrorMessages(form);
        clearErrorLabels(form);

        var errors = false;
        var field = null;
        var continueValidation = true;
        var errorFields = new Array();
        var errorMessages = new Array();
        var validateFields = {};
		var oErrorWidget = null;
			oErrorWidget = ec_entity_edit_formDialogErrorBarWidget;
		if(errors){
			addError(form,oErrorWidget,errorFields,errorMessages);
		}

        //return !errors;
        return false;
    }
</script>
<script type="text/javascript">
(function(){
	//注册命名空间
	CUI.ns("runtime.entity");
	runtime.entity.changeEntityIsControl=function(obj){
		if(CUI(obj).prop('checked')) {
			CUI("#isControl").val('true');
		}else{
			CUI("#isControl").val('false');
		}
	}
	runtime.entity.changePayCloseAttention=function(obj){
		if(CUI(obj).prop('checked')) {
			CUI("#payCloseAttention").val('true');
		}else{
			CUI("#payCloseAttention").val('false');
		}
	}
	runtime.entity.changeCrossCompanyFlag=function(obj){
		if(CUI(obj).prop('checked')) {
			CUI("#crossCompanyFlag").val('true');
		}else{
			CUI("#crossCompanyFlag").val('false');
		}
	}
	runtime.entity.changeEnableFieldsPermissionConf=function(obj){
		if(CUI(obj).prop('checked')) {
			CUI("#enableFieldsPermissionConf").val('true');
		}else{
 			CUI("#enableFieldsPermissionConf").val('false');
		}
	}
	runtime.entity.changeMobile=function(obj){
		if(CUI(obj).prop('checked')) {
			CUI("#mobile").val('true');
		}else{
			CUI("#mobile").val('false');
		}
	}
	 /* 编码规则验证
	 * @method ec_module_code_validate
	 * @public
	 */
	runtime.entity.codeValidate=function(){
	 	var validate=/^[a-z]{1}[a-zA-Z_0-9]+$/;
	 	var obj = $("#ec_entity_edit_form input[name='entity.entityName']").val();
	 	$("#ec_entity_edit_form input[name='entity.entityName']").val(obj.trim());
		if (validate.test($("#ec_entity_edit_form input[name='entity.entityName']").val())){
			if($('input[name="entity.entityName"]').val()!='' && $('input[name="entity.prefix"]').val() == '') {
				$('input[name="entity.prefix"]').val($('input[name="entity.entityName"]').val());
			}
			return true;
		}else{
		    ec_entity_edit_formDialogErrorBarWidget.show("${getHtmlText('ec.entity.formatmessage')}");
			return false;
		}
	}

	runtime.entity.isBaseCheckChoose=function(obj){
		if(obj.value==='false'){
			$(".tr-workflow").show();
			$(".td-workflow").show();
			$("#ec_entity_edit_form #entity_workflowEnabled").val(true);
			$("#ec_entity_edit_form #entity_workflowEnabled_ck").prop('checked', true);
			$("#ec_entity_edit_form #entity_workflowEnabled_ck").prop('disabled', true);
		}else{
			$("#ec_entity_edit_form #entity_workflowEnabled").val(false);
			$("#ec_entity_edit_form #entity_workflowEnabled_ck").prop('checked', false);
			$("#ec_entity_edit_form #entity_workflowEnabled_ck").prop('disabled', false);
			$("#ec_entity_edit_form #payCloseAttention").prop('checked', false);
			$("#ec_entity_edit_form input[name='entity.groupEnabled']").prop('checked', false);
			$(".tr-workflow").hide();
			$(".td-workflow").hide();
		}
	}
	runtime.entity.prefixShowOrHide=function(){
		if($('input[name="entity.isBase"]').prop('checked') || $('input[name="entity.isInherentedBase"]').prop('checked')) {
			$('*[name="entity.prefix"]').parents('tr').first().hide();
		} else {
			$('*[name="entity.prefix"]').parents('tr').first().show();
		}
	}
	runtime.entity.isInherentedBaseCheckChoose=function(obj){
		if(CUI(obj).prop('checked')) {
			$("#ec_entity_edit_form input[name='entity.workflowEnabled']").prop('disabled', true);
			$("#ec_entity_edit_form input[name='entity.isBase']").prop('disabled', true);
			$("#ec_entity_edit_form input[name='entity.isBase']").prop('checked', true);
			$("#ec_entity_edit_form input[name='entity.groupEnabled']").prop('disabled', true);
		}else{
			$("#ec_entity_edit_form input[name='entity.workflowEnabled']").prop('disabled', false);
			$("#ec_entity_edit_form input[name='entity.isBase']").prop('disabled', false);
			$("#ec_entity_edit_form input[name='entity.groupEnabled']").prop('disabled', false);
		}
	}
	runtime.entity.workflowCheckChoose=function(obj){
		if(CUI(obj).prop('checked')) {
			$("#ec_entity_edit_form input[name='entity.isBase']").prop('disabled', true);
			$("#ec_entity_edit_form input[name='entity.isInherentedBase']").prop('disabled', true);
		}else{
			$("#ec_entity_edit_form input[name='entity.isBase']").prop('disabled', false);
			$("#ec_entity_edit_form input[name='entity.isInherentedBase']").prop('disabled', false);
		}
	}

	runtime.entity.load = function() {
		<#if (responseMap.isRead)>
			CUI('input,textarea', CUI('#ec_entity_edit_form')).prop("readonly", true);
			CUI('input,textarea', CUI('#ec_entity_edit_form')).addClass('cui-readonly-field');
			CUI('input[type="checkbox"]', CUI('#ec_entity_edit_form')).prop("disabled", true);

		</#if>


	}

	runtime.entity.setAclRestrict = function(){
		if($('#entityAclRestrict').prop('checked')){
			$('input[name="entity.enableAclRestrict"]').val('true');
		}else{
			$('input[name="entity.enableAclRestrict"]').val('false');
		}
	}

	runtime.entity.setAudit = function(){
		if($('#entityAudit').prop('checked')){
			$('input[name="entity.enableAudit"]').val('true');
		}else{
			$('input[name="entity.enableAudit"]').val('false');
		}
	}

	runtime.entity.load();
	runtime.entity.prefixShowOrHide();
	runtime.entity.isBaseCheckChoose($('#entity_isBase')[0]);
	$('input[name="entity.isBase"],input[name="entity.isInherentedBase"],input[name="entity.workflowEnabled"]').click(function(){
		runtime.entity.prefixShowOrHide();
	});
	$('input[name="entity.entityName"]').blur(function(){
		if($('input[name="entity.entityName"]').val()!='' && $('input[name="entity.prefix"]').val() == '') {
			$('input[name="entity.prefix"]').val($('input[name="entity.entityName"]').val());
		}
	});
	<#if (isView)?? && (isView)>
	//$('input,textarea').prop('disabled', true);
	</#if>
})();
</script>