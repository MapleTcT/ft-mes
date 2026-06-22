window.importCommonJs || loadScript("/msService/WTS/commonJs/loader");
importCommonJs("wtsCommon");
var CurrentTicketsKey = "workAppointment_6.1.6.1_workAction_workActionEditdg1654668908211";

//var HazidTicketKey = "WAPS_5.2.8.1_workAction_workActionEditdg1653898587755";

//var RiskSafeTicketKey = "WAPS_5.2.8.1_workAction_workActionEditdg1653898597611";

var ticketType = new Map([
    ["WTS_workType/fireWork", "动火安全作业"],
    ["WTS_workType/limitSpaceWork", "受限空间安全作业"],
    ["WTS_workType/blockWork", "盲板抽堵安全作业"],
    ["WTS_workType/heightWork", "高处安全作业"],
    ["WTS_workType/liftWork", "吊装安全作业"],
    ["WTS_workType/electricityWork", "临时用电安全作业"],
    ["WTS_workType/soilWork", "动土安全作业"],
    ["WTS_workType/breakWork", "断路安全作业"]
])

var LvGroup = new Map([
    ["fireWork", [{ key: "workAppointment_ticketLv/fireLv1", value: "一级动火" },
    { key: "workAppointment_ticketLv/fireLv2", value: "二级动火" },
    { key: "workAppointment_ticketLv/fireLvS", value: "特级动火" }]
    ],
    [
        "heightWork", [{ key: "workAppointment_ticketLv/highLv1", value: "一级高处" },
        { key: "workAppointment_ticketLv/highLv2", value: "二级高处" },
        { key: "workAppointment_ticketLv/highLv3", value: "三级高处" },
        { key: "workAppointment_ticketLv/highLvS", value: "特级高处" }]
    ],
    [
        "liftWork", [{ key: "workAppointment_ticketLv/liftLv1", value: "一级吊装" },
        { key: "workAppointment_ticketLv/liftLv2", value: "二级吊装" },
        { key: "workAppointment_ticketLv/liftLv3", value: "三级吊装" }]
    ],
    ["other", [{ key: "workAppointment_ticketLv/noneLv", value: "无" }]]
]);

//加载过的危害识别与安全措施
/**
 * key: "WTS_workType/breakWork"
 *  value:
 *   candidateValue: (3) [{…}, {…}, {…}]
 *   riskSafeyPart: (3) [{…}, {…}, {…}]
 */
var hadHazid$Risk = new Map();

//选作业票类型后--添加对应的安全措施与危害识别
function onChangeTicket(value) {
    var CurrentTicketsApi = ReactAPI.getComponentAPI()["SupDataGrid"].APIs(CurrentTicketsKey);
   // var HazidTicketApi = ReactAPI.getComponentAPI()["SupDataGrid"].APIs(HazidTicketKey);
   // var RiskSafeTicketApi = ReactAPI.getComponentAPI()["SupDataGrid"].APIs(RiskSafeTicketKey);

    if (value) {//有值--添加

        //校验重复
        var thisRow=   CurrentTicketsApi.getSelecteds()[0].rowIndex ;
        if(CurrentTicketsApi.getDatagridData().filter(element=>element.rowIndex!=thisRow&&element.workType!=null).map(o => o.workType.id).includes(value)){
            ReactAPI.showMessage("w","作业类型不允许重复！");
            setTimeout( ()=>{  CurrentTicketsApi.setValueByKey( thisRow , "workType", null); } )
            return false;
        }

      /*  //获取作业类型作业下的--危害识别与安全措施
        if (hadHazid$Risk.get(value) == undefined) {
            var result = ReactAPI.request({
                type: "get",
                data: { ticketType: value },
                url: "/msService/WAPS/workPlan/workPlan/getHazid$RiskByTicketType",
                async: false
            });
            hadHazid$Risk.set(value, result.data)
        }

        //将危害识别与安全措施的id赋值到sourceId后清空--并添加作业票类型
        hadHazid$Risk.get(value).candidateValue.forEach(o => { o.sourceId = o.id; o.id = undefined; o.workType = { id: value, value: ticketType.get(value) } });
        hadHazid$Risk.get(value).riskSafeyPart.forEach(o => { o.sourceId = o.id; o.id = undefined; o.workType = { id: value, value: ticketType.get(value) } });
        //赋值
        HazidTicketApi.addLine(hadHazid$Risk.get(value).candidateValue, true);
        RiskSafeTicketApi.addLine(hadHazid$Risk.get(value).riskSafeyPart, true);
        */
        //作业票类型赋候选值
        setLvOptions(value,thisRow )

    } else {//平台clear事件失效bug--删除值写在onChange

     /*   //危害识别删除
        var delectHazidIndex = HazidTicketApi.getDatagridData()
            .filter(thisData => !CurrentTicketsApi.getDatagridData().map(o => o.ticketType.id).includes(thisData.workType.id))
            .map(element => element.rowIndex).join(",")
        if (delectHazidIndex != "") {
            HazidTicketApi.deleteLine(delectHazidIndex);
        }
        //安全措施删除
        var delectRiskSafeIndex = RiskSafeTicketApi.getDatagridData()
            .filter(thisData => !CurrentTicketsApi.getDatagridData().map(o => o.ticketType.id).includes(thisData.workType.id))
            .map(element => element.rowIndex).join(",")
        if (delectRiskSafeIndex != "") {
            RiskSafeTicketApi.deleteLine(delectRiskSafeIndex);
        } */

    }

}

