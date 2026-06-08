/*
 * @Author: DWP
 * @Date: 2020-07-27 16:59:27
 * @LastEditors: DWP
 * @LastEditTime: 2020-09-11 16:25:45
 */
import { request } from 'sup-rc-utility';

const basePath = '/inter-api/systemcode/v1';

// 获取APP列表
export function getTree() {
  return request.get(`${basePath}/modules`);
}

// 获取编码列表
export function getList({ selectedKey, ...params }) {
  return request.get(`${basePath}/entities`, {
    params: { ...params, moduleId: selectedKey }
  });
}

// 新增编码
export function addItem(params) {
  return request.post(`${basePath}/entity`, {
    ...params
  });
}

// 修改编码
export function editItem(params) {
  return request.put(`${basePath}/entity`, {
    ...params
  });
}

// 批量删除编码
export function deleteItem({ list = [] }) {
  return request.delete(`${basePath}/entities/${list.join(',')}`);
}
