<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<meta name="viewport" content="width=device-width" />	
<style type="text/css">
body {
	margin: 0;
}
.bc {
	padding-left:40px;
	height:35px;
	line-height:35px;
	display:block;
}
.page403-content {
	width: 100%;
    max-width: 500px;
    height: 52px;
    position: absolute;
    top: 50%;
	left: 50%;
	padding-left: 20px;
    font-size: 12px;
    transform: translate(-50%, -50%);
    -webkit-transform: translate(-50%, -50%);
}
.page403-msg {
	display: inline-block;
    width: 85%;
    bottom: 20px;
    position: relative;
    white-space: normal;
}
</style>
</head>
<body onload="page403Load()">
<div class="page403-content">
	<img src="/bap/static/errorimg/noPermission.gif"></img>
	<span class="page403-msg">${errMsg.exceptionMsg}</span>
</div>
<script type="text/javascript" language="javascript" charset="utf-8">
function page403Load(){
	var pd = window.parent;
	if(pd && pd.foundation && pd.foundation.common) {
		pd.foundation.common._errorCode = 403;
	}
}
</script>
</body>
</html>