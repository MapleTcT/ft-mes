import { request } from 'sup-rc-utility';

// 用户列表
export function fetchUserList(params) {
  return request({
    method: 'GET',
    params,
    url: '/inter-api/auth/v1/user'
  });
}

// 用户详情
export function fetchUserDetail(id) {
  return request({
    method: 'GET',
    url: `/inter-api/auth/v1/user/${id}`
  });
}

// 新增用户
export function addNewUser(data) {
  return request({
    method: 'POST',
    data,
    url: '/inter-api/auth/v1/user'
  });
}

// 修改用户
export function editUser(data) {
  return request({
    method: 'PUT',
    url: '/inter-api/auth/v1/user',
    data
  });
}

// 删除用户
export function removeUser(data) {
  return request({
    method: 'DELETE',
    url: '/inter-api/auth/v1/user',
    data
  });
}

// 修改密码
export function updatePassword(data) {
  return request({
    method: 'PUT',
    url: '/inter-api/auth/v1/user/password',
    data
  });
}

// 锁定操作
export function lockUser(data) {
  return request({
    method: 'PUT',
    url: '/inter-api/auth/v1/user/status',
    data
  });
}

// 搜索用户
export function searchUsers(params) {
  let { keyword } = params;
  keyword = encodeURIComponent(keyword || '');
  delete params.keyword;
  return request({
    method: 'GET',
    params,
    url: `/inter-api/auth/v1/user/search?keyword=${keyword}`
  });
}

// 公司树查询
export function companyTree() {
  return request.get('/inter-api/organization/v1/companies/sub');
}

// TODO 发起导出任务
export function createExportTask(params) {
  return request({
    method: 'GET',
    url: '/inter-api/auth/v1/excel',
    params
  });
}

// TODO 查询导出状态
export function queryExportStatus(params) {
  return request({
    method: 'GET',
    url: '/inter-api/auth/v1/excel/status',
    params
  });
}

// TODO 获取excel下载链接
export function downloadExcel(id) {
  return `/inter-api/auth/v1/excel/file?id=${id}`;
}

// TODO 轮询导入状态
export function checkUploadStatus(params) {
  return request({
    method: 'GET',
    url: '/inter-api/auth/v1/excel/status',
    params
  });
}

// 获取当前用户信息
export function getUserSessionInfo() {
  return request({
    method: 'GET',
    url: '/inter-api/auth/v1/user/userSessionInfo'
  });
}
