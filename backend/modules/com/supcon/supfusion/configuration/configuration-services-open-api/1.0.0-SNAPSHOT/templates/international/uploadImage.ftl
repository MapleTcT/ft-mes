<@errorbar id="InternationalUploadformDialogErrorBar" />
<@s.form id="InternationalUploadform" action="/foundation/international/save-lang.action"  enctype="multipart/form-data" callback="foundation.international.callBackInfo">
		<@s.hidden name="language.key"/>
		<@s.hidden name="language.internationalKey"/>
	<div style="padding-left: 70px;padding-top: 50px;">
		<span style="color:#B30303;">${getHtmlText('foundation.international.imageLabel')}：</span>
		<input type="file" name="languagefile" onchange="CheckType(this);" />
	</div>
	<span class="description" style="padding-left: 50px;">${getHtmlText('foundation.international.imageWarning')}</span>
</@s.form>
<script type="text/javascript" charset="UTF-8" language="javascript">
var AllImgType=".jpg|.jpeg|.gif|.bmp|.png|"//全部图片格式类型
function CheckType(obj)
{
  if(obj.value==""){
  	return false;
  }
  var FileType=obj.value.substr(obj.value.lastIndexOf(".")).toLowerCase();
  if(AllImgType.indexOf(FileType+"|")==-1) //判断文件类型是否允许上传
  {
	  var ErrMsg="${getHtmlText('foundation.international.imageTypeLimit')}";
	  InternationalUploadformDialogErrorBarWidget.show(ErrMsg,"f");
	  return false;
  }
    return true;
}

 </script>