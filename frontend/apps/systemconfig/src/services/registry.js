/*
 * @Author: DWP
 * @Date: 2020-11-04 16:28:22
 * @LastEditors: DWP
 * @LastEditTime: 2020-12-11 09:18:48
 */
import { request } from 'sup-rc-utility';

const basePath = '/inter-api/installer/v1/registry';

// 查询状态
export function checkStatus(params) {
  return request.get(`${basePath}/status`, {
    params: { ...params }
  });
}

// 获取镜像管理列表
export function getList(params) {
  return request.get(`${basePath}/catalog`, {
    params: { ...params }
  });
}

// 新增镜像
export function addMirror(params) {
  return request.post(`${basePath}/push`, {
    ...params
  });
}

// 删除镜像
export function deleteMirror(params) {
  return request.post(`${basePath}/delete`, {
    ...params
  });
}

// 开始下载镜像
export function startDownfile(params) {
  return request.get('/inter-api/installer/v1/registry/download', {
    params: { ...params }
  });
}

// 下载镜像
export function downfile(params) {
  return request.get(`/inter-api/installer/v1/registry/download/${params.taskId}`, {
    responseType: 'blob',
    timeout: 60e3
  });
}
