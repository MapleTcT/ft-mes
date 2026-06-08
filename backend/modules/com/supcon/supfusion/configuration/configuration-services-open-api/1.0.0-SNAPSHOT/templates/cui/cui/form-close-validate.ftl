<#--
/*
 * $Id: form-close-validate.ftl 759162 2009-03-27 14:49:39Z musachy $
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
-->
<#--
START SNIPPET: supported-validators
Only the following validators are supported:
* required validator
* requiredstring validator
* stringlength validator
* regex validator
* email validator
* url validator
* int validator
* double validator
END SNIPPET: supported-validators
-->
<#if ((parameters.validate?default(false) == true) && (parameters.performValidation?default(false) == true)) && validators??>
<script type="text/javascript">
(function(){  //onload事件，自动调用
	var form = $('#${parameters.id}');
        <#list validators as validator>
        $("*[name='${validator.fieldName}']",form).each(function(index,item){
        	<#if validator.validatorType = "required" || validator.validatorType = "requiredstring">
        		CUI(this).parent().prev().css('color','#B30303');
        	</#if>
        });
        </#list>
})();
   
    function validateForm_${parameters.id?replace('[^a-zA-Z0-9_]', '_', 'r')}() {
        form = document.getElementById("${parameters.id}");
        // 内容前后去空格
    	$('select,input[type="text"],textarea', '#${parameters.id}').each(function(){	
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
       <#list validators as validator>
        // field name: ${validator.fieldName}
        // validator name: ${validator.validatorType}
        if (form.elements['${validator.fieldName}']) {
        	if(!validateFields['${validator.fieldName}']&&CUI('[name="${validator.fieldName}"][type!="hidden"]', CUI(form)).length>0) {
	        	validateFields['${validator.fieldName}']='${validator.fieldName}';
	       		field = CUI('[name="${validator.fieldName}"][type!="hidden"]', CUI(form)).get(0);
	            var error = "${validator.getMessage(action)?js_string}";
	            <#if validator.validatorType = "required">
	            if ($.trim(field.value) == "") {
	                errorFields.push(field);
	                errorMessages.push(error);
	                showErrorField(field);
	                errors = true;
	                <#if validator.shortCircuit>continueValidation = false;</#if>
	            }
	            <#elseif validator.validatorType = "requiredstring">
	            if (continueValidation && field.value != null && (field.value == "" || field.value.replace(/^\s+|\s+$/g,"").length == 0)) {
	                errorFields.push(field);
	                errorMessages.push(error);
	               	showErrorField(field);
	                errors = true;
	                <#if validator.shortCircuit>continueValidation = false;</#if>
	            }
	            <#elseif validator.validatorType = "stringlength">
	            if (continueValidation && field.value != null) {
	                var value = field.value;
	                <#if validator.trim>
	                    //trim field value
	                    while (value.substring(0,1) == ' ')
	                        value = value.substring(1, value.length);
	                    while (value.substring(value.length-1, value.length) == ' ')
	                        value = value.substring(0, value.length-1);
	                </#if>
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
	            if (continueValidation && field.value != null && field.value != '' && !field.value.match("${validator.regex?js_string}")) {
	                errorFields.push(field);
	                errorMessages.push(error);
	                showErrorField(field);
	                errors = true;
	                <#if validator.shortCircuit>continueValidation = false;</#if>
	            }
	            <#elseif validator.validatorType = "email">
	            if (continueValidation && field.value != null && field.value.length > 0 && field.value.match(/\b(^[_A-Za-z0-9-]+(\.[_A-Za-z0-9-]+)*@([A-Za-z0-9-])+(\.[A-Za-z0-9-]+)*((\.[A-Za-z0-9]{2,})|(\.[A-Za-z0-9]{2,}\.[A-Za-z0-9]{2,}))$)\b/gi)==null) {
	                errorFields.push(field);
	                errorMessages.push(error);
	                showErrorField(field);
	                errors = true;
	                <#if validator.shortCircuit>continueValidation = false;</#if>
	            }
	            <#elseif validator.validatorType = "url">
	            if (continueValidation && field.value != null && field.value.length > 0 && field.value.match(/(^(ftp|http|https):\/\/(\.[_A-Za-z0-9-]+)*(@?([A-Za-z0-9-])+)?(\.[A-Za-z0-9-]+)*((\.[A-Za-z0-9]{2,})|(\.[A-Za-z0-9]{2,}\.[A-Za-z0-9]{2,}))(:[0-9]+)?([/A-Za-z0-9?#_-]*)?$)/gi)==null) {
	                errorFields.push(field);
	                errorMessages.push(error);
	                showErrorField(field);
	                errors = true;
	                <#if validator.shortCircuit>continueValidation = false;</#if>
	            }
	            <#elseif validator.validatorType = "int">
	            if (continueValidation && field.value != null) {
	            	var validFlag = false;
	                try{parseInt(field.value)}catch(e){validFlag = true;}
	                if (validFlag || <#if validator.min??>parseInt(field.value) <
	                     ${validator.min?c}<#else>false</#if> ||
	                        <#if validator.max??>parseInt(field.value) >
	                           ${validator.max?c}<#else>false</#if>) {
	                    errorFields.push(field);
	                	errorMessages.push(error);
	                	showErrorField(field);
	                    errors = true;
	                    <#if validator.shortCircuit>continueValidation = false;</#if>
	                }
	            }
	            <#elseif validator.validatorType = "double">
	            if (continueValidation && field.value != null) {
	                var value = parseFloat(field.value);
	                if (<#if validator.minInclusive??>value < ${validator.minInclusive}<#else>false</#if> ||
	                        <#if validator.maxInclusive??>value > ${validator.maxInclusive}<#else>false</#if> ||
	                        <#if validator.minExclusive??>value <= ${validator.minExclusive}<#else>false</#if> ||
	                        <#if validator.maxExclusive??>value >= ${validator.maxExclusive}<#else>false</#if>) {
	                    errorFields.push(field);
	                	errorMessages.push(error);
	                	showErrorField(field);
	                    errors = true;
	                    <#if validator.shortCircuit>continueValidation = false;</#if>
	                }
	            }
	            </#if>
	        }
        }
        </#list>
		var oErrorWidget = null;
		<#if parameters.dynamicAttributes["errorBarId"]??>
			oErrorWidget = ${parameters.dynamicAttributes["errorBarId"]}Widget;
		<#else>
			oErrorWidget = ${parameters.id}DialogErrorBarWidget;
		</#if>
		if(errors){
			addError(form,oErrorWidget,errorFields,errorMessages);
		}
		
        return !errors;
    }
</script>
<#elseif ((parameters.validate?default(false) == true) && (parameters.performValidation?default(false) == true))>
<script type="text/javascript">
(function(){  //onload事件，自动调用
	var form = CUI('#${parameters.id}');
    <#list parameters.tagNames as tagName>
        <#list tag.getValidators("${tagName}") as validator>
        CUI("*[name='${validator.fieldName}']",form).each(function(index,item){
        	<#if validator.validatorType = "required" || validator.validatorType = "requiredstring">
        		CUI(this).parent().prev().css('color','#B30303');
        		
        		
        	</#if>
        });
        </#list>
    </#list>
})();
   
    function validateForm_${parameters.id?replace('[^a-zA-Z0-9_]', '_', 'r')}() {
        form = document.getElementById("${parameters.id}");
        // 内容前后去空格
    	$('select,input[type="text"],textarea', '#${parameters.id}').each(function(){
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
    <#list parameters.tagNames as tagName>
        <#list tag.getValidators("${tagName}") as validator>
        // field name: ${validator.fieldName}
        // validator name: ${validator.validatorType}
        if (form.elements['${validator.fieldName}']) {
        	if(!validateFields['${validator.fieldName}']&&CUI('*[name="${validator.fieldName}"][disabled=false][type!="hidden"]', CUI(form)).length>0) {
	        	validateFields['${validator.fieldName}']='${validator.fieldName}';
	       		field = CUI('*[name="${validator.fieldName}"][disabled=false][type!="hidden"]', CUI(form)).get(0);
	            var error = "${validator.getMessage(action)?js_string}";
	            <#if validator.validatorType = "required">
	            if ($.trim(field.value) == "") {
	                errorFields.push(field);
	                errorMessages.push(error);
	                showErrorField(field);
	                errors = true;
	                <#if validator.shortCircuit>continueValidation = false;</#if>
	            }
	            <#elseif validator.validatorType = "requiredstring">
	            if (continueValidation && field.value != null && (field.value == "" || field.value.replace(/^\s+|\s+$/g,"").length == 0)) {
	                errorFields.push(field);
	                errorMessages.push(error);
	               	showErrorField(field);
	                errors = true;
	                <#if validator.shortCircuit>continueValidation = false;</#if>
	            }
	            <#elseif validator.validatorType = "stringlength">
	            if (continueValidation && field.value != null) {
	                var value = field.value;
	                <#if validator.trim>
	                    //trim field value
	                    while (value.substring(0,1) == ' ')
	                        value = value.substring(1, value.length);
	                    while (value.substring(value.length-1, value.length) == ' ')
	                        value = value.substring(0, value.length-1);
	                </#if>
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
	            if (continueValidation && field.value != null && field.value != '' && !field.value.match("${validator.regex?js_string}")) {
	                errorFields.push(field);
	                errorMessages.push(error);
	                showErrorField(field);
	                errors = true;
	                <#if validator.shortCircuit>continueValidation = false;</#if>
	            }
	            <#elseif validator.validatorType = "email">
	            if (continueValidation && field.value != null && field.value.length > 0 && field.value.match(/\b(^[_A-Za-z0-9-]+(\.[_A-Za-z0-9-]+)*@([A-Za-z0-9-])+(\.[A-Za-z0-9-]+)*((\.[A-Za-z0-9]{2,})|(\.[A-Za-z0-9]{2,}\.[A-Za-z0-9]{2,}))$)\b/gi)==null) {
	                errorFields.push(field);
	                errorMessages.push(error);
	                showErrorField(field);
	                errors = true;
	                <#if validator.shortCircuit>continueValidation = false;</#if>
	            }
	            <#elseif validator.validatorType = "url">
	            if (continueValidation && field.value != null && field.value.length > 0 && field.value.match(/(^(ftp|http|https):\/\/(\.[_A-Za-z0-9-]+)*(@?([A-Za-z0-9-])+)?(\.[A-Za-z0-9-]+)*((\.[A-Za-z0-9]{2,})|(\.[A-Za-z0-9]{2,}\.[A-Za-z0-9]{2,}))(:[0-9]+)?([/A-Za-z0-9?#_-]*)?$)/gi)==null) {
	                errorFields.push(field);
	                errorMessages.push(error);
	                showErrorField(field);
	                errors = true;
	                <#if validator.shortCircuit>continueValidation = false;</#if>
	            }
	            <#elseif validator.validatorType = "int">
	            if (continueValidation && field.value != null) {
	            	var validFlag = false;
	                try{parseInt(field.value)}catch(e){validFlag = true;}
	                if (validFlag || <#if validator.min??>parseInt(field.value) <
	                     ${validator.min?c}<#else>false</#if> ||
	                        <#if validator.max??>parseInt(field.value) >
	                           ${validator.max?c}<#else>false</#if>) {
	                    errorFields.push(field);
	                	errorMessages.push(error);
	                	showErrorField(field);
	                    errors = true;
	                    <#if validator.shortCircuit>continueValidation = false;</#if>
	                }
	            }
	            <#elseif validator.validatorType = "double">
	            if (continueValidation && field.value != null) {
	                var value = parseFloat(field.value);
	                if (<#if validator.minInclusive??>value < ${validator.minInclusive}<#else>false</#if> ||
	                        <#if validator.maxInclusive??>value > ${validator.maxInclusive}<#else>false</#if> ||
	                        <#if validator.minExclusive??>value <= ${validator.minExclusive}<#else>false</#if> ||
	                        <#if validator.maxExclusive??>value >= ${validator.maxExclusive}<#else>false</#if>) {
	                    errorFields.push(field);
	                	errorMessages.push(error);
	                	showErrorField(field);
	                    errors = true;
	                    <#if validator.shortCircuit>continueValidation = false;</#if>
	                }
	            }
	            </#if>
	        }
        }
        </#list>
    </#list>
		var oErrorWidget = null;
		<#if parameters.dynamicAttributes["errorBarId"]??>
			oErrorWidget = ${parameters.dynamicAttributes["errorBarId"]}Widget;
		<#else>
			oErrorWidget = ${parameters.id}DialogErrorBarWidget;
		</#if>
		if(errors){
			addError(form,oErrorWidget,errorFields,errorMessages);
		}
		
        //return !errors;
        return false;
    }
</script>
<#elseif parameters.dynamicAttributes["ecform"]?has_content && parameters.dynamicAttributes["ecform"]=='true' && validators??>
<script type="text/javascript">
// 实体配置专用
(function(){  //onload事件，自动调用
	$('#${parameters.id} td[nullable="false"]').each(function(){
		if($('select,input,textarea', this).length == 0) {
			$(this).css('color','#B30303');
		}
	});
})();
   
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
		}catch(e){
		
		}
    	
        var validateRequiredFlag = true;
		var cancelItem = $('input[name="workFlowVarStatus"]');
        <#if !parameters.dynamicAttributes["foundationform"]?has_content>
        validateRequiredFlag = $('input[name="operateType"]').val()=='submit' && (!cancelItem || cancelItem.length == 0 || !(cancelItem.val()=='cancel' || cancelItem.val()=='reject'));
        </#if>
        form = document.getElementById("${parameters.id}");
        clearErrorMessages(form);
        clearErrorLabels(form);

        var errors = false;
        var field = null;
        var continueValidation = true;
        var errorFields = new Array();
        var errorMessages = new Array();
        var validateFields = {};
        <#list validators as validator>
        <#if !(validator.validatorType = 'multSelectRequired' || validator.validatorType = 'fileValidator')>
        // field name: ${validator.fieldName}
        // validator name: ${validator.validatorType}
        //console.log('${validator.validatorType}');
        if($('[name="${validator.fieldName}"]', form).length > 0) {
        	if(!validateFields['${validator.fieldName}|${validator.validatorType}']&& ($('*[name="${validator.fieldName}"]<#if validator.validatorType = "required" || validator.validatorType = "requiredstring">[type!="hidden"]</#if>', CUI(form)).length>0 || $('textarea[name="${validator.fieldName}"]', form).length>0 || ($('input[name="${validator.fieldName}"]', form).length>0 && $('input[name="${validator.fieldName}"]', form).next('input').attr('name')=="${(validator.fieldName)?replace('.', '_')}") || $('input[name="${validator.fieldName}"][#${validator.fieldName}-select-time]', form).length>0|| $('select[name="${validator.fieldName}"][name^="_complex_"]', form).length>0 || $('[name="${validator.fieldName}"][name$="EnumName"][name^="_complex_"]', form).length>0 || $('[name="${validator.fieldName}"][validateType="SystemCode"]', form).length>0 ||($('[name="${validator.fieldName}"][name$=".id"]', CUI(form)).length>0 || $('[name="${validator.fieldName}"][iscustom="true"]', form).length>0))) {
	        	validateFields['${validator.fieldName}|${validator.validatorType}']='${validator.fieldName}|${validator.validatorType}';
	       		field = $('*[name="${validator.fieldName}"]<#if validator.validatorType = "required" || validator.validatorType = "requiredstring">[type!="hidden"]</#if>', CUI(form)).get(0);
	       		if(field == null) {
	       			field = $('textarea[name="${validator.fieldName}"]', CUI(form)).get(0);
	       			if(field == null) {
	       				field = $('[name="${validator.fieldName}"][name$=".id"]', CUI(form)).get(0);
	       				if(field == null) {
		       				field = $('[name="${validator.fieldName}"]', CUI(form)).get(0);
		       				if(field == null) {
			       				field = $('[name="${validator.fieldName}"][validateType="SystemCode"]', CUI(form)).get(0);
			       				if(field == null) {
			       					field = $('input[name="${validator.fieldName}"]', form).get(0);
			       				}
			       			}
		       			}
	       			}
	       		}
	            var error = "${validator.getMessage(action)?js_string}";
	            <#if validator.validatorType = "required">
	            if($(field).prop('tagName') == 'INPUT'&&$(field).prop('type') == 'radio' && validateRequiredFlag) {
	            	if($("input[name='${validator.fieldName}']:checked").length==0){
	            		showErrorField($(field).parent().parent());
	            		errorFields.push(field);
	                	errorMessages.push(error);
	               	 	errors = true;
	            	}
	            }else if (field.value == "" && validateRequiredFlag) {
	            	if('${validator.fieldName}'.endsWith('.id') || $('[name="${validator.fieldName}"][iscustom="true"]', form).length>0) {
	            		if($(field).prop('tagName') == 'SELECT') {
	            			showErrorField($(field).parent());
	            		} else {
							if( $('[name="${validator.fieldName}"][iscustom="true"]', form).length>0 ){
								showErrorField( $( 'input[type="text"]', $('[name="${validator.fieldName}"][iscustom="true"]', form).parents('td')[0] ) );
							}else{
								$("*[name^='${validator.fieldName?substring(0, validator.fieldName?length - 2)}'][type!='hidden']", form).each(function(){
									if($(this).parents('td[nullable=false]').length > 0) {
										showErrorField($(this));
									}
								});
							}
	            		}
	            	} else {
	            		if($('input[name="${validator.fieldName}"]', form).length>0 && $('input[name="${validator.fieldName}"]', form).next('input').attr('name')=="${(validator.fieldName)?replace('.', '_')}") {
	            			showErrorField($(field).next('input'));
	            		} else if($(field).css('display') == 'none') {
	            			showErrorField($(field).parent());
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
	            if(field.type == 'radio' && validateRequiredFlag) {
	            	$('input[name="' + field.name + '"]:radio').each(function(){
	            		if(this.checked) {
	            			flag = true;
	            		}
	            	});
	            	if(!flag){
	            		showErrorField($(field).parent());
	            		errorFields.push(field);
		                errorMessages.push(error);
		                errors = true;
		                <#if validator.shortCircuit>continueValidation = false;</#if>
	            	}
	            } else if (continueValidation && validateRequiredFlag && field.value != null && (field.value == "" || field.value.replace(/^\s+|\s+$/g,"").length == 0)) {
	                if('${validator.fieldName}'.endsWith('.id')) {
	            		showErrorField(CUI("*[name^='${validator.fieldName?substring(0, validator.fieldName?length - 2)}'][type!='hidden']", form).first());
	            	} else {
	            		if($(field).attr('validateType') == 'SystemCode') {
	            			showErrorField($('input[type="text"]',$(field).parent('div')));
	            		} else if($(field).css('display') == 'none') {
	            			showErrorField($(field).parent());
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
	            if (continueValidation && field.value != null && field.value != "" && field.value.replace(/^\s+|\s+$/g,"").length > 0) {
	                var value = field.value;
	                <#if validator.trim>
	                    //trim field value
	                    while (value.substring(0,1) == ' ')
	                        value = value.substring(1, value.length);
	                    while (value.substring(value.length-1, value.length) == ' ')
	                        value = value.substring(0, value.length-1);
	                </#if>
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
	            if (continueValidation && field.value != null && field.value != '' && !field.value.trim().match("${validator.regex?js_string}")) {
	                errorFields.push(field);
	                errorMessages.push(error);
	                showErrorField(field);
	                errors = true;
	                <#if validator.shortCircuit>continueValidation = false;</#if>
	            }
	            <#elseif validator.validatorType = "email">
	            if (continueValidation && field.value != null && field.value.length > 0 && field.value.match(/\b(^[_A-Za-z0-9-]+(\.[_A-Za-z0-9-]+)*@([A-Za-z0-9-])+(\.[A-Za-z0-9-]+)*((\.[A-Za-z0-9]{2,})|(\.[A-Za-z0-9]{2,}\.[A-Za-z0-9]{2,}))$)\b/gi)==null) {
	                errorFields.push(field);
	                errorMessages.push(error);
	                showErrorField(field);
	                errors = true;
	                <#if validator.shortCircuit>continueValidation = false;</#if>
	            }
	            <#elseif validator.validatorType = "url">
	            if (continueValidation && field.value != null && field.value.length > 0 && field.value.match(/(^(ftp|http|https):\/\/(\.[_A-Za-z0-9-]+)*(@?([A-Za-z0-9-])+)?(\.[A-Za-z0-9-]+)*((\.[A-Za-z0-9]{2,})|(\.[A-Za-z0-9]{2,}\.[A-Za-z0-9]{2,}))(:[0-9]+)?([/A-Za-z0-9?#_-]*)?$)/gi)==null) {
	                errorFields.push(field);
	                errorMessages.push(error);
	                showErrorField(field);
	                errors = true;
	                <#if validator.shortCircuit>continueValidation = false;</#if>
	            }
	            <#elseif validator.validatorType = "int">
	            if (continueValidation && field.value != null && field.value != '') {
	            	var validFlag = false;
	                try{parseInt(field.value);}catch(e){validFlag = true;}
	                if (validFlag || <#if validator.min??>parseInt(field.value) <
	                     ${validator.min?c}<#else>false</#if> ||
	                        <#if validator.max??>parseInt(field.value) >
	                           ${validator.max?c}<#else>false</#if> ||
	                           !isInteger(field.value)) {
	                    errorFields.push(field);
	                	errorMessages.push(error);
	                	showErrorField(field);
	                    errors = true;
	                    <#if validator.shortCircuit>continueValidation = false;</#if>
	                }
	            }
	            <#elseif validator.validatorType = "int_plus">
	            if (continueValidation && field.value != null && field.value != '') {
	            	var validFlag = false;
	                try{parseInt(field.value);}catch(e){validFlag = true;}
	                if (validFlag || !isInteger(field.value) || parseInt(field.value) < 0) {
	                    errorFields.push(field);
	                	errorMessages.push(error);
	                	showErrorField(field);
	                    errors = true;
	                    <#if validator.shortCircuit>continueValidation = false;</#if>
	                }
	            }
	            <#elseif validator.validatorType = "int_minus">
	            if (continueValidation && field.value != null && field.value != '') {
	            	var validFlag = false;
	                try{parseInt(field.value);}catch(e){validFlag = true;}
	                if (validFlag || !isInteger(field.value) || parseInt(field.value) > 0) {
	                    errorFields.push(field);
	                	errorMessages.push(error);
	                	showErrorField(field);
	                    errors = true;
	                    <#if validator.shortCircuit>continueValidation = false;</#if>
	                }
	            }
	            <#elseif validator.validatorType = "intRang">
	            if (continueValidation && field.value != null && field.value != '') {
	            	var validFlag = false;
	                try{parseInt(field.value);}catch(e){validFlag = true;}
	                if (validFlag || <#if validator.min??>parseInt(field.value) <
	                     ${validator.min?c}<#else>false</#if> ||
	                        <#if validator.max??>parseInt(field.value) >
	                           ${validator.max?c}<#else>false</#if> ||
	                           !isInteger(field.value)) {
	                    errorFields.push(field);
	                	errorMessages.push(error);
	                	showErrorField(field);
	                    errors = true;
	                    <#if validator.shortCircuit>continueValidation = false;</#if>
	                }
	            }
	            <#elseif validator.validatorType = "doubleRang">
	            if (continueValidation && field.value != null && field.value != '') {
	            	var validFlag = false;
	                try{parseInt(field.value);}catch(e){validFlag = true;}
	                if (validFlag || <#if validator.minInclusive??>parseFloat(field.value) <
	                     ${validator.minInclusive?c}<#else>false</#if> ||
	                        <#if validator.maxInclusive??>parseFloat(field.value) >
	                           ${validator.maxInclusive?c}<#else>false</#if> ||
	                           !isDecimal(field.value)) {
	                    errorFields.push(field);
	                	errorMessages.push(error);
	                	showErrorField(field);
	                    errors = true;
	                    <#if validator.shortCircuit>continueValidation = false;</#if>
	                }
	            }
	            <#elseif validator.validatorType = "long">
	            if (continueValidation && field.value != null && field.value != '') {
	            	var validFlag = false;
	                try{parseInt(field.value);}catch(e){validFlag = true;}
	                if (validFlag || <#if validator.min??>parseInt(field.value) <
	                     ${validator.min?c}<#else>false</#if> ||
	                        <#if validator.max??>parseInt(field.value) >
	                           ${validator.max?c}<#else>false</#if> ||
	                           !isLong(field.value)) {
	                    errorFields.push(field);
	                	errorMessages.push(error);
	                	showErrorField(field);
	                    errors = true;
	                    <#if validator.shortCircuit>continueValidation = false;</#if>
	                }
	            }
	            <#elseif validator.validatorType = "long_plus">
	            if (continueValidation && field.value != null && field.value != '') {
	            	var validFlag = false;
	                try{parseInt(field.value);}catch(e){validFlag = true;}
	                if (validFlag || !isLong(field.value) || parseInt(field.value) < 0) {
	                    errorFields.push(field);
	                	errorMessages.push(error);
	                	showErrorField(field);
	                    errors = true;
	                    <#if validator.shortCircuit>continueValidation = false;</#if>
	                }
	            }
	            <#elseif validator.validatorType = "long_minus">
	            if (continueValidation && field.value != null && field.value != '') {
	            	var validFlag = false;
	                try{parseInt(field.value);}catch(e){validFlag = true;}
	                if (validFlag || !isLong(field.value) || parseInt(field.value) > 0) {
	                    errorFields.push(field);
	                	errorMessages.push(error);
	                	showErrorField(field);
	                    errors = true;
	                    <#if validator.shortCircuit>continueValidation = false;</#if>
	                }
	            }
	            <#elseif validator.validatorType = "longRang">
	            if (continueValidation && field.value != null && field.value != '') {
	            	var validFlag = false;
	                try{parseInt(field.value);}catch(e){validFlag = true;}
	                if (isInteger(field.value) && <#if validator.min??>parseInt(field.value) <
	                     ${validator.min?c}<#else>false</#if> ||
	                        <#if validator.max??>parseInt(field.value) >
	                           ${validator.max?c}<#else>false</#if> ||
	                           !isLong(field.value)) {
	                    errorFields.push(field);
	                	errorMessages.push(error);
	                	showErrorField(field);
	                    errors = true;
	                    <#if validator.shortCircuit>continueValidation = false;</#if>
	                }
	            }
	            <#elseif validator.validatorType = "double">
	            if (continueValidation && field.value != null && field.value != '') {
	                var validFlag = false;
	                var value = 0;
	                try{value = parseFloat(field.value);}catch(e){validFlag = true;}
	                if (validFlag || <#if validator.minInclusive??>value < ${validator.minInclusive}<#else>false</#if> ||
	                        <#if validator.maxInclusive??>value > ${validator.maxInclusive}<#else>false</#if> ||
	                        <#if validator.minExclusive??>value <= ${validator.minExclusive}<#else>false</#if> ||
	                        <#if validator.maxExclusive??>value >= ${validator.maxExclusive}<#else>false</#if> ||
	                        !isDecimal(field.value)) {
	                    errorFields.push(field);
	                	errorMessages.push(error);
	                	showErrorField(field);
	                    errors = true;
	                    <#if validator.shortCircuit>continueValidation = false;</#if>
	                }
	            }
	            <#elseif validator.validatorType = "double_plus">
	            if (continueValidation && field.value != null && field.value != '') {
	                var validFlag = false;
	                var value = 0;
	                try{value = parseFloat(field.value);}catch(e){validFlag = true;}
	                if (validFlag || !isDecimal(field.value) || value <= 0) {
	                    errorFields.push(field);
	                	errorMessages.push(error);
	                	showErrorField(field);
	                    errors = true;
	                    <#if validator.shortCircuit>continueValidation = false;</#if>
	                }
	            }
	            <#elseif validator.validatorType = "double_minus">
	            if (continueValidation && field.value != null && field.value != '') {
	                var validFlag = false;
	                var value = 0;
	                try{value = parseFloat(field.value);}catch(e){validFlag = true;}
	                if (validFlag || !isDecimal(field.value) || value > 0) {
	                    errorFields.push(field);
	                	errorMessages.push(error);
	                	showErrorField(field);
	                    errors = true;
	                    <#if validator.shortCircuit>continueValidation = false;</#if>
	                }
	            }
	            <#elseif validator.validatorType = "post" || validator.validatorType = "mobile" || validator.validatorType = "streightLine" || validator.validatorType = "shortMobile" || validator.validatorType = "telphone">
	            if (continueValidation && field.value != null && field.value != '' && !field.value.match("${validator.extRegex?js_string}")) {
	                errorFields.push(field);
	                errorMessages.push(error);
	                showErrorField(field);
	                errors = true;
	                <#if validator.shortCircuit>continueValidation = false;</#if>
	            }
	            <#elseif validator.validatorType = "compare">
	            var anotherValue = $('input[name="${validator.another}"]').val();
	            if (continueValidation && field.value != null && field.value != '' && anotherValue !=null && anotherValue != '') {
	                var compareResultError = false;
	                if (isInteger(field.value) && isInteger(anotherValue)){
	                	if(!(parseInt(field.value) ${validator.operator} parseInt(anotherValue))) {
	                		compareResultError = true;
	                	}
	                } else if (isDecimal(field.value) && isDecimal(anotherValue)){
	                	if(!(parseFloat(field.value) ${validator.operator} parseFloat(anotherValue))) {
	                		compareResultError = true;
	                	}
	                } else if (!(field.value ${validator.operator} anotherValue)){
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
	            if (continueValidation && field.value != null && field.value != '') {
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
	            if (continueValidation && field.value != null && field.value != '') {
	                var validFlag = false;
	                var value = 0;
	                <#if validator.longLength gt 0>
	                	<#if validator.scale?? && validator.scale gt 0>
	                		var regEx = /^[+-]?(\d{1,${validator.longLength}}\.\d{0,${validator.scale}})$|^[+-]?(\d{1,${validator.longLength}}\.?)$/;
	                	<#else>
	                		var regEx = /^[+-]?(\d{1,${validator.longLength}}\.?)$/;
	                	</#if>
		                try{value = parseFloat(field.value);}catch(e){validFlag = true;}
		                if (isDecimal(field.value) && !regEx.test(value)) {
		                	if($(field).prop('type') && $(field).prop('type').toUpperCase() == 'HIDDEN' && $(field).next() && $(field).next().length > 0) {
		                		field = $(field).next()[0];
		                	}
		                    errorFields.push(field);
		                	errorMessages.push(error);
		                	showErrorField(field);
		                    errors = true;
		                    <#if validator.shortCircuit>continueValidation = false;</#if>
		                }
	                </#if>
	            }
	            <#elseif validator.validatorType = "dateRang">
	            if (continueValidation && field.value != null && field.value != '') {
	            	var validFlag = false;
	            	var fieldTime = new Date(field.value.replace("-", "/")).valueOf();
            	<#if validator.min??>            		
            		var startTime = "${validator.min?string("yyyy-MM-dd hh:mm:ss")}";
            		startTime = new Date(startTime.replace("-", "/")).valueOf();
            		if(startTime > fieldTime){
            			validFlag = true;
            		}
            	</#if>
            	<#if validator.max??>
            		var endTime = "${validator.max?string("yyyy-MM-dd hh:mm:ss")}";
            		endTime = new Date(endTime.replace("-", "/")).valueOf();
            		if(endTime < fieldTime){
            			validFlag = true;
            		}
            	</#if>
	                if (validFlag) {
	                    errorFields.push(field);
	                	errorMessages.push(error);
	                	showErrorField(field);
	                    errors = true;
	                    <#if validator.shortCircuit>continueValidation = false;</#if>
	                }
	            }
	            </#if>
	        }
        }
        <#elseif validator.validatorType = 'multSelectRequired'>
        if (validateRequiredFlag && $('[name="${validator.fieldName}AddIds"]', form).length > 0) {
	        var error = "${validator.getMessage(action)?js_string}";
	        var addField = $('[name="${validator.fieldName}AddIds"]', form)[0];
	        var delField = $('[name="${validator.fieldName}DeleteIds"]', form)[0];
	        var existsField = $('[name="${validator.fieldName}MultiIDs"]', form)[0];
	        var existsObj = {};
	        if(existsField && existsField.value.length > 0) {
		        var arr = existsField.value.split(',');
		        $.each(arr, function(ind, tmpItem){
		        	if(tmpItem && tmpItem.length > 0) {
		        		existsObj[tmpItem] = tmpItem;
		        	}
		        });
	        }
	        if(addField && addField.value.length > 0) {
		        var arr = addField.value.split(',');
		        $.each(arr, function(ind, tmpItem){
		        	if(tmpItem && tmpItem.length > 0) {
		        		existsObj[tmpItem] = tmpItem;
		        	}
		        });
	        }
	         if(delField && delField.value.length > 0) {
		        var arr = delField.value.split(',');
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
        <#elseif validator.validatorType = "fileValidator">
    	var error = "${validator.getMessage(action)?js_string}";
        var fieldName = "${validator.fieldName}".split(".")[1];
        var identifier = '[id$='+fieldName+'MultiIDsContainer]';
        //console.log(fieldName);
        //console.log($(identifier).children().size() < 2);
        if($(identifier).children().size() < 2 && validateRequiredFlag){
        	var divName = '[id$='+fieldName+'MultiIDsContainerDiv]';
        	var field = $(divName);
        	errorFields.push(field);
        	errorMessages.push(error);
        	showErrorField(field);
            errors = true;
        }
	    </#if>
        </#list>
		
		//PT验证
		var formId = '${parameters.id?replace('[^a-zA-Z0-9_]', '_', 'r')}';
		if(formId && formId.length > 5){
			var pageId = formId.substring(0, formId.length - 5);
			var datagrids = $('body').data(pageId + '_datagrids');
			
	
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
						            <#elseif validator.validatorType = "long">
						            	validator.message = "${validator.getMessage(action)?js_string}";
						            	<#if validator.min??>
						            		validator.min = "${validator.min}";
						            	</#if>
						            	<#if validator.max??>
						            		validator.max = "${validator.max}";
						            	</#if>
						            <#elseif validator.validatorType = "long_plus">
						            	validator.message = "${validator.getMessage(action)?js_string}";
						            	<#if validator.min??>
						            		validator.min = "${validator.min}";
						            	</#if>
						            	<#if validator.max??>
						            		validator.max = "${validator.max}";
						            	</#if>
						            <#elseif validator.validatorType = "long_minus">
						            	validator.message = "${validator.getMessage(action)?js_string}";
						            	<#if validator.min??>
						            		validator.min = "${validator.min}";
						            	</#if>
						            	<#if validator.max??>
						            		validator.max = "${validator.max}";
						            	</#if>
						            <#elseif validator.validatorType = "longRang">
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
</#if>