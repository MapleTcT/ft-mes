import axios from './request';

function stringify(params) {
  const items = [];
  for (const i in params) {
    if (Object.prototype.hasOwnProperty.call(params, i) && (typeof params[i] === 'boolean' || params[i])) {
      if (typeof params[i] === 'object') {
        if (params[i].length > 0) {
          items.push(`${i}=${params[i].map((item) => encodeURIComponent(item))}`);
        }
      } else {
        items.push(`${i}=${encodeURIComponent(params[i])}`);
      }
    }
  }
  return items.join('&');
}
// 公司树查询
export function companyTree(params) {
  return axios.get(`/inter-api/organization/v1/companies/sub/ref?${stringify(params)}`);
}
// 模糊查询列表
export function companyKeyword(params) {
  return axios.get(`/inter-api/organization/v1/company/keyword?${stringify(params)}`);
}
// 按钮权限获取
export function getAuthority(code) {
  return axios.get(`/inter-api/rbac/v1/userPermission/findUserOperate?menuInfoCode=${code}`);
}
