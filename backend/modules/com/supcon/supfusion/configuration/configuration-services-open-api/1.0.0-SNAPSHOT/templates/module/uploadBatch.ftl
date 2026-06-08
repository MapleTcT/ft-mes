<!DOCTYPE html>
<html  xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="X-UA-Compatible" content="IE=edge" />
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<title>${getText('ec.module.upload')}</title>
<@maincss/>
<@mainjs/>
<style type="text/css">
	.upload-label{
		background:url(/bap/static/new/img/uploadBatchSelect.png);
		width: 352px;
		padding-top: 20px;
		height: 185px;
		display: block;
    	margin: 100px auto;
	}


	#receiveFile {  
    width: 138px;
    height: 53px;
    opacity: 0;
    cursor: pointer;
    -ms-filter:"alpha(opacity=0)";
    font-size: 26px;
    margin-left: 107px;
    margin-top: 14px;
    outline: none;
    border: none;
    text-indent: -9999px;

	}

	#filename {
	    margin-top: 44px;
	    display: block;
	    text-align: center;
	}
</style>

</head>
<body id="upload_page">
<script type="text/javascript">


	function beforesubmit(){
		var fileName = $('#receiveFile').val();
		if(!fileName || fileName.length == 0) {
			CUI.Dialog.alert("${getHtmlText("ec.module.generate.upload.select")}");
			return false;
		}else if(!fileName.endsWith('.zip')){
			CUI.Dialog.alert("${getHtmlText("ec.module.upload.fileTypeError")}");
			return false;
		}
		return true;
	}
	function changeuptype(obj){
		document.getElementById('changeFile').value= 'true';
		var val = obj.value;
		if(val.indexOf(".xml") > 0){
			document.getElementById("uptype").value = "xml";
		}
		var index = val.lastIndexOf('\\')+1;
		$('#filename').text(val.substring(index));
	}
	function uploadALL(obj){
	}
</script>
<div style="padding:15px 20px 0 20px;">
<form id="uploadBatchForm" target="transfer" onSubmit="javascript:return beforesubmit();" action="/msService/ec/module/listEntity" method="post" enctype="multipart/form-data">
<input type="hidden" id="uptype" name="uptype" value="${uptype!'zip'}" />
<@s.hidden name="module.code" />
<@s.hidden name="changeFile" />
<label class="upload-label" for="receiveFile" >	
	<input type="file" name="receiveFile" id="receiveFile" onchange="changeuptype(this)" />
	<span id="filename"></span>
</label>
<br/><br/>
</form>
</div>
<iframe style="display:none;" id="transfer" name="transfer" ></iframe>

<script type="text/javascript">
	function openfileinput(argument) {
		// if ($.browser.msie8 || $.browser.msie7){
		// 	$('#receiveFile').click();
		// }
	}
</script>
</body>
</html>