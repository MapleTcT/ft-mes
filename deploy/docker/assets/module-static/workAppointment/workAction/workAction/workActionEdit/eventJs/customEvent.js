//======================================================================================
//视图的事件，请不要在这里修改代码，修改的代码会被覆盖
//======================================================================================

/*==================================onload事件==================================*/
var workAppointment_workAction_workAction_onload = function(){
	onload();
}

/*==================================onsave事件==================================*/
var workAppointment_workAction_workAction_onsave = function(){
	return onsave();
}

/*==================================onchange='onchangeTicketType(value)'事件==================================*/
function onchangeTicketType(value){
   onChangeTicket(value)
}

/*==================================onclick='customDeleteTicket(event)'事件==================================*/
function customDeleteTicket(event){
	deleteTicketLine()
}
