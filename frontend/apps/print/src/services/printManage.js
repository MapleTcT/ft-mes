import { request } from 'sup-rc-utility';
import { getSource } from '../utils/index.js';

const source = getSource();

// 左侧树
// appName
export function queryApp(params) {
  return request({
    url: `/inter-api/printer/v1/apps?source=${source}`,
    method: 'GET',
    params
  });
}

// 打印模板列表
export function tableData(params) {
  const { code, pagination } = params || {};
  return request({
    url: `/inter-api/printer/v1/templates/${code}`,
    method: 'GET',
    params: { ...pagination }
  });
}

// 详情查看
export function tableDetail(params) {
  return request({
    url: `/inter-api/printer/v1/template/${params.templateId}`,
    method: 'GET'
    // params
  });
}

// 打印模板新增保存
export function saveData(params, type) {
  const url = '/inter-api/printer/v1/template';
  return request({
    url,
    method: type === 'new' ? 'POST' : 'PUT',
    data: params
  });
}

// 打印模板批量删除
export function deleteData(params) {
  return request({
    url: `/inter-api/printer/v1/templates/${params.id}`,
    method: 'DELETE'
    // params
  });
}

// 标签新增保存
export function addTag(params) {
  return request({
    url: '/inter-api/printer/v1/label',
    method: 'POST',
    data: params
  });
}

// 标签列表
export function queryTags(params) {
  return request({
    url: '/inter-api/printer/v1/labels',
    method: 'GET',
    params
  });
}

// 获取关联页面列表
export function queryAlignList(params) {
  return request({
    url: `/inter-api/printer/v1/pages?source=${source}`,
    method: 'GET',
    params
  });
}

// 复制
export function copyData(params) {
  return request({
    url: '/inter-api/printer/v1/template/copy',
    method: 'PUT',
    data: params
  });
}

// 批量修改状态
// state-1 已发布 2 未发布 3 已修改 4 已停用
export function modifyState(params) {
  return request({
    url: '/inter-api/printer/v1/template/batch',
    method: 'PUT',
    data: params
  });
}
