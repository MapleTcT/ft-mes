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

// 查询岗位树
export function getPostTree(params) {
  return axios.get(`/inter-api/organization/v1/positions/ref?${stringify(params)}`);
}

// 查询岗位人员
export function getPostPerson(params) {
  return axios.get(`/inter-api/organization/v1/position/person?${stringify(params)}`);
}

// 根据岗位新增人员
export function addPerson(params) {
  return axios.post('/inter-api/organization/v1/person', params);
}

// 修改岗位人员
export function updatePerson(params) {
  return axios.put('/inter-api/organization/v1/person', params);
}

// 删除岗位人员
export function delPerson(ids) {
  return axios.delete(`/inter-api/organization/v1/person/${ids}`);
}

// 查询部门人员树
export function getDepartmentTree(params) {
  return axios.get(`/inter-api/organization/v1/departments/ref?${stringify(params)}`);
}

// 部门查询人员
export function getDepartmentPerson(params) {
  return axios.get(`/inter-api/organization/v1/department/person?${stringify(params)}`);
}

// 上传人员路径
export function uploadUrl(params) {
  return `/inter-api/organization/v1/excel?${stringify(params)}`;
}

// 修改初始化页面，性别状态
export function updateInitPage(params) {
  return axios.get(`/inter-api/organization/v1/person/modify/page?${stringify(params)}`);
}

// 岗位调入
export function transferInPost(params) {
  return axios.post('/inter-api/organization/v1/person/transfer/position', params);
}

// 获取系统编码，性别，岗位状态
export function getSysCode(params) {
  return axios.get(`/inter-api/systemcode/v1/values/list?${stringify(params)}`);
}

// 导出xls
export function exportXls(params) {
  return axios.get(`/inter-api/organization/v1/excel?${stringify(params)}`);
}

// 模板下载
export function template() {
  return axios({
    method: 'GET',
    url: '/inter-api/organization/v1/excel/template?type=Person',
    responseType: 'blob',
    timeout: 60e3
  });
}

// 查询导入状态
export function importStatus(id) {
  return axios.get(`/inter-api/organization/v1/excel/status?id=${id}`);
}

// 模糊查询岗位
export function keywordPosition(params) {
  return axios.get('/inter-api/organization/v1/position/keyword/ref', {
    params
  });
}

// 模糊查询部门
export function keywordDepartment(params) {
  return axios.get(`/inter-api/organization/v1/department/keyword/ref?${stringify(params)}`);
}

// 查询人员关联岗位
export function getStaffPosition(params) {
  return axios.get(`/inter-api/organization/v1/person/position?${stringify(params)}`);
}

// 岗位调离
export function transferOut(params) {
  return axios.post('/inter-api/organization/v1/person/off/position?', params);
}
// 按钮权限获取
export function getAuthority(code) {
  return axios.get(`/inter-api/rbac/v1/userPermission/findUserOperate?menuInfoCode=${code}`);
}
// axios下载文件
export function downfile(id) {
  return axios({
    method: 'GET',
    url: `/inter-api/organization/v1/excel/file?id=${id}`,
    responseType: 'blob',
    timeout: 60e3
  });
}

// 查图片
export function getImg(filePaths) {
  return axios({
    method: 'POST',
    responseType: 'json',
    url: '/inter-api/organization/v1/persons/downloadImage',
    params: {
      filePaths
    }
  });
}

// 上传图片
export function uploadImg(params) {
  return axios({
    method: 'POST',
    url: '/inter-api/organization/v1/persons/image',
    ...params
  });
}
