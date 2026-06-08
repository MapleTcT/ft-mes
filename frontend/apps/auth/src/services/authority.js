import { request } from 'sup-rc-utility';

// 菜单树
export function getMenuTree() {
  // return axios.get('/inter-api/rbac/v1/menus');
  return request({
    method: 'GET',
    url: '/inter-api/rbac/v1/menus/ref?restrict=false'
  });
}
// 获取用户分配权限
export function getUserAuthority(params) {
  // return axios.get(`/inter-api/rbac/v1/userPermissions?${stringify(params)}`);
  return request({
    method: 'GET',
    url: '/inter-api/rbac/v1/userPermissions',
    params
  });
}
// 获取用户分配权限
export function getRoleAuthority(params) {
  // return axios.get(`/inter-api/rbac/v1/rolePermissions?${stringify(params)}`);
  return request({
    method: 'GET',
    url: '/inter-api/rbac/v1/rolePermissions',
    params
  });
}
// 删除，修改，分配权限接口
export function postUserAuthority(data) {
  // return axios.post('/inter-api/rbac/v1/userPermission', params);
  return request({
    method: 'POST',
    url: '/inter-api/rbac/v1/userPermission',
    data
  });
}
export function postRoleAuthority(data) {
  // return axios.post('/inter-api/rbac/v1/rolePermission', params);
  return request({
    method: 'POST',
    url: '/inter-api/rbac/v1/rolePermission',
    data
  });
}
// 获取所有已分配
export function getAllUserAssign(params) {
  // return axios.get(`/inter-api/rbac/v1/userPermissions/assigned?${stringify(params)}`);
  return request({
    method: 'GET',
    url: '/inter-api/rbac/v1/userPermissions/assigned',
    params
  });
}
export function getAllRoleAssign(params) {
  // return axios.get(`/inter-api/rbac/v1/rolePermissions/assigned?${stringify(params)}`);
  return request({
    method: 'GET',
    url: '/inter-api/rbac/v1/rolePermissions/assigned',
    params
  });
}
// 根据部门id批量查询岗位信息
export function getPersonInfos(params) {
  return request({
    method: 'GET',
    url: '/inter-api/organization/v1/persons/ids',
    params
  });
}
// 根据部门id批量查询岗位信息
export function getPositionInfos(params) {
  return request({
    method: 'GET',
    url: '/inter-api/organization/v1/positions/ids',
    params
  });
}
// 根据部门id批量查询部门信息
export function getDepartmentInfos(params) {
  return request({
    method: 'GET',
    url: '/inter-api/organization/v1/departments/ids',
    params
  });
}
// 临时使用树模糊查询和精确查询
export function getSearchTree(params) {
  return request({
    method: 'GET',
    url: '/inter-api/rbac/v1/menus/findByKeyword/ref',
    params
  });
}
// 继承角色权限查询
export function getFromRole(params) {
  return request({
    method: 'GET',
    url: '/inter-api/rbac/v1/userPermissions/fromRole',
    params
  });
}
// 继承角色权限查询---已分配
export function getAssignFromRole(params) {
  return request({
    method: 'GET',
    url: '/inter-api/rbac/v1/userPermissions/assigned/fromRole',
    params
  });
}
// 资源权限, 获取资源集
export function getSourceGroup() {
  return request({
    method: 'GET',
    url: '/inter-api/rbac/v1/data/resource/groups'
  });
}
// 获取角色数据权限
export function getRoleSourceAuth(roleId, code) {
  return request({
    method: 'GET',
    url: `/inter-api/rbac/v1/role/${roleId}/data/resource/${code}`
  });
}
// 保存角色数据权限
export function saveRoleSourceAuth(roleId, code, data) {
  return request({
    method: 'POST',
    url: `/inter-api/rbac/v1/role/${roleId}/data/resource/${code}`,
    data
  });
}
// 获取用户数据权限
export function getUserSourceAuth(userId, code) {
  return request({
    method: 'GET',
    url: `/inter-api/rbac/v1/user/${userId}/data/resource/${code}`
  });
}
// 保存用户数据权限
export function saveUserSourceAuth(userId, code, data) {
  return request({
    method: 'POST',
    url: `/inter-api/rbac/v1/user/${userId}/data/resource/${code}`,
    data
  });
}
// 获取资源权限列表
export function getSourceTable(url, params) {
  return request({
    method: 'GET',
    url,
    params
  });
}
// 是否加载资源权限页面
export function loadBap() {
  return request({
    method: 'GET',
    url: '/inter-api/rbac/v1/bap'
  });
}
