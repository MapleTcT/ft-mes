/*
 * @Author: DWP
 * @Date: 2020-07-21 11:24:56
 * @LastEditors: DWP
 * @LastEditTime: 2020-08-13 15:21:14
 */
import { request } from 'sup-rc-utility';

const basePath = '/inter-api';

// 获取权限
export function getAuthority({ code }) {
  return request.get(`${basePath}/rbac/v1/userPermission/findUserOperate?menuInfoCode=${code}`);
}
