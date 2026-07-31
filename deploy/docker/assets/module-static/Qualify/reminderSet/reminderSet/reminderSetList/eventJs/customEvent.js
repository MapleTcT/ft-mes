//======================================================================================
//视图的事件，请不要在这里修改代码，修改的代码会被覆盖
//======================================================================================

/*==================================onclick='customAdd()'事件==================================*/
function customAdd(){
	if(!ReactAPI.getComponentAPI("NavTree").APIs("Qualify_1.0.0_reminderSet_reminderSetLayout_reminderSet_nt").getSelectedTreeNode()){
		//请先选择部门
		ReactAPI.showMessage("w", ReactAPI.international.getText("Qualify.reminderSet.ReminderSet.chooseNodeFirst"));
		return false;
	}
	if(ReactAPI.getComponentAPI("NavTree").APIs("Qualify_1.0.0_reminderSet_reminderSetLayout_reminderSet_nt").getSelectedTreeNode().id==-1){
		//请先选择部门
		ReactAPI.showMessage("w", ReactAPI.international.getText("Qualify.reminderSet.ReminderSet.chooseNodeFirst"));
		return false;
	}
	ReactAPI.getPowerCode("Qualify_1.0.0_reminderSet_reminderSetLayout_reminderSetList_customAdd_add_Qualify_1.0.0_reminderSet_reminderSetList", function(res) {
		var pcParam=res['Qualify_1.0.0_reminderSet_reminderSetLayout_reminderSetList_customAdd_add_Qualify_1.0.0_reminderSet_reminderSetList'];
		var departmentId=ReactAPI.getComponentAPI("NavTree").APIs("Qualify_1.0.0_reminderSet_reminderSetLayout_reminderSet_nt").getSelectedTreeNode().id;
		var departmentName=ReactAPI.getComponentAPI("NavTree").APIs("Qualify_1.0.0_reminderSet_reminderSetLayout_reminderSet_nt").getSelectedTreeNode().name;
		ReactAPI.createDialog("reminderSetEditDailog", {
			title: ReactAPI.international.getText("Qualify.reminderSet.ReminderSet"),
			width: "400px",
			height: "350px",
			url: "/msService/Qualify/reminderSet/reminderSet/reminderSetEdit?__pc__="+pcParam+"&viewCode=Qualify_1.0.0_reminderSet_reminderSetLayout&entityCode=Qualify_1.0.0_reminderSet&iscrosscompany=false&openType=dialog&viewType=edit&departmentId="+departmentId+"&departmentName="+departmentName,
			onOk: function(event) {
				event.ReactAPI.submitFormData("save", function(result) {
					if(result){
						ReactAPI.destroyDialog("reminderSetEditDailog");
					}
				});
			},
			okText : ReactAPI.international.getText("Button.text.save"),
			onCancel: function() {
				ReactAPI.destroyDialog("reminderSetEditDailog");
			}
		});
	});
}

