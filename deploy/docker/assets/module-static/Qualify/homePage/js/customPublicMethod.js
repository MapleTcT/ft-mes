/**
 * 计算多少周周末日期
 * @param {Integer} nWeeks
 */
function fnGetWeekEndDate(nWeeks) {
	var _dNow = new Date(); // 当前时间
	var _dMonday = new Date(_dNow - (_dNow.getDay()) * 86400000); // 当前周第一天
	_dMonday.setDate(_dMonday.getDate() + 7 * (nWeeks)); // 跨周计算月份
	var _nMonth = Number(_dMonday.getMonth()) + 1; // 当前月
	// 日期长度补充
	_nMonth.toString().length == 1 ? _nMonth = '0' + _nMonth : _nMonth;
	_nDay = _dMonday.getDate();
	_nDay.toString().length == 1 ? _nDay = '0' + _nDay : _nDay;
	var _nYear = _dMonday.getYear(); // 当前年份
	_nYear += (_nYear < 2000) ? 1900 : 0; // 跨19世纪当前年份
	return _nYear + "-" + _nMonth + "-" + _nDay;
}

/**
 * 计算多少月月末日期
 * @param {Integer} nMonths
 */
function fnGetMonthEndDate(nMonths) {
	var _dNow = new Date(); // 当前时间
	var _nYear = _dNow.getYear(); // 当前年份
	_nYear += (_nYear < 2000) ? 1900 : 0; // 跨19世纪当前年份
	var _nMonth = _dNow.getMonth() + nMonths; // 当前月
	// 跨年处理数据 月减十二 年加一
	while(_nMonth > 12) {
		_nMonth -= 12;
		_nYear++;
	}
	// 计算当前月天数
	var _dMonthStart = new Date(_nYear, _nMonth, 1);
	var _dMonthEnd = new Date(_nYear, _nMonth + 1, 1);
	var _nMonthDays = (_dMonthEnd - _dMonthStart) / (1000 * 60 * 60 * 24); // 当前月天数
	_nMonth += 1;
	// 日期长度补充
	_nMonth.toString().length == 1 ? _nMonth = '0' + _nMonth : _nMonth;
	return _nYear + "-" + _nMonth + "-" + _nMonthDays;
}

/**
 * 计算多少周周末相距小时
 * @param {Integer} nWeeks
 */
function fnGetWeekEndHours(nWeeks) {
	// 得到n周之后的周末八点
	var _nextWeek = fnGetWeekEndDate(nWeeks);
	// 当前周末日期 - 当前时间 + 时区差 + 一天 为相差微秒
	var _nDifferMicroseconds = new Date(_nextWeek) - new Date() + new Date().getTimezoneOffset() * 60 * 1000 + 24 * 60 * 60 * 1000;
	var _nDifferHours = _nDifferMicroseconds / 1000 / 60 / 60;
	return _nDifferHours.toFixed(6);
}

/**
 * 计算多少月月末相距小时
 * @param {Integer} nMonths
 */
function fnGetMonthENdHours(nMonths) {
	// 得到n月之后的周末日期
	var _nextMonth = fnGetMonthEndDate(nMonths);
	// 当前周末日期 - 当前时间 + 时区差 + 一天 为相差微秒
	var _nDifferMicroseconds = new Date(_nextMonth) - new Date() + new Date().getTimezoneOffset() * 60 * 1000 + 24 * 60 * 60 * 1000;
	var _nDifferHours = _nDifferMicroseconds / 1000 / 60 / 60;
	return _nDifferHours.toFixed(6);
}

/**
 * 将时间戳转换为时间 YYYY-MM-DD
 * @param {Integer} nDate
 */
function fnTimeToDate(nDate) {
	var _dDate = new Date(nDate);
	var _nYear = _dDate.getFullYear(); // 年
	var _nMonth = _dDate.getMonth() + 1; // 月份 getMonth()得到的月份是0-11  
	var _nDay = _dDate.getDate(); // 日
	_nMonth = _nMonth < 10 ? "0" + _nMonth : _nMonth;
	_nDay = _nDay < 10 ? "0" + _nDay : _nDay;
	return _nYear + '-' + _nMonth + '-' + _nDay;
}

/**
 * 打开设备查看页面
 * @param {String} beamID
 */
function fnOpenBeamView(beamID) {
	var _nWindowHeight = window.screen.availHeight - 63;
	var _nWindowWidth = window.screen.availWidth - 20;
	var _sShowStyle = "width=" + _nWindowWidth + ",height=" + _nWindowHeight + ",scrollbars=yes,top=0,left=0,resizable =yes,toolbar=no,menubar=no,location=no,status=yes";
	var url = '/msService/EAM/baseInfo/baseInfo/baseInfoView?id=' + beamID;
	window.open(url, "", _sShowStyle);
}

/**
 * 打开URL页面
 * @param {String} beamID
 */
function fnOpenNewWindow(URL) {
	var _nWindowHeight = window.screen.availHeight - 63;
	var _nWindowWidth = window.screen.availWidth - 20;
	var _sShowStyle = "width=" + _nWindowWidth + ",height=" + _nWindowHeight + ",scrollbars=yes,top=0,left=0,resizable =yes,toolbar=no,menubar=no,location=no,status=yes";
	window.open(URL, "", _sShowStyle);
}

/**
 * 根据code打开菜单
 * 对应的菜单格式如下，注意需要保留onclick和 code属性
 * <span onclick="openMenu(this)" code="base_roleManage">角色管理</span>
 * @param {Object} el
 */
function fnOpenMenu(el) {
	$('#v3_menu_search_box').data('menu-finder').openMenu($(el));
}

/**
 * 获取用户权限
 * @param {String} editMenuOperateCode
 */
function fnGetUserPc(sEditMenuOperateCode) {
	// 请求数据
	$.ajax({
		url: '/public/BEAM2/beamHomePage/getUserPc.action',
		async: false,
		dataType: "json ",
		data: {
			'editMenuOperateCode': sEditMenuOperateCode
		},
		success: function(msg) {
			return msg.resultJson;
		}
	});
}

/**
 * 将小数按照配置，保留小数位 by jiangshiyi
 * @param {Object} value
 * @param {Object} widget
 * @param {Object} key
 */
function fnFormatNum(value, widget, key){
	// 获得tr
	var _tr = widget._bTbody.rows[0];
	// 获得td
	var _elTd = YUD.getElementsBy(function(o) {
		if (o.getAttribute("key") === key) {
			return true;
		} else {
			return false;
		}
	}, "td", _tr)[0];
	// td列属性
	var _oColumn = widget._oColumnSet.keys[_elTd.cellIndex];
	// 保留小数位
	var _decimal;
	if (_oColumn.decimal && _oColumn.decimal != "") {
		_decimal = parseInt(_oColumn.decimal);
	} else {
		_decimal = 0;
	}
	if (value !== "") {
		value = parseFloat(value).toFixed(_decimal);
	}
	return value;
}

/**
 * 按照行号rowIndex对数组排序   by jiangshiyi
 * @param {Object} a
 * @param {Object} b
 */
function sortRowIndex(a,b){
	return a.rowHtmlObj.rowIndex-b.rowHtmlObj.rowIndex;
}
