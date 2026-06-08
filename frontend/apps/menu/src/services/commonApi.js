/*
 * @Author: DWP
 * @Date: 2020-07-21 11:24:56
 * @LastEditors: DWP
 * @LastEditTime: 2020-09-10 19:07:27
 */
import { request } from 'sup-rc-utility';

const basePath = '/inter-api';

// 获取App列表
export function getApps() {
  return request.get(`${basePath}/module-registry/v1/modules`);
}

// 获取公司树
export function getCompanyTree() {
  return request.get(`${basePath}/organization/v1/companies/ref`);
}

// 获取权限
export function getAuthority({ code }) {
  return request.get(`${basePath}/rbac/v1/userPermission/findUserOperate?menuInfoCode=${code}`);
}
