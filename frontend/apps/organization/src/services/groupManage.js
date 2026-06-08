import axios from './request';

// 新增
export function addGroup(data) {
  return axios({
    url: '/inter-api/organization/v1/group',
    method: 'POST',
    data
  });
}

// 修改
export function editGroup(data) {
  return axios({
    url: '/inter-api/organization/v1/group',
    method: 'PUT',
    data
  });
}

// 详情
export function fetchGroupDetail(id) {
  return axios({
    url: `/inter-api/organization/v1/group?id=${id}`,
    method: 'GET'
  });
}

// 删除
export function removeGroup(id) {
  return axios({
    url: `/inter-api/organization/v1/group/${id}`,
    method: 'DELETE'
  });
}

// 列表
export function fetchGroups(params) {
  let { keyword } = params;
  keyword = encodeURIComponent(keyword || '');
  delete params.keyword;
  return axios({
    url: `/inter-api/organization/v1/groups?keyword=${keyword}`,
    method: 'GET',
    params
  });
}

export function getFetchGroupsUrl() {
  return '/inter-api/organization/v1/groups';
}

// 组搜索
export function searchGroup(data) {
  return axios({
    url: '/inter-api/organization/v1/groups/search',
    method: 'POST',
    data
  });
}

export function getSearchGroupUrl() {
  return '/inter-api/organization/v1/groups/search';
}

// 公司树查询
export function companyTree() {
  return axios.get('/inter-api/organization/v1/companies/sub/ref');
}

// 删除公司树结点
export function delCompanyTree(id) {
  return axios.delete(`/inter-api/organization/v1/company/${id}`);
}

// 新增, 修改公司树结点
export function addCompanyTree(params) {
  return axios.post('/inter-api/organization/v1/company', params);
}

// 新增, 修改公司树结点
export function updateCompanyTree(params) {
  return axios.put('/inter-api/organization/v1/company', params);
}

// 查询公司标签
export function companyTags() {
  return axios.get('/inter-api/organization/v1/company/tag');
}

// 组解除关联人员
export function groupDisconnectPerson(groupId, personIds) {
  return axios({
    url: `/inter-api/organization/v1/group/${groupId}/person/${personIds}`,
    method: 'DELETE'
  });
}

// 组关联人员
export function groupConnectPerson(data) {
  return axios({
    url: '/inter-api/organization/v1/group/person',
    method: 'POST',
    data
  });
}

// 组搜索关联人员
export function searchGroupPerson(params) {
  return axios({
    url: '/inter-api/organization/v1/group/person',
    method: 'GET',
    params
  });
}
