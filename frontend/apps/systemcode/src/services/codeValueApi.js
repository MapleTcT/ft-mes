/*
 * @Author: DWP
 * @Date: 2020-07-27 16:59:27
 * @LastEditors: DWP
 * @LastEditTime: 2020-09-17 17:43:32
 */
import { request } from 'sup-rc-utility';

const basePath = '/inter-api/systemcode/v1';

// 获取父节点树
export function getTree(params) {
  return request.get(`${basePath}/value/tree`, {
    params: { ...params }
  });
}

// 获取编码值列表
export function getList({ selectedKey, ...params }) {
  return request.get(`${basePath}/values`, {
    params: { ...params }
  });
}

// 根据父节点获取编码值列表
export function getListByParent({ selectedKey, ...params }) {
  return request.get(`${basePath}/value/nodes`, {
    params: { ...params, parentId: selectedKey }
  });
}

// 新增编码值
export function addCodeValue(params) {
  return request.post(`${basePath}/value`, {
    ...params
  });
}

// 修改编码值
export function editCodeValue(params) {
  return request.put(`${basePath}/value`, {
    ...params
  });
}

// 批量删除编码值
export function deleteItem(params) {
  return request.delete(`${basePath}/${params.entityCode}/values/${params.list.join(',')}`, {
    entityCode: params.entityCode,
    list: params.list
  });
}

// 拖拽排序
export function sort(params) {
  return request.put(`${basePath}/value/sort`, {
    ...params
  });
}
