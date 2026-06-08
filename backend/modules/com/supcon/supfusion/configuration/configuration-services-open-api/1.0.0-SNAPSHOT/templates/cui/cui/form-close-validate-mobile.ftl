<script type="text/javascript">
    function validateForm_${parameters.id?replace('[^a-zA-Z0-9_]', '_', 'r')}() {
    	var oErrorWidget = null;
		<#if parameters.dynamicAttributes["errorBarId"]??>
			oErrorWidget = ${parameters.dynamicAttributes["errorBarId"]}Widget;
		<#else>
			oErrorWidget = ${parameters.id}DialogErrorBarWidget;
		</#if>
		//每次提交时先隐藏上次报错信息
		try{
			oErrorWidget.close();
		}catch(e){}

        var validateRequiredFlag = true;
		var cancelItem = $('input[name="workFlowVarStatus"]');
        validateRequiredFlag = $('input[name="operateType"]').val()=='submit' && (!cancelItem || cancelItem.length == 0 || !(cancelItem.val()=='cancel' || cancelItem.val()=='reject'));
        form = $("#${parameters.id}");
        clearErrorLabels(form);

        var errors = false;
        var field = null;
        var continueValidation = true;
        var errorFields = new Array();
        var errorMessages = new Array();
        var validateFields = {};
        <#if validators??>
        <#list validators as validator>
        <#if validator.validatorType != "multSelectRequired">
        if($('[name="${validator.fieldName}"]', form).length > 0) {
        	if(!validateFields['${validator.fieldName}|${validator.validatorType}']&& ($('[name="${validator.fieldName}"]<#if validator.validatorType = "required" || validator.validatorType = "requiredstring">[type!="hidden"]</#if>', form).length>0 || $('textarea[name="${validator.fieldName}"]', form).length>0 || $('select[name="${validator.fieldName}"][name^="_complex_"]', form).length>0 || $('[name="${validator.fieldName}"][name$="EnumName"][name^="_complex_"]', form).length>0 || $('[name="${validator.fieldName}"][name$=".id"]', form).length>0)) {
	        	validateFields['${validator.fieldName}|${validator.validatorType}']='${validator.fieldName}|${validator.validatorType}';
	       		field = $('[name="${validator.fieldName}"]<#if validator.validatorType = "required" || validator.validatorType = "requiredstring">[type!="hidden"]</#if>', form);
	       		if(field == null || field.length == 0) {
	       			field = $('textarea[name="${validator.fieldName}"]', form);
	       			if(field == null || field.length == 0) {
	       				field = $('[name="${validator.fieldName}"][name$=".id"]', form);
	       				if(field == null || field.length == 0) {
		       				field = $('[name="${validator.fieldName}"]', form);
		       			}
	       			}
	       		}
	            var error = "${validator.getMessage(action)?js_string}";
	            <#if validator.validatorType = "required">
	            if (field.val() == "" && validateRequiredFlag) {
	            	if('${validator.fieldName}'.endsWith('.id')) {
	            		if(field.prop('tagName') == 'SELECT') {
	            			showErrorField(field);
	            		} else {
	            			$("div[nullable=false] [name^='${validator.fieldName?substring(0, validator.fieldName?length - 2)}'][type!='hidden']", form).each(function(){
								showErrorField($(this));
							});
	            		}
	            	} else {
	            		if(field.css('display') == 'none') {
	            			showErrorField(field.parent());
	            		} else {
		            		showErrorField(field);
	            		}
	            	}
	                errorFields.push(field);
	                errorMessages.push(error);
	                errors = true;
	                <#if validator.shortCircuit>continueValidation = false;</#if>
	            }
	            <#elseif validator.validatorType = "requiredstring">
	           	var flag = false;
	            if(field.attr('type') == 'radio' && validateRequiredFlag) {
	            	$.each(field, function(){
	            		if(this.checked) {
	            			flag = true;
	            		}
	            	});
	            	if(!flag){
	            		showErrorField(field.parent());
	            		errorFields.push(field);
		                errorMessages.push(error);
		                errors = true;
		                <#if validator.shortCircuit>continueValidation = false;</#if>
	            	}
	            } else if (continueValidation && validateRequiredFlag && field.val() != null && (field.val() == "" || field.val().replace(/^\s+|\s+$/g,"").length == 0)) {
	                if('${validator.fieldName}'.endsWith('.id')) {
	            		showErrorField($("[name^='${validator.fieldName?substring(0, validator.fieldName?length - 2)}'][type!='hidden']", form).first());
	            	} else {
	            		if(field.css('display') == 'none') {
	            			showErrorField(field.parent());
	            		} else {
		            		showErrorField(field);
	            		}
	            	}
	                errorFields.push(field);
	                errorMessages.push(error);
	                errors = true;
	                <#if validator.shortCircuit>continueValidation = false;</#if>
	            }
	            <#elseif validator.validatorType = "stringlength">
	            if (continueValidation && field.val() != null && field.val() != "" && field.val().replace(/^\s+|\s+$/g,"").length > 0) {
	                var value = $.trim(field.val());
	                if ((${validator.minLength?c} > -1 && value.length < ${validator.minLength?c}) ||
	                    (${validator.maxLength?c} > -1 && value.length > ${validator.maxLength?c})) {
	                    errorFields.push(field);
	                	errorMessages.push(error);
	                	showErrorField(field);
	                    errors = true;
	                    <#if validator.shortCircuit>continueValidation = false;</#if>
	                }
	            }
	            <#elseif validator.validatorType = "regex">
	            if (continueValidation && field.val() != null && field.val() != '' && !field.val().match("${validator.regex?js_string}")) {
	                errorFields.push(field);
	                errorMessages.push(error);
	                showErrorField(field);
	                errors = true;
	                <#if validator.shortCircuit>continueValidation = false;</#if>
	            }
	            <#elseif validator.validatorType = "email">
	            if (continueValidation && field.val() != null && field.val().length > 0 && field.val().match(/\b(^[_A-Za-z0-9-]+(\.[_A-Za-z0-9-]+)*@([A-Za-z0-9-])+(\.[A-Za-z0-9-]+)*((\.[A-Za-z0-9]{2,})|(\.[A-Za-z0-9]{2,}\.[A-Za-z0-9]{2,}))$)\b/gi)==null) {
	                errorFields.push(field);
	                errorMessages.push(error);
	                showErrorField(field);
	                errors = true;
	                <#if validator.shortCircuit>continueValidation = false;</#if>
	            }
	            <#elseif validator.validatorType = "url">
	            if (continueValidation && field.val() != null && field.val().length > 0 && field.val().match(/(^(ftp|http|https):\/\/(\.[_A-Za-z0-9-]+)*(@?([A-Za-z0-9-])+)?(\.[A-Za-z0-9-]+)*((\.[A-Za-z0-9]{2,})|(\.[A-Za-z0-9]{2,}\.[A-Za-z0-9]{2,}))(:[0-9]+)?([/A-Za-z0-9?#_-]*)?$)/gi)==null) {
	                errorFields.push(field);
	                errorMessages.push(error);
	                showErrorField(field);
	                errors = true;
	                <#if validator.shortCircuit>continueValidation = false;</#if>
	            }
	            <#elseif validator.validatorType = "int">
	            if (continueValidation && field.val() != null && field.val() != '') {
	            	var validFlag = false;
	                try{parseInt(field.val());}catch(e){validFlag = true;}
	                if (validFlag || <#if validator.min??>parseInt(field.val()) <
	                     ${validator.min?c}<#else>false</#if> ||
	                        <#if validator.max??>parseInt(field.val()) >
	                           ${validator.max?c}<#else>false</#if> ||
	                           !isInteger(field.val())) {
	                    errorFields.push(field);
	                	errorMessages.push(error);
	                	showErrorField(field);
	                    errors = true;
	                    <#if validator.shortCircuit>continueValidation = false;</#if>
	                }
	            }
	            <#elseif validator.validatorType = "int_plus">
	            if (continueValidation && field.val() != null && field.val() != '') {
	            	var validFlag = false;
	                try{parseInt(field.val());}catch(e){validFlag = true;}
	                if (validFlag || !isInteger(field.val()) || parseInt(field.val()) < 0) {
	                    errorFields.push(field);
	                	errorMessages.push(error);
	                	showErrorField(field);
	                    errors = true;
	                    <#if validator.shortCircuit>continueValidation = false;</#if>
	                }
	            }
	            <#elseif validator.validatorType = "int_minus">
	            if (continueValidation && field.val() != null && field.val() != '') {
	            	var validFlag = false;
	                try{parseInt(field.val());}catch(e){validFlag = true;}
	                if (validFlag || !isInteger(field.val()) || parseInt(field.val()) > 0) {
	                    errorFields.push(field);
	                	errorMessages.push(error);
	                	showErrorField(field);
	                    errors = true;
	                    <#if validator.shortCircuit>continueValidation = false;</#if>
	                }
	            }
	            <#elseif validator.validatorType = "double">
	            if (continueValidation && field.val() != null && field.val() != '') {
	                var validFlag = false;
	                var value = 0;
	                try{value = parseFloat(field.val());}catch(e){validFlag = true;}
	                if (validFlag || <#if validator.minInclusive??>value < ${validator.minInclusive}<#else>false</#if> ||
	                        <#if validator.maxInclusive??>value > ${validator.maxInclusive}<#else>false</#if> ||
	                        <#if validator.minExclusive??>value <= ${validator.minExclusive}<#else>false</#if> ||
	                        <#if validator.maxExclusive??>value >= ${validator.maxExclusive}<#else>false</#if> ||
	                        !isDecimal(field.val())) {
	                    errorFields.push(field);
	                	errorMessages.push(error);
	                	showErrorField(field);
	                    errors = true;
	                    <#if validator.shortCircuit>continueValidation = false;</#if>
	                }
	            }
	            <#elseif validator.validatorType = "double_plus">
	            if (continueValidation && field.val() != null && field.val() != '') {
	                var validFlag = false;
	                var value = 0;
	                try{value = parseFloat(field.val());}catch(e){validFlag = true;}
	                if (validFlag || !isDecimal(field.val()) || value < 0) {
	                    errorFields.push(field);
	                	errorMessages.push(error);
	                	showErrorField(field);
	                    errors = true;
	                    <#if validator.shortCircuit>continueValidation = false;</#if>
	                }
	            }
	            <#elseif validator.validatorType = "double_minus">
	            if (continueValidation && field.val() != null && field.val() != '') {
	                var validFlag = false;
	                var value = 0;
	                try{value = parseFloat(field.val());}catch(e){validFlag = true;}
	                if (validFlag || !isDecimal(field.val()) || value > 0) {
	                    errorFields.push(field);
	                	errorMessages.push(error);
	                	showErrorField(field);
	                    errors = true;
	                    <#if validator.shortCircuit>continueValidation = false;</#if>
	                }
	            }
	            <#elseif validator.validatorType = "post" || validator.validatorType = "mobile" || validator.validatorType = "streightLine" || validator.validatorType = "shortMobile" || validator.validatorType = "telphone">
	            if (continueValidation && field.val() != null && field.val() != '' && !field.val().match("${validator.extRegex?js_string}")) {
	                errorFields.push(field);
	                errorMessages.push(error);
	                showErrorField(field);
	                errors = true;
	                <#if validator.shortCircuit>continueValidation = false;</#if>
	            }
	            <#elseif validator.validatorType = "compare">
	            var anotherValue = $('input[name="${validator.another}"]').val();
	            if (continueValidation && field.val() != null && field.val() != '' && anotherValue !=null && anotherValue != '') {
	                var compareResultError = false;
	                if (isInteger(field.val()) && isInteger(anotherValue)){
	                	if(!(parseInt(field.val()) ${validator.operator} parseInt(anotherValue))) {
	                		compareResultError = true;
	                	}
	                } else if (isDecimal(field.val()) && isDecimal(anotherValue)){
	                	if(!(parseFloat(field.val()) ${validator.operator} parseFloat(anotherValue))) {
	                		compareResultError = true;
	                	}
	                } else if (!(field.val() ${validator.operator} anotherValue)){
	                	compareResultError = true;
	                }
	                if(compareResultError) {
	                    errorFields.push(field);
	                	errorMessages.push(error);
	                	showErrorField(field);
	                    errors = true;
	                    <#if validator.shortCircuit>continueValidation = false;</#if>
	                }
	            }
				<#elseif validator.validatorType = "custom">
	            if (continueValidation && field.val() != null && field.val() != '') {
	            	function customFunction(){
	            		${validator.jsCode}
	            	}
	            	var customFunctionReturn = customFunction();
	                if (customFunctionReturn != null && !customFunctionReturn) {
	                    errorFields.push(field);
	                	errorMessages.push(error);
	                	showErrorField(field);
	                    errors = true;
	                    <#if validator.shortCircuit>continueValidation = false;</#if>
	                }
	            }
	            <#elseif validator.validatorType = "number_precision">
	            if (continueValidation && field.val() != null && field.val() != '') {
	                var validFlag = false;
	                var value = 0;
	                <#if validator.longLength gt 0>
	                	<#if validator.scale?? && validator.scale gt 0>
	                		var regEx = /^[+-]?(\d{1,${validator.longLength}}\.\d{0,${validator.scale}})$|^[+-]?(\d{1,${validator.longLength}}\.?)$/;
	                	<#else>
	                		var regEx = /^[+-]?(\d{1,${validator.longLength}}\.?)$/;
	                	</#if>
		                try{value = parseFloat(field.val());}catch(e){validFlag = true;}
		                if (validFlag || !isDecimal(field.val()) || !regEx.test(value)) {
		                	if(field.prop('type') && field.prop('type').toUpperCase() == 'HIDDEN' && field.next() && field.next().length > 0) {
		                		field = field.next();
		                	}
		                    errorFields.push(field);
		                	errorMessages.push(error);
		                	showErrorField(field);
		                    errors = true;
		                    <#if validator.shortCircuit>continueValidation = false;</#if>
		                }
	                </#if>
	            }
	            </#if>
	        }
        }
        <#else>
        if (validateRequiredFlag && $('input[name="${validator.fieldName}AddIds"]', form).length > 0) {
	        var error = "${validator.getMessage(action)?js_string}";
	        var addField = $('input[name="${validator.fieldName}AddIds"]', form);
	        var delField = $('input[name="${validator.fieldName}DeleteIds"]', form);
	        var existsField = $('input[name="${validator.fieldName}MultiIDs"]', form);
	        var existsObj = {};
	        if(existsField && existsField.val().length > 0) {
		        var arr = existsField.val().split(',');
		        $.each(arr, function(ind, tmpItem){
		        	if(tmpItem && tmpItem.length > 0) {
		        		existsObj[tmpItem] = tmpItem;
		        	}
		        });
	        }
	        if(addField && addField.val().length > 0) {
		        var arr = addField.val().split(',');
		        $.each(arr, function(ind, tmpItem){
		        	if(tmpItem && tmpItem.length > 0) {
		        		existsObj[tmpItem] = tmpItem;
		        	}
		        });
	        }
	         if(delField && delField.val().length > 0) {
		        var arr = delField.val().split(',');
		        $.each(arr, function(ind, tmpItem){
		        	if(tmpItem && tmpItem.length > 0) {
		        		existsObj[tmpItem] = -1;
		        	}
		        });
	        }
	        var existsFlag = false;
	        $.each(existsObj, function(key, value){
	        	if(value != -1) {
	        		existsFlag = true;
	        		return false;
	        	}
	        });
	        if(!existsFlag) {
	        	var field = $(addField).parent('div');
	        	errorFields.push(field);
            	errorMessages.push(error);
            	showErrorField(field);
            	errors = true;
	        }
        }
        </#if>
        </#list>
        </#if>
	
		//PT验证
		var formId = '${parameters.id?replace('[^a-zA-Z0-9_]', '_', 'r')}';
		if(formId && formId.length > 5){
			var pageId = formId.substring(0, formId.length - 5);
			var datagrids = $('body').data(pageId + '__mobile___datagrids');
			
	
			if(datagrids) {
				<#if dgvalidators?exists>
			    	var validataJson = {};
			    	<#list dgvalidators?keys as dg>
			    		validataJson["${dg}"] = {};
			    		<#if dgvalidators[dg]??>
			    			<#list dgvalidators[dg]?keys as column>
			    				var columnData = []; 
			    				<#list dgvalidators[dg][column] as validator>
			    					var validator = new Object();
			    					validator.fieldName = "${validator.fieldName}";
			    					validator.validatorType = "${validator.validatorType}";
			    					<#if validator.validatorType = "required">
			    						validator.message = "${validator.getMessage(action)?js_string}";
						            <#elseif validator.validatorType = "requiredstring">
						            	validator.message = "${validator.getMessage(action)?js_string}";
						            <#elseif validator.validatorType = "stringlength">
						            	validator.minLength = "${validator.minLength?c}";
						            	validator.maxLength = "${validator.maxLength?c}";
						            	validator.message = "${validator.getMessage(action)?js_string}";
						            <#elseif validator.validatorType = "regex">
						            	validator.expression = /${validator.regex?string}/;
						            	validator.message = "${validator.getMessage(action)?js_string}";
						            <#elseif validator.validatorType = "email" || validator.validatorType = "url">
						            	validator.expression = "${validator.regex?js_string}";
						            	validator.message = "${validator.getMessage(action)?js_string}";
						            <#elseif validator.validatorType = "int">
						            	validator.message = "${validator.getMessage(action)?js_string}";
						            	<#if validator.min??>
						            		validator.min = "${validator.min}";
						            	</#if>
						            	<#if validator.max??>
						            		validator.max = "${validator.max}";
						            	</#if>
						            <#elseif validator.validatorType = "int_plus">
						            	validator.message = "${validator.getMessage(action)?js_string}";
						            	<#if validator.min??>
						            		validator.min = "${validator.min}";
						            	</#if>
						            	<#if validator.max??>
						            		validator.max = "${validator.max}";
						            	</#if>
						            <#elseif validator.validatorType = "int_minus">
						            	validator.message = "${validator.getMessage(action)?js_string}";
						            	<#if validator.min??>
						            		validator.min = "${validator.min}";
						            	</#if>
						            	<#if validator.max??>
						            		validator.max = "${validator.max}";
						            	</#if>
						            <#elseif validator.validatorType = "intRang">
						            	validator.message = "${validator.getMessage(action)?js_string}";
						            	<#if validator.min??>
						            		validator.min = "${validator.min}";
						            	</#if>
						            	<#if validator.max??>
						            		validator.max = "${validator.max}";
						            	</#if>
						            <#elseif validator.validatorType = "double">
						            	validator.message = "${validator.getMessage(action)?js_string}";
						            	<#if validator.minExclusive??>
						            		validator.minExclusive = "${validator.minExclusive}";
						            	</#if>
						            	<#if validator.maxInclusive??>
						            		validator.maxInclusive = "${validator.maxInclusive}";
						            	</#if>
						            <#elseif validator.validatorType = "doubleRang">
						            	validator.message = "${validator.getMessage(action)?js_string}";
						            	<#if validator.minInclusive??>
						            		validator.minExclusive = "${validator.minInclusive}";
						            	</#if>
						            	<#if validator.maxInclusive??>
						            		validator.maxInclusive = "${validator.maxInclusive}";
						            	</#if>
						            <#elseif validator.validatorType = "double_plus">
						            	validator.message = "${validator.getMessage(action)?js_string}";
						            	<#if validator.minExclusive??>
						            		validator.minExclusive = "${validator.minExclusive}";
						            	</#if>
						            	<#if validator.maxInclusive??>
						            		validator.maxInclusive = "${validator.maxInclusive}";
						            	</#if>
						            <#elseif validator.validatorType = "double_minus">
						            	validator.message = "${validator.getMessage(action)?js_string}";
						            	<#if validator.minExclusive??>
						            		validator.minExclusive = "${validator.minExclusive}";
						            	</#if>
						            	<#if validator.maxInclusive??>
						            		validator.maxInclusive = "${validator.maxInclusive}";
						            	</#if>
						            <#elseif validator.validatorType = "post" || validator.validatorType = "mobile" || validator.validatorType = "streightLine" || validator.validatorType = "shortMobile" || validator.validatorType = "telphone">
						            	validator.message = "${validator.getMessage(action)?js_string}";
						            <#elseif validator.validatorType = "compare">
						            	validator.another = "${validator.another}";
						            	validator.operator = "${validator.operator}";
						            	validator.message = "${validator.getMessage(action)?js_string}";
									<#elseif validator.validatorType = "custom">
										validator.jscode = "${validator.jsCode}";
										validator.message = "${validator.getMessage(action)?js_string}";
									<#elseif validator.validatorType = "dateRang">
						            	validator.message = "${validator.getMessage(action)?js_string}";
						            	<#if validator.min??>
						            		validator.min = "${validator.min?string("yyyy-MM-dd")}";
						            	</#if>
						            	<#if validator.max??>
						            		validator.max = "${validator.max?string("yyyy-MM-dd")}";
						            	</#if>
									<#elseif validator.validatorType = "number_precision">
										validator.precision = ${validator.precision!19};
										validator.scale = ${validator.scale!0};
										validator.message = "${validator.getMessage(action)?js_string}";
						            </#if>
						            columnData.push(validator);
			    				</#list>
			    				validataJson["${dg}"]["${column}"] = columnData;
			    			</#list>
			    		</#if>
			 		</#list>
			    </#if>
			    var validateResult = '';
			   	if(typeof(validataJson) != "undefined"){
					//$("#" + pageId + "_main_div .edit-panes-s").each(function(index){
					for(var index = 0; index <datagrids.length; index++) {
						for(var i=0;i<datagrids[index].length;i++) {
							var dgName = datagrids[index][i].split("_")[1];
							var rules = validataJson[dgName];
							try {
								var dgwidget = eval(datagrids[index][i]+'Widget');
								if(dgwidget) {
									dgwidget.need_validate_required = validateRequiredFlag;
									dgwidget.validator.validateRules = rules;
									validateResult += dgwidget.validateTable(true);
									// 验证不通过,需要将editDatas设为null
									if( ( errors || validateResult ) && dgwidget.isJS){
										dgwidget._DT.editDatas = null;
									}
								}
							} catch (e) {}
							
						}
					}
					//});
			   	}
				//显示错误信息
				if(validateResult !== ''){
					errorMessages.push(validateResult);
					errors = true;
				}
			}
		}
		

		if(errors){
			addError(form,oErrorWidget,errorFields,errorMessages);
		}
		
        return !errors;
    }
</script>