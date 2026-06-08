import { request } from 'sup-rc-utility';

// 国际化列表查询
export function fetchIntlList(data) {
  return request({
    method: 'POST',
    url: '/inter-api/i18n/v1/resource/code/page',
    data
  });
}

// 国际化修改
export function updateIntl(data) {
  return request({
    method: 'PUT',
    url: '/inter-api/i18n/v1/resource/code',
    data
  });
}

// 国际化新建
export function addIntl(data) {
  return request({
    method: 'POST',
    url: '/inter-api/i18n/v1/resource/code',
    data
  });
}

// 国际化删除
export function removeIntl(key) {
  return request({
    method: 'DELETE',
    url: `/inter-api/i18n/v1/resource/code/${key}`
  });
}

// 国际化语言查询
export function fetchIntlLans() {
  return request({
    method: 'GET',
    url: '/inter-api/i18n/v1/language/all'
  });
}

// 国际化语言修改
export function updateIntlLans(data) {
  return request({
    method: 'PUT',
    url: '/inter-api/i18n/v1/language/code',
    data
  });
}

// 模块列表
export function fetchModuleList() {
  return request({
    method: 'GET',
    url: '/inter-api/i18n/v1/resource/modules'
  });
}

// 新建获取国际化key
export function createI18nKey(params) {
  return request({
    method: 'GET',
    url: '/inter-api/i18n/v1/resource/initkey',
    params
  });
}

// 上传
export function uploadIntls(data) {
  return request({
    method: 'POST',
    url: '/inter-api/i18n/v1/resource/file',
    data
  });
}

// 获取导入状态
export function checkUploadStatus(params) {
  return request({
    method: 'GET',
    url: '/inter-api/i18n/v1/resource/file/heart',
    params
  });
}

// 下载导入错误文件
export function getImportErrorFile(id) {
  return `/inter-api/i18n/v1/resource/file/error?id=${id}`;
}

// 发起导出请求
export function createDownloadExcel(data) {
  return request({
    method: 'POST',
    url: '/inter-api/i18n/v1/resource/export/download/file',
    data
  });
}

// 获取导出状态
export function checkDownloadStatus(params) {
  return request({
    method: 'GET',
    url: '/inter-api/i18n/v1/resource/export/download/heart',
    params
  });
}

// 下载
export function downloadExcel(params) {
  return request({
    method: 'GET',
    url: '/inter-api/i18n/v1/resource/export/download',
    params,
    responseType: 'blob',
    timeout: 60e3
  });
}

// 国际化列筛选联想
export function sugRec() {
  return '/inter-api/i18n/v1/resource/code/page/sugrec';
}

// 国际化key是否存在校验
export function i18nKeyExist(params) {
  return request({
    method: 'GET',
    url: '/inter-api/i18n/v1/resource/code/page/key/exist',
    params
  });
}

export function getAuthority(code) {
  return request.get(
    `/inter-api/rbac/v1/userPermission/findUserOperate?menuInfoCode=${code}`
  );
}
