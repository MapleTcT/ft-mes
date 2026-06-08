<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />

	<title>${getText("foundation.basic.menuManagement")} </title>

	<link href="/bap/static/foundation/css/style.css" rel="stylesheet" type="text/css" charset="utf-8"/>
	<@head/>
</head>
<body class="yui-skin-sam">
</body>
<script type="text/javascript" charset="utf-8" language="javascript">
(function(){
    //注册命名空间
    CUI.ns("foundation.pageMenuInfo");
  	foundation.pageMenuInfo.refresh = function(){
  		var url="/msService/ec/foundation/menuInfo/frame?configPage=true&moduleArtifact=${moduleArtifact!}";
    	CUI('body').load(url, null, null, false);
    }
	foundation.pageMenuInfo.refresh(); 	 
})();	
</script>
</html>
