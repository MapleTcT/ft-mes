<table width="100%" class="menubar" cellspacing="0">
	<tr>
		<td width="50" align="center" onmouseover="this.bgColor='#e2fec1';this.style.color='#3a6a01';" onmouseout="this.bgColor='';this.style.color='';"><a href="mainFrame">主页</a></td>
		<td width="1" class="menubar_dot"></td>
		<td width="70" align="center" id="allMenus" onmouseover="this.bgColor='#e2fec1';this.style.color='#3a6a01';" onmouseout="this.bgColor='';this.style.color='';">全部菜单</td>
		<td>&nbsp;</td>
	</tr>
</table>
<script type="text/javascript">
$(function(){
	$('#allMenus').popupmenu({
		items : ${parameters.menus!"[]"},
		titleMapper: '${parameters.titleMapper!"name"}',
        iconMapper: '${parameters.iconMapper!"icon"}',
        childrenMapper: '${parameters.childrenMapper!"children"}',
        idMapper: '${parameters.idMapper!"id"}',
        xhrMapper: '${parameters.xhrMapper!"url"}',
        onClick: ${parameters.onclick!'function(item){}'},
        onClose: ${parameters.onClose!'function(item){}'}
	});
});
</script>