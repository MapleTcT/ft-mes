<#-- ==================================== 签名控件 ==================================== -->

<#macro signature name="" userIds="" positionIds="" roleIds="" userId="" userId1="" staffName="" signature_userlogin="" use_signature_double="" actionData="" entityName="" viewName="" view_Path="" tableInfoId="" editPageNs="" >
	<#assign lastIndex = name?last_index_of(".")>
	<#assign objectName = name?substring(0,lastIndex) >
	<#assign id = name?replace(".","_") >
	<div class="fix-input">
		<div class="fix-search-click">
			<input id="${(name!)?replace('.','_')}_name" name="${name!}.name" signature_field="true"  type="text" class="cui-noborder-input" readonly="true" value="${staffName!}" />
			<input id="${(name!)?replace('.','_')}" name="${name!}" signature_field="true" type="hidden" reset="false" value="${userId!}"/>
			<#if use_signature_double?? && use_signature_double == 'true'>
			<input id="${id}_id1" name="${objectName}.id1" signature_field="true" type="hidden" reset="false" value="${userId1!}"/>
			</#if>
			<input id="${id}_signature_userlogin" type="hidden" value="${signature_userlogin!}"/>
			<input id="${id}_view_Path" type="hidden" value="${view_Path!}"/>
			<input id="${id}_tableInfoId" type="hidden" value="${tableInfoId!}"/>
			<input id="${id}_use_signature_double" type="hidden" value="${use_signature_double!}"/>
			<input id="${id}_editPageNs" type="hidden" value="${editPageNs!}"/>
			<input id="${id}_signature_clear_btn" type="button" class="cui-edit-clear"  />
			<input id="${id}_signature_show_btn" style="display:block;" type="button"  class="cui-search-click" value="&nbsp;" ></input>
		</div>
	</div>
	
	<script type="text/javascript">
		$(function(){
			setTimeout(function(){
				var signatureUserId = $('#${(name!)?replace('.','_')}').val();
				if(null != signatureUserId && signatureUserId != ""){
					$("input[name='${name!}.name']").trigger("signature_complete");
				}
			},200);
		});
		$( '#${id}_signature_clear_btn' ).click( function(){
			var entityName = '${entityName!}';
			var field = '';
			var dependField = '';
			<#if actionData != "">
			var actionData = ${actionData!};
			var data = [];
			data = actionData.signature_complete.data;
			for(var i=0;i<data.length;i++){
				if (data[i].title == "控件依赖关系") {
				    var fields = [];
					fields = data[i].fields;
					for(var j=0;j<fields.length;j++){
						if( fields[j].checked ){
							if(field == ''){
								field = '针对实体【${entityName!}】、视图【${viewName!}】、字段【' + fields[j].label + '】';
							}else{
								field = field + ',【' + fields[j].label + '】 取消签名';
							}
							var fieldName=fields[j].name.split('.');
							if(dependField == ''){
								dependField = fieldName[1];
							}else{
								dependField = dependField + ',' + fieldName[1];
							}
						}
					}
				}
			}
			if(field == ''){
				field = '针对实体【${entityName!}】、视图【${viewName!}】取消签名';
			}else{
				field = field + '取消签名';
			}
			<#else>
				field = '针对实体【${entityName!}】、视图【${viewName!}】取消签名';
			</#if>
			if( $( '#${(name!)?replace('.','_')}' ).val() ){
				var url;
				if( $('#${id}_use_signature_double').val() == 'true' ){
					url = '/foundation/signature/frame?openType=frame&use_signature_double=' + $('#${id}_use_signature_double').val() + '&userId=' + $('#${(name!)?replace('.','_')}').val() + '&userId1=' + $('#${id}_id1').val() + '&__tt__=' + ( + new Date() ) + '&content=' + field;
				}else{
					url = '/foundation/signature/frame?openType=frame&use_signature_double=' + $('#${id}_use_signature_double').val() + '&userId=' + $('#${(name!)?replace('.','_')}').val() + '&__tt__=' + ( + new Date() ) + '&content=' + field;
				}
				if( ${id}_signature_dialog ){
				${id}_signature_dialog.setTitle( '数字签名撤销' );
				${id}_signature_dialog._config.url = url,
				${id}_signature_dialog.show();
				${id}_signature_dialog.setButtonbar([
					{
						name : "确定",
						handler : function() {
							var url;
							if( $('#${id}_use_signature_double').val() == 'true' ){
								url = '/foundation/signature/check?userId=' + ${id}_signature_iframe.$( '#userId' ).val() + '&password=' + ${id}_signature_iframe.$( '#password' ).val() + '&description=' + ${id}_signature_iframe.$( 'textarea[name="description"]' ).val() + '&userId1=' + ${id}_signature_iframe.$( '#userId1' ).val() + '&password1=' + ${id}_signature_iframe.$( '#password1' ).val() + '&description1=' + ${id}_signature_iframe.$( 'textarea[name="description1"]' ).val()+ '&operateType=concel&use_signature_double=' + $('#${id}_use_signature_double').val() + '&signature_userlogin=' + $('#${id}_signature_userlogin').val() + '&content=' + ${id}_signature_iframe.$( 'textarea[name="content"]' ).val() + '&dependField=' + dependField + '&view_Path=' + $('#${id}_view_Path').val() + '&tableInfoId=' + $('#${id}_tableInfoId').val() ;
							}else{
								url = '/foundation/signature/check?userId=' + ${id}_signature_iframe.$( '#userId' ).val() + '&password=' + ${id}_signature_iframe.$( '#password' ).val() + '&description=' + ${id}_signature_iframe.$( 'textarea[name="description"]' ).val() + '&operateType=concel&use_signature_double=' + $('#${id}_use_signature_double').val() + '&signature_userlogin=' + $('#${id}_signature_userlogin').val() + '&content=' + ${id}_signature_iframe.$( 'textarea[name="content"]' ).val() + '&dependField=' + dependField + '&view_Path=' + $('#${id}_view_Path').val() + '&tableInfoId=' + $('#${id}_tableInfoId').val();
							}
							$.ajax({
								type : 'GET',
								url : url,
								async : false,
								cache : false,
								dataType: 'json',
								success : function ( data ) {
									if( data.signatureSuccess ){
										$( '#${(name!)?replace('.','_')}_name' ).val( '' );
										$( '#${(name!)?replace('.','_')}' ).val( '' );
										//$( '#${id}_id1' ).val( '' );
										$( '#${(name!)?replace('.','_')}_name' ).trigger( 'signature_clear' );
										<#if actionData != "">
										var actionData = ${actionData!};
										var data = [];
										data = actionData.signature_complete.data;
										for(var i=0;i<data.length;i++){
											if (data[i].name == "depends") {//控件依赖
											    var fields = [];
												fields = data[i].fields;
												for(var j=0;j<fields.length;j++){
													if( fields[j].checked ){
														$( 'input[name="' + fields[j].name + '"]').removeAttr( 'disabled' );
													}
												}
											}
										}
										</#if>
										${editPageNs}.signatureAfterSave();
										try{
											workbenchErrorBarWidget.showMessage('签名已撤销！', 's');
										}catch(e){}
										${id}_signature_dialog.close();
									}else{
										try{
											workbenchErrorBarWidget.showMessage(  data.msg );
										}catch(e){}
									}
								}
							});
						}
					}, {
						name : "取消",
						handler : function() {
							this.close();
						}
					}
				]);
				}else{
					${id}_signature_dialog = new CUI.Dialog({
										title : '数字签名撤销',
										modal : true,
										height : 400,
										width : 600,
										iframe: '${id}_signature_iframe',
										url : url,
										dragable : true,
										buttons : [
												{
						name : "确定",
						handler : function() {
							var url;
							if( $('#${id}_use_signature_double').val() == 'true' ){
								url = '/foundation/signature/check?userId=' + ${id}_signature_iframe.$( '#userId' ).val() + '&password=' + ${id}_signature_iframe.$( '#password' ).val() + '&description=' + ${id}_signature_iframe.$( 'textarea[name="description"]' ).val() + '&userId1=' + ${id}_signature_iframe.$( '#userId1' ).val() + '&password1=' + ${id}_signature_iframe.$( '#password1' ).val() + '&description1=' + ${id}_signature_iframe.$( 'textarea[name="description1"]' ).val()+ '&operateType=concel&use_signature_double=' + $('#${id}_use_signature_double').val() + '&signature_userlogin=' + $('#${id}_signature_userlogin').val() + '&content=' + ${id}_signature_iframe.$( 'textarea[name="content"]' ).val() + '&dependField=' + dependField + '&view_Path=' + $('#${id}_view_Path').val()  + '&tableInfoId=' + $('#${id}_tableInfoId').val();
							}else{
								url = '/foundation/signature/check?userId=' + ${id}_signature_iframe.$( '#userId' ).val() + '&password=' + ${id}_signature_iframe.$( '#password' ).val() + '&description=' + ${id}_signature_iframe.$( 'textarea[name="description"]' ).val() + '&operateType=concel&use_signature_double=' + $('#${id}_use_signature_double').val() + '&signature_userlogin=' + $('#${id}_signature_userlogin').val() + '&content=' + ${id}_signature_iframe.$( 'textarea[name="content"]' ).val() + '&dependField=' + dependField + '&view_Path=' + $('#${id}_view_Path').val() + '&tableInfoId=' + $('#${id}_tableInfoId').val();
							}
							$.ajax({
								type : 'GET',
								url : url,
								async : false,
								cache : false,
								dataType: 'json',
								success : function ( data ) {
									if( data.signatureSuccess ){
										$( '#${(name!)?replace('.','_')}_name' ).val( '' );
										$( '#${(name!)?replace('.','_')}' ).val( '' );
										//$( '#${id}_id1' ).val( '' );
										$( '#${(name!)?replace('.','_')}_name' ).trigger( 'signature_clear' );
										<#if actionData != "">
										var actionData = ${actionData!};
										var data = [];
										data = actionData.signature_complete.data;
										for(var i=0;i<data.length;i++){
											if (data[i].name == "depends") {//控件依赖
											    var fields = [];
												fields = data[i].fields;
												for(var j=0;j<fields.length;j++){
													if( fields[j].checked ){
														$( 'input[name="' + fields[j].name + '"]').removeAttr( 'disabled' );
													}
												}
											}
										}
										</#if>
										${editPageNs}.signatureAfterSave();
										try{
											workbenchErrorBarWidget.showMessage('签名已撤销！', 's');
										}catch(e){}
										${id}_signature_dialog.close();
									}else{
										try{
											workbenchErrorBarWidget.showMessage(  data.msg );
										}catch(e){}
									}
								}
							});
						}
					}, {
						name : "取消",
						handler : function() {
							this.close();
						}
					} ]
					})
				}
			}
		})
		
		$( document.body ).click( function(e){
			if( ${id}_signature_overlay ){
				${id}_signature_overlay.hide();
			}
		})
		
		$( '#${id}_signature_show_btn' ).click( function(e){
			if($( '#${(name!)?replace('.','_')}' ).val() == ""){
			e.stopPropagation();
			var t = $("div[class='overlay-layer']");
			for(var i=0;i<t.length;i++){
				var c = $(t[i]).attr('id');
				var d = c.split('overlay-id');
				var t_id = d[1].split('_signature_overlay_wrap');
				var sign_id = t_id[0] + '_signature_overlay';
				if ( t_id[0] != '${id}' ) {
					$("#"+c).hide();
				}else{
					$("#"+c).show();
				}
			}
			show_${id}_signature_overlay(this);
			}
		})
		
		var ${id}_signature_overlay = null;
		var ${id}_signature_dialog = null;
			
		function show_${id}_signature_overlay(obj){
			var entityName = '${entityName!}';
			var field = '';
			var dependField = '';
			<#if actionData != "">
			var actionData = ${actionData!};
			var data = [];
			data = actionData.signature_complete.data;
			for(var i=0;i<data.length;i++){
				if (data[i].title == "控件依赖关系") {
				    var fields = [];
					fields = data[i].fields;
					for(var j=0;j<fields.length;j++){
					    if( fields[j].checked ){
					    	if($('input[name="'+fields[j].name+'"]').val()==""){
								workbenchErrorBarWidget.showMessage("<b>"+fields[j].name.split('.')+"</b>字段不能为空！");
								return;
							}
							if(field == ''){
								field = '针对实体【${entityName!}】、视图【${viewName!}】、字段【' + fields[j].label + '】';
							}else{
								field = field + ',【' + fields[j].label + '】';
							}
							var fieldName=fields[j].name.split('.');
							if(dependField == ''){
								dependField = fieldName[1];
							}else{
								dependField = dependField + ',' + fieldName[1];
							}
						}
					}
				}
			}
			if(field == ''){
				field = '针对实体【${entityName!}】、视图【${viewName!}】进行签名';
			}else{
				field = field + '进行签名';
			}
			<#else>
				field = '针对实体【${entityName!}】、视图【${viewName!}】进行签名';
			</#if>
			if( ${id}_signature_overlay ){
				${id}_signature_overlay.setPosition();
				${id}_signature_overlay.show();
				return;
			}
			${id}_signature_overlay = new CUI.Overlay({
				align:obj,
				el:'${id}_signature_overlay_wrap',
				title:'请选择签名人员',
				width:180,
				height:282,
				zIndex:9999,
				shadow:false,
				buttons:[
						{	name:"<span i18n='foundation.workbench.mainPage.sure'>确定</span>",
							id:"${id}_signature_overlay_btn_select",
							handler:function(){
								var checkedBox = $( '#${id}_signature_container input[type=checkbox]:checked' );
								if( checkedBox.length == 0 ){
									alert( '请指定签名人员！' );
									return;
								}
								if( $('#${id}_use_signature_double').val() == 'true' ){
									if( checkedBox.length == 1 ){
									alert( '已启用双签名,请再选择一个人员！' );
									return;
									}
								}
								
								${id}_signature_overlay.hide();
								
								if( !${id}_signature_dialog ){
									var url;
									if( $('#${id}_use_signature_double').val() == 'true' ){
										url = '/foundation/signature/frame?openType=frame&use_signature_double=' + $('#${id}_use_signature_double').val() + '&signature_userlogin=' + $('#${id}_signature_userlogin').val() + '&userId=' + checkedBox[0].value + ',' + checkedBox[1].value + '&__tt__=' + ( + new Date() ) + '&content=' + field;
									}else{
										url = '/foundation/signature/frame?openType=frame&use_signature_double=' + $('#${id}_use_signature_double').val() + '&signature_userlogin=' + $('#${id}_signature_userlogin').val() + '&userId=' + checkedBox[0].value + '&__tt__=' + ( + new Date() ) + '&content=' + field;
									}
									${id}_signature_dialog = new CUI.Dialog({
										title : '数字签名确认',
										modal : true,
										height : 400,
										width : 600,
										iframe: '${id}_signature_iframe',
										url : url,
										dragable : true,
										buttons : [
												{
													name : "确定",
													handler : function() {
														var url;
														if( $('#${id}_use_signature_double').val() == 'true' ){
															url = '/foundation/signature/check?userId=' + ${id}_signature_iframe.$( '#userId' ).val() + '&password=' + ${id}_signature_iframe.$( '#password' ).val() + '&description=' + ${id}_signature_iframe.$( 'textarea[name="description"]' ).val() + '&userId1=' + ${id}_signature_iframe.$( '#userId1' ).val() + '&password1=' + ${id}_signature_iframe.$( '#password1' ).val() + '&description1=' + ${id}_signature_iframe.$( 'textarea[name="description1"]' ).val()+ '&operateType=signature&use_signature_double=' + $('#${id}_use_signature_double').val() + '&signature_userlogin=' + $('#${id}_signature_userlogin').val() + '&content=' + ${id}_signature_iframe.$( 'textarea[name="content"]' ).val() + '&dependField=' + dependField + '&view_Path=' + $('#${id}_view_Path').val() + '&tableInfoId=' + $('#${id}_tableInfoId').val();
														}else{
															url = '/foundation/signature/check?userId=' + ${id}_signature_iframe.$( '#userId' ).val() + '&password=' + ${id}_signature_iframe.$( '#password' ).val() + '&description=' + ${id}_signature_iframe.$( 'textarea[name="description"]' ).val() + '&operateType=signature&use_signature_double=' + $('#${id}_use_signature_double').val() + '&signature_userlogin=' + $('#${id}_signature_userlogin').val() + '&content=' + ${id}_signature_iframe.$( 'textarea[name="content"]' ).val() + '&dependField=' + dependField + '&view_Path=' + $('#${id}_view_Path').val() + '&tableInfoId=' + $('#${id}_tableInfoId').val();
														}
														$.ajax({
															type : 'GET',
															url : url,
															async : false,
															cache : false,
															dataType: 'json',
															success : function ( data ) {
																if( data.signatureSuccess ){
																	var a = ${id}_signature_iframe.$( '#staffName' ).val();
																	console.log(a);
																	var b = ${id}_signature_iframe.$( '#staffName1' ).val();
																	var userId = ${id}_signature_iframe.$( '#userId' ).val();
																	//var userId1 = ${id}_signature_iframe.$( '#userId1' ).val();
																	if( $('#${id}_use_signature_double').val() == 'true' ){
																		$( '#${(name!)?replace('.','_')}_name' ).val( a + ',' + b );
																	}else{
																		$( '#${(name!)?replace('.','_')}_name' ).val( a );
																	}
																	$( '#${(name!)?replace('.','_')}' ).val( userId);
																	//$( '#${id}' ).val( userId );
																	$( '#${(name!)?replace('.','_')}_name' ).trigger( 'signature_complete' );
																	${editPageNs}.signatureAfterSave();
																	try{
																		workbenchErrorBarWidget.showMessage('签名成功！', 's');
																	}catch(e){}
																	${id}_signature_dialog.close();
																}else{
																	try{
																		workbenchErrorBarWidget.showMessage(  data.msg );
																	}catch(e){}
																}
															}
														});
													}
												}, {
													name : "取消",
													handler : function() {
														this.close();
													}
												} ]
									})
								}else{
									var url;
									if( $('#${id}_use_signature_double').val() == 'true' ){
										url = '/foundation/signature/frame?openType=frame&use_signature_double=' + $('#${id}_use_signature_double').val() + '&signature_userlogin=' + $('#${id}_signature_userlogin').val() + '&userId=' + checkedBox[0].value + ',' + checkedBox[1].value + '&__tt__=' + ( + new Date() ) + '&content=' + field;
									}else{
										url = '/foundation/signature/frame?openType=frame&use_signature_double=' + $('#${id}_use_signature_double').val() + '&signature_userlogin=' + $('#${id}_signature_userlogin').val() + '&userId=' + checkedBox[0].value + '&__tt__=' + ( + new Date() ) + '&content=' + field;
									}
									${id}_signature_dialog.setTitle( '数字签名确认' );
									${id}_signature_dialog._config.url = url,
									${id}_signature_dialog.setButtonbar([
										{
											name : "确定",
											handler : function() {
												if( $('#${id}_use_signature_double').val() == 'true' ){
													url = '/foundation/signature/check?userId=' + ${id}_signature_iframe.$( '#userId' ).val() + '&password=' + ${id}_signature_iframe.$( '#password' ).val() + '&description=' + ${id}_signature_iframe.$( 'textarea[name="description"]' ).val() + '&userId1=' + ${id}_signature_iframe.$( '#userId1' ).val() + '&password1=' + ${id}_signature_iframe.$( '#password1' ).val() + '&description1=' + ${id}_signature_iframe.$( 'textarea[name="description1"]' ).val() + '&operateType=signature&use_signature_double=' + $('#${id}_use_signature_double').val() + '&signature_userlogin=' + $('#${id}_signature_userlogin').val() + '&content=' + ${id}_signature_iframe.$( 'textarea[name="content"]' ).val() + '&dependField=' + dependField + '&view_Path=' + $('#${id}_view_Path').val() + '&tableInfoId=' + $('#${id}_tableInfoId').val();
												}else{
													url = '/foundation/signature/check?userId=' + ${id}_signature_iframe.$( '#userId' ).val() + '&password=' + ${id}_signature_iframe.$( '#password' ).val() + '&description=' + ${id}_signature_iframe.$( 'textarea[name="description"]' ).val() + '&operateType=signature&use_signature_double=' + $('#${id}_use_signature_double').val() + '&signature_userlogin=' + $('#${id}_signature_userlogin').val() + '&content=' + ${id}_signature_iframe.$( 'textarea[name="content"]' ).val() + '&dependField=' + dependField + '&view_Path=' + $('#${id}_view_Path').val() + '&tableInfoId=' + $('#${id}_tableInfoId').val();
												}
												$.ajax({
													type : 'GET',
													url : url,
													async : false,
													cache : false,
													dataType: 'json',
													success : function ( data ) {
														if( data.signatureSuccess ){
															var a = ${id}_signature_iframe.$( '#staffName' ).val();
															var b = ${id}_signature_iframe.$( '#staffName1' ).val();
															var userId = ${id}_signature_iframe.$( '#userId' ).val();
															//var userId1 = ${id}_signature_iframe.$( '#userId1' ).val();
															if( $('#${id}_use_signature_double').val() == 'true'){
																$( '#${(name!)?replace('.','_')}_name' ).val( a + ',' + b );
															}else{
																$( '#${(name!)?replace('.','_')}_name' ).val( a );
															}
															$( '#${(name!)?replace('.','_')}' ).val( userId);
															//$( '#${(name!)?replace('.','_')}' ).val( ${id}_signature_iframe.$( '#staffName' ).val() );
															//$( '#${id}' ).val( ${id}_signature_iframe.$( '#userId' ).val() );
															//$( '#${id}_id1' ).val( ${id}_signature_iframe.$( '#userId1' ).val() );
															$( '#${(name!)?replace('.','_')}_name' ).trigger( 'signature_complete' );
															${editPageNs}.signatureAfterSave();
															try{
																workbenchErrorBarWidget.showMessage('签名成功！', 's');
															}catch(e){}
															${id}_signature_dialog.close();
														}else{
															try{
																workbenchErrorBarWidget.showMessage(  data.msg );
															}catch(e){}
														}
													}
												});
											}
										}, {
											name : "取消",
											handler : function() {
												this.close();
											}
										} 
									]);
								
								}
								
								${id}_signature_dialog.show();
							}
						},
						{	name:"<span i18n='${getText("calendar.common.cancal")}'>取消</span>",
							id:"${id}_signature_overlay_btn_cancel",
							handler:function(){
								${id}_signature_overlay.hide();
							}
						}]
				
			});
			

			
			${id}_signature_overlay.render();
			
			$("#overlay-id${id}_signature_overlay_wrap").click(
				function(e){
					
					ev = e || window.event;
					if (ev) { // 停止事件冒泡
						ev.stopPropagation();
					} else {
						window.event.cancelBubble = true;
					}
				}
			)

			${id}_signature_overlay.show();
			
			$("#${id}_signature_overlay_wrap").html('<table style="width:100%;height:100%;text-align:center;"><tr><td><label class="datagrid-loading">正在加载...</label></td></tr></table>');
			
			$.ajax({
				type : 'GET',
				url : '/foundation/signature/staff/get?userIds=${userIds}&positionIds=${positionIds}&roleIds=${roleIds}&use_signature_double=' + $('#${id}_use_signature_double').val(),
				cache : false,
				dataType: 'json',
				success : function ( data ) {
					var html_srting = '<div id="${id}_signature_container" class="signature-root">';
					for( var i = 0; i < data.length; i++ ){
						html_srting += '<div class="signature-list-in"><input type="checkbox"  value="' + data[i].id + '"><span>' + data[i].staffName + '</span></div>'
					}
					html_srting += '</div>';
					$('#${id}_signature_overlay_wrap').html( html_srting )
					$( '#${id}_signature_container' ).delegate( 'input[type=checkbox]', 'change', function (e) {
						var checked = this.checked;
						//$( '#${id}_signature_container input[type=checkbox]' ).prop( 'checked', false );
						//this.checked = checked;
					})
				}
			});
	}
		
	</script>
	
</#macro>