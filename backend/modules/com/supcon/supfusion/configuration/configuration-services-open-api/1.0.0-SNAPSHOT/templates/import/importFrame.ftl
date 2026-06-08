<script type="text/javascript" src="/bap/static/foundation/js/zh_cn/export_common.js"></script>
<script type="text/javascript" src="/bap/static/foundation/js/zh_cn/export_list.js"></script>
<div id="import-field-displayname-dialog" style="display: none;">
 <table id="tab-common" class="infoTable" cellpadding="0" cellspacing="0" border="0" width="95%" align="left" style="margin-top: 10px">
  <tr> 
   <td class="la showname" width="40%" align="right" padding-right="10px">${getHtmlText('ec.view.showname')}</td>
   <td class="co showname" width="60%">
   <@international name="fieldProperty.showName" isNew=true isOldEdit=true moduleCode="${artifact!}" key="" style="width:100%;margin-right: -5px;" cssClass="cui-edit-field" />
   </td>
  </tr>
 </table>
</div>
<script>
	// 双击修改字段名称对话框
	 CUI.ExportConfList.prototype.setFieldDisplayName = function(el){
	  var objMe = this;
	  var $el = $(el);
	  this._fieldDisplayDialog = new CUI.Dialog({
	   "title":"字段属性设置",
	   "width":400,
	   "height":120,
	   "modal":true,
	   "buttons": [
	    {"name":"保存", 
	     "handler": function(){
		     	$el.attr('namekey', $('#international_fieldPropertyshowName').val());
		     	$el.find('.field-name').text( $('#international_fieldPropertyshowName_showName').val() );
		     	objMe._fieldDisplayDialog.close();
		     	$('#import-field-displayname-dialog').hide();
	      }
	    },
	    {
	    	"name":"取消", 
	     	"handler": function(){
	     		objMe._fieldDisplayDialog.close();
	     	}
	    }
	   ],
	   "elementId":"import-field-displayname-dialog"
	  });  
	  
	  $('#import-field-displayname-dialog').show();
	  $('#international_fieldPropertyshowName').val(objMe._get_namekey($el));
	  $('#international_fieldPropertyshowName_showName').val(objMe._get_name($el));
	  
	  this._fieldDisplayDialog.show();
	 };
	CUI.ExportConfList.prototype.addExportField = function(obj) {
		var objMe = this;
		var li = $(obj);
		var propertyCode = li.attr('propertyCode');
		var flag = true;
		var nullableParam = li.attr('nullable');
		$('tr','#fastColOrder').each(function(index){
			if($(this).attr('propertyCode') == propertyCode) {
				flag = false;
				return false;
			}
		});
		var	cellCode = "cell_" + new Date().getTime() +'_'+ Math.round(Math.random(1)*10000);
		if(flag) {
			var target = $('<tr cellCode="' + cellCode+ '"><td class="field-name"></td></tr>');
			if(undefined != nullableParam && null != nullableParam && "false" == nullableParam){
				$('td:first', target).addClass('field-nullable');
			}
			/*
			 * if($('tr:last','#fastColOrder').attr('numType') == "even") {
			 * target.attr("numType","odd"); target.addClass("odd-num"); } else {
			 * target.attr("numType","even"); target.addClass("even-num"); }
			 */
			var objImg = $('<td class="field-action"><img title="点击删除该字段" style="cursor:pointer;" src="/bap/static/ec/images/importTemplate/icon_delete.png" onMouseOver="deleteBtnChange(this)" onMouseOut="deleteBtnChange(this)"></img></td>');
			//objImg.unbind('click').bind('click',function(){
			//	objMe.deleteFastQueryField(this);
			//});
			objImg.attr('onclick', 'listec.deleteFastQueryField(this)');
			target.append(objImg);
			var columnType = li.attr('columnType');
			var showType = columnType;
			target.removeAttr('ondblclick');
			target.unbind('dblclick').dblclick(function(){
				objMe.selectRow('fast',this);
				objMe.setFieldDisplayName( this );
				//objMe.fastFieldProperty();
			});
			if(li.attr('propertyCode')!=undefined && li.attr('propertyCode').search('\\|')!=-1)  {
				var  propertyCodes =  new Array();
				var  finalString  = "";
				var  reverseString = "";
				var  reverse =  new Array();
				propertyCodes=li.attr('propertyCode').split('||');
				var parent = li;
				for(var i=1;i<propertyCodes.length;i++)  {
					parent = parent.parent();
					var  targetString = $.trim(parent.prev().text());
					finalString+=targetString.substring(0,targetString.indexOf('[')-1)+".";
				}
				reverse=$.trim(finalString).split(".");
				for(var j=reverse.length-1;j>=0;j--)  {
					if(reverse[j]!="")  {
					reverseString+=reverse[j]+".";
					}
				}
				$('td:first', target).text(reverseString+$.trim(li.text()));
				//$('td:first', target).text(targetString.substring(0,targetString.indexOf('[')-1)+"."+$.trim(li.text()));
			}else {
				$('td:first', target).text($.trim(li.text()));
			}
			
			if(li.attr('propertyCode')!=undefined && li.attr('propertyCode').search('\\|')!=-1)  {
				var  propertyCodes =  new Array();
				var  finalString  = "";
				var  reverseString = "";
				var  reverse =  new Array();
				propertyCodes=li.attr('propertyCode').split('||');
				var parent = li;
				for(var i=1;i<propertyCodes.length;i++)  {
					parent = parent.parent();
					var  targetString =  $.trim(parent.prev().find('span:first').attr("i18n"));
					finalString+=targetString+",";
				}
				reverse=$.trim(finalString).split(",");
				for(var j=reverse.length-1;j>=0;j--)  {
					if(reverse[j]!="")  {
					reverseString+=reverse[j]+",";
					}
				}
				target.removeAttr('namekey').attr('namekey', reverseString+li.attr('namekey'));
				//$('td:first', target).text(targetString.substring(0,targetString.indexOf('[')-1)+"."+$.trim(li.text()));
			}else {
				target.removeAttr('namekey').attr('namekey', li.attr('namekey'));
			}
			
			target.removeAttr('key').attr('key', li.attr('name')).removeAttr('partDepend').attr('partDepend',li.attr('partDepend')).removeAttr('propertyCode').attr('propertyCode',li.attr('propertyCode')).removeAttr('layRec').attr('layRec', li.attr('layRec')).removeAttr('name').removeAttr('showType').removeAttr('checkname').removeAttr('columnType').removeAttr('nullable').attr('name', li.attr('name')).attr('checkname',$.trim(li.text())).attr('nullable',li.attr('nullable')).removeAttr('mnecode').attr('mnecode', li.attr('mnecode'))
			.attr('columnType', li.attr('columnType')).removeAttr('selfType').attr('selfType', li.attr('columnType')).removeAttr('funcname').removeAttr('funcbody').removeAttr('sourcepropertyname').removeAttr('multable').attr('multable', li.attr('multable')).removeAttr('seniorsystemcode').attr('seniorsystemcode', li.attr('seniorsystemcode')).removeAttr('modelcode').attr('modelcode', li.attr('modelcode')).removeAttr('propDefaultValue').attr('propDefaultValue',li.attr('propDefaultValue')).removeAttr('defaultValue').attr('defaultValue',li.attr('propDefaultValue')).removeAttr('defaultValueHasChanged').attr('defaultValueHasChanged',false)
			.removeAttr('propShowFormat').attr('propShowFormat',li.attr('propShowFormat')).removeAttr('propShowType').attr('propShowType',li.attr('propShowType')).removeAttr('showTypeHasChanged').attr('showTypeHasChanged',false).removeAttr('showFormatHasChanged').attr('showFormatHasChanged',false).attr('showFormat',li.attr('propShowFormat')).attr('showType',li.attr('propShowType')).removeAttr('columnName').attr('columnName', li.attr('columnname'));
			target.removeAttr('modelDataType').attr('modelDataType', li.attr('modelDataType'));
			target.removeAttr('propPrecision').attr('propPrecision', li.attr('propPrecision'));
			target.removeAttr('showWidth').attr('showWidth', li.attr('showWidth'));
			target.removeAttr('isCustom').attr('isCustom', li.attr('isCustom'));
			if(columnType == 'TEXT' || columnType == 'LONGTEXT') {
				target.removeAttr('exp').attr('exp','like');
			}else {
				target.removeAttr('exp').attr('exp','equal');
			}
			if(columnType == 'BOOLEAN') {
				target.removeAttr('showType').removeAttr('showFormat').removeAttr('showTypeHasChanged').removeAttr('showFormatHasChanged').attr('showType','SELECT').attr('showFormat','SELECT').attr('showFormatHasChanged','true').attr('showTypeHasChanged','true');
			}
			
			if(li.attr('entityCode')) {
				target.removeAttr('entityCode').attr('entityCode', li.attr('entityCode'));
			}
			if(li.attr('assTar')) {
				target.removeAttr('assTar').attr('assTar', li.attr('assTar')).removeAttr('assOrg').attr('assOrg', li.attr('assOrg'));
			}
			if(li.attr('assPropertyName')) {
				target.removeAttr('assPropertyName').attr('assPropertyName', li.attr('assPropertyName'));
			}
			if(columnType == 'DATETIME' || columnType == 'DATE' || columnType == 'DECIMAL' || columnType == 'MONEY' || columnType == 'INTEGER' || 'LONG' == columnType || columnType == 'SYSTEMCODE' || columnType == 'ENUMERATE' || columnType == 'CHECKBOX' || columnType == 'SELECT' || columnType == 'BOOLEAN') {
			}else{
				if(li.attr('propertyCode')!=undefined && li.attr('propertyCode').search('\\|')!=-1){
					target.removeAttr('showFormat').attr('showFormat','SELECTCOMP');
					target.removeAttr('showType').attr('showType','SELECTCOMP');
					target.removeAttr('showTypeHasChanged').attr('showTypeHasChanged',true);
					target.removeAttr('showFormatHasChanged').attr('showFormatHasChanged',true);
				}
			}
			
			if(null!=li.attr('fillcontent') && "null" != li.attr('fillcontent')){
				// 如果模型的属性配置了系统编码或是自定义枚举的 则自动带入
				target.removeAttr('fill').attr('fill',li.attr('fillcontent'));
				var Tempfill = $.parseJSON(li.attr('fillcontent'));
				var TempfillType = null;
				if(Tempfill==null) {
					TempfillType = '-1';
				} else {
					TempfillType = Tempfill.fillType;
				}
				
				if(parseInt(TempfillType)==4){
					var tempfc=""; 
					target.removeAttr('fillType').attr('fillType',4); 
					$.each(Tempfill.fillContent, function(key, value) { 
						tempfc +=","+key+":"+value; 
					}); 
					tempfc=tempfc.substr(1);// 只取fillcontent的内容，回填到li属性中
					target.removeAttr('fillcontent').attr('fillcontent',tempfc); 
				} 
				if(parseInt(TempfillType)==4 || parseInt(TempfillType)==3){ 
					if(li.attr('columnType') == 'ENUMERATE') {
						showType = "SELECT";
					} else if(li.attr('columnType') == 'SYSTEMCODE') {
						showType ="SELECTCOMP";
					}
				}
			}else {
				target.attr('fill','');
				target.attr('fillcontent','');
			}
			if(columnType == 'DATETIME' || columnType == 'DATE' || columnType == 'DECIMAL' || columnType == 'MONEY' || columnType == 'INTEGER' || 'LONG' == columnType || columnType == 'SYSTEMCODE' || columnType == 'ENUMERATE' || columnType == 'CHECKBOX' || columnType == 'SELECT' || columnType == 'BOOLEAN') {
			}else {
				if(null!=li.attr('modelcode') && "null" != li.attr('modelcode')) {
					try{
						var oResponse = CUI('body').data(li.attr('modelcode'));
						if (oResponse==null ||oResponse == 'null' && oResponse == undefined || oResponse=='') {
							CUI.ajax({
								url: "/msService/ec/view/referenceViews",
								type: 'post',
								async: false,
								data: {"modelCode" : li.attr('modelcode')},
								success: function(msg) {
									CUI('body').data(li.attr('modelcode'), msg);
								}
							});
							oResponse = CUI('body').data(li.attr('modelcode'));
						}
						showType ="SELECTCOMP";
						target.removeAttr('showType').attr('showType', showType); 
						target.removeAttr('referenceview').attr('referenceview',oResponse[0].code); 
					}catch (e){
						// alert("js.ec.referencevliew.setted");
						// target.removeAttr('name').removeAttr('showType').removeAttr('sourcePropertyName').removeAttr('checkname').removeAttr('columnType').removeAttr('nullable').removeAttr('columnLong').removeAttr('isreadonly').removeAttr('fill').removeAttr('exp').removeAttr('funcname').removeAttr('funcbody').removeAttr('fillType').removeAttr('fillContent').removeAttr('propertyCode').removeAttr('assTar').removeAttr('assOrg').removeAttr('exp').removeAttr('callbackbody').removeAttr('callbackname').removeAttr('funcname').removeAttr('funcbody').removeAttr('entityCode').removeAttr('modelcode').removeAttr('decimalNum');
						// return false;
					}
				}
			}
			// 增加原属性的input标示，以便确认是否已经添加过关联实体的属性
			if(li.attr('sourcePropertyName') && li.attr('sourcePropertyName')!=''){
				target.attr('sourcePropertyName', li.attr('sourcePropertyName'));
			}	
			target.bind('click',function(obj){
				objMe.selectRow('fast',this);
			});
			target.attr('newAdd',"add");
			$('#fastColOrder').append(target);
			li.addClass("dragout");
			var select = $('.sort-selected','#fastColOrder');
			var pre_index=CUI(select).prev().index();
			var next_index=CUI(select).next().index();
			if(pre_index==-1){
				objMe.disableUPBtn('fast',1);
			}else{
				objMe.disableUPBtn('fast',0);
			}	
			if(next_index==-1){
				objMe.disableDownBtn('fast',1);
			}else{
				objMe.disableDownBtn('fast',0);
			}	
			objMe.addSortStyle('fast');
		}
	};
	CUI.ExportConfList.prototype._build_model_export_queryXml = function () {
		var xmlStr = '<?xml version="1.0" encoding="UTF-8"?>';
		xmlStr += '<list>';
		var xmlStrNullAble = '';
		$('tr', '#fastColOrder').each(function () {
			if ($.trim($('td', this).html()) != '') {
				// 组织li中的元素
				var tempNum = 1;
				for (var i = 0; i < tempNum; i++) {

					//上面注释的代码把必填字段放到excel最前面
					var name = $(this).attr('name');

					if ((name == 'ownerStaff.name' && xmlStr.indexOf("ownerStaff.name") != -1) || (name == 'ownerPosition.name' && xmlStr.indexOf("ownerPosition.name") != -1)) {
						return true;
					}
					xmlStr += '<list-item>';
					xmlStr += '<name><![CDATA[' + name + ']]></name>';
					if ($(this).attr('namekey')) {
						xmlStr += '<dispalyName><![CDATA[' + $(this).attr("namekey") + ']]></dispalyName>';
					}
					if ($(this).attr('propertyCode')) {
						xmlStr += '<propertyCode><![CDATA[' + $(this).attr("propertyCode") + ']]></propertyCode>';
					}
					if ($(this).attr('namekey')) {
						xmlStr += '<namekey><![CDATA[' + $(this).attr("namekey") + ']]></namekey>';
					}

					if ($(this).attr('multable')) {
						xmlStr += '<multable><![CDATA[' + $(this).attr("multable") + ']]></multable>';
					}
					if ($(this).attr('seniorsystemcode')) {
						xmlStr += '<seniorsystemcode><![CDATA[' + $(this).attr("seniorsystemcode") + ']]></seniorsystemcode>';
					}
					if ($(this).attr('propshowformat')) {
						xmlStr += '<propshowformat><![CDATA[' + $(this).attr("propshowformat") + ']]></propshowformat>';
					}
					if ($(this).attr('propPrecision')) {
						xmlStr += '<propPrecision><![CDATA[' + $(this).attr("propPrecision") + ']]></propPrecision>';
					}
					if ($(this).attr('containLower') && $(this).attr('containLower') != '') {
						xmlStr += '<containLower><![CDATA[' + $(this).attr('containLower') + ']]></containLower>';
					}
					if ($(this).attr('caseSensitive') && $(this).attr('caseSensitive') != '') {
						xmlStr += '<caseSensitive><![CDATA[' + $(this).attr('caseSensitive') + ']]></caseSensitive>';
					}
					if ($(this).attr('assPropertyName')) {
						xmlStr += '<assPropertyName><![CDATA[' + $(this).attr("assPropertyName") + ']]></assPropertyName>';
					}
					if ($(this).attr('modelCode')) {
						xmlStr += '<modelCode><![CDATA[' + $(this).attr("modelCode") + ']]></modelCode>';
					}
					if ($(this).attr('columnName')) {
						xmlStr += '<columnName><![CDATA[' + $(this).attr("columnName") + ']]></columnName>';
					}
					if ($(this).attr('layRec')) {
						xmlStr += '<layRec><![CDATA[' + $(this).attr("layRec") + ']]></layRec>';
					}
					if ($(this).attr('key')) {
						xmlStr += '<key><![CDATA[' + $(this).attr("key") + ']]></key>';
					}
					if ($(this).attr('showType')) {
						xmlStr += '<showType><![CDATA[' + $(this).attr("showType") + ']]></showType>';
					}
					if ($(this).attr('nullable')) {
						xmlStr += '<nullable><![CDATA[' + $(this).attr("nullable") + ']]></nullable>';
					}
					if ($(this).attr('columntype')) {
						xmlStr += '<columntype><![CDATA[' + $(this).attr("columntype") + ']]></columntype>';
					}
					if ($(this).attr('showWidth')) {
						xmlStr += '<showWidth><![CDATA[' + $(this).attr("showWidth") + ']]></showWidth>';
					}
					if ($(this).attr('isCustom')) {
						xmlStr += '<isCustom><![CDATA[' + $(this).attr("isCustom") + ']]></isCustom>';
					}
					if ($(this).attr('customPropImportCode')) {
						xmlStr += '<customPropImportCode><![CDATA[' + $(this).attr("customPropImportCode") + ']]></customPropImportCode>';
					}
					xmlStr += '</list-item>';
				}
			}
		});

		xmlStr += '</list>';// section的
		return xmlStr;
	};
	
	/**
	 * 显示导出关联属性
	 * 
	 * @returns
	 */
	CUI.ExportConfList.prototype.showExportAssPropertyNew = function(obj,modelCode) {
		if(CUI(obj).attr('hasclick')!='true'&&CUI(obj).attr('hasclick')!=true){
			// 第一次点击展开进入的时候，通过ajax查出属性的关联实体的属性
			try{
	    		var oResponse;
	    		CUI.ajax({
	    			url: "/msService/ec/import/select_subs",
	    			type: 'post',
	    			async: false,
	    			data: {
	    				"model.code" : modelCode,
	    				"type" : $(obj).attr('sourceType'),
	    				"isProj":"${isProj?string('true', 'false')}"
	    			},
	    			success: function(jsonResponse) {
	    				oResponse = jsonResponse;
	    				if(oResponse == "undefined") {
	    					alert("${getText('js.ec.corresponding.attribute')}");
	    					return false;
	    				}
	    				if(window.closeLoadPanel){closeLoadPanel();}
	    			}
	    		});
	    		try{this.createExportAsspropertyNode(obj,oResponse);}catch(e){}
	    		CUI(obj).attr('hasclick', true);
	    		CUI(obj).attr('flag', false);
	    		obj.src="/bap/static/treeview/assets/ectree_expand.gif";
			}catch (e){
				return false;
			}
		}else{
			if(CUI(obj).attr('flag')=='true' || CUI(obj).attr('flag')==true){
				obj.src="/bap/static/treeview/assets/ectree_expand.gif";
				$(obj).parent().next().show();
	    		CUI(obj).attr('flag', false);
			}else{
				obj.src="/bap/static/treeview/assets/ectree_colse.gif";
				$(obj).parent().next().hide();
	    		CUI(obj).attr('flag', true);
			}
		}
	};

	CUI.ExportConfList.prototype.appendSelectedField = function (data, appendtype) {
		var $elSelectedFieldTbody = $('#fastColOrder');
		var orginalLength = $elSelectedFieldTbody.find("tr").length;
		var className;

		if (orginalLength % 2 == 1) {
			className = "odd-num";
		} else {
			className = "even-num";
		}
		for (var i = 0; i < data.length; i++) {
			var flag = false;
			$('tr', $elSelectedFieldTbody).each(function () {
				if ($.trim($('td', this).html()) != '') {
					if ($(this).attr('propertyCode') == data[i].propertyCode) {//检查查询出来的值是否已经在左侧div中
						flag = true;
						if (data[i].customPropImportCode != "" && typeof (data[i].customPropImportCode) != "undefined") {
							$(this).remove();
							flag = false;
						}
						return flag;
					}
				}
			});
			if (flag) {
				continue;
			}
			var appendTr = '<tr appendtype="' + appendtype + '" class=' + className + ' columnname="' + data[i].columnName + '" key="' + data[i].key + '" ondblclick="listec.selectRow(\'fast\',this);CUI.ExportConfList.prototype.setFieldDisplayName(this)"   onmousedown="listec.selectRow(\'fast\',this);"  cellcode="' + data[i].cellcode + '" columntype="' + data[i].columnType + '" propertycode="' + data[i].propertyCode + '" nullable="' + data[i].nullAble + '"' + '" isCustom="' + data[i].isCustom + '"' + '" customPropImportCode="' + data[i].customPropImportCode + '"';
			var appendTr2 = ' propPrecision="' + data[i].propPrecision + '" propShowFormat="' + data[i].propShowFormat + '" multable="' + data[i].multable + '" seniorsystemcode="' + data[i].seniorsystemcode + '"  namekey="' + data[i].displayName + '"   name="' + data[i].name + '"  layrec="' + data[i].layRec + '" > ';
			var appendTd = '<td class="field-name"><span i18n="' + data[i].displayName + '">' + data[i].displayText + '</span></td>';
			if(data[i].nullAble == "false"){
				var appendTd = '<td class="field-name field-nullable"><span i18n="' + data[i].displayName + '">' + data[i].displayText + '</span></td>';
			}
			var imageTd = '<td class="field-action" onclick="listec.deleteFastQueryField(this)"  del_td="true"><img title="点击删除该字段" style="cursor:pointer;" src="/bap/static/ec/images/importTemplate/icon_delete.png" onMouseOver=\'deleteBtnChange(this)\' onMouseOut=\'deleteBtnChange(this)\'></img></td>';
			var endTr = '</tr>';
			var total = $(appendTr + appendTr2 + appendTd + imageTd + endTr);

			$elSelectedFieldTbody.append(total);

			$('td[del_td="true"]', total).bind('mousedown', function (e) {
				e.stopPropagation();
			});

			if (className == "even-num") {
				className = "odd-num";
			} else {
				className = "even-num";
			}
		}
	};

	CUI.ExportConfList.prototype.createEntityPropertyList = function (data) {

		var elAllField = $('#export-config-field-list-container');

		var list = [];

		$.each(data.properties, function (i, v) {
			var t = v.type,
				p = v,
				n = v.name;
			var ext = ['LONGTEXT', 'OFFICE', 'PROPERTYATTACHMENT'];
			var exn = ['tableInfoId', 'status', 'id', 'version', 'layNo', 'layRec'];

			if (ext.indexOf(t) === -1 && exn.indexOf(n) === -1) {
				var modelDataType = 'simple';
				if (v.model.dataType == 2) {
					modelDataType = 'tree';
				}

				list.push("<li source='main'");
				list.push(' modelDataType="' + modelDataType + '"');
				list.push(" onclick='listec.addExportField(this)'");
				list.push(" partDepend='common'");
				list.push(" propDefaultValue='" + v.defaultValue + "'");
				list.push(" propertyCode='" + v.code + "'");
				list.push(" namekey='" + p.displayName + "'");
				list.push(" name='" + p.name + "'");
				list.push(" columnType='" + p.type + "'");
				list.push(" columnName='" + p.columnName + "'");
				list.push(" propShowFormat='" + p.format + "'");
				list.push(" propShowType='" + p.fieldType + "'");
				list.push(" entityCode='" + p.model.code + "'");
				list.push(" multable='" + p.multable + "'");
				list.push(" seniorSystemCode='" + p.seniorSystemCode + "'");
				list.push(" showWidth='" + p.showWidth + "'");
				list.push(" nullable='" + p.nullable + "'");
				list.push(" list='" + (p.isUsedForList ? 'true' : 'false') + "'");
				list.push(" fillContent='" + p.fillcontent + "'");
				list.push(" layRec='" + p.name + "'");
				list.push(" mnecode='" + p.isUsedMneCode + "'");
				list.push(" propPrecision='" + p.decimalNum + "'");
				list.push(" isCustom='" + p.isCustom + "'");
				// list.push(" ");
				list.push(">");
				list.push(v.displayNameInternational);
				list.push('</li>');
			}

		})

		$.each(data.associatedInfos, function (i, v) {
			if (v.originalProperty && v.originalProperty.name != 'status') {
				var parentModelDataType = 'simple';
				var assTar = v.targetProperty.code;
				var assPropertyName = v.targetProperty.name;
				var propertyCode = v.originalProperty.code,
					assOrg = v.originalProperty.code,
					dbname = v.targetProperty.model.modelName,
					xname = v.originalProperty.name,
					multable = v.targetProperty.multable,
					seniorSystemCode = v.targetProperty.seniorSystemCode,
					entityCode = v.targetProperty.model.code,
					displayName = v.originalProperty.displayNameInternational,
					relativeName = v.targetProperty.model.tableName + ',' + v.targetProperty.columnName + ',' + v.originalProperty.model.tableName + ',' + v.originalProperty.columnName,
					modelName = v.targetProperty.model.nameInternational;

				if (v.targetProperty.model.dataType && v.targetProperty.model.dataType == 2) {
					parentModelDataType = 'tree';
				}

				list.push("<li source='test'");
				list.push(" parentModelDataType='" + parentModelDataType + "'");
				list.push(" partDepend='common'");
				list.push(" assTar='" + assTar + "'");
				list.push(" assPropertyName='" + assPropertyName + "'");
				list.push(" propertyCode='" + propertyCode + "'");
				list.push(" assOrg='" + assOrg + "'");
				list.push(" dbname='" + dbname + "'");
				list.push(" name='" + xname + "'");
				list.push(" multable='" + multable + "'");
				list.push(" seniorSystemCode='" + seniorSystemCode + "'");
				list.push(" entityCode='" + entityCode + "'");
				list.push(" relativeName='" + relativeName + "'>");
				list.push("<img sourceType='fast' align='absmiddle' src='/bap/static/treeview/assets/ectree_colse.gif' flag='true' hasclick='false'");
				list.push(" onclick='listec.showExportAssPropertyNew(this,\"" + entityCode + "\")'");
				list.push(">");
				list.push(displayName + '[' + modelName + ']');
				list.push('</li>')

			}

		})
		$(elAllField).html(list.join(''));
	};
