import { request } from 'sup-rc-utility';

// 查询第三方认证列表
export function fetchAuthList() {
  return request({
    method: 'GET',
    url: '/inter-api/auth/v1/identityProviders'
  });
}

// 查询第三方认证列表
export function searchAuth(params) {
  return request({
    method: 'GET',
    url: '/inter-api/auth/v1/identityProviders/keyword',
    params
  });
}

// 新增第三方认证
export function addAuthApp(data) {
  return request({
    method: 'POST',
    url: '/inter-api/auth/v1/identityProviders/add',
    data
  });
}

// 删除第三方认证
export function removeAuthApp(params) {
  const { ids } = params;
  return request({
    method: 'DELETE',
    url: `/inter-api/auth/v1/identityProviders/${ids}`
  });
}

// 修改第三方认证
export function updateAuthApp(appId, data) {
  return request({
    method: 'POST',
    url: `/inter-api/auth/v1/identityProviders/${appId}/update`,
    data
  });
}

// 获取第三方认证详细信息
export function getAuthDetailInfo(appId) {
  return request({
    method: 'GET',
    url: `/inter-api/auth/v1/identityProviders/${appId}`
  });
}

// 认证启停
export function enableAuthApp(appId, status) {
  return request({
    method: 'POST',
    url: `/inter-api/auth/v1/identityProviders/${appId}/${status}`
  });
}
