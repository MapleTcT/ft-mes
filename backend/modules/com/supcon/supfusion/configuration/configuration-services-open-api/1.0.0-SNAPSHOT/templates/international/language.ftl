<style>
.configtable{display:none;}
</style>
<@errorbar id="languageErrorBar"/>
<#--<div style="margin-top:10px;margin-bottom:5px;margin-left:20px;width:90%;height:30px;" id="availableInternational">
		<span class="lab" style="float:left;line-height:28px;height:28px;">${getHtmlText('foundation.internation.Available')}:</span>
		<div style="width:60%;float:left;margin-right:15px;">
		<select id="availableLocales">
			<#list optionLocales as al>
			<option value="${al.language}_${al.country}">${al.displayLanguage}</option>
			</#list>
		</select>
		</div>
		<div >
		<@querybutton formId="availableInternational"  style="margin-top:0;" onclick="foundation.international.addLanguage()" isCustomize=true customizeName="foundation.basic.international.add" />
		</div>
</div>-->
<@datatable dtPage="languagesPage" formId="international_language" hidekey="['locale.displayLanguage','locale.displayName']" pageOrder="" paginator=false dataUrl="/foundation/international/languagePage.action" firstLoad=true style="margin:0px 10px;"  exportExcel=false  transMethod="post" id="internationalLangaugeListTable" >
			<@operatebar operates="code:base_international_delete||name:${getHtmlText('foundation.view.del')}||iconcls:del||onclick:foundation.international.delLanguage();" operateType="noPower"  resultType="json" />
			<@operatebar operates="code:base_international_enable||name:${getHtmlText('foundation.language.enable')}||iconcls:modify||onclick:foundation.international.enableLanguage();" operateType="noPower"  resultType="json" />
			<@operatebar operates="code:base_international_disable||name:${getHtmlText('foundation.language.disable')}||iconcls:modify||onclick:foundation.international.disableLanguage();" operateType="noPower"  resultType="json" />
			<@datacolumn key="key" label="${getHtmlText('foundation.international.key')}" width="100"/>
			<@datacolumn key="displayName" label="${getHtmlText('foundation.userset.language')}"  width="150"/>	
			<@datacolumn key="isUsed" label="${getHtmlText('foundation.international.isUsed')}" width="100" type="boolean" />
</@datatable>
<!-- <div style="margin-top:20px;">
	
	<table width="90%" align="center" cellspacing="1" bgcolor="#CCCCCC" id="selectedLanguages">
		<tr><td colspan="3" style="padding-left:3px;">${getText('foundation.internation.Selected')}:</td></tr>
		<#list languages as l>
		<tr bgcolor="#FFFFFF" id="${l.key}">
			<td style="padding-left:3px;">${l.key}</td>
			<td style="padding-left:3px;">${getText(l.internationalKey)}</td>
			<td align="center"><a href="#" onclick="foundation.international.delLanguage('${l.key}')">${getText('foundation.inter.dellang')}</a></td>
		</tr>
		
		</#list>
	</table>
	<div style="margin:20px auto;width:90%;" id="availableInternational">
		<span>${getText('foundation.internation.Available')}:</span><br/>
		<select id="availableLocales">
			<#list availableLocales as al>
			<option>${al.language}_${al.country}</option>
			</#list>
		</select>
		<@querybutton formId="availableInternational" onclick="foundation.international.addLanguage()" isCustomize=true customizeName="foundation.basic.international.add"/>
	</div>
