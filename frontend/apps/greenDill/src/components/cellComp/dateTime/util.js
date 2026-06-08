import _commonJs from "@/assets/js/itemList/index.js";

const formatDefaultTime = (val, showFormat) => {
  let time = val;
  switch (val) {
    // 当前时间
    case "currentTime":
    // 当天
    case "today":
      time = _commonJs.getDateFormat(showFormat, new Date());
      break;
    case "firstday":
      // 当月第一天
      {
        const date = new Date();
        const year = date.getFullYear();
        const month = date.getMonth() + 1;
        const complexTime = `${year}/${month}/1 00:00:00`;
        time = _commonJs.getDateFormat(showFormat, new Date(complexTime));
      }
      break;
    case "lastday":
      // 当月最后一天
      {
        const date = new Date();
        const year = date.getFullYear();
        const month = date.getMonth() + 1;
        const lastDay = new Date(year, month, 0);
        const day = lastDay.getDate();
        const complexTime = `${year}/${month}/${day} 23:59:59`;
        time = _commonJs.getDateFormat(showFormat, new Date(complexTime));
      }
      break;
    case "nextsevenday":
      // 七天后
      {
        const date = new Date(); // 获取今天日期
        date.setDate(date.getDate() + 7); // 七天后
        time = _commonJs.getDateFormat(showFormat, date);
      }
      break;
    default:
      break;
  }
  return time;
};

/**
 * 获取格式起始和结束时间
 * @params {object} timeThis vue组件对象(this)
 * @params {time} startyear 起始时间
 */
const startAndEndTime = (timeThis, startyear) => {
  (function() {
    const curtime = new Date();
    const year = curtime.getFullYear();
    const month = curtime.getMonth();
    const date = curtime.getDate();
    const start = new Date(`${startyear}/${month + 1}/${date}`);
    const end = new Date(`${year + 100}/${month + 1}/${date}`);
    timeThis.startdate = start;
    timeThis.enddate = end;
    timeThis.curstart = start;
    timeThis.curend = end;
  })();
};

export { formatDefaultTime, startAndEndTime };



// WEBPACK FOOTER //
// ./src/components/cellComp/dateTime/util.js