<style type="text/css">
#ec_group_edit_form .infoTable tr{height:23px;}
#ec_group_edit_form .infoTable tr td {padding-bottom:0px;height:23px;}
#ec_data_manage {background-color:#f3f3f3;}
</style>
<@errorbar id="ec_group_edit_formDialogErrorBar" />
<@frameset id="ec_data_manage">
<input type="hidden" name="layoutName" value="" id="layoutName">
<#if targetModel??>
<input type="hidden"  name="targetModel.code" value="" />
</#if>
	<@frame id="left_in" region="west" width=200 offsetH=6 resize=true style="overflow:auto">
	<#if layoutName??>
		<#assign layoutName = "&layoutName=" + layoutName >
	<#else>
		<#assign layoutName = "" >
	</#if>
	<#if targetModel??>
		<#assign targetModelCode = "&targetModel.code=" + targetModel.code >
	<#else>
		<#assign targetModelCode = "" >
	</#if>
	<@tree id="ec_dataGroup_Tree" nameCol="displayNameInternational" dataUrl="/msService/ec/view/data_grouplist?view.code=${view.code}${targetModelCode}${layoutName}${isProj?string('&isProj=true','')}" rootName="${getText('ec.view.datagroup')}"
		 callback="{onClick:function(event,treeId,node){ec.datagroup.showClassificInfo(node);CUI.setCookie('nodeCookie_' + treeId,node.layRec,'d1');}}" />
	</@frame>
	<#if !isReadOnlyMode>
	<div style="position:absolute;bottom:0px;z-index:55">
		<div id="opratebar" class="opratebar">
			<a id="ec_datagroup_btn_add" class="cui-btn mr10 cui-btn-add" href="#" onclick='ec.datagroup.add()'>${getHtmlText('ec.view.adddatagroup')}</a>
			<a id="ec_datagroup_btn_del" class="cui-btn mr10 cui-btn-del" href="#" onclick='ec.datagroup.del();'>${getHtmlText('foundation.role.delete')}</a>
		</div>
	</div>
	</#if>
	<@frame id="ec_project_dataclassific_container_main" region="center" offsetH=4>
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
        					var formId = 'ec_group_edit_form';
        					if(formId && formId.length > 5){
        						var pageId = formId.substring(0, formId.length - 5);
        						var datagrids = $('body').data(pageId + '_datagrids');
        					}
        					if(msg && msg.success == false){
        	        			var errorMsgs = "";
        						CUI.each(msg.items,function(index,item){
        							if(index.indexOf('.id') != -1 && index.indexOf('.id')+3 == index.length) {
        								$("#ec_group_edit_form *[name^='"+(index.substring(0,index.length -3))+"'][type!='hidden']").each(function(){
        									if($(this).parents('td[nullable=false]').length > 0) {
        										showErrorField($(this));
        									}
        								});
        							} else if($("#ec_group_edit_form input[name='"+index+"AddIds'][type='hidden']").length > 0) {
        								// 多选控件验证
        								showErrorField($("#ec_group_edit_form *[name='"+index+"AddIds']").parent('div'));
        							} else {
        								var field = CUI("#ec_group_edit_form *[name='"+index+"']");
        								if(field.prop('type') && field.prop('type').toUpperCase() == 'HIDDEN' && field.next() && field.next().length > 0) {
        			                		showErrorField(field.next());
        			                	} else {
        			                		showErrorField(field);
        			                	}
        							}
        							CUI("#ec_group_edit_form *[name='"+index+"']").first().focus();
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
        						oErrorWidget = ec_group_edit_formDialogErrorBarWidget;
        						oErrorWidget.showMessage(errorMsgs);
        						// oErrorWidget.show(errorMsgs);
        						if(CUI.Dialog){
        					    	 CUI.Dialog.toggleAllButton('ec_group_edit_form', true);
        					     }
        					}else{
        						window.onbeforeunload = null;
        						if(window.containerLoadPanelWidget) {
        							//setTimeout(function(){closeLoadPanel();}, 2800);
        						}
        						ec.datagroup.editCallback(msg,CUI('#'+formId).serialize());
        					}
        				}
        			}
        		);
        	}

        	function submitBapForm(){//电子签名成功之后出现进度条并提交表单
        		var ecFormFlag = false;
        		var retrialFormFlag = false;
        		if(ecFormFlag && ( $('#ec_group_edit_form').parents('.ewc-dialog-blove').length > 0 || ( window != top && !window.frameElement.getAttribute( 'v3_tab_iframe' ) ) ) ) {
        			// dialog不出进度条
        			ecFormFlag = false;
        		}
        		ecFormFlag = (ecFormFlag || retrialFormFlag);

        		//前台验证通过之后出进度条
        		CUI.Dialog.toggleAllButton('ec_group_edit_form',true,ecFormFlag, true);
        	// TODO 延时600ms，让页面上已触发 未执行完成的事件处理完成，临时性解决办法
        	setTimeout(function(){

        			// 延迟保存数据, 解决onchange事件无法触发问题
        			var formId = 'ec_group_edit_form';
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
        							$("#ec_group_edit_form *[name^='"+(index.substring(0,index.length -3))+"'][type!='hidden']").each(function(){
        								if($(this).parents('td[nullable=false]').length > 0) {
        									showErrorField($(this));
        								}
        							});
        						} else {
        							var field = CUI("#ec_group_edit_form *[name='"+index+"']");
        							if(field.prop('type') && field.prop('type').toUpperCase() == 'HIDDEN' && field.next() && field.next().length > 0) {
        		                		showErrorField(field.next());
        		                	} else {
        		                		showErrorField(field);
        		                	}
        						}
        						CUI("#ec_group_edit_form *[name='"+index+"']").first().focus();
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
        					oErrorWidget = ec_group_edit_formDialogErrorBarWidget;
        					if ( $(oErrorWidget.errorInfoBar).closest('.ewc-dialog-el').length>0 ) {
        						oErrorWidget.show(errorMsgs);// 对话框内错误提示显示顶部横条
        					}	else {
        						oErrorWidget.showMessage(errorMsgs);// 编辑界面错误提示显示右上
        					}
        					if(CUI.Dialog){
        						CUI.Dialog.toggleAllButton('ec_group_edit_form', true);
        				    }
        				},
        				success : function(msg){
        					window.onbeforeunload = null;
        					if(window.containerLoadPanelWidget) {
        						setTimeout(function(){closeLoadPanel();}, 500);
        					}
        					ec.datagroup.editCallback(msg,postData);
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
        	CUI('#ec_group_edit_form').unbind('submit.bapForm').bind('submit.bapForm',function(){
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
        		//if(CUI.Dialog) CUI.Dialog.toggleAllButton('ec_group_edit_form',true,ecFormFlag);
        		//});
        			//if(!validateForm_ec_group_edit_form())	return false;
        		$('#ec_group_edit_form').trigger('beforeSubmit');
        		if($('#ec_group_edit_form input[name="operateType"]').val() == "submit"){
        			var deploymentId=$('#ec_group_edit_form input[name="deploymentId"]');
        			var buttonCode=$('#ec_group_edit_form input[name="buttonCode"]');
        			var namespace=$('#ec_group_edit_form input[name="namespace"]');
        			if( deploymentId.length > 0 && deploymentId.val() != undefined && deploymentId.val() != ''){
        				var signatureInfo=signatureUtil.getSignatureInfo(true,'',deploymentId.val(),'','')//判断是否需要进行电子签名
        				if(signatureInfo[0] != '') {
        					var cancelItem = $('input[name="workFlowVarStatus"]');
        					if(cancelItem.val() != "cancel") {
        						signatureUtil.showSignatureDialog(signatureInfo[1],signatureInfo[0],'ec.flow.submit','','ec_group_edit_form');
        						$('a[name="signature_signatureDialog_showDialog_0"]').unbind('click').bind('click',function(){singleSignOk('','ec_group_edit_form','ec.flow.submit',false)});
        					}
        					else {
        						signatureUtil.showSignatureDialog(signatureInfo[1],signatureInfo[0],'ec.edit.remove','','ec_group_edit_form');
        						$('a[name="signature_signatureDialog_showDialog_0"]').unbind('click').bind('click',function(){singleSignOk('','ec_group_edit_form','ec.edit.remove',false)});
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
        						parent.signatureUtil.showSignatureDialog(signatureInfo[1],signatureInfo[0],buttonCode.val(),namespace.val(),"ec_group_edit_form",false,'');
        						if(signatureInfo[0] == 'singleSign') {
        							parent.$('a[name="signature_signatureDialog_showDialog_0"]').unbind('click').bind('click',function(){singleSignOk(namespace.val(),'ec_group_edit_form',buttonCode.val(),false)});
        						}
        						else {
        							setTimeout(function(){parent.$('#secondSignerOK').unbind('click').bind('click',function(){doubleSignOK(this,namespace.val(),'ec_group_edit_form',buttonCode.val(),false)});},2000);
        						}
        					}
        					else {
        						signatureUtil.showSignatureDialog(signatureInfo[1],signatureInfo[0],buttonCode.val(),'',"ec_group_edit_form",false,'');
        						if(signatureInfo[0] == 'singleSign') {
        							$('a[name="signature_signatureDialog_showDialog_0"]').unbind('click').bind('click',function(){singleSignOk('','ec_group_edit_form',buttonCode.val(),false)});
        						}
        						else {
        							setTimeout(function(){$('#secondSignerOK').unbind('click').bind('click',function(){doubleSignOK(this,'','ec_group_edit_form',buttonCode.val(),false)});},2000);
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
		<form id="ec_group_edit_form" name="ec_group_edit_form" action="datagroupsave" namespace="/msService/ec/view" validate="true" callback="ec.datagroup.editCallback">
			<input type="hidden" <#if dataGroup?? && dataGroup.version??>value="${dataGroup.version}"</#if> name="dataGroup.version" id="ec_group_edit_form_dataGroup_version" />
			<input type="hidden" <#if dataGroup?? && dataGroup.code??>value="${dataGroup.code}"</#if>  name="dataGroup.code" id="ec_group_edit_form_dataGroup_code" />
			<input type="hidden" <#if dataGroup?? && dataGroup.view?? && dataGroup.view.code??>value="${dataGroup.view.code}"</#if>  name="dataGroup.view.code" id="ec_group_edit_form_dataGroup_view_code"  />
			<input type="hidden"  name="isProj" value="${isProj?string}"/>
			<table class="infoTable" id="ec_classific_edit_table" cellpadding="0" cellspacing="0" border="0" style="margin-top:8px;margin-left:15px;width:89%;">
				<tr>
					<td class="la" align="right" style="padding-right:2px;">${getHtmlText('ec.dataGroup.code')}</td>
					<td class="co">
						<input type="text"  name="dataGroup.name"  <#if dataGroup?? && dataGroup.name??>value="${dataGroup.name}"</#if> readonly="true" cssClass="cui-readonly-field" id="ec_group_edit_form_dataGroup_name" />
					</td>
					<td style="width:4%;"></td>
					<td class="la" align="right" style="padding-right:2px;">${getHtmlText('ec.dataGroup.name')}</td>
					<td class="co">
						<input type="hidden" id="dataGroupDisplayName"  name="dataGroup.displayName"  <#if dataGroup?? && dataGroup.displayName??>value="${dataGroup.displayName}"</#if> />
						<@international name="dataGroup.displayName.edit" isNew=true isOldEdit=true moduleCode="${view.assModel.entity.module.artifact!}" key="" cssClass="cui-edit-field"></@international>
					</td>
				</tr>
				<#if !(view??&&view.mobile)>
				<tr>
					<td class="la" align="right" style="padding-right:2px;">${getHtmlText('ec.dataGroup.isMultiple')} </td>
					<td class="co"><input style="margin-left:4px;" type="radio" name="dataGroup.isMultiple" value="false"/><label class="cui-label-font">${getHtmlText('ec.property.radio')}</label><input type="radio" style="margin-left: 6px;" name="dataGroup.isMultiple" value="true"/><label class="cui-label-font">${getHtmlText('ec.view.property.checkbox')}</label></td>
				</tr>
				</#if>
			</table>
		</form>
		</div>
		<@datatable dtPage="dataClassificList" caption="${getText('ec.view.dataclass')}" hidekey="['code','version']" id="ec_dataclassific_table" editable=false dataUrl="/msService/ec/view/data_classificlist${isProj?string('?isProj=true','')}" dblclick="ec.dataclassific.mod">
			<#if !isReadOnlyMode>
			<@operatebar operates="code:ec_dataclassific_btn_add||name:${getHtmlText('ec.view.dataclassadd')}||iconcls:add||useInMore:false||onclick:ec.dataclassific.add();
						code:ec_dataclassific_btn_mod||iconcls:edit||name:${getHtmlText('ec.view.dataclassedit')}||useInMore:false||onclick:ec.dataclassific.mod();
						code:ec_dataclassific_btn_del||iconcls:del||name:${getHtmlText('ec.view.dataclassdel')}||useInMore:false||onclick:ec.dataclassific.del()"
						operateType="noPower" resultType="json" />
			</#if>
			<@datacolumn key="name" label="${getHtmlText('ec.dataclassific.code')}" width=80 />
			<@datacolumn key="displayName" label="${getHtmlText('ec.dataclassific.name')}" width=100 />
			<@datacolumn key="condition" label="${getHtmlText('ec.dataclassific.condition')}" width=200 />
			<@datacolumn key="code" label="${getHtmlText('ec.view.identiy')}" width=150 />
		</@datatable>
	</@frame>
</@frameset>
<script type="text/javascript">
(function(){
	//注册命名空间
	CUI.ns("ec.datagroup");
	CUI.ns("ec.dataclassific");
	var _proj_config_flag = false;
	ec.datagroup.setMultiple = function(obj,formId){
		if($('input[name="isMultiple"]:checked').attr('type') == 'yesMultiple') {
			CUI('#'+formId + ' input[name="dataGroup.isMultiple"]').val('true');
		} else {
			CUI('#'+formId + ' input[name="dataGroup.isMultiple"]').val('false');
		}
	}
	
	/**
	 * 点击数据分组树节点，显示数据分类详细信息
	 * @method ec.datagroup.showClassificInfo
	 * @param {Node} oNode
	 */
	ec.datagroup.showClassificInfo = function(oNode){
		if(oNode.code != null && oNode.code != undefined) {
			$("#ec_group_edit_form input[name='dataGroup.code']").val(oNode.code);
			$("#ec_group_edit_form input[name='dataGroup.name']").val(oNode.name);
			$("#ec_group_edit_form input[name='dataGroup.view.code']").val(oNode.view.code);
			if(oNode.isMultiple) {
				$("#ec_group_edit_form input[name='dataGroup.isMultiple'][value='true']").prop('checked',true);
			} else {
				$("#ec_group_edit_form input[name='dataGroup.isMultiple'][value='false']").prop('checked',true);
			}
			$("#ec_group_edit_form input[name='dataGroup.displayName.edit']").val(oNode.displayName);
			$("#ec_group_edit_form input[name='international_dataGroupdisplayNameedit_showName']").val(getLocalMessage(oNode.displayName));
			
			$("#ec_group_edit_form input[name='dataGroup.version']").val(oNode.version);
			datatable_ec_dataclassific_table.setRequestDataUrl('/msService/ec/view/data_classificlist?dataGroup.code='+oNode.code+(window._proj_config_flag?'&isProj=true':''));
		}else{
			datatable_ec_dataclassific_table.setRequestDataUrl("/msService/ec/view/data_classificlist?view.code=${view.code}"+(window._proj_config_flag?'&isProj=true':''));
			document.getElementById('ec_group_edit_form').reset();
		}
	};
	ec.datagroup.editDlg;
	ec.dataclassific.editDlg;
	ec.dataclassific.renew;
	/**
	 * 数据分组添加
	 * @method ec.datagroup.add
	 * @public
	 */
	ec.datagroup.add=function(){
		var layoutName = $("[name='layoutName']").val();
		<#if targetModel??>
		var targetModelCode = $("[name='targetModel.code']").val();
		</#if>
		var dlg= ec.datagroup.editDlg = new CUI.Dialog({
			title : "${getHtmlText('ec.view.adddatagroup')}",
			type : 2,
			modal : true,
			url : "/msService/ec/view/datagroupedit?view.code=${view.code}${isProj?string('&isProj=true','')}&layoutName="+layoutName<#if targetModel??>+"&targetModel.code="+targetModelCode</#if>,
			buttons:[
				{	name:"${getHtmlText('common.button.save')}",
					handler:function(){	
						$("#ec_group_add_form input[name='dataGroup.view.code']").val("${view.code}");
						if($("#ec_group_add_form input[name='dataGroup.name']").val() == null || $("#ec_group_add_form input[name='dataGroup.name']").val() == "") {
							ec_group_add_formDialogErrorBarWidget.showMessage("${getHtmlText('ec.view.datagroup.nameNotNull')}",'f');
							$("#ec_group_add_form input[name='dataGroup.name']")[0].focus();
							return false;
						}
						if($("#ec_group_add_form input[name='dataGroup.displayName']").val() == null || $("#ec_group_add_form input[name='dataGroup.displayName']").val() == "") {
							ec_group_add_formDialogErrorBarWidget.showMessage("${getHtmlText('ec.view.datagroup.displayNameNotNull')}",'f')
							$("#ec_group_add_form input[name='international_dataGroupdisplayName_showName']")[0].focus();
							return false;
						}
						$("#ec_group_add_form").submit();
					}
				},
				{	name:"${getHtmlText('common.button.cancel')}",
					handler:function(){this.close();}
				}]
		});
		dlg.show();
	}
	/**
	 * 数据分组修改
	 * @method ec.datagroup.mod
	 * @public
	 */
	 ec.datagroup.mod=function(){
		if(ec_dataGroup_Tree.getSelectedNodes()[0]==null || ec_dataGroup_Tree.getSelectedNodes()[0].level==0){
				CUI.Dialog.alert("${getHtmlText('ec.view.choicedata')}");
			return;
		}
		if($("#ec_group_edit_form input[name='dataGroup.name']").val() == null || $("#ec_group_edit_form input[name='dataGroup.name']").val() == "") {
			ec_group_edit_formDialogErrorBarWidget.showMessage("${getHtmlText('ec.view.datagroup.nameNotNull')}",'f');
			$("#ec_group_edit_form input[name='dataGroup.name']")[0].focus();
			return false;
		}
		if($("#ec_group_edit_form input[name='international_dataGroupdisplayNameedit_showName']").val() == null || $("#ec_group_edit_form input[name='international_dataGroupdisplayNameedit_showName']").val() == "") {
			ec_group_edit_formDialogErrorBarWidget.showMessage("${getHtmlText('ec.view.datagroup.displayNameNotNull')}",'f');
			$("#ec_group_edit_form input[name='international_dataGroupdisplayNameedit_showName']")[0].focus();
			return false;
		}
		$('#dataGroupDisplayName').val($("#ec_group_edit_form input[name='international_dataGroupdisplayNameedit_showName']").val());
		$("#ec_group_edit_form").submit();
	}
	/**
	 * 数据分组删除
	 * @method ec.datagroup.del
	 */
	 ec.datagroup.del=function(){
	 	if(ec_dataGroup_Tree.getSelectedNodes()[0]==null || !ec_dataGroup_Tree.getSelectedNodes()[0].code || ec_dataGroup_Tree.getSelectedNodes()[0].level==0){
			CUI.Dialog.alert("${getHtmlText('ec.view.choicedatadel')}");
			return;
		}
		if(confirm("${getText('common.button.suredelete')}")){
			$.ajax({
				data : {"dataGroup.code":ec_dataGroup_Tree.getSelectedNodes()[0].code,"dataGroup.version":ec_dataGroup_Tree.getSelectedNodes()[0].version?ec_dataGroup_Tree.getSelectedNodes()[0].version:0},
				url : "/msService/ec/view/datagroupdelete" ,
				success : function(msg){
					if(msg && msg.success){
						ec_group_edit_formDialogErrorBarWidget.showMessage("${getHtmlText('common.delete.success')}",'s');
						var rootNode = ec_dataGroup_Tree.getNodeByParam("id", -1);
						ec_dataGroup_Tree.reAsyncChildNodes(rootNode, "refresh");
						datatable_ec_dataclassific_table.setRequestDataUrl("/msService/ec/view/data_classificlist?view.code=${view.code}"+(window._proj_config_flag?'&isProj=true':''));
						document.getElementById('ec_group_edit_form').reset();
					}else{
						if(msg && msg.exceptionMsg){alert(msg.exceptionMsg);}
						else{ec_group_edit_formDialogErrorBarWidget.showMessage("${getHtmlText('common.delete.failure')}",'f');}
					}
				}	
			});
		}
	 }
	 /**
	 * 数据分组提交返回数据分组及回刷树
	 * @method ec.module.Callback
	 */
	ec.datagroup.editCallback=function(msg){
		if(msg && msg.code) {
			ec_group_edit_formDialogErrorBarWidget.show("${getHtmlText('ec.dataclassific.submitsuccessful')}","s");
			var rootNode = ec_dataGroup_Tree.getNodeByParam("id", -1);
			ec_dataGroup_Tree.reAsyncChildNodes(rootNode, "refresh");
			$("#ec_group_edit_form input[name='dataGroup.version']").val(msg.version);
			datatable_ec_dataclassific_table.setRequestDataUrl('/msService/ec/view/data_classificlist?dataGroup.code='+ msg.code+(window._proj_config_flag?'&isProj=true':''));
		}else{
			ec_group_edit_formDialogErrorBarWidget.show("${getHtmlText('ec.dataclassific.submitfailure')}");
        }
         CUI.Dialog.toggleAllButton(null,true,true);
		
	}
	
	ec.datagroup.addCallback=function(msg){
		if(msg && msg.code){
			ec_group_add_formDialogErrorBarWidget.show("${getHtmlText('ec.dataclassific.submitsuccessful')}","s");
			setTimeout(function(){
				try{ec.datagroup.editDlg.close();}catch(e){}
				},1500);
			var rootNode = ec_dataGroup_Tree.getNodeByParam("id", -1);
			ec_dataGroup_Tree.reAsyncChildNodes(rootNode, "refresh");
		}else{
			ec_group_add_formDialogErrorBarWidget.show("${getHtmlText('ec.dataclassific.submitfailure')}");
			setTimeout(function(){
				try{ec.datagroup.editDlg.close();}catch(e){}
				},1500);
         }
		
	}
	
	 /**
	 * 数据分类添加
	 * @method ec.dataclassific.add
	 */
	ec.dataclassific.add=function(){
		if(ec_dataGroup_Tree.getSelectedNodes()[0]==null || ec_dataGroup_Tree.getSelectedNodes()[0].level==0){
				CUI.Dialog.alert("${getHtmlText('ec.view.choicedatagroup')}");
			return;
		}
		var dlg = ec.dataclassific.editDlg = new CUI.Dialog({
			title : "${getHtmlText('ec.view.dataclassadd')}",
			type : 3,
			modal : true,
			url : "/msService/ec/view/dataclassificedit?view.code=${view.code}${targetModelCode!}&dataGroup.code="+ec_dataGroup_Tree.getSelectedNodes()[0].code+(window._proj_config_flag?'&isProj=true':''),
			buttons:[
				{	name:"${getHtmlText('common.button.save')}",
					handler:function(d){
						$("#ec_classific_edit_form input[name='dataClassific.dataGroup.code']").val(ec_dataGroup_Tree.getSelectedNodes()[0].code);
						if($("#ec_classific_edit_form input[name='dataClassific.name']").val() == null || $("#ec_classific_edit_form input[name='dataClassific.name']").val() == "") {
							ec_classific_edit_formDialogErrorBarWidget.showMessage("${getHtmlText('ec.view.dataclassific.nameNotNull')}",'f');
							$("#ec_classific_edit_form input[name='dataClassific.name']")[0].focus();
							return false;
						}
						if($("#ec_classific_edit_form input[name='dataClassific.displayName']").val() == null || $("#ec_classific_edit_form input[name='dataClassific.displayName']").val() == "") {
							ec_classific_edit_formDialogErrorBarWidget.showMessage("${getHtmlText('ec.view.datagroup.displayNameNotNull')}",'f');
							$("#ec_classific_edit_form input[name='dataClassific.displayName']")[0].focus();
							return false;
						}
						$("#ec_classific_edit_form").submit();
					}
				},
				{	name:"${getHtmlText('common.button.cancel')}",
					handler:function(){this.close();}
				}]
		});
		dlg.show();
	}
	 /**
	 * 数据分类修改
	 * @method ec.dataclassific.mod
	 */
	ec.dataclassific.mod=function(){
		<#if !isReadOnlyMode>
		if(datatable_ec_dataclassific_table.selectedRows.length==0){
			CUI.Dialog.alert("${getHtmlText('ec.view.choicedataclass')}");
			return;
		}
		var dlg = ec.dataclassific.editDlg = new CUI.Dialog({
			title : "${getHtmlText('ec.view.dataclassedit')}",
			type : 3,
			modal : true,
			url : "/msService/ec/view/dataclassificedit?view.code=${view.code}${targetModelCode!}&dataClassific.code="+datatable_ec_dataclassific_table.selectedRows[0].code+(window._proj_config_flag?'&isProj=true':''),
			buttons:[
				{	name:"${getHtmlText('common.button.save')}",
					handler:function(d){
						$("#ec_classific_edit_form").submit();
					}
				},
				{	name:"${getHtmlText('common.button.cancel')}",
					handler:function(){this.close();}
				}]
		});
		dlg.show();
		</#if>
	}
	 /**
	 * 数据分类删除
	 * @method ec.dataclassific.del
	 */
	ec.dataclassific.del=function(){
		if(datatable_ec_dataclassific_table.selectedRows.length==0){
			CUI.Dialog.alert("${getHtmlText('ec.view.choicedataclassdel')}");
			return;
		}
		if(confirm("${getText('common.button.suredelete')}")){
			$.ajax({
				data : {"dataClassific.code":datatable_ec_dataclassific_table.selectedRows[0].code,"dataClassific.version":datatable_ec_dataclassific_table.selectedRows[0].version},
				url : "/msService/ec/view/dataclassificdelete"+(window._proj_config_flag?'?isProj=true':'') ,
				success : function(msg){
					if(msg && msg.success){
						CUI.Dialog.alert("${getHtmlText('common.delete.success')}");
						datatable_ec_dataclassific_table.setRequestDataUrl("/msService/ec/view/data_classificlist?view.code=${view.code}&dataGroup.code="+ec_dataGroup_Tree.getSelectedNodes()[0].code+(window._proj_config_flag?'&isProj=true':''));
					}else{
						if(msg && msg.exceptionMsg){alert(msg.exceptionMsg);}
						else{CUI.Dialog.alert("${getHtmlText('common.delete.failure')}");}
					}
				}	
			});
		}
	}
	/**
	 * 数据分类提交返回数据分类及回列表
	 * @method ec.dataclassific.Callback
	 * @public
	 */
	ec.dataclassific.Callback=function(msg){
		var dataGroupCode = "";
		if(ec_dataGroup_Tree.getSelectedNodes()[0]!=null) {
			dataGroupCode = ec_dataGroup_Tree.getSelectedNodes()[0].code;
		}
		if(msg && msg.success){
			ec_classific_edit_formDialogErrorBarWidget.show("${getHtmlText('ec.dataclassific.submitsuccessful')}","s");
			setTimeout(function(){
				try{ec.dataclassific.editDlg.close();}catch(e){}
				ec.datagroup.refresh();
				},1500);
			$('#dataClassificCode').val(msg.data);
			dataclassifc.condition.submitCustomerCon();
		}else{
			ec_classific_edit_formDialogErrorBarWidget.show("${getHtmlText('ec.dataclassific.submitfailure')}");
			setTimeout(function(){
				try{ec.dataclassific.editDlg.close();}catch(e){}
				ec.datagroup.refresh();
				},1500);
        }
	}
	ec.datagroup.refresh=function(){
		if (ec_dataGroup_Tree.getSelectedNodes()[0] != null && ec_dataGroup_Tree.getSelectedNodes()[0].level > 0) {
			ec.datagroup.showClassificInfo(ec_dataGroup_Tree.getSelectedNodes()[0]);
		}else{
			reload();
		}
	}
})();
</script>