</script>


<style>
#export-model-wrap {
		position: relative;
		height: 100%;
		overflow: hidden;
		background-color: #dbe8f0;
	}

	#export-model-action {
		height: 33px;
		margin-top: 15px;
		margin-left: 8px;
	}
	
	#export-config-buttons {
		height: 30px;
		background-color: #c7d6e0;
	}

    #export-config-buttons .action-btn {
        margin-left: 8px;
        cursor: pointer;
        margin-top: 6px;
        display: inline-block;
    }

    #export-config-buttons .action-btn img {
        margin-right: 4px;
        vertical-align: bottom;
    }

    .action-title {
        margin-left: 8px;
        font-size: 15px;
        color: #156698;
        display: inline-block;
        padding-right: 18px;
        line-height: 1;
    }
	#export-config-buttons -btn {
		margin-left: 8px;
		cursor: pointer;
		margin-top: 6px;
    	display: inline-block;
	}

	#export-config-buttons -btn img {
		margin-right: 4px;
		vertical-align: bottom;
	}

	-title {
		margin-left: 8px;
		font-size: 15px;
		color: #156698;
		display: inline-block;
		padding-right: 18px;
		line-height: 1;
	}

	#export-model-action .cui-btn {
		border: none;
		margin-left: 14px;
	}

	#export-model-list {
		position: absolute;
		left: 9px;
		width: 232px;
		bottom: 9px;
		top: 48px;
		background: #fff;
		box-sizing: border-box;
	}

	#export-model-list #ec_model_Tree {
		box-sizing: border-box;
		height: 100% !important;
		/* border: 1px solid #C6D4E1; */
	}

	#export-model-field-config-wrap {
		position: absolute;
		left: 246px;
		top: 48px;
		bottom: 9px;
		right: 9px;
		box-sizing: border-box;
	}

	#export-model-field-config {
		position: absolute;
		box-sizing: border-box;
		top: 0px;
		left: 0;
		bottom: 0;
		right: 0;
	}

	#export-config-field-list {
		box-sizing: border-box;
		background-color: #fff;
		position: absolute;
		left: 0px;
		top: 30px;
		bottom: 0px;
		width: 253px;
	}

	#export-config-field-list #fast_select_elements {
		height: 100%;
		position: relative;
		width: 253px;
		background-color: white;
		box-sizing: border-box;
	}

	#export-config-field-list #fast_select_elements h2 {
		padding: 7px 0;
		text-align: center;
		color: #5d788a;
		font-size: 13px;
		font-weight: normal;
		cursor: pointer;
		background-color: #fff;
		height: 17px;
	}

	#export-config-field-list #fast_select_elements .accordion_pane {
		overflow: auto;
		cursor: pointer;
		border-width: 0 2px;
		padding: 0px;
		font-size: 12px;
		position: absolute;
		top: 31px;
		left: 0;
		right: 0;
		bottom: 0;
		border-top: 1px solid #dbe8f0;
	}

	#export-config-field-list #fast_select_elements .main_properties_container {
		padding-left: 5px;
	}

	#export-config-field-list #fast_select_elements .main_properties_container li {
		font-size: 12px;
		color: #000000;
		cursor: pointer;
		line-height: 18px;
		z-index: 100;
		margin-left: 4px;
	}

	#export-config-field-list #fast_select_elements .main_properties_container li.dragout {
		color: #BBBBBB;
		cursor: default;
	}

	#export-config-field-enabled-wrap {
		box-sizing: border-box;
		position: absolute;
		left: 255px;
		top: 30px;
		right: 0px;
		bottom: 0px;
	}

	#export-config-field-enabled {
		box-sizing: border-box;
		position: relative;
		height: 100%;
	}

	#export-config-field-enabled h2 {
		box-sizing: border-box;
		height: 31px;
		text-align: center;
		padding: 7px;
		font-size: 13px;
		font-weight: normal;
		cursor: pointer;
		background-color: #fff;
		color: #5d788a;
	}

	#export-config-field-enabled #checkAll {
		height: 36px;
		background-color: #f3f3f3;
		color: #7d7d7d;
		font-size: 13px;
		line-height: 36px;
		padding-left: 21px;
	}

	#export-config-field-enabled #checkAll #isCheckAll {
		vertical-align: middle;
	}

	#export-config-field-enabled-content {
		position: absolute;
		top: 67px;
		left: 0;
		right: 0;
		bottom: 0;
		background-color: #fff;
	}

	#export-config-field-enabled-content::after {
		clear: both;
		display: table;
		content: ' ';
		visibility: hidden;
		height: 0;
	}

	#content-fluid-table {
		overflow-y: auto;
		position: absolute;
		right: 121px;
		left: 0;
		top: 0;
		bottom: 0;
	}

	#content-fluid-table table {
		border-collapse: collapse;
		width: 99%;
	}

	#content-fluid-table tr {
		height: 36px;
		border-bottom: 1px solid #dfe8ee;
		padding-left: 21px;
		padding-right: 26px;
	}

	#content-fluid-table tr .field-name,
	#content-fluid-table tr td:first-child {
		padding-left: 21px;
		text-align: left;
		width: 99%;
	}

	#content-fluid-table tr .field-nullable {
		color:red;
	}

	#content-fluid-table tr .field-action {
		padding-right: 26px;
		width: 16px;
	}

	#content-fluid-table tr.even-num {
		background-color: #fff;
	}

	#content-fluid-table tr.sort-selected {
		background-color: #f9f0c5;
	}

	#content-fixed-sort {
		position: absolute;
		right: 0;
		top: 50%;
		height: 216px;
		margin-top: -108px;
		width: 70px;
		background: url("/bap/static/ec/images/importTemplate/btn_bg.png");
	}

	#fastContent {
		width: 100%;
		height: 100%;
		position: relative;
	}

	#fastContent div {
		position: relative;
		left: 15px;
		width: 40px;
		display: inline-block;
		height: 38px;
		text-align: center;
		cursor: pointer;
		margin-top: 12px;
	}

	.ec-list-topbtn {
		background-image: url("/bap/static/ec/images/importTemplate/btn_top.png");
	}

	.ec-list-dis-topbtn {
		background-image: url("/bap/static/ec/images/importTemplate/btn_top_no.png");
	}

	.ec-list-prevbtn {
		background: url("/bap/static/ec/images/importTemplate/btn_up.png");
	}

	.ec-list-dis-prevbtn {
		background-image: url("/bap/static/ec/images/importTemplate/btn_up_no.png");
	}	

	.ec-list-nextbtn {
		background: url("/bap/static/ec/images/importTemplate/btn_down.png");
	}

	.ec-list-dis-nextbtn {
		background: url("/bap/static/ec/images/importTemplate/btn_down_no.png");
	}

	.ec-list-lastbtn {
		background: url("/bap/static/ec/images/importTemplate/btn_bottom.png");
	}

	.ec-list-dis-lastbtn {
		background: url("/bap/static/ec/images/importTemplate/btn_bottom_no.png");
	}	
