<#assign rootIdStr="-1">
<#if type??>
	<#if type == 2>
		<#assign postfix="_service">
		<#assign rootIdStr="10">
	<#elseif type == 3>
		<#assign postfix="_view">
		<#assign rootIdStr="6">
	</#if>
</#if>
<div class="customcode_search_box" id="customcode_search_box${postfix!}">
	<div class="customcode_search_input_wrap" id="customcode_search_input_wrap${postfix!}">
		<span class="icon" title="搜索"></span>
		<input type="text" autocomplete="off" class="customcode_search_input" id="customcode_search_input${postfix!}">
	</div>
	<div class="customcode_search_result" id="customcode_search_result${postfix!}">
		<ul class="customcode_search_list">
		</ul>
	</div>
</div>
<@tree id="ec_customcode_Tree${postfix!}" rootId="${rootIdStr}" nameCol="name" dataUrl="/msService/ec/customCode/list?entityCode=${entityCode!}&type=${type!1}" params="path" rootName="${getText('ec.configMenu.customCode')}"
callback="{onClick:function(event,treeId,node){ec.customcode.dealCustomCode(node);},onDblClick:function(event,treeId,node){ec.customcode.dblClickCustomCode(treeId,node);}}" />
<script>
	var now = -1;
	var resLength = 0;
	$('#customcode_search_input${postfix!}').keyup(function(event) {
		var searchInputVal = $('#customcode_search_input${postfix!}').val();	
		if (event.keyCode == 38 || event.keyCode == 40) {
			return;
		}
		if (searchInputVal != '') {
			$.ajax({
				type : "GET",
				url : "/msService/ec/customCode/list?entityCode=${entityCode!}&type=${type!1}&id=-1&path=-1",
				async : true,
				data : {"filterName" : searchInputVal},
				dataType : 'json',
				success : function(res) {
					resLength = res.length;
					var oli_i = "";
					var len = res.length;
					if (len > 10) {
						len = 10;
					}
					$('#customcode_search_result${postfix!} ul.customcode_search_list').html('');
					for (var i = 0; i < len; i++) {
						oli_i = "<li path=" +  res[i].path + ">" + res[i].name + "</li>";
						$('#customcode_search_result${postfix!} ul.customcode_search_list').append(oli_i);
						$('#customcode_search_result${postfix!} ul.customcode_search_list li').eq(i).hover(function(){
							$(this).addClass('active').siblings().removeClass('active')
						});
						$('#customcode_search_result${postfix!} ul.customcode_search_list li').eq(i).click(function(){
							customcodeFindNode${postfix!}();
						});
					}
					$("#customcode_search_result${postfix!} ul.customcode_search_list").css("display","block");
				},
				error : function(res) {
					console.log(res)  
				}
			});
		} else {
			$('#customcode_search_result${postfix!} ul.customcode_search_list').html('');
			$("#customcode_search_result${postfix!} ul.customcode_search_list").css("display","none");
		}
	});

	$('#customcode_search_input${postfix!}').keydown(function(ev){
		if (ev.keyCode == 40) {
			now++;
			$('#customcode_search_result${postfix!} ul.customcode_search_list li').eq(now).addClass('active').siblings().removeClass('active');
			if (now >= resLength - 1) {   
				now = -1;
			}
		}
		if (ev.keyCode == 38) {
			now--;
			$('#customcode_search_result${postfix!} ul.customcode_search_list li').eq(now).addClass('active').siblings().removeClass('active');
			if (now < -1) {
				now = resLength - 1;
			}
		}
		if (ev.keyCode == 13) {
			customcodeFindNode${postfix!}();
		}
	});

	function customcodeFindNode${postfix!}() {
		var treeObj = $.fn.zTree.getZTreeObj("ec_customcode_Tree${postfix!}"); //获取ztree对象
		var nodes = treeObj.getNodesByFilter(function (node) { return node.level == 0 }); //获取根节点
		var filePath = $('#customcode_search_result${postfix!} ul.customcode_search_list li.active').attr('path');
		ec.customcode.findNode(filePath,treeObj,nodes[0]);	
		$('#customcode_search_input${postfix!}').val('');
		$('#customcode_search_result${postfix!} ul.customcode_search_list').html('');
		$("#customcode_search_result${postfix!} ul.customcode_search_list").css("display","none");
		now = -1;
		resLength = 0;
	}
</script>