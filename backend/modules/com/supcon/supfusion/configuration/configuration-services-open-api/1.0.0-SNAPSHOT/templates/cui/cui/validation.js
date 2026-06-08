/*
 * $Id: validation.js 692578 2008-09-05 23:30:16Z davenewton $
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
function clearErrorMessages(form) {

}

function clearErrorLabels(form) {

}

function addError(form,oErrorWidget,errorFields,errorMessages) {
	try {
    	//显示错误信息
    	var errors = "";
    	for(var i=0; i<errorMessages.length; i++){
    		errors += "<p>" + errorMessages[i] + "</p>";
    	}
    	oErrorWidget.show(errors);
    	
    	//定位到第一个出错框
    	errorFields[0].focus();
    	//激活按钮
    	CUI("body").one("click", function(event){
    		if(CUI.Dialog) CUI.Dialog.toggleAllButton(CUI(event.target).parent().parent()[0], true);
    	});
    } catch (e) {
	}
}

function showErrorField(field){
	CUI(field).addClass('cui-error-fieldortext');
}
function removeErrorField(field){
	CUI(field).removeClass('cui-error-fieldortext');
}