</div>-->
<script type="text/javascript">
	(function($){
		CUI.ns("foundation.international");
		
		$("#availableLocales").mSelect();
		foundation.international.addLanguage = function(){
			foundation.international.uploadImageDlg =	new CUI.Dialog({
				title: "${getHtmlText('foundation.international.imageUploadTitle')}",
				url :"/foundation/international/uploadlanguageImage.action?language.key="+$('#availableLocales').val()+"&language.internationalKey="+$('#availableLocales').val(),
				modal:true,
				type:1,
				//elementId:'internationalImage',
				buttons:[{	name:"${getHtmlText('common.button.save')}",
							handler:function(){foundation.international.SaveInfo();}
						},
						{	name:"${getHtmlText('common.button.cancel')}",
							handler:function(){this.close()}
						}]
			});
			foundation.international.uploadImageDlg.show();
		}
		foundation.international.SaveInfo=function(){
			var AllImgType=".jpg|.jpeg|.gif|.bmp|.png|"//全部图片格式类型
			var value=CUI("#InternationalUploadform input[name='languagefile']").val();
			if(value==""){
			  	return false;
			}
			var FileType=value.substr(value.lastIndexOf(".")).toLowerCase();
			if(AllImgType.indexOf(FileType+"|")==-1) //判断文件类型是否允许上传
			 {
				  var ErrMsg="${getHtmlText('foundation.international.imageTypeLimit')}";
				  InternationalUploadformDialogErrorBarWidget.show(ErrMsg,"f");
				  return false;
			 }
			else{
				CUI("#InternationalUploadform").submit();
			}
		};
		/**
	 * 保存完毕回调
	 * @method callBackInfo
	**/
	foundation.international.callBackInfo=function(res){
		if(res.dealSuccessFlag == true){
			foundation.international.uploadImageDlg.close();
			languageErrorBarWidget.show("${getHtmlText('foundation.inter.addsuccess')}","s");
			var url="/foundation/international/languagePage.action";
			internationalLangaugeListTableWidget.setRequestDataUrl(encodeURI(url));
		} else {
			InternationalUploadformDialogErrorBarWidget.show(res.Message,"f");
			CUI.Dialog.toggleAllButton("InternationalUploadform",true);
		}
	};
	/*
		foundation.international.addLanguage = function(){
			var optionValue=$('#availableLocales').val();
			CUI.ajax({
				type : "POST",
				url : "/foundation/international/save-lang.action",
				data : {"language.key" : $('#availableLocales').val(),"language.internationalKey" : $('#availableLocales').val()},
				success : function(msg){
					if(msg && msg.success == true){
						languageErrorBarWidget.show("${getHtmlText('foundation.inter.addsuccess')}","s");
						var url="/foundation/international/languagePage.action";
						internationalLangaugeListTableWidget.setRequestDataUrl(encodeURI(url));
						$('#availableLocales').delOption(optionValue);
					}
					else if(msg){languageErrorBarWidget.show(msg.exceptionMsg,"f");}
					else {languageErrorBarWidget.show("${getHtmlText('foundation.inter.addfail')}","f");}
				}
			});
		};
	*/
		foundation.international.delLanguage = function(){
			if(internationalLangaugeListTableWidget.selectedRows.length == 0){
				languageErrorBarWidget.showMessage("${getHtmlText('foundation.language.checkselected')}","f");
				return false;
			}
			if(internationalLangaugeListTableWidget.selectedRows[0].isUsed){
				languageErrorBarWidget.showMessage("${getHtmlText('foundation.language.enable.del.forbidden')}","f");
				return false;
			}
			var allRows = internationalLangaugeListTableWidget.getAllRows();
			if(allRows.length <= 1){
				languageErrorBarWidget.showMessage("${getHtmlText('foundation.language.last.forbidden')}","f");
				return false;
			}
			var key=internationalLangaugeListTableWidget.selectedRows[0].key;
			var displayLanguage=internationalLangaugeListTableWidget.selectedRows[0].locale.displayLanguage;
			CUI.Dialog.confirm("${getHtmlText('common.button.suredelete')}",function(){
				CUI.ajax({
					type : "POST",
					url : "/foundation/international/del-lang.action",
					data : {"language.key" : key},
					success : function(msg){
						if(msg && msg.success == true){
							languageErrorBarWidget.show("${getHtmlText('common.delete.success')}","s");
							var url="/foundation/international/languagePage.action";
							internationalLangaugeListTableWidget.setRequestDataUrl(encodeURI(url));
							$('#availableLocales').addOption(displayLanguage,key,0);
							foundation.international.isNeedRefresh="yes";
						} else if(msg){
							languageErrorBarWidget.show(msg.exceptionMsg,"f");
						} else {
							languageErrorBarWidget.show("${getHtmlText('common.delete.failure')}","f");
						}
					}
			   });
			})
		};	
		
		foundation.international.enableLanguage = function(){
			if(internationalLangaugeListTableWidget.selectedRows.length == 0){
				languageErrorBarWidget.showMessage("${getHtmlText('foundation.language.checkselected')}","f");
				return false;
			}
			if(internationalLangaugeListTableWidget.selectedRows[0].isUsed){
				languageErrorBarWidget.showMessage("${getHtmlText('foundation.language.hasEnabled')}","f");
				return false;
			}
			var key=internationalLangaugeListTableWidget.selectedRows[0].key;
			//var displayLanguage=internationalLangaugeListTableWidget.selectedRows[0].locale.displayLanguage;
			CUI.ajax({
				type : "POST",
				url : "/foundation/international/change-state.action",
				data : {"language.key" : key,"language.isUsed":true},
				success : function(msg){
					if(msg && msg.success == true){
						languageErrorBarWidget.show("${getHtmlText('foundation.language.enable.success')}","s");
						var url="/foundation/international/languagePage.action";
						internationalLangaugeListTableWidget.setRequestDataUrl(encodeURI(url));
						foundation.international.isNeedRefresh="yes";
					} else if(msg){
						languageErrorBarWidget.show(msg.exceptionMsg,"f");
					} else {
						languageErrorBarWidget.show("${getHtmlText('foundation.language.enable.failure')}","f");
					}
					
				}
		   });
		};	
		
		foundation.international.disableLanguage = function(){
			if(internationalLangaugeListTableWidget.selectedRows.length == 0){
				languageErrorBarWidget.showMessage("${getHtmlText('foundation.language.checkselected')}","f");
				return false;
			}
			if(!internationalLangaugeListTableWidget.selectedRows[0].isUsed){
				languageErrorBarWidget.showMessage("${getHtmlText('foundation.language.hasDisabled')}","f");
				return false;
			}
			var key=internationalLangaugeListTableWidget.selectedRows[0].key;
			var defaultLanguage = "${getConfigProperty('platform/bap/basic/international.defaultLanguage')}";
			if(null == defaultLanguage || undefined == defaultLanguage || defaultLanguage == ''){
				defaultLanguage = "zh_CN";
			}
			if(key == defaultLanguage){
				languageErrorBarWidget.showMessage("${getHtmlText('foundation.default.language.forbidden')}","f");
				return false;
			}
			var allRows = internationalLangaugeListTableWidget.getAllRows();
			var singleDisable = true;
			for(var i=0;i<allRows.length;i++){
				if(allRows[i].key != key && allRows[i].isUsed){
					singleDisable = false;
					break;
				}
			}
			if(singleDisable){
				languageErrorBarWidget.showMessage("${getHtmlText('foundation.language.disabled.single')}","f");
				return false;
			}

			CUI.ajax({
				type : "POST",
				url : "/foundation/international/change-state.action",
				data : {"language.key" : key,"language.isUsed":false},
				success : function(msg){
					if(msg && msg.success == true){
						languageErrorBarWidget.show("${getHtmlText('foundation.language.disable.success')}","s");
						var url="/foundation/international/languagePage.action";
						internationalLangaugeListTableWidget.setRequestDataUrl(encodeURI(url));
						foundation.international.isNeedRefresh="yes";
					} else if(msg){
						languageErrorBarWidget.show(msg.exceptionMsg,"f");
					} else {
						languageErrorBarWidget.show("${getHtmlText('foundation.language.disable.failure')}","f");
					}
				}
		   });
		};		
	})(jQuery);
</script>