</style>

<div id="export-model-wrap">
	<div id="export-model-action" height="24">
		<span class="action-title">
			Excel导入模板配置
		</span>
	</div>

	<div id="export-model-list">
		<@tree id="ec_model_Tree" nameCol="nameInternational" dataUrl="/msService/ec/model/list?entity.code="+entity.code rootName="${getText('ec.model.list')}"
			callback="{beforeClick:function(){ec.model.exportObj.setLastNode();},onClick:function(event,treeId,node){ec.model.exportObj.getModelField(node);}}" />
	</div>

	<div id="export-model-field-config-wrap">
		<div id="export-model-field-config">

		</div>
	</div>
</div>

<div id="main_form_tab" class="etv-navset" style="display:none">
	<ul class="etv-nav">
		<li class="selected">
			<span i18n='ec.view.selectd'></span>
		</li>
	</ul>
	<div style="background-color:#e3f1f9;" class="etv-content">
	</div>
</div>

<script>

	function deleteBtnChange(obj) {
		if($(obj).attr('src').indexOf('/bap/static/ec/images/importTemplate/icon_delete.png') > -1) {
			$(obj).attr('src','/bap/static/ec/images/importTemplate/icon_delete_p.png');
		} else {
			$(obj).attr('src','/bap/static/ec/images/importTemplate/icon_delete.png');
		}
	}
	
	CUI.ns('ec.model.exportObj');

	var listec,
		main_tab;
	$(function () {

		main_tab = new CUI.TabView("main_form_tab", { tabposition: 'top', removable: true });//初始化主设计区域页签
		listec = new CUI.ExportConfList(
			'bap_export_config',
			{ pageConfig: { colNum: 6 } },
			main_tab,
			'',
			'0',
			'',
			'0',
			{
				'singlSelect': { 'SELECT': '下拉单选' },
				'multiSelect': { 'CHECKBOX': '多选框', 'MULTSELECT': 'ec.view.multselect' },
				'objectSelect': { 'TEXTFIELD': '普通文本', 'SELECTCOMP': '选择控件' },
				'normalSelect': {},
				'viewColumnTypes': {},
				'viewFormats': {}
			},
			{
				'equalExp': { 'equal': '等于', 'unequal': '不等于' },
				'likeExp': { 'equal': '模糊匹配' },
				'textExp': { 'equal': '等于', 'like': '模糊匹配', 'llike': '左匹配', 'rlike': '右匹配' }
			}
		);

	});

	// 请求全部,已选择字段页面
	ec.model.exportObj.getModelFieldConfPage = function (code) {
		var url = "/msService/ec/import/propertyList?isProj=${isProj?string('true', 'false')}";
		if (code) {
			url += '&model.code=' + code;
		}

		$('#export-model-field-config').load(url, null, function () {
			try {
				closeLoadPanel();
				listec.fastFieldColumnTroggle();
				keys = ec.model.exportObj.listKeys();
			} catch (e) {
			}
		});
	};
	
	ec.model.exportObj.listKeys = function () {
		var keyList = []; // 获取现在配置
		$('tr', '#fastColOrder').each(function () {
			if ($.trim($('td', this).html()) != '') {
				var key = $(this).attr('key');
				keyList.push(key);
			}
		});
		return keyList;
	}
	
	var lastNode; //上一个节点
	var keys = []; // 上一个节点已保存配置
	ec.model.exportObj.setLastNode = function () {
		lastNode = $.fn.zTree.getZTreeObj("ec_model_Tree").getSelectedNodes()[0];
	}
	
	ec.model.exportObj.isModified = function () {
		var isModified = false;
		var keysNow = ec.model.exportObj.listKeys();
		return (keysNow.toString()==keys.toString())?false:true;
	}

	// 选择模型
	ec.model.exportObj.getModelField = function (node) {
		var isModified = ec.model.exportObj.isModified();
		if (isModified) {
			CUI.Dialog.confirm(
			    '当前模板配置未保存，是否继续？',  // 提示消息
			    function(){ec.model.exportObj.getModelFieldConfPage(node.code);},
			    function(){$.fn.zTree.getZTreeObj("ec_model_Tree").selectNode(lastNode);}
			);
		} else {
			ec.model.exportObj.getModelFieldConfPage(node.code);
		}
	};

	// 保存
	ec.model.exportObj.save = function () {
		var treeSelectedNode=$.fn.zTree.getZTreeObj("ec_model_Tree").getSelectedNodes()[0];
		var modelCode=null;
		if(treeSelectedNode!=null&&treeSelectedNode.level!=0){
			modelCode=treeSelectedNode.code;
			var queryConfig = listec._build_model_export_queryXml();
			$.ajax({
				url: '/msService/ec/import/save?isProj=${isProj?string('true', 'false')}&model.code='+modelCode,
				type: 'POST',
				data: { queryConfig: queryConfig },
				success: function (res) {
					CUI.Dialog.alert('保存成功');
					ec.model.exportObj.getModelFieldConfPage(modelCode);
				}
			})
		}else{
			CUI.Dialog.alert('未选择要保存的模型！');
		}
	};
	
	// 还原
	ec.model.exportObj.restore = function () {
		var treeSelectedNode=$.fn.zTree.getZTreeObj("ec_model_Tree").getSelectedNodes()[0];
		var modelCode=null;
		if(treeSelectedNode!=null&&treeSelectedNode.level!=0){
			modelCode=treeSelectedNode.code;
			var queryConfig = listec._build_model_export_queryXml();
			$.ajax({
				url: '/msService/ec/import/restore?isProj=${isProj?string('true', 'false')}&model.code='+modelCode,
				type: 'POST',
				success: function (res) {
					CUI.Dialog.alert('还原成功');
					ec.model.exportObj.getModelFieldConfPage(modelCode);
				}
			})
		}else{
			CUI.Dialog.alert('未选择要保存的模型！');
		}
	};
	
	// 发布
	ec.model.exportObj.publish = function () {
		var treeSelectedNode=$.fn.zTree.getZTreeObj("ec_model_Tree").getSelectedNodes()[0];
		var modelCode=null;
		if(treeSelectedNode!=null&&treeSelectedNode.level!=0){
			modelCode=treeSelectedNode.code;
			var queryConfig = listec._build_model_export_queryXml();
			$.ajax({
				url: '/msService/ec/import/publish?isProj=${isProj?string('true', 'false')}&model.code='+modelCode,
				type: 'POST',
				data: { queryConfig: queryConfig },
				success: function (res) {
					CUI.Dialog.alert('发布成功');
					ec.model.exportObj.getModelFieldConfPage(modelCode);
				}
			})
		}else{
			CUI.Dialog.alert('未选择要发布的模型！');
		}
	};
	
	ec.model.exportObj.checkAllItemOfEntity = function (code) {
		if(code!=''){
			var checkbox = $('#isCheckAll');
			var checked = checkbox.prop('checked');
		
			if (checked) {
				ec.model.exportObj.loadingWidget = new CUI.loading({
					head:"正在操作中..." ,
					container:$('#ecContentDiv')[0],  
					opacity:50,  
					bgColor:"#666666", 
					show:true,
					paddingLeft:250
				});
				$.ajax({
					url: '/msService/ec/import/getAllField?isProj=${isProj?string('true', 'false')}&model.code='+code,
					success: function (data) {
						listec.appendSelectedField(data.data, 'fromcheckbox');
						listec.fastFieldColumnTroggle();
						listec.refreshSelectBtnStatus('fast');
						ec.model.exportObj.loadingWidget.close();
					}
				});
			} else {
				$("#fastColOrder").find("tr[appendtype='fromcheckbox']").remove();
				listec.refreshSelectBtnStatus('fast');
			}
			listec.fastFieldColumnTroggle();
		}
	};
	

	$(function () {
		// 首次默认加载
		ec.model.exportObj.getModelFieldConfPage();
	});
</script>