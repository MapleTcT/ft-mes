// import _ from 'lodash';
import pako from 'pako';

/**
* @description 判断是否属于知之浏览器
* @returns {boolean} true false
*/
export function isZhizhiBrowser() {
  return !!navigator.userAgent.match(/Zhizhi/i);
}

export function isMobile() {
  return !!(navigator.userAgent.match(/(phone|pad|pod|iPhone|iPod|ios|iPad|Android|Mobile|BlackBerry|IEMobile|MQQBrowser|JUC|Fennec|wOSBrowser|BrowserNG|WebOS|Symbian|Windows Phone)/i));
}

/**
 * @description json压缩
 * @param {*} json
 */
export function zip(json) {
  try {
    const str = JSON.stringify(json);
    const deflate = new pako.Deflate({ level: 6, to: 'string' });
    deflate.push(str, true);
    return deflate.result;
  } catch (e) {
    return json;
  }
}

/**
 * @description json解压
 * @param {*} binaryString
 */
export function unzip(binaryString) {
  try {
    const str = pako.inflate(binaryString, { to: 'string' });
    const json = JSON.parse(str);
    // console.log(str, json);
    return json;
  } catch (e) {
    return binaryString;
  }
}

/**
 * URL取参
 * @param {*} queryString
 */
export function getQueryString(queryString = '') {
  const parameters = queryString.split('&');
  const arr = [];
  for (const parameter of parameters) {
    const content = parameter.split('=');
    arr.push(content);
  }
  return _.fromPairs(arr) || {};
}
