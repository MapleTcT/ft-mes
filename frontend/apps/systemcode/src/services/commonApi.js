/*
 * @Author: DWP
 * @Date: 2020-07-21 11:24:56
 * @LastEditors: DWP
 * @LastEditTime: 2020-09-11 16:11:06
 */
import { request } from 'sup-rc-utility';

const basePath = '/inter-api';

// 获取APP列表
export function getTree() {
  return request.get(`${basePath}/module-registry/v1/modules`);
}

// 获取权限
export function getAuthority({ code }) {
  return request.get(`${basePath}/rbac/v1/userPermission/findUserOperate?menuInfoCode=${code}`);
}
