/*
 * @Author: DWP
 * @Date: 2020-08-05 09:41:56
 * @LastEditors: DWP
 * @LastEditTime: 2020-08-05 13:24:10
 */
import { request } from 'sup-rc-utility';

const basePath = '/inter-api/auth/v1';

// 获取在线用户列表
export function getList(params) {
  return request.get(`${basePath}/online-user`, {
    params: { ...params }
  });
}

// 注销用户
export async function logout(params) {
  return request({
    method: 'DELETE',
    url: `${basePath}/online-user`,
    params
  });
}
