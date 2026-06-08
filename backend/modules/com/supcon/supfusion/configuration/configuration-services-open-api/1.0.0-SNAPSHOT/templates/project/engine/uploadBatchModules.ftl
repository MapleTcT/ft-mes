<!DOCTYPE html>
<html  xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="X-UA-Compatible" content="IE=edge" />
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<@maincss />
<@mainjs />

<title>${getText('ec.module.plsz.title')}</title>
</head>
<body id="entityList_page">
<script type="text/javascript">
	function beforesubmit(){
		//组织json数据
		var jsonstr = '';
		var isSubmit = false;
		var versionMessage ='';
		var count = 0 ;
		$('#ec_modules_uploadbatch_datatable_tbody tr').each(function(index,obj){
		    if($(obj).find('td[key = "checkbox"]').attr('truevalue')=="true" && count<500){
		    //这边是组织传入后台的json的数据
		        jsonstr+= ",{"; 
		        var jsonTemp = "";
		        $(obj).find('td,input').each(function(i,o){
		            if($(o).is('td')==true){
                        jsonTemp+=",";
                        jsonTemp+= $(o).attr('key');
                        jsonTemp+=":'";
                        jsonTemp+=$(o).attr('truevalue');
                        jsonTemp+="'"; 
		            }else{
		                jsonTemp+=",";
                        jsonTemp+= $(o).attr('name');
                        jsonTemp+=":'";
                        jsonTemp+=$(o).val();
                        jsonTemp+="'"; 
		            }
		        })
		        jsonstr+=jsonTemp.substr(1);
		        jsonstr+= "}"; 
		        //这里开始检验模块包的新老版本并进行提示
		       var oldVersion =  $(obj).find(" td[key='oldVersion']").attr("truevalue");
		       var curVersion =  $(obj).find(" td[key='curVersion']").attr("truevalue");
		       var moduleName =  $(obj).find(" td[key='moduleName']").attr("truevalue");
		       var isFirstImport =  $(obj).find(" input[name='isFirstImport']").val();
		       var reg = /[^0-9]/ig;
		       if(isFirstImport == "false" && oldVersion != null && curVersion != null && oldVersion!=curVersion){
			       oldVersion = oldVersion.replace(reg,'');
				   curVersion = curVersion.replace(reg,'');
				   if(curVersion>oldVersion){
				   		versionMessage += "、"+moduleName;
				   }
		       }
		       count++;
		    }
		});
		//没有选择模块的时候提示选择
		if(count == 0){
			CUI.Dialog.alert('${getText("ec.module.plsz.qxzyyszdmk")}');
			return;
		}
		if(versionMessage != ''){
		    versionMessage += '上载的模块版本低于当前的模块版本，请确认是否继续？';
		    var dialog = CUI.Dialog.confirm(  
            versionMessage.substring(1),  // 提示消息  
            function(){
        		//$('#uploadBatch').attr('onSubmit','');
            	jsonstr="["+jsonstr.substr(1)+"]";
				$('#uploadBatch input[name="batchUploadJson"]').val(jsonstr);
				$.ajax({
					type : 'post',
					data : {"batchUploadJson": jsonstr},
					url : '/msService/ec/module/uploadBatchBeforeSubmit',
					async:false, 
					success : function(res){
						if(undefined != res.exceptionMsg && res.exceptionMsg != ""){
							CUI.Dialog.alert(res.exceptionMsg)
						}else{
							$('#uploadBatch').attr('onSubmit','')
							var view3 = parent.ec.module.uploadWizardDialogBatch.getView( 'wizar_iframe_batch_step3' );	
							view3.show();
							view3.load();
							parent.wizar_iframe_batch_step2.uploadBatch.target = "wizar_iframe_batch_step3";
							parent.wizar_iframe_batch_step2.uploadBatch.submit();
						}
					},
					error:function(res){
						CUI.Dialog.alert('${getText("ec.module.plsz.error")}')
					}
				});
            }, // 确定后事件  
            function(){
            }, // 取消后事件  
            '${getText("foundation.inter.sz")}',  // 标题  
            70,  // 高度, 可选, 默认70  
            400  // 宽度, 可选, 默认400  
        ); 
	}else{
    	jsonstr="["+jsonstr.substr(1)+"]";
		$('#uploadBatch input[name="batchUploadJson"]').val(jsonstr);
		$.ajax({
			type: 'post',
			data : {"batchUploadJson": jsonstr},
			url : '/msService/ec/module/uploadBatchBeforeSubmit',
			async:false, 
			success : function(res){
				if(undefined != res.exceptionMsg && res.exceptionMsg != ""){
					CUI.Dialog.alert('<div style="max-height: 200px;overflow-y: auto;"><div>'+res.exceptionMsg+'</div></div>')
				}else{
					$('#uploadBatch').attr('onSubmit','')
					var view3 = parent.ec.module.uploadWizardDialogBatch.getView( 'wizar_iframe_batch_step3' );	
					view3.show();
					view3.load();
					parent.wizar_iframe_batch_step2.uploadBatch.target = "wizar_iframe_batch_step3";
					parent.wizar_iframe_batch_step2.uploadBatch.submit();
				}
			},
			error:function(res){
				CUI.Dialog.alert('${getText("ec.module.plsz.error")}')
			}
		});
	}
	return false;
}	
	
