import moment from 'moment';

export function getTimeDistance(type) {
  const now = new Date();
  const oneDay = 1000 * 60 * 60 * 24;

  if (type === 'today') {
    now.setHours(0);
    now.setMinutes(0);
    now.setSeconds(0);
    return [moment(now), moment(now.getTime() + (oneDay - 1000))];
  }

  if (type === 'week') {
    let day = now.getDay();
    now.setHours(0);
    now.setMinutes(0);
    now.setSeconds(0);

    if (day === 0) {
      day = 6;
    } else {
      day -= 1;
    }

    const beginTime = now.getTime() - day * oneDay;
    return [moment(beginTime), moment(beginTime + (7 * oneDay - 1000))];
  }

  const year = now.getFullYear();

  if (type === 'month') {
    const month = now.getMonth();
    const nextDate = moment(now).add(1, 'months');
    const nextYear = nextDate.year();
    const nextMonth = nextDate.month();
    return [
      moment(`${year}-${fixedZero(month + 1)}-01 00:00:00`),
      moment(
        moment(
          `${nextYear}-${fixedZero(nextMonth + 1)}-01 00:00:00`
        ).valueOf() - 1000
      )
    ];
  }

  return [moment(`${year}-01-01 00:00:00`), moment(`${year}-12-31 23:59:59`)];
}

export function fixedZero(val) {
  return val * 1 < 10 ? `0${val}` : val;
}

// 获取登录用户名
export function getCurrentUserName() {
  const loginMsg = localStorage.getItem('loginMsg');
  return loginMsg ? JSON.parse(loginMsg).user.userName : null;
}

/**
 * 请求浏览器参数值
 * @param {string} name - 参数名
 */
export const getQueryString = (name) => {
  const url = window.location.href;
  const paramIndex = url.indexOf('?');
  let res = null;
  if (paramIndex > -1) {
    const paramStr = url.substring(paramIndex + 1, url.length);
    const paramArr = paramStr.split('&');
    paramArr.forEach((item) => {
      const pi = item.split('=');
      const [key, value] = pi;
      if (key === name) res = value;
    });
  }
  return res;
};
/**
 * 用于get方法后面参数的拼接，传入data是对象
 * @param {*} data - 参数对象
 */
export const getUrlConcat = (data) => {
  let dataStr = ''; // 数据拼接字符串
  let url = '';
  Object.keys(data).forEach((key) => {
    dataStr += `${key}=${data[key]}&`;
  });
  if (dataStr !== '') {
    dataStr = dataStr.substr(0, dataStr.lastIndexOf('&')); // 去除掉最后一个"&"字符
    url = `${url}?${dataStr}`;
  }
  return url;
};

// 获取跨公司参照配置信息
export const getRefCompanyConfig = () => {
  let loginMsg = window.localStorage.getItem('loginMsg');
  if (loginMsg) {
    loginMsg = JSON.parse(loginMsg);
    const { currentCompany = {} } = loginMsg;
    const { id: defaultValue } = currentCompany;
    if (defaultValue) return { defaultValue };
  }
  return { defaultValue: 1000 };
};

export const IS_DEV = process.env.NODE_ENV !== 'production'; // 是否为调试模式
