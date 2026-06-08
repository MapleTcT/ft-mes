/* eslint-disable spaced-comment */
import axios from './request';

function stringify(params) {
  const items = [];
  for (const i in params) {
    if (
      Object.prototype.hasOwnProperty.call(params, i) && (typeof params[i] === 'boolean' || params[i])
    ) {
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

// 新增部门
// http://localhost:8889/inter-api/organization/v1/department
export function addDepartment(data) {
  return axios({
    url: '/inter-api/organization/v1/department',
    method: 'POST',
    data
  });
}

// 修改部门
// http://localhost:8889/inter-api/organization/v1/department
export function editDepartment(data) {
  return axios({
    url: '/inter-api/organization/v1/department',
    method: 'PUT',
    data
  });
}

// 部门详情
// http://localhost:8889/inter-api/organization/v1/department?id=1
export function departmentDetail(id) {
  return axios({
    url: `/inter-api/organization/v1/department?id=${id}`,
    method: 'GET'
  });
}

// 删除部门
// http://localhost:8889/inter-api/organization/v1/department/{id}
export function removeDepartment(id) {
  return axios({
    url: `/inter-api/organization/v1/department/${id}`,
    method: 'DELETE'
  });
}

// 部门树查询
//http://localhost:8889/inter-api/organization/v1/departments?companyId=1&parentId=2&keyword=研发
export function departmentTree(params) {
  return axios.get(
    `/inter-api/organization/v1/departments?${stringify(params)}`,
  );
}

// 移动部门位置
// /inter-api/organization/v1/department/location
export function moveDep(params) {
  const data = {
    ...params
  };
  return axios({
    url: '/inter-api/organization/v1/department/location',
    method: 'PUT',
    data
  });
}

// 查询部门关联的人员
// /inter-api/organization/v1/department/person
export function getDepRelatedPerson(params) {
  return axios.get(`/inter-api/organization/v1/department/person?${stringify(params)}`);
}

// 部门模糊查询列表
// /inter-api/organization/v1/department/keyword
export function depSearch(params) {
  return axios.get(`/inter-api/organization/v1/department/keyword?${stringify(params)}`);
}

// 新增岗位
// http://localhost:8889/inter-api/organization/v1/position
export function addPosition(data) {
  return axios({
    url: '/inter-api/organization/v1/position',
    method: 'POST',
    data
  });
}

// 修改岗位信息
// http://localhost:8889/inter-api/organization/v1/position
export function editPosition(data) {
  return axios({
    url: '/inter-api/organization/v1/position',
    method: 'PUT',
    data
  });
}

// 岗位详情
// http://localhost:8889/inter-api/organization/v1/position?id=1
export function positionDetail(id) {
  return axios({
    url: `/inter-api/organization/v1/position?id=${id}`,
    method: 'GET'
  });
}

// 删除岗位
//http://localhost:8889/inter-api/organization/v1/position/{id}
export function removePosition(id) {
  return axios({
    url: `/inter-api/organization/v1/position/${id}`,
    method: 'DELETE'
  });
}

// 岗位树查询
// http://localhost:8889/inter-api/organization/v1/positions?companyId=1&keyword=研发
export function positionTree(params) {
  return axios.get(`/inter-api/organization/v1/positions?${stringify(params)}`);
}

// 移动岗位位置
// /inter-api/organization/v1/position/location
export function movePosition(params) {
  const data = {
    ...params
  };
  return axios({
    url: '/inter-api/organization/v1/position/location',
    method: 'PUT',
    data
  });
}

// 岗位关联角色
// http://localhost:8889/inter-api/organization/v1/position/role
export function relatedRoles(data) {
  return axios({
    url: '/inter-api/organization/v1/position/role',
    method: 'POST',
    data
  });
}

// 查询岗位关联的角色
export function searchPositionRoles(id) {
  return axios({
    url: `/inter-api/organization/v1/position/role?positionId=${id}`,
    method: 'GET'
  });
}

// 删除岗位关联的角色
export function removePositionRole(params) {
  return axios.delete(
    `/inter-api/organization/v1/position/role?${stringify(params)}`,
  );
}

// 查询岗位关联的人员
// /inter-api/organization/v1/position/person
export function getPositionRelatedPerson(params) {
  return axios.get(`/inter-api/organization/v1/position/person?${stringify(params)}`);
}

// 岗位模糊查询列表
// /inter-api/organization/v1/position/keyword
export function posSearch(params) {
  return axios.get(`/inter-api/organization/v1/position/keyword?${stringify(params)}`);
}

// 导出
export function exportFile(params) {
  return axios.get(`/inter-api/organization/v1/excel?${stringify(params)}`);
}

// 导入
// /inter-api/organization/v1/excel
export function importFile(params) {
  return `/inter-api/organization/v1/excel?${stringify(params)}`;
}

// 导入导出状态查询
// /inter-api/organization/v1/excel/status
export function importStatus(id) {
  return axios.get(`/inter-api/organization/v1/excel/status?id=${id}`);
}

// 文件下载
// /inter-api/organization/v1/excel/file
export function fileDown(id) {
  return axios.get(`/inter-api/organization/v1/excel/file?id=${id}`);
}

// 模板下载
// /inter-api/organization/v1/excel/template
// export function templateDown(type) {
//   return `/inter-api/organization/v1/excel/template?type=${type}`;
// }
export function templateDown(type) {
  return axios({
    method: 'GET',
    url: `/inter-api/organization/v1/excel/template?type=${type}`,
    responseType: 'blob',
    timeout: 60e3
  });
}

// 删除岗位关联人员
// /inter-api/organization/v1/position/{positionId}/person/{personIds}
export function delPosPerson(positionId, personIds) {
  return axios.delete(`/inter-api/organization/v1/position/${positionId}/person/${personIds}`);
}

// 按钮权限
export function getAuthority(code) {
  return axios.get(`/inter-api/rbac/v1/userPermission/findUserOperate?menuInfoCode=${code}`);
}

// 下载文件流
export function downfile(id) {
  return axios({
    method: 'GET',
    url: `/inter-api/organization/v1/excel/file?id=${id}`,
    responseType: 'blob',
    timeout: 60e3
  });
}
