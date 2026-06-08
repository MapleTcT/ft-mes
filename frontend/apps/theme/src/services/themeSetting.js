import { request } from 'sup-rc-utility';

// 查询系统主题
export function getSystemThemes() {
  return request({
    url: '/inter-api/theme/v1/systemThemes',
    method: 'GET'
  });
}

// 修改系统主题接口
export function updateSystemTheme(params) {
  return request({
    url: '/inter-api/theme/v1/systemTheme',
    method: 'PUT',
    data: params
  });
}

// 查询个人主题接口
export function getPersonalTheme() {
  return request({
    url: '/inter-api/theme/v1/personalTheme',
    method: 'GET'
  });
}

// 修改/删除 个人主题接口
export function updatePersonalTheme(params, type) {
  return request({
    url: '/inter-api/theme/v1/personalTheme',
    method: type || 'PUT',
    data: params
  });
}

// 上传接口
export function getUpload(params) {
  return request({
    url: '/inter-api/theme/v1/upload',
    method: 'POST',
    ...params
  });
}