</script>
<form id="uploadBatch" onSubmit="javascript:return beforesubmit();" action="/msService/ec/module/uploadBatchProcess" method="post" enctype="multipart/form-data">
<input type="hidden" name="errorMsg" <#if errorMsg??>value="${errorMsg}"</#if> id="errorMsg"/>
<input type="hidden" name="filePath" <#if filePath??>value="${filePath}"</#if> id="filePath"/>
<input type="hidden" name="batchUploadJson" value="" id="batchUploadJson"/>
<input type="hidden" name="relationsCh" value="" id="relationsCh"/>
<@datatable paginator=false withoutConfigTable=true firstLoad=true hidekey="['code','version','errorMsg','isFirstImport','uploadFileName','relationsInternation']" id="ec_modules_uploadbatch_datatable" style="margin:-35px 0px 0px 0px;" dataUrl="/msService/ec/engine/uploadBatchModuleQuery?filePath=${(filePath)!}" pageInitMethod="ec_modules_uploadbatch_datatablepageInitMethod" renderOverEvent="ec_modules_uploadbatch_datatableRenderOverEvent" >
			<@datacolumn key="checkbox" type="checkbox" checkall="true" textalign="center" label="" width=30 />
			<@datacolumn key="category" label="${getHtmlText('ec.module.mkfl')}" width=100 rowMerge=true sortable=false />
			<@datacolumn key="moduleCode" label="${getHtmlText('ec.module.mkbm')}" sortable=false width=100 />
			<@datacolumn key="moduleName" label="${getHtmlText('ec.module.modulename')}" sortable=false width=100 />
			<@datacolumn key="oldVersion" label="${getHtmlText('ec.module.szbb')}" sortable=false width=60 />
			<@datacolumn key="curVersion" label="${getHtmlText('ec.module.dqbb')}" sortable=false width=60 />
			<@datacolumn key="relations" label="${getHtmlText('ec.module.ylmk')}" sortable=false width=100 />
			<@datacolumn key="references" label="${getHtmlText('ec.view.referenceModule')}" hiddenCol=true sortable=false width=100 />
			<@datacolumn textalign="center" type="checkbox" key="isMetadata" label ="${getText('ec.module.ysj')}" width=70  sortable=false />
			<@datacolumn textalign="center" type="checkbox" key="isCustomcode"  label="${getHtmlText('ec.module.zdydm')}" width = 90 sortable=false />
			<@datacolumn textalign="center" type="checkbox" key="isFlow"  label="${getHtmlText('ec.module.gzl')}" width=80 sortable=false />
			<@datacolumn textalign="center" type="checkbox" key="isImportTemplate"  label ="${getText('ec.module.exceldrmb')}" width=100 sortable=false />
			<@datacolumn textalign="center" type="checkbox" key="isUploadschedulerJob"  label ="${getText('ec.module.drrwdd')}" width=100 sortable=false />
