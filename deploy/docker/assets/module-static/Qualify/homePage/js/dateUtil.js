//日期格式化，返回值形式为yyyy-mm-dd
function dateFormat(date) {
    if(!(date instanceof Date)){
        console.error("date 不是日期类型!");
        return "";
    }
    var yyyy = date.getFullYear(); //年
    var mm = (Array(2).join(0) + (date.getMonth() + 1)); //月
    var dd = (Array(2).join(0) + date.getDate()).slice(-2); //日
    return yyyy + "-" + mm + "-" + dd;
}
//日期格式化，返回值形式为yyyy-mm-dd
function dateTimeFormat(dateTime) {
  	if(dateTime===""){
		return dateTime
	}
    var date = new Date(dateTime);
    var yyyy = date.getFullYear(); //年
    var mm = (Array(2).join(0) + (date.getMonth() + 1)); //月
    var dd = (Array(2).join(0) + date.getDate()).slice(-2); //日
    return yyyy + "-" + mm + "-" + dd;
}
/**
 * 计算日期所在周的周一
 * @param {Date} date
 */
function getFirstDayInWeek(date) {
    //获取周数 1-7
    //周日为0，设置为7
    var weekIndex = date.getDay()||7;
    //获取 (weekIndex-1)天前的日期 即为周一
    date.setDate(date.getDate() - (weekIndex-1));
    return dateFormat(date);
}
/**
 * 计算日期所在周的周日
 * @param {Date} date
 */
function getLastDayInWeek(date) {
    //获取周数 1-7
    //周日为0，设置为7
    var weekIndex = date.getDay()||7;
    //获取 (7-weekIndex)天后的日期 即为周日
    date.setDate(date.getDate() + (7-weekIndex));
    return dateFormat(date);
}
/**
 * 计算日期所在月的第一天
 * @param {Date} date
 */
function getFirstDayInMonth(date) {
    date.setDate(1);
    return dateFormat(date);
}
/**
 * 计算日期所在月的第一天
 * @param {Date} date
 */
function getLastDayInMonth(date) {
    //获取当前日期年、月
    var year = date.getFullYear();
    var month = date.getMonth();
    //获取下个月第一天
    var nextMonthDate  = new Date(year,month+1,1);
    //nextMonthDate减一天 即为当前月最后一天
    nextMonthDate.setDate(nextMonthDate.getDate()-1);
    return dateFormat(nextMonthDate);
}