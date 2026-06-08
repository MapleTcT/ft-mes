import { request } from 'sup-rc-utility';

// 查询用户目录列表
export function fetchUserDirs(params, data) {
  return request({
    method: 'POST',
    url: '/inter-api/auth/v1/user-directories',
    params,
    data
  });
}

// 新增用户目录
export function createUserDir(data) {
  return request({
    method: 'POST',
    url: '/inter-api/auth/v1/user-directory',
    data
  });
}

// 删除用户目录
export function removeUserDir(params) {
  return request({
    method: 'DELETE',
    params,
    url: '/inter-api/auth/v1/user-directory'
  });
}

// 测试用户目录连接
export function connectUserDir(data) {
  return request({
    method: 'POST',
    url: '/inter-api/auth/v1/user-directory/connect',
    data
  });
}

// 获取用户目录
export function getUserDir(id) {
  return request({
    method: 'GET',
    url: `/inter-api/auth/v1/user-directory/${id}`
  });
}

// 更新用户目录
export function updateUserDir(id, data) {
  return request({
    method: 'PUT',
    url: `/inter-api/auth/v1/user-directory/${id}`,
    data
  });
}

// 启用/禁用用户目录
export function enableUserDir(id, enabled) {
  return request({
    method: 'PUT',
    url: `/inter-api/auth/v1/user-directory/${id}/enable/${enabled}`
  });
}

// 更新用户目录排序
export function updateUserDirSort(id, direction) {
  return request({
    method: 'PUT',
    url: `/inter-api/auth/v1/user-directory/${id}/sort/${direction}`
  });
}