/**
 * 根据作业票类型设置--->作业等级候选值
 * @param ticketType
 * @param rowIndex
 */
function setLvOptions(ticketType, rowIndex) {
    var currentValue= ReactAPI.getComponentAPI("SupDataGrid").APIs(CurrentTicketsKey).getValueByKey(rowIndex,"ticketLv" )

    var thisOptions;
    switch (ticketType) {
        case "WTS_workType/fireWork":
            thisOptions = LvGroup.get("fireWork");
            break;
        case "WTS_workType/heightWork":
            thisOptions = LvGroup.get("heightWork");
            break;
        case "WTS_workType/liftWork":
            thisOptions = LvGroup.get("liftWork");
            break;
        default:
            thisOptions = LvGroup.get("other");

            break;
    }
    if(thisOptions[0].key=='workAppointment_ticketLv/noneLv'){//无--->直接赋值

        ReactAPI.getComponentAPI("SupDataGrid").APIs(CurrentTicketsKey).setValueByKey(rowIndex,"ticketLv",{ id : 'workAppointment_ticketLv/noneLv' , value:'无'  }  );

    }else if( currentValue&&!thisOptions.includes(currentValue.id ) ){//如果当前级不在范围内

        ReactAPI.getComponentAPI("SupDataGrid").APIs(CurrentTicketsKey).setValueByKey(rowIndex,"ticketLv",null );
    }
    //设置候选值
    ReactAPI.getComponentAPI("SupDataGrid")
        .APIs(CurrentTicketsKey).setCellOptions(rowIndex, "ticketLv", thisOptions);
}


//删行
function deleteTicketLine() {
    var CurrentTicketsApi = ReactAPI.getComponentAPI()["SupDataGrid"].APIs(CurrentTicketsKey);
   // var HazidTicketApi = ReactAPI.getComponentAPI()["SupDataGrid"].APIs(HazidTicketKey);
    //var RiskSafeTicketApi = ReactAPI.getComponentAPI()["SupDataGrid"].APIs(RiskSafeTicketKey);

     var selectLine=     CurrentTicketsApi.getSelecteds();

     if(selectLine.length==0 ){
        ReactAPI.showMessage("w","请选择一条记录进行操作！")
        return false;
     }else{
        //删行
        CurrentTicketsApi.deleteLine(selectLine[0].rowIndex +"" );
     /*   //危害识别删除
        var delectHazidIndex = HazidTicketApi.getDatagridData()
        .filter(thisData => !CurrentTicketsApi.getDatagridData().map(o => o.ticketType.id).includes(thisData.workType.id))
        .map(element => element.rowIndex).join(",")
        if (delectHazidIndex != "") {
        HazidTicketApi.deleteLine(delectHazidIndex);
        }
        //安全措施删除
        var delectRiskSafeIndex = RiskSafeTicketApi.getDatagridData()
        .filter(thisData => !CurrentTicketsApi.getDatagridData().map(o => o.ticketType.id).includes(thisData.workType.id))
        .map(element => element.rowIndex).join(",")
        if (delectRiskSafeIndex != "") {
        RiskSafeTicketApi.deleteLine(delectRiskSafeIndex);
        }
         */
     }
}





function onload(){
    //iframe打开
   if(ReactAPI.getParamsInRequestUrl().openType=="iframe" ){
     //  头部隐藏
    $('#app > div > form > div.m-layout-mian > div > div > div:nth-child(1) > div').hide();
    $('.Nfi2S-uX').hide();
   }

}


//保存事件
function onsave() {
    try {
        var saveData = getRegularSaveData(window)
        saveData.id = ReactAPI.getParamsInRequestUrl().id
        //自定义校验
        if (!actionCustomCheck()) {
            return false;
        }
        //校验通过
        window.parent.workActions.push(saveData);
        window.parent.readyIndex.push(window.parent.currentIndex);
        return false;
    } finally {
        return false;
    }
}


function actionCustomCheck() {

    var errorMsg = "";
    var guardianInside = ReactAPI.getComponentAPI()["Reference"].APIs('workAction.guardianInside.name').getValue()
    var guardianOutside = ReactAPI.getComponentAPI()["Reference"].APIs('workAction.guardianOutside.staffName').getValue()

    if (guardianInside.length == 0 && guardianOutside.length == 0) {
        errorMsg += "内外部监护人至少填写一个！</br>"
    }

    var tickets = ReactAPI.getComponentAPI()["SupDataGrid"].APIs(CurrentTicketsKey).getDatagridData();

    if (tickets.length == 0) {
        errorMsg += "作业票信息至少增加一行！</br>"
    } else {

        //保存的数据  空对象 变 null -->判空要写两种情况
        var errorTickets = tickets.filter(o => {

          if (o.workDeptInside != null && o.workDeptInside.id != null) {
            return false;
          } else if (o.workDeptOutside != null && o.workDeptOutside.id != null) {
            return false;
          } else {
            return true;
          }


        });


        if (errorTickets.length > 0) {

            errorMsg += "第" + errorTickets.map(o => (o.rowIndex + 1) + "").join(",") + "行作业票信息内外部作业单位至少填写一个！</br>"

        }
    }

    if (errorMsg != "") {

        ReactAPI.showMessage('f', errorMsg)
        return false

    }

    return true;

}
