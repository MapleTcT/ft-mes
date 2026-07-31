var more = getLocalMessage("foundation.more");//更多
var isLoading = getLocalMessage("foundation.common.tips.loading"); //  正在加载...
var noInfo = getLocalMessage("foundation.workflow.nodata"); //无信息
var error = getLocalMessage("SupPictureUploader.serverError"); // 	服务器出错
var loading = '<div style="height: 34px; text-align: center;"  class="beamPortalDataGrid"><label class="datagrid-loading">' + isLoading + '</label></div>';//正在加载...
var noData = '<div style="height: 34px; text-align: center;"  class="beamPortalDataGrid"><span>' + noInfo + '</span></div>';//  无信息
var error = '<div align="" style="padding-top: 8px;padding-bottom: 4px; text-align: center;"><span i18n="foundation.workflow.nodata">' + error + '</span></div>'; // 展示无信息  BEAM2.custom.randon1558612945963  错误

/**
 * 显示加载中
 */
function showLoading(id) {
    $(id + " #content").html(loading);
}
/**
 * 显示无数据
 */
function showNoData(id) {
    $(id + " #content").html(noData);
}
/**
 * 显示异常
 */
function showError(id) {
    $(id + " #content").html(error);
}

/**
 * 改变当前点击样式 
 */
function changeCurrent(e, id) {
    if (e != null) {
        //去掉所有 li的 current 类
        $(id).removeClass("current");
        //当前点击li 添加 current类
        $(e).parent().parent().parent().addClass("current");
    }
}
/**
 * 根据时间类型获取起止日期
 * @param timeType  0 本周 1 下周 2 本月
 */
function getTimeInfo(date, timeType) {
    //开始时间
    var timeStart = "";
    //结束时间
    var timeEnd = "";
    switch (timeType) {
        case 0:
            //本周
            timeStart = getFirstDayInWeek(date);
            timeEnd = getLastDayInWeek(date);
            break;
        case 1:
            //下周
            //获取当前日期一周后的日期
            date.setDate(date.getDate() + 7);
            timeStart = getFirstDayInWeek(date);
            timeEnd = getLastDayInWeek(date);
            break;
        case 2:
            //本月
            timeStart = getFirstDayInMonth(date);
            timeEnd = getLastDayInMonth(date);
    }
    return {
        timeStart: timeStart,
        timeEnd: timeEnd
    }
}
/**
 * 请求数据
 */
function getData(params) {
    var requestParams = {
        pageNo: 1,
        pageSize: 8,
        paging: true,
        permissionCode: params.permissionCode,
    }
    if(params.fastQueryCond!=undefined&&params.fastQueryCond!=null){
       requestParams.fastQueryCond= JSON.stringify(params.fastQueryCond)
	}
    // 请求数据
    $.ajax({
        url: params.url,
        type: 'post',
        async: true,
        dataType: "json ",
        contentType: "application/json",
        data: JSON.stringify(requestParams),
        beforeSend: function () {
            showLoading(params.domId);
        },
        success: function (res) {
            if (res && res.code == 200) {
                //请求返回成功
                params.success(res.data);
            } else {
                //请求返回失败
                showError(params.domId);
            }
        },
        error: function () {
            //服务器异常
            showError(params.domId);
        }
    });
}
/**
 * 渲染表格数据
 * @param domId 页面最外层DOM ID
 * @param contentDOM 数据内容DOM结构
 * @param moreMenuCode 更多按钮 打开的页面菜单code
 */
function refreshDom(domId, contentDOM, moreMenuCode) {
    if (contentDOM === '') {
        showNoData(domId);
    } else {
        // 添加组件数据
        var contentHtml = '<ul class="port-list">';
        contentHtml += contentDOM;
        contentHtml += '</ul>';
        // 更多按钮
        contentHtml += '<a href="#" class="beamPortalDataGridLink" onclick="fnOpenMenu(this)" code="' + moreMenuCode + '"><span>' + more + '</span></a>';
        $(domId + ' #content').html(contentHtml);
    }
}


Date.prototype.Format = function (fmt) {
    var o = {
        "M+": this.getMonth() + 1, //月份   
        "d+": this.getDate(), //日   
        "h+": this.getHours(), //小时   
        "m+": this.getMinutes(), //分   
        "s+": this.getSeconds(), //秒   
        "q+": Math.floor((this.getMonth() + 3) / 3), //季度   
        "S": this.getMilliseconds() //毫秒   
    };
    if (/(y+)/.test(fmt)) fmt = fmt.replace(RegExp.$1, (this.getFullYear() + "").substr(4 - RegExp.$1.length));
    for (var k in o) {
    if (new RegExp("(" + k + ")").test(fmt)) fmt = fmt.replace(RegExp.$1, RegExp.$1.length == 1 ? o[k] : ("00" + o[k]).substr(("" + o[k]).length));
    }return fmt;
};