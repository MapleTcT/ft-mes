import axios from 'axios';
/**
 * 请求地址参数
 * @param {string} url 地址栏
 * @param {string} name 参数值
 */
export const getUrlParams = (url = window.location.href) => {
  let curl = url;
  curl = decodeURI(curl); // 获取url中"?"符后的字串
  const theRequest = {};
  if (curl.indexOf('?') !== -1) {
    const str = curl.split('?');
    const strs = str[1].split('&');
    for (let i = 0; i < strs.length; i += 1) {
      // eslint-disable-next-line prefer-destructuring
      strs[i] = strs[i].split('#')[0];
      const [key, value] = strs[i].split('=');
      if (key !== '' && key !== undefined && key !== null) {
        theRequest[key] = value;
      }
    }
    return theRequest;
  }
  return '';
};

export const getSource = () => {
  // eslint-disable-next-line no-undef
  const { source = SOURCE } = getUrlParams() || {};
  return source;
};

// 获取授权码
export const getLicenseKey = () => {
  // let url = './licensekey.txt';
  // if (process.env.NODE_ENV !== 'production') url = '/src/licensekey.txt';
  const url = '/inter-api/systemconfig/v1/config/catalog/by/module?moduleCode=printer&key=spreadjs.licence';
  return axios.get(url);
};