</@datatable>
</form>
<script type="text/javascript">
	//0219
	CUI.ns('foundation.uploadBatchModules');
	foundation.uploadBatchModules.selectedRows = [];

	foundation.uploadBatchModules.toggleSelectRowStyle = function(index, selected){
		var pt = ec_modules_uploadbatch_datatableWidget;
		var row = $('#ec_modules_uploadbatch_datatable_tbody tr')[index];
		pt[selected ? '_setSelected' : '_setUnSelected'](row);
	};

	foundation.uploadBatchModules.customToggleSelectRow = function(index, selected){
		var selectedArr = foundation.uploadBatchModules.selectedRows;
		var toggleSelectRow = foundation.uploadBatchModules.toggleSelectRowStyle;
		var isSelected = foundation.uploadBatchModules.getSelectedIndex(index);
		if (selected) {
			if ( isSelected === -1){
				selectedArr.push(index);
				toggleSelectRow(index, true);
			}
		} else {
			if (-1 < isSelected){
				selectedArr.splice(isSelected, 1);
				toggleSelectRow(index, false);
			}
		}
	};

	foundation.uploadBatchModules.getSelectedIndex = function(rowindex){
		return $.inArray(rowindex, foundation.uploadBatchModules.selectedRows);
	};
	
	foundation.uploadBatchModules.triggerCheckboxChangeEvent = function(rowIndex){
		$('#ec_modules_uploadbatch_datatable_tbody tbody tr').eq(rowIndex).find('[name="ec_modules_uploadbatch_datatable_checkall_checkbox"]').trigger('change');
	};

	foundation.uploadBatchModules.customClickTr = function(tr){
		var $tr = $(tr);
		var rowIndex = $tr.index();
		var isSelected = foundation.uploadBatchModules.getSelectedIndex(rowIndex);
		var selectedRows = foundation.uploadBatchModules.selectedRows;
		// 当前行未选中
		if (isSelected === -1) {
			// 已有一条选中行，需取消该行
			if (selectedRows.length === 1){
				var firstIndex = selectedRows[0];
    			foundation.uploadBatchModules.customToggleSelectRow(firstIndex, false);
    			foundation.uploadBatchModules.triggerCheckboxChangeEvent( firstIndex );
			}
			foundation.uploadBatchModules.customToggleSelectRow(rowIndex, true);
			foundation.uploadBatchModules.triggerCheckboxChangeEvent( rowIndex );			
		}
	};

	foundation.uploadBatchModules.customClickCheckbox = function(checkbox){
		var checked = $(checkbox).prop('checked');
		var rowIndex = $(checkbox).closest('tr').index();
		foundation.uploadBatchModules.customToggleSelectRow(rowIndex, checked);	
	};
	
	foundation.uploadBatchModules.changeTrueValue = function(checkbox){
		var checked = $(checkbox).prop('checked');
		$(checkbox).parent().parent().attr('truevalue',checked);
	};
	
	function ec_modules_uploadbatch_datatableRenderOverEvent() {
		$('#ec_modules_uploadbatch_datatable_tbody tbody tr').each(function(index,obj){
			YUE.purgeElement(obj, true , 'click');
		})
		
		// 0219
		// 单独绑定tr click事件
		$('#ec_modules_uploadbatch_datatable_tbody tbody tr').click(function (e) {
			var target = $(e.target);
			if (target.hasClass('dt_checkbox')) {
				if (target.attr('name') === 'ec_modules_uploadbatch_datatable_checkall_checkbox'){
					foundation.uploadBatchModules.customClickCheckbox(target);
				}
				foundation.uploadBatchModules.changeTrueValue(target);
			} else  {
				foundation.uploadBatchModules.customClickTr(this);
			}
		})
			
		//parent.ec.module.loading.close();
		//parent.$('#dialog-iframe-loading').remove()
		$('#ec_modules_uploadbatch_datatable').show(); 
		// 显示表格后需刷新序号
		ec_modules_uploadbatch_datatableWidget.setOrderNumber();
		var errorMsg = ec_modules_uploadbatch_datatableWidget.getAllData()[0].errorMsg;
		//这边是重复返回的代码，到时候放开
		if(errorMsg!=null && errorMsg!=''){
			CUI.Dialog.alert(errorMsg);
			/*$( '#dialog_btn_upload_batch2', parent.ec.module.uploadWizardDialogBatch.dialog._buttonbar ).attr('canclick','false').removeClass().addClass('cui-btn-dialog cui-btn-grey');
			$( '#dialog_btn_upload_close2', parent.ec.module.uploadWizardDialogBatch.dialog._buttonbar ).attr('canclick','false').removeClass().addClass('cui-btn-dialog cui-btn-grey');
			var view1 = parent.ec.module.uploadWizardDialogBatch.getView( 'wizar_iframe_batch_step1' );
			//view1.changeFile.value=false;
			view1.show();
			$('#ec_modules_uploadbatch_datatable').hide();*/
			parent.ec.module.uploadWizardDialogBatch.hide();
			return ;
		}
		$( '#dialog_btn_upload_batch2', parent.ec.module.uploadWizardDialogBatch.dialog._buttonbar ).attr('canclick','true').removeClass().addClass('cui-btn-dialog cui-btn-blue');
		$( '#dialog_btn_upload_close2', parent.ec.module.uploadWizardDialogBatch.dialog._buttonbar ).attr('canclick','true').removeClass().addClass('cui-btn-dialog cui-btn-blue');

		$(ec_modules_uploadbatch_datatableWidget.getAllData()).each(function(index, rowObj) {
	        if(rowObj.isFirstImport == true){
	        	$(rowObj.rowHtmlObj).find('td[celltype="checkbox"]').each(function(i,obj){
	        		if(i>0){
	        			$(obj).find('input').get(0).checked = true;
	        			$(obj).find('input').attr("disabled", "disabled");
	        			$(obj).attr('truevalue','true');
	        		}
	        	})
	        }else{
	        	$(rowObj.rowHtmlObj).find('td[celltype="checkbox"]').each(function(i,obj){
	        		$(obj).find('input').get(0).checked = true;
	        		$(obj).attr('truevalue','true');
	        	})
	        }
	    });

	    ec_modules_uploadbatch_datatableBindChecked();
		
	}
	function ec_modules_uploadbatch_datatablepageInitMethod() {
		$('#ec_modules_uploadbatch_datatable').hide();
		//$('.ewc-dialog-button-right',parent.ec.module.uploadWizardDialogBatch.dialog._buttonbar).hide();
	}
	
	function ec_modules_uploadbatch_datatableBindChecked() {
		
		$('#ec_modules_uploadbatch_datatable_hdbox thead tr td[type="checkbox"]').each(function(index,obj){
			var checkbox = '';
			if($(obj).attr('key')=='isMetadata'){
				checkbox = '<input style="height:13px;top:2px;position:relative;margin-right:1px;" type="checkbox" key="isMetadata" id="ec_modules_uploadbatch_datatable_checkall_id_isMetadata" name="ec_modules_uploadbatch_datatable_checkall_isMetadata" checked="true">${getText("ec.module.ysj")}';
				$(obj).find('.dg-hd-td-label').html('');
				$(obj).find('.dg-hd-td-label').append(checkbox);
				$(obj).find('.dg-hd-td-label input').unbind().bind('click',function(input){
					$('#ec_modules_uploadbatch_datatable_tbody tbody tr').each(function(i,o){
						if($(o).find("input[name='ec_modules_uploadbatch_datatable_checkall_checkbox']")[0].checked == true && !$(o).find("input[name='ec_modules_uploadbatch_datatableisMetadata']")[0].disabled){
							$(o).find("input[name='ec_modules_uploadbatch_datatableisMetadata']")[0].checked = input.srcElement.checked;
							$(o).find("td[key='isMetadata']").attr('truevalue',input.srcElement.checked);
						}
							
					})
				})
			}else if($(obj).attr('key')=='isCustomcode'){
				checkbox = '<input style="height:13px;top:2px;position:relative;margin-right:1px;" type="checkbox" key="isCustomcode" id="ec_modules_uploadbatch_datatable_checkall_id_isCustomcode" name="ec_modules_uploadbatch_datatable_checkall_isCustomcode" checked="true">${getText("ec.module.zdydm")}';
				$(obj).find('.dg-hd-td-label').html('');
				$(obj).find('.dg-hd-td-label').append(checkbox);
				$(obj).find('.dg-hd-td-label input').bind('click',function(input){
					$('#ec_modules_uploadbatch_datatable_tbody tbody tr').each(function(i,o){
						if($(o).find("input[name='ec_modules_uploadbatch_datatable_checkall_checkbox']")[0].checked == true && !$(o).find("input[name='ec_modules_uploadbatch_datatableisCustomcode']")[0].disabled){
							$(o).find("input[name='ec_modules_uploadbatch_datatableisCustomcode']")[0].checked = input.srcElement.checked;
							$(o).find("td[key='isCustomcode']").attr('truevalue',input.srcElement.checked);
						}
							
					})
				})
			}else if($(obj).attr('key')=='isFlow'){
				checkbox = '<input style="height:13px;top:2px;position:relative;margin-right:1px;" type="checkbox" key="isFlow" id="ec_modules_uploadbatch_datatable_checkall_id_isFlow" name="ec_modules_uploadbatch_datatable_checkall_isFlow" checked="true">${getText("ec.module.gzl")}';
				$(obj).find('.dg-hd-td-label').html('');
				$(obj).find('.dg-hd-td-label').append(checkbox);
				$(obj).find('.dg-hd-td-label input').bind('click',function(input){
					$('#ec_modules_uploadbatch_datatable_tbody tbody tr').each(function(i,o){
						if($(o).find("input[name='ec_modules_uploadbatch_datatable_checkall_checkbox']")[0].checked == true && !$(o).find("input[name='ec_modules_uploadbatch_datatableisFlow']")[0].disabled){
							$(o).find("input[name='ec_modules_uploadbatch_datatableisFlow']")[0].checked = input.srcElement.checked;
							$(o).find("td[key='isFlow']").attr('truevalue',input.srcElement.checked);
						}
							
					})
				})
			}else if($(obj).attr('key')=='isImportTemplate'){
				checkbox = '<input style="height:13px;top:2px;position:relative;margin-right:1px;" type="checkbox" key="isImportTemplate" id="ec_modules_uploadbatch_datatable_checkall_id_isImportTemplate" name="ec_modules_uploadbatch_datatable_checkall_isImportTemplate" checked="true">${getText("ec.module.exceldrmb")}';
				$(obj).find('.dg-hd-td-label').html('');
				$(obj).find('.dg-hd-td-label').append(checkbox);
				$(obj).find('.dg-hd-td-label input').bind('click',function(input){
					$('#ec_modules_uploadbatch_datatable_tbody tbody tr').each(function(i,o){
						if($(o).find("input[name='ec_modules_uploadbatch_datatable_checkall_checkbox']")[0].checked == true && !$(o).find("input[name='ec_modules_uploadbatch_datatableisImportTemplate']")[0].disabled){
							$(o).find("input[name='ec_modules_uploadbatch_datatableisImportTemplate']")[0].checked = input.srcElement.checked;
							$(o).find("td[key='isImportTemplate']").attr('truevalue',input.srcElement.checked);
						}
							
					})
				})
			}else if($(obj).attr('key')=='isUploadschedulerJob'){
				checkbox = '<input style="height:13px;top:2px;position:relative;margin-right:1px;" type="checkbox" key="isUploadschedulerJob" id="ec_modules_uploadbatch_datatable_checkall_id_isUploadschedulerJob" name="ec_modules_uploadbatch_datatable_checkall_isUploadschedulerJob" checked="true">${getText("ec.module.drrwdd")}';
				$(obj).find('.dg-hd-td-label').html('');
				$(obj).find('.dg-hd-td-label').append(checkbox);
				$(obj).find('.dg-hd-td-label input').bind('click',function(input){
					$('#ec_modules_uploadbatch_datatable_tbody tbody tr').each(function(i,o){
						if($(o).find("input[name='ec_modules_uploadbatch_datatable_checkall_checkbox']")[0].checked == true && !$(o).find("input[name='ec_modules_uploadbatch_datatableisUploadschedulerJob']")[0].disabled){
							$(o).find("input[name='ec_modules_uploadbatch_datatableisUploadschedulerJob']")[0].checked = input.srcElement.checked;
							$(o).find("td[key='isUploadschedulerJob']").attr('truevalue',input.srcElement.checked);
						}
							
					})
				})
			}
		})
		/*$('#ec_modules_uploadbatch_datatable_tbody tr td[key="checkbox"]').each(function(checkboxIndex,checkboxObj){
			$(checkboxObj).find('input').unbind().bind('click',function(input){
				$(input.srcElement).parent().parent().parent().find('td[key="isMetadata"],td[key="isCustomcode"],td[key="isFlow"],td[key="isImportTemplate"]').each(function(index,obj){
					$(obj).attr('truevalue',input.srcElement.checked);
					$(obj).find('input')[0].checked = input.srcElement.checked;
				})
			})
		})*/
		//绑定每个checkbox满的时候勾选顶层checkedbox，取消的时候取消顶层checkbox
		var length = $('#ec_modules_uploadbatch_datatable_tbody tbody tr').length;
		$('#ec_modules_uploadbatch_datatable_tbody tbody tr').find('td[key!="checkbox"][celltype ="checkbox"] input').each(function(index,obj){
			YUE.purgeElement($(obj).parent().parent().parent(), true , 'click');//把datagrid的触发事件禁止了
		   $(obj).unbind().bind('change',function(e){
		   	   var target = '';
		   	   var checkbox = '';
		   	   if(e.srcElement != undefined){
					target = $(e.srcElement).parent().parent();
					checkbox = $(e.srcElement);
		   	   }else{
		   	   		target = $(e.currentTarget).parent().parent();
		   	   		checkbox = $(e.currentTarget);
		   	   }
		       var key = target.attr('key');
		       var headCheckdbox = $("#ec_modules_uploadbatch_datatable_hdbox thead tr td[key= " + key + "] input");
		       var checkedLength = $("#ec_modules_uploadbatch_datatable_tbody tbody tr td[key= " + key + "] input:checked").length;
		       if(length==checkedLength){
		            headCheckdbox[0].checked = true;
		       }else{
		           headCheckdbox[0].checked = false;
		       }
		   })
		})

		function checkOtherCheckbox(checkbox, tr){
			var isFirstImport = $(tr).find('input[name="isFirstImport"]').val();
			$(tr).find('td[key!="checkbox"][celltype ="checkbox"] input').each(function(i,o){

			
			if(checkbox.get(0).checked != o.checked){
				if(isFirstImport == 'true'){
					o.disabled = false;
				}
				if(checkbox.get(0).checked){
					o.disabled = false;
				}
				o.click();
				if($.browser.msie){
					o.blur();
					//o.focus();
				}
				if(isFirstImport == 'true'){
					o.disabled = true;
				}
			}

				if(!checkbox.get(0).checked){
					o.disabled = true;
				}
			})
		}


		//绑定头checkbox的事件，联动后面的选项
		$('#ec_modules_uploadbatch_datatable_tbody tbody tr').find('td[key="checkbox"] input').each(function(index,obj){
			$(obj).unbind().bind('change',function(e){
		   	   var target = '';
		   	   var checkbox = '';
		   	   if(e.srcElement != undefined){
					target = $(e.srcElement).parent().parent();
					checkbox = $(e.srcElement);
		   	   }else{
		   	   		target = $(e.currentTarget).parent().parent();
		   	   		checkbox = $(e.currentTarget);
		   	   }
		   target.attr('truevalue',checkbox[0].checked);
	       ec_modules_uploadbatch_datatableWidget._judgeCheckAll(YUD.getElementsByClassName("dt_checkbox","input",$(this).parent().parent().parent()[0])[0]);
	       checkOtherCheckbox(checkbox, target.parent());

		   })
		})

		//单独绑定全选事件
		YUE.purgeElement('ec_modules_uploadbatch_datatable_checkall_id_checkbox', true, 'click');
	    
		//$('#ec_modules_uploadbatch_datatable_checkall_id_checkbox').trigger('click')//默认全选
		$('#ec_modules_uploadbatch_datatable_checkall_id_checkbox').change(function(){
			var checked = this.checked;
			$('#ec_modules_uploadbatch_datatable_tbody input[name="ec_modules_uploadbatch_datatable_checkall_checkbox"]').each(function(){
				this.checked = checked;
				// 0219
				$('#ec_modules_uploadbatch_datatable_tbody tbody tr').each(function(rowIndex){
					foundation.uploadBatchModules.customToggleSelectRow(rowIndex, checked);
				});
				
				this.parentNode.parentNode.setAttribute('truevalue', checked);
				checkOtherCheckbox($(this), this.parentNode.parentNode.parentNode);
			});
			
			// checkOtherCheckbox(target.parent());
		});
		$('#ec_modules_uploadbatch_datatable_checkall_id_checkbox').trigger('click')//默认全选
		/*$('#ec_modules_uploadbatch_datatable_tbody tbody tr').find('td[key="checkbox"] input').each(function(index,obj){
			$(obj).mouseover(function(obj){
				var trobj = $(obj.srcElement).parent().parent().parent()[0];
			  YUE.on(trobj, 'click',function(evt){
			     if (evt) {
			      var ev = window.event || evt;
			      var evTarget = ev.target || ev.srcElement;
			     }     
			     // 选中当前行
			     ec_modules_uploadbatch_datatableWidget._selectRow(ev,trobj); 
			     // 判断触发事件来源
			     if ((evTarget.parentNode && evTarget.parentNode.tagName == "TD" && evTarget.parentNode.firstChild == evTarget) && (ec_modules_uploadbatch_datatableWidget.get('editable') === 'true'|| ec_modules_uploadbatch_datatableWidget.get('editable') === true)) {
			      ec_modules_uploadbatch_datatableWidget._firstClick = evTarget.parentNode;
			                        ec_modules_uploadbatch_datatableWidget._foucsCell = evTarget.parentNode;
			      ec_modules_uploadbatch_datatableWidget._dblEdit(ev);
			     }  
			    }, ec_modules_uploadbatch_datatableWidget, true);  
			});
			$(obj).mouseout(function(obj){
			  YUE.purgeElement($(obj.srcElement).parent().parent().parent()[0], true , 'click');
			});
		})*/
	}
</script>
</body>
</html>
