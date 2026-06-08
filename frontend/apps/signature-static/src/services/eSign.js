import axios from './request';

// function stringify(params) {
//   const items = [];
//   for (const i in params) {
//     if (
//       Object.prototype.hasOwnProperty.call(params, i) && (typeof params[i] === 'boolean' || params[i])
//     ) {
//       if (typeof params[i] === 'object') {
//         if (params[i].length > 0) {
//           items.push(`${i}=${params[i]}`);
//         }
//       } else {
//         items.push(`${i}=${params[i]}`);
//       }
//     }
//   }
//   return items.join('&');
// }

// 查询模块、实体、视图树形列表
// /inter-api/foundation/projectButton/viewTree
export function signTree(data) {
  return axios({
    url: '/inter-api/signature/foundation/projectButton/viewTree',
    method: 'POST',
    data
  });
}

// 按钮详情
// /inter-api/foundation//projectButton/buttonListQuery
export function btnDetail(data) {
  return axios({
    url: '/inter-api/signature/foundation/projectButton/buttonListQuery',
    method: 'POST',
    data
  });
}

// 是否启用签名
// /inter-api/foundation/projectButton/signatureConfig/save
export function enableSign(data) {
  return axios({
    url: '/inter-api/signature/foundation/projectButton/signatureConfig/save',
    method: 'POST',
    data
  });
}

// 查询签名日志
// /inter-api/signature/signatureLogListQuery
export function querySignLog(data) {
  return axios({
    url: '/inter-api/signature/signatureLogListQuery',
    method: 'POST',
    data
  });
}

// 导出签名日志
// /inter-api/signature/exportSignatureLogExcel?ids=acce6e3dec42478e8262a6bc27591546,9b89ea7f4e794348affae40e5d932c38
export function exportSignLOg(data) {
  return axios({
    url: '/inter-api/signature/log/export',
    method: 'POST',
    data
  });
}

// 导出状态查询
// /inter-api/signature/log/export/status
export function pollingStatus(id) {
  return axios.get(`/inter-api/signature/log/export/status?id=${id}`);
}

// 文件下载
// /inter-api/signature/log/export
export function downfile(id) {
  return axios({
    method: 'GET',
    url: `/inter-api/signature/log/export?id=${id}`,
    responseType: 'blob',
    timeout: 60e3
  });
}
