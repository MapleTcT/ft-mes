/*
 * @Author: DWP
 * @Date: 2020-08-06 09:29:21
 * @LastEditors: DWP
 * @LastEditTime: 2020-08-07 10:12:06
 */
import { request } from 'sup-rc-utility';

const basePath = '/inter-api/auth/v1';

// 获取ip名单列表
export async function getList(params) {
  return request.get(`${basePath}/ip-black-white/list`, {
    params: { ...params }
  });
}

// 判断是否包含自身
export async function checkIncludeSelf(params) {
  return request.post(`${basePath}/ip-black-white/check/tip`, {
    ...params
  });
}

// 新增IP
export async function addIP(params) {
  return request.post(`${basePath}/ip-black-white`, {
    ...params
  });
}

// 删除IP
export async function deleteIP({ ids }) {
  return request.delete(`${basePath}/ip-black-white?ids=${ids}`);
}
