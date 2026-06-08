/*
 * @Author: DWP
 * @Date: 2020-07-21 11:24:56
 * @LastEditors: DWP
 * @LastEditTime: 2020-11-05 09:53:59
 */
import { request } from 'sup-rc-utility';

const basePath = '/inter-api/rbac/v1';

// 筛选菜单树
export function queryMenus(params) {
  return request.get(`${basePath}/menus/findByKeyword`, {
    params: { ...params }
  });
}

// 获取菜单树
export function getMenuTree(params) {
  return request.get(`${basePath}/menus`, {
    params: { ...params }
  });
}

// 筛选Supos菜单树
export function querySuposMenus(params) {
  return request.get(`${basePath}/resources/runtime/findByKeyword`, {
    params: { ...params }
  });
}

// 获取Supos菜单树
export function getSuposMenuTree(params) {
  return request.get(`${basePath}/resources/runtime`, {
    params: { ...params }
  });
}

// 新增菜单
export function addMenu(params) {
  return request.post(`${basePath}/menu`, {
    ...params
  });
}

// 修改菜单
export function updateMenu(params) {
  return request.put(`${basePath}/menu`, {
    ...params
  });
}

// 删除菜单
export function deleteMenu({ codes }) {
  return request.delete(`${basePath}/menus/${codes}`);
}

// 排序菜单
export function sort(params) {
  return request.put(`${basePath}/menu/sort`, {
    ...params
  });
}

// 启用停用菜单
export function modifyEnableStatus(params) {
  return request.put(`${basePath}/menu/modifyEnableStatus`, {
    ...params
  });
}

// 获取操作列
export function getOptList(params) {
  return request.get(`${basePath}/menuOperatePage`, {
    params: { ...params }
  });
}

// 新增操作
export function addOpt(params) {
  return request.post(`${basePath}/menuOperate`, {
    ...params
  });
}

// 修改操作
export function updateOpt(params) {
  return request.put(`${basePath}/menuOperate`, {
    ...params
  });
}

// 删除操作
export function deleteOpt({ codes }) {
  return request.delete(`${basePath}/menuOperates/${codes}`);
}

// 获取适用范围
export function getRangeValue(moduleId) {
  return request.delete(`/msService/ec/module/companyRef?moduleId=${moduleId}`);
}
