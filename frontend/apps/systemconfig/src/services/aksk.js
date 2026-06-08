/*
 * @Author: DWP
 * @Date: 2020-11-04 16:28:22
 * @LastEditors: DWP
 * @LastEditTime: 2020-11-16 16:11:41
 */
import { request } from 'sup-rc-utility';

const basePath = '/inter-api/supos-iam/v1/iam/aksk';

// 获取凭证管理列表
export function getList(params) {
  return request.get(`${basePath}s`, {
    params: { ...params }
  });
}

// 新增凭证
export function addAksk(params) {
  return request.post(`${basePath}`, {
    ...params
  });
}

// 修改凭证
export function editAksk(params) {
  return request.put(`${basePath}/${params.id}`, {
    ...params
  });
}

// 删除凭证
export function deleteAksk(params) {
  return request.post(`${basePath}/ids`, {
    ...params
  });
}

// 下载凭证
export function downfile({ id }) {
  return request.get(`/inter-api/supos-iam/v1/iam/aksk/file?id=${id}`, {
    responseType: 'blob',
    timeout: 60e3
  });
}
