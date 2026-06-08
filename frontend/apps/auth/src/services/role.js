import { request } from 'sup-rc-utility';

// 获取角色标签树
export function fetchRoleTagTree(params) {
  return request({
    method: 'GET',
    url: '/inter-api/rbac/v1/roles/tree',
    params
  });
}

export function fetchNoTagRoleTree(params) {
  return request({
    method: 'GET',
    url: '/inter-api/rbac/v1/roles/tree/notag',
    params
  });
}

// 新增角色
export function addRole(data) {
  return request({
    method: 'POST',
    url: '/inter-api/rbac/v1/role',
    data
  });
}

// 修改角色
export function updateRole(data) {
  return request({
    method: 'PUT',
    url: '/inter-api/rbac/v1/role',
    data
  });
}

// 删除角色
export function removeRole(roleCode) {
  return request({
    method: 'DELETE',
    url: `/inter-api/rbac/v1/role/${roleCode}`
  });
}

// 所有角色用户列表
export function fetchRoleUser(params) {
  let { keyword } = params;
  keyword = encodeURIComponent(keyword || '');
  delete params.keyword;
  return request({
    method: 'GET',
    url: `/inter-api/rbac/v1/roleUsers?keyword=${keyword}`,
    params
  });
}

// 添加角色用户关联
export function addUserRoleConnection(data) {
  return request({
    method: 'POST',
    url: '/inter-api/rbac/v1/roleUser',
    data
  });
}

// 解除用户角色关联
export function removeUserRoleConnection(userIds) {
  return request({
    method: 'DELETE',
    url: `/inter-api/rbac/v1/roleUser/${userIds}`
  });
}

// 获取标签
export function fetchTagsList() {
  return request({
    method: 'GET',
    url: '/inter-api/rbac/v1/tag/findTagsName'
  });
}

// 获取角色详细信息
export function fetchRolDetail(params) {
  return request({
    method: 'GET',
    url: '/inter-api/rbac/v1/role/findOne',
    params
  });
}

// 角色搜索
export function searchRoleByKeyword(params) {
  let { keyword } = params;
  keyword = encodeURIComponent(keyword || '');
  return request({
    method: 'GET',
    url: `/inter-api/rbac/v1/roles/findRoleByKeyword?keyword=${keyword}`
  });
}

// 角色搜索无标签
export function searchNoTagRoleByKeyword(params) {
  let { keyword } = params;
  keyword = encodeURIComponent(keyword || '');
  return request({
    method: 'GET',
    url: `/inter-api/rbac/v1/roles/findRoleByKeyword/notag?keyword=${keyword}`
  });
}

export function getSearchRoleByKeywordAPI() {
  return '/inter-api/rbac/v1/roles/associate';
}

// 发起导出任务
export function createExportTask(data) {
  return request({
    method: 'POST',
    url: '/inter-api/rbac/v1/roleUser/createTemp',
    data
  });
}

// 查询导出状态
export function queryExportStatus(params) {
  return request({
    method: 'GET',
    url: '/inter-api/rbac/v1/roleUser/getFileStatus',
    params
  });
}

// 获取excel下载链接
export function downloadExcel(id) {
  return `/inter-api/rbac/v1/roleUser/downloadFile?id=${id}`;
}

// 公司树查询
export function companyTree() {
  return request.get('/inter-api/organization/v1/companies/sub');
}
