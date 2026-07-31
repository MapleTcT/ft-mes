//======================================================================================
//视图的事件，请不要在这里修改代码，修改的代码会被覆盖
//======================================================================================

/*==================================onclick='addCertificateType()'事件==================================*/
function addCertificateType() {
	var selNode = ReactAPI.getComponentAPI("NavTree").APIs("Qualify_1.0.0_certificateType_cerTypeLayOut_certificateType_nt").getSelectedTreeNode();
	
	if(selNode == null) {
		ReactAPI.showMessage("w", ReactAPI.international.getText("TreeList.NavTree.chooseNode"));
		return false;
	}
	//新建分类
	if(selNode.id == -1) {
		ReactAPI.showMessage("w", ReactAPI.international.getText("Qualify.TreeList.NavTree.addNewClass"));
		return false;
	}
	
	if(selNode.leaf) {
		//末级节点需要判断是否已被资质关联，若已被关联不能新增下级分类
		ReactAPI.request({
			type: "get",
			data: {},
			url: "/msService/Qualify/certificateType/certificateType/haveCertificate?certificateTypeId=" + selNode.id
		},function(res) {
			console.log(res);
			if(res.haveCertificate) {
				ReactAPI.showMessage("w", ReactAPI.international.getText("Qualify.certificateType.certTypeIsRelated"));
				return false;
			} else {
				openCertificateTypeAdd(selNode.id);
			}
		});
		
	} else { //非末级节点打开新增编辑页面
		openCertificateTypeAdd(selNode.id);
	}
}

/*==================================onclick='deleteCertType()'事件==================================*/
function deleteCertType() {
	var selRows = ReactAPI.getComponentAPI("SupDataGrid").APIs("Qualify_1.0.0_certificateType_cerTypeLayOut_certificateType_sdg").getSelecteds();
	//未选择时，提示
	if(selRows.length == 0) {
		ReactAPI.showMessage("w", ReactAPI.international.getText("SupDatagrid.button.error"));
		return false;
	}
	//判断是否是当前登录人所属公司的数据
	var certTypeCId=selRows[0].cid;
	var currentPersonCID=ReactAPI.getUserInfo().company.id;
	if(certTypeCId != currentPersonCID){
		//无法操作非本公司数据
		ReactAPI.showMessage("w", ReactAPI.international.getText("ec.edit.button.uncurrent.company"));
		return false;
	}
	var ids = "";
	for(var i = 0; i < selRows.length; i++) {
		//不可删除顶层节点分类：人员资质和企业资质
		if(selRows[i].id == 1000 || selRows[i].id == 1001) {
			ReactAPI.showMessage("w", ReactAPI.international.getText("Qualify.operate.noDeletePermission"));
			return false;
		}
		ids += selRows[i].id + "@" + selRows[i].version + ",";
	}
	//删除分类id集合
	ids = ids.substring(0, ids.length - 1);
	//确认框
	ReactAPI.openConfirm({
		message: ReactAPI.international.getText("SupDatagrid.button.delete"),
		buttons: [
			{
				operatetype: "yes",
				text: ReactAPI.international.getText("ec.common.confirm"),
				type: "primary",
				onClick: function() {
					$.ajax({
						type: 'post',
						url: "/msService/Qualify/certificateType/certificateType/delete",
						data: {ids : ids},
						async: false,
						success: function(res) {
							ReactAPI.closeConfirm();
							//刷新列表
							ReactAPI.getComponentAPI("SupDataGrid").APIs("Qualify_1.0.0_certificateType_cerTypeLayOut_certificateType_sdg").refreshDataByRequst();
							//刷新树
							ReactAPI.getComponentAPI("NavTree").APIs("Qualify_1.0.0_certificateType_cerTypeLayOut_certificateType_nt").refreshTreeNode(-1);
							ReactAPI.showMessage("s", ReactAPI.international.getText("Notification.message.deleteSuccess"));
							
						},
						error: function(res) {
							ReactAPI.closeConfirm();
							var resultInfo = JSON.parse(res.responseText);
							ReactAPI.showMessage("f", resultInfo.message, ReactAPI.international.getText("Notification.message.title.info"));
						}
					});	
				}
			},
			{
				operatetype: "cancel",
				text: ReactAPI.international.getText("Button.text.cancel"),
				onClick: function() {
					ReactAPI.closeConfirm();
				}
			}
		]
	});
}

