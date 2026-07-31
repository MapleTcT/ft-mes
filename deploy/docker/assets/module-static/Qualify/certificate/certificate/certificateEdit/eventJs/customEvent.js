//======================================================================================
//视图的事件，请不要在这里修改代码，修改的代码会被覆盖
//======================================================================================

/*==================================onload事件==================================*/
var Qualify_certificate_certificate_onload = function(){
	//修改时，资质编码为只读
if(ReactAPI.getParamsInRequestUrl().id != undefined && ReactAPI.getParamsInRequestUrl().id != "") {
	ReactAPI.getComponentAPI("Input").APIs("certificate.code").setReadonly(true);
}
}

/*==================================onsave事件==================================*/
var Qualify_certificate_certificate_onsave = function(){
	var certLevel = ReactAPI.getComponentAPI("SupDataGrid").APIs("Qualify_1.0.0_certificate_certificateEditdg1569417183963");
var levelRows = certLevel.getDatagridData();
var checkedRows = new Array();
var k = 0;
var nullK = 0;
//判断等级是否为空
for (var i = 0; i < levelRows.length; i++) {
  if (levelRows[i].code != null) {
    checkedRows[k] = certLevel.getValueByKey(i, "code");
    k = k + 1;
  } else {//等级存在空行
    nullK = nullK + 1;
  }
}
//是否设置默认等级
var result = ReactAPI.getSystemConfig({ moduleCode: "Qualify", key: "Qualify.setDefaultLevel" })["Qualify.setDefaultLevel"];
if (!result || result=='false') {
  //无等级数据提示
  if (levelRows.length <= 0) {
    ReactAPI.showMessage("w", ReactAPI.international.getText("Qualify.certificate.certificate.aRecordNeeded"));
    return false;
  }
  //空无等级数据提示
  if (checkedRows.length == 0) {
    ReactAPI.showMessage("w", ReactAPI.international.getText("Qualify.certificate.certificate.certLevelNotNull"));
    return false;
  }
}
if (nullK != 0) {
  //等级存在空行
  ReactAPI.showMessage("w", ReactAPI.international.getText("Qualify.certificate.certificate.certLevelIsNull"));
  return false;
}
//等级编码重复校验
for (var i = 0; i < checkedRows.length; i++) {
  var codeI = checkedRows[i];
  for (var j = i + 1; j < checkedRows.length; j++) {
    var codeJ = checkedRows[j];
    if (codeI == codeJ) {
      ReactAPI.showMessage("w", ReactAPI.international.getText("Qualify.certificate.certificate.certLevelRepeat"));
      return false;
    }
  }
}
}

/*==================================Qualify_1.0.0_certificate_certificateEditdg1569417183963_ptPageInit事件==================================*/
// var result=ReactAPI.getSystemConfig({entityCode:"Qualify_1.0.0_certificate",keys:"Qualify.setDefaultLevel"})["Qualify.setDefaultLevel"];
// if(result){
	// $("#btn-add").hide();
	// $("#btn-delete").hide();
// }

/*==================================deleteCustomBack(event,obj)事件==================================*/
function deleteCustomBack(event,obj){
   console.log(obj);
   console.log(event);
